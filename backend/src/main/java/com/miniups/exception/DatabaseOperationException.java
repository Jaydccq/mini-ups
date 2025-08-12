/**
 * Database Operation Exception
 * 
 * Database operation exception used to handle errors in the data access layer
 * 
 * Common scenarios:
 * - Database connection failure
 * - SQL execution timeout
 * - Data integrity constraint violation
 * - Transaction rollback exception
 * 
 *
 
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
    
    // Convenience methods for common database operation exceptions
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