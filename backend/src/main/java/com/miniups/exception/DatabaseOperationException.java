/**
 * Database Operation Exception
 * 
 * 数据库操作异常，用于处理数据访问层的错误
 * 
 * Common scenarios:
 * - 数据库连接失败
 * - SQL执行超时
 * - 数据完整性约束违反
 * - 事务回滚异常
 * 
 * @author Mini-UPS Team
 * @version 1.0.0
 */
package com.miniups.exception;

import com.miniups.model.enums.ExceptionSeverity;

public class DatabaseOperationException extends BaseBusinessException {
    
    private final String operation;
    private final String entity;
    
    public DatabaseOperationException(String operation, String message) {
        super("DATABASE_OPERATION_ERROR", String.format("Database operation [%s] failed: %s", operation, message), ExceptionSeverity.HIGH);
        this.operation = operation;
        this.entity = null;
    }
    
    public DatabaseOperationException(String operation, String message, Throwable cause) {
        super("DATABASE_OPERATION_ERROR", String.format("Database operation [%s] failed: %s", operation, message), ExceptionSeverity.HIGH, cause);
        this.operation = operation;
        this.entity = null;
    }
    
    public DatabaseOperationException(String operation, String entity, String message) {
        super("DATABASE_OPERATION_ERROR", String.format("Database operation [%s] on entity [%s] failed: %s", operation, entity, message), ExceptionSeverity.HIGH);
        this.operation = operation;
        this.entity = entity;
    }
    
    public DatabaseOperationException(String operation, String entity, String message, Throwable cause) {
        super("DATABASE_OPERATION_ERROR", String.format("Database operation [%s] on entity [%s] failed: %s", operation, entity, message), ExceptionSeverity.HIGH, cause);
        this.operation = operation;
        this.entity = entity;
    }
    
    public String getOperation() {
        return operation;
    }
    
    public String getEntity() {
        return entity;
    }
    
    // 便利方法用于常见数据库操作异常
    public static DatabaseOperationException save(String entity, Throwable cause) {
        return new DatabaseOperationException("SAVE", entity, "Failed to save entity", cause);
    }
    
    public static DatabaseOperationException update(String entity, Throwable cause) {
        return new DatabaseOperationException("UPDATE", entity, "Failed to update entity", cause);
    }
    
    public static DatabaseOperationException delete(String entity, Throwable cause) {
        return new DatabaseOperationException("DELETE", entity, "Failed to delete entity", cause);
    }
    
    public static DatabaseOperationException find(String entity, String identifier) {
        return new DatabaseOperationException("FIND", entity, String.format("Entity not found with identifier: %s", identifier));
    }
}