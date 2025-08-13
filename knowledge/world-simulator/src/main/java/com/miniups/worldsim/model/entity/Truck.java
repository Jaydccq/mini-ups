package com.miniups.worldsim.model.entity;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

import com.miniups.worldsim.model.enums.TruckStatus;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Entity representing a truck in the World Simulator.
 * 
 * A truck is a mobile entity responsible for transporting packages between
 * warehouses and delivery destinations. Each truck has a current position,
 * target destination, capacity, and operational status. Trucks are assigned
 * to warehouses and can carry multiple packages up to their capacity limit.
 * 
 * The truck's position is updated in real-time during simulation ticks based
 * on its speed and target coordinates. Trucks follow a state machine pattern
 * managed by the TruckController service.
 * 
 * Key responsibilities:
 * - Transport packages from warehouses to destinations
 * - Maintain current position and navigation to targets
 * - Track load capacity and current packages
 * - Follow assigned routes and schedules
 * - Report position updates to connected clients
 * 
 * @author World Simulator Team
 * @version 1.0.0
 * @since 2025-01-13
 */
@Entity
@Table(name = "trucks", uniqueConstraints = {
    @UniqueConstraint(columnNames = "truck_id", name = "uk_trucks_truck_id")
})
public class Truck extends BaseEntity {

    @NotBlank(message = "Truck ID is required")
    @Size(max = 50, message = "Truck ID must not exceed 50 characters")
    @Column(name = "truck_id", nullable = false, unique = true, length = 50)
    private String truckId;

    @NotNull(message = "Truck status is required")
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private TruckStatus status = TruckStatus.IDLE;

    @NotNull(message = "Current X coordinate is required")
    @Column(name = "current_x", nullable = false)
    private Integer currentX = 0;

    @NotNull(message = "Current Y coordinate is required")
    @Column(name = "current_y", nullable = false)
    private Integer currentY = 0;

    @Column(name = "target_x")
    private Integer targetX;

    @Column(name = "target_y")
    private Integer targetY;

    @Min(value = 1, message = "Speed must be positive")
    @Column(name = "speed", nullable = false)
    private Integer speed = 50;

    @Min(value = 1, message = "Capacity must be positive")
    @Column(name = "capacity", nullable = false)
    private Integer capacity = 100;

    @Min(value = 0, message = "Current load cannot be negative")
    @Column(name = "current_load", nullable = false)
    private Integer currentLoad = 0;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_warehouse_id", referencedColumnName = "id")
    private Warehouse assignedWarehouse;

    @Column(name = "estimated_arrival_time")
    private LocalDateTime estimatedArrivalTime;

    @NotNull(message = "Last position update is required")
    @Column(name = "last_position_update", nullable = false)
    private LocalDateTime lastPositionUpdate = LocalDateTime.now();

    @Min(value = 0, message = "Sequence number cannot be negative")
    @Column(name = "sequence_number", nullable = false)
    private Long sequenceNumber = 0L;

