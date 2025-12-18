package com.miniups.config;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.config.MeterFilter;
import io.micrometer.core.instrument.distribution.DistributionStatisticConfig;
import org.springframework.boot.actuate.autoconfigure.metrics.MeterRegistryCustomizer;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
/**
 * Prometheus Metrics Configuration for High-Performance Monitoring
 *
 * Provides comprehensive performance metrics collection for monitoring:
 * - Application throughput and latency (15K+ QPS capability)
 * - WebSocket connection count and message rates
 * - RabbitMQ message processing performance
 * - Database connection pool utilization
 * - Custom business metrics (shipments, orders, tracking)
 * - System resource utilization and JVM metrics
 *
 * Metrics Categories:
 * - Request Metrics: HTTP request rates, response times, error rates
 * - Business Metrics: Shipment processing, order fulfillment, delivery tracking
 * - Infrastructure Metrics: Database, message queue, cache performance
 * - WebSocket Metrics: Connection count, message throughput, latency
 * - Security Metrics: Authentication attempts, authorization failures
 *
 * Architecture Pattern: Observable Systems with Prometheus Integration
 * Metric Flow: Application Events → Micrometer → Prometheus → Grafana Dashboard
 *
 * @author Mini-UPS Development Team
 * @version 1.0
 * @since 2024-12-01
 */
@Configuration
@EnableScheduling
@ConditionalOnProperty(name = "metrics.enabled", havingValue = "true", matchIfMissing = false)
public class MetricsConfig {


    private static final Logger log = LoggerFactory.getLogger(MetricsConfig.class);
    private final MeterRegistry meterRegistry;

    // Custom metrics storage
    private final AtomicLong totalRequests = new AtomicLong(0);
    private final AtomicLong totalErrors = new AtomicLong(0);
    private final AtomicInteger activeWebSocketConnections = new AtomicInteger(0);
    private final AtomicLong totalWebSocketMessages = new AtomicLong(0);
    private final AtomicLong totalRabbitMQMessages = new AtomicLong(0);
    private final AtomicLong totalShipmentsProcessed = new AtomicLong(0);
    private final AtomicLong totalTrackingUpdates = new AtomicLong(0);

    // SLO Configuration Constants
    private static final Duration SLO_50MS = Duration.ofMillis(50);
    private static final Duration SLO_100MS = Duration.ofMillis(100);
    private static final Duration SLO_200MS = Duration.ofMillis(200);
    private static final Duration SLO_500MS = Duration.ofMillis(500);
    private static final Duration SLO_1S = Duration.ofSeconds(1);

    public MetricsConfig(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
        initializeCustomMetrics();
    }

    /**
     * Configure histogram buckets and percentiles for all Mini-UPS Timer metrics.
     * This enables proper P50/P95/P99 percentile tracking and SLO-based monitoring.
     *
     * Histogram buckets are configured at:
     * - 50ms: Fast operations (cache hits, simple queries)
     * - 100ms: Normal operations
     * - 200ms: SLO target for API responses
     * - 500ms: Slow operations warning threshold
     * - 1s: Critical latency threshold
     */
    @Bean
    public MeterRegistryCustomizer<MeterRegistry> metricsHistogramCustomizer() {
        return registry -> registry.config().meterFilter(new MeterFilter() {
            @Override
            public DistributionStatisticConfig configure(Meter.Id id, DistributionStatisticConfig config) {
                if (id.getName().startsWith("miniups.") && id.getType() == Meter.Type.TIMER) {
                    return DistributionStatisticConfig.builder()
                            .percentiles(0.5, 0.75, 0.95, 0.99) // P50, P75, P95, P99
                            .percentilesHistogram(true)
                            .serviceLevelObjectives(
                                    SLO_50MS.toNanos(),
                                    SLO_100MS.toNanos(),
                                    SLO_200MS.toNanos(),
                                    SLO_500MS.toNanos(),
                                    SLO_1S.toNanos()
                            )
                            .expiry(java.time.Duration.ofMinutes(5))
                            .build()
                            .merge(config);
                }
                return config;
            }
        });
    }

