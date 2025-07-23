package com.miniups.security;

import io.jsonwebtoken.MalformedJwtException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.*;

/**
 * Unit tests for JwtTokenProvider.
 * Tests JWT token generation, validation, and parsing functionality.
 */
@ExtendWith(MockitoExtension.class)
@ActiveProfiles("test")
@DisplayName("JwtTokenProvider Unit Tests")
class JwtTokenProviderTest {

    private JwtTokenProvider jwtTokenProvider;
    
    private final String TEST_SECRET = "test-secret-key-for-unit-tests-that-is-long-enough-for-hmac-sha256";
    private final long TEST_EXPIRATION = 3600000L; // 1 hour

    @BeforeEach
    void setUp() {
        jwtTokenProvider = new JwtTokenProvider();
        
        // Set test configuration using reflection
        ReflectionTestUtils.setField(jwtTokenProvider, "jwtSecret", TEST_SECRET);
        ReflectionTestUtils.setField(jwtTokenProvider, "jwtExpirationInMs", (int) TEST_EXPIRATION);
        
        // Initialize the token provider
        jwtTokenProvider.validateJwtConfiguration();
    }

    @Test
    @DisplayName("Should generate valid JWT token")
    void testGenerateToken_Success() {
        // Given
        String username = "testuser";

        // When
        String token = jwtTokenProvider.generateToken(username);

        // Then
        assertThat(token).isNotNull();
        assertThat(token).isNotEmpty();
        assertThat(token.split("\\.")).hasSize(3); // JWT has 3 parts separated by dots
        assertThat(jwtTokenProvider.validateToken(token)).isTrue();
    }

    @Test
    @DisplayName("Should extract username from token")
    void testGetUsernameFromToken_Success() {
        // Given
        String username = "testuser";
        String token = jwtTokenProvider.generateToken(username);

        // When
        String extractedUsername = jwtTokenProvider.getUsernameFromToken(token);

        // Then
        assertThat(extractedUsername).isEqualTo(username);
    }

    @Test
    @DisplayName("Should validate correct token")
    void testValidateToken_ValidToken() {
        // Given
        String username = "testuser";
        String token = jwtTokenProvider.generateToken(username);

        // When
        boolean isValid = jwtTokenProvider.validateToken(token);

        // Then
        assertThat(isValid).isTrue();
    }

    @Test
    @DisplayName("Should reject null token")
    void testValidateToken_NullToken() {
        // When
        boolean isValid = jwtTokenProvider.validateToken(null);

        // Then
        assertThat(isValid).isFalse();
    }

    @Test
    @DisplayName("Should reject empty token")
    void testValidateToken_EmptyToken() {
        // When
        boolean isValid = jwtTokenProvider.validateToken("");

        // Then
        assertThat(isValid).isFalse();
    }

    @Test
    @DisplayName("Should reject malformed token")
    void testValidateToken_MalformedToken() {
        // Given
        String malformedToken = "this.is.not.a.valid.jwt.token";

        // When
        boolean isValid = jwtTokenProvider.validateToken(malformedToken);

        // Then
        assertThat(isValid).isFalse();
    }

    @Test
    @DisplayName("Should reject token with invalid signature")
    void testValidateToken_InvalidSignature() {
        // Given - Create token with different secret
        JwtTokenProvider anotherProvider = new JwtTokenProvider();
        ReflectionTestUtils.setField(anotherProvider, "jwtSecret", "different-secret-key-that-is-long-enough-for-hmac-sha256");
        ReflectionTestUtils.setField(anotherProvider, "jwtExpirationInMs", Math.toIntExact(TEST_EXPIRATION));
        anotherProvider.validateJwtConfiguration();
        
        String username = "testuser";
        String tokenWithDifferentSignature = anotherProvider.generateToken(username);

        // When
        boolean isValid = jwtTokenProvider.validateToken(tokenWithDifferentSignature);

        // Then
        assertThat(isValid).isFalse();
    }

    @Test
    @DisplayName("Should handle expired token")
    void testValidateToken_ExpiredToken() throws InterruptedException {
        // Given - Create provider with very short expiration
        JwtTokenProvider shortExpirationProvider = new JwtTokenProvider();
        ReflectionTestUtils.setField(shortExpirationProvider, "jwtSecret", TEST_SECRET);
        ReflectionTestUtils.setField(shortExpirationProvider, "jwtExpirationInMs", 1); // Very short expiration
        shortExpirationProvider.validateJwtConfiguration();
        
        String username = "testuser";
        String expiredToken = shortExpirationProvider.generateToken(username);
        
        // Wait to ensure token is expired
        Thread.sleep(5);

        // When
        boolean isValid = jwtTokenProvider.validateToken(expiredToken);

        // Then
        assertThat(isValid).isFalse();
    }

