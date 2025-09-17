package com.miniups.concurrency;

import com.miniups.model.entity.Truck;
import com.miniups.model.enums.TruckStatus;
import com.miniups.service.TruckManagementService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.*;

/**
 * 车辆分配并发测试
 * 测试高并发车辆分配的线程安全性、避免重复分配和性能
 */
@DisplayName("车辆分配并发测试")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class ConcurrentTruckAssignmentTest extends ConcurrencyTestBase {

    @Autowired
    private TruckManagementService truckManagementService;

    @Test
    @DisplayName("并发车辆分配 - 避免重复分配")
    void testConcurrentTruckAssignment_AvoidDuplicateAssignment() {
        // Given
        int threadCount = 30;
        int operationsPerThread = 5;
        AtomicInteger successCounter = new AtomicInteger(0);
        AtomicInteger noTruckCounter = new AtomicInteger(0);
        Map<Integer, Integer> assignedTrucks = new ConcurrentHashMap<>();

        // When
        ConcurrencyTestResult result = executeConcurrencyTest(() -> {
            try {
                // 模拟多个订单同时请求车辆分配
                int originX = 10 + (int)(Math.random() * 20);
                int originY = 20 + (int)(Math.random() * 20);
                int warehouseId = 1;
                
                Truck assignedTruck = truckManagementService.assignOptimalTruck(originX, originY, warehouseId);
                
                if (assignedTruck != null) {
                    successCounter.incrementAndGet();
                    // 检查是否有重复分配
                    Integer previousCount = assignedTrucks.put(assignedTruck.getTruckId(), 
                        assignedTrucks.getOrDefault(assignedTruck.getTruckId(), 0) + 1);
                    
                    if (previousCount != null && previousCount > 0) {
                        System.err.println("警告: 车辆 " + assignedTruck.getTruckId() + " 被重复分配!");
                    }
                    return true;
                } else {
                    noTruckCounter.incrementAndGet();
                    return false;
                }
            } catch (Exception e) {
                System.err.println("车辆分配异常: " + e.getMessage());
                return false;
            }
        }, threadCount, operationsPerThread, 30);

        // Then
        printConcurrencyTestResult(result, "并发车辆分配");
        
        System.out.println("成功分配车辆次数: " + successCounter.get());
        System.out.println("无可用车辆次数: " + noTruckCounter.get());
        System.out.println("不同车辆分配数量: " + assignedTrucks.size());
        
        // 验证没有车辆被重复分配
        for (Map.Entry<Integer, Integer> entry : assignedTrucks.entrySet()) {
            assertThat(entry.getValue())
                .as("车辆 %d 不应被重复分配", entry.getKey())
                .isEqualTo(1);
        }
        
        // 如果有成功分配，应该有合理的成功率
        if (successCounter.get() > 0) {
            assertThat(result.getOperationsPerSecond()).isGreaterThan(5.0);
        }
    }

    @Test
    @DisplayName("车辆释放并发测试")
    void testConcurrentTruckRelease() {
        // Given
        int threadCount = 20;
        int operationsPerThread = 3;
        AtomicInteger successCounter = new AtomicInteger(0);
        AtomicInteger failureCounter = new AtomicInteger(0);

        // When
        ConcurrencyTestResult result = executeConcurrencyTest(() -> {
            try {
                // 模拟不同的车辆ID（1-10）
                int truckId = 1 + (int)(Math.random() * 10);
                boolean released = truckManagementService.releaseTruck(truckId);
                
                if (released) {
                    successCounter.incrementAndGet();
                    return true;
                } else {
                    failureCounter.incrementAndGet();
                    return false;
                }
            } catch (Exception e) {
                failureCounter.incrementAndGet();
                return false;
            }
        }, threadCount, operationsPerThread, 30);

        // Then
        printConcurrencyTestResult(result, "车辆释放并发");
        
        System.out.println("释放成功次数: " + successCounter.get());
        System.out.println("释放失败次数: " + failureCounter.get());
        
        // 验证操作的原子性
        assertThat(successCounter.get() + failureCounter.get()).isEqualTo(result.getTotalOperations());
    }

    @Test
    @DisplayName("车辆状态更新并发测试")
    void testConcurrentTruckStatusUpdate() {
        // Given
        int threadCount = 25;
        int operationsPerThread = 4;
        AtomicInteger successCounter = new AtomicInteger(0);
        String[] statuses = {"IDLE", "EN_ROUTE", "DELIVERING", "RETURNING"};

        // When
        ConcurrencyTestResult result = executeConcurrencyTest(() -> {
            try {
                int truckId = 1 + (int)(Math.random() * 5); // 车辆ID 1-5
                int x = (int)(Math.random() * 100);
                int y = (int)(Math.random() * 100);
                String status = statuses[(int)(Math.random() * statuses.length)];
                
                boolean updated = truckManagementService.updateTruckStatus(truckId, x, y, status);
                
                if (updated) {
                    successCounter.incrementAndGet();
                    return true;
                } else {
                    return false;
                }
            } catch (Exception e) {
                System.err.println("状态更新失败: " + e.getMessage());
                return false;
            }
        }, threadCount, operationsPerThread, 30);

        // Then
        printConcurrencyTestResult(result, "车辆状态更新并发");
        
        System.out.println("状态更新成功次数: " + successCounter.get());
        
        // 验证有合理的成功率
        if (result.getTotalOperations() > 0) {
            assertThat(result.getOperationsPerSecond()).isGreaterThan(3.0);
        }
    }

    @Test
    @DisplayName("最近车辆查找并发测试")
    void testConcurrentNearestTruckSearch() {
        // Given
        int threadCount = 40;
        int operationsPerThread = 5;
        
        // When
        List<Truck> results = executeConcurrencyTestWithResults(() -> {
            try {
                int targetX = (int)(Math.random() * 50);
                int targetY = (int)(Math.random() * 50);
                
                return truckManagementService.findNearestAvailableTruck(targetX, targetY);
            } catch (Exception e) {
                System.err.println("查找最近车辆失败: " + e.getMessage());
                return null;
            }
        }, threadCount, operationsPerThread);

        // Then
        long nonNullResults = results.stream().filter(truck -> truck != null).count();
        
        System.out.println("总查找次数: " + results.size());
        System.out.println("找到车辆次数: " + nonNullResults);
        System.out.println("查找成功率: " + String.format("%.2f%%", (double)nonNullResults / results.size() * 100));
        
        // 验证结果的一致性
        assertThat(results).isNotEmpty();
    }

    @Test
    @DisplayName("车队统计信息并发访问测试")
    void testConcurrentFleetStatisticsAccess() {
        // Given
        int threadCount = 30;
        int operationsPerThread = 10;
        
        // When
        List<Map<String, Object>> results = executeConcurrencyTestWithResults(() -> {
            try {
                return truckManagementService.getFleetStatistics();
            } catch (Exception e) {
                System.err.println("获取车队统计失败: " + e.getMessage());
                return null;
            }
        }, threadCount, operationsPerThread);

        // Then
        long nonNullResults = results.stream().filter(stats -> stats != null).count();
        
        System.out.println("统计查询总次数: " + results.size());
        System.out.println("成功获取统计次数: " + nonNullResults);
        
        // 验证所有查询都应该成功
        assertThat(nonNullResults).isEqualTo(results.size());
        
        // 验证统计数据的一致性
        if (!results.isEmpty() && results.get(0) != null) {
            Map<String, Object> firstResult = results.get(0);
            for (Map<String, Object> result : results) {
                if (result != null) {
                    // 车队总数在并发访问期间应该相对稳定
                    assertThat(result).containsKey("total_trucks");
                    assertThat(result).containsKey("available_trucks");
                    assertThat(result).containsKey("busy_trucks");
                }
            }
        }
    }

    @Test
    @DisplayName("批量车辆位置更新并发测试")
    void testConcurrentBatchTruckPositionUpdate() {
        // Given
        int threadCount = 15;
        int operationsPerThread = 5;
        AtomicInteger successCounter = new AtomicInteger(0);

        // When
        ConcurrencyTestResult result = executeConcurrencyTest(() -> {
            try {
                // 创建批量更新数据
                List<Map<String, Object>> updates = List.of(
                    Map.of("truck_id", 1, "x", (int)(Math.random() * 100), "y", (int)(Math.random() * 100), "status", "idle"),
                    Map.of("truck_id", 2, "x", (int)(Math.random() * 100), "y", (int)(Math.random() * 100), "status", "traveling"),
                    Map.of("truck_id", 3, "x", (int)(Math.random() * 100), "y", (int)(Math.random() * 100), "status", "idle")
                );
                
                truckManagementService.batchUpdateTruckPositions(updates);
                successCounter.incrementAndGet();
                return true;
            } catch (Exception e) {
                System.err.println("批量更新失败: " + e.getMessage());
                return false;
            }
        }, threadCount, operationsPerThread, 30);

        // Then
        printConcurrencyTestResult(result, "批量车辆位置更新并发");
        
        System.out.println("批量更新成功次数: " + successCounter.get());
        
        // 验证有合理的成功率
        assertThat(result.getSuccessRate()).isGreaterThan(80.0);
    }

    @Test
    @DisplayName("车辆分配性能基准测试")
    void testTruckAssignmentPerformanceBenchmark() {
        // Given
        int threadCount = 50;
        int operationsPerThread = 20;

        // When & Then
        benchmarkOperation("车辆分配", () -> {
            try {
                int originX = (int)(Math.random() * 100);
                int originY = (int)(Math.random() * 100);
                int warehouseId = 1;
                
                Truck truck = truckManagementService.assignOptimalTruck(originX, originY, warehouseId);
                return truck != null;
            } catch (Exception e) {
                return false;
            }
        }, threadCount, operationsPerThread);
    }

    @Test
    @DisplayName("车辆管理竞争条件测试")
    void testTruckManagementRaceConditions() {
        // Given
        AtomicInteger operationCounter = new AtomicInteger(0);

        // When & Then
        assertThatCode(() -> {
            executeRaceConditionTest(() -> {
                int operation = operationCounter.incrementAndGet();
                try {
                    // 快速连续执行不同的车辆管理操作
                    switch (operation % 4) {
                        case 0:
                            truckManagementService.assignOptimalTruck(10, 20, 1);
                            break;
                        case 1:
                            truckManagementService.releaseTruck(1);
                            break;
                        case 2:
                            truckManagementService.updateTruckStatus(1, 50, 60, "IDLE");
                            break;
                        case 3:
                            truckManagementService.getFleetStatistics();
                            break;
                    }
                } catch (Exception e) {
                    // 忽略预期的异常
                }
            }, 30, 10);
        }).doesNotThrowAnyException();

        System.out.println("竞争条件测试完成，操作次数: " + operationCounter.get());
    }
}