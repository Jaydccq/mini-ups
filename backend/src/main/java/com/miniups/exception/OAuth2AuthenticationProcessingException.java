package com.miniups.exception;

import org.springframework.security.core.AuthenticationException;

/**
 * OAuth2 Authentication Processing Exception
 * 
 * Thrown when an error occurs during OAuth2 authentication processing,
 * such as missing required user information from the OAuth2 provider
 * or account linking conflicts.
 */
public class OAuth2AuthenticationProcessingException extends AuthenticationException {
    
    public OAuth2AuthenticationProcessingException(String message) {
        super(message);
    }
    
    public OAuth2AuthenticationProcessingException(String message, Throwable cause) {
        super(message, cause);
    }
}