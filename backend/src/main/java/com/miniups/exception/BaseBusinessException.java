/**
 * Base Business Exception
 * 
 * 业务异常基类，所有业务相关异常都应继承此类
 * 
 * Features:
 * - 提供统一的错误代码和消息格式
 * - 支持异常链追踪
 * - 便于异常分类处理
 * - 提供异常严重等级分类
 * 
 * @author Mini-UPS Team
 * @version 1.0.0
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