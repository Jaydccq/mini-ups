package com.miniups.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.miniups.config.TestConfig;
import com.miniups.model.entity.Shipment;
import com.miniups.model.entity.ShipmentStatusHistory;
import com.miniups.model.entity.User;
import com.miniups.model.enums.ShipmentStatus;
import com.miniups.model.enums.UserRole;
import com.miniups.repository.ShipmentRepository;
import com.miniups.repository.ShipmentStatusHistoryRepository;
import com.miniups.repository.UserRepository;
import com.miniups.security.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureWebMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Map;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for TrackingController.
 * Tests REST API endpoints with security, database integration, and JSON serialization.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebMvc
@ActiveProfiles("test")
@Import(TestConfig.class)
@Transactional
@DisplayName("TrackingController Integration Tests")
class TrackingControllerIntegrationTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ShipmentRepository shipmentRepository;

    @Autowired
    private ShipmentStatusHistoryRepository statusHistoryRepository;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private ObjectMapper objectMapper;

    private MockMvc mockMvc;
    private User testUser;
    private User adminUser;
    private Shipment testShipment;
    private String userToken;
    private String adminToken;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(webApplicationContext)
                .apply(springSecurity())
                .build();

        // Create test users with unique usernames to avoid database constraint violations
        String uniqueSuffix = String.valueOf(System.nanoTime());
        testUser = createUser("testuser_" + uniqueSuffix, "test_" + uniqueSuffix + "@example.com", UserRole.USER);
        adminUser = createUser("admin_" + uniqueSuffix, "admin_" + uniqueSuffix + "@example.com", UserRole.ADMIN);
        
        userRepository.save(testUser);
        userRepository.save(adminUser);

        // Generate JWT tokens
        userToken = generateToken(testUser);
        adminToken = generateToken(adminUser);

        // Create test shipment
        testShipment = createShipment(testUser, "UPS123456789", "SHIPMENT_TEST_A");
        shipmentRepository.save(testShipment);

        // Create status history
        createStatusHistory(testShipment, ShipmentStatus.CREATED, "Order created");
    }

    @Test
    @DisplayName("Should get shipment by tracking number successfully")
    void testGetShipmentByTrackingNumber_Success() throws Exception {
        mockMvc.perform(get("/api/tracking/{trackingNumber}", testShipment.getUpsTrackingId()))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.trackingNumber").value(testShipment.getUpsTrackingId()))
                .andExpect(jsonPath("$.data.status").value(ShipmentStatus.CREATED.name()));
    }

    @Test
    @DisplayName("Should return 404 for non-existent tracking number")
    void testGetShipmentByTrackingNumber_NotFound() throws Exception {
        mockMvc.perform(get("/api/tracking/{trackingNumber}", "UPS999999999"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Should get user shipments with authentication")
    void testGetUserShipments_Success() throws Exception {
        mockMvc.perform(get("/api/tracking/user/{userId}", testUser.getId())
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data[0].trackingNumber").value(testShipment.getUpsTrackingId()));
    }

    @Test
    @DisplayName("Should require authentication for user shipments")
    void testGetUserShipments_Unauthorized() throws Exception {
        mockMvc.perform(get("/api/tracking/user/{userId}", testUser.getId()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Should get status history for shipment")
    void testGetStatusHistory_Success() throws Exception {
        mockMvc.perform(get("/api/tracking/{trackingNumber}/history", testShipment.getUpsTrackingId()))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data[0].status").value(ShipmentStatus.CREATED.name()));
    }

    @Test
    @DisplayName("Should update shipment status with admin privileges")
    void testUpdateShipmentStatus_Success() throws Exception {
        String requestBody = objectMapper.writeValueAsString(Map.of(
                "status", ShipmentStatus.IN_TRANSIT.name(),
                "notes", "Package picked up"
        ));

        mockMvc.perform(put("/api/tracking/{trackingNumber}/status", testShipment.getUpsTrackingId())
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Status updated successfully"));
    }

    @Test
    @DisplayName("Should require admin role for status updates")
    void testUpdateShipmentStatus_Forbidden() throws Exception {
        String requestBody = objectMapper.writeValueAsString(Map.of(
                "status", ShipmentStatus.IN_TRANSIT.name(),
                "notes", "Package picked up"
        ));

        mockMvc.perform(put("/api/tracking/{trackingNumber}/status", testShipment.getUpsTrackingId())
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Should validate status update request body")
    void testUpdateShipmentStatus_InvalidRequest() throws Exception {
        String invalidRequestBody = objectMapper.writeValueAsString(Map.of(
                "status", "INVALID_STATUS",
                "notes", "Test notes"
        ));

        mockMvc.perform(put("/api/tracking/{trackingNumber}/status", testShipment.getUpsTrackingId())
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidRequestBody))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("Should handle malformed tracking numbers")
    void testGetShipment_MalformedTrackingNumber() throws Exception {
        mockMvc.perform(get("/api/tracking/{trackingNumber}", "INVALID123"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Invalid tracking number format"));
    }

    @Test
    @DisplayName("Should handle empty status history")
    void testGetStatusHistory_EmptyHistory() throws Exception {
        // Create shipment without status history
        Shipment shipmentWithoutHistory = createShipment(testUser, "UPS987654321", "SHIPMENT_TEST_B");
        shipmentRepository.save(shipmentWithoutHistory);

        mockMvc.perform(get("/api/tracking/{trackingNumber}/history", shipmentWithoutHistory.getUpsTrackingId()))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data").isEmpty());
    }

    @Test
    @DisplayName("Should prevent users from accessing other users' shipments")
    void testGetUserShipments_AccessControl() throws Exception {
        // Create another user with unique username
        String uniqueSuffix = String.valueOf(System.nanoTime());
        User anotherUser = createUser("anotheruser_" + uniqueSuffix, "another_" + uniqueSuffix + "@example.com", UserRole.USER);
        userRepository.save(anotherUser);

        // Try to access another user's shipments
        mockMvc.perform(get("/api/tracking/user/{userId}", anotherUser.getId())
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isForbidden())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Access denied"));
    }

    @Test
    @DisplayName("Should allow admin to access any user's shipments")
    void testGetUserShipments_AdminAccess() throws Exception {
        mockMvc.perform(get("/api/tracking/user/{userId}", testUser.getId())
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    @DisplayName("Should reflect the last status after sequential updates")
    void testSequentialStatusUpdates_ShouldReflectLastUpdate() throws Exception {
        String requestBody1 = objectMapper.writeValueAsString(Map.of(
                "status", ShipmentStatus.IN_TRANSIT.name(),
                "notes", "First update"
        ));

        String requestBody2 = objectMapper.writeValueAsString(Map.of(
                "status", ShipmentStatus.DELIVERED.name(),
                "notes", "Second update"
        ));

        // First update
        mockMvc.perform(put("/api/tracking/{trackingNumber}/status", testShipment.getUpsTrackingId())
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody1))
                .andExpect(status().isOk());

        // Second update
        mockMvc.perform(put("/api/tracking/{trackingNumber}/status", testShipment.getUpsTrackingId())
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody2))
                .andExpect(status().isOk());

        // Verify final status
        mockMvc.perform(get("/api/tracking/{trackingNumber}", testShipment.getUpsTrackingId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value(ShipmentStatus.DELIVERED.name()));
    }

    /**
     * Helper method to create test user.
     */
    private User createUser(String username, String email, UserRole role) {
        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode("password123"));
        user.setRole(role);
        user.setEnabled(true);
        user.setCreatedAt(LocalDateTime.now());
        return user;
    }

    /**
     * Helper method to create test shipment.
     */
    private Shipment createShipment(User user, String trackingNumber) {
        return createShipment(user, trackingNumber, "SH" + System.currentTimeMillis());
    }
    
    private Shipment createShipment(User user, String trackingNumber, String shipmentId) {
        Shipment shipment = new Shipment();
        shipment.setShipmentId(shipmentId);
        shipment.setUpsTrackingId(trackingNumber);
        shipment.setUser(user);
        shipment.setStatus(ShipmentStatus.CREATED);
        shipment.setWeight(new java.math.BigDecimal("5.0"));
        shipment.setOriginX(10);
        shipment.setOriginY(20);
        shipment.setDestX(15);
        shipment.setDestY(25);
        shipment.setCreatedAt(LocalDateTime.now());
        return shipment;
    }

    /**
     * Helper method to create status history entry.
     */
    private void createStatusHistory(Shipment shipment, ShipmentStatus status, String notes) {
        ShipmentStatusHistory history = new ShipmentStatusHistory();
        history.setShipment(shipment);
        history.setStatus(status);
        history.setNotes(notes);
        history.setTimestamp(LocalDateTime.now());
        statusHistoryRepository.save(history);
    }

    /**
     * Helper method to generate JWT token for user.
     */
    private String generateToken(User user) {
        return jwtTokenProvider.generateToken(user.getUsername());
    }
}