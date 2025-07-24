/**
 * Truck Management Service
 * 
 * Functionality:
 * - Manages UPS fleet scheduling and dispatch algorithms
 * - Handles truck status updates and location tracking
 * - Provides intelligent truck assignment and route optimization
 * 
 * Core Features:
 * - Intelligent truck assignment algorithms
 * - Real-time status monitoring and updates
 * - Route optimization and multi-point delivery
 * - Truck utilization statistics
 * 
 * Scheduling Algorithms:
 * - Distance priority: Select trucks closest to target
 * - Load balancing: Consider current truck load
 * - Priority sorting: Urgent orders get priority assignment
 * 
 * @author Mini-UPS Team
 * @version 1.0.0
 */
package com.miniups.service;

import com.miniups.model.entity.Shipment;
import com.miniups.model.entity.Truck;
import com.miniups.model.enums.ShipmentStatus;
import com.miniups.model.enums.TruckStatus;
import com.miniups.repository.ShipmentRepository;
import com.miniups.repository.TruckRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
public class TruckManagementService {
    
    private static final Logger logger = LoggerFactory.getLogger(TruckManagementService.class);
    
    @Autowired
    private TruckRepository truckRepository;
    
    @Autowired
    private ShipmentRepository shipmentRepository;
    
    @Autowired
    private WorldSimulatorService worldSimulatorService;
    
    /**
     * Intelligently assign truck to shipment order with retry mechanism for concurrency
     * 
     * @param originX Origin X coordinate
     * @param originY Origin Y coordinate
     * @param priority Priority level (1-5, 5 is highest)
     * @return Assigned truck, null if no available trucks
     */
    @Retryable(
        value = {OptimisticLockingFailureException.class},
        maxAttempts = 5,
        backoff = @Backoff(delay = 50, multiplier = 1.5, maxDelay = 500)
    )
    public Truck assignOptimalTruck(Integer originX, Integer originY, Integer priority) {
        return doAssignOptimalTruck(originX, originY, priority);
    }
    
    private Truck doAssignOptimalTruck(Integer originX, Integer originY, Integer priority) {
        try {
            // Find all available trucks
            List<Truck> availableTrucks = truckRepository.findByStatus(TruckStatus.IDLE);
            
            if (availableTrucks.isEmpty()) {
                logger.debug("No available trucks for assignment at ({}, {})", originX, originY);
                return null;
            }
            
            // Sort by distance and other factors to select best truck
            Truck bestTruck = findBestTruck(availableTrucks, originX, originY, priority);
            
            if (bestTruck != null) {
                // Refresh the truck entity to get the latest version
                Optional<Truck> freshTruckOpt = truckRepository.findById(bestTruck.getId());
                if (freshTruckOpt.isEmpty() || freshTruckOpt.get().getStatus() != TruckStatus.IDLE) {
                    logger.debug("Truck {} no longer available, will retry", bestTruck.getTruckId());
                    throw new OptimisticLockingFailureException("Truck no longer available");
                }
                
                Truck freshTruck = freshTruckOpt.get();
                // Update truck status to busy
                freshTruck.setStatus(TruckStatus.EN_ROUTE);
                freshTruck = truckRepository.save(freshTruck);
                
                logger.info("Assigned truck {} to pickup at ({}, {})", 
                           freshTruck.getTruckId(), originX, originY);
                return freshTruck;
            }
            
            return null;
            
        } catch (OptimisticLockingFailureException e) {
            logger.debug("Optimistic locking failure during truck assignment, will retry");
            throw e; // Re-throw to trigger retry
        } catch (Exception e) {
            logger.error("Error assigning truck", e);
            return null;
        }
    }
    
