/**
 * OpenRouter Provider
 * 
 * 管理与OpenRouter API的交互，支持多模型策略和回退机制
 */

import OpenAI from 'openai';
import { getConfig } from '../utils/config.js';
import { logger } from '../utils/logger.js';

// ===== Types =====
export interface ModelConfig {
  name: string;
  maxTokens: number;
  temperature: number;
  timeout: number;
  costPer1kTokens: {
    input: number;
    output: number;
  };
}

export interface CompletionRequest {
  model: string;
  messages: Array<{
    role: 'system' | 'user' | 'assistant';
    content: string;
  }>;
  temperature?: number;
  max_tokens?: number;
  response_format?: { type: 'json_object' };
  timeout?: number;
}

export interface CompletionResponse {
  content: string;
  tokensUsed: {
    input: number;
    output: number;
    total: number;
  };
  model: string;
  cost: number;
  duration: number;
  cached: boolean;
}

export type ModelStrategy = 'fast' | 'strict' | 'answer';

// ===== Model Configurations =====
const MODEL_CONFIGS: Record<string, ModelConfig> = {
  // Fast models for intent parsing
  'openai/gpt-4o-mini': {
    name: 'openai/gpt-4o-mini',
    maxTokens: 4000,
    temperature: 0.1,
    timeout: 3000,
    costPer1kTokens: { input: 0.00015, output: 0.0006 }
  },
  'meta-llama/llama-3.1-8b-instruct:free': {
    name: 'meta-llama/llama-3.1-8b-instruct:free',
    maxTokens: 2000,
    temperature: 0.1,
    timeout: 5000,
    costPer1kTokens: { input: 0, output: 0 }
  },
  
  // Strict JSON models
  'google/gemini-1.5-flash': {
    name: 'google/gemini-1.5-flash',
    maxTokens: 3000,
    temperature: 0.0,
    timeout: 8000,
    costPer1kTokens: { input: 0.000075, output: 0.0003 }
  },
  
  // High-quality answer models
  'openai/gpt-4o': {
    name: 'openai/gpt-4o',
    maxTokens: 4000,
    temperature: 0.3,
    timeout: 15000,
    costPer1kTokens: { input: 0.005, output: 0.015 }
  },
  'google/gemini-1.5-pro': {
    name: 'google/gemini-1.5-pro',
    maxTokens: 3000,
    temperature: 0.3,
    timeout: 12000,
    costPer1kTokens: { input: 0.00125, output: 0.005 }
  }
};

// ===== OpenRouter Client =====
export class OpenRouterProvider {
  private client: OpenAI;
  private config: ReturnType<typeof getConfig>;
  private costTracking: {
    dailySpent: number;
    requestCount: number;
    lastReset: string;
  };
  private responseCache = new Map<string, CompletionResponse>();

  constructor() {
    this.config = getConfig();
    this.client = new OpenAI({
      apiKey: this.config.openrouter.apiKey,
      baseURL: this.config.openrouter.baseUrl,
      defaultHeaders: {
        'HTTP-Referer': this.config.openrouter.siteUrl,
        'X-Title': this.config.openrouter.appName
      }
    });

    this.costTracking = {
      dailySpent: 0,
      requestCount: 0,
      lastReset: new Date().toDateString()
    };

    logger.info('OpenRouter provider initialized', {
      baseUrl: this.config.openrouter.baseUrl,
      modelsConfigured: Object.keys(MODEL_CONFIGS).length
    });
  }

  /**
   * Get model configuration for a strategy
   */
  getModelForStrategy(strategy: ModelStrategy): string {
    switch (strategy) {
      case 'fast':
        return this.config.openrouter.modelFast;
      case 'strict':
        return this.config.openrouter.modelStrict;
      case 'answer':
        return this.config.openrouter.modelAnswer;
      default:
        return this.config.openrouter.modelFast;
    }
  }

