/**
 * Admin Debug Controller
 * 
 * Purpose:
 * - REST API endpoints for World Simulator debugging functionality
 * - Provides admin interface for debug message history and statistics
 * - Controls debug system state and configuration
 * - Serves data for admin debugging dashboard
 * 
 * Features:
 * - Message history retrieval with pagination
 * - Real-time statistics and performance metrics
 * - Debug system control (enable/disable, clear cache)
 * - Message filtering and search capabilities
 * - Connection health monitoring
 * 
 * Security:
 * - Admin role required for all endpoints
 * - Request validation and sanitization
 * - Rate limiting to prevent abuse
 * - Audit logging for admin actions
 * 
 * Endpoints:
 * - GET /api/admin/debug/simulator/messages - Get recent messages
 * - GET /api/admin/debug/simulator/stats - Get current statistics
 * - POST /api/admin/debug/simulator/clear - Clear message cache
 * - GET /api/admin/debug/simulator/status - Get debug system status
 * 
 *
 
 */
package com.miniups.controller;

import com.miniups.debug.WorldSimulatorDebugEventListener;
import com.miniups.model.dto.debug.WorldSimulatorDebugMessageDto;
import com.miniups.model.dto.common.ApiResponse;
import com.miniups.service.WorldSimulatorService;
import com.miniups.network.netty.service.NettyWorldSimulatorService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/debug")
@PreAuthorize("hasRole('ADMIN')")
@ConditionalOnProperty(name = "app.debug.world-simulator.enabled", havingValue = "true", matchIfMissing = false)
public class AdminDebugController {
    
    private static final Logger logger = LoggerFactory.getLogger(AdminDebugController.class);
    
    @Autowired
    private WorldSimulatorDebugEventListener debugEventListener;
    
    @Autowired(required = false)
    private WorldSimulatorService worldSimulatorService;
    
    @Autowired(required = false)
    private NettyWorldSimulatorService nettyWorldSimulatorService;
    
    @Value("${app.debug.world-simulator.enabled:false}")
    private boolean debugEnabled;
    
    @Value("${app.debug.world-simulator.buffer-size:5000}")
    private int bufferSize;
    
    @Value("${app.debug.world-simulator.retention-seconds:300}")
    private int retentionSeconds;
    
    /**
     * Helper method to get the available world simulator service (socket or netty).
     */
    private boolean isWorldSimulatorConnected() {
        if (worldSimulatorService != null) {
            return worldSimulatorService.isConnected();
        } else if (nettyWorldSimulatorService != null) {
            return nettyWorldSimulatorService.isConnected();
        }
        return false;
    }
    
    /**
     * Helper method to get world ID from the available service.
     */
    private Long getWorldId() {
        if (worldSimulatorService != null) {
            return worldSimulatorService.getWorldId();
        } else if (nettyWorldSimulatorService != null) {
            return nettyWorldSimulatorService.getWorldId();
        }
        return null;
    }
    
    /**
     * Helper method to test connection health from the available service.
     */
    private boolean testConnectionHealth() {
        if (worldSimulatorService != null) {
            return worldSimulatorService.isConnectionHealthy();
        } else if (nettyWorldSimulatorService != null) {
            return nettyWorldSimulatorService.isConnected(); // Netty service doesn't have isConnectionHealthy method
        }
        return false;
    }
    
    /**
     * Helper method to perform connection test from the available service.
     */
    private boolean performConnectionTest() {
        if (worldSimulatorService != null) {
            return worldSimulatorService.testConnection();
        } else if (nettyWorldSimulatorService != null) {
            return nettyWorldSimulatorService.isConnected(); // For Netty, just check if connected
        }
        return false;
    }
    
    /**
     * Get recent World Simulator debug messages
     * 
     * @param limit Maximum number of messages to return (default: 100, max: 1000)
     * @return List of recent debug messages
     */
    @GetMapping("/simulator/messages")
    public ResponseEntity<ApiResponse<List<WorldSimulatorDebugMessageDto>>> getRecentMessages(
            @RequestParam(defaultValue = "100") int limit) {
        
        try {
            // Validate limit parameter
            if (limit < 1) {
                limit = 100;
            } else if (limit > 1000) {
                limit = 1000;
            }
            
            List<WorldSimulatorDebugMessageDto> messages = debugEventListener.getRecentMessages(limit);
            
            logger.debug("Retrieved {} recent debug messages for admin", messages.size());
            
            return ResponseEntity.ok(ApiResponse.success(
                String.format("Retrieved %d recent debug messages", messages.size()),
                messages
            ));
            
        } catch (Exception e) {
            logger.error("Error retrieving recent debug messages", e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("Failed to retrieve debug messages"));
        }
    }
    
