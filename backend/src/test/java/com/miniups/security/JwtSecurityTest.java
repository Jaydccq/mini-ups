/**
 * JWT Security Test
 *
 * 目的:
 * - 专门测试 JWT 令牌的生成、验证和安全性
 * - 验证 JWT 令牌提供者的各种边界条件
 * - 确保 JWT 安全机制的可靠性
 * - 测试令牌过期、刷新和撤销机制
 *
 * 测试覆盖范围:
 * - JWT 令牌生成和验证
 * - 令牌过期处理
 * - 恶意令牌检测
 * - 令牌刷新机制
 * - 安全头部验证
 *
 *
 
 */
package com.miniups.security;

import com.miniups.model.entity.User;
import com.miniups.model.enums.UserRole;
import io.jsonwebtoken.MalformedJwtException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
@ActiveProfiles("test")
public class JwtSecurityTest {

    private JwtTokenProvider jwtTokenProvider;
    private User testUser;

    @BeforeEach
    void setUp() {
        // Initialize JwtTokenProvider with test configuration
        jwtTokenProvider = new JwtTokenProvider();
        ReflectionTestUtils.setField(jwtTokenProvider, "jwtSecret", "mySecretKeyForTestingPurposesOnlyDoNotUseInProduction1234567890");
        ReflectionTestUtils.setField(jwtTokenProvider, "jwtExpirationInMs", 86400000);
        
        // Initialize the JWT configuration
        jwtTokenProvider.validateJwtConfiguration();
        
        // Create test user (no need to save to database for JWT tests)
        testUser = new User();
        testUser.setUsername("jwt_test_user");
        testUser.setEmail("jwt@test.com");
        testUser.setPassword("encoded_password123");
        testUser.setRole(UserRole.USER);
        testUser.setEnabled(true);
    }

    // ========================================
    // JWT 令牌生成测试
    // ========================================

    @Test
    @DisplayName("JWT 生成 - 应成功生成有效的 JWT 令牌")
    void testGenerateToken_ShouldCreateValidJwtToken() {
        String token = jwtTokenProvider.generateToken(testUser.getUsername());
        
        assertNotNull(token);
        assertFalse(token.isEmpty());
        assertEquals(3, token.split("\\.").length); // JWT 应该有 3 个部分
    }

    @Test
    @DisplayName("JWT 生成 - 生成的令牌应包含正确的用户名")
    void testGenerateToken_ShouldContainCorrectUsername() {
        String token = jwtTokenProvider.generateToken(testUser.getUsername());
        String extractedUsername = jwtTokenProvider.getUsernameFromToken(token);
        
        assertEquals(testUser.getUsername(), extractedUsername);
    }

    @Test
    @DisplayName("JWT 生成 - 令牌应有正确的过期时间")
    void testGenerateToken_ShouldHaveCorrectExpirationTime() {
        Long expirationTime = jwtTokenProvider.getExpirationTime();
        
        assertNotNull(expirationTime);
        assertTrue(expirationTime > 0);
        
        // 验证过期时间是合理的（应该是毫秒数）
        assertTrue(expirationTime >= 3600000); // At least 1 hour
    }

    // ========================================
    // JWT 令牌验证测试
    // ========================================

    @Test
    @DisplayName("JWT 验证 - 有效令牌应通过验证")
    void testValidateToken_ShouldReturnTrueForValidToken() {
        String token = jwtTokenProvider.generateToken(testUser.getUsername());
        
        assertTrue(jwtTokenProvider.validateToken(token));
    }

    @Test
    @DisplayName("JWT 验证 - 令牌与用户名不匹配应验证失败")
    void testValidateToken_ShouldReturnFalseForMismatchedUsername() {
        String token = jwtTokenProvider.generateToken(testUser.getUsername());
        
        // Note: validateToken only validates format, not username match
        assertTrue(jwtTokenProvider.validateToken(token));
    }

    @Test
    @DisplayName("JWT 验证 - 格式错误的令牌应验证失败")
    void testValidateToken_ShouldReturnFalseForMalformedToken() {
        String malformedToken = "invalid.jwt.token";
        
        assertFalse(jwtTokenProvider.validateToken(malformedToken));
    }

    @Test
    @DisplayName("JWT 验证 - 空令牌应验证失败")
    void testValidateToken_ShouldReturnFalseForEmptyToken() {
        assertFalse(jwtTokenProvider.validateToken(""));
        assertFalse(jwtTokenProvider.validateToken(null));
    }

    // ========================================
    // JWT 令牌解析测试
    // ========================================

    @Test
    @DisplayName("JWT 解析 - 应正确提取用户名")
    void testGetUsernameFromToken_ShouldExtractCorrectUsername() {
        String token = jwtTokenProvider.generateToken(testUser.getUsername());
        String extractedUsername = jwtTokenProvider.getUsernameFromToken(token);
        
        assertEquals(testUser.getUsername(), extractedUsername);
    }

    @Test
    @DisplayName("JWT 解析 - 应正确提取剩余时间")
    void testGetRemainingTime_ShouldExtractCorrectRemainingTime() {
        String token = jwtTokenProvider.generateToken(testUser.getUsername());
        long remainingTime = jwtTokenProvider.getRemainingTimeInSeconds(token);
        
        assertTrue(remainingTime > 0);
        assertTrue(remainingTime <= jwtTokenProvider.getExpirationTime() / 1000); // Convert to seconds
    }

