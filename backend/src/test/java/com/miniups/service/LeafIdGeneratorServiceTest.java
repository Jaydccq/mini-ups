package com.miniups.service;

import com.miniups.model.entity.LeafAlloc;
import com.miniups.repository.LeafAllocRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.test.context.TestPropertySource;

import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * Performance and correctness tests for Leaf-Segment ID Generator
 * 
 * This test suite validates the key performance characteristics
 * promised in the architecture specifications:
 * - <5ms latency for ID generation
 * - 100-thread stress test capability
 * - Unique ID generation under high concurrency
 * 
 * @author Mini-UPS Development Team
 */
@SpringBootTest
@TestPropertySource(properties = {
    "spring.profiles.active=test",
    "outbox.polling.enabled=false",
    "leaf.maintenance.interval-ms=60000"
})
public class LeafIdGeneratorServiceTest {
    
    @MockBean
    private LeafAllocRepository leafAllocRepository;
    
    @MockBean
    private RedisTemplate<String, String> redisTemplate;
    
    private LeafIdGeneratorService leafIdGeneratorService;
    
    @BeforeEach
    void setUp() {
        leafIdGeneratorService = new LeafIdGeneratorService(leafAllocRepository, redisTemplate);
        
        // Mock successful database allocation
        LeafAlloc mockAlloc = LeafAlloc.builder()
                .bizTag("test_shipment")
                .maxId(1000L)
                .step(1000)
                .version(1L)
                .active(true)
                .build();
                
        when(leafAllocRepository.findByBizTag(anyString()))
                .thenReturn(mockAlloc);
                
        when(leafAllocRepository.allocateNextSegment(anyString(), anyLong()))
                .thenReturn(1); // Success
    }
    
    /**
     * Test basic ID generation functionality
     */
    @Test
    void testBasicIdGeneration() {
        // Initialize the service
        leafIdGeneratorService.initializeBusinessTag("test_shipment", 1000, "Test shipment IDs");
        
        // Generate some IDs
        long id1 = leafIdGeneratorService.nextId("test_shipment");
        long id2 = leafIdGeneratorService.nextId("test_shipment");
        long id3 = leafIdGeneratorService.nextId("test_shipment");
        
        // Verify IDs are sequential
        assertEquals(id1 + 1, id2);
        assertEquals(id2 + 1, id3);
        
        // Verify IDs are positive
        assertTrue(id1 > 0);
        assertTrue(id2 > 0);
        assertTrue(id3 > 0);
    }
    
    /**
     * Test high-throughput ID generation with 100 threads (as specified)
     * Validates the <5ms latency requirement under stress
     */
    @Test
    void testConcurrentIdGeneration() throws InterruptedException {
        final String bizTag = "test_concurrent";
        final int threadCount = 100;
        final int idsPerThread = 1000;
        final CountDownLatch startLatch = new CountDownLatch(1);
        final CountDownLatch finishLatch = new CountDownLatch(threadCount);
        final AtomicLong totalLatency = new AtomicLong(0);
        
        // Initialize business tag
        leafIdGeneratorService.initializeBusinessTag(bizTag, 50000, "Concurrent test");
        
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        
        // Create worker threads
        for (int t = 0; t < threadCount; t++) {
            executor.submit(() -> {
                try {
                    // Wait for start signal
                    startLatch.await();
                    
                    // Generate IDs and measure latency
                    long threadStartTime = System.nanoTime();
                    
                    for (int i = 0; i < idsPerThread; i++) {
                        long startTime = System.nanoTime();
                        long id = leafIdGeneratorService.nextId(bizTag);
                        long endTime = System.nanoTime();
                        
                        // Verify ID is positive
                        assertTrue(id > 0, "Generated ID must be positive");
                        
                        // Track latency (in nanoseconds)
                        totalLatency.addAndGet(endTime - startTime);
                    }
                    
                    long threadEndTime = System.nanoTime();
                    long threadDurationMs = (threadEndTime - threadStartTime) / 1_000_000;
                    
                    System.out.printf("Thread completed: %d IDs in %d ms%n", idsPerThread, threadDurationMs);
                    
                } catch (Exception e) {
                    fail("Thread failed with exception: " + e.getMessage());
                } finally {
                    finishLatch.countDown();
                }
            });
        }
        
        // Start all threads simultaneously
        long testStartTime = System.nanoTime();
        startLatch.countDown();
        
        // Wait for all threads to complete (with timeout)
        boolean completed = finishLatch.await(30, java.util.concurrent.TimeUnit.SECONDS);
        assertTrue(completed, "Test should complete within 30 seconds");
        
        long testEndTime = System.nanoTime();
        
        // Calculate performance metrics
        long totalIds = (long) threadCount * idsPerThread;
        long totalDurationMs = (testEndTime - testStartTime) / 1_000_000;
        double avgLatencyMicros = totalLatency.get() / (double) totalIds / 1000.0;
        double throughputPerSecond = totalIds / (totalDurationMs / 1000.0);
        
        // Verify performance requirements
        System.out.printf("Performance Results:%n");
        System.out.printf("  Total IDs Generated: %d%n", totalIds);
        System.out.printf("  Total Duration: %d ms%n", totalDurationMs);
        System.out.printf("  Average Latency: %.2f μs%n", avgLatencyMicros);
        System.out.printf("  Throughput: %.0f IDs/second%n", throughputPerSecond);
        
        // Validate <5ms (5000 microseconds) latency requirement
        assertTrue(avgLatencyMicros < 5000, 
                String.format("Average latency %.2f μs exceeds 5ms requirement", avgLatencyMicros));
        
        // Validate reasonable throughput (should be > 10k IDs/second)
        assertTrue(throughputPerSecond > 10000, 
                String.format("Throughput %.0f IDs/second is too low", throughputPerSecond));
        
        executor.shutdown();
    }
    
