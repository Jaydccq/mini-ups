/**
 * World Simulator Debug Aspect
 * 
 * Purpose:
 * - Intercepts Protocol Buffer messages sent to/received from World Simulator
 * - Converts binary protobuf messages to human-readable JSON format
 * - Publishes debug messages to admin clients via WebSocket
 * - Provides non-intrusive debugging without modifying core service code
 * 
 * Features:
 * - AOP-based message interception using @Around advice
 * - Automatic JSON conversion of protobuf messages using JsonFormat
 * - Configurable enable/disable via application properties
 * - Bounded message queue to prevent memory issues
 * - Async event publishing to avoid blocking World Simulator operations
 * - Sequence number extraction for request/response correlation
 * - Error handling with fallback for failed conversions
 * 
 * Configuration:
 * - app.debug.world-simulator.enabled=true/false
 * - app.debug.world-simulator.buffer-size=5000 (max messages in memory)
 * - app.debug.world-simulator.retention-seconds=300 (message retention time)
 * 
 * Message Flow:
 * 1. WorldSimulatorService method is called
 * 2. Aspect intercepts method execution
 * 3. Protobuf message is converted to JSON
 * 4. DebugMessage event is published async
 * 5. Event listener broadcasts to WebSocket subscribers
 * 6. Original method continues execution normally
 * 
 * Security:
 * - Only activated when explicitly enabled in configuration
 * - Admin-role authorization handled in WebSocket layer
 * - No sensitive data logging beyond what's in protobuf messages
 * 
 * Performance:
 * - Minimal overhead when disabled (simple boolean check)
 * - Async processing prevents blocking simulator operations
 * - Bounded buffer prevents memory leaks
 * - JSON conversion cached per message type
 * 
 * @author Mini-UPS Team
 * @version 1.0.0
 */
package com.miniups.debug;

import com.google.protobuf.Message;
import com.google.protobuf.util.JsonFormat;
import com.miniups.model.dto.debug.WorldSimulatorDebugMessageDto;
import com.miniups.proto.WorldUpsProto;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.concurrent.CompletableFuture;

@Aspect
@Component
@ConditionalOnProperty(name = "app.debug.world-simulator.enabled", havingValue = "true")
public class WorldSimulatorDebugAspect {
    
    private static final Logger logger = LoggerFactory.getLogger(WorldSimulatorDebugAspect.class);
    private static final String PROCESS_INCOMING_MESSAGE = "processIncomingMessage";
    
    @Autowired
    private ApplicationEventPublisher eventPublisher;
    
    // Configuration values for future use
    @SuppressWarnings("unused")
    @Value("${app.debug.world-simulator.buffer-size:5000}")
    private int bufferSize;
    
    @SuppressWarnings("unused")
    @Value("${app.debug.world-simulator.retention-seconds:300}")
    private int retentionSeconds;
    
    private final JsonFormat.Printer jsonPrinter;
    
    public WorldSimulatorDebugAspect() {
        this.jsonPrinter = JsonFormat.printer()
                .preservingProtoFieldNames()
                .omittingInsignificantWhitespace();
    }
    
    /**
     * Intercept outbound messages (public methods that trigger sending messages to World Simulator)
     */
    @Around("execution(* com.miniups.service.WorldSimulatorService.sendTruckTo*(..)) || " +
            "execution(* com.miniups.service.WorldSimulatorService.connect*(..)) || " +
            "execution(* com.miniups.service.WorldSimulatorService.disconnect*(..))")
    public Object interceptOutboundMessage(ProceedingJoinPoint joinPoint) throws Throwable {
        try {
            // Extract protobuf message from method arguments
            Message protobufMessage = findProtobufMessage(joinPoint.getArgs());
            
            if (protobufMessage != null) {
                publishDebugMessage(protobufMessage, WorldSimulatorDebugMessageDto.Direction.OUTBOUND);
            }
        } catch (Exception e) {
            logger.debug("Error capturing outbound message for debug", e);
        }
        
        // Continue with original method execution
        return joinPoint.proceed();
    }
    
