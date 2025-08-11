package com.miniups.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * JWT Configuration Properties
 * 
 * Defines configuration parameters related to JWT tokens
 */
@Data
@Component
@ConfigurationProperties(prefix = "jwt")
public class JwtProperties {

    /**
     * JWT secret key
     */
    private String secret;

    /**
     * Token expiration time (milliseconds)
     */
    private long expiration = 86400000L; // 24 hours
}