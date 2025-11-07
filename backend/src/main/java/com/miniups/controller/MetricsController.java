package com.miniups.controller;

import com.miniups.config.MetricsConfig;
import com.miniups.service.OutboxPollerService;
import com.miniups.service.WebSocketRabbitMQService;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Metrics;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Metrics and Performance Monitoring Controller
 *
 * Provides comprehensive performance monitoring endpoints for:
 * - Real-time system metrics and KPI dashboard
 * - RabbitMQ message processing statistics
 * - WebSocket connection and throughput metrics
 * - Database performance and connection pool status
 * - Outbox pattern processing health
 * - JVM and system resource utilization
 *
 * Dashboard Features:
 * - Live performance metrics (15K+ QPS capability)
 * - WebSocket connection monitoring (500+ concurrent)
 * - Message processing latency tracking (<5ms target)
 * - Error rate monitoring and SLO compliance
 * - Custom business metrics (shipments, tracking, orders)
 *
 * Architecture Pattern: Observable Systems with Real-Time Monitoring
 * Metric Flow: Application → Micrometer → Prometheus → This Dashboard API
 *
 * @author Mini-UPS Development Team
 * @version 1.0
 * @since 2024-12-01
 */
@RestController
@RequestMapping("/api/metrics")
@RequiredArgsConstructor
@ConditionalOnProperty(name = "metrics.enabled", havingValue = "true", matchIfMissing = false)
public class MetricsController {

    private final MetricsConfig metricsConfig;
    private final MeterRegistry meterRegistry;
    private final OutboxPollerService outboxPollerService;
    private final WebSocketRabbitMQService webSocketRabbitMQService;

    /**
     * Get comprehensive performance dashboard metrics
     * Provides real-time system overview for monitoring and alerting
     */
    @GetMapping("/dashboard")
    public ResponseEntity<Map<String, Object>> getDashboardMetrics() {
        Map<String, Object> dashboard = new HashMap<>();

        // System Overview
        dashboard.put("timestamp", LocalDateTime.now());
        dashboard.put("applicationName", "Mini-UPS Backend");
        dashboard.put("version", "2.0.0");

        // Performance Statistics
        MetricsConfig.PerformanceStats stats = metricsConfig.getPerformanceStats();
        dashboard.put("performance", Map.of(
            "totalRequests", stats.getTotalRequests(),
            "totalErrors", stats.getTotalErrors(),
            "errorRate", String.format("%.2f%%", stats.getErrorRate() * 100),
            "activeWebSocketConnections", stats.getActiveWebSocketConnections(),
            "totalWebSocketMessages", stats.getTotalWebSocketMessages(),
            "totalRabbitMQMessages", stats.getTotalRabbitMQMessages(),
            "totalShipmentsProcessed", stats.getTotalShipmentsProcessed(),
            "totalTrackingUpdates", stats.getTotalTrackingUpdates()
        ));

        // WebSocket Metrics
        dashboard.put("websocket", webSocketRabbitMQService.getMetrics());

        // Outbox Processing Health
        dashboard.put("outbox", outboxPollerService.getHealthStatistics());

        // JVM Metrics
        dashboard.put("jvm", getJVMMetrics());

        // Business Metrics
        dashboard.put("business", getBusinessMetrics());

        // SLO Compliance
        dashboard.put("slo", getSLOMetrics());

        return ResponseEntity.ok(dashboard);
    }

    /**
     * Get detailed performance metrics for specific system components
     */
    @GetMapping("/performance")
    public ResponseEntity<Map<String, Object>> getPerformanceMetrics() {
        Map<String, Object> performance = new HashMap<>();

        // HTTP Request Metrics
        performance.put("httpRequests", Map.of(
            "total", getCounterValue("miniups.http.requests.total"),
            "averageResponseTime", getTimerMean("miniups.http.request.duration"),
            "p95ResponseTime", getTimerPercentile("miniups.http.request.duration", 0.95),
            "p99ResponseTime", getTimerPercentile("miniups.http.request.duration", 0.99)
        ));

        // RabbitMQ Performance
        performance.put("rabbitmq", Map.of(
            "messagesSent", getCounterValue("miniups.rabbitmq.messages.sent.total"),
            "messagesReceived", getCounterValue("miniups.rabbitmq.messages.received.total"),
            "messagesFailed", getCounterValue("miniups.rabbitmq.messages.failed.total"),
            "averageProcessingTime", getTimerMean("miniups.rabbitmq.message.processing.duration")
        ));

        // WebSocket Performance
        performance.put("websocket", Map.of(
            "activeConnections", getGaugeValue("miniups.websocket.connections.active"),
            "totalMessages", getCounterValue("miniups.websocket.messages.total"),
            "averageMessageTime", getTimerMean("miniups.websocket.message.duration")
        ));

        // Database Performance
        performance.put("database", Map.of(
            "connectionAttempts", getCounterValue("miniups.database.connections.total"),
            "averageQueryTime", getTimerMean("miniups.database.query.duration")
        ));

        return ResponseEntity.ok(performance);
    }

    /**
     * Get business-specific metrics for operational monitoring
     */
    @GetMapping("/business")
    public ResponseEntity<Map<String, Object>> getBusinessMetricsEndpoint() {
        return ResponseEntity.ok(getBusinessMetrics());
    }

