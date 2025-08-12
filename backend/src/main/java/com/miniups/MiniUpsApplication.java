/**
 * Mini-UPS Backend Application Main Startup Class
 * 
 * Spring Boot-based distributed logistics system backend service, providing:
 * - User authentication and permission management
 * - Order and package management
 * - Vehicle dispatching and route optimization
 * - Real-time tracking and status updates
 * - Amazon platform integration
 * - World simulator integration
 * 
 * @author Mini-UPS Development Team
 
 * @since 2024
 */
package com.miniups;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableAsync
@EnableScheduling
@EnableRetry
@ConfigurationPropertiesScan
public class MiniUpsApplication {
    
    public static void main(String[] args) {
        SpringApplication.run(MiniUpsApplication.class, args);
    }
}
