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
import com.miniups.util.Constants;

public class CreateShipmentDto {
    
    @Size(max = Constants.MAX_SHIPMENT_ID_LENGTH, message = "Shipment ID must not exceed " + Constants.MAX_SHIPMENT_ID_LENGTH + " characters")
    private String shipmentId;
    
    @NotBlank(message = "Customer ID cannot be blank")
    @Size(max = Constants.MAX_USERNAME_LENGTH, message = "Customer ID must not exceed " + Constants.MAX_USERNAME_LENGTH + " characters")
    private String customerId;
    
    @NotBlank(message = "Customer name cannot be blank")
    @Size(max = Constants.MAX_CUSTOMER_NAME_LENGTH, message = "Customer name must not exceed " + Constants.MAX_CUSTOMER_NAME_LENGTH + " characters")
    private String customerName;
    
    @Email(message = "Invalid email format")
    @NotBlank(message = "Customer email cannot be blank")
    private String customerEmail;
    
    @NotNull(message = "Origin X coordinate is required")
    @Min(value = Constants.MIN_COORDINATE, message = "Origin X coordinate must be non-negative")
    @Max(value = Constants.MAX_COORDINATE_X, message = "Origin X coordinate must not exceed " + Constants.MAX_COORDINATE_X)
    private Integer originX;
    
    @NotNull(message = "Origin Y coordinate is required")
    @Min(value = Constants.MIN_COORDINATE, message = "Origin Y coordinate must be non-negative")
    @Max(value = Constants.MAX_COORDINATE_Y, message = "Origin Y coordinate must not exceed " + Constants.MAX_COORDINATE_Y)
    private Integer originY;
    
    @NotNull(message = "Destination X coordinate is required")
    @Min(value = Constants.MIN_COORDINATE, message = "Destination X coordinate must be non-negative")
    @Max(value = Constants.MAX_COORDINATE_X, message = "Destination X coordinate must not exceed " + Constants.MAX_COORDINATE_X)
    private Integer destX;
    
    @NotNull(message = "Destination Y coordinate is required")
    @Min(value = Constants.MIN_COORDINATE, message = "Destination Y coordinate must be non-negative")
    @Max(value = Constants.MAX_COORDINATE_Y, message = "Destination Y coordinate must not exceed " + Constants.MAX_COORDINATE_Y)
    private Integer destY;
    
    @NotNull(message = "Weight is required")
    @DecimalMin(value = "" + Constants.MIN_PACKAGE_WEIGHT, message = "Weight must be at least " + Constants.MIN_PACKAGE_WEIGHT + " kg")
    @DecimalMax(value = "" + Constants.MAX_PACKAGE_WEIGHT, message = "Weight cannot exceed " + Constants.MAX_PACKAGE_WEIGHT + " kg")
    private BigDecimal weight;
    
    @Pattern(regexp = Constants.PRIORITY_REGEX, message = "Invalid priority")
    private String priority = Constants.DEFAULT_PRIORITY;
    
    @Size(max = Constants.MAX_SPECIAL_INSTRUCTIONS_LENGTH, message = "Special instructions must not exceed " + Constants.MAX_SPECIAL_INSTRUCTIONS_LENGTH + " characters")
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