package com.miniups.concurrency;

import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.Transactional;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;
import java.util.List;
import java.util.ArrayList;
import java.util.Collections;

import static org.assertj.core.api.Assertions.*;

/**
 * 高并发测试基础框架
 * 提供并发测试的通用工具和方法
 */
@SpringBootTest
@ExtendWith(SpringExtension.class)
@ActiveProfiles("test")
@Transactional
public abstract class ConcurrencyTestBase {

    protected static final int DEFAULT_THREAD_COUNT = 50;
    protected static final int DEFAULT_OPERATIONS_PER_THREAD = 10;
    protected static final long DEFAULT_TIMEOUT_SECONDS = 30;

    /**
     * 并发测试结果
     */
    public static class ConcurrencyTestResult {
        private final int totalOperations;
        private final int successCount;
        private final int failureCount;
        private final long executionTimeMs;
        private final List<Exception> exceptions;
        private final double operationsPerSecond;

        public ConcurrencyTestResult(int totalOperations, int successCount, int failureCount, 
                                   long executionTimeMs, List<Exception> exceptions) {
            this.totalOperations = totalOperations;
            this.successCount = successCount;
            this.failureCount = failureCount;
            this.executionTimeMs = executionTimeMs;
            this.exceptions = Collections.unmodifiableList(exceptions);
            this.operationsPerSecond = totalOperations / (executionTimeMs / 1000.0);
        }

        // Getters
        public int getTotalOperations() { return totalOperations; }
        public int getSuccessCount() { return successCount; }
        public int getFailureCount() { return failureCount; }
        public long getExecutionTimeMs() { return executionTimeMs; }
        public List<Exception> getExceptions() { return exceptions; }
        public double getOperationsPerSecond() { return operationsPerSecond; }
        public double getSuccessRate() { return (double) successCount / totalOperations * 100; }
        public double getFailureRate() { return (double) failureCount / totalOperations * 100; }
    }

    /**
     * 执行并发测试
     * 
     * @param operation 要并发执行的操作
     * @param threadCount 线程数量
     * @param operationsPerThread 每个线程执行的操作次数
     * @param timeoutSeconds 超时时间（秒）
     * @return 测试结果
     */
    protected ConcurrencyTestResult executeConcurrencyTest(
            Supplier<Boolean> operation,
            int threadCount,
            int operationsPerThread,
            long timeoutSeconds) {

        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch endLatch = new CountDownLatch(threadCount);
        
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);
        List<Exception> exceptions = Collections.synchronizedList(new ArrayList<>());
        
        long startTime = System.currentTimeMillis();

        // 创建并启动线程
        for (int i = 0; i < threadCount; i++) {
            final int threadIndex = i;
            executor.submit(() -> {
                try {
                    // 等待所有线程就绪
                    startLatch.await();
                    
                    // 执行操作
                    for (int j = 0; j < operationsPerThread; j++) {
                        try {
                            boolean success = operation.get();
                            if (success) {
                                successCount.incrementAndGet();
                            } else {
                                failureCount.incrementAndGet();
                            }
                        } catch (Exception e) {
                            failureCount.incrementAndGet();
                            exceptions.add(new RuntimeException(
                                String.format("Thread %d, Operation %d failed", threadIndex, j), e));
                        }
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    exceptions.add(e);
                } finally {
                    endLatch.countDown();
                }
            });
        }

        try {
            // 启动所有线程
            startLatch.countDown();
            
            // 等待所有线程完成
            if (!endLatch.await(timeoutSeconds, TimeUnit.SECONDS)) {
                throw new RuntimeException("Concurrency test timed out after " + timeoutSeconds + " seconds");
            }
            
            long endTime = System.currentTimeMillis();
            long executionTime = endTime - startTime;
            
            int totalOperations = threadCount * operationsPerThread;
            
            return new ConcurrencyTestResult(
                totalOperations,
                successCount.get(),
                failureCount.get(),
                executionTime,
                exceptions
            );
            
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Concurrency test was interrupted", e);
        } finally {
            executor.shutdown();
            try {
                if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                    executor.shutdownNow();
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                executor.shutdownNow();
            }
        }
    }

    /**
     * 执行并发测试（使用默认参数）
     */
    protected ConcurrencyTestResult executeConcurrencyTest(Supplier<Boolean> operation) {
        return executeConcurrencyTest(operation, DEFAULT_THREAD_COUNT, 
                                    DEFAULT_OPERATIONS_PER_THREAD, DEFAULT_TIMEOUT_SECONDS);
    }

