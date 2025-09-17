package com.miniups.network.netty;

import com.miniups.model.entity.Shipment;
import com.miniups.model.entity.Truck;
import com.miniups.model.enums.ShipmentStatus;
import com.miniups.model.enums.TruckStatus;
import com.miniups.network.netty.handler.MessageHandlerService;
import com.miniups.proto.WorldUpsProto.UFinished;
import com.miniups.proto.WorldUpsProto.UDeliveryMade;
import com.miniups.proto.WorldUpsProto.UTruck;
import com.miniups.proto.WorldUpsProto.UErr;
import com.miniups.repository.ShipmentRepository;
import com.miniups.repository.TruckRepository;
import com.miniups.service.AmazonIntegrationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for MessageHandlerService.
 * 
 * These tests validate the business logic for processing World Simulator
 * messages and database operations.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Message Handler Service Tests")
class MessageHandlerServiceTest {

    @Mock
    private TruckRepository mockTruckRepository;

    @Mock
    private ShipmentRepository mockShipmentRepository;

    @Mock
    private AmazonIntegrationService mockAmazonIntegrationService;

    private MessageHandlerService messageHandlerService;

    private Truck testTruck;
    private Shipment testShipment;

    @BeforeEach
    void setUp() {
        messageHandlerService = new MessageHandlerService(
            mockTruckRepository, 
            mockShipmentRepository, 
            mockAmazonIntegrationService
        );

        // Create test entities
        testTruck = new Truck();
        testTruck.setId(1L);
        testTruck.setTruckId(100);
        testTruck.setCurrentX(0);
        testTruck.setCurrentY(0);
        testTruck.setStatus(TruckStatus.EN_ROUTE);

        testShipment = new Shipment();
        testShipment.setId(1L);
        testShipment.setShipmentId("SHIP123");
        testShipment.setStatus(ShipmentStatus.IN_TRANSIT);
        testShipment.setTruck(testTruck);
    }

    @Test
    @DisplayName("Should handle truck completion with idle status")
    void testHandleTruckCompletionIdle() {
        // Arrange
        UFinished completion = UFinished.newBuilder()
            .setTruckid(100)
            .setX(50)
            .setY(60)
            .setStatus("idle")
            .setSeqnum(12345L)
            .build();

        when(mockTruckRepository.findByTruckId(100)).thenReturn(Optional.of(testTruck));
        when(mockTruckRepository.save(any(Truck.class))).thenReturn(testTruck);

        // Act
        messageHandlerService.handleTruckCompletion(completion);

        // Assert
        verify(mockTruckRepository).findByTruckId(100);
        verify(mockTruckRepository).save(testTruck);
        
        assertThat(testTruck.getCurrentX()).isEqualTo(50);
        assertThat(testTruck.getCurrentY()).isEqualTo(60);
        assertThat(testTruck.getStatus()).isEqualTo(TruckStatus.IDLE);
    }

    @Test
    @DisplayName("Should handle truck completion with warehouse arrival")
    void testHandleTruckCompletionWarehouseArrival() {
        // Arrange
        UFinished completion = UFinished.newBuilder()
            .setTruckid(100)
            .setX(100)
            .setY(200)
            .setStatus("arrive warehouse")
            .setSeqnum(12346L)
            .build();

        when(mockTruckRepository.findByTruckId(100)).thenReturn(Optional.of(testTruck));
        when(mockTruckRepository.save(any(Truck.class))).thenReturn(testTruck);
        when(mockShipmentRepository.findByTruck(testTruck)).thenReturn(List.of(testShipment));

        // Act
        messageHandlerService.handleTruckCompletion(completion);

        // Assert
        verify(mockTruckRepository).findByTruckId(100);
        verify(mockTruckRepository).save(testTruck);
        verify(mockShipmentRepository).findByTruck(testTruck);
        verify(mockAmazonIntegrationService).notifyTruckArrived(
            eq("1"), 
            eq("100_200"), 
            eq("SHIP123")
        );
        
        assertThat(testTruck.getCurrentX()).isEqualTo(100);
        assertThat(testTruck.getCurrentY()).isEqualTo(200);
        assertThat(testTruck.getStatus()).isEqualTo(TruckStatus.AT_WAREHOUSE);
    }

    @Test
    @DisplayName("Should handle truck completion for non-existent truck")
    void testHandleTruckCompletionNonExistentTruck() {
        // Arrange
        UFinished completion = UFinished.newBuilder()
            .setTruckid(999)
            .setX(50)
            .setY(60)
            .setStatus("idle")
            .setSeqnum(12347L)
            .build();

        when(mockTruckRepository.findByTruckId(999)).thenReturn(Optional.empty());

        // Act
        messageHandlerService.handleTruckCompletion(completion);

        // Assert
        verify(mockTruckRepository).findByTruckId(999);
        verify(mockTruckRepository, never()).save(any(Truck.class));
    }

