/**
 * Security Utilities
 * 
 * Function Description:
 * - Provides security-related utility methods
 * - Password generation, validation, encryption functions
 * - Utility class to ensure system security
 * 
 * Main Functions:
 * - Generate secure random passwords
 * - Password strength validation
 * - Encryption utility methods
 * 
 * Security Features:
 * - Uses SecureRandom to generate random numbers
 * - Complies with industry-standard password policies
 * - Prevents predictability attacks
 * 
 *
 
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
    
    // Password character set definition
    private static final String UPPERCASE_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final String LOWERCASE_CHARS = "abcdefghijklmnopqrstuvwxyz";
    private static final String DIGIT_CHARS = "0123456789";
    private static final String SPECIAL_CHARS = "!@#$%&*";
    private static final String ALL_CHARS = UPPERCASE_CHARS + LOWERCASE_CHARS + DIGIT_CHARS + SPECIAL_CHARS;
    
    /**
     * Generate a secure temporary password
     * 
     * Features:
     * - 12 characters long, containing uppercase letters, lowercase letters, digits, and special characters
     * - Uses SecureRandom to ensure randomness
     * - Complies with password security policies
     * 
     * @return Secure temporary password (plaintext)
     */
    public static String generateSecureTemporaryPassword() {
        StringBuilder password = new StringBuilder(12);
        
        // Ensure at least one character of each type
        password.append(UPPERCASE_CHARS.charAt(secureRandom.nextInt(UPPERCASE_CHARS.length())));
        password.append(LOWERCASE_CHARS.charAt(secureRandom.nextInt(LOWERCASE_CHARS.length())));
        password.append(DIGIT_CHARS.charAt(secureRandom.nextInt(DIGIT_CHARS.length())));
        password.append(SPECIAL_CHARS.charAt(secureRandom.nextInt(SPECIAL_CHARS.length())));
        
        // Fill remaining positions
        for (int i = 4; i < 12; i++) {
            password.append(ALL_CHARS.charAt(secureRandom.nextInt(ALL_CHARS.length())));
        }
        
        // Shuffle character order
        return shuffleString(password.toString());
    }
    
    /**
     * Generate a secure token for password reset
     * 
     * @return Base64 encoded secure token
     */
    public static String generateSecureToken() {
        byte[] tokenBytes = new byte[32];
        secureRandom.nextBytes(tokenBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(tokenBytes);
    }
    
    /**
     * Encrypt password
     * 
     * @param plainPassword Plaintext password
     * @return BCrypt encrypted password
     */
    public static String encodePassword(String plainPassword) {
        return passwordEncoder.encode(plainPassword);
    }
    
    /**
     * Verify password
     * 
     * @param plainPassword Plaintext password
     * @param encodedPassword Encrypted password
     * @return Whether passwords match
     */
    public static boolean matches(String plainPassword, String encodedPassword) {
        return passwordEncoder.matches(plainPassword, encodedPassword);
    }
    
    /**
     * Verify password strength
     * 
     * @param password Password to be verified
     * @return Whether password meets security requirements
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
     * Shuffle string order
     * 
     * @param input Input string
     * @return String with shuffled order
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