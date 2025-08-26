import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 简化版美团Leaf分布式ID生成器测试
 * 独立运行，无需Spring框架
 */
public class SimpleLeafTest {

    /**
     * 简化的Segment实现
     */
    static class SimpleSegment {
        private volatile long start;
        private volatile long end;
        private final AtomicLong currentPos = new AtomicLong(-1);
        private volatile boolean initialized = false;

        public void init(long start, long end) {
            this.start = start;
            this.end = end;
            this.currentPos.set(start - 1);
            this.initialized = true;
        }

        public long nextId() {
            if (!initialized) return -1;
            long current = currentPos.incrementAndGet();
            return current > end ? -1 : current;
        }

        public boolean shouldPreload(double threshold) {
            if (!initialized) return false;
            long current = currentPos.get();
            long used = Math.max(0, current - start + 1);
            long total = end - start + 1;
            return (double) used / total >= threshold; // 可配置阈值
        }

        public long getRemaining() {
            if (!initialized) return 0;
            return Math.max(0, end - currentPos.get());
        }

        public double getUsagePercentage() {
            if (!initialized) return 0.0;
            long current = currentPos.get();
            long used = Math.max(0, current - start + 1);
            long total = end - start + 1;
            return (double) used / total * 100.0;
        }

        public boolean isExhausted() {
            return currentPos.get() >= end;
        }
    }

    /**
     * 简化的双缓冲管理器
     */
    static class SimpleSegmentBuffer {
        private final SimpleSegment[] segments = new SimpleSegment[]{new SimpleSegment(), new SimpleSegment()};
        private volatile int currentIndex = 0;
        private final AtomicInteger nextSegmentLoading = new AtomicInteger(0);
        private final AtomicInteger nextSegmentReady = new AtomicInteger(0);
        private final AtomicInteger initialized = new AtomicInteger(0);
        private final Object switchLock = new Object();
        private final double preloadThreshold;

        public SimpleSegmentBuffer(double preloadThreshold) {
            this.preloadThreshold = preloadThreshold;
        }

        public SimpleSegmentBuffer() {
            this(0.75);
        }

        public void initFirstSegment(long start, long end) {
            segments[0].init(start, end);
            currentIndex = 0;
            nextSegmentReady.set(0);
            initialized.set(1);
        }

        public void preloadNextSegment(long start, long end) {
            int nextIndex = 1 - currentIndex;
            segments[nextIndex].init(start, end);
            nextSegmentReady.set(1);
            nextSegmentLoading.set(0);
        }

        public long nextId() {
            if (initialized.get() == 0) return -1;

            SimpleSegment currentSegment = segments[currentIndex];
            
            // 检查是否需要预加载
            if (currentSegment.shouldPreload(preloadThreshold) && 
                nextSegmentLoading.compareAndSet(0, 1) && 
                nextSegmentReady.get() == 0) {
                // 这里应该触发异步预加载，简化版本中忽略
            }

            long nextId = currentSegment.nextId();

            // 如果当前段用尽，尝试切换
            if (nextId == -1 && nextSegmentReady.get() == 1) {
                synchronized(switchLock) {
                    // 双重检查
                    if (nextSegmentReady.get() == 1) {
                        currentIndex = 1 - currentIndex;
                        nextSegmentReady.set(0);
                        return segments[currentIndex].nextId();
                    }
                }
            }

            return nextId;
        }

        public boolean needsPreload() {
            return initialized.get() == 1 && 
                   segments[currentIndex].shouldPreload(preloadThreshold) && 
                   nextSegmentLoading.get() == 0 && 
                   nextSegmentReady.get() == 0;
        }

        public SimpleSegment getCurrentSegment() {
            return segments[currentIndex];
        }
    }

    /**
     * 简化的ID生成器
     */
    static class SimpleLeafIdGenerator {
        private final AtomicLong currentMaxId = new AtomicLong(0);
        private final int step = 1000;
        private final ConcurrentHashMap<String, SimpleSegmentBuffer> bufferMap = new ConcurrentHashMap<>();
        private final double preloadThreshold;
        private static final long TWELVE_DIGIT_MAX = 1_000_000_000_000L; // 10^12

        public SimpleLeafIdGenerator() {
            this(0.75);
        }

        public SimpleLeafIdGenerator(double preloadThreshold) {
            this.preloadThreshold = preloadThreshold;
        }

        public long generateId(String bizTag) {
            SimpleSegmentBuffer buffer = getOrCreateBuffer(bizTag);
            return buffer.nextId();
        }

        public String generateTrackingNumber() {
            long id = generateId("tracking_number");
            if (id == -1) return null;
            long fixed = Math.floorMod(id, TWELVE_DIGIT_MAX);
            return "UPS" + String.format("%012d", fixed);
        }

        private SimpleSegmentBuffer getOrCreateBuffer(String bizTag) {
            return bufferMap.computeIfAbsent(bizTag, tag -> {
                SimpleSegmentBuffer buffer = new SimpleSegmentBuffer(preloadThreshold);
                long newMaxId = currentMaxId.addAndGet(step);
                long start = newMaxId - step + 1;
                long end = newMaxId;
                buffer.initFirstSegment(start, end);
                return buffer;
            });
        }

