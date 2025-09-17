/**
 * Test Setup Configuration
 * 
 * 全局测试配置和环境设置
 */

import { vi } from 'vitest';

// 设置测试环境变量
process.env.NODE_ENV = 'test';
process.env.LOG_LEVEL = 'error'; // Reduce log noise during tests
process.env.OPENROUTER_API_KEY = 'test-api-key';
process.env.BACKEND_BASE_URL = 'http://localhost:8081';
process.env.RATE_LIMIT_ENABLED = 'true';
process.env.CACHE_ENABLED = 'true';
process.env.COST_BUDGET_ENABLED = 'false'; // Disable cost tracking in tests
process.env.PII_MASKING_ENABLED = 'false'; // Disable PII masking for test readability

// Mock timers if needed
// vi.useFakeTimers();

// Global test hooks
beforeEach(() => {
  // Reset all mocks before each test
  vi.clearAllMocks();
});

afterAll(() => {
  // Restore real timers if fake timers were used
  // vi.useRealTimers();
});

// Silence console output during tests unless debugging
const originalConsole = { ...console };
global.console = {
  ...console,
  // Comment out the lines below to see console output during tests
  log: vi.fn(),
  info: vi.fn(),
  debug: vi.fn(),
  // Keep error and warn for debugging
  error: originalConsole.error,
  warn: originalConsole.warn
};

// Extend expect matchers if needed
expect.extend({
  // Custom matchers could go here
});

// Global test utilities
global.testUtils = {
  // Helper functions for tests
  delay: (ms: number) => new Promise(resolve => setTimeout(resolve, ms)),
  
  mockLlmResponse: (content: any, options: Partial<any> = {}) => ({
    content: typeof content === 'string' ? content : JSON.stringify(content),
    tokensUsed: { input: 50, output: 100, total: 150 },
    model: 'test-model',
    cost: 0.001,
    duration: 1000,
    cached: false,
    ...options
  }),
  
  mockBackendResponse: (data: any, options: Partial<any> = {}) => ({
    success: true,
    data,
    duration: 500,
    fromCache: false,
    retryCount: 0,
    ...options
  }),
  
  createMockStep: (endpoint: string, options: Partial<any> = {}) => ({
    endpoint,
    method: 'GET' as const,
    description: `Test ${endpoint}`,
    required: true,
    ...options
  })
};

// Type declarations for global utilities
declare global {
  var testUtils: {
    delay: (ms: number) => Promise<void>;
    mockLlmResponse: (content: any, options?: Partial<any>) => any;
    mockBackendResponse: (data: any, options?: Partial<any>) => any;
    createMockStep: (endpoint: string, options?: Partial<any>) => any;
  };
}