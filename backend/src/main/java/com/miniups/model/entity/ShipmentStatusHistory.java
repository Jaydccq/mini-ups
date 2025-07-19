/**
 * ShipmentStatusHistory.java
 * 
 * Entity class representing the status history tracking for shipments in the Mini-UPS system.
 * This class maintains a complete audit trail of all status changes for each shipment,
 * enabling package tracking and delivery monitoring throughout the shipment lifecycle.
 * 
 * Purpose:
 * - Track status changes for shipments with timestamps
 * - Maintain location history when status changes occur
 * - Provide audit trail for shipment progress
 * - Enable customer tracking and delivery notifications
 * - Support shipment status analytics and reporting
 * 
 * Main Fields:
 * - status: Current status from ShipmentStatus enum (PENDING, PICKED_UP, EN_ROUTE, etc.)
 * - timestamp: When the status change occurred
 * - locationX/locationY: Coordinates where status change happened (optional)
 * - notes: Additional information about the status change
 * - shipment: Reference to the associated shipment
 * 
 * Database Design:
 * - Table: shipment_status_history
 * - Primary Key: id (inherited from BaseEntity)
 * - Foreign Key: shipment_id references shipments(id)
 * - Indexes: 
 *   - idx_status_history_shipment on shipment_id for efficient shipment queries
 *   - idx_status_history_timestamp on timestamp for chronological ordering
 * 
 * Business Logic:
 * - Automatically sets timestamp on creation
 * - Immutable once created to maintain audit integrity
 * - Used for tracking package progress and customer notifications
 * - Enables delivery status reporting and analytics
 * - Supports location-based tracking when coordinates are available
 * 
 * Usage Examples:
 * - Recording when package is picked up from warehouse
 * - Tracking package movement during transit
 * - Recording delivery completion with location
 * - Maintaining status history for customer inquiries
 * 
 * @author Mini-UPS Development Team
 * @version 1.0
 * @since 2024-01-01
 */
package com.miniups.model.entity;

import com.miniups.model.enums.ShipmentStatus;
import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "shipment_status_history", indexes = {
    @Index(name = "idx_status_history_shipment", columnList = "shipment_id"),
    @Index(name = "idx_status_history_timestamp", columnList = "timestamp")
})
public class ShipmentStatusHistory extends BaseEntity {
    
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private ShipmentStatus status;
    
    @Column(name = "timestamp", nullable = false)
    private LocalDateTime timestamp;
    
    @Column(name = "location_x")
    private Integer locationX;
    
    @Column(name = "location_y")
    private Integer locationY;
    
    @Column(name = "notes", length = 500)
    private String notes;
    
    // Relationships
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "shipment_id", nullable = false)
    private Shipment shipment;
    
    // Constructors
    public ShipmentStatusHistory() {
        this.timestamp = LocalDateTime.now();
    }
    
    public ShipmentStatusHistory(Shipment shipment, ShipmentStatus status) {
        this();
        this.shipment = shipment;
        this.status = status;
    }
    
    // Getters and Setters
    public ShipmentStatus getStatus() {
        return status;
    }
    
    public void setStatus(ShipmentStatus status) {
        this.status = status;
    }
    
    public LocalDateTime getTimestamp() {
        return timestamp;
    }
    
    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }
    
    public Integer getLocationX() {
        return locationX;
    }
    
    public void setLocationX(Integer locationX) {
        this.locationX = locationX;
    }
    
    public Integer getLocationY() {
        return locationY;
    }
    
    public void setLocationY(Integer locationY) {
        this.locationY = locationY;
    }
    
    public String getNotes() {
        return notes;
    }
    
    public void setNotes(String notes) {
        this.notes = notes;
    }
    
    public Shipment getShipment() {
        return shipment;
    }
    
    public void setShipment(Shipment shipment) {
        this.shipment = shipment;
    }
}