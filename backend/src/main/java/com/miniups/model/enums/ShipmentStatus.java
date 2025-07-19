package com.miniups.model.enums;

public enum ShipmentStatus {
    CREATED("Created"),
    TRUCK_DISPATCHED("Truck Dispatched"),
    PICKED_UP("Picked Up"),
    IN_TRANSIT("In Transit"),
    OUT_FOR_DELIVERY("Out for Delivery"),
    DELIVERY_ATTEMPTED("Delivery Attempted"),
    DELIVERED("Delivered"),
    CANCELLED("Cancelled"),
    EXCEPTION("Delivery Exception"),
    RETURNED("Returned");
    
    private final String displayName;
    
    ShipmentStatus(String displayName) {
        this.displayName = displayName;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    public boolean isCompleted() {
        return this == DELIVERED || this == RETURNED || this == CANCELLED;
    }
    
    public boolean canBeModified() {
        return this == CREATED || this == TRUCK_DISPATCHED || this == PICKED_UP;
    }
    
    public boolean isInProgress() {
        return this == IN_TRANSIT || this == OUT_FOR_DELIVERY;
    }
    
    public ShipmentStatus getNextStatus() {
        return switch (this) {
            case CREATED -> TRUCK_DISPATCHED;
            case TRUCK_DISPATCHED -> PICKED_UP;
            case PICKED_UP -> IN_TRANSIT;
            case IN_TRANSIT -> OUT_FOR_DELIVERY;
            case OUT_FOR_DELIVERY -> DELIVERED;
            default -> this;
        };
    }
}