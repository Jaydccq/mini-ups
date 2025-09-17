/**
 * IntentPlan v1 - 中间表示数据结构
 * 
 * 用于LLM解析用户意图并规划后端API调用序列
 */

import { z } from 'zod';

// ===== Intent Types =====
export const IntentTypeSchema = z.enum([
  'package_tracking',       // 包裹追踪 - 根据追踪号查询包裹状态和历史
  'user_management',        // 用户管理 - 用户信息查询和管理
  'driver_management',      // 司机管理 - 司机信息和状态查询
  'fleet_management',       // 车队管理 - 卡车和车队统计信息
  'admin_dashboard',        // 管理员仪表盘 - 统计数据和KPI
  'system_health',          // 系统健康 - 健康检查和调试信息
  'authentication',         // 认证验证 - 令牌验证和用户检查
  'user_shipments',         // 用户运单 - 特定用户的运单信息
  'test_endpoints',         // 测试端点 - API测试和验证
  'generic_fallback'        // 通用回退 - 无法识别的意图
]);

export type IntentType = z.infer<typeof IntentTypeSchema>;

// ===== Filter Definitions =====
export const DateRangeSchema = z.object({
  startDate: z.string().optional(),
  endDate: z.string().optional(),
  days: z.number().min(1).max(365).optional()
});

export const FiltersSchema = z.object({
  // 包裹追踪相关
  trackingNumber: z.string().optional(),
  
  // 用户相关
  userId: z.number().optional(),
  username: z.string().optional(),
  email: z.string().optional(),
  role: z.enum(['USER', 'ADMIN', 'DRIVER', 'OPERATOR']).optional(),
  
  // 司机相关
  driverId: z.number().optional(),
  driverName: z.string().optional(),
  driverStatus: z.enum([
    'UNASSIGNED', 'ASSIGNED', 'ON_DUTY', 'OFF_DUTY', 'ON_LEAVE', 'INACTIVE'
  ]).optional(),
  
  // 车队相关
  truckId: z.number().optional(),
  targetX: z.number().optional(),
  targetY: z.number().optional(),
  
  // 调试和健康检查相关
  hoursBack: z.number().min(1).max(168).optional(), // 最多7天
  direction: z.string().optional(),
  messageType: z.string().optional(),
  success: z.boolean().optional(),
  limit: z.number().min(1).max(1000).optional(),
  
  // 时间过滤
  dateRange: DateRangeSchema.optional(),
  
  // 分页
  page: z.number().min(0).default(0),
  size: z.number().min(1).max(100).default(20),
  
  // 地理位置
  originX: z.number().optional(),
  originY: z.number().optional(),
  destX: z.number().optional(),
  destY: z.number().optional(),
  
  // 其他过滤条件
  priority: z.number().optional(),
  includeDetails: z.boolean().default(true)
});

export type Filters = z.infer<typeof FiltersSchema>;

// ===== Field Specifications =====
export const RequiredFieldsSchema = z.object({
  basic: z.array(z.string()).default(['id', 'status', 'createdAt']),
  detailed: z.array(z.string()).default([]),
  sensitive: z.array(z.string()).default([]),
  exclude: z.array(z.string()).default([])
});

export type RequiredFields = z.infer<typeof RequiredFieldsSchema>;

// ===== Backend API Call Steps =====
export const BackendStepSchema = z.object({
  endpoint: z.string(),
  method: z.enum(['GET']).default('GET'),
  baseUrlOverride: z.string().optional(),
  pathParams: z.record(z.string(), z.any()).optional(),
  queryParams: z.record(z.string(), z.any()).optional(),
  description: z.string(),
  required: z.boolean().default(true),
  timeout: z.number().default(10000),
  retries: z.number().default(3)
});

export type BackendStep = z.infer<typeof BackendStepSchema>;