    /**
     * Get World Simulator debug statistics
     * 
     * @return Current debug statistics and performance metrics
     */
    @GetMapping("/simulator/stats")
    public ResponseEntity<ApiResponse<WorldSimulatorDebugEventListener.DebugStatistics>> getStatistics() {
        try {
            WorldSimulatorDebugEventListener.DebugStatistics stats = debugEventListener.getStatistics();
            
            logger.debug("Retrieved debug statistics: {} total messages", stats.getTotalMessages());
            
            return ResponseEntity.ok(ApiResponse.success(
                "Debug statistics retrieved successfully",
                stats
            ));
            
        } catch (Exception e) {
            logger.error("Error retrieving debug statistics", e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("Failed to retrieve debug statistics"));
        }
    }
    
    /**
     * Clear debug message cache and reset statistics
     * 
     * @return Confirmation of cache clear operation
     */
    @PostMapping("/simulator/clear")
    public ResponseEntity<ApiResponse<String>> clearCache() {
        try {
            debugEventListener.clearCache();
            
            logger.info("Debug message cache cleared by admin");
            
            return ResponseEntity.ok(ApiResponse.success(
                "Debug message cache and statistics have been reset",
                "Cache cleared successfully"
            ));
            
        } catch (Exception e) {
            logger.error("Error clearing debug message cache", e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("Failed to clear debug cache"));
        }
    }
    
    /**
     * Get World Simulator debug system status
     * 
     * @return Current status of the debug system and World Simulator connection
     */
    @GetMapping("/simulator/status")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getDebugStatus() {
        try {
            Map<String, Object> status = new HashMap<>();
            
            // Debug system configuration
            status.put("debugEnabled", debugEnabled);
            status.put("bufferSize", bufferSize);
            status.put("retentionSeconds", retentionSeconds);
            
            // World Simulator connection status
            status.put("simulatorConnected", isWorldSimulatorConnected());
            status.put("connectionHealthy", testConnectionHealth());
            status.put("worldId", getWorldId());
            
            // Debug statistics
            WorldSimulatorDebugEventListener.DebugStatistics stats = debugEventListener.getStatistics();
            status.put("totalMessages", stats.getTotalMessages());
            status.put("cachedMessages", stats.getCachedMessages());
            status.put("messagesPerSecond", stats.getMessagesPerSecond());
            status.put("uptime", java.time.Duration.between(stats.getStartTime(), stats.getCurrentTime()).getSeconds());
            
            logger.debug("Retrieved debug system status");
            
            return ResponseEntity.ok(ApiResponse.success(
                "Debug system status retrieved successfully",
                status
            ));
            
        } catch (Exception e) {
            logger.error("Error retrieving debug system status", e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("Failed to retrieve debug system status"));
        }
    }
    
    /**
     * Broadcast current statistics to WebSocket subscribers
     * 
     * @return Confirmation of statistics broadcast
     */
    @PostMapping("/simulator/broadcast-stats")
    public ResponseEntity<ApiResponse<String>> broadcastStatistics() {
        try {
            debugEventListener.broadcastStatistics();
            
            logger.debug("Statistics broadcasted to WebSocket subscribers by admin");
            
            return ResponseEntity.ok(ApiResponse.success(
                "Current debug statistics have been sent to all connected clients",
                "Statistics broadcasted successfully"
            ));
            
        } catch (Exception e) {
            logger.error("Error broadcasting statistics", e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("Failed to broadcast statistics"));
        }
    }
    
    /**
     * Test World Simulator connection health
     * 
     * @return Connection test results
     */
    @PostMapping("/simulator/test-connection")
    public ResponseEntity<ApiResponse<Map<String, Object>>> testConnection() {
        try {
            Map<String, Object> result = new HashMap<>();
            
            boolean connectionHealthy = performConnectionTest();
            
            result.put("connectionHealthy", connectionHealthy);
            result.put("connected", isWorldSimulatorConnected());
            result.put("worldId", getWorldId());
            result.put("testTimestamp", java.time.LocalDateTime.now());
            
            String message = connectionHealthy ? 
                "Connection test passed" : 
                "Connection test failed - check World Simulator status";
            
            logger.info("World Simulator connection test: {}", connectionHealthy ? "PASSED" : "FAILED");
            
            return ResponseEntity.ok(ApiResponse.success(message, result));
            
        } catch (Exception e) {
            logger.error("Error testing World Simulator connection", e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("Failed to test connection"));
        }
    }
}