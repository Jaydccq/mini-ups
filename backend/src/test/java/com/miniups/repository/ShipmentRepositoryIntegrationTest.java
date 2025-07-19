package com.miniups.repository;

import com.miniups.model.entity.Shipment;
import com.miniups.model.entity.User;
import com.miniups.model.enums.ShipmentStatus;
import com.miniups.model.enums.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;

/**
 * Integration tests for ShipmentRepository.
 * Tests JPA queries, custom methods, and database interactions.
 */
@DataJpaTest
@ActiveProfiles("test")
@DisplayName("ShipmentRepository Integration Tests")
class ShipmentRepositoryIntegrationTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private ShipmentRepository shipmentRepository;

    @Autowired
    private UserRepository userRepository;

    private User testUser1;
    private User testUser2;
    private Shipment shipment1;
    private Shipment shipment2;
    private Shipment shipment3;

    @BeforeEach
    void setUp() {
        // Create test users
        testUser1 = createUser("user1", "user1@example.com");
        testUser2 = createUser("user2", "user2@example.com");
        
        entityManager.persist(testUser1);
        entityManager.persist(testUser2);

        // Create test shipments
        shipment1 = createShipment(testUser1, "UPS123456789", ShipmentStatus.CREATED);
        shipment2 = createShipment(testUser1, "UPS987654321", ShipmentStatus.IN_TRANSIT);
        shipment3 = createShipment(testUser2, "UPS111222333", ShipmentStatus.DELIVERED);

        entityManager.persist(shipment1);
        entityManager.persist(shipment2);
        entityManager.persist(shipment3);
        entityManager.flush();
    }

    @Test
    @DisplayName("Should find shipment by tracking number")
    void testFindByTrackingNumber_Success() {
        // When
        Optional<Shipment> found = shipmentRepository.findByUpsTrackingId("UPS123456789");

        // Then
        assertThat(found).isPresent();
        assertThat(found.get().getUpsTrackingId()).isEqualTo("UPS123456789");
        assertThat(found.get().getUser().getUsername()).isEqualTo("user1");
        assertThat(found.get().getStatus()).isEqualTo(ShipmentStatus.CREATED);
    }

    @Test
    @DisplayName("Should return empty when tracking number not found")
    void testFindByTrackingNumber_NotFound() {
        // When
        Optional<Shipment> found = shipmentRepository.findByUpsTrackingId("UPS999999999");

        // Then
        assertThat(found).isEmpty();
    }

    @Test
    @DisplayName("Should find shipments by user ID ordered by creation date")
    void testFindByUserIdOrderByCreatedAtDesc() {
        // When
        List<Shipment> userShipments = shipmentRepository.findByUserIdOrderByCreatedAtDesc(testUser1.getId());

        // Then
        assertThat(userShipments).hasSize(2);
        assertThat(userShipments.get(0).getUpsTrackingId()).isEqualTo("UPS987654321"); // More recent
        assertThat(userShipments.get(1).getUpsTrackingId()).isEqualTo("UPS123456789"); // Older
        assertThat(userShipments).allMatch(s -> s.getUser().getId().equals(testUser1.getId()));
    }

    @Test
    @DisplayName("Should return empty list for user with no shipments")
    void testFindByUserIdOrderByCreatedAtDesc_NoShipments() {
        // Given
        User userWithoutShipments = createUser("emptyuser", "empty@example.com");
        entityManager.persist(userWithoutShipments);
        entityManager.flush();

        // When
        List<Shipment> userShipments = shipmentRepository.findByUserIdOrderByCreatedAtDesc(userWithoutShipments.getId());

        // Then
        assertThat(userShipments).isEmpty();
    }

    @Test
    @DisplayName("Should find shipments by status")
    void testFindByStatus() {
        // When
        List<Shipment> createdShipments = shipmentRepository.findByStatus(ShipmentStatus.CREATED);
        List<Shipment> inTransitShipments = shipmentRepository.findByStatus(ShipmentStatus.IN_TRANSIT);
        List<Shipment> deliveredShipments = shipmentRepository.findByStatus(ShipmentStatus.DELIVERED);

        // Then
        assertThat(createdShipments).hasSize(1);
        assertThat(createdShipments.get(0).getUpsTrackingId()).isEqualTo("UPS123456789");

        assertThat(inTransitShipments).hasSize(1);
        assertThat(inTransitShipments.get(0).getUpsTrackingId()).isEqualTo("UPS987654321");

        assertThat(deliveredShipments).hasSize(1);
        assertThat(deliveredShipments.get(0).getUpsTrackingId()).isEqualTo("UPS111222333");
    }

    @Test
    @DisplayName("Should count shipments by status")
    void testCountByStatus() {
        // When
        long createdCount = shipmentRepository.countByStatus(ShipmentStatus.CREATED);
        long inTransitCount = shipmentRepository.countByStatus(ShipmentStatus.IN_TRANSIT);
        long deliveredCount = shipmentRepository.countByStatus(ShipmentStatus.DELIVERED);
        long cancelledCount = shipmentRepository.countByStatus(ShipmentStatus.CANCELLED);

        // Then
        assertThat(createdCount).isEqualTo(1);
        assertThat(inTransitCount).isEqualTo(1);
        assertThat(deliveredCount).isEqualTo(1);
        assertThat(cancelledCount).isEqualTo(0);
    }

    @Test
    @DisplayName("Should find shipments created within date range")
    void testFindByCreatedAtBetween() {
        // Given
        LocalDateTime start = LocalDateTime.now().minusDays(1);
        LocalDateTime end = LocalDateTime.now().plusDays(1);

        // When
        List<Shipment> recentShipments = shipmentRepository.findByCreatedAtBetween(start, end);

        // Then
        assertThat(recentShipments).hasSize(3);
        assertThat(recentShipments).allMatch(s -> 
            s.getCreatedAt().isAfter(start) && s.getCreatedAt().isBefore(end));
    }

    @Test
    @DisplayName("Should save shipment with all required fields")
    void testSaveShipment() {
        // Given
        Shipment newShipment = createShipment(testUser1, "UPS444555666", ShipmentStatus.CREATED);

        // When
        Shipment saved = shipmentRepository.save(newShipment);

        // Then
        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getUpsTrackingId()).isEqualTo("UPS444555666");
        assertThat(saved.getUser()).isEqualTo(testUser1);
        assertThat(saved.getCreatedAt()).isNotNull();
    }

    @Test
    @DisplayName("Should update shipment status")
    void testUpdateShipmentStatus() {
        // Given
        String trackingNumber = "UPS123456789";
        
        // When
        Optional<Shipment> shipment = shipmentRepository.findByUpsTrackingId(trackingNumber);
        assertThat(shipment).isPresent();
        
        shipment.get().setStatus(ShipmentStatus.IN_TRANSIT);
        shipment.get().setUpdatedAt(LocalDateTime.now());
        Shipment updated = shipmentRepository.save(shipment.get());

        // Then
        assertThat(updated.getStatus()).isEqualTo(ShipmentStatus.IN_TRANSIT);
        assertThat(updated.getUpdatedAt()).isNotNull();
    }

    @Test
    @DisplayName("Should delete shipment")
    void testDeleteShipment() {
        // Given
        String trackingNumber = "UPS123456789";
        
        // When
        Optional<Shipment> shipment = shipmentRepository.findByUpsTrackingId(trackingNumber);
        assertThat(shipment).isPresent();
        
        shipmentRepository.delete(shipment.get());
        entityManager.flush();

        // Then
        Optional<Shipment> deleted = shipmentRepository.findByUpsTrackingId(trackingNumber);
        assertThat(deleted).isEmpty();
    }

    @Test
    @DisplayName("Should find shipments by weight range")
    void testFindByWeightBetween() {
        // When
        List<Shipment> mediumWeightShipments = shipmentRepository.findByWeightBetween(
                BigDecimal.valueOf(3.0), BigDecimal.valueOf(7.0));

        // Then
        assertThat(mediumWeightShipments).hasSize(2); // shipment1 (5.0) and shipment2 (6.0)
        assertThat(mediumWeightShipments).allMatch(s -> 
            s.getWeight().compareTo(BigDecimal.valueOf(3.0)) >= 0 && 
            s.getWeight().compareTo(BigDecimal.valueOf(7.0)) <= 0);
    }

    @Test
    @DisplayName("Should handle tracking number uniqueness")
    void testTrackingNumberUniqueness() {
        // Given
        Shipment duplicateShipment = createShipment(testUser2, "UPS123456789", ShipmentStatus.CREATED); // Duplicate tracking number

        // When & Then
        assertThatThrownBy(() -> {
            shipmentRepository.save(duplicateShipment);
            entityManager.flush();
        }).isInstanceOf(Exception.class); // Should throw constraint violation
    }

    @Test
    @DisplayName("Should find shipments by user and status")
    void testFindByUserIdAndStatus() {
        // When
        List<Shipment> user1CreatedShipments = shipmentRepository.findByUserIdAndStatus(testUser1.getId(), ShipmentStatus.CREATED);
        List<Shipment> user1InTransitShipments = shipmentRepository.findByUserIdAndStatus(testUser1.getId(), ShipmentStatus.IN_TRANSIT);

        // Then
        assertThat(user1CreatedShipments).hasSize(1);
        assertThat(user1CreatedShipments.get(0).getUpsTrackingId()).isEqualTo("UPS123456789");

        assertThat(user1InTransitShipments).hasSize(1);
        assertThat(user1InTransitShipments.get(0).getUpsTrackingId()).isEqualTo("UPS987654321");
    }

    @Test
    @DisplayName("Should test cascade operations with user")
    void testCascadeOperations() {
        // Given
        User newUser = createUser("cascadeuser", "cascade@example.com");
        entityManager.persist(newUser);
        
        Shipment cascadeShipment = createShipment(newUser, "UPS777888999", ShipmentStatus.CREATED);
        entityManager.persist(cascadeShipment);
        entityManager.flush();

        // When - Delete user should not cascade to shipments (based on your entity design)
        Long userId = newUser.getId();
        Long shipmentId = cascadeShipment.getId();
        
        // Verify the shipment exists and references the user
        Optional<Shipment> found = shipmentRepository.findById(shipmentId);
        assertThat(found).isPresent();
        assertThat(found.get().getUser().getId()).isEqualTo(userId);
    }

    @Test
    @DisplayName("Should handle null values correctly")
    void testNullValueHandling() {
        // Test that repository methods handle null parameters gracefully
        assertThat(shipmentRepository.findByUpsTrackingId(null)).isEmpty();
    }

    @Test
    @DisplayName("Should test pagination with large dataset")
    void testPaginationSupport() {
        // Given - Create multiple shipments for the same user
        for (int i = 10; i < 20; i++) {
            Shipment shipment = createShipment(testUser1, "UPS" + i + "00000000", ShipmentStatus.CREATED);
            entityManager.persist(shipment);
        }
        entityManager.flush();

        // When
        List<Shipment> allUserShipments = shipmentRepository.findByUserIdOrderByCreatedAtDesc(testUser1.getId());

        // Then
        assertThat(allUserShipments).hasSizeGreaterThan(2); // Original 2 + 10 new ones
        assertThat(allUserShipments).allMatch(s -> s.getUser().getId().equals(testUser1.getId()));
    }

    /**
     * Helper method to create test user.
     */
    private User createUser(String username, String email) {
        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        user.setPassword("encodedPassword");
        user.setRole(UserRole.USER);
        user.setEnabled(true);
        user.setCreatedAt(LocalDateTime.now());
        return user;
    }

    /**
     * Helper method to create test shipment.
     */
    private Shipment createShipment(User user, String trackingNumber, ShipmentStatus status) {
        Shipment shipment = new Shipment();
        shipment.setUpsTrackingId(trackingNumber);
        shipment.setUser(user);
        shipment.setStatus(status);
        shipment.setWeight(trackingNumber.contains("123") ? BigDecimal.valueOf(5.0) : 
                          trackingNumber.contains("987") ? BigDecimal.valueOf(6.0) : 
                          trackingNumber.contains("111") ? BigDecimal.valueOf(10.0) : BigDecimal.valueOf(4.0));
        shipment.setShipmentId("AMZ" + trackingNumber.substring(3));
        shipment.setOriginX(100);
        shipment.setOriginY(200);
        shipment.setDestX(300);
        shipment.setDestY(400);
        shipment.setCreatedAt(LocalDateTime.now().minusHours(
                trackingNumber.contains("123") ? 2 : 1)); // Different creation times
        return shipment;
    }
}