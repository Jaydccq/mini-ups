/**
 * World Simulator Health Indicator
 * 
 * Purpose:
 * - Integrates World Simulator health check with Spring Boot Actuator
 * - Provides standardized health status for monitoring systems
 * - Automatically included in /actuator/health endpoint
 * 
 * Benefits over custom controller:
 * - Standard health check format compatible with monitoring tools
 * - Automatic aggregation with other health indicators
 * - Built-in support for detailed health information
 * - Integration with Spring Boot Admin and other monitoring solutions
 * 
 *
 * @version 2.0.0
 */
package com.miniups.health;

import com.miniups.service.WorldSimulatorService;
import com.miniups.network.netty.service.NettyWorldSimulatorService;
import org.springframework.beans.factory.annotation.Autowired;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

@Component
public class WorldSimulatorHealthIndicator implements HealthIndicator {

    private static final Logger logger = LoggerFactory.getLogger(WorldSimulatorHealthIndicator.class);
    
    private final WorldSimulatorService worldSimulatorService;
    private final NettyWorldSimulatorService nettyWorldSimulatorService;

    public WorldSimulatorHealthIndicator(@Autowired(required = false) WorldSimulatorService worldSimulatorService,
                                       @Autowired(required = false) NettyWorldSimulatorService nettyWorldSimulatorService) {
        this.worldSimulatorService = worldSimulatorService;
        this.nettyWorldSimulatorService = nettyWorldSimulatorService;
    }
    
    /**
     * Helper method to check if any world simulator is connected.
     */
    private boolean isWorldSimulatorConnected() {
        if (worldSimulatorService != null) {
            return worldSimulatorService.isConnected();
        } else if (nettyWorldSimulatorService != null) {
            return nettyWorldSimulatorService.isConnected();
        }
        return false;
    }
    
    /**
     * Helper method to check if world simulator connection is healthy.
     */
    private boolean isWorldSimulatorHealthy() {
        if (worldSimulatorService != null) {
            return worldSimulatorService.isConnectionHealthy();
        } else if (nettyWorldSimulatorService != null) {
            return nettyWorldSimulatorService.isConnected(); // For Netty, connected = healthy
        }
        return false;
    }
    
    /**
     * Helper method to get world ID from the available service.
     */
    private Long getWorldId() {
        if (worldSimulatorService != null) {
            return worldSimulatorService.getWorldId();
        } else if (nettyWorldSimulatorService != null) {
            return nettyWorldSimulatorService.getWorldId();
        }
        return null;
    }
    
    /**
     * Helper method to get implementation type.
     */
    private String getImplementationType() {
        if (worldSimulatorService != null) {
            return "socket";
        } else if (nettyWorldSimulatorService != null) {
            return "netty";
        }
        return "none";
    }

    @Override
    public Health health() {
        try {
            boolean connected = isWorldSimulatorConnected();
            boolean healthy = isWorldSimulatorHealthy();
            
            // Don't fail health check if World Simulator is intentionally not connected
            // This allows the application to start normally without external dependencies
            Health.Builder builder = Health.up();
            
            builder.withDetail("connected", connected)
                   .withDetail("healthy", healthy);
            
            Long worldId = getWorldId();
            if (worldId != null) {
                builder.withDetail("worldId", worldId);
            } else {
                builder.withDetail("worldId", "Not connected");
            }
            
            builder.withDetail("implementation", getImplementationType());
            
            if (connected && healthy) {
                builder.withDetail("message", "World Simulator connection is healthy");
                logger.debug("World Simulator health check: UP - Connected and healthy");
            } else if (connected && !healthy) {
                builder.withDetail("message", "World Simulator is connected but not healthy");
                logger.warn("World Simulator health check: UP - Connected but unhealthy");
            } else {
                builder.withDetail("message", "World Simulator is not connected (optional service)");
                logger.debug("World Simulator health check: UP - Not connected (optional)");
            }
            
            return builder.build();
            
        } catch (Exception e) {
            logger.error("World Simulator health check failed", e);
            return Health.down()
                         .withDetail("connected", false)
                         .withDetail("healthy", false)
                         .withDetail("error", e.getMessage())
                         .withDetail("message", "World Simulator health check failed")
                         .build();
        }
    }
}