/**
 * Backend Connector Unit Tests
 * 
 * 测试后端连接器功能，包括：
 * - HTTP请求处理和重试机制
 * - 熔断器模式实现
 * - 响应缓存管理
 * - 错误处理和回退策略
 */

import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import axios from 'axios';
import { BackendConnector } from '../../connectors/backend-connector.js';
import { BackendStep } from '../../schemas/intent-plan.js';

// Mock axios
vi.mock('axios');
const mockAxios = vi.mocked(axios);

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
    backend: {
      baseUrl: 'http://localhost:8081',
      timeout: 5000,
      retries: 3,
      retryDelay: 1000,
      authToken: 'test-token',
      apiKey: 'test-api-key',
      maxConcurrentCalls: 5
    }
  })
}));

describe('BackendConnector', () => {
  let connector: BackendConnector;
  let mockAxiosInstance: any;

  beforeEach(() => {
    // Reset all mocks
    vi.clearAllMocks();
    
    // Mock axios.create to return a mock instance
    mockAxiosInstance = {
      request: vi.fn(),
      interceptors: {
        request: { use: vi.fn() },
        response: { use: vi.fn() }
      }
    };
    mockAxios.create.mockReturnValue(mockAxiosInstance);

    connector = new BackendConnector({
      cache: { enabled: false }, // Disable cache for predictable testing
      circuitBreaker: { enabled: false } // Disable circuit breaker for basic tests
    });
  });

  afterEach(() => {
    connector.clearCache();
  });

  describe('HTTP Request Execution', () => {
    it('should execute successful GET request', async () => {
      const step: BackendStep = {
        endpoint: '/api/tracking/{trackingId}',
        method: 'GET',
        pathParams: { trackingId: '1Z999AA123456789' },
        queryParams: { includeDetails: 'true' },
        description: 'Get tracking status',
        required: true
      };

      const mockResponse = {
        status: 200,
        data: {
          trackingId: '1Z999AA123456789',
          status: 'IN_TRANSIT',
          location: 'Beijing'
        },
        headers: {
          'content-type': 'application/json',
          'x-response-time': '150ms'
        }
      };

      mockAxiosInstance.request.mockResolvedValueOnce(mockResponse);

      const result = await connector.executeStep(step);

      expect(result.success).toBe(true);
      expect(result.data).toEqual(mockResponse.data);
      expect(result.statusCode).toBe(200);
      expect(result.fromCache).toBe(false);
      expect(result.retryCount).toBe(0);
      expect(result.duration).toBeGreaterThan(0);
      
      // Verify correct URL was built
      expect(mockAxiosInstance.request).toHaveBeenCalledWith({
        method: 'GET',
        url: '/api/tracking/1Z999AA123456789',
        params: { includeDetails: 'true' },
        timeout: 5000
      });
    });

    it('should handle path parameter replacement correctly', async () => {
      const step: BackendStep = {
        endpoint: '/api/orders/{orderId}/items/{itemId}',
        method: 'GET',
        pathParams: { 
          orderId: '12345',
          itemId: 'item-abc-123'
        },
        description: 'Get order item details',
        required: true
      };

      mockAxiosInstance.request.mockResolvedValueOnce({
        status: 200,
        data: { itemId: 'item-abc-123', name: 'Product A' }
      });

      await connector.executeStep(step);

      expect(mockAxiosInstance.request).toHaveBeenCalledWith(
        expect.objectContaining({
          url: '/api/orders/12345/items/item-abc-123'
        })
      );
    });

    it('should handle special characters in path parameters', async () => {
      const step: BackendStep = {
        endpoint: '/api/search/{query}',
        method: 'GET',
        pathParams: { query: 'test query with spaces & symbols!' },
        description: 'Search items',
        required: true
      };

      mockAxiosInstance.request.mockResolvedValueOnce({
        status: 200,
        data: { results: [] }
      });

      await connector.executeStep(step);

      expect(mockAxiosInstance.request).toHaveBeenCalledWith(
        expect.objectContaining({
          url: '/api/search/test%20query%20with%20spaces%20%26%20symbols!'
        })
      );
    });
  });

  describe('Retry Logic', () => {
    it('should retry on retryable HTTP errors', async () => {
      const step: BackendStep = {
        endpoint: '/api/orders/12345',
        method: 'GET',
        description: 'Get order details',
        required: true
      };

      // Mock retry scenario: fail twice, then succeed
      mockAxiosInstance.request
        .mockRejectedValueOnce({
          response: { status: 503 },
          message: 'Service Unavailable'
        })
        .mockRejectedValueOnce({
          response: { status: 502 },
          message: 'Bad Gateway'
        })
        .mockResolvedValueOnce({
          status: 200,
          data: { orderId: '12345', status: 'DELIVERED' }
        });

      const result = await connector.executeStep(step);

      expect(result.success).toBe(true);
      expect(result.retryCount).toBe(2);
      expect(result.data.orderId).toBe('12345');
      expect(mockAxiosInstance.request).toHaveBeenCalledTimes(3);
    });

    it('should not retry on non-retryable HTTP errors', async () => {
      const step: BackendStep = {
        endpoint: '/api/orders/nonexistent',
        method: 'GET',
        description: 'Get non-existent order',
        required: true
      };

      mockAxiosInstance.request.mockRejectedValueOnce({
        response: { status: 404 },
        message: 'Not Found'
      });

      const result = await connector.executeStep(step);

      expect(result.success).toBe(false);
      expect(result.retryCount).toBe(0);
      expect(result.statusCode).toBe(404);
      expect(mockAxiosInstance.request).toHaveBeenCalledTimes(1);
    });

    it('should retry on network errors', async () => {
      const step: BackendStep = {
        endpoint: '/api/test',
        method: 'GET',
        description: 'Test endpoint',
        required: true
      };

      // Network error followed by success
      mockAxiosInstance.request
        .mockRejectedValueOnce({
          code: 'ECONNRESET',
          message: 'Connection reset'
        })
        .mockResolvedValueOnce({
          status: 200,
          data: { success: true }
        });

      const result = await connector.executeStep(step);

      expect(result.success).toBe(true);
      expect(result.retryCount).toBe(1);
      expect(mockAxiosInstance.request).toHaveBeenCalledTimes(2);
    });

    it('should fail after max retries exceeded', async () => {
      const step: BackendStep = {
        endpoint: '/api/failing-service',
        method: 'GET',
        description: 'Consistently failing service',
        required: true
      };

      // Always fail with retryable error
      mockAxiosInstance.request.mockRejectedValue({
        response: { status: 503 },
        message: 'Service Unavailable'
      });

      const result = await connector.executeStep(step);

      expect(result.success).toBe(false);
      expect(result.retryCount).toBe(3); // Max retries
      expect(result.error).toContain('Service Unavailable');
      expect(mockAxiosInstance.request).toHaveBeenCalledTimes(4); // Initial + 3 retries
    });
  });

  describe('Parallel Execution', () => {
    it('should execute multiple steps in parallel', async () => {
      const steps: BackendStep[] = [
        {
          endpoint: '/api/orders/1',
          method: 'GET',
          description: 'Get order 1',
          required: true
        },
        {
          endpoint: '/api/orders/2',
          method: 'GET',
          description: 'Get order 2',
          required: true
        },
        {
          endpoint: '/api/orders/3',
          method: 'GET',
          description: 'Get order 3',
          required: false
        }
      ];

      // Mock responses for all three requests
      mockAxiosInstance.request
        .mockResolvedValueOnce({
          status: 200,
          data: { orderId: '1', status: 'DELIVERED' }
        })
        .mockResolvedValueOnce({
          status: 200,
          data: { orderId: '2', status: 'IN_TRANSIT' }
        })
        .mockRejectedValueOnce({
          response: { status: 500 },
          message: 'Internal Server Error'
        });

      const results = await connector.executeStepsParallel(steps);

      expect(results).toHaveLength(3);
      expect(results[0].success).toBe(true);
      expect(results[0].data.orderId).toBe('1');
      expect(results[1].success).toBe(true);
      expect(results[1].data.orderId).toBe('2');
      expect(results[2].success).toBe(false);
      expect(results[2].error).toContain('Internal Server Error');
      
      // All requests should be made
      expect(mockAxiosInstance.request).toHaveBeenCalledTimes(3);
    });

    it('should respect concurrency limits', async () => {
      const steps: BackendStep[] = Array.from({ length: 10 }, (_, i) => ({
        endpoint: `/api/item/${i}`,
        method: 'GET',
        description: `Get item ${i}`,
        required: true
      }));

      // Mock all requests to succeed after a delay
      mockAxiosInstance.request.mockImplementation(() => 
        new Promise(resolve => {
          setTimeout(() => resolve({
            status: 200,
            data: { success: true }
          }), 10);
        })
      );

      const maxConcurrency = 3;
      const startTime = Date.now();
      
      const results = await connector.executeStepsParallel(steps, maxConcurrency);
      
      const duration = Date.now() - startTime;

      expect(results).toHaveLength(10);
      expect(results.every(r => r.success)).toBe(true);
      
      // With 3 concurrent requests and 10 total requests taking ~10ms each,
      // execution should take roughly 40ms (4 batches * 10ms)
      // Allow some margin for test execution variance
      expect(duration).toBeGreaterThan(30);
      expect(duration).toBeLessThan(100);
    });
  });

  describe('Response Caching', () => {
    it('should cache successful responses', async () => {
      const connectorWithCache = new BackendConnector({
        cache: { 
          enabled: true,
          ttlMs: 60000,
          maxSize: 100
        }
      });

      const step: BackendStep = {
        endpoint: '/api/cached-data',
        method: 'GET',
        description: 'Get cacheable data',
        required: true
      };

      const mockResponse = {
        status: 200,
        data: { timestamp: Date.now(), cached: true }
      };

      mockAxiosInstance.request.mockResolvedValueOnce(mockResponse);

      // First request - should hit backend
      const result1 = await connectorWithCache.executeStep(step);
      expect(result1.success).toBe(true);
      expect(result1.fromCache).toBe(false);

      // Second request - should return cached result
      const result2 = await connectorWithCache.executeStep(step);
      expect(result2.success).toBe(true);
      expect(result2.fromCache).toBe(true);
      expect(result2.data).toEqual(result1.data);

      // Verify backend was only called once
      expect(mockAxiosInstance.request).toHaveBeenCalledTimes(1);
    });

    it('should not cache failed responses', async () => {
      const connectorWithCache = new BackendConnector({
        cache: { enabled: true }
      });

      const step: BackendStep = {
        endpoint: '/api/failing-endpoint',
        method: 'GET',
        description: 'Failing endpoint',
        required: true
      };

      mockAxiosInstance.request.mockRejectedValue({
        response: { status: 500 },
        message: 'Internal Server Error'
      });

      // First request - should fail
      const result1 = await connectorWithCache.executeStep(step);
      expect(result1.success).toBe(false);

      // Second request - should try backend again (not cached)
      const result2 = await connectorWithCache.executeStep(step);
      expect(result2.success).toBe(false);

      // Backend should be called twice (no caching of failures)
      expect(mockAxiosInstance.request).toHaveBeenCalledTimes(2);
    });

    it('should respect cache TTL', async () => {
      const connectorWithCache = new BackendConnector({
        cache: { 
          enabled: true,
          ttlMs: 50 // Very short TTL for testing
        }
      });

      const step: BackendStep = {
        endpoint: '/api/ttl-test',
        method: 'GET',
        description: 'TTL test',
        required: true
      };

      mockAxiosInstance.request
        .mockResolvedValueOnce({
          status: 200,
          data: { call: 1 }
        })
        .mockResolvedValueOnce({
          status: 200,
          data: { call: 2 }
        });

      // First request
      const result1 = await connectorWithCache.executeStep(step);
      expect(result1.fromCache).toBe(false);
      expect(result1.data.call).toBe(1);

      // Wait for cache to expire
      await new Promise(resolve => setTimeout(resolve, 60));

      // Second request after TTL - should hit backend again
      const result2 = await connectorWithCache.executeStep(step);
      expect(result2.fromCache).toBe(false);
      expect(result2.data.call).toBe(2);

      expect(mockAxiosInstance.request).toHaveBeenCalledTimes(2);
    });
  });

  describe('Circuit Breaker', () => {
    it('should open circuit breaker after failure threshold', async () => {
      const connectorWithCircuitBreaker = new BackendConnector({
        circuitBreaker: {
          enabled: true,
          failureThreshold: 3,
          recoveryTimeMs: 1000
        },
        retry: { maxRetries: 0 } // Disable retries for cleaner testing
      });

      const step: BackendStep = {
        endpoint: '/api/failing-service',
        method: 'GET',
        description: 'Failing service',
        required: true
      };

      // Mock consistent failures
      mockAxiosInstance.request.mockRejectedValue({
        response: { status: 500 },
        message: 'Service Error'
      });

      // Make requests until circuit breaker opens
      const results = [];
      for (let i = 0; i < 5; i++) {
        const result = await connectorWithCircuitBreaker.executeStep(step);
        results.push(result);
      }

      // First 3 should fail due to backend errors
      expect(results[0].success).toBe(false);
      expect(results[0].error).toContain('Service Error');
      expect(results[1].success).toBe(false);
      expect(results[2].success).toBe(false);

      // Last 2 should fail due to circuit breaker being open
      expect(results[3].success).toBe(false);
      expect(results[3].error).toContain('Circuit breaker is open');
      expect(results[4].success).toBe(false);
      expect(results[4].error).toContain('Circuit breaker is open');

      // Backend should only be called 3 times (until breaker opens)
      expect(mockAxiosInstance.request).toHaveBeenCalledTimes(3);
    });

    it('should transition to half-open after recovery time', async () => {
      const connectorWithCircuitBreaker = new BackendConnector({
        circuitBreaker: {
          enabled: true,
          failureThreshold: 2,
          recoveryTimeMs: 50 // Short recovery time for testing
        },
        retry: { maxRetries: 0 }
      });

      const step: BackendStep = {
        endpoint: '/api/recovery-test',
        method: 'GET',
        description: 'Recovery test',
        required: true
      };

      // First fail to open circuit breaker
      mockAxiosInstance.request
        .mockRejectedValueOnce({
          response: { status: 500 },
          message: 'Service Error'
        })
        .mockRejectedValueOnce({
          response: { status: 500 },
          message: 'Service Error'
        })
        .mockResolvedValueOnce({
          status: 200,
          data: { recovered: true }
        });

      // Trigger failures to open circuit breaker
      await connectorWithCircuitBreaker.executeStep(step);
      await connectorWithCircuitBreaker.executeStep(step);

      // Verify circuit breaker is open
      const openResult = await connectorWithCircuitBreaker.executeStep(step);
      expect(openResult.error).toContain('Circuit breaker is open');

      // Wait for recovery time
      await new Promise(resolve => setTimeout(resolve, 60));

      // Next request should succeed (half-open -> closed)
      const recoveryResult = await connectorWithCircuitBreaker.executeStep(step);
      expect(recoveryResult.success).toBe(true);
      expect(recoveryResult.data.recovered).toBe(true);
    });
  });

  describe('Health Check', () => {
    it('should perform health check successfully', async () => {
      mockAxiosInstance.request.mockResolvedValueOnce({
        status: 200,
        data: { status: 'UP', timestamp: Date.now() }
      });

      const healthResult = await connector.healthCheck();

      expect(healthResult.status).toBe('healthy');
      expect(healthResult.details.backendConnectivity).toBe(true);
      expect(healthResult.details.responseTime).toBeGreaterThan(0);
      expect(healthResult.details.cacheStats).toBeDefined();
      expect(healthResult.details.circuitBreaker).toBeDefined();
    });

    it('should report unhealthy status on health check failure', async () => {
      mockAxiosInstance.request.mockRejectedValueOnce({
        response: { status: 503 },
        message: 'Service Unavailable'
      });

      const healthResult = await connector.healthCheck();

      expect(healthResult.status).toBe('unhealthy');
      expect(healthResult.details.error).toBeDefined();
    });
  });

  describe('Configuration and Statistics', () => {
    it('should provide accurate cache statistics', () => {
      const stats = connector.getCacheStats();
      
      expect(stats).toEqual({
        size: 0,
        maxSize: expect.any(Number),
        enabled: false
      });
    });

    it('should provide circuit breaker status', () => {
      const status = connector.getCircuitBreakerStatus();
      
      expect(status).toEqual({
        state: 'closed',
        failures: 0,
        threshold: expect.any(Number),
        enabled: false
      });
    });

    it('should clear cache successfully', () => {
      connector.clearCache();
      
      const stats = connector.getCacheStats();
      expect(stats.size).toBe(0);
    });
  });
});