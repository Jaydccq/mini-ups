/**
 * Shipment Status History Repository
 * 
 * Function Description:
 * - Manages data access for transport order status change history records
 * - Provides status change timeline query functionality
 * - Supports filtering by time range and status type
 * 
 * Query Functions:
 * - Find all status history for a transport order
 * - Query status changes by time range
 * - Find change records for specific statuses
 * - Count status change frequency and patterns
 * 
 * Performance Optimization:
 * - Indexing on status and timestamp fields
 * - Pagination query support
 * - Batch operation optimization
 * 
 *
 
 */
package com.miniups.repository;

import com.miniups.model.entity.Shipment;
import com.miniups.model.entity.ShipmentStatusHistory;
import com.miniups.model.enums.ShipmentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ShipmentStatusHistoryRepository extends JpaRepository<ShipmentStatusHistory, Long> {
    
    /**
     * Find all status history for the specified transport order
     * 
     * @param shipment Transport order
     * @return List of status history, ordered by time descending
     */
    List<ShipmentStatusHistory> findByShipmentOrderByTimestampDesc(Shipment shipment);
    
    /**
     * Find all status history for the specified transport order ID
     * 
     * @param shipmentId Transport order ID
     * @return List of status history, ordered by time descending
     */
    @Query("SELECT h FROM ShipmentStatusHistory h WHERE h.shipment.id = :shipmentId ORDER BY h.timestamp DESC")
    List<ShipmentStatusHistory> findByShipmentIdOrderByTimestampDesc(@Param("shipmentId") Long shipmentId);
    
    /**
     * Find the latest status record for the specified transport order
     * 
     * @param shipment Transport order
     * @return Latest status record
     */
    Optional<ShipmentStatusHistory> findFirstByShipmentOrderByTimestampDesc(Shipment shipment);
    
    /**
     * Find status history for the specified transport order within a specific time range
     * 
     * @param shipment Transport order
     * @param startTime Start time
     * @param endTime End time
     * @return List of status history
     */
    List<ShipmentStatusHistory> findByShipmentAndTimestampBetweenOrderByTimestampDesc(
        Shipment shipment, LocalDateTime startTime, LocalDateTime endTime);
    
    /**
     * Find specific status records for the specified transport order
     * 
     * @param shipment Transport order
     * @param status Transport status
     * @return List of status records
     */
    List<ShipmentStatusHistory> findByShipmentAndStatusOrderByTimestampDesc(
        Shipment shipment, ShipmentStatus status);
    
    /**
     * Check if the transport order has experienced a specific status
     * 
     * @param shipment Transport order
     * @param status Transport status
     * @return Whether the status has been experienced
     */
    boolean existsByShipmentAndStatus(Shipment shipment, ShipmentStatus status);
    
    /**
     * Paginated query for all status history
     * 
     * @param pageable Pagination parameters
     * @return Paginated status history records
     */
    Page<ShipmentStatusHistory> findAllByOrderByTimestampDesc(Pageable pageable);
    
    /**
     * Find all status changes within the specified time range
     * 
     * @param startTime Start time
     * @param endTime End time
     * @param pageable Pagination parameters
     * @return Paginated status history records
     */
    Page<ShipmentStatusHistory> findByTimestampBetweenOrderByTimestampDesc(
        LocalDateTime startTime, LocalDateTime endTime, Pageable pageable);
    
    /**
     * Count the total number of specified statuses
     * 
     * @param status Transport status
     * @return Number of statuses
     */
    long countByStatus(ShipmentStatus status);
    
    /**
     * Count the number of status changes within the specified time range
     * 
     * @param startTime Start time
     * @param endTime End time
     * @return Number of status changes
     */
    long countByTimestampBetween(LocalDateTime startTime, LocalDateTime endTime);
    
    /**
     * Delete all status history for the specified shipment
     * 
     * @param shipment Shipment
     */
    void deleteByShipment(Shipment shipment);
    
    /**
     * Delete status history before the specified time (for data cleanup)
     * 
     * @param cutoffTime Cutoff time
     */
    void deleteByTimestampBefore(LocalDateTime cutoffTime);
    
    /**
     * Find shipments with anomalous status changes (multiple changes in a short time)
     * 
     * @param cutoffTime Time window cutoff
     * @param threshold Change count threshold
     * @return Status history of anomalous shipments
     */
    @Query("SELECT h FROM ShipmentStatusHistory h WHERE h.shipment IN " +
           "(SELECT h2.shipment FROM ShipmentStatusHistory h2 " +
           "WHERE h2.timestamp >= :cutoffTime " +
           "GROUP BY h2.shipment HAVING COUNT(h2) >= :threshold) " +
           "ORDER BY h.shipment.id, h.timestamp DESC")
    List<ShipmentStatusHistory> findShipmentsWithFrequentStatusChanges(
        @Param("cutoffTime") LocalDateTime cutoffTime, 
        @Param("threshold") long threshold);
    
    /**
     * Find shipments with stale statuses (no updates for a long time)
     * 
     * @param cutoffTime Cutoff time
     * @return Status history of shipments with stale statuses
     */
    @Query("SELECT h FROM ShipmentStatusHistory h WHERE h.id IN " +
           "(SELECT MAX(h2.id) FROM ShipmentStatusHistory h2 " +
           "GROUP BY h2.shipment HAVING MAX(h2.timestamp) < :cutoffTime)")
    List<ShipmentStatusHistory> findShipmentsWithStaleStatus(@Param("cutoffTime") LocalDateTime cutoffTime);
    
    /**
     * Get status change statistics report
     * 
     * @param startTime Start time
     * @param endTime End time
     * @return Status statistics data
     */
    @Query("SELECT h.status as status, COUNT(h) as count, " +
           "MIN(h.timestamp) as firstOccurrence, MAX(h.timestamp) as lastOccurrence " +
           "FROM ShipmentStatusHistory h " +
           "WHERE h.timestamp BETWEEN :startTime AND :endTime " +
           "GROUP BY h.status ORDER BY count DESC")
    List<Object[]> getStatusChangeStatistics(
        @Param("startTime") LocalDateTime startTime, 
        @Param("endTime") LocalDateTime endTime);
}