    /**
     * Get real-time status of all trucks
     * 
     * @return List of truck statuses
     */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getAllTruckStatuses() {
        List<Truck> trucks = truckRepository.findAll();
        
        return trucks.stream().map(truck -> {
            Map<String, Object> status = new HashMap<>();
            status.put("truck_id", truck.getTruckId());
            status.put("status", truck.getStatus().toString());
            status.put("status_display", truck.getStatus().getDisplayName());
            status.put("current_x", truck.getCurrentX());
            status.put("current_y", truck.getCurrentY());
            status.put("capacity", truck.getCapacity());
            status.put("available", truck.isAvailable());
            
            // Get currently assigned shipment
            Optional<Shipment> currentShipment = shipmentRepository.findByTruck(truck);
            if (currentShipment.isPresent()) {
                Shipment shipment = currentShipment.get();
                status.put("current_shipment", Map.of(
                    "shipment_id", shipment.getShipmentId(),
                    "tracking_number", shipment.getUpsTrackingId(),
                    "status", shipment.getStatus().toString(),
                    "destination_x", shipment.getDestX(),
                    "destination_y", shipment.getDestY()
                ));
            }
            
            return status;
        }).collect(Collectors.toList());
    }
    
    /**
     * Update truck location and status with retry mechanism for concurrency
     * 
     * @param truckId Truck ID
     * @param x X coordinate
     * @param y Y coordinate
     * @param status New status
     * @return Whether update was successful
     */
    @Retryable(
        value = {OptimisticLockingFailureException.class},
        maxAttempts = 3,
        backoff = @Backoff(delay = 25, multiplier = 2.0)
    )
    public boolean updateTruckStatus(Integer truckId, Integer x, Integer y, String status) {
        try {
            Optional<Truck> truckOpt = truckRepository.findByTruckId(truckId);
            if (truckOpt.isEmpty()) {
                logger.warn("Truck {} not found for status update", truckId);
                return false;
            }
            
            Truck truck = truckOpt.get();
            truck.updateLocation(x, y);
            
            // Convert status string to enum
            TruckStatus newStatus = convertWorldStatusToTruckStatus(status);
            if (newStatus != null) {
                truck.setStatus(newStatus);
            }
            
            truckRepository.save(truck);
            
            logger.debug("Updated truck {} to position ({}, {}) with status: {}", 
                        truckId, x, y, status);
            
            return true;
            
        } catch (OptimisticLockingFailureException e) {
            logger.debug("Optimistic locking failure updating truck {}, will retry", truckId);
            throw e; // Re-throw to trigger retry
        } catch (Exception e) {
            logger.error("Error updating truck status for truck {}", truckId, e);
            return false;
        }
    }
    
    /**
     * Get fleet utilization statistics
     * 
     * @return Statistical information
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getFleetStatistics() {
        List<Truck> allTrucks = truckRepository.findAll();
        
        Map<TruckStatus, Long> statusCounts = allTrucks.stream()
            .collect(Collectors.groupingBy(Truck::getStatus, Collectors.counting()));
        
        // Calculate various statistical metrics
        long totalTrucks = allTrucks.size();
        long availableTrucks = statusCounts.getOrDefault(TruckStatus.IDLE, 0L);
        long busyTrucks = totalTrucks - availableTrucks;
        
        double utilizationRate = totalTrucks > 0 ? (double) busyTrucks / totalTrucks * 100 : 0;
        
        // Count shipments by status
        Map<ShipmentStatus, Long> shipmentStatusCounts = shipmentRepository.findAll().stream()
            .collect(Collectors.groupingBy(Shipment::getStatus, Collectors.counting()));
        
        Map<String, Object> statistics = new HashMap<>();
        statistics.put("total_trucks", totalTrucks);
        statistics.put("available_trucks", availableTrucks);
        statistics.put("busy_trucks", busyTrucks);
        statistics.put("utilization_rate", Math.round(utilizationRate * 100.0) / 100.0);
        statistics.put("truck_status_breakdown", statusCounts);
        statistics.put("shipment_status_breakdown", shipmentStatusCounts);
        statistics.put("world_connected", worldSimulatorService.isConnected());
        statistics.put("world_id", worldSimulatorService.getWorldId());
        statistics.put("generated_at", LocalDateTime.now());
        
        return statistics;
    }
    
    /**
     * Release truck (after completing task)
     * 
     * @param truckId Truck ID
     * @return Whether operation was successful
     */
    public boolean releaseTruck(Integer truckId) {
        try {
            Optional<Truck> truckOpt = truckRepository.findByTruckId(truckId);
            if (truckOpt.isEmpty()) {
                logger.warn("Truck {} not found for release", truckId);
                return false;
            }
            
            Truck truck = truckOpt.get();
            truck.setStatus(TruckStatus.IDLE);
            truckRepository.save(truck);
            
            logger.info("Released truck {} back to idle status", truckId);
            return true;
            
        } catch (Exception e) {
            logger.error("Error releasing truck", e);
            return false;
        }
    }
    
