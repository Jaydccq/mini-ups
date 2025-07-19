/**
 * Authentication Response Data Transfer Object
 * 
 * Functionality:
 * - Encapsulates authentication information returned to client after successful login
 * - Contains JWT access token and basic user information
 * - Provides token type and expiration time information
 * 
 * Return Content:
 * - accessToken: JWT access token
 * - tokenType: Token type (default Bearer)
 * - expiresIn: Token expiration time (seconds)
 * - user: Secure user information (excluding password)
 * 
 * Security Features:
 * - Does not return sensitive information (such as password hash)
 * - Token includes expiration time to prevent permanent validity
 * - Supports token refresh mechanism extension
 * 
 * Usage Scenarios:
 * - Login API response body
 * - Token refresh API response
 * - Frontend authentication state management
 * 
 * @author Mini-UPS Team
 * @version 1.0.0
 */
package com.miniups.model.dto.auth;

import com.miniups.model.dto.user.UserDto;
import com.fasterxml.jackson.annotation.JsonProperty;

public class AuthResponseDto {
    
    @JsonProperty("access_token")
    private String accessToken;
    
    @JsonProperty("token_type")
    private String tokenType = "Bearer";
    
    @JsonProperty("expires_in")
    private Long expiresIn;
    
    private UserDto user;
    
    private String message;
    
    // Constructors
    public AuthResponseDto() {}
    
    public AuthResponseDto(String accessToken, UserDto user) {
        this.accessToken = accessToken;
        this.user = user;
        this.message = "Login successful";
    }
    
    public AuthResponseDto(String accessToken, Long expiresIn, UserDto user) {
        this.accessToken = accessToken;
        this.expiresIn = expiresIn;
        this.user = user;
        this.message = "Login successful";
    }
    
    // Static factory methods
    public static AuthResponseDto success(String token, UserDto user) {
        return new AuthResponseDto(token, user);
    }
    
    public static AuthResponseDto success(String token, Long expiresIn, UserDto user) {
        return new AuthResponseDto(token, expiresIn, user);
    }
    
    public static AuthResponseDto loginSuccess(String token, Long expiresIn, UserDto user) {
        AuthResponseDto response = new AuthResponseDto(token, expiresIn, user);
        response.setMessage("Login successful, welcome back!");
        return response;
    }
    
    public static AuthResponseDto registerSuccess(String token, Long expiresIn, UserDto user) {
        AuthResponseDto response = new AuthResponseDto(token, expiresIn, user);
        response.setMessage("Registration successful, welcome to Mini-UPS!");
        return response;
    }
    
    // Getters and Setters
    public String getAccessToken() {
        return accessToken;
    }
    
    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }
    
    public String getTokenType() {
        return tokenType;
    }
    
    public void setTokenType(String tokenType) {
        this.tokenType = tokenType;
    }
    
    public Long getExpiresIn() {
        return expiresIn;
    }
    
    public void setExpiresIn(Long expiresIn) {
        this.expiresIn = expiresIn;
    }
    
    public UserDto getUser() {
        return user;
    }
    
    public void setUser(UserDto user) {
        this.user = user;
    }
    
    public String getMessage() {
        return message;
    }
    
    public void setMessage(String message) {
        this.message = message;
    }
    
    // Helper methods
    public boolean hasValidToken() {
        return accessToken != null && !accessToken.trim().isEmpty();
    }
    
    public boolean isTokenExpired() {
        if (expiresIn == null) {
            return false; // If no expiration time information, assume valid
        }
        return expiresIn <= 0;
    }
    
    @Override
    public String toString() {
        return "AuthResponseDto{" +
                "tokenType='" + tokenType + '\'' +
                ", expiresIn=" + expiresIn +
                ", user=" + user +
                ", message='" + message + '\'' +
                ", hasToken=" + hasValidToken() +
                '}';
    }
}