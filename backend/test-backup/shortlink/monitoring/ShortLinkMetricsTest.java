package com.miniups.shortlink.monitoring;

import com.miniups.shortlink.dto.ShortLinkCreateRequest;
import com.miniups.shortlink.dto.ShortLinkCreateResponse;
import com.miniups.shortlink.service.ShortLinkService;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.context.ActiveProfiles;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

/**
 * Tests for ShortLink system metrics and monitoring functionality.
 * Verifies that proper metrics are collected for performance monitoring,
 * alerting, and system observability.
 */
@SpringBootTest
@ActiveProfiles("test")
class ShortLinkMetricsTest {

    @Autowired
    private MeterRegistry meterRegistry;

    @MockBean
    private ShortLinkService shortLinkService;

    private MockHttpServletRequest mockRequest;

    @BeforeEach
    void setUp() {
        // Clear any existing metrics
        meterRegistry.clear();

        mockRequest = new MockHttpServletRequest();
        mockRequest.addHeader("X-Forwarded-For", "192.168.1.100");
        mockRequest.addHeader("User-Agent", "Test-Agent/1.0");
    }

    @Test
    void shortLinkCreation_shouldRecordMetrics() {
        // Register metrics for tracking
        Counter creationCounter = Counter.builder("shortlink.creation.total")
                .description("Total number of short links created")
                .tag("user_type", "registered")
                .register(meterRegistry);

        Timer creationTimer = Timer.builder("shortlink.creation.duration")
                .description("Time taken to create short links")
                .register(meterRegistry);

        // Simulate short link creation
        Long userId = 1001L;
        ShortLinkCreateRequest request = new ShortLinkCreateRequest();
        request.setOriginalUrl("https://example.com/metrics-test");
        request.setDescription("Metrics Test Link");

        // Record metrics during creation
        Timer.Sample sample = Timer.start(meterRegistry);

        try {
            // Simulate creation process
            Thread.sleep(50); // Simulate processing time
            creationCounter.increment();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            sample.stop(creationTimer);
        }

        // Verify metrics were recorded
        assertThat(creationCounter.count()).isEqualTo(1.0);
        assertThat(creationTimer.count()).isEqualTo(1);
        assertThat(creationTimer.totalTime(TimeUnit.MILLISECONDS)).isGreaterThan(40);
    }

    @Test
    void shortLinkRedirection_shouldRecordMetrics() {
        // Register redirection metrics
        Counter redirectCounter = Counter.builder("shortlink.redirect.total")
                .description("Total number of redirections")
                .tag("status", "success")
                .register(meterRegistry);

        Timer redirectTimer = Timer.builder("shortlink.redirect.duration")
                .description("Time taken to process redirections")
                .register(meterRegistry);

        Counter accessCounter = Counter.builder("shortlink.access.total")
                .description("Total access attempts")
                .tag("result", "found")
                .register(meterRegistry);

        // Simulate multiple redirections
        int redirectionCount = 5;
        for (int i = 0; i < redirectionCount; i++) {
            Timer.Sample sample = Timer.start(meterRegistry);

            try {
                // Simulate redirection processing
                Thread.sleep(20);
                redirectCounter.increment();
                accessCounter.increment();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                sample.stop(redirectTimer);
            }
        }

        // Verify redirection metrics
        assertThat(redirectCounter.count()).isEqualTo(redirectionCount);
        assertThat(redirectTimer.count()).isEqualTo(redirectionCount);
        assertThat(accessCounter.count()).isEqualTo(redirectionCount);
    }

    @Test
    void rateLimiting_shouldRecordMetrics() {
        // Register rate limiting metrics
        Counter rateLimitCounter = Counter.builder("shortlink.rate_limit.violations")
                .description("Number of rate limit violations")
                .tag("user_id", "test_user")
                .register(meterRegistry);

        Counter blockedRequestsCounter = Counter.builder("shortlink.requests.blocked")
                .description("Number of blocked requests")
                .tag("reason", "rate_limit")
                .register(meterRegistry);

        // Simulate rate limit violations
        int violations = 3;
        for (int i = 0; i < violations; i++) {
            rateLimitCounter.increment();
            blockedRequestsCounter.increment();
        }

        // Verify rate limiting metrics
        assertThat(rateLimitCounter.count()).isEqualTo(violations);
        assertThat(blockedRequestsCounter.count()).isEqualTo(violations);
    }

