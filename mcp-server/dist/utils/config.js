/**
 * Configuration Management - 配置管理工具
 *
 * 负责加载和验证环境配置，包括：
 * - OpenRouter API配置
 * - 后端服务配置
 * - 缓存和限流设置
 * - 安全和隐私配置
 */
import dotenv from 'dotenv';
import { logger } from './logger.js';
// Load environment variables
dotenv.config();
// ===== Configuration Validation =====
function validateRequiredEnvVars() {
    const required = [
        'OPENROUTER_API_KEY',
        'BACKEND_BASE_URL'
    ];
    const missing = required.filter(key => !process.env[key]);
    if (missing.length > 0) {
        throw new Error(`Missing required environment variables: ${missing.join(', ')}`);
    }
}
function getEnvVar(key, defaultValue) {
    const value = process.env[key];
    if (value === undefined) {
        if (defaultValue !== undefined) {
            return defaultValue;
        }
        throw new Error(`Required environment variable ${key} is not set`);
    }
    return value;
}
function getEnvNumber(key, defaultValue) {
    const value = process.env[key];
    if (value === undefined) {
        return defaultValue;
    }
    const parsed = parseInt(value, 10);
    if (isNaN(parsed)) {
        logger.warn(`Invalid number for ${key}: ${value}, using default: ${defaultValue}`);
        return defaultValue;
    }
    return parsed;
}
function getEnvFloat(key, defaultValue) {
    const value = process.env[key];
    if (value === undefined) {
        return defaultValue;
    }
    const parsed = parseFloat(value);
    if (isNaN(parsed)) {
        logger.warn(`Invalid float for ${key}: ${value}, using default: ${defaultValue}`);
        return defaultValue;
    }
    return parsed;
}
function getEnvBoolean(key, defaultValue) {
    const value = process.env[key];
    if (value === undefined) {
        return defaultValue;
    }
    return value.toLowerCase() === 'true' || value === '1';
}
// ===== Configuration Loading =====
export function getConfig() {
    // Validate required environment variables
    validateRequiredEnvVars();
    const config = {
        server: {
            name: getEnvVar('MCP_SERVER_NAME', 'mini-ups-nlq-mcp-server'),
            version: getEnvVar('MCP_SERVER_VERSION', '1.0.0'),
            environment: getEnvVar('NODE_ENV', 'development')
        },
        openrouter: {
            apiKey: getEnvVar('OPENROUTER_API_KEY'),
            baseUrl: getEnvVar('OPENROUTER_BASE_URL', 'https://openrouter.ai/api/v1'),
            siteUrl: getEnvVar('OPENROUTER_SITE_URL', 'https://mini-ups.local'),
            appName: getEnvVar('OPENROUTER_APP_NAME', 'Mini-UPS-NLQ'),
            modelFast: getEnvVar('OPENROUTER_MODEL_FAST', 'openai/gpt-4o-mini'),
            modelFastFallback: getEnvVar('OPENROUTER_MODEL_FAST_FALLBACK', 'meta-llama/llama-3.1-8b-instruct:free'),
            modelStrict: getEnvVar('OPENROUTER_MODEL_STRICT', 'openai/gpt-4o-mini'),
            modelStrictFallback: getEnvVar('OPENROUTER_MODEL_STRICT_FALLBACK', 'google/gemini-1.5-flash'),
            modelAnswer: getEnvVar('OPENROUTER_MODEL_ANSWER', 'openai/gpt-4o'),
            modelAnswerFallback: getEnvVar('OPENROUTER_MODEL_ANSWER_FALLBACK', 'google/gemini-1.5-pro')
        },
        backend: {
            baseUrl: getEnvVar('BACKEND_BASE_URL'),
            timeout: getEnvNumber('BACKEND_API_TIMEOUT', 10000),
            retries: getEnvNumber('BACKEND_API_RETRIES', 3),
            retryDelay: getEnvNumber('BACKEND_API_RETRY_DELAY', 1000),
            authToken: process.env.BACKEND_AUTH_TOKEN,
            apiKey: process.env.BACKEND_API_KEY,
            maxConcurrentCalls: getEnvNumber('MAX_CONCURRENT_BACKEND_CALLS', 5)
        },
        rateLimit: {
            enabled: getEnvBoolean('RATE_LIMIT_ENABLED', true),
            requestsPerMinute: getEnvNumber('RATE_LIMIT_REQUESTS_PER_MINUTE', 30),
            burstSize: getEnvNumber('RATE_LIMIT_BURST_SIZE', 10)
        },
        costControl: {
            enabled: getEnvBoolean('COST_BUDGET_ENABLED', true),
            dailyLimit: getEnvFloat('COST_BUDGET_DAILY_LIMIT', 5.00),
            warningThreshold: getEnvFloat('COST_BUDGET_WARNING_THRESHOLD', 4.00)
        },
        cache: {
            enabled: getEnvBoolean('CACHE_ENABLED', true),
            ttlIntentParsing: getEnvNumber('CACHE_TTL_INTENT_PARSING', 300000),
            ttlBackendResponse: getEnvNumber('CACHE_TTL_BACKEND_RESPONSE', 60000),
            ttlFinalAnswer: getEnvNumber('CACHE_TTL_FINAL_ANSWER', 180000),
            maxSize: getEnvNumber('CACHE_MAX_SIZE', 1000)
        },
        query: {
            maxResults: getEnvNumber('MAX_RESULTS_PER_QUERY', 100),
            maxPaginationSize: getEnvNumber('MAX_PAGINATION_SIZE', 50),
            maxDateRangeDays: getEnvNumber('MAX_DATE_RANGE_DAYS', 90),
            intentConfidenceThreshold: getEnvFloat('INTENT_CONFIDENCE_THRESHOLD', 0.7),
            fallbackToGenericThreshold: getEnvFloat('FALLBACK_TO_GENERIC_THRESHOLD', 0.5)
        },
        privacy: {
            piiMaskingEnabled: getEnvBoolean('PII_MASKING_ENABLED', true),
            maskEmail: getEnvBoolean('PII_MASK_EMAIL', true),
            maskPhone: getEnvBoolean('PII_MASK_PHONE', true),
            maskAddress: getEnvBoolean('PII_MASK_ADDRESS', true),
            maskTrackingNumbers: getEnvBoolean('PII_MASK_TRACKING_NUMBERS', false)
        },
        logging: {
            level: getEnvVar('LOG_LEVEL', 'info'),
            format: getEnvVar('LOG_FORMAT', 'json'),
            piiEnabled: getEnvBoolean('LOG_PII_ENABLED', false),
            tokensEnabled: getEnvBoolean('LOG_TOKENS_ENABLED', true)
        },
        development: {
            mockLlmEnabled: getEnvBoolean('MOCK_LLM_ENABLED', false),
            mockBackendEnabled: getEnvBoolean('MOCK_BACKEND_ENABLED', false),
            evaluationMode: getEnvBoolean('EVALUATION_MODE', false),
            debugIntentParsing: getEnvBoolean('DEBUG_INTENT_PARSING', false),
            debugBackendCalls: getEnvBoolean('DEBUG_BACKEND_CALLS', false),
            debugAnswerGeneration: getEnvBoolean('DEBUG_ANSWER_GENERATION', false)
        }
    };
    // Validate configuration
    validateConfig(config);
    return config;
}
// ===== Configuration Validation =====
function validateConfig(config) {
    // Validate OpenRouter configuration
    if (!config.openrouter.apiKey || config.openrouter.apiKey === 'your-openrouter-api-key-here') {
        throw new Error('Invalid OpenRouter API key. Please set OPENROUTER_API_KEY environment variable.');
    }
    // Validate backend URL
    try {
        new URL(config.backend.baseUrl);
    }
    catch (error) {
        throw new Error(`Invalid backend URL: ${config.backend.baseUrl}`);
    }
    // Validate rate limiting
    if (config.rateLimit.requestsPerMinute <= 0) {
        throw new Error('Rate limit requests per minute must be positive');
    }
    if (config.rateLimit.burstSize <= 0) {
        throw new Error('Rate limit burst size must be positive');
    }
    // Validate cost control
    if (config.costControl.dailyLimit <= 0) {
        throw new Error('Daily cost limit must be positive');
    }
    if (config.costControl.warningThreshold >= config.costControl.dailyLimit) {
        throw new Error('Warning threshold must be less than daily limit');
    }
    // Validate query constraints
    if (config.query.maxResults <= 0 || config.query.maxResults > 1000) {
        throw new Error('Max results per query must be between 1 and 1000');
    }
    if (config.query.intentConfidenceThreshold < 0 || config.query.intentConfidenceThreshold > 1) {
        throw new Error('Intent confidence threshold must be between 0 and 1');
    }
    // Validate timeouts
    if (config.backend.timeout <= 0) {
        throw new Error('Backend timeout must be positive');
    }
    // Log configuration summary (without sensitive data)
    logger.info('Configuration loaded successfully', {
        environment: config.server.environment,
        serverName: config.server.name,
        version: config.server.version,
        backendUrl: config.backend.baseUrl,
        rateLimit: config.rateLimit,
        costControl: {
            enabled: config.costControl.enabled,
            dailyLimit: config.costControl.dailyLimit
        },
        cache: config.cache,
        privacy: config.privacy
    });
}
// ===== Configuration Utilities =====
export function isProduction() {
    return getConfig().server.environment === 'production';
}
export function isDevelopment() {
    return getConfig().server.environment === 'development';
}
export function isDebugMode() {
    const config = getConfig();
    return config.development.debugIntentParsing ||
        config.development.debugBackendCalls ||
        config.development.debugAnswerGeneration;
}
export function getModelConfig(strategy) {
    const config = getConfig();
    switch (strategy) {
        case 'fast':
            return {
                primary: config.openrouter.modelFast,
                fallback: config.openrouter.modelFastFallback
            };
        case 'strict':
            return {
                primary: config.openrouter.modelStrict,
                fallback: config.openrouter.modelStrictFallback
            };
        case 'answer':
            return {
                primary: config.openrouter.modelAnswer,
                fallback: config.openrouter.modelAnswerFallback
            };
        default:
            throw new Error(`Unknown model strategy: ${strategy}`);
    }
}
