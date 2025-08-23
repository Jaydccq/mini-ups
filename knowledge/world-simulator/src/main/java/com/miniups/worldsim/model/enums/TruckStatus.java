package com.miniups.worldsim.model.enums;

/**
 * Enumeration representing the possible states of a truck in the World Simulator.
 * 
 * This enum defines the lifecycle states that a truck can be in during the simulation.
 * Each state represents a specific phase of the truck's operation and determines
 * what actions are available and what behavior the truck exhibits.
 * 
 * State transitions are managed by the TruckController and follow these patterns:
 * - IDLE → TRAVELING (when assigned a delivery)
 * - TRAVELING → LOADING/UNLOADING (when arriving at warehouse/destination)
 * - LOADING → DELIVERING (when packages are loaded)
 * - DELIVERING → UNLOADING (when arriving at destination)
 * - UNLOADING → RETURNING/IDLE (when packages are delivered)
 * - RETURNING → IDLE (when returning to warehouse)
 * - Any state → MAINTENANCE (for maintenance operations)
 * 
 * @author World Simulator Team
 * @version 1.0.0
 * @since 2025-01-13
 */
public enum TruckStatus {
    
    /**
     * Truck is idle and available for assignment.
     * The truck is parked at a warehouse and ready to accept new delivery tasks.
     */
    IDLE("Truck is idle and available for assignment"),
    
    /**
     * Truck is traveling to a destination.
     * The truck is in motion between locations (warehouse to destination or return).
     */
    TRAVELING("Truck is traveling to destination"),
    
    /**
     * Truck is loading packages at a warehouse.
     * The truck is at a warehouse and packages are being loaded onto it.
     */
    LOADING("Truck is loading packages at warehouse"),
    
    /**
     * Truck is unloading packages at a destination.
     * The truck has arrived at the delivery destination and packages are being unloaded.
     */
    UNLOADING("Truck is unloading packages at destination"),
    
    /**
     * Truck is delivering packages to customers.
     * The truck is actively delivering packages (may involve multiple stops).
     */
    DELIVERING("Truck is delivering packages to customers"),
    
    /**
     * Truck is returning to warehouse after delivery.
     * The truck has completed deliveries and is returning to its assigned warehouse.
     */
    RETURNING("Truck is returning to warehouse after delivery"),
    
    /**
     * Truck is undergoing maintenance.
     * The truck is temporarily out of service for maintenance or repairs.
     */
    MAINTENANCE("Truck is undergoing maintenance");
    
    private final String description;
    
    /**
     * Constructor for TruckStatus enum values.
     * 
     * @param description Human-readable description of the truck status
     */
    TruckStatus(String description) {
        this.description = description;
    }
    
    /**
     * Gets the human-readable description of this truck status.
     * 
     * @return Description string explaining what this status means
     */
    public String getDescription() {
        return description;
    }
    
    /**
     * Checks if the truck is currently in motion.
     * 
     * @return true if the truck is traveling or delivering, false otherwise
     */
    public boolean isMoving() {
        return this == TRAVELING || this == DELIVERING || this == RETURNING;
    }
    
    /**
     * Checks if the truck is available for new assignments.
     * 
     * @return true if the truck is idle, false otherwise
     */
    public boolean isAvailable() {
        return this == IDLE;
    }
    
    /**
     * Checks if the truck is currently occupied with a task.
     * 
     * @return true if the truck is busy (not idle or maintenance), false otherwise
     */
    public boolean isBusy() {
        return !isAvailable() && this != MAINTENANCE;
    }
    
    /**
     * Checks if the truck is at a fixed location (not moving).
     * 
     * @return true if the truck is not moving, false otherwise
     */
    public boolean isStationary() {
        return !isMoving();
    }
    
    @Override
    public String toString() {
        return name() + " (" + description + ")";
    }
}