/**
 * Application Constants
 * 
 * Function Description:
 * - Defines system-wide constants
 * - Avoids magic numbers and hard-coded values
 * - Improves code maintainability and readability
 * 
 * Constant Categories:
 * - Business rule constants
 * - System limit constants
 * - Default value constants
 * - Configuration-related constants
 * 
 *
 
 */
package com.miniups.util;

public final class Constants {
    
    // Prevent instantiation
    private Constants() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }
    
    // ==================== Coordinate System Constants ====================
    
    /**
     * Maximum X coordinate value in the world coordinate system
     */
    public static final int MAX_COORDINATE_X = 1000;
    
    /**
     * Maximum Y coordinate value in the world coordinate system
     */
    public static final int MAX_COORDINATE_Y = 1000;
    
    /**
     * Minimum coordinate value in the world coordinate system
     */
    public static final int MIN_COORDINATE = 0;
    
    // ==================== Shipment Related Constants ====================
    
    /**
     * Default package count
     */
    public static final int DEFAULT_PACKAGE_COUNT = 1;
    
    /**
     * Maximum shipment ID length
     */
    public static final int MAX_SHIPMENT_ID_LENGTH = 50;
    
    /**
     * Maximum customer name length
     */
    public static final int MAX_CUSTOMER_NAME_LENGTH = 100;
    
    /**
     * Maximum special instructions length
     */
    public static final int MAX_SPECIAL_INSTRUCTIONS_LENGTH = 500;
    
    // ==================== Package Weight Constants ====================
    
    /**
     * Minimum package weight (kg)
     */
    public static final double MIN_PACKAGE_WEIGHT = 0.1;
    
    /**
     * Maximum package weight (kg)
     */
    public static final double MAX_PACKAGE_WEIGHT = 50.0;
    
    // ==================== User Related Constants ====================
    
    /**
     * Maximum username length
     */
    public static final int MAX_USERNAME_LENGTH = 50;
    
    /**
     * Minimum username length
     */
    public static final int MIN_USERNAME_LENGTH = 3;
    
    /**
     * Minimum password length
     */
    public static final int MIN_PASSWORD_LENGTH = 8;
    
    /**
     * Maximum password length
     */
    public static final int MAX_PASSWORD_LENGTH = 128;
    
    // ==================== Business Rule Constants ====================
    
    /**
     * Tracking number length
     */
    public static final int TRACKING_NUMBER_LENGTH = 12;
    
    /**
     * Tracking number prefix
     */
    public static final String TRACKING_NUMBER_PREFIX = "UPS";
    
    /**
     * Default shipping priority
     */
    public static final String DEFAULT_PRIORITY = "STANDARD";
    
    // ==================== Cache Related Constants ====================
    
    /**
     * JWT token cache expiration time (seconds)
     */
    public static final long JWT_TOKEN_CACHE_EXPIRY = 3600; // 1 hour
    
    /**
     * User session cache expiration time (seconds)
     */
    public static final long USER_SESSION_CACHE_EXPIRY = 1800; // 30 minutes
    
    // ==================== System Limit Constants ====================
    
    /**
     * Default page size for pagination queries
     */
    public static final int DEFAULT_PAGE_SIZE = 20;
    
    /**
     * Maximum page size for pagination queries
     */
    public static final int MAX_PAGE_SIZE = 100;
    
    /**
     * Maximum retry attempts for concurrent processing
     */
    public static final int MAX_RETRY_ATTEMPTS = 3;
    
    /**
     * Database connection timeout (milliseconds)
     */
    public static final int DATABASE_CONNECTION_TIMEOUT = 30000; // 30 seconds
    
    // ==================== Regular Expression Constants ====================
    
    /**
     * Email validation regular expression
     */
    public static final String EMAIL_REGEX = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$";
    
    /**
     * Phone number validation regular expression
     */
    public static final String PHONE_REGEX = "^[\\+]?[1-9][\\d\\s\\-\\(\\)]{7,15}$";
    
    /**
     * Shipping priority validation regular expression
     */
    public static final String PRIORITY_REGEX = "STANDARD|EXPRESS|OVERNIGHT";
    
    // ==================== Error Message Constants ====================
    
    /**
     * Generic business error message
     */
    public static final String GENERIC_BUSINESS_ERROR = "A business error occurred while processing your request";
    
    /**
     * User not found error message
     */
    public static final String USER_NOT_FOUND_ERROR = "User not found";
    
    /**
     * Shipment not found error message
     */
    public static final String SHIPMENT_NOT_FOUND_ERROR = "Shipment not found";
    
    /**
     * Invalid credentials error message
     */
    public static final String INVALID_CREDENTIALS_ERROR = "Invalid username or password";
}