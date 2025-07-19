/**
 * 卡车数据访问接口
 * 
 * 功能说明：
 * - 管理UPS车队的数据访问操作
 * - 支持卡车调度、状态管理、司机分配等核心功能
 * - 为运输订单分配和追踪提供数据支持
 * 
 * 核心查询方法：
 * - findByTruckId: 根据世界模拟器的卡车ID查找（系统集成）
 * - findByStatus: 根据状态查找卡车（调度算法核心）
 * - findByDriverId: 查找司机的分配车辆（司机管理）
 * - existsByTruckId: 验证卡车ID是否存在（数据一致性）
 * 
 * 业务应用场景：
 * - 卡车调度算法：查找可用卡车（IDLE状态）
 * - 运输任务分配：根据位置和载重分配最优卡车
 * - 司机管理：查询司机的车辆分配情况
 * - 车队监控：实时查看所有卡车状态
 * - 世界模拟器集成：同步卡车位置和状态
 * 
 * 性能优化：
 * - 基于状态的索引快速查找可用卡车
 * - exists方法高效验证卡车存在性
 * - 支持批量状态更新操作
 * 
 * 调度算法支持：
 * - 查找最近的可用卡车
 * - 按载重能力筛选适合的车辆
 * - 支持多点配送路径优化
 * 
 * @author Mini-UPS Team
 * @version 1.0.0
 */
package com.miniups.repository;

import com.miniups.model.entity.Truck;
import com.miniups.model.enums.TruckStatus;
import org.springframework.data.jpa.repository.JpaRepository;
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
}