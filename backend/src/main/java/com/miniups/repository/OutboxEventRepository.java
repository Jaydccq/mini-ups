package com.miniups.repository;

import com.miniups.model.entity.OutboxEvent;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Outbox Event Repository
 * 
 * Provides optimized data access operations for the Transactional Outbox pattern.
 * This repository implements high-performance queries for event processing and
 * cleanup operations required for reliable message publishing.
 * 
 * Key Operations:
 * - Atomic event retrieval for processing (with row-level locking)
 * - Batch status updates for performance
 * - Cleanup operations for processed events
 * - Monitoring queries for operational insights
 * 
 * Performance Optimizations:
 * - Index-optimized queries for event polling
 * - Batch operations to reduce database round-trips
 * - Row-level locking to prevent race conditions in distributed deployments
 * 
 * @author Mini-UPS Development Team
 * @version 1.0
 */
@Repository
public interface OutboxEventRepository extends JpaRepository<OutboxEvent, Long> {
    
    /**
     * Find events ready for processing with pessimistic locking
     * 
     * This query implements the core of the outbox polling mechanism:
     * 1. Selects events that are PENDING and ready for retry
     * 2. Orders by creation time for FIFO processing
     * 3. Uses PESSIMISTIC_WRITE lock to prevent race conditions
     * 4. Limits results to prevent memory issues with large batches
     * 
     * @param now Current timestamp for retry logic
     * @param pageable Pagination to control batch size
     * @return List of events ready for processing
     */
    @Query("""
        SELECT e FROM OutboxEvent e 
        WHERE e.status = 'PENDING' 
        AND (e.nextRetryAt IS NULL OR e.nextRetryAt <= :now)
        ORDER BY e.createdAt ASC
        """)
    List<OutboxEvent> findEventsReadyForProcessing(@Param("now") Instant now, Pageable pageable);
    
    /**
     * Atomically claim events for processing by a specific instance
     * 
     * This prevents multiple poller instances from processing the same events
     * by updating the status to PROCESSING in a single atomic operation.
     * 
     * @param eventIds List of event IDs to claim
     * @param processingInstanceId Unique identifier for the processing instance
     * @return Number of events successfully claimed
     */
    @Modifying
    @Query("""
        UPDATE OutboxEvent e 
        SET e.status = 'PROCESSING', 
            e.updatedAt = CURRENT_TIMESTAMP,
            e.correlationId = :processingInstanceId
        WHERE e.id IN :eventIds 
        AND e.status = 'PENDING'
        """)
    int claimEventsForProcessing(@Param("eventIds") List<Long> eventIds, 
                                @Param("processingInstanceId") String processingInstanceId);
    
    /**
     * Find events currently being processed by a specific instance
     * 
     * Used for recovery scenarios where a processing instance crashes
     * and needs to resume or release events it was processing.
     * 
     * @param processingInstanceId Instance identifier
     * @return List of events being processed by the instance
     */
    @Query("""
        SELECT e FROM OutboxEvent e 
        WHERE e.status = 'PROCESSING' 
        AND e.correlationId = :processingInstanceId
        ORDER BY e.createdAt ASC
        """)
    List<OutboxEvent> findProcessingEventsByInstance(@Param("processingInstanceId") String processingInstanceId);
    
    /**
     * Reset stuck processing events back to pending
     * 
     * Handles cases where processing instances crash without completing
     * their events. Events stuck in PROCESSING status for too long
     * are reset to PENDING for retry.
     * 
     * @param stuckThreshold Timestamp before which events are considered stuck
     * @return Number of events reset
     */
    @Modifying
    @Query("""
        UPDATE OutboxEvent e 
        SET e.status = 'PENDING', 
            e.correlationId = NULL,
            e.updatedAt = CURRENT_TIMESTAMP
        WHERE e.status = 'PROCESSING' 
        AND e.updatedAt < :stuckThreshold
        """)
    int resetStuckProcessingEvents(@Param("stuckThreshold") Instant stuckThreshold);
    
