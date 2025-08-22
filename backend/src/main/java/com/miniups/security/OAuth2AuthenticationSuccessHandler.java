package com.miniups.security;

import com.miniups.exception.OAuth2AuthenticationProcessingException;
import com.miniups.service.AuthService;
import com.miniups.model.entity.User;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * OAuth2 Authentication Success Handler
 * 
 * Handles successful OAuth2 authentication by:
 * 1. Processing the OAuth2 user information
 * 2. Creating or linking local user accounts
 * 3. Generating JWT tokens
 * 4. Redirecting to the frontend with appropriate parameters
 * 
 * This handler serves as the bridge between OAuth2 authentication
 * and the application's JWT-based authentication system.
 */
@Component
public class OAuth2AuthenticationSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {
    
    private static final Logger logger = LoggerFactory.getLogger(OAuth2AuthenticationSuccessHandler.class);
    
    private final AuthService authService;
    private final JwtTokenProvider jwtTokenProvider;
    private final String frontendRedirectUrl;
    private final String frontendErrorUrl;
    
    public OAuth2AuthenticationSuccessHandler(
            AuthService authService,
            JwtTokenProvider jwtTokenProvider,
            @Value("${app.oauth2.redirect-uri:http://localhost:3000/auth/callback}") String frontendRedirectUrl,
            @Value("${app.oauth2.error-redirect-uri:http://localhost:3000/login}") String frontendErrorUrl) {
        this.authService = authService;
        this.jwtTokenProvider = jwtTokenProvider;
        this.frontendRedirectUrl = frontendRedirectUrl;
        this.frontendErrorUrl = frontendErrorUrl;
    }
    
    @Override
    public void onAuthenticationSuccess(
            HttpServletRequest request, 
            HttpServletResponse response, 
            Authentication authentication) throws IOException {
        
        logger.info("OAuth2 authentication successful, processing user information");
        
        try {
            // Extract OIDC user information
            OidcUser oidcUser = (OidcUser) authentication.getPrincipal();
            logger.debug("Processing OAuth2 user: {}", oidcUser.getEmail());
            
            // Process the OAuth2 user through our AuthService
            User localUser = authService.processOAuth2PostLogin(oidcUser);
            
            // Generate JWT token using our existing token provider
            String token = jwtTokenProvider.generateToken(localUser.getUsername());
            Long expiresIn = jwtTokenProvider.getExpirationTime();
            
            // Build success redirect URL
            String targetUrl = UriComponentsBuilder.fromUriString(frontendRedirectUrl)
                    .queryParam("token", token)
                    .queryParam("expiresIn", expiresIn)
                    .queryParam("success", "true")
                    .build().toUriString();
            
            logger.info("OAuth2 authentication completed successfully for user: {}, redirecting to: {}", 
                       localUser.getUsername(), frontendRedirectUrl);
            
            getRedirectStrategy().sendRedirect(request, response, targetUrl);
            
        } catch (OAuth2AuthenticationProcessingException e) {
            logger.warn("OAuth2 authentication processing failed: {}", e.getMessage());
            handleAuthenticationError(request, response, e.getMessage());
            
        } catch (Exception e) {
            logger.error("Unexpected error during OAuth2 authentication success handling", e);
            handleAuthenticationError(request, response, "OAuth2 login failed. Please try again.");
        }
    }
    
    /**
     * Handle authentication errors by redirecting to frontend with error information
     * 
     * @param request HTTP request
     * @param response HTTP response  
     * @param errorMessage error message to display
     */
    private void handleAuthenticationError(
            HttpServletRequest request, 
            HttpServletResponse response, 
            String errorMessage) throws IOException {
        
        String encodedErrorMessage = URLEncoder.encode(errorMessage, StandardCharsets.UTF_8);
        
        String errorUrl = UriComponentsBuilder.fromUriString(frontendErrorUrl)
                .queryParam("error", "oauth_failed")
                .queryParam("message", encodedErrorMessage)
                .build().toUriString();
        
        logger.info("Redirecting to error page: {}", errorUrl);
        getRedirectStrategy().sendRedirect(request, response, errorUrl);
    }
}