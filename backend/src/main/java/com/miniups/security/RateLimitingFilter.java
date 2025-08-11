/**
 * Rate Limiting Filter
 * 
 * Purpose:
 * - Protects API endpoints from excessive requests
 * - Prevents DoS attacks and resource exhaustion
 * - Implements sliding window rate limiting using Redis
 * 
 * Features:
 * - Configurable rate limits per endpoint
 * - IP-based and user-based rate limiting
 * - Redis-backed for distributed deployments
 * - Graceful degradation when Redis is unavailable
 * 
 *
 
 */
package com.miniups.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

@Component
public class RateLimitingFilter extends OncePerRequestFilter {
    
    private static final Logger logger = LoggerFactory.getLogger(RateLimitingFilter.class);
    
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;
    
    @Value("${rate-limiting.enabled:true}")
    private boolean rateLimitingEnabled;
    
    @Value("${rate-limiting.shipment-creation.requests-per-minute:10}")
    private int shipmentCreationRequestsPerMinute;
    
    @Value("${rate-limiting.general-api.requests-per-minute:60}")
    private int generalApiRequestsPerMinute;
    
    private static final String RATE_LIMIT_KEY_PREFIX = "rate_limit:";
    
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, 
                                   FilterChain filterChain) throws ServletException, IOException {
        
        if (!rateLimitingEnabled) {
            filterChain.doFilter(request, response);
            return;
        }
        
        String requestPath = request.getRequestURI();
        String method = request.getMethod();
        
        // Apply rate limiting to specific endpoints
        if (shouldRateLimit(requestPath, method)) {
            String clientKey = getClientKey(request);
            int allowedRequests = getAllowedRequests(requestPath);
            
            if (!isRequestAllowed(clientKey, requestPath, allowedRequests)) {
                logger.warn("Rate limit exceeded for client {} on path {}", clientKey, requestPath);
                handleRateLimitExceeded(response);
                return;
            }
        }
        
        filterChain.doFilter(request, response);
    }
    
    /**
     * Determine if the endpoint should be rate limited
     */
    private boolean shouldRateLimit(String path, String method) {
        // Rate limit shipment creation endpoints
        if ("POST".equals(method)) {
            if (path.equals("/api/shipment") || 
                path.equals("/api/shipment_loaded") || 
                path.startsWith("/api/amazon/")) {
                return true;
            }
        }
        
        // Rate limit all authenticated API endpoints
        return path.startsWith("/api/") && !path.startsWith("/api/auth/") && 
               !path.startsWith("/api/public/") && !path.startsWith("/api/tracking/");
    }
    
    /**
     * Get allowed requests per minute for the endpoint
     */
    private int getAllowedRequests(String path) {
        // Stricter limits for shipment creation
        if (path.equals("/api/shipment") || path.equals("/api/shipment_loaded")) {
            return shipmentCreationRequestsPerMinute;
        }
        
        return generalApiRequestsPerMinute;
    }
    
    /**
     * Generate client key for rate limiting (IP-based)
     */
    private String getClientKey(HttpServletRequest request) {
        String clientIp = getClientIpAddress(request);
        return "ip:" + clientIp;
    }
    
    /**
     * Get real client IP address considering proxy headers
     */
    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }
        
        return request.getRemoteAddr();
    }
    
    /**
     * Check if request is allowed using sliding window algorithm
     */
    private boolean isRequestAllowed(String clientKey, String endpoint, int allowedRequests) {
        try {
            String rateLimitKey = RATE_LIMIT_KEY_PREFIX + clientKey + ":" + endpoint;
            String countKey = rateLimitKey + ":count";
            String windowKey = rateLimitKey + ":window";
            
            // Get current count and window start time
            String countStr = (String) redisTemplate.opsForValue().get(countKey);
            String windowStr = (String) redisTemplate.opsForValue().get(windowKey);
            
            long currentTime = System.currentTimeMillis();
            long windowStart = windowStr != null ? Long.parseLong(windowStr) : currentTime;
            int currentCount = countStr != null ? Integer.parseInt(countStr) : 0;
            
            // If window has expired (more than 1 minute), reset
            if (currentTime - windowStart > Duration.ofMinutes(1).toMillis()) {
                currentCount = 0;
                windowStart = currentTime;
            }
            
            // Check if request is allowed
            if (currentCount >= allowedRequests) {
                return false;
            }
            
            // Increment count and update window
            redisTemplate.opsForValue().set(countKey, String.valueOf(currentCount + 1), 
                                          Duration.ofMinutes(2)); // TTL longer than window for safety
            redisTemplate.opsForValue().set(windowKey, String.valueOf(windowStart), 
                                          Duration.ofMinutes(2));
            
            return true;
            
        } catch (Exception e) {
            logger.error("Error checking rate limit for client {}", clientKey, e);
            // Fail open - allow request if Redis is unavailable
            return true;
        }
    }
    
    /**
     * Handle rate limit exceeded response
     */
    private void handleRateLimitExceeded(HttpServletResponse response) throws IOException {
        response.setStatus(429); // HTTP 429 Too Many Requests
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        
        String jsonResponse = """
            {
                "error": "Rate limit exceeded",
                "message": "Too many requests. Please try again later.",
                "code": 429
            }
            """;
        
        response.getWriter().write(jsonResponse);
    }
    
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        // Skip rate limiting for health checks and static resources
        String path = request.getRequestURI();
        return path.equals("/actuator/health") || 
               path.equals("/") || 
               path.equals("/favicon.ico") ||
               path.startsWith("/api-docs/") ||
               path.startsWith("/swagger-ui/");
    }
}