// ===== Main IntentPlan Schema =====
export const IntentPlanSchema = z.object({
  // 基本意图信息
  intent: IntentTypeSchema,
  confidence: z.number().min(0).max(1),
  originalQuery: z.string(),
  
  // 过滤条件
  filters: FiltersSchema,
  
  // 需要的数据字段
  fields: RequiredFieldsSchema.optional(),
  
  // 后端API调用序列
  steps: z.array(BackendStepSchema),
  
  // 响应处理
  summarize: z.boolean().default(true),
  responseFormat: z.enum(['detailed', 'summary', 'table', 'json']).default('detailed'),
  
  // 元数据
  estimatedCost: z.number().optional(),
  priority: z.enum(['low', 'normal', 'high']).default('normal'),
  
  // 验证约束
  constraints: z.object({
    maxResults: z.number().default(100),
    maxDateRange: z.number().default(90),
    requireAuth: z.boolean().default(false),
    sensitiveData: z.boolean().default(false)
  }).optional()
});

export type IntentPlan = z.infer<typeof IntentPlanSchema>;

// ===== Response Schemas =====
export const IntentParsingResultSchema = z.object({
  success: z.boolean(),
  plan: IntentPlanSchema.optional(),
  error: z.string().optional(),
  fallbackReason: z.string().optional(),
  processingTimeMs: z.number(),
  modelUsed: z.string(),
  tokensUsed: z.number().optional()
});

export type IntentParsingResult = z.infer<typeof IntentParsingResultSchema>;

export const ExecutionResultSchema = z.object({
  success: z.boolean(),
  data: z.any().optional(),
  errors: z.array(z.string()).default([]),
  warnings: z.array(z.string()).default([]),
  stepResults: z.array(z.object({
    step: BackendStepSchema,
    success: z.boolean(),
    data: z.any().optional(),
    error: z.string().optional(),
    duration: z.number()
  })),
  totalDuration: z.number(),
  cacheHits: z.number().default(0)
});

export type ExecutionResult = z.infer<typeof ExecutionResultSchema>;

export const FinalAnswerSchema = z.object({
  success: z.boolean(),
  answer: z.string(),
  confidence: z.number().min(0).max(1),
  sources: z.array(z.string()).default([]),
  metadata: z.object({
    intent: IntentTypeSchema,
    resultsCount: z.number().optional(),
    processingTime: z.number(),
    modelUsed: z.string(),
    tokensUsed: z.number().optional(),
    cacheUsed: z.boolean().default(false)
  }),
  warnings: z.array(z.string()).default([])
});

export type FinalAnswer = z.infer<typeof FinalAnswerSchema>;

// ===== Domain Vocabulary =====
export const DomainVocabularySchema = z.object({
  // 运输相关术语
  shipping: z.array(z.string()).default([
    'package', 'shipment', 'delivery', 'tracking', 'order',
    'parcel', 'freight', 'cargo', 'logistics', 'transport'
  ]),
  
  // 状态相关术语
  statuses: z.array(z.string()).default([
    'pending', 'processing', 'shipped', 'delivered', 'cancelled',
    'in transit', 'out for delivery', 'returned', 'delayed'
  ]),
  
  // 时间相关术语
  temporal: z.array(z.string()).default([
    'today', 'yesterday', 'last week', 'last month', 'recent',
    'past', 'since', 'before', 'after', 'within'
  ]),
  
  // 数量和指标
  metrics: z.array(z.string()).default([
    'total', 'count', 'sum', 'average', 'statistics', 'metrics',
    'dashboard', 'kpi', 'performance', 'overview'
  ])
});

export type DomainVocabulary = z.infer<typeof DomainVocabularySchema>;

// ===== Intent Confidence Rules =====
export interface IntentConfidenceRules {
  exactMatch: {
    trackingPattern: RegExp;
    orderIdPattern: RegExp;
    customerIdPattern: RegExp;
    skuPattern: RegExp;
  };
  
  keywordWeights: {
    [key: string]: {
      intent: IntentType;
      weight: number;
      contexts?: string[];
    };
  };
  
  ambiguityHandling: {
    multipleIntents: 'highest_confidence' | 'ask_clarification' | 'combine';
    lowConfidence: 'fallback' | 'ask_clarification' | 'best_guess';
    thresholds: {
      confident: number;
      uncertain: number;
      fallback: number;
    };
  };
}

