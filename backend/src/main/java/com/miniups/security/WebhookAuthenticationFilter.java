/**
 * Webhook Authentication Filter
 * 
 * Purpose:
 * - Validates Amazon webhook requests using signature verification
 * - Prevents unauthorized webhook calls from external sources
 * - Ensures webhook integrity and authenticity
 * 
 * Security Features:
 * - Signature-based authentication using shared secret
 * - Request body validation to prevent tampering
 * - Configurable secret key for different environments
 * - Comprehensive logging for security monitoring
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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

@Component
public class WebhookAuthenticationFilter extends OncePerRequestFilter {
    
    private static final Logger logger = LoggerFactory.getLogger(WebhookAuthenticationFilter.class);
    
    @Value("${amazon.webhook.secret:default-webhook-secret-key}")
    private String webhookSecret;
    
    @Value("${amazon.webhook.signature-header:X-Amazon-Signature}")
    private String signatureHeader;
    
    @Value("${amazon.webhook.authentication.enabled:true}")
    private boolean webhookAuthEnabled;
    
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, 
                                   FilterChain filterChain) throws ServletException, IOException {
        
        String requestPath = request.getRequestURI();
        
        // Only apply to webhook endpoints
        if (!requestPath.startsWith("/api/webhooks/") && 
            !requestPath.equals("/api/shipment") && 
            !requestPath.equals("/api/shipment_loaded") && 
            !requestPath.equals("/api/shipment_status") && 
            !requestPath.equals("/api/address_change")) {
            filterChain.doFilter(request, response);
            return;
        }
        
        // Skip authentication if disabled (for development)
        if (!webhookAuthEnabled) {
            logger.warn("Webhook authentication is disabled - allowing request to {}", requestPath);
            filterChain.doFilter(request, response);
            return;
        }
        
        // Wrap the request to allow multiple reads of the body
        ContentCachingRequestWrapper wrappedRequest = new ContentCachingRequestWrapper(request);
        
        try {
            // Get signature from header
            String signature = request.getHeader(signatureHeader);
            if (signature == null || signature.isEmpty()) {
                logger.warn("Missing signature header for webhook request to {}", requestPath);
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.getWriter().write("{\"error\":\"Missing signature header\"}");
                return;
            }
            
            // Read the request body to cache it
            String requestBody = getRequestBody(wrappedRequest);
            
            // Validate signature before processing
            if (!validateSignature(requestBody, signature)) {
                logger.warn("Invalid signature for webhook request to {} from {}", 
                           requestPath, request.getRemoteAddr());
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.getWriter().write("{\"error\":\"Invalid signature\"}");
                return;
            }
            
            logger.debug("Webhook authentication successful for {}", requestPath);
            
            // Continue with the filter chain using the wrapped request
            filterChain.doFilter(wrappedRequest, response);
            
        } catch (Exception e) {
            logger.error("Error during webhook authentication for {}", requestPath, e);
            if (!response.isCommitted()) {
                response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                response.getWriter().write("{\"error\":\"Authentication error\"}");
            }
        }
    }
    
    /**
     * Read request body and cache it for later use
     */
    private String getRequestBody(ContentCachingRequestWrapper request) throws IOException {
        // Read the input stream once to cache it
        request.getInputStream().readAllBytes();
        
        // Get the cached body content
        byte[] requestBodyBytes = request.getContentAsByteArray();
        return new String(requestBodyBytes, StandardCharsets.UTF_8);
    }
    
    /**
     * Validate webhook signature using HMAC-SHA256
     */
    private boolean validateSignature(String requestBody, String signature) {
        try {
            // Generate expected signature
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKey = new SecretKeySpec(
                webhookSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            mac.init(secretKey);
            
            byte[] hash = mac.doFinal(requestBody.getBytes(StandardCharsets.UTF_8));
            String expectedSignature = Base64.getEncoder().encodeToString(hash);
            
            // Compare signatures (constant time comparison to prevent timing attacks)
            return constantTimeEquals(signature, expectedSignature);
            
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            logger.error("Error generating signature", e);
            return false;
        }
    }
    
    /**
     * Constant time string comparison to prevent timing attacks
     */
    private boolean constantTimeEquals(String a, String b) {
        if (a.length() != b.length()) {
            return false;
        }
        
        int result = 0;
        for (int i = 0; i < a.length(); i++) {
            result |= a.charAt(i) ^ b.charAt(i);
        }
        
        return result == 0;
    }
    
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        // This filter should only run for webhook endpoints
        String path = request.getRequestURI();
        return !(path.startsWith("/api/webhooks/") || 
                path.equals("/api/shipment") || 
                path.equals("/api/shipment_loaded") || 
                path.equals("/api/shipment_status") || 
                path.equals("/api/address_change"));
    }
}