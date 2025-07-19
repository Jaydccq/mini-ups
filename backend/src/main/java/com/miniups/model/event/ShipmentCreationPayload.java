package com.miniups.model.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ShipmentCreationPayload {

    private Long amazonShipmentId;
    private Integer warehouseId;
    private Integer destX;
    private Integer destY;
    private Long userId;
    private String packageDescription;
    private Double packageWeight;
    private Double packageLength;
    private Double packageWidth;
    private Double packageHeight;
    private String specialInstructions;
    private String priority;
    private String deliveryTimeframe;

    public Double getPackageVolume() {
        if (packageLength != null && packageWidth != null && packageHeight != null) {
            return packageLength * packageWidth * packageHeight;
        }
        return 0.0;
    }

    public boolean hasSpecialInstructions() {
        return specialInstructions != null && !specialInstructions.trim().isEmpty();
    }

    public boolean isPriorityShipment() {
        return priority != null && !"normal".equalsIgnoreCase(priority.trim());
    }

    public boolean isValid() {
        return amazonShipmentId != null &&
               warehouseId != null &&
               destX != null &&
               destY != null &&
               userId != null;
    }

    // Explicit getters and setters for Lombok compatibility
    public Long getAmazonShipmentId() { return amazonShipmentId; }
    public void setAmazonShipmentId(Long amazonShipmentId) { this.amazonShipmentId = amazonShipmentId; }
    
    public Integer getWarehouseId() { return warehouseId; }
    public void setWarehouseId(Integer warehouseId) { this.warehouseId = warehouseId; }
    
    public Integer getDestX() { return destX; }
    public void setDestX(Integer destX) { this.destX = destX; }
    
    public Integer getDestY() { return destY; }
    public void setDestY(Integer destY) { this.destY = destY; }
    
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    
    public String getPackageDescription() { return packageDescription; }
    public void setPackageDescription(String packageDescription) { this.packageDescription = packageDescription; }
    
    public Double getPackageWeight() { return packageWeight; }
    public void setPackageWeight(Double packageWeight) { this.packageWeight = packageWeight; }
    
    public Double getPackageLength() { return packageLength; }
    public void setPackageLength(Double packageLength) { this.packageLength = packageLength; }
    
    public Double getPackageWidth() { return packageWidth; }
    public void setPackageWidth(Double packageWidth) { this.packageWidth = packageWidth; }
    
    public Double getPackageHeight() { return packageHeight; }
    public void setPackageHeight(Double packageHeight) { this.packageHeight = packageHeight; }
    
    public String getSpecialInstructions() { return specialInstructions; }
    public void setSpecialInstructions(String specialInstructions) { this.specialInstructions = specialInstructions; }
    
    public String getPriority() { return priority; }
    public void setPriority(String priority) { this.priority = priority; }
    
    public String getDeliveryTimeframe() { return deliveryTimeframe; }
    public void setDeliveryTimeframe(String deliveryTimeframe) { this.deliveryTimeframe = deliveryTimeframe; }

    @Override
    public String toString() {
        return String.format("ShipmentCreationPayload{amazonId=%d, warehouseId=%d, destCoords=(%d,%d), userId=%d}", 
                amazonShipmentId, warehouseId, destX, destY, userId);
    }
}
