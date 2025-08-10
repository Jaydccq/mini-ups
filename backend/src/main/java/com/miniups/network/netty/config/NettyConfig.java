package com.miniups.network.netty.config;

import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import jakarta.annotation.PreDestroy;

/**
 * Spring configuration class for Netty components.
 * 
 * This configuration class sets up the Netty infrastructure beans
 * including EventLoopGroup for I/O operations. It follows Spring Boot's
 * auto-configuration patterns and provides proper lifecycle management.
 * 
 * The Netty client implementation is conditionally enabled through
 * configuration properties, allowing for easy switching between
 * implementations during migration.
 * 
 * @author Mini-UPS System
 * @version 1.0
 */
@Configuration
@EnableConfigurationProperties(NettyProperties.class)
@ConditionalOnProperty(
    name = "world.simulator.client.type", 
    havingValue = "netty", 
    matchIfMissing = false
)
public class NettyConfig {

    private static final Logger log = LoggerFactory.getLogger(NettyConfig.class);

    private final NettyProperties nettyProperties;
    private EventLoopGroup workerGroup;

    public NettyConfig(NettyProperties nettyProperties) {
        this.nettyProperties = nettyProperties;
        log.info("NettyConfig initialized with {} worker threads", 
                nettyProperties.getWorkerThreads());
    }

    /**
     * Creates and configures the EventLoopGroup for Netty I/O operations.
     * 
     * The EventLoopGroup manages the I/O threads and handles all network
     * operations asynchronously. Using NioEventLoopGroup for optimal
     * performance on modern systems.
     * 
     * @return configured EventLoopGroup instance
     */
    @Bean(destroyMethod = "shutdownGracefully")
    public EventLoopGroup eventLoopGroup() {
        log.info("Creating NioEventLoopGroup with {} threads", 
                nettyProperties.getWorkerThreads());
        
        this.workerGroup = new NioEventLoopGroup(nettyProperties.getWorkerThreads());
        return this.workerGroup;
    }

    /**
     * Gracefully shutdown the EventLoopGroup on application shutdown.
     * 
     * This ensures all pending I/O operations are completed and resources
     * are properly released when the application stops.
     */
    @PreDestroy
    public void shutdown() {
        if (workerGroup != null && !workerGroup.isShutdown()) {
            log.info("Shutting down Netty EventLoopGroup gracefully");
            try {
                workerGroup.shutdownGracefully().sync();
                log.info("Netty EventLoopGroup shutdown completed");
            } catch (InterruptedException e) {
                log.warn("Interrupted while shutting down EventLoopGroup", e);
                Thread.currentThread().interrupt();
            }
        }
    }
}