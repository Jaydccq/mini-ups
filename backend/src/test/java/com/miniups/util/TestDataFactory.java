package com.miniups.util;

import com.miniups.model.entity.*;
import com.miniups.model.enums.*;
import com.miniups.model.dto.amazon.AmazonOrderDto;

import java.time.LocalDateTime;

/**
 * Test data factory for creating test entities and DTOs.
 * Provides consistent test data creation methods across all test classes.
 */
public class TestDataFactory {

    /**
     * Creates a test user with default values.
     */
    public static User createTestUser() {
        return createTestUser("testuser", "test@example.com", UserRole.USER);
    }

    /**
     * Creates a test user with specified parameters.
     */
    public static User createTestUser(String username, String email, UserRole role) {
        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        user.setPassword("encodedPassword123");
        user.setRole(role);
        user.setEnabled(true);
        user.setCreatedAt(LocalDateTime.now());
        return user;
    }

    /**
     * Creates a test admin user.
     */
    public static User createTestAdmin() {
        return createTestUser("admin", "admin@example.com", UserRole.ADMIN);
    }

    /**
     * Creates a test driver user.
     */
    public static User createTestDriver() {
        return createTestUser("driver", "driver@example.com", UserRole.DRIVER);
    }

    /**
     * Creates a test shipment with default values.
     */
    public static Shipment createTestShipment() {
        return createTestShipment(createTestUser(), "UPS123456789", ShipmentStatus.CREATED);
    }

    /**
     * Creates a test shipment with specified parameters.
     */
    public static Shipment createTestShipment(User user, String trackingNumber, ShipmentStatus status) {
        Shipment shipment = new Shipment();
        shipment.setUpsTrackingId(trackingNumber);
        shipment.setUser(user);
        shipment.setStatus(status);
        shipment.setWeight(java.math.BigDecimal.valueOf(5.0));
        shipment.setOriginX(10);
        shipment.setOriginY(20);
        shipment.setDestX(15);
        shipment.setDestY(25);
        shipment.setDeliveryAddress("123 Main St");
        shipment.setDeliveryCity("Test City");
        shipment.setDeliveryZipCode("12345");
        shipment.setCreatedAt(LocalDateTime.now());
        return shipment;
    }

    /**
     * Creates a test truck with default values.
     */
    public static Truck createTestTruck() {
        return createTestTruck(1L, "TRUCK001", TruckStatus.IDLE);
    }

    /**
     * Creates a test truck with specified parameters.
     * Note: Primary key ID is not set here to avoid JPA persistence issues.
     * Use the returned entity from repository.save() to get the generated ID.
     */
    public static Truck createTestTruck(Long id, String licensePlate, TruckStatus status) {
        Truck truck = new Truck();
        // Do not set primary key ID manually - let JPA handle ID generation
        // But set the truckId field which is the business identifier
        truck.setTruckId(id != null ? id.intValue() : 1);
        truck.setLicensePlate(licensePlate);
        truck.setStatus(status);
        truck.setCurrentX(10);
        truck.setCurrentY(20);
        truck.setCapacity(1000);
        truck.setCurrentLoad(0.0);
        truck.setCreatedAt(LocalDateTime.now());
        return truck;
    }

    /**
     * Creates a test heavy duty truck.
     */
    public static Truck createTestHeavyDutyTruck() {
        Truck truck = createTestTruck(2L, "HEAVY001", TruckStatus.IDLE);
        truck.setCapacity(2000);
        return truck;
    }

    /**
     * Creates a test shipment status history entry.
     */
    public static ShipmentStatusHistory createTestStatusHistory(Shipment shipment, ShipmentStatus status, String notes) {
        ShipmentStatusHistory history = new ShipmentStatusHistory();
        history.setShipment(shipment);
        history.setStatus(status);
        history.setNotes(notes);
        history.setTimestamp(LocalDateTime.now());
        return history;
    }