  /**
   * Get fallback model for a strategy
   */
  getFallbackModelForStrategy(strategy: ModelStrategy): string {
    switch (strategy) {
      case 'fast':
        return this.config.openrouter.modelFastFallback;
      case 'strict':
        return this.config.openrouter.modelStrictFallback;
      case 'answer':
        return this.config.openrouter.modelAnswerFallback;
      default:
        return this.config.openrouter.modelFastFallback;
    }
  }

  /**
   * Complete a chat request with retry and fallback
   */
  async complete(request: CompletionRequest, strategy: ModelStrategy = 'fast'): Promise<CompletionResponse> {
    this.checkDailyCostLimit();
    this.resetDailyCostIfNeeded();

    const cacheKey = this.generateCacheKey(request);
    const cached = this.responseCache.get(cacheKey);
    if (cached) {
      logger.debug('Returning cached response', { model: cached.model });
      return { ...cached, cached: true };
    }

    // Try primary model first
    const primaryModel = this.getModelForStrategy(strategy);
    try {
      const response = await this.executeRequest({ ...request, model: primaryModel });
      this.responseCache.set(cacheKey, response);
      return response;
    } catch (error) {
      logger.warn('Primary model failed, trying fallback', {
        primaryModel,
        error: (error as Error).message
      });

      // Try fallback model
      const fallbackModel = this.getFallbackModelForStrategy(strategy);
      try {
        const response = await this.executeRequest({ ...request, model: fallbackModel });
        this.responseCache.set(cacheKey, response);
        return response;
      } catch (fallbackError) {
        logger.error('Both primary and fallback models failed', {
          primaryModel,
          fallbackModel,
          error: (fallbackError as Error).message
        });
        throw new Error(`All models failed: ${(error as Error).message}, ${(fallbackError as Error).message}`);
      }
    }
  }

  /**
   * Execute a single request to OpenRouter
   */
  private async executeRequest(request: CompletionRequest): Promise<CompletionResponse> {
    const startTime = Date.now();
    const modelConfig = MODEL_CONFIGS[request.model];
    
    if (!modelConfig) {
      throw new Error(`Unknown model: ${request.model}`);
    }

    // Estimate input tokens
    const inputTokens = this.estimateTokens(
      request.messages.map(m => m.content).join(' ')
    );

    // Apply model-specific settings
    const completionRequest = {
      model: request.model,
      messages: request.messages,
      temperature: request.temperature ?? modelConfig.temperature,
      max_tokens: Math.min(request.max_tokens ?? modelConfig.maxTokens, modelConfig.maxTokens),
      response_format: request.response_format
    };

    logger.debug('Making OpenRouter request', {
      model: request.model,
      inputTokens,
      temperature: completionRequest.temperature,
      maxTokens: completionRequest.max_tokens
    });

    try {
      // Set timeout for the request
      const timeoutMs = request.timeout ?? modelConfig.timeout;
      const controller = new AbortController();
      const timeoutId = setTimeout(() => controller.abort(), timeoutMs);

      const completion = await this.client.chat.completions.create(
        completionRequest,
        { signal: controller.signal }
      );

      clearTimeout(timeoutId);

      const duration = Date.now() - startTime;
      const content = completion.choices[0]?.message?.content || '';
      const outputTokens = this.estimateTokens(content);
      const totalTokens = inputTokens + outputTokens;
      
      // Calculate cost
      const cost = this.calculateCost(request.model, inputTokens, outputTokens);
      this.updateCostTracking(cost);

      const response: CompletionResponse = {
        content,
        tokensUsed: {
          input: inputTokens,
          output: outputTokens,
          total: totalTokens
        },
        model: request.model,
        cost,
        duration,
        cached: false
      };

      logger.info('OpenRouter request completed', {
        model: request.model,
        duration,
        tokensUsed: totalTokens,
        cost: cost.toFixed(6)
      });

      return response;

    } catch (error: any) {
      const duration = Date.now() - startTime;
      logger.error('OpenRouter request failed', {
        model: request.model,
        duration,
        error: error.message
      });

      if (error.name === 'AbortError') {
        throw new Error(`Request timeout after ${request.timeout || modelConfig.timeout}ms`);
      }

      throw error;
    }
  }

