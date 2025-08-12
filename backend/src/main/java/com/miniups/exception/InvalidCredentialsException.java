/**
 * Invalid Credentials Exception
 * 
 * Thrown when user provides invalid login credentials
 * 
 *
 
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