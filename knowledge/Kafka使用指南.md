# Mini-UPS Kafka 完整使用指南

## 目录
1. [Kafka 基础概念](#1-kafka-基础概念)
2. [项目中的Kafka架构](#2-项目中的kafka架构)
3. [核心组件介绍](#3-核心组件介绍)
4. [配置和启用](#4-配置和启用)
5. [实际使用示例](#5-实际使用示例)
6. [故障排查](#6-故障排查)
7. [最佳实践](#7-最佳实践)

---

## 1. Kafka 基础概念

### 什么是 Apache Kafka？
Kafka 是一个分布式的流处理平台，主要用于：
- **消息队列**：异步处理业务消息
- **事件流**：实时数据流处理
- **日志收集**：系统日志和事件的统一处理

### 核心概念
- **Topic（主题）**：消息的分类，类似于文件夹
- **Partition（分区）**：Topic的物理分割，提高并发性
- **Producer（生产者）**：发送消息的客户端
- **Consumer（消费者）**：接收消息的客户端
- **Broker（代理）**：Kafka服务器节点

### 为什么使用 Kafka？
1. **高吞吐量**：单节点支持数百万条消息/秒
2. **持久化**：消息持久化存储到磁盘
3. **分布式**：天然支持集群和故障恢复
4. **实时性**：毫秒级别的消息延迟

---

## 2. 项目中的Kafka架构

### 整体设计模式
本项目采用了 **事务性发件箱模式（Transactional Outbox Pattern）**：

```
业务操作 → 数据库事务 → OutboxEvent表 → Kafka发布
    ↓           ↓            ↓           ↓
  订单创建   保存到数据库   记录事件   异步发送到Kafka
```

### 架构优势
1. **事务一致性**：确保业务操作和消息发送的一致性
2. **故障恢复**：即使Kafka暂时不可用，事件也不会丢失
3. **重试机制**：自动重试失败的消息发送
4. **监控能力**：可以追踪每个事件的处理状态

---

## 3. 核心组件介绍

### 3.1 配置类 - KafkaMessagingProperties
```java
// 位置：backend/src/main/java/com/miniups/config/KafkaMessagingProperties.java
```
**功能**：管理所有Kafka相关的配置参数

**重要配置项**：
- `enabled`：是否启用Kafka（默认false）
- `outboxEnabled`：是否启用发件箱模式（默认false）
- `topicPrefix`：所有Topic的前缀
- `defaultTopic`：默认Topic名称
- `sendTimeoutMs`：发送超时时间
- `autoCreateTopics`：是否自动创建Topic

### 3.2 Kafka配置类 - KafkaConfig
```java
// 位置：backend/src/main/java/com/miniups/config/KafkaConfig.java
```
**功能**：自动配置Kafka基础设施

**核心功能**：
- 自动创建业务相关的Topic
- 配置分区数和副本数
- 支持Topic名称前缀

**默认创建的Topics**：
- `shipment.create.request`：货物创建请求
- `shipment.status.updated`：货物状态更新
- `user.registered`：用户注册事件
- `truck.dispatch`：卡车调度事件
- `audit.log.created`：审计日志事件

### 3.3 消息发布器 - KafkaOutboxMessagePublisher
```java
// 位置：backend/src/main/java/com/miniups/service/messaging/kafka/KafkaOutboxMessagePublisher.java
```
**功能**：将OutboxEvent发送到Kafka

**发送流程**：
1. 根据routingKey解析Topic名称
2. 创建ProducerRecord并设置消息头
3. 异步发送到Kafka
4. 等待确认并返回结果

### 3.4 事件实体 - OutboxEvent
```java
// 位置：backend/src/main/java/com/miniups/model/entity/OutboxEvent.java
```
**功能**：存储待发送的事件信息

**核心字段**：
- `eventId`：事件唯一标识
- `aggregateId`：业务实体ID
- `aggregateType`：业务实体类型
- `eventType`：事件类型
- `payload`：事件内容（JSON格式）
- `routingKey`：路由键（映射到Topic）
- `status`：事件状态（PENDING/PUBLISHED/FAILED）

---

## 4. 配置和启用

### 4.1 环境变量配置
在 `backend/src/main/resources/application.yml` 中配置：

```yaml
messaging:
  kafka:
    enabled: ${MESSAGING_KAFKA_ENABLED:false}           # 启用Kafka
    outbox-enabled: ${MESSAGING_KAFKA_OUTBOX_ENABLED:false}  # 启用发件箱
    topic-prefix: ${MESSAGING_KAFKA_TOPIC_PREFIX:}      # Topic前缀
    default-topic: ${MESSAGING_KAFKA_DEFAULT_TOPIC:}    # 默认Topic
    send-timeout-ms: ${MESSAGING_KAFKA_SEND_TIMEOUT_MS:5000}  # 发送超时
    auto-create-topics: ${MESSAGING_KAFKA_AUTO_CREATE_TOPICS:true}  # 自动创建Topic
```

### 4.2 启用Kafka的步骤

#### 步骤1：设置环境变量
```bash
# 基础配置
export MESSAGING_KAFKA_ENABLED=true
export MESSAGING_KAFKA_OUTBOX_ENABLED=true

# 可选配置
export MESSAGING_KAFKA_TOPIC_PREFIX="dev."
export MESSAGING_KAFKA_DEFAULT_TOPIC="outbox.fallback"
```

#### 步骤2：启动Kafka服务器
```bash
# 使用Docker启动Kafka
docker run -p 9092:9092 \
  -e KAFKA_ZOOKEEPER_CONNECT=zookeeper:2181 \
  -e KAFKA_ADVERTISED_LISTENERS=PLAINTEXT://localhost:9092 \
  confluent/kafka
```

#### 步骤3：启动应用
```bash
cd backend
mvn spring-boot:run
```

### 4.3 验证配置
应用启动后查看日志：
```
INFO  - Kafka auto-creation enabled for topics: dev.shipment.create.request, dev.shipment.status.updated, ...
```

---

## 5. 实际使用示例

### 5.1 发送业务事件
在业务代码中使用OutboxEvent：

```java
@Service
@Transactional
public class ShipmentService {

    @Autowired
    private OutboxEventService outboxEventService;

    public Shipment createShipment(CreateShipmentRequest request) {
        // 1. 执行业务操作
        Shipment shipment = new Shipment();
        shipment.setTrackingNumber(generateTrackingNumber());
        shipment = shipmentRepository.save(shipment);

        // 2. 创建事件（在同一个事务中）
        OutboxEvent event = OutboxEvent.builder()
            .eventId(UUID.randomUUID().toString())
            .aggregateId(shipment.getId().toString())
            .aggregateType("Shipment")
            .eventType("ShipmentCreated")
            .payload(toJson(shipment))
            .routingKey("shipment.create.request")
            .status(OutboxEvent.OutboxStatus.PENDING)
            .build();

        outboxEventService.save(event);

        return shipment;
    }
}
```

### 5.2 消费Kafka消息
创建消息消费者：

```java
@Component
@ConditionalOnProperty(prefix = "messaging.kafka", name = "enabled", havingValue = "true")
public class ShipmentEventConsumer {

    @KafkaListener(topics = "#{kafkaConfig.resolveTopicName('shipment.create.request')}")
    public void handleShipmentCreated(
            @Payload String payload,
            @Header Map<String, Object> headers) {

        String eventId = new String((byte[]) headers.get("event-id"));
        String correlationId = new String((byte[]) headers.get("correlation-id"));

        log.info("Received shipment created event: {}", eventId);

        // 处理业务逻辑
        processShipmentCreated(payload);
    }

    private void processShipmentCreated(String payload) {
        // 实现具体的业务处理逻辑
        // 例如：通知Amazon、更新追踪状态等
    }
}
```

### 5.3 监控事件状态
查询事件处理状态：

```java
@RestController
@RequestMapping("/api/events")
public class EventController {

    @Autowired
    private OutboxEventService outboxEventService;

    @GetMapping("/outbox/{eventId}")
    public OutboxEvent getEventStatus(@PathVariable String eventId) {
        return outboxEventService.findByEventId(eventId);
    }

    @GetMapping("/outbox/failed")
    public List<OutboxEvent> getFailedEvents() {
        return outboxEventService.findByStatus(OutboxEvent.OutboxStatus.FAILED);
    }
}
```

---

## 6. 故障排查

### 6.1 常见问题

#### 问题1：Kafka连接失败
**症状**：日志显示连接超时
```
ERROR - Failed to send event to Kafka: Connection timeout
```

**解决方案**：
1. 检查Kafka服务是否运行：`docker ps | grep kafka`
2. 验证网络连接：`telnet localhost 9092`
3. 检查防火墙设置

#### 问题2：Topic不存在
**症状**：日志显示Topic未找到
```
ERROR - Topic 'shipment.create.request' does not exist
```

**解决方案**：
1. 确认`auto-create-topics`设置为true
2. 手动创建Topic：
```bash
kafka-topics.sh --create --topic shipment.create.request \
  --bootstrap-server localhost:9092 --partitions 3 --replication-factor 1
```

#### 问题3：事件堆积
**症状**：OutboxEvent表中大量PENDING状态事件

**解决方案**：
1. 检查OutboxPollerService是否正常运行
2. 查看Kafka消费者lag情况
3. 增加轮询频率或并发度

### 6.2 调试技巧

#### 查看Kafka Topic列表
```bash
kafka-topics.sh --list --bootstrap-server localhost:9092
```

#### 监控消息队列
```bash
kafka-console-consumer.sh --topic shipment.create.request \
  --bootstrap-server localhost:9092 --from-beginning
```

#### 查看事件表状态
```sql
SELECT status, COUNT(*) FROM outbox_events GROUP BY status;
```

---

## 7. 最佳实践

### 7.1 性能优化

#### 批量处理
- OutboxPollerService支持批量处理多个事件
- 建议批大小设置为50-100个事件

#### 合理分区
- 根据业务量设置适当的分区数
- 订单相关：6个分区
- 用户相关：3个分区

### 7.2 数据一致性

#### 幂等性处理
- 每个事件包含唯一的eventId
- 消费者应实现幂等性逻辑

#### 事务边界
- 确保业务操作和OutboxEvent在同一事务中
- 使用`@Transactional`注解

### 7.3 监控和告警

#### 关键指标
- 事件发送成功率
- 消息队列延迟
- 失败事件数量

#### 告警设置
```yaml
# 在application.yml中配置
management:
  metrics:
    export:
      prometheus:
        enabled: true
```

### 7.4 环境配置

#### 开发环境
```bash
export MESSAGING_KAFKA_ENABLED=true
export MESSAGING_KAFKA_TOPIC_PREFIX="dev."
```

#### 生产环境
```bash
export MESSAGING_KAFKA_ENABLED=true
export MESSAGING_KAFKA_TOPIC_PREFIX="prod."
export MESSAGING_KAFKA_SEND_TIMEOUT_MS=10000
```

---

## 总结

本项目的Kafka集成提供了：

1. **可靠的消息传递**：通过发件箱模式确保事件不丢失
2. **简单的配置**：通过环境变量轻松启用/禁用
3. **自动化管理**：自动创建Topic和管理分区
4. **完整的监控**：事件状态跟踪和故障恢复

遵循本指南，你可以：
- 理解Kafka在项目中的作用
- 正确配置和启用Kafka功能
- 编写生产者和消费者代码
- 解决常见的问题和故障

需要帮助时，可以查看项目中的测试用例或联系开发团队。