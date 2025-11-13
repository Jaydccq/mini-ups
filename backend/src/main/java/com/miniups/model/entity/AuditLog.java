package com.miniups.model.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class AuditLog extends BaseEntity {

    private String eventId;

    private String correlationId;

    private Instant eventTime;

    private String sourceService;

    private String operationType;

    private String operationDescription;

    private String operationResult;

    private Instant operationTimestamp;

    private Long operationDurationMs;

    private Long userId;

    private String username;

    private String sessionId;

    private String ipAddress;

    private String userAgent;

    private String endpoint;

    private String httpMethod;

    private Long requestSize;

    private Long responseSize;

    private String entityId;

    private String entityType;

    private Integer resultCode;

    private String errorMessage;

    private String additionalData;

    private Instant auditCreatedAt;

    public boolean isSuccessful() {
        return "SUCCESS".equalsIgnoreCase(operationResult) && 
               (resultCode == null || (resultCode >= 200 && resultCode < 300));
    }

    public boolean isFailed() {
        return "FAILED".equalsIgnoreCase(operationResult) || 
               (resultCode != null && resultCode >= 400);
    }

    public boolean isUserInitiated() {
        return userId != null;
    }

    public boolean hasAdditionalData() {
        return additionalData != null && !additionalData.trim().isEmpty() && 
               !"{}".equals(additionalData.trim());
    }

    public double getOperationDurationSeconds() {
        return operationDurationMs != null ? operationDurationMs / 1000.0 : 0.0;
    }

    // Setter methods for missing properties (non-duplicates)
    public void setEventId(String eventId) { this.eventId = eventId; }
    public void setCorrelationId(String correlationId) { this.correlationId = correlationId; }
    public void setEventTime(Instant eventTime) { this.eventTime = eventTime; }
    public void setSourceService(String sourceService) { this.sourceService = sourceService; }
    public void setOperationResult(String operationResult) { this.operationResult = operationResult; }
    public void setOperationTimestamp(Instant operationTimestamp) { this.operationTimestamp = operationTimestamp; }
    public void setOperationDurationMs(Long operationDurationMs) { this.operationDurationMs = operationDurationMs; }
    public void setUsername(String username) { this.username = username; }
    public void setSessionId(String sessionId) { this.sessionId = sessionId; }
    public void setIpAddress(String ipAddress) { this.ipAddress = ipAddress; }
    public void setUserAgent(String userAgent) { this.userAgent = userAgent; }
    public void setEndpoint(String endpoint) { this.endpoint = endpoint; }
    public void setHttpMethod(String httpMethod) { this.httpMethod = httpMethod; }
    public void setRequestSize(Long requestSize) { this.requestSize = requestSize; }
    public void setResponseSize(Long responseSize) { this.responseSize = responseSize; }
    public void setResultCode(Integer resultCode) { this.resultCode = resultCode; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }

    protected void onCreate() {
        if (auditCreatedAt == null) {
            auditCreatedAt = Instant.now();
        }
    }

    public String getSummary() {
        return String.format("AuditLog{id=%d, operation='%s', user='%s', entity='%s:%s', result='%s'}", 
                getId(), operationType, username, entityType, entityId, operationResult);
    }
    
    public String getAction() {
        return operationType;
    }

    // Explicit getters and setters for Lombok compatibility
    public String getOperationType() { return operationType; }
    public void setOperationType(String operationType) { this.operationType = operationType; }
    
    public String getEntityType() { return entityType; }
    public void setEntityType(String entityType) { this.entityType = entityType; }
    
    public String getEntityId() { return entityId; }
    public void setEntityId(String entityId) { this.entityId = entityId; }
    
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    
    public String getOperationDescription() { return operationDescription; }
    public void setOperationDescription(String operationDescription) { this.operationDescription = operationDescription; }
    
    public String getAdditionalData() { return additionalData; }
    public void setAdditionalData(String additionalData) { this.additionalData = additionalData; }
    
    public Instant getAuditCreatedAt() { return auditCreatedAt; }
    public void setAuditCreatedAt(Instant auditCreatedAt) { this.auditCreatedAt = auditCreatedAt; }
}
