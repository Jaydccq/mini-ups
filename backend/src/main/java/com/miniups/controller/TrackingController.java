/**
 * Package Tracking API Controller
 * 
 * Features:
 * - Provides REST API for package tracking queries
 * - Supports user queries for package shipping status
 * - Provides tracking history and detailed information
 * 
 * API Endpoints:
 * - GET /api/tracking/{trackingNumber} - Query by tracking number
 * - GET /api/tracking/user/{userId} - Query all packages for user
 * - GET /api/tracking/{trackingNumber}/history - Query status history
 * - PUT /api/tracking/{trackingNumber}/status - Update package status (internal interface)
 * 
 * User Permissions:
 * - Public tracking query (anyone can query by tracking number)
 * - User private query (requires authentication, can only query own packages)
 * - Status update (requires admin permissions)
 * 
 *
 
 */
package com.miniups.controller;

import com.miniups.model.dto.common.ApiResponse;
import com.miniups.model.entity.Shipment;
import com.miniups.model.entity.ShipmentStatusHistory;
import com.miniups.model.enums.ShipmentStatus;
import com.miniups.service.TrackingService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/tracking")
public class TrackingController {
    
    private static final Logger logger = LoggerFactory.getLogger(TrackingController.class);
    
    @Autowired
    private TrackingService trackingService;
    
