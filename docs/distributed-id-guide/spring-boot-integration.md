# Spring Boot集成配置指南

## 目录

1. [项目依赖配置](#1-项目依赖配置)
2. [配置文件设置](#2-配置文件设置)
3. [核心服务实现](#3-核心服务实现)
4. [配置类和Bean注册](#4-配置类和bean注册)
5. [控制器集成](#5-控制器集成)
6. [异常处理](#6-异常处理)
7. [测试配置](#7-测试配置)

---

## 1. 项目依赖配置

### 1.1 Maven依赖（pom.xml）

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0">
    <!-- 项目基本信息 -->
    <groupId>com.miniups</groupId>
    <artifactId>mini-ups-backend</artifactId>
    <version>1.0.0</version>

    <!-- Spring Boot父项目 -->
    <parent>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>3.2.0</version>
        <relativePath/>
    </parent>

    <properties>
        <java.version>17</java.version>
    </properties>

    <dependencies>
        <!-- Spring Boot基础依赖 -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>

        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-data-jpa</artifactId>
        </dependency>

        <!-- 数据库相关 -->
        <dependency>
            <groupId>org.postgresql</groupId>
            <artifactId>postgresql</artifactId>
            <scope>runtime</scope>
        </dependency>

        <!-- JDBC模板（Leaf需要） -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-jdbc</artifactId>
        </dependency>

        <!-- Redis支持 -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-data-redis</artifactId>
        </dependency>

        <!-- 监控和指标 -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-actuator</artifactId>
        </dependency>

        <dependency>
            <groupId>io.micrometer</groupId>
            <artifactId>micrometer-registry-prometheus</artifactId>
        </dependency>

        <!-- 配置处理 -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-configuration-processor</artifactId>
            <optional>true</optional>
        </dependency>

        <!-- 工具库 -->
        <dependency>
            <groupId>org.apache.commons</groupId>
            <artifactId>commons-lang3</artifactId>
        </dependency>

        <!-- 日志 -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-logging</artifactId>
        </dependency>

        <!-- 测试依赖 -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
        </dependency>

        <dependency>
            <groupId>com.h2database</groupId>
            <artifactId>h2</artifactId>
            <scope>test</scope>
        </dependency>
    </dependencies>
</project>
```

### 1.2 Gradle依赖（build.gradle）

```gradle
plugins {
    id 'java'
    id 'org.springframework.boot' version '3.2.0'
    id 'io.spring.dependency-management' version '1.1.4'
}

group = 'com.miniups'
version = '1.0.0'
java.sourceCompatibility = JavaVersion.VERSION_17

repositories {
    mavenCentral()
}

dependencies {
    // Spring Boot核心
    implementation 'org.springframework.boot:spring-boot-starter-web'
    implementation 'org.springframework.boot:spring-boot-starter-data-jpa'
    implementation 'org.springframework.boot:spring-boot-starter-jdbc'

    // 数据库
    runtimeOnly 'org.postgresql:postgresql'

    // Redis
    implementation 'org.springframework.boot:spring-boot-starter-data-redis'

    // 监控
    implementation 'org.springframework.boot:spring-boot-starter-actuator'
    implementation 'io.micrometer:micrometer-registry-prometheus'

    // 配置
    annotationProcessor 'org.springframework.boot:spring-boot-configuration-processor'

    // 工具
    implementation 'org.apache.commons:commons-lang3'

    // 测试
    testImplementation 'org.springframework.boot:spring-boot-starter-test'
    testImplementation 'com.h2database:h2'
}
```

## 2. 配置文件设置

### 2.1 主配置文件（application.yml）

```yaml
# ===========================================
# Mini-UPS Spring Boot配置文件
# Leaf分布式ID系统配置
# ===========================================

server:
  port: 8081
  servlet:
    context-path: /api

spring:
  application:
    name: mini-ups-backend

  # 数据源配置
  datasource:
    driver-class-name: org.postgresql.Driver
    url: jdbc:postgresql://localhost:5432/ups_db
    username: postgres
    password: abc123

    # HikariCP连接池配置
    hikari:
      pool-name: UpsHikariCP
      maximum-pool-size: 20
      minimum-idle: 5
      connection-timeout: 30000
      idle-timeout: 600000
      max-lifetime: 1800000
      leak-detection-threshold: 60000

  # JPA配置
  jpa:
    database-platform: org.hibernate.dialect.PostgreSQLDialect
    show-sql: false
    hibernate:
      ddl-auto: none
      naming:
        physical-strategy: org.hibernate.boot.model.naming.SnakeCasePhysicalNamingStrategy
    properties:
      hibernate:
        format_sql: true
        jdbc:
          batch_size: 20
        order_inserts: true
        order_updates: true

  # Redis配置
  redis:
    host: localhost
    port: 6380
    password: # 如果有密码
    database: 0
    lettuce:
      pool:
        max-active: 8
        max-wait: -1ms
        max-idle: 8
        min-idle: 0

# Leaf分布式ID配置
leaf:
  segment:
    enabled: true
    # 默认步长
    default-step: 1000
    # 异步更新阈值（90%）
    update-threshold: 0.9
    # 连接池大小
    thread-pool-size: 10
    # 最大重试次数
    max-retries: 3
    # 重试间隔（毫秒）
    retry-interval: 100

# 日志配置
logging:
  level:
    root: INFO
    com.miniups: DEBUG
    com.miniups.service.LeafIdGeneratorService: DEBUG
    org.springframework.jdbc: WARN
  pattern:
    console: "%d{HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n"
    file: "%d{yyyy-MM-dd HH:mm:ss.SSS} [%thread] %-5level %logger{50} - %msg%n"
  file:
    name: logs/mini-ups.log

# 监控配置
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus,leaf
  endpoint:
    health:
      show-details: always
    metrics:
      enabled: true
    prometheus:
      enabled: true
  metrics:
    export:
      prometheus:
        enabled: true
```

### 2.2 开发环境配置（application-local.yml）

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/ups_db
    username: postgres
    password: abc123

  jpa:
    show-sql: true
    hibernate:
      ddl-auto: validate

leaf:
  segment:
    default-step: 500  # 开发环境使用较小步长
    update-threshold: 0.8

logging:
  level:
    com.miniups: DEBUG
    org.springframework.jdbc.core.JdbcTemplate: DEBUG
```

### 2.3 测试环境配置（application-test.yml）

```yaml
spring:
  datasource:
    driver-class-name: org.h2.Driver
    url: jdbc:h2:mem:testdb;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE
    username: sa
    password:

  jpa:
    show-sql: true
    hibernate:
      ddl-auto: create-drop

  h2:
    console:
      enabled: true

leaf:
  segment:
    enabled: true
    default-step: 100  # 测试环境使用更小步长
    max-retries: 1

logging:
  level:
    com.miniups: DEBUG
```

### 2.4 Docker环境配置（application-docker.yml）

```yaml
spring:
  datasource:
    url: jdbc:postgresql://upsdb:5432/ups_db
    username: postgres
    password: abc123

  redis:
    host: redis
    port: 6379

leaf:
  segment:
    default-step: 2000  # Docker环境适中步长
    thread-pool-size: 15

logging:
  level:
    root: WARN
    com.miniups: INFO
```

## 3. 核心服务实现

### 3.1 配置属性类

```java
package com.miniups.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Leaf分布式ID配置属性类
 * 映射application.yml中的leaf配置
 */
@ConfigurationProperties(prefix = "leaf")
public record LeafProperties(
    SegmentProperties segment
) {
    public record SegmentProperties(
        @DefaultValue("true") boolean enabled,
        @DefaultValue("1000") int defaultStep,
        @DefaultValue("0.9") double updateThreshold,
        @DefaultValue("10") int threadPoolSize,
        @DefaultValue("3") int maxRetries,
        @DefaultValue("100") long retryInterval
    ) {}
}
```

### 3.2 LeafIdGeneratorService完整实现

```java
package com.miniups.service;

import com.miniups.config.LeafProperties;
import com.miniups.exception.LeafIdGenerationException;
import com.miniups.model.entity.LeafAlloc;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

@Service
@Slf4j
public class LeafIdGeneratorService {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private LeafProperties leafProperties;

    @Autowired
    private MeterRegistry meterRegistry;

    // 业务号段缓存
    private final ConcurrentHashMap<String, SegmentBuffer> cache = new ConcurrentHashMap<>();

    // 线程池
    private ExecutorService executorService;

    // 监控指标
    private Counter idGenerationCounter;
    private Timer dbAccessTimer;
    private Counter errorCounter;

    @PostConstruct
    public void init() {
        if (!leafProperties.segment().enabled()) {
            log.warn("Leaf segment is disabled, ID generation service will not start");
            return;
        }

        // 初始化线程池
        this.executorService = Executors.newFixedThreadPool(
            leafProperties.segment().threadPoolSize(),
            r -> {
                Thread t = new Thread(r);
                t.setName("leaf-segment-" + t.getId());
                t.setDaemon(true);
                return t;
            }
        );

        // 初始化监控指标
        initMetrics();

        log.info("LeafIdGeneratorService initialized with config: {}", leafProperties.segment());
    }

    @PreDestroy
    public void destroy() {
        if (executorService != null) {
            executorService.shutdown();
            try {
                if (!executorService.awaitTermination(30, TimeUnit.SECONDS)) {
                    executorService.shutdownNow();
                }
            } catch (InterruptedException e) {
                executorService.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
    }

    private void initMetrics() {
        this.idGenerationCounter = Counter.builder("leaf.id.generated")
            .description("Number of IDs generated")
            .register(meterRegistry);

        this.dbAccessTimer = Timer.builder("leaf.db.access")
            .description("Database access time for segment updates")
            .register(meterRegistry);

        this.errorCounter = Counter.builder("leaf.error")
            .description("Number of errors in ID generation")
            .register(meterRegistry);
    }

    /**
     * 获取下一个ID - 主要对外接口
     */
    public long nextId(String bizTag) {
        if (!leafProperties.segment().enabled()) {
            throw new LeafIdGenerationException("Leaf segment service is disabled");
        }

        if (StringUtils.isEmpty(bizTag)) {
            throw new IllegalArgumentException("bizTag cannot be empty");
        }

        Timer.Sample sample = Timer.start(meterRegistry);
        try {
            SegmentBuffer buffer = getOrCreateBuffer(bizTag);
            long id = buffer.nextId(bizTag, this);

            idGenerationCounter.increment();
            return id;

        } catch (Exception e) {
            errorCounter.increment();
            log.error("Failed to generate ID for bizTag: {}", bizTag, e);
            throw new LeafIdGenerationException("Failed to generate ID for bizTag: " + bizTag, e);
        } finally {
            sample.stop(Timer.builder("leaf.id.generation").register(meterRegistry));
        }
    }

    /**
     * 获取或创建SegmentBuffer
     */
    private SegmentBuffer getOrCreateBuffer(String bizTag) {
        return cache.computeIfAbsent(bizTag, key -> {
            SegmentBuffer buffer = new SegmentBuffer();
            buffer.setKey(key);
            return buffer;
        });
    }

    /**
     * 从数据库更新号段
     */
    @Transactional
    public LeafAlloc updateMaxIdAndGetLeafAlloc(String bizTag) {
        String updateSql = "UPDATE leaf_alloc SET max_id = max_id + step WHERE biz_tag = ?";
        String selectSql = "SELECT biz_tag, max_id, step, description FROM leaf_alloc WHERE biz_tag = ?";

        Timer.Sample sample = Timer.start(meterRegistry);
        try {
            int affectedRows = jdbcTemplate.update(updateSql, bizTag);
            if (affectedRows == 0) {
                throw new LeafIdGenerationException("No record found for bizTag: " + bizTag);
            }

            LeafAlloc leafAlloc = jdbcTemplate.queryForObject(
                selectSql,
                new BeanPropertyRowMapper<>(LeafAlloc.class),
                bizTag
            );

            log.debug("Updated segment for bizTag: {}, new max_id: {}", bizTag, leafAlloc.getMaxId());
            return leafAlloc;

        } catch (DataAccessException e) {
            throw new LeafIdGenerationException("Database error when updating segment for bizTag: " + bizTag, e);
        } finally {
            sample.stop(dbAccessTimer);
        }
    }

    /**
     * 获取所有业务标识
     */
    public List<String> getAllBizTags() {
        String sql = "SELECT biz_tag FROM leaf_alloc ORDER BY biz_tag";
        return jdbcTemplate.queryForList(sql, String.class);
    }

    /**
     * 获取业务统计信息
     */
    public List<LeafAlloc> getAllLeafAllocs() {
        String sql = "SELECT biz_tag, max_id, step, description, update_time FROM leaf_alloc ORDER BY update_time DESC";
        return jdbcTemplate.query(sql, new BeanPropertyRowMapper<>(LeafAlloc.class));
    }

    /**
     * SegmentBuffer内部类
     */
    public class SegmentBuffer {
        private String key;
        private Segment[] segments;
        private volatile int currentPos;
        private volatile boolean nextReady;
        private volatile boolean initOk;
        private final AtomicBoolean threadRunning;
        private final ReadWriteLock lock;

        public SegmentBuffer() {
            segments = new Segment[]{new Segment(), new Segment()};
            currentPos = 0;
            nextReady = false;
            initOk = false;
            threadRunning = new AtomicBoolean(false);
            lock = new ReentrantReadWriteLock();
        }

        public long nextId(String bizTag, LeafIdGeneratorService service) {
            if (!initOk) {
                synchronized (this) {
                    if (!initOk) {
                        try {
                            updateSegmentFromDB(bizTag, getCurrentSegment(), service);
                            initOk = true;
                        } catch (Exception e) {
                            throw new LeafIdGenerationException("Initialize segment failed for bizTag: " + bizTag, e);
                        }
                    }
                }
            }

            return getIdFromSegment(bizTag, service);
        }

        private long getIdFromSegment(String bizTag, LeafIdGeneratorService service) {
            int maxRetries = leafProperties.segment().maxRetries();
            long retryInterval = leafProperties.segment().retryInterval();

            for (int retry = 0; retry < maxRetries; retry++) {
                lock.readLock().lock();
                try {
                    Segment segment = getCurrentSegment();

                    // 检查是否需要预加载
                    if (!nextReady && shouldUpdateNext(segment) &&
                        threadRunning.compareAndSet(false, true)) {
                        asyncUpdateNextSegment(bizTag, service);
                    }

                    long value = segment.getIdAndIncrement();
                    if (value < segment.getMax()) {
                        return value;
                    }

                } finally {
                    lock.readLock().unlock();
                }

                // 当前号段用完，切换到下一个
                waitAndSwitchSegment(bizTag, service);

                if (retry > 0) {
                    try {
                        Thread.sleep(retryInterval);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        throw new LeafIdGenerationException("Thread interrupted during retry", e);
                    }
                }
            }

            throw new LeafIdGenerationException("Failed to get ID after " + maxRetries + " retries");
        }

        private boolean shouldUpdateNext(Segment segment) {
            long current = segment.getValue().get();
            long max = segment.getMax();
            double threshold = leafProperties.segment().updateThreshold();
            return current >= max * threshold;
        }

        private void asyncUpdateNextSegment(String bizTag, LeafIdGeneratorService service) {
            executorService.execute(() -> {
                try {
                    Segment nextSegment = getNextSegment();
                    updateSegmentFromDB(bizTag, nextSegment, service);
                    nextReady = true;
                    log.debug("Async loaded next segment for bizTag: {}", bizTag);
                } catch (Exception e) {
                    log.error("Failed to async update next segment for bizTag: {}", bizTag, e);
                } finally {
                    threadRunning.set(false);
                }
            });
        }

        private void waitAndSwitchSegment(String bizTag, LeafIdGeneratorService service) {
            lock.writeLock().lock();
            try {
                if (!nextReady) {
                    Segment nextSegment = getNextSegment();
                    updateSegmentFromDB(bizTag, nextSegment, service);
                    nextReady = true;
                }

                currentPos = (currentPos + 1) % 2;
                nextReady = false;

                log.debug("Switched to next segment for bizTag: {}", bizTag);
            } finally {
                lock.writeLock().unlock();
            }
        }

        private void updateSegmentFromDB(String bizTag, Segment segment, LeafIdGeneratorService service) {
            LeafAlloc leafAlloc = service.updateMaxIdAndGetLeafAlloc(bizTag);

            segment.getValue().set(leafAlloc.getMaxId() - leafAlloc.getStep());
            segment.setMax(leafAlloc.getMaxId());
            segment.setStep(leafAlloc.getStep());
            segment.setReady(true);

            log.debug("Updated segment for bizTag: {}, range: [{}-{}]",
                     bizTag, segment.getValue().get(), segment.getMax());
        }

        private Segment getCurrentSegment() {
            return segments[currentPos];
        }

        private Segment getNextSegment() {
            return segments[(currentPos + 1) % 2];
        }

        // Getters and setters
        public void setKey(String key) { this.key = key; }
        public String getKey() { return key; }
    }

    /**
     * Segment内部类
     */
    public static class Segment {
        private AtomicLong value;
        private volatile long max;
        private volatile int step;
        private volatile boolean ready;

        public Segment() {
            this.value = new AtomicLong(0);
            this.max = 0;
            this.step = 0;
            this.ready = false;
        }

        public long getIdAndIncrement() {
            return value.getAndIncrement();
        }

        public boolean useful() {
            return value.get() < max;
        }

        // Getters and setters
        public AtomicLong getValue() { return value; }
        public long getMax() { return max; }
        public void setMax(long max) { this.max = max; }
        public int getStep() { return step; }
        public void setStep(int step) { this.step = step; }
        public boolean isReady() { return ready; }
        public void setReady(boolean ready) { this.ready = ready; }
    }
}
```

## 4. 配置类和Bean注册

### 4.1 Leaf配置类

```java
package com.miniups.config;

import com.miniups.service.LeafIdGeneratorService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/**
 * Leaf分布式ID系统配置类
 */
@Configuration
@EnableConfigurationProperties(LeafProperties.class)
@Slf4j
public class LeafConfiguration {

    /**
     * 注册LeafIdGeneratorService Bean
     * 只在非测试环境中启用
     */
    @Bean
    @Profile("!test")
    public LeafIdGeneratorService leafIdGeneratorService() {
        log.info("Creating LeafIdGeneratorService bean");
        return new LeafIdGeneratorService();
    }

    /**
     * 测试环境的Mock Bean
     */
    @Bean
    @Profile("test")
    public LeafIdGeneratorService mockLeafIdGeneratorService() {
        log.info("Creating Mock LeafIdGeneratorService for testing");
        return new MockLeafIdGeneratorService();
    }
}

/**
 * 测试环境使用的Mock实现
 */
class MockLeafIdGeneratorService extends LeafIdGeneratorService {
    private final AtomicLong counter = new AtomicLong(1);

    @Override
    public long nextId(String bizTag) {
        return counter.getAndIncrement();
    }
}
```

### 4.2 异常类定义

```java
package com.miniups.exception;

/**
 * Leaf ID生成异常
 */
public class LeafIdGenerationException extends RuntimeException {

    public LeafIdGenerationException(String message) {
        super(message);
    }

    public LeafIdGenerationException(String message, Throwable cause) {
        super(message, cause);
    }
}
```

### 4.3 健康检查配置

```java
package com.miniups.actuator;

import com.miniups.service.LeafIdGeneratorService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

/**
 * Leaf服务健康检查
 */
@Component("leaf")
@RequiredArgsConstructor
public class LeafHealthIndicator implements HealthIndicator {

    private final LeafIdGeneratorService leafIdGeneratorService;

    @Override
    public Health health() {
        try {
            // 尝试生成一个测试ID
            long testId = leafIdGeneratorService.nextId("health_check");

            return Health.up()
                .withDetail("service", "LeafIdGeneratorService")
                .withDetail("test_id", testId)
                .withDetail("status", "operational")
                .build();

        } catch (Exception e) {
            return Health.down()
                .withDetail("service", "LeafIdGeneratorService")
                .withDetail("error", e.getMessage())
                .withDetail("status", "failed")
                .build();
        }
    }
}
```

## 5. 控制器集成

### 5.1 管理控制器

```java
package com.miniups.controller;

import com.miniups.model.entity.LeafAlloc;
import com.miniups.service.LeafIdGeneratorService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Leaf分布式ID管理控制器
 * 提供ID生成和管理接口
 */
@RestController
@RequestMapping("/admin/leaf")
@RequiredArgsConstructor
@Tag(name = "Leaf ID Management", description = "分布式ID管理接口")
public class LeafAdminController {

    private final LeafIdGeneratorService leafIdGeneratorService;

    @Operation(summary = "生成ID", description = "为指定业务生成下一个ID")
    @PostMapping("/generate/{bizTag}")
    public ResponseEntity<Map<String, Object>> generateId(@PathVariable String bizTag) {
        long id = leafIdGeneratorService.nextId(bizTag);

        Map<String, Object> response = new HashMap<>();
        response.put("bizTag", bizTag);
        response.put("id", id);
        response.put("timestamp", System.currentTimeMillis());

        return ResponseEntity.ok(response);
    }

    @Operation(summary = "批量生成ID", description = "为指定业务批量生成ID")
    @PostMapping("/generate/{bizTag}/batch")
    public ResponseEntity<Map<String, Object>> generateBatchIds(
            @PathVariable String bizTag,
            @RequestParam(defaultValue = "10") int count) {

        if (count > 1000) {
            return ResponseEntity.badRequest().body(Map.of("error", "Count cannot exceed 1000"));
        }

        List<Long> ids = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            ids.add(leafIdGeneratorService.nextId(bizTag));
        }

        Map<String, Object> response = new HashMap<>();
        response.put("bizTag", bizTag);
        response.put("count", count);
        response.put("ids", ids);
        response.put("timestamp", System.currentTimeMillis());

        return ResponseEntity.ok(response);
    }

    @Operation(summary = "获取所有业务标识", description = "获取系统中配置的所有业务标识")
    @GetMapping("/biz-tags")
    public ResponseEntity<List<String>> getAllBizTags() {
        List<String> bizTags = leafIdGeneratorService.getAllBizTags();
        return ResponseEntity.ok(bizTags);
    }

    @Operation(summary = "获取号段状态", description = "获取所有业务的号段分配状态")
    @GetMapping("/status")
    public ResponseEntity<List<LeafAlloc>> getLeafStatus() {
        List<LeafAlloc> allocs = leafIdGeneratorService.getAllLeafAllocs();
        return ResponseEntity.ok(allocs);
    }

    @Operation(summary = "获取指定业务状态", description = "获取指定业务的号段分配状态")
    @GetMapping("/status/{bizTag}")
    public ResponseEntity<Map<String, Object>> getBizTagStatus(@PathVariable String bizTag) {
        try {
            // 这里可以添加更详细的状态查询逻辑
            Map<String, Object> status = new HashMap<>();
            status.put("bizTag", bizTag);
            status.put("status", "active");
            // 可以添加更多状态信息

            return ResponseEntity.ok(status);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
```

### 5.2 业务控制器集成示例

```java
package com.miniups.controller;

import com.miniups.dto.CreateOrderDto;
import com.miniups.model.entity.Order;
import com.miniups.service.LeafIdGeneratorService;
import com.miniups.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * 订单控制器 - 集成Leaf ID生成
 */
@RestController
@RequestMapping("/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;
    private final LeafIdGeneratorService leafIdGeneratorService;

    @PostMapping
    public ResponseEntity<Order> createOrder(@RequestBody CreateOrderDto dto) {
        // 生成订单ID
        long orderId = leafIdGeneratorService.nextId("order_id");

        // 创建订单
        Order order = orderService.createOrder(orderId, dto);

        return ResponseEntity.ok(order);
    }

    @PostMapping("/{orderId}/shipments")
    public ResponseEntity<Map<String, Object>> createShipment(
            @PathVariable Long orderId,
            @RequestBody CreateShipmentDto dto) {

        // 生成运单ID和追踪号
        long shipmentId = leafIdGeneratorService.nextId("shipment_id");
        long trackingNumber = leafIdGeneratorService.nextId("tracking_number");

        // 业务逻辑处理
        Map<String, Object> result = orderService.createShipment(
            orderId, shipmentId, trackingNumber, dto);

        return ResponseEntity.ok(result);
    }
}
```

## 6. 异常处理

### 6.1 全局异常处理

```java
package com.miniups.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * 全局异常处理器
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(LeafIdGenerationException.class)
    public ResponseEntity<Map<String, Object>> handleLeafIdGenerationException(
            LeafIdGenerationException e) {

        log.error("Leaf ID generation error: {}", e.getMessage(), e);

        Map<String, Object> error = new HashMap<>();
        error.put("error", "ID_GENERATION_FAILED");
        error.put("message", "Failed to generate distributed ID");
        error.put("details", e.getMessage());
        error.put("timestamp", LocalDateTime.now());
        error.put("status", HttpStatus.INTERNAL_SERVER_ERROR.value());

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalArgumentException(
            IllegalArgumentException e) {

        Map<String, Object> error = new HashMap<>();
        error.put("error", "INVALID_PARAMETER");
        error.put("message", e.getMessage());
        error.put("timestamp", LocalDateTime.now());
        error.put("status", HttpStatus.BAD_REQUEST.value());

        return ResponseEntity.badRequest().body(error);
    }
}
```

## 7. 测试配置

### 7.1 单元测试

```java
package com.miniups.service;

import com.miniups.config.LeafProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@SpringBootTest
@ActiveProfiles("test")
class LeafIdGeneratorServiceTest {

    @MockBean
    private JdbcTemplate jdbcTemplate;

    private LeafIdGeneratorService leafIdGeneratorService;
    private LeafProperties leafProperties;

    @BeforeEach
    void setUp() {
        leafProperties = new LeafProperties(
            new LeafProperties.SegmentProperties(
                true, 100, 0.9, 5, 3, 100
            )
        );

        leafIdGeneratorService = new LeafIdGeneratorService();
        // 设置依赖...
    }

    @Test
    void testNextId_FirstTime_ShouldInitializeAndReturnId() {
        // Given
        when(jdbcTemplate.update(anyString(), anyString())).thenReturn(1);
        when(jdbcTemplate.queryForObject(anyString(), any(BeanPropertyRowMapper.class), anyString()))
            .thenReturn(createMockLeafAlloc("test_id", 100L, 100));

        // When
        long id = leafIdGeneratorService.nextId("test_id");

        // Then
        assertThat(id).isGreaterThan(0);
        verify(jdbcTemplate, times(1)).update(anyString(), eq("test_id"));
    }

    @Test
    void testNextId_EmptyBizTag_ShouldThrowException() {
        // When & Then
        assertThatThrownBy(() -> leafIdGeneratorService.nextId(""))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("bizTag cannot be empty");
    }

    private LeafAlloc createMockLeafAlloc(String bizTag, Long maxId, Integer step) {
        LeafAlloc alloc = new LeafAlloc();
        alloc.setBizTag(bizTag);
        alloc.setMaxId(maxId);
        alloc.setStep(step);
        return alloc;
    }
}
```

### 7.2 集成测试

```java
package com.miniups.integration;

import com.miniups.service.LeafIdGeneratorService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Sql(scripts = "/test-data.sql")
class LeafIdGeneratorIntegrationTest {

    @Autowired
    private LeafIdGeneratorService leafIdGeneratorService;

    @Test
    void testConcurrentIdGeneration() throws InterruptedException {
        // Given
        int threadCount = 10;
        int idsPerThread = 100;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);
        Set<Long> generatedIds = ConcurrentHashMap.newKeySet();

        // When
        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    for (int j = 0; j < idsPerThread; j++) {
                        long id = leafIdGeneratorService.nextId("test_concurrent");
                        generatedIds.add(id);
                    }
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executor.shutdown();

        // Then
        assertThat(generatedIds).hasSize(threadCount * idsPerThread);
    }
}
```

### 7.3 测试数据SQL（test-data.sql）

```sql
-- 测试数据初始化
INSERT INTO leaf_alloc (biz_tag, max_id, step, description) VALUES
('test_concurrent', 1, 100, 'Concurrent test'),
('test_basic', 1, 50, 'Basic test'),
('test_large_step', 1, 1000, 'Large step test')
ON CONFLICT (biz_tag) DO UPDATE SET
    max_id = EXCLUDED.max_id,
    step = EXCLUDED.step;
```

通过以上完整的Spring Boot集成配置，你可以在项目中快速集成和使用Leaf分布式ID生成服务。记住根据实际业务需求调整配置参数，并做好监控和异常处理。