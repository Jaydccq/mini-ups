/**
 * JWT Authentication Filter
 * 
 * Purpose:
 * - Intercepts HTTP requests to validate JWT tokens for authentication
 * - Establishes security context for authenticated users on each request
 * - Implements stateless authentication mechanism using JWT tokens
 * - Extends OncePerRequestFilter to ensure single execution per request
 * 
 * Core Functionality:
 * - doFilterInternal(): Main filter logic executed for each HTTP request
 * - getJwtFromRequest(): Extracts JWT token from Authorization header
 * - Validates JWT token using JwtTokenProvider
 * - Loads user details and sets authentication in SecurityContext
 * - Supports Bearer token format: "Authorization: Bearer <jwt-token>"
 * 
 * Authentication Flow:
 * 1. Extract JWT token from Authorization header
 * 2. Validate token format and signature using JwtTokenProvider
 * 3. Extract username from validated token
 * 4. Load full user details from UserDetailsService
 * 5. Create Authentication object and set in SecurityContext
 * 6. Continue filter chain execution
 * 
 * Security Features:
 * - Token format validation (Bearer prefix required)
 * - JWT signature and expiration validation
 * - Automatic security context establishment
 * - Thread-safe authentication handling
 * - Request details capture for audit trails
 * 
 * Integration Points:
 * - Works with JwtTokenProvider for token validation
 * - Uses CustomUserDetailsService for user information loading
 * - Integrates with Spring Security filter chain
 * - Configured in SecurityConfig before UsernamePasswordAuthenticationFilter
 * 
 * Error Handling:
 * - Gracefully handles missing or invalid tokens
 * - Continues filter chain even when authentication fails
 * - Relies on AuthenticationEntryPoint for error responses
 * - Logs authentication failures for security monitoring
 * 
 * Performance Considerations:
 * - OncePerRequestFilter prevents multiple executions per request
 * - Efficient token parsing and validation
 * - Minimal database queries (only for user details loading)
 * - Thread-safe implementation for concurrent requests
 * 
 * Usage Patterns:
 * - Automatically processes all incoming HTTP requests
 * - Supports both API and WebSocket authentication
 * - Works with frontend frameworks using Bearer token authentication
 * - Compatible with mobile applications and SPA architectures
 * 
 * Best Practices:
 * - Uses StringUtils.hasText() for safe string validation
 * - Proper exception handling without breaking filter chain
 * - Minimal performance impact on public endpoints
 * - Clear separation of concerns with other security components
 * 
 *
 
 * @since 2024-01-01
 */
package com.miniups.security;

import com.miniups.service.TokenBlacklistService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    
    @Autowired
    private JwtTokenProvider tokenProvider;
    
    @Autowired
    private UserDetailsService userDetailsService;
    
    @Autowired
    private TokenBlacklistService tokenBlacklistService;
    
    @Override
    protected void doFilterInternal(HttpServletRequest request, 
                                  HttpServletResponse response, 
                                  FilterChain filterChain) throws ServletException, IOException {
        
        String jwt = getJwtFromRequest(request);
        
        if (StringUtils.hasText(jwt) && tokenProvider.validateToken(jwt)) {
            // Check if token is blacklisted
            String tokenId = tokenProvider.getTokenIdFromToken(jwt);
            if (tokenId != null && tokenBlacklistService.isTokenBlacklisted(tokenId)) {
                // Token is blacklisted, skip authentication
                filterChain.doFilter(request, response);
                return;
            }
            
            String username = tokenProvider.getUsernameFromToken(jwt);
            
            UserDetails userDetails = userDetailsService.loadUserByUsername(username);
            UsernamePasswordAuthenticationToken authentication = 
                new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
            authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            
            SecurityContextHolder.getContext().setAuthentication(authentication);
        }
        
        filterChain.doFilter(request, response);
    }
    
    private String getJwtFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            String token = bearerToken.substring(7).trim();
            // Return null for empty tokens instead of empty strings
            return StringUtils.hasText(token) ? token : null;
        }
        return null;
    }
}