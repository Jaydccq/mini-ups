/**
 * Business Validation Exception
 * 
 * Business validation exception used to handle cases where business rule validation fails
 * 
 * Common scenarios:
 * - Order status does not allow current operation
 * - Insufficient truck capacity
 * - Delivery address is out of service range
 * - Package weight exceeds limit
 * 
 *
 
 */
package com.miniups.exception;

import com.miniups.model.enums.ExceptionSeverity;
import java.util.Map;

public class BusinessValidationException extends BaseBusinessException {
    
    private final String validationType;
    private final Map<String, Object> validationDetails;
    
    public BusinessValidationException(String validationType, String message) {
        super(validationType, message, ExceptionSeverity.LOW); // Use validationType as error code
        this.validationType = validationType;
        this.validationDetails = null;
    }
    
    public BusinessValidationException(String validationType, String message, Map<String, Object> validationDetails) {
        super(validationType, message, ExceptionSeverity.LOW); // Use validationType as error code
        this.validationType = validationType;
        this.validationDetails = validationDetails;
    }
    
    public String getValidationType() {
        return validationType;
    }
    
    public Map<String, Object> getValidationDetails() {
        return validationDetails;
    }
    
    // Convenience methods for common business validation exceptions
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
