# 实际使用案例和测试指南

## 目录

1. [基础使用案例](#1-基础使用案例)
2. [高级使用场景](#2-高级使用场景)
3. [性能测试](#3-性能测试)
4. [故障测试](#4-故障测试)
5. [最佳实践](#5-最佳实践)
6. [常见问题解决](#6-常见问题解决)

---

## 1. 基础使用案例

### 1.1 简单ID生成

```java
@RestController
@RequestMapping("/api/basic")
@RequiredArgsConstructor
public class BasicUsageController {

    private final LeafIdGeneratorService leafIdGenerator;

    /**
     * 最简单的使用方式
     */
    @GetMapping("/simple/{bizTag}")
    public ResponseEntity<Map<String, Object>> generateSimpleId(@PathVariable String bizTag) {
        // 直接生成ID
        long id = leafIdGenerator.nextId(bizTag);

        Map<String, Object> response = Map.of(
            "bizTag", bizTag,
            "id", id,
            "timestamp", System.currentTimeMillis()
        );

        return ResponseEntity.ok(response);
    }

    /**
     * 带异常处理的ID生成
     */
    @PostMapping("/safe/{bizTag}")
    public ResponseEntity<Map<String, Object>> generateSafeId(@PathVariable String bizTag) {
        try {
            long id = leafIdGenerator.nextId(bizTag);

            return ResponseEntity.ok(Map.of(
                "success", true,
                "bizTag", bizTag,
                "id", id
            ));

        } catch (LeafIdGenerationException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of(
                    "success", false,
                    "error", e.getMessage(),
                    "bizTag", bizTag
                ));
        }
    }
}
```

### 1.2 业务实体中的应用

```java
@Entity
@Table(name = "orders")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Order {

    @Id
    private Long id;  // 由Leaf生成

    @Column(name = "order_number")
    private String orderNumber;  // 格式化的订单号

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "total_amount")
    private BigDecimal totalAmount;

    @CreationTimestamp
    private LocalDateTime createdAt;

    // 其他字段...
}

@Service
@RequiredArgsConstructor
@Transactional
public class OrderService {

    private final LeafIdGeneratorService leafIdGenerator;
    private final OrderRepository orderRepository;

    /**
     * 创建订单 - 自动生成ID
     */
    public Order createOrder(CreateOrderDto dto) {
        // 生成订单ID
        long orderId = leafIdGenerator.nextId("order_id");

        // 生成格式化的订单号 (例如: ORD202401150001234567)
        String orderNumber = generateOrderNumber(orderId);

        Order order = new Order();
        order.setId(orderId);
        order.setOrderNumber(orderNumber);
        order.setUserId(dto.getUserId());
        order.setTotalAmount(dto.getTotalAmount());

        return orderRepository.save(order);
    }

    /**
     * 生成格式化的订单号
     */
    private String generateOrderNumber(long orderId) {
        String datePrefix = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        return String.format("ORD%s%010d", datePrefix, orderId);
    }
}
```

### 1.3 批量ID生成

```java
@Service
@RequiredArgsConstructor
public class BatchIdService {

    private final LeafIdGeneratorService leafIdGenerator;

    /**
     * 批量生成ID
     */
    public List<Long> generateBatchIds(String bizTag, int count) {
        if (count > 10000) {
            throw new IllegalArgumentException("Batch size cannot exceed 10000");
        }

        List<Long> ids = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            ids.add(leafIdGenerator.nextId(bizTag));
        }

        return ids;
    }

    /**
     * 并发批量生成（更高效）
     */
    public List<Long> generateBatchIdsConcurrent(String bizTag, int count) {
        int threadCount = Math.min(10, count / 100 + 1);
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        List<CompletableFuture<List<Long>>> futures = new ArrayList<>();

        int batchSize = count / threadCount;
        for (int i = 0; i < threadCount; i++) {
            int currentBatchSize = (i == threadCount - 1) ? count - i * batchSize : batchSize;

            CompletableFuture<List<Long>> future = CompletableFuture.supplyAsync(() -> {
                List<Long> batchIds = new ArrayList<>();
                for (int j = 0; j < currentBatchSize; j++) {
                    batchIds.add(leafIdGenerator.nextId(bizTag));
                }
                return batchIds;
            }, executor);

            futures.add(future);
        }

        List<Long> allIds = new ArrayList<>();
        for (CompletableFuture<List<Long>> future : futures) {
            try {
                allIds.addAll(future.get());
            } catch (InterruptedException | ExecutionException e) {
                throw new RuntimeException("Failed to generate batch IDs", e);
            }
        }

        executor.shutdown();
        return allIds;
    }
}
```

## 2. 高级使用场景

### 2.1 多业务场景管理

```java
@Component
@RequiredArgsConstructor
public class BusinessIdManager {

    private final LeafIdGeneratorService leafIdGenerator;

    // 业务类型枚举
    public enum BusinessType {
        ORDER("order_id", "ORD"),
        USER("user_id", "USR"),
        SHIPMENT("shipment_id", "SHP"),
        TRACKING("tracking_number", "TRK"),
        INVOICE("invoice_id", "INV"),
        PAYMENT("payment_id", "PAY"),
        REFUND("refund_id", "RFD");

        private final String bizTag;
        private final String prefix;

        BusinessType(String bizTag, String prefix) {
            this.bizTag = bizTag;
            this.prefix = prefix;
        }

        public String getBizTag() { return bizTag; }
        public String getPrefix() { return prefix; }
    }

    /**
     * 根据业务类型生成ID
     */
    public long generateId(BusinessType businessType) {
        return leafIdGenerator.nextId(businessType.getBizTag());
    }

    /**
     * 生成带前缀的业务编号
     */
    public String generateBusinessNumber(BusinessType businessType) {
        long id = generateId(businessType);
        return String.format("%s%015d", businessType.getPrefix(), id);
    }

    /**
     * 生成带日期的业务编号
     */
    public String generateDateBusinessNumber(BusinessType businessType) {
        long id = generateId(businessType);
        String dateStr = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        return String.format("%s%s%08d", businessType.getPrefix(), dateStr, id);
    }
}

// 使用示例
@Service
@RequiredArgsConstructor
public class ComprehensiveBusinessService {

    private final BusinessIdManager idManager;

    public Order createOrder(CreateOrderDto dto) {
        // 生成订单ID和编号
        long orderId = idManager.generateId(BusinessIdManager.BusinessType.ORDER);
        String orderNumber = idManager.generateDateBusinessNumber(BusinessIdManager.BusinessType.ORDER);

        Order order = new Order();
        order.setId(orderId);
        order.setOrderNumber(orderNumber);
        // ... 其他逻辑
        return order;
    }

    public Shipment createShipment(Long orderId, CreateShipmentDto dto) {
        // 生成运单ID和追踪号
        long shipmentId = idManager.generateId(BusinessIdManager.BusinessType.SHIPMENT);
        String trackingNumber = idManager.generateBusinessNumber(BusinessIdManager.BusinessType.TRACKING);

        Shipment shipment = new Shipment();
        shipment.setId(shipmentId);
        shipment.setTrackingNumber(trackingNumber);
        shipment.setOrderId(orderId);
        // ... 其他逻辑
        return shipment;
    }
}
```

### 2.2 分布式事务中的ID生成

```java
@Service
@RequiredArgsConstructor
@Transactional
public class DistributedTransactionService {

    private final LeafIdGeneratorService leafIdGenerator;
    private final OrderRepository orderRepository;
    private final PaymentRepository paymentRepository;
    private final NotificationService notificationService;

    /**
     * 复杂业务流程中的ID生成
     * 涉及订单、支付、通知等多个业务实体
     */
    public OrderCreationResult createOrderWithPayment(CreateOrderWithPaymentDto dto) {
        // 1. 预生成所有需要的ID（避免事务中途失败导致ID浪费）
        long orderId = leafIdGenerator.nextId("order_id");
        long paymentId = leafIdGenerator.nextId("payment_id");
        long notificationId = leafIdGenerator.nextId("notification_id");

        try {
            // 2. 创建订单
            Order order = new Order();
            order.setId(orderId);
            order.setUserId(dto.getUserId());
            order.setTotalAmount(dto.getTotalAmount());
            order = orderRepository.save(order);

            // 3. 创建支付记录
            Payment payment = new Payment();
            payment.setId(paymentId);
            payment.setOrderId(orderId);
            payment.setAmount(dto.getTotalAmount());
            payment.setStatus(PaymentStatus.PENDING);
            payment = paymentRepository.save(payment);

            // 4. 发送通知（异步）
            CompletableFuture.runAsync(() -> {
                notificationService.sendOrderCreatedNotification(
                    notificationId, orderId, dto.getUserId()
                );
            });

            return OrderCreationResult.builder()
                .order(order)
                .payment(payment)
                .notificationId(notificationId)
                .success(true)
                .build();

        } catch (Exception e) {
            // 事务回滚，但ID已经被消费（这是正常的，避免了ID重复使用的风险）
            log.error("Failed to create order with payment, orderId: {}, paymentId: {}",
                     orderId, paymentId, e);
            throw new BusinessException("Failed to create order with payment", e);
        }
    }
}
```

### 2.3 缓存友好的ID生成

```java
@Service
@RequiredArgsConstructor
public class CachedIdService {

    private final LeafIdGeneratorService leafIdGenerator;
    private final RedisTemplate<String, Object> redisTemplate;

    /**
     * 带缓存的ID生成（用于减少数据库压力）
     */
    @Cacheable(value = "leaf-ids", key = "#bizTag + '-' + #count")
    public List<Long> getPreGeneratedIds(String bizTag, int count) {
        List<Long> ids = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            ids.add(leafIdGenerator.nextId(bizTag));
        }
        return ids;
    }

    /**
     * 预热ID池
     */
    @PostConstruct
    public void warmupIdPool() {
        // 为高频业务预生成ID
        String[] highFrequencyBizTags = {"order_id", "tracking_number", "user_id"};

        for (String bizTag : highFrequencyBizTags) {
            try {
                List<Long> preGeneratedIds = getPreGeneratedIds(bizTag, 1000);
                log.info("Warmed up {} IDs for bizTag: {}", preGeneratedIds.size(), bizTag);
            } catch (Exception e) {
                log.error("Failed to warm up IDs for bizTag: {}", bizTag, e);
            }
        }
    }

    /**
     * 从预生成的ID池中获取ID
     */
    public Long getIdFromPool(String bizTag) {
        String poolKey = "leaf-id-pool:" + bizTag;
        Object id = redisTemplate.opsForList().leftPop(poolKey);

        if (id != null) {
            // 如果池中ID数量不足，异步补充
            Long poolSize = redisTemplate.opsForList().size(poolKey);
            if (poolSize != null && poolSize < 100) {
                CompletableFuture.runAsync(() -> refillIdPool(bizTag));
            }
            return (Long) id;
        } else {
            // 池为空，直接生成
            return leafIdGenerator.nextId(bizTag);
        }
    }

    /**
     * 补充ID池
     */
    @Async
    public void refillIdPool(String bizTag) {
        String poolKey = "leaf-id-pool:" + bizTag;
        List<Long> newIds = getPreGeneratedIds(bizTag, 500);

        // 批量添加到Redis
        redisTemplate.opsForList().rightPushAll(poolKey, newIds.toArray());
        redisTemplate.expire(poolKey, Duration.ofHours(1));

        log.info("Refilled {} IDs for bizTag: {}", newIds.size(), bizTag);
    }
}
```

## 3. 性能测试

### 3.1 单线程性能测试

```java
@Component
@RequiredArgsConstructor
@Slf4j
public class LeafPerformanceTest {

    private final LeafIdGeneratorService leafIdGenerator;

    /**
     * 单线程性能测试
     */
    public PerformanceResult testSingleThreadPerformance(String bizTag, int count) {
        long startTime = System.currentTimeMillis();
        Set<Long> generatedIds = new HashSet<>();

        for (int i = 0; i < count; i++) {
            long id = leafIdGenerator.nextId(bizTag);
            generatedIds.add(id);
        }

        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;

        return PerformanceResult.builder()
            .testType("Single Thread")
            .bizTag(bizTag)
            .totalIds(count)
            .uniqueIds(generatedIds.size())
            .durationMs(duration)
            .qps(count * 1000.0 / duration)
            .success(generatedIds.size() == count)
            .build();
    }

    /**
     * 预热测试（避免冷启动影响性能测试结果）
     */
    public void warmup(String bizTag) {
        log.info("Starting warmup for bizTag: {}", bizTag);
        for (int i = 0; i < 1000; i++) {
            leafIdGenerator.nextId(bizTag);
        }
        log.info("Warmup completed for bizTag: {}", bizTag);
    }
}

@Data
@Builder
public class PerformanceResult {
    private String testType;
    private String bizTag;
    private int totalIds;
    private int uniqueIds;
    private long durationMs;
    private double qps;
    private boolean success;
    private String errorMessage;
}
```

### 3.2 并发性能测试

```java
@Component
@RequiredArgsConstructor
@Slf4j
public class ConcurrentPerformanceTest {

    private final LeafIdGeneratorService leafIdGenerator;

    /**
     * 并发性能测试
     */
    public PerformanceResult testConcurrentPerformance(
            String bizTag, int threadCount, int idsPerThread) {

        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);
        ConcurrentHashMap<Long, Boolean> generatedIds = new ConcurrentHashMap<>();
        AtomicLong errorCount = new AtomicLong(0);

        long startTime = System.currentTimeMillis();

        // 启动所有线程
        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    for (int j = 0; j < idsPerThread; j++) {
                        try {
                            long id = leafIdGenerator.nextId(bizTag);
                            generatedIds.put(id, true);
                        } catch (Exception e) {
                            errorCount.incrementAndGet();
                            log.error("Error generating ID in thread", e);
                        }
                    }
                } finally {
                    latch.countDown();
                }
            });
        }

        try {
            // 等待所有线程完成
            latch.await(60, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;
        int expectedTotal = threadCount * idsPerThread;

        executor.shutdown();

        return PerformanceResult.builder()
            .testType("Concurrent")
            .bizTag(bizTag)
            .totalIds(expectedTotal)
            .uniqueIds(generatedIds.size())
            .durationMs(duration)
            .qps(expectedTotal * 1000.0 / duration)
            .success(generatedIds.size() == expectedTotal && errorCount.get() == 0)
            .errorMessage(errorCount.get() > 0 ? "Errors: " + errorCount.get() : null)
            .build();
    }

    /**
     * 压力测试
     */
    public List<PerformanceResult> stressTest(String bizTag) {
        List<PerformanceResult> results = new ArrayList<>();

        // 不同并发级别的测试
        int[] threadCounts = {1, 5, 10, 20, 50, 100};
        int idsPerThread = 1000;

        for (int threadCount : threadCounts) {
            log.info("Running stress test with {} threads", threadCount);

            PerformanceResult result = testConcurrentPerformance(bizTag, threadCount, idsPerThread);
            results.add(result);

            log.info("Thread count: {}, QPS: {}, Success: {}",
                    threadCount, String.format("%.2f", result.getQps()), result.isSuccess());

            // 线程间休息避免过度压力
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }

        return results;
    }
}
```

### 3.3 性能测试控制器

```java
@RestController
@RequestMapping("/admin/leaf/test")
@RequiredArgsConstructor
public class LeafPerformanceTestController {

    private final LeafPerformanceTest performanceTest;
    private final ConcurrentPerformanceTest concurrentTest;

    @PostMapping("/single-thread")
    public ResponseEntity<PerformanceResult> testSingleThread(
            @RequestParam String bizTag,
            @RequestParam(defaultValue = "10000") int count) {

        // 预热
        performanceTest.warmup(bizTag);

        // 执行测试
        PerformanceResult result = performanceTest.testSingleThreadPerformance(bizTag, count);

        return ResponseEntity.ok(result);
    }

    @PostMapping("/concurrent")
    public ResponseEntity<PerformanceResult> testConcurrent(
            @RequestParam String bizTag,
            @RequestParam(defaultValue = "10") int threadCount,
            @RequestParam(defaultValue = "1000") int idsPerThread) {

        PerformanceResult result = concurrentTest.testConcurrentPerformance(
            bizTag, threadCount, idsPerThread);

        return ResponseEntity.ok(result);
    }

    @PostMapping("/stress")
    public ResponseEntity<List<PerformanceResult>> stressTest(
            @RequestParam String bizTag) {

        List<PerformanceResult> results = concurrentTest.stressTest(bizTag);

        return ResponseEntity.ok(results);
    }

    @GetMapping("/benchmark")
    public ResponseEntity<Map<String, Object>> benchmark() {
        Map<String, Object> benchmark = new HashMap<>();

        // 测试不同业务的性能
        String[] bizTags = {"order_id", "user_id", "tracking_number"};

        for (String bizTag : bizTags) {
            performanceTest.warmup(bizTag);
            PerformanceResult result = performanceTest.testSingleThreadPerformance(bizTag, 5000);
            benchmark.put(bizTag, result);
        }

        return ResponseEntity.ok(benchmark);
    }
}
```

## 4. 故障测试

### 4.1 数据库故障模拟

```java
@Component
@RequiredArgsConstructor
@Slf4j
public class FailureSimulationTest {

    private final LeafIdGeneratorService leafIdGenerator;
    private final DataSource dataSource;

    /**
     * 模拟数据库连接中断
     */
    public void simulateDatabaseFailure() {
        try {
            // 获取HikariCP连接池
            HikariDataSource hikariDataSource = (HikariDataSource) dataSource;

            // 临时关闭连接池
            hikariDataSource.close();

            log.info("Database connection pool closed");

            // 尝试生成ID（应该失败）
            try {
                long id = leafIdGenerator.nextId("test_failure");
                log.error("ID generation should have failed, but got: {}", id);
            } catch (Exception e) {
                log.info("Expected failure: {}", e.getMessage());
            }

        } catch (Exception e) {
            log.error("Error in failure simulation", e);
        }
    }

    /**
     * 测试网络延迟影响
     */
    public PerformanceResult testWithSimulatedLatency(String bizTag, int delayMs) {
        // 模拟网络延迟的代理DataSource
        DataSource slowDataSource = createSlowDataSource(dataSource, delayMs);

        long startTime = System.currentTimeMillis();
        int successCount = 0;
        int totalAttempts = 100;

        for (int i = 0; i < totalAttempts; i++) {
            try {
                long id = leafIdGenerator.nextId(bizTag);
                successCount++;
            } catch (Exception e) {
                log.warn("ID generation failed with delay {}: {}", delayMs, e.getMessage());
            }
        }

        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;

        return PerformanceResult.builder()
            .testType("Latency Simulation")
            .bizTag(bizTag)
            .totalIds(totalAttempts)
            .uniqueIds(successCount)
            .durationMs(duration)
            .qps(successCount * 1000.0 / duration)
            .success(successCount == totalAttempts)
            .errorMessage(successCount < totalAttempts ?
                "Failed: " + (totalAttempts - successCount) : null)
            .build();
    }

    private DataSource createSlowDataSource(DataSource originalDataSource, int delayMs) {
        // 创建代理数据源，在每次获取连接时增加延迟
        return (DataSource) Proxy.newProxyInstance(
            originalDataSource.getClass().getClassLoader(),
            new Class[]{DataSource.class},
            (proxy, method, args) -> {
                if ("getConnection".equals(method.getName())) {
                    try {
                        Thread.sleep(delayMs);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }
                return method.invoke(originalDataSource, args);
            }
        );
    }
}
```

### 4.2 并发冲突测试

```java
@Component
@RequiredArgsConstructor
@Slf4j
public class ConcurrencyConflictTest {

    private final LeafIdGeneratorService leafIdGenerator;

    /**
     * 测试高并发下的ID唯一性
     */
    public ConcurrencyTestResult testIdUniqueness(String bizTag, int threadCount, int idsPerThread) {
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch finishLatch = new CountDownLatch(threadCount);

        ConcurrentHashMap<Long, AtomicInteger> idCounts = new ConcurrentHashMap<>();
        AtomicLong totalGenerated = new AtomicLong(0);
        AtomicLong errorCount = new AtomicLong(0);

        // 创建所有线程但不立即执行
        for (int i = 0; i < threadCount; i++) {
            final int threadId = i;
            executor.submit(() -> {
                try {
                    // 等待统一开始信号
                    startLatch.await();

                    for (int j = 0; j < idsPerThread; j++) {
                        try {
                            long id = leafIdGenerator.nextId(bizTag);
                            totalGenerated.incrementAndGet();

                            // 记录ID出现次数（检测重复）
                            idCounts.computeIfAbsent(id, k -> new AtomicInteger(0))
                                   .incrementAndGet();

                        } catch (Exception e) {
                            errorCount.incrementAndGet();
                            log.error("Thread {} error at iteration {}: {}", threadId, j, e.getMessage());
                        }
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    finishLatch.countDown();
                }
            });
        }

        // 统一开始所有线程
        long startTime = System.currentTimeMillis();
        startLatch.countDown();

        try {
            // 等待所有线程完成
            finishLatch.await(120, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        long endTime = System.currentTimeMillis();
        executor.shutdown();

        // 分析结果
        long duplicateIds = idCounts.entrySet().stream()
            .filter(entry -> entry.getValue().get() > 1)
            .count();

        int expectedTotal = threadCount * idsPerThread;
        long actualTotal = totalGenerated.get();

        return ConcurrencyTestResult.builder()
            .bizTag(bizTag)
            .threadCount(threadCount)
            .idsPerThread(idsPerThread)
            .expectedTotal(expectedTotal)
            .actualGenerated(actualTotal)
            .uniqueIds(idCounts.size())
            .duplicateIds(duplicateIds)
            .errorCount(errorCount.get())
            .durationMs(endTime - startTime)
            .success(duplicateIds == 0 && errorCount.get() == 0)
            .build();
    }

    @Data
    @Builder
    public static class ConcurrencyTestResult {
        private String bizTag;
        private int threadCount;
        private int idsPerThread;
        private int expectedTotal;
        private long actualGenerated;
        private int uniqueIds;
        private long duplicateIds;
        private long errorCount;
        private long durationMs;
        private boolean success;
    }
}
```

## 5. 最佳实践

### 5.1 ID生成工具类

```java
@Component
@RequiredArgsConstructor
@Slf4j
public class IdGenerationUtils {

    private final LeafIdGeneratorService leafIdGenerator;

    /**
     * 安全的ID生成方法（带重试机制）
     */
    public long generateIdSafely(String bizTag, int maxRetries) {
        int attempt = 0;
        Exception lastException = null;

        while (attempt < maxRetries) {
            try {
                return leafIdGenerator.nextId(bizTag);
            } catch (Exception e) {
                lastException = e;
                attempt++;
                log.warn("ID generation attempt {} failed for bizTag: {}, error: {}",
                        attempt, bizTag, e.getMessage());

                if (attempt < maxRetries) {
                    // 指数退避重试
                    try {
                        Thread.sleep(100 * (1L << (attempt - 1)));
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }
        }

        throw new LeafIdGenerationException(
            String.format("Failed to generate ID for bizTag: %s after %d attempts", bizTag, maxRetries),
            lastException
        );
    }

    /**
     * 批量生成ID（优化版本）
     */
    public List<Long> generateBatchIdsOptimized(String bizTag, int count) {
        if (count <= 0) {
            return Collections.emptyList();
        }

        if (count == 1) {
            return Collections.singletonList(generateIdSafely(bizTag, 3));
        }

        // 对于大批量，使用并行生成
        if (count > 1000) {
            return generateBatchIdsParallel(bizTag, count);
        }

        // 中等批量，串行生成
        List<Long> ids = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            ids.add(leafIdGenerator.nextId(bizTag));
        }

        return ids;
    }

    private List<Long> generateBatchIdsParallel(String bizTag, int count) {
        int threadCount = Math.min(Runtime.getRuntime().availableProcessors(), count / 100);
        int batchSize = count / threadCount;

        List<CompletableFuture<List<Long>>> futures = new ArrayList<>();

        for (int i = 0; i < threadCount; i++) {
            final int currentBatchSize = (i == threadCount - 1) ? count - i * batchSize : batchSize;

            CompletableFuture<List<Long>> future = CompletableFuture.supplyAsync(() -> {
                List<Long> batchIds = new ArrayList<>(currentBatchSize);
                for (int j = 0; j < currentBatchSize; j++) {
                    batchIds.add(leafIdGenerator.nextId(bizTag));
                }
                return batchIds;
            });

            futures.add(future);
        }

        return futures.stream()
            .map(CompletableFuture::join)
            .flatMap(List::stream)
            .collect(Collectors.toList());
    }

    /**
     * 验证ID格式（业务相关）
     */
    public boolean isValidId(String bizTag, long id) {
        // 基本验证
        if (id <= 0) {
            return false;
        }

        // 业务特定验证
        switch (bizTag) {
            case "order_id":
                return id >= 100000; // 订单ID应该从100000开始
            case "user_id":
                return id >= 10000;  // 用户ID应该从10000开始
            case "tracking_number":
                return id >= 1000000; // 追踪号应该从1000000开始
            default:
                return true;
        }
    }
}
```

### 5.2 监控和告警

```java
@Component
@RequiredArgsConstructor
@Slf4j
public class LeafMonitoringService {

    private final LeafIdGeneratorService leafIdGenerator;
    private final MeterRegistry meterRegistry;
    private final AlertService alertService;

    @EventListener
    @Async
    public void handleIdGenerationEvent(IdGenerationEvent event) {
        // 记录指标
        Timer.Sample sample = Timer.start(meterRegistry);
        sample.stop(Timer.builder("leaf.id.generation")
                .tag("biz_tag", event.getBizTag())
                .tag("success", String.valueOf(event.isSuccess()))
                .register(meterRegistry));

        // 检查是否需要告警
        checkAndAlert(event);
    }

    private void checkAndAlert(IdGenerationEvent event) {
        if (!event.isSuccess()) {
            // ID生成失败告警
            alertService.sendAlert(AlertLevel.HIGH,
                "Leaf ID Generation Failed",
                String.format("Failed to generate ID for bizTag: %s, error: %s",
                    event.getBizTag(), event.getErrorMessage()));
        }

        // 检查生成速度
        double recentQps = getRecentQps(event.getBizTag());
        if (recentQps > 10000) {  // QPS过高告警
            alertService.sendAlert(AlertLevel.MEDIUM,
                "High ID Generation QPS",
                String.format("BizTag %s has high QPS: %.2f", event.getBizTag(), recentQps));
        }
    }

    private double getRecentQps(String bizTag) {
        // 从监控指标中获取最近的QPS
        Timer timer = meterRegistry.find("leaf.id.generation")
            .tag("biz_tag", bizTag)
            .tag("success", "true")
            .timer();

        return timer != null ? timer.mean(TimeUnit.SECONDS) : 0.0;
    }

    /**
     * 健康检查定时任务
     */
    @Scheduled(fixedDelay = 60000) // 每分钟检查一次
    public void healthCheck() {
        String[] criticalBizTags = {"order_id", "user_id", "tracking_number"};

        for (String bizTag : criticalBizTags) {
            try {
                // 尝试生成测试ID
                long testId = leafIdGenerator.nextId("health_check_" + bizTag);
                log.debug("Health check passed for bizTag: {}, testId: {}", bizTag, testId);

            } catch (Exception e) {
                log.error("Health check failed for bizTag: {}", bizTag, e);
                alertService.sendAlert(AlertLevel.HIGH,
                    "Leaf Service Health Check Failed",
                    String.format("Health check failed for bizTag: %s, error: %s", bizTag, e.getMessage()));
            }
        }
    }
}

// 事件类
@Data
@AllArgsConstructor
public class IdGenerationEvent {
    private String bizTag;
    private long id;
    private boolean success;
    private String errorMessage;
    private long timestamp;
}
```

## 6. 常见问题解决

### 6.1 性能问题诊断

```java
@Component
@RequiredArgsConstructor
@Slf4j
public class LeafDiagnosticService {

    private final JdbcTemplate jdbcTemplate;
    private final LeafIdGeneratorService leafIdGenerator;

    /**
     * 诊断性能问题
     */
    public DiagnosticReport diagnosePerformance(String bizTag) {
        DiagnosticReport report = new DiagnosticReport();
        report.setBizTag(bizTag);
        report.setTimestamp(LocalDateTime.now());

        // 1. 检查数据库配置
        report.setDatabaseConfig(checkDatabaseConfig());

        // 2. 检查号段配置
        report.setSegmentConfig(checkSegmentConfig(bizTag));

        // 3. 检查当前性能
        report.setPerformanceMetrics(measureCurrentPerformance(bizTag));

        // 4. 生成建议
        report.setSuggestions(generateSuggestions(report));

        return report;
    }

    private DatabaseConfig checkDatabaseConfig() {
        try {
            String connectionPoolInfo = jdbcTemplate.queryForObject(
                "SELECT current_setting('shared_buffers') as shared_buffers, " +
                "current_setting('max_connections') as max_connections",
                (rs, rowNum) -> String.format("shared_buffers: %s, max_connections: %s",
                    rs.getString("shared_buffers"), rs.getString("max_connections"))
            );

            return DatabaseConfig.builder()
                .healthy(true)
                .connectionPoolInfo(connectionPoolInfo)
                .build();

        } catch (Exception e) {
            return DatabaseConfig.builder()
                .healthy(false)
                .error(e.getMessage())
                .build();
        }
    }

    private SegmentConfig checkSegmentConfig(String bizTag) {
        try {
            LeafAlloc alloc = jdbcTemplate.queryForObject(
                "SELECT * FROM leaf_alloc WHERE biz_tag = ?",
                new BeanPropertyRowMapper<>(LeafAlloc.class),
                bizTag
            );

            return SegmentConfig.builder()
                .exists(true)
                .currentMaxId(alloc.getMaxId())
                .step(alloc.getStep())
                .lastUpdateTime(alloc.getUpdateTime())
                .build();

        } catch (Exception e) {
            return SegmentConfig.builder()
                .exists(false)
                .error(e.getMessage())
                .build();
        }
    }

    private PerformanceMetrics measureCurrentPerformance(String bizTag) {
        long startTime = System.currentTimeMillis();

        try {
            // 快速性能测试
            for (int i = 0; i < 100; i++) {
                leafIdGenerator.nextId(bizTag);
            }

            long duration = System.currentTimeMillis() - startTime;
            double qps = 100 * 1000.0 / duration;

            return PerformanceMetrics.builder()
                .successful(true)
                .sampleQps(qps)
                .avgLatencyMs(duration / 100.0)
                .build();

        } catch (Exception e) {
            return PerformanceMetrics.builder()
                .successful(false)
                .error(e.getMessage())
                .build();
        }
    }

    private List<String> generateSuggestions(DiagnosticReport report) {
        List<String> suggestions = new ArrayList<>();

        SegmentConfig segmentConfig = report.getSegmentConfig();
        PerformanceMetrics metrics = report.getPerformanceMetrics();

        if (segmentConfig.exists && segmentConfig.step < 1000) {
            suggestions.add("Consider increasing step size to reduce database access frequency");
        }

        if (metrics.successful && metrics.sampleQps < 1000) {
            suggestions.add("QPS is below expected range, check database connection pool settings");
        }

        if (!report.getDatabaseConfig().healthy) {
            suggestions.add("Database connection issues detected, check network and configuration");
        }

        return suggestions;
    }

    @Data
    @Builder
    public static class DiagnosticReport {
        private String bizTag;
        private LocalDateTime timestamp;
        private DatabaseConfig databaseConfig;
        private SegmentConfig segmentConfig;
        private PerformanceMetrics performanceMetrics;
        private List<String> suggestions;
    }

    @Data
    @Builder
    public static class DatabaseConfig {
        private boolean healthy;
        private String connectionPoolInfo;
        private String error;
    }

    @Data
    @Builder
    public static class SegmentConfig {
        private boolean exists;
        private Long currentMaxId;
        private Integer step;
        private Date lastUpdateTime;
        private String error;
    }

    @Data
    @Builder
    public static class PerformanceMetrics {
        private boolean successful;
        private Double sampleQps;
        private Double avgLatencyMs;
        private String error;
    }
}
```

### 6.2 故障恢复脚本

```java
@Component
@RequiredArgsConstructor
@Slf4j
public class LeafRecoveryService {

    private final JdbcTemplate jdbcTemplate;
    private final LeafIdGeneratorService leafIdGenerator;

    /**
     * 自动故障恢复
     */
    public RecoveryResult autoRecover(String bizTag) {
        log.info("Starting auto recovery for bizTag: {}", bizTag);

        RecoveryResult result = new RecoveryResult();
        result.setBizTag(bizTag);
        result.setStartTime(LocalDateTime.now());

        try {
            // 1. 检查数据库连接
            if (!checkDatabaseConnection()) {
                result.addStep("Database connection check failed");
                return result;
            }
            result.addStep("Database connection OK");

            // 2. 检查业务配置
            if (!checkBizTagExists(bizTag)) {
                createDefaultBizTag(bizTag);
                result.addStep("Created missing biz tag: " + bizTag);
            } else {
                result.addStep("Biz tag exists: " + bizTag);
            }

            // 3. 测试ID生成
            long testId = leafIdGenerator.nextId(bizTag);
            result.addStep("ID generation test passed: " + testId);

            result.setSuccess(true);
            result.setEndTime(LocalDateTime.now());

        } catch (Exception e) {
            result.addStep("Recovery failed: " + e.getMessage());
            result.setSuccess(false);
            result.setError(e.getMessage());
        }

        log.info("Auto recovery completed for bizTag: {}, success: {}", bizTag, result.isSuccess());
        return result;
    }

    private boolean checkDatabaseConnection() {
        try {
            jdbcTemplate.queryForObject("SELECT 1", Integer.class);
            return true;
        } catch (Exception e) {
            log.error("Database connection check failed", e);
            return false;
        }
    }

    private boolean checkBizTagExists(String bizTag) {
        try {
            Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM leaf_alloc WHERE biz_tag = ?",
                Integer.class, bizTag
            );
            return count != null && count > 0;
        } catch (Exception e) {
            log.error("Error checking biz tag existence", e);
            return false;
        }
    }

    private void createDefaultBizTag(String bizTag) {
        jdbcTemplate.update(
            "INSERT INTO leaf_alloc (biz_tag, max_id, step, description) VALUES (?, 1, 1000, 'Auto-created')",
            bizTag
        );
        log.info("Created default configuration for bizTag: {}", bizTag);
    }

    @Data
    public static class RecoveryResult {
        private String bizTag;
        private boolean success;
        private LocalDateTime startTime;
        private LocalDateTime endTime;
        private List<String> steps = new ArrayList<>();
        private String error;

        public void addStep(String step) {
            steps.add(LocalDateTime.now() + ": " + step);
        }
    }
}
```

通过以上实际使用案例和测试指南，你可以全面了解如何在生产环境中使用Leaf分布式ID系统。记住根据实际业务需求调整配置参数，并建立完善的监控和告警机制。