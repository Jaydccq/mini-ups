/**
 * Shipment Creation Exception
 * 
 * Feature description:
 * - Specifically handles business exceptions during shipment creation
 * - Provides detailed error information and context
 * - Facilitates exception categorization and handling
 * 
 * Usage scenarios:
 * - Shipment data validation failure
 * - Truck assignment failure
 * - User creation failure
 * - Insufficient system resources
 * 
 *
 
 */
package com.miniups.exception;

import com.miniups.model.enums.ExceptionSeverity;

public class ShipmentCreationException extends BaseBusinessException {
    
    private static final String DEFAULT_MESSAGE = "Failed to create shipment";
    private static final String ERROR_CODE = "SHIPMENT_CREATION_ERROR";
    
    /**
     * Create shipment creation exception
     * 
     * @param message Exception message
     */
    public ShipmentCreationException(String message) {
        super(ERROR_CODE, message, ExceptionSeverity.MEDIUM);
    }
    
    /**
     * Create shipment creation exception with cause
     * 
     * @param message Exception message
     * @param cause Original exception
     */
    public ShipmentCreationException(String message, Throwable cause) {
        super(ERROR_CODE, message, ExceptionSeverity.MEDIUM, cause);
    }
    
    /**
     * Create exception with default message
     * 
     * @param cause Original exception
     */
    public ShipmentCreationException(Throwable cause) {
        super(ERROR_CODE, DEFAULT_MESSAGE, ExceptionSeverity.MEDIUM, cause);
    }
    
    /**
     * Create exception with customer information
     * 
     * @param customerName Customer name
     * @param cause Original exception
     * @return Shipment creation exception
     */
    public static ShipmentCreationException forCustomer(String customerName, Throwable cause) {
        String message = String.format("Failed to create shipment for customer '%s'", customerName);
        return new ShipmentCreationException(message, cause);
    }
    
    /**
     * Create exception for truck assignment failure
     * 
     * @param trackingNumber Tracking number
     * @return Shipment creation exception
     */
    public static ShipmentCreationException truckAssignmentFailed(String trackingNumber) {
        String message = String.format("Failed to assign truck for shipment '%s'", trackingNumber);
        return new ShipmentCreationException(message);
    }
    
    /**
     * Create exception for user creation failure
     * 
     * @param customerEmail Customer email
     * @param cause Original exception
     * @return Shipment creation exception
     */
    public static ShipmentCreationException userCreationFailed(String customerEmail, Throwable cause) {
        String message = String.format("Failed to create user for customer email '%s'", customerEmail);
        return new ShipmentCreationException(message, cause);
    }
}