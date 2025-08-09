/**
 * World Simulator Debug Message DTO
 * 
 * Purpose:
 * - Represents a captured Protocol Buffer message for debugging
 * - Provides structured data for real-time debugging interface
 * - Converts binary protobuf messages to human-readable JSON format
 * 
 * Features:
 * - Direction tracking (INBOUND/OUTBOUND) for message flow visualization
 * - Timestamp with millisecond precision for accurate timeline
 * - Message type classification for filtering and categorization
 * - JSON conversion of protobuf content for easy reading
 * - Size tracking for performance monitoring
 * - Sequence number correlation for request/response matching
 * 
 * Usage:
 * - Created by WorldSimulatorDebugAspect when messages are sent/received
 * - Serialized to JSON for WebSocket transmission to admin clients
 * - Displayed in real-time debugging interface for troubleshooting
 * - Filtered and searched by message type, direction, and content
 * 
 * @author Mini-UPS Team
 * @version 1.0.0
 */
package com.miniups.model.dto.debug;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;

public class WorldSimulatorDebugMessageDto {
    
    public enum Direction {
        INBOUND,  // Messages received from World Simulator
        OUTBOUND  // Messages sent to World Simulator
    }
    
    @JsonProperty("direction")
    private Direction direction;
    
    @JsonProperty("timestamp")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss.SSS")
    private LocalDateTime timestamp;
    
    @JsonProperty("messageType")
    private String messageType;
    
    @JsonProperty("jsonContent")
    private String jsonContent;
    
    @JsonProperty("sizeBytes")
    private int sizeBytes;
    
    @JsonProperty("sequenceNumber")
    private Long sequenceNumber;
    
    @JsonProperty("summary")
    private String summary;
    
    // Constructors
    
    public WorldSimulatorDebugMessageDto() {
        this.timestamp = LocalDateTime.now();
    }
    
    public WorldSimulatorDebugMessageDto(Direction direction, String messageType, 
                                        String jsonContent, int sizeBytes) {
        this();
        this.direction = direction;
        this.messageType = messageType;
        this.jsonContent = jsonContent;
        this.sizeBytes = sizeBytes;
        this.summary = generateSummary();
    }
    
    public WorldSimulatorDebugMessageDto(Direction direction, String messageType, 
                                        String jsonContent, int sizeBytes, Long sequenceNumber) {
        this(direction, messageType, jsonContent, sizeBytes);
        this.sequenceNumber = sequenceNumber;
    }
    
    // Getters and Setters
    
    public Direction getDirection() {
        return direction;
    }
    
    public void setDirection(Direction direction) {
        this.direction = direction;
    }
    