    /**
     * Find nearest available truck
     * 
     * @param targetX Target X coordinate
     * @param targetY Target Y coordinate
     * @return Nearest truck, null if no available trucks
     */
    public Truck findNearestAvailableTruck(Integer targetX, Integer targetY) {
        List<Truck> availableTrucks = truckRepository.findByStatus(TruckStatus.IDLE);
        
        return availableTrucks.stream()
            .min(Comparator.comparingDouble(truck -> 
                calculateDistance(truck.getCurrentX(), truck.getCurrentY(), targetX, targetY)))
            .orElse(null);
    }
    
    /**
     * Batch update truck positions (sync from World Simulator)
     * 
     * @param truckUpdates List of truck updates
     */
    public void batchUpdateTruckPositions(List<Map<String, Object>> truckUpdates) {
        try {
            for (Map<String, Object> update : truckUpdates) {
                Integer truckId = (Integer) update.get("truck_id");
                Integer x = (Integer) update.get("x");
                Integer y = (Integer) update.get("y");
                String status = (String) update.get("status");
                
                updateTruckStatus(truckId, x, y, status);
            }
            
            logger.info("Batch updated {} truck positions", truckUpdates.size());
            
        } catch (Exception e) {
            logger.error("Error in batch update truck positions", e);
        }
    }
    
    // Private helper methods
    
    private Truck findBestTruck(List<Truck> availableTrucks, Integer originX, Integer originY, Integer priority) {
        // Simple distance-priority algorithm
        // In real applications, consider more factors: load, fuel, driver hours, etc.
        
        return availableTrucks.stream()
            .min(Comparator.comparingDouble(truck -> {
                double distance = calculateDistance(
                    truck.getCurrentX(), truck.getCurrentY(), 
                    originX, originY
                );
                
                // Can adjust weight based on priority
                double priorityWeight = priority != null ? (6 - priority) * 0.1 : 1.0;
                
                return distance * priorityWeight;
            }))
            .orElse(null);
    }
    
    private double calculateDistance(Integer x1, Integer y1, Integer x2, Integer y2) {
        if (x1 == null || y1 == null || x2 == null || y2 == null) {
            return Double.MAX_VALUE;
        }
        
        // Euclidean distance
        return Math.sqrt(Math.pow(x2 - x1, 2) + Math.pow(y2 - y1, 2));
    }
    
    private TruckStatus convertWorldStatusToTruckStatus(String worldStatus) {
        if (worldStatus == null) {
            return null;
        }
        
        return switch (worldStatus.toLowerCase()) {
            case "idle" -> TruckStatus.IDLE;
            case "traveling" -> TruckStatus.EN_ROUTE;
            case "arrive warehouse" -> TruckStatus.AT_WAREHOUSE;
            case "loading" -> TruckStatus.LOADING;
            case "delivering" -> TruckStatus.DELIVERING;
            default -> {
                logger.warn("Unknown world status: {}", worldStatus);
                yield null;
            }
        };
    }
}