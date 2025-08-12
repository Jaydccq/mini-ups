package com.miniups.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * World Simulator Configuration Properties
 * 
 * Defines configuration parameters for integration with the world simulator service
 */
@Data
@Component
@ConfigurationProperties(prefix = "world")
public class WorldSimulatorProperties {

    /**
     * Simulator configuration
     */
    private Simulator simulator = new Simulator();

    @Data
    public static class Simulator {
        /**
         * Simulator host address
         */
        private String host = "localhost";

        /**
         * Simulator port
         */
        private int port = 12345;

        /**
         * Connection timeout (milliseconds)
         */
        private int connectionTimeout = 10000;

        /**
         * Read timeout (milliseconds)
         */
        private int readTimeout = 10000;

        /**
         * Default simulation speed
         */
        private int defaultSimSpeed = 1000;

        /**
         * Whether simulator integration is enabled
         */
        private boolean enabled = true;
    }
}