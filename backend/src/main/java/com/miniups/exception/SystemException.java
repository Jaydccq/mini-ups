/**
 * System Exception
 * 
 * System-level exception used to handle internal system errors
 * 
 *  
 
 */
package com.miniups.exception;

import com.miniups.model.enums.ExceptionSeverity;

public class SystemException extends BaseBusinessException {
    
    public SystemException(String errorCode, String message) {
        super(errorCode, message, ExceptionSeverity.CRITICAL);
    }
    
    public SystemException(String errorCode, String message, Throwable cause) {
        super(errorCode, message, ExceptionSeverity.CRITICAL, cause);
    }
}