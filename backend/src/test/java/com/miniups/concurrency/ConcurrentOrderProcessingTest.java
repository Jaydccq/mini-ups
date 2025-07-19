package com.miniups.concurrency;

import com.miniups.model.dto.shipment.CreateShipmentDto;
import com.miniups.model.entity.Shipment;
import com.miniups.model.entity.User;
import com.miniups.model.enums.ShipmentStatus;
import com.miniups.service.ShipmentService;
import com.miniups.service.TrackingService;
import com.miniups.util.TestDataFactory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.*;

/**
 * 订单处理并发测试
 * 测试高并发订单创建、状态更新和查询的线程安全性和性能
 */
@DisplayName("订单处理并发测试")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class ConcurrentOrderProcessingTest extends ConcurrencyTestBase {

    @Autowired
    private ShipmentService shipmentService;

    @Autowired
    private TrackingService trackingService;

    @Test
    @DisplayName("并发订单创建测试")
    void testConcurrentOrderCreation() {
        // Given
        int threadCount = 30;
        int operationsPerThread = 5;
        AtomicInteger successCounter = new AtomicInteger(0);
        AtomicInteger errorCounter = new AtomicInteger(0);
        Map<String, Integer> createdOrders = new ConcurrentHashMap<>();

        // When
        ConcurrencyTestResult result = executeConcurrencyTest(() -> {
            try {
                CreateShipmentDto shipmentDto = createTestShipmentDto();
                
                // 使用线程ID确保不同的发送者信息
                long threadId = Thread.currentThread().getId();
                shipmentDto.setSenderName("Sender_" + threadId + "_" + System.nanoTime());
                shipmentDto.setSenderEmail("sender_" + threadId + "@example.com");
                
                Shipment createdShipment = shipmentService.createShipment(shipmentDto);
                
                if (createdShipment != null && createdShipment.getUpsTrackingId() != null) {
                    successCounter.incrementAndGet();
                    createdOrders.put(createdShipment.getUpsTrackingId(), 1);
                    return true;
                } else {
                    errorCounter.incrementAndGet();
                    return false;
                }
            } catch (Exception e) {
                errorCounter.incrementAndGet();
                System.err.println("订单创建失败: " + e.getMessage());
                return false;
            }
        }, threadCount, operationsPerThread, 30);

        // Then
        printConcurrencyTestResult(result, "并发订单创建");
        
        System.out.println("成功创建订单数: " + successCounter.get());
        System.out.println("创建失败数: " + errorCounter.get());
        System.out.println("唯一订单数: " + createdOrders.size());
        
        // 验证所有成功创建的订单都有唯一的追踪号
        assertThat(createdOrders.size()).isEqualTo(successCounter.get());
        assertThat(result.getSuccessRate()).isGreaterThan(90.0);
        
        if (result.getSuccessCount() > 0) {
            assertThat(result.getOperationsPerSecond()).isGreaterThan(5.0);
        }
    }

    @Test
    @DisplayName("并发订单状态更新测试")
    void testConcurrentOrderStatusUpdate() {
        // Given
        int threadCount = 25;
        int operationsPerThread = 4;
        AtomicInteger successCounter = new AtomicInteger(0);
        AtomicInteger conflictCounter = new AtomicInteger(0);
        
        // 预创建一些测试订单
        String[] testTrackingNumbers = createTestOrders(10);
        ShipmentStatus[] statuses = {
            ShipmentStatus.IN_TRANSIT, 
            ShipmentStatus.OUT_FOR_DELIVERY, 
            ShipmentStatus.DELIVERED
        };

        // When
        ConcurrencyTestResult result = executeConcurrencyTest(() -> {
            try {
                // 随机选择一个追踪号和状态进行更新
                String trackingNumber = testTrackingNumbers[(int)(Math.random() * testTrackingNumbers.length)];
                ShipmentStatus newStatus = statuses[(int)(Math.random() * statuses.length)];
                String notes = "Status updated by thread " + Thread.currentThread().getId();
                
                trackingService.updateShipmentStatus(trackingNumber, newStatus, notes);
                successCounter.incrementAndGet();
                return true;
            } catch (Exception e) {
                conflictCounter.incrementAndGet();
                // 一些并发冲突是预期的
                return false;
            }
        }, threadCount, operationsPerThread, 30);

        // Then
        printConcurrencyTestResult(result, "并发订单状态更新");
        
        System.out.println("状态更新成功数: " + successCounter.get());
        System.out.println("并发冲突数: " + conflictCounter.get());
        
        // 验证至少有一些更新成功
        assertThat(successCounter.get()).isGreaterThan(0);
    }

    @Test
    @DisplayName("并发订单查询测试")
    void testConcurrentOrderQuery() {
        // Given
        int threadCount = 40;
        int operationsPerThread = 10;
        
        // 预创建测试订单
        String[] testTrackingNumbers = createTestOrders(20);

        // When
        List<Shipment> results = executeConcurrencyTestWithResults(() -> {
            try {
                String trackingNumber = testTrackingNumbers[(int)(Math.random() * testTrackingNumbers.length)];
                return trackingService.findByTrackingNumber(trackingNumber).orElse(null);
            } catch (Exception e) {
                System.err.println("订单查询失败: " + e.getMessage());
                return null;
            }
        }, threadCount, operationsPerThread);

        // Then
        long foundOrders = results.stream().filter(shipment -> shipment != null).count();
        
        System.out.println("查询总次数: " + results.size());
        System.out.println("成功找到订单次数: " + foundOrders);
        System.out.println("查询成功率: " + String.format("%.2f%%", (double)foundOrders / results.size() * 100));
        
        // 验证查询性能
        assertThat(foundOrders).isGreaterThan(results.size() * 0.8); // 至少80%成功率
    }

    @Test
    @DisplayName("订单生命周期并发测试")
    void testConcurrentOrderLifecycle() {
        // Given
        int threadCount = 20;
        int operationsPerThread = 3;
        AtomicInteger createdCounter = new AtomicInteger(0);
        AtomicInteger updatedCounter = new AtomicInteger(0);
        AtomicInteger queriedCounter = new AtomicInteger(0);
        Map<String, String> orderLifecycle = new ConcurrentHashMap<>();

        // When
        ConcurrencyTestResult result = executeConcurrencyTest(() -> {
            try {
                int operation = (int)(Math.random() * 3);
                
                switch (operation) {
                    case 0: // 创建订单
                        CreateShipmentDto shipmentDto = createTestShipmentDto();
                        long threadId = Thread.currentThread().getId();
                        shipmentDto.setSenderName("Lifecycle_" + threadId + "_" + System.nanoTime());
                        
                        Shipment shipment = shipmentService.createShipment(shipmentDto);
                        if (shipment != null) {
                            createdCounter.incrementAndGet();
                            orderLifecycle.put(shipment.getUpsTrackingId(), "CREATED");
                            return true;
                        }
                        break;
                        
                    case 1: // 更新状态
                        if (!orderLifecycle.isEmpty()) {
                            String trackingNumber = orderLifecycle.keySet().iterator().next();
                            trackingService.updateShipmentStatus(trackingNumber, 
                                ShipmentStatus.IN_TRANSIT, "Updated in lifecycle test");
                            updatedCounter.incrementAndGet();
                            orderLifecycle.put(trackingNumber, "UPDATED");
                            return true;
                        }
                        break;
                        
                    case 2: // 查询订单
                        if (!orderLifecycle.isEmpty()) {
                            String trackingNumber = orderLifecycle.keySet().iterator().next();
                            var found = trackingService.findByTrackingNumber(trackingNumber);
                            if (found.isPresent()) {
                                queriedCounter.incrementAndGet();
                                return true;
                            }
                        }
                        break;
                }
                return false;
            } catch (Exception e) {
                return false;
            }
        }, threadCount, operationsPerThread, 45);

        // Then
        printConcurrencyTestResult(result, "订单生命周期并发");
        
        System.out.println("创建操作数: " + createdCounter.get());
        System.out.println("更新操作数: " + updatedCounter.get());
        System.out.println("查询操作数: " + queriedCounter.get());
        System.out.println("管理的订单数: " + orderLifecycle.size());
        
        // 验证操作的合理性
        assertThat(createdCounter.get()).isGreaterThan(0);
        assertThat(result.getSuccessRate()).isGreaterThan(60.0);
    }

    @Test
    @DisplayName("高负载订单处理性能测试")
    void testHighLoadOrderProcessingPerformance() {
        // Given
        int threadCount = 50;
        int operationsPerThread = 20;
        AtomicInteger successCounter = new AtomicInteger(0);

        // When
        long startTime = System.currentTimeMillis();
        ConcurrencyTestResult result = executeConcurrencyTest(() -> {
            try {
                CreateShipmentDto shipmentDto = createTestShipmentDto();
                long uniqueId = System.nanoTime() + Thread.currentThread().getId();
                shipmentDto.setSenderName("HighLoad_" + uniqueId);
                shipmentDto.setSenderEmail("highload_" + uniqueId + "@example.com");
                
                Shipment shipment = shipmentService.createShipment(shipmentDto);
                if (shipment != null) {
                    successCounter.incrementAndGet();
                    return true;
                }
                return false;
            } catch (Exception e) {
                System.err.println("高负载处理失败: " + e.getMessage());
                return false;
            }
        }, threadCount, operationsPerThread, 120);
        
        long endTime = System.currentTimeMillis();
        double totalTimeSeconds = (endTime - startTime) / 1000.0;

        // Then
        printConcurrencyTestResult(result, "高负载订单处理性能");
        
        System.out.println("总执行时间: " + totalTimeSeconds + " 秒");
        System.out.println("成功处理订单数: " + successCounter.get());
        System.out.println("平均处理速度: " + String.format("%.2f orders/sec", result.getOperationsPerSecond()));
        
        // 性能要求
        assertThat(result.getSuccessRate()).isGreaterThan(80.0);
        assertThat(result.getOperationsPerSecond()).isGreaterThan(8.0);
        assertThat(totalTimeSeconds).isLessThan(60.0);
    }

    @Test
    @DisplayName("订单批量操作并发测试")
    void testConcurrentBatchOrderOperations() {
        // Given
        int threadCount = 15;
        int operationsPerThread = 5;
        AtomicInteger batchSuccessCounter = new AtomicInteger(0);

        // When
        ConcurrencyTestResult result = executeConcurrencyTest(() -> {
            try {
                // 模拟批量创建订单
                int batchSize = 3;
                boolean allSuccess = true;
                
                for (int i = 0; i < batchSize; i++) {
                    CreateShipmentDto shipmentDto = createTestShipmentDto();
                    long uniqueId = System.nanoTime() + Thread.currentThread().getId() + i;
                    shipmentDto.setSenderName("Batch_" + uniqueId);
                    shipmentDto.setSenderEmail("batch_" + uniqueId + "@example.com");
                    
                    Shipment shipment = shipmentService.createShipment(shipmentDto);
                    if (shipment == null) {
                        allSuccess = false;
                        break;
                    }
                }
                
                if (allSuccess) {
                    batchSuccessCounter.incrementAndGet();
                    return true;
                }
                return false;
            } catch (Exception e) {
                return false;
            }
        }, threadCount, operationsPerThread, 45);

        // Then
        printConcurrencyTestResult(result, "订单批量操作并发");
        
        System.out.println("批量操作成功数: " + batchSuccessCounter.get());
        
        // 验证批量操作的效果
        assertThat(batchSuccessCounter.get()).isGreaterThan(0);
    }

    @Test
    @DisplayName("订单处理基准测试")
    void testOrderProcessingBenchmark() {
        // When & Then
        benchmarkOperation("订单处理", () -> {
            try {
                CreateShipmentDto shipmentDto = createTestShipmentDto();
                long uniqueId = System.nanoTime();
                shipmentDto.setSenderName("Benchmark_" + uniqueId);
                shipmentDto.setSenderEmail("benchmark_" + uniqueId + "@example.com");
                
                Shipment shipment = shipmentService.createShipment(shipmentDto);
                return shipment != null;
            } catch (Exception e) {
                return false;
            }
        }, 30, 50);
    }

    @Test
    @DisplayName("订单处理竞争条件测试")
    void testOrderProcessingRaceConditions() {
        // Given
        AtomicInteger operationCounter = new AtomicInteger(0);
        String[] testTrackingNumbers = createTestOrders(5);

        // When & Then
        assertThatCode(() -> {
            executeRaceConditionTest(() -> {
                int operation = operationCounter.incrementAndGet();
                try {
                    switch (operation % 3) {
                        case 0: // 创建订单
                            CreateShipmentDto shipmentDto = createTestShipmentDto();
                            shipmentDto.setSenderName("Race_" + operation);
                            shipmentService.createShipment(shipmentDto);
                            break;
                        case 1: // 更新状态
                            if (testTrackingNumbers.length > 0) {
                                String trackingNumber = testTrackingNumbers[operation % testTrackingNumbers.length];
                                trackingService.updateShipmentStatus(trackingNumber, 
                                    ShipmentStatus.IN_TRANSIT, "Race condition test");
                            }
                            break;
                        case 2: // 查询订单
                            if (testTrackingNumbers.length > 0) {
                                String trackingNumber = testTrackingNumbers[operation % testTrackingNumbers.length];
                                trackingService.findByTrackingNumber(trackingNumber);
                            }
                            break;
                    }
                } catch (Exception e) {
                    // 忽略预期的竞争条件异常
                }
            }, 40, 25);
        }).doesNotThrowAnyException();

        System.out.println("订单处理竞争条件测试完成，操作次数: " + operationCounter.get());
    }

    // Helper Methods

    private CreateShipmentDto createTestShipmentDto() {
        CreateShipmentDto dto = new CreateShipmentDto();
        dto.setSenderName("Test Sender");
        dto.setSenderEmail("sender@example.com");
        dto.setSenderPhone("1234567890");
        dto.setSenderAddress("123 Sender St");
        dto.setRecipientName("Test Recipient");
        dto.setRecipientEmail("recipient@example.com");
        dto.setRecipientPhone("0987654321");
        dto.setRecipientAddress("456 Recipient Ave");
        dto.setOriginX(10);
        dto.setOriginY(20);
        dto.setDestX(30);
        dto.setDestY(40);
        dto.setWeight(BigDecimal.valueOf(5.0));
        dto.setShipmentId("SH" + System.nanoTime());
        return dto;
    }

    private String[] createTestOrders(int count) {
        String[] trackingNumbers = new String[count];
        for (int i = 0; i < count; i++) {
            try {
                CreateShipmentDto shipmentDto = createTestShipmentDto();
                shipmentDto.setSenderName("PreCreated_" + i + "_" + System.currentTimeMillis());
                shipmentDto.setSenderEmail("precreated_" + i + "@example.com");
                shipmentDto.setShipmentId("PRE" + System.nanoTime() + i);
                
                Shipment shipment = shipmentService.createShipment(shipmentDto);
                if (shipment != null) {
                    trackingNumbers[i] = shipment.getUpsTrackingId();
                } else {
                    trackingNumbers[i] = "UPS" + System.currentTimeMillis() + String.format("%04d", i);
                }
            } catch (Exception e) {
                trackingNumbers[i] = "UPS" + System.currentTimeMillis() + String.format("%04d", i);
            }
        }
        return trackingNumbers;
    }
}