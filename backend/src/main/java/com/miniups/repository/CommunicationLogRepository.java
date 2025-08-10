/**
 * Communication Log Repository
 * 
 * Functionality:
 * - Data access layer for Amazon-UPS communication logging
 * - Supports querying by direction, message type, and time ranges
 * - Provides methods for debug interface and monitoring
 * 
 * Query Capabilities:
 * - Find recent communication logs for debugging
 * - Filter by incoming/outgoing direction
 * - Search by message type and status
 * - Time-based queries for troubleshooting
 * 
 * @author Mini-UPS Team
 * @version 1.0.0
 */
package com.miniups.repository;

import com.miniups.model.entity.CommunicationLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface CommunicationLogRepository extends JpaRepository<CommunicationLog, Long> {
    
    // Find recent logs for debugging
    List<CommunicationLog> findTop50ByOrderByCreatedAtDesc();
    
    // Filter by direction
    List<CommunicationLog> findByDirectionOrderByCreatedAtDesc(String direction);
    
    // Filter by message type
    List<CommunicationLog> findByMessageTypeOrderByCreatedAtDesc(String messageType);
    
    // Filter by success status
    List<CommunicationLog> findBySuccessOrderByCreatedAtDesc(Boolean success);
    
    // Find by shipment ID for tracking specific orders
    List<CommunicationLog> findByShipmentIdOrderByCreatedAtAsc(String shipmentId);
    
    // Time range queries for debugging
    List<CommunicationLog> findByCreatedAtBetweenOrderByCreatedAtDesc(
        LocalDateTime startTime, LocalDateTime endTime);
    
    // Complex query for debug interface
    @Query("SELECT c FROM CommunicationLog c WHERE " +
           "(:direction IS NULL OR c.direction = :direction) AND " +
           "(:messageType IS NULL OR c.messageType = :messageType) AND " +
           "(:success IS NULL OR c.success = :success) AND " +
           "c.createdAt >= :since " +
           "ORDER BY c.createdAt DESC")
    List<CommunicationLog> findFilteredLogs(
        @Param("direction") String direction,
        @Param("messageType") String messageType, 
        @Param("success") Boolean success,
        @Param("since") LocalDateTime since);
    
    // Paginated query for web interface
    Page<CommunicationLog> findByOrderByCreatedAtDesc(Pageable pageable);
    
    // Count statistics for dashboard
    @Query("SELECT c.messageType, COUNT(c) FROM CommunicationLog c " +
           "WHERE c.createdAt >= :since GROUP BY c.messageType")
    List<Object[]> countMessageTypesSince(@Param("since") LocalDateTime since);
    
    @Query("SELECT c.success, COUNT(c) FROM CommunicationLog c " +
           "WHERE c.createdAt >= :since GROUP BY c.success")
    List<Object[]> countSuccessRatesSince(@Param("since") LocalDateTime since);
    
    // Performance statistics
    @Query("SELECT AVG(c.processingTimeMs) FROM CommunicationLog c " +
           "WHERE c.processingTimeMs IS NOT NULL AND c.createdAt >= :since")
    Double averageProcessingTimeSince(@Param("since") LocalDateTime since);
    
    // Find errors for troubleshooting
    List<CommunicationLog> findTop20BySuccessFalseOrderByCreatedAtDesc();
}