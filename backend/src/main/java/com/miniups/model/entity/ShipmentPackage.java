/**
 * Shipment Package Entity
 * 
 * Description:
 * - Records detailed information about individual packages in a shipment
 * - Each package has its own delivery address and attributes
 * - Supports package-level status tracking and management
 * 
 * Key Fields:
 * - packageId: Package ID from the Amazon system
 * - destinationX/Y: Delivery target coordinates
 * - destinationAddress: Detailed delivery address
 * - description: Package description
 * - weight: Package weight
 * 
 * Database Design:
 * - Table: packages
 * - Indexes: package_id, shipment_id
 * - Foreign key: shipment_id (owning shipment)
 * 
 * Business Relations:
 * - Multiple packages can belong to the same shipment
 * - Each package may have an independent delivery address (supports multi-drop delivery)
 * - Package status follows the shipment status changes
 * 
 * Note: Renamed to ShipmentPackage to avoid conflict with java.lang.Package
 * 
 *
 
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
    
    @Column(name = "\"value\"", precision = 12, scale = 2)
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
