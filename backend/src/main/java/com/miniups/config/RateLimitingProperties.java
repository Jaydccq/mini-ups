package com.miniups.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Rate Limiting Configuration Properties
 * 
 * Defines configuration parameters for API request rate limiting
 */
@Data
@Component
@ConfigurationProperties(prefix = "rate-limiting")
public class RateLimitingProperties {

    /**
     * Whether rate limiting is enabled
     */
    private boolean enabled = true;

    /**
     * Shipment creation rate limiting
     */
    private ShipmentCreation shipmentCreation = new ShipmentCreation();

    /**
     * General API rate limiting
     */
    private GeneralApi generalApi = new GeneralApi();

    @Data
    public static class ShipmentCreation {
        /**
         * Requests per minute limit
         */
        private int requestsPerMinute = 10;
    }

    @Data
    public static class GeneralApi {
        /**
         * Requests per minute limit
         */
        private int requestsPerMinute = 60;
    }
}