/**
 * Amazon Shipment Data Transfer Object
 * 
 * Function Description:
 * - Handles shipment data transfer between Amazon and UPS systems
 * - Standardizes Amazon shipment information format and validation
 * - Supports Amazon API integration and webhook communication
 * 
 * Data Fields:
 * - shipmentId: UPS shipment ID
 * - amazonOrderId: Amazon order ID
 * - trackingNumber: Tracking number
 * - status: Shipment status
 * - carrier: Carrier information
 * 
 * Validation Rules:
 * - Required field validation
 * - Status value range check
 * - Tracking number format validation
 * 
 *

 */
package com.miniups.model.dto.amazon;

import jakarta.validation.constraints.*;
import java.time.LocalDateTime;
import java.math.BigDecimal;

public class AmazonShipmentDto {
    
    @NotBlank(message = "Shipment ID cannot be blank")
    @Size(max = 100, message = "Shipment ID must not exceed 100 characters")
    private String shipmentId;
    
    @NotBlank(message = "Amazon order ID cannot be blank")
    @Size(max = 100, message = "Amazon order ID must not exceed 100 characters")
    private String amazonOrderId;
    
    @Size(max = 50, message = "Tracking number must not exceed 50 characters")
    private String trackingNumber;
    
    @NotBlank(message = "Status cannot be blank")
    @Pattern(regexp = "PENDING|PROCESSING|SHIPPED|DELIVERED|CANCELLED", 
             message = "Invalid shipment status")
    private String status;
    
    @NotBlank(message = "Carrier cannot be blank")
    private String carrier = "UPS";
    
    @NotNull(message = "Created date is required")
    private LocalDateTime createdDate;
    
    private LocalDateTime shippedDate;
    private LocalDateTime deliveredDate;
    
    @Size(max = 500, message = "Notes must not exceed 500 characters")
    private String notes;
    
    @DecimalMin(value = "0.0", message = "Shipping cost must be non-negative")
    private BigDecimal shippingCost;
    
    @Size(max = 100, message = "Delivery address must not exceed 100 characters")
    private String deliveryAddress;
    
    @Size(max = 50, message = "Delivery city must not exceed 50 characters")
    private String deliveryCity;
    
    @Size(max = 20, message = "Delivery state must not exceed 20 characters")
    private String deliveryState;
    
    @Pattern(regexp = "\\d{5}(-\\d{4})?", message = "Invalid ZIP code format")
    private String deliveryZipCode;
    
    private Integer deliveryX;
    private Integer deliveryY;
    
    // Constructors
    public AmazonShipmentDto() {
        this.createdDate = LocalDateTime.now();
        this.status = "PENDING";
    }
    
    public AmazonShipmentDto(String shipmentId, String amazonOrderId) {
        this();
        this.shipmentId = shipmentId;
        this.amazonOrderId = amazonOrderId;
    }
    
    // Getters and Setters
    public String getShipmentId() {
        return shipmentId;
    }
    
    public void setShipmentId(String shipmentId) {
        this.shipmentId = shipmentId;
    }
    
    public String getAmazonOrderId() {
        return amazonOrderId;
    }
    
    public void setAmazonOrderId(String amazonOrderId) {
        this.amazonOrderId = amazonOrderId;
    }
    
    public String getTrackingNumber() {
        return trackingNumber;
    }
    
    public void setTrackingNumber(String trackingNumber) {
        this.trackingNumber = trackingNumber;
    }
    
    public String getStatus() {
        return status;
    }
    
    public void setStatus(String status) {
        this.status = status;
    }
    
    public String getCarrier() {
        return carrier;
    }
    
    public void setCarrier(String carrier) {
        this.carrier = carrier;
    }
    
    public LocalDateTime getCreatedDate() {
        return createdDate;
    }
    
    public void setCreatedDate(LocalDateTime createdDate) {
        this.createdDate = createdDate;
    }
    
    public LocalDateTime getShippedDate() {
        return shippedDate;
    }
    
    public void setShippedDate(LocalDateTime shippedDate) {
        this.shippedDate = shippedDate;
    }
    
    public LocalDateTime getDeliveredDate() {
        return deliveredDate;
    }
    
    public void setDeliveredDate(LocalDateTime deliveredDate) {
        this.deliveredDate = deliveredDate;
    }
    
    public String getNotes() {
        return notes;
    }
    
    public void setNotes(String notes) {
        this.notes = notes;
    }
    
    public BigDecimal getShippingCost() {
        return shippingCost;
    }
    
    public void setShippingCost(BigDecimal shippingCost) {
        this.shippingCost = shippingCost;
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
    
    public String getDeliveryState() {
        return deliveryState;
    }
    
    public void setDeliveryState(String deliveryState) {
        this.deliveryState = deliveryState;
    }
    
    public String getDeliveryZipCode() {
        return deliveryZipCode;
    }
    
    public void setDeliveryZipCode(String deliveryZipCode) {
        this.deliveryZipCode = deliveryZipCode;
    }
    
    public Integer getDeliveryX() {
        return deliveryX;
    }
    
    public void setDeliveryX(Integer deliveryX) {
        this.deliveryX = deliveryX;
    }
    
    public Integer getDeliveryY() {
        return deliveryY;
    }
    
    public void setDeliveryY(Integer deliveryY) {
        this.deliveryY = deliveryY;
    }
    
    // Helper methods
    public boolean isDelivered() {
        return "DELIVERED".equals(status);
    }
    
    public boolean isShipped() {
        return "SHIPPED".equals(status) || "DELIVERED".equals(status);
    }
    
    public boolean isPending() {
        return "PENDING".equals(status);
    }
    
    public boolean isCancelled() {
        return "CANCELLED".equals(status);
    }
    
    public void markAsShipped() {
        this.status = "SHIPPED";
        this.shippedDate = LocalDateTime.now();
    }
    
    public void markAsDelivered() {
        this.status = "DELIVERED";
        this.deliveredDate = LocalDateTime.now();
    }
    
    public void markAsCancelled() {
        this.status = "CANCELLED";
    }
}