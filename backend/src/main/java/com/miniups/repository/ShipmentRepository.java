/**
 * Shipment Data Access Interface
 * 
 * Function Description:
 * - Data access layer for Mini-UPS core business, managing all data operations for transport orders
 * - Provides complex query methods to support order tracking, status management, user queries, etc.
 * - Supports pagination to handle large volumes of order data
 * 
 * Core Query Methods:
 * - findByShipmentId: Find by Amazon order ID (system integration)
 * - findByUpsTrackingId: Find by UPS tracking number (customer query)
 * - findByUserId: Query all orders for a user (with pagination support)
 * - findByStatus: Find orders by status (operations management)
 * 
 * Custom JPQL Queries:
 * - findByUserIdAndStatus: Query specific status orders for a particular user
 * - countByStatus: Count orders by status (data reporting)
 * 
 * Performance Features:
 * - Utilizes database indexes to optimize query performance
 * - Supports pagination to prevent memory overflow
 * - @Query annotation provides support for complex queries
 * - Parameterized queries prevent SQL injection
 * 
 * Business Application Scenarios:
 * - Order status tracking and management
 * - User order history queries
 * - Operations data statistics and reporting
 * - Amazon system integration interface
 * - Customer service support
 * 
 *
 
 */
package com.miniups.repository;

import com.miniups.model.entity.Shipment;
import com.miniups.model.enums.ShipmentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Repository
public interface ShipmentRepository extends JpaRepository<Shipment, Long> {
    
    Optional<Shipment> findByShipmentId(String shipmentId);
    
    Optional<Shipment> findByUpsTrackingId(String upsTrackingId);
    
    List<Shipment> findByUserId(Long userId);
    
    Page<Shipment> findByUserId(Long userId, Pageable pageable);
    
    List<Shipment> findByStatus(ShipmentStatus status);
    
    @Query("SELECT s FROM Shipment s WHERE s.user.id = :userId AND s.status = :status")
    List<Shipment> findByUserIdAndStatus(@Param("userId") Long userId, @Param("status") ShipmentStatus status);
    
    @Query("SELECT COUNT(s) FROM Shipment s WHERE s.status = :status")
    long countByStatus(@Param("status") ShipmentStatus status);
    
    boolean existsByUpsTrackingId(String upsTrackingId);
    
    
    List<Shipment> findByUserIdOrderByCreatedAtDesc(Long userId);
    
    // Additional repository methods for testing
    List<Shipment> findByCreatedAtBetween(LocalDateTime startDate, LocalDateTime endDate);
    
    List<Shipment> findByWeightBetween(BigDecimal minWeight, BigDecimal maxWeight);
    
    List<Shipment> findByTruck(com.miniups.model.entity.Truck truck);
    
    @Query("SELECT s.status as status, COUNT(s) as count FROM Shipment s GROUP BY s.status")
    List<Map<String, Object>> getStatusCounts();
    
    @Query("SELECT s FROM Shipment s ORDER BY s.createdAt DESC")
    Page<Shipment> findRecentShipments(Pageable pageable);
}