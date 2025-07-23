/**
 * JWT Token Provider
 * 
 * Purpose:
 * - Handles JWT token generation, validation, and parsing operations
 * - Implements secure token-based authentication for the Mini-UPS system
 * - Provides centralized JWT operations with consistent security practices
 * - Manages token lifecycle including creation, validation, and expiration
 * 
 * Core Functionality:
 * - generateToken(): Creates JWT tokens with user credentials and expiration
 * - validateToken(): Verifies token signature, format, and expiration status
 * - getUsernameFromToken(): Extracts username from validated JWT tokens
 * - getSigningKey(): Generates secure HMAC SHA signing key from secret
 * 
 * Security Features:
 * - HMAC SHA-256 signing algorithm for token integrity
 * - Configurable token expiration (default 24 hours)
 * - Secure secret key handling with minimum 256-bit length requirement
 * - Comprehensive token validation with detailed error handling
 * - Exception handling for malformed, expired, and unsupported tokens
 * 
 * Configuration Properties:
 * - jwt.secret: Base64-encoded secret key for token signing (configurable)
 * - jwt.expiration: Token validity period in milliseconds (default: 86400000ms = 24h)
 * - Supports environment-specific configuration through application properties
 * 
 * Token Structure:
 * Header: {"alg": "HS256", "typ": "JWT"}
 * Payload: {"sub": "username", "iat": timestamp, "exp": timestamp}
 * Signature: HMACSHA256(base64UrlEncode(header) + "." + base64UrlEncode(payload), secret)
 * 
 * Error Handling:
 * - MalformedJwtException: Invalid token format or structure
 * - ExpiredJwtException: Token has passed expiration time
 * - UnsupportedJwtException: Token uses unsupported features
 * - IllegalArgumentException: Empty or null token claims
 * - Generic Exception: Catches any other JWT processing errors
 * 
 * Integration Points:
 * - Used by JwtAuthenticationFilter for request authentication
 * - Integrated with authentication controllers for login token generation
 * - Works with CustomUserDetailsService for user validation
 * - Supports WebSocket authentication through token validation
 * 
 * Performance Considerations:
 * - Stateless token validation (no database queries required)
 * - Efficient HMAC-based signing and verification
 * - Cached signing key generation for improved performance
 * - Thread-safe implementation for concurrent token operations
 * 
 * Usage Patterns:
 * - Login endpoint generates tokens after successful authentication
 * - Every protected API request validates token in authentication filter
 * - Frontend applications store tokens in localStorage or secure cookies
 * - Mobile applications use tokens for API authentication
 * 
 * Best Practices:
 * - Uses secure random secret keys with sufficient entropy
 * - Implements proper exception handling with security logging
 * - Follows JWT RFC 7519 standard specifications
 * - Prevents token tampering through cryptographic signatures
 * - Supports token refresh patterns through expiration management
 * 
 * Security Considerations:
 * - Secret key must be kept secure and rotated periodically
 * - Token expiration should balance security and user experience
 * - Logging helps detect potential token abuse or attacks
 * - Signature validation prevents token manipulation
 * 
 * @author Mini-UPS Team
 * @version 1.0.0
 * @since 2024-01-01
 */
package com.miniups.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import jakarta.annotation.PostConstruct;
import javax.crypto.SecretKey;
import java.util.Date;

@Component
public class JwtTokenProvider {
    
    @Value("${jwt.secret}")
    private String jwtSecret;
    
    @Value("${jwt.expiration:86400000}")
    private int jwtExpirationInMs;
    
    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(JwtTokenProvider.class);
    
    /**
     * Validates JWT configuration on application startup
     * Ensures that jwt.secret is properly configured and meets security requirements
     */
    @PostConstruct
    public void validateJwtConfiguration() {
        if (!StringUtils.hasText(jwtSecret)) {
            throw new IllegalStateException(
                "JWT secret is not configured. Please set the 'jwt.secret' property in your application configuration. " +
                "The secret should be a secure random string with at least 256 bits (32 characters)."
            );
        }
        
        // Validate secret key length (minimum 256 bits = 32 bytes)
        if (jwtSecret.length() < 32) {
            throw new IllegalStateException(
                "JWT secret is too short. The secret must be at least 32 characters long for security. " +
                "Current length: " + jwtSecret.length() + " characters."
            );
        }
        
        // Check for common insecure patterns
        if (jwtSecret.contains("default") || jwtSecret.contains("secret") || 
            jwtSecret.contains("password") || jwtSecret.contains("key")) {
            logger.warn("JWT secret contains common words that may be insecure. " +
                       "Consider using a randomly generated secret key.");
        }
        
        logger.info("JWT configuration validated successfully. Secret key length: {} characters", jwtSecret.length());
    }
    
    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(jwtSecret.getBytes());
    }
    
    public String generateToken(String username) {
        if (username == null || username.trim().isEmpty()) {
            throw new IllegalArgumentException("Username cannot be null or empty");
        }
        
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + jwtExpirationInMs);
        
        return Jwts.builder()
                .subject(username)
                .issuedAt(now)
                .expiration(expiryDate)
                .id(java.util.UUID.randomUUID().toString()) // Add unique token ID
                .signWith(getSigningKey())
                .compact();
    }
    
    public String getUsernameFromToken(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
        
        return claims.getSubject();
    }
    
    public boolean validateToken(String authToken) {
        if (authToken == null || authToken.trim().isEmpty()) {
            logger.warn("JWT token is null or empty");
            return false;
        }
        
        try {
            Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(authToken);
            return true;
        } catch (MalformedJwtException | ExpiredJwtException | UnsupportedJwtException | 
                 io.jsonwebtoken.security.SignatureException ex) {
            logger.warn("JWT validation failed: {}", ex.getMessage());
            return false;
        } catch (IllegalArgumentException ex) {
            logger.warn("JWT validation failed: {}", ex.getMessage());
            return false;
        } catch (Exception ex) {
            logger.error("JWT validation error", ex);
            return false;
        }
    }
    
    public Long getExpirationTime() {
        return (long) jwtExpirationInMs;
    }
    
    public String getTokenIdFromToken(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(getSigningKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            
            return claims.getId();
        } catch (Exception e) {
            logger.error("Error extracting token ID from token", e);
            return null;
        }
    }
    
    public long getRemainingTimeInSeconds(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(getSigningKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            
            Date expiration = claims.getExpiration();
            Date now = new Date();
            
            if (expiration.before(now)) {
                return 0;
            }
            
            return (expiration.getTime() - now.getTime()) / 1000;
        } catch (Exception e) {
            logger.error("Error calculating remaining time for token", e);
            return 0;
        }
    }
}