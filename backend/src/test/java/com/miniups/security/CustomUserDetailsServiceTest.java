package com.miniups.security;

import com.miniups.model.entity.User;
import com.miniups.model.enums.UserRole;
import com.miniups.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.test.context.ActiveProfiles;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for CustomUserDetailsService.
 * Tests user authentication, role mapping, and security integration.
 */
@ExtendWith(MockitoExtension.class)
@ActiveProfiles("test")
@DisplayName("CustomUserDetailsService Unit Tests")
class CustomUserDetailsServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private CustomUserDetailsService customUserDetailsService;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("testuser");
        testUser.setEmail("test@example.com");
        testUser.setPassword("encodedPassword123");
        testUser.setRole(UserRole.USER);
        testUser.setEnabled(true);
    }

    @Test
    @DisplayName("Should load user by username successfully")
    void testLoadUserByUsername_Success() {
        // Given
        String username = "testuser";
        when(userRepository.findByUsername(username)).thenReturn(Optional.of(testUser));

        // When
        UserDetails userDetails = customUserDetailsService.loadUserByUsername(username);

        // Then
        assertThat(userDetails).isNotNull();
        assertThat(userDetails.getUsername()).isEqualTo(username);
        assertThat(userDetails.getPassword()).isEqualTo("encodedPassword123");
        assertThat(userDetails.isEnabled()).isTrue();
        assertThat(userDetails.isAccountNonExpired()).isTrue();
        assertThat(userDetails.isAccountNonLocked()).isTrue();
        assertThat(userDetails.isCredentialsNonExpired()).isTrue();
        
        verify(userRepository).findByUsername(username);
    }

    @Test
    @DisplayName("Should throw UsernameNotFoundException when user not found")
    void testLoadUserByUsername_UserNotFound() {
        // Given
        String username = "nonexistentuser";
        when(userRepository.findByUsername(username)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> customUserDetailsService.loadUserByUsername(username))
                .isInstanceOf(UsernameNotFoundException.class)
                .hasMessageContaining("User not found: " + username);

        verify(userRepository).findByUsername(username);
    }

    @Test
    @DisplayName("Should map USER role to ROLE_USER authority")
    void testLoadUserByUsername_UserRole() {
        // Given
        String username = "regularuser";
        testUser.setRole(UserRole.USER);
        when(userRepository.findByUsername(username)).thenReturn(Optional.of(testUser));

        // When
        UserDetails userDetails = customUserDetailsService.loadUserByUsername(username);

        // Then
        assertThat(userDetails.getAuthorities()).hasSize(1);
        GrantedAuthority authority = userDetails.getAuthorities().iterator().next();
        assertThat(authority.getAuthority()).isEqualTo("ROLE_USER");
    }

    @Test
    @DisplayName("Should map ADMIN role to ROLE_ADMIN authority")
    void testLoadUserByUsername_AdminRole() {
        // Given
        String username = "adminuser";
        testUser.setRole(UserRole.ADMIN);
        when(userRepository.findByUsername(username)).thenReturn(Optional.of(testUser));

        // When
        UserDetails userDetails = customUserDetailsService.loadUserByUsername(username);

        // Then
        assertThat(userDetails.getAuthorities()).hasSize(1);
        GrantedAuthority authority = userDetails.getAuthorities().iterator().next();
        assertThat(authority.getAuthority()).isEqualTo("ROLE_ADMIN");
    }

    @Test
    @DisplayName("Should map DRIVER role to ROLE_DRIVER authority")
    void testLoadUserByUsername_DriverRole() {
        // Given
        String username = "driveruser";
        testUser.setRole(UserRole.DRIVER);
        when(userRepository.findByUsername(username)).thenReturn(Optional.of(testUser));

        // When
        UserDetails userDetails = customUserDetailsService.loadUserByUsername(username);

        // Then
        assertThat(userDetails.getAuthorities()).hasSize(1);
        GrantedAuthority authority = userDetails.getAuthorities().iterator().next();
        assertThat(authority.getAuthority()).isEqualTo("ROLE_DRIVER");
    }

    @Test
    @DisplayName("Should map OPERATOR role to ROLE_OPERATOR authority")
    void testLoadUserByUsername_OperatorRole() {
        // Given
        String username = "operatoruser";
        testUser.setRole(UserRole.OPERATOR);
        when(userRepository.findByUsername(username)).thenReturn(Optional.of(testUser));

        // When
        UserDetails userDetails = customUserDetailsService.loadUserByUsername(username);

        // Then
        assertThat(userDetails.getAuthorities()).hasSize(1);
        GrantedAuthority authority = userDetails.getAuthorities().iterator().next();
        assertThat(authority.getAuthority()).isEqualTo("ROLE_OPERATOR");
    }

    @Test
    @DisplayName("Should handle disabled user account")
    void testLoadUserByUsername_DisabledUser() {
        // Given
        String username = "disableduser";
        testUser.setUsername(username);
        testUser.setEnabled(false);
        when(userRepository.findByUsername(username)).thenReturn(Optional.of(testUser));

        // When
        UserDetails userDetails = customUserDetailsService.loadUserByUsername(username);

        // Then
        assertThat(userDetails).isNotNull();
        assertThat(userDetails.isEnabled()).isFalse();
        assertThat(userDetails.getUsername()).isEqualTo(username);
    }

    @Test
    @DisplayName("Should return CustomUserPrincipal instance")
    void testLoadUserByUsername_CustomUserPrincipal() {
        // Given
        String username = "testuser";
        when(userRepository.findByUsername(username)).thenReturn(Optional.of(testUser));

        // When
        UserDetails userDetails = customUserDetailsService.loadUserByUsername(username);

        // Then
        assertThat(userDetails).isInstanceOf(CustomUserDetailsService.CustomUserPrincipal.class);
        
        CustomUserDetailsService.CustomUserPrincipal principal = 
                (CustomUserDetailsService.CustomUserPrincipal) userDetails;
        assertThat(principal.getUser()).isEqualTo(testUser);
    }

    @Test
    @DisplayName("Should handle null username gracefully")
    void testLoadUserByUsername_NullUsername() {
        // Given
        String username = null;
        when(userRepository.findByUsername(username)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> customUserDetailsService.loadUserByUsername(username))
                .isInstanceOf(UsernameNotFoundException.class);

        verify(userRepository).findByUsername(username);
    }

    @Test
    @DisplayName("Should handle empty username gracefully")
    void testLoadUserByUsername_EmptyUsername() {
        // Given
        String username = "";
        when(userRepository.findByUsername(username)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> customUserDetailsService.loadUserByUsername(username))
                .isInstanceOf(UsernameNotFoundException.class)
                .hasMessageContaining("User not found: ");

        verify(userRepository).findByUsername(username);
    }

    @Test
    @DisplayName("Should preserve user ID in CustomUserPrincipal")
    void testCustomUserPrincipal_UserIdPreservation() {
        // Given
        String username = "testuser";
        testUser.setId(12345L);
        when(userRepository.findByUsername(username)).thenReturn(Optional.of(testUser));

        // When
        UserDetails userDetails = customUserDetailsService.loadUserByUsername(username);

        // Then
        CustomUserDetailsService.CustomUserPrincipal principal = 
                (CustomUserDetailsService.CustomUserPrincipal) userDetails;
        assertThat(principal.getUser().getId()).isEqualTo(12345L);
    }

    @Test
    @DisplayName("Should handle repository exceptions gracefully")
    void testLoadUserByUsername_RepositoryException() {
        // Given
        String username = "testuser";
        when(userRepository.findByUsername(username)).thenThrow(new RuntimeException("Database error"));

        // When & Then
        assertThatThrownBy(() -> customUserDetailsService.loadUserByUsername(username))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Database error");

        verify(userRepository).findByUsername(username);
    }

    @Test
    @DisplayName("Should verify all UserDetails interface methods")
    void testUserDetails_AllMethods() {
        // Given
        String username = "testuser";
        when(userRepository.findByUsername(username)).thenReturn(Optional.of(testUser));

        // When
        UserDetails userDetails = customUserDetailsService.loadUserByUsername(username);

        // Then - Verify all UserDetails methods return expected values
        assertThat(userDetails.getUsername()).isNotNull();
        assertThat(userDetails.getPassword()).isNotNull();
        assertThat(userDetails.getAuthorities()).isNotEmpty();
        assertThat(userDetails.isAccountNonExpired()).isTrue();
        assertThat(userDetails.isAccountNonLocked()).isTrue();
        assertThat(userDetails.isCredentialsNonExpired()).isTrue();
        assertThat(userDetails.isEnabled()).isTrue();
    }
}