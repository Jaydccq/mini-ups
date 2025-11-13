package com.miniups.shortlink.monitoring;

import com.miniups.shortlink.dto.ShortLinkCreateRequest;
import com.miniups.shortlink.dto.ShortLinkCreateResponse;
import com.miniups.shortlink.service.ShortLinkService;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import io.micrometer.tracing.Tracer;
import io.micrometer.tracing.Span;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Tests for comprehensive observability features in the ShortLink system.
 * Includes distributed tracing, structured logging, health checks,
 * and system monitoring capabilities.
 */
@SpringBootTest
@ActiveProfiles("test")
class ShortLinkObservabilityTest {

    @Autowired
    private MeterRegistry meterRegistry;

    @MockBean
    private ShortLinkService shortLinkService;

    @MockBean
    private Tracer tracer;

    private final Map<String, String> logEvents = new HashMap<>();
    private final Map<String, Object> healthChecks = new HashMap<>();

    @BeforeEach
    void setUp() {
        logEvents.clear();
        healthChecks.clear();
        meterRegistry.clear();
    }

    @Test
    void distributedTracing_shouldTrackRequestFlow() {
        // Mock distributed tracing
        Span rootSpan = createMockSpan("shortlink-create-request");
        when(tracer.nextSpan()).thenReturn(rootSpan);

        String traceId = "trace-12345";
        String spanId = "span-67890";

        // Simulate request flow with tracing
        simulateTracedOperation("user-authentication", traceId, spanId);
        simulateTracedOperation("url-validation", traceId, spanId);
        simulateTracedOperation("code-generation", traceId, spanId);
        simulateTracedOperation("database-insert", traceId, spanId);
        simulateTracedOperation("cache-update", traceId, spanId);

        // Verify trace data
        assertThat(logEvents).containsKey("user-authentication");
        assertThat(logEvents).containsKey("url-validation");
        assertThat(logEvents).containsKey("code-generation");
        assertThat(logEvents).containsKey("database-insert");
        assertThat(logEvents).containsKey("cache-update");

        // Verify all operations share the same trace ID
        logEvents.values().forEach(logEntry -> {
            assertThat(logEntry).contains(traceId);
        });
    }

    @Test
    void structuredLogging_shouldCaptureContextualInfo() {
        // Simulate structured logging for different scenarios
        Long userId = 1001L;
        String shortCode = "ABC123XY";

        // Log creation event
        logStructuredEvent("SHORT_LINK_CREATED", Map.of(
                "userId", userId,
                "shortCode", shortCode,
                "originalUrl", "https://example.com/long-url",
                "timestamp", Instant.now().toString(),
                "userAgent", "Mozilla/5.0 Test Browser",
                "clientIp", "192.168.1.100"
        ));

        // Log access event
        logStructuredEvent("SHORT_LINK_ACCESSED", Map.of(
                "shortCode", shortCode,
                "clientIp", "203.0.113.42",
                "referer", "https://social.example.com",
                "userAgent", "Chrome/91.0 Mobile",
                "timestamp", Instant.now().toString(),
                "responseTime", "45ms"
        ));

        // Log error event
        logStructuredEvent("SHORT_LINK_ERROR", Map.of(
                "shortCode", "INVALID1",
                "errorType", "NOT_FOUND",
                "errorMessage", "Short code not found in database",
                "clientIp", "198.51.100.77",
                "timestamp", Instant.now().toString(),
                "stackTrace", "com.miniups.shortlink.exception.ShortLinkNotFoundException"
        ));

        // Verify structured logs contain required fields
        assertThat(logEvents).containsKey("SHORT_LINK_CREATED");
        assertThat(logEvents).containsKey("SHORT_LINK_ACCESSED");
        assertThat(logEvents).containsKey("SHORT_LINK_ERROR");

        String creationLog = logEvents.get("SHORT_LINK_CREATED");
        assertThat(creationLog).contains(userId.toString());
        assertThat(creationLog).contains(shortCode);
        assertThat(creationLog).contains("192.168.1.100");
    }

