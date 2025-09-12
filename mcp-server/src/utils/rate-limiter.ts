/**
 * Rate Limiter - Token Bucket实现
 * 
 * 提供基于令牌桶算法的速率限制功能，支持：
 * - 每分钟请求数限制
 * - 突发请求处理
 * - 用户级别和全局级别限制
 * - 实时统计和监控
 */

import { logger } from './logger.js';
import { getConfig } from './config.js';

// ===== Types =====
export interface RateLimitConfig {
  enabled: boolean;
  requestsPerMinute: number;
  burstSize: number;
  windowMs: number;
}

export interface RateLimitResult {
  allowed: boolean;
  tokensRemaining: number;
  resetTime: number;
  retryAfter?: number;
}

interface TokenBucket {
  tokens: number;
  lastRefill: number;
  capacity: number;
  refillRate: number; // tokens per millisecond
}

interface RateLimitEntry {
  bucket: TokenBucket;
  requestCount: number;
  firstRequestTime: number;
}

// ===== Rate Limiter Implementation =====
export class RateLimiter {
  private config: RateLimitConfig;
  private userBuckets = new Map<string, RateLimitEntry>();
  private globalBucket: TokenBucket;
  private cleanupInterval: NodeJS.Timeout;

  constructor(config?: Partial<RateLimitConfig>) {
    const appConfig = getConfig();
    
    this.config = {
      enabled: config?.enabled ?? appConfig.rateLimit.enabled,
      requestsPerMinute: config?.requestsPerMinute ?? appConfig.rateLimit.requestsPerMinute,
      burstSize: config?.burstSize ?? appConfig.rateLimit.burstSize,
      windowMs: config?.windowMs ?? 60000 // 1 minute
    };

    // Initialize global bucket
    this.globalBucket = this.createTokenBucket(
      this.config.burstSize,
      this.config.requestsPerMinute / 60000 // convert to tokens per ms
    );

    // Cleanup expired entries every 5 minutes
    this.cleanupInterval = setInterval(() => {
      this.cleanupExpiredEntries();
    }, 5 * 60 * 1000);

    logger.info('Rate limiter initialized', {
      enabled: this.config.enabled,
      requestsPerMinute: this.config.requestsPerMinute,
      burstSize: this.config.burstSize
    });
  }

  /**
   * Check if request is allowed for a specific user
   */
  checkLimit(userId?: string): RateLimitResult {
    if (!this.config.enabled) {
      return {
        allowed: true,
        tokensRemaining: this.config.burstSize,
        resetTime: Date.now() + this.config.windowMs
      };
    }

    const now = Date.now();

    // Check global rate limit first
    const globalResult = this.checkTokenBucket(this.globalBucket, now);
    if (!globalResult.allowed) {
      logger.warn('Global rate limit exceeded');
      return globalResult;
    }

    // Check user-specific rate limit if userId provided
    if (userId) {
      const userResult = this.checkUserLimit(userId, now);
      if (!userResult.allowed) {
        logger.warn('User rate limit exceeded', { userId });
        return userResult;
      }
      
      // Both global and user limits passed, consume tokens
      this.consumeToken(this.globalBucket, now);
      this.consumeUserToken(userId, now);
      
      return userResult;
    }

    // Only global limit, consume token
    this.consumeToken(this.globalBucket, now);
    return globalResult;
  }

  /**
   * Check user-specific rate limit
   */
  private checkUserLimit(userId: string, now: number): RateLimitResult {
    let entry = this.userBuckets.get(userId);
    
    if (!entry) {
      // Create new entry for user
      entry = {
        bucket: this.createTokenBucket(
          this.config.burstSize,
          this.config.requestsPerMinute / 60000
        ),
        requestCount: 0,
        firstRequestTime: now
      };
      this.userBuckets.set(userId, entry);
    }

    return this.checkTokenBucket(entry.bucket, now);
  }

