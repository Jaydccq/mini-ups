package com.miniups.worldsim.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;

/**
 * Configuration properties for the World Simulator application.
 * 
 * This class centralizes all configuration settings for the World Simulator,
 * including network settings, simulation parameters, and performance tuning options.
 * It uses Spring Boot's @ConfigurationProperties for type-safe configuration
 * binding and validation.
 * 
 * The configuration is organized into logical sections:
 * - Network: TCP server ports, flakiness, connection management
 * - Simulation: Tick intervals, speeds, timing parameters
 * - Performance: Batching, caching, and optimization settings
 * 
 * @author World Simulator Team
 * @version 1.0.0
 * @since 2025-01-13
 */
@ConfigurationProperties(prefix = "world-sim")
@Validated
public record WorldSimProperties(
    @Valid Network network,
    @Valid Simulation simulation,
    @Valid Performance performance
) {
    
    /**
     * Network-related configuration properties.
     * 
     * Controls TCP server behavior, connection management, and network reliability features.
     */
    public record Network(
        @Min(1024)
        @Max(65535)
        int upsPort,
        
        @Min(1024)
        @Max(65535)
        int amazonPort,
        
        @Min(0)
        @Max(99)
        int flakiness,
        
        @Positive
        long ackTimeoutMs,
        
        @Min(1)
        @Max(10)
        int maxRetries,
        
        @Positive
        int maxConnections,
        
        @Positive
        long connectionIdleTimeoutMs
    ) {
        public Network {
            // Validation: UPS and Amazon ports must be different
            if (upsPort == amazonPort) {
                throw new IllegalArgumentException("UPS port and Amazon port must be different");
            }
        }
    }
    
    /**
     * Simulation engine configuration properties.
     * 
     * Controls the behavior of the real-time simulation including timing,
     * movement speeds, and update frequencies.
     */
    public record Simulation(
        @Min(10)
        @Max(1000)
        long tickIntervalMs,
        
        @Positive
        int truckSpeedUnitsPerSec,
        
        @Min(100)
        @Max(10000)
        long updateBroadcastIntervalMs,
        
        @Min(1000)
        @Max(60000)
        long warehouseLoadingTimeMs
    ) {}
    
    /**
     * Performance optimization configuration properties.
     * 
     * Controls caching, batching, and other performance-related settings.
     */
    public record Performance(
        @Min(1)
        @Max(1000)
        int databaseBatchSize,
        
        @Min(60)
        @Max(3600)
        long cacheTtlSeconds,
        
        boolean metricsEnabled
    ) {}
}