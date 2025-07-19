/**
 * 卡车实体类
 * 
 * 功能说明：
 * - 管理UPS运输车辆的基本信息和实时状态
 * - 记录卡车位置、载重能力、当前任务等
 * - 与运输订单建立一对多关系（一辆卡车可同时处理多个订单）
 * 
 * 主要字段：
 * - truckId: 卡车在世界模拟器中的唯一标识
 * - status: 卡车状态(IDLE/BUSY/MAINTENANCE/OUT_OF_SERVICE)
 * - currentX/currentY: 当前位置坐标
 * - capacity: 载重能力
 * - currentLoad: 当前载重
 * 
 * 数据库设计：
 * - 表名: trucks
 * - 索引: truck_id、status（用于快速查找可用卡车）
 * - 外键关联: shipments（当前承载的运输任务）
 * 
 * 业务逻辑：
 * - 卡车调度算法根据距离和载重分配任务
 * - 实时位置更新用于追踪和路径优化
 * - 支持多包裹配送路径规划
 * 
 * @author Mini-UPS Team
 * @version 1.0.0
 */
package com.miniups.model.entity;

import com.miniups.model.enums.TruckStatus;
import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "trucks", indexes = {
    @Index(name = "idx_truck_truck_id", columnList = "truck_id"),
    @Index(name = "idx_truck_status", columnList = "status")
})
public class Truck extends BaseEntity {
    
    @NotNull
    @Column(name = "truck_id", nullable = false, unique = true)
    private Integer truckId;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private TruckStatus status = TruckStatus.IDLE;
    
    @Min(0)
    @Column(name = "current_x", nullable = false)
    private Integer currentX = 0;
    
    @Min(0)
    @Column(name = "current_y", nullable = false)
    private Integer currentY = 0;
    
    @Min(1)
    @Column(name = "capacity", nullable = false)
    private Integer capacity = 100;
    
    @Column(name = "driver_id")
    private Long driverId;
    
    @Column(name = "world_id")
    private Long worldId;
    
    @Column(name = "license_plate")
    private String licensePlate;
    
    @Column(name = "current_load")
    private Double currentLoad = 0.0;
    
    // Relationships
    @OneToMany(mappedBy = "truck", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Shipment> shipments = new ArrayList<>();
    
    @OneToMany(mappedBy = "truck", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<TruckLocationHistory> locationHistory = new ArrayList<>();
    
    // Constructors
    public Truck() {}
    
    public Truck(Integer truckId, Integer capacity) {
        this.truckId = truckId;
        this.capacity = capacity;
    }
    
    // Getters and Setters
    public Integer getTruckId() {
        return truckId;
    }
    
    public void setTruckId(Integer truckId) {
        this.truckId = truckId;
    }
    
    public TruckStatus getStatus() {
        return status;
    }
    
    public void setStatus(TruckStatus status) {
        this.status = status;
    }
    
    public Integer getCurrentX() {
        return currentX;
    }
    
    public void setCurrentX(Integer currentX) {
        this.currentX = currentX;
    }
    
    public Integer getCurrentY() {
        return currentY;
    }
    
    public void setCurrentY(Integer currentY) {
        this.currentY = currentY;
    }
    
    public Integer getCapacity() {
        return capacity;
    }
    
    public void setCapacity(Integer capacity) {
        this.capacity = capacity;
    }
    
    public Long getDriverId() {
        return driverId;
    }
    
    public void setDriverId(Long driverId) {
        this.driverId = driverId;
    }
    
    public Long getWorldId() {
        return worldId;
    }
    
    public void setWorldId(Long worldId) {
        this.worldId = worldId;
    }
    
    public List<Shipment> getShipments() {
        return shipments;
    }
    
    public void setShipments(List<Shipment> shipments) {
        this.shipments = shipments;
    }
    
    public List<TruckLocationHistory> getLocationHistory() {
        return locationHistory;
    }
    
    public void setLocationHistory(List<TruckLocationHistory> locationHistory) {
        this.locationHistory = locationHistory;
    }
    
    public String getLicensePlate() {
        return licensePlate;
    }
    
    public void setLicensePlate(String licensePlate) {
        this.licensePlate = licensePlate;
    }
    
    public Double getCurrentLoad() {
        return currentLoad;
    }
    
    public void setCurrentLoad(Double currentLoad) {
        this.currentLoad = currentLoad;
    }
    
    
    // Helper methods
    public boolean isAvailable() {
        return status == TruckStatus.IDLE;
    }
    
    public void updateLocation(Integer x, Integer y) {
        this.currentX = x;
        this.currentY = y;
    }
}