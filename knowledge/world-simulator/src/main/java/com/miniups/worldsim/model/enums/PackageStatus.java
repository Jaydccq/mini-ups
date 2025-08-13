package com.miniups.worldsim.model.enums;

/**
 * Enumeration representing the possible states of a package in the World Simulator.
 * 
 * This enum defines the lifecycle states that a package progresses through during
 * the simulation from creation to final delivery. Each status represents a specific
 * phase in the package's journey and determines where the package is located
 * and what operations can be performed on it.
 * 
 * Package lifecycle follows this general flow:
 * CREATED → AT_WAREHOUSE → IN_TRANSIT → DELIVERED
 * 
 * Alternative paths for failed deliveries:
 * IN_TRANSIT → FAILED → RETURNED (back to warehouse)
 * 
 * @author World Simulator Team
 * @version 1.0.0
 * @since 2025-01-13
 */
public enum PackageStatus {
    
    /**
     * Package has been created but not yet assigned to a warehouse.
     * This is the initial state when a package is first entered into the system.
     */
    CREATED("Package created and awaiting warehouse assignment"),
    
    /**
     * Package is stored at a warehouse waiting for truck assignment.
     * The package has been received at a warehouse and is ready for dispatch.
     */
    AT_WAREHOUSE("Package is stored at warehouse awaiting dispatch"),
    
    /**
     * Package is loaded on a truck and in transit to destination.
     * The package is currently being transported by a truck.
     */
    IN_TRANSIT("Package is loaded on truck and in transit"),
    
    /**
     * Package has been successfully delivered to the destination.
     * This is the successful end state for a package delivery.
     */
    DELIVERED("Package has been successfully delivered"),
    
    /**
     * Package delivery failed and requires attention.
     * The delivery attempt was unsuccessful (address not found, recipient unavailable, etc.).
     */
    FAILED("Package delivery failed and requires attention"),
    
    /**
     * Package has been returned to the warehouse after failed delivery.
     * The package was returned to the origin warehouse due to delivery failure.
     */
    RETURNED("Package returned to warehouse after failed delivery");
    
    private final String description;
    
    /**
     * Constructor for PackageStatus enum values.
     * 
     * @param description Human-readable description of the package status
     */
    PackageStatus(String description) {
        this.description = description;
    }
    
    /**
     * Gets the human-readable description of this package status.
     * 
     * @return Description string explaining what this status means
     */
    public String getDescription() {
        return description;
    }
    
    /**
     * Checks if the package is currently in motion.
     * 
     * @return true if the package is in transit, false otherwise
     */
    public boolean isInMotion() {
        return this == IN_TRANSIT;
    }
    
    /**
     * Checks if the package is at a fixed location.
     * 
     * @return true if the package is at a warehouse, false otherwise
     */
    public boolean isAtWarehouse() {
        return this == CREATED || this == AT_WAREHOUSE || this == RETURNED;
    }
    
    /**
     * Checks if the package has reached a final state.
     * 
     * @return true if the package is delivered, failed, or returned, false otherwise
     */
    public boolean isFinalState() {
        return this == DELIVERED || this == FAILED || this == RETURNED;
    }
    
    /**
     * Checks if the package delivery was successful.
     * 
     * @return true if the package was delivered successfully, false otherwise
     */
    public boolean isDelivered() {
        return this == DELIVERED;
    }
    
    /**
     * Checks if the package is available for truck assignment.
     * 
     * @return true if the package is at a warehouse and can be assigned to a truck
     */
    public boolean isAvailableForAssignment() {
        return this == AT_WAREHOUSE;
    }
    
    /**
     * Checks if the package delivery encountered problems.
     * 
     * @return true if the package failed delivery or was returned, false otherwise
     */
    public boolean hasDeliveryIssues() {
        return this == FAILED || this == RETURNED;
    }
    
    @Override
    public String toString() {
        return name() + " (" + description + ")";
    }
}