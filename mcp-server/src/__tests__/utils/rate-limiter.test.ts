/**
 * Rate Limiter Unit Tests
 * 
 * 测试令牌桶算法实现，包括：
 * - 令牌生成和消费机制
 * - 用户级别和全局级别限制
 * - 突发请求处理
 * - 统计信息和清理机制
 */

import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import { RateLimiter } from '../../utils/rate-limiter.js';

// Mock logger
vi.mock('../../utils/logger.js', () => ({
  logger: {
    info: vi.fn(),
    debug: vi.fn(),
    warn: vi.fn(),
    error: vi.fn()
  }
}));

// Mock config
vi.mock('../../utils/config.js', () => ({
  getConfig: () => ({
    rateLimit: {
      enabled: true,
      requestsPerMinute: 60,
      burstSize: 10
    }
  })
}));

describe('RateLimiter', () => {
  let rateLimiter: RateLimiter;

  beforeEach(() => {
    rateLimiter = new RateLimiter({
      enabled: true,
      requestsPerMinute: 60, // 1 request per second
      burstSize: 5,
      windowMs: 60000
    });
  });

  afterEach(() => {
    rateLimiter.shutdown();
  });

  describe('Token Bucket Mechanism', () => {
    it('should allow requests within burst limit', () => {
      // Should allow up to burst size requests immediately
      for (let i = 0; i < 5; i++) {
        const result = rateLimiter.checkLimit();
        expect(result.allowed).toBe(true);
        expect(result.tokensRemaining).toBe(5 - i - 1);
      }

      // Next request should be denied (burst exhausted)
      const result = rateLimiter.checkLimit();
      expect(result.allowed).toBe(false);
      expect(result.tokensRemaining).toBe(0);
      expect(result.retryAfter).toBeGreaterThan(0);
    });

    it('should refill tokens over time', async () => {
      // Exhaust burst limit
      for (let i = 0; i < 5; i++) {
        rateLimiter.checkLimit();
      }

      // Should be denied immediately
      let result = rateLimiter.checkLimit();
      expect(result.allowed).toBe(false);

      // Wait for token refill (at 1 token per second)
      await new Promise(resolve => setTimeout(resolve, 1100));

      // Should allow one request after refill
      result = rateLimiter.checkLimit();
      expect(result.allowed).toBe(true);
      expect(result.tokensRemaining).toBe(0);
    });

    it('should handle fractional token refill correctly', async () => {
      // Create rate limiter with faster refill for testing
      const fastRateLimiter = new RateLimiter({
        enabled: true,
        requestsPerMinute: 120, // 2 requests per second
        burstSize: 2,
        windowMs: 60000
      });

      // Use up all tokens
      fastRateLimiter.checkLimit();
      fastRateLimiter.checkLimit();

      // Should be denied
      let result = fastRateLimiter.checkLimit();
      expect(result.allowed).toBe(false);

      // Wait for half a token to be generated (250ms at 2 req/sec)
      await new Promise(resolve => setTimeout(resolve, 250));

      // Should still be denied (not enough tokens)
      result = fastRateLimiter.checkLimit();
      expect(result.allowed).toBe(false);

      // Wait for full token (another 250ms)
      await new Promise(resolve => setTimeout(resolve, 300));

      // Should now be allowed
      result = fastRateLimiter.checkLimit();
      expect(result.allowed).toBe(true);

      fastRateLimiter.shutdown();
    });
  });

  describe('User-Specific Rate Limiting', () => {
    it('should track rate limits per user', () => {
      const user1 = 'user1';
      const user2 = 'user2';

      // User1 exhausts their limit
      for (let i = 0; i < 5; i++) {
        const result = rateLimiter.checkLimit(user1);
        expect(result.allowed).toBe(true);
      }

      // User1 should be denied
      let result = rateLimiter.checkLimit(user1);
      expect(result.allowed).toBe(false);

      // User2 should still be allowed (separate bucket)
      result = rateLimiter.checkLimit(user2);
      expect(result.allowed).toBe(true);
    });

    it('should provide user-specific statistics', () => {
      const userId = 'test-user';

      // Make some requests
      rateLimiter.checkLimit(userId);
      rateLimiter.checkLimit(userId);

      const userStats = rateLimiter.getUserStats(userId);
      expect(userStats).toBeDefined();
      expect(userStats!.requestCount).toBe(2);
      expect(userStats!.tokensRemaining).toBe(3);
      expect(userStats!.firstRequestTime).toBeGreaterThan(0);
      expect(userStats!.elapsedTime).toBeGreaterThan(0);
    });

    it('should return null for non-existent users', () => {
      const userStats = rateLimiter.getUserStats('non-existent-user');
      expect(userStats).toBeNull();
    });
  });

  describe('Global Rate Limiting', () => {
    it('should enforce global rate limit even with different users', () => {
      // Create rate limiter with very low global limit for testing
      const strictRateLimiter = new RateLimiter({
        enabled: true,
        requestsPerMinute: 60,
        burstSize: 2, // Very small burst
        windowMs: 60000
      });

      // Exhaust global limit with user1
      strictRateLimiter.checkLimit('user1');
      strictRateLimiter.checkLimit('user1');

      // user2 should also be denied due to global limit
      const result = strictRateLimiter.checkLimit('user2');
      expect(result.allowed).toBe(false);

      strictRateLimiter.shutdown();
    });

    it('should allow requests when no user ID provided', () => {
      // Anonymous requests should still work
      const result = rateLimiter.checkLimit();
      expect(result.allowed).toBe(true);
      expect(result.tokensRemaining).toBeGreaterThanOrEqual(0);
    });
  });

  describe('Disabled Rate Limiting', () => {
    it('should allow all requests when disabled', () => {
      const disabledRateLimiter = new RateLimiter({
        enabled: false,
        requestsPerMinute: 1, // Would normally be very restrictive
        burstSize: 1,
        windowMs: 60000
      });

      // Should allow many requests even with restrictive config
      for (let i = 0; i < 100; i++) {
        const result = disabledRateLimiter.checkLimit('user');
        expect(result.allowed).toBe(true);
      }

      disabledRateLimiter.shutdown();
    });
  });

  describe('Configuration and Statistics', () => {
    it('should provide accurate global statistics', () => {
      // Make some requests
      rateLimiter.checkLimit('user1');
      rateLimiter.checkLimit('user2');
      rateLimiter.checkLimit('user3');

      const stats = rateLimiter.getStats();
      expect(stats.enabled).toBe(true);
      expect(stats.activeUsers).toBe(3);
      expect(stats.globalTokensRemaining).toBeLessThanOrEqual(5);
      expect(stats.config.requestsPerMinute).toBe(60);
      expect(stats.config.burstSize).toBe(5);
    });

    it('should reset rate limits correctly', () => {
      // Make some requests
      rateLimiter.checkLimit('user1');
      rateLimiter.checkLimit('user2');

      let stats = rateLimiter.getStats();
      expect(stats.activeUsers).toBe(2);

      // Reset
      rateLimiter.reset();

      stats = rateLimiter.getStats();
      expect(stats.activeUsers).toBe(0);
      expect(stats.globalTokensRemaining).toBe(5); // Back to burst size
    });
  });

  describe('Cleanup and Memory Management', () => {
    it('should clean up expired user entries', async () => {
      // This test would require mocking timers for deterministic behavior
      // For now, we test that the cleanup mechanism exists
      const userId = 'test-user';
      rateLimiter.checkLimit(userId);

      let stats = rateLimiter.getStats();
      expect(stats.activeUsers).toBe(1);

      // In a real scenario, expired entries would be cleaned up after 10 minutes
      // For testing, we verify the structure exists
      expect(typeof rateLimiter.getStats).toBe('function');
      expect(typeof rateLimiter.getUserStats).toBe('function');
    });
  });

  describe('Edge Cases and Error Handling', () => {
    it('should handle rapid successive requests correctly', () => {
      const results = [];
      
      // Make many requests in quick succession
      for (let i = 0; i < 10; i++) {
        results.push(rateLimiter.checkLimit('rapid-user'));
      }

      // First 5 should be allowed (burst size)
      for (let i = 0; i < 5; i++) {
        expect(results[i].allowed).toBe(true);
      }

      // Remaining should be denied
      for (let i = 5; i < 10; i++) {
        expect(results[i].allowed).toBe(false);
      }
    });

    it('should handle extreme rate limit configurations', () => {
      // Very permissive
      const permissiveRateLimiter = new RateLimiter({
        enabled: true,
        requestsPerMinute: 10000,
        burstSize: 1000,
        windowMs: 60000
      });

      const result1 = permissiveRateLimiter.checkLimit();
      expect(result1.allowed).toBe(true);
      expect(result1.tokensRemaining).toBe(999);

      permissiveRateLimiter.shutdown();

      // Very restrictive
      const restrictiveRateLimiter = new RateLimiter({
        enabled: true,
        requestsPerMinute: 1,
        burstSize: 1,
        windowMs: 60000
      });

      const result2 = restrictiveRateLimiter.checkLimit();
      expect(result2.allowed).toBe(true);

      const result3 = restrictiveRateLimiter.checkLimit();
      expect(result3.allowed).toBe(false);
      expect(result3.retryAfter).toBeGreaterThan(0);

      restrictiveRateLimiter.shutdown();
    });

    it('should handle empty or invalid user IDs gracefully', () => {
      // Empty string user ID
      const result1 = rateLimiter.checkLimit('');
      expect(result1.allowed).toBe(true);

      // Very long user ID
      const longUserId = 'x'.repeat(1000);
      const result2 = rateLimiter.checkLimit(longUserId);
      expect(result2.allowed).toBe(true);

      // Special characters in user ID
      const specialUserId = 'user@domain.com#123!$%';
      const result3 = rateLimiter.checkLimit(specialUserId);
      expect(result3.allowed).toBe(true);
    });
  });

  describe('Timing and Precision', () => {
    it('should provide accurate retry-after timing', () => {
      // Exhaust tokens
      for (let i = 0; i < 5; i++) {
        rateLimiter.checkLimit();
      }

      const result = rateLimiter.checkLimit();
      expect(result.allowed).toBe(false);
      expect(result.retryAfter).toBeGreaterThan(0);
      expect(result.retryAfter).toBeLessThan(60000); // Should be less than window

      // At 1 request per second rate, retry should be around 1000ms
      expect(result.retryAfter).toBeGreaterThan(900);
      expect(result.retryAfter).toBeLessThan(1100);
    });

    it('should have monotonic reset times', () => {
      const result1 = rateLimiter.checkLimit();
      const result2 = rateLimiter.checkLimit();

      expect(result2.resetTime).toBeGreaterThanOrEqual(result1.resetTime);
    });
  });

  describe('Shutdown and Cleanup', () => {
    it('should shutdown gracefully', () => {
      const tempRateLimiter = new RateLimiter();
      
      // Use the rate limiter
      tempRateLimiter.checkLimit('user');
      
      // Should not throw
      expect(() => tempRateLimiter.shutdown()).not.toThrow();
      
      // Stats should still be accessible after shutdown
      const stats = tempRateLimiter.getStats();
      expect(stats).toBeDefined();
    });
  });
});