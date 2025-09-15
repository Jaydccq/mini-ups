package com.miniups.controller;

import com.miniups.service.LeafIdGeneratorService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * Leaf-Segment ID Generator Management Controller
 * 
 * Provides REST endpoints for monitoring and managing the Leaf-Segment
 * distributed ID generation system. These endpoints are essential for
 * operational visibility and performance tuning.
 * 
 * Key Features:
 * - Real-time performance monitoring and health checks
 * - Business tag management and initialization
 * - Segment allocation statistics and buffer status
 * - Load testing and performance validation endpoints
 * 
 * Security:
 * - Admin-only access for sensitive operations
 * - Public read-only endpoints for health monitoring
 * - Integration with Spring Security RBAC
 * 
 * Performance Monitoring:
 * - ID generation throughput metrics
 * - Segment allocation success rates
 * - Buffer utilization and prefetch effectiveness
 * - Database contention and optimization insights
 * 
 * @author Mini-UPS Development Team
 * @version 1.0
 */
@RestController
@RequestMapping("/api/admin/leaf")
@RequiredArgsConstructor
public class LeafIdManagementController {
    
    private final LeafIdGeneratorService leafIdGeneratorService;
    
    /**
     * Get comprehensive health and performance statistics
     * 
     * This endpoint provides a complete view of the Leaf-Segment system
     * performance, including throughput, success rates, and buffer status.
     * 
     * @return Comprehensive system health data
     */
    @GetMapping("/health")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> getSystemHealth() {
        try {
            Map<String, Object> health = leafIdGeneratorService.getHealthStatistics();
            return ResponseEntity.ok(health);
            
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("healthy", false);
            error.put("error", e.getMessage());
            error.put("timestamp", java.time.Instant.now());
            
            return ResponseEntity.status(500).body(error);
        }
    }
    
    /**
     * Get status for a specific business tag
     * 
     * @param bizTag Business tag to query
     * @return Business tag status including database and buffer information
     */
    @GetMapping("/status/{bizTag}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> getBusinessTagStatus(@PathVariable String bizTag) {
        try {
            Map<String, Object> status = leafIdGeneratorService.getBusinessTagStatus(bizTag);
            
            if (status.isEmpty()) {
                return ResponseEntity.notFound().build();
            }
            
            return ResponseEntity.ok(status);
            
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("bizTag", bizTag);
            error.put("error", e.getMessage());
            error.put("timestamp", java.time.Instant.now());
            
            return ResponseEntity.status(500).body(error);
        }
    }
    
