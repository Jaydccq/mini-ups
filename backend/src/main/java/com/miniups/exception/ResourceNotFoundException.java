/**
 * Resource Not Found Exception
 * 
 * Resource not found exception, used to handle cases where various resources cannot be found
 * 
 * Common scenarios:
 * - Shipment not found
 * - Truck not found
 * - Package not found
 * - Warehouse not found
 * 
 *
 
 */
package com.miniups.exception;

import com.miniups.model.enums.ExceptionSeverity;

public class ResourceNotFoundException extends BaseBusinessException {
    
    public ResourceNotFoundException(String resourceType, String identifier) {
        super("RESOURCE_NOT_FOUND", String.format("%s not found with identifier: %s", resourceType, identifier), ExceptionSeverity.LOW);
    }
    
    public ResourceNotFoundException(String resourceType, Long id) {
        this(resourceType, String.valueOf(id));
    }
    
    // Convenience methods for common resource types
    public static ResourceNotFoundException shipment(Long shipmentId) {
        return new ResourceNotFoundException("Shipment", shipmentId);
    }
    
    public static ResourceNotFoundException truck(Long truckId) {
        return new ResourceNotFoundException("Truck", truckId);
    }
    
    public static ResourceNotFoundException packageResource(Long packageId) {
        return new ResourceNotFoundException("Package", packageId);
    }
    
    public static ResourceNotFoundException warehouse(Long warehouseId) {
        return new ResourceNotFoundException("Warehouse", warehouseId);
    }
    
    public static ResourceNotFoundException tracking(String trackingNumber) {
        return new ResourceNotFoundException("Tracking", trackingNumber);
    }
}