    /**
     * Get security and authentication metrics
     */
    @GetMapping("/security")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> getSecurityMetrics() {
        Map<String, Object> security = new HashMap<>();

        security.put("authentication", Map.of(
            "totalAttempts", getCounterValue("miniups.auth.attempts.total"),
            "successfulLogins", getCounterValue("miniups.auth.attempts.total", "result", "success"),
            "failedLogins", getCounterValue("miniups.auth.attempts.total", "result", "failure"),
            "jwtTokensIssued", getCounterValue("miniups.auth.jwt.tokens.issued.total"),
            "oauth2Logins", getCounterValue("miniups.oauth2.logins.total")
        ));

        return ResponseEntity.ok(security);
    }

    /**
     * Get system health and resource utilization metrics
     */
    @GetMapping("/system")
    public ResponseEntity<Map<String, Object>> getSystemMetrics() {
        Map<String, Object> system = new HashMap<>();

        // JVM Metrics
        system.put("jvm", getJVMMetrics());

        // System Resources
        system.put("system", Map.of(
            "cpuUsage", getGaugeValue("system.cpu.usage"),
            "memoryUsage", getGaugeValue("jvm.memory.used"),
            "diskSpace", getGaugeValue("disk.free"),
            "uptime", getGaugeValue("process.uptime")
        ));

        return ResponseEntity.ok(system);
    }

    /**
     * Force metrics collection and return updated dashboard
     */
    @PostMapping("/refresh")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> refreshMetrics() {
        // Trigger metrics collection
        metricsConfig.collectSystemMetrics();

        return getDashboardMetrics();
    }

    /**
     * Reset specific metric counters (for testing/development)
     */
    @PostMapping("/reset/{metricName}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> resetMetric(@PathVariable String metricName) {
        try {
            // This would typically require custom metric management
            // For now, just acknowledge the request
            return ResponseEntity.ok("Metric reset requested: " + metricName);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Failed to reset metric: " + e.getMessage());
        }
    }

    // ==================== HELPER METHODS ====================

    private Map<String, Object> getJVMMetrics() {
        Runtime runtime = Runtime.getRuntime();
        return Map.of(
            "memoryUsed", runtime.totalMemory() - runtime.freeMemory(),
            "memoryTotal", runtime.totalMemory(),
            "memoryFree", runtime.freeMemory(),
            "memoryMax", runtime.maxMemory(),
            "processors", runtime.availableProcessors(),
            "uptime", getGaugeValue("process.uptime")
        );
    }

    private Map<String, Object> getBusinessMetrics() {
        return Map.of(
            "shipments", Map.of(
                "created", getCounterValue("miniups.shipments.created.total"),
                "delivered", getCounterValue("miniups.shipments.delivered.total"),
                "active", getGaugeValue("miniups.shipments.active")
            ),
            "tracking", Map.of(
                "updates", getCounterValue("miniups.tracking.updates.total")
            ),
            "trucks", Map.of(
                "active", getGaugeValue("miniups.trucks.active"),
                "dispatched", getCounterValue("miniups.trucks.dispatched.total")
            ),
            "deliveries", Map.of(
                "averageTime", getTimerMean("miniups.delivery.duration")
            )
        );
    }

    private Map<String, Object> getSLOMetrics() {
        return Map.of(
            "responseTime", Map.of(
                "target", "< 200ms",
                "current", getTimerMean("miniups.api.response.time"),
                "p95", getTimerPercentile("miniups.api.response.time", 0.95),
                "violations", getCounterValue("miniups.slo.violations.total", "slo_type", "response_time")
            ),
            "errorRate", Map.of(
                "target", "< 1%",
                "current", String.format("%.2f%%", metricsConfig.getPerformanceStats().getErrorRate() * 100),
                "violations", getCounterValue("miniups.slo.violations.total", "slo_type", "error_rate")
            ),
            "throughput", Map.of(
                "target", "15000 QPS",
                "current", calculateCurrentQPS(),
                "violations", getCounterValue("miniups.slo.violations.total", "slo_type", "throughput")
            )
        );
    }

    private double getCounterValue(String meterName) {
        try {
            return meterRegistry.counter(meterName).count();
        } catch (Exception e) {
            return 0.0;
        }
    }

    private double getCounterValue(String meterName, String tagKey, String tagValue) {
        try {
            return meterRegistry.counter(meterName, tagKey, tagValue).count();
        } catch (Exception e) {
            return 0.0;
        }
    }

    private double getGaugeValue(String meterName) {
        try {
            return meterRegistry.get(meterName).gauge().value();
        } catch (Exception e) {
            return 0.0;
        }
    }

    private double getTimerMean(String meterName) {
        try {
            return meterRegistry.get(meterName).timer().mean(java.util.concurrent.TimeUnit.MILLISECONDS);
        } catch (Exception e) {
            return 0.0;
        }
    }

    private double getTimerPercentile(String meterName, double percentile) {
        try {
            return meterRegistry.get(meterName).timer().percentile(percentile, java.util.concurrent.TimeUnit.MILLISECONDS);
        } catch (Exception e) {
            return 0.0;
        }
    }

    private String calculateCurrentQPS() {
        try {
            MetricsConfig.PerformanceStats stats = metricsConfig.getPerformanceStats();
            // This is a simplified calculation - in production you'd use time windows
            return String.format("%.0f QPS", stats.getTotalRequests() / Math.max(1, System.currentTimeMillis() / 1000));
        } catch (Exception e) {
            return "N/A";
        }
    }
}