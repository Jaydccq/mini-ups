package com.miniups.worldsim.model.enums;

/**
 * Enumeration representing the operational status of warehouses in the World Simulator.
 * 
 * This enum defines the different operational states a warehouse can be in,
 * which affects its ability to store packages, dispatch trucks, and participate
 * in the logistics operations. The status determines what operations are
 * permitted at the warehouse.
 * 
 * @author World Simulator Team
 * @version 1.0.0
 * @since 2025-01-13
 */
public enum WarehouseStatus {
    
    /**
     * Warehouse is fully operational and available for all operations.
     * Can accept packages, dispatch trucks, and perform all normal warehouse functions.
     */
    ACTIVE("Warehouse is fully operational and available for all operations"),
    
    /**
     * Warehouse is temporarily inactive but not undergoing maintenance.
     * May be due to staffing issues, temporary closure, or operational decisions.
     * Cannot accept new packages or dispatch trucks.
     */
    INACTIVE("Warehouse is temporarily inactive and not accepting operations"),
    
    /**
     * Warehouse is undergoing maintenance or repairs.
     * Cannot perform any operations until maintenance is complete.
     * Existing packages may remain but no new operations are permitted.
     */
    MAINTENANCE("Warehouse is undergoing maintenance or repairs");
    
    private final String description;
    
    /**
     * Constructor for WarehouseStatus enum values.
     * 
     * @param description Human-readable description of the warehouse status
     */
    WarehouseStatus(String description) {
        this.description = description;
    }
    
    /**
     * Gets the human-readable description of this warehouse status.
     * 
     * @return Description string explaining what this status means
     */
    public String getDescription() {
        return description;
    }
    
    /**
     * Checks if the warehouse is operational and can accept packages.
     * 
     * @return true if the warehouse is active, false otherwise
     */
    public boolean isOperational() {
        return this == ACTIVE;
    }
    
    /**
     * Checks if the warehouse can dispatch trucks for deliveries.
     * 
     * @return true if the warehouse is active and can dispatch trucks
     */
    public boolean canDispatchTrucks() {
        return this == ACTIVE;
    }
    
    /**
     * Checks if the warehouse can receive new packages.
     * 
     * @return true if the warehouse is active and can receive packages
     */
    public boolean canReceivePackages() {
        return this == ACTIVE;
    }
    
    /**
     * Checks if the warehouse is temporarily unavailable.
     * 
     * @return true if the warehouse is inactive or under maintenance
     */
    public boolean isUnavailable() {
        return !isOperational();
    }
    
    /**
     * Checks if the warehouse status allows for limited operations.
     * Currently, only ACTIVE allows any operations.
     * 
     * @return true if limited operations are allowed, false otherwise
     */
    public boolean allowsLimitedOperations() {
        return this == ACTIVE; // Could be extended for partial operations in the future
    }
    
    @Override
    public String toString() {
        return name() + " (" + description + ")";
    }
}