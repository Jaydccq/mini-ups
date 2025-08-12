package com.miniups.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Amazon Integration Service Configuration Properties
 * 
 * Defines configuration parameters for integration with Amazon services
 */
@Data
@Component
@ConfigurationProperties(prefix = "amazon")
public class AmazonIntegrationProperties {

    /**
     * Base URL
     */
    private String baseUrl = "http://localhost:8080";

    /**
     * Webhook configuration
     */
    private Webhook webhook = new Webhook();

    /**
     * API configuration
     */
    private Api api = new Api();

    /**
     * Webhook endpoints configuration
     */
    private WebhookEndpoints webhookEndpoints = new WebhookEndpoints();

    @Data
    public static class Webhook {
        /**
         * Webhook secret
         */
        private String secret = "default-webhook-secret-key";

        /**
         * Signature header
         */
        private String signatureHeader = "X-Amazon-Signature";

        /**
         * Authentication configuration
         */
        private Authentication authentication = new Authentication();

        @Data
        public static class Authentication {
            /**
             * Whether authentication is enabled
             */
            private boolean enabled = true;
        }
    }

    @Data
    public static class Api {
        /**
         * API URL
         */
        private String url = "http://localhost:8080";

        /**
         * Timeout (milliseconds)
         */
        private int timeout = 5000;

        /**
         * Whether API integration is enabled
         */
        private boolean enabled = true;
    }

    @Data
    public static class WebhookEndpoints {
        /**
         * Truck arrived endpoint
         */
        private String truckArrived = "/api/webhooks/truck-arrived";

        /**
         * Truck dispatched endpoint
         */
        private String truckDispatched = "/api/webhooks/truck-dispatched";

        /**
         * Shipment delivered endpoint
         */
        private String shipmentDelivered = "/api/webhooks/shipment-delivered";

        /**
         * Shipment detail request endpoint
         */
        private String shipmentDetailRequest = "/api/webhooks/shipment-detail-request";
    }
}