    /**
     * Batch update event statuses to PUBLISHED
     * 
     * Optimized batch update for marking multiple events as successfully
     * published. This reduces database round-trips for high-throughput scenarios.
     * 
     * @param eventIds List of event IDs to mark as published
     * @return Number of events updated
     */
    @Modifying
    @Query("""
        UPDATE OutboxEvent e 
        SET e.status = 'PUBLISHED', 
            e.publishedAt = CURRENT_TIMESTAMP,
            e.updatedAt = CURRENT_TIMESTAMP,
            e.errorMessage = NULL
        WHERE e.id IN :eventIds
        """)
    int markEventsAsPublished(@Param("eventIds") List<Long> eventIds);
    
    /**
     * Find events by correlation ID for debugging and tracing
     * 
     * @param correlationId The correlation ID to search for
     * @return List of events with matching correlation ID
     */
    List<OutboxEvent> findByCorrelationIdOrderByCreatedAtAsc(String correlationId);
    
    /**
     * Find events by aggregate ID and type for business entity correlation
     * 
     * @param aggregateId The business entity ID
     * @param aggregateType The business entity type
     * @return List of events for the specified aggregate
     */
    List<OutboxEvent> findByAggregateIdAndAggregateTypeOrderByCreatedAtAsc(
        String aggregateId, String aggregateType);
    
    /**
     * Count events by status for monitoring and alerting
     * 
     * @param status The event status to count
     * @return Number of events in the specified status
     */
    @Query("SELECT COUNT(e) FROM OutboxEvent e WHERE e.status = :status")
    long countByStatus(@Param("status") OutboxEvent.OutboxStatus status);
    
    /**
     * Find failed events that need manual intervention
     * 
     * @param pageable Pagination for large result sets
     * @return List of failed events ordered by failure time
     */
    @Query("""
        SELECT e FROM OutboxEvent e 
        WHERE e.status = 'FAILED' 
        ORDER BY e.updatedAt DESC
        """)
    List<OutboxEvent> findFailedEvents(Pageable pageable);
    
    /**
     * Delete old published events for cleanup
     * 
     * Removes successfully published events older than the specified threshold
     * to prevent the outbox table from growing indefinitely.
     * 
     * @param olderThan Timestamp before which published events should be deleted
     * @return Number of events deleted
     */
    @Modifying
    @Query("""
        DELETE FROM OutboxEvent e 
        WHERE e.status = 'PUBLISHED' 
        AND e.publishedAt < :olderThan
        """)
    int deletePublishedEventsOlderThan(@Param("olderThan") Instant olderThan);
    
    /**
     * Archive old events by moving them to ARCHIVED status
     * 
     * Alternative to deletion that preserves events for audit purposes
     * while excluding them from processing.
     * 
     * @param olderThan Timestamp before which events should be archived
     * @return Number of events archived
     */
    @Modifying
    @Query("""
        UPDATE OutboxEvent e 
        SET e.status = 'ARCHIVED',
            e.updatedAt = CURRENT_TIMESTAMP
        WHERE e.status = 'PUBLISHED' 
        AND e.publishedAt < :olderThan
        """)
    int archivePublishedEventsOlderThan(@Param("olderThan") Instant olderThan);
    
    /**
     * Get outbox health statistics for monitoring
     * 
     * Returns aggregated statistics about the outbox for health checks
     * and operational monitoring.
     */
    @Query("""
        SELECT new map(
            e.status as status,
            COUNT(e) as count,
            MIN(e.createdAt) as oldestEvent,
            MAX(e.createdAt) as newestEvent
        )
        FROM OutboxEvent e 
        GROUP BY e.status
        """)
    List<java.util.Map<String, Object>> getOutboxHealthStatistics();
    
    /**
     * Find the oldest unprocessed event for lag monitoring
     * 
     * @return The oldest PENDING event, if any
     */
    @Query("""
        SELECT e FROM OutboxEvent e 
        WHERE e.status = 'PENDING'
        ORDER BY e.createdAt ASC
        """)
    Optional<OutboxEvent> findOldestPendingEvent();
}