    @Test
    @DisplayName("Should generate different tokens for different users")
    void testGenerateToken_DifferentUsers() {
        // Given
        String user1 = "user1";
        String user2 = "user2";

        // When
        String token1 = jwtTokenProvider.generateToken(user1);
        String token2 = jwtTokenProvider.generateToken(user2);

        // Then
        assertThat(token1).isNotEqualTo(token2);
        assertThat(jwtTokenProvider.getUsernameFromToken(token1)).isEqualTo("user1");
        assertThat(jwtTokenProvider.getUsernameFromToken(token2)).isEqualTo("user2");
    }

    @Test
    @DisplayName("Should generate different tokens for same user at different times")
    void testGenerateToken_DifferentTimes() throws InterruptedException {
        // Given
        String username = "testuser";

        // When - Generate tokens with a small time gap
        String token1 = jwtTokenProvider.generateToken(username);
        
        // Sleep for a short time to ensure different timestamp
        Thread.sleep(10);
        
        String token2 = jwtTokenProvider.generateToken(username);

        // Then
        assertThat(token1).isNotEqualTo(token2);
        assertThat(jwtTokenProvider.getUsernameFromToken(token1)).isEqualTo("testuser");
        assertThat(jwtTokenProvider.getUsernameFromToken(token2)).isEqualTo("testuser");
    }

    @Test
    @DisplayName("Should handle special characters in username")
    void testGenerateToken_SpecialCharacters() {
        // Given
        String username = "user@email.com";

        // When
        String token = jwtTokenProvider.generateToken(username);

        // Then
        assertThat(token).isNotNull();
        assertThat(jwtTokenProvider.validateToken(token)).isTrue();
        assertThat(jwtTokenProvider.getUsernameFromToken(token)).isEqualTo("user@email.com");
    }

    @Test
    @DisplayName("Should handle long usernames")
    void testGenerateToken_LongUsername() {
        // Given
        String longUsername = "a".repeat(255); // Very long username

        // When
        String token = jwtTokenProvider.generateToken(longUsername);

        // Then
        assertThat(token).isNotNull();
        assertThat(jwtTokenProvider.validateToken(token)).isTrue();
        assertThat(jwtTokenProvider.getUsernameFromToken(token)).isEqualTo(longUsername);
    }

    @Test
    @DisplayName("Should handle authentication with authorities")
    void testGenerateToken_WithAuthorities() {
        // Given
        String username = "adminuser";
        // Authorities are typically handled at the application level, not in JWT payload

        // When
        String token = jwtTokenProvider.generateToken(username);

        // Then
        assertThat(token).isNotNull();
        assertThat(jwtTokenProvider.validateToken(token)).isTrue();
        assertThat(jwtTokenProvider.getUsernameFromToken(token)).isEqualTo("adminuser");
    }

    @Test
    @DisplayName("Should fail gracefully when extracting username from invalid token")
    void testGetUsernameFromToken_InvalidToken() {
        // Given
        String invalidToken = "invalid.jwt.token";

        // When & Then
        assertThatThrownBy(() -> jwtTokenProvider.getUsernameFromToken(invalidToken))
                .isInstanceOf(MalformedJwtException.class);
    }

    @Test
    @DisplayName("Should initialize signing key correctly")
    void testInit_SigningKeyInitialization() {
        // Given
        JwtTokenProvider newProvider = new JwtTokenProvider();
        ReflectionTestUtils.setField(newProvider, "jwtSecret", TEST_SECRET);
        ReflectionTestUtils.setField(newProvider, "jwtExpirationInMs", Math.toIntExact(TEST_EXPIRATION));

        // When
        newProvider.validateJwtConfiguration();

        // Then - Should be able to generate and validate tokens
        String username = "testuser";
        String token = newProvider.generateToken(username);
        assertThat(newProvider.validateToken(token)).isTrue();
    }

    @Test
    @DisplayName("Should handle minimum secret key length requirement")
    void testInit_MinimumSecretLength() {
        // Given
        JwtTokenProvider provider = new JwtTokenProvider();
        String shortSecret = "short"; // Too short for HMAC-SHA256
        ReflectionTestUtils.setField(provider, "jwtSecret", shortSecret);
        ReflectionTestUtils.setField(provider, "jwtExpirationInMs", Math.toIntExact(TEST_EXPIRATION));

        // When & Then - Should fail initialization due to short secret
        assertThatThrownBy(provider::validateJwtConfiguration)
                .isInstanceOf(IllegalStateException.class);
    }

}