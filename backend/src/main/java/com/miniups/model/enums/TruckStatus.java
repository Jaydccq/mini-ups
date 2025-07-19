package com.miniups.model.enums;

public enum TruckStatus {
    IDLE("Idle"),
    EN_ROUTE("En Route"),
    TRAVELING("Traveling to Warehouse"),
    AT_WAREHOUSE("At Warehouse"),
    ARRIVE_WAREHOUSE("Arrived at Warehouse"),
    LOADING("Loading Package"),
    DELIVERING("Out for Delivery");
    
    private final String displayName;
    
    TruckStatus(String displayName) {
        this.displayName = displayName;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    public boolean isAvailable() {
        return this == IDLE;
    }
    
    public boolean isBusy() {
        return this == TRAVELING || this == LOADING || this == DELIVERING;
    }
    
    public boolean canReceiveNewOrders() {
        return this == IDLE || this == ARRIVE_WAREHOUSE;
    }
}