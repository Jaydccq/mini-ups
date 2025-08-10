/**
 * World Simulator Management Controller
 * 
 * Features:
 * - Provides REST API for World Simulator connection management
 * - Supports system administrators controlling connections to simulation world
 * - Provides truck status query and control interfaces
 * 
 * API Endpoints:
 * - POST /api/world/connect - Connect to World Simulator
 * - POST /api/world/disconnect - Disconnect connection
 * - GET /api/world/status - Query connection status
 * - GET /api/world/trucks - Query all truck statuses
 * - POST /api/world/trucks/{truckId}/pickup - Send truck for pickup
 * - POST /api/world/trucks/{truckId}/deliver - Send truck for delivery
 * - GET /api/world/trucks/{truckId}/status - Query truck status
 * 
 * Permission Control:
 * - Requires admin or operator permissions
 * - Connection management requires elevated permissions
 * - Truck operations require operator permissions
 * 
 * @author Mini-UPS Team
 * @version 1.0.0
 */
package com.miniups.controller;

import com.miniups.model.entity.Truck;
import com.miniups.proto.WorldUpsProto;
import com.miniups.service.WorldSimulatorService;
import com.miniups.network.netty.service.NettyWorldSimulatorService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/world")
@CrossOrigin(origins = "*")
public class WorldSimulatorController {
    
    private static final Logger logger = LoggerFactory.getLogger(WorldSimulatorController.class);
    
    @Autowired(required = false)
    private WorldSimulatorService worldSimulatorService;
    
    @Autowired(required = false)
    private NettyWorldSimulatorService nettyWorldSimulatorService;
    
    /**
     * Helper method to check if any world simulator is connected.
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
     * Helper method to get available trucks from the available service.
     */
    private java.util.List<com.miniups.model.entity.Truck> getAvailableTrucks() {
        if (worldSimulatorService != null) {
            return worldSimulatorService.getAvailableTrucks();
        } else if (nettyWorldSimulatorService != null) {
            // Netty service doesn't have getAvailableTrucks method, return empty list
            return new java.util.ArrayList<>();
        }
        return new java.util.ArrayList<>();
    }
    
    /**
     * Helper method to connect to world simulator.
     */
    private boolean connectToWorldSimulator(Long worldId) {
        if (worldSimulatorService != null) {
            return worldSimulatorService.connect(worldId);
        } else if (nettyWorldSimulatorService != null) {
            // Netty service automatically connects, just check if connected
            return nettyWorldSimulatorService.isConnected();
        }
        return false;
    }
    
    /**
     * Helper method to disconnect from world simulator.
     */
    private void disconnectFromWorldSimulator() {
        if (worldSimulatorService != null) {
            worldSimulatorService.disconnect();
        } else if (nettyWorldSimulatorService != null) {
            // Netty service handles disconnect automatically in shutdown
            logger.info("Netty World Simulator Service disconnection handled automatically");
        }
    }
    
    /**
     * Helper method to send truck to pickup.
     */
    private java.util.concurrent.CompletableFuture<Boolean> sendTruckToPickup(Integer truckId, Integer warehouseId) {
        if (worldSimulatorService != null) {
            return worldSimulatorService.sendTruckToPickup(truckId, warehouseId);
        } else if (nettyWorldSimulatorService != null) {
            return nettyWorldSimulatorService.sendTruckToPickup(truckId, warehouseId);
        }
        return java.util.concurrent.CompletableFuture.completedFuture(false);
    }
    
    /**
     * Helper method to send truck to deliver.
     */
    private java.util.concurrent.CompletableFuture<Boolean> sendTruckToDeliverHelper(Integer truckId, java.util.Map<Long, int[]> deliveries) {
        if (worldSimulatorService != null) {
            return worldSimulatorService.sendTruckToDeliver(truckId, deliveries);
        } else if (nettyWorldSimulatorService != null) {
            return nettyWorldSimulatorService.sendTruckToDeliver(truckId, deliveries);
        }
        return java.util.concurrent.CompletableFuture.completedFuture(false);
    }
    
    /**
     * Helper method to query truck status.
     */
    private java.util.concurrent.CompletableFuture<WorldUpsProto.UTruck> queryTruckStatus(Integer truckId) {
        if (worldSimulatorService != null) {
            return worldSimulatorService.queryTruckStatus(truckId);
        } else if (nettyWorldSimulatorService != null) {
            return nettyWorldSimulatorService.queryTruckStatus(truckId);
        }
        return java.util.concurrent.CompletableFuture.completedFuture(null);
    }
    