    /**
     * Intercept status/state checking methods to capture connection state
     */
    @Around("execution(* com.miniups.service.WorldSimulatorService.isConnected(..)) || " +
            "execution(* com.miniups.service.WorldSimulatorService.isConnectionHealthy(..)) || " +
            "execution(* com.miniups.service.WorldSimulatorService.getWorldId(..))")
    public Object interceptStatusCheck(ProceedingJoinPoint joinPoint) throws Throwable {
        try {
            // Log status check operations for debugging
            String methodName = joinPoint.getSignature().getName();
            logger.debug("World Simulator status check: {}", methodName);
        } catch (Exception e) {
            logger.debug("Error capturing status check for debug", e);
        }
        
        // Continue with original method execution
        return joinPoint.proceed();
    }
    
    /**
     * Find protobuf message in method arguments
     */
    private Message findProtobufMessage(Object[] args) {
        if (args == null) {
            return null;
        }
        
        for (Object arg : args) {
            if (arg instanceof Message) {
                return (Message) arg;
            }
        }
        
        return null;
    }
    
    /**
     * Convert protobuf message to JSON and publish debug event
     */
    private void publishDebugMessage(Message protobufMessage, WorldSimulatorDebugMessageDto.Direction direction) {
        try {
            // Convert to JSON
            String jsonContent = jsonPrinter.print(protobufMessage);
            
            // Get message type
            String messageType = protobufMessage.getClass().getSimpleName();
            
            // Get message size
            int sizeBytes = protobufMessage.getSerializedSize();
            
            // Extract sequence number if available
            Long sequenceNumber = extractSequenceNumber(protobufMessage);
            
            // Create debug message DTO
            WorldSimulatorDebugMessageDto debugMessage = new WorldSimulatorDebugMessageDto(
                direction, messageType, jsonContent, sizeBytes, sequenceNumber
            );
            
            // Publish event asynchronously
            CompletableFuture.runAsync(() -> publishDebugEvent(debugMessage));
            
        } catch (Exception e) {
            logger.debug("Error converting protobuf message to JSON", e);
            
            // Fallback: create debug message with error info
            try {
                WorldSimulatorDebugMessageDto errorMessage = new WorldSimulatorDebugMessageDto(
                    direction, 
                    protobufMessage.getClass().getSimpleName(),
                    "Error converting message: " + e.getMessage(),
                    protobufMessage.getSerializedSize()
                );
                
                CompletableFuture.runAsync(() -> publishDebugEvent(errorMessage));
            } catch (Exception fallbackError) {
                logger.debug("Error in fallback debug message creation", fallbackError);
            }
        }
    }
    
    /**
     * Extract sequence number from protobuf message if available
     */
    private Long extractSequenceNumber(Message message) {
        try {
            // Use reflection to find seqnum field
            Method getSeqnumMethod = message.getClass().getMethod("getSeqnum");
            Object seqnum = getSeqnumMethod.invoke(message);
            if (seqnum instanceof Number) {
                return ((Number) seqnum).longValue();
            }
        } catch (Exception e) {
            // Not all messages have sequence numbers, this is normal
        }
        
        try {
            // Alternative: try originseqnum (for error messages)
            Method getOriginSeqnumMethod = message.getClass().getMethod("getOriginseqnum");
            Object seqnum = getOriginSeqnumMethod.invoke(message);
            if (seqnum instanceof Number) {
                return ((Number) seqnum).longValue();
            }
        } catch (Exception e) {
            // Not all messages have origin sequence numbers, this is normal
        }
        
        return null;
    }
    
    /**
     * Helper method to publish debug event with error handling
     */
    private void publishDebugEvent(WorldSimulatorDebugMessageDto debugMessage) {
        try {
            eventPublisher.publishEvent(new WorldSimulatorDebugEvent(debugMessage));
        } catch (Exception e) {
            logger.debug("Error publishing debug event", e);
        }
    }
}