    public LocalDateTime getTimestamp() {
        return timestamp;
    }
    
    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }
    
    public String getMessageType() {
        return messageType;
    }
    
    public void setMessageType(String messageType) {
        this.messageType = messageType;
        this.summary = generateSummary();
    }
    
    public String getJsonContent() {
        return jsonContent;
    }
    
    public void setJsonContent(String jsonContent) {
        this.jsonContent = jsonContent;
        this.summary = generateSummary();
    }
    
    public int getSizeBytes() {
        return sizeBytes;
    }
    
    public void setSizeBytes(int sizeBytes) {
        this.sizeBytes = sizeBytes;
    }
    
    public Long getSequenceNumber() {
        return sequenceNumber;
    }
    
    public void setSequenceNumber(Long sequenceNumber) {
        this.sequenceNumber = sequenceNumber;
    }
    
    public String getSummary() {
        return summary;
    }
    
    public void setSummary(String summary) {
        this.summary = summary;
    }
    
    // Helper Methods
    
    /**
     * Generate a human-readable summary of the message content
     */
    private String generateSummary() {
        if (messageType == null) {
            return "Unknown message";
        }
        
        StringBuilder sb = new StringBuilder();
        sb.append(messageType.replace("WorldUpsProto.", ""));
        
        if (sequenceNumber != null) {
            sb.append(" (seq: ").append(sequenceNumber).append(")");
        }
        
        // Add specific details based on message type
        if (jsonContent != null && !jsonContent.isEmpty()) {
            if (messageType.contains("UGoPickup") && jsonContent.contains("whid")) {
                String whid = extractJsonField("whid");
                String truckid = extractJsonField("truckid");
                if (whid != null && truckid != null) {
                    sb.append(" - Truck ").append(truckid).append(" → Warehouse ").append(whid);
                }
            } else if (messageType.contains("UGoDeliver") && jsonContent.contains("packages")) {
                String truckid = extractJsonField("truckid");
                int packageCount = countJsonArrayItems("packages");
                if (truckid != null) {
                    sb.append(" - Truck ").append(truckid).append(" delivering ").append(packageCount).append(" packages");
                }
            } else if (messageType.contains("UFinished")) {
                String truckid = extractJsonField("truckid");
                String status = extractJsonField("status");
                if (truckid != null && status != null) {
                    sb.append(" - Truck ").append(truckid).append(" ").append(status);
                }
            } else if (messageType.contains("UDeliveryMade")) {
                String packageid = extractJsonField("packageid");
                String truckid = extractJsonField("truckid");
                if (packageid != null && truckid != null) {
                    sb.append(" - Package ").append(packageid).append(" delivered by Truck ").append(truckid);
                }
            } else if (messageType.contains("UTruck")) {
                String truckid = extractJsonField("truckid");
                String status = extractJsonField("status");
                String x = extractJsonField("x");
                String y = extractJsonField("y");
                if (truckid != null) {
                    sb.append(" - Truck ").append(truckid);
                    if (status != null) {
                        sb.append(" (").append(status).append(")");
                    }
                    if (x != null && y != null) {
                        sb.append(" at (").append(x).append(",").append(y).append(")");
                    }
                }
            } else if (messageType.contains("UErr")) {
                String error = extractJsonField("err");
                if (error != null) {
                    sb.append(" - Error: ").append(error);
                }
            }
        }
        
        return sb.toString();
    }
    
    /**
     * Extract a field value from JSON content (simple regex-based extraction)
     */
    private String extractJsonField(String fieldName) {
        if (jsonContent == null) {
            return null;
        }
        
        String pattern = "\"" + fieldName + "\"\\s*:\\s*([\"']?[^,}\\]\"']*[\"']?)";
        java.util.regex.Pattern regex = java.util.regex.Pattern.compile(pattern);
        java.util.regex.Matcher matcher = regex.matcher(jsonContent);
        
        if (matcher.find()) {
            String value = matcher.group(1);
            // Remove quotes if present
            if (value.startsWith("\"") && value.endsWith("\"")) {
                value = value.substring(1, value.length() - 1);
            }
            return value;
        }
        
        return null;
    }
    
    /**
     * Count items in a JSON array field
     */
    private int countJsonArrayItems(String arrayFieldName) {
        if (jsonContent == null) {
            return 0;
        }
        
        String pattern = "\"" + arrayFieldName + "\"\\s*:\\s*\\[(.*?)\\]";
        java.util.regex.Pattern regex = java.util.regex.Pattern.compile(pattern, java.util.regex.Pattern.DOTALL);
        java.util.regex.Matcher matcher = regex.matcher(jsonContent);
        
        if (matcher.find()) {
            String arrayContent = matcher.group(1).trim();
            if (arrayContent.isEmpty()) {
                return 0;
            }
            // Simple count by number of commas + 1, accounting for nested objects
            int braceLevel = 0;
            int count = 1;
            for (char c : arrayContent.toCharArray()) {
                if (c == '{') braceLevel++;
                else if (c == '}') braceLevel--;
                else if (c == ',' && braceLevel == 0) count++;
            }
            return count;
        }
        
        return 0;
    }
    
    @Override
    public String toString() {
        return String.format("WorldSimDebugMsg[%s %s - %s (%d bytes)]", 
                           direction, timestamp, summary, sizeBytes);
    }
}