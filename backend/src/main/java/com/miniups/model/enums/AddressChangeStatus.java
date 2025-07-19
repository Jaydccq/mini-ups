package com.miniups.model.enums;

public enum AddressChangeStatus {
    PENDING("Pending"),
    APPROVED("Approved"),
    REJECTED("Rejected");
    
    private final String displayName;
    
    AddressChangeStatus(String displayName) {
        this.displayName = displayName;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    public boolean isPending() {
        return this == PENDING;
    }
    
    public boolean isProcessed() {
        return this == APPROVED || this == REJECTED;
    }
}