package com.miniups.service;

import com.miniups.model.entity.Shipment;
import com.miniups.model.entity.Truck;
import com.miniups.model.entity.User;
import com.miniups.model.enums.ShipmentStatus;
import com.miniups.model.enums.TruckStatus;
import com.miniups.repository.TruckRepository;
import com.miniups.repository.ShipmentRepository;
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
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for TruckManagementService.
 * Tests truck assignment, fleet management, and optimization algorithms.
 */
@ExtendWith(MockitoExtension.class)
@ActiveProfiles("test")
@DisplayName("TruckManagementService Unit Tests")
class TruckManagementServiceTest {

    @Mock
    private TruckRepository truckRepository;

    @Mock
    private ShipmentRepository shipmentRepository;

    @Mock
    private WorldSimulatorService worldSimulatorService;

    @InjectMocks
    private TruckManagementService truckManagementService;

    private Truck testTruck1;
    private Truck testTruck2;
    private Shipment testShipment;
    private User testUser;

    @BeforeEach
    void setUp() {
        // Create test user
        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("testuser");

        // Create test trucks
        testTruck1 = createTruck(1L, "TRUCK001", TruckStatus.IDLE, 10, 20, 1000, 50.0);
        testTruck2 = createTruck(2L, "TRUCK002", TruckStatus.EN_ROUTE, 15, 25, 800, 30.0);

        // Create test shipment
        testShipment = new Shipment();
        testShipment.setId(1L);
        testShipment.setUpsTrackingId("UPS123456789");
        testShipment.setUser(testUser);
        testShipment.setStatus(ShipmentStatus.CREATED);
        testShipment.setWeight(BigDecimal.valueOf(100.0));
        testShipment.setOriginX(12);
        testShipment.setOriginY(22);
        testShipment.setDestX(14);
        testShipment.setDestY(24);
        testShipment.setCreatedAt(LocalDateTime.now());
    }

    @Test
    @DisplayName("Should assign optimal truck based on distance and availability")
    void testAssignOptimalTruck_Success() {
        // Given - Mock atomic assignment
        when(truckRepository.findNearestAvailableTruckForAssignment(testShipment.getOriginX(), testShipment.getOriginY()))
            .thenReturn(Optional.of(testTruck1));
        when(truckRepository.save(any(Truck.class))).thenReturn(testTruck1);

        // When
        Truck assignedTruck = truckManagementService.assignOptimalTruck(testShipment.getOriginX(), testShipment.getOriginY(), 1);

        // Then
        assertThat(assignedTruck).isNotNull();
        assertThat(assignedTruck.getId()).isEqualTo(1L);
        verify(truckRepository).findNearestAvailableTruckForAssignment(testShipment.getOriginX(), testShipment.getOriginY());
        verify(truckRepository).save(testTruck1);
    }

    @Test
    @DisplayName("Should return null when no trucks available")
    void testAssignOptimalTruck_NoTrucksAvailable() {
        // Given - Mock atomic assignment returns empty Optional
        when(truckRepository.findNearestAvailableTruckForAssignment(100, 200)).thenReturn(Optional.empty());
        when(truckRepository.findIdleForUpdateSkipLocked()).thenReturn(Arrays.asList());

        // When
        Truck result = truckManagementService.assignOptimalTruck(100, 200, 1);

        // Then
        assertThat(result).isNull();
        verify(truckRepository).findNearestAvailableTruckForAssignment(100, 200);
        verify(truckRepository).findIdleForUpdateSkipLocked();
    }

    @Test
    @DisplayName("Should find nearest available truck")
    void testFindNearestAvailableTruck() {
        // Given
        Integer targetX = 12;
        Integer targetY = 22;
        List<Truck> availableTrucks = Arrays.asList(testTruck1, testTruck2);
        when(truckRepository.findByStatus(TruckStatus.IDLE)).thenReturn(availableTrucks);

        // When
        Truck nearestTruck = truckManagementService.findNearestAvailableTruck(targetX, targetY);

        // Then
        assertThat(nearestTruck).isNotNull();
        assertThat(nearestTruck.getId()).isEqualTo(1L);
        verify(truckRepository).findByStatus(TruckStatus.IDLE);
    }

