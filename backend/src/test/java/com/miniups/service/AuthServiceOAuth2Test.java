package com.miniups.service;

import com.miniups.exception.OAuth2AuthenticationProcessingException;
import com.miniups.model.entity.User;
import com.miniups.model.enums.AuthProvider;
import com.miniups.model.enums.UserRole;
import com.miniups.repository.UserRepository;
import com.miniups.security.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for OAuth2 functionality in AuthService
 * 
 * Tests cover the processOAuth2PostLogin method and related OAuth2 logic
 * including user creation, account linking detection, and error handling.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AuthServiceOAuth2Test {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private OidcUser oidcUser;

    @InjectMocks
    private AuthService authService;

    @BeforeEach
    void setUp() {
        // Common OIDC user setup
        when(oidcUser.getEmail()).thenReturn("test@gmail.com");
        when(oidcUser.getSubject()).thenReturn("google-user-123");
        when(oidcUser.getGivenName()).thenReturn("John");
        when(oidcUser.getFamilyName()).thenReturn("Doe");
    }

    @Test
    void processOAuth2PostLogin_NewUser_ShouldCreateUser() {
        // Arrange
        when(userRepository.findByAuthProviderAndProviderId(AuthProvider.GOOGLE, "google-user-123"))
                .thenReturn(Optional.empty());
        when(userRepository.findByEmail("test@gmail.com"))
                .thenReturn(Optional.empty());
        when(userRepository.existsByUsername("test")).thenReturn(false);
        
        User savedUser = createTestUser();
        when(userRepository.save(any(User.class))).thenReturn(savedUser);

        // Act
        User result = authService.processOAuth2PostLogin(oidcUser);

        // Assert
        assertNotNull(result);
        assertEquals("test@gmail.com", result.getEmail());
        assertEquals(AuthProvider.GOOGLE, result.getAuthProvider());
        assertEquals("google-user-123", result.getProviderId());
        assertEquals("John", result.getFirstName());
        assertEquals("Doe", result.getLastName());
        assertNull(result.getPassword());
        assertEquals(UserRole.USER, result.getRole());
        assertTrue(result.getEnabled());

        verify(userRepository).save(any(User.class));
    }

    @Test
    void processOAuth2PostLogin_ExistingGoogleUser_ShouldReturnUser() {
        // Arrange
        User existingUser = createTestUser();
        when(userRepository.findByAuthProviderAndProviderId(AuthProvider.GOOGLE, "google-user-123"))
                .thenReturn(Optional.of(existingUser));

        // Act
        User result = authService.processOAuth2PostLogin(oidcUser);

        // Assert
        assertNotNull(result);
        assertEquals(existingUser, result);
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void processOAuth2PostLogin_EmailExistsWithLocalProvider_ShouldThrowException() {
        // Arrange
        User localUser = createTestUser();
        localUser.setAuthProvider(AuthProvider.LOCAL);
        
        when(userRepository.findByAuthProviderAndProviderId(AuthProvider.GOOGLE, "google-user-123"))
                .thenReturn(Optional.empty());
        when(userRepository.findByEmail("test@gmail.com"))
                .thenReturn(Optional.of(localUser));

        // Act & Assert
        OAuth2AuthenticationProcessingException exception = assertThrows(
                OAuth2AuthenticationProcessingException.class,
                () -> authService.processOAuth2PostLogin(oidcUser)
        );

        assertTrue(exception.getMessage().contains("account with this email already exists"));
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void processOAuth2PostLogin_NullEmail_ShouldThrowException() {
        // Arrange
        when(oidcUser.getEmail()).thenReturn(null);

        // Act & Assert
        OAuth2AuthenticationProcessingException exception = assertThrows(
                OAuth2AuthenticationProcessingException.class,
                () -> authService.processOAuth2PostLogin(oidcUser)
        );

        assertEquals("Email not found from OAuth2 provider", exception.getMessage());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void processOAuth2PostLogin_NullProviderId_ShouldThrowException() {
        // Arrange
        when(oidcUser.getSubject()).thenReturn(null);

        // Act & Assert
        OAuth2AuthenticationProcessingException exception = assertThrows(
                OAuth2AuthenticationProcessingException.class,
                () -> authService.processOAuth2PostLogin(oidcUser)
        );

        assertEquals("Provider ID not found from OAuth2 provider", exception.getMessage());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void processOAuth2PostLogin_DuplicateUsername_ShouldGenerateUniqueUsername() {
        // Arrange
        when(userRepository.findByAuthProviderAndProviderId(AuthProvider.GOOGLE, "google-user-123"))
                .thenReturn(Optional.empty());
        when(userRepository.findByEmail("test@gmail.com"))
                .thenReturn(Optional.empty());
        when(userRepository.existsByUsername("test")).thenReturn(true);
        when(userRepository.existsByUsername("test1")).thenReturn(false);
        
        User savedUser = createTestUser();
        savedUser.setUsername("test1");
        when(userRepository.save(any(User.class))).thenReturn(savedUser);

        // Act
        User result = authService.processOAuth2PostLogin(oidcUser);

        // Assert
        assertNotNull(result);
        assertEquals("test1", result.getUsername());
        verify(userRepository).save(any(User.class));
    }

    @Test
    void requiresAccountLinking_LocalUserExists_ShouldReturnTrue() {
        // Arrange
        User localUser = createTestUser();
        localUser.setAuthProvider(AuthProvider.LOCAL);
        when(userRepository.findByEmail("test@gmail.com")).thenReturn(Optional.of(localUser));

        // Act
        boolean result = authService.requiresAccountLinking("test@gmail.com");

        // Assert
        assertTrue(result);
    }

    @Test
    void requiresAccountLinking_GoogleUserExists_ShouldReturnFalse() {
        // Arrange
        User googleUser = createTestUser();
        googleUser.setAuthProvider(AuthProvider.GOOGLE);
        when(userRepository.findByEmail("test@gmail.com")).thenReturn(Optional.of(googleUser));

        // Act
        boolean result = authService.requiresAccountLinking("test@gmail.com");

        // Assert
        assertFalse(result);
    }

    @Test
    void requiresAccountLinking_NoUserExists_ShouldReturnFalse() {
        // Arrange
        when(userRepository.findByEmail("test@gmail.com")).thenReturn(Optional.empty());

        // Act
        boolean result = authService.requiresAccountLinking("test@gmail.com");

        // Assert
        assertFalse(result);
    }

    private User createTestUser() {
        User user = new User();
        user.setId(1L);
        user.setUsername("test");
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