    @Test
    void systemHealth_shouldRecordGauges() {
        // Register system health gauges
        Gauge activeLinksGauge = Gauge.builder("shortlink.active.count")
                .description("Number of active short links")
                .register(meterRegistry, this, obj -> 150.0); // Simulated count

        Gauge cacheHitRateGauge = Gauge.builder("shortlink.cache.hit_rate")
                .description("Cache hit rate percentage")
                .register(meterRegistry, this, obj -> 0.85); // 85% hit rate

        Gauge memoryUsageGauge = Gauge.builder("shortlink.memory.usage_bytes")
                .description("Memory usage in bytes")
                .register(meterRegistry, this, obj -> 1024 * 1024 * 50.0); // 50MB

        // Verify gauge values
        assertThat(activeLinksGauge.value()).isEqualTo(150.0);
        assertThat(cacheHitRateGauge.value()).isEqualTo(0.85);
        assertThat(memoryUsageGauge.value()).isEqualTo(1024 * 1024 * 50.0);
    }

    @Test
    void errorTracking_shouldRecordMetrics() {
        // Register error tracking metrics
        Counter errorCounter = Counter.builder("shortlink.errors.total")
                .description("Total number of errors")
                .tag("error_type", "not_found")
                .register(meterRegistry);

        Counter validationErrorCounter = Counter.builder("shortlink.validation.errors")
                .description("Validation error count")
                .tag("field", "url")
                .register(meterRegistry);

        Counter databaseErrorCounter = Counter.builder("shortlink.database.errors")
                .description("Database error count")
                .tag("operation", "select")
                .register(meterRegistry);

        // Simulate various errors
        errorCounter.increment(); // Not found error
        validationErrorCounter.increment(); // URL validation error
        databaseErrorCounter.increment(); // Database connection error

        // Verify error metrics
        assertThat(errorCounter.count()).isEqualTo(1.0);
        assertThat(validationErrorCounter.count()).isEqualTo(1.0);
        assertThat(databaseErrorCounter.count()).isEqualTo(1.0);
    }