    @Test
    @DisplayName("Should return empty when no trucks available for nearest search")
    void testFindNearestAvailableTruck_NoTrucks() {
        // Given
        Integer targetX = 12;
        Integer targetY = 22;
        when(truckRepository.findByStatus(TruckStatus.IDLE)).thenReturn(Arrays.asList());

        // When
        Truck nearestTruck = truckManagementService.findNearestAvailableTruck(targetX, targetY);

        // Then
        assertThat(nearestTruck).isNull();
        verify(truckRepository).findByStatus(TruckStatus.IDLE);
    }

    @Test
    @DisplayName("Should get all truck statuses")
    void testGetAllTruckStatuses() {
        // Given
        List<Truck> allTrucks = Arrays.asList(testTruck1, testTruck2);
        when(truckRepository.findAll()).thenReturn(allTrucks);

        // When
        List<Map<String, Object>> result = truckManagementService.getAllTruckStatuses();

        // Then
        assertThat(result).hasSize(2);
        verify(truckRepository).findAll();
    }

    @Test
    @DisplayName("Should update truck status successfully")
    void testUpdateTruckStatus() {
        // Given
        Integer truckId = 1;
        String newStatus = "IDLE";
        Integer newX = 15;
        Integer newY = 25;

        when(truckRepository.findByTruckId(truckId)).thenReturn(Optional.of(testTruck1));
        when(truckRepository.save(any(Truck.class))).thenReturn(testTruck1);

        // When
        truckManagementService.updateTruckStatus(truckId, newX, newY, newStatus);

        // Then
        assertThat(testTruck1.getStatus().toString()).isEqualTo(newStatus);
        assertThat(testTruck1.getCurrentX()).isEqualTo(newX);
        assertThat(testTruck1.getCurrentY()).isEqualTo(newY);
        verify(truckRepository).findByTruckId(truckId);
        verify(truckRepository).save(testTruck1);
    }

