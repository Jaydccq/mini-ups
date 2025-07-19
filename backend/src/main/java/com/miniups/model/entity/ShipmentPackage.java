/**
 * 运输包裹实体类
 * 
 * 功能说明：
 * - 记录运输订单中具体包裹的详细信息
 * - 每个包裹有独立的配送地址和属性
 * - 支持包裹级别的状态追踪和管理
 * 
 * 主要字段：
 * - packageId: Amazon系统的包裹ID
 * - destinationX/Y: 配送目标坐标
 * - destinationAddress: 详细配送地址
 * - description: 包裹描述
 * - weight: 包裹重量
 * 
 * 数据库设计：
 * - 表名: packages
 * - 索引: package_id、shipment_id
 * - 外键: shipment_id（所属运输订单）
 * 
 * 业务关系：
 * - 多个包裹可以属于同一个运输订单
 * - 每个包裹有独立的配送地址（支持多点配送）
 * - 包裹状态跟随订单状态变化
 * 
 * 注意：重命名为 ShipmentPackage 以避免与 java.lang.Package 冲突
 * 
 * @author Mini-UPS Team
 * @version 1.0.0
 */

package com.miniups.model.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

@Entity
@Table(name = "packages", indexes = {
    @Index(name = "idx_package_package_id", columnList = "package_id"),
    @Index(name = "idx_package_shipment", columnList = "shipment_id")
})
public class ShipmentPackage extends BaseEntity {
    
    @NotBlank
    @Size(max = 100)
    @Column(name = "package_id", nullable = false, unique = true, length = 100)
    private String packageId;
    
    @Size(max = 500)
    @Column(name = "description", length = 500)
    private String description;
    
    @Column(name = "weight", precision = 10, scale = 2)
    private BigDecimal weight;
    
    @Column(name = "length_cm", precision = 8, scale = 2)
    private BigDecimal length;
    
    @Column(name = "width_cm", precision = 8, scale = 2)
    private BigDecimal width;
    
    @Column(name = "height_cm", precision = 8, scale = 2)
    private BigDecimal height;
    
    @Column(name = "value", precision = 12, scale = 2)
    private BigDecimal value;
    
    @Column(name = "fragile", nullable = false)
    private Boolean fragile = false;
    
    // Relationships
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "shipment_id", nullable = false)
    private Shipment shipment;
    
    // Constructors
    public ShipmentPackage() {}
    
    public ShipmentPackage(String packageId, String description, Shipment shipment) {
        this.packageId = packageId;
        this.description = description;
        this.shipment = shipment;
    }
    
    // Getters and Setters
    public String getPackageId() {
        return packageId;
    }
    
    public void setPackageId(String packageId) {
        this.packageId = packageId;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    public BigDecimal getWeight() {
        return weight;
    }
    
    public void setWeight(BigDecimal weight) {
        this.weight = weight;
    }
    
    public BigDecimal getLength() {
        return length;
    }
    
    public void setLength(BigDecimal length) {
        this.length = length;
    }
    
    public BigDecimal getWidth() {
        return width;
    }
    
    public void setWidth(BigDecimal width) {
        this.width = width;
    }
    
    public BigDecimal getHeight() {
        return height;
    }
    
    public void setHeight(BigDecimal height) {
        this.height = height;
    }
    
    public BigDecimal getValue() {
        return value;
    }
    
    public void setValue(BigDecimal value) {
        this.value = value;
    }
    
    public Boolean getFragile() {
        return fragile;
    }
    
    public void setFragile(Boolean fragile) {
        this.fragile = fragile;
    }
    
    public Shipment getShipment() {
        return shipment;
    }
    
    public void setShipment(Shipment shipment) {
        this.shipment = shipment;
    }
    
    // Helper methods
    public BigDecimal getVolumetricWeight() {
        if (length != null && width != null && height != null) {
            // Calculate volumetric weight (L x W x H) / 5000
            return length.multiply(width).multiply(height).divide(BigDecimal.valueOf(5000));
        }
        return BigDecimal.ZERO;
    }
    
    public BigDecimal getBillableWeight() {
        BigDecimal actualWeight = weight != null ? weight : BigDecimal.ZERO;
        BigDecimal volumetricWeight = getVolumetricWeight();
        return actualWeight.max(volumetricWeight);
    }
}