    @OneToMany(mappedBy = "assignedTruck", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private Set<Package> packages = new HashSet<>();

    /**
     * Default constructor for JPA.
     */
    protected Truck() {
        super();
    }

    /**
     * Constructor for creating a new truck.
     * 
     * @param truckId Unique identifier for the truck
     * @param currentX Initial X coordinate
     * @param currentY Initial Y coordinate
     * @param speed Movement speed in world units per second
     * @param capacity Maximum package capacity
     */
    public Truck(String truckId, Integer currentX, Integer currentY, Integer speed, Integer capacity) {
        this();
        this.truckId = truckId;
        this.currentX = currentX;
        this.currentY = currentY;
        this.speed = speed;
        this.capacity = capacity;
    }

    // Getters and Setters

    public String getTruckId() {
        return truckId;
    }

    public void setTruckId(String truckId) {
        this.truckId = truckId;
    }

    public TruckStatus getStatus() {
        return status;
    }

    public void setStatus(TruckStatus status) {
        this.status = status;
    }

    public Integer getCurrentX() {
        return currentX;
    }

    public void setCurrentX(Integer currentX) {
        this.currentX = currentX;
    }

    public Integer getCurrentY() {
        return currentY;
    }

    public void setCurrentY(Integer currentY) {
        this.currentY = currentY;
    }

    public Integer getTargetX() {
        return targetX;
    }

    public void setTargetX(Integer targetX) {
        this.targetX = targetX;
    }

    public Integer getTargetY() {
        return targetY;
    }

    public void setTargetY(Integer targetY) {
        this.targetY = targetY;
    }

    public Integer getSpeed() {
        return speed;
    }

    public void setSpeed(Integer speed) {
        this.speed = speed;
    }

    public Integer getCapacity() {
        return capacity;
    }

    public void setCapacity(Integer capacity) {
        this.capacity = capacity;
    }

    public Integer getCurrentLoad() {
        return currentLoad;
    }

    public void setCurrentLoad(Integer currentLoad) {
        this.currentLoad = currentLoad;
    }

    public Warehouse getAssignedWarehouse() {
        return assignedWarehouse;
    }

    public void setAssignedWarehouse(Warehouse assignedWarehouse) {
        this.assignedWarehouse = assignedWarehouse;
    }

    public LocalDateTime getEstimatedArrivalTime() {
        return estimatedArrivalTime;
    }

    public void setEstimatedArrivalTime(LocalDateTime estimatedArrivalTime) {
        this.estimatedArrivalTime = estimatedArrivalTime;
    }

    public LocalDateTime getLastPositionUpdate() {
        return lastPositionUpdate;
    }

    public void setLastPositionUpdate(LocalDateTime lastPositionUpdate) {
        this.lastPositionUpdate = lastPositionUpdate;
    }

    public Long getSequenceNumber() {
        return sequenceNumber;
    }

    public void setSequenceNumber(Long sequenceNumber) {
        this.sequenceNumber = sequenceNumber;
    }

    public Set<Package> getPackages() {
        return packages;
    }

    public void setPackages(Set<Package> packages) {
        this.packages = packages;
    }

    // Business Logic Methods

    /**
     * Calculates the available capacity in the truck.
     * 
     * @return Number of package units that can still be loaded
     */
    public Integer getAvailableCapacity() {
        return capacity - currentLoad;
    }

    /**
     * Calculates the load utilization percentage.
     * 
     * @return Percentage of capacity currently in use (0-100)
     */
    public Double getLoadUtilization() {
        if (capacity == 0) {
            return 0.0;
        }
        return (double) currentLoad / capacity * 100.0;
    }

    /**
     * Checks if the truck can accept more packages.
     * 
     * @param packageWeight Weight of the package to be loaded
     * @return true if the truck has sufficient available capacity
     */
    public boolean canAcceptPackage(Integer packageWeight) {
        return getAvailableCapacity() >= packageWeight;
    }

    /**
     * Checks if the truck is currently at full capacity.
     * 
     * @return true if the truck cannot accept any more packages
     */
    public boolean isAtCapacity() {
        return currentLoad >= capacity;
    }

    /**
     * Checks if the truck is empty (no packages loaded).
     * 
     * @return true if the truck has no current load
     */
    public boolean isEmpty() {
        return currentLoad == 0;
    }

    /**
     * Checks if the truck has a target destination set.
     * 
     * @return true if both target coordinates are set
     */
    public boolean hasTarget() {
        return targetX != null && targetY != null;
    }

    /**
     * Calculates the Euclidean distance to the target destination.
     * 
     * @return Distance to target in world units, or 0 if no target is set
     */
    public double getDistanceToTarget() {
        if (!hasTarget()) {
            return 0.0;
        }
        int deltaX = currentX - targetX;
        int deltaY = currentY - targetY;
        return Math.sqrt(deltaX * deltaX + deltaY * deltaY);
    }

    /**
     * Calculates the distance to a specific point.
     * 
     * @param x X coordinate of the point
     * @param y Y coordinate of the point
     * @return Euclidean distance to the point
     */
    public double getDistanceTo(Integer x, Integer y) {
        int deltaX = currentX - x;
        int deltaY = currentY - y;
        return Math.sqrt(deltaX * deltaX + deltaY * deltaY);
    }

    /**
     * Checks if the truck has arrived at its target destination.
     * Uses a small threshold to account for floating-point precision.
     * 
     * @return true if the truck is at the target location
     */
    public boolean hasArrivedAtTarget() {
        if (!hasTarget()) {
            return false;
        }
        return getDistanceToTarget() < 1.0; // Within 1 unit of target
    }

    /**
     * Checks if the truck is currently at the assigned warehouse location.
     * 
     * @return true if the truck is at its assigned warehouse
     */
    public boolean isAtAssignedWarehouse() {
        if (assignedWarehouse == null) {
            return false;
        }
        return getDistanceTo(assignedWarehouse.getXCoordinate(), assignedWarehouse.getYCoordinate()) < 1.0;
    }

    /**
     * Sets the target destination for the truck.
     * 
     * @param x Target X coordinate
     * @param y Target Y coordinate
     */
    public void setTarget(Integer x, Integer y) {
        this.targetX = x;
        this.targetY = y;
        updateEstimatedArrivalTime();
    }

    /**
     * Clears the current target destination.
     */
    public void clearTarget() {
        this.targetX = null;
        this.targetY = null;
        this.estimatedArrivalTime = null;
    }

    /**
     * Updates the truck's position towards the target.
     * This method is called during simulation ticks.
     * 
     * @param deltaTime Time elapsed since last update in seconds
     */
    public void updatePosition(double deltaTime) {
        if (!hasTarget() || !status.isMoving()) {
            return;
        }

        double distance = getDistanceToTarget();
        if (distance <= 1.0) {
            // Arrived at target
            currentX = targetX;
            currentY = targetY;
            clearTarget();
            return;
        }

        // Calculate movement vector
        double moveDistance = speed * deltaTime;
        if (moveDistance >= distance) {
            // Will arrive at target this tick
            currentX = targetX;
            currentY = targetY;
            clearTarget();
        } else {
            // Move towards target
            double ratio = moveDistance / distance;
            int deltaX = (int) ((targetX - currentX) * ratio);
            int deltaY = (int) ((targetY - currentY) * ratio);
            currentX += deltaX;
            currentY += deltaY;
        }

        lastPositionUpdate = LocalDateTime.now();
    }

    /**
     * Increments the sequence number for message ordering.
     * 
     * @return The new sequence number
     */
    public Long incrementSequenceNumber() {
        return ++sequenceNumber;
    }

    /**
     * Updates the estimated arrival time based on current position and speed.
     */
    private void updateEstimatedArrivalTime() {
        if (!hasTarget()) {
            estimatedArrivalTime = null;
            return;
        }

        double distance = getDistanceToTarget();
        double travelTime = distance / speed; // Time in seconds
        estimatedArrivalTime = LocalDateTime.now().plusSeconds((long) travelTime);
    }

    /**
     * Gets the number of packages currently loaded on the truck.
     * 
     * @return Count of packages loaded
     */
    public int getPackageCount() {
        return packages.size();
    }

    @Override
    public String toString() {
        return String.format("Truck{id=%d, truckId='%s', status=%s, position=(%d,%d), " +
                "target=(%s,%s), load=%d/%d, packages=%d}",
                getId(), truckId, status, currentX, currentY,
                targetX, targetY, currentLoad, capacity, packages.size());
    }
}