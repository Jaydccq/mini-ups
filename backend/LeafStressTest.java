import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

/**
 * 美团Leaf分布式ID生成器压力测试
 * 模拟真实生产环境的高并发场景
 */
public class LeafStressTest {

    /**
     * 增强版SimpleSegment with metrics
     */
    static class EnhancedSegment {
        private volatile long start;
        private volatile long end;
        private final AtomicLong currentPos = new AtomicLong(-1);
        private volatile boolean initialized = false;
        private final AtomicLong accessCount = new AtomicLong(0);

        public void init(long start, long end) {
            this.start = start;
            this.end = end;
            this.currentPos.set(start - 1);
            this.initialized = true;
        }

        public long nextId() {
            if (!initialized) return -1;
            accessCount.incrementAndGet();
            long current = currentPos.incrementAndGet();
            return current > end ? -1 : current;
        }

        public boolean shouldPreload(double threshold) {
            if (!initialized) return false;
            long current = currentPos.get();
            long used = Math.max(0, current - start + 1);
            long total = end - start + 1;
            return (double) used / total >= threshold;
        }

        public long getAccessCount() { return accessCount.get(); }
        public long getRemaining() { return initialized ? Math.max(0, end - currentPos.get()) : 0; }
        public boolean isExhausted() { return currentPos.get() >= end; }
        public long getStart() { return start; }
        public long getEnd() { return end; }
    }

    /**
     * 增强版SegmentBuffer with metrics and advanced preloading
     */
    static class EnhancedSegmentBuffer {
        private final EnhancedSegment[] segments = new EnhancedSegment[]{new EnhancedSegment(), new EnhancedSegment()};
        private volatile int currentIndex = 0;
        private final AtomicBoolean nextSegmentLoading = new AtomicBoolean(false);
        private final AtomicBoolean nextSegmentReady = new AtomicBoolean(false);
        private final AtomicBoolean initialized = new AtomicBoolean(false);
        private final AtomicLong switchCount = new AtomicLong(0);
        private final String bizTag;
        private final Object switchLock = new Object();
        private final double preloadThreshold;

        public EnhancedSegmentBuffer(String bizTag, double preloadThreshold) {
            this.bizTag = bizTag;
            this.preloadThreshold = preloadThreshold;
        }

        public void initFirstSegment(long start, long end) {
            segments[0].init(start, end);
            currentIndex = 0;
            nextSegmentReady.set(false);
            initialized.set(true);
        }

        public void preloadNextSegment(long start, long end) {
            int nextIndex = 1 - currentIndex;
            segments[nextIndex].init(start, end);
            nextSegmentReady.set(true);
            nextSegmentLoading.set(false);
        }

        public long nextId() {
            if (!initialized.get()) return -1;

            EnhancedSegment currentSegment = segments[currentIndex];
            long nextId = currentSegment.nextId();

            // 如果当前段用尽，尝试切换
            if (nextId == -1 && nextSegmentReady.get()) {
                synchronized(switchLock) {
                    if (nextSegmentReady.get()) {
                        currentIndex = 1 - currentIndex;
                        nextSegmentReady.set(false);
                        switchCount.incrementAndGet();
                        return segments[currentIndex].nextId();
                    }
                }
            }

            return nextId;
        }

        public boolean needsPreload() {
            return initialized.get() && 
                   segments[currentIndex].shouldPreload(preloadThreshold) && 
                   !nextSegmentLoading.get() && 
                   !nextSegmentReady.get();
        }

        public EnhancedSegment getCurrentSegment() { return segments[currentIndex]; }
        public long getSwitchCount() { return switchCount.get(); }
        public String getBizTag() { return bizTag; }
        
        public String getDetailedStatus() {
            EnhancedSegment current = getCurrentSegment();
            return String.format("Buffer[%s]: current=[%d-%d], pos=%d, remaining=%d, switches=%d",
                bizTag, current.getStart(), current.getEnd(), 
                current.currentPos.get(), current.getRemaining(), switchCount.get());
        }
    }

    /**
     * 增强版ID生成器 with metrics and monitoring
     */
    static class EnhancedLeafIdGenerator {
        private final AtomicLong currentMaxId = new AtomicLong(0);
        private final int step = 2000; // 增大步长提高性能
        private final ConcurrentHashMap<String, EnhancedSegmentBuffer> bufferMap = new ConcurrentHashMap<>();
        private final AtomicLong totalGenerated = new AtomicLong(0);
        private final AtomicLong segmentLoads = new AtomicLong(0);
        private final double preloadThreshold;
        private static final long TWELVE_DIGIT_MAX = 1_000_000_000_000L; // 10^12
        private final Object preloadFlagLock = new Object();

