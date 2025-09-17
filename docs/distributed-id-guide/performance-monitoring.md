# 性能调优和监控指南

## 目录

1. [性能调优策略](#1-性能调优策略)
2. [监控体系建设](#2-监控体系建设)
3. [告警和通知](#3-告警和通知)
4. [性能基准测试](#4-性能基准测试)
5. [容量规划](#5-容量规划)
6. [故障排除](#6-故障排除)

---

## 1. 性能调优策略

### 1.1 数据库层面调优

#### PostgreSQL优化配置

```sql
-- postgresql.conf 关键参数调优

-- 内存配置
shared_buffers = 256MB                    -- 设置为系统内存的25%
effective_cache_size = 1GB                -- 设置为系统内存的75%
work_mem = 4MB                            -- 单个查询操作的内存
maintenance_work_mem = 64MB               -- 维护操作内存

-- 连接配置
max_connections = 200                     -- 最大连接数
superuser_reserved_connections = 3       -- 超级用户保留连接

-- 检查点配置
checkpoint_completion_target = 0.9        -- 检查点完成目标
wal_buffers = 16MB                        -- WAL缓冲区大小
checkpoint_timeout = 300s                 -- 检查点超时时间

-- 查询调优
random_page_cost = 1.1                    -- SSD优化设置
effective_io_concurrency = 200            -- 并发I/O设置

-- 日志配置（生产环境关闭详细日志）
log_statement = 'none'                    -- 关闭SQL语句日志
log_min_duration_statement = 1000         -- 只记录超过1秒的慢查询
```

#### 索引优化

```sql
-- 1. 确保主键索引存在（自动创建）
-- ALTER TABLE leaf_alloc ADD CONSTRAINT pk_leaf_alloc PRIMARY KEY (biz_tag);

-- 2. 为更新时间创建索引（用于监控查询）
CREATE INDEX IF NOT EXISTS idx_leaf_alloc_update_time ON leaf_alloc(update_time);

-- 3. 部分索引（针对活跃业务）
CREATE INDEX IF NOT EXISTS idx_leaf_alloc_active_business
ON leaf_alloc(biz_tag)
WHERE update_time > (CURRENT_TIMESTAMP - INTERVAL '1 day');

-- 4. 分析索引使用情况
SELECT schemaname, tablename, indexname, idx_tup_read, idx_tup_fetch
FROM pg_stat_user_indexes
WHERE schemaname = 'public' AND tablename = 'leaf_alloc';

-- 5. 定期更新统计信息
ANALYZE leaf_alloc;
```

#### 连接池调优

```yaml
# application.yml - HikariCP优化配置
spring:
  datasource:
    hikari:
      # 连接池大小配置
      maximum-pool-size: 30              # 根据数据库max_connections调整
      minimum-idle: 10                   # 保持最小空闲连接

      # 连接生命周期
      max-lifetime: 1800000              # 30分钟，小于数据库超时时间
      idle-timeout: 600000               # 10分钟空闲超时

      # 连接获取
      connection-timeout: 30000          # 30秒连接超时
      validation-timeout: 5000           # 5秒验证超时

      # 连接测试
      connection-test-query: SELECT 1    # 连接测试查询
      leak-detection-threshold: 60000    # 连接泄漏检测

      # 性能优化
      auto-commit: false                 # 关闭自动提交
      read-only: false                   # 读写连接
      pool-name: LeafHikariCP            # 连接池名称
```

### 1.2 应用层面调优

#### Leaf服务优化配置

```yaml
# application.yml - Leaf性能优化
leaf:
  segment:
    enabled: true

    # 步长策略（根据业务QPS调整）
    default-step: 2000                   # 默认步长
    update-threshold: 0.85               # 85%时预加载（降低阈值减少等待）

    # 线程池配置
    thread-pool-size: 20                 # 异步更新线程池大小
    max-retries: 5                       # 最大重试次数
    retry-interval: 50                   # 重试间隔（毫秒）

    # 缓存配置
    cache-enabled: true                  # 启用本地缓存
    cache-expire-time: 3600              # 缓存过期时间（秒）

# JVM参数优化
java:
  opts: >
    -Xms1g -Xmx2g
    -XX:+UseG1GC
    -XX:MaxGCPauseMillis=200
    -XX:+HeapDumpOnOutOfMemoryError
    -XX:HeapDumpPath=/logs/heapdump.hprof
    -XX:+PrintGCDetails
    -XX:+PrintGCTimeStamps
    -Xloggc:/logs/gc.log
```

#### 业务级别步长调优

```sql
-- 根据业务QPS调整步长
-- 计算公式：step = QPS × 60秒 × 安全系数(1.5-2.0)

-- 高频业务 (QPS > 1000)
UPDATE leaf_alloc SET step = 10000 WHERE biz_tag = 'order_id';
UPDATE leaf_alloc SET step = 15000 WHERE biz_tag = 'tracking_number';

-- 中频业务 (QPS 100-1000)
UPDATE leaf_alloc SET step = 3000 WHERE biz_tag = 'shipment_id';
UPDATE leaf_alloc SET step = 2000 WHERE biz_tag = 'user_id';

-- 低频业务 (QPS < 100)
UPDATE leaf_alloc SET step = 500 WHERE biz_tag = 'truck_id';
UPDATE leaf_alloc SET step = 200 WHERE biz_tag = 'warehouse_id';

-- 查看步长使用效率
SELECT
    biz_tag,
    step,
    EXTRACT(EPOCH FROM (CURRENT_TIMESTAMP - update_time)) / 60 as minutes_since_update,
    CASE
        WHEN EXTRACT(EPOCH FROM (CURRENT_TIMESTAMP - update_time)) > 3600 THEN 'Step too large'
        WHEN EXTRACT(EPOCH FROM (CURRENT_TIMESTAMP - update_time)) < 60 THEN 'Step too small'
        ELSE 'OK'
    END as step_efficiency
FROM leaf_alloc
ORDER BY update_time DESC;
```

### 1.3 代码级别优化

#### 高效的ID生成器实现

```java
@Service
@Slf4j
public class OptimizedLeafIdGeneratorService {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private MeterRegistry meterRegistry;

    // 使用ConcurrentHashMap减少锁竞争
    private final ConcurrentHashMap<String, SegmentBuffer> cache = new ConcurrentHashMap<>();

    // 预编译SQL语句提升性能
    private static final String UPDATE_SQL = "UPDATE leaf_alloc SET max_id = max_id + step WHERE biz_tag = ?";
    private static final String SELECT_SQL = "SELECT biz_tag, max_id, step, description FROM leaf_alloc WHERE biz_tag = ?";

    // 使用对象池减少GC压力
    private final GenericObjectPool<StringBuilder> stringBuilderPool = new GenericObjectPool<>(
        new BasePooledObjectFactory<StringBuilder>() {
            @Override
            public StringBuilder create() {
                return new StringBuilder(64);
            }

            @Override
            public PooledObject<StringBuilder> wrap(StringBuilder obj) {
                return new DefaultPooledObject<>(obj);
            }

            @Override
            public void passivateObject(PooledObject<StringBuilder> pooledObject) {
                pooledObject.getObject().setLength(0); // 清空StringBuilder
            }
        }
    );

    /**
     * 优化的ID生成方法
     */
    public long nextId(String bizTag) {
        // 快速参数验证
        if (bizTag == null || bizTag.isEmpty()) {
            throw new IllegalArgumentException("bizTag cannot be null or empty");
        }

        // 使用双重检查锁定模式减少同步开销
        SegmentBuffer buffer = cache.get(bizTag);
        if (buffer == null) {
            synchronized (this) {
                buffer = cache.get(bizTag);
                if (buffer == null) {
                    buffer = new OptimizedSegmentBuffer();
                    buffer.setKey(bizTag);
                    cache.put(bizTag, buffer);
                }
            }
        }

        // 记录监控指标（使用对象池避免创建临时对象）
        Timer.Sample sample = Timer.start(meterRegistry);
        try {
            long id = buffer.nextId(bizTag, this);

            // 增加计数器（避免字符串拼接）
            meterRegistry.counter("leaf.id.generated", "biz_tag", bizTag).increment();

            return id;
        } finally {
            sample.stop(Timer.builder("leaf.id.generation").tag("biz_tag", bizTag).register(meterRegistry));
        }
    }

    /**
     * 批量更新优化
     */
    @Transactional
    public List<LeafAlloc> batchUpdateMaxIdAndGetLeafAlloc(List<String> bizTags) {
        if (bizTags.isEmpty()) {
            return Collections.emptyList();
        }

        // 批量更新
        List<Object[]> batchArgs = bizTags.stream()
            .map(bizTag -> new Object[]{bizTag})
            .collect(Collectors.toList());

        jdbcTemplate.batchUpdate(UPDATE_SQL, batchArgs);

        // 批量查询
        String inClause = bizTags.stream()
            .map(tag -> "?")
            .collect(Collectors.joining(","));

        String batchSelectSql = "SELECT biz_tag, max_id, step, description FROM leaf_alloc WHERE biz_tag IN (" + inClause + ")";

        return jdbcTemplate.query(batchSelectSql,
            new BeanPropertyRowMapper<>(LeafAlloc.class),
            bizTags.toArray());
    }
}
```

## 2. 监控体系建设

### 2.1 Prometheus监控集成

#### 自定义监控指标

```java
@Component
@RequiredArgsConstructor
public class LeafMetricsCollector {

    private final MeterRegistry meterRegistry;
    private final LeafIdGeneratorService leafIdGenerator;
    private final JdbcTemplate jdbcTemplate;

    @PostConstruct
    public void initMetrics() {
        // 注册自定义Gauge指标
        registerCustomGauges();

        // 启动定时收集任务
        startMetricsCollection();
    }

    private void registerCustomGauges() {
        // 1. 各业务的步长配置
        Gauge.builder("leaf.segment.step")
            .description("Current step size for business tag")
            .tag("type", "config")
            .register(meterRegistry, this, obj -> {
                try {
                    List<Map<String, Object>> results = jdbcTemplate.queryForList(
                        "SELECT biz_tag, step FROM leaf_alloc");
                    return results.stream().mapToInt(row -> (Integer) row.get("step")).average().orElse(0);
                } catch (Exception e) {
                    return 0;
                }
            });

        // 2. 各业务的当前最大ID
        Gauge.builder("leaf.segment.max_id")
            .description("Current max ID for business tag")
            .register(meterRegistry, this, obj -> {
                try {
                    List<Map<String, Object>> results = jdbcTemplate.queryForList(
                        "SELECT biz_tag, max_id FROM leaf_alloc");
                    return results.stream().mapToLong(row -> (Long) row.get("max_id")).average().orElse(0);
                } catch (Exception e) {
                    return 0;
                }
            });

        // 3. 数据库连接池状态
        Gauge.builder("leaf.datasource.active_connections")
            .description("Active database connections")
            .register(meterRegistry, this, obj -> getActiveConnectionCount());

        // 4. 缓存命中率
        Gauge.builder("leaf.cache.hit_ratio")
            .description("Cache hit ratio")
            .register(meterRegistry, this, obj -> calculateCacheHitRatio());
    }

    private void startMetricsCollection() {
        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

        // 每30秒收集一次详细指标
        scheduler.scheduleAtFixedRate(this::collectDetailedMetrics, 0, 30, TimeUnit.SECONDS);

        // 每5分钟收集一次业务指标
        scheduler.scheduleAtFixedRate(this::collectBusinessMetrics, 0, 5, TimeUnit.MINUTES);
    }

    private void collectDetailedMetrics() {
        try {
            // 收集各业务的活跃程度
            List<Map<String, Object>> results = jdbcTemplate.queryForList(
                "SELECT biz_tag, " +
                "EXTRACT(EPOCH FROM (CURRENT_TIMESTAMP - update_time)) as seconds_since_update " +
                "FROM leaf_alloc");

            for (Map<String, Object> row : results) {
                String bizTag = (String) row.get("biz_tag");
                Number secondsSinceUpdate = (Number) row.get("seconds_since_update");

                Gauge.builder("leaf.business.last_update_seconds")
                    .description("Seconds since last update for business")
                    .tag("biz_tag", bizTag)
                    .register(meterRegistry, () -> secondsSinceUpdate.doubleValue());
            }

        } catch (Exception e) {
            log.error("Failed to collect detailed metrics", e);
        }
    }

    private void collectBusinessMetrics() {
        try {
            // 计算各业务的ID消费速度
            List<Map<String, Object>> results = jdbcTemplate.queryForList(
                "SELECT biz_tag, step, " +
                "EXTRACT(EPOCH FROM (CURRENT_TIMESTAMP - update_time)) as seconds_since_update " +
                "FROM leaf_alloc " +
                "WHERE update_time > CURRENT_TIMESTAMP - INTERVAL '1 hour'");

            for (Map<String, Object> row : results) {
                String bizTag = (String) row.get("biz_tag");
                Integer step = (Integer) row.get("step");
                Number secondsSinceUpdate = (Number) row.get("seconds_since_update");

                if (secondsSinceUpdate.doubleValue() > 0) {
                    double idsPerSecond = step.doubleValue() / secondsSinceUpdate.doubleValue();

                    Gauge.builder("leaf.business.consumption_rate")
                        .description("ID consumption rate per second")
                        .tag("biz_tag", bizTag)
                        .register(meterRegistry, () -> idsPerSecond);
                }
            }

        } catch (Exception e) {
            log.error("Failed to collect business metrics", e);
        }
    }

    private double getActiveConnectionCount() {
        try {
            Integer count = jdbcTemplate.queryForObject(
                "SELECT numbackends FROM pg_stat_database WHERE datname = current_database()",
                Integer.class);
            return count != null ? count.doubleValue() : 0;
        } catch (Exception e) {
            return 0;
        }
    }

    private double calculateCacheHitRatio() {
        // 这里需要根据实际的缓存实现来计算
        // 示例：从内存中的统计数据计算
        return 0.95; // 示例值
    }
}
```

#### Grafana仪表板配置

```json
{
  "dashboard": {
    "id": null,
    "title": "Leaf分布式ID监控",
    "tags": ["leaf", "distributed-id", "mini-ups"],
    "timezone": "browser",
    "panels": [
      {
        "id": 1,
        "title": "ID生成QPS",
        "type": "graph",
        "targets": [
          {
            "expr": "rate(leaf_id_generated_total[5m])",
            "legendFormat": "{{biz_tag}} QPS"
          }
        ],
        "yAxes": [
          {
            "label": "QPS",
            "min": 0
          }
        ]
      },
      {
        "id": 2,
        "title": "ID生成延迟",
        "type": "graph",
        "targets": [
          {
            "expr": "histogram_quantile(0.95, rate(leaf_id_generation_seconds_bucket[5m]))",
            "legendFormat": "95th percentile"
          },
          {
            "expr": "histogram_quantile(0.50, rate(leaf_id_generation_seconds_bucket[5m]))",
            "legendFormat": "50th percentile"
          }
        ],
        "yAxes": [
          {
            "label": "延迟 (秒)",
            "min": 0
          }
        ]
      },
      {
        "id": 3,
        "title": "数据库访问频率",
        "type": "graph",
        "targets": [
          {
            "expr": "rate(leaf_db_access_total[5m])",
            "legendFormat": "DB Access Rate"
          }
        ]
      },
      {
        "id": 4,
        "title": "业务活跃度",
        "type": "table",
        "targets": [
          {
            "expr": "leaf_business_last_update_seconds",
            "format": "table",
            "instant": true
          }
        ],
        "transformations": [
          {
            "id": "organize",
            "options": {
              "columns": [
                "biz_tag",
                "Value"
              ]
            }
          }
        ]
      }
    ],
    "time": {
      "from": "now-1h",
      "to": "now"
    },
    "refresh": "30s"
  }
}
```

### 2.2 日志监控

#### 结构化日志配置

```xml
<!-- logback-spring.xml -->
<configuration>
    <springProfile name="!local">
        <!-- 生产环境：JSON格式日志 -->
        <appender name="STDOUT" class="ch.qos.logback.core.ConsoleAppender">
            <encoder class="net.logstash.logback.encoder.LoggingEventCompositeJsonEncoder">
                <providers>
                    <timestamp/>
                    <logLevel/>
                    <loggerName/>
                    <message/>
                    <mdc/>
                    <stackTrace/>
                </providers>
            </encoder>
        </appender>

        <!-- Leaf专用日志文件 -->
        <appender name="LEAF_FILE" class="ch.qos.logback.core.rolling.RollingFileAppender">
            <file>logs/leaf-id-generator.log</file>
            <rollingPolicy class="ch.qos.logback.core.rolling.SizeAndTimeBasedRollingPolicy">
                <fileNamePattern>logs/leaf-id-generator.%d{yyyy-MM-dd}.%i.gz</fileNamePattern>
                <maxFileSize>100MB</maxFileSize>
                <maxHistory>30</maxHistory>
                <totalSizeCap>3GB</totalSizeCap>
            </rollingPolicy>
            <encoder class="net.logstash.logback.encoder.LoggingEventCompositeJsonEncoder">
                <providers>
                    <timestamp/>
                    <logLevel/>
                    <loggerName/>
                    <message/>
                    <mdc/>
                    <stackTrace/>
                </providers>
            </encoder>
        </appender>

        <!-- 错误日志单独文件 -->
        <appender name="ERROR_FILE" class="ch.qos.logback.core.rolling.RollingFileAppender">
            <filter class="ch.qos.logback.classic.filter.LevelFilter">
                <level>ERROR</level>
                <onMatch>ACCEPT</onMatch>
                <onMismatch>DENY</onMismatch>
            </filter>
            <file>logs/leaf-errors.log</file>
            <rollingPolicy class="ch.qos.logback.core.rolling.SizeAndTimeBasedRollingPolicy">
                <fileNamePattern>logs/leaf-errors.%d{yyyy-MM-dd}.%i.gz</fileNamePattern>
                <maxFileSize>50MB</maxFileSize>
                <maxHistory>30</maxHistory>
            </rollingPolicy>
            <encoder class="net.logstash.logback.encoder.LoggingEventCompositeJsonEncoder">
                <providers>
                    <timestamp/>
                    <logLevel/>
                    <loggerName/>
                    <message/>
                    <mdc/>
                    <stackTrace/>
                </providers>
            </encoder>
        </appender>
    </springProfile>

    <!-- Leaf服务专用Logger -->
    <logger name="com.miniups.service.LeafIdGeneratorService" level="INFO" additivity="false">
        <appender-ref ref="LEAF_FILE"/>
        <appender-ref ref="ERROR_FILE"/>
        <appender-ref ref="STDOUT"/>
    </logger>

    <root level="INFO">
        <appender-ref ref="STDOUT"/>
    </root>
</configuration>
```

#### 关键事件日志

```java
@Component
@RequiredArgsConstructor
@Slf4j
public class LeafAuditLogger {

    private final MeterRegistry meterRegistry;

    /**
     * 记录ID生成事件
     */
    public void logIdGeneration(String bizTag, long id, long duration, boolean success, String error) {
        MDC.put("event_type", "id_generation");
        MDC.put("biz_tag", bizTag);
        MDC.put("id", String.valueOf(id));
        MDC.put("duration_ms", String.valueOf(duration));
        MDC.put("success", String.valueOf(success));

        if (success) {
            log.info("ID generated successfully for bizTag: {}, id: {}, duration: {}ms",
                    bizTag, id, duration);
        } else {
            MDC.put("error", error);
            log.error("ID generation failed for bizTag: {}, error: {}, duration: {}ms",
                    bizTag, error, duration);
        }

        MDC.clear();
    }

    /**
     * 记录数据库访问事件
     */
    public void logDatabaseAccess(String bizTag, String operation, long duration, boolean success, String error) {
        MDC.put("event_type", "database_access");
        MDC.put("biz_tag", bizTag);
        MDC.put("operation", operation);
        MDC.put("duration_ms", String.valueOf(duration));
        MDC.put("success", String.valueOf(success));

        if (success) {
            log.debug("Database access completed for bizTag: {}, operation: {}, duration: {}ms",
                    bizTag, operation, duration);
        } else {
            MDC.put("error", error);
            log.error("Database access failed for bizTag: {}, operation: {}, error: {}, duration: {}ms",
                    bizTag, operation, error, duration);

            // 增加错误计数器
            meterRegistry.counter("leaf.database.errors",
                "biz_tag", bizTag,
                "operation", operation).increment();
        }

        MDC.clear();
    }

    /**
     * 记录性能相关事件
     */
    public void logPerformanceEvent(String eventType, String bizTag, Map<String, Object> metrics) {
        MDC.put("event_type", "performance");
        MDC.put("performance_event", eventType);
        MDC.put("biz_tag", bizTag);

        // 添加所有指标到MDC
        metrics.forEach((key, value) -> MDC.put(key, String.valueOf(value)));

        log.info("Performance event: {}, bizTag: {}, metrics: {}", eventType, bizTag, metrics);

        MDC.clear();
    }
}
```

## 3. 告警和通知

### 3.1 告警规则配置

#### Prometheus告警规则

```yaml
# leaf-alerts.yml
groups:
  - name: leaf-id-generator
    rules:
      # ID生成失败率过高
      - alert: LeafIdGenerationFailureHigh
        expr: rate(leaf_error_total[5m]) > 0.01
        for: 2m
        labels:
          severity: critical
          service: leaf-id-generator
        annotations:
          summary: "Leaf ID generation failure rate is high"
          description: "Leaf ID generation failure rate is {{ $value | humanizePercentage }} for the last 5 minutes"

      # ID生成延迟过高
      - alert: LeafIdGenerationLatencyHigh
        expr: histogram_quantile(0.95, rate(leaf_id_generation_seconds_bucket[5m])) > 0.1
        for: 5m
        labels:
          severity: warning
          service: leaf-id-generator
        annotations:
          summary: "Leaf ID generation latency is high"
          description: "95th percentile latency is {{ $value }}s for the last 5 minutes"

      # 数据库连接池耗尽
      - alert: LeafDatabaseConnectionPoolExhausted
        expr: leaf_datasource_active_connections / leaf_datasource_max_connections > 0.9
        for: 1m
        labels:
          severity: critical
          service: leaf-id-generator
        annotations:
          summary: "Database connection pool nearly exhausted"
          description: "Connection pool usage is {{ $value | humanizePercentage }}"

      # 业务长时间未更新
      - alert: LeafBusinessInactive
        expr: leaf_business_last_update_seconds > 3600
        for: 0m
        labels:
          severity: warning
          service: leaf-id-generator
        annotations:
          summary: "Business {{ $labels.biz_tag }} has been inactive"
          description: "Business {{ $labels.biz_tag }} hasn't generated IDs for {{ $value | humanizeDuration }}"

      # QPS异常高
      - alert: LeafIdGenerationQPSHigh
        expr: rate(leaf_id_generated_total[5m]) > 10000
        for: 3m
        labels:
          severity: warning
          service: leaf-id-generator
        annotations:
          summary: "ID generation QPS is unusually high"
          description: "QPS for {{ $labels.biz_tag }} is {{ $value }} requests/second"

      # 步长效率低（频繁访问数据库）
      - alert: LeafStepEfficiencyLow
        expr: rate(leaf_db_access_total[5m]) > 0.1
        for: 5m
        labels:
          severity: warning
          service: leaf-id-generator
        annotations:
          summary: "Database access frequency is high"
          description: "Database is being accessed {{ $value }} times per second, consider increasing step size"
```

### 3.2 通知集成

#### Spring Boot告警服务

```java
@Component
@RequiredArgsConstructor
@Slf4j
public class LeafAlertService {

    private final NotificationService notificationService;
    private final MeterRegistry meterRegistry;
    private final LeafProperties leafProperties;

    // 告警频率限制（避免告警风暴）
    private final Map<String, Long> lastAlertTime = new ConcurrentHashMap<>();
    private static final long ALERT_COOLDOWN_MS = 300000; // 5分钟冷却期

    public enum AlertLevel {
        LOW, MEDIUM, HIGH, CRITICAL
    }

    /**
     * 发送告警
     */
    public void sendAlert(AlertLevel level, String title, String message) {
        String alertKey = title + "_" + level;

        // 检查告警冷却期
        Long lastAlert = lastAlertTime.get(alertKey);
        long now = System.currentTimeMillis();

        if (lastAlert != null && (now - lastAlert) < ALERT_COOLDOWN_MS) {
            log.debug("Alert skipped due to cooldown: {}", title);
            return;
        }

        lastAlertTime.put(alertKey, now);

        try {
            // 构建告警内容
            AlertMessage alertMessage = AlertMessage.builder()
                .level(level)
                .title(title)
                .message(message)
                .service("leaf-id-generator")
                .timestamp(LocalDateTime.now())
                .build();

            // 根据告警级别选择通知方式
            switch (level) {
                case CRITICAL:
                    sendCriticalAlert(alertMessage);
                    break;
                case HIGH:
                    sendHighAlert(alertMessage);
                    break;
                case MEDIUM:
                case LOW:
                    sendNormalAlert(alertMessage);
                    break;
            }

            // 记录告警指标
            meterRegistry.counter("leaf.alerts.sent",
                "level", level.toString().toLowerCase()).increment();

            log.info("Alert sent: level={}, title={}", level, title);

        } catch (Exception e) {
            log.error("Failed to send alert: {}", title, e);
            meterRegistry.counter("leaf.alerts.failed").increment();
        }
    }

    private void sendCriticalAlert(AlertMessage alert) {
        // 关键告警：短信 + 邮件 + 钉钉/微信
        notificationService.sendSms(getOnCallNumbers(), formatSmsMessage(alert));
        notificationService.sendEmail(getAlertEmails(), alert.getTitle(), formatEmailMessage(alert));
        notificationService.sendDingTalk(getDingTalkWebhook(), formatDingTalkMessage(alert));
    }

    private void sendHighAlert(AlertMessage alert) {
        // 高级告警：邮件 + 钉钉/微信
        notificationService.sendEmail(getAlertEmails(), alert.getTitle(), formatEmailMessage(alert));
        notificationService.sendDingTalk(getDingTalkWebhook(), formatDingTalkMessage(alert));
    }

    private void sendNormalAlert(AlertMessage alert) {
        // 普通告警：仅邮件
        notificationService.sendEmail(getAlertEmails(), alert.getTitle(), formatEmailMessage(alert));
    }

    private String formatSmsMessage(AlertMessage alert) {
        return String.format("[%s]%s: %s - %s",
            alert.getLevel(),
            alert.getService(),
            alert.getTitle(),
            alert.getMessage().substring(0, Math.min(alert.getMessage().length(), 100)));
    }

    private String formatEmailMessage(AlertMessage alert) {
        return String.format("""
            <h3>告警详情</h3>
            <p><strong>服务:</strong> %s</p>
            <p><strong>级别:</strong> %s</p>
            <p><strong>标题:</strong> %s</p>
            <p><strong>描述:</strong> %s</p>
            <p><strong>时间:</strong> %s</p>

            <h3>建议操作</h3>
            <ul>
                <li>检查应用日志: /logs/leaf-id-generator.log</li>
                <li>检查数据库连接状态</li>
                <li>查看监控面板: <a href="http://grafana.example.com/d/leaf-dashboard">Leaf监控</a></li>
            </ul>
            """,
            alert.getService(),
            alert.getLevel(),
            alert.getTitle(),
            alert.getMessage(),
            alert.getTimestamp().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
        );
    }

    private String formatDingTalkMessage(AlertMessage alert) {
        return String.format("""
            ## 🚨 Leaf ID生成器告警

            **服务:** %s
            **级别:** %s
            **标题:** %s
            **描述:** %s
            **时间:** %s

            [查看监控面板](http://grafana.example.com/d/leaf-dashboard)
            """,
            alert.getService(),
            alert.getLevel(),
            alert.getTitle(),
            alert.getMessage(),
            alert.getTimestamp().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
        );
    }

    // 配置方法（从配置文件读取）
    private List<String> getOnCallNumbers() {
        return Arrays.asList("13800138000", "13900139000");
    }

    private List<String> getAlertEmails() {
        return Arrays.asList("admin@example.com", "devops@example.com");
    }

    private String getDingTalkWebhook() {
        return "https://oapi.dingtalk.com/robot/send?access_token=xxx";
    }

    @Data
    @Builder
    public static class AlertMessage {
        private AlertLevel level;
        private String title;
        private String message;
        private String service;
        private LocalDateTime timestamp;
    }
}
```

### 3.3 自动恢复机制

```java
@Component
@RequiredArgsConstructor
@Slf4j
public class LeafAutoRecoveryService {

    private final LeafIdGeneratorService leafIdGenerator;
    private final LeafAlertService alertService;
    private final JdbcTemplate jdbcTemplate;

    /**
     * 健康检查和自动恢复
     */
    @Scheduled(fixedDelay = 30000) // 每30秒检查一次
    public void healthCheckAndRecover() {
        String[] criticalBizTags = {"order_id", "user_id", "tracking_number"};

        for (String bizTag : criticalBizTags) {
            try {
                performHealthCheck(bizTag);
            } catch (Exception e) {
                log.error("Health check failed for bizTag: {}", bizTag, e);
                attemptRecovery(bizTag, e);
            }
        }
    }

    private void performHealthCheck(String bizTag) {
        // 1. 测试ID生成
        long startTime = System.currentTimeMillis();
        long testId = leafIdGenerator.nextId("health_check_" + bizTag);
        long duration = System.currentTimeMillis() - startTime;

        // 2. 检查响应时间
        if (duration > 1000) { // 超过1秒
            alertService.sendAlert(LeafAlertService.AlertLevel.HIGH,
                "Leaf Health Check Slow",
                String.format("Health check for %s took %dms", bizTag, duration));
        }

        // 3. 检查数据库连接
        checkDatabaseHealth();

        log.debug("Health check passed for bizTag: {}, testId: {}, duration: {}ms",
                 bizTag, testId, duration);
    }

    private void checkDatabaseHealth() {
        try {
            Integer result = jdbcTemplate.queryForObject("SELECT 1", Integer.class);
            if (result == null || result != 1) {
                throw new RuntimeException("Database health check failed");
            }
        } catch (Exception e) {
            alertService.sendAlert(LeafAlertService.AlertLevel.CRITICAL,
                "Database Health Check Failed",
                "Database connection test failed: " + e.getMessage());
            throw e;
        }
    }

    private void attemptRecovery(String bizTag, Exception error) {
        log.info("Attempting recovery for bizTag: {}", bizTag);

        try {
            // 1. 检查并创建缺失的业务配置
            if (!bizTagExists(bizTag)) {
                createDefaultBizTag(bizTag);
                log.info("Created missing biz tag: {}", bizTag);
            }

            // 2. 重置连接池（如果是连接问题）
            if (isConnectionError(error)) {
                resetConnectionPool();
                log.info("Reset database connection pool");
            }

            // 3. 清理可能的死锁
            if (isLockError(error)) {
                clearPotentialLocks(bizTag);
                log.info("Cleared potential locks for bizTag: {}", bizTag);
            }

            // 4. 验证恢复结果
            long testId = leafIdGenerator.nextId("recovery_test_" + bizTag);
            log.info("Recovery successful for bizTag: {}, testId: {}", bizTag, testId);

            alertService.sendAlert(LeafAlertService.AlertLevel.MEDIUM,
                "Leaf Auto Recovery Success",
                String.format("Successfully recovered bizTag: %s", bizTag));

        } catch (Exception e) {
            log.error("Auto recovery failed for bizTag: {}", bizTag, e);
            alertService.sendAlert(LeafAlertService.AlertLevel.CRITICAL,
                "Leaf Auto Recovery Failed",
                String.format("Failed to recover bizTag: %s, error: %s", bizTag, e.getMessage()));
        }
    }

    private boolean bizTagExists(String bizTag) {
        try {
            Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM leaf_alloc WHERE biz_tag = ?",
                Integer.class, bizTag);
            return count != null && count > 0;
        } catch (Exception e) {
            return false;
        }
    }

    private void createDefaultBizTag(String bizTag) {
        jdbcTemplate.update(
            "INSERT INTO leaf_alloc (biz_tag, max_id, step, description) VALUES (?, 1, 1000, 'Auto-created by recovery')",
            bizTag);
    }

    private boolean isConnectionError(Exception error) {
        String message = error.getMessage().toLowerCase();
        return message.contains("connection") ||
               message.contains("timeout") ||
               message.contains("socket");
    }

    private boolean isLockError(Exception error) {
        String message = error.getMessage().toLowerCase();
        return message.contains("lock") ||
               message.contains("deadlock") ||
               message.contains("timeout");
    }

    private void resetConnectionPool() {
        // 这里可以实现连接池重置逻辑
        // 具体实现取决于使用的连接池类型
        log.info("Connection pool reset triggered");
    }

    private void clearPotentialLocks(String bizTag) {
        try {
            // 查询可能的锁等待
            jdbcTemplate.queryForList(
                "SELECT pg_cancel_backend(pid) FROM pg_stat_activity " +
                "WHERE state = 'active' AND query LIKE '%leaf_alloc%' AND query_start < NOW() - INTERVAL '30 seconds'");
        } catch (Exception e) {
            log.warn("Failed to clear potential locks", e);
        }
    }
}
```

## 4. 性能基准测试

### 4.1 基准测试套件

```java
@Component
@RequiredArgsConstructor
@Slf4j
public class LeafBenchmarkSuite {

    private final LeafIdGeneratorService leafIdGenerator;

    /**
     * 执行完整的性能基准测试
     */
    public BenchmarkReport runFullBenchmark() {
        log.info("Starting Leaf ID Generator benchmark suite");

        BenchmarkReport report = new BenchmarkReport();
        report.setStartTime(LocalDateTime.now());

        try {
            // 1. 预热
            warmup();

            // 2. 单线程性能测试
            report.setSingleThreadResults(runSingleThreadBenchmark());

            // 3. 多线程性能测试
            report.setConcurrentResults(runConcurrentBenchmark());

            // 4. 压力测试
            report.setStressResults(runStressTest());

            // 5. 延迟测试
            report.setLatencyResults(runLatencyBenchmark());

            report.setSuccess(true);

        } catch (Exception e) {
            log.error("Benchmark failed", e);
            report.setSuccess(false);
            report.setError(e.getMessage());
        }

        report.setEndTime(LocalDateTime.now());
        report.setTotalDuration(Duration.between(report.getStartTime(), report.getEndTime()));

        log.info("Benchmark completed: {}", report.getSummary());
        return report;
    }

    private void warmup() {
        log.info("Starting warmup...");
        String bizTag = "benchmark_warmup";

        for (int i = 0; i < 10000; i++) {
            leafIdGenerator.nextId(bizTag);
        }

        log.info("Warmup completed");
    }

    private SingleThreadBenchmarkResult runSingleThreadBenchmark() {
        log.info("Running single thread benchmark");

        String bizTag = "benchmark_single";
        int iterations = 100000;

        long startTime = System.nanoTime();
        Set<Long> uniqueIds = new HashSet<>();

        for (int i = 0; i < iterations; i++) {
            long id = leafIdGenerator.nextId(bizTag);
            uniqueIds.add(id);
        }

        long endTime = System.nanoTime();
        long durationMs = (endTime - startTime) / 1_000_000;

        return SingleThreadBenchmarkResult.builder()
            .iterations(iterations)
            .uniqueIds(uniqueIds.size())
            .durationMs(durationMs)
            .qps(iterations * 1000.0 / durationMs)
            .avgLatencyNs((endTime - startTime) / iterations)
            .success(uniqueIds.size() == iterations)
            .build();
    }

    private ConcurrentBenchmarkResult runConcurrentBenchmark() {
        log.info("Running concurrent benchmark");

        String bizTag = "benchmark_concurrent";
        int threadCount = 50;
        int iterationsPerThread = 2000;

        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);
        ConcurrentHashMap<Long, Boolean> allIds = new ConcurrentHashMap<>();
        AtomicLong errorCount = new AtomicLong(0);

        long startTime = System.nanoTime();

        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    for (int j = 0; j < iterationsPerThread; j++) {
                        try {
                            long id = leafIdGenerator.nextId(bizTag);
                            allIds.put(id, true);
                        } catch (Exception e) {
                            errorCount.incrementAndGet();
                        }
                    }
                } finally {
                    latch.countDown();
                }
            });
        }

        try {
            latch.await(60, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        long endTime = System.nanoTime();
        long durationMs = (endTime - startTime) / 1_000_000;
        int expectedTotal = threadCount * iterationsPerThread;

        executor.shutdown();

        return ConcurrentBenchmarkResult.builder()
            .threadCount(threadCount)
            .iterationsPerThread(iterationsPerThread)
            .expectedTotal(expectedTotal)
            .actualGenerated(allIds.size())
            .errorCount(errorCount.get())
            .durationMs(durationMs)
            .qps(expectedTotal * 1000.0 / durationMs)
            .success(allIds.size() == expectedTotal && errorCount.get() == 0)
            .build();
    }

    private StressBenchmarkResult runStressTest() {
        log.info("Running stress test");

        List<StressTestResult> results = new ArrayList<>();
        int[] threadCounts = {1, 10, 25, 50, 100, 200};
        int iterationsPerThread = 1000;

        for (int threadCount : threadCounts) {
            try {
                String bizTag = "benchmark_stress_" + threadCount;

                StressTestResult result = runStressTestWithThreads(bizTag, threadCount, iterationsPerThread);
                results.add(result);

                log.info("Stress test completed: threads={}, qps={:.2f}", threadCount, result.getQps());

                // 线程间短暂休息
                Thread.sleep(2000);

            } catch (Exception e) {
                log.error("Stress test failed for thread count: " + threadCount, e);
            }
        }

        return StressBenchmarkResult.builder()
            .results(results)
            .maxQps(results.stream().mapToDouble(StressTestResult::getQps).max().orElse(0))
            .optimalThreadCount(results.stream()
                .max(Comparator.comparingDouble(StressTestResult::getQps))
                .map(StressTestResult::getThreadCount)
                .orElse(0))
            .build();
    }

    private StressTestResult runStressTestWithThreads(String bizTag, int threadCount, int iterationsPerThread) {
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);
        AtomicLong successCount = new AtomicLong(0);
        AtomicLong errorCount = new AtomicLong(0);

        long startTime = System.nanoTime();

        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    for (int j = 0; j < iterationsPerThread; j++) {
                        try {
                            leafIdGenerator.nextId(bizTag);
                            successCount.incrementAndGet();
                        } catch (Exception e) {
                            errorCount.incrementAndGet();
                        }
                    }
                } finally {
                    latch.countDown();
                }
            });
        }

        try {
            latch.await(120, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        long endTime = System.nanoTime();
        long durationMs = (endTime - startTime) / 1_000_000;

        executor.shutdown();

        return StressTestResult.builder()
            .threadCount(threadCount)
            .iterationsPerThread(iterationsPerThread)
            .successCount(successCount.get())
            .errorCount(errorCount.get())
            .durationMs(durationMs)
            .qps(successCount.get() * 1000.0 / durationMs)
            .errorRate(errorCount.get() * 1.0 / (successCount.get() + errorCount.get()))
            .build();
    }

    private LatencyBenchmarkResult runLatencyBenchmark() {
        log.info("Running latency benchmark");

        String bizTag = "benchmark_latency";
        int iterations = 10000;
        List<Long> latencies = new ArrayList<>(iterations);

        for (int i = 0; i < iterations; i++) {
            long startTime = System.nanoTime();
            leafIdGenerator.nextId(bizTag);
            long endTime = System.nanoTime();

            latencies.add(endTime - startTime);
        }

        Collections.sort(latencies);

        return LatencyBenchmarkResult.builder()
            .iterations(iterations)
            .minLatencyNs(latencies.get(0))
            .maxLatencyNs(latencies.get(iterations - 1))
            .p50LatencyNs(latencies.get(iterations / 2))
            .p95LatencyNs(latencies.get((int) (iterations * 0.95)))
            .p99LatencyNs(latencies.get((int) (iterations * 0.99)))
            .avgLatencyNs(latencies.stream().mapToLong(Long::longValue).sum() / iterations)
            .build();
    }

    // 数据类定义...
    @Data
    @Builder
    public static class BenchmarkReport {
        private LocalDateTime startTime;
        private LocalDateTime endTime;
        private Duration totalDuration;
        private boolean success;
        private String error;

        private SingleThreadBenchmarkResult singleThreadResults;
        private ConcurrentBenchmarkResult concurrentResults;
        private StressBenchmarkResult stressResults;
        private LatencyBenchmarkResult latencyResults;

        public String getSummary() {
            if (!success) {
                return "Benchmark failed: " + error;
            }

            return String.format(
                "Benchmark completed in %s. Single-thread QPS: %.2f, Max concurrent QPS: %.2f, P95 latency: %.2fms",
                totalDuration,
                singleThreadResults.getQps(),
                stressResults.getMaxQps(),
                latencyResults.getP95LatencyNs() / 1_000_000.0
            );
        }
    }

    // 其他结果类...
    @Data
    @Builder
    public static class SingleThreadBenchmarkResult {
        private int iterations;
        private int uniqueIds;
        private long durationMs;
        private double qps;
        private long avgLatencyNs;
        private boolean success;
    }

    @Data
    @Builder
    public static class ConcurrentBenchmarkResult {
        private int threadCount;
        private int iterationsPerThread;
        private int expectedTotal;
        private int actualGenerated;
        private long errorCount;
        private long durationMs;
        private double qps;
        private boolean success;
    }

    @Data
    @Builder
    public static class StressBenchmarkResult {
        private List<StressTestResult> results;
        private double maxQps;
        private int optimalThreadCount;
    }

    @Data
    @Builder
    public static class StressTestResult {
        private int threadCount;
        private int iterationsPerThread;
        private long successCount;
        private long errorCount;
        private long durationMs;
        private double qps;
        private double errorRate;
    }

    @Data
    @Builder
    public static class LatencyBenchmarkResult {
        private int iterations;
        private long minLatencyNs;
        private long maxLatencyNs;
        private long avgLatencyNs;
        private long p50LatencyNs;
        private long p95LatencyNs;
        private long p99LatencyNs;
    }
}
```

通过以上完整的性能调优和监控体系，你可以确保Leaf分布式ID系统在生产环境中稳定、高效地运行。记住定期执行基准测试，根据实际业务负载调整配置参数，并建立完善的告警和自动恢复机制。