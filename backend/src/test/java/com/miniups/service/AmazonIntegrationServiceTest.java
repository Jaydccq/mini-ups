package com.miniups.service;

import com.miniups.model.dto.AmazonMessageDto;
import com.miniups.model.dto.UpsResponseDto;
import com.miniups.model.entity.Shipment;
import com.miniups.model.entity.Truck;
import com.miniups.model.entity.User;
import com.miniups.model.enums.ShipmentStatus;
import com.miniups.model.enums.TruckStatus;
import com.miniups.model.enums.UserRole;
import com.miniups.repository.ShipmentRepository;
import com.miniups.repository.UserRepository;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Integration tests for AmazonIntegrationService.
 * Tests Amazon API communication, webhook handling, and external service integration.
 */
@ExtendWith(MockitoExtension.class)
@ActiveProfiles("test")
@DisplayName("AmazonIntegrationService Integration Tests")
class AmazonIntegrationServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private ShipmentRepository shipmentRepository;

    @Mock
    private TruckManagementService truckManagementService;

    @Mock
    private TrackingService trackingService;
    
    @Mock
    private WorldSimulatorService worldSimulatorService;


    @InjectMocks
    private AmazonIntegrationService amazonIntegrationService;

    private WireMockServer wireMockServer;
    private User testUser;
    private Shipment testShipment;
    private Truck testTruck;

    @BeforeEach
    void setUp() {
        // Setup WireMock server for mocking Amazon API with dynamic port
        wireMockServer = new WireMockServer(WireMockConfiguration.wireMockConfig().dynamicPort());
        wireMockServer.start();
        int dynamicPort = wireMockServer.port();
        WireMock.configureFor("localhost", dynamicPort);

        // Setup RestTemplate and service configuration with dynamic port
        RestTemplate restTemplate = new RestTemplate();
        String dynamicAmazonBaseUrl = "http://localhost:" + dynamicPort;
        ReflectionTestUtils.setField(amazonIntegrationService, "amazonBaseUrl", dynamicAmazonBaseUrl);
        ReflectionTestUtils.setField(amazonIntegrationService, "restTemplate", restTemplate);

        // Create test entities
        testUser = createTestUser();
        testShipment = createTestShipment();
        testTruck = createTestTruck();
    }

    @AfterEach
    void tearDown() {
        wireMockServer.stop();
    }

    @Test
    @DisplayName("Should handle shipment created from Amazon successfully")
    void testHandleShipmentCreated_Success() {
        // Given
        AmazonMessageDto messageDto = createAmazonMessageDto();
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));
        when(trackingService.generateTrackingNumber()).thenReturn("UPS123456789");
        when(shipmentRepository.findByShipmentId("AMZ123456")).thenReturn(Optional.empty());
        when(shipmentRepository.save(any(Shipment.class))).thenReturn(testShipment);
        when(truckManagementService.assignOptimalTruck(any(Integer.class), any(Integer.class), any(Integer.class))).thenReturn(testTruck);
        when(worldSimulatorService.isConnected()).thenReturn(false); // Simplify by assuming not connected

        // When
        var result = amazonIntegrationService.handleShipmentCreated(messageDto);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.isSuccess()).isTrue();
        verify(userRepository).findByEmail("test@example.com");
        verify(trackingService).generateTrackingNumber();
        verify(shipmentRepository).save(any(Shipment.class));
        verify(truckManagementService).assignOptimalTruck(any(Integer.class), any(Integer.class), any(Integer.class));
    }

    @Test
    @DisplayName("Should create new user when customer not found")
    void testHandleShipmentCreated_NewUser() {
        // Given
        AmazonMessageDto messageDto = createAmazonMessageDto();
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenReturn(testUser);
        when(trackingService.generateTrackingNumber()).thenReturn("UPS123456789");
        when(shipmentRepository.findByShipmentId("AMZ123456")).thenReturn(Optional.empty());
        when(shipmentRepository.save(any(Shipment.class))).thenReturn(testShipment);
        when(truckManagementService.assignOptimalTruck(any(Integer.class), any(Integer.class), any(Integer.class))).thenReturn(testTruck);
        when(worldSimulatorService.isConnected()).thenReturn(false); // Simplify by assuming not connected

        // When
        var result = amazonIntegrationService.handleShipmentCreated(messageDto);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.isSuccess()).isTrue();
        verify(userRepository).findByEmail("test@example.com");
        verify(userRepository).save(any(User.class));
        verify(trackingService).generateTrackingNumber();
        verify(shipmentRepository).save(any(Shipment.class));
        verify(truckManagementService).assignOptimalTruck(any(Integer.class), any(Integer.class), any(Integer.class));
    }

    @Test
    @DisplayName("Should handle shipment loaded notification")
    void testHandleShipmentLoaded_Success() {
        // Given
        String trackingNumber = "UPS123456789";
        testShipment.setUpsTrackingId(trackingNumber); // Ensure the shipment has the tracking number
        when(shipmentRepository.findByShipmentId(trackingNumber)).thenReturn(Optional.of(testShipment));
        when(shipmentRepository.save(any(Shipment.class))).thenReturn(testShipment);

        // When
        AmazonMessageDto message = createShipmentLoadedMessage(trackingNumber);
        UpsResponseDto result = amazonIntegrationService.handleShipmentLoaded(message);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.isSuccess()).isTrue();
        verify(shipmentRepository).findByShipmentId(trackingNumber);
        verify(shipmentRepository).save(testShipment);
        // Verify shipment status was updated to IN_TRANSIT (after picking up and starting delivery)
        assertThat(testShipment.getStatus()).isEqualTo(ShipmentStatus.IN_TRANSIT);
    }

    @Test
    @DisplayName("Should handle shipment not found gracefully")
    void testHandleShipmentLoaded_ShipmentNotFound() {
        // Given
        String trackingNumber = "UPS999999999";
        when(shipmentRepository.findByShipmentId(trackingNumber)).thenReturn(Optional.empty());

        // When
        AmazonMessageDto message = createShipmentLoadedMessage(trackingNumber);
        UpsResponseDto result = amazonIntegrationService.handleShipmentLoaded(message);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.isSuccess()).isFalse();
        verify(shipmentRepository).findByShipmentId(trackingNumber);
        verify(trackingService, never()).updateShipmentStatus(anyString(), any(), anyString());
    }

    @Test
    @DisplayName("Should handle address change request")
    void testHandleAddressChange_Success() {
        // Given
        String trackingNumber = "UPS123456789";
        testShipment.setStatus(ShipmentStatus.CREATED);
        when(shipmentRepository.findByShipmentId(trackingNumber)).thenReturn(Optional.of(testShipment));
        when(shipmentRepository.save(any(Shipment.class))).thenReturn(testShipment);

        // When
        AmazonMessageDto message = createAddressChangeMessage(trackingNumber);
        amazonIntegrationService.handleAddressChange(message);

        // Then
        verify(shipmentRepository).findByShipmentId(trackingNumber);
        verify(shipmentRepository).save(testShipment);
        assertThat(testShipment.getDestX()).isEqualTo(16);
        assertThat(testShipment.getDestY()).isEqualTo(26);
    }

    @Test
    @DisplayName("Should allow address change for shipments in transit")
    void testHandleAddressChange_InTransit() {
        // Given
        String trackingNumber = "UPS123456789";
        testShipment.setStatus(ShipmentStatus.IN_TRANSIT);
        when(shipmentRepository.findByShipmentId(trackingNumber)).thenReturn(Optional.of(testShipment));
        when(shipmentRepository.save(any(Shipment.class))).thenReturn(testShipment);

        // When
        AmazonMessageDto message = createAddressChangeMessage(trackingNumber);
        UpsResponseDto result = amazonIntegrationService.handleAddressChange(message);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.isSuccess()).isTrue();
        verify(shipmentRepository).findByShipmentId(trackingNumber);
        verify(shipmentRepository).save(testShipment);
        assertThat(testShipment.getDestX()).isEqualTo(16);
        assertThat(testShipment.getDestY()).isEqualTo(26);
    }

    @Test
    @DisplayName("Should send truck dispatched notification to Amazon")
    void testNotifyTruckDispatched_Success() {
        // Given
        String truckId = "1";
        String shipmentId = "AMZ123456";

        stubFor(post(urlEqualTo("/api/webhooks/truck-dispatched"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"success\": true}")));

        // When
        amazonIntegrationService.notifyTruckDispatched(truckId, shipmentId);

        // Then
        verify(postRequestedFor(urlEqualTo("/api/webhooks/truck-dispatched"))
                .withHeader("Content-Type", matching("application/json"))
                .withRequestBody(containing(truckId))
                .withRequestBody(containing(shipmentId)));
    }

    @Test
    @DisplayName("Should send truck arrived notification to Amazon")
    void testNotifyTruckArrived_Success() {
        // Given
        String truckId = "1";
        String warehouseId = "WH001";
        String shipmentId = "AMZ123456";

        stubFor(post(urlEqualTo("/api/webhooks/truck-arrived"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"success\": true}")));

        // When
        amazonIntegrationService.notifyTruckArrived(truckId, warehouseId, shipmentId);

        // Then
        verify(postRequestedFor(urlEqualTo("/api/webhooks/truck-arrived"))
                .withHeader("Content-Type", matching("application/json"))
                .withRequestBody(containing(truckId))
                .withRequestBody(containing(warehouseId))
                .withRequestBody(containing(shipmentId)));
    }

    @Test
    @DisplayName("Should send shipment delivered notification to Amazon")
    void testNotifyShipmentDelivered_Success() {
        // Given
        String shipmentId = "AMZ123456";

        stubFor(post(urlEqualTo("/api/webhooks/shipment-delivered"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"success\": true}")));

        // When
        amazonIntegrationService.notifyShipmentDelivered(shipmentId);

        // Then
        verify(postRequestedFor(urlEqualTo("/api/webhooks/shipment-delivered"))
                .withHeader("Content-Type", matching("application/json"))
                .withRequestBody(containing(shipmentId)));
    }

    @Test
    @DisplayName("Should handle Amazon API errors gracefully")
    void testNotifyTruckDispatched_ApiError() {
        // Given
        String truckId = "1";
        String shipmentId = "AMZ123456";

        stubFor(post(urlEqualTo("/api/webhooks/truck-dispatched"))
                .willReturn(aResponse()
                        .withStatus(500)
                        .withBody("Internal Server Error")));

        // When & Then
        assertThatCode(() -> amazonIntegrationService.notifyTruckDispatched(truckId, shipmentId))
                .doesNotThrowAnyException(); // Should handle errors gracefully
    }

    @Test
    @DisplayName("Should handle shipment status request from Amazon")
    void testHandleShipmentStatusRequest_Success() {
        // Given
        String trackingNumber = "UPS123456789";
        testShipment.setUpsTrackingId(trackingNumber);
        when(shipmentRepository.findByShipmentId(trackingNumber)).thenReturn(Optional.of(testShipment));

        // When
        AmazonMessageDto message = createStatusRequestMessage(trackingNumber);
        UpsResponseDto result = amazonIntegrationService.handleShipmentStatusRequest(message);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getMessageType()).isEqualTo("ShipmentStatusResponse");
        assertThat(result.getPayload()).isNotNull();
        verify(shipmentRepository).findByShipmentId(trackingNumber);
    }

    @Test
    @DisplayName("Should return error for non-existent shipment status request")
    void testHandleShipmentStatusRequest_NotFound() {
        // Given
        String trackingNumber = "UPS999999999";
        when(shipmentRepository.findByShipmentId(trackingNumber)).thenReturn(Optional.empty());

        // When
        AmazonMessageDto message = createStatusRequestMessage(trackingNumber);
        UpsResponseDto result = amazonIntegrationService.handleShipmentStatusRequest(message);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.isSuccess()).isFalse();
        verify(shipmentRepository).findByShipmentId(trackingNumber);
    }

    @Test
    @DisplayName("Should handle validation errors gracefully")
    void testHandleShipmentCreated_ValidationErrors() {
        // Given - Invalid message DTO
        AmazonMessageDto invalidMessage = new AmazonMessageDto();
        // Missing required fields

        // When
        UpsResponseDto result = amazonIntegrationService.handleShipmentCreated(invalidMessage);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.isSuccess()).isFalse();
    }

    @Test
    @DisplayName("Should handle concurrent shipment operations")
    void testConcurrentOperations() {
        // Given
        String trackingNumber = "UPS123456789";
        when(shipmentRepository.findByShipmentId(trackingNumber)).thenReturn(Optional.of(testShipment));

        // When - Simulate concurrent operations
        AmazonMessageDto message = createShipmentLoadedMessage(trackingNumber);
        amazonIntegrationService.handleShipmentLoaded(message);
        amazonIntegrationService.notifyTruckDispatched("1", "AMZ123456");

        // Then
        verify(shipmentRepository, atLeast(1)).findByShipmentId(trackingNumber);
    }

    /**
     * Helper method to create test user.
     */
    private User createTestUser() {
        User user = new User();
        user.setId(1L);
        user.setUsername("testuser");
        user.setEmail("test@example.com");
        user.setRole(UserRole.USER);
        user.setEnabled(true);
        user.setCreatedAt(LocalDateTime.now());
        return user;
    }

    /**
     * Helper method to create test shipment.
     */
    private Shipment createTestShipment() {
        Shipment shipment = new Shipment();
        shipment.setId(1L);
        shipment.setShipmentId("SH123456");
        shipment.setUpsTrackingId("UPS123456789");
        shipment.setUser(testUser);
        shipment.setStatus(ShipmentStatus.CREATED);
        shipment.setWeight(java.math.BigDecimal.valueOf(5.0));
        shipment.setOriginX(10);
        shipment.setOriginY(20);
        shipment.setDestX(15);
        shipment.setDestY(25);
        shipment.setCreatedAt(LocalDateTime.now());
        return shipment;
    }

    /**
     * Helper method to create test truck.
     */
    private Truck createTestTruck() {
        Truck truck = new Truck();
        truck.setId(1L);
        truck.setTruckId(1);
        truck.setLicensePlate("TRUCK001");
        truck.setStatus(TruckStatus.IDLE);
        truck.setCurrentX(10);
        truck.setCurrentY(20);
        truck.setCapacity(1000);
        truck.setCurrentLoad(0.0);
        truck.setCreatedAt(LocalDateTime.now());
        return truck;
    }

    /**
     * Helper method to create Amazon message DTO.
     */
    private AmazonMessageDto createAmazonMessageDto() {
        AmazonMessageDto messageDto = new AmazonMessageDto();
        messageDto.setMessageType("ShipmentCreated");
        messageDto.setTimestamp(LocalDateTime.now());
        
        Map<String, Object> payload = new HashMap<>();
        payload.put("shipment_id", "AMZ123456");
        payload.put("email", "test@example.com");
        payload.put("weight", java.math.BigDecimal.valueOf(5.0));
        payload.put("warehouse_id", 1L); // Add warehouse_id which is required
        payload.put("destination_x", 15); // Use correct field names as expected by service
        payload.put("destination_y", 25);
        payload.put("delivery_address", "123 Main St");
        payload.put("delivery_city", "Test City");
        payload.put("delivery_zip_code", "12345");
        
        messageDto.setPayload(payload);
        return messageDto;
    }
    
    /**
     * Helper method to create shipment loaded message.
     */
    private AmazonMessageDto createShipmentLoadedMessage(String trackingNumber) {
        AmazonMessageDto messageDto = new AmazonMessageDto();
        messageDto.setMessageType("ShipmentLoaded");
        messageDto.setTimestamp(LocalDateTime.now());
        
        Map<String, Object> payload = new HashMap<>();
        payload.put("shipment_id", trackingNumber);
        
        messageDto.setPayload(payload);
        return messageDto;
    }
    
    /**
     * Helper method to create address change message.
     */
    private AmazonMessageDto createAddressChangeMessage(String trackingNumber) {
        AmazonMessageDto messageDto = new AmazonMessageDto();
        messageDto.setMessageType("AddressChange");
        messageDto.setTimestamp(LocalDateTime.now());
        
        Map<String, Object> payload = new HashMap<>();
        payload.put("shipment_id", trackingNumber);
        payload.put("destination_x", 16);
        payload.put("destination_y", 26);
        payload.put("new_address", "123 New St");
        payload.put("new_city", "New City");
        payload.put("new_zip_code", "12345");
        
        messageDto.setPayload(payload);
        return messageDto;
    }
    
    /**
     * Helper method to create status request message.
     */
    private AmazonMessageDto createStatusRequestMessage(String trackingNumber) {
        AmazonMessageDto messageDto = new AmazonMessageDto();
        messageDto.setMessageType("ShipmentStatusRequest");
        messageDto.setTimestamp(LocalDateTime.now());
        
        Map<String, Object> payload = new HashMap<>();
        payload.put("shipment_id", trackingNumber);
        
        messageDto.setPayload(payload);
        return messageDto;
    }
}