/**
 * Invalid Credentials Exception
 * 
 * Thrown when user provides invalid login credentials
 * 
 * @author Mini-UPS Team
 * @version 1.0.0
 */
package com.miniups.exception;

import com.miniups.model.enums.ExceptionSeverity;

public class InvalidCredentialsException extends BaseBusinessException {
    
    public InvalidCredentialsException() {
        super("INVALID_CREDENTIALS", "Invalid username or password", ExceptionSeverity.LOW);
    }
    
    public InvalidCredentialsException(String message) {
        super("INVALID_CREDENTIALS", message, ExceptionSeverity.LOW);
    }
}