  /**
   * Estimate token count (approximate)
   */
  private estimateTokens(text: string): number {
    // Simple estimation: ~4 characters per token for most languages
    // This is approximate - for exact counting, we'd need tiktoken for each model
    return Math.ceil(text.length / 4);
  }

  /**
   * Calculate cost for a request
   */
  private calculateCost(model: string, inputTokens: number, outputTokens: number): number {
    const config = MODEL_CONFIGS[model];
    if (!config) return 0;

    const inputCost = (inputTokens / 1000) * config.costPer1kTokens.input;
    const outputCost = (outputTokens / 1000) * config.costPer1kTokens.output;
    
    return inputCost + outputCost;
  }

  /**
   * Update cost tracking
   */
  private updateCostTracking(cost: number): void {
    this.costTracking.dailySpent += cost;
    this.costTracking.requestCount += 1;
  }

  /**
   * Check daily cost limit
   */
  private checkDailyCostLimit(): void {
    if (!this.config.costControl.enabled) return;

    if (this.costTracking.dailySpent >= this.config.costControl.dailyLimit) {
      throw new Error(`Daily cost limit exceeded: $${this.costTracking.dailySpent.toFixed(4)}`);
    }

    if (this.costTracking.dailySpent >= this.config.costControl.warningThreshold) {
      logger.warn('Approaching daily cost limit', {
        spent: this.costTracking.dailySpent,
        limit: this.config.costControl.dailyLimit
      });
    }
  }

  /**
   * Reset daily cost tracking if needed
   */
  private resetDailyCostIfNeeded(): void {
    const today = new Date().toDateString();
    if (this.costTracking.lastReset !== today) {
      logger.info('Resetting daily cost tracking', {
        previousDaySpent: this.costTracking.dailySpent,
        previousDayRequests: this.costTracking.requestCount
      });

      this.costTracking = {
        dailySpent: 0,
        requestCount: 0,
        lastReset: today
      };
    }
  }

  /**
   * Generate cache key for request
   */
  private generateCacheKey(request: CompletionRequest): string {
    const key = {
      model: request.model,
      messages: request.messages,
      temperature: request.temperature,
      max_tokens: request.max_tokens,
      response_format: request.response_format
    };
    return Buffer.from(JSON.stringify(key)).toString('base64');
  }

  /**
   * Get cost tracking information
   */
  getCostTracking() {
    return { ...this.costTracking };
  }

  /**
   * Clear response cache
   */
  clearCache(): void {
    this.responseCache.clear();
    logger.info('Response cache cleared');
  }

  /**
   * Get available models
   */
  getAvailableModels(): Array<{ name: string; strategy: ModelStrategy[] }> {
    const models = [
      { name: this.config.openrouter.modelFast, strategy: ['fast' as ModelStrategy] },
      { name: this.config.openrouter.modelStrict, strategy: ['strict' as ModelStrategy] },
      { name: this.config.openrouter.modelAnswer, strategy: ['answer' as ModelStrategy] }
    ];

    return models.filter((model, index, self) => 
      index === self.findIndex(m => m.name === model.name)
    );
  }

  /**
   * Health check for OpenRouter service
   */
  async healthCheck(): Promise<{ status: 'healthy' | 'unhealthy'; details: any }> {
    try {
      const startTime = Date.now();
      
      // Simple test request
      const response = await this.complete({
        model: this.config.openrouter.modelFast,
        messages: [
          { role: 'user', content: 'Hello' }
        ],
        temperature: 0,
        max_tokens: 10
      }, 'fast');

      const duration = Date.now() - startTime;

      return {
        status: 'healthy',
        details: {
          responseTime: duration,
          model: response.model,
          costTracking: this.getCostTracking(),
          cacheSize: this.responseCache.size
        }
      };
    } catch (error) {
      return {
        status: 'unhealthy',
        details: {
          error: (error as Error).message,
          costTracking: this.getCostTracking()
        }
      };
    }
  }
}

// Export singleton instance
export const openRouterProvider = new OpenRouterProvider();
