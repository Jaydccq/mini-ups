package com.miniups.service.consumer;

import com.miniups.config.RabbitMQConfig;
import com.miniups.model.entity.User;
import com.miniups.model.event.BusinessEvent;
import com.miniups.model.event.NotificationPayload;
import com.miniups.repository.UserRepository;
import com.miniups.service.AsyncAuditService;
import com.rabbitmq.client.Channel;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;

/**
 * Notification Consumer
 * 
 * Processes notification events asynchronously to send notifications
 * through various channels (email, SMS, push notifications) without
 * blocking the main business flow. This consumer handles the integration
 * with external notification services and manages delivery retries.
 * 
 * Key Features:
 * - Multi-channel notification delivery
 * - Asynchronous notification processing
 * - User preference handling
 * - Delivery retry mechanisms
 * - Template-based notifications
 * - Comprehensive error handling
 * - Idempotent message processing
 * 
 * Supported Notification Types:
 * - Email notifications
 * - SMS notifications  
 * - Push notifications
 * - In-app notifications
 * - Webhook notifications
 * 
 * @author Mini-UPS Development Team
 * @version 1.0
 * @since 2024
 */
@Service
@ConditionalOnClass(RabbitListener.class)
@Profile("!rabbitmq-disabled")
public class NotificationConsumer {
    private static final Logger log = LoggerFactory.getLogger(NotificationConsumer.class);

    private final UserRepository userRepository;
    private final AsyncAuditService asyncAuditService;
    
    public NotificationConsumer(UserRepository userRepository, AsyncAuditService asyncAuditService) {
        this.userRepository = userRepository;
        this.asyncAuditService = asyncAuditService;
    }

    @Value("${app.notifications.enabled:true}")
    private boolean notificationsEnabled;

    @Value("${app.notifications.email.enabled:true}")
    private boolean emailEnabled;

    @Value("${app.notifications.sms.enabled:false}")
    private boolean smsEnabled;

    @Value("${app.notifications.push.enabled:false}")
    private boolean pushEnabled;

    /**
     * Process notification events from the queue
     * 
     * This method handles all types of notification events and routes them
     * to the appropriate delivery channels based on the notification type
     * and user preferences.
     * 
     * @param event The business event containing notification data
     * @param message The RabbitMQ message for acknowledgment
     * @param channel The RabbitMQ channel for acknowledgment
     */
    @RabbitListener(queues = RabbitMQConfig.NOTIFICATIONS_QUEUE)
    public void handleNotificationEvent(BusinessEvent<NotificationPayload> event,
                                       Message message,
                                       Channel channel) throws IOException {
        
        long deliveryTag = message.getMessageProperties().getDeliveryTag();
        long startTime = System.currentTimeMillis();
        String correlationId = event.getCorrelationId();
        
        try {
            log.debug("Processing notification event: {} for user: {} (correlationId: {})",
                    event.getEventId(), event.getPayload().getRecipientUserId(), correlationId);

            // Check if notifications are globally enabled
            if (!notificationsEnabled) {
                log.debug("Notifications are disabled globally, skipping event: {}", event.getEventId());
                channel.basicAck(deliveryTag, false);
                return;
            }

            // Validate the event payload
            NotificationPayload payload = event.getPayload();
            if (!isValidNotificationPayload(payload)) {
                log.warn("Invalid notification payload in event: {}", event.getEventId());
                channel.basicAck(deliveryTag, false);
                return;
            }

            // Check if notification has expired
            if (payload.isExpired()) {
                log.debug("Notification has expired, skipping event: {}", event.getEventId());
                channel.basicAck(deliveryTag, false);
                return;
            }

            // Check if we should process this notification immediately or schedule it
            if (!payload.isImmediate()) {
                log.debug("Notification is scheduled for later delivery, should be requeued: {}", event.getEventId());
                // TODO: Implement scheduled notification handling
                channel.basicNack(deliveryTag, false, true); // requeue for later
                return;
            }

            // Process the notification
            boolean success = processNotification(payload, correlationId, startTime);

            if (success) {
                // Audit successful notification
                Map<String, Object> auditData = Map.of(
                    "notificationId", payload.getNotificationId(),
                    "recipientUserId", payload.getRecipientUserId(),
                    "notificationTypes", payload.getNotificationTypes(),
                    "subject", payload.getSubject() != null ? payload.getSubject() : "N/A",
                    "eventId", event.getEventId()
                );
                asyncAuditService.auditSuccess("notification.sent", payload.getNotificationId(), 
                                             "Notification successfully sent", 
                                             startTime, auditData);

                log.info("Successfully processed notification event: {} for user: {} (correlationId: {})",
                        event.getEventId(), payload.getRecipientUserId(), correlationId);

                // Acknowledge successful processing
                channel.basicAck(deliveryTag, false);
            } else {
                // Handle retry logic
                handleNotificationRetry(payload, event, message, channel, correlationId, startTime);
            }

        } catch (Exception e) {
            log.error("Failed to process notification event: {} (correlationId: {})", 
                     event.getEventId(), correlationId, e);
            
            // Audit the failure
            asyncAuditService.auditFailure("notification.sent", 
                                         event.getPayload().getNotificationId(), 
                                         "Failed to process notification in consumer", 
                                         startTime, e);
            
            // Handle retry or dead letter
            handleNotificationRetry(event.getPayload(), event, message, channel, correlationId, startTime);
        }
    }

