package com.miniups.security;

import com.miniups.exception.OAuth2AuthenticationProcessingException;
import com.miniups.model.entity.User;
import com.miniups.model.enums.AuthProvider;
import com.miniups.model.enums.UserRole;
import com.miniups.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.web.RedirectStrategy;

import java.io.IOException;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for OAuth2AuthenticationSuccessHandler
 * 
 * Tests cover successful OAuth2 authentication handling,
 * JWT token generation, and redirect behavior.
 */
@ExtendWith(MockitoExtension.class)
class OAuth2AuthenticationSuccessHandlerTest {

    @Mock
    private AuthService authService;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private Authentication authentication;

    @Mock
    private OidcUser oidcUser;

    @Mock
    private RedirectStrategy redirectStrategy;

    private OAuth2AuthenticationSuccessHandler successHandler;

    private static final String FRONTEND_REDIRECT_URL = "http://localhost:3000/auth/callback";
    private static final String FRONTEND_ERROR_URL = "http://localhost:3000/login";

    @BeforeEach
    void setUp() {
        successHandler = new OAuth2AuthenticationSuccessHandler(
                authService,
                jwtTokenProvider,
                FRONTEND_REDIRECT_URL,
                FRONTEND_ERROR_URL
        );
        
        // Use reflection to set the redirect strategy mock
        successHandler.setRedirectStrategy(redirectStrategy);
        
        // Common setup
        when(authentication.getPrincipal()).thenReturn(oidcUser);
        when(oidcUser.getEmail()).thenReturn("test@gmail.com");
    }

    @Test
    void onAuthenticationSuccess_ValidUser_ShouldRedirectWithToken() throws IOException {
        // Arrange
        User testUser = createTestUser();
        when(authService.processOAuth2PostLogin(oidcUser)).thenReturn(testUser);
        when(jwtTokenProvider.generateToken("testuser")).thenReturn("test-jwt-token");
        when(jwtTokenProvider.getExpirationTime()).thenReturn(86400000L);

        // Act
        successHandler.onAuthenticationSuccess(request, response, authentication);

        // Assert
        verify(authService).processOAuth2PostLogin(oidcUser);
        verify(jwtTokenProvider).generateToken("testuser");
        verify(redirectStrategy).sendRedirect(eq(request), eq(response), 
                argThat(url -> url.contains(FRONTEND_REDIRECT_URL) && url.contains("token=test-jwt-token")));
    }

    @Test
    void onAuthenticationSuccess_OAuth2ProcessingException_ShouldRedirectToError() throws IOException {
        // Arrange
        String errorMessage = "Account linking required";
        when(authService.processOAuth2PostLogin(oidcUser))
                .thenThrow(new OAuth2AuthenticationProcessingException(errorMessage));

        // Act
        successHandler.onAuthenticationSuccess(request, response, authentication);

        // Assert
        verify(authService).processOAuth2PostLogin(oidcUser);
        verify(jwtTokenProvider, never()).generateToken(anyString());
        verify(redirectStrategy).sendRedirect(eq(request), eq(response), 
                argThat(url -> url.contains(FRONTEND_ERROR_URL) && url.contains("error=oauth_failed")));
    }

    @Test
    void onAuthenticationSuccess_UnexpectedException_ShouldRedirectToError() throws IOException {
        // Arrange
        when(authService.processOAuth2PostLogin(oidcUser))
                .thenThrow(new RuntimeException("Unexpected error"));

        // Act
        successHandler.onAuthenticationSuccess(request, response, authentication);

        // Assert
        verify(authService).processOAuth2PostLogin(oidcUser);
        verify(jwtTokenProvider, never()).generateToken(anyString());
        verify(redirectStrategy).sendRedirect(eq(request), eq(response), 
                argThat(url -> url.contains(FRONTEND_ERROR_URL) && url.contains("error=oauth_failed")));
    }

    @Test
    void onAuthenticationSuccess_TokenGenerationSuccess_ShouldIncludeExpirationTime() throws IOException {
        // Arrange
        User testUser = createTestUser();
        when(authService.processOAuth2PostLogin(oidcUser)).thenReturn(testUser);
        when(jwtTokenProvider.generateToken("testuser")).thenReturn("test-jwt-token");
        when(jwtTokenProvider.getExpirationTime()).thenReturn(3600000L); // 1 hour

        // Act
        successHandler.onAuthenticationSuccess(request, response, authentication);

        // Assert
        verify(redirectStrategy).sendRedirect(eq(request), eq(response), 
                argThat(url -> url.contains("token=test-jwt-token") && 
                              url.contains("expiresIn=3600000") && 
                              url.contains("success=true")));
    }

    private User createTestUser() {
        User user = new User();
        user.setId(1L);
        user.setUsername("testuser");
        user.setEmail("test@gmail.com");
        user.setFirstName("John");
        user.setLastName("Doe");
        user.setAuthProvider(AuthProvider.GOOGLE);
        user.setProviderId("google-user-123");
        user.setRole(UserRole.USER);
        user.setEnabled(true);
        return user;
    }
}