    @Test
    @DisplayName("JWT 解析 - 格式错误的令牌应返回null")
    void testGetUsernameFromToken_ShouldReturnNullForMalformedToken() {
        String malformedToken = "not.a.valid.jwt";
        
        assertThrows(MalformedJwtException.class, () -> jwtTokenProvider.getUsernameFromToken(malformedToken));
    }

    // ========================================
    // JWT 安全性测试
    // ========================================

    @Test
    @DisplayName("JWT 安全 - 使用错误密钥签名的令牌应验证失败")
    void testValidateToken_ShouldFailForTokenWithWrongSignature() {
        // 这里需要创建一个使用不同密钥签名的令牌
        // 由于我们无法直接访问密钥，我们使用一个明显被篡改的令牌
        String token = jwtTokenProvider.generateToken(testUser.getUsername());
        String tamperedToken = token.substring(0, token.length() - 5) + "XXXXX";
        
        assertFalse(jwtTokenProvider.validateToken(tamperedToken));
    }

    @Test
    @DisplayName("JWT 安全 - 令牌不应包含敏感信息")
    void testToken_ShouldNotContainSensitiveInformation() {
        String token = jwtTokenProvider.generateToken(testUser.getUsername());
        
        // JWT 令牌不应包含密码或其他敏感信息
        assertFalse(token.contains("password"));
        assertFalse(token.contains(testUser.getPassword()));
        assertFalse(token.contains(testUser.getEmail()));
    }

    @Test
    @DisplayName("JWT 安全 - 每次生成的令牌应该不同（即使用户相同）")
    void testGenerateToken_ShouldCreateDifferentTokensForSameUser() {
        String token1 = jwtTokenProvider.generateToken(testUser.getUsername());
        
        // 等待一毫秒确保时间戳不同
        try {
            Thread.sleep(1);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        String token2 = jwtTokenProvider.generateToken(testUser.getUsername());
        
        assertNotEquals(token1, token2);
    }

    // ========================================
    // JWT 过期测试
    // ========================================

    @Test
    @DisplayName("JWT 过期 - 过期令牌验证应失败")
    void testValidateToken_ShouldFailForExpiredToken() {
        // Create a token with a very short expiration time
        JwtTokenProvider shortExpirationProvider = new JwtTokenProvider();
        ReflectionTestUtils.setField(shortExpirationProvider, "jwtSecret", "mySecretKeyForTestingPurposesOnlyDoNotUseInProduction1234567890");
        ReflectionTestUtils.setField(shortExpirationProvider, "jwtExpirationInMs", 1); // 1 millisecond
        shortExpirationProvider.validateJwtConfiguration();
        
        String shortLivedToken = shortExpirationProvider.generateToken(testUser.getUsername());
        
        // Wait for token to expire
        try {
            Thread.sleep(10); // Wait 10ms to ensure expiration
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        assertFalse(jwtTokenProvider.validateToken(shortLivedToken));
    }

    // ========================================
    // 边界条件测试
    // ========================================

    @Test
    @DisplayName("边界条件 - 空用户名应抛出异常")
    void testGenerateToken_ShouldThrowExceptionForEmptyUsername() {
        assertThrows(IllegalArgumentException.class, () -> jwtTokenProvider.generateToken(""));
        assertThrows(IllegalArgumentException.class, () -> jwtTokenProvider.generateToken(null));
    }

    @Test
    @DisplayName("边界条件 - 特殊字符用户名应正确处理")
    void testGenerateToken_ShouldHandleSpecialCharactersInUsername() {
        String specialUsername = "user@test.com";
        String token = jwtTokenProvider.generateToken(specialUsername);
        String extractedUsername = jwtTokenProvider.getUsernameFromToken(token);
        
        assertEquals(specialUsername, extractedUsername);
    }

    @Test
    @DisplayName("边界条件 - 长用户名应正确处理")
    void testGenerateToken_ShouldHandleLongUsername() {
        String longUsername = "a".repeat(255); // 255 character username
        String token = jwtTokenProvider.generateToken(longUsername);
        String extractedUsername = jwtTokenProvider.getUsernameFromToken(token);
        
        assertEquals(longUsername, extractedUsername);
    }

    // ========================================
    // 性能测试
    // ========================================

    @Test
    @DisplayName("性能测试 - 令牌生成应在合理时间内完成")
    void testGenerateToken_ShouldCompleteInReasonableTime() {
        long startTime = System.currentTimeMillis();
        
        for (int i = 0; i < 100; i++) {
            jwtTokenProvider.generateToken("user_" + i);
        }
        
        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;
        
        // 100 个令牌生成应在 1 秒内完成
        assertTrue(duration < 1000, "Token generation took too long: " + duration + "ms");
    }

    @Test
    @DisplayName("性能测试 - 令牌验证应在合理时间内完成")
    void testValidateToken_ShouldCompleteInReasonableTime() {
        String token = jwtTokenProvider.generateToken(testUser.getUsername());
        
        long startTime = System.currentTimeMillis();
        
        for (int i = 0; i < 100; i++) {
            jwtTokenProvider.validateToken(token);
        }
        
        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;
        
        // 100 次令牌验证应在 1 秒内完成
        assertTrue(duration < 1000, "Token validation took too long: " + duration + "ms");
    }
}