package com.miniups.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * HTTP Client Configuration Properties
 * 
 * Defines configuration parameters related to the HTTP client
 */
@Data
@Component
@ConfigurationProperties(prefix = "http")
public class HttpClientProperties {

    /**
     * Client configuration
     */
    private Client client = new Client();

    @Data
    public static class Client {
        /**
         * Maximum total connections
         */
        private int maxTotalConnections = 100;

        /**
         * Default maximum connections per route
         */
        private int maxDefaultPerRoute = 20;

        /**
         * Connection timeout (milliseconds)
         */
        private int connectTimeoutMs = 10000;

        /**
         * Socket timeout (milliseconds)
         */
        private int socketTimeoutMs = 30000;
    }
}