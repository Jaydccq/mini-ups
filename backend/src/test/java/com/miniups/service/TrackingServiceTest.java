package com.miniups.service;

import com.miniups.model.entity.Shipment;
import com.miniups.model.entity.ShipmentStatusHistory;
import com.miniups.model.entity.User;
import com.miniups.model.enums.ShipmentStatus;
import com.miniups.repository.ShipmentRepository;
import com.miniups.repository.ShipmentStatusHistoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for TrackingService.
 * Tests tracking number generation, package lookup, status updates, and history tracking.
 */
@ExtendWith(MockitoExtension.class)
@ActiveProfiles("test")
@DisplayName("TrackingService Unit Tests")
class TrackingServiceTest {

    @Mock
    private ShipmentRepository shipmentRepository;

    @Mock
    private ShipmentStatusHistoryRepository statusHistoryRepository;

    @InjectMocks
    private TrackingService trackingService;

    private Shipment testShipment;
    private User testUser;

    @BeforeEach
    void setUp() {
        // Create test user
        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("testuser");
        testUser.setEmail("test@example.com");

        // Create test shipment
        testShipment = new Shipment();
        testShipment.setId(1L);
        testShipment.setShipmentId("SH123456");
        testShipment.setUpsTrackingId("UPS123456789");
        testShipment.setUser(testUser);
        testShipment.setStatus(ShipmentStatus.CREATED);
        testShipment.setWeight(new BigDecimal("5.0"));
        testShipment.setCreatedAt(LocalDateTime.now());
    }

    @Test
    @DisplayName("Should generate valid tracking number")
    void testGenerateTrackingNumber() {
        // When
        String trackingNumber = trackingService.generateTrackingNumber();

        // Then
        assertThat(trackingNumber).isNotNull();
        assertThat(trackingNumber).startsWith("UPS");
        assertThat(trackingNumber).hasSize(21); // UPS + 14 digits timestamp + 4 random
        assertThat(trackingService.isValidTrackingNumberFormat(trackingNumber)).isTrue();
    }

    @Test
    @DisplayName("Should find shipment by valid tracking number")
    void testFindByTrackingNumber_Success() {
        // Given
        String trackingNumber = "UPS123456789";
        when(shipmentRepository.findByUpsTrackingId(trackingNumber))
                .thenReturn(Optional.of(testShipment));

        // When
        Optional<Shipment> result = trackingService.findByTrackingNumber(trackingNumber);

        // Then
        assertThat(result).isPresent();
        assertThat(result.get().getUpsTrackingId()).isEqualTo(trackingNumber);
        verify(shipmentRepository).findByUpsTrackingId(trackingNumber);
    }

    @Test
    @DisplayName("Should return empty when tracking number not found")
    void testFindByTrackingNumber_NotFound() {
        // Given
        String trackingNumber = "UPS999999999";
        when(shipmentRepository.findByUpsTrackingId(trackingNumber))
                .thenReturn(Optional.empty());

        // When
        Optional<Shipment> result = trackingService.findByTrackingNumber(trackingNumber);

        // Then
        assertThat(result).isEmpty();
        verify(shipmentRepository).findByUpsTrackingId(trackingNumber);
    }

    @Test
    @DisplayName("Should update shipment status successfully")
    void testUpdateShipmentStatus_Success() {
        // Given
        String trackingNumber = "UPS123456789";
        ShipmentStatus newStatus = ShipmentStatus.IN_TRANSIT;
        String notes = "Package picked up";

        when(shipmentRepository.findByUpsTrackingId(trackingNumber))
                .thenReturn(Optional.of(testShipment));
        when(shipmentRepository.save(any(Shipment.class))).thenReturn(testShipment);

        // When
        trackingService.updateShipmentStatus(trackingNumber, newStatus, notes);

        // Then
        assertThat(testShipment.getStatus()).isEqualTo(newStatus);
        verify(shipmentRepository).findByUpsTrackingId(trackingNumber);
        verify(shipmentRepository).save(testShipment);
        verify(statusHistoryRepository).save(any(ShipmentStatusHistory.class));
    }

    @Test
    @DisplayName("Should throw exception when updating non-existent shipment")
    void testUpdateShipmentStatus_ShipmentNotFound() {
        // Given
        String trackingNumber = "UPS999999999";
        ShipmentStatus newStatus = ShipmentStatus.IN_TRANSIT;
        String notes = "Package picked up";

        when(shipmentRepository.findByUpsTrackingId(trackingNumber))
                .thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> 
            trackingService.updateShipmentStatus(trackingNumber, newStatus, notes))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Shipment not found");