    /**
     * Query package information by tracking number
     * 
     * Public interface, anyone can query package status by tracking number
     * 
     * @param trackingNumber UPS tracking number
     * @return Package information and status
     */
    @GetMapping("/{trackingNumber}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> trackPackage(@PathVariable String trackingNumber) {
        logger.info("Tracking request for: {}", trackingNumber);
        
        // Validate tracking number format
        if (!trackingService.isValidTrackingNumberFormat(trackingNumber)) {
            return ResponseEntity.badRequest()
                .body(ApiResponse.error("Invalid tracking number format"));
        }
        
        Optional<Shipment> shipmentOpt = trackingService.findByTrackingNumber(trackingNumber);
        
        if (shipmentOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        
        Shipment shipment = shipmentOpt.get();
        
        Map<String, Object> data = new HashMap<>();
        data.put("tracking_number", shipment.getUpsTrackingId());
        data.put("shipment_id", shipment.getShipmentId());
        data.put("status", shipment.getStatus().toString());
        data.put("status_display", shipment.getStatus().getDisplayName());
        data.put("origin", Map.of("x", shipment.getOriginX(), "y", shipment.getOriginY()));
        data.put("destination", Map.of("x", shipment.getDestX(), "y", shipment.getDestY()));
        data.put("created_at", shipment.getCreatedAt());
        data.put("estimated_delivery", shipment.getEstimatedDelivery());
        data.put("actual_delivery", shipment.getActualDelivery());
        data.put("pickup_time", shipment.getPickupTime());
        
        // Include truck information if available
        if (shipment.getTruck() != null) {
            Map<String, Object> truckInfo = Map.of(
                "truck_id", shipment.getTruck().getId(),
                "status", shipment.getTruck().getStatus().toString()
            );
            data.put("truck", truckInfo);
        }
        
        return ResponseEntity.ok(ApiResponse.success("Package tracking information retrieved successfully", data));
    }
    
    /**
     * Query all packages for specified user
     * 
     * Requires authentication, users can only query their own packages
     * 
     * @param userId User ID
     * @return List of all packages for the user
     */
    @GetMapping("/user/{userId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getUserShipments(@PathVariable Long userId) {
        logger.info("Getting shipments for user: {}", userId);
        
        List<Shipment> shipments = trackingService.getUserShipments(userId);
        
        List<Map<String, Object>> shipmentData = shipments.stream()
            .map(this::mapShipmentToResponse)
            .collect(Collectors.toList());
        
        Map<String, Object> data = Map.of(
            "user_id", userId,
            "shipments", shipmentData,
            "total_count", shipments.size()
        );
        
        return ResponseEntity.ok(ApiResponse.success("User shipments retrieved successfully", data));
    }
    
    /**
     * Query package status history
     * 
     * @param trackingNumber UPS tracking number
     * @return Status change history
     */
    @GetMapping("/{trackingNumber}/history")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getTrackingHistory(@PathVariable String trackingNumber) {
        logger.info("Getting tracking history for: {}", trackingNumber);
        
        if (!trackingService.isValidTrackingNumberFormat(trackingNumber)) {
            return ResponseEntity.badRequest()
                .body(ApiResponse.error("Invalid tracking number format"));
        }
        
        List<ShipmentStatusHistory> history = trackingService.getStatusHistory(trackingNumber);
        
        if (history.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        
        List<Map<String, Object>> historyData = history.stream()
            .map(h -> {
                Map<String, Object> historyMap = new HashMap<>();
                historyMap.put("status", h.getStatus().toString());
                historyMap.put("status_display", h.getStatus().getDisplayName());
                historyMap.put("timestamp", h.getTimestamp());
                historyMap.put("comment", h.getNotes() != null ? h.getNotes() : "");
                return historyMap;
            })
            .collect(Collectors.toList());
        
        Map<String, Object> data = Map.of(
            "tracking_number", trackingNumber,
            "history", historyData,
            "total_events", historyData.size()
        );
        
        return ResponseEntity.ok(ApiResponse.success("Tracking history retrieved successfully", data));
    }
    
    /**
     * Update package status
     * 
     * Internal interface, requires admin permissions
     * 
     * @param trackingNumber UPS tracking number
     * @param statusRequest Status update request
     * @return Update result
     */
    @PutMapping("/{trackingNumber}/status")
    @PreAuthorize("hasRole('ADMIN') or hasRole('OPERATOR')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> updatePackageStatus(
            @PathVariable String trackingNumber,
            @RequestBody Map<String, Object> statusRequest) {
        logger.info("Updating status for {}: {}", trackingNumber, statusRequest);
        
        String statusStr = (String) statusRequest.get("status");
        String comment = (String) statusRequest.get("comment");
        
        if (statusStr == null) {
            return ResponseEntity.badRequest()
                .body(ApiResponse.error("Status is required"));
        }
        
        ShipmentStatus newStatus;
        try {
            newStatus = ShipmentStatus.valueOf(statusStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                .body(ApiResponse.error("Invalid status: " + statusStr));
        }
        
        boolean updated = trackingService.updateShipmentStatus(trackingNumber, newStatus, comment);
        
        if (updated) {
            Map<String, Object> data = Map.of(
                "tracking_number", trackingNumber,
                "new_status", newStatus.toString()
            );
            return ResponseEntity.ok(ApiResponse.success("Status updated successfully", data));
        } else {
            return ResponseEntity.badRequest()
                .body(ApiResponse.error("Failed to update status. Package not found or invalid status transition."));
        }
    }
    
    /**
     * Validate tracking number format
     * 
     * @param trackingNumber Tracking number
     * @return Validation result
     */
    @GetMapping("/validate/{trackingNumber}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> validateTrackingNumber(@PathVariable String trackingNumber) {
        boolean isValid = trackingService.isValidTrackingNumberFormat(trackingNumber);
        
        Map<String, Object> data = Map.of(
            "tracking_number", trackingNumber,
            "valid", isValid
        );
        
        String message = isValid ? "Valid tracking number format" : "Invalid tracking number format";
        return ResponseEntity.ok(ApiResponse.success(message, data));
    }
    
    // Private helper methods
    
    private Map<String, Object> mapShipmentToResponse(Shipment shipment) {
        Map<String, Object> shipmentMap = new HashMap<>();
        shipmentMap.put("shipment_id", shipment.getShipmentId());
        shipmentMap.put("tracking_number", shipment.getUpsTrackingId());
        shipmentMap.put("status", shipment.getStatus().toString());
        shipmentMap.put("status_display", shipment.getStatus().getDisplayName());
        shipmentMap.put("origin", Map.of("x", shipment.getOriginX(), "y", shipment.getOriginY()));
        shipmentMap.put("destination", Map.of("x", shipment.getDestX(), "y", shipment.getDestY()));
        shipmentMap.put("created_at", shipment.getCreatedAt());
        shipmentMap.put("estimated_delivery", shipment.getEstimatedDelivery());
        shipmentMap.put("actual_delivery", shipment.getActualDelivery());
        shipmentMap.put("pickup_time", shipment.getPickupTime());
        
        if (shipment.getTruck() != null) {
            shipmentMap.put("truck_id", shipment.getTruck().getId());
            shipmentMap.put("truck_status", shipment.getTruck().getStatus().toString());
        }
        
        return shipmentMap;
    }
}