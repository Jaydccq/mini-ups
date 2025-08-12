/**
 * Debug Controller
 * 
 * Functionality:
 * - Provides debug endpoints for monitoring Amazon-UPS communication
 * - RESTful API for debug interface frontend
 * - Real-time communication log viewing and filtering
 * 
 * Endpoints:
 * - GET /api/debug/communications - Get filtered communication logs
 * - GET /api/debug/statistics - Get communication statistics
 * - GET /api/debug/shipment/{id}/logs - Get logs for specific shipment
 * - GET /api/debug/errors - Get recent errors
 * 
 *
 
 */
package com.miniups.controller;

import com.miniups.model.entity.CommunicationLog;
import com.miniups.service.CommunicationLogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/debug")
@CrossOrigin(origins = "*")
public class DebugController {
    
    @Autowired
    private CommunicationLogService communicationLogService;
    
    /**
     * Get communication logs with optional filtering
     */
    @GetMapping("/communications")
    public ResponseEntity<Map<String, Object>> getCommunicationLogs(
            @RequestParam(required = false) String direction,
            @RequestParam(required = false) String messageType,
            @RequestParam(required = false) Boolean success,
            @RequestParam(defaultValue = "24") int hoursBack,
            @RequestParam(defaultValue = "50") int limit) {
        
        List<CommunicationLog> logs = communicationLogService.getFilteredLogs(
            direction, messageType, success, hoursBack);
        
        Map<String, Object> response = new HashMap<>();
        response.put("logs", logs);
        response.put("count", logs.size());
        response.put("filters", Map.of(
            "direction", direction,
            "messageType", messageType,
            "success", success,
            "hoursBack", hoursBack
        ));
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Get communication statistics for dashboard
     */
    @GetMapping("/statistics")
    public ResponseEntity<Map<String, Object>> getStatistics(
            @RequestParam(defaultValue = "24") int hoursBack) {
        
        Map<String, Object> stats = communicationLogService.getCommunicationStatistics(hoursBack);
        stats.put("timeRange", hoursBack + " hours");
        
        return ResponseEntity.ok(stats);
    }
    
    /**
     * Get logs for specific shipment
     */
    @GetMapping("/shipment/{shipmentId}/logs")
    public ResponseEntity<Map<String, Object>> getShipmentLogs(@PathVariable String shipmentId) {
        List<CommunicationLog> logs = communicationLogService.getLogsForShipment(shipmentId);
        
        Map<String, Object> response = new HashMap<>();
        response.put("shipmentId", shipmentId);
        response.put("logs", logs);
        response.put("count", logs.size());
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Get recent errors for troubleshooting
     */
    @GetMapping("/errors")
    public ResponseEntity<Map<String, Object>> getRecentErrors() {
        List<CommunicationLog> errors = communicationLogService.getFilteredLogs(
            null, null, false, 48); // Last 48 hours of errors
        
        Map<String, Object> response = new HashMap<>();
        response.put("errors", errors);
        response.put("count", errors.size());
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Get system health status
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> getSystemHealth() {
        Map<String, Object> health = new HashMap<>();
        
        // Get recent communication stats (last hour)
        Map<String, Object> recentStats = communicationLogService.getCommunicationStatistics(1);
        
        @SuppressWarnings("unchecked")
        Map<String, Long> successStats = (Map<String, Long>) recentStats.get("successStats");
        
        long successful = successStats.getOrDefault("successful", 0L);
        long failed = successStats.getOrDefault("failed", 0L);
        long total = successful + failed;
        
        double successRate = total > 0 ? (double) successful / total * 100 : 100.0;
        
        health.put("successRate", Math.round(successRate * 100.0) / 100.0);
        health.put("totalMessages", total);
        health.put("failedMessages", failed);
        health.put("averageProcessingTime", recentStats.get("averageProcessingTimeMs"));
        health.put("status", successRate >= 90 ? "healthy" : successRate >= 70 ? "warning" : "critical");
        
        return ResponseEntity.ok(health);
    }
    
    /**
     * Clear old logs (admin endpoint)
     */
    @DeleteMapping("/logs/cleanup")
    public ResponseEntity<Map<String, Object>> cleanupOldLogs(
            @RequestParam(defaultValue = "168") int olderThanHours) {
        
        // This is a placeholder - implement cleanup logic as needed
        Map<String, Object> response = new HashMap<>();
        response.put("message", "Cleanup functionality not implemented yet");
        response.put("olderThanHours", olderThanHours);
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Redirect to debug interface
     */
    @GetMapping({"/", "/interface"})
    public String debugInterface() {
        return "redirect:/debug.html";
    }
}