    /**
     * Creates a test Amazon order DTO.
     */
    public static AmazonOrderDto createTestAmazonOrder() {
        AmazonOrderDto order = new AmazonOrderDto();
        order.setOrderId("AMZ123456");
        order.setCustomerEmail("customer@example.com");
        order.setTotalWeight(java.math.BigDecimal.valueOf(5.0));
        // Set shipping address
        AmazonOrderDto.ShippingAddressDto address = new AmazonOrderDto.ShippingAddressDto();
        address.setStreet("123 Customer St");
        address.setCity("Customer City");
        address.setState("NY");
        address.setZipCode("54321");
        address.setX(15);
        address.setY(25);
        order.setShippingAddress(address);
        return order;
    }

    /**
     * Creates a shipment with heavy weight for capacity testing.
     */
    public static Shipment createHeavyShipment(User user) {
        Shipment shipment = createTestShipment(user, "UPS999888777", ShipmentStatus.CREATED);
        shipment.setWeight(java.math.BigDecimal.valueOf(1500.0)); // Exceeds normal truck capacity
        return shipment;
    }

    /**
     * Creates multiple test trucks for fleet testing.
     * Note: IDs will be auto-generated when trucks are persisted.
     */
    public static Truck[] createTestFleet(int size) {
        Truck[] fleet = new Truck[size];
        for (int i = 0; i < size; i++) {
            fleet[i] = createTestTruck((long) (i + 1), "TRUCK" + String.format("%03d", i + 1), 
                                     i % 2 == 0 ? TruckStatus.IDLE : TruckStatus.DELIVERING);
            // Vary locations for distance testing
            fleet[i].setCurrentX(10 + i);
            fleet[i].setCurrentY(20 + i);
        }
        return fleet;
    }

    /**
     * Creates test shipment with specific coordinates for distance calculations.
     */
    public static Shipment createShipmentAtLocation(User user, String trackingNumber, 
                                                   double pickupLat, double pickupLon, 
                                                   double deliveryLat, double deliveryLon) {
        Shipment shipment = createTestShipment(user, trackingNumber, ShipmentStatus.CREATED);
        shipment.setOriginX((int) pickupLat);
        shipment.setOriginY((int) pickupLon);
        shipment.setDestX((int) deliveryLat);
        shipment.setDestY((int) deliveryLon);
        return shipment;
    }

    /**
     * Creates test truck at specific location for distance calculations.
     * Note: Primary key ID parameter is kept for compatibility but not used internally.
     * The truckId field is properly set for business logic.
     */
    public static Truck createTruckAtLocation(Long id, String licensePlate, 
                                            double lat, double lon, TruckStatus status) {
        Truck truck = createTestTruck(id, licensePlate, status);
        truck.setCurrentX((int) lat);
        truck.setCurrentY((int) lon);
        return truck;
    }

    /**
     * Creates invalid tracking numbers for negative testing.
     */
    public static String[] createInvalidTrackingNumbers() {
        return new String[]{
            null,
            "",
            "INVALID123",
            "ups123456789", // lowercase
            "UPS12345", // too short
            "UPS1234567890123", // too long
            "ABC123456789", // wrong prefix
            "UPS12345678A" // contains letter
        };
    }

    /**
     * Creates valid tracking numbers for positive testing.
     */
    public static String[] createValidTrackingNumbers() {
        return new String[]{
            "UPS123456789",
            "UPS000000001",
            "UPS999999999",
            "UPS555444333"
        };
    }

    /**
     * Creates test shipment with all status transitions.
     */
    public static Shipment createShipmentWithFullHistory(User user, String trackingNumber) {
        Shipment shipment = createTestShipment(user, trackingNumber, ShipmentStatus.DELIVERED);
        
        // This would typically be handled by the service layer
        // but useful for testing status history functionality
        return shipment;
    }

    /**
     * Private constructor to prevent instantiation.
     */
    private TestDataFactory() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }
}