  /**
   * Consume token for user
   */
  private consumeUserToken(userId: string, now: number): void {
    const entry = this.userBuckets.get(userId);
    if (entry) {
      this.consumeToken(entry.bucket, now);
      entry.requestCount++;
    }
  }

  /**
   * Check token bucket and return result
   */
  private checkTokenBucket(bucket: TokenBucket, now: number): RateLimitResult {
    this.refillBucket(bucket, now);

    const allowed = bucket.tokens >= 1;
    const resetTime = now + (this.config.windowMs - (now % this.config.windowMs));
    
    if (!allowed) {
      const tokensNeeded = 1 - bucket.tokens;
      const timeToNextToken = tokensNeeded / bucket.refillRate;
      
      return {
        allowed: false,
        tokensRemaining: Math.floor(bucket.tokens),
        resetTime,
        retryAfter: Math.ceil(timeToNextToken)
      };
    }

    return {
      allowed: true,
      tokensRemaining: Math.floor(bucket.tokens) - 1, // After consuming one token
      resetTime
    };
  }

  /**
   * Consume a token from bucket
   */
  private consumeToken(bucket: TokenBucket, now: number): void {
    this.refillBucket(bucket, now);
    if (bucket.tokens >= 1) {
      bucket.tokens -= 1;
    }
  }

  /**
   * Refill bucket based on elapsed time
   */
  private refillBucket(bucket: TokenBucket, now: number): void {
    const elapsed = now - bucket.lastRefill;
    const tokensToAdd = elapsed * bucket.refillRate;
    
    bucket.tokens = Math.min(bucket.capacity, bucket.tokens + tokensToAdd);
    bucket.lastRefill = now;
  }

  /**
   * Create new token bucket
   */
  private createTokenBucket(capacity: number, refillRate: number): TokenBucket {
    return {
      tokens: capacity,
      lastRefill: Date.now(),
      capacity,
      refillRate
    };
  }

  /**
   * Clean up expired user entries
   */
  private cleanupExpiredEntries(): void {
    const now = Date.now();
    const expireThreshold = 10 * 60 * 1000; // 10 minutes
    let cleanedCount = 0;

    for (const [userId, entry] of this.userBuckets.entries()) {
      if (now - entry.firstRequestTime > expireThreshold) {
        this.userBuckets.delete(userId);
        cleanedCount++;
      }
    }

    if (cleanedCount > 0) {
      logger.debug('Cleaned up expired rate limit entries', { cleanedCount });
    }
  }

  /**
   * Get rate limit statistics
   */
  getStats() {
    return {
      enabled: this.config.enabled,
      activeUsers: this.userBuckets.size,
      globalTokensRemaining: Math.floor(this.globalBucket.tokens),
      config: {
        requestsPerMinute: this.config.requestsPerMinute,
        burstSize: this.config.burstSize
      }
    };
  }

  /**
   * Get user-specific stats
   */
  getUserStats(userId: string) {
    const entry = this.userBuckets.get(userId);
    if (!entry) {
      return null;
    }

    const now = Date.now();
    this.refillBucket(entry.bucket, now);

    return {
      tokensRemaining: Math.floor(entry.bucket.tokens),
      requestCount: entry.requestCount,
      firstRequestTime: entry.firstRequestTime,
      elapsedTime: now - entry.firstRequestTime
    };
  }

  /**
   * Reset rate limits for testing
   */
  reset(): void {
    this.userBuckets.clear();
    this.globalBucket = this.createTokenBucket(
      this.config.burstSize,
      this.config.requestsPerMinute / 60000
    );
    logger.debug('Rate limiter reset');
  }

  /**
   * Shutdown rate limiter
   */
  shutdown(): void {
    if (this.cleanupInterval) {
      clearInterval(this.cleanupInterval);
    }
    this.userBuckets.clear();
    logger.info('Rate limiter shutdown');
  }
}

// Export singleton instance
export const rateLimiter = new RateLimiter();