    /**
     * Helper method to set simulation speed.
     */
    private void setSimulationSpeed(int speed) {
        if (worldSimulatorService != null) {
            worldSimulatorService.setSimulationSpeed(speed);
        } else if (nettyWorldSimulatorService != null) {
            nettyWorldSimulatorService.setSimulationSpeed(speed);
        }
    }
    
    /**
     * Connect to World Simulator
     * 
     * @param connectionRequest Connection request parameters
     * @return Connection result
     */
    @PostMapping("/connect")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> connectToWorld(@RequestBody(required = false) Map<String, Object> connectionRequest) {
        try {
            logger.info("Received request to connect to World Simulator");
            
            // Check if already connected
            if (isWorldSimulatorConnected()) {
                Map<String, Object> response = Map.of(
                    "success", false,
                    "message", "Already connected to World Simulator",
                    "world_id", getWorldId(),
                    "timestamp", LocalDateTime.now()
                );
                return ResponseEntity.ok(response);
            }
            
            // Extract world ID (optional)
            Long worldId = null;
            if (connectionRequest != null && connectionRequest.containsKey("world_id")) {
                Object worldIdObj = connectionRequest.get("world_id");
                if (worldIdObj instanceof Number) {
                    worldId = ((Number) worldIdObj).longValue();
                }
            }
            
            // Attempt connection
            boolean connected = connectToWorldSimulator(worldId);
            
            if (connected) {
                Map<String, Object> response = Map.of(
                    "success", true,
                    "message", "Successfully connected to World Simulator",
                    "world_id", getWorldId(),
                    "trucks_count", getAvailableTrucks().size(),
                    "timestamp", LocalDateTime.now()
                );
                return ResponseEntity.ok(response);
            } else {
                Map<String, Object> response = Map.of(
                    "success", false,
                    "message", "Failed to connect to World Simulator",
                    "timestamp", LocalDateTime.now()
                );
                return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(response);
            }
            
        } catch (Exception e) {
            logger.error("Error connecting to World Simulator", e);
            Map<String, Object> response = Map.of(
                "success", false,
                "message", "Internal error: " + e.getMessage(),
                "timestamp", LocalDateTime.now()
            );
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
    
    /**
     * Disconnect from World Simulator
     * 
     * @return Disconnect result
     */
    @PostMapping("/disconnect")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> disconnectFromWorld() {
        try {
            logger.info("Received request to disconnect from World Simulator");
            
            if (!isWorldSimulatorConnected()) {
                Map<String, Object> response = Map.of(
                    "success", false,
                    "message", "Not connected to World Simulator",
                    "timestamp", LocalDateTime.now()
                );
                return ResponseEntity.ok(response);
            }
            
            disconnectFromWorldSimulator();
            
            Map<String, Object> response = Map.of(
                "success", true,
                "message", "Successfully disconnected from World Simulator",
                "timestamp", LocalDateTime.now()
            );
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Error disconnecting from World Simulator", e);
            Map<String, Object> response = Map.of(
                "success", false,
                "message", "Internal error: " + e.getMessage(),
                "timestamp", LocalDateTime.now()
            );
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
    
    /**
     * Query World Simulator connection status
     * 
     * @return Connection status information
     */
    @GetMapping("/status")
    @PreAuthorize("hasRole('ADMIN') or hasRole('OPERATOR')")
    public ResponseEntity<Map<String, Object>> getConnectionStatus() {
        Map<String, Object> response = new HashMap<>();
        response.put("connected", isWorldSimulatorConnected());
        response.put("world_id", getWorldId());
        response.put("trucks_count", getAvailableTrucks().size());
        response.put("timestamp", LocalDateTime.now());
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Query all truck statuses
     * 
     * @return Truck status list
     */
    @GetMapping("/trucks")
    @PreAuthorize("hasRole('ADMIN') or hasRole('OPERATOR')")
    public ResponseEntity<Map<String, Object>> getAllTrucks() {
        try {
            List<Truck> trucks = getAvailableTrucks();
            
            List<Map<String, Object>> trucksData = trucks.stream()
                .map(this::mapTruckToResponse)
                .toList();
            
            Map<String, Object> response = Map.of(
                "success", true,
                "trucks", trucksData,
                "total_count", trucksData.size(),
                "connected", isWorldSimulatorConnected(),
                "timestamp", LocalDateTime.now()
            );
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Error getting truck status", e);
            Map<String, Object> response = Map.of(
                "success", false,
                "message", "Error retrieving truck information: " + e.getMessage(),
                "timestamp", LocalDateTime.now()
            );
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
    
    /**
     * Send truck to warehouse for pickup
     * 
     * @param truckId Truck ID
     * @param pickupRequest Pickup request parameters
     * @return Operation result
     */
    @PostMapping("/trucks/{truckId}/pickup")
    @PreAuthorize("hasRole('ADMIN') or hasRole('OPERATOR')")
    public ResponseEntity<Map<String, Object>> sendTruckToPickup(
            @PathVariable Integer truckId,
            @RequestBody Map<String, Object> pickupRequest) {
        try {
            logger.info("Sending truck {} to pickup", truckId);
            
            if (!isWorldSimulatorConnected()) {
                Map<String, Object> response = Map.of(
                    "success", false,
                    "message", "Not connected to World Simulator",
                    "timestamp", LocalDateTime.now()
                );
                return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(response);
            }
            
            // 提取仓库ID
            Object warehouseIdObj = pickupRequest.get("warehouse_id");
            if (warehouseIdObj == null) {
                Map<String, Object> response = Map.of(
                    "success", false,
                    "message", "warehouse_id is required",
                    "timestamp", LocalDateTime.now()
                );
                return ResponseEntity.badRequest().body(response);
            }
            
            Integer warehouseId = ((Number) warehouseIdObj).intValue();
            
            // 发送取货指令
            CompletableFuture<Boolean> future = sendTruckToPickup(truckId, warehouseId);
            
            // 等待结果
            Boolean success = future.get(10, TimeUnit.SECONDS);
            
            if (success) {
                Map<String, Object> response = Map.of(
                    "success", true,
                    "message", "Truck sent to pickup successfully",
                    "truck_id", truckId,
                    "warehouse_id", warehouseId,
                    "timestamp", LocalDateTime.now()
                );
                return ResponseEntity.ok(response);
            } else {
                Map<String, Object> response = Map.of(
                    "success", false,
                    "message", "Failed to send truck to pickup",
                    "truck_id", truckId,
                    "warehouse_id", warehouseId,
                    "timestamp", LocalDateTime.now()
                );
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
            }
            
        } catch (Exception e) {
            logger.error("Error sending truck to pickup", e);
            Map<String, Object> response = Map.of(
                "success", false,
                "message", "Internal error: " + e.getMessage(),
                "timestamp", LocalDateTime.now()
            );
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
    
    /**
     * Send truck for delivery
     * 
     * @param truckId Truck ID
     * @param deliveryRequest Delivery request parameters
     * @return Operation result
     */
    @PostMapping("/trucks/{truckId}/deliver")
    @PreAuthorize("hasRole('ADMIN') or hasRole('OPERATOR')")
    public ResponseEntity<Map<String, Object>> sendTruckToDeliver(
            @PathVariable Integer truckId,
            @RequestBody Map<String, Object> deliveryRequest) {
        try {
            logger.info("Sending truck {} to deliver", truckId);
            
            if (!isWorldSimulatorConnected()) {
                Map<String, Object> response = Map.of(
                    "success", false,
                    "message", "Not connected to World Simulator",
                    "timestamp", LocalDateTime.now()
                );
                return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(response);
            }
            
            // 提取配送信息
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> deliveries = (List<Map<String, Object>>) deliveryRequest.get("deliveries");
            
            if (deliveries == null || deliveries.isEmpty()) {
                Map<String, Object> response = Map.of(
                    "success", false,
                    "message", "deliveries list is required and cannot be empty",
                    "timestamp", LocalDateTime.now()
                );
                return ResponseEntity.badRequest().body(response);
            }
            
            // 转换配送数据
            Map<Long, int[]> deliveryMap = new HashMap<>();
            for (Map<String, Object> delivery : deliveries) {
                Long packageId = ((Number) delivery.get("package_id")).longValue();
                Integer x = ((Number) delivery.get("x")).intValue();
                Integer y = ((Number) delivery.get("y")).intValue();
                deliveryMap.put(packageId, new int[]{x, y});
            }
            
            // 发送配送指令
            CompletableFuture<Boolean> future = sendTruckToDeliverHelper(truckId, deliveryMap);
            
            // 等待结果
            Boolean success = future.get(10, TimeUnit.SECONDS);
            
            if (success) {
                Map<String, Object> response = Map.of(
                    "success", true,
                    "message", "Truck sent to deliver successfully",
                    "truck_id", truckId,
                    "deliveries_count", deliveryMap.size(),
                    "timestamp", LocalDateTime.now()
                );
                return ResponseEntity.ok(response);
            } else {
                Map<String, Object> response = Map.of(
                    "success", false,
                    "message", "Failed to send truck to deliver",
                    "truck_id", truckId,
                    "timestamp", LocalDateTime.now()
                );
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
            }
            
        } catch (Exception e) {
            logger.error("Error sending truck to deliver", e);
            Map<String, Object> response = Map.of(
                "success", false,
                "message", "Internal error: " + e.getMessage(),
                "timestamp", LocalDateTime.now()
            );
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
    
    /**
     * Query specified truck status
     * 
     * @param truckId Truck ID
     * @return Truck status information
     */
    @GetMapping("/trucks/{truckId}/status")
    @PreAuthorize("hasRole('ADMIN') or hasRole('OPERATOR')")
    public ResponseEntity<Map<String, Object>> getTruckStatus(@PathVariable Integer truckId) {
        try {
            if (!isWorldSimulatorConnected()) {
                Map<String, Object> response = Map.of(
                    "success", false,
                    "message", "Not connected to World Simulator",
                    "timestamp", LocalDateTime.now()
                );
                return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(response);
            }
            
            // 查询卡车状态
            CompletableFuture<WorldUpsProto.UTruck> future = queryTruckStatus(truckId);
            
            // 等待结果
            WorldUpsProto.UTruck truckStatus = future.get(10, TimeUnit.SECONDS);
            
            if (truckStatus != null) {
                Map<String, Object> truckData = Map.of(
                    "truck_id", truckStatus.getTruckid(),
                    "status", truckStatus.getStatus(),
                    "x", truckStatus.getX(),
                    "y", truckStatus.getY(),
                    "seqnum", truckStatus.getSeqnum()
                );
                
                Map<String, Object> response = Map.of(
                    "success", true,
                    "truck", truckData,
                    "timestamp", LocalDateTime.now()
                );
                return ResponseEntity.ok(response);
            } else {
                Map<String, Object> response = Map.of(
                    "success", false,
                    "message", "Failed to get truck status or truck not found",
                    "truck_id", truckId,
                    "timestamp", LocalDateTime.now()
                );
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
            }
            
        } catch (Exception e) {
            logger.error("Error getting truck status", e);
            Map<String, Object> response = Map.of(
                "success", false,
                "message", "Internal error: " + e.getMessage(),
                "timestamp", LocalDateTime.now()
            );
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
    
    /**
     * Set simulation speed
     * 
     * @param speedRequest Speed setting request
     * @return Operation result
     */
    @PostMapping("/simulation-speed")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> setSimulationSpeed(@RequestBody Map<String, Object> speedRequest) {
        try {
            if (!isWorldSimulatorConnected()) {
                Map<String, Object> response = Map.of(
                    "success", false,
                    "message", "Not connected to World Simulator",
                    "timestamp", LocalDateTime.now()
                );
                return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(response);
            }
            
            Object speedObj = speedRequest.get("speed");
            if (speedObj == null) {
                Map<String, Object> response = Map.of(
                    "success", false,
                    "message", "speed parameter is required",
                    "timestamp", LocalDateTime.now()
                );
                return ResponseEntity.badRequest().body(response);
            }
            
            int speed = ((Number) speedObj).intValue();
            
            setSimulationSpeed(speed);
            
            Map<String, Object> response = Map.of(
                "success", true,
                "message", "Simulation speed updated successfully",
                "speed", speed,
                "timestamp", LocalDateTime.now()
            );
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Error setting simulation speed", e);
            Map<String, Object> response = Map.of(
                "success", false,
                "message", "Internal error: " + e.getMessage(),
                "timestamp", LocalDateTime.now()
            );
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
    
    // Private helper methods
    
    private Map<String, Object> mapTruckToResponse(Truck truck) {
        Map<String, Object> truckMap = new HashMap<>();
        truckMap.put("id", truck.getId());
        truckMap.put("truck_id", truck.getTruckId());
        truckMap.put("status", truck.getStatus().toString());
        truckMap.put("status_display", truck.getStatus().getDisplayName());
        truckMap.put("current_x", truck.getCurrentX());
        truckMap.put("current_y", truck.getCurrentY());
        truckMap.put("capacity", truck.getCapacity());
        truckMap.put("driver_id", truck.getDriverId());
        truckMap.put("world_id", truck.getWorldId());
        truckMap.put("available", truck.isAvailable());
        truckMap.put("created_at", truck.getCreatedAt());
        truckMap.put("updated_at", truck.getUpdatedAt());
        
        return truckMap;
    }
}