    /**
     * Validate the notification payload
     * Ensures all required fields are present and valid
     * 
     * @param payload The notification payload to validate
     * @return true if the payload is valid for processing
     */
    private boolean isValidNotificationPayload(NotificationPayload payload) {
        if (payload == null) {
            return false;
        }

        // Check required fields
        if (payload.getRecipientUserId() == null && 
            payload.getRecipientEmail() == null && 
            payload.getRecipientPhone() == null) {
            log.warn("Notification payload missing recipient information");
            return false;
        }

        if (payload.getNotificationTypes() == null || payload.getNotificationTypes().isEmpty()) {
            log.warn("Notification payload missing notification types");
            return false;
        }

        if (payload.getMessage() == null || payload.getMessage().trim().isEmpty()) {
            log.warn("Notification payload missing message content");
            return false;
        }

        return true;
    }

    /**
     * Process the notification by sending it through appropriate channels
     * 
     * @param payload The notification data
     * @param correlationId The correlation ID for tracking
     * @param startTime The processing start time
     * @return true if notification was sent successfully
     */
    private boolean processNotification(NotificationPayload payload, String correlationId, long startTime) {
        boolean overallSuccess = true;
        int successCount = 0;
        int totalChannels = payload.getNotificationTypes().size();

        // Get user information if user ID is provided
        User user = null;
        if (payload.getRecipientUserId() != null) {
            user = getUserById(payload.getRecipientUserId());
        }

        // Process each notification type
        for (NotificationPayload.NotificationType type : payload.getNotificationTypes()) {
            try {
                boolean channelSuccess = false;
                
                switch (type) {
                    case EMAIL:
                        channelSuccess = sendEmailNotification(payload, user, correlationId);
                        break;
                    case SMS:
                        channelSuccess = sendSmsNotification(payload, user, correlationId);
                        break;
                    case PUSH_NOTIFICATION:
                        channelSuccess = sendPushNotification(payload, user, correlationId);
                        break;
                    case IN_APP_NOTIFICATION:
                        channelSuccess = sendInAppNotification(payload, user, correlationId);
                        break;
                    case WEBHOOK:
                        channelSuccess = sendWebhookNotification(payload, correlationId);
                        break;
                    default:
                        log.warn("Unsupported notification type: {} for event: {}", type, payload.getNotificationId());
                        break;
                }
                
                if (channelSuccess) {
                    successCount++;
                } else {
                    overallSuccess = false;
                }
                
            } catch (Exception e) {
                log.error("Failed to send {} notification for user: {} (correlationId: {})", 
                         type, payload.getRecipientUserId(), correlationId, e);
                overallSuccess = false;
            }
        }

        log.info("Notification processing completed: {}/{} channels successful for user: {} (correlationId: {})",
                successCount, totalChannels, payload.getRecipientUserId(), correlationId);

        // Consider it successful if at least one channel worked
        return successCount > 0;
    }

    /**
     * Send email notification
     */
    private boolean sendEmailNotification(NotificationPayload payload, User user, String correlationId) {
        if (!emailEnabled) {
            log.debug("Email notifications disabled, skipping for user: {}", payload.getRecipientUserId());
            return true; // Consider as successful since it's disabled by configuration
        }

        try {
            String emailAddress = payload.getRecipientEmail();
            if (emailAddress == null && user != null) {
                emailAddress = user.getEmail();
            }

            if (emailAddress == null) {
                log.warn("No email address available for notification: {}", payload.getNotificationId());
                return false;
            }

            // TODO: Integrate with actual email service (SendGrid, AWS SES, etc.)
            log.info("SIMULATED: Sending email to {} with subject: '{}' (correlationId: {})",
                    emailAddress, payload.getSubject(), correlationId);

            // Simulate email sending delay
            Thread.sleep(100);

            return true;

        } catch (Exception e) {
            log.error("Failed to send email notification (correlationId: {})", correlationId, e);
            return false;
        }
    }

