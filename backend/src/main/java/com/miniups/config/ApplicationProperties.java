package com.miniups.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Application Custom Configuration Properties
 * 
 * Used to define and validate custom configuration properties for the application,
 * resolving IDE warnings about custom configurations.
 */
@Data
@Component
@ConfigurationProperties(prefix = "app")
public class ApplicationProperties {

    /**
     * CORS configuration
     */
    private Cors cors = new Cors();

    /**
     * RabbitMQ configuration
     */
    private RabbitMQ rabbitmq = new RabbitMQ();

    /**
     * Notifications configuration
     */
    private Notifications notifications = new Notifications();

    @Data
    public static class Cors {
        /**
         * List of allowed origins
         */
        private String allowedOrigins = "http://localhost:3000";
    }

    @Data
    public static class RabbitMQ {
        /**
         * Whether RabbitMQ functionality is enabled
         */
        private boolean enabled = true;
        
        /**
         * Test configuration
         */
        private Test test = new Test();

        @Data
        public static class Test {
            /**
             * Whether test mode is enabled (using mocks)
             */
            private boolean enabled = true;
        }
    }

    @Data
    public static class Notifications {
        /**
         * Whether notifications are enabled
         */
        private boolean enabled = true;

        /**
         * Email notification configuration
         */
        private Email email = new Email();

        /**
         * SMS notification configuration
         */
        private Sms sms = new Sms();

        /**
         * Push notification configuration
         */
        private Push push = new Push();

        @Data
        public static class Email {
            private boolean enabled = true;
        }

        @Data
        public static class Sms {
            private boolean enabled = false;
        }

        @Data
        public static class Push {
            private boolean enabled = false;
        }
    }
}