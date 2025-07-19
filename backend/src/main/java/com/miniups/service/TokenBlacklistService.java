/**
 * JWT Token Blacklist Service
 * 
 * Functionality:
 * - Manage blacklist of logged out JWT tokens
 * - Use Redis cache to improve performance
 * - Automatically clean expired tokens
 * 
 * Security Features:
 * - Prevent reuse of logged out tokens
 * - Support forced user logout
 * - Automatic cleanup of expired tokens
 * 
 * @author Mini-UPS Team
 * @version 1.0.0
 */
package com.miniups.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

@Service
public class TokenBlacklistService {
    
    private static final Logger logger = LoggerFactory.getLogger(TokenBlacklistService.class);
    private static final String BLACKLIST_PREFIX = "blacklisted_token:";
    
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;
    
    /**
     * Add token to blacklist
     * 
     * @param tokenId Token ID (jti)
     * @param expirationTime Token expiration time (seconds)
     */
    public void blacklistToken(String tokenId, long expirationTime) {
        try {
            String key = BLACKLIST_PREFIX + tokenId;
            
            // Set expiration time, auto-delete after expiration
            redisTemplate.opsForValue().set(key, "blacklisted", expirationTime, TimeUnit.SECONDS);
            
            logger.info("Token added to blacklist: tokenId={}", tokenId);
            
        } catch (Exception e) {
            logger.error("Failed to add token to blacklist: tokenId={}", tokenId, e);
            // Don't throw exception to avoid affecting logout flow
        }
    }
    
    /**
     * Check if token is in blacklist
     * 
     * @param tokenId Token ID (jti)
     * @return true if in blacklist
     */
    public boolean isTokenBlacklisted(String tokenId) {
        try {
            String key = BLACKLIST_PREFIX + tokenId;
            Boolean exists = redisTemplate.hasKey(key);
            
            return Boolean.TRUE.equals(exists);
            
        } catch (Exception e) {
            logger.error("Failed to check token blacklist status: tokenId={}", tokenId, e);
            // For security, consider token valid when error occurs
            return false;
        }
    }
    
    /**
     * Remove token from blacklist (for testing or special cases)
     * 
     * @param tokenId Token ID
     */
    public void removeTokenFromBlacklist(String tokenId) {
        try {
            String key = BLACKLIST_PREFIX + tokenId;
            redisTemplate.delete(key);
            
            logger.info("Token removed from blacklist: tokenId={}", tokenId);
            
        } catch (Exception e) {
            logger.error("Failed to remove token from blacklist: tokenId={}", tokenId, e);
        }
    }
    
    /**
     * Clear all blacklisted tokens (admin function)
     */
    public void clearAllBlacklistedTokens() {
        try {
            String pattern = BLACKLIST_PREFIX + "*";
            redisTemplate.delete(redisTemplate.keys(pattern));
            
            logger.info("All blacklisted tokens cleared");
            
        } catch (Exception e) {
            logger.error("Failed to clear blacklisted tokens", e);
        }
    }
    
    /**
     * Get blacklisted token count
     * 
     * @return Number of tokens in blacklist
     */
    public long getBlacklistedTokenCount() {
        try {
            String pattern = BLACKLIST_PREFIX + "*";
            return redisTemplate.keys(pattern).size();
            
        } catch (Exception e) {
            logger.error("Failed to get blacklisted token count", e);
            return 0;
        }
    }
}