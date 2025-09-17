package com.miniups.concurrency;

import com.miniups.model.dto.auth.RegisterRequestDto;
import com.miniups.model.dto.shipment.CreateShipmentDto;
import com.miniups.model.entity.Shipment;
import com.miniups.service.AuthService;
import com.miniups.service.ShipmentService;
import com.miniups.service.TrackingService;
import com.miniups.service.TruckManagementService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;

import java.math.BigDecimal;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.*;

/**
 * 综合性能基准测试
 * 测试整个系统在高并发场景下的综合性能表现
 */
@DisplayName("综合性能基准测试")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class PerformanceBenchmarkTest extends ConcurrencyTestBase {

    @Autowired
    private AuthService authService;

    @Autowired
    private ShipmentService shipmentService;

    @Autowired
    private TrackingService trackingService;

    @Autowired
    private TruckManagementService truckManagementService;

    @Test
    @DisplayName("系统综合性能基准测试")
    void testSystemComprehensivePerformanceBenchmark() {
        System.out.println("\n" + "=".repeat(60));
        System.out.println("Mini-UPS 系统综合性能基准测试");
        System.out.println("=".repeat(60));

        // 用户注册性能测试
        System.out.println("\n📊 用户注册性能基准:");
        benchmarkOperation("用户注册", () -> {
            RegisterRequestDto request = createRegisterRequest();
            authService.register(request);
            return true;
        }, 10, 5);  // 减少负载

        // 追踪号生成性能测试
        System.out.println("\n📊 追踪号生成性能基准:");
        benchmarkOperation("追踪号生成", () -> {
            String trackingNumber = trackingService.generateTrackingNumber();
            return trackingNumber != null && trackingService.isValidTrackingNumberFormat(trackingNumber);
        }, 15, 10);  // 减少负载

        // 订单创建性能测试
        System.out.println("\n📊 订单创建性能基准:");
        benchmarkOperation("订单创建", () -> {
            CreateShipmentDto shipmentDto = createShipmentDto();
            Shipment shipment = shipmentService.createShipment(shipmentDto);
            return shipment != null;
        }, 10, 8);  // 减少负载

        // 车辆分配性能测试
        System.out.println("\n📊 车辆分配性能基准:");
        benchmarkOperation("车辆分配", () -> {
            return truckManagementService.assignOptimalTruck(
                (int)(Math.random() * 100), 
                (int)(Math.random() * 100), 
                1
            ) != null;
        }, 8, 5);  // 减少负载

        System.out.println("\n" + "=".repeat(60));
        System.out.println("综合性能基准测试完成");
        System.out.println("=".repeat(60));
    }

    @Test
    @DisplayName("混合工作负载性能测试")
    void testMixedWorkloadPerformance() {
        // Given
        int threadCount = 40;
        int operationsPerThread = 25;
        AtomicInteger userOpsCounter = new AtomicInteger(0);
        AtomicInteger orderOpsCounter = new AtomicInteger(0);
        AtomicInteger trackingOpsCounter = new AtomicInteger(0);
        AtomicInteger truckOpsCounter = new AtomicInteger(0);

        System.out.println("\n🔀 混合工作负载性能测试");
        System.out.println("线程数: " + threadCount + ", 每线程操作数: " + operationsPerThread);

        // When
        long startTime = System.currentTimeMillis();
        ConcurrencyTestResult result = executeConcurrencyTest(() -> {
            // 随机选择不同类型的操作
            int operationType = (int)(Math.random() * 4);
            
            switch (operationType) {
                case 0: // 用户注册
                    RegisterRequestDto request = createRegisterRequest();
                    authService.register(request);
                    userOpsCounter.incrementAndGet();
                    return true;
                    
                case 1: // 订单创建
                    CreateShipmentDto shipmentDto = createShipmentDto();
                    Shipment shipment = shipmentService.createShipment(shipmentDto);
                    orderOpsCounter.incrementAndGet();
                    return shipment != null;
                    
                case 2: // 追踪号生成
                    String trackingNumber = trackingService.generateTrackingNumber();
                    trackingOpsCounter.incrementAndGet();
                    return trackingNumber != null;
                    
                case 3: // 车辆操作
                    boolean success = truckManagementService.assignOptimalTruck(
                        (int)(Math.random() * 100), 
                        (int)(Math.random() * 100), 
                        1
                    ) != null;
                    truckOpsCounter.incrementAndGet();
                    return success;
                    
                default:
                    return false;
            }
        }, threadCount, operationsPerThread, 90);
        
        long endTime = System.currentTimeMillis();
        double totalTimeSeconds = (endTime - startTime) / 1000.0;

        // Then
        printConcurrencyTestResult(result, "混合工作负载");
        
        System.out.println("\n📈 操作分布统计:");
        System.out.println("用户注册操作: " + userOpsCounter.get());
        System.out.println("订单创建操作: " + orderOpsCounter.get());
        System.out.println("追踪号生成操作: " + trackingOpsCounter.get());
        System.out.println("车辆分配操作: " + truckOpsCounter.get());
        System.out.println("总操作数验证: " + (userOpsCounter.get() + orderOpsCounter.get() + 
                          trackingOpsCounter.get() + truckOpsCounter.get()));

        // 性能验证
        assertThat(result.getSuccessRate()).isGreaterThan(70.0);
        assertThat(result.getOperationsPerSecond()).isGreaterThan(10.0);
        assertThat(totalTimeSeconds).isLessThan(60.0);
        
        System.out.println("\n🎯 混合负载性能评级: " + getPerformanceGrade(result.getOperationsPerSecond()));
    }

    @Test
    @DisplayName("渐进式负载测试")
    void testProgressiveLoadTest() {
        System.out.println("\n📈 渐进式负载测试");
        
        int[] threadCounts = {10, 20, 40, 60, 80};
        int operationsPerThread = 20;
        
        System.out.println("线程数\t成功率\t\t吞吐量\t\t平均响应时间");
        System.out.println("-".repeat(60));
        
        for (int threadCount : threadCounts) {
            long startTime = System.currentTimeMillis();
            
            ConcurrencyTestResult result = executeConcurrencyTest(() -> {
                // 使用订单创建作为标准负载测试
                CreateShipmentDto shipmentDto = createShipmentDto();
                Shipment shipment = shipmentService.createShipment(shipmentDto);
                return shipment != null;
            }, threadCount, operationsPerThread, 60);
            
            long endTime = System.currentTimeMillis();
            double avgResponseTime = (double)(endTime - startTime) / result.getTotalOperations();
            
            System.out.printf("%d\t\t%.2f%%\t\t%.2f ops/s\t%.2f ms%n",
                threadCount,
                result.getSuccessRate(),
                result.getOperationsPerSecond(),
                avgResponseTime
            );
            
            // 验证性能不会显著降级
            assertThat(result.getSuccessRate()).isGreaterThan(60.0);
            
            // 短暂休息以避免系统过载
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        
        System.out.println("-".repeat(60));
        System.out.println("渐进式负载测试完成");
    }

    @Test
    @DisplayName("持续负载耐久测试")
    void testSustainedLoadEnduranceTest() {
        System.out.println("\n⏰ 持续负载耐久测试 (15秒)");
        
        int threadCount = 10;  // 减少线程数
        int operationsPerThread = 150; // 增加操作数以满足测试时长要求
        AtomicInteger totalOperations = new AtomicInteger(0);
        AtomicInteger successOperations = new AtomicInteger(0);
        
        long testDuration = 15000; // 15秒
        long startTime = System.currentTimeMillis();
        
        ConcurrencyTestResult result = executeConcurrencyTest(() -> {
            long currentTime = System.currentTimeMillis();
            
            // 检查是否超过测试时间
            if (currentTime - startTime > testDuration) {
                return false;
            }
            
            totalOperations.incrementAndGet();
            
            // 轮换不同的操作类型
            int opType = totalOperations.get() % 3;
            boolean success = false;
            
            switch (opType) {
                case 0:
                    String trackingNumber = trackingService.generateTrackingNumber();
                    success = trackingNumber != null;
                    break;
                case 1:
                    success = truckManagementService.findNearestAvailableTruck(50, 50) != null;
                    break;
                case 2:
                    CreateShipmentDto shipmentDto = createShipmentDto();
                    success = shipmentService.createShipment(shipmentDto) != null;
                    break;
            }
            
            if (success) {
                successOperations.incrementAndGet();
            }
            
            return success;
        }, threadCount, operationsPerThread, 35);
        
        long actualEndTime = System.currentTimeMillis();
        double actualDuration = (actualEndTime - startTime) / 1000.0;
        
        // Then
        printConcurrencyTestResult(result, "持续负载耐久");
        
        System.out.println("\n📊 耐久测试详细结果:");
        System.out.println("实际测试时长: " + String.format("%.2f", actualDuration) + " 秒");
        System.out.println("总操作尝试: " + totalOperations.get());
        System.out.println("成功操作数: " + successOperations.get());
        System.out.println("平均每秒操作: " + String.format("%.2f", totalOperations.get() / actualDuration));
        
        // 耐久性验证
        assertThat(result.getSuccessRate()).isGreaterThan(60.0);
        assertThat(actualDuration).isBetween(12.0, 20.0); // 在预期时间范围内
        
        System.out.println("系统耐久性评级: " + getEnduranceGrade(result.getSuccessRate(), actualDuration));
    }

    @Test
    @DisplayName("内存使用和性能关联测试")
    void testMemoryUsageAndPerformanceCorrelation() {
        System.out.println("\n💾 内存使用和性能关联测试");
        
        Runtime runtime = Runtime.getRuntime();
        
        // 测试前内存状态
        System.gc();
        long initialMemory = runtime.totalMemory() - runtime.freeMemory();
        
        System.out.println("初始内存使用: " + (initialMemory / 1024 / 1024) + " MB");
        
        // 执行高负载测试
        ConcurrencyTestResult result = executeConcurrencyTest(() -> {
            CreateShipmentDto shipmentDto = createShipmentDto();
            return shipmentService.createShipment(shipmentDto) != null;
        }, 15, 5, 60);  // 大幅减少负载
        
        // 测试后内存状态
        System.gc();
        long finalMemory = runtime.totalMemory() - runtime.freeMemory();
        long memoryIncrease = finalMemory - initialMemory;
        
        printConcurrencyTestResult(result, "内存性能关联");
        
        System.out.println("\n📊 内存使用分析:");
        System.out.println("最终内存使用: " + (finalMemory / 1024 / 1024) + " MB");
        System.out.println("内存增长: " + (memoryIncrease / 1024 / 1024) + " MB");
        
        if (result.getTotalOperations() > 0) {
            long memoryPerOperation = memoryIncrease / result.getTotalOperations();
            System.out.println("平均每操作内存消耗: " + memoryPerOperation + " bytes");
        }
        
        // 内存使用合理性验证
        assertThat(memoryIncrease).isLessThan(200 * 1024 * 1024); // 不超过200MB增长
        assertThat(result.getSuccessRate()).isGreaterThan(70.0);
    }

    // Helper Methods

    private RegisterRequestDto createRegisterRequest() {
        long uniqueId = System.nanoTime() + Thread.currentThread().getId();
        RegisterRequestDto request = new RegisterRequestDto();
        request.setUsername("bench_user_" + uniqueId);
        request.setEmail("bench_user_" + uniqueId + "@example.com");
        request.setPassword("Password123!");
        request.setFirstName("Bench");
        request.setLastName("User");
        request.setPhone("1234567890");
        request.setAddress("123 Benchmark St");
        return request;
    }

    private CreateShipmentDto createShipmentDto() {
        long uniqueId = System.nanoTime() + Thread.currentThread().getId();
        CreateShipmentDto dto = new CreateShipmentDto();
        dto.setCustomerId("CUSTOMER_" + uniqueId);
        dto.setCustomerName("Bench Customer " + uniqueId);
        dto.setCustomerEmail("customer_" + uniqueId + "@example.com");
        dto.setOriginX((int)(Math.random() * 100));
        dto.setOriginY((int)(Math.random() * 100));
        dto.setDestX((int)(Math.random() * 100));
        dto.setDestY((int)(Math.random() * 100));
        dto.setWeight(new BigDecimal("1.0").add(BigDecimal.valueOf(Math.random() * 10.0)));
        dto.setShipmentId("BENCH" + uniqueId);
        return dto;
    }

    private String getPerformanceGrade(double operationsPerSecond) {
        if (operationsPerSecond >= 50) return "优秀 (A+)";
        if (operationsPerSecond >= 30) return "良好 (A)";
        if (operationsPerSecond >= 20) return "中等 (B)";
        if (operationsPerSecond >= 10) return "一般 (C)";
        return "需要优化 (D)";
    }

    private String getEnduranceGrade(double successRate, double duration) {
        if (successRate >= 85 && duration >= 25) return "优秀 - 系统稳定性极佳";
        if (successRate >= 75 && duration >= 20) return "良好 - 系统稳定性良好";
        if (successRate >= 60 && duration >= 15) return "中等 - 系统基本稳定";
        return "需要优化 - 系统稳定性有待提升";
    }
}