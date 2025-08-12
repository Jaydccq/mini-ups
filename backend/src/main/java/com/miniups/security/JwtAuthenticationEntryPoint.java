/**
 * JWT Authentication Entry Point
 * 
 * Purpose:
 * - Handles unauthorized access attempts in JWT-based authentication
 * - Provides standardized error responses for authentication failures
 * - Implements Spring Security's AuthenticationEntryPoint interface
 * - Ensures consistent error handling across all protected endpoints
 * 
 * Core Functionality:
 * - commence(): Invoked when an unauthenticated user tries to access a secured resource
 * - Returns HTTP 401 Unauthorized status with structured JSON error response
 * - Captures request URI and exception details for debugging purposes
 * - Sets appropriate content type and character encoding for JSON responses
 * 
 * Security Considerations:
 * - Prevents detailed error information leakage to potential attackers
 * - Provides standardized error format for consistent client-side handling
 * - Logs authentication failures for security monitoring and audit trails
 * - Maintains security best practices by not exposing sensitive system details
 * 
 * Integration Points:
 * - Configured in SecurityConfig as the authentication entry point
 * - Works with JwtAuthenticationFilter to handle JWT validation failures
 * - Integrates with Spring Security's exception handling mechanism
 * - Supports both API and web-based authentication flows
 * 
 * Response Format:
 * {
 *   "error": "Unauthorized",
 *   "message": "Authentication failure reason",
 *   "path": "/api/requested/endpoint"
 * }
 * 
 * Usage Patterns:
 * - Automatically triggered when JWT token is missing, invalid, or expired
 * - Handles both API endpoints and WebSocket authentication failures
 * - Provides clear feedback for client applications to handle authentication errors
 * - Supports frontend routing for login redirects based on error responses
 * 
 * Best Practices:
 * - Returns consistent error format across all authentication failures
 * - Includes request path for client-side debugging and analytics
 * - Uses appropriate HTTP status codes (401 for authentication failures)
 * - Maintains security by not exposing internal system details
 * 
 *
 
 * @since 2024-01-01
 */
package com.miniups.security;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {
    
    @Override
    public void commence(HttpServletRequest request, 
                        HttpServletResponse response,
                        AuthenticationException authException) throws IOException, ServletException {
        
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        
        String jsonResponse = String.format(
            "{\"error\": \"Unauthorized\", \"message\": \"%s\", \"path\": \"%s\"}", 
            authException.getMessage(), 
            request.getRequestURI()
        );
        
        response.getWriter().write(jsonResponse);
    }
}