        verify(shipmentRepository).findByUpsTrackingId(trackingNumber);
        verify(shipmentRepository, never()).save(any());
        verify(statusHistoryRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should get status history for shipment")
    void testGetStatusHistory() {
        // Given
        String trackingNumber = "UPS123456789";
        List<ShipmentStatusHistory> mockHistory = Arrays.asList(
                createStatusHistory(ShipmentStatus.CREATED, "Order created"),
                createStatusHistory(ShipmentStatus.IN_TRANSIT, "Package picked up")
        );

        when(statusHistoryRepository.findByShipmentIdOrderByTimestampDesc(1L))
                .thenReturn(mockHistory);

        // When
        List<ShipmentStatusHistory> result = trackingService.getStatusHistory(trackingNumber);

        // Then
        assertThat(result).hasSize(2);
        assertThat(result.get(0).getStatus()).isEqualTo(ShipmentStatus.CREATED);
        assertThat(result.get(1).getStatus()).isEqualTo(ShipmentStatus.IN_TRANSIT);
        verify(statusHistoryRepository).findByShipmentIdOrderByTimestampDesc(1L);
    }

    @Test
    @DisplayName("Should validate tracking number format correctly")
    void testTrackingNumberValidation() {
        // Valid tracking numbers
        assertThat(trackingService.isValidTrackingNumberFormat("UPS123456789")).isTrue();
        assertThat(trackingService.isValidTrackingNumberFormat("UPS000000001")).isTrue();

        // Invalid tracking numbers
        assertThat(trackingService.isValidTrackingNumberFormat("ABC123456789")).isFalse();
        assertThat(trackingService.isValidTrackingNumberFormat("UPS12345")).isFalse();
        assertThat(trackingService.isValidTrackingNumberFormat("UPS12345678901")).isFalse();
        assertThat(trackingService.isValidTrackingNumberFormat("ups123456789")).isFalse();
        assertThat(trackingService.isValidTrackingNumberFormat("")).isFalse();
        assertThat(trackingService.isValidTrackingNumberFormat(null)).isFalse();
    }

    @Test
    @DisplayName("Should get user shipments successfully")
    void testGetUserShipments() {
        // Given
        Long userId = 1L;
        List<Shipment> mockShipments = Arrays.asList(testShipment, createSecondShipment());

        when(shipmentRepository.findByUserIdOrderByCreatedAtDesc(userId))
                .thenReturn(mockShipments);

        // When
        List<Shipment> result = trackingService.getUserShipments(userId);

        // Then
        assertThat(result).hasSize(2);
        assertThat(result.get(0).getUser().getId()).isEqualTo(userId);
        verify(shipmentRepository).findByUserIdOrderByCreatedAtDesc(userId);
    }

    @Test
    @DisplayName("Should handle concurrent status updates correctly")
    void testConcurrentStatusUpdate() {
        // Given
        String trackingNumber = "UPS123456789";
        testShipment.setVersion(1L); // Simulate optimistic locking

        when(shipmentRepository.findByUpsTrackingId(trackingNumber))
                .thenReturn(Optional.of(testShipment));
        when(shipmentRepository.save(any(Shipment.class)))
                .thenThrow(new RuntimeException("Optimistic locking failure"));

        // When & Then
        assertThatThrownBy(() -> 
            trackingService.updateShipmentStatus(trackingNumber, ShipmentStatus.IN_TRANSIT, "Test"))
                .isInstanceOf(RuntimeException.class);
    }

    @Test
    @DisplayName("Should generate unique tracking numbers")
    void testTrackingNumberUniqueness() {
        // When
        String tracking1 = trackingService.generateTrackingNumber();
        String tracking2 = trackingService.generateTrackingNumber();
        String tracking3 = trackingService.generateTrackingNumber();

        // Then
        assertThat(tracking1).isNotEqualTo(tracking2);
        assertThat(tracking2).isNotEqualTo(tracking3);
        assertThat(tracking1).isNotEqualTo(tracking3);
    }

    /**
     * Helper method to create status history entry.
     */
    private ShipmentStatusHistory createStatusHistory(ShipmentStatus status, String notes) {
        ShipmentStatusHistory history = new ShipmentStatusHistory();
        history.setId(1L);
        history.setShipment(testShipment);
        history.setStatus(status);
        history.setNotes(notes);
        history.setTimestamp(LocalDateTime.now());
        return history;
    }

    /**
     * Helper method to create second test shipment.
     */
    private Shipment createSecondShipment() {
        Shipment shipment = new Shipment();
        shipment.setId(2L);
        shipment.setShipmentId("SH987654");
        shipment.setUpsTrackingId("UPS987654321");
        shipment.setUser(testUser);
        shipment.setStatus(ShipmentStatus.IN_TRANSIT);
        shipment.setWeight(new BigDecimal("3.0"));
        shipment.setCreatedAt(LocalDateTime.now().minusHours(1));
        return shipment;
    }
}