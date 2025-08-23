package com.miniups.worldsim.model.entity;

import java.util.HashSet;
import java.util.Set;

import com.miniups.worldsim.model.enums.WarehouseStatus;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Entity representing a warehouse in the World Simulator.
 * 
 * A warehouse is a physical location where packages are stored and from which
 * trucks are dispatched for deliveries. Each warehouse has a specific location
 * (x, y coordinates), capacity, and operational status. The warehouse manages
 * its inventory of packages and coordinates with trucks for pickup and delivery
 * operations.
 * 
 * Key responsibilities:
 * - Store packages awaiting delivery
 * - Dispatch trucks for package delivery
 * - Track inventory levels and capacity
 * - Provide location coordinates for route planning
 * - Maintain operational status for availability
 * 
 * @author World Simulator Team
 * @version 1.0.0
 * @since 2025-01-13
 */
@Entity
@Table(name = "warehouses", uniqueConstraints = {
    @UniqueConstraint(columnNames = "warehouse_id", name = "uk_warehouses_warehouse_id")
})
public class Warehouse extends BaseEntity {

    @NotBlank(message = "Warehouse ID is required")
    @Size(max = 50, message = "Warehouse ID must not exceed 50 characters")
    @Column(name = "warehouse_id", nullable = false, unique = true, length = 50)
    private String warehouseId;

    @NotBlank(message = "Warehouse name is required")
    @Size(max = 100, message = "Warehouse name must not exceed 100 characters")
    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @NotNull(message = "X coordinate is required")
    @Column(name = "x_coordinate", nullable = false)
    private Integer xCoordinate;

    @NotNull(message = "Y coordinate is required")
    @Column(name = "y_coordinate", nullable = false)
    private Integer yCoordinate;

    @Min(value = 1, message = "Capacity must be positive")
    @Column(name = "capacity", nullable = false)
    private Integer capacity = 1000;

    @Min(value = 0, message = "Current inventory cannot be negative")
    @Column(name = "current_inventory", nullable = false)
    private Integer currentInventory = 0;

    @NotNull(message = "Warehouse status is required")
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private WarehouseStatus status = WarehouseStatus.ACTIVE;

    @OneToMany(mappedBy = "currentWarehouse", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private Set<Package> packages = new HashSet<>();

    @OneToMany(mappedBy = "assignedWarehouse", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private Set<Truck> assignedTrucks = new HashSet<>();

    /**
     * Default constructor for JPA.
     */
    protected Warehouse() {
        super();
    }

    /**
     * Constructor for creating a new warehouse.
     * 
     * @param warehouseId Unique identifier for the warehouse
     * @param name Display name of the warehouse
     * @param xCoordinate X coordinate in the world space
     * @param yCoordinate Y coordinate in the world space
     * @param capacity Maximum package capacity of the warehouse
     */
    public Warehouse(String warehouseId, String name, Integer xCoordinate, Integer yCoordinate, Integer capacity) {
        this();
        this.warehouseId = warehouseId;
        this.name = name;
        this.xCoordinate = xCoordinate;
        this.yCoordinate = yCoordinate;
        this.capacity = capacity;
    }

    // Getters and Setters

    public String getWarehouseId() {
        return warehouseId;
    }

    public void setWarehouseId(String warehouseId) {
        this.warehouseId = warehouseId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Integer getXCoordinate() {
        return xCoordinate;
    }

    public void setXCoordinate(Integer xCoordinate) {
        this.xCoordinate = xCoordinate;
    }

    public Integer getYCoordinate() {
        return yCoordinate;
    }

    public void setYCoordinate(Integer yCoordinate) {
        this.yCoordinate = yCoordinate;
    }

    public Integer getCapacity() {
        return capacity;
    }

    public void setCapacity(Integer capacity) {
        this.capacity = capacity;
    }

    public Integer getCurrentInventory() {
        return currentInventory;
    }

    public void setCurrentInventory(Integer currentInventory) {
        this.currentInventory = currentInventory;
    }

    public WarehouseStatus getStatus() {
        return status;
    }

    public void setStatus(WarehouseStatus status) {
        this.status = status;
    }

    public Set<Package> getPackages() {
        return packages;
    }

    public void setPackages(Set<Package> packages) {
        this.packages = packages;
    }

    public Set<Truck> getAssignedTrucks() {
        return assignedTrucks;
    }

    public void setAssignedTrucks(Set<Truck> assignedTrucks) {
        this.assignedTrucks = assignedTrucks;
    }

    // Business Logic Methods

    /**
     * Calculates the available capacity in the warehouse.
     * 
     * @return Number of packages that can still be stored
     */
    public Integer getAvailableCapacity() {
        return capacity - currentInventory;
    }

    /**
     * Calculates the capacity utilization percentage.
     * 
     * @return Percentage of capacity currently in use (0-100)
     */
    public Double getCapacityUtilization() {
        if (capacity == 0) {
            return 0.0;
        }
        return (double) currentInventory / capacity * 100.0;
    }

    /**
     * Checks if the warehouse can accept new packages.
     * 
     * @return true if the warehouse has available capacity and is operational
     */
    public boolean canAcceptPackages() {
        return status.canReceivePackages() && getAvailableCapacity() > 0;
    }

    /**
     * Checks if the warehouse can dispatch trucks.
     * 
     * @return true if the warehouse is operational and can dispatch trucks
     */
    public boolean canDispatchTrucks() {
        return status.canDispatchTrucks() && currentInventory > 0;
    }

    /**
     * Checks if the warehouse is at full capacity.
     * 
     * @return true if the warehouse cannot accept any more packages
     */
    public boolean isAtCapacity() {
        return currentInventory >= capacity;
    }

    /**
     * Checks if the warehouse is empty.
     * 
     * @return true if the warehouse has no packages in inventory
     */
    public boolean isEmpty() {
        return currentInventory == 0;
    }

    /**
     * Calculates the Euclidean distance to another point.
     * 
     * @param targetX X coordinate of the target point
     * @param targetY Y coordinate of the target point
     * @return Distance in world units
     */
    public double getDistanceTo(Integer targetX, Integer targetY) {
        int deltaX = this.xCoordinate - targetX;
        int deltaY = this.yCoordinate - targetY;
        return Math.sqrt(deltaX * deltaX + deltaY * deltaY);
    }

    /**
     * Calculates the Manhattan distance to another point.
     * 
     * @param targetX X coordinate of the target point
     * @param targetY Y coordinate of the target point
     * @return Manhattan distance in world units
     */
    public int getManhattanDistanceTo(Integer targetX, Integer targetY) {
        return Math.abs(this.xCoordinate - targetX) + Math.abs(this.yCoordinate - targetY);
    }

    /**
     * Gets the number of trucks currently assigned to this warehouse.
     * 
     * @return Count of assigned trucks
     */
    public int getAssignedTruckCount() {
        return assignedTrucks.size();
    }

    /**
     * Gets the number of packages currently stored at this warehouse.
     * 
     * @return Count of packages in inventory
     */
    public int getPackageCount() {
        return packages.size();
    }

    @Override
    public String toString() {
        return String.format("Warehouse{id=%d, warehouseId='%s', name='%s', location=(%d,%d), " +
                "capacity=%d, inventory=%d, status=%s}",
                getId(), warehouseId, name, xCoordinate, yCoordinate,
                capacity, currentInventory, status);
    }
}