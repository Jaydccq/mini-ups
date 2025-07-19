/**
 * AddressChange.java
 * 
 * Entity class representing address change requests for shipments in the Mini-UPS system.
 * This class manages customer requests to modify delivery addresses for packages that
 * are already in transit, providing a controlled workflow for address modifications.
 * 
 * Purpose:
 * - Handle customer requests to change delivery addresses
 * - Maintain audit trail of address modification requests
 * - Provide approval workflow for address changes
 * - Track old and new coordinates for delivery location updates
 * - Enable customer service operations for delivery modifications
 * 
 * Main Fields:
 * - oldX/oldY: Original delivery coordinates
 * - newX/newY: Requested new delivery coordinates
 * - status: Current status (PENDING, APPROVED, REJECTED)
 * - requestedAt: When the change was requested
 * - processedAt: When the request was approved/rejected
 * - reason: Additional information or rejection reason
 * - shipment: Reference to the shipment being modified
 * - requestedBy: User who requested the change
 * 
 * Database Design:
 * - Table: address_changes
 * - Primary Key: id (inherited from BaseEntity)
 * - Foreign Keys: 
 *   - shipment_id references shipments(id)
 *   - requested_by references users(id)
 * - Indexes:
 *   - idx_address_change_shipment on shipment_id for shipment queries
 *   - idx_address_change_status on status for workflow management
 * 
 * Business Logic:
 * - Automatically sets requestedAt timestamp on creation
 * - Default status is PENDING for new requests
 * - Provides helper methods approve() and reject() for workflow
 * - Sets processedAt timestamp when status changes
 * - Validates coordinate changes before processing
 * - May trigger shipment rerouting when approved
 * 
 * Workflow:
 * 1. Customer requests address change via API
 * 2. System creates PENDING AddressChange record
 * 3. Customer service or automated system reviews request
 * 4. Request is APPROVED (updates shipment) or REJECTED (with reason)
 * 5. Customer is notified of decision
 * 
 * Usage Examples:
 * - Customer moves and needs package delivered to new address
 * - Incorrect address provided during order, needs correction
 * - Business address change for commercial deliveries
 * - Emergency delivery location changes
 * 
 * @author Mini-UPS Development Team
 * @version 1.0
 * @since 2024-01-01
 */
package com.miniups.model.entity;

import com.miniups.model.enums.AddressChangeStatus;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

@Entity
@Table(name = "address_changes", indexes = {
    @Index(name = "idx_address_change_shipment", columnList = "shipment_id"),
    @Index(name = "idx_address_change_status", columnList = "status")
})
public class AddressChange extends BaseEntity {
    
    @NotNull
    @Column(name = "old_x", nullable = false)
    private Integer oldX;
    
    @NotNull
    @Column(name = "old_y", nullable = false)
    private Integer oldY;
    
    @NotNull
    @Column(name = "new_x", nullable = false)
    private Integer newX;
    
    @NotNull
    @Column(name = "new_y", nullable = false)
    private Integer newY;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private AddressChangeStatus status = AddressChangeStatus.PENDING;
    
    @Column(name = "requested_at", nullable = false)
    private LocalDateTime requestedAt;
    
    @Column(name = "processed_at")
    private LocalDateTime processedAt;
    
    @Column(name = "reason", length = 500)
    private String reason;
    
    // Relationships
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "shipment_id", nullable = false)
    private Shipment shipment;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "requested_by")
    private User requestedBy;
    
    // Constructors
    public AddressChange() {
        this.requestedAt = LocalDateTime.now();
    }
    
    public AddressChange(Shipment shipment, Integer oldX, Integer oldY, 
                        Integer newX, Integer newY, User requestedBy) {
        this();
        this.shipment = shipment;
        this.oldX = oldX;
        this.oldY = oldY;
        this.newX = newX;
        this.newY = newY;
        this.requestedBy = requestedBy;
    }
    
    // Getters and Setters
    public Integer getOldX() {
        return oldX;
    }
    
    public void setOldX(Integer oldX) {
        this.oldX = oldX;
    }
    
    public Integer getOldY() {
        return oldY;
    }
    
    public void setOldY(Integer oldY) {
        this.oldY = oldY;
    }
    
    public Integer getNewX() {
        return newX;
    }
    
    public void setNewX(Integer newX) {
        this.newX = newX;
    }
    
    public Integer getNewY() {
        return newY;
    }
    
    public void setNewY(Integer newY) {
        this.newY = newY;
    }
    
    public AddressChangeStatus getStatus() {
        return status;
    }
    
    public void setStatus(AddressChangeStatus status) {
        this.status = status;
    }
    
    public LocalDateTime getRequestedAt() {
        return requestedAt;
    }
    
    public void setRequestedAt(LocalDateTime requestedAt) {
        this.requestedAt = requestedAt;
    }
    
    public LocalDateTime getProcessedAt() {
        return processedAt;
    }
    
    public void setProcessedAt(LocalDateTime processedAt) {
        this.processedAt = processedAt;
    }
    
    public String getReason() {
        return reason;
    }
    
    public void setReason(String reason) {
        this.reason = reason;
    }
    
    public Shipment getShipment() {
        return shipment;
    }
    
    public void setShipment(Shipment shipment) {
        this.shipment = shipment;
    }
    
    public User getRequestedBy() {
        return requestedBy;
    }
    
    public void setRequestedBy(User requestedBy) {
        this.requestedBy = requestedBy;
    }
    
    // Helper methods
    public void approve() {
        this.status = AddressChangeStatus.APPROVED;
        this.processedAt = LocalDateTime.now();
    }
    
    public void reject(String reason) {
        this.status = AddressChangeStatus.REJECTED;
        this.processedAt = LocalDateTime.now();
        this.reason = reason;
    }
}