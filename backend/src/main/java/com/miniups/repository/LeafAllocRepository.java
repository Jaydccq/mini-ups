package com.miniups.repository;

import com.miniups.model.entity.LeafAlloc;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Leaf Allocation Repository
 * 
 * Provides optimized data access operations for the Leaf-Segment distributed ID
 * generation algorithm. This repository implements atomic segment allocation
 * operations with optimistic locking to prevent race conditions in distributed
 * deployments.
 * 
 * Key Operations:
 * - Atomic segment allocation with version-based optimistic locking
 * - Business tag-based ID sequence management
 * - Performance monitoring and health check queries
 * - Administrative operations for step size tuning
 * 
 * Performance Characteristics:
 * - Sub-millisecond query execution for segment allocation
 * - Optimized indexes for high-throughput scenarios
 * - Minimal lock contention through optimistic locking strategy
 * - Efficient batch operations for administrative tasks
 * 
 * @author Mini-UPS Development Team
 * @version 1.0
 */
@Repository
public interface LeafAllocRepository extends JpaRepository<LeafAlloc, Long> {
    
    /**
     * Find allocation by business tag
     * 
     * This is the primary lookup method for ID generation. The business tag
     * uniquely identifies the ID sequence type (e.g., "shipment", "user").
     * 
     * @param bizTag The business tag identifier
     * @return Optional LeafAlloc for the specified business tag
     */
    Optional<LeafAlloc> findByBizTag(String bizTag);
    
    /**
     * Atomically allocate the next segment for a business tag
     * 
     * This method implements the core of the Leaf-Segment algorithm by atomically
     * incrementing the maxId field and returning the allocated segment boundaries.
     * The version field provides optimistic locking to handle concurrent allocation
     * attempts across multiple application instances.
     * 
     * @param bizTag Business tag to allocate segment for
     * @param currentVersion Current version for optimistic locking
     * @return Number of rows updated (1 if successful, 0 if version conflict)
     */
    @Modifying
    @Query("""
        UPDATE LeafAlloc l 
        SET l.maxId = l.maxId + l.step,
            l.version = l.version + 1,
            l.lastAllocTime = CURRENT_TIMESTAMP,
            l.updateTime = CURRENT_TIMESTAMP
        WHERE l.bizTag = :bizTag 
        AND l.version = :currentVersion
        AND l.active = true
        """)
    int allocateNextSegment(@Param("bizTag") String bizTag, 
                           @Param("currentVersion") Long currentVersion);
    
    /**
     * Update step size for a business tag
     * 
     * Allows dynamic adjustment of segment sizes based on allocation patterns
     * and traffic load. Larger step sizes reduce database load but increase
     * memory usage.
     * 
     * @param bizTag Business tag to update
     * @param newStep New segment size
     * @param currentVersion Current version for optimistic locking
     * @return Number of rows updated
     */
    @Modifying
    @Query("""
        UPDATE LeafAlloc l 
        SET l.step = :newStep,
            l.version = l.version + 1,
            l.updateTime = CURRENT_TIMESTAMP
        WHERE l.bizTag = :bizTag 
        AND l.version = :currentVersion
        """)
    int updateStepSize(@Param("bizTag") String bizTag, 
                      @Param("newStep") Integer newStep,
                      @Param("currentVersion") Long currentVersion);
    
    /**
     * Update allocation rate statistics for monitoring
     * 
     * @param bizTag Business tag to update
     * @param avgRate Average allocation rate (allocations per second)
     * @return Number of rows updated
     */
    @Modifying
    @Query("""
        UPDATE LeafAlloc l 
        SET l.avgRate = :avgRate,
            l.updateTime = CURRENT_TIMESTAMP
        WHERE l.bizTag = :bizTag
        """)
    int updateAllocationRate(@Param("bizTag") String bizTag, 
                            @Param("avgRate") Double avgRate);
    
    /**
     * Find all active allocations
     * 
     * @return List of all active LeafAlloc entries
     */
    List<LeafAlloc> findByActiveTrue();
    
    /**
     * Find allocations that haven't been used recently
     * 
     * Useful for identifying inactive ID sequences that might be candidates
     * for cleanup or step size reduction.
     * 
     * @param threshold Timestamp threshold for "recent" usage
     * @return List of allocations not used since threshold
     */
    @Query("""
        SELECT l FROM LeafAlloc l 
        WHERE l.active = true 
        AND (l.lastAllocTime IS NULL OR l.lastAllocTime < :threshold)
        """)
    List<LeafAlloc> findInactiveAllocations(@Param("threshold") Instant threshold);
    
