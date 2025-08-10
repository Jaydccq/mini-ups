package com.miniups.network.netty.handler;

import com.miniups.network.netty.client.NettyClient;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Netty handler for automatic reconnection functionality.
 * 
 * This handler monitors the channel state and automatically attempts to
 * reconnect when the connection is lost. It implements exponential backoff
 * to avoid overwhelming the server with reconnection attempts.
 * 
 * Key features:
 * - Automatic reconnection on connection loss
 * - Exponential backoff with configurable parameters
 * - Maximum retry limit to prevent infinite attempts
 * - Integration with NettyClient for connection management
 * 
 * The reconnection logic is triggered by the channelInactive event,
 * which is fired when the connection is closed for any reason.
 * 
 * @author Mini-UPS System
 * @version 1.0
 */
public class ReconnectionHandler extends ChannelInboundHandlerAdapter {

    private static final Logger log = LoggerFactory.getLogger(ReconnectionHandler.class);

    private final NettyClient nettyClient;
    private final AtomicInteger reconnectionAttempts = new AtomicInteger(0);
    
    // Reconnection configuration
    private static final int MAX_RECONNECTION_ATTEMPTS = 10;
    private static final long INITIAL_DELAY_MS = 1000L;      // 1 second
    private static final long MAX_DELAY_MS = 30000L;         // 30 seconds
    private static final double BACKOFF_MULTIPLIER = 2.0;
    
    // State tracking
    private volatile boolean reconnectionInProgress = false;

    public ReconnectionHandler(NettyClient nettyClient) {
        this.nettyClient = nettyClient;
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        log.warn("Channel became inactive: {}", ctx.channel().remoteAddress());
        
        // Only attempt reconnection if we're not shutting down
        if (!nettyClient.isShutdown() && shouldAttemptReconnection()) {
            scheduleReconnection(ctx);
        } else {
            log.info("Skipping reconnection attempt - client shutdown: {}, max attempts reached: {}", 
                    nettyClient.isShutdown(), 
                    reconnectionAttempts.get() >= MAX_RECONNECTION_ATTEMPTS);
        }
        
        super.channelInactive(ctx);
    }

    /**
     * Determine if we should attempt reconnection based on current state.
     */
    private boolean shouldAttemptReconnection() {
        return !reconnectionInProgress && 
               reconnectionAttempts.get() < MAX_RECONNECTION_ATTEMPTS;
    }

    /**
     * Schedule a reconnection attempt with exponential backoff.
     */
    private void scheduleReconnection(ChannelHandlerContext ctx) {
        if (reconnectionInProgress) {
            log.debug("Reconnection already in progress, skipping");
            return;
        }
        
        reconnectionInProgress = true;
        int attemptNumber = reconnectionAttempts.incrementAndGet();
        long delay = calculateDelay(attemptNumber);
        
        log.warn("Scheduling reconnection attempt {} in {} ms", attemptNumber, delay);
        
        // Use the channel's EventLoop to schedule the reconnection
        ctx.channel().eventLoop().schedule(() -> {
            attemptReconnection(ctx, attemptNumber);
        }, delay, TimeUnit.MILLISECONDS);
    }

    /**
     * Calculate the delay for the next reconnection attempt using exponential backoff.
     */
    private long calculateDelay(int attemptNumber) {
        long delay = (long) (INITIAL_DELAY_MS * Math.pow(BACKOFF_MULTIPLIER, attemptNumber - 1));
        return Math.min(delay, MAX_DELAY_MS);
    }

    /**
     * Attempt to reconnect to the World Simulator.
     */
    private void attemptReconnection(ChannelHandlerContext ctx, int attemptNumber) {
        if (nettyClient.isShutdown()) {
            log.info("Client is shutting down, cancelling reconnection attempt {}", attemptNumber);
            reconnectionInProgress = false;
            return;
        }
        
        String host = nettyClient.getCurrentHost();
        int port = nettyClient.getCurrentPort();
        Long worldId = nettyClient.getWorldId();
        
        if (host == null || worldId == null) {
            log.error("Cannot reconnect - missing connection details (host: {}, worldId: {})", host, worldId);
            reconnectionInProgress = false;
            return;
        }
        
        log.info("Attempting reconnection {} to World Simulator at {}:{} with worldId={}", 
                attemptNumber, host, port, worldId);
        
        // Attempt reconnection through the NettyClient
        nettyClient.connect(host, port, worldId)
            .whenComplete((result, throwable) -> {
                reconnectionInProgress = false;
                
                if (throwable == null) {
                    // Reconnection successful
                    log.info("Reconnection attempt {} succeeded to {}:{}", attemptNumber, host, port);
                    reconnectionAttempts.set(0); // Reset counter on successful connection
                } else {
                    // Reconnection failed
                    log.error("Reconnection attempt {} failed to {}:{}: {}", 
                            attemptNumber, host, port, throwable.getMessage());
                    
                    // If we haven't exceeded max attempts, the next channelInactive event
                    // will trigger another reconnection attempt
                    if (reconnectionAttempts.get() >= MAX_RECONNECTION_ATTEMPTS) {
                        log.error("Maximum reconnection attempts ({}) exceeded. Giving up reconnection to {}:{}", 
                                MAX_RECONNECTION_ATTEMPTS, host, port);
                    }
                }
            });
    }

    /**
     * Reset the reconnection state when a successful connection is established.
     * This method can be called externally when a connection is manually established.
     */
    public void resetReconnectionState() {
        reconnectionAttempts.set(0);
        reconnectionInProgress = false;
        log.debug("Reconnection state reset");
    }

    /**
     * Get the current number of reconnection attempts.
     */
    public int getReconnectionAttempts() {
        return reconnectionAttempts.get();
    }

    /**
     * Check if a reconnection is currently in progress.
     */
    public boolean isReconnectionInProgress() {
        return reconnectionInProgress;
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        log.error("Exception in reconnection handler: {}", cause.getMessage(), cause);
        
        // Don't close the channel here - let other handlers decide
        // But log the exception for debugging
        super.exceptionCaught(ctx, cause);
    }
}