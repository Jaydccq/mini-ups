package com.miniups.config;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * SLO (Service Level Objectives) Metrics Configuration
 *
 * Provides comprehensive SLO monitoring and tracking for Mini-UPS services.
 *
 * Key SLOs monitored:
 * - Response Time: P99 < 200ms for API endpoints
 * - Error Rate: < 1% of all requests
 * - Availability: > 99.9% uptime
 * - Throughput: Support 15K+ QPS
 *
 * RED Metrics Pattern Implementation:
 * - Rate: Request rate per endpoint
 * - Errors: Error count and rate by type
 * - Duration: Latency distribution with percentiles
 *
 * @author Mini-UPS Development Team
 * @version 1.0
 * @since 2024-12-16
 */
@Component
@ConditionalOnProperty(name = "metrics.enabled", havingValue = "true", matchIfMissing = false)
public class SLOMetricsConfig {

    private static final Logger log = LoggerFactory.getLogger(SLOMetricsConfig.class);

    // SLO Thresholds
    public static final long P99_LATENCY_THRESHOLD_MS = 200;
    public static final double ERROR_RATE_THRESHOLD = 0.01; // 1%
    public static final double AVAILABILITY_THRESHOLD = 0.999; // 99.9%

    private final MeterRegistry meterRegistry;

    // SLO tracking counters
    private final AtomicLong totalRequests = new AtomicLong(0);
    private final AtomicLong successfulRequests = new AtomicLong(0);
    private final AtomicLong failedRequests = new AtomicLong(0);
    private final AtomicLong sloViolations = new AtomicLong(0);

    // RED Metrics - Rate
    private final Counter requestRateCounter;

    // RED Metrics - Errors
    private final Counter errorCounter4xx;
    private final Counter errorCounter5xx;
    private final Counter errorCounterTimeout;
    private final Counter errorCounterValidation;

    // RED Metrics - Duration
    private final Timer requestDurationTimer;

    // SLO Gauges
    private final AtomicLong currentP99LatencyMs = new AtomicLong(0);
    private final AtomicLong currentErrorRatePercent = new AtomicLong(0);

    public SLOMetricsConfig(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;

        // Initialize RED Metrics - Rate
        this.requestRateCounter = Counter.builder("miniups.red.requests.total")
                .description("Total number of requests (RED Rate metric)")
                .tag("service", "mini-ups")
                .register(meterRegistry);

        // Initialize RED Metrics - Errors
        this.errorCounter4xx = Counter.builder("miniups.red.errors.total")
                .description("Total number of 4xx errors")
                .tag("error_type", "client_error")
                .tag("status_class", "4xx")
                .register(meterRegistry);

        this.errorCounter5xx = Counter.builder("miniups.red.errors.total")
                .description("Total number of 5xx errors")
                .tag("error_type", "server_error")
                .tag("status_class", "5xx")
                .register(meterRegistry);

        this.errorCounterTimeout = Counter.builder("miniups.red.errors.total")
                .description("Total number of timeout errors")
                .tag("error_type", "timeout")
                .tag("status_class", "timeout")
                .register(meterRegistry);

        this.errorCounterValidation = Counter.builder("miniups.red.errors.total")
                .description("Total number of validation errors")
                .tag("error_type", "validation")
                .tag("status_class", "validation")
                .register(meterRegistry);

        // Initialize RED Metrics - Duration
        this.requestDurationTimer = Timer.builder("miniups.red.request.duration")
                .description("Request duration distribution (RED Duration metric)")
                .tag("service", "mini-ups")
                .register(meterRegistry);

        // Initialize SLO Gauges
        Gauge.builder("miniups.slo.p99.latency.ms", currentP99LatencyMs, AtomicLong::get)
                .description("Current P99 latency in milliseconds")
                .tag("slo_target", String.valueOf(P99_LATENCY_THRESHOLD_MS))
                .register(meterRegistry);

        Gauge.builder("miniups.slo.error.rate.percent", currentErrorRatePercent, AtomicLong::get)
                .description("Current error rate as percentage")
                .tag("slo_target", String.valueOf(ERROR_RATE_THRESHOLD * 100))
                .register(meterRegistry);

        Gauge.builder("miniups.slo.availability", this, obj -> obj.calculateAvailability())
                .description("Current availability percentage")
                .tag("slo_target", String.valueOf(AVAILABILITY_THRESHOLD * 100))
                .register(meterRegistry);

        // SLO Violation Counter
        Counter.builder("miniups.slo.violations.total")
                .description("Total SLO violations")
                .tag("slo_type", "all")
                .register(meterRegistry);

        log.info("SLO Metrics initialized - Thresholds: P99<{}ms, ErrorRate<{}%, Availability>{}%",
                P99_LATENCY_THRESHOLD_MS, ERROR_RATE_THRESHOLD * 100, AVAILABILITY_THRESHOLD * 100);
    }

