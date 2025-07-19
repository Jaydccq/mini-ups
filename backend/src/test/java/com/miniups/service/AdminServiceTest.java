package com.miniups.service;

import com.miniups.model.entity.Shipment;
import com.miniups.model.entity.Truck;
import com.miniups.model.entity.User;
import com.miniups.model.enums.ShipmentStatus;
import com.miniups.model.enums.TruckStatus;
import com.miniups.model.enums.UserRole;
import com.miniups.repository.ShipmentRepository;
import com.miniups.repository.TruckRepository;
import com.miniups.repository.UserRepository;
import com.miniups.repository.AuditLogRepository;
import com.miniups.service.consumer.AnalyticsConsumer;
import com.miniups.util.TestDataFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.ActiveProfiles;

import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * AdminService 核心业务逻辑测试
 * 重点测试：统计计算、数据聚合、管理功能、性能指标分析
 */
@ExtendWith(MockitoExtension.class)
@ActiveProfiles("test")
@DisplayName("AdminService 管理后台业务逻辑测试")
class AdminServiceTest {

    @Mock
    private ShipmentRepository shipmentRepository;

    @Mock
    private TruckRepository truckRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private AuditLogRepository auditLogRepository;

    @Mock
    private AnalyticsConsumer analyticsConsumer;

    @InjectMocks
    private AdminService adminService;

    private List<Truck> testTrucks;
    private List<Shipment> testShipments;
    private List<User> testUsers;
    private List<Map<String, Object>> mockStatusCounts;

    @BeforeEach
    void setUp() {
        setupTestTrucks();
        setupTestShipments();
        setupTestUsers();
        setupMockStatusCounts();
    }

    @Test
    @DisplayName("仪表板统计 - 计算综合统计数据")
    void getDashboardStatistics_shouldCalculateComprehensiveStats() {
        // Given
        when(shipmentRepository.getStatusCounts()).thenReturn(mockStatusCounts);
        when(truckRepository.findAll()).thenReturn(testTrucks);
        when(truckRepository.count()).thenReturn((long) testTrucks.size());
        when(truckRepository.countByStatus(TruckStatus.IDLE)).thenReturn(2L);
        when(truckRepository.countByStatus(TruckStatus.TRAVELING)).thenReturn(1L);
        when(userRepository.count()).thenReturn((long) testUsers.size());
        when(userRepository.countByRole(UserRole.ADMIN)).thenReturn(1L);
        when(userRepository.countByRole(UserRole.USER)).thenReturn(2L);
        when(shipmentRepository.findAll()).thenReturn(testShipments);

        // When
        Map<String, Object> statistics = adminService.getDashboardStatistics();

        // Then
        assertThat(statistics).isNotNull();
        assertThat(statistics).containsKeys("orders", "fleet", "users", "revenue", "lastUpdated");

        // 验证订单统计
        @SuppressWarnings("unchecked")
        Map<String, Object> orderStats = (Map<String, Object>) statistics.get("orders");
        assertThat(orderStats.get("total")).isEqualTo(4L);
        assertThat(orderStats.get("completed")).isEqualTo(1L);
        assertThat(orderStats.get("cancelled")).isEqualTo(1L);

        // 验证车队统计
        @SuppressWarnings("unchecked")
        Map<String, Object> fleetStats = (Map<String, Object>) statistics.get("fleet");
        assertThat(fleetStats.get("total")).isEqualTo(3L);
        assertThat(fleetStats.get("available")).isEqualTo(2L);
        assertThat(fleetStats.get("inTransit")).isEqualTo(1L);

        // 验证用户统计
        @SuppressWarnings("unchecked")
        Map<String, Object> userStats = (Map<String, Object>) statistics.get("users");
        assertThat(userStats.get("total")).isEqualTo(3L);
        assertThat(userStats.get("admins")).isEqualTo(1L);
        assertThat(userStats.get("regular")).isEqualTo(2L);

        verify(shipmentRepository).getStatusCounts();
        verify(truckRepository).count();
        verify(userRepository).count();
    }