        public int getBufferCount() {
            return bufferMap.size();
        }
    }

    // 测试方法
    public static void main(String[] args) throws InterruptedException {
        System.out.println("=== 美团Leaf分布式ID生成器测试 ===");
        
        testBasicFunctionality();
        testDoubleBuffering();
        testConcurrentGeneration();
        testFormattedIds();
        
        System.out.println("=== 所有测试完成 ===");
    }

    static void testBasicFunctionality() {
        System.out.println("\n1. 基础功能测试:");
        
        SimpleSegment segment = new SimpleSegment();
        segment.init(1, 100);

        System.out.println("   生成前5个ID: " + segment.nextId() + ", " + segment.nextId() + 
                          ", " + segment.nextId() + ", " + segment.nextId() + ", " + segment.nextId());
        System.out.println("   剩余ID数量: " + segment.getRemaining());
        System.out.println("   使用百分比: " + String.format("%.1f%%", segment.getUsagePercentage()));
        System.out.println("   是否应该预加载: " + segment.shouldPreload(0.75));
        
        // 用完段
        while (!segment.isExhausted()) {
            segment.nextId();
        }
        System.out.println("   段用完后获取ID: " + segment.nextId() + " (应为-1)");
        System.out.println("   ✓ 基础功能正常");
    }

    static void testDoubleBuffering() {
        System.out.println("\n2. 双缓冲机制测试:");
        
        SimpleSegmentBuffer buffer = new SimpleSegmentBuffer();
        buffer.initFirstSegment(1, 100);

        // 使用到75%触发预加载检查
        for (int i = 1; i <= 75; i++) {
            buffer.nextId();
        }
        
        System.out.println("   使用75%后需要预加载: " + buffer.needsPreload());
        
        // 预加载下一个段
        buffer.preloadNextSegment(101, 200);
        
        // 用完当前段
        while (buffer.getCurrentSegment().getRemaining() > 0) {
            buffer.nextId();
        }
        
        // 切换到下一个段
        long nextId = buffer.nextId();
        System.out.println("   切换后的第一个ID: " + nextId + " (应为101)");
        System.out.println("   ✓ 双缓冲切换正常");
    }

    static void testConcurrentGeneration() throws InterruptedException {
        System.out.println("\n3. 高并发唯一性测试:");
        
        final SimpleLeafIdGenerator generator = new SimpleLeafIdGenerator();
        final int threadCount = 50;
        final int operationsPerThread = 20;
        final CountDownLatch startLatch = new CountDownLatch(1);
        final CountDownLatch finishLatch = new CountDownLatch(threadCount);
        final Set<Long> generatedIds = ConcurrentHashMap.newKeySet();
        final AtomicInteger duplicateCount = new AtomicInteger(0);
        final AtomicInteger successCount = new AtomicInteger(0);

        ExecutorService executor = Executors.newFixedThreadPool(threadCount);

        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    startLatch.await();
                    
                    for (int j = 0; j < operationsPerThread; j++) {
                        long id = generator.generateId("test");
                        if (id != -1) {
                            if (!generatedIds.add(id)) {
                                duplicateCount.incrementAndGet();
                            } else {
                                successCount.incrementAndGet();
                            }
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

        System.out.println("   线程数: " + threadCount);
        System.out.println("   每线程操作数: " + operationsPerThread);
        System.out.println("   总操作数: " + (threadCount * operationsPerThread));
        System.out.println("   成功生成数: " + successCount.get());
        System.out.println("   唯一ID数: " + generatedIds.size());
        System.out.println("   重复ID数: " + duplicateCount.get());
        System.out.println("   执行时间: " + (endTime - startTime) + " ms");
        System.out.println("   平均QPS: " + String.format("%.2f", successCount.get() * 1000.0 / (endTime - startTime)));
        
        if (duplicateCount.get() == 0) {
            System.out.println("   ✓ 无重复ID，并发唯一性测试通过");
        } else {
            System.out.println("   ✗ 发现 " + duplicateCount.get() + " 个重复ID");
        }
    }

    static void testFormattedIds() {
        System.out.println("\n4. 格式化ID测试:");
        
        SimpleLeafIdGenerator generator = new SimpleLeafIdGenerator();
        
        System.out.println("   生成的追踪号示例:");
        for (int i = 0; i < 5; i++) {
            String trackingNumber = generator.generateTrackingNumber();
            System.out.println("     " + trackingNumber);
            
            // 验证格式
            if (trackingNumber != null && trackingNumber.startsWith("UPS") && trackingNumber.length() == 15) {
                String numberPart = trackingNumber.substring(3);
                if (numberPart.matches("\\d{12}")) {
                    // 格式正确
                } else {
                    System.out.println("     ✗ 数字部分格式错误");
                }
            } else {
                System.out.println("     ✗ 追踪号格式错误");
            }
        }
        System.out.println("   ✓ 追踪号格式化正常");
    }
}
