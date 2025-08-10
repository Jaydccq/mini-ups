/**
 * 运输订单数据访问接口
 * 
 * 功能说明：
 * - Mini-UPS核心业务的数据访问层，管理所有运输订单的数据操作
 * - 提供复杂的查询方法支持订单追踪、状态管理、用户查询等功能
 * - 支持分页查询处理大量订单数据
 * 
 * 核心查询方法：
 * - findByShipmentId: 根据Amazon订单ID查找（系统间集成）
 * - findByUpsTrackingId: 根据UPS追踪号查找（客户查询）
 * - findByUserId: 查询用户的所有订单（支持分页）
 * - findByStatus: 根据状态查找订单（运营管理）
 * 
 * 自定义JPQL查询：
 * - findByUserIdAndStatus: 查询特定用户的特定状态订单
 * - countByStatus: 统计各状态订单数量（数据报表）
 * 
 * 性能特性：
 * - 利用数据库索引优化查询性能
 * - 支持分页查询避免内存溢出
 * - @Query注解提供复杂查询支持
 * - 参数化查询防止SQL注入
 * 
 * 业务应用场景：
 * - 订单状态追踪和管理
 * - 用户订单历史查询
 * - 运营数据统计和报表
 * - Amazon系统集成接口
 * - 客户服务支持
 * 
 * @author Mini-UPS Team
 * @version 1.0.0
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