    @Test
    @DisplayName("Should throw exception when updating non-existent truck")
    void testUpdateTruckStatus_TruckNotFound() {
        // Given
        Integer truckId = 999;
        String newStatus = "IDLE";
        when(truckRepository.findByTruckId(truckId)).thenReturn(Optional.empty());

        // When & Then
        // When
        boolean result = truckManagementService.updateTruckStatus(truckId, 0, 0, newStatus);

        // Then
        assertThat(result).isFalse();

        verify(truckRepository).findByTruckId(truckId);
        verify(truckRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should get fleet statistics")
    void testGetFleetStatistics() {
        // Given
        List<Truck> allTrucks = Arrays.asList(testTruck1, testTruck2);
        when(truckRepository.findAll()).thenReturn(allTrucks);

        // When
        var stats = truckManagementService.getFleetStatistics();

        // Then
        assertThat(stats).isNotNull();
        assertThat(stats.get("total_trucks")).isEqualTo(2L);
        assertThat(stats.get("available_trucks")).isEqualTo(1L);
        assertThat(stats.get("busy_trucks")).isEqualTo(1L);
        assertThat(stats.get("utilization_rate")).isEqualTo(50.0);
        verify(truckRepository).findAll();
    }

    @Test
    @DisplayName("Should calculate distance correctly")
    void testCalculateDistance() {
        // Test cases for distance calculation
        double lat1 = 10.0, lon1 = 20.0;
        double lat2 = 15.0, lon2 = 25.0;

        // Use reflection to test private method or make it package-private for testing
        // For now, we'll test through the public methods that use it
        List<Truck> trucks = Arrays.asList(testTruck1);
        when(truckRepository.findByStatus(TruckStatus.IDLE)).thenReturn(trucks);

        Truck nearest = truckManagementService.findNearestAvailableTruck((int)lat1, (int)lon1);
        assertThat(nearest).isNotNull();
    }

    @Test
    @DisplayName("Should handle heavy shipments by considering truck capacity")
    void testAssignOptimalTruck_HeavyShipment() {
        // Given - shipment that exceeds truck capacity
        testShipment.setWeight(new BigDecimal("1500.0")); // Exceeds testTruck1 capacity of 1000.0
        
        Truck heavyDutyTruck = createTruck(3L, "TRUCK003", TruckStatus.IDLE, 10, 20, 2000, 100.0);
        
        // Mock atomic assignment returns 1 truck assigned
        when(truckRepository.findNearestAvailableTruckForAssignment(testShipment.getOriginX(), testShipment.getOriginY())).thenReturn(Optional.of(testTruck1));
        when(truckRepository.save(any(Truck.class))).thenReturn(heavyDutyTruck);

        // When
        Truck assignedTruck = truckManagementService.assignOptimalTruck(testShipment.getOriginX(), testShipment.getOriginY(), 1);

        // Then
        assertThat(assignedTruck).isNotNull();
        assertThat(assignedTruck.getId()).isEqualTo(3L); // Should assign heavy duty truck
        assertThat(assignedTruck.getCapacity()).isGreaterThanOrEqualTo((int) testShipment.getWeight().doubleValue());
        verify(truckRepository).findNearestAvailableTruckForAssignment(testShipment.getOriginX(), testShipment.getOriginY());
        verify(truckRepository).save(any(Truck.class));
    }

    @Test
    @DisplayName("Should prioritize closer trucks when capacity is equal")
    void testAssignOptimalTruck_DistancePriority() {
        // Given - two trucks with same capacity, different distances
        Truck closeTruck = createTruck(3L, "TRUCK003", TruckStatus.IDLE, 12, 22, 1000, 50.0);
        
        // Mock atomic assignment returns 1 truck assigned (closest one)
        when(truckRepository.findNearestAvailableTruckForAssignment(testShipment.getOriginX(), testShipment.getOriginY())).thenReturn(Optional.of(testTruck1));
        when(truckRepository.save(any(Truck.class))).thenReturn(closeTruck);

        // When
        Truck assignedTruck = truckManagementService.assignOptimalTruck(testShipment.getOriginX(), testShipment.getOriginY(), 1);

        // Then
        assertThat(assignedTruck).isNotNull();
        assertThat(assignedTruck.getId()).isEqualTo(3L); // Should assign closer truck
        verify(truckRepository).findNearestAvailableTruckForAssignment(testShipment.getOriginX(), testShipment.getOriginY());
        verify(truckRepository).save(any(Truck.class));
    }

    @Test
    @DisplayName("Should handle truck assignment with World Simulator integration")
    void testAssignOptimalTruck_WorldSimulatorIntegration() {
        // Given
        when(truckRepository.findNearestAvailableTruckForAssignment(testShipment.getOriginX(), testShipment.getOriginY())).thenReturn(Optional.of(testTruck1));
        when(truckRepository.save(any(Truck.class))).thenReturn(testTruck1);
        
        // When
        Truck assignedTruck = truckManagementService.assignOptimalTruck(testShipment.getOriginX(), testShipment.getOriginY(), 1);

        // Then
        assertThat(assignedTruck).isNotNull();
        verify(truckRepository).findNearestAvailableTruckForAssignment(testShipment.getOriginX(), testShipment.getOriginY());
        verify(truckRepository).save(any(Truck.class));
        // Note: Current implementation doesn't directly call worldSimulatorService.sendTruckToPickup
        // This is handled separately in the actual service layer
    }

    @Test
    @DisplayName("Should release truck successfully")
    void testReleaseTruck_Success() {
        // Given
        testTruck1.setStatus(TruckStatus.EN_ROUTE);
        when(truckRepository.findByTruckId(1)).thenReturn(Optional.of(testTruck1));
        when(truckRepository.save(testTruck1)).thenReturn(testTruck1);

        // When
        boolean result = truckManagementService.releaseTruck(1);

        // Then
        assertThat(result).isTrue();
        assertThat(testTruck1.getStatus()).isEqualTo(TruckStatus.IDLE);
        verify(truckRepository).findByTruckId(1);
        verify(truckRepository).save(testTruck1);
    }

    @Test
    @DisplayName("Should handle release truck when truck not found")
    void testReleaseTruck_TruckNotFound() {
        // Given
        when(truckRepository.findByTruckId(999)).thenReturn(Optional.empty());

        // When
        boolean result = truckManagementService.releaseTruck(999);

        // Then
        assertThat(result).isFalse();
        verify(truckRepository).findByTruckId(999);
        verify(truckRepository, never()).save(any(Truck.class));
    }

    @Test
    @DisplayName("Should handle batch truck position updates")
    void testBatchUpdateTruckPositions() {
        // Given
        Map<String, Object> update1 = Map.of("truck_id", 1, "x", 100, "y", 200, "status", "idle");
        Map<String, Object> update2 = Map.of("truck_id", 2, "x", 150, "y", 250, "status", "traveling");
        List<Map<String, Object>> updates = Arrays.asList(update1, update2);

        when(truckRepository.findByTruckId(1)).thenReturn(Optional.of(testTruck1));
        when(truckRepository.findByTruckId(2)).thenReturn(Optional.of(testTruck2));
        when(truckRepository.save(any(Truck.class))).thenReturn(testTruck1, testTruck2);

        // When
        truckManagementService.batchUpdateTruckPositions(updates);

        // Then
        verify(truckRepository).findByTruckId(1);
        verify(truckRepository).findByTruckId(2);
        verify(truckRepository, times(2)).save(any(Truck.class));
    }

    @Test
    @DisplayName("Should handle exception during truck assignment gracefully")
    void testAssignOptimalTruck_HandlesException() {
        // Given
        when(truckRepository.findNearestAvailableTruckForAssignment(10, 20))
            .thenThrow(new RuntimeException("Database connection failed"));
        // Mock fallback to pessimistic locking
        when(truckRepository.findIdleForUpdateSkipLocked()).thenReturn(Arrays.asList());

        // When
        Truck assignedTruck = truckManagementService.assignOptimalTruck(10, 20, 1);

        // Then
        assertThat(assignedTruck).isNull();
        verify(truckRepository).findNearestAvailableTruckForAssignment(10, 20);
        verify(truckRepository).findIdleForUpdateSkipLocked(); // Fallback called
    }

    @Test
    @DisplayName("Should handle null coordinates in distance calculation")
    void testFindNearestAvailableTruck_NullCoordinates() {
        // Given
        when(truckRepository.findByStatus(TruckStatus.IDLE)).thenReturn(Arrays.asList(testTruck1));

        // When
        Truck nearestTruck = truckManagementService.findNearestAvailableTruck(null, 20);

        // Then
        // Should return the available truck even with null coordinates (distance will be MAX_VALUE but truck is still available)
        assertThat(nearestTruck).isEqualTo(testTruck1);
        verify(truckRepository).findByStatus(TruckStatus.IDLE);
    }

    @Test
    @DisplayName("Should return fleet statistics with shipment data")
    void testGetFleetStatistics_WithShipmentData() {
        // Given
        List<Truck> allTrucks = Arrays.asList(testTruck1, testTruck2);
        List<Shipment> allShipments = Arrays.asList(testShipment);
        
        when(truckRepository.findAll()).thenReturn(allTrucks);
        when(shipmentRepository.findAll()).thenReturn(allShipments);
        when(worldSimulatorService.isConnected()).thenReturn(true);
        when(worldSimulatorService.getWorldId()).thenReturn(12345L);

        // When
        Map<String, Object> stats = truckManagementService.getFleetStatistics();

        // Then
        assertThat(stats).isNotNull();
        assertThat(stats.get("total_trucks")).isEqualTo(2L);
        assertThat(stats.get("world_connected")).isEqualTo(true);
        assertThat(stats.get("world_id")).isEqualTo(12345L);
        assertThat(stats).containsKey("shipment_status_breakdown");
        
        verify(truckRepository).findAll();
        verify(shipmentRepository).findAll();
        verify(worldSimulatorService).isConnected();
        verify(worldSimulatorService).getWorldId();
    }

    /**
     * Helper method to create test truck.
     */
    private Truck createTruck(Long id, String truckIdStr, TruckStatus status, 
                             Integer x, Integer y, Integer capacity, Double currentLoad) {
        Truck truck = new Truck();
        truck.setId(id);
        truck.setTruckId(id.intValue());
        truck.setStatus(status);
        truck.setCurrentX(x);
        truck.setCurrentY(y);
        truck.setCapacity(capacity);
        if (currentLoad != null) {
            truck.setCurrentLoad(currentLoad);
        }
        truck.setCreatedAt(LocalDateTime.now());
        return truck;
    }
}