    /**
     * Initialize custom business and performance metrics
     */
    private void initializeCustomMetrics() {
        log.info("Initializing Prometheus custom metrics for Mini-UPS");

        // HTTP Request Metrics
        registerCounter("miniups.http.requests.total", "Total HTTP requests");
        registerTimer("miniups.http.request.duration", "HTTP request duration");

        // Business Metrics
        registerCounter("miniups.shipments.created.total", "Total shipments created");
        registerCounter("miniups.shipments.delivered.total", "Total shipments delivered");
        registerCounter("miniups.tracking.updates.total", "Total tracking updates");
        registerGauge("miniups.shipments.active", "Active shipments in transit", totalShipmentsProcessed);

        // WebSocket Metrics
        registerGauge("miniups.websocket.connections.active", "Active WebSocket connections", activeWebSocketConnections);
        registerCounter("miniups.websocket.messages.total", "Total WebSocket messages sent");
        registerTimer("miniups.websocket.message.duration", "WebSocket message processing time");

        // RabbitMQ Metrics
        registerCounter("miniups.rabbitmq.messages.sent.total", "Total RabbitMQ messages sent");
        registerCounter("miniups.rabbitmq.messages.received.total", "Total RabbitMQ messages received");
        registerCounter("miniups.rabbitmq.messages.failed.total", "Total RabbitMQ message failures");
        registerTimer("miniups.rabbitmq.message.processing.duration", "RabbitMQ message processing time");

        // Authentication & Security Metrics
        registerCounter("miniups.auth.attempts.total", "Total authentication attempts");
        registerCounter("miniups.auth.jwt.tokens.issued.total", "Total JWT tokens issued");
        registerCounter("miniups.oauth2.logins.total", "Total OAuth2 logins");

        // Database Metrics
        registerTimer("miniups.database.query.duration", "Database query duration");
        registerCounter("miniups.database.connections.total", "Database connection attempts");

        // Truck & Logistics Metrics
        registerGauge("miniups.trucks.active", "Active trucks in service", new AtomicInteger(0));
        registerCounter("miniups.trucks.dispatched.total", "Total trucks dispatched");
        registerTimer("miniups.delivery.duration", "Average delivery time");

        // Performance SLO Metrics
        registerTimer("miniups.api.response.time", "API response time for SLO monitoring");
        registerCounter("miniups.slo.violations.total", "SLO violations count");

        log.info("Prometheus metrics initialization completed");
    }

    /**
     * Register a counter metric with tags
     */
    private void registerCounter(String name, String description, String... tags) {
        Counter.Builder builder = Counter.builder(name)
                .description(description);

        // Add tags if provided (must be even number of arguments: key1, value1, key2, value2, ...)
        if (tags != null && tags.length > 0) {
            if (tags.length % 2 != 0) {
                throw new IllegalArgumentException("Tags must be provided in key-value pairs (even number of arguments)");
            }
            for (int i = 0; i < tags.length; i += 2) {
                builder.tag(tags[i], tags[i + 1]);
            }
        }

        builder.register(meterRegistry);
    }

    /**
     * Register a gauge metric
     */
    private void registerGauge(String name, String description, AtomicInteger value) {
        Gauge.builder(name, value, AtomicInteger::get)
                .description(description)
                .register(meterRegistry);
    }

    /**
     * Register a gauge metric for AtomicLong
     */
    private void registerGauge(String name, String description, AtomicLong value) {
        Gauge.builder(name, value, AtomicLong::get)
                .description(description)
                .register(meterRegistry);
    }

    /**
     * Register a timer metric
     */
    private void registerTimer(String name, String description, String... tags) {
        Timer.Builder builder = Timer.builder(name)
                .description(description);

        // Add tags if provided (must be even number of arguments: key1, value1, key2, value2, ...)
        if (tags != null && tags.length > 0) {
            if (tags.length % 2 != 0) {
                throw new IllegalArgumentException("Tags must be provided in key-value pairs (even number of arguments)");
            }
            for (int i = 0; i < tags.length; i += 2) {
                builder.tag(tags[i], tags[i + 1]);
            }
        }

        builder.register(meterRegistry);
    }

    // ==================== METRIC UPDATE METHODS ====================

    /**
     * Record HTTP request metrics
     */
    public void recordHttpRequest(String endpoint, String method, String status, long durationMs) {
        meterRegistry.counter("miniups.http.requests.total",
                "endpoint", endpoint, "method", method, "status", status).increment();

        meterRegistry.timer("miniups.http.request.duration",
                "endpoint", endpoint).record(durationMs, java.util.concurrent.TimeUnit.MILLISECONDS);

        totalRequests.incrementAndGet();
        if (!status.startsWith("2")) {
            totalErrors.incrementAndGet();
        }
    }

    /**
     * Record WebSocket connection metrics
     */
    public void recordWebSocketConnection(boolean connected) {
        if (connected) {
            activeWebSocketConnections.incrementAndGet();
        } else {
            activeWebSocketConnections.decrementAndGet();
        }
    }

    /**
     * Record WebSocket message metrics
     */
    public void recordWebSocketMessage(long processingTimeMs) {
        meterRegistry.counter("miniups.websocket.messages.total").increment();
        meterRegistry.timer("miniups.websocket.message.duration")
                .record(processingTimeMs, java.util.concurrent.TimeUnit.MILLISECONDS);
        totalWebSocketMessages.incrementAndGet();
    }

    /**
     * Record RabbitMQ message metrics
     */
    public void recordRabbitMQMessage(String operation, boolean success, long processingTimeMs) {
        String counterName = "miniups.rabbitmq.messages." + operation + ".total";
        meterRegistry.counter(counterName).increment();

        if (!success) {
            meterRegistry.counter("miniups.rabbitmq.messages.failed.total").increment();
        }

        meterRegistry.timer("miniups.rabbitmq.message.processing.duration")
                .record(processingTimeMs, java.util.concurrent.TimeUnit.MILLISECONDS);

        totalRabbitMQMessages.incrementAndGet();
    }

