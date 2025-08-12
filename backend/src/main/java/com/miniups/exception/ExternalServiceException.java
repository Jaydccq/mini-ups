/**
 * External Service Exception
 * 
 * External service invocation exception used to handle errors related to external API and service integration
 * 
 * Common scenarios:
 * - Amazon API call failure
 * - World Simulator connection exception
 * - Third-party payment service exception
 * - Network connection timeout
 * 
 *
 
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
    
    // Convenience methods for common external service exceptions
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