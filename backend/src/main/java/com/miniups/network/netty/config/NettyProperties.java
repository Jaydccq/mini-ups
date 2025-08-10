package com.miniups.network.netty.config;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Configuration properties for Netty client settings.
 * 
 * This class holds all configurable parameters for the Netty-based
 * World Simulator client, allowing for easy customization across
 * different environments without code changes.
 * 
 * @author Mini-UPS System
 * @version 1.0
 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "world.simulator.netty")
public class NettyProperties {

    /**
     * Number of I/O worker threads for the EventLoopGroup.
     * Default is 2 threads for optimal performance on most systems.
     */
    private int workerThreads = 2;

    /**
     * Connection timeout in milliseconds.
     * How long to wait for initial connection establishment.
     */
    private int connectionTimeoutMs = 10000;

    /**
     * Socket keep-alive setting.
     * Enables TCP keep-alive packets to detect dead connections.
     */
    private boolean keepAlive = true;

    /**
     * TCP no-delay setting.
     * Disables Nagle's algorithm for lower latency.
     */
    private boolean tcpNoDelay = true;

    /**
     * Reconnection settings.
     */
    private final Reconnection reconnection = new Reconnection();

    /**
     * Message timeout settings.
     */
    private final Message message = new Message();

    @Data
    public static class Reconnection {
        /**
         * Whether automatic reconnection is enabled.
         */
        private boolean enabled = true;

        /**
         * Maximum number of reconnection attempts.
         * -1 means infinite attempts.
         */
        private int maxAttempts = 10;

        /**
         * Initial delay between reconnection attempts in milliseconds.
         */
        private long initialDelayMs = 1000;

        /**
         * Maximum delay between reconnection attempts in milliseconds.
         */
        private long maxDelayMs = 30000;

        /**
         * Multiplier for exponential backoff.
         */
        private double backoffMultiplier = 2.0;
    }

    @Data
    public static class Message {
        /**
         * Timeout for individual message responses in milliseconds.
         */
        private long responseTimeoutMs = 30000;

        /**
         * Maximum size of pending response queue.
         */
        private int maxPendingResponses = 1000;
    }
}