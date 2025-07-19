/**
 * Truck Management Controller
 * 
 * Features:
 * - Provides REST API interfaces for truck fleet management
 * - Supports truck status queries, dispatch management, and statistical analysis
 * - Provides fleet monitoring functionality for operations personnel
 * 
 * API Endpoints:
 * - GET /api/trucks - Get all truck statuses
 * - GET /api/trucks/{truckId} - Get specified truck details
 * - PUT /api/trucks/{truckId}/status - Update truck status
 * - POST /api/trucks/{truckId}/assign - Manually assign truck
 * - POST /api/trucks/{truckId}/release - Release truck
 * - GET /api/trucks/statistics - Get fleet statistics
 * - GET /api/trucks/available - Get available truck list
 * 
 * Permission Control:
 * - Query operations: Operator and above permissions
 * - Management operations: Admin permissions
 * - Statistics viewing: Operator and above permissions
 * 
 * @author Mini-UPS Team
 * @version 1.0.0
 */
package com.miniups.controller;

import com.miniups.model.dto.common.ApiResponse;
import com.miniups.model.entity.Truck;
import com.miniups.service.TruckManagementService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/trucks")
@CrossOrigin(origins = "*")
public class TruckManagementController {
    
    private static final Logger logger = LoggerFactory.getLogger(TruckManagementController.class);
    
    @Autowired
    private TruckManagementService truckManagementService;
    
    /**
     * Get status information for all trucks
     * 
     * @return Truck status list
     */
    @GetMapping
    @PreAuthorize("hasRole('ADMIN') or hasRole('OPERATOR')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getAllTrucks() {
        logger.info("Getting all truck statuses");
        
        List<Map<String, Object>> truckStatuses = truckManagementService.getAllTruckStatuses();
        
        Map<String, Object> responseData = Map.of(
            "trucks", truckStatuses,
            "total_count", truckStatuses.size()
        );
        
        return ResponseEntity.ok(ApiResponse.success("Truck statuses retrieved successfully", responseData));
    }
    
    /**
     * Get fleet statistics
     * 
     * @return Statistical data
     */
    @GetMapping("/statistics")
    @PreAuthorize("hasRole('ADMIN') or hasRole('OPERATOR')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getFleetStatistics() {
        logger.info("Getting fleet statistics");
        
        Map<String, Object> statistics = truckManagementService.getFleetStatistics();
        
        return ResponseEntity.ok(ApiResponse.success("Fleet statistics retrieved successfully", statistics));
    }
    
    /**
     * Update truck status and position
     * 
     * @param truckId Truck ID
     * @param statusUpdate Status update request
     * @return Update result
     */
    @PutMapping("/{truckId}/status")
    @PreAuthorize("hasRole('ADMIN') or hasRole('OPERATOR')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> updateTruckStatus(
            @PathVariable Integer truckId,
            @RequestBody Map<String, Object> statusUpdate) {
        logger.info("Updating status for truck {}: {}", truckId, statusUpdate);
        
        Integer x = extractIntegerValue(statusUpdate, "x");
        Integer y = extractIntegerValue(statusUpdate, "y");
        String status = (String) statusUpdate.get("status");
        
        if (x == null || y == null || status == null) {
            return ResponseEntity.badRequest().body(
                ApiResponse.error("Missing required fields: x, y, status")
            );
        }
        
        boolean updated = truckManagementService.updateTruckStatus(truckId, x, y, status);
        
        if (updated) {
            Map<String, Object> responseData = Map.of(
                "truck_id", truckId,
                "new_position", Map.of("x", x, "y", y),
                "new_status", status
            );
            return ResponseEntity.ok(ApiResponse.success("Truck status updated successfully", responseData));
        } else {
            return ResponseEntity.badRequest().body(
                ApiResponse.error("Failed to update truck status. Truck not found or invalid data.")
            );
        }
    }
    