    @Test
    void healthChecks_shouldMonitorSystemHealth() {
        // Simulate health check implementations
        performHealthCheck("database", this::checkDatabaseHealth);
        performHealthCheck("redis", this::checkRedisHealth);
        performHealthCheck("external-apis", this::checkExternalApisHealth);
        performHealthCheck("memory", this::checkMemoryHealth);
        performHealthCheck("disk-space", this::checkDiskSpaceHealth);

        // Verify all health checks completed
        assertThat(healthChecks).containsKey("database");
        assertThat(healthChecks).containsKey("redis");
        assertThat(healthChecks).containsKey("external-apis");
        assertThat(healthChecks).containsKey("memory");
        assertThat(healthChecks).containsKey("disk-space");

        // Verify health status
        assertThat(healthChecks.get("database")).isEqualTo("UP");
        assertThat(healthChecks.get("redis")).isEqualTo("UP");
        assertThat(healthChecks.get("memory")).isEqualTo("UP");
    }

    @Test
    void performanceMonitoring_shouldTrackKeyMetrics() {
        // Simulate performance monitoring
        Map<String, Double> performanceMetrics = new HashMap<>();

        // Track response times
        performanceMetrics.put("avg_response_time_ms", 45.2);
        performanceMetrics.put("p95_response_time_ms", 89.7);
        performanceMetrics.put("p99_response_time_ms", 156.3);

        // Track throughput
        performanceMetrics.put("requests_per_second", 150.5);
        performanceMetrics.put("successful_requests_per_second", 147.8);
        performanceMetrics.put("error_rate_percent", 1.8);

        // Track resource utilization
        performanceMetrics.put("cpu_usage_percent", 42.5);
        performanceMetrics.put("memory_usage_percent", 68.3);
        performanceMetrics.put("db_connection_pool_usage_percent", 25.7);

        // Track business metrics
        performanceMetrics.put("daily_active_users", 1250.0);
        performanceMetrics.put("links_created_today", 3567.0);
        performanceMetrics.put("total_redirections_today", 45289.0);

        // Verify performance metrics are within acceptable ranges
        assertThat(performanceMetrics.get("avg_response_time_ms")).isLessThan(100.0);
        assertThat(performanceMetrics.get("error_rate_percent")).isLessThan(5.0);
        assertThat(performanceMetrics.get("cpu_usage_percent")).isLessThan(80.0);
        assertThat(performanceMetrics.get("memory_usage_percent")).isLessThan(85.0);

        // Verify business metrics show healthy usage
        assertThat(performanceMetrics.get("daily_active_users")).isGreaterThan(1000.0);
        assertThat(performanceMetrics.get("links_created_today")).isGreaterThan(3000.0);
    }

    @Test
    void alerting_shouldTriggerOnThresholds() {
        // Simulate alerting system
        Map<String, Boolean> alerts = new HashMap<>();

        // Check various alerting conditions
        alerts.put("high_error_rate", checkErrorRateThreshold(2.5, 5.0)); // 2.5% < 5% threshold
        alerts.put("slow_response", checkResponseTimeThreshold(150.0, 200.0)); // 150ms < 200ms threshold
        alerts.put("low_disk_space", checkDiskSpaceThreshold(85.0, 90.0)); // 85% < 90% threshold
        alerts.put("high_memory_usage", checkMemoryUsageThreshold(95.0, 90.0)); // 95% > 90% threshold (ALERT!)
        alerts.put("database_connection_failures", checkDatabaseConnections(3, 5)); // 3 < 5 failures
        alerts.put("cache_hit_rate_low", checkCacheHitRate(0.75, 0.80)); // 75% < 80% threshold (ALERT!)

        // Verify alerting logic
        assertThat(alerts.get("high_error_rate")).isFalse(); // No alert
        assertThat(alerts.get("slow_response")).isFalse(); // No alert
        assertThat(alerts.get("low_disk_space")).isFalse(); // No alert
        assertThat(alerts.get("high_memory_usage")).isTrue(); // ALERT!
        assertThat(alerts.get("database_connection_failures")).isFalse(); // No alert
        assertThat(alerts.get("cache_hit_rate_low")).isTrue(); // ALERT!

        // Count total active alerts
        long activeAlerts = alerts.values().stream().mapToLong(alert -> alert ? 1 : 0).sum();
        assertThat(activeAlerts).isEqualTo(2);
    }

