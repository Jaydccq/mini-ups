/**
 * Resource Not Found Exception
 * 
 * 资源未找到异常，用于处理各类资源查找失败的情况
 * 
 * Common scenarios:
 * - 订单未找到
 * - 卡车未找到
 * - 包裹未找到
 * - 仓库未找到
 * 
 * @author Mini-UPS Team
 * @version 1.0.0
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
    
    // 便利方法用于常见资源类型
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