    /**
     * Find allocations with high allocation frequency
     * 
     * Identifies hot ID sequences that might benefit from larger step sizes
     * to reduce database load.
     * 
     * @param rateThreshold Minimum allocation rate threshold
     * @return List of high-frequency allocations
     */
    @Query("""
        SELECT l FROM LeafAlloc l 
        WHERE l.active = true 
        AND l.avgRate IS NOT NULL 
        AND l.avgRate > :rateThreshold
        ORDER BY l.avgRate DESC
        """)
    List<LeafAlloc> findHighFrequencyAllocations(@Param("rateThreshold") Double rateThreshold);
    
    /**
     * Get allocation statistics for monitoring dashboard
     * 
     * @return Aggregated statistics about all allocations
     */
    @Query("""
        SELECT new map(
            COUNT(l) as totalAllocations,
            COUNT(CASE WHEN l.active = true THEN 1 END) as activeAllocations,
            AVG(l.step) as avgStepSize,
            MAX(l.step) as maxStepSize,
            MIN(l.step) as minStepSize,
            AVG(l.avgRate) as avgAllocationRate,
            MAX(l.maxId) as maxIdAllocated
        )
        FROM LeafAlloc l
        """)
    java.util.Map<String, Object> getAllocationStatistics();
    
    /**
     * Find allocations requiring step size adjustment
     * 
     * Returns allocations where auto-tuning is enabled (minStep and maxStep are set)
     * and recent allocation patterns suggest the step size should be adjusted.
     * 
     * @return List of allocations that might benefit from step size adjustment
     */
    @Query("""
        SELECT l FROM LeafAlloc l 
        WHERE l.active = true 
        AND l.minStep IS NOT NULL 
        AND l.maxStep IS NOT NULL
        AND l.lastAllocTime IS NOT NULL
        AND l.lastAllocTime > :recentThreshold
        ORDER BY l.avgRate DESC
        """)
    List<LeafAlloc> findAllocationsForTuning(@Param("recentThreshold") Instant recentThreshold);
    
    /**
     * Initialize a new business tag allocation
     * 
     * Creates a new LeafAlloc entry for a business tag if it doesn't exist.
     * This method uses native SQL to handle the INSERT OR IGNORE pattern
     * safely across different database vendors.
     * 
     * @param bizTag Business tag to initialize
     * @param initialStep Initial segment size
     * @param description Descriptive name for this allocation
     * @return Number of rows inserted (1 if new, 0 if already exists)
     */
    @Modifying
    @Query(value = """
        INSERT INTO leaf_alloc (biz_tag, max_id, step, version, description, active, update_time)
        SELECT :bizTag, 0, :initialStep, 0, :description, true, CURRENT_TIMESTAMP
        WHERE NOT EXISTS (
            SELECT 1 FROM leaf_alloc WHERE biz_tag = :bizTag
        )
        """, nativeQuery = true)
    int initializeAllocation(@Param("bizTag") String bizTag,
                           @Param("initialStep") Integer initialStep,
                           @Param("description") String description);
    
    /**
     * Disable allocation for a business tag
     * 
     * Sets active = false to temporarily stop ID generation for a business type.
     * 
     * @param bizTag Business tag to disable
     * @return Number of rows updated
     */
    @Modifying
    @Query("""
        UPDATE LeafAlloc l 
        SET l.active = false,
            l.updateTime = CURRENT_TIMESTAMP
        WHERE l.bizTag = :bizTag
        """)
    int disableAllocation(@Param("bizTag") String bizTag);
    
    /**
     * Enable allocation for a business tag
     * 
     * Sets active = true to resume ID generation for a business type.
     * 
     * @param bizTag Business tag to enable
     * @return Number of rows updated
     */
    @Modifying
    @Query("""
        UPDATE LeafAlloc l 
        SET l.active = true,
            l.updateTime = CURRENT_TIMESTAMP
        WHERE l.bizTag = :bizTag
        """)
    int enableAllocation(@Param("bizTag") String bizTag);
    
    /**
     * Get current allocation status for health checks
     * 
     * @param bizTag Business tag to check
     * @return Map containing current status information
     */
    @Query("""
        SELECT new map(
            l.bizTag as bizTag,
            l.maxId as maxId,
            l.step as step,
            l.active as active,
            l.avgRate as avgRate,
            l.lastAllocTime as lastAllocTime,
            l.updateTime as updateTime,
            l.version as version
        )
        FROM LeafAlloc l 
        WHERE l.bizTag = :bizTag
        """)
    Optional<java.util.Map<String, Object>> getAllocationStatus(@Param("bizTag") String bizTag);
}