    @Test
    @DisplayName("车队概览 - 获取详细车辆信息")
    void getFleetOverview_shouldReturnDetailedTruckInfo() {
        // Given
        when(truckRepository.findAll()).thenReturn(testTrucks);

        // When
        Map<String, Object> fleetOverview = adminService.getFleetOverview();

        // Then
        assertThat(fleetOverview).isNotNull();
        assertThat(fleetOverview).containsKeys("trucks", "statusDistribution", "totalTrucks");

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> trucks = (List<Map<String, Object>>) fleetOverview.get("trucks");
        assertThat(trucks).hasSize(3);

        // 验证第一辆车的信息
        Map<String, Object> firstTruck = trucks.get(0);
        assertThat(firstTruck).containsKeys("id", "plateNumber", "capacity", "status", "currentLocation");

        // 验证状态分布
        @SuppressWarnings("unchecked")
        Map<String, Long> statusDistribution = (Map<String, Long>) fleetOverview.get("statusDistribution");
        assertThat(statusDistribution).containsKey("IDLE");
        assertThat(statusDistribution).containsKey("EN_ROUTE");

        assertThat(fleetOverview.get("totalTrucks")).isEqualTo(3);

        verify(truckRepository).findAll();
    }

    @Test
    @DisplayName("订单管理摘要 - 状态分布和最近订单")
    void getOrderSummary_shouldReturnOrderBreakdownAndRecentOrders() {
        // Given
        when(shipmentRepository.getStatusCounts()).thenReturn(mockStatusCounts);
        
        Pageable pageable = PageRequest.of(0, 10, any());
        Page<Shipment> mockPage = new PageImpl<>(testShipments.subList(0, 2));
        when(shipmentRepository.findRecentShipments(any(Pageable.class))).thenReturn(mockPage);

        // When
        Map<String, Object> orderSummary = adminService.getOrderSummary();

        // Then
        assertThat(orderSummary).isNotNull();
        assertThat(orderSummary).containsKeys("statusBreakdown", "recentOrders", "totalOrders");

        @SuppressWarnings("unchecked")
        Map<String, Long> statusBreakdown = (Map<String, Long>) orderSummary.get("statusBreakdown");
        assertThat(statusBreakdown).isNotEmpty();

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> recentOrders = (List<Map<String, Object>>) orderSummary.get("recentOrders");
        assertThat(recentOrders).hasSize(2);

        // 验证订单信息
        Map<String, Object> firstOrder = recentOrders.get(0);
        assertThat(firstOrder).containsKeys("id", "upsTrackingId", "status", "senderName", "createdAt");

        verify(shipmentRepository).getStatusCounts();
        verify(shipmentRepository).findRecentShipments(any(Pageable.class));
    }

    @Test
    @DisplayName("司机管理 - 获取司机列表和统计")
    void getDriverManagement_shouldReturnDriverStats() {
        // Given
        when(userRepository.findByRole(UserRole.ADMIN)).thenReturn(testUsers.subList(0, 1));

        // When
        Map<String, Object> driverManagement = adminService.getDriverManagement();

        // Then
        assertThat(driverManagement).isNotNull();
        assertThat(driverManagement).containsKeys("drivers", "totalDrivers", "activeDrivers");

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> drivers = (List<Map<String, Object>>) driverManagement.get("drivers");
        assertThat(drivers).hasSize(1);

        Map<String, Object> driver = drivers.get(0);
        assertThat(driver).containsKeys("id", "name", "email", "phone", "status", "lastActive");

        assertThat(driverManagement.get("totalDrivers")).isEqualTo(1);

        verify(userRepository).findByRole(UserRole.ADMIN);
    }