    @Test
    @DisplayName("Should handle delivery made successfully")
    void testHandleDeliveryMade() {
        // Arrange
        UDeliveryMade delivery = UDeliveryMade.newBuilder()
            .setTruckid(100)
            .setPackageid(123L) // This is actually shipment ID
            .setSeqnum(12348L)
            .build();

        when(mockShipmentRepository.findByShipmentId("123")).thenReturn(Optional.of(testShipment));
        when(mockShipmentRepository.save(any(Shipment.class))).thenReturn(testShipment);

        // Act
        messageHandlerService.handleDeliveryMade(delivery);

        // Assert
        verify(mockShipmentRepository).findByShipmentId("123");
        verify(mockShipmentRepository).save(testShipment);
        verify(mockAmazonIntegrationService).notifyShipmentDelivered("SHIP123");
        
        assertThat(testShipment.getStatus()).isEqualTo(ShipmentStatus.DELIVERED);
        assertThat(testShipment.getActualDelivery()).isNotNull();
    }

    @Test
    @DisplayName("Should handle delivery made for non-existent shipment")
    void testHandleDeliveryMadeNonExistentShipment() {
        // Arrange
        UDeliveryMade delivery = UDeliveryMade.newBuilder()
            .setTruckid(100)
            .setPackageid(999L)
            .setSeqnum(12349L)
            .build();

        when(mockShipmentRepository.findByShipmentId("999")).thenReturn(Optional.empty());

        // Act
        messageHandlerService.handleDeliveryMade(delivery);

        // Assert
        verify(mockShipmentRepository).findByShipmentId("999");
        verify(mockShipmentRepository, never()).save(any(Shipment.class));
        verify(mockAmazonIntegrationService, never()).notifyShipmentDelivered(anyString());
    }

    @Test
    @DisplayName("Should handle truck status updates correctly")
    void testHandleTruckStatus() {
        // Arrange
        UTruck truckStatus = UTruck.newBuilder()
            .setTruckid(100)
            .setX(75)
            .setY(85)
            .setStatus("traveling")
            .setSeqnum(12350L)
            .build();

        when(mockTruckRepository.findByTruckId(100)).thenReturn(Optional.of(testTruck));
        when(mockTruckRepository.save(any(Truck.class))).thenReturn(testTruck);

        // Act
        messageHandlerService.handleTruckStatus(truckStatus);

        // Assert
        verify(mockTruckRepository).findByTruckId(100);
        verify(mockTruckRepository).save(testTruck);
        
        assertThat(testTruck.getCurrentX()).isEqualTo(75);
        assertThat(testTruck.getCurrentY()).isEqualTo(85);
        assertThat(testTruck.getStatus()).isEqualTo(TruckStatus.EN_ROUTE);
    }

    @Test
    @DisplayName("Should handle various truck status mappings")
    void testTruckStatusMappings() {
        // Test data: World Simulator status -> Expected TruckStatus
        Object[][] testCases = {
            {"idle", TruckStatus.IDLE},
            {"traveling", TruckStatus.EN_ROUTE},
            {"arrive warehouse", TruckStatus.AT_WAREHOUSE},
            {"loading", TruckStatus.LOADING},
            {"delivering", TruckStatus.DELIVERING}
        };

        when(mockTruckRepository.findByTruckId(100)).thenReturn(Optional.of(testTruck));
        when(mockTruckRepository.save(any(Truck.class))).thenReturn(testTruck);

        for (Object[] testCase : testCases) {
            String worldSimStatus = (String) testCase[0];
            TruckStatus expectedStatus = (TruckStatus) testCase[1];

            // Create status update
            UTruck truckStatus = UTruck.newBuilder()
                .setTruckid(100)
                .setX(10)
                .setY(20)
                .setStatus(worldSimStatus)
                .setSeqnum(12351L)
                .build();

            // Act
            messageHandlerService.handleTruckStatus(truckStatus);

            // Assert
            assertThat(testTruck.getStatus()).isEqualTo(expectedStatus);
        }

        // Verify interactions
        verify(mockTruckRepository, times(testCases.length)).findByTruckId(100);
        verify(mockTruckRepository, times(testCases.length)).save(testTruck);
    }

    @Test
    @DisplayName("Should handle unknown truck status gracefully")
    void testHandleUnknownTruckStatus() {
        // Arrange
        UTruck truckStatus = UTruck.newBuilder()
            .setTruckid(100)
            .setX(10)
            .setY(20)
            .setStatus("unknown_status")
            .setSeqnum(12352L)
            .build();

        TruckStatus originalStatus = testTruck.getStatus();
        when(mockTruckRepository.findByTruckId(100)).thenReturn(Optional.of(testTruck));
        when(mockTruckRepository.save(any(Truck.class))).thenReturn(testTruck);

        // Act
        messageHandlerService.handleTruckStatus(truckStatus);

        // Assert
        verify(mockTruckRepository).findByTruckId(100);
        verify(mockTruckRepository).save(testTruck);
        
        // Position should be updated but status should remain unchanged
        assertThat(testTruck.getCurrentX()).isEqualTo(10);
        assertThat(testTruck.getCurrentY()).isEqualTo(20);
        assertThat(testTruck.getStatus()).isEqualTo(originalStatus); // Status unchanged
    }

    @Test
    @DisplayName("Should handle error messages")
    void testHandleError() {
        // Arrange
        UErr error = UErr.newBuilder()
            .setErr("Invalid command format")
            .setOriginseqnum(12345L)
            .setSeqnum(12353L)
            .build();

        // Act (should not throw any exception)
        messageHandlerService.handleError(error);

        // Assert - just verify no exceptions are thrown
        // Error handling is primarily logging-based
    }
}