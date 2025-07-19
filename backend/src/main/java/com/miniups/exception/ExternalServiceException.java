/**
 * External Service Exception
 * 
 * 外部服务调用异常，用于处理与外部API、服务集成相关的错误
 * 
 * Common scenarios:
 * - Amazon API调用失败
 * - World Simulator连接异常
 * - 第三方支付服务异常
 * - 网络连接超时
 * 
 * @author Mini-UPS Team
 * @version 1.0.0
 */
package com.miniups.exception;

import com.miniups.model.enums.ExceptionSeverity;

public class ExternalServiceException extends BaseBusinessException {
    
    private final String serviceName;
    
    public ExternalServiceException(String serviceName, String message) {
        super("EXTERNAL_SERVICE_ERROR", String.format("External service [%s] error: %s", serviceName, message), ExceptionSeverity.HIGH);
        this.serviceName = serviceName;
    }
    
    public ExternalServiceException(String serviceName, String message, Throwable cause) {
        super("EXTERNAL_SERVICE_ERROR", String.format("External service [%s] error: %s", serviceName, message), ExceptionSeverity.HIGH, cause);
        this.serviceName = serviceName;
    }
    
    public ExternalServiceException(String errorCode, String serviceName, String message) {
        super(errorCode, String.format("External service [%s] error: %s", serviceName, message), ExceptionSeverity.HIGH);
        this.serviceName = serviceName;
    }
    
    public ExternalServiceException(String errorCode, String serviceName, String message, Throwable cause) {
        super(errorCode, String.format("External service [%s] error: %s", serviceName, message), ExceptionSeverity.HIGH, cause);
        this.serviceName = serviceName;
    }
    
    public String getServiceName() {
        return serviceName;
    }
    
    // 便利方法用于常见的外部服务异常
    public static ExternalServiceException amazonService(String message) {
        return new ExternalServiceException("Amazon", message);
    }
    
    public static ExternalServiceException amazonService(String message, Throwable cause) {
        return new ExternalServiceException("Amazon", message, cause);
    }
    
    public static ExternalServiceException worldSimulator(String message) {
        return new ExternalServiceException("WorldSimulator", message);
    }
    
    public static ExternalServiceException worldSimulator(String message, Throwable cause) {
        return new ExternalServiceException("WorldSimulator", message, cause);
    }
}