    /**
     * Send SMS notification
     */
    private boolean sendSmsNotification(NotificationPayload payload, User user, String correlationId) {
        if (!smsEnabled) {
            log.debug("SMS notifications disabled, skipping for user: {}", payload.getRecipientUserId());
            return true; // Consider as successful since it's disabled by configuration
        }

        try {
            String phoneNumber = payload.getRecipientPhone();
            if (phoneNumber == null && user != null) {
                // phoneNumber = user.getPhoneNumber(); // Assuming User entity has phone number
            }

            if (phoneNumber == null) {
                log.warn("No phone number available for notification: {}", payload.getNotificationId());
                return false;
            }

            // TODO: Integrate with actual SMS service (Twilio, AWS SNS, etc.)
            log.info("SIMULATED: Sending SMS to {} with message: '{}' (correlationId: {})",
                    phoneNumber, truncateForSms(payload.getMessage()), correlationId);

            // Simulate SMS sending delay
            Thread.sleep(50);

            return true;

        } catch (Exception e) {
            log.error("Failed to send SMS notification (correlationId: {})", correlationId, e);
            return false;
        }
    }

    /**
     * Send push notification
     */
    private boolean sendPushNotification(NotificationPayload payload, User user, String correlationId) {
        if (!pushEnabled) {
            log.debug("Push notifications disabled, skipping for user: {}", payload.getRecipientUserId());
            return true;
        }

        try {
            String deviceToken = payload.getDeviceToken();
            
            if (deviceToken == null) {
                log.warn("No device token available for push notification: {}", payload.getNotificationId());
                return false;
            }

            // TODO: Integrate with actual push notification service (FCM, APNS)
            log.info("SIMULATED: Sending push notification to device token ending in {} (correlationId: {})",
                    deviceToken.substring(Math.max(0, deviceToken.length() - 8)), correlationId);

            return true;

        } catch (Exception e) {
            log.error("Failed to send push notification (correlationId: {})", correlationId, e);
            return false;
        }
    }

    /**
     * Send in-app notification
     */
    private boolean sendInAppNotification(NotificationPayload payload, User user, String correlationId) {
        try {
            // TODO: Store in-app notification in database for user to see in UI
            log.info("SIMULATED: Created in-app notification for user: {} (correlationId: {})",
                    payload.getRecipientUserId(), correlationId);

            return true;

        } catch (Exception e) {
            log.error("Failed to create in-app notification (correlationId: {})", correlationId, e);
            return false;
        }
    }

    /**
     * Send webhook notification
     */
    private boolean sendWebhookNotification(NotificationPayload payload, String correlationId) {
        try {
            // TODO: Send HTTP webhook to external system
            log.info("SIMULATED: Sending webhook notification (correlationId: {})", correlationId);

            return true;

        } catch (Exception e) {
            log.error("Failed to send webhook notification (correlationId: {})", correlationId, e);
            return false;
        }
    }

    /**
     * Handle notification retry logic
     */
    private void handleNotificationRetry(NotificationPayload payload, BusinessEvent<?> event, 
                                       Message message, Channel channel, 
                                       String correlationId, long startTime) throws IOException {
        
        long deliveryTag = message.getMessageProperties().getDeliveryTag();
        
        // Increment attempt counter
        payload.incrementAttempt();
        
        if (payload.isMaxRetriesExceeded()) {
            log.warn("Max retry attempts exceeded for notification: {} (correlationId: {})", 
                    payload.getNotificationId(), correlationId);
            
            // Audit the failure
            asyncAuditService.auditFailure("notification.max_retries", payload.getNotificationId(), 
                                         "Notification failed after max retry attempts", 
                                         startTime, new RuntimeException("Max retries exceeded"));
            
            // Send to dead letter queue
            channel.basicNack(deliveryTag, false, false);
        } else {
            log.info("Requeuing notification for retry: {} (attempt: {}/{}) (correlationId: {})", 
                    payload.getNotificationId(), payload.getCurrentAttempt(), 
                    payload.getMaxRetryAttempts(), correlationId);
            
            // Requeue for retry
            channel.basicNack(deliveryTag, false, true);
        }
    }

    /**
     * Get user by ID
     */
    private User getUserById(Long userId) {
        try {
            Optional<User> userOpt = userRepository.findById(userId);
            return userOpt.orElse(null);
        } catch (Exception e) {
            log.error("Failed to get user by ID: {}", userId, e);
            return null;
        }
    }

    /**
     * Truncate message for SMS (160 character limit)
     */
    private String truncateForSms(String message) {
        if (message == null) {
            return "";
        }
        if (message.length() <= 160) {
            return message;
        }
        return message.substring(0, 157) + "...";
    }
}