    /**
     * Record authentication metrics
     */
    public void recordAuthentication(String result) {
        meterRegistry.counter("miniups.auth.attempts.total", "result", result).increment();
    }

    /**
     * Record OAuth2 login metrics
     */
    public void recordOAuth2Login(String provider) {
        meterRegistry.counter("miniups.oauth2.logins.total", "provider", provider).increment();
    }

    /**
     * Record shipment metrics
     */
    public void recordShipmentCreated() {
        meterRegistry.counter("miniups.shipments.created.total").increment();
        totalShipmentsProcessed.incrementAndGet();
    }

    public void recordShipmentDelivered() {
        meterRegistry.counter("miniups.shipments.delivered.total").increment();
    }

    /**
     * Record tracking update metrics
     */
    public void recordTrackingUpdate() {
        meterRegistry.counter("miniups.tracking.updates.total").increment();
        totalTrackingUpdates.incrementAndGet();
    }

    /**
     * Record database operation metrics
     */
    public void recordDatabaseOperation(String operation, boolean success, long durationMs) {
        meterRegistry.timer("miniups.database.query.duration", "operation", operation)
                .record(durationMs, java.util.concurrent.TimeUnit.MILLISECONDS);

        meterRegistry.counter("miniups.database.connections.total",
                "result", success ? "success" : "failure").increment();
    }

    /**
     * Record SLO violation
     */
    public void recordSLOViolation(String sloType) {
        meterRegistry.counter("miniups.slo.violations.total", "slo_type", sloType).increment();
    }

    // ==================== SCHEDULED METRICS COLLECTION ====================

    /**
     * Collect and update system metrics every 30 seconds
     */
    @Scheduled(fixedRate = 30000)
    public void collectSystemMetrics() {
        try {
            // JVM Memory metrics
            Runtime runtime = Runtime.getRuntime();
            long totalMemory = runtime.totalMemory();
            long freeMemory = runtime.freeMemory();
            long usedMemory = totalMemory - freeMemory;

            meterRegistry.gauge("miniups.jvm.memory.used", usedMemory);
            meterRegistry.gauge("miniups.jvm.memory.total", totalMemory);
            meterRegistry.gauge("miniups.jvm.memory.free", freeMemory);

            // Application health metrics
            double errorRate = totalRequests.get() > 0 ?
                    (double) totalErrors.get() / totalRequests.get() : 0.0;
            meterRegistry.gauge("miniups.error.rate", errorRate);

            // WebSocket performance metrics
            meterRegistry.gauge("miniups.websocket.throughput",
                    totalWebSocketMessages.get() / Math.max(1, System.currentTimeMillis() / 1000));

            log.debug("System metrics collected - Memory: {}MB, Error Rate: {:.2%}",
                     usedMemory / 1024 / 1024, errorRate);

        } catch (Exception e) {
            log.error("Error collecting system metrics", e);
        }
    }

    /**
     * Get current performance statistics
     */
    public PerformanceStats getPerformanceStats() {
        return new PerformanceStats(
                totalRequests.get(),
                totalErrors.get(),
                activeWebSocketConnections.get(),
                totalWebSocketMessages.get(),
                totalRabbitMQMessages.get(),
                totalShipmentsProcessed.get(),
                totalTrackingUpdates.get()
        );
    }

    /**
     * Performance statistics data class
     */
    public static class PerformanceStats {
        private final long totalRequests;
        private final long totalErrors;
        private final int activeWebSocketConnections;
        private final long totalWebSocketMessages;
        private final long totalRabbitMQMessages;
        private final long totalShipmentsProcessed;
        private final long totalTrackingUpdates;

        public PerformanceStats(long totalRequests, long totalErrors, int activeWebSocketConnections,
                               long totalWebSocketMessages, long totalRabbitMQMessages,
                               long totalShipmentsProcessed, long totalTrackingUpdates) {
            this.totalRequests = totalRequests;
            this.totalErrors = totalErrors;
            this.activeWebSocketConnections = activeWebSocketConnections;
            this.totalWebSocketMessages = totalWebSocketMessages;
            this.totalRabbitMQMessages = totalRabbitMQMessages;
            this.totalShipmentsProcessed = totalShipmentsProcessed;
            this.totalTrackingUpdates = totalTrackingUpdates;
        }

        // Getters
        public long getTotalRequests() { return totalRequests; }
        public long getTotalErrors() { return totalErrors; }
        public int getActiveWebSocketConnections() { return activeWebSocketConnections; }
        public long getTotalWebSocketMessages() { return totalWebSocketMessages; }
        public long getTotalRabbitMQMessages() { return totalRabbitMQMessages; }
        public long getTotalShipmentsProcessed() { return totalShipmentsProcessed; }
        public long getTotalTrackingUpdates() { return totalTrackingUpdates; }

        public double getErrorRate() {
            return totalRequests > 0 ? (double) totalErrors / totalRequests : 0.0;
        }
    }
}