/**
 * Security Utilities
 * 
 * 功能说明：
 * - 提供安全相关的工具方法
 * - 密码生成、验证、加密等功能
 * - 确保系统安全性的工具类
 * 
 * 主要功能：
 * - 生成安全随机密码
 * - 密码强度验证
 * - 加密工具方法
 * 
 * 安全特性：
 * - 使用 SecureRandom 生成随机数
 * - 符合行业标准的密码策略
 * - 防止可预测性攻击
 * 
 * @author Mini-UPS Team
 * @version 1.0.0
 */
package com.miniups.util;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.util.Base64;

@Component
public class SecurityUtils {
    
    private static final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    private static final SecureRandom secureRandom = new SecureRandom();
    
    // 密码字符集定义
    private static final String UPPERCASE_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final String LOWERCASE_CHARS = "abcdefghijklmnopqrstuvwxyz";
    private static final String DIGIT_CHARS = "0123456789";
    private static final String SPECIAL_CHARS = "!@#$%&*";
    private static final String ALL_CHARS = UPPERCASE_CHARS + LOWERCASE_CHARS + DIGIT_CHARS + SPECIAL_CHARS;
    
    /**
     * 生成安全的临时密码
     * 
     * 特性:
     * - 12位长度，包含大小写字母、数字、特殊字符
     * - 使用 SecureRandom 确保随机性
     * - 符合密码安全策略
     * 
     * @return 安全的临时密码（明文）
     */
    public static String generateSecureTemporaryPassword() {
        StringBuilder password = new StringBuilder(12);
        
        // 确保至少包含每种字符类型
        password.append(UPPERCASE_CHARS.charAt(secureRandom.nextInt(UPPERCASE_CHARS.length())));
        password.append(LOWERCASE_CHARS.charAt(secureRandom.nextInt(LOWERCASE_CHARS.length())));
        password.append(DIGIT_CHARS.charAt(secureRandom.nextInt(DIGIT_CHARS.length())));
        password.append(SPECIAL_CHARS.charAt(secureRandom.nextInt(SPECIAL_CHARS.length())));
        
        // 填充剩余位置
        for (int i = 4; i < 12; i++) {
            password.append(ALL_CHARS.charAt(secureRandom.nextInt(ALL_CHARS.length())));
        }
        
        // 打乱字符顺序
        return shuffleString(password.toString());
    }
    
    /**
     * 生成用于密码重置的安全令牌
     * 
     * @return Base64编码的安全令牌
     */
    public static String generateSecureToken() {
        byte[] tokenBytes = new byte[32];
        secureRandom.nextBytes(tokenBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(tokenBytes);
    }
    
    /**
     * 加密密码
     * 
     * @param plainPassword 明文密码
     * @return BCrypt加密后的密码
     */
    public static String encodePassword(String plainPassword) {
        return passwordEncoder.encode(plainPassword);
    }
    
    /**
     * 验证密码
     * 
     * @param plainPassword 明文密码
     * @param encodedPassword 加密后的密码
     * @return 密码是否匹配
     */
    public static boolean matches(String plainPassword, String encodedPassword) {
        return passwordEncoder.matches(plainPassword, encodedPassword);
    }
    
    /**
     * 验证密码强度
     * 
     * @param password 待验证的密码
     * @return 密码是否符合安全要求
     */
    public static boolean isPasswordStrong(String password) {
        if (password == null || password.length() < 8) {
            return false;
        }
        
        boolean hasUpper = password.chars().anyMatch(Character::isUpperCase);
        boolean hasLower = password.chars().anyMatch(Character::isLowerCase);
        boolean hasDigit = password.chars().anyMatch(Character::isDigit);
        boolean hasSpecial = password.chars().anyMatch(ch -> "!@#$%&*".indexOf(ch) >= 0);
        
        return hasUpper && hasLower && hasDigit && hasSpecial;
    }
    
    /**
     * 打乱字符串顺序
     * 
     * @param input 输入字符串
     * @return 打乱顺序后的字符串
     */
    private static String shuffleString(String input) {
        char[] chars = input.toCharArray();
        for (int i = chars.length - 1; i > 0; i--) {
            int j = secureRandom.nextInt(i + 1);
            char temp = chars[i];
            chars[i] = chars[j];
            chars[j] = temp;
        }
        return new String(chars);
    }
}