    /**
     * Initialize a new business tag allocation
     * 
     * Creates a new ID sequence for the specified business tag with
     * the given initial step size.
     * 
     * @param request Initialization request containing bizTag, step, and description
     * @return Success/failure response
     */
    @PostMapping("/initialize")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> initializeBusinessTag(
            @RequestBody InitializeBusinessTagRequest request) {
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            // Validate request
            if (request.getBizTag() == null || request.getBizTag().trim().isEmpty()) {
                response.put("success", false);
                response.put("error", "Business tag cannot be empty");
                return ResponseEntity.badRequest().body(response);
            }
            
            if (request.getInitialStep() <= 0) {
                response.put("success", false);
                response.put("error", "Initial step size must be positive");
                return ResponseEntity.badRequest().body(response);
            }
            
            // Initialize the business tag
            boolean created = leafIdGeneratorService.initializeBusinessTag(
                request.getBizTag(),
                request.getInitialStep(),
                request.getDescription()
            );
            
            response.put("success", true);
            response.put("created", created);
            response.put("bizTag", request.getBizTag());
            response.put("initialStep", request.getInitialStep());
            response.put("message", created ? "Business tag created successfully" : "Business tag already exists");
            response.put("timestamp", java.time.Instant.now());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            response.put("success", false);
            response.put("error", e.getMessage());
            response.put("timestamp", java.time.Instant.now());
            
            return ResponseEntity.status(500).body(response);
        }
    }
    
    /**
     * Generate a batch of IDs for load testing
     * 
     * This endpoint is useful for performance testing and validation.
     * It generates the specified number of IDs and reports timing statistics.
     * 
     * @param bizTag Business tag to generate IDs for
     * @param count Number of IDs to generate (max 10000 for safety)
     * @return Performance statistics for the batch generation
     */
    @PostMapping("/generate-batch/{bizTag}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> generateBatch(
            @PathVariable String bizTag,
            @RequestParam(defaultValue = "1000") int count) {
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            // Safety limit
            if (count > 10000) {
                response.put("error", "Maximum batch size is 10000");
                return ResponseEntity.badRequest().body(response);
            }
            
            if (count <= 0) {
                response.put("error", "Count must be positive");
                return ResponseEntity.badRequest().body(response);
            }
            
            // Generate IDs and measure performance
            long startTime = System.nanoTime();
            long[] ids = new long[count];
            
            for (int i = 0; i < count; i++) {
                ids[i] = leafIdGeneratorService.nextId(bizTag);
            }
            
            long endTime = System.nanoTime();
            long durationNanos = endTime - startTime;
            double durationMs = durationNanos / 1_000_000.0;
            double avgLatencyMicros = durationNanos / (double) count / 1000.0;
            double throughputPerSecond = count / (durationMs / 1000.0);
            
            // Validate ID uniqueness and ordering
            boolean sequential = true;
            for (int i = 1; i < ids.length; i++) {
                if (ids[i] != ids[i-1] + 1) {
                    sequential = false;
                    break;
                }
            }
            
            response.put("success", true);
            response.put("bizTag", bizTag);
            response.put("count", count);
            response.put("firstId", ids[0]);
            response.put("lastId", ids[count - 1]);
            response.put("sequential", sequential);
            response.put("totalDurationMs", durationMs);
            response.put("avgLatencyMicros", avgLatencyMicros);
            response.put("throughputPerSecond", throughputPerSecond);
            response.put("timestamp", java.time.Instant.now());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            response.put("success", false);
            response.put("bizTag", bizTag);
            response.put("count", count);
            response.put("error", e.getMessage());
            response.put("timestamp", java.time.Instant.now());
            
            return ResponseEntity.status(500).body(response);
        }
    }
    
    /**
     * Simple health check endpoint for basic monitoring
     * 
     * This endpoint provides a lightweight health check that can be used
     * by load balancers and monitoring systems.
     * 
     * @return Basic health status
     */
    @GetMapping("/ping")
    public ResponseEntity<Map<String, Object>> ping() {
        Map<String, Object> response = new HashMap<>();
        
        try {
            // Simple health check - generate a single ID
            long testId = leafIdGeneratorService.nextId("health_check");
            
            response.put("status", "healthy");
            response.put("testId", testId);
            response.put("timestamp", java.time.Instant.now());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            response.put("status", "unhealthy");
            response.put("error", e.getMessage());
            response.put("timestamp", java.time.Instant.now());
            
            return ResponseEntity.status(503).body(response);
        }
    }
    
    /**
     * Data transfer object for business tag initialization requests
     */
    public static class InitializeBusinessTagRequest {
        private String bizTag;
        private int initialStep = 1000;
        private String description;
        
        // Constructors
        public InitializeBusinessTagRequest() {}
        
        public InitializeBusinessTagRequest(String bizTag, int initialStep, String description) {
            this.bizTag = bizTag;
            this.initialStep = initialStep;
            this.description = description;
        }
        
        // Getters and setters
        public String getBizTag() {
            return bizTag;
        }
        
        public void setBizTag(String bizTag) {
            this.bizTag = bizTag;
        }
        
        public int getInitialStep() {
            return initialStep;
        }
        
        public void setInitialStep(int initialStep) {
            this.initialStep = initialStep;
        }
        
        public String getDescription() {
            return description;
        }
        
        public void setDescription(String description) {
            this.description = description;
        }
    }
}