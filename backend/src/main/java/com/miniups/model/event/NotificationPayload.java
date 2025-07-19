package com.miniups.model.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class NotificationPayload {

    public enum NotificationType {
        EMAIL,
        SMS,
        PUSH_NOTIFICATION,
        IN_APP_NOTIFICATION,
        WEBHOOK
    }

    public enum Priority {
        LOW,
        NORMAL,
        HIGH,
        URGENT
    }

    private String notificationId;
    private Long recipientUserId;
    private String recipientEmail;
    private String recipientPhone;
    private String deviceToken;
    private List<NotificationType> notificationTypes;
    private String subject;
    private String message;
    private String htmlContent;
    private String templateId;
    private Map<String, Object> templateVariables;
    private Priority priority;
    private Instant scheduledDeliveryTime;
    private Instant expirationTime;
    private Integer maxRetryAttempts;
    private Integer currentAttempt;
    private String category;
    private String relatedEntityId;
    private String relatedEntityType;
    private String deepLinkUrl;
    private List<NotificationAction> actions;
    private Map<String, Object> metadata;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class NotificationAction {
        private String actionId;
        private String label;
        private String url;
        private String actionType;
    }

    public boolean isImmediate() {
        return scheduledDeliveryTime == null || scheduledDeliveryTime.isBefore(Instant.now());
    }

    public boolean isExpired() {
        return expirationTime != null && expirationTime.isBefore(Instant.now());
    }

    public boolean isMaxRetriesExceeded() {
        return maxRetryAttempts != null && currentAttempt != null && 
               currentAttempt >= maxRetryAttempts;
    }

    public void incrementAttempt() {
        if (currentAttempt == null) {
            currentAttempt = 1;
        } else {
            currentAttempt++;
        }
    }

    public boolean includesEmail() {
        return notificationTypes != null && notificationTypes.contains(NotificationType.EMAIL);
    }

    public boolean includesSms() {
        return notificationTypes != null && notificationTypes.contains(NotificationType.SMS);
    }

    public boolean includesPush() {
        return notificationTypes != null && notificationTypes.contains(NotificationType.PUSH_NOTIFICATION);
    }

    public boolean usesTemplate() {
        return templateId != null && !templateId.trim().isEmpty();
    }

    public boolean hasTemplateVariables() {
        return templateVariables != null && !templateVariables.isEmpty();
    }

    public void addTemplateVariable(String key, Object value) {
        if (templateVariables == null) {
            templateVariables = new java.util.HashMap<>();
        }
        templateVariables.put(key, value);
    }

    public void addMetadata(String key, Object value) {
        if (metadata == null) {
            metadata = new java.util.HashMap<>();
        }
        metadata.put(key, value);
    }

    // Explicit getters and setters for Lombok compatibility
    public String getNotificationId() { return notificationId; }
    public void setNotificationId(String notificationId) { this.notificationId = notificationId; }
    
    public Long getRecipientUserId() { return recipientUserId; }
    public void setRecipientUserId(Long recipientUserId) { this.recipientUserId = recipientUserId; }
    
    public String getRecipientEmail() { return recipientEmail; }
    public void setRecipientEmail(String recipientEmail) { this.recipientEmail = recipientEmail; }
    
    public String getRecipientPhone() { return recipientPhone; }
    public void setRecipientPhone(String recipientPhone) { this.recipientPhone = recipientPhone; }
    
    public String getDeviceToken() { return deviceToken; }
    public void setDeviceToken(String deviceToken) { this.deviceToken = deviceToken; }
    
    public List<NotificationType> getNotificationTypes() { return notificationTypes; }
    public void setNotificationTypes(List<NotificationType> notificationTypes) { this.notificationTypes = notificationTypes; }
    
    public String getSubject() { return subject; }
    public void setSubject(String subject) { this.subject = subject; }
    
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    
    public String getHtmlContent() { return htmlContent; }
    public void setHtmlContent(String htmlContent) { this.htmlContent = htmlContent; }
    
    public String getTemplateId() { return templateId; }
    public void setTemplateId(String templateId) { this.templateId = templateId; }
    
    public Map<String, Object> getTemplateVariables() { return templateVariables; }
    public void setTemplateVariables(Map<String, Object> templateVariables) { this.templateVariables = templateVariables; }
    
    public Priority getPriority() { return priority; }
    public void setPriority(Priority priority) { this.priority = priority; }
    
    public Integer getCurrentAttempt() { return currentAttempt; }
    public void setCurrentAttempt(Integer currentAttempt) { this.currentAttempt = currentAttempt; }
    
    public Integer getMaxRetryAttempts() { return maxRetryAttempts; }
    public void setMaxRetryAttempts(Integer maxRetryAttempts) { this.maxRetryAttempts = maxRetryAttempts; }
    
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    @Override
    public String toString() {
        return String.format("NotificationPayload{id='%s', recipient=%d, types=%s, priority=%s}", 
                notificationId, recipientUserId, notificationTypes, priority);
    }
}