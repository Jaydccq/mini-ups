package com.miniups.model.enums;

public enum UserRole {
    USER("User"),
    ADMIN("Administrator"),
    DRIVER("Driver"),
    OPERATOR("Operator");
    
    private final String displayName;
    
    UserRole(String displayName) {
        this.displayName = displayName;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    public boolean hasRole(UserRole role) {
        return this == role;
    }
    
    public boolean isAdmin() {
        return this == ADMIN;
    }
    
    public boolean isDriver() {
        return this == DRIVER;
    }
}