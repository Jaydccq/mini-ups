/**
 * 运输订单实体类
 * 
 * 功能说明：
 * - Mini-UPS系统的核心业务实体，记录包裹运输的完整生命周期
 * - 管理从下单到配送完成的全部状态变化
 * - 与用户、卡车、包裹等实体建立关联关系
 * 
 * 主要字段：
 * - shipmentId: Amazon系统的运输订单ID
 * - upsTrackingId: UPS生成的追踪号码
 * - status: 运输状态(CREATED/PICKED_UP/IN_TRANSIT/DELIVERED等)
 * - truck: 分配的运输卡车
 * - destinations: 目标地址坐标和详细地址
 * 
 * 数据库设计：
 * - 表名: shipments
 * - 索引: shipment_id、tracking_id、status、user_id
 * - 外键: user_id(用户)、truck_id(卡车)
 * 
 * 业务流程：
 * 1. Amazon创建运输订单
 * 2. UPS分配卡车和生成追踪号
 * 3. 卡车前往取货点
 * 4. 运输中实时更新位置
 * 5. 到达目的地完成配送
 * 
 * @author Mini-UPS Team
 * @version 1.0.0
 */
package com.miniups.model.entity;

import com.miniups.model.enums.ShipmentStatus;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "shipments", indexes = {
    @Index(name = "idx_shipment_shipment_id", columnList = "shipment_id"),
    @Index(name = "idx_shipment_tracking_id", columnList = "ups_tracking_id"),
    @Index(name = "idx_shipment_status", columnList = "status"),
    @Index(name = "idx_shipment_user", columnList = "user_id")
})
public class Shipment extends BaseEntity {
    
    @NotBlank
    @Size(max = 100)
    @Column(name = "shipment_id", nullable = false, unique = true, length = 100)
    private String shipmentId;
    
    @Size(max = 100)
    @Column(name = "ups_tracking_id", unique = true, length = 100)
    private String upsTrackingId;
    
    @Size(max = 100)
    @Column(name = "amazon_order_id", length = 100)
    private String amazonOrderId;
    
    @Column(name = "warehouse_id", length = 50)
    private String warehouseId;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private ShipmentStatus status = ShipmentStatus.CREATED;
    
    @NotNull
    @Column(name = "origin_x", nullable = false)
    private Integer originX;
    
    @NotNull
    @Column(name = "origin_y", nullable = false)
    private Integer originY;
    
    @NotNull
    @Column(name = "dest_x", nullable = false)
    private Integer destX;
    
    @NotNull
    @Column(name = "dest_y", nullable = false)
    private Integer destY;
    
    @Column(name = "weight", precision = 10, scale = 2)
    private BigDecimal weight;
    
    @Column(name = "estimated_delivery")
    private LocalDateTime estimatedDelivery;
    
    @Column(name = "actual_delivery")
    private LocalDateTime actualDelivery;
    
    @Column(name = "pickup_time")
    private LocalDateTime pickupTime;
    
    @Column(name = "world_id")
    private Long worldId;
    
    @Column(name = "delivery_address")
    private String deliveryAddress;
    
    @Column(name = "delivery_city")
    private String deliveryCity;
    
    @Column(name = "delivery_zip_code")
    private String deliveryZipCode;
    
