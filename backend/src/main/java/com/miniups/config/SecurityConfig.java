/**
 * Security Configuration Class
 * 
 * Purpose:
 * - Configures comprehensive security settings for the Mini-UPS application
 * - Implements JWT-based authentication with role-based access control
 * - Defines security filter chain and authentication mechanisms
 * - Establishes CORS policies and endpoint protection strategies
 * 
 * Core Security Components:
 * - JWT Authentication: Stateless token-based authentication system
 * - Role-based Access Control: USER, ADMIN, DRIVER, OPERATOR role hierarchy
 * - CORS Configuration: Cross-origin resource sharing for frontend integration
 * - Password Encoding: BCrypt hashing for secure password storage
 * - Method Security: Annotation-based security for fine-grained access control
 * 
 * Authentication Flow:
 * 1. User provides credentials to /api/auth/login endpoint
 * 2. CustomUserDetailsService loads user from database
 * 3. DaoAuthenticationProvider validates credentials with BCrypt
 * 4. JWT token generated and returned to client
 * 5. Subsequent requests include JWT in Authorization header
 * 6. JwtAuthenticationFilter validates token and sets SecurityContext
 * 
 * Authorization Hierarchy:
 * - Public Endpoints: /api/auth/**, /api/public/**, /api/tracking/**
 * - Admin Endpoints: /api/admin/** (ADMIN role required)
 * - Driver Endpoints: /api/driver/** (DRIVER or ADMIN roles)
 * - Protected Endpoints: All other endpoints require authentication
 * 
 * Security Features:
 * - Stateless sessions (SessionCreationPolicy.STATELESS)
 * - CSRF protection disabled (appropriate for JWT-based APIs)
 * - BCrypt password encoding with secure salt generation
 * - JWT token validation on every request
 * - Comprehensive exception handling for authentication failures
 * 
 * CORS Configuration:
 * - Allows all origins with pattern matching for development flexibility
 * - Supports GET, POST, PUT, DELETE, OPTIONS HTTP methods
 * - Permits all headers for maximum compatibility
 * - Credentials support enabled for authenticated requests
 * 
 * Method Security:
 * - @EnableMethodSecurity(prePostEnabled = true) enables @PreAuthorize/@PostAuthorize
 * - Supports SpEL expressions for complex authorization logic
 * - Method-level security complements URL-based security rules
 * - Enables fine-grained access control in service layers
 * 
 * Public Endpoints:
 * - /api/auth/**: Authentication endpoints (login, register, refresh)
 * - /api/public/**: Public API endpoints (documentation, health checks)
 * - /api/tracking/**: Package tracking without authentication
 * - /api/webhooks/**: Amazon integration webhooks
 * - /api-docs/**, /swagger-ui/**: API documentation
 * - /actuator/health: Application health monitoring
 * 
 * Protected Endpoints:
 * - /api/admin/**: Administrative functions (user management, system config)
 * - /api/driver/**: Driver-specific operations (truck management, deliveries)
 * - All other /api/** endpoints: General authenticated operations
 * 
 * Authentication Provider Configuration:
 * - DaoAuthenticationProvider: Database-backed authentication
 * - CustomUserDetailsService: Loads user details from database
 * - BCryptPasswordEncoder: Secure password hashing and verification
 * - AuthenticationManager: Coordinates authentication process
 * 
 * Filter Chain Order:
 * 1. CORS filter (built-in)
 * 2. JwtAuthenticationFilter (custom, before UsernamePasswordAuthenticationFilter)
 * 3. UsernamePasswordAuthenticationFilter (Spring Security default)
 * 4. Other Spring Security filters
 * 
 * Security Best Practices:
 * - Stateless authentication prevents session hijacking
 * - Strong password encoding with BCrypt
 * - Comprehensive input validation and sanitization
 * - Proper exception handling without information leakage
 * - Regular security audits and vulnerability assessments
 * 
 * Integration Points:
 * - Works with CustomUserDetailsService for user loading
 * - Integrates with JWT components for token handling
 * - Supports WebSocket security for real-time features
 * - Compatible with Spring Boot Security auto-configuration
 * 
 * Development vs Production:
 * - CORS configuration should be more restrictive in production
 * - JWT secret keys must be environment-specific and secure
 * - HTTPS should be enforced in production environments
 * - Security headers should be added for production deployment
 * 
 * Monitoring and Auditing:
 * - Authentication events logged for security monitoring
 * - Failed authentication attempts tracked for intrusion detection
 * - Security metrics available through Spring Boot Actuator
 * - Integration with external security monitoring systems
 * 
 *
 

 */
package com.miniups.config;

