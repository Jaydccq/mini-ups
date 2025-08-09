/**
 * World Simulator Debug Event
 * 
 * Purpose:
 * - Spring Application Event for World Simulator message debugging
 * - Carries debug message data from AOP aspect to WebSocket broadcaster
 * - Enables loose coupling between message capture and broadcasting
 * - Supports async event processing to avoid blocking simulator operations
 * 
 * Features:
 * - Immutable event data for thread safety
 * - Built-in timestamp for event ordering
 * - Integration with Spring's event system
 * - Supports event filtering and processing
 * 
 * Usage:
 * - Published by WorldSimulatorDebugAspect when messages are captured
 * - Consumed by WorldSimulatorDebugEventListener for WebSocket broadcasting
 * - Can be extended for additional debug event types
 * 
 * Event Flow:
 * 1. AOP aspect captures protobuf message
 * 2. Aspect creates WorldSimulatorDebugEvent
 * 3. Spring ApplicationEventPublisher publishes event
 * 4. Event listener receives and processes event
 * 5. Message is broadcast to WebSocket subscribers
 * 
 * @author Mini-UPS Team
 * @version 1.0.0
 */
package com.miniups.debug;

import com.miniups.model.dto.debug.WorldSimulatorDebugMessageDto;
import org.springframework.context.ApplicationEvent;

public class WorldSimulatorDebugEvent extends ApplicationEvent {
    
    private final WorldSimulatorDebugMessageDto debugMessage;
    
    public WorldSimulatorDebugEvent(Object source, WorldSimulatorDebugMessageDto debugMessage) {
        super(source);
        this.debugMessage = debugMessage;
    }
    
    public WorldSimulatorDebugEvent(WorldSimulatorDebugMessageDto debugMessage) {
        super(debugMessage);
        this.debugMessage = debugMessage;
    }
    
    public WorldSimulatorDebugMessageDto getDebugMessage() {
        return debugMessage;
    }
    
    @Override
    public String toString() {
        return String.format("WorldSimulatorDebugEvent[%s]", debugMessage);
    }
}