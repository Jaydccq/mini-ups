package com.miniups.network.netty.client;

import com.miniups.network.netty.config.NettyProperties;
import com.miniups.network.netty.handler.ClientChannelInitializer;
import com.miniups.network.netty.handler.MessageHandlerService;
import com.miniups.proto.WorldUpsProto.UCommands;
import com.miniups.proto.WorldUpsProto.UResponses;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.socket.nio.NioSocketChannel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.net.InetSocketAddress;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Netty-based TCP client for World Simulator communication.
 * 
 * This client handles the low-level network communication with the World Simulator
 * using Netty's asynchronous I/O model. It provides connection management,
 * automatic reconnection, and message routing capabilities.
 * 
 * Key features:
 * - Non-blocking I/O operations
 * - Automatic reconnection with exponential backoff
 * - Request-response correlation using sequence numbers
 * - Proper resource cleanup and lifecycle management
 * 
 * @author Mini-UPS System
 * @version 1.0
 */
@Slf4j
@Component
@ConditionalOnProperty(
    name = "world.simulator.client.type",
    havingValue = "netty"
)
public class NettyClient {

    private final EventLoopGroup workerGroup;
    private final NettyProperties nettyProperties;
    private final MessageHandlerService messageHandlerService;
    
    // Network components
    private Bootstrap bootstrap;
    private Channel channel;
    
    // Connection state
    private final AtomicBoolean connected = new AtomicBoolean(false);
    private final AtomicBoolean shutdown = new AtomicBoolean(false);
    
    // Message correlation
    private final Map<Long, CompletableFuture<Object>> pendingResponses = new ConcurrentHashMap<>();
    
    // Connection details
    private String currentHost;
    private int currentPort;
    private Long worldId;

    public NettyClient(EventLoopGroup workerGroup, 
                      NettyProperties nettyProperties,
                      MessageHandlerService messageHandlerService) {
        this.workerGroup = workerGroup;
        this.nettyProperties = nettyProperties;
        this.messageHandlerService = messageHandlerService;
    }

    /**
     * Initialize the Netty bootstrap with proper configuration.
     */
    @PostConstruct
    public void initialize() {
        log.info("Initializing Netty client with configuration: {}", nettyProperties);
        
        this.bootstrap = new Bootstrap()
            .group(workerGroup)
            .channel(NioSocketChannel.class)
            .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, nettyProperties.getConnectionTimeoutMs())
            .option(ChannelOption.SO_KEEPALIVE, nettyProperties.isKeepAlive())
            .option(ChannelOption.TCP_NODELAY, nettyProperties.isTcpNoDelay())
            .option(ChannelOption.SO_REUSEADDR, true)
            .handler(new ClientChannelInitializer(messageHandlerService, pendingResponses, this));
            
