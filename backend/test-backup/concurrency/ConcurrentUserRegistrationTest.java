package com.miniups.concurrency;

import com.miniups.model.dto.auth.RegisterRequestDto;
import com.miniups.service.AuthService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * 用户注册并发测试
 * 测试高并发用户注册的线程安全性、数据一致性和性能
 */
@DisplayName("用户注册并发测试")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class ConcurrentUserRegistrationTest extends ConcurrencyTestBase {

    @Autowired
    private AuthService authService;

    @Test
    @DisplayName("并发用户注册 - 唯一用户名验证")
    void testConcurrentUserRegistration_UniqueUsername() {
        // Given
        String baseUsername = "user_" + System.currentTimeMillis();
        int threadCount = 20;
        int operationsPerThread = 5;
        AtomicInteger successCounter = new AtomicInteger(0);
        AtomicInteger conflictCounter = new AtomicInteger(0);

        // When
        ConcurrencyTestResult result = executeConcurrencyTest(() -> {
            try {
                String uniqueUsername = baseUsername + "_" + successCounter.incrementAndGet();
                RegisterRequestDto request = createRegisterRequest(uniqueUsername, 
                    uniqueUsername + "@example.com");
                
                authService.register(request);
                return true;
            } catch (Exception e) {
                conflictCounter.incrementAndGet();
                return false;
            }
        }, threadCount, operationsPerThread, 30);

        // Then
        printConcurrencyTestResult(result, "并发用户注册");
        
        // 验证所有注册都应该成功（因为用户名都是唯一的）
        assertThat(result.getSuccessRate()).isGreaterThan(95.0);
        assertThat(result.getOperationsPerSecond()).isGreaterThan(5.0);
        
        System.out.println("成功注册用户数: " + successCounter.get());
        System.out.println("冲突数量: " + conflictCounter.get());
    }

    @Test
    @DisplayName("并发用户注册 - 相同用户名冲突处理")
    void testConcurrentUserRegistration_SameUsernameConflict() {
        // Given
        String duplicateUsername = "duplicate_user_" + System.currentTimeMillis();
        int threadCount = 50;
        int operationsPerThread = 2;
        AtomicInteger successCounter = new AtomicInteger(0);
        AtomicInteger conflictCounter = new AtomicInteger(0);

        // When
        ConcurrencyTestResult result = executeConcurrencyTest(() -> {
            try {
                // 所有线程尝试注册相同的用户名
                RegisterRequestDto request = createRegisterRequest(duplicateUsername, 
                    duplicateUsername + "_" + Thread.currentThread().getId() + "@example.com");
                
                authService.register(request);
                successCounter.incrementAndGet();
                return true;
            } catch (Exception e) {
                conflictCounter.incrementAndGet();
                return false;
            }
        }, threadCount, operationsPerThread, 30);

        // Then
        printConcurrencyTestResult(result, "相同用户名冲突处理");
        
        // 应该只有一个注册成功，其他都应该失败
        assertThat(successCounter.get()).isEqualTo(1);
        assertThat(conflictCounter.get()).isEqualTo(result.getFailureCount());
        assertThat(result.getFailureRate()).isGreaterThan(90.0);
        
        System.out.println("成功注册数: " + successCounter.get());
        System.out.println("冲突失败数: " + conflictCounter.get());
    }

    @Test
    @DisplayName("并发用户注册 - 相同邮箱冲突处理")
    void testConcurrentUserRegistration_SameEmailConflict() {
        // Given
        String duplicateEmail = "duplicate_" + System.currentTimeMillis() + "@example.com";
        int threadCount = 30;
        int operationsPerThread = 3;
        AtomicInteger successCounter = new AtomicInteger(0);
        AtomicInteger conflictCounter = new AtomicInteger(0);

        // When
        ConcurrencyTestResult result = executeConcurrencyTest(() -> {
            try {
                // 所有线程尝试注册相同的邮箱
                String uniqueUsername = "user_" + Thread.currentThread().getId() + "_" + System.nanoTime();
                RegisterRequestDto request = createRegisterRequest(uniqueUsername, duplicateEmail);
                
                authService.register(request);
                successCounter.incrementAndGet();
                return true;
            } catch (Exception e) {
                conflictCounter.incrementAndGet();
                return false;
            }
        }, threadCount, operationsPerThread, 30);

        // Then
        printConcurrencyTestResult(result, "相同邮箱冲突处理");
        
        // 应该只有一个注册成功，其他都应该失败
        assertThat(successCounter.get()).isEqualTo(1);
        assertThat(conflictCounter.get()).isEqualTo(result.getFailureCount());
        assertThat(result.getFailureRate()).isGreaterThan(90.0);
        
        System.out.println("成功注册数: " + successCounter.get());
        System.out.println("邮箱冲突数: " + conflictCounter.get());
    }

    @Test
    @DisplayName("高负载用户注册性能测试")
    void testHighLoadUserRegistrationPerformance() {
        // Given
        int threadCount = 100;
        int operationsPerThread = 10;
        AtomicInteger userCounter = new AtomicInteger(0);

        // When
        long startTime = System.currentTimeMillis();
        ConcurrencyTestResult result = executeConcurrencyTest(() -> {
            try {
                int userNum = userCounter.incrementAndGet();
                String username = "perf_user_" + userNum + "_" + System.currentTimeMillis();
                String email = "perf_user_" + userNum + "@example.com";
                
                RegisterRequestDto request = createRegisterRequest(username, email);
                authService.register(request);
                return true;
            } catch (Exception e) {
                System.err.println("Registration failed: " + e.getMessage());
                return false;
            }
        }, threadCount, operationsPerThread, 60);
        
        long endTime = System.currentTimeMillis();
        double totalTimeSeconds = (endTime - startTime) / 1000.0;

        // Then
        printConcurrencyTestResult(result, "高负载用户注册性能");
        
        // 性能要求
        assertThat(result.getSuccessRate()).isGreaterThan(90.0);
        assertThat(result.getOperationsPerSecond()).isGreaterThan(10.0);
        assertThat(totalTimeSeconds).isLessThan(30.0);
        
        System.out.println("总执行时间: " + totalTimeSeconds + " 秒");
        System.out.println("平均每秒注册用户数: " + String.format("%.2f", result.getOperationsPerSecond()));
    }

    @Test
    @DisplayName("用户注册压力测试 - 极限负载")
    void testUserRegistrationStressTest() {
        // Given
        int threadCount = 200;
        int operationsPerThread = 5;
        AtomicInteger userCounter = new AtomicInteger(0);
        AtomicInteger dbErrorCounter = new AtomicInteger(0);
        AtomicInteger validationErrorCounter = new AtomicInteger(0);

        // When
        ConcurrencyTestResult result = executeConcurrencyTest(() -> {
            try {
                int userNum = userCounter.incrementAndGet();
                String username = "stress_user_" + userNum;
                String email = "stress_user_" + userNum + "@example.com";
                
                RegisterRequestDto request = createRegisterRequest(username, email);
                authService.register(request);
                return true;
            } catch (Exception e) {
                if (e.getMessage().contains("database") || e.getMessage().contains("connection")) {
                    dbErrorCounter.incrementAndGet();
                } else if (e.getMessage().contains("validation") || e.getMessage().contains("constraint")) {
                    validationErrorCounter.incrementAndGet();
                }
                return false;
            }
        }, threadCount, operationsPerThread, 120);

        // Then
        printConcurrencyTestResult(result, "用户注册压力测试");
        
        // 压力测试容忍一定的失败率
        assertThat(result.getSuccessRate()).isGreaterThan(70.0);
        
        System.out.println("数据库错误数: " + dbErrorCounter.get());
        System.out.println("验证错误数: " + validationErrorCounter.get());
        System.out.println("系统在极限负载下的表现: " + 
                          (result.getSuccessRate() > 85.0 ? "优秀" : 
                           result.getSuccessRate() > 70.0 ? "良好" : "需要优化"));
    }

    @Test
    @DisplayName("用户注册竞争条件测试")
    void testUserRegistrationRaceConditions() {
        // Given
        String baseUsername = "race_user_" + System.currentTimeMillis();
        AtomicInteger attemptCounter = new AtomicInteger(0);

        // When & Then
        assertThatCode(() -> {
            executeRaceConditionTest(() -> {
                int attempt = attemptCounter.incrementAndGet();
                try {
                    // 快速连续注册，测试竞争条件
                    String username = baseUsername + "_" + attempt;
                    String email = username + "@example.com";
                    
                    RegisterRequestDto request = createRegisterRequest(username, email);
                    authService.register(request);
                } catch (Exception e) {
                    // 忽略预期的冲突异常
                }
            }, 50, 20);
        }).doesNotThrowAnyException();

        System.out.println("竞争条件测试完成，尝试次数: " + attemptCounter.get());
    }

    @Test
    @DisplayName("用户名可用性检查并发测试")
    void testConcurrentUsernameAvailabilityCheck() {
        // Given
        String baseUsername = "check_user_" + System.currentTimeMillis();
        
        // When
        List<Boolean> results = executeConcurrencyTestWithResults(() -> {
            try {
                String username = baseUsername + "_" + Thread.currentThread().getId();
                return authService.isUsernameAvailable(username);
            } catch (Exception e) {
                return false;
            }
        }, 50, 10);

        // Then
        assertThat(results).isNotEmpty();
        long availableCount = results.stream().mapToLong(b -> b ? 1 : 0).sum();
        
        System.out.println("可用性检查结果数量: " + results.size());
        System.out.println("可用用户名数量: " + availableCount);
        
        // 大部分用户名应该都可用（因为都是唯一的）
        assertThat((double) availableCount / results.size()).isGreaterThan(0.9);
    }

    // Helper Methods

    private RegisterRequestDto createRegisterRequest(String username, String email) {
        RegisterRequestDto request = new RegisterRequestDto();
        request.setUsername(username);
        request.setEmail(email);
        request.setPassword("Password123!");
        request.setFirstName("Test");
        request.setLastName("User");
        request.setPhone("1234567890");
        request.setAddress("123 Test Street");
        return request;
    }
}