// ===== PII Masking Configuration =====
export const PiiMaskingConfigSchema = z.object({
  enabled: z.boolean().default(true),
  rules: z.object({
    email: z.object({
      enabled: z.boolean().default(true),
      pattern: z.instanceof(RegExp).default(/\b[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Z|a-z]{2,}\b/),
      replacement: z.string().default('[EMAIL]')
    }),
    phone: z.object({
      enabled: z.boolean().default(true),
      pattern: z.instanceof(RegExp).default(/\b\d{3}-?\d{3}-?\d{4}\b/),
      replacement: z.string().default('[PHONE]')
    }),
    trackingNumber: z.object({
      enabled: z.boolean().default(false),
      pattern: z.instanceof(RegExp).default(/\b1Z[0-9A-Z]{16}\b/),
      replacement: z.string().default('[TRACKING]')
    }),
    address: z.object({
      enabled: z.boolean().default(true),
      keywords: z.array(z.string()).default(['street', 'avenue', 'drive', 'road', 'lane']),
      replacement: z.string().default('[ADDRESS]')
    })
  })
});

export type PiiMaskingConfig = z.infer<typeof PiiMaskingConfigSchema>;

// ===== Validation Utilities =====
export function validateIntentPlan(plan: unknown): IntentPlan {
  return IntentPlanSchema.parse(plan);
}

export function isValidIntent(intent: string): intent is IntentType {
  return IntentTypeSchema.safeParse(intent).success;
}

export function createEmptyFilters(): Filters {
  return FiltersSchema.parse({});
}

export function createFallbackPlan(query: string, reason: string): IntentPlan {
  return {
    intent: 'generic_fallback',
    confidence: 0,
    originalQuery: query,
    filters: createEmptyFilters(),
    steps: [{
      endpoint: '/actuator/health',
      method: 'GET',
      description: 'Health check for fallback response',
      required: false,
      timeout: 10000,
      retries: 0
    }],
    summarize: true,
    responseFormat: 'summary',
    priority: 'low'
  };
}

// ===== Default Configurations =====
export const DEFAULT_INTENT_CONFIDENCE_RULES: IntentConfidenceRules = {
  exactMatch: {
    trackingPattern: /\b1Z[0-9A-Z]{16}\b/i,
    orderIdPattern: /\border[:\s#]*(\d+)\b/i,
    customerIdPattern: /\bcustomer[:\s#]*(\d+)\b/i,
    skuPattern: /\bsku[:\s#]*([A-Z0-9-]+)\b/i
  },
  
  keywordWeights: {
    'track': { intent: 'package_tracking', weight: 0.9 },
    'tracking': { intent: 'package_tracking', weight: 0.9 },
    'status': { intent: 'package_tracking', weight: 0.7 },
    'package': { intent: 'package_tracking', weight: 0.6 },
    'shipment': { intent: 'package_tracking', weight: 0.8 },
    'user': { intent: 'user_management', weight: 0.7 },
    'profile': { intent: 'user_management', weight: 0.8 },
    'driver': { intent: 'driver_management', weight: 0.9 },
    'drivers': { intent: 'driver_management', weight: 0.8 },
    'health': { intent: 'system_health', weight: 0.9 },
    'system': { intent: 'system_health', weight: 0.6 },
    'fleet': { intent: 'fleet_management', weight: 0.9 },
    'truck': { intent: 'fleet_management', weight: 0.8 },
    'dashboard': { intent: 'admin_dashboard', weight: 0.8 },
    'statistics': { intent: 'admin_dashboard', weight: 0.7 },
    'metrics': { intent: 'admin_dashboard', weight: 0.7 },
    'admin': { intent: 'admin_dashboard', weight: 0.8 },
    'test': { intent: 'test_endpoints', weight: 0.8 },
    'auth': { intent: 'authentication', weight: 0.9 }
  },
  
  ambiguityHandling: {
    multipleIntents: 'highest_confidence',
    lowConfidence: 'fallback',
    thresholds: {
      confident: 0.8,
      uncertain: 0.5,
      fallback: 0.3
    }
  }
};
