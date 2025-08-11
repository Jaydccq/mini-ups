/**
 * Shipping Order Creation Request Data Transfer Object
 * 
 * Functionality:
 * - Receives ShipmentCreated messages from Amazon system
 * - Contains all information needed to create shipping orders
 * - Supports user association and address information management
 * 
 * Required Fields:
 * - user_id: User ID
 * - email: User email
 * - shipment_id: Amazon's shipping order ID
 * - warehouse_id: Warehouse ID (pickup point)
 * - destination_x/y: Target coordinates
 * 
 * Optional Fields:
 * - ups_account: UPS account association
 * 
 *
 
 */
package com.miniups.model.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class ShipmentCreatedDto {
    
    @NotNull(message = "User ID is required")
    private Long userId;
    
    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    @Size(max = 255, message = "Email must not exceed 255 characters")
    private String email;
    
    @NotBlank(message = "Shipment ID is required")
    @Size(max = 100, message = "Shipment ID must not exceed 100 characters")
    private String shipmentId;
    
    @NotNull(message = "Warehouse ID is required")
    private Long warehouseId;
    
    @NotNull(message = "Destination X coordinate is required")
    private Integer destinationX;
    
    @NotNull(message = "Destination Y coordinate is required")
    private Integer destinationY;
    
    @Size(max = 100, message = "UPS account must not exceed 100 characters")
    private String upsAccount;
    
    // Constructors
    public ShipmentCreatedDto() {}
    
    public ShipmentCreatedDto(Long userId, String email, String shipmentId, 
                            Long warehouseId, Integer destinationX, Integer destinationY) {
        this.userId = userId;
        this.email = email;
        this.shipmentId = shipmentId;
        this.warehouseId = warehouseId;
        this.destinationX = destinationX;
        this.destinationY = destinationY;
    }
    
    // Getters and Setters
    public Long getUserId() {
        return userId;
    }
    
    public void setUserId(Long userId) {
        this.userId = userId;
    }
    
    public String getEmail() {
        return email;
    }
    
    public void setEmail(String email) {
        this.email = email;
    }
    
    public String getShipmentId() {
        return shipmentId;
    }
    
    public void setShipmentId(String shipmentId) {
        this.shipmentId = shipmentId;
    }
    
    public Long getWarehouseId() {
        return warehouseId;
    }
    
    public void setWarehouseId(Long warehouseId) {
        this.warehouseId = warehouseId;
    }
    
    public Integer getDestinationX() {
        return destinationX;
    }
    
    public void setDestinationX(Integer destinationX) {
        this.destinationX = destinationX;
    }
    
    public Integer getDestinationY() {
        return destinationY;
    }
    
    public void setDestinationY(Integer destinationY) {
        this.destinationY = destinationY;
    }
    
    public String getUpsAccount() {
        return upsAccount;
    }
    
    public void setUpsAccount(String upsAccount) {
        this.upsAccount = upsAccount;
    }
    
    // Helper methods
    public boolean hasUpsAccount() {
        return upsAccount != null && !upsAccount.trim().isEmpty();
    }
    
    @Override
    public String toString() {
        return "ShipmentCreatedDto{" +
                "userId=" + userId +
                ", email='" + email + '\'' +
                ", shipmentId='" + shipmentId + '\'' +
                ", warehouseId=" + warehouseId +
                ", destinationX=" + destinationX +
                ", destinationY=" + destinationY +
                ", upsAccount='" + upsAccount + '\'' +
                '}';
    }
}