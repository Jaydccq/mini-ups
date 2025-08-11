/**
 * Exception Severity Enum
 * 
 * Enumeration for categorizing the severity of exceptions
 * 
 *
 
 */
package com.miniups.model.enums;

public enum ExceptionSeverity {
    
    /**
     * Low - User input errors, validation failures, etc.
     */
    LOW("Low", "User error or minor issue"),
    
    /**
     * Medium - Business logic issues, authorization problems, etc.
     */
    MEDIUM("Medium", "Business logic issue"),
    
    /**
     * High - System errors, external service failures, database issues, etc.
     */
    HIGH("High", "System or infrastructure issue"),
    
    /**
     * Critical - Critical errors affecting the entire system
     */
    CRITICAL("Critical", "Critical system failure");
    
    private final String displayName;
    private final String description;
    
    ExceptionSeverity(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    public String getDescription() {
        return description;
    }
    
    @Override
    public String toString() {
        return name();
    }
}