    // Relationships
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "truck_id")
    private Truck truck;
    
    @OneToMany(mappedBy = "shipment", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<ShipmentPackage> packages = new ArrayList<>();
    
    @OneToMany(mappedBy = "shipment", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<ShipmentStatusHistory> statusHistory = new ArrayList<>();
    
    @OneToMany(mappedBy = "shipment", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<AddressChange> addressChanges = new ArrayList<>();
    
    // Constructors
    public Shipment() {}
    
    public Shipment(String shipmentId, Integer originX, Integer originY, 
                   Integer destX, Integer destY) {
        this.shipmentId = shipmentId;
        this.originX = originX;
        this.originY = originY;
        this.destX = destX;
        this.destY = destY;
    }
    
    // Getters and Setters
    public String getShipmentId() {
        return shipmentId;
    }
    
    public void setShipmentId(String shipmentId) {
        this.shipmentId = shipmentId;
    }
    
    public String getUpsTrackingId() {
        return upsTrackingId;
    }
    
    public void setUpsTrackingId(String upsTrackingId) {
        this.upsTrackingId = upsTrackingId;
    }
    
    
    public String getAmazonOrderId() {
        return amazonOrderId;
    }
    
    public void setAmazonOrderId(String amazonOrderId) {
        this.amazonOrderId = amazonOrderId;
    }
    
    public String getWarehouseId() {
        return warehouseId;
    }
    
    public void setWarehouseId(String warehouseId) {
        this.warehouseId = warehouseId;
    }
    
    public ShipmentStatus getStatus() {
        return status;
    }
    
    public void setStatus(ShipmentStatus status) {
        this.status = status;
    }
    
    public Integer getOriginX() {
        return originX;
    }
    
    public void setOriginX(Integer originX) {
        this.originX = originX;
    }
    
    public Integer getOriginY() {
        return originY;
    }
    
    public void setOriginY(Integer originY) {
        this.originY = originY;
    }
    
    public Integer getDestX() {
        return destX;
    }
    
    public void setDestX(Integer destX) {
        this.destX = destX;
    }
    
    public Integer getDestY() {
        return destY;
    }
    
    public void setDestY(Integer destY) {
        this.destY = destY;
    }
    
    
    public BigDecimal getWeight() {
        return weight;
    }
    
    public void setWeight(BigDecimal weight) {
        this.weight = weight;
    }
    
    public LocalDateTime getEstimatedDelivery() {
        return estimatedDelivery;
    }
    
    public void setEstimatedDelivery(LocalDateTime estimatedDelivery) {
        this.estimatedDelivery = estimatedDelivery;
    }
    
    public LocalDateTime getActualDelivery() {
        return actualDelivery;
    }
    
    public void setActualDelivery(LocalDateTime actualDelivery) {
        this.actualDelivery = actualDelivery;
    }
    
    public LocalDateTime getPickupTime() {
        return pickupTime;
    }
    
    public void setPickupTime(LocalDateTime pickupTime) {
        this.pickupTime = pickupTime;
    }
    
    public Long getWorldId() {
        return worldId;
    }
    
    public void setWorldId(Long worldId) {
        this.worldId = worldId;
    }
    
    public String getDeliveryAddress() {
        return deliveryAddress;
    }
    
    public void setDeliveryAddress(String deliveryAddress) {
        this.deliveryAddress = deliveryAddress;
    }
    
    public String getDeliveryCity() {
        return deliveryCity;
    }
    
    public void setDeliveryCity(String deliveryCity) {
        this.deliveryCity = deliveryCity;
    }
    
    public String getDeliveryZipCode() {
        return deliveryZipCode;
    }
    
    public void setDeliveryZipCode(String deliveryZipCode) {
        this.deliveryZipCode = deliveryZipCode;
    }
    
    public User getUser() {
        return user;
    }
    
    public void setUser(User user) {
        this.user = user;
    }
    
    public Truck getTruck() {
        return truck;
    }
    
    public void setTruck(Truck truck) {
        this.truck = truck;
    }
    
    public List<ShipmentPackage> getPackages() {
        return packages;
    }
    
    public void setPackages(List<ShipmentPackage> packages) {
        this.packages = packages;
    }
    
    public List<ShipmentStatusHistory> getStatusHistory() {
        return statusHistory;
    }
    
    public void setStatusHistory(List<ShipmentStatusHistory> statusHistory) {
        this.statusHistory = statusHistory;
    }
    
    public List<AddressChange> getAddressChanges() {
        return addressChanges;
    }
    
    public void setAddressChanges(List<AddressChange> addressChanges) {
        this.addressChanges = addressChanges;
    }
    
    // Helper methods
    public boolean canChangeAddress() {
        return status == ShipmentStatus.CREATED || 
               status == ShipmentStatus.PICKED_UP || 
               status == ShipmentStatus.IN_TRANSIT;
    }
    
    public void updateStatus(ShipmentStatus newStatus) {
        this.status = newStatus;
        // Note: Status history is now managed by TrackingService to avoid duplication
    }
}