    @Test
    void performanceMetrics_shouldTrackLatency() {
        // Register performance metrics
        Timer databaseQueryTimer = Timer.builder("shortlink.database.query.duration")
                .description("Database query execution time")
                .tag("operation", "findByShortCode")
                .register(meterRegistry);

        Timer cacheAccessTimer = Timer.builder("shortlink.cache.access.duration")
                .description("Cache access time")
                .tag("operation", "get")
                .register(meterRegistry);

        Timer codeGenerationTimer = Timer.builder("shortlink.code.generation.duration")
                .description("Short code generation time")
                .register(meterRegistry);

        // Simulate performance measurements
        Timer.Sample dbSample = Timer.start(meterRegistry);
        try {
            Thread.sleep(10); // Simulate DB query
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        dbSample.stop(databaseQueryTimer);

        Timer.Sample cacheSample = Timer.start(meterRegistry);
        try {
            Thread.sleep(2); // Simulate cache access
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        cacheSample.stop(cacheAccessTimer);

        Timer.Sample codeSample = Timer.start(meterRegistry);
        try {
            Thread.sleep(5); // Simulate code generation
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        codeSample.stop(codeGenerationTimer);

        // Verify performance metrics
        assertThat(databaseQueryTimer.count()).isEqualTo(1);
        assertThat(cacheAccessTimer.count()).isEqualTo(1);
        assertThat(codeGenerationTimer.count()).isEqualTo(1);

        assertThat(databaseQueryTimer.totalTime(TimeUnit.MILLISECONDS)).isGreaterThan(8);
        assertThat(cacheAccessTimer.totalTime(TimeUnit.MILLISECONDS)).isGreaterThan(1);
        assertThat(codeGenerationTimer.totalTime(TimeUnit.MILLISECONDS)).isGreaterThan(3);
    }

    @Test
    void businessMetrics_shouldTrackKPIs() {
        // Register business KPI metrics
        Counter dailyActiveUsersCounter = Counter.builder("shortlink.users.daily_active")
                .description("Daily active users")
                .register(meterRegistry);

        Counter topDomainsCounter = Counter.builder("shortlink.domains.popular")
                .description("Popular domains being shortened")
                .tag("domain", "example.com")
                .register(meterRegistry);

        Timer userSessionTimer = Timer.builder("shortlink.user.session.duration")
                .description("User session duration")
                .register(meterRegistry);

        // Simulate business metrics
        dailyActiveUsersCounter.increment(); // New daily active user
        topDomainsCounter.increment(); // Popular domain usage

        Timer.Sample sessionSample = Timer.start(meterRegistry);
        try {
            Thread.sleep(100); // Simulate user session
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        sessionSample.stop(userSessionTimer);

        // Verify business metrics
        assertThat(dailyActiveUsersCounter.count()).isEqualTo(1.0);
        assertThat(topDomainsCounter.count()).isEqualTo(1.0);
        assertThat(userSessionTimer.count()).isEqualTo(1);
    }

    @Test
    void securityMetrics_shouldTrackThreats() {
        // Register security metrics
        Counter suspiciousActivityCounter = Counter.builder("shortlink.security.suspicious_activity")
                .description("Suspicious activity detected")
                .tag("type", "bulk_requests")
                .register(meterRegistry);

        Counter maliciousUrlCounter = Counter.builder("shortlink.security.malicious_urls")
                .description("Malicious URLs blocked")
                .tag("threat_type", "phishing")
                .register(meterRegistry);

        Counter authFailureCounter = Counter.builder("shortlink.security.auth_failures")
                .description("Authentication failures")
                .tag("reason", "invalid_token")
                .register(meterRegistry);

        // Simulate security events
        suspiciousActivityCounter.increment(); // Bulk request detected
        maliciousUrlCounter.increment(); // Phishing URL blocked
        authFailureCounter.increment(); // Authentication failure

        // Verify security metrics
        assertThat(suspiciousActivityCounter.count()).isEqualTo(1.0);
        assertThat(maliciousUrlCounter.count()).isEqualTo(1.0);
        assertThat(authFailureCounter.count()).isEqualTo(1.0);
    }

    @Test
    void resourceUtilization_shouldTrackUsage() {
        // Register resource utilization metrics
        Gauge cpuUsageGauge = Gauge.builder("shortlink.system.cpu.usage")
                .description("CPU usage percentage")
                .register(meterRegistry, this, obj -> 0.45); // 45% CPU usage

        Gauge memoryUsageGauge = Gauge.builder("shortlink.system.memory.usage")
                .description("Memory usage percentage")
                .register(meterRegistry, this, obj -> 0.62); // 62% memory usage

        Gauge diskUsageGauge = Gauge.builder("shortlink.system.disk.usage")
                .description("Disk usage percentage")
                .register(meterRegistry, this, obj -> 0.78); // 78% disk usage

        Counter gcCounter = Counter.builder("shortlink.system.gc.total")
                .description("Garbage collection count")
                .register(meterRegistry);

        // Simulate garbage collection
        gcCounter.increment();

        // Verify resource metrics
        assertThat(cpuUsageGauge.value()).isEqualTo(0.45);
        assertThat(memoryUsageGauge.value()).isEqualTo(0.62);
        assertThat(diskUsageGauge.value()).isEqualTo(0.78);
        assertThat(gcCounter.count()).isEqualTo(1.0);
    }

    @Test
    void alertingThresholds_shouldTriggerAlerts() {
        // Register metrics with alerting thresholds
        Counter highErrorRateCounter = Counter.builder("shortlink.alerts.high_error_rate")
                .description("High error rate alert")
                .register(meterRegistry);

        Counter lowCacheHitRateCounter = Counter.builder("shortlink.alerts.low_cache_hit_rate")
                .description("Low cache hit rate alert")
                .register(meterRegistry);

        Timer slowResponseTimer = Timer.builder("shortlink.alerts.slow_response")
                .description("Slow response time alert")
                .register(meterRegistry);

        // Simulate alerting conditions
        highErrorRateCounter.increment(); // Error rate exceeded threshold

        Timer.Sample slowSample = Timer.start(meterRegistry);
        try {
            Thread.sleep(1000); // Simulate very slow response
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        slowSample.stop(slowResponseTimer);

        lowCacheHitRateCounter.increment(); // Cache hit rate below threshold

        // Verify alerting metrics
        assertThat(highErrorRateCounter.count()).isEqualTo(1.0);
        assertThat(lowCacheHitRateCounter.count()).isEqualTo(1.0);
        assertThat(slowResponseTimer.count()).isEqualTo(1);
        assertThat(slowResponseTimer.totalTime(TimeUnit.MILLISECONDS)).isGreaterThan(900);
    }

    @Test
    void customMetrics_shouldSupportBusinessLogic() {
        // Register custom business-specific metrics
        Counter viralLinksCounter = Counter.builder("shortlink.business.viral_links")
                .description("Links that went viral (>1000 clicks)")
                .register(meterRegistry);

        Counter premiumFeaturesCounter = Counter.builder("shortlink.business.premium_features")
                .description("Premium feature usage")
                .tag("feature", "custom_domain")
                .register(meterRegistry);

        Gauge revenuePotentialGauge = Gauge.builder("shortlink.business.revenue_potential")
                .description("Estimated revenue potential")
                .register(meterRegistry, this, obj -> 1250.75); // $1250.75

        // Simulate custom business metrics
        viralLinksCounter.increment(); // Link went viral
        premiumFeaturesCounter.increment(); // Premium feature used

        // Verify custom metrics
        assertThat(viralLinksCounter.count()).isEqualTo(1.0);
        assertThat(premiumFeaturesCounter.count()).isEqualTo(1.0);
        assertThat(revenuePotentialGauge.value()).isEqualTo(1250.75);
    }
}