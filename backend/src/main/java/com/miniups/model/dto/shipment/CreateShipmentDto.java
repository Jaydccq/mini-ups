/**
 * Create Shipment Data Transfer Object
 * 
 * 功能说明：
 * - 用于创建新运单的请求数据传输对象
 * - 支持Amazon系统和外部API的运单创建请求
 * - 包含运单创建所需的所有必要信息
 * 
 * 数据字段：
 * - shipmentId: 运单ID (可选，系统自动生成)
 * - customerId: 客户ID
 * - customerName: 客户姓名
 * - customerEmail: 客户邮箱
 * - originX/Y: 起始坐标
 * - destX/Y: 目标坐标
 * - weight: 包裹重量
 * - priority: 运输优先级
 * 
 * 验证规则：
 * - 必填字段验证
 * - 坐标范围验证
 * - 重量范围检查
 * - 邮箱格式验证
 * 
 * @author Mini-UPS Team
 * @version 1.0.0
 */
package com.miniups.model.dto.shipment;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;

public class CreateShipmentDto {
    
    @Size(max = 50, message = "Shipment ID must not exceed 50 characters")
    private String shipmentId;
    
    @NotBlank(message = "Customer ID cannot be blank")
    @Size(max = 50, message = "Customer ID must not exceed 50 characters")
    private String customerId;
    
    @NotBlank(message = "Customer name cannot be blank")
    @Size(max = 100, message = "Customer name must not exceed 100 characters")
    private String customerName;
    
    @Email(message = "Invalid email format")
    @NotBlank(message = "Customer email cannot be blank")
    private String customerEmail;
    
    @NotNull(message = "Origin X coordinate is required")
    @Min(value = 0, message = "Origin X coordinate must be non-negative")
    @Max(value = 1000, message = "Origin X coordinate must not exceed 1000")
    private Integer originX;
    
    @NotNull(message = "Origin Y coordinate is required")
    @Min(value = 0, message = "Origin Y coordinate must be non-negative")
    @Max(value = 1000, message = "Origin Y coordinate must not exceed 1000")
    private Integer originY;
    
    @NotNull(message = "Destination X coordinate is required")
    @Min(value = 0, message = "Destination X coordinate must be non-negative")
    @Max(value = 1000, message = "Destination X coordinate must not exceed 1000")
    private Integer destX;
    
    @NotNull(message = "Destination Y coordinate is required")
    @Min(value = 0, message = "Destination Y coordinate must be non-negative")
    @Max(value = 1000, message = "Destination Y coordinate must not exceed 1000")
    private Integer destY;
    
    @NotNull(message = "Weight is required")
    @DecimalMin(value = "0.1", message = "Weight must be at least 0.1 kg")
    @DecimalMax(value = "50.0", message = "Weight cannot exceed 50 kg")
    private BigDecimal weight;
    
    @Pattern(regexp = "STANDARD|EXPRESS|OVERNIGHT", message = "Invalid priority")
    private String priority = "STANDARD";
    
    @Size(max = 500, message = "Special instructions must not exceed 500 characters")
    private String specialInstructions;
    
    private Boolean requiresSignature = false;
    private Boolean fragile = false;
    
    // Constructors
    public CreateShipmentDto() {}
    
    public CreateShipmentDto(String customerId, String customerName, String customerEmail,
                           Integer originX, Integer originY, Integer destX, Integer destY,
                           BigDecimal weight) {
        this.customerId = customerId;
        this.customerName = customerName;
        this.customerEmail = customerEmail;
        this.originX = originX;
        this.originY = originY;
        this.destX = destX;
        this.destY = destY;
        this.weight = weight;
    }
    
    // Getters and Setters
    public String getShipmentId() {
        return shipmentId;
    }
    
    public void setShipmentId(String shipmentId) {
        this.shipmentId = shipmentId;
    }
    
    public String getCustomerId() {
        return customerId;
    }
    
    public void setCustomerId(String customerId) {
        this.customerId = customerId;
    }
    
    public String getCustomerName() {
        return customerName;
    }
    
    public void setCustomerName(String customerName) {
        this.customerName = customerName;
    }
    
    public String getCustomerEmail() {
        return customerEmail;
    }
    
    public void setCustomerEmail(String customerEmail) {
        this.customerEmail = customerEmail;
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
    
    public String getPriority() {
        return priority;
    }
    
    public void setPriority(String priority) {
        this.priority = priority;
    }
    
    public String getSpecialInstructions() {
        return specialInstructions;
    }
    
    public void setSpecialInstructions(String specialInstructions) {
        this.specialInstructions = specialInstructions;
    }
    
    public Boolean getRequiresSignature() {
        return requiresSignature;
    }
    
    public void setRequiresSignature(Boolean requiresSignature) {
        this.requiresSignature = requiresSignature;
    }
    
    public Boolean getFragile() {
        return fragile;
    }
    
    public void setFragile(Boolean fragile) {
        this.fragile = fragile;
    }
    
    // Helper methods
    public boolean isHighPriority() {
        return "EXPRESS".equals(priority) || "OVERNIGHT".equals(priority);
    }
    
    public boolean hasSpecialHandlingRequirements() {
        return fragile || requiresSignature;
    }
    
    public double calculateDistance() {
        return Math.sqrt(Math.pow(destX - originX, 2) + Math.pow(destY - originY, 2));
    }
}