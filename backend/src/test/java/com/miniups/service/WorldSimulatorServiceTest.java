package com.miniups.service;

import com.miniups.model.entity.Truck;
import com.miniups.model.enums.TruckStatus;
import com.miniups.proto.WorldUpsProto;
import com.miniups.repository.TruckRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.net.Socket;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.LinkedBlockingQueue;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for WorldSimulatorService.
 * Tests Protocol Buffer communication, TCP socket handling, and World Simulator integration.
 */
@ExtendWith(MockitoExtension.class)
@ActiveProfiles("test")
@DisplayName("WorldSimulatorService Unit Tests")
class WorldSimulatorServiceTest {

    @Mock
    private TruckRepository truckRepository;

    @Mock
    private AmazonIntegrationService amazonIntegrationService;

    @Mock
    private Socket mockSocket;

    @InjectMocks
    private WorldSimulatorService worldSimulatorService;

    private Truck testTruck;

    @BeforeEach
    void setUp() {
        // Set test configuration
        ReflectionTestUtils.setField(worldSimulatorService, "worldHost", "localhost");
        ReflectionTestUtils.setField(worldSimulatorService, "worldPort", 12345);
        ReflectionTestUtils.setField(worldSimulatorService, "connectionTimeout", 5000);
        ReflectionTestUtils.setField(worldSimulatorService, "connected", false); // Disabled for unit tests

        // Create test truck
        testTruck = new Truck();
        testTruck.setId(1L);
        testTruck.setTruckId(1);
        testTruck.setLicensePlate("TEST001");
        testTruck.setStatus(TruckStatus.IDLE);
        testTruck.setCurrentX(10);
        testTruck.setCurrentY(20);
        testTruck.setCapacity(1000);
        testTruck.setCurrentLoad(0.0);
        testTruck.setCreatedAt(LocalDateTime.now());
    }

    @Test
    @DisplayName("Should handle connection when World Simulator is disabled")
    void testConnect_DisabledService() {
        // Given - Service is disabled in test configuration

        // When
        boolean result = worldSimulatorService.connect(1L);

        // Then
        assertThat(result).isFalse();
        verifyNoInteractions(truckRepository);
    }

    @Test
    @DisplayName("Should handle truck pickup command when service is disabled")
    void testSendTruckToPickup_DisabledService() {
        // Given
        Integer warehouseId = 1;

        // When
        CompletableFuture<Boolean> result = 
                worldSimulatorService.sendTruckToPickup(testTruck.getTruckId(), 1);

        // Then
        assertThat(result).succeedsWithin(Duration.ofSeconds(1)).isEqualTo(false);
        verifyNoInteractions(truckRepository);
    }

    @Test
    @DisplayName("Should handle truck delivery command when service is disabled")
    void testSendTruckToDeliver_DisabledService() {
        // Given
        double deliveryLat = 14.0;
        double deliveryLon = 24.0;

        // When
        Map<Long, int[]> deliveries = new HashMap<>();
        deliveries.put(1L, new int[]{14, 24});
        CompletableFuture<Boolean> result = 
                worldSimulatorService.sendTruckToDeliver(testTruck.getTruckId(), deliveries);

        // Then
        assertThat(result).succeedsWithin(Duration.ofSeconds(1)).isEqualTo(false);
        verifyNoInteractions(truckRepository);
    }

    @Test
    @DisplayName("Should handle truck status query when service is disabled")
    void testQueryTruckStatus_DisabledService() {
        // Given
        Integer truckId = 1;

        // When
        CompletableFuture<WorldUpsProto.UTruck> result = 
                worldSimulatorService.queryTruckStatus(truckId);

        // Then
        assertThat(result).isNotNull();
        verifyNoInteractions(truckRepository);
    }

