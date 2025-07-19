/**
 * Business Validation Exception
 * 
 * 业务验证异常，用于处理业务规则验证失败的情况
 * 
 * Common scenarios:
 * - 订单状态不允许当前操作
 * - 卡车容量不足
 * - 配送地址超出服务范围
 * - 包裹重量超出限制
 * 
 * @author Mini-UPS Team
 * @version 1.0.0
 */
package com.miniups.exception;

import com.miniups.model.enums.ExceptionSeverity;
import java.util.Map;

public class BusinessValidationException extends BaseBusinessException {
    
    private final String validationType;
    private final Map<String, Object> validationDetails;
    
    public BusinessValidationException(String validationType, String message) {
        super("BUSINESS_VALIDATION_ERROR", message, ExceptionSeverity.LOW);
        this.validationType = validationType;
        this.validationDetails = null;
    }
    
    public BusinessValidationException(String validationType, String message, Map<String, Object> validationDetails) {
        super("BUSINESS_VALIDATION_ERROR", message, ExceptionSeverity.LOW);
        this.validationType = validationType;
        this.validationDetails = validationDetails;
    }
    
    public String getValidationType() {
        return validationType;
    }
    
    public Map<String, Object> getValidationDetails() {
        return validationDetails;
    }
    
    // 便利方法用于常见业务验证异常
    public static BusinessValidationException invalidShipmentStatus(String currentStatus, String requiredStatus) {
        return new BusinessValidationException("SHIPMENT_STATUS", 
            String.format("Invalid shipment status. Current: %s, Required: %s", currentStatus, requiredStatus));
    }
    
    public static BusinessValidationException insufficientCapacity(String resourceType, double required, double available) {
        return new BusinessValidationException("CAPACITY_CHECK", 
            String.format("Insufficient %s capacity. Required: %.2f, Available: %.2f", resourceType, required, available));
    }
    
    public static BusinessValidationException addressOutOfRange(String address) {
        return new BusinessValidationException("ADDRESS_VALIDATION", 
            String.format("Delivery address is out of service range: %s", address));
    }
    
    public static BusinessValidationException weightExceedsLimit(double weight, double limit) {
        return new BusinessValidationException("WEIGHT_CHECK", 
            String.format("Package weight exceeds limit. Weight: %.2f kg, Limit: %.2f kg", weight, limit));
    }
}