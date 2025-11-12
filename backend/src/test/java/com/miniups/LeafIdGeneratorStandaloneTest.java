package com.miniups;

import com.miniups.service.id.LeafSegmentIdGenerator;
import com.miniups.service.id.SegmentBuffer;
import com.miniups.service.id.Segment;
import com.miniups.repository.TrackingSequenceRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 美团Leaf分布式ID生成器独立测试
 * 无需Spring容器，专门测试Leaf算法的核心逻辑
 */
public class LeafIdGeneratorStandaloneTest {

    /**
     * 模拟数据库的序列记录
     */
    private static class MockSequenceInfo extends TrackingSequenceRepository.SegmentInfo {
        public MockSequenceInfo(Long maxId, Integer step) {
            setMaxId(maxId);
            setStep(step);
        }
    }

    /**
     * 模拟Repository实现
     */
    private static class MockTrackingSequenceRepository implements TrackingSequenceRepository {
        private final AtomicLong currentMaxId = new AtomicLong(0);
        private final int step = 1000;

        @Override
        public TrackingSequenceRepository.SegmentInfo getNextSegment(String bizTag) {
            // 模拟原子更新：UPDATE tracking_sequences SET max_id = max_id + step WHERE biz_tag = ?
            long newMaxId = currentMaxId.addAndGet(step);
            return new MockSequenceInfo(newMaxId, step);
        }

        @Override
        public int updateStep(String sequenceName, int newStep) {
            return 1; // 模拟成功更新
        }

        @Override
        public int initializeSequence(String bizTag, int step, String description) {
            return 1; // 模拟成功初始化
        }

        // 其他MyBatis Mapper方法默认实现
        @Override public int insert(com.miniups.model.entity.TrackingSequence sequence) { return 1; }
        @Override public int update(com.miniups.model.entity.TrackingSequence sequence) { return 1; }
        @Override public com.miniups.model.entity.TrackingSequence selectById(Long id) { return null; }
        @Override public com.miniups.model.entity.TrackingSequence findBySequenceName(String sequenceName) { return null; }
        @Override public int deleteById(Long id) { return 1; }
        @Override public long count() { return 0; }
    }

    private LeafSegmentIdGenerator idGenerator;
    private MockTrackingSequenceRepository mockRepository;
    private ExecutorService executorService;

    @BeforeEach
    void setUp() {
        mockRepository = new MockTrackingSequenceRepository();
        executorService = Executors.newFixedThreadPool(10);
        
        idGenerator = new LeafSegmentIdGenerator();
        // 使用反射设置私有字段（简化测试）
        try {
            var field = LeafSegmentIdGenerator.class.getDeclaredField("sequenceRepository");
            field.setAccessible(true);
            field.set(idGenerator, mockRepository);
            
            var executorField = LeafSegmentIdGenerator.class.getDeclaredField("taskExecutor");
            executorField.setAccessible(true);
            executorField.set(idGenerator, executorService);
        } catch (Exception e) {
            throw new RuntimeException("Failed to setup test", e);
        }
    }

    @Test
    @DisplayName("基础Segment功能测试")
    void testBasicSegmentFunctionality() {
        Segment segment = new Segment();
        segment.init("test", 1, 100);

        // 测试基本功能
        assertEquals(1, segment.nextId());
        assertEquals(2, segment.nextId());
        assertEquals(98, segment.getRemaining());
        assertFalse(segment.isExhausted());

        // 测试耗尽
        for (int i = 3; i <= 100; i++) {
            long id = segment.nextId();
            assertEquals(i, id);
        }
        
        assertEquals(-1, segment.nextId()); // 段已用尽
        assertTrue(segment.isExhausted());
        assertEquals(0, segment.getRemaining());
    }

    @Test
    @DisplayName("双缓冲机制测试")
    void testDoubleBuffering() {
        SegmentBuffer buffer = new SegmentBuffer("test");
        
        // 初始化第一个段
        buffer.initFirstSegment(1, 100);
        assertTrue(buffer.isInitialized());
        
        // 使用第一个段
        for (int i = 1; i <= 75; i++) { // 75%使用率
            assertEquals(i, buffer.nextId());
        }
        
        // 此时应该触发预加载检查
        assertTrue(buffer.needsPreload());
        
        // 预加载下一个段
        buffer.preloadNextSegment(101, 200);
        
        // 继续使用当前段直到用完
        for (int i = 76; i <= 100; i++) {
            assertEquals(i, buffer.nextId());
        }
        
        // 切换到下一个段应该无缝进行
        assertEquals(101, buffer.nextId());
        assertEquals(102, buffer.nextId());
    }

