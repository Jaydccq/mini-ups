package com.miniups.model.enums;

/**
 * Authentication Provider Enumeration
 * 
 * Defines the different authentication providers supported by the system.
 * Used to track how users authenticate and manage account linking security.
 * 
 * Values:
 * - LOCAL: Traditional username/password authentication
 * - GOOGLE: Google OAuth2 authentication
 */
public enum AuthProvider {
    /**
     * Traditional local authentication using username and password
     */
    LOCAL,
    
    /**
     * Google OAuth2 authentication
     */
    GOOGLE
}