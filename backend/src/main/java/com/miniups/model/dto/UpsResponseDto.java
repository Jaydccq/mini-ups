/**
 * UPS Response Message Data Transfer Object
 * 
 * Functionality:
 * - Defines standard response format that UPS returns to Amazon system
 * - Supports unified handling of success and failure responses
 * - Provides status code and error message management
 * 
 * Response Types:
 * - Acknowledgement: Confirmation message
 * - Error: Error response
 * - TruckDispatched: Truck dispatch notification
 * - TruckArrived: Truck arrival notification
 * - ShipmentDelivered: Package delivery notification
 * 
 *
 
 */
package com.miniups.model.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

public class UpsResponseDto {
    
    private String messageType;
    private LocalDateTime timestamp;
    private Map<String, Object> payload;
    
    // Constructors
    public UpsResponseDto() {
        this.timestamp = LocalDateTime.now();
        this.payload = new HashMap<>();
    }
    
    public UpsResponseDto(String messageType) {
        this();
        this.messageType = messageType;
    }
    
    public UpsResponseDto(String messageType, Map<String, Object> payload) {
        this(messageType);
        this.payload = payload != null ? payload : new HashMap<>();
    }
    
    // Static factory methods for common responses
    public static UpsResponseDto success(String message) {
        UpsResponseDto response = new UpsResponseDto("Acknowledgement");
        response.addPayload("status", "success");
        response.addPayload("code", 200);
        response.addPayload("message", message != null ? message : "Operation completed successfully");
        return response;
    }
    
    public static UpsResponseDto success() {
        return success(null);
    }
    
    public static UpsResponseDto error(int code, String message) {
        UpsResponseDto response = new UpsResponseDto("Error");
        response.addPayload("status", "fail");
        response.addPayload("code", code);
        response.addPayload("message", message != null ? message : "An error occurred");
        return response;
    }
    
    public static UpsResponseDto error(String message) {
        return error(500, message);
    }
    
    public static UpsResponseDto truckDispatched(String truckId, String shipmentId) {
        UpsResponseDto response = new UpsResponseDto("TruckDispatched");
        
        // Convert string values to integers for Amazon compatibility
        try {
            response.addPayload("truck_id", Integer.parseInt(truckId));
        } catch (NumberFormatException e) {
            response.addPayload("truck_id", truckId);
        }
        
        try {
            response.addPayload("shipment_id", Integer.parseInt(shipmentId));
        } catch (NumberFormatException e) {
            response.addPayload("shipment_id", shipmentId);
        }
        
        return response;
    }
    
    public static UpsResponseDto truckArrived(String truckId, String warehouseId, String shipmentId) {
        UpsResponseDto response = new UpsResponseDto("TruckArrived");
        
        // Convert string values to integers for Amazon compatibility
        try {
            response.addPayload("truck_id", Integer.parseInt(truckId));
        } catch (NumberFormatException e) {
            response.addPayload("truck_id", truckId); // fallback to string if not a number
        }
        
        try {
            response.addPayload("warehouse_id", Integer.parseInt(warehouseId));
        } catch (NumberFormatException e) {
            response.addPayload("warehouse_id", warehouseId); // fallback to string if not a number
        }
        
        try {
            response.addPayload("shipment_id", Integer.parseInt(shipmentId));
        } catch (NumberFormatException e) {
            response.addPayload("shipment_id", shipmentId); // fallback to string if not a number
        }
        
        return response;
    }
    
    public static UpsResponseDto shipmentDelivered(String shipmentId) {
        UpsResponseDto response = new UpsResponseDto("ShipmentDelivered");
        
        // Convert string value to integer for Amazon compatibility
        try {
            response.addPayload("shipment_id", Integer.parseInt(shipmentId));
        } catch (NumberFormatException e) {
            response.addPayload("shipment_id", shipmentId);
        }
        
        return response;
    }
    
    // Getters and Setters
    @JsonProperty("message_type")
    public String getMessageType() {
        return messageType;
    }
    
    public void setMessageType(String messageType) {
        this.messageType = messageType;
    }
    
    @JsonProperty("timestamp")
    public String getTimestampString() {
        return timestamp.atOffset(ZoneOffset.UTC).format(DateTimeFormatter.ISO_INSTANT);
    }
    
    @JsonIgnore
    public LocalDateTime getTimestamp() {
        return timestamp;
    }
    
    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }
    
    public Map<String, Object> getPayload() {
        return payload;
    }
    
    public void setPayload(Map<String, Object> payload) {
        this.payload = payload != null ? payload : new HashMap<>();
    }
    
    // Utility methods
    public UpsResponseDto addPayload(String key, Object value) {
        this.payload.put(key, value);
        return this;
    }
    
    @Override
    public String toString() {
        return "UpsResponseDto{" +
               "messageType='" + messageType + '\'' +
               ", timestamp=" + timestamp +
               ", payload=" + payload +
               '}';
    }
    
    @JsonIgnore
    public boolean isSuccess() {
        return "success".equals(payload.get("status"));
    }
    
    @JsonIgnore
    public boolean isError() {
        return "Error".equals(messageType) || "fail".equals(payload.get("status"));
    }
    
    @JsonIgnore
    public int getCode() {
        Object code = payload.get("code");
        if (code instanceof Number) {
            return ((Number) code).intValue();
        }
        return 500;
    }
    
    @JsonIgnore
    public String getMessage() {
        return (String) payload.get("message");
    }
}