/**
 * Truck Entity
 * 
 * Description:
 * - Manages basic information and real-time status of UPS trucks
 * - Records truck location, load capacity, current tasks, etc.
 * - Establishes one-to-many relationships with shipments (a truck can handle multiple orders)
 * 
 * Key Fields:
 * - truckId: Unique identifier of the truck in the world simulator
 * - status: Truck status (IDLE/BUSY/MAINTENANCE/OUT_OF_SERVICE)
 * - currentX/currentY: Current location coordinates
 * - capacity: Load capacity
 * - currentLoad: Current load
 * 
 * Database Design:
 * - Table: trucks
 * - Indexes: truck_id, status (for fast lookup of available trucks)
 * - Foreign key relations: shipments (currently assigned transport tasks)
 * 
 * Business Logic:
 * - Truck scheduling algorithm assigns tasks based on distance and load
 * - Real-time location updates for tracking and route optimization
 * - Supports multi-package delivery route planning
 * 
 *
 
 */
package com.miniups.model.entity;

import com.miniups.model.enums.TruckStatus;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.util.ArrayList;
import java.util.List;

public class Truck extends BaseEntity {
    
    @NotNull
    private Integer truckId;
    
    private TruckStatus status = TruckStatus.IDLE;
    
    @Min(0)
    private Integer currentX = 0;
    
    @Min(0)
    private Integer currentY = 0;
    
    @Min(1)
    private Integer capacity = 100;
    
    // Bidirectional one-to-one relationship with Driver
    private Driver driver;
    
    private Long worldId;
    
    private String licensePlate;
    
    private Double currentLoad = 0.0;
    
    // Relationships
    private List<Shipment> shipments = new ArrayList<>();
    
    private List<TruckLocationHistory> locationHistory = new ArrayList<>();
    
    // Constructors
    public Truck() {}
    
    public Truck(Integer truckId, Integer capacity) {
        this.truckId = truckId;
        this.capacity = capacity;
    }
    
    // Getters and Setters
    public Integer getTruckId() {
        return truckId;
    }
    
    public void setTruckId(Integer truckId) {
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
    
    public Integer getCapacity() {
        return capacity;
    }
    
    public void setCapacity(Integer capacity) {
        this.capacity = capacity;
    }
    
    public Driver getDriver() {
        return driver;
    }
    
    public void setDriver(Driver driver) {
        this.driver = driver;
    }
    
    public Long getWorldId() {
        return worldId;
    }
    
    public void setWorldId(Long worldId) {
        this.worldId = worldId;
    }
    
    public List<Shipment> getShipments() {
        return shipments;
    }
    
    public void setShipments(List<Shipment> shipments) {
        this.shipments = shipments;
    }
    
    public List<TruckLocationHistory> getLocationHistory() {
        return locationHistory;
    }
    
    public void setLocationHistory(List<TruckLocationHistory> locationHistory) {
        this.locationHistory = locationHistory;
    }
    
    public String getLicensePlate() {
        return licensePlate;
    }
    
    public void setLicensePlate(String licensePlate) {
        this.licensePlate = licensePlate;
    }
    
    public Double getCurrentLoad() {
        return currentLoad;
    }
    
    public void setCurrentLoad(Double currentLoad) {
        this.currentLoad = currentLoad;
    }
    
    // Helper methods
    public boolean isAvailable() {
        return status == TruckStatus.IDLE;
    }
    
    public void updateLocation(Integer x, Integer y) {
        this.currentX = x;
        this.currentY = y;
    }
    
    public boolean hasDriver() {
        return driver != null;
    }
    
    public void assignDriver(Driver driver) {
        if (driver != null && driver.isAvailableForAssignment()) {
            this.driver = driver;
            driver.assignToTruck(this);
        }
    }
    
    public void unassignDriver() {
        if (this.driver != null) {
            this.driver.unassignFromTruck();
            this.driver = null;
        }
    }
    
    public String getDriverName() {
        return driver != null ? driver.getName() : null;
    }
}
