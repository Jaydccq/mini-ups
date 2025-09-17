/**
 * NLQ Orchestrator Unit Tests
 * 
 * 测试核心编排器的功能，包括：
 * - 意图解析和计划生成
 * - 后端执行协调
 * - 答案生成和格式化
 * - 错误处理和回退机制
 */

import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import { NlqOrchestrator } from '../../orchestrator/nlq-orchestrator.js';
import { openRouterProvider } from '../../providers/openrouter.js';
import { backendConnector } from '../../connectors/backend-connector.js';

// Mock dependencies
vi.mock('../../providers/openrouter.js');
vi.mock('../../connectors/backend-connector.js');
vi.mock('../../utils/logger.js', () => ({
  logger: {
    info: vi.fn(),
    debug: vi.fn(),
    warn: vi.fn(),
    error: vi.fn()
  }
}));

const mockOpenRouterProvider = vi.mocked(openRouterProvider);
const mockBackendConnector = vi.mocked(backendConnector);

describe('NlqOrchestrator', () => {
  let orchestrator: NlqOrchestrator;

  beforeEach(() => {
    orchestrator = new NlqOrchestrator({
      enableDebugMode: true,
      cacheEnabled: false, // Disable cache for predictable testing
      validateIntentConfidence: false // Disable confidence validation for testing
    });

    // Reset all mocks
    vi.clearAllMocks();
  });

  afterEach(() => {
    orchestrator.clearCache();
  });

  describe('Intent Parsing', () => {
    it('should successfully parse tracking number query', async () => {
      // Mock OpenRouter response for intent parsing
      mockOpenRouterProvider.complete.mockResolvedValueOnce({
        content: JSON.stringify({
          intent: 'shipment_status',
          confidence: 0.95,
          originalQuery: '查询追踪号1Z999AA123456789的包裹状态',
          filters: {
            trackingId: '1Z999AA123456789',
            includeDetails: true
          },
          steps: [{
            endpoint: '/api/tracking/{trackingId}',
            method: 'GET',
            pathParams: { trackingId: '1Z999AA123456789' },
            description: '获取包裹追踪状态',
            required: true
          }],
          summarize: true,
          responseFormat: 'detailed',
          priority: 'normal'
        }),
        tokensUsed: { input: 50, output: 100, total: 150 },
        model: 'openai/gpt-4o-mini',
        cost: 0.001,
        duration: 1500,
        cached: false
      });

      // Mock backend connector response
      mockBackendConnector.executeStepsParallel.mockResolvedValueOnce([
        {
          success: true,
          data: {
            trackingId: '1Z999AA123456789',
            status: 'IN_TRANSIT',
            currentLocation: 'Beijing Distribution Center',
            estimatedDelivery: '2024-01-17T17:00:00Z'
          },
          duration: 500,
          fromCache: false,
          retryCount: 0
        }
      ]);

      // Mock answer generation
      mockOpenRouterProvider.complete.mockResolvedValueOnce({
        content: '您的包裹 (追踪号: 1Z999AA123456789) 当前状态是**配送途中**。\n\n📦 **配送信息**:\n- 当前位置: Beijing Distribution Center\n- 预计送达: 2024年1月17日下午5:00\n\n您的包裹正在按计划配送中。',
        tokensUsed: { input: 200, output: 80, total: 280 },
        model: 'openai/gpt-4o',
        cost: 0.005,
        duration: 2000,
        cached: false
      });

      const result = await orchestrator.processQuery(
        '查询追踪号1Z999AA123456789的包裹状态'
      );

      expect(result.success).toBe(true);
      expect(result.metadata.intent).toBe('shipment_status');
      expect(result.confidence).toBe(0.95);
      expect(result.answer).toContain('1Z999AA123456789');
      expect(result.answer).toContain('配送途中');
      expect(result.metadata.stagesCompleted).toEqual([
        'intent_parsing',
        'backend_execution', 
        'answer_generation'
      ]);
      expect(result.metadata.tokensUsed).toBeGreaterThan(0);
      expect(result.metadata.cost).toBeGreaterThan(0);
    });

    it('should handle invalid JSON response from intent parsing', async () => {
      // Mock invalid JSON response
      mockOpenRouterProvider.complete.mockResolvedValueOnce({
        content: 'Invalid JSON response',
        tokensUsed: { input: 50, output: 20, total: 70 },
        model: 'openai/gpt-4o-mini',
        cost: 0.001,
        duration: 1500,
        cached: false
      });

      const result = await orchestrator.processQuery('测试查询');

      expect(result.success).toBe(false);
      expect(result.answer).toContain('抱歉');
      expect(result.warnings).toHaveLength(1);
      expect(result.warnings[0]).toContain('Processing failed');
    });

    it('should handle low confidence intent with fallback', async () => {
      const orchestratorWithValidation = new NlqOrchestrator({
        validateIntentConfidence: true,
        confidenceThreshold: 0.8,
        fallbackToGeneric: true,
        cacheEnabled: false
      });

      // Mock low confidence intent parsing
      mockOpenRouterProvider.complete.mockResolvedValueOnce({
        content: JSON.stringify({
          intent: 'generic_fallback',
          confidence: 0.6, // Below threshold
          originalQuery: '模糊查询',
          filters: {},
          steps: [{
            endpoint: '/actuator/health',
            method: 'GET',
            description: 'Health check for fallback response',
            required: false
          }],
          summarize: true,
          responseFormat: 'summary',
          priority: 'low'
        }),
        tokensUsed: { input: 40, output: 60, total: 100 },
        model: 'openai/gpt-4o-mini',
        cost: 0.001,
        duration: 1200,
        cached: false
      });

      // Mock backend response for fallback
      mockBackendConnector.executeStepsParallel.mockResolvedValueOnce([
        {
          success: true,
          data: { status: 'UP' },
          duration: 300,
          fromCache: false,
          retryCount: 0
        }
      ]);

      // Mock answer generation for fallback
      mockOpenRouterProvider.complete.mockResolvedValueOnce({
        content: '抱歉，我无法理解您的查询。请尝试更具体的问题，例如查询追踪号或订单信息。',
        tokensUsed: { input: 100, output: 50, total: 150 },
        model: 'openai/gpt-4o',
        cost: 0.002,
        duration: 1800,
        cached: false
      });

      const result = await orchestratorWithValidation.processQuery('模糊查询');

      expect(result.success).toBe(true);
      expect(result.metadata.intent).toBe('generic_fallback');
      expect(result.confidence).toBe(0.6);
      expect(result.warnings).toContain('Intent confidence below threshold, using generic fallback');
      expect(result.answer).toContain('抱歉');
    });
  });

  describe('Backend Execution', () => {
    it('should handle backend execution failures gracefully', async () => {
      // Mock successful intent parsing
      mockOpenRouterProvider.complete.mockResolvedValueOnce({
        content: JSON.stringify({
          intent: 'order_lookup',
          confidence: 0.9,
          originalQuery: '查询订单12345',
          filters: { orderId: '12345' },
          steps: [{
            endpoint: '/api/orders/{orderId}',
            method: 'GET',
            pathParams: { orderId: '12345' },
            description: '获取订单详情',
            required: true
          }]
        }),
        tokensUsed: { input: 50, output: 80, total: 130 },
        model: 'openai/gpt-4o-mini',
        cost: 0.001,
        duration: 1400,
        cached: false
      });

      // Mock backend failure
      mockBackendConnector.executeStepsParallel.mockResolvedValueOnce([
        {
          success: false,
          error: 'Backend service unavailable',
          duration: 1000,
          fromCache: false,
          retryCount: 3
        }
      ]);

      // Mock answer generation for error case
      mockOpenRouterProvider.complete.mockResolvedValueOnce({
        content: '抱歉，无法获取订单信息。系统暂时不可用，请稍后重试。',
        tokensUsed: { input: 150, output: 40, total: 190 },
        model: 'openai/gpt-4o',
        cost: 0.003,
        duration: 1600,
        cached: false
      });

      const result = await orchestrator.processQuery('查询订单12345');

      expect(result.success).toBe(true); // Answer generation succeeded
      expect(result.metadata.intent).toBe('order_lookup');
      expect(result.answer).toContain('抱歉');
      expect(result.answer).toContain('暂时不可用');
      expect(result.confidence).toBeLessThan(0.9); // Lower confidence due to backend failure
    });

    it('should handle mixed success/failure in parallel backend calls', async () => {
      // Mock intent parsing for multiple steps
      mockOpenRouterProvider.complete.mockResolvedValueOnce({
        content: JSON.stringify({
          intent: 'customer_orders',
          confidence: 0.85,
          originalQuery: '客户123的订单',
          filters: { customerId: 123 },
          steps: [
            {
              endpoint: '/api/tracking/user/{userId}',
              method: 'GET',
              pathParams: { userId: '123' },
              description: '获取用户订单',
              required: true
            },
            {
              endpoint: '/api/admin/orders/summary',
              method: 'GET',
              queryParams: { userId: '123' },
              description: '获取订单统计',
              required: false
            }
          ]
        }),
        tokensUsed: { input: 60, output: 120, total: 180 },
        model: 'openai/gpt-4o-mini',
        cost: 0.002,
        duration: 1600,
        cached: false
      });

      // Mock mixed backend responses
      mockBackendConnector.executeStepsParallel.mockResolvedValueOnce([
        {
          success: true,
          data: [
            { orderId: '001', status: 'DELIVERED' },
            { orderId: '002', status: 'IN_TRANSIT' }
          ],
          duration: 800,
          fromCache: false,
          retryCount: 0
        },
        {
          success: false,
          error: 'Summary service timeout',
          duration: 5000,
          fromCache: false,
          retryCount: 2
        }
      ]);

      // Mock answer generation
      mockOpenRouterProvider.complete.mockResolvedValueOnce({
        content: '找到客户123的订单信息：\n\n订单001: 已送达\n订单002: 配送中\n\n注意：订单统计信息暂时不可用。',
        tokensUsed: { input: 200, output: 60, total: 260 },
        model: 'openai/gpt-4o',
        cost: 0.004,
        duration: 2000,
        cached: false
      });

      const result = await orchestrator.processQuery('客户123的订单');

      expect(result.success).toBe(true);
      expect(result.metadata.intent).toBe('customer_orders');
      expect(result.answer).toContain('客户123');
      expect(result.answer).toContain('订单001');
      expect(result.answer).toContain('暂时不可用');
    });
  });

  describe('Answer Generation', () => {
    it('should handle answer generation failure with fallback', async () => {
      // Mock successful intent parsing and backend execution
      mockOpenRouterProvider.complete
        .mockResolvedValueOnce({
          content: JSON.stringify({
            intent: 'shipment_status',
            confidence: 0.9,
            originalQuery: '测试查询',
            filters: {},
            steps: [{ endpoint: '/test', method: 'GET', description: '测试', required: true }]
          }),
          tokensUsed: { input: 50, output: 80, total: 130 },
          model: 'openai/gpt-4o-mini',
          cost: 0.001,
          duration: 1000,
          cached: false
        })
        .mockRejectedValueOnce(new Error('Answer generation failed'))
        .mockResolvedValueOnce({
          content: '抱歉，系统暂时无法处理您的查询。请稍后重试或联系客服获取帮助。',
          tokensUsed: { input: 100, output: 30, total: 130 },
          model: 'openai/gpt-4o-mini',
          cost: 0.001,
          duration: 800,
          cached: false
        });

      mockBackendConnector.executeStepsParallel.mockResolvedValueOnce([
        {
          success: true,
          data: { test: 'data' },
          duration: 500,
          fromCache: false,
          retryCount: 0
        }
      ]);

      const result = await orchestrator.processQuery('测试查询');

      expect(result.success).toBe(false);
      expect(result.answer).toContain('抱歉');
      expect(result.warnings).toHaveLength(1);
      expect(result.warnings[0]).toContain('Processing failed');
    });
  });

  describe('Caching', () => {
    it('should cache and return cached results', async () => {
      const orchestratorWithCache = new NlqOrchestrator({
        cacheEnabled: true
      });

      const query = '缓存测试查询';

      // First request - should call LLM
      mockOpenRouterProvider.complete
        .mockResolvedValueOnce({
          content: JSON.stringify({
            intent: 'generic_fallback',
            confidence: 0.7,
            originalQuery: query,
            filters: {},
            steps: [{ endpoint: '/test', method: 'GET', description: '测试', required: true }]
          }),
          tokensUsed: { input: 50, output: 80, total: 130 },
          model: 'openai/gpt-4o-mini',
          cost: 0.001,
          duration: 1000,
          cached: false
        })
        .mockResolvedValueOnce({
          content: '这是一个缓存测试回答',
          tokensUsed: { input: 100, output: 40, total: 140 },
          model: 'openai/gpt-4o',
          cost: 0.002,
          duration: 1500,
          cached: false
        });

      mockBackendConnector.executeStepsParallel.mockResolvedValue([
        {
          success: true,
          data: { cached: 'data' },
          duration: 300,
          fromCache: false,
          retryCount: 0
        }
      ]);

      // First call
      const result1 = await orchestratorWithCache.processQuery(query);
      expect(result1.success).toBe(true);
      expect(result1.metadata.cacheHits).toBe(0);

      // Second call - should return cached result
      const result2 = await orchestratorWithCache.processQuery(query);
      expect(result2.success).toBe(true);
      expect(result2.metadata.cacheHits).toBe(1);
      expect(result2.answer).toBe(result1.answer);

      // Verify LLM was only called for first request
      expect(mockOpenRouterProvider.complete).toHaveBeenCalledTimes(2); // Intent + Answer for first call only
    });
  });

  describe('Health Check', () => {
    it('should perform comprehensive health check', async () => {
      // Mock provider health checks
      mockOpenRouterProvider.healthCheck.mockResolvedValueOnce({
        status: 'healthy',
        details: { responseTime: 150 }
      });

      mockBackendConnector.healthCheck.mockResolvedValueOnce({
        status: 'healthy',
        details: { responseTime: 200 }
      });

      // Mock a simple test query for orchestrator health check
      mockOpenRouterProvider.complete
        .mockResolvedValueOnce({
          content: JSON.stringify({
            intent: 'service_health',
            confidence: 0.95,
            originalQuery: 'health check',
            filters: {},
            steps: [{ endpoint: '/actuator/health', method: 'GET', description: 'Health check', required: true }]
          }),
          tokensUsed: { input: 30, output: 50, total: 80 },
          model: 'openai/gpt-4o-mini',
          cost: 0.001,
          duration: 800,
          cached: false
        })
        .mockResolvedValueOnce({
          content: '系统运行正常',
          tokensUsed: { input: 80, output: 20, total: 100 },
          model: 'openai/gpt-4o',
          cost: 0.001,
          duration: 600,
          cached: false
        });

      mockBackendConnector.executeStepsParallel.mockResolvedValueOnce([
        {
          success: true,
          data: { status: 'UP' },
          duration: 200,
          fromCache: false,
          retryCount: 0
        }
      ]);

      const healthResult = await orchestrator.healthCheck();

      expect(healthResult.status).toBe('healthy');
      expect(healthResult.details).toBeDefined();
      expect(healthResult.details.testQuerySuccess).toBe(true);
    });

    it('should report unhealthy status when components fail', async () => {
      // Mock provider failures
      mockOpenRouterProvider.healthCheck.mockRejectedValueOnce(
        new Error('OpenRouter service unavailable')
      );

      mockBackendConnector.healthCheck.mockResolvedValueOnce({
        status: 'unhealthy',
        details: { error: 'Backend connection failed' }
      });

      const healthResult = await orchestrator.healthCheck();

      expect(healthResult.status).toBe('unhealthy');
      expect(healthResult.details.error).toBeDefined();
    });
  });

  describe('Configuration', () => {
    it('should respect configuration settings', () => {
      const customOrchestrator = new NlqOrchestrator({
        enableDebugMode: false,
        maxRetries: 5,
        timeoutMs: 60000,
        fallbackToGeneric: false,
        cacheEnabled: false,
        validateIntentConfidence: true,
        confidenceThreshold: 0.9
      });

      const cacheStats = customOrchestrator.getCacheStats();
      expect(cacheStats.enabled).toBe(false);

      customOrchestrator.clearCache(); // Should not throw
    });
  });
});