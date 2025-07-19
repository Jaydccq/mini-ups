/**
 * Amazon Order Data Transfer Object
 * 
 * 功能说明：
 * - 处理Amazon系统与UPS系统之间的订单数据传输
 * - 标准化Amazon订单信息的格式和验证
 * - 支持Amazon API集成和webhook通信
 * 
 * 数据字段：
 * - orderId: Amazon订单ID
 * - customerId: 客户ID
 * - items: 订单商品列表
 * - shippingAddress: 配送地址信息
 * - dimensions: 包裹尺寸和重量
 * 
 * 验证规则：
 * - 必填字段验证
 * - 地址格式验证
 * - 重量和尺寸范围检查
 * - 商品数量合理性验证
 * 
 * @author Mini-UPS Team
 * @version 1.0.0
 */
package com.miniups.model.dto.amazon;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class AmazonOrderDto {
    
    @NotBlank(message = "Amazon order ID cannot be blank")
    @Size(max = 100, message = "Order ID must not exceed 100 characters")
    private String orderId;
    
    @NotBlank(message = "Customer ID cannot be blank")
    @Size(max = 50, message = "Customer ID must not exceed 50 characters")
    private String customerId;
    
    @NotBlank(message = "Customer name cannot be blank")
    @Size(max = 100, message = "Customer name must not exceed 100 characters")
    private String customerName;
    
    @Email(message = "Invalid email format")
    @NotBlank(message = "Customer email cannot be blank")
    private String customerEmail;
    
    @Valid
    @NotNull(message = "Shipping address is required")
    private ShippingAddressDto shippingAddress;
    
    @Valid
    @NotEmpty(message = "Order must contain at least one item")
    private List<OrderItemDto> items = new ArrayList<>();
    
    @DecimalMin(value = "0.1", message = "Total weight must be at least 0.1 kg")
    @DecimalMax(value = "50.0", message = "Total weight cannot exceed 50 kg")
    private BigDecimal totalWeight;
    
    @DecimalMin(value = "0.01", message = "Total value must be positive")
    private BigDecimal totalValue;
    
    @Pattern(regexp = "STANDARD|EXPRESS|OVERNIGHT", message = "Invalid shipping priority")
    private String shippingPriority = "STANDARD";
    
    private LocalDateTime orderDate;
    private LocalDateTime requestedDeliveryDate;
    
    @Size(max = 500, message = "Special instructions must not exceed 500 characters")
    private String specialInstructions;
    
    private Boolean requiresSignature = false;
    private Boolean fragile = false;
    private Boolean hazardous = false;
    
    // Constructors
    public AmazonOrderDto() {
        this.orderDate = LocalDateTime.now();
    }
    
    public AmazonOrderDto(String orderId, String customerId, String customerName, String customerEmail) {
        this();
        this.orderId = orderId;
        this.customerId = customerId;
        this.customerName = customerName;
        this.customerEmail = customerEmail;
    }
    
    // Getters and Setters
    public String getOrderId() {
        return orderId;
    }
    
    public void setOrderId(String orderId) {
        this.orderId = orderId;
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
    
    public ShippingAddressDto getShippingAddress() {
        return shippingAddress;
    }
    
    public void setShippingAddress(ShippingAddressDto shippingAddress) {
        this.shippingAddress = shippingAddress;
    }
    
    public List<OrderItemDto> getItems() {
        return items;
    }
    
    public void setItems(List<OrderItemDto> items) {
        this.items = items;
    }
    
    public BigDecimal getTotalWeight() {
        return totalWeight;
    }
    
    public void setTotalWeight(BigDecimal totalWeight) {
        this.totalWeight = totalWeight;
    }
    
    public BigDecimal getTotalValue() {
        return totalValue;
    }
    
    public void setTotalValue(BigDecimal totalValue) {
        this.totalValue = totalValue;
    }
    
    public String getShippingPriority() {
        return shippingPriority;
    }
    
    public void setShippingPriority(String shippingPriority) {
        this.shippingPriority = shippingPriority;
    }
    
    public LocalDateTime getOrderDate() {
        return orderDate;
    }
    
    public void setOrderDate(LocalDateTime orderDate) {
        this.orderDate = orderDate;
    }
    
    public LocalDateTime getRequestedDeliveryDate() {
        return requestedDeliveryDate;
    }
    
    public void setRequestedDeliveryDate(LocalDateTime requestedDeliveryDate) {
        this.requestedDeliveryDate = requestedDeliveryDate;
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
    
    public Boolean getHazardous() {
        return hazardous;
    }
    
    public void setHazardous(Boolean hazardous) {
        this.hazardous = hazardous;
    }
    
    // Helper methods
    public void addItem(OrderItemDto item) {
        if (this.items == null) {
            this.items = new ArrayList<>();
        }
        this.items.add(item);
    }
    
    public int getTotalItemCount() {
        return items.stream().mapToInt(OrderItemDto::getQuantity).sum();
    }
    
    public boolean isHighPriority() {
        return "EXPRESS".equals(shippingPriority) || "OVERNIGHT".equals(shippingPriority);
    }
    
    public boolean hasSpecialHandlingRequirements() {
        return fragile || hazardous || requiresSignature;
    }
    
    // Nested DTOs
    public static class ShippingAddressDto {
        @NotBlank(message = "Street address cannot be blank")
        private String street;
        
        @NotBlank(message = "City cannot be blank")
        private String city;
        
        @NotBlank(message = "State cannot be blank")
        private String state;
        
        @Pattern(regexp = "\\d{5}(-\\d{4})?", message = "Invalid ZIP code format")
        private String zipCode;
        
        @NotBlank(message = "Country cannot be blank")
        private String country = "USA";
        
        @NotNull(message = "X coordinate is required")
        private Integer x;
        
        @NotNull(message = "Y coordinate is required")
        private Integer y;
        
        // Constructors, getters, and setters
        public ShippingAddressDto() {}
        
        public String getStreet() { return street; }
        public void setStreet(String street) { this.street = street; }
        
        public String getCity() { return city; }
        public void setCity(String city) { this.city = city; }
        
        public String getState() { return state; }
        public void setState(String state) { this.state = state; }
        
        public String getZipCode() { return zipCode; }
        public void setZipCode(String zipCode) { this.zipCode = zipCode; }
        
        public String getCountry() { return country; }
        public void setCountry(String country) { this.country = country; }
        
        public Integer getX() { return x; }
        public void setX(Integer x) { this.x = x; }
        
        public Integer getY() { return y; }
        public void setY(Integer y) { this.y = y; }
    }
    
    public static class OrderItemDto {
        @NotBlank(message = "Product ID cannot be blank")
        private String productId;
        
        @NotBlank(message = "Product name cannot be blank")
        private String productName;
        
        @Min(value = 1, message = "Quantity must be at least 1")
        private int quantity;
        
        @DecimalMin(value = "0.01", message = "Unit price must be positive")
        private BigDecimal unitPrice;
        
        @DecimalMin(value = "0.01", message = "Unit weight must be positive")
        private BigDecimal unitWeight;
        
        // Constructors, getters, and setters
        public OrderItemDto() {}
        
        public OrderItemDto(String productId, String productName, int quantity, 
                           BigDecimal unitPrice, BigDecimal unitWeight) {
            this.productId = productId;
            this.productName = productName;
            this.quantity = quantity;
            this.unitPrice = unitPrice;
            this.unitWeight = unitWeight;
        }
        
        public String getProductId() { return productId; }
        public void setProductId(String productId) { this.productId = productId; }
        
        public String getProductName() { return productName; }
        public void setProductName(String productName) { this.productName = productName; }
        
        public int getQuantity() { return quantity; }
        public void setQuantity(int quantity) { this.quantity = quantity; }
        
        public BigDecimal getUnitPrice() { return unitPrice; }
        public void setUnitPrice(BigDecimal unitPrice) { this.unitPrice = unitPrice; }
        
        public BigDecimal getUnitWeight() { return unitWeight; }
        public void setUnitWeight(BigDecimal unitWeight) { this.unitWeight = unitWeight; }
        
        public BigDecimal getTotalPrice() {
            return unitPrice.multiply(BigDecimal.valueOf(quantity));
        }
        
        public BigDecimal getTotalWeight() {
            return unitWeight.multiply(BigDecimal.valueOf(quantity));
        }
    }
}