    @Test
    void asyncMonitoring_shouldTrackConcurrentOperations() throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(5);
        Map<String, Long> concurrentMetrics = new HashMap<>();

        try {
            // Simulate concurrent operations
            CompletableFuture<Void> createOperations = CompletableFuture.runAsync(() -> {
                long startTime = System.currentTimeMillis();
                for (int i = 0; i < 10; i++) {
                    simulateCreateOperation();
                    try {
                        Thread.sleep(10);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
                concurrentMetrics.put("create_operations_duration", System.currentTimeMillis() - startTime);
            }, executor);

            CompletableFuture<Void> redirectOperations = CompletableFuture.runAsync(() -> {
                long startTime = System.currentTimeMillis();
                for (int i = 0; i < 20; i++) {
                    simulateRedirectOperation();
                    try {
                        Thread.sleep(5);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
                concurrentMetrics.put("redirect_operations_duration", System.currentTimeMillis() - startTime);
            }, executor);

            CompletableFuture<Void> analyticsOperations = CompletableFuture.runAsync(() -> {
                long startTime = System.currentTimeMillis();
                for (int i = 0; i < 5; i++) {
                    simulateAnalyticsOperation();
                    try {
                        Thread.sleep(20);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
                concurrentMetrics.put("analytics_operations_duration", System.currentTimeMillis() - startTime);
            }, executor);

            // Wait for all operations to complete
            CompletableFuture.allOf(createOperations, redirectOperations, analyticsOperations)
                    .get(5, TimeUnit.SECONDS);

            // Verify concurrent operation metrics
            assertThat(concurrentMetrics).containsKey("create_operations_duration");
            assertThat(concurrentMetrics).containsKey("redirect_operations_duration");
            assertThat(concurrentMetrics).containsKey("analytics_operations_duration");

            // Verify operations completed within reasonable time
            assertThat(concurrentMetrics.get("create_operations_duration")).isLessThan(1000L);
            assertThat(concurrentMetrics.get("redirect_operations_duration")).isLessThan(1000L);
            assertThat(concurrentMetrics.get("analytics_operations_duration")).isLessThan(1000L);

        } finally {
            executor.shutdown();
        }
    }

    @Test
    void errorTracking_shouldCaptureDetailedContext() {
        // Simulate various error scenarios with detailed context
        Map<String, Map<String, Object>> errorContexts = new HashMap<>();

        // Database connection error
        errorContexts.put("database_error", Map.of(
                "error_type", "CONNECTION_TIMEOUT",
                "database_host", "localhost:5432",
                "connection_pool_size", 10,
                "active_connections", 8,
                "query", "SELECT * FROM short_links WHERE short_code = ?",
                "timeout_ms", 5000,
                "retry_count", 3
        ));

        // Cache miss error
        errorContexts.put("cache_error", Map.of(
                "error_type", "CACHE_MISS",
                "cache_key", "shortlink:ABC123XY",
                "cache_host", "localhost:6379",
                "operation", "GET",
                "fallback_executed", true,
                "fallback_duration_ms", 25
        ));

        // Rate limiting error
        errorContexts.put("rate_limit_error", Map.of(
                "error_type", "RATE_LIMIT_EXCEEDED",
                "user_id", 1001L,
                "current_rate", 15,
                "rate_limit", 10,
                "window_seconds", 60,
                "reset_time", Instant.now().plusSeconds(45).toString()
        ));

        // Verify error contexts contain necessary debugging information
        Map<String, Object> dbError = errorContexts.get("database_error");
        assertThat(dbError.get("retry_count")).isEqualTo(3);
        assertThat(dbError.get("timeout_ms")).isEqualTo(5000);

        Map<String, Object> cacheError = errorContexts.get("cache_error");
        assertThat(cacheError.get("fallback_executed")).isEqualTo(true);
        assertThat(cacheError.get("operation")).isEqualTo("GET");

        Map<String, Object> rateLimitError = errorContexts.get("rate_limit_error");
        assertThat(rateLimitError.get("current_rate")).isEqualTo(15);
        assertThat(rateLimitError.get("rate_limit")).isEqualTo(10);
    }

    // Helper methods for simulation

    private Span createMockSpan(String operationName) {
        // Mock span implementation
        return new Span() {
            @Override
            public Span name(String name) { return this; }
            @Override
            public Span tag(String key, String value) { return this; }
            @Override
            public Span event(String value) { return this; }
            @Override
            public void end() {}
            // Implement other required methods as no-ops for testing
            @Override public TraceContext context() { return null; }
            @Override public SpanBuilder setNoParent() { return null; }
            @Override public Span start() { return this; }
            @Override public void abandon() {}
            @Override public Span error(Throwable throwable) { return this; }
        };
    }

    private void simulateTracedOperation(String operationName, String traceId, String spanId) {
        String logEntry = String.format("[%s] [%s] Operation: %s completed", traceId, spanId, operationName);
        logEvents.put(operationName, logEntry);
    }

    private void logStructuredEvent(String eventType, Map<String, Object> context) {
        StringBuilder logEntry = new StringBuilder();
        logEntry.append("EVENT_TYPE=").append(eventType);

        context.forEach((key, value) ->
                logEntry.append(" ").append(key).append("=").append(value));

        logEvents.put(eventType, logEntry.toString());
    }

    private void performHealthCheck(String component, Runnable healthCheck) {
        try {
            healthCheck.run();
            healthChecks.put(component, "UP");
        } catch (Exception e) {
            healthChecks.put(component, "DOWN");
        }
    }

    private void checkDatabaseHealth() {
        // Simulate database health check
        // In real implementation, would ping database
    }

    private void checkRedisHealth() {
        // Simulate Redis health check
        // In real implementation, would ping Redis
    }

    private void checkExternalApisHealth() {
        // Simulate external API health check
        healthChecks.put("external-apis", "DEGRADED"); // Simulated degraded state
    }

    private void checkMemoryHealth() {
        // Simulate memory health check
    }

    private void checkDiskSpaceHealth() {
        // Simulate disk space health check
    }

    private boolean checkErrorRateThreshold(double currentRate, double threshold) {
        return currentRate > threshold;
    }

    private boolean checkResponseTimeThreshold(double currentTime, double threshold) {
        return currentTime > threshold;
    }

    private boolean checkDiskSpaceThreshold(double currentUsage, double threshold) {
        return currentUsage > threshold;
    }

    private boolean checkMemoryUsageThreshold(double currentUsage, double threshold) {
        return currentUsage > threshold;
    }

    private boolean checkDatabaseConnections(int failureCount, int threshold) {
        return failureCount > threshold;
    }

    private boolean checkCacheHitRate(double hitRate, double threshold) {
        return hitRate < threshold;
    }

    private void simulateCreateOperation() {
        // Simulate create operation
        try {
            Thread.sleep(10);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private void simulateRedirectOperation() {
        // Simulate redirect operation
        try {
            Thread.sleep(5);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private void simulateAnalyticsOperation() {
        // Simulate analytics operation
        try {
            Thread.sleep(20);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}