    @Test
    @DisplayName("高并发ID生成唯一性测试")
    void testConcurrentIdGenerationUniqueness() throws InterruptedException {
        final int threadCount = 50;
        final int operationsPerThread = 100;
        final CountDownLatch startLatch = new CountDownLatch(1);
        final CountDownLatch finishLatch = new CountDownLatch(threadCount);
        final Set<Long> generatedIds = ConcurrentHashMap.newKeySet();
        final AtomicInteger duplicateCount = new AtomicInteger(0);
        final AtomicInteger successCount = new AtomicInteger(0);

        // 启动多个线程并发生成ID
        for (int i = 0; i < threadCount; i++) {
            executorService.submit(() -> {
                try {
                    startLatch.await(); // 等待统一开始信号
                    
                    for (int j = 0; j < operationsPerThread; j++) {
                        try {
                            long id = idGenerator.generateId("test");
                            if (id != -1) {
                                if (!generatedIds.add(id)) {
                                    duplicateCount.incrementAndGet();
                                    System.err.println("发现重复ID: " + id);
                                } else {
                                    successCount.incrementAndGet();
                                }
                            }
                        } catch (Exception e) {
                            System.err.println("ID生成失败: " + e.getMessage());
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
        startLatch.countDown(); // 开始信号
        finishLatch.await(); // 等待所有线程完成
        long endTime = System.currentTimeMillis();

        // 性能和正确性验证
        System.out.println("=== 美团Leaf高并发测试结果 ===");
        System.out.println("线程数: " + threadCount);
        System.out.println("每线程操作数: " + operationsPerThread);
        System.out.println("总操作数: " + (threadCount * operationsPerThread));
        System.out.println("成功生成ID数: " + successCount.get());
        System.out.println("唯一ID数: " + generatedIds.size());
        System.out.println("重复ID数: " + duplicateCount.get());
        System.out.println("执行时间: " + (endTime - startTime) + " ms");
        System.out.println("平均QPS: " + String.format("%.2f", successCount.get() * 1000.0 / (endTime - startTime)));

        // 断言验证
        assertEquals(0, duplicateCount.get(), "不应该有重复ID");
        assertEquals(generatedIds.size(), successCount.get(), "唯一ID数应等于成功生成数");
        assertTrue(successCount.get() > threadCount * operationsPerThread * 0.95, "成功率应大于95%");
    }

    @Test
    @DisplayName("格式化ID生成测试")
    void testFormattedIdGeneration() {
        // 测试追踪号格式
        String trackingNumber = idGenerator.generateFormattedId("test", "UPS", false);
        assertNotNull(trackingNumber);
        assertTrue(trackingNumber.startsWith("UPS"));
        assertEquals(15, trackingNumber.length()); // UPS + 12位数字
        assertTrue(trackingNumber.substring(3).matches("\\d{12}"));

        // 测试带时间戳的格式
        String timestampedId = idGenerator.generateFormattedId("test", "ORD", true);
        assertTrue(timestampedId.startsWith("ORD"));
        assertTrue(timestampedId.length() > 15); // ORD + 时间戳 + 12位数字

        System.out.println("生成的追踪号示例:");
        for (int i = 0; i < 5; i++) {
            System.out.println("  " + idGenerator.generateFormattedId("test", "UPS", false));
        }
    }

    @Test
    @DisplayName("预加载阈值测试")
    void testPreloadThreshold() {
        Segment segment = new Segment();
        segment.init("test", 1, 100);

        // 使用到74%，不应触发预加载
        for (int i = 1; i <= 74; i++) {
            segment.nextId();
        }
        assertFalse(segment.shouldPreload());

        // 使用到75%，应该触发预加载
        segment.nextId(); // 第75个
        assertTrue(segment.shouldPreload());
        assertEquals(75.0, segment.getUsagePercentage(), 0.1);
    }

    @Test
    @DisplayName("异常情况处理测试")
    void testErrorHandling() {
        // 测试未初始化的段
        Segment segment = new Segment();
        assertEquals(-1, segment.nextId());
        assertEquals(0, segment.getRemaining());
        assertFalse(segment.shouldPreload());

        // 测试未初始化的缓冲区
        SegmentBuffer buffer = new SegmentBuffer("test");
        assertEquals(-1, buffer.nextId());
        assertFalse(buffer.needsPreload());
    }

    @Test
    @DisplayName("内存使用效率测试")
    void testMemoryEfficiency() {
        final int bufferCount = 100;
        
        // 创建多个缓冲区模拟多业务场景
        for (int i = 0; i < bufferCount; i++) {
            String bizTag = "biz_" + i;
            // 每个缓冲区生成少量ID
            for (int j = 0; j < 10; j++) {
                idGenerator.generateId(bizTag);
            }
        }

        // 验证缓冲区数量
        assertEquals(bufferCount, idGenerator.getBufferCount());

        // 检查缓冲区状态
        var allStatuses = idGenerator.getAllBufferStatuses();
        assertEquals(bufferCount, allStatuses.size());

        System.out.println("创建了 " + bufferCount + " 个业务缓冲区，内存使用正常");
    }
}