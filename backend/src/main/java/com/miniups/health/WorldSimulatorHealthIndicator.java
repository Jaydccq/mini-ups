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
 * @author Mini-UPS Team
 * @version 2.0.0
 */
package com.miniups.health;

import com.miniups.service.WorldSimulatorService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

@Component
public class WorldSimulatorHealthIndicator implements HealthIndicator {

    private static final Logger logger = LoggerFactory.getLogger(WorldSimulatorHealthIndicator.class);
    
    private final WorldSimulatorService worldSimulatorService;

    public WorldSimulatorHealthIndicator(WorldSimulatorService worldSimulatorService) {
        this.worldSimulatorService = worldSimulatorService;
    }

    @Override
    public Health health() {
        try {
            boolean connected = worldSimulatorService.isConnected();
            boolean healthy = worldSimulatorService.isConnectionHealthy();
            
            Health.Builder builder = healthy ? Health.up() : Health.down();
            
            builder.withDetail("connected", connected)
                   .withDetail("healthy", healthy);
            
            Long worldId = worldSimulatorService.getWorldId();
            if (worldId != null) {
                builder.withDetail("worldId", worldId);
            } else {
                builder.withDetail("worldId", "Not connected");
            }
            
            if (healthy) {
                builder.withDetail("message", "World Simulator connection is healthy");
                logger.debug("World Simulator health check: UP");
            } else {
                builder.withDetail("message", "World Simulator connection is not healthy");
                logger.warn("World Simulator health check: DOWN");
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