    /**
     * Manually assign truck to specified position
     * 
     * @param truckId Truck ID
     * @param assignmentRequest Assignment request
     * @return Assignment result
     */
    @PostMapping("/{truckId}/assign")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> assignTruck(
            @PathVariable Integer truckId,
            @RequestBody Map<String, Object> assignmentRequest) {
        logger.info("Manual assignment for truck {}: {}", truckId, assignmentRequest);
        
        Integer targetX = extractIntegerValue(assignmentRequest, "target_x");
        Integer targetY = extractIntegerValue(assignmentRequest, "target_y");
        Integer priority = extractIntegerValue(assignmentRequest, "priority");
        
        if (targetX == null || targetY == null) {
            return ResponseEntity.badRequest().body(
                ApiResponse.error("Missing required fields: target_x, target_y")
            );
        }
        
        // Use intelligent assignment algorithm
        Truck assignedTruck = truckManagementService.assignOptimalTruck(
            targetX, targetY, priority != null ? priority : 3
        );
        
        if (assignedTruck != null) {
            Map<String, Object> responseData = Map.of(
                "assigned_truck", Map.of(
                    "truck_id", assignedTruck.getTruckId(),
                    "current_x", assignedTruck.getCurrentX(),
                    "current_y", assignedTruck.getCurrentY(),
                    "status", assignedTruck.getStatus().toString()
                ),
                "target_position", Map.of("x", targetX, "y", targetY)
            );
            return ResponseEntity.ok(ApiResponse.success("Truck assigned successfully", responseData));
        } else {
            return ResponseEntity.status(503).body(
                ApiResponse.error("No available trucks for assignment")
            );
        }
    }
    
    /**
     * Release truck (set to idle status)
     * 
     * @param truckId Truck ID
     * @return Release result
     */
    @PostMapping("/{truckId}/release")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> releaseTruck(@PathVariable Integer truckId) {
        logger.info("Releasing truck {}", truckId);
        
        boolean released = truckManagementService.releaseTruck(truckId);
        
        if (released) {
            Map<String, Object> responseData = Map.of("truck_id", truckId);
            return ResponseEntity.ok(ApiResponse.success("Truck released successfully", responseData));
        } else {
            return ResponseEntity.badRequest().body(
                ApiResponse.error("Failed to release truck. Truck not found.")
            );
        }
    }
    
    /**
     * Find nearest available truck
     * 
     * @param targetX Target X coordinate
     * @param targetY Target Y coordinate
     * @return Nearest available truck
     */
    @GetMapping("/nearest")
    @PreAuthorize("hasRole('ADMIN') or hasRole('OPERATOR')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> findNearestTruck(
            @RequestParam Integer targetX,
            @RequestParam Integer targetY) {
        logger.info("Finding nearest truck to ({}, {})", targetX, targetY);
        
        Truck nearestTruck = truckManagementService.findNearestAvailableTruck(targetX, targetY);
        
        if (nearestTruck != null) {
            Map<String, Object> truckData = Map.of(
                "truck_id", nearestTruck.getTruckId(),
                "current_x", nearestTruck.getCurrentX(),
                "current_y", nearestTruck.getCurrentY(),
                "status", nearestTruck.getStatus().toString(),
                "capacity", nearestTruck.getCapacity()
            );
            
            // Calculate distance
            double distance = Math.sqrt(
                Math.pow(targetX - nearestTruck.getCurrentX(), 2) +
                Math.pow(targetY - nearestTruck.getCurrentY(), 2)
            );
            
            Map<String, Object> responseData = Map.of(
                "nearest_truck", truckData,
                "distance", Math.round(distance * 100.0) / 100.0,
                "target_position", Map.of("x", targetX, "y", targetY)
            );
            return ResponseEntity.ok(ApiResponse.success("Nearest truck found successfully", responseData));
        } else {
            return ResponseEntity.status(404).body(
                ApiResponse.error("No available trucks found")
            );
        }
    }
    
    /**
     * Batch update truck positions (for World Simulator callback use)
     * 
     * @param batchUpdate Batch update data
     * @return Update result
     */
    @PostMapping("/batch-update")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SYSTEM')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> batchUpdateTrucks(
            @RequestBody Map<String, Object> batchUpdate) {
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> truckUpdates = (List<Map<String, Object>>) batchUpdate.get("truck_updates");
        
        if (truckUpdates == null || truckUpdates.isEmpty()) {
            return ResponseEntity.badRequest().body(
                ApiResponse.error("truck_updates list is required and cannot be empty")
            );
        }
        
        truckManagementService.batchUpdateTruckPositions(truckUpdates);
        
        Map<String, Object> responseData = Map.of("updated_count", truckUpdates.size());
        return ResponseEntity.ok(ApiResponse.success("Batch update completed successfully", responseData));
    }
    
    // Private helper methods
    
    private Integer extractIntegerValue(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        return null;
    }
}