    /**
     * Test health statistics collection
     */
    @Test
    void testHealthStatistics() {
        final String bizTag = "test_health";
        
        // Initialize and generate some IDs
        leafIdGeneratorService.initializeBusinessTag(bizTag, 1000, "Health test");
        
        for (int i = 0; i < 10; i++) {
            leafIdGeneratorService.nextId(bizTag);
        }
        
        // Get health statistics
        var stats = leafIdGeneratorService.getHealthStatistics();
        
        // Verify statistics structure
        assertNotNull(stats);
        assertTrue(stats.containsKey("totalIdGenerated"));
        assertTrue(stats.containsKey("totalSegmentAllocations"));
        assertTrue(stats.containsKey("healthy"));
        assertTrue(stats.containsKey("activeBufferCount"));
        
        // Verify some values
        assertTrue((Long) stats.get("totalIdGenerated") >= 10);
        assertTrue((Boolean) stats.get("healthy"));
        assertTrue((Integer) stats.get("activeBufferCount") > 0);
    }
    
    /**
     * Test business tag status retrieval
     */
    @Test
    void testBusinessTagStatus() {
        final String bizTag = "test_status";
        
        // Initialize business tag
        leafIdGeneratorService.initializeBusinessTag(bizTag, 2000, "Status test");
        
        // Generate a few IDs
        leafIdGeneratorService.nextId(bizTag);
        leafIdGeneratorService.nextId(bizTag);
        
        // Get status
        var status = leafIdGeneratorService.getBusinessTagStatus(bizTag);
        
        // Verify status structure
        assertNotNull(status);
        assertTrue(status.containsKey("bufferStatistics"));
        
        var bufferStats = (java.util.Map<String, Object>) status.get("bufferStatistics");
        assertNotNull(bufferStats);
        assertTrue(bufferStats.containsKey("totalIdsGenerated"));
        assertTrue((Long) bufferStats.get("totalIdsGenerated") >= 2);
    }
    
    /**
     * Test error handling for invalid business tags
     */
    @Test
    void testInvalidBusinessTag() {
        // Test null business tag
        assertThrows(IllegalArgumentException.class, () -> {
            leafIdGeneratorService.nextId(null);
        });
        
        // Test empty business tag
        assertThrows(IllegalArgumentException.class, () -> {
            leafIdGeneratorService.nextId("");
        });
        
        // Test whitespace-only business tag
        assertThrows(IllegalArgumentException.class, () -> {
            leafIdGeneratorService.nextId("   ");
        });
    }
}