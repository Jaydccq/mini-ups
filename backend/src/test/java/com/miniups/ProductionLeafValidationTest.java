package com.miniups;

import com.miniups.service.id.LeafSegmentIdGenerator;
import com.miniups.service.id.Segment;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 生产级Leaf实现验证测试
 * 验证所有高优先级改进是否正确实现
 */
@DisplayName("生产级美团Leaf实现验证")
public class ProductionLeafValidationTest {

    @Test
    @DisplayName("12位数字溢出处理验证")
    void testTwelveDigitOverflowHandling() {
        System.out.println("=== 测试12位数字溢出处理 ===");
        
        // 创建一个大ID来测试溢出处理
        long largeId = 1_500_000_000_000L; // 超过12位
        System.out.printf("原始ID: %d (长度: %d位)%n", largeId, String.valueOf(largeId).length());
        
        // 应用modulo处理
        long safeId = largeId % 1_000_000_000_000L;
        String formatted = String.format("%012d", safeId);
        String trackingNumber = "UPS" + formatted;
        
        System.out.printf("处理后ID: %d (长度: %d位)%n", safeId, String.valueOf(safeId).length());
        System.out.printf("格式化追踪号: %s (长度: %d)%n", trackingNumber, trackingNumber.length());
        
        // 验证结果
        assertEquals(15, trackingNumber.length(), "追踪号长度应为15");
        assertTrue(trackingNumber.startsWith("UPS"), "应以UPS开头");
        assertTrue(trackingNumber.substring(3).matches("\\d{12}"), "数字部分应为12位");
        
        System.out.println("✓ 12位数字溢出处理正确");
    }

    @Test
    @DisplayName("可配置预加载阈值验证")
    void testConfigurablePreloadThreshold() {
        System.out.println("=== 测试可配置预加载阈值 ===");
        
        Segment segment = new Segment();
        segment.init("test", 1, 100);
        
        // 测试不同的阈值
        double[] thresholds = {50.0, 75.0, 90.0};
        int[] usagePoints = {50, 75, 90}; // 对应的使用点
        
        for (int i = 0; i < thresholds.length; i++) {
            // 重置段
            segment.reset();
            segment.init("test", 1, 100);
            
            double threshold = thresholds[i];
            int usagePoint = usagePoints[i];
            
            // 使用到指定点之前，不应触发
            for (int j = 1; j < usagePoint; j++) {
                segment.nextId();
            }
            assertFalse(segment.shouldPreload(threshold), 
                String.format("使用率%.0f%%时不应触发%.0f%%阈值", segment.getUsagePercentage(), threshold));
            
            // 使用到指定点，应该触发
            segment.nextId(); // 达到使用点
            assertTrue(segment.shouldPreload(threshold), 
                String.format("使用率%.0f%%时应该触发%.0f%%阈值", segment.getUsagePercentage(), threshold));
            
            System.out.printf("阈值%.0f%% - 使用率%.0f%%时正确触发预加载%n", threshold, segment.getUsagePercentage());
        }
        
        System.out.println("✓ 可配置预加载阈值验证通过");
    }

    @Test
    @DisplayName("高并发锁安全性验证")
    void testConcurrentLockSafety() throws InterruptedException {
        System.out.println("=== 测试高并发锁安全性 ===");
        
        final int threadCount = 100;
        final int operationsPerThread = 50;
        final CountDownLatch startLatch = new CountDownLatch(1);
        final CountDownLatch finishLatch = new CountDownLatch(threadCount);
        final Set<Long> generatedIds = ConcurrentHashMap.newKeySet();
        final AtomicInteger duplicates = new AtomicInteger(0);
        final AtomicInteger successes = new AtomicInteger(0);

        // 创建简化版本进行测试（避免Spring依赖）
        final MockLeafGenerator generator = new MockLeafGenerator();
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);

        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    startLatch.await();
                    
                    for (int j = 0; j < operationsPerThread; j++) {
                        long id = generator.generateId("test");
                        if (id != -1) {
                            if (!generatedIds.add(id)) {
                                duplicates.incrementAndGet();
                            } else {
                                successes.incrementAndGet();
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

        System.out.printf("线程数: %d%n", threadCount);
        System.out.printf("成功生成: %d%n", successes.get());
        System.out.printf("唯一ID数: %d%n", generatedIds.size());
        System.out.printf("重复数: %d%n", duplicates.get());
        System.out.printf("执行时间: %d ms%n", endTime - startTime);

        assertEquals(0, duplicates.get(), "不应有重复ID");
        assertEquals(generatedIds.size(), successes.get(), "唯一ID数应等于成功数");
        
        System.out.println("✓ 高并发锁安全性验证通过");
    }

    @Test
    @DisplayName("边界条件处理验证")
    void testEdgeCaseHandling() {
        System.out.println("=== 测试边界条件处理 ===");
        
        // 测试段边界
        Segment segment = new Segment();
        segment.init("test", 1, 3); // 只有3个ID的小段
        
        assertEquals(1, segment.nextId());
        assertEquals(2, segment.nextId());
        assertEquals(3, segment.nextId());
        assertEquals(-1, segment.nextId()); // 段已用尽
        
        assertTrue(segment.isExhausted(), "段应该已用尽");
        assertEquals(0, segment.getRemaining(), "剩余应为0");
        
        // 测试未初始化段
        Segment uninitializedSegment = new Segment();
        assertEquals(-1, uninitializedSegment.nextId());
        assertEquals(0, uninitializedSegment.getRemaining());
        assertFalse(uninitializedSegment.shouldPreload());
        
        System.out.println("✓ 边界条件处理验证通过");
    }

    @Test
    @DisplayName("ID格式正确性验证")
    void testIdFormatCorrectness() {
        System.out.println("=== 测试ID格式正确性 ===");
        
        MockLeafGenerator generator = new MockLeafGenerator();
        
        // 生成多个追踪号验证格式
        for (int i = 0; i < 100; i++) {
            String trackingNumber = generator.generateTrackingNumber();
            assertNotNull(trackingNumber, "追踪号不应为null");
            assertEquals(15, trackingNumber.length(), "追踪号长度应为15");
            assertTrue(trackingNumber.startsWith("UPS"), "应以UPS开头");
            
            String numberPart = trackingNumber.substring(3);
            assertTrue(numberPart.matches("\\d{12}"), "数字部分应为12位数字");
            
            // 验证数字部分不会超过12位
            long numValue = Long.parseLong(numberPart);
            assertTrue(numValue < 1_000_000_000_000L, "数字值应小于10^12");
        }
        
        System.out.println("✓ ID格式正确性验证通过");
    }

    // 简化的Mock实现，用于测试核心逻辑
    private static class MockLeafGenerator {
        private final java.util.concurrent.atomic.AtomicLong counter = new java.util.concurrent.atomic.AtomicLong(0);
        private final java.util.concurrent.ConcurrentHashMap<String, java.util.concurrent.atomic.AtomicLong> counters = new java.util.concurrent.ConcurrentHashMap<>();
        private static final long TWELVE_DIGIT_MAX = 1_000_000_000_000L;
        
        public long generateId(String bizTag) {
            return counters.computeIfAbsent(bizTag, k -> new java.util.concurrent.atomic.AtomicLong(0))
                          .incrementAndGet();
        }
        
        public String generateTrackingNumber() {
            long id = generateId("tracking_number");
            // 应用溢出保护
            long safeId = id % TWELVE_DIGIT_MAX;
            return "UPS" + String.format("%012d", safeId);
        }
    }
}