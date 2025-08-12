/**
 * Base Business Exception
 * 
 * Base class for business exceptions, all business-related exceptions should inherit from this class
 * 
 * Features:
 * - Provides unified error codes and message format
 * - Supports exception chain tracing
 * - Facilitates exception categorization and handling
 * - Provides exception severity level classification
 * 
 *
 
 */
package com.miniups.exception;

import com.miniups.model.enums.ExceptionSeverity;

public abstract class BaseBusinessException extends RuntimeException {
    
    private final String errorCode;
    private final Object[] args;
    private final ExceptionSeverity severity;
    
    public BaseBusinessException(String errorCode, String message, ExceptionSeverity severity) {
        super(message);
        this.errorCode = errorCode;
        this.args = null;
        this.severity = severity;
    }
    
    public BaseBusinessException(String errorCode, String message, ExceptionSeverity severity, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
        this.args = null;
        this.severity = severity;
    }
    
    public BaseBusinessException(String errorCode, String message, Object[] args, ExceptionSeverity severity) {
        super(message);
        this.errorCode = errorCode;
        this.args = args;
        this.severity = severity;
    }
    
    public BaseBusinessException(String errorCode, String message, Object[] args, ExceptionSeverity severity, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
        this.args = args;
        this.severity = severity;
    }
    
    public String getErrorCode() {
        return errorCode;
    }
    
    public Object[] getArgs() {
        return args;
    }
    
    public ExceptionSeverity getSeverity() {
        return severity;
    }
}