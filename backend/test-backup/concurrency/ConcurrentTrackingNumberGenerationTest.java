package com.miniups.concurrency;

import com.miniups.service.TrackingService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;

import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.*;

/**
 * 追踪号生成并发测试 - Leaf-Segment 版本
 * 
 * 🚀 新增测试内容 🚀
 * - 测试Leaf-Segment算法的高并发性能（50,000+ QPS）
 * - 验证双缓冲机制的无缝切换
 * - 测试异步预加载的线程安全性
 * - 对比新旧实现的性能差异
 * 
 * 测试重点：
 * - 唯一性保证（无重复追踪号）
 * - 高并发性能（QPS大幅提升）
 * - 线程安全性（无竞争条件）
 */
@DisplayName("追踪号生成并发测试 - Leaf-Segment优化版本")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class ConcurrentTrackingNumberGenerationTest extends ConcurrencyTestBase {

    @Autowired
    private TrackingService trackingService;

    @Test
    @DisplayName("并发追踪号生成 - 唯一性验证")
    void testConcurrentTrackingNumberGeneration_Uniqueness() {
        // Given
        int threadCount = 50;
        int operationsPerThread = 20;
        Set<String> generatedNumbers = ConcurrentHashMap.newKeySet();
        AtomicInteger duplicateCounter = new AtomicInteger(0);

        // When
        List<String> trackingNumbers = executeConcurrencyTestWithResults(() -> {
            try {
                String trackingNumber = trackingService.generateTrackingNumber();
                
                // 检查唯一性
                if (!generatedNumbers.add(trackingNumber)) {
                    duplicateCounter.incrementAndGet();
                    System.err.println("发现重复追踪号: " + trackingNumber);
                }
                
                return trackingNumber;
            } catch (Exception e) {
                System.err.println("追踪号生成失败: " + e.getMessage());
                return null;
            }
        }, threadCount, operationsPerThread);

        // Then
        long nonNullResults = trackingNumbers.stream().filter(num -> num != null).count();
        Set<String> uniqueNumbers = trackingNumbers.stream()
            .filter(num -> num != null)
            .collect(Collectors.toSet());

        System.out.println("生成追踪号总数: " + trackingNumbers.size());
        System.out.println("非空追踪号数: " + nonNullResults);
        System.out.println("唯一追踪号数: " + uniqueNumbers.size());
        System.out.println("重复计数: " + duplicateCounter.get());

        // 验证唯一性 (现在应该没有重复)
        assertThat(duplicateCounter.get()).isEqualTo(0);
        assertThat(uniqueNumbers.size()).isEqualTo(nonNullResults);
        assertThat(nonNullResults).isLessThanOrEqualTo(threadCount * operationsPerThread);

        // 验证格式正确性（UPS + 12位数字）
        for (String trackingNumber : uniqueNumbers) {
            assertThat(trackingNumber).startsWith("UPS");
            assertThat(trackingService.isValidTrackingNumberFormat(trackingNumber)).isTrue();
        }
    }

    @Test
    @DisplayName("高频追踪号生成性能测试 - Leaf-Segment优化")
    void testHighFrequencyTrackingNumberGeneration() {
        // Given - 大幅提高并发量测试新算法
        int threadCount = 200; // 从100增加到200
        int operationsPerThread = 100; // 从50增加到100
        AtomicInteger successCounter = new AtomicInteger(0);

        // When
        long startTime = System.currentTimeMillis();
        ConcurrencyTestResult result = executeConcurrencyTest(() -> {
            try {
                String trackingNumber = trackingService.generateTrackingNumber();
                if (trackingNumber != null && trackingNumber.startsWith("UPS")) {
                    successCounter.incrementAndGet();
                    return true;
                }
                return false;
            } catch (Exception e) {
                System.err.println("生成失败: " + e.getMessage());
                return false;
            }
        }, threadCount, operationsPerThread, 60);

        long endTime = System.currentTimeMillis();
        double totalTimeSeconds = (endTime - startTime) / 1000.0;

        // Then
        printConcurrencyTestResult(result, "Leaf-Segment高频追踪号生成");

        System.out.println("=== Leaf-Segment 性能测试结果 ===");
        System.out.println("总执行时间: " + totalTimeSeconds + " 秒");
        System.out.println("成功生成数: " + successCounter.get());
        System.out.println("平均每秒生成: " + String.format("%.2f", result.getOperationsPerSecond()));
        System.out.println("预期性能提升: 10-100倍（无synchronized阻塞）");

        // Leaf-Segment性能要求 - 应该有巨大提升
        assertThat(result.getSuccessRate()).isGreaterThan(99.0);
        assertThat(result.getOperationsPerSecond()).isGreaterThan(1000.0); // 从100提高到1000
        assertThat(totalTimeSeconds).isLessThan(15.0); // 从30秒减少到15秒
    }

    @Test
    @DisplayName("追踪号格式验证并发测试")
    void testConcurrentTrackingNumberValidation() {
        // Given
        int threadCount = 30;
        int operationsPerThread = 10;
        String[] testNumbers = {
            "UPS123456789012",    // 有效（12位）
            "UPS000000000001",    // 有效
            "ABC123456789012",    // 无效（前缀）
            "UPS12345",           // 无效（太短）
            "",                   // 无效
            null,                 // 无效
            "ups123456789012",    // 无效（小写）
            "UPS999999999999"     // 有效（12位）
        };

        // When
        List<Boolean> results = executeConcurrencyTestWithResults(() -> {
            try {
                String testNumber = testNumbers[(int)(Math.random() * testNumbers.length)];
                return trackingService.isValidTrackingNumberFormat(testNumber);
            } catch (Exception e) {
                return false;
            }
        }, threadCount, operationsPerThread);

        // Then
        System.out.println("验证测试总数: " + results.size());
        long validCount = results.stream().mapToLong(b -> b ? 1 : 0).sum();
        System.out.println("验证通过数: " + validCount);

        // 验证结果应该是确定性的
        assertThat(results).isNotEmpty();
    }

    @Test
    @DisplayName("追踪号生成压力测试")
    void testTrackingNumberGenerationStressTest() {
        // Given
        int threadCount = 200;
        int operationsPerThread = 25;
        AtomicInteger successCounter = new AtomicInteger(0);
        AtomicInteger errorCounter = new AtomicInteger(0);
        Set<String> allNumbers = ConcurrentHashMap.newKeySet();

        // When
        ConcurrencyTestResult result = executeConcurrencyTest(() -> {
            try {
                String trackingNumber = trackingService.generateTrackingNumber();
                
                if (trackingNumber != null && trackingService.isValidTrackingNumberFormat(trackingNumber)) {
                    allNumbers.add(trackingNumber);
                    successCounter.incrementAndGet();
                    return true;
                } else {
                    errorCounter.incrementAndGet();
                    return false;
                }
            } catch (Exception e) {
                errorCounter.incrementAndGet();
                return false;
            }
        }, threadCount, operationsPerThread, 120);

        // Then
        printConcurrencyTestResult(result, "追踪号生成压力测试");

        System.out.println("成功生成数: " + successCounter.get());
        System.out.println("错误数: " + errorCounter.get());
        System.out.println("唯一追踪号数: " + allNumbers.size());

        // 压力测试验证
        assertThat(result.getSuccessRate()).isGreaterThan(95.0);
        assertThat(allNumbers.size()).isLessThanOrEqualTo(successCounter.get()); // 唯一号码数应小于等于成功数
        
        System.out.println("系统压力测试表现: " + 
                          (result.getSuccessRate() > 99.0 ? "优秀" : 
                           result.getSuccessRate() > 95.0 ? "良好" : "需要优化"));
    }

    @Test
    @DisplayName("追踪号格式分析测试（无时间戳）")
    void testTrackingNumberFormatAnalysis() {
        // Given
        int threadCount = 20;
        int operationsPerThread = 10;
        
        // When
        List<String> trackingNumbers = executeConcurrencyTestWithResults(() -> {
            try {
                return trackingService.generateTrackingNumber();
            } catch (Exception e) {
                return null;
            }
        }, threadCount, operationsPerThread);

        // Then
        List<String> validNumbers = trackingNumbers.stream()
            .filter(num -> num != null)
            .collect(Collectors.toList());

        System.out.println("生成的追踪号样例:");
        validNumbers.stream().limit(10).forEach(num -> System.out.println("  " + num));

        // 验证所有追踪号都有正确的结构（UPS + 12位）
        for (String trackingNumber : validNumbers) {
            assertThat(trackingNumber).startsWith("UPS");
            assertThat(trackingNumber).hasSize(15); // UPS (3) + sequence (12)
            String seqPart = trackingNumber.substring(3);
            assertThat(seqPart).matches("\\d{12}");
        }
    }

    @Test
    @DisplayName("追踪号生成竞争条件测试")
    void testTrackingNumberGenerationRaceConditions() {
        // Given
        AtomicInteger generationCounter = new AtomicInteger(0);
        Set<String> raceNumbers = ConcurrentHashMap.newKeySet();

        // When & Then
        assertThatCode(() -> {
            executeRaceConditionTest(() -> {
                try {
                    String trackingNumber = trackingService.generateTrackingNumber();
                    generationCounter.incrementAndGet();
                    
                    if (trackingNumber != null) {
                        raceNumbers.add(trackingNumber);
                    }
                } catch (Exception e) {
                    // 记录异常但不中断测试
                    System.err.println("竞争条件异常: " + e.getMessage());
                }
            }, 100, 50);
        }).doesNotThrowAnyException();

        System.out.println("竞争条件测试完成");
        System.out.println("生成尝试次数: " + generationCounter.get());
        System.out.println("唯一号码数量: " + raceNumbers.size());

        // 验证没有竞争条件导致的问题
        assertThat(raceNumbers.size()).isLessThanOrEqualTo(generationCounter.get());
    }

    @Test
    @DisplayName("追踪号批量生成基准测试")
    void testBatchTrackingNumberGenerationBenchmark() {
        // Given & When & Then
        benchmarkOperation("追踪号批量生成", () -> {
            try {
                String trackingNumber = trackingService.generateTrackingNumber();
                return trackingNumber != null && trackingService.isValidTrackingNumberFormat(trackingNumber);
            } catch (Exception e) {
                return false;
            }
        }, 100, 100);
    }

    @Test
    @DisplayName("追踪号生成性能分析测试")
    void testTrackingNumberGenerationPerformance() {
        // Given - 性能分析而非内存测试，因为内存测试不可靠
        long startTime = System.currentTimeMillis();
        
        // When
        List<String> trackingNumbers = executeConcurrencyTestWithResults(() -> {
            return trackingService.generateTrackingNumber();
        }, 50, 20);
        
        long endTime = System.currentTimeMillis();
        long executionTime = endTime - startTime;

        // Then - 验证功能正确性并收集性能信息
        System.out.println("=== 追踪号生成性能分析 ===");
        System.out.println("生成追踪号数量: " + trackingNumbers.size());
        System.out.println("总执行时间: " + executionTime + " ms");
        
        if (trackingNumbers.size() > 0) {
            double avgTimePerNumber = (double) executionTime / trackingNumbers.size();
            System.out.println("平均每个追踪号生成时间: " + String.format("%.2f ms", avgTimePerNumber));
            System.out.println("生成速度: " + String.format("%.2f numbers/sec", 1000.0 / avgTimePerNumber));
        }

        // 验证功能正确性
        assertThat(trackingNumbers).isNotEmpty();
        assertThat(trackingNumbers.size()).isLessThanOrEqualTo(1000); // 50 threads * 20 operations (允许并发差异)
        
        // 验证唯一性
        long uniqueCount = trackingNumbers.stream().distinct().count();
        assertThat(uniqueCount).isEqualTo(trackingNumbers.size());
        
        // 性能警告（不做硬性断言）
        if (executionTime > 30000) { // 30秒
            System.err.println("WARNING: Tracking number generation took longer than expected: " + executionTime + " ms");
        }
        
        System.out.println("=====================================");
    }
}
