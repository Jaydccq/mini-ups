/**
 * Shipment Status History Repository
 * 
 * 功能说明：
 * - 管理运输订单状态变更历史记录的数据访问
 * - 提供状态变更时间线查询功能
 * - 支持按时间范围和状态类型筛选
 * 
 * 查询功能：
 * - 按运输订单查找所有状态历史
 * - 按时间范围查询状态变更
 * - 查找特定状态的变更记录
 * - 统计状态变更频率和模式
 * 
 * 性能优化：
 * - 状态和时间戳字段索引
 * - 分页查询支持
 * - 批量操作优化
 * 
 * @author Mini-UPS Team
 * @version 1.0.0
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
     * 查找指定运输订单的所有状态历史
     * 
     * @param shipment 运输订单
     * @return 状态历史列表，按时间倒序
     */
    List<ShipmentStatusHistory> findByShipmentOrderByTimestampDesc(Shipment shipment);
    
    /**
     * 查找指定运输订单ID的所有状态历史
     * 
     * @param shipmentId 运输订单ID
     * @return 状态历史列表，按时间倒序
     */
    @Query("SELECT h FROM ShipmentStatusHistory h WHERE h.shipment.id = :shipmentId ORDER BY h.timestamp DESC")
    List<ShipmentStatusHistory> findByShipmentIdOrderByTimestampDesc(@Param("shipmentId") Long shipmentId);
    
    /**
     * 查找指定运输订单的最新状态记录
     * 
     * @param shipment 运输订单
     * @return 最新状态记录
     */
    Optional<ShipmentStatusHistory> findFirstByShipmentOrderByTimestampDesc(Shipment shipment);
    
    /**
     * 查找指定运输订单在特定时间范围内的状态历史
     * 
     * @param shipment 运输订单
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 状态历史列表
     */
    List<ShipmentStatusHistory> findByShipmentAndTimestampBetweenOrderByTimestampDesc(
        Shipment shipment, LocalDateTime startTime, LocalDateTime endTime);
    
    /**
     * 查找指定运输订单的特定状态记录
     * 
     * @param shipment 运输订单
     * @param status 运输状态
     * @return 状态记录列表
     */
    List<ShipmentStatusHistory> findByShipmentAndStatusOrderByTimestampDesc(
        Shipment shipment, ShipmentStatus status);
    
    /**
     * 检查运输订单是否经历过特定状态
     * 
     * @param shipment 运输订单
     * @param status 运输状态
     * @return 是否经历过该状态
     */
    boolean existsByShipmentAndStatus(Shipment shipment, ShipmentStatus status);
    
    /**
     * 分页查询所有状态历史
     * 
     * @param pageable 分页参数
     * @return 分页的状态历史记录
     */
    Page<ShipmentStatusHistory> findAllByOrderByTimestampDesc(Pageable pageable);
    
    /**
     * 查找指定时间范围内的所有状态变更
     * 
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @param pageable 分页参数
     * @return 分页的状态历史记录
     */
    Page<ShipmentStatusHistory> findByTimestampBetweenOrderByTimestampDesc(
        LocalDateTime startTime, LocalDateTime endTime, Pageable pageable);
    
    /**
     * 统计指定状态的总数
     * 
     * @param status 运输状态
     * @return 状态数量
     */
    long countByStatus(ShipmentStatus status);
    
    /**
     * 统计指定时间范围内的状态变更数量
     * 
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 状态变更数量
     */
    long countByTimestampBetween(LocalDateTime startTime, LocalDateTime endTime);
    
    /**
     * 删除指定运输订单的所有状态历史
     * 
     * @param shipment 运输订单
     */
    void deleteByShipment(Shipment shipment);
    
    /**
     * 删除指定时间之前的状态历史记录（用于数据清理）
     * 
     * @param cutoffTime 截止时间
     */
    void deleteByTimestampBefore(LocalDateTime cutoffTime);
    
    /**
     * 查找状态变更异常的订单（短时间内多次状态变更）
     * 
     * @param minutes 时间窗口（分钟）
     * @param threshold 变更次数阈值
     * @return 异常订单的状态历史
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
     * 查找长时间未更新状态的订单
     * 
     * @param hoursAgo 小时数
     * @return 长时间未更新的订单状态历史
     */
    @Query("SELECT h FROM ShipmentStatusHistory h WHERE h.id IN " +
           "(SELECT MAX(h2.id) FROM ShipmentStatusHistory h2 " +
           "GROUP BY h2.shipment HAVING MAX(h2.timestamp) < :cutoffTime)")
    List<ShipmentStatusHistory> findShipmentsWithStaleStatus(@Param("cutoffTime") LocalDateTime cutoffTime);
    
    /**
     * 获取状态变更统计报告
     * 
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 状态统计数据
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