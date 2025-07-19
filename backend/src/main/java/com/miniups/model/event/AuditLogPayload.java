package com.miniups.model.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AuditLogPayload {

    private String operationType;
    private Long userId;
    private String username;
    private String sessionId;
    private String ipAddress;
    private String userAgent;
    private String entityId;
    private String entityType;
    private String operationDescription;
    private String operationResult;
    private Integer resultCode;
    private String errorMessage;
    private Long operationDurationMs;
    private Map<String, Object> additionalData;
    private Instant operationTimestamp;
    private String endpoint;
    private String httpMethod;
    private Long requestSize;
    private Long responseSize;

    public boolean isSuccessful() {
        return "SUCCESS".equalsIgnoreCase(operationResult) && 
               (resultCode == null || (resultCode >= 200 && resultCode < 300));
    }

    public boolean isFailed() {
        return "FAILED".equalsIgnoreCase(operationResult) || 
               (resultCode != null && resultCode >= 400);
    }

    public boolean hasAdditionalData() {
        return additionalData != null && !additionalData.isEmpty();
    }

    public boolean isUserInitiated() {
        return userId != null;
    }

    public double getOperationDurationSeconds() {
        return operationDurationMs != null ? operationDurationMs / 1000.0 : 0.0;
    }

    public void addAdditionalData(String key, Object value) {
        if (additionalData == null) {
            additionalData = new java.util.HashMap<>();
        }
        additionalData.put(key, value);
    }

    // Explicit getters and setters for Lombok compatibility
    public String getOperationType() { return operationType; }
    public void setOperationType(String operationType) { this.operationType = operationType; }
    
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    
    public String getEntityId() { return entityId; }
    public void setEntityId(String entityId) { this.entityId = entityId; }
    
    public String getEntityType() { return entityType; }
    public void setEntityType(String entityType) { this.entityType = entityType; }
    
    public String getOperationDescription() { return operationDescription; }
    public void setOperationDescription(String operationDescription) { this.operationDescription = operationDescription; }
    
    public String getOperationResult() { return operationResult; }
    public void setOperationResult(String operationResult) { this.operationResult = operationResult; }
    
    public Instant getOperationTimestamp() { return operationTimestamp; }
    public void setOperationTimestamp(Instant operationTimestamp) { this.operationTimestamp = operationTimestamp; }
    
    public Long getOperationDurationMs() { return operationDurationMs; }
    public void setOperationDurationMs(Long operationDurationMs) { this.operationDurationMs = operationDurationMs; }
    
    public Map<String, Object> getAdditionalData() { return additionalData; }
    public void setAdditionalData(Map<String, Object> additionalData) { this.additionalData = additionalData; }
    
    public String getSessionId() { return sessionId; }
    public void setSessionId(String sessionId) { this.sessionId = sessionId; }
    
    public String getIpAddress() { return ipAddress; }
    public void setIpAddress(String ipAddress) { this.ipAddress = ipAddress; }
    
    public String getUserAgent() { return userAgent; }
    public void setUserAgent(String userAgent) { this.userAgent = userAgent; }
    
    public String getEndpoint() { return endpoint; }
    public void setEndpoint(String endpoint) { this.endpoint = endpoint; }
    
    public String getHttpMethod() { return httpMethod; }
    public void setHttpMethod(String httpMethod) { this.httpMethod = httpMethod; }
    
    public Integer getResultCode() { return resultCode; }
    public void setResultCode(Integer resultCode) { this.resultCode = resultCode; }
    
    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
    
    public Long getRequestSize() { return requestSize; }
    public void setRequestSize(Long requestSize) { this.requestSize = requestSize; }
    
    public Long getResponseSize() { return responseSize; }
    public void setResponseSize(Long responseSize) { this.responseSize = responseSize; }

    @Override
    public String toString() {
        return String.format("AuditLogPayload{operation='%s', user='%s', entity='%s:%s', result='%s'}", 
                operationType, username, entityType, entityId, operationResult);
    }
}