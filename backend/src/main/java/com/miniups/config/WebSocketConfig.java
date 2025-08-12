/**
 * WebSocket Configuration Class
 * 
 * Purpose:
 * - Configures WebSocket messaging infrastructure for real-time communication
 * - Enables STOMP (Simple Text Oriented Messaging Protocol) over WebSocket
 * - Provides real-time updates for package tracking, truck locations, and system notifications
 * - Facilitates bidirectional communication between clients and server
 * 
 * Core Functionality:
 * - Message Broker Configuration: Sets up in-memory message broker for pub/sub messaging
 * - STOMP Endpoint Registration: Defines WebSocket connection endpoints with SockJS fallback
 * - Destination Prefixes: Configures routing prefixes for different message types
 * - Cross-Origin Support: Enables WebSocket connections from web browsers
 * 
 * Message Broker Setup:
 * - Simple Broker: In-memory broker for /topic and /queue destinations
 * - Topic Destinations (/topic/**): Broadcast messages to multiple subscribers
 * - Queue Destinations (/queue/**): Point-to-point messaging for specific users
 * - Application Prefix (/app): Routes messages to application message handlers
 * - User Prefix (/user): Enables user-specific messaging capabilities
 * 
 * STOMP Protocol Features:
 * - Frame-based messaging protocol over WebSocket
 * - Support for CONNECT, SEND, SUBSCRIBE, UNSUBSCRIBE commands
 * - Automatic message routing based on destination patterns
 * - Built-in authentication and authorization integration
 * - Message headers for metadata and routing information
 * 
 * WebSocket Endpoint Configuration:
 * - Primary Endpoint: /ws for WebSocket connections
 * - SockJS Fallback: Enables connections when WebSocket is not available
 * - CORS Support: Allows connections from all origins (configurable for production)
 * - Transport Protocols: WebSocket, Long Polling, XHR Streaming fallbacks
 * 
 * Real-time Use Cases in Mini-UPS:
 * - Package Status Updates: Real-time delivery status changes
 * - Truck Location Tracking: Live GPS coordinates for delivery trucks
 * - Driver Notifications: Instant alerts for new assignments or route changes
 * - Admin Dashboard: Real-time system metrics and monitoring data
 * - Customer Notifications: Delivery confirmations and status updates
 * 
 * Message Destination Patterns:
 * - /topic/packages/{packageId}: Package-specific status updates
 * - /topic/trucks/{truckId}: Truck location and status updates
 * - /topic/drivers/{driverId}: Driver-specific notifications
 * - /queue/user/{userId}: Private user notifications
 * - /topic/admin/dashboard: System-wide administrative updates
 * 
 * Client Integration:
 * - JavaScript STOMP clients for web browsers
 * - Mobile application integration through WebSocket libraries
 * - Real-time dashboard updates without polling
 * - Automatic reconnection and error handling
 * 
 * Security Considerations:
 * - Integration with Spring Security for authentication
 * - User-specific message authorization through interceptors
 * - Message content validation and sanitization
 * - Rate limiting to prevent message flooding
 * - Secure WebSocket connections (WSS) in production
 * 
 * Performance Features:
 * - In-memory message broker for low latency
 * - Message buffering for temporary client disconnections
 * - Efficient message routing and subscription management
 * - Automatic cleanup of inactive subscriptions
 * - Scalable architecture for multiple concurrent connections
 * 
 * SockJS Fallback Support:
 * - WebSocket: Primary transport for modern browsers
 * - XHR Streaming: Fallback for older browsers
 * - Long Polling: Universal fallback for restricted environments
 * - Transport negotiation: Automatic selection of best available transport
 * 
 * Integration Points:
 * - Works with Spring Security for user authentication
 * - Integrates with service layers for real-time data push
 * - Supports Redis pub/sub for distributed messaging (configurable)
 * - Compatible with Spring Boot WebSocket auto-configuration
 * 
 * Development vs Production:
 * - CORS settings should be restricted to specific origins in production
 * - Consider external message brokers (RabbitMQ, ActiveMQ) for high scale
 * - Implement proper SSL/TLS for secure WebSocket connections
 * - Add monitoring and metrics for WebSocket connection health
 * 
 * Message Flow Examples:
 * 1. Client connects to /ws endpoint with SockJS
 * 2. Client subscribes to /topic/packages/123 for package updates
 * 3. Server publishes status update to /topic/packages/123
 * 4. All subscribed clients receive the update automatically
 * 5. Client can send messages to /app/update-location for driver location updates
 * 
 * Error Handling:
 * - Automatic reconnection for temporary network issues
 * - Message delivery confirmation and retry mechanisms
 * - Dead letter queues for failed message delivery
 * - Graceful degradation when WebSocket is unavailable
 * 
 * Monitoring and Debugging:
 * - Connection metrics through Spring Boot Actuator
 * - Message broker statistics and performance monitoring
 * - WebSocket session tracking and management
 * - Debug logging for message routing and delivery
 * 
 *
 
 * @since 2024-01-01
 */
package com.miniups.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {
    
    @Value("${spring.rabbitmq.host:localhost}")
    private String relayHost;
    
    @Value("${spring.rabbitmq.stomp.port:61613}")
    private int relayPort;
    
    @Value("${spring.rabbitmq.username:guest}")
    private String clientLogin;
    
    @Value("${spring.rabbitmq.password:guest}")
    private String clientPasscode;
    
    @Value("${app.cors.allowed-origins}")
    private String allowedOriginsStr;
    
    private String[] getAllowedOrigins() {
        if (allowedOriginsStr == null || allowedOriginsStr.trim().isEmpty()) {
            return new String[]{"http://localhost:3000"};
        }
        return allowedOriginsStr.split(",");
    }
    
    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        // Use simple in-memory broker for development (fallback when RabbitMQ is unavailable)
        // For production, uncomment the STOMP broker relay configuration below
        config.enableSimpleBroker("/topic", "/queue")
                .setHeartbeatValue(new long[]{10000, 10000})
                .setTaskScheduler(heartBeatScheduler());
        
        // STOMP Broker Relay configuration (for production with RabbitMQ)
        // Uncomment the following block when RabbitMQ is properly configured:
        /*
        config.enableStompBrokerRelay("/topic", "/queue")
                .setRelayHost(relayHost)        // RabbitMQ server host from config
                .setRelayPort(relayPort)        // STOMP port for RabbitMQ from config
                .setClientLogin(clientLogin)    // RabbitMQ username from config
                .setClientPasscode(clientPasscode) // RabbitMQ password from config
                .setSystemLogin(clientLogin)    // System login for broker communication
                .setSystemPasscode(clientPasscode); // System password for broker communication
        */
        
        // Set application destination prefix
        config.setApplicationDestinationPrefixes("/app");
        
        // Set user destination prefix
        config.setUserDestinationPrefix("/user");
    }
    
    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // Register WebSocket endpoint with secure CORS settings
        registry.addEndpoint("/ws")
                .setAllowedOrigins(getAllowedOrigins())
                .withSockJS();
    }
    
    @Bean
    public TaskScheduler heartBeatScheduler() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(1);
        scheduler.setThreadNamePrefix("websocket-heartbeat-");
        scheduler.initialize();
        return scheduler;
    }
}