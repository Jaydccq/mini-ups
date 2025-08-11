/**
 * Custom User Details Service
 * 
 * Description:
 * - Implements Spring Security's UserDetailsService interface
 * - Loads user information from the database for authentication
 * - Converts custom User entity to Spring Security's UserDetails object
 * 
 * Core Responsibilities:
 * - loadUserByUsername: Load user information from the database by username or email
 * - Authority mapping: Convert user roles to Spring Security authorities
 * - Account status checks: Validate whether the account is enabled, expired, or locked
 * 
 * Inner Class CustomUserPrincipal:
 * - Implements UserDetails, wrapping the custom User entity
 * - Provides user authentication information required by Spring Security
 * - Supports role-to-authority mapping (USER/ADMIN/DRIVER/OPERATOR)
 * - Manages account status (enabled/disabled, expired, locked, etc.)
 * 
 * Security Features:
 * - Account enabled status check (based on user.enabled field)
 * - Automatic role-to-authority mapping (adds ROLE_ prefix)
 * - Secure password handling (no plaintext exposure)
 * 
 * Integration Points:
 * - Seamlessly integrates with Spring Security authentication flow
 * - Supports user info extraction when generating JWT tokens
 * - Provides authority support for method-level security annotations
 * 
 *
 
 */
package com.miniups.security;

import com.miniups.model.entity.User;
import com.miniups.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@Service
public class CustomUserDetailsService implements UserDetailsService {
    
    @Autowired
    private UserRepository userRepository;
    
    @Override
    public UserDetails loadUserByUsername(String usernameOrEmail) throws UsernameNotFoundException {
        if (usernameOrEmail == null || usernameOrEmail.trim().isEmpty()) {
            throw new UsernameNotFoundException("User not found: " + usernameOrEmail);
        }
        
        User user;
        
        // Check if the input contains '@' to determine if it's an email
        if (usernameOrEmail.contains("@")) {
            user = userRepository.findByEmail(usernameOrEmail)
                    .orElseThrow(() -> new UsernameNotFoundException("User not found: " + usernameOrEmail));
        } else {
            user = userRepository.findByUsername(usernameOrEmail)
                    .orElseThrow(() -> new UsernameNotFoundException("User not found: " + usernameOrEmail));
        }
        
        return new CustomUserPrincipal(user);
    }
    
    public static class CustomUserPrincipal implements UserDetails {
        private User user;
        
        public CustomUserPrincipal(User user) {
            this.user = user;
        }
        
        @Override
        public Collection<? extends GrantedAuthority> getAuthorities() {
            List<GrantedAuthority> authorities = new ArrayList<>();
            authorities.add(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()));
            return authorities;
        }
        
        @Override
        public String getPassword() {
            return user.getPassword();
        }
        
        @Override
        public String getUsername() {
            return user.getUsername();
        }
        
        @Override
        public boolean isAccountNonExpired() {
            return true;
        }
        
        @Override
        public boolean isAccountNonLocked() {
            return true;
        }
        
        @Override
        public boolean isCredentialsNonExpired() {
            return true;
        }
        
        @Override
        public boolean isEnabled() {
            return user.getEnabled();
        }
        
        public User getUser() {
            return user;
        }
    }
}