    /**
     * 执行带返回值的并发测试
     * 
     * @param operation 要并发执行的操作
     * @param threadCount 线程数量
     * @param operationsPerThread 每个线程执行的操作次数
     * @return 所有操作的结果列表
     */
    protected <T> List<T> executeConcurrencyTestWithResults(
            Supplier<T> operation,
            int threadCount,
            int operationsPerThread) {

        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch endLatch = new CountDownLatch(threadCount);
        
        List<T> results = Collections.synchronizedList(new ArrayList<>());
        List<Exception> exceptions = Collections.synchronizedList(new ArrayList<>());

        // 创建并启动线程
        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    startLatch.await();
                    
                    for (int j = 0; j < operationsPerThread; j++) {
                        try {
                            T result = operation.get();
                            if (result != null) {
                                results.add(result);
                            }
                        } catch (Exception e) {
                            exceptions.add(e);
                        }
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    exceptions.add(e);
                } finally {
                    endLatch.countDown();
                }
            });
        }

        try {
            startLatch.countDown();
            endLatch.await(DEFAULT_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            
            if (!exceptions.isEmpty()) {
                throw new RuntimeException("Concurrency test had exceptions: " + exceptions.size());
            }
            
            return new ArrayList<>(results);
            
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Concurrency test was interrupted", e);
        } finally {
            executor.shutdown();
        }
    }

    /**
     * 验证并发测试结果
     */
    protected void assertConcurrencyTestResult(ConcurrencyTestResult result, 
                                             double minSuccessRate, 
                                             double maxAcceptableFailureRate) {
        assertThat(result).isNotNull();
        assertThat(result.getSuccessRate())
            .as("Success rate should be at least %.2f%%", minSuccessRate)
            .isGreaterThanOrEqualTo(minSuccessRate);
        
        assertThat(result.getFailureRate())
            .as("Failure rate should be at most %.2f%%", maxAcceptableFailureRate)
            .isLessThanOrEqualTo(maxAcceptableFailureRate);
        
        if (!result.getExceptions().isEmpty()) {
            System.err.println("Exceptions during concurrency test:");
            result.getExceptions().forEach(e -> e.printStackTrace());
        }
    }

    /**
     * 打印并发测试结果
     */
    protected void printConcurrencyTestResult(ConcurrencyTestResult result, String testName) {
        System.out.println("\n=== " + testName + " 并发测试结果 ===");
        System.out.println("总操作数: " + result.getTotalOperations());
        System.out.println("成功数: " + result.getSuccessCount());
        System.out.println("失败数: " + result.getFailureCount());
        System.out.println("执行时间: " + result.getExecutionTimeMs() + " ms");
        System.out.println("成功率: " + String.format("%.2f%%", result.getSuccessRate()));
        System.out.println("失败率: " + String.format("%.2f%%", result.getFailureRate()));
        System.out.println("吞吐量: " + String.format("%.2f ops/sec", result.getOperationsPerSecond()));
        
        if (!result.getExceptions().isEmpty()) {
            System.out.println("异常数量: " + result.getExceptions().size());
            System.out.println("首个异常: " + result.getExceptions().get(0).getMessage());
        }
        System.out.println("==========================================\n");
    }

    /**
     * 创建竞争条件测试
     * 多个线程同时访问共享资源，验证线程安全性
     */
    protected void executeRaceConditionTest(Runnable sharedResourceOperation, 
                                          int threadCount, 
                                          int operationsPerThread) {
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch endLatch = new CountDownLatch(threadCount);
        AtomicReference<Exception> firstException = new AtomicReference<>();

        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    startLatch.await();
                    
                    for (int j = 0; j < operationsPerThread; j++) {
                        try {
                            sharedResourceOperation.run();
                        } catch (Exception e) {
                            firstException.compareAndSet(null, e);
                        }
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    firstException.compareAndSet(null, e);
                } finally {
                    endLatch.countDown();
                }
            });
        }

        try {
            startLatch.countDown();
            endLatch.await(DEFAULT_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            
            if (firstException.get() != null) {
                throw new RuntimeException("Race condition test failed", firstException.get());
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Race condition test was interrupted", e);
        } finally {
            executor.shutdown();
        }
    }

    /**
     * 性能基准测试
     */
    protected void benchmarkOperation(String operationName, 
                                    Supplier<Boolean> operation, 
                                    int threadCount, 
                                    int operationsPerThread) {
        ConcurrencyTestResult result = executeConcurrencyTest(operation, threadCount, operationsPerThread, 60);
        printConcurrencyTestResult(result, operationName + " 性能基准");
        
        // 基本性能断言
        assertThat(result.getSuccessRate()).isGreaterThan(95.0);
        assertThat(result.getOperationsPerSecond()).isGreaterThan(10.0);
    }
}