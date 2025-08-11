/**
 * Truck Data Access Interface
 * 
 * Function Description:
 * - Manages data access operations for the UPS fleet
 * - Supports core functions such as truck dispatching, status management, and driver assignment
 * - Provides data support for transport order assignment and tracking
 * 
 * Core Query Methods:
 * - findByTruckId: Find by truck ID from the world simulator (system integration)
 * - findByStatus: Find trucks by status (core of dispatching algorithm)
 * - findByDriverId: Find assigned vehicle for a driver (driver management)
 * - existsByTruckId: Verify if truck ID exists (data consistency)
 * 
 * Business Application Scenarios:
 * - Truck dispatching algorithm: Find available trucks (IDLE status)
 * - Transport task assignment: Assign optimal trucks based on location and load capacity
 * - Driver management: Query vehicle assignments for drivers
 * - Fleet monitoring: Real-time view of all truck statuses
 * - World simulator integration: Synchronize truck positions and statuses
 * 
 * Performance Optimization:
 * - Fast lookup of available trucks based on status index
 * - Efficient verification of truck existence with exists methods
 * - Support for batch status update operations
 * 
 * Dispatching Algorithm Support:
 * - Find the nearest available truck
 * - Filter suitable vehicles by load capacity
 * - Support multi-point delivery route optimization
 * 
 *
 
 */
package com.miniups.repository;

import com.miniups.model.entity.Truck;
import com.miniups.model.enums.TruckStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TruckRepository extends JpaRepository<Truck, Long> {
    
    Optional<Truck> findByTruckId(Integer truckId);
    
    List<Truck> findByStatus(TruckStatus status);
    
    List<Truck> findByDriverId(Long driverId);
    
    boolean existsByTruckId(Integer truckId);
    
    @Query("SELECT COUNT(t) FROM Truck t WHERE t.status = :status")
    long countByStatus(@Param("status") TruckStatus status);
    
    /**
     * Find available trucks using PostgreSQL SKIP LOCKED for high concurrency
     * This prevents deadlocks by skipping already locked trucks
     */
    @Query(value = """
        SELECT * FROM trucks 
        WHERE status = 'IDLE' 
        ORDER BY id ASC 
        FOR UPDATE SKIP LOCKED
        """, nativeQuery = true)
    List<Truck> findIdleForUpdateSkipLocked();
    
    /**
     * Find nearest available truck for assignment using SKIP LOCKED
     * This prevents deadlocks by skipping already locked trucks
     */
    @Query(value = """
        SELECT * FROM trucks 
        WHERE status = 'IDLE' 
        AND ABS(current_x - :originX) + ABS(current_y - :originY) < 1000
        ORDER BY (ABS(current_x - :originX) + ABS(current_y - :originY)) ASC
        LIMIT 1
        FOR UPDATE SKIP LOCKED
        """, nativeQuery = true)
    Optional<Truck> findNearestAvailableTruckForAssignment(@Param("originX") Integer originX, @Param("originY") Integer originY);
    
    /**
     * Get the truck that was just assigned atomically
     */
    @Query("SELECT t FROM Truck t WHERE t.status = 'EN_ROUTE' ORDER BY t.updatedAt DESC")
    List<Truck> findRecentlyAssignedTrucks();
    
    /**
     * Find and lock one available truck for assignment - optimized for high concurrency
     * Uses SKIP LOCKED to prevent blocking when multiple threads compete for trucks
     */
    @Query(value = """
        SELECT * FROM trucks 
        WHERE status = 'IDLE' 
        ORDER BY id ASC 
        LIMIT 1
        FOR UPDATE SKIP LOCKED
        """, nativeQuery = true)
    Optional<Truck> findAndLockOneAvailableTruck();
}