        log.info("Netty client bootstrap initialized successfully");
    }

    /**
     * Establish connection to the World Simulator.
     * 
     * @param host the hostname or IP address
     * @param port the port number
     * @param worldId the world ID for the connection
     * @return CompletableFuture that completes when connection is established
     */
    public CompletableFuture<Void> connect(String host, int port, Long worldId) {
        if (shutdown.get()) {
            return CompletableFuture.failedFuture(new IllegalStateException("Client has been shut down"));
        }
        
        this.currentHost = host;
        this.currentPort = port;
        this.worldId = worldId;
        
        CompletableFuture<Void> connectFuture = new CompletableFuture<>();
        
        log.info("Attempting to connect to World Simulator at {}:{} with worldId={}", host, port, worldId);
        
        ChannelFuture channelFuture = bootstrap.connect(new InetSocketAddress(host, port));
        
        channelFuture.addListener(new ChannelFutureListener() {
            @Override
            public void operationComplete(ChannelFuture future) {
                if (future.isSuccess()) {
                    channel = future.channel();
                    connected.set(true);
                    connectFuture.complete(null);
                    log.info("Successfully connected to World Simulator at {}:{}", host, port);
                    
                    // Add channel close listener
                    channel.closeFuture().addListener(closeFuture -> {
                        log.warn("Channel closed to World Simulator");
                        connected.set(false);
                        handleDisconnection();
                    });
                } else {
                    connected.set(false);
                    Throwable cause = future.cause();
                    log.error("Failed to connect to World Simulator at {}:{}", host, port, cause);
                    connectFuture.completeExceptionally(cause);
                }
            }
        });
        
        return connectFuture;
    }

    /**
     * Send a command to the World Simulator.
     * 
     * @param command the UCommands protobuf message to send
     * @return ChannelFuture for monitoring the send operation
     */
    public ChannelFuture sendCommand(UCommands command) {
        if (!isConnected()) {
            return channel.newFailedFuture(new IllegalStateException("Not connected to World Simulator"));
        }
        
        log.debug("Sending command with {} pickups, {} deliveries, {} queries", 
                 command.getPickupsCount(), 
                 command.getDeliveriesCount(), 
                 command.getQueriesCount());
        
        return channel.writeAndFlush(command);
    }

    /**
     * Send a command and wait for a specific response based on sequence number.
     * 
     * @param command the command to send
     * @param sequenceNumber the sequence number to wait for
     * @param timeoutMs timeout in milliseconds
     * @return CompletableFuture with the response
     */
    public CompletableFuture<Object> sendCommandAndWait(UCommands command, long sequenceNumber, long timeoutMs) {
        CompletableFuture<Object> responseFuture = new CompletableFuture<>();
        
        // Store the future for correlation
        pendingResponses.put(sequenceNumber, responseFuture);
        
        // Schedule timeout
        workerGroup.schedule(() -> {
            CompletableFuture<Object> timeoutFuture = pendingResponses.remove(sequenceNumber);
            if (timeoutFuture != null && !timeoutFuture.isDone()) {
                timeoutFuture.completeExceptionally(
                    new RuntimeException("Request timeout after " + timeoutMs + "ms for sequence " + sequenceNumber));
            }
        }, timeoutMs, TimeUnit.MILLISECONDS);
        
        // Send the command
        sendCommand(command).addListener(new ChannelFutureListener() {
            @Override
            public void operationComplete(ChannelFuture future) {
                if (!future.isSuccess()) {
                    CompletableFuture<Object> failedFuture = pendingResponses.remove(sequenceNumber);
                    if (failedFuture != null && !failedFuture.isDone()) {
                        failedFuture.completeExceptionally(future.cause());
                    }
                }
            }
        });
        
        return responseFuture;
    }

    /**
     * Complete a pending response future with the received response.
     * Called by the ClientHandler when a response is received.
     * 
     * @param sequenceNumber the sequence number of the response
     * @param response the response object
     */
    public void completePendingResponse(long sequenceNumber, Object response) {
        CompletableFuture<Object> future = pendingResponses.remove(sequenceNumber);
        if (future != null && !future.isDone()) {
            future.complete(response);
            log.debug("Completed pending response for sequence {}", sequenceNumber);
        }
    }

    /**
     * Disconnect from the World Simulator.
     * 
     * @return CompletableFuture that completes when disconnection is finished
     */
    public CompletableFuture<Void> disconnect() {
        log.info("Disconnecting from World Simulator");
        
        CompletableFuture<Void> disconnectFuture = new CompletableFuture<>();
        
        if (channel != null && channel.isActive()) {
            connected.set(false);
            
            channel.close().addListener(new ChannelFutureListener() {
                @Override
                public void operationComplete(ChannelFuture future) {
                    if (future.isSuccess()) {
                        log.info("Successfully disconnected from World Simulator");
                        disconnectFuture.complete(null);
                    } else {
                        log.error("Error during disconnection", future.cause());
                        disconnectFuture.completeExceptionally(future.cause());
                    }
                }
            });
        } else {
            disconnectFuture.complete(null);
        }
        
        return disconnectFuture;
    }

    /**
     * Handle disconnection events (for reconnection logic).
     */
    private void handleDisconnection() {
        connected.set(false);
        
        // Cancel all pending responses
        pendingResponses.forEach((seqNum, future) -> {
            if (!future.isDone()) {
                future.completeExceptionally(new RuntimeException("Connection lost"));
            }
        });
        pendingResponses.clear();
        
        log.warn("Connection lost to World Simulator. Pending responses cleared.");
    }

    /**
     * Shutdown the client and release all resources.
     */
    @PreDestroy
    public void shutdown() {
        if (shutdown.compareAndSet(false, true)) {
            log.info("Shutting down Netty client");
            
            // Cancel all pending responses
            pendingResponses.forEach((seqNum, future) -> {
                if (!future.isDone()) {
                    future.completeExceptionally(new RuntimeException("Client shutting down"));
                }
            });
            pendingResponses.clear();
            
            // Close the channel
            if (channel != null && channel.isActive()) {
                try {
                    channel.close().sync();
                } catch (InterruptedException e) {
                    log.warn("Interrupted while closing channel", e);
                    Thread.currentThread().interrupt();
                }
            }
            
            connected.set(false);
            log.info("Netty client shutdown completed");
        }
    }

    // Getter methods for status monitoring
    
    public boolean isConnected() {
        return connected.get() && channel != null && channel.isActive();
    }
    
    public boolean isShutdown() {
        return shutdown.get();
    }
    
    public String getCurrentHost() {
        return currentHost;
    }
    
    public int getCurrentPort() {
        return currentPort;
    }
    
    public Long getWorldId() {
        return worldId;
    }
    
    public int getPendingResponseCount() {
        return pendingResponses.size();
    }
}