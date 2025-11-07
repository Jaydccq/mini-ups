package com.miniups.controller;

import com.miniups.service.EventPublisherService;
import com.miniups.service.OutboxPollerService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * Outbox Management Controller
 * 
 * Provides REST endpoints for monitoring and managing the Transactional Outbox
 * pattern implementation. These endpoints are essential for operational visibility
 * into the event publishing system's health and performance.
 * 
 * Key Features:
 * - Real-time outbox health monitoring
 * - Event processing statistics and metrics
 * - Active poller instance tracking for distributed deployments
 * - Integration with Spring Boot Actuator for standardized health checks
 * 
 * Security:
 * - Admin-only access for sensitive operational data
 * - Integration with Spring Security RBAC
 * 
 * Monitoring Integration:
 * - Compatible with Prometheus metrics collection
 * - Provides structured JSON responses for monitoring systems
 * - Supports alerting on outbox processing lag and failure rates
 * 
 * @author Mini-UPS Development Team
 * @version 1.0
 */
@RestController
@RequestMapping("/api/admin/outbox")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class OutboxManagementController {

    private final EventPublisherService eventPublisherService;
    private final OutboxPollerService outboxPollerService;

    /**
     * Get comprehensive outbox health information
     * 
     * This endpoint provides a complete view of the outbox system health,
     * including event statistics, processing metrics, and poller status.
     * 
     * @return Comprehensive outbox health data
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> getOutboxHealth() {
        Map<String, Object> health = new HashMap<>();
        
        try {
            // Get outbox event statistics
            Map<String, Object> eventStats = eventPublisherService.getOutboxHealthStatistics();
            health.put("eventStatistics", eventStats);
            
            // Get poller health information
            Map<String, Object> pollerStats = outboxPollerService.getHealthStatistics();
            health.put("pollerStatistics", pollerStats);
            
            // Get active poller instances
            var activeInstances = outboxPollerService.getActiveInstances();
            health.put("activePollerInstances", activeInstances);
            health.put("activePollerCount", activeInstances.size());
            
            // Overall health assessment
            boolean overallHealthy = determineOverallHealth(eventStats, pollerStats);
            health.put("healthy", overallHealthy);
            health.put("timestamp", java.time.Instant.now());
            
            return ResponseEntity.ok(health);
            
        } catch (Exception e) {
            health.put("healthy", false);
            health.put("error", e.getMessage());
            health.put("timestamp", java.time.Instant.now());
            
            return ResponseEntity.status(500).body(health);
        }
    }
    
    /**
     * Get detailed event statistics from the outbox
     * 
     * @return Event statistics including counts by status and processing lag
     */
    @GetMapping("/statistics")
    public ResponseEntity<Map<String, Object>> getOutboxStatistics() {
        try {
            Map<String, Object> stats = eventPublisherService.getOutboxHealthStatistics();
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", e.getMessage());
            error.put("timestamp", java.time.Instant.now());
            return ResponseEntity.status(500).body(error);
        }
    }
    
    /**
     * Get outbox poller performance metrics
     * 
     * @return Poller performance data including throughput and error rates
     */
    @GetMapping("/poller/metrics")
    public ResponseEntity<Map<String, Object>> getPollerMetrics() {
        try {
            Map<String, Object> metrics = outboxPollerService.getHealthStatistics();
            return ResponseEntity.ok(metrics);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", e.getMessage());
            error.put("timestamp", java.time.Instant.now());
            return ResponseEntity.status(500).body(error);
        }
    }
    
    /**
     * Get information about active poller instances
     * 
     * Useful for monitoring distributed deployments where multiple
     * application instances are running outbox pollers.
     * 
     * @return List of active poller instances with their metadata
     */
    @GetMapping("/poller/instances")
    public ResponseEntity<Map<String, Object>> getPollerInstances() {
        try {
            var instances = outboxPollerService.getActiveInstances();
            
            Map<String, Object> response = new HashMap<>();
            response.put("activeInstances", instances);
            response.put("count", instances.size());
            response.put("timestamp", java.time.Instant.now());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", e.getMessage());
            error.put("timestamp", java.time.Instant.now());
            return ResponseEntity.status(500).body(error);
        }
    }
    
    /**
     * Determine overall outbox system health based on component statistics
     * 
     * @param eventStats Event statistics from the repository
     * @param pollerStats Poller performance statistics
     * @return true if the system is considered healthy
     */
    private boolean determineOverallHealth(Map<String, Object> eventStats, Map<String, Object> pollerStats) {
        try {
            // Check if basic statistics are available
            boolean eventStatsHealthy = (Boolean) eventStats.getOrDefault("healthy", false);
            boolean pollerStatsHealthy = (Boolean) pollerStats.getOrDefault("healthy", false);
            
            // Check for excessive event lag (more than 5 minutes for oldest pending)
            Object oldestPendingAge = eventStats.get("oldestPendingEventAgeSeconds");
            boolean noExcessiveLag = oldestPendingAge == null || 
                                   (oldestPendingAge instanceof Number && 
                                    ((Number) oldestPendingAge).longValue() < 300);
            
            // Check for reasonable failure rate (less than 10% failures)
            long totalProcessed = ((Number) pollerStats.getOrDefault("totalProcessed", 0)).longValue();
            long totalFailed = ((Number) pollerStats.getOrDefault("totalFailed", 0)).longValue();
            
            boolean reasonableFailureRate = totalProcessed == 0 || 
                                          (totalFailed * 100.0 / totalProcessed) < 10.0;
            
            return eventStatsHealthy && pollerStatsHealthy && noExcessiveLag && reasonableFailureRate;
            
        } catch (Exception e) {
            // If we can't determine health, assume unhealthy
            return false;
        }
    }
}