    // ==================== RED METRICS RECORDING ====================

    /**
     * Record a request with its duration for RED metrics
     */
    public void recordRequest(long durationMs, boolean success, String errorType) {
        requestRateCounter.increment();
        requestDurationTimer.record(durationMs, TimeUnit.MILLISECONDS);
        totalRequests.incrementAndGet();

        if (success) {
            successfulRequests.incrementAndGet();
        } else {
            failedRequests.incrementAndGet();
            recordError(errorType);
        }

        // Check SLO violations
        if (durationMs > P99_LATENCY_THRESHOLD_MS) {
            recordSLOViolation("latency");
        }
    }

    /**
     * Record an error by type
     */
    public void recordError(String errorType) {
        if (errorType == null) {
            errorCounter5xx.increment();
            return;
        }
        switch (errorType.toLowerCase()) {
            case "4xx":
            case "client_error":
                errorCounter4xx.increment();
                break;
            case "5xx":
            case "server_error":
                errorCounter5xx.increment();
                break;
            case "timeout":
                errorCounterTimeout.increment();
                break;
            case "validation":
                errorCounterValidation.increment();
                break;
            default:
                // Record as server error if unknown
                errorCounter5xx.increment();
                break;
        }
    }

    /**
     * Record an SLO violation
     */
    public void recordSLOViolation(String sloType) {
        sloViolations.incrementAndGet();
        meterRegistry.counter("miniups.slo.violations.total", "slo_type", sloType).increment();
        log.warn("SLO violation detected: {}", sloType);
    }

    // ==================== SLO CALCULATION ====================

    /**
     * Calculate current availability
     */
    public double calculateAvailability() {
        long total = totalRequests.get();
        if (total == 0) {
            return 1.0; // 100% availability if no requests
        }
        return (double) successfulRequests.get() / total;
    }

    /**
     * Calculate current error rate
     */
    public double calculateErrorRate() {
        long total = totalRequests.get();
        if (total == 0) {
            return 0.0;
        }
        return (double) failedRequests.get() / total;
    }

    /**
     * Check if current metrics are within SLO targets
     */
    public boolean isWithinSLO() {
        double errorRate = calculateErrorRate();
        double availability = calculateAvailability();

        return errorRate <= ERROR_RATE_THRESHOLD && availability >= AVAILABILITY_THRESHOLD;
    }

    // ==================== SCHEDULED METRICS UPDATE ====================

    /**
     * Periodically update SLO metrics every minute
     */
    @Scheduled(fixedRate = 60000)
    public void updateSLOMetrics() {
        try {
            // Update error rate gauge
            double errorRate = calculateErrorRate();
            currentErrorRatePercent.set((long) (errorRate * 100));

            // Check and record error rate SLO violation
            if (errorRate > ERROR_RATE_THRESHOLD) {
                recordSLOViolation("error_rate");
            }

            // Check availability SLO
            double availability = calculateAvailability();
            if (availability < AVAILABILITY_THRESHOLD) {
                recordSLOViolation("availability");
            }

            log.debug("SLO metrics updated - ErrorRate: {}%, Availability: {}%, Violations: {}",
                    String.format("%.2f", errorRate * 100), 
                    String.format("%.2f", availability * 100), 
                    sloViolations.get());

        } catch (Exception e) {
            log.error("Error updating SLO metrics", e);
        }
    }

    // ==================== GETTERS FOR MONITORING ====================

    public long getTotalRequests() {
        return totalRequests.get();
    }

    public long getSuccessfulRequests() {
        return successfulRequests.get();
    }

    public long getFailedRequests() {
        return failedRequests.get();
    }

    public long getSLOViolations() {
        return sloViolations.get();
    }
}
