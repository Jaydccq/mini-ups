/**
 * TruckLocationHistory.java
 * 
 * Entity class representing the location and status history tracking for trucks in the Mini-UPS system.
 * This class maintains a complete movement trail for delivery trucks, enabling route optimization,
 * fleet management, and real-time tracking capabilities throughout the delivery network.
 * 
 * Purpose:
 * - Track truck movements and location changes over time
 * - Maintain status history for fleet management
 * - Enable route analysis and optimization
 * - Support real-time truck tracking and monitoring
 * - Provide audit trail for delivery operations
 * - Enable performance analytics for delivery efficiency
 * 
 * Main Fields:
 * - xCoordinate/yCoordinate: Current truck location in world simulator coordinates
 * - status: Current truck status from TruckStatus enum (IDLE, EN_ROUTE, DELIVERING, etc.)
 * - timestamp: When the location/status was recorded
 * - notes: Additional information about the location or status change
 * - truck: Reference to the associated truck
 * 
 * Database Design:
 * - Table: truck_location_history
 * - Primary Key: id (inherited from BaseEntity)
 * - Foreign Key: truck_id references trucks(id)
 * - Indexes:
 *   - idx_truck_location_truck on truck_id for efficient truck queries
 *   - idx_truck_location_timestamp on timestamp for chronological tracking
 * 
 * Business Logic:
 * - Automatically sets timestamp on creation
 * - Immutable once created to maintain movement integrity
 * - Used by world simulator integration for truck coordination
 * - Enables route reconstruction and analysis
 * - Supports real-time truck location services
 * - Used for calculating delivery ETAs and route optimization
 * 
 * Integration:
 * - Updated by world simulator when truck positions change
 * - Used by route planning algorithms
 * - Consumed by customer tracking interfaces
 * - Analyzed for fleet performance metrics
 * 
 * Usage Examples:
 * - Recording truck departure from warehouse
 * - Tracking truck movement during delivery routes
 * - Recording arrival at delivery locations
 * - Monitoring truck idle time and efficiency
 * - Analyzing route performance and optimization opportunities
 * 
 * @author Mini-UPS Development Team
 * @version 1.0
 * @since 2024-01-01
 */
package com.miniups.model.entity;

import com.miniups.model.enums.TruckStatus;
import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "truck_location_history", indexes = {
    @Index(name = "idx_truck_location_truck", columnList = "truck_id"),
    @Index(name = "idx_truck_location_timestamp", columnList = "timestamp")
})
public class TruckLocationHistory extends BaseEntity {
    
    @Column(name = "x_coordinate", nullable = false)
    private Integer xCoordinate;
    
    @Column(name = "y_coordinate", nullable = false)
    private Integer yCoordinate;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private TruckStatus status;
    
    @Column(name = "timestamp", nullable = false)
    private LocalDateTime timestamp;
    
    @Column(name = "notes", length = 255)
    private String notes;
    
    // Relationships
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "truck_id", nullable = false)
    private Truck truck;
    
    // Constructors
    public TruckLocationHistory() {
        this.timestamp = LocalDateTime.now();
    }
    
    public TruckLocationHistory(Truck truck, Integer x, Integer y, TruckStatus status) {
        this();
        this.truck = truck;
        this.xCoordinate = x;
        this.yCoordinate = y;
        this.status = status;
    }
    
    // Getters and Setters
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
    
    public TruckStatus getStatus() {
        return status;
    }
    
    public void setStatus(TruckStatus status) {
        this.status = status;
    }
    
    public LocalDateTime getTimestamp() {
        return timestamp;
    }
    
    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }
    
    public String getNotes() {
        return notes;
    }
    
    public void setNotes(String notes) {
        this.notes = notes;
    }
    
    public Truck getTruck() {
        return truck;
    }
    
    public void setTruck(Truck truck) {
        this.truck = truck;
    }
}