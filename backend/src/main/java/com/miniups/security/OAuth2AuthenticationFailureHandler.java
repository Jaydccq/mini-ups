package com.miniups.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * OAuth2 Authentication Failure Handler
 * 
 * Handles OAuth2 authentication failures by redirecting users to the frontend
 * with appropriate error information. This provides a better user experience
 * than the default Spring Security error handling.
 * 
 * Common failure scenarios:
 * - User denies consent at Google
 * - Network connectivity issues
 * - Invalid OAuth2 configuration
 * - Missing required user information
 */
@Component
public class OAuth2AuthenticationFailureHandler extends SimpleUrlAuthenticationFailureHandler {
    
    private static final Logger logger = LoggerFactory.getLogger(OAuth2AuthenticationFailureHandler.class);
    
    private final String frontendFailureRedirectUrl;
    
    public OAuth2AuthenticationFailureHandler(
            @Value("${app.oauth2.failure-redirect-uri:http://localhost:3000/login}") String frontendFailureRedirectUrl) {
        this.frontendFailureRedirectUrl = frontendFailureRedirectUrl;
    }
    
    @Override
    public void onAuthenticationFailure(
            HttpServletRequest request, 
            HttpServletResponse response, 
            AuthenticationException exception) throws IOException {
        
        logger.warn("OAuth2 authentication failed: {}", exception.getMessage());
        
        String errorMessage = determineErrorMessage(exception);
        String errorCode = determineErrorCode(exception);
        
        String targetUrl = UriComponentsBuilder.fromUriString(frontendFailureRedirectUrl)
                .queryParam("error", errorCode)
                .queryParam("message", URLEncoder.encode(errorMessage, StandardCharsets.UTF_8))
                .build().toUriString();
        
        logger.info("Redirecting to failure URL: {}", targetUrl);
        getRedirectStrategy().sendRedirect(request, response, targetUrl);
    }
    
    /**
     * Determine user-friendly error message based on exception type
     * 
     * @param exception the authentication exception
     * @return user-friendly error message
     */
    private String determineErrorMessage(AuthenticationException exception) {
        if (exception instanceof OAuth2AuthenticationException) {
            OAuth2AuthenticationException oauth2Exception = (OAuth2AuthenticationException) exception;
            String errorCode = oauth2Exception.getError().getErrorCode();
            
            switch (errorCode) {
                case "access_denied":
                    return "You canceled the Google login process. Please try again if you want to log in with Google.";
                case "invalid_request":
                    return "There was a problem with the login request. Please try again.";
                case "invalid_client":
                    return "There was a configuration problem. Please contact support.";
                case "invalid_grant":
                    return "The authorization code was invalid or expired. Please try logging in again.";
                case "unsupported_response_type":
                case "invalid_scope":
                    return "There was a configuration problem. Please contact support.";
                default:
                    return "Google login failed: " + oauth2Exception.getError().getDescription();
            }
        }
        
        // Generic fallback message
        return "OAuth2 login failed. Please try again or contact support if the problem persists.";
    }
    
    /**
     * Determine error code for frontend handling
     * 
     * @param exception the authentication exception
     * @return error code for frontend
     */
    private String determineErrorCode(AuthenticationException exception) {
        if (exception instanceof OAuth2AuthenticationException) {
            OAuth2AuthenticationException oauth2Exception = (OAuth2AuthenticationException) exception;
            String errorCode = oauth2Exception.getError().getErrorCode();
            
            switch (errorCode) {
                case "access_denied":
                    return "user_cancelled";
                case "invalid_client":
                case "unsupported_response_type":
                case "invalid_scope":
                    return "configuration_error";
                case "invalid_request":
                case "invalid_grant":
                    return "request_error";
                default:
                    return "oauth2_error";
            }
        }
        
        return "authentication_failed";
    }
}