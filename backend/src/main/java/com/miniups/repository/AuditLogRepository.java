package com.miniups.repository;

import com.miniups.model.entity.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Audit Log Repository
 * 
 * Data access layer for audit log operations. Provides methods for
 * querying audit logs with various filters and optimizations for
 * compliance reporting and system monitoring.
 * 
 * Key Features:
 * - Efficient querying by common criteria
 * - Pagination support for large datasets
 * - Aggregation queries for reporting
 * - Performance-optimized for write-heavy workloads
 * 
 * @author Mini-UPS Development Team
 * @version 1.0
 * @since 2024
 */
@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    /**
     * Check if an audit log with the given event ID already exists
     * Used for idempotency checking in event processingj
     * 
     * @param eventId The unique event identifier
     * @return true if an audit log with this event ID exists
     */
    boolean existsByEventId(String eventId);

    /**
     * Find audit log by event ID
     * Used for event correlation and duplicate detection
     * 
     * @param eventId The unique event identifier
     * @return Optional containing the audit log if found
     */
    Optional<AuditLog> findByEventId(String eventId);

    /**
     * Find audit logs by correlation ID
     * Used for tracing related operations across service boundaries
     * 
     * @param correlationId The correlation identifier
     * @return List of audit logs with the same correlation ID
     */
    List<AuditLog> findByCorrelationIdOrderByOperationTimestampAsc(String correlationId);

    /**
     * Find audit logs for a specific user
     * Used for user activity tracking and compliance
     * 
     * @param userId The user identifier
     * @param pageable Pagination parameters
     * @return Page of audit logs for the user
     */
    Page<AuditLog> findByUserIdOrderByOperationTimestampDesc(Long userId, Pageable pageable);

    /**
     * Find audit logs by operation type
     * Used for analyzing specific types of operations
     * 
     * @param operationType The type of operation
     * @param pageable Pagination parameters
     * @return Page of audit logs for the operation type
     */
    Page<AuditLog> findByOperationTypeOrderByOperationTimestampDesc(String operationType, Pageable pageable);

    /**
     * Find audit logs by operation result
     * Used for error analysis and success rate monitoring
     * 
     * @param operationResult The result of the operation
     * @param pageable Pagination parameters
     * @return Page of audit logs with the specified result
     */
    Page<AuditLog> findByOperationResultOrderByOperationTimestampDesc(String operationResult, Pageable pageable);

    /**
     * Find audit logs for a specific entity
     * Used for tracking all operations on a particular entity
     * 
     * @param entityType The type of entity
     * @param entityId The entity identifier
     * @param pageable Pagination parameters
     * @return Page of audit logs for the entity
     */
    Page<AuditLog> findByEntityTypeAndEntityIdOrderByOperationTimestampDesc(
            String entityType, String entityId, Pageable pageable);

    /**
     * Find audit logs within a time range
     * Used for compliance reporting and periodic analysis
     * 
     * @param startTime Start of the time range (inclusive)
     * @param endTime End of the time range (inclusive)
     * @param pageable Pagination parameters
     * @return Page of audit logs within the time range
     */
    Page<AuditLog> findByOperationTimestampBetweenOrderByOperationTimestampDesc(
            Instant startTime, Instant endTime, Pageable pageable);

    /**
     * Find failed operations within a time range
     * Used for error monitoring and analysis
     * 
     * @param startTime Start of the time range (inclusive)
     * @param endTime End of the time range (inclusive)
     * @param pageable Pagination parameters
     * @return Page of failed audit logs within the time range
     */
    @Query("SELECT a FROM AuditLog a WHERE a.operationTimestamp BETWEEN :startTime AND :endTime " +
           "AND (a.operationResult = 'FAILED' OR a.resultCode >= 400) " +
           "ORDER BY a.operationTimestamp DESC")
    Page<AuditLog> findFailedOperationsBetween(
            @Param("startTime") Instant startTime,
            @Param("endTime") Instant endTime,
            Pageable pageable);

    /**
     * Find audit logs by user and time range
     * Used for user activity analysis and compliance
     * 
     * @param userId The user identifier
     * @param startTime Start of the time range (inclusive)
     * @param endTime End of the time range (inclusive)
     * @param pageable Pagination parameters
     * @return Page of audit logs for the user within the time range
     */
    Page<AuditLog> findByUserIdAndOperationTimestampBetweenOrderByOperationTimestampDesc(
            Long userId, Instant startTime, Instant endTime, Pageable pageable);

    /**
     * Count operations by type within a time range
     * Used for operational metrics and reporting
     * 
     * @param operationType The type of operation
     * @param startTime Start of the time range (inclusive)
     * @param endTime End of the time range (inclusive)
     * @return Count of operations of the specified type
     */
    @Query("SELECT COUNT(a) FROM AuditLog a WHERE a.operationType = :operationType " +
           "AND a.operationTimestamp BETWEEN :startTime AND :endTime")
    long countByOperationTypeAndTimeBetween(
            @Param("operationType") String operationType,
            @Param("startTime") Instant startTime,
            @Param("endTime") Instant endTime);

    /**
     * Count failed operations within a time range
     * Used for error rate monitoring
     * 
     * @param startTime Start of the time range (inclusive)
     * @param endTime End of the time range (inclusive)
     * @return Count of failed operations
     */
    @Query("SELECT COUNT(a) FROM AuditLog a WHERE a.operationTimestamp BETWEEN :startTime AND :endTime " +
           "AND (a.operationResult = 'FAILED' OR a.resultCode >= 400)")
    long countFailedOperationsBetween(
            @Param("startTime") Instant startTime,
            @Param("endTime") Instant endTime);

    /**
     * Get average operation duration by type within a time range
     * Used for performance monitoring and optimization
     * 
     * @param operationType The type of operation
     * @param startTime Start of the time range (inclusive)
     * @param endTime End of the time range (inclusive)
     * @return Average duration in milliseconds
     */
    @Query("SELECT AVG(a.operationDurationMs) FROM AuditLog a WHERE a.operationType = :operationType " +
           "AND a.operationTimestamp BETWEEN :startTime AND :endTime " +
           "AND a.operationDurationMs IS NOT NULL")
    Double getAverageOperationDuration(
            @Param("operationType") String operationType,
            @Param("startTime") Instant startTime,
            @Param("endTime") Instant endTime);

    /**
     * Find audit logs with long operation durations
     * Used for performance issue identification
     * 
     * @param thresholdMs Minimum duration in milliseconds
     * @param startTime Start of the time range (inclusive)
     * @param endTime End of the time range (inclusive)
     * @param pageable Pagination parameters
     * @return Page of audit logs with long durations
     */
    @Query("SELECT a FROM AuditLog a WHERE a.operationDurationMs > :thresholdMs " +
           "AND a.operationTimestamp BETWEEN :startTime AND :endTime " +
           "ORDER BY a.operationDurationMs DESC")
    Page<AuditLog> findSlowOperations(
            @Param("thresholdMs") Long thresholdMs,
            @Param("startTime") Instant startTime,
            @Param("endTime") Instant endTime,
            Pageable pageable);

    /**
     * Find recent audit logs for a specific IP address
     * Used for security monitoring and threat analysis
     * 
     * @param ipAddress The IP address to search for
     * @param startTime Start of the time range (inclusive)
     * @param pageable Pagination parameters
     * @return Page of audit logs from the IP address
     */
    Page<AuditLog> findByIpAddressAndOperationTimestampAfterOrderByOperationTimestampDesc(
            String ipAddress, Instant startTime, Pageable pageable);

    /**
     * Delete old audit logs before a certain date
     * Used for data retention and cleanup
     * 
     * @param cutoffDate Audit logs before this date will be deleted
     * @return Number of deleted records
     */
    @Query("DELETE FROM AuditLog a WHERE a.operationTimestamp < :cutoffDate")
    int deleteByOperationTimestampBefore(@Param("cutoffDate") Instant cutoffDate);
}