package com.miniups.shortlink.performance;

import com.miniups.shortlink.sharding.ShortLinkShardUtils;
import com.miniups.shortlink.util.ShortLinkCodeGenerator;
import com.miniups.shortlink.config.ShortLinkProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Performance tests for the shortlink system components.
 * Tests throughput, latency, and scalability characteristics.
 */
class ShortLinkPerformanceTest {

    private ShortLinkCodeGenerator codeGenerator;
    private NavigableMap<Integer, String> weightMap;

    @BeforeEach
    void setUp() {
        ShortLinkProperties properties = new ShortLinkProperties();
        properties.getCode().setMinLength(8);
        codeGenerator = new ShortLinkCodeGenerator(properties);

        // Setup sharding weights
        Map<String, Integer> weights = new HashMap<>();
        weights.put("short_links_0", 4);
        weights.put("short_links_1", 3);
        weights.put("short_links_2", 2);
        weights.put("short_links_3", 1);
        weightMap = ShortLinkShardUtils.buildWeightMap(weights);
    }

    @Test
    void codeGeneration_shouldMeetThroughputRequirements() {
        int targetCodes = 10000;

        Instant start = Instant.now();

        Set<String> generatedCodes = new HashSet<>();
        for (int i = 0; i < targetCodes; i++) {
            String code = codeGenerator.generate("https://example.com/test" + i, (long) i, 0);
            generatedCodes.add(code);
        }

        Duration elapsed = Duration.between(start, Instant.now());

        // Verify all codes are unique
        assertThat(generatedCodes).hasSize(targetCodes);

        // Calculate throughput (should be > 1000 codes/second)
        double codesPerSecond = targetCodes / (elapsed.toMillis() / 1000.0);
        System.out.println("Code generation throughput: " + codesPerSecond + " codes/second");
        assertThat(codesPerSecond).isGreaterThan(1000);

        // Verify code length requirements
        for (String code : generatedCodes) {
            assertThat(code.length()).isGreaterThanOrEqualTo(8);
        }
    }

    @Test
    void sharding_shouldMeetPerformanceRequirements() {
        int targetOperations = 100000;

        Instant start = Instant.now();

        Map<String, Integer> shardCounts = new HashMap<>();

        for (int i = 0; i < targetOperations; i++) {
            String code = "test_code_" + i;
            String table = ShortLinkShardUtils.resolveTable(code, weightMap);
            shardCounts.merge(table, 1, Integer::sum);
        }

        Duration elapsed = Duration.between(start, Instant.now());

        // Calculate throughput (should be > 10000 operations/second)
        double operationsPerSecond = targetOperations / (elapsed.toMillis() / 1000.0);
        System.out.println("Sharding throughput: " + operationsPerSecond + " operations/second");
        assertThat(operationsPerSecond).isGreaterThan(10000);

        // Verify distribution roughly follows weight ratios (4:3:2:1)
        System.out.println("Shard distribution: " + shardCounts);

        // Allow some variance but verify general distribution
        int total = shardCounts.values().stream().mapToInt(Integer::intValue).sum();
        for (Map.Entry<String, Integer> entry : shardCounts.entrySet()) {
            double percentage = (double) entry.getValue() / total;
            assertThat(percentage).isBetween(0.05, 0.55); // Reasonable range
        }
    }

