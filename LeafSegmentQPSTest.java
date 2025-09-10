import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAdder;
import java.time.Duration;
import java.time.Instant;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Leaf-Segment算法QPS性能测试
 * 
 * 这是一个独立的性能测试程序，专门测试Leaf-Segment ID生成算法的QPS性能。
 * 模拟了美团Leaf-Segment的核心特性：
 * - 双缓冲机制
 * - 异步预加载
 * - 高并发ID生成
 * 
 * 测试目标：
 * - QPS > 50,000 (美团官方数据)
 * - 零重复ID
 * - 低延迟响应
 * 
 * @author Mini-UPS Team
 */
public class LeafSegmentQPSTest {
    
    private static final int WARMUP_SECONDS = 3;
    private static final int TEST_DURATION_SECONDS = 10;
    private static final int[] THREAD_COUNTS = {10, 20, 50, 100, 200, 500, 1000};
    
    public static void main(String[] args) throws InterruptedException {
        System.out.println("=== Leaf-Segment算法QPS性能测试 ===\n");
        
        LeafSegmentQPSTest tester = new LeafSegmentQPSTest();
        
        // 运行不同线程数的测试
        for (int threadCount : THREAD_COUNTS) {
            System.out.println("🧵 测试线程数: " + threadCount);
            tester.runQPSTest(threadCount);
            System.out.println();
            
            // 让系统稍作休息
            Thread.sleep(1000);
        }
        
        System.out.println("=== 测试完成 ===");
    }
    
    /**
     * 运行指定线程数的QPS测试
     */
    private void runQPSTest(int threadCount) throws InterruptedException {
        SimpleLeafSegmentGenerator generator = new SimpleLeafSegmentGenerator();
        
        // 预热阶段
        System.out.println("  🔥 预热中...");
        runTest(generator, threadCount, WARMUP_SECONDS, false);
        
        // 正式测试
        System.out.println("  ⚡ 正式测试中...");
        TestResult result = runTest(generator, threadCount, TEST_DURATION_SECONDS, true);
        
        // 输出结果
        printResult(result, threadCount);
    }
    
