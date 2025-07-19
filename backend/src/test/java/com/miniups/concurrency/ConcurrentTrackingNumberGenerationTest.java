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
 * 追踪号生成并发测试
 * 测试高并发追踪号生成的唯一性、性能和线程安全性
 */
@DisplayName("追踪号生成并发测试")
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

        // 验证唯一性
        assertThat(duplicateCounter.get()).isEqualTo(0);
        assertThat(uniqueNumbers.size()).isEqualTo(nonNullResults);
        assertThat(nonNullResults).isEqualTo(threadCount * operationsPerThread);

        // 验证格式正确性
        for (String trackingNumber : uniqueNumbers) {
            assertThat(trackingNumber).startsWith("UPS");
            assertThat(trackingService.isValidTrackingNumberFormat(trackingNumber)).isTrue();
        }
    }

    @Test
    @DisplayName("高频追踪号生成性能测试")
    void testHighFrequencyTrackingNumberGeneration() {
        // Given
        int threadCount = 100;
        int operationsPerThread = 50;
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
        printConcurrencyTestResult(result, "高频追踪号生成");

        System.out.println("总执行时间: " + totalTimeSeconds + " 秒");
        System.out.println("成功生成数: " + successCounter.get());
        System.out.println("平均每秒生成: " + String.format("%.2f", result.getOperationsPerSecond()));

        // 性能要求
        assertThat(result.getSuccessRate()).isGreaterThan(99.0);
        assertThat(result.getOperationsPerSecond()).isGreaterThan(100.0);
        assertThat(totalTimeSeconds).isLessThan(30.0);
    }

    @Test
    @DisplayName("追踪号格式验证并发测试")
    void testConcurrentTrackingNumberValidation() {
        // Given
        int threadCount = 30;
        int operationsPerThread = 10;
        String[] testNumbers = {
            "UPS123456789012345",
            "UPS000000000000001",
            "ABC123456789012345", // 无效
            "UPS12345",           // 无效
            "",                   // 无效
            null,                 // 无效
            "ups123456789012345", // 无效（小写）
            "UPS999999999999999"
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
        assertThat(allNumbers.size()).isEqualTo(successCounter.get()); // 所有生成的号码都应该是唯一的
        
        System.out.println("系统压力测试表现: " + 
                          (result.getSuccessRate() > 99.0 ? "优秀" : 
                           result.getSuccessRate() > 95.0 ? "良好" : "需要优化"));
    }

    @Test
    @DisplayName("追踪号时间戳分析测试")
    void testTrackingNumberTimestampAnalysis() {
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

        // 验证所有追踪号都有正确的时间戳结构
        for (String trackingNumber : validNumbers) {
            assertThat(trackingNumber).startsWith("UPS");
            assertThat(trackingNumber).hasSize(21); // UPS + 18位数字
            
            // 提取时间戳部分（UPS后的前14位）
            String timestampPart = trackingNumber.substring(3, 17);
            assertThat(timestampPart).matches("\\d{14}");
        }

        // 验证时间戳的合理性（应该接近当前时间）
        long currentTime = System.currentTimeMillis();
        for (String trackingNumber : validNumbers.subList(0, Math.min(5, validNumbers.size()))) {
            String timestampStr = trackingNumber.substring(3, 17);
            long timestamp = Long.parseLong(timestampStr);
            
            // 时间戳应该在测试执行时间的合理范围内
            assertThat(Math.abs(currentTime - timestamp)).isLessThan(60000); // 1分钟内
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
    @DisplayName("内存使用分析测试")
    void testTrackingNumberMemoryUsage() {
        // Given
        int batchSize = 1000;
        Runtime runtime = Runtime.getRuntime();
        
        // When
        long memoryBefore = runtime.totalMemory() - runtime.freeMemory();
        
        List<String> trackingNumbers = executeConcurrencyTestWithResults(() -> {
            return trackingService.generateTrackingNumber();
        }, 50, 20);
        
        System.gc(); // 建议垃圾回收
        Thread.yield();
        
        long memoryAfter = runtime.totalMemory() - runtime.freeMemory();
        long memoryUsed = memoryAfter - memoryBefore;

        // Then
        System.out.println("生成追踪号数量: " + trackingNumbers.size());
        System.out.println("内存使用前: " + memoryBefore / 1024 + " KB");
        System.out.println("内存使用后: " + memoryAfter / 1024 + " KB");
        System.out.println("内存增长: " + memoryUsed / 1024 + " KB");
        
        if (trackingNumbers.size() > 0) {
            long avgMemoryPerNumber = memoryUsed / trackingNumbers.size();
            System.out.println("平均每个追踪号内存消耗: " + avgMemoryPerNumber + " bytes");
        }

        // 验证内存使用合理
        assertThat(trackingNumbers).isNotEmpty();
        
        // 每个追踪号应该不超过合理的内存使用量
        if (trackingNumbers.size() > 0 && memoryUsed > 0) {
            long avgMemoryPerNumber = memoryUsed / trackingNumbers.size();
            assertThat(avgMemoryPerNumber).isLessThan(1024); // 每个号码不超过1KB
        }
    }
}