    @Test
    void concurrentCodeGeneration_shouldBeThreadSafe() throws InterruptedException {
        int numThreads = 10;
        int codesPerThread = 1000;
        ExecutorService executor = Executors.newFixedThreadPool(numThreads);
        CountDownLatch latch = new CountDownLatch(numThreads);
        ConcurrentHashMap<String, Boolean> allCodes = new ConcurrentHashMap<>();

        Instant start = Instant.now();

        for (int t = 0; t < numThreads; t++) {
            final int threadId = t;
            executor.submit(() -> {
                try {
                    for (int i = 0; i < codesPerThread; i++) {
                        String code = codeGenerator.generate(
                            "https://example.com/thread" + threadId + "/item" + i,
                            (long) (threadId * 1000 + i),
                            0
                        );
                        allCodes.put(code, true);
                    }
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await(30, TimeUnit.SECONDS);
        executor.shutdown();

        Duration elapsed = Duration.between(start, Instant.now());

        // Verify all codes are unique (no collisions in concurrent generation)
        int expectedTotal = numThreads * codesPerThread;
        assertThat(allCodes.size()).isEqualTo(expectedTotal);

        // Calculate concurrent throughput
        double codesPerSecond = expectedTotal / (elapsed.toMillis() / 1000.0);
        System.out.println("Concurrent code generation throughput: " + codesPerSecond + " codes/second");
        assertThat(codesPerSecond).isGreaterThan(2000); // Should be higher with concurrency
    }

    @Test
    void concurrentSharding_shouldBeThreadSafe() throws InterruptedException {
        int numThreads = 8;
        int operationsPerThread = 5000;
        ExecutorService executor = Executors.newFixedThreadPool(numThreads);
        CountDownLatch latch = new CountDownLatch(numThreads);
        ConcurrentHashMap<String, Integer> globalShardCounts = new ConcurrentHashMap<>();

        Instant start = Instant.now();

        for (int t = 0; t < numThreads; t++) {
            final int threadId = t;
            executor.submit(() -> {
                try {
                    Map<String, Integer> localCounts = new HashMap<>();
                    for (int i = 0; i < operationsPerThread; i++) {
                        String code = "thread" + threadId + "_code_" + i;
                        String table = ShortLinkShardUtils.resolveTable(code, weightMap);
                        localCounts.merge(table, 1, Integer::sum);
                    }

                    // Merge local counts into global counts
                    for (Map.Entry<String, Integer> entry : localCounts.entrySet()) {
                        globalShardCounts.merge(entry.getKey(), entry.getValue(), Integer::sum);
                    }
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await(30, TimeUnit.SECONDS);
        executor.shutdown();

        Duration elapsed = Duration.between(start, Instant.now());

        // Verify total operations
        int totalOperations = globalShardCounts.values().stream().mapToInt(Integer::intValue).sum();
        assertThat(totalOperations).isEqualTo(numThreads * operationsPerThread);

        // Calculate concurrent throughput
        double operationsPerSecond = totalOperations / (elapsed.toMillis() / 1000.0);
        System.out.println("Concurrent sharding throughput: " + operationsPerSecond + " operations/second");
        assertThat(operationsPerSecond).isGreaterThan(15000);

        System.out.println("Concurrent shard distribution: " + globalShardCounts);
    }

    @Test
    void memoryUsage_shouldBeReasonable() {
        Runtime runtime = Runtime.getRuntime();
        runtime.gc(); // Force garbage collection before test

        long memoryBefore = runtime.totalMemory() - runtime.freeMemory();

        // Generate a large number of codes
        Set<String> codes = new HashSet<>();
        for (int i = 0; i < 50000; i++) {
            String code = codeGenerator.generate("https://example.com/memory-test" + i, (long) i, 0);
            codes.add(code);
        }

        runtime.gc(); // Force garbage collection after test
        long memoryAfter = runtime.totalMemory() - runtime.freeMemory();

        long memoryUsed = memoryAfter - memoryBefore;
        double memoryPerCode = (double) memoryUsed / codes.size();

        System.out.println("Memory usage: " + memoryUsed + " bytes for " + codes.size() + " codes");
        System.out.println("Memory per code: " + memoryPerCode + " bytes");

        // Each code should use reasonable memory (less than 1KB per code including overhead)
        assertThat(memoryPerCode).isLessThan(1024);

        // Verify all codes are still unique
        assertThat(codes.size()).isEqualTo(50000);
    }

    @Test
    void latency_shouldMeetRequirements() {
        int warmupIterations = 1000;
        int testIterations = 10000;

        // Warmup
        for (int i = 0; i < warmupIterations; i++) {
            codeGenerator.generate("https://example.com/warmup" + i, (long) i, 0);
            ShortLinkShardUtils.resolveTable("warmup_code_" + i, weightMap);
        }

        // Measure code generation latency
        List<Long> codeGenLatencies = new ArrayList<>();
        for (int i = 0; i < testIterations; i++) {
            long start = System.nanoTime();
            codeGenerator.generate("https://example.com/latency-test" + i, (long) i, 0);
            long end = System.nanoTime();
            codeGenLatencies.add(end - start);
        }

        // Measure sharding latency
        List<Long> shardingLatencies = new ArrayList<>();
        for (int i = 0; i < testIterations; i++) {
            long start = System.nanoTime();
            ShortLinkShardUtils.resolveTable("latency_test_" + i, weightMap);
            long end = System.nanoTime();
            shardingLatencies.add(end - start);
        }

        // Calculate statistics
        double avgCodeGenLatency = codeGenLatencies.stream().mapToLong(Long::longValue).average().orElse(0);
        double avgShardingLatency = shardingLatencies.stream().mapToLong(Long::longValue).average().orElse(0);

        Collections.sort(codeGenLatencies);
        Collections.sort(shardingLatencies);

        long p95CodeGen = codeGenLatencies.get((int) (testIterations * 0.95));
        long p95Sharding = shardingLatencies.get((int) (testIterations * 0.95));

        System.out.println("Code generation - Avg: " + (avgCodeGenLatency / 1000) + "μs, P95: " + (p95CodeGen / 1000) + "μs");
        System.out.println("Sharding - Avg: " + (avgShardingLatency / 1000) + "μs, P95: " + (p95Sharding / 1000) + "μs");

        // Latency requirements (P95 should be under 100μs for both operations)
        assertThat(p95CodeGen).isLessThan(100_000); // 100μs in nanoseconds
        assertThat(p95Sharding).isLessThan(100_000);  // 100μs in nanoseconds
    }

    @Test
    void scalability_shouldHandleLargeVolumes() {
        // Test with different load levels to verify scalability
        int[] loadLevels = {1000, 5000, 10000, 25000};

        for (int targetOps : loadLevels) {
            Instant start = Instant.now();

            Set<String> codes = new HashSet<>();
            for (int i = 0; i < targetOps; i++) {
                String code = codeGenerator.generate("https://example.com/scale-test-" + targetOps + "-" + i, (long) i, 0);
                codes.add(code);

                // Also test sharding
                ShortLinkShardUtils.resolveTable(code, weightMap);
            }

            Duration elapsed = Duration.between(start, Instant.now());
            double throughput = targetOps / (elapsed.toMillis() / 1000.0);

            System.out.println("Load " + targetOps + ": " + throughput + " ops/second");

            // Verify uniqueness maintained at scale
            assertThat(codes.size()).isEqualTo(targetOps);

            // Throughput should not degrade significantly with increased load
            assertThat(throughput).isGreaterThan(500); // Minimum acceptable throughput
        }
    }
}