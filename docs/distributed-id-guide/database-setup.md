# 数据库配置和初始化指南

## 目录

1. [数据库表设计](#1-数据库表设计)
2. [初始化脚本](#2-初始化脚本)
3. [业务数据配置](#3-业务数据配置)
4. [索引优化](#4-索引优化)
5. [数据库连接配置](#5-数据库连接配置)
6. [监控和维护](#6-监控和维护)

---

## 1. 数据库表设计

### 1.1 创建Leaf分配表

```sql
-- 删除已存在的表（如果需要重建）
DROP TABLE IF EXISTS leaf_alloc;

-- 创建leaf分配表
CREATE TABLE leaf_alloc (
    biz_tag VARCHAR(128) NOT NULL DEFAULT '' COMMENT '业务标识',
    max_id BIGINT NOT NULL DEFAULT 1 COMMENT '当前已分配的最大ID',
    step INT NOT NULL DEFAULT 1000 COMMENT '每次分配的步长',
    description VARCHAR(256) DEFAULT '' COMMENT '业务描述',
    update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '数据更新时间',
    PRIMARY KEY (biz_tag)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Leaf分布式ID分配表';
```

### 1.2 表结构详解

| 字段 | 类型 | 说明 | 示例 |
|------|------|------|------|
| `biz_tag` | VARCHAR(128) | 业务标识，主键，不同业务使用不同tag | 'order_id', 'user_id' |
| `max_id` | BIGINT | 当前已分配的最大ID值 | 10000 |
| `step` | INT | 每次分配的号段大小 | 1000 |
| `description` | VARCHAR(256) | 业务描述信息 | '订单ID生成器' |
| `update_time` | TIMESTAMP | 记录最后更新时间 | '2024-01-15 10:30:00' |

### 1.3 设计考虑

#### 为什么使用BIGINT类型？
```sql
-- BIGINT范围：-9,223,372,036,854,775,808 到 9,223,372,036,854,775,807
-- 约920万亿，足够长期使用
-- 如果每秒生成10万个ID，可以使用约30万年
```

#### 为什么需要step字段？
```sql
-- step控制每次从数据库获取的号段大小
-- 小step: 减少浪费，但增加数据库访问频率
-- 大step: 减少数据库压力，但可能浪费ID
-- 建议根据业务QPS动态调整
```

## 2. 初始化脚本

### 2.1 完整初始化SQL

```sql
-- ===========================================
-- Leaf分布式ID系统初始化脚本
-- 适用于：Mini-UPS项目
-- 数据库：PostgreSQL 15+
-- ===========================================

-- 1. 创建数据库（如果不存在）
-- CREATE DATABASE ups_db;
-- \c ups_db;

-- 2. 创建leaf_alloc表
DROP TABLE IF EXISTS leaf_alloc CASCADE;

CREATE TABLE leaf_alloc (
    biz_tag VARCHAR(128) NOT NULL DEFAULT '',
    max_id BIGINT NOT NULL DEFAULT 1,
    step INT NOT NULL DEFAULT 1000,
    description VARCHAR(256) DEFAULT '',
    update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (biz_tag)
);

-- 3. 创建更新触发器（PostgreSQL版本）
CREATE OR REPLACE FUNCTION update_leaf_alloc_update_time()
RETURNS TRIGGER AS $$
BEGIN
    NEW.update_time = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trigger_leaf_alloc_update_time ON leaf_alloc;
CREATE TRIGGER trigger_leaf_alloc_update_time
    BEFORE UPDATE ON leaf_alloc
    FOR EACH ROW
    EXECUTE FUNCTION update_leaf_alloc_update_time();

-- 4. 添加表注释
COMMENT ON TABLE leaf_alloc IS 'Leaf分布式ID分配表';
COMMENT ON COLUMN leaf_alloc.biz_tag IS '业务标识，不同业务使用不同的tag';
COMMENT ON COLUMN leaf_alloc.max_id IS '当前已分配的最大ID';
COMMENT ON COLUMN leaf_alloc.step IS '每次分配的号段大小';
COMMENT ON COLUMN leaf_alloc.description IS '业务描述';
COMMENT ON COLUMN leaf_alloc.update_time IS '最后更新时间';

-- 5. 创建索引
CREATE INDEX idx_leaf_alloc_update_time ON leaf_alloc(update_time);

-- 6. 插入初始业务数据
INSERT INTO leaf_alloc (biz_tag, max_id, step, description) VALUES
-- 核心业务ID
('order_id', 100000, 2000, 'Order ID - 订单唯一标识符'),
('user_id', 10000, 1000, 'User ID - 用户唯一标识符'),
('shipment_id', 50000, 1500, 'Shipment ID - 运单唯一标识符'),
('tracking_number', 1000000, 5000, 'Tracking Number - 包裹追踪号'),

-- 扩展业务ID
('truck_id', 1000, 100, 'Truck ID - 车辆唯一标识符'),
('warehouse_id', 100, 50, 'Warehouse ID - 仓库唯一标识符'),
('notification_id', 10000, 1000, 'Notification ID - 通知消息ID'),
('audit_log_id', 50000, 2000, 'Audit Log ID - 审计日志ID'),

-- 业务流程ID
('invoice_id', 10000, 1000, 'Invoice ID - 发票号码'),
('payment_id', 5000, 500, 'Payment ID - 支付流水号'),
('refund_id', 1000, 100, 'Refund ID - 退款单号'),

-- 技术相关ID
('session_id', 100000, 3000, 'Session ID - 会话标识'),
('request_id', 500000, 10000, 'Request ID - 请求追踪号'),
('batch_job_id', 1000, 100, 'Batch Job ID - 批处理作业ID')

ON CONFLICT (biz_tag) DO UPDATE SET
    description = EXCLUDED.description,
    update_time = CURRENT_TIMESTAMP;

-- 7. 验证数据插入
SELECT
    biz_tag,
    max_id,
    step,
    description,
    update_time
FROM leaf_alloc
ORDER BY biz_tag;

-- 8. 创建管理视图
CREATE OR REPLACE VIEW v_leaf_alloc_status AS
SELECT
    biz_tag,
    max_id,
    step,
    CONCAT(max_id - step + 1, ' - ', max_id) AS current_range,
    CONCAT(max_id + 1, ' - ', max_id + step) AS next_range,
    description,
    update_time,
    EXTRACT(EPOCH FROM (CURRENT_TIMESTAMP - update_time)) AS seconds_since_update
FROM leaf_alloc
ORDER BY update_time DESC;

-- 查看状态视图
SELECT * FROM v_leaf_alloc_status;
```

### 2.2 MySQL版本（如果使用MySQL）

```sql
-- MySQL版本的初始化脚本
CREATE TABLE leaf_alloc (
    biz_tag VARCHAR(128) NOT NULL DEFAULT '' COMMENT '业务标识',
    max_id BIGINT NOT NULL DEFAULT 1 COMMENT '当前已分配的最大ID',
    step INT NOT NULL DEFAULT 1000 COMMENT '每次分配的步长',
    description VARCHAR(256) DEFAULT '' COMMENT '业务描述',
    update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '数据更新时间',
    PRIMARY KEY (biz_tag)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Leaf分布式ID分配表';

-- MySQL索引
CREATE INDEX idx_leaf_alloc_update_time ON leaf_alloc(update_time);
```

## 3. 业务数据配置

### 3.1 根据业务场景配置步长

```sql
-- 高频业务（每秒1000+请求）- 大步长
UPDATE leaf_alloc SET step = 10000 WHERE biz_tag = 'tracking_number';
UPDATE leaf_alloc SET step = 5000 WHERE biz_tag = 'order_id';

-- 中频业务（每秒100-1000请求）- 中等步长
UPDATE leaf_alloc SET step = 2000 WHERE biz_tag = 'shipment_id';
UPDATE leaf_alloc SET step = 1000 WHERE biz_tag = 'user_id';

-- 低频业务（每秒<100请求）- 小步长
UPDATE leaf_alloc SET step = 500 WHERE biz_tag = 'truck_id';
UPDATE leaf_alloc SET step = 100 WHERE biz_tag = 'warehouse_id';
```

### 3.2 业务标识命名规范

```sql
-- 建议的命名规范：
-- 1. 使用下划线分隔
-- 2. 以实体名_id结尾
-- 3. 保持简洁明了

-- 好的例子：
'user_id'           -- 用户ID
'order_id'          -- 订单ID
'tracking_number'   -- 追踪号
'audit_log_id'      -- 审计日志ID

-- 避免的例子：
'UserIdentifier'    -- 驼峰命名
'id_for_user'       -- 冗余词汇
'usr_id'           -- 过度缩写
'business_user_account_id' -- 过长
```

### 3.3 添加新业务配置

```sql
-- 添加新业务的标准流程
INSERT INTO leaf_alloc (biz_tag, max_id, step, description)
VALUES ('new_business_id', 1, 1000, 'New Business Description')
ON CONFLICT (biz_tag) DO UPDATE SET
    description = EXCLUDED.description;

-- 或者使用存储过程
CREATE OR REPLACE FUNCTION add_leaf_biz(
    p_biz_tag VARCHAR(128),
    p_step INT DEFAULT 1000,
    p_description VARCHAR(256) DEFAULT ''
)
RETURNS VOID AS $$
BEGIN
    INSERT INTO leaf_alloc (biz_tag, max_id, step, description)
    VALUES (p_biz_tag, 1, p_step, p_description)
    ON CONFLICT (biz_tag) DO UPDATE SET
        step = EXCLUDED.step,
        description = EXCLUDED.description;
END;
$$ LANGUAGE plpgsql;

-- 使用存储过程添加
SELECT add_leaf_biz('custom_id', 2000, 'Custom Business ID');
```

## 4. 索引优化

### 4.1 创建必要索引

```sql
-- 1. 主键索引（自动创建）
-- PRIMARY KEY (biz_tag) - 已存在

-- 2. 更新时间索引（用于监控）
CREATE INDEX IF NOT EXISTS idx_leaf_alloc_update_time
ON leaf_alloc(update_time);

-- 3. 复合索引（如果需要按step查询）
CREATE INDEX IF NOT EXISTS idx_leaf_alloc_step_update_time
ON leaf_alloc(step, update_time);

-- 4. 分析索引使用情况
EXPLAIN (ANALYZE, BUFFERS)
SELECT * FROM leaf_alloc WHERE biz_tag = 'order_id';
```

### 4.2 性能优化配置

```sql
-- PostgreSQL性能参数调优
-- 在postgresql.conf中设置：

-- shared_buffers = 256MB          # 共享缓冲区
-- effective_cache_size = 1GB      # 有效缓存大小
-- work_mem = 4MB                  # 工作内存
-- maintenance_work_mem = 64MB     # 维护工作内存
-- checkpoint_completion_target = 0.9  # 检查点完成目标

-- 连接池配置（在应用层）
-- maximum-pool-size: 20
-- minimum-idle: 5
-- connection-timeout: 30000
-- idle-timeout: 600000
```

## 5. 数据库连接配置

### 5.1 Spring Boot配置

```yaml
# application.yml
spring:
  datasource:
    driver-class-name: org.postgresql.Driver
    url: jdbc:postgresql://localhost:5432/ups_db
    username: postgres
    password: abc123

    # HikariCP连接池配置
    hikari:
      maximum-pool-size: 20        # 最大连接数
      minimum-idle: 5              # 最小空闲连接数
      connection-timeout: 30000    # 连接超时时间(ms)
      idle-timeout: 600000         # 空闲超时时间(ms)
      max-lifetime: 1800000        # 连接最大生命周期(ms)
      leak-detection-threshold: 60000  # 连接泄漏检测阈值
      pool-name: LeafCP            # 连接池名称

  jpa:
    database-platform: org.hibernate.dialect.PostgreSQLDialect
    show-sql: false
    hibernate:
      ddl-auto: none  # 不自动创建表，使用手动SQL
    properties:
      hibernate:
        format_sql: true
        jdbc:
          batch_size: 20
        order_inserts: true
        order_updates: true
        jdbc:
          batch_versioned_data: true
```

### 5.2 数据源健康检查

```java
@Component
public class LeafDatabaseHealthIndicator implements HealthIndicator {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Override
    public Health health() {
        try {
            // 检查数据库连接
            jdbcTemplate.queryForObject("SELECT 1", Integer.class);

            // 检查leaf_alloc表
            Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM leaf_alloc", Integer.class);

            return Health.up()
                .withDetail("database", "PostgreSQL")
                .withDetail("leaf_alloc_records", count)
                .withDetail("status", "healthy")
                .build();

        } catch (Exception e) {
            return Health.down()
                .withDetail("database", "PostgreSQL")
                .withDetail("error", e.getMessage())
                .build();
        }
    }
}
```

## 6. 监控和维护

### 6.1 监控查询语句

```sql
-- 1. 查看各业务的当前状态
SELECT
    biz_tag,
    max_id,
    step,
    CONCAT((max_id - step + 1), ' - ', max_id) AS current_range,
    description,
    update_time,
    EXTRACT(EPOCH FROM (CURRENT_TIMESTAMP - update_time)) AS seconds_since_update
FROM leaf_alloc
ORDER BY update_time DESC;

-- 2. 查看最活跃的业务（最近更新的）
SELECT
    biz_tag,
    max_id,
    step,
    update_time,
    CASE
        WHEN update_time > CURRENT_TIMESTAMP - INTERVAL '1 minute' THEN 'Very Active'
        WHEN update_time > CURRENT_TIMESTAMP - INTERVAL '5 minutes' THEN 'Active'
        WHEN update_time > CURRENT_TIMESTAMP - INTERVAL '1 hour' THEN 'Moderate'
        ELSE 'Inactive'
    END AS activity_level
FROM leaf_alloc
ORDER BY update_time DESC;

-- 3. 统计ID消耗速度
SELECT
    biz_tag,
    max_id,
    step,
    ROUND(step / EXTRACT(EPOCH FROM (CURRENT_TIMESTAMP - update_time))) AS ids_per_second
FROM leaf_alloc
WHERE update_time > CURRENT_TIMESTAMP - INTERVAL '1 hour'
ORDER BY ids_per_second DESC;

-- 4. 检查潜在的ID耗尽风险
SELECT
    biz_tag,
    max_id,
    CASE
        WHEN max_id > 9000000000000000000::BIGINT THEN 'CRITICAL - Near BIGINT limit'
        WHEN max_id > 1000000000000000000::BIGINT THEN 'WARNING - High ID usage'
        ELSE 'OK'
    END AS risk_level,
    ROUND(max_id::NUMERIC / 9223372036854775807::NUMERIC * 100, 6) AS usage_percentage
FROM leaf_alloc
ORDER BY max_id DESC;
```

### 6.2 维护脚本

```sql
-- 定期维护脚本（建议每月执行）

-- 1. 分析表统计信息
ANALYZE leaf_alloc;

-- 2. 重建索引（如果需要）
REINDEX INDEX idx_leaf_alloc_update_time;

-- 3. 检查表大小
SELECT
    schemaname,
    tablename,
    pg_size_pretty(pg_total_relation_size(schemaname||'.'||tablename)) as size,
    pg_size_pretty(pg_relation_size(schemaname||'.'||tablename)) as table_size,
    pg_size_pretty(pg_total_relation_size(schemaname||'.'||tablename) -
                   pg_relation_size(schemaname||'.'||tablename)) as index_size
FROM pg_tables
WHERE tablename = 'leaf_alloc';

-- 4. 备份关键数据
CREATE TABLE leaf_alloc_backup AS
SELECT * FROM leaf_alloc;

-- 5. 清理过期的备份表（保留最近3个）
-- DROP TABLE IF EXISTS leaf_alloc_backup_old;
```

### 6.3 性能监控指标

```java
@Component
public class LeafMetricsCollector {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private MeterRegistry meterRegistry;

    @EventListener
    @Async
    public void collectMetrics() {
        try {
            // 收集各业务的活跃度
            List<Map<String, Object>> results = jdbcTemplate.queryForList(
                "SELECT biz_tag, EXTRACT(EPOCH FROM (CURRENT_TIMESTAMP - update_time)) as seconds_since_update " +
                "FROM leaf_alloc");

            results.forEach(row -> {
                String bizTag = (String) row.get("biz_tag");
                Number secondsSinceUpdate = (Number) row.get("seconds_since_update");

                Gauge.builder("leaf.business.last_update_seconds")
                    .description("Seconds since last update for business")
                    .tag("biz_tag", bizTag)
                    .register(meterRegistry, () -> secondsSinceUpdate.doubleValue());
            });

        } catch (Exception e) {
            log.error("Failed to collect Leaf metrics", e);
        }
    }
}
```

## 故障恢复

### 6.1 数据丢失恢复

```sql
-- 如果意外删除了数据，可以从备份恢复
-- 1. 从备份表恢复
INSERT INTO leaf_alloc
SELECT * FROM leaf_alloc_backup
ON CONFLICT (biz_tag) DO UPDATE SET
    max_id = GREATEST(leaf_alloc.max_id, EXCLUDED.max_id),
    step = EXCLUDED.step,
    description = EXCLUDED.description;

-- 2. 手动重置某个业务（注意：可能导致ID重复）
UPDATE leaf_alloc
SET max_id = 1000000
WHERE biz_tag = 'order_id';
```

### 6.2 并发问题排查

```sql
-- 检查是否有长时间运行的事务
SELECT
    pid,
    now() - pg_stat_activity.query_start AS duration,
    query,
    state
FROM pg_stat_activity
WHERE query LIKE '%leaf_alloc%'
AND state = 'active'
ORDER BY duration DESC;

-- 检查锁等待情况
SELECT
    blocked_locks.pid AS blocked_pid,
    blocked_activity.usename AS blocked_user,
    blocking_locks.pid AS blocking_pid,
    blocking_activity.usename AS blocking_user,
    blocked_activity.query AS blocked_statement,
    blocking_activity.query AS blocking_statement
FROM pg_catalog.pg_locks blocked_locks
JOIN pg_catalog.pg_stat_activity blocked_activity ON blocked_activity.pid = blocked_locks.pid
JOIN pg_catalog.pg_locks blocking_locks ON blocking_locks.locktype = blocked_locks.locktype
    AND blocking_locks.DATABASE IS NOT DISTINCT FROM blocked_locks.DATABASE
    AND blocking_locks.relation IS NOT DISTINCT FROM blocked_locks.relation
    AND blocking_locks.page IS NOT DISTINCT FROM blocked_locks.page
    AND blocking_locks.tuple IS NOT DISTINCT FROM blocked_locks.tuple
    AND blocking_locks.virtualxid IS NOT DISTINCT FROM blocked_locks.virtualxid
    AND blocking_locks.transactionid IS NOT DISTINCT FROM blocked_locks.transactionid
    AND blocking_locks.classid IS NOT DISTINCT FROM blocked_locks.classid
    AND blocking_locks.objid IS NOT DISTINCT FROM blocked_locks.objid
    AND blocking_locks.objsubid IS NOT DISTINCT FROM blocked_locks.objsubid
    AND blocking_locks.pid != blocked_locks.pid
JOIN pg_catalog.pg_stat_activity blocking_activity ON blocking_activity.pid = blocking_locks.pid
WHERE NOT blocked_locks.GRANTED;
```

通过以上配置，你可以建立一个稳定、高性能的分布式ID数据库基础设施。记住定期监控和维护，确保系统长期稳定运行。