        public EnhancedLeafIdGenerator() {
            this(0.75);
        }

        public EnhancedLeafIdGenerator(double preloadThreshold) {
            this.preloadThreshold = preloadThreshold;
        }
        
        public long generateId(String bizTag) {
            EnhancedSegmentBuffer buffer = getOrCreateBuffer(bizTag);
            
            // 模拟预加载逻辑
            if (buffer.needsPreload()) {
                // 异步预加载下一个段
                CompletableFuture.runAsync(() -> preloadSegment(buffer));
            }
            
            long id = buffer.nextId();
            if (id != -1) {
                totalGenerated.incrementAndGet();
            }
            return id;
        }

        public String generateTrackingNumber() {
            long id = generateId("tracking_number");
            if (id == -1) return null;
            long fixed = Math.floorMod(id, TWELVE_DIGIT_MAX);
            return "UPS" + String.format("%012d", fixed);
        }

        private void preloadSegment(EnhancedSegmentBuffer buffer) {
            try {
                // 模拟数据库延迟
                Thread.sleep(1); // 1ms数据库延迟
                
                long newMaxId = currentMaxId.addAndGet(step);
                long start = newMaxId - step + 1;
                long end = newMaxId;
                
                buffer.preloadNextSegment(start, end);
                segmentLoads.incrementAndGet();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        private EnhancedSegmentBuffer getOrCreateBuffer(String bizTag) {
            return bufferMap.computeIfAbsent(bizTag, tag -> {
                EnhancedSegmentBuffer buffer = new EnhancedSegmentBuffer(tag, preloadThreshold);
                long newMaxId = currentMaxId.addAndGet(step);
                long start = newMaxId - step + 1;
                long end = newMaxId;
                buffer.initFirstSegment(start, end);
                segmentLoads.incrementAndGet();
                return buffer;
            });
        }

        public Map<String, Object> getMetrics() {
            Map<String, Object> metrics = new HashMap<>();
            metrics.put("totalGenerated", totalGenerated.get());
            metrics.put("segmentLoads", segmentLoads.get());
            metrics.put("bufferCount", bufferMap.size());
            
            long totalSwitches = bufferMap.values().stream()
                .mapToLong(EnhancedSegmentBuffer::getSwitchCount).sum();
            metrics.put("totalSwitches", totalSwitches);
            
            return metrics;
        }
        
        public void printDetailedStatus() {
            System.out.println("=== 详细缓冲区状态 ===");
            bufferMap.values().forEach(buffer -> 
                System.out.println("  " + buffer.getDetailedStatus()));
        }
    }

    public static void main(String[] args) throws InterruptedException {
        System.out.println("=== 美团Leaf分布式ID生成器压力测试 ===\n");
        
        runHighConcurrencyTest();
        runMultiBusinessTest();
        runEnduranceTest();
        runPerformanceBenchmark();
        
        System.out.println("\n=== 所有压力测试完成 ===");
    }

    /**
     * 高并发唯一性测试 - 模拟生产环境
     */
    static void runHighConcurrencyTest() throws InterruptedException {
        System.out.println("1. 高并发唯一性测试 (生产环境模拟):");
        
        final EnhancedLeafIdGenerator generator = new EnhancedLeafIdGenerator();
        final int threadCount = 100; // 100个并发线程
        final int operationsPerThread = 500; // 每线程500次操作
        final CountDownLatch startLatch = new CountDownLatch(1);
        final CountDownLatch finishLatch = new CountDownLatch(threadCount);
        final Set<String> generatedIds = ConcurrentHashMap.newKeySet();
        final AtomicInteger duplicateCount = new AtomicInteger(0);
        final AtomicInteger successCount = new AtomicInteger(0);
        final AtomicInteger errorCount = new AtomicInteger(0);

        ExecutorService executor = Executors.newFixedThreadPool(threadCount);

        for (int i = 0; i < threadCount; i++) {
            final int threadId = i;
            executor.submit(() -> {
                try {
                    startLatch.await();
                    
                    for (int j = 0; j < operationsPerThread; j++) {
                        try {
                            String trackingId = generator.generateTrackingNumber();
                            if (trackingId != null) {
                                if (!generatedIds.add(trackingId)) {
                                    duplicateCount.incrementAndGet();
                                    System.err.printf("线程%d发现重复ID: %s%n", threadId, trackingId);
                                } else {
                                    successCount.incrementAndGet();
                                }
                            } else {
                                errorCount.incrementAndGet();
                            }
                        } catch (Exception e) {
                            errorCount.incrementAndGet();
                            System.err.printf("线程%d生成异常: %s%n", threadId, e.getMessage());
                        }
                        
                        // 模拟实际业务间隔
                        if (j % 100 == 0) {
                            Thread.sleep(1);
                        }
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    finishLatch.countDown();
                }
            });
        }

        long startTime = System.currentTimeMillis();
        startLatch.countDown();
        finishLatch.await();
        long endTime = System.currentTimeMillis();

        executor.shutdown();
        executor.awaitTermination(10, TimeUnit.SECONDS);

        // 结果统计
        System.out.printf("   线程数: %d%n", threadCount);
        System.out.printf("   每线程操作数: %d%n", operationsPerThread);
        System.out.printf("   总操作数: %d%n", threadCount * operationsPerThread);
        System.out.printf("   成功生成数: %d%n", successCount.get());
        System.out.printf("   唯一ID数: %d%n", generatedIds.size());
        System.out.printf("   重复ID数: %d%n", duplicateCount.get());
        System.out.printf("   错误数: %d%n", errorCount.get());
        System.out.printf("   执行时间: %d ms%n", endTime - startTime);
        System.out.printf("   平均QPS: %.2f%n", successCount.get() * 1000.0 / (endTime - startTime));
        
        // 打印生成器指标
        Map<String, Object> metrics = generator.getMetrics();
        System.out.printf("   段加载次数: %s%n", metrics.get("segmentLoads"));
        System.out.printf("   段切换次数: %s%n", metrics.get("totalSwitches"));
        
        if (duplicateCount.get() == 0 && errorCount.get() == 0) {
            System.out.println("   ✓ 高并发测试通过 - 无重复，无错误");
        } else {
            System.out.printf("   ✗ 高并发测试异常 - 重复:%d, 错误:%d%n", 
                duplicateCount.get(), errorCount.get());
        }
        
        System.out.println();
    }

    /**
     * 多业务类型并发测试
     */
    static void runMultiBusinessTest() throws InterruptedException {
        System.out.println("2. 多业务类型并发测试:");
        
        final EnhancedLeafIdGenerator generator = new EnhancedLeafIdGenerator();
        final String[] bizTags = {"tracking_number", "order_id", "user_id", "payment_id", "shipment_id"};
        final int threadsPerBiz = 20;
        final int operationsPerThread = 100;
        final CountDownLatch startLatch = new CountDownLatch(1);
        final CountDownLatch finishLatch = new CountDownLatch(bizTags.length * threadsPerBiz);
        final ConcurrentHashMap<String, Set<Long>> bizResults = new ConcurrentHashMap<>();
        final AtomicInteger totalSuccess = new AtomicInteger(0);

        ExecutorService executor = Executors.newFixedThreadPool(bizTags.length * threadsPerBiz);

        // 初始化结果容器
        for (String bizTag : bizTags) {
            bizResults.put(bizTag, ConcurrentHashMap.newKeySet());
        }

        for (String bizTag : bizTags) {
            for (int i = 0; i < threadsPerBiz; i++) {
                executor.submit(() -> {
                    try {
                        startLatch.await();
                        
                        for (int j = 0; j < operationsPerThread; j++) {
                            long id = generator.generateId(bizTag);
                            if (id != -1) {
                                bizResults.get(bizTag).add(id);
                                totalSuccess.incrementAndGet();
                            }
                        }
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    } finally {
                        finishLatch.countDown();
                    }
                });
            }
        }

        long startTime = System.currentTimeMillis();
        startLatch.countDown();
        finishLatch.await();
        long endTime = System.currentTimeMillis();

        executor.shutdown();

        // 统计结果
        System.out.printf("   业务类型数: %d%n", bizTags.length);
        System.out.printf("   每业务线程数: %d%n", threadsPerBiz);
        System.out.printf("   总成功生成: %d%n", totalSuccess.get());
        System.out.printf("   执行时间: %d ms%n", endTime - startTime);
        
        boolean allUnique = true;
        for (String bizTag : bizTags) {
            Set<Long> ids = bizResults.get(bizTag);
            int expectedCount = threadsPerBiz * operationsPerThread;
            System.out.printf("   %s: 生成=%d, 唯一=%d%n", bizTag, expectedCount, ids.size());
            if (ids.size() != expectedCount) {
                allUnique = false;
            }
        }
        
        if (allUnique) {
            System.out.println("   ✓ 多业务并发测试通过");
        } else {
            System.out.println("   ✗ 多业务测试存在重复ID");
        }
        
        generator.printDetailedStatus();
        System.out.println();
    }

    /**
     * 耐久性测试 - 长时间运行
     */
    static void runEnduranceTest() throws InterruptedException {
        System.out.println("3. 耐久性测试 (长时间运行):");
        
        final EnhancedLeafIdGenerator generator = new EnhancedLeafIdGenerator();
        final int threadCount = 10;
        final int testDurationSeconds = 5; // 5秒持续测试
        final AtomicBoolean running = new AtomicBoolean(true);
        final AtomicLong totalGenerated = new AtomicLong(0);
        final Set<String> allIds = ConcurrentHashMap.newKeySet();
        
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        
        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                while (running.get()) {
                    try {
                        String id = generator.generateTrackingNumber();
                        if (id != null) {
                            allIds.add(id);
                            totalGenerated.incrementAndGet();
                        }
                        Thread.sleep(1); // 小间隔避免过度占用CPU
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            });
        }
        
        long startTime = System.currentTimeMillis();
        Thread.sleep(testDurationSeconds * 1000);
        running.set(false);
        long endTime = System.currentTimeMillis();
        
        executor.shutdown();
        executor.awaitTermination(2, TimeUnit.SECONDS);
        
        double actualDuration = (endTime - startTime) / 1000.0;
        System.out.printf("   运行时间: %.1f 秒%n", actualDuration);
        System.out.printf("   总生成数: %d%n", totalGenerated.get());
        System.out.printf("   唯一ID数: %d%n", allIds.size());
        System.out.printf("   平均QPS: %.2f%n", totalGenerated.get() / actualDuration);
        
        Map<String, Object> metrics = generator.getMetrics();
        System.out.printf("   段加载: %s 次%n", metrics.get("segmentLoads"));
        System.out.printf("   段切换: %s 次%n", metrics.get("totalSwitches"));
        
        if (allIds.size() == totalGenerated.get()) {
            System.out.println("   ✓ 耐久性测试通过 - 无重复ID");
        } else {
            System.out.printf("   ✗ 发现重复ID: %d%n", totalGenerated.get() - allIds.size());
        }
        
        System.out.println();
    }

    /**
     * 性能基准测试
     */
    static void runPerformanceBenchmark() throws InterruptedException {
        System.out.println("4. 性能基准测试:");
        
        final EnhancedLeafIdGenerator generator = new EnhancedLeafIdGenerator();
        final int[] threadCounts = {1, 10, 50, 100, 200};
        final int operationsPerThread = 1000;
        
        System.out.println("   线程数 | 总操作数 | 执行时间(ms) | QPS     | 平均延迟(μs)");
        System.out.println("   -------|----------|-------------|---------|-------------");
        
        for (int threadCount : threadCounts) {
            final CountDownLatch startLatch = new CountDownLatch(1);
            final CountDownLatch finishLatch = new CountDownLatch(threadCount);
            final AtomicLong successCount = new AtomicLong(0);
            final AtomicLong totalLatency = new AtomicLong(0);
            
            ExecutorService executor = Executors.newFixedThreadPool(threadCount);
            
            for (int i = 0; i < threadCount; i++) {
                executor.submit(() -> {
                    try {
                        startLatch.await();
                        
                        for (int j = 0; j < operationsPerThread; j++) {
                            long opStart = System.nanoTime();
                            String id = generator.generateTrackingNumber();
                            long opEnd = System.nanoTime();
                            
                            if (id != null) {
                                successCount.incrementAndGet();
                                totalLatency.addAndGet(opEnd - opStart);
                            }
                        }
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    } finally {
                        finishLatch.countDown();
                    }
                });
            }
            
            long startTime = System.currentTimeMillis();
            startLatch.countDown();
            finishLatch.await();
            long endTime = System.currentTimeMillis();
            
            executor.shutdown();
            executor.awaitTermination(5, TimeUnit.SECONDS);
            
            long duration = endTime - startTime;
            double qps = successCount.get() * 1000.0 / duration;
            double avgLatency = totalLatency.get() / (successCount.get() * 1000.0); // 微秒
            
            System.out.printf("   %6d | %8d | %11d | %7.2f | %11.2f%n",
                threadCount, threadCount * operationsPerThread, duration, qps, avgLatency);
        }
        
        System.out.println("   ✓ 性能基准测试完成");
    }
}
