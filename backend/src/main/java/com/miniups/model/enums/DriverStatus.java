/**
 * Driver Status Enum
 * 
 * Description:
 * - Defines possible statuses for drivers in the fleet management system
 * - Used to track driver availability and work status
 * 
 * Status Values:
 * - UNASSIGNED: Driver is available but not assigned to any truck
 * - ASSIGNED: Driver is assigned to a truck and available for work
 * - ON_DUTY: Driver is currently working (delivering packages)
 * - OFF_DUTY: Driver is not available for work (break, end of shift)
 * - ON_LEAVE: Driver is on vacation or sick leave
 * - INACTIVE: Driver account is disabled or suspended
 * 
 * Business Logic:
 * - Only UNASSIGNED drivers can be assigned to trucks
 * - ASSIGNED drivers can start deliveries (become ON_DUTY)
 * - ON_DUTY drivers cannot be reassigned until they finish their tasks
 * 
 */
package com.miniups.model.enums;

public enum DriverStatus {
    UNASSIGNED("Available for assignment"),
    ASSIGNED("Assigned to truck"),
    ON_DUTY("Currently working"),
    OFF_DUTY("Not on duty"),
    ON_LEAVE("On leave"),
    INACTIVE("Inactive/Suspended");
    
    private final String description;
    
    DriverStatus(String description) {
        this.description = description;
    }
    
    public String getDescription() {
        return description;
    }
    
    public boolean canBeAssigned() {
        return this == UNASSIGNED;
    }
    
    public boolean canStartWork() {
        return this == ASSIGNED;
    }
    
    public boolean isActive() {
        return this != INACTIVE;
    }
}