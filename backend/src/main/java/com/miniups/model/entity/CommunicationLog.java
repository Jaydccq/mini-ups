/**
 * Communication Log Entity
 * 
 * Functionality:
 * - Records all communication between UPS and Amazon systems
 * - Stores request/response pairs for debugging purposes
 * - Tracks message types, timestamps, and payload data
 * 
 * Debug Features:
 * - Incoming messages from Amazon (ShipmentCreated, etc.)
 * - Outgoing notifications to Amazon (TruckArrived, ShipmentDelivered, etc.)
 * - Response status and error tracking
 * - Performance timing information
 * 
 * @author Mini-UPS Team  
 * @version 1.0.0
 */
package com.miniups.model.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "communication_logs")
public class CommunicationLog extends BaseEntity {
    
    @Column(name = "direction", nullable = false, length = 20)
    private String direction; // "INCOMING" or "OUTGOING"
    
    @Column(name = "message_type", nullable = false, length = 50)
    private String messageType;
    
    @Column(name = "endpoint", length = 200)
    private String endpoint;
    
    @Column(name = "payload", columnDefinition = "TEXT")
    private String payload;
    
    @Column(name = "response", columnDefinition = "TEXT")
    private String response;
    
    @Column(name = "status_code")
    private Integer statusCode;
    
    @Column(name = "processing_time_ms")
    private Long processingTimeMs;
    
    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;
    
    @Column(name = "success")
    private Boolean success;
    
    @Column(name = "shipment_id", length = 50)
    private String shipmentId;
    
    @Column(name = "truck_id")
    private Integer truckId;
    
    @Column(name = "warehouse_id", length = 50)
    private String warehouseId;
    
    // Constructors
    public CommunicationLog() {
        this.success = true;
        this.statusCode = 200;
    }
    
    public CommunicationLog(String direction, String messageType, String endpoint) {
        this();
        this.direction = direction;
        this.messageType = messageType;
        this.endpoint = endpoint;
    }
    
    // Static factory methods
    public static CommunicationLog incoming(String messageType, String endpoint) {
        return new CommunicationLog("INCOMING", messageType, endpoint);
    }
    
    public static CommunicationLog outgoing(String messageType, String endpoint) {
        return new CommunicationLog("OUTGOING", messageType, endpoint);
    }
    
    // Getters and Setters
    public String getDirection() {
        return direction;
    }
    
    public void setDirection(String direction) {
        this.direction = direction;
    }
    
    public String getMessageType() {
        return messageType;
    }
    
    public void setMessageType(String messageType) {
        this.messageType = messageType;
    }
    
    public String getEndpoint() {
        return endpoint;
    }
    
    public void setEndpoint(String endpoint) {
        this.endpoint = endpoint;
    }
    
    public String getPayload() {
        return payload;
    }
    
    public void setPayload(String payload) {
        this.payload = payload;
    }
    
    public String getResponse() {
        return response;
    }
    
    public void setResponse(String response) {
        this.response = response;
    }
    
    public Integer getStatusCode() {
        return statusCode;
    }
    
    public void setStatusCode(Integer statusCode) {
        this.statusCode = statusCode;
    }
    
    public Long getProcessingTimeMs() {
        return processingTimeMs;
    }
    
    public void setProcessingTimeMs(Long processingTimeMs) {
        this.processingTimeMs = processingTimeMs;
    }
    
    public String getErrorMessage() {
        return errorMessage;
    }
    
    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }
    
    public Boolean getSuccess() {
        return success;
    }
    
    public void setSuccess(Boolean success) {
        this.success = success;
    }
    
    public String getShipmentId() {
        return shipmentId;
    }
    
    public void setShipmentId(String shipmentId) {
        this.shipmentId = shipmentId;
    }
    
    public Integer getTruckId() {
        return truckId;
    }
    
    public void setTruckId(Integer truckId) {
        this.truckId = truckId;
    }
    
    public String getWarehouseId() {
        return warehouseId;
    }
    
    public void setWarehouseId(String warehouseId) {
        this.warehouseId = warehouseId;
    }
    
    // Utility methods
    public void markAsError(String errorMessage) {
        this.success = false;
        this.errorMessage = errorMessage;
        this.statusCode = 500;
    }
    
    public void markAsError(int statusCode, String errorMessage) {
        this.success = false;
        this.errorMessage = errorMessage;
        this.statusCode = statusCode;
    }
    
    public boolean isIncoming() {
        return "INCOMING".equals(direction);
    }
    
    public boolean isOutgoing() {
        return "OUTGOING".equals(direction);
    }
}