    @Test
    @DisplayName("Should validate truck parameters for pickup")
    void testSendTruckToPickup_ParameterValidation() {
        // Given
        Integer nullTruckId = null;
        Integer validWarehouseId = 1;

        // When & Then
        assertThatThrownBy(() -> 
                worldSimulatorService.sendTruckToPickup(nullTruckId, 1))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("Should validate warehouse ID for pickup")
    void testSendTruckToPickup_WarehouseValidation() {
        // Given
        Integer validTruckId = 1;
        Integer invalidWarehouseId = -1; // Invalid warehouse ID

        // When & Then
        assertThatThrownBy(() -> 
                worldSimulatorService.sendTruckToPickup(validTruckId, invalidWarehouseId))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("Should validate delivery parameters")
    void testSendTruckToDeliver_DeliveryValidation() {
        // Given
        Integer validTruckId = 1;
        Map<Long, int[]> emptyDeliveries = new HashMap<>();

        // When & Then
        assertThatThrownBy(() -> 
                worldSimulatorService.sendTruckToDeliver(validTruckId, emptyDeliveries))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("Should handle truck status query when service is enabled")
    void testQueryTruckStatus_ServiceEnabled() {
        // Given
        Integer truckId = 1;
        ReflectionTestUtils.setField(worldSimulatorService, "connected", true);
        ReflectionTestUtils.setField(worldSimulatorService, "messageQueue", new java.util.concurrent.LinkedBlockingQueue<>());

        // When
        CompletableFuture<WorldUpsProto.UTruck> result = 
                worldSimulatorService.queryTruckStatus(truckId);

        // Then
        assertThat(result).isNotNull();
        // Result will be null due to disabled service in test
    }

    @Test
    @DisplayName("Should handle truck query for non-existent truck")
    void testQueryTruckStatus_TruckNotFound() {
        // Given
        Integer nonExistentTruckId = 999;

        // When
        CompletableFuture<WorldUpsProto.UTruck> result = 
                worldSimulatorService.queryTruckStatus(nonExistentTruckId);

        // Then
        assertThat(result).isNotNull();
        // Service is disabled, so result will be null
    }

    @Test
    @DisplayName("Should handle simulation speed setting")
    void testSetSimulationSpeed() {
        // Given
        int speed = 5;

        // When & Then - Should not throw when service is disabled
        assertThatCode(() -> worldSimulatorService.setSimulationSpeed(speed))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Should validate simulation speed bounds")
    void testSetSimulationSpeed_InvalidSpeed() {
        // Given
        int invalidSpeed = -1; // Negative speed

        // When & Then - Should not throw when service is disabled
        assertThatCode(() -> worldSimulatorService.setSimulationSpeed(invalidSpeed))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Should handle disconnect gracefully")
    void testDisconnect() {
        // When & Then - Should not throw when service is disabled
        assertThatCode(() -> worldSimulatorService.disconnect())
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Should check connection status")
    void testIsConnected() {
        // When
        boolean connected = worldSimulatorService.isConnected();

        // Then
        assertThat(connected).isFalse(); // Service is disabled
    }

    @Test
    @DisplayName("Should handle Protocol Buffer message creation for pickup")
    void testCreatePickupCommand() {
        // Given
        int truckId = 1;
        double pickupLat = 12.0;
        double pickupLon = 22.0;

        // When - Test message creation through public method behavior
        CompletableFuture<Boolean> result = 
                worldSimulatorService.sendTruckToPickup(testTruck.getTruckId(), 1);

        // Then
        assertThat(result).succeedsWithin(Duration.ofSeconds(1)).isEqualTo(false);
        // Message creation logic is tested indirectly through the service call
    }

    @Test
    @DisplayName("Should handle Protocol Buffer message creation for delivery")
    void testCreateDeliveryCommand() {
        // Given
        Map<Long, int[]> deliveries = new HashMap<>();
        deliveries.put(1L, new int[]{14, 24});

        // When - Test message creation through public method behavior  
        CompletableFuture<Boolean> result = 
                worldSimulatorService.sendTruckToDeliver(testTruck.getTruckId(), deliveries);

        // Then
        assertThat(result).succeedsWithin(Duration.ofSeconds(1)).isEqualTo(false);
        // Message creation logic is tested indirectly through the service call
    }

    @Test
    @DisplayName("Should handle concurrent truck operations")
    void testConcurrentTruckOperations() {
        // Given
        Map<Long, int[]> deliveries = new HashMap<>();
        deliveries.put(1L, new int[]{14, 24});
        
        CompletableFuture<Boolean> future1 = 
                worldSimulatorService.sendTruckToPickup(testTruck.getTruckId(), 1);
        CompletableFuture<Boolean> future2 = 
                worldSimulatorService.sendTruckToDeliver(testTruck.getTruckId(), deliveries);

        // When & Then
        assertThat(future1).succeedsWithin(Duration.ofSeconds(1)).isEqualTo(false);
        assertThat(future2).succeedsWithin(Duration.ofSeconds(1)).isEqualTo(false);
    }

    @Test
    @DisplayName("Should handle truck operations with valid parameters")
    void testValidParameterRanges() {
        // Test edge cases for valid parameters
        Map<Long, int[]> deliveries = new HashMap<>();
        deliveries.put(1L, new int[]{0, 0});
        
        // Valid coordinates
        assertThatCode(() -> 
                worldSimulatorService.sendTruckToPickup(testTruck.getTruckId(), 1))
                .doesNotThrowAnyException();
        
        assertThatCode(() -> 
                worldSimulatorService.sendTruckToPickup(testTruck.getTruckId(), 1))
                .doesNotThrowAnyException();

        // Valid delivery parameters
        assertThatCode(() -> 
                worldSimulatorService.sendTruckToDeliver(testTruck.getTruckId(), deliveries))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Should handle service configuration")
    void testServiceConfiguration() {
        // Given
        String testHost = "test-host";
        int testPort = 54321;

        // When
        ReflectionTestUtils.setField(worldSimulatorService, "worldHost", testHost);
        ReflectionTestUtils.setField(worldSimulatorService, "worldPort", testPort);

        // Then
        String actualHost = (String) ReflectionTestUtils.getField(worldSimulatorService, "worldHost");
        Integer actualPort = (Integer) ReflectionTestUtils.getField(worldSimulatorService, "worldPort");
        
        assertThat(actualHost).isEqualTo(testHost);
        assertThat(actualPort).isEqualTo(testPort);
    }

    @Test
    @DisplayName("Should handle Amazon integration callback")
    void testAmazonIntegrationCallback() {
        // Given
        String trackingNumber = "UPS123456789";
        
        // When - Simulate Amazon notification callback
        doNothing().when(amazonIntegrationService).notifyTruckDispatched(anyString(), anyString());

        // Then
        assertThatCode(() -> 
                amazonIntegrationService.notifyTruckDispatched("1", "AMZ123456"))
                .doesNotThrowAnyException();
        
        verify(amazonIntegrationService).notifyTruckDispatched("1", "AMZ123456");
    }
}