import com.miniups.security.JwtAuthenticationEntryPoint;
import com.miniups.security.JwtAuthenticationFilter;
import com.miniups.security.OAuth2AuthenticationSuccessHandler;
import com.miniups.security.OAuth2AuthenticationFailureHandler;
import com.miniups.security.RateLimitingFilter;
import com.miniups.security.WebhookAuthenticationFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.config.annotation.web.builders.WebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {
    
    @Autowired
    private UserDetailsService userDetailsService;
    
    @Autowired
    private JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;
    
    @Autowired
    private JwtAuthenticationFilter jwtAuthenticationFilter;
    
    @Autowired
    private WebhookAuthenticationFilter webhookAuthenticationFilter;
    
    @Autowired
    private RateLimitingFilter rateLimitingFilter;
    
    @Autowired(required = false)
    @Lazy
    private OAuth2AuthenticationSuccessHandler oAuth2AuthenticationSuccessHandler;
    
    @Autowired(required = false)
    private OAuth2AuthenticationFailureHandler oAuth2AuthenticationFailureHandler;
    
    @Value("${cors.allowed-origins:http://localhost:3000,http://localhost:3001}")
    private String allowedOrigins;
    
    @Value("${cors.allowed-methods:GET,POST,PUT,DELETE,OPTIONS}")
    private String allowedMethods;
    
    @Value("${cors.allow-credentials:true}")
    private boolean allowCredentials;
    
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
    
    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(userDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder());
        return authProvider;
    }
    
    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration authConfig) throws Exception {
        return authConfig.getAuthenticationManager();
    }
    
    // High-priority chain: fully allow Swagger/OpenAPI + static docs
    @Bean
    @org.springframework.core.annotation.Order(1)
    public SecurityFilterChain apiDocsSecurityFilterChain(HttpSecurity http) throws Exception {
        http
            .securityMatcher(
                "/v3/api-docs", "/v3/api-docs/**",
                "/api-docs", "/api-docs/**",
                "/swagger-ui/**", "/swagger-ui.html"
            )
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(authz -> authz.anyRequest().permitAll());

        return http.build();
    }

    // Main chain: app security
    @Bean
    @org.springframework.core.annotation.Order(2)
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .csrf(csrf -> csrf.disable())
            .exceptionHandling(exception -> 
                exception.authenticationEntryPoint(jwtAuthenticationEntryPoint))
            .sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(authz -> authz
                .requestMatchers(org.springframework.http.HttpMethod.OPTIONS, "/**").permitAll()
                // Public endpoints
                .requestMatchers("/", "/index.html", "/favicon.ico").permitAll()
                .requestMatchers("/api/auth/**", "/auth/**").permitAll() // Fixed: Add /api/auth/** pattern
                .requestMatchers("/oauth2/**", "/login/oauth2/code/*").permitAll() // OAuth2 endpoints
                .requestMatchers("/api/public/**", "/public/**").permitAll()
                .requestMatchers("/api/tracking/**", "/tracking/**").permitAll() // Allow tracking without auth
                .requestMatchers("/api/webhooks/**", "/webhooks/**").permitAll() // Amazon webhooks
                .requestMatchers("/api/users/*/public", "/users/*/public").permitAll() // Public user profiles
                .requestMatchers("/api/shipment", "/api/shipment_loaded", "/api/shipment_status", "/api/address_change").permitAll() // Amazon integration endpoints
                .requestMatchers("/shipment", "/shipment_loaded", "/shipment_status", "/address_change").permitAll() // Amazon integration endpoints

                // API Documentation (handled also by high-priority chain)
                .requestMatchers(
                    "/v3/api-docs", "/v3/api-docs/**",
                    "/api-docs", "/api-docs/**",
                    "/swagger-ui/**", "/swagger-ui.html"
                ).permitAll()

                // Health check
                .requestMatchers("/actuator/health", "/actuator/info", "/actuator/**").permitAll()
                // Debug endpoints for development (consider restricting in production)
                .requestMatchers("/api/debug/**", "/debug/**").permitAll()

                // Admin endpoints
                .requestMatchers("/api/admin/**", "/admin/**").hasRole("ADMIN")

                // Driver endpoints
                .requestMatchers("/api/driver/**", "/driver/**").hasAnyRole("DRIVER", "ADMIN")

                // All other endpoints require authentication
                .anyRequest().authenticated()
            );
        
        // Conditionally configure OAuth2 if the handlers are available
        if (oAuth2AuthenticationSuccessHandler != null && oAuth2AuthenticationFailureHandler != null) {
            http.oauth2Login(oauth2 -> oauth2
                .successHandler(oAuth2AuthenticationSuccessHandler)
                .failureHandler(oAuth2AuthenticationFailureHandler)
            );
        }
        
        http.authenticationProvider(authenticationProvider());
        http.addFilterBefore(rateLimitingFilter, UsernamePasswordAuthenticationFilter.class);
        http.addFilterBefore(webhookAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        http.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        
        return http.build();
    }

    // WebSecurityCustomizer no longer needed due to explicit API docs chain above
    
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        
        // Parse allowed origins from configuration
        List<String> origins = Arrays.asList(allowedOrigins.split(","));
        configuration.setAllowedOrigins(origins);
        
        // Parse allowed methods from configuration
        List<String> methods = Arrays.asList(allowedMethods.split(","));
        configuration.setAllowedMethods(methods);
        
        // Allow common headers but be more restrictive than "*"
        configuration.setAllowedHeaders(Arrays.asList(
            "Origin", "Content-Type", "Accept", "Authorization", 
            "X-Requested-With", "X-Amazon-Signature", "Cache-Control"
        ));
        
        // Expose headers that frontend might need
        configuration.setExposedHeaders(Arrays.asList(
            "Authorization", "X-Total-Count", "X-Page-Number", "X-Page-Size"
        ));
        
        configuration.setAllowCredentials(allowCredentials);
        configuration.setMaxAge(3600L); // Cache preflight requests for 1 hour
        
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