    /**
     * 执行测试
     */
    private TestResult runTest(SimpleLeafSegmentGenerator generator, int threadCount, 
                              int duration, boolean collectStats) throws InterruptedException {
        
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch stopLatch = new CountDownLatch(threadCount);
        
        LongAdder totalOperations = new LongAdder();
        LongAdder successOperations = new LongAdder();
        Set<Long> generatedIds = collectStats ? ConcurrentHashMap.newKeySet() : null;
        
        Instant testStart = Instant.now();
        
        // 启动工作线程
        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    startLatch.await();
                    
                    while (!Thread.currentThread().isInterrupted()) {
                        try {
                            long id = generator.generateId();
                            totalOperations.increment();
                            
                            if (id > 0) {
                                successOperations.increment();
                                if (collectStats && generatedIds != null) {
                                    generatedIds.add(id);
                                }
                            }
                        } catch (Exception e) {
                            // 忽略异常，继续测试
                        }
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    stopLatch.countDown();
                }
            });
        }
        
        // 开始测试
        startLatch.countDown();
        Thread.sleep(duration * 1000);
        
        // 停止测试
        executor.shutdownNow();
        stopLatch.await(5, TimeUnit.SECONDS);
        
        Instant testEnd = Instant.now();
        Duration actualDuration = Duration.between(testStart, testEnd);
        
        return new TestResult(
            totalOperations.sum(),
            successOperations.sum(),
            actualDuration.toMillis(),
            collectStats ? generatedIds.size() : 0
        );
    }
    
    /**
     * 打印测试结果
     */
    private void printResult(TestResult result, int threadCount) {
        double durationSeconds = result.durationMs / 1000.0;
        double qps = result.successOperations / durationSeconds;
        double successRate = (double) result.successOperations / result.totalOperations * 100;
        
        System.out.printf("  📊 总操作数: %,d\n", result.totalOperations);
        System.out.printf("  ✅ 成功操作数: %,d\n", result.successOperations);
        System.out.printf("  🎯 成功率: %.2f%%\n", successRate);
        System.out.printf("  ⏱️  执行时间: %.2f秒\n", durationSeconds);
        System.out.printf("  🚀 QPS: %,.0f\n", qps);
        
        if (result.uniqueIds > 0) {
            System.out.printf("  🔢 唯一ID数: %,d\n", result.uniqueIds);
            System.out.printf("  ✨ 重复率: %.6f%%\n", 
                (result.successOperations - result.uniqueIds) * 100.0 / result.successOperations);
        }
        
        // 性能评级
        String grade = getPerformanceGrade(qps);
        System.out.printf("  🏆 性能评级: %s\n", grade);
    }
    
    /**
     * 根据QPS给出性能评级
     */
    private String getPerformanceGrade(double qps) {
        if (qps >= 100_000) return "🏆 卓越 (≥100K QPS)";
        if (qps >= 50_000) return "🥇 优秀 (≥50K QPS)";
        if (qps >= 25_000) return "🥈 良好 (≥25K QPS)";
        if (qps >= 10_000) return "🥉 一般 (≥10K QPS)";
        if (qps >= 1_000) return "⚠️  需要优化 (≥1K QPS)";
        return "❌ 性能不足 (<1K QPS)";
    }
    
    /**
     * 测试结果
     */
    private static class TestResult {
        final long totalOperations;
        final long successOperations;
        final long durationMs;
        final int uniqueIds;
        
        TestResult(long totalOperations, long successOperations, long durationMs, int uniqueIds) {
            this.totalOperations = totalOperations;
            this.successOperations = successOperations;
            this.durationMs = durationMs;
            this.uniqueIds = uniqueIds;
        }
    }
    
    /**
     * 简化的Leaf-Segment生成器（用于测试）
     */
    private static class SimpleLeafSegmentGenerator {
        private final Object lock = new Object();
        private volatile Segment currentSegment;
        private volatile Segment nextSegment;
        private final AtomicLong segmentCounter = new AtomicLong(0);
        private final ExecutorService preloadExecutor = Executors.newSingleThreadExecutor();
        
        public SimpleLeafSegmentGenerator() {
            // 初始化第一个段
            currentSegment = new Segment(1, 2000);
        }
        
        public long generateId() {
            Segment segment = currentSegment;
            if (segment == null) {
                return -1;
            }
            
            long id = segment.nextId();
            
            // 检查是否需要预加载
            if (id != -1 && segment.shouldPreload() && nextSegment == null) {
                preloadNextSegment();
            }
            
            // 如果当前段用尽，切换到下一个段
            if (id == -1 && nextSegment != null) {
                switchToNextSegment();
                id = currentSegment.nextId();
            }
            
            return id;
        }
        
        private void preloadNextSegment() {
            preloadExecutor.submit(() -> {
                if (nextSegment == null) {
                    synchronized (lock) {
                        if (nextSegment == null) {
                            long segmentNum = segmentCounter.incrementAndGet();
                            long start = segmentNum * 2000 + 1;
                            nextSegment = new Segment(start, start + 1999);
                        }
                    }
                }
            });
        }
        
        private void switchToNextSegment() {
            synchronized (lock) {
                if (nextSegment != null) {
                    currentSegment = nextSegment;
                    nextSegment = null;
                }
            }
        }
        
        /**
         * 段（Segment）
         */
        private static class Segment {
            private final long start;
            private final long max;
            private final AtomicLong current;
            private final long preloadThreshold;
            
            Segment(long start, long max) {
                this.start = start;
                this.max = max;
                this.current = new AtomicLong(start);
                this.preloadThreshold = start + (max - start) * 3 / 4; // 75%时预加载
            }
            
            long nextId() {
                long id = current.getAndIncrement();
                return id <= max ? id : -1;
            }
            
            boolean shouldPreload() {
                return current.get() >= preloadThreshold;
            }
        }
    }
}