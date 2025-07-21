/**
 * Shipment Creation Exception
 * 
 * 功能说明：
 * - 专门处理运单创建过程中的业务异常
 * - 提供详细的错误信息和上下文
 * - 便于异常分类和处理
 * 
 * 使用场景：
 * - 运单数据验证失败
 * - 车辆分配失败
 * - 用户创建失败
 * - 系统资源不足
 * 
 * @author Mini-UPS Team
 * @version 1.0.0
 */
package com.miniups.exception;

import com.miniups.model.enums.ExceptionSeverity;

public class ShipmentCreationException extends BaseBusinessException {
    
    private static final String DEFAULT_MESSAGE = "Failed to create shipment";
    private static final String ERROR_CODE = "SHIPMENT_CREATION_ERROR";
    
    /**
     * 创建运单创建异常
     * 
     * @param message 异常消息
     */
    public ShipmentCreationException(String message) {
        super(ERROR_CODE, message, ExceptionSeverity.MEDIUM);
    }
    
    /**
     * 创建运单创建异常，包含原因
     * 
     * @param message 异常消息
     * @param cause 原始异常
     */
    public ShipmentCreationException(String message, Throwable cause) {
        super(ERROR_CODE, message, ExceptionSeverity.MEDIUM, cause);
    }
    
    /**
     * 使用默认消息创建异常
     * 
     * @param cause 原始异常
     */
    public ShipmentCreationException(Throwable cause) {
        super(ERROR_CODE, DEFAULT_MESSAGE, ExceptionSeverity.MEDIUM, cause);
    }
    
    /**
     * 创建带有客户信息的异常
     * 
     * @param customerName 客户姓名
     * @param cause 原始异常
     * @return 运单创建异常
     */
    public static ShipmentCreationException forCustomer(String customerName, Throwable cause) {
        String message = String.format("Failed to create shipment for customer '%s'", customerName);
        return new ShipmentCreationException(message, cause);
    }
    
    /**
     * 创建车辆分配失败的异常
     * 
     * @param trackingNumber 追踪号
     * @return 运单创建异常
     */
    public static ShipmentCreationException truckAssignmentFailed(String trackingNumber) {
        String message = String.format("Failed to assign truck for shipment '%s'", trackingNumber);
        return new ShipmentCreationException(message);
    }
    
    /**
     * 创建用户创建失败的异常
     * 
     * @param customerEmail 客户邮箱
     * @param cause 原始异常
     * @return 运单创建异常
     */
    public static ShipmentCreationException userCreationFailed(String customerEmail, Throwable cause) {
        String message = String.format("Failed to create user for customer email '%s'", customerEmail);
        return new ShipmentCreationException(message, cause);
    }
}