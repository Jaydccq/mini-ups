/**
 * System Exception
 * 
 * 系统级异常，用于处理系统内部错误
 * 
 * @author Mini-UPS Team  
 * @version 1.0.0
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