    @Test
    @DisplayName("分析趋势 - 生成性能指标和趋势数据")
    void getAnalyticsTrends_shouldGeneratePerformanceMetrics() {
        // Given
        Map<String, Object> mockAnalyticsData = new HashMap<>();
        mockAnalyticsData.put("activeConnections", 25);
        mockAnalyticsData.put("throughput", 156.7);
        when(analyticsConsumer.getAnalyticsSummary()).thenReturn(mockAnalyticsData);

        // When
        Map<String, Object> trends = adminService.getAnalyticsTrends();

        // Then
        assertThat(trends).isNotNull();
        assertThat(trends).containsKeys("ordersOverTime", "performanceMetrics", "analyticsData");

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> ordersOverTime = (List<Map<String, Object>>) trends.get("ordersOverTime");
        assertThat(ordersOverTime).hasSize(7); // 7天数据

        @SuppressWarnings("unchecked")
        Map<String, Object> performanceMetrics = (Map<String, Object>) trends.get("performanceMetrics");
        assertThat(performanceMetrics).containsKeys("averageDeliveryTime", "onTimeDeliveryRate", 
                                                   "customerSatisfaction", "operationalEfficiency");

        assertThat(trends.get("analyticsData")).isEqualTo(mockAnalyticsData);

        verify(analyticsConsumer).getAnalyticsSummary();
    }

    @Test
    @DisplayName("系统健康状态 - 检查各组件状态")
    void getSystemHealth_shouldReturnSystemComponentsStatus() {
        // Given
        when(analyticsConsumer.getActiveUserCount()).thenReturn(42);

        // When
        Map<String, Object> health = adminService.getSystemHealth();

        // Then
        assertThat(health).isNotNull();
        assertThat(health).containsKeys("database", "redis", "rabbitmq", "application", 
                                       "overallStatus", "lastCheck");

        @SuppressWarnings("unchecked")
        Map<String, Object> dbHealth = (Map<String, Object>) health.get("database");
        assertThat(dbHealth).containsKeys("status", "responseTime", "connections", "maxConnections");
        assertThat(dbHealth.get("status")).isEqualTo("UP");

        @SuppressWarnings("unchecked")
        Map<String, Object> appMetrics = (Map<String, Object>) health.get("application");
        assertThat(appMetrics.get("activeUsers")).isEqualTo(42);

        assertThat(health.get("overallStatus")).isEqualTo("HEALTHY");

        verify(analyticsConsumer).getActiveUserCount();
    }

    @Test
    @DisplayName("最近活动 - 分页获取审计日志")
    void getRecentActivities_shouldReturnPaginatedAuditLogs() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);
        List<MockAuditLog> mockLogs = Arrays.asList(
            new MockAuditLog(1L, "CREATE", "User", "1", "user123", "Created new user"),
            new MockAuditLog(2L, "UPDATE", "Shipment", "2", "admin", "Updated shipment status")
        );
        Page<MockAuditLog> mockPage = new PageImpl<>(mockLogs, pageable, mockLogs.size());
        
        when(auditLogRepository.findAll(pageable)).thenReturn((Page) mockPage);

        // When
        Map<String, Object> recentActivities = adminService.getRecentActivities(pageable);

        // Then
        assertThat(recentActivities).isNotNull();
        assertThat(recentActivities).containsKeys("activities", "currentPage", "totalPages", 
                                                 "totalElements", "hasNext");

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> activities = (List<Map<String, Object>>) recentActivities.get("activities");
        assertThat(activities).hasSize(2);

        Map<String, Object> firstActivity = activities.get(0);
        assertThat(firstActivity).containsKeys("id", "action", "entityType", "entityId", "userId", "details");

        assertThat(recentActivities.get("totalElements")).isEqualTo(2L);
        assertThat(recentActivities.get("currentPage")).isEqualTo(0);

        verify(auditLogRepository).findAll(pageable);
    }

    @Test
    @DisplayName("车队CRUD - 创建新车辆")
    void createTruck_shouldReturnCreatedTruckInfo() {
        // Given
        Map<String, Object> truckData = new HashMap<>();
        truckData.put("plateNumber", "TRUCK005");
        truckData.put("capacity", 1500);
        truckData.put("location", "Warehouse A");

        // When
        Map<String, Object> result = adminService.createTruck(truckData);

        // Then
        assertThat(result).isNotNull();
        assertThat(result).containsKeys("id", "plateNumber", "capacity", "location", "status", "createdAt");
        assertThat(result.get("plateNumber")).isEqualTo("TRUCK005");
        assertThat(result.get("capacity")).isEqualTo(1500);
        assertThat(result.get("status")).isEqualTo("IDLE");
    }

    @Test
    @DisplayName("车队CRUD - 更新车辆信息")
    void updateTruck_shouldReturnUpdatedTruckInfo() {
        // Given
        Long truckId = 1L;
        Map<String, Object> truckData = new HashMap<>();
        truckData.put("plateNumber", "TRUCK001_UPDATED");
        truckData.put("capacity", 2000);
        truckData.put("status", "MAINTENANCE");

        // When
        Map<String, Object> result = adminService.updateTruck(truckId, truckData);

        // Then
        assertThat(result).isNotNull();
        assertThat(result).containsKeys("id", "plateNumber", "capacity", "status", "updatedAt");
        assertThat(result.get("id")).isEqualTo(truckId);
        assertThat(result.get("plateNumber")).isEqualTo("TRUCK001_UPDATED");
        assertThat(result.get("capacity")).isEqualTo(2000);
        assertThat(result.get("status")).isEqualTo("MAINTENANCE");
    }

    @Test
    @DisplayName("车队CRUD - 删除车辆")
    void deleteTruck_shouldLogDeletion() {
        // Given
        Long truckId = 1L;

        // When & Then
        assertThatCode(() -> adminService.deleteTruck(truckId)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("司机CRUD - 创建新司机")
    void createDriver_shouldReturnCreatedDriverInfo() {
        // Given
        Map<String, Object> driverData = new HashMap<>();
        driverData.put("name", "John Driver");
        driverData.put("email", "john.driver@example.com");
        driverData.put("phone", "1234567890");
        driverData.put("licenseNumber", "DL123456");

        // When
        Map<String, Object> result = adminService.createDriver(driverData);

        // Then
        assertThat(result).isNotNull();
        assertThat(result).containsKeys("id", "name", "email", "phone", "licenseNumber", "status", "createdAt");
        assertThat(result.get("name")).isEqualTo("John Driver");
        assertThat(result.get("email")).isEqualTo("john.driver@example.com");
        assertThat(result.get("status")).isEqualTo("ACTIVE");
    }

    @Test
    @DisplayName("统计计算 - 完成率计算")
    void calculateOrderStatistics_shouldCalculateCompletionRate() {
        // Given
        List<Map<String, Object>> statusCounts = Arrays.asList(
            createStatusCount("DELIVERED", 80),
            createStatusCount("CANCELLED", 20),
            createStatusCount("CREATED", 50),
            createStatusCount("IN_TRANSIT", 30)
        );
        when(shipmentRepository.getStatusCounts()).thenReturn(statusCounts);

        // When
        Map<String, Object> statistics = adminService.getDashboardStatistics();

        // Then
        @SuppressWarnings("unchecked")
        Map<String, Object> orderStats = (Map<String, Object>) statistics.get("orders");
        
        // 总订单 = 80 + 20 + 50 + 30 = 180
        assertThat(orderStats.get("total")).isEqualTo(180L);
        assertThat(orderStats.get("completed")).isEqualTo(80L);
        
        // 完成率 = 80/180 * 100 = 44.44%
        double completionRate = (Double) orderStats.get("completionRate");
        assertThat(completionRate).isBetween(44.0, 45.0);
    }

    @Test
    @DisplayName("车队利用率 - 计算使用率")
    void calculateFleetStatistics_shouldCalculateUtilizationRate() {
        // Given
        when(truckRepository.count()).thenReturn(10L);
        when(truckRepository.countByStatus(TruckStatus.IDLE)).thenReturn(3L);
        when(truckRepository.countByStatus(TruckStatus.TRAVELING)).thenReturn(7L);

        // When
        Map<String, Object> statistics = adminService.getDashboardStatistics();

        // Then
        @SuppressWarnings("unchecked")
        Map<String, Object> fleetStats = (Map<String, Object>) statistics.get("fleet");
        
        assertThat(fleetStats.get("total")).isEqualTo(10L);
        assertThat(fleetStats.get("available")).isEqualTo(3L);
        assertThat(fleetStats.get("inTransit")).isEqualTo(7L);
        
        // 利用率 = 7/10 * 100 = 70%
        assertThat(fleetStats.get("utilizationRate")).isEqualTo(70.0);
    }

    @Test
    @DisplayName("边界条件 - 空数据处理")
    void handleEmptyData_shouldReturnDefaultValues() {
        // Given
        when(shipmentRepository.getStatusCounts()).thenReturn(Collections.emptyList());
        when(truckRepository.findAll()).thenReturn(Collections.emptyList());
        when(truckRepository.count()).thenReturn(0L);
        when(truckRepository.countByStatus(any())).thenReturn(0L);
        when(userRepository.count()).thenReturn(0L);
        when(userRepository.countByRole(any())).thenReturn(0L);
        when(shipmentRepository.findAll()).thenReturn(Collections.emptyList());

        // When
        Map<String, Object> statistics = adminService.getDashboardStatistics();

        // Then
        assertThat(statistics).isNotNull();
        
        @SuppressWarnings("unchecked")
        Map<String, Object> orderStats = (Map<String, Object>) statistics.get("orders");
        assertThat(orderStats.get("total")).isEqualTo(0L);
        assertThat(orderStats.get("completionRate")).isEqualTo(0.0);

        @SuppressWarnings("unchecked")
        Map<String, Object> fleetStats = (Map<String, Object>) statistics.get("fleet");
        assertThat(fleetStats.get("total")).isEqualTo(0L);
        assertThat(fleetStats.get("utilizationRate")).isEqualTo(0.0);
    }

    // Helper methods

    private void setupTestTrucks() {
        testTrucks = Arrays.asList(
            TestDataFactory.createTestTruck("TRUCK001", TruckStatus.IDLE),
            TestDataFactory.createTestTruck("TRUCK002", TruckStatus.EN_ROUTE),
            TestDataFactory.createTestTruck("TRUCK003", TruckStatus.IDLE)
        );
        testTrucks.get(0).setId(1L);
        testTrucks.get(1).setId(2L);
        testTrucks.get(2).setId(3L);
    }

    private void setupTestShipments() {
        User user = TestDataFactory.createTestUser();
        testShipments = Arrays.asList(
            TestDataFactory.createTestShipment(user, "UPS001", ShipmentStatus.CREATED),
            TestDataFactory.createTestShipment(user, "UPS002", ShipmentStatus.IN_TRANSIT),
            TestDataFactory.createTestShipment(user, "UPS003", ShipmentStatus.DELIVERED),
            TestDataFactory.createTestShipment(user, "UPS004", ShipmentStatus.CANCELLED)
        );
    }

    private void setupTestUsers() {
        testUsers = Arrays.asList(
            TestDataFactory.createTestAdmin(),
            TestDataFactory.createTestUser(),
            TestDataFactory.createTestUser("user2", "user2@example.com", UserRole.USER)
        );
        testUsers.get(0).setId(1L);
        testUsers.get(1).setId(2L);
        testUsers.get(2).setId(3L);
    }

    private void setupMockStatusCounts() {
        mockStatusCounts = Arrays.asList(
            createStatusCount("CREATED", 1),
            createStatusCount("IN_TRANSIT", 1),
            createStatusCount("DELIVERED", 1),
            createStatusCount("CANCELLED", 1)
        );
    }

    private Map<String, Object> createStatusCount(String status, int count) {
        Map<String, Object> statusCount = new HashMap<>();
        statusCount.put("status", status);
        statusCount.put("count", count);
        return statusCount;
    }

    // Mock AuditLog class for testing
    private static class MockAuditLog {
        private Long id;
        private String operationType;
        private String entityType;
        private String entityId;
        private String userId;
        private String operationDescription;

        public MockAuditLog(Long id, String operationType, String entityType, String entityId, 
                           String userId, String operationDescription) {
            this.id = id;
            this.operationType = operationType;
            this.entityType = entityType;
            this.entityId = entityId;
            this.userId = userId;
            this.operationDescription = operationDescription;
        }

        // Getters
        public Long getId() { return id; }
        public String getOperationType() { return operationType; }
        public String getEntityType() { return entityType; }
        public String getEntityId() { return entityId; }
        public String getUserId() { return userId; }
        public String getOperationDescription() { return operationDescription; }
        public java.time.LocalDateTime getCreatedAt() { return java.time.LocalDateTime.now(); }
    }
}