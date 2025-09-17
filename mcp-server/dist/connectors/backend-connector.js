/**
 * Backend Connector - Mini-UPS后端API连接器
 *
 * 负责与Mini-UPS后端服务的HTTP通信，包括：
 * - 重试机制和回退策略
 * - 连接池和超时管理
 * - 响应缓存和错误处理
 * - 认证和安全控制
 */
import axios from 'axios';
import { logger } from '../utils/logger.js';
import { getConfig } from '../utils/config.js';
// ===== Backend Connector Implementation =====
export class BackendConnector {
    client;
    config;
    responseCache = new Map();
    circuitBreaker = {
        state: 'closed',
        failures: 0
    };
    constructor(config) {
        const appConfig = getConfig();
        this.config = {
            baseUrl: config?.baseUrl || appConfig.backend.baseUrl,
            timeout: config?.timeout || appConfig.backend.timeout,
            authToken: config?.authToken || appConfig.backend.authToken,
            apiKey: config?.apiKey || appConfig.backend.apiKey,
            retry: {
                maxRetries: 3,
                baseDelayMs: 1000,
                maxDelayMs: 10000,
                backoffMultiplier: 2,
                retryableStatusCodes: [408, 429, 500, 502, 503, 504],
                ...config?.retry
            },
            cache: {
                enabled: true,
                ttlMs: 60000, // 1 minute default
                maxSize: 1000,
                keyPattern: 'backend:{method}:{url}:{params}',
                ...config?.cache
            },
            circuitBreaker: {
                enabled: true,
                failureThreshold: 5,
                recoveryTimeMs: 30000, // 30 seconds
                ...config?.circuitBreaker
            },
            services: {
                amazonBaseUrl: process.env.AMAZON_BASE_URL,
                ...config?.services
            }
        };
        this.client = axios.create({
            baseURL: this.config.baseUrl,
            timeout: this.config.timeout,
            headers: {
                'Content-Type': 'application/json',
                'Accept': 'application/json',
                'User-Agent': 'Mini-UPS-NLQ-Server/1.0.0',
                ...(this.config.authToken && { 'Authorization': `Bearer ${this.config.authToken}` }),
                ...(this.config.apiKey && { 'X-API-Key': this.config.apiKey })
            }
        });
        // Setup axios interceptors
        this.setupInterceptors();
        logger.info('Backend connector initialized', {
            baseUrl: this.config.baseUrl,
            timeout: this.config.timeout,
            cacheEnabled: this.config.cache.enabled,
            circuitBreakerEnabled: this.config.circuitBreaker.enabled
        });
    }
    /**
     * Execute a backend step with retry and caching
     */
    async executeStep(step) {
        const startTime = Date.now();
        const cacheKey = this.generateCacheKey(step);
        logger.debug('Executing backend step', {
            endpoint: step.endpoint,
            method: step.method,
            cacheKey: this.config.cache.enabled ? cacheKey : 'disabled'
        });
        // Check cache first
        if (this.config.cache.enabled) {
            const cached = this.getCachedResponse(cacheKey);
            if (cached) {
                logger.debug('Returning cached response', { endpoint: step.endpoint });
                return {
                    ...cached,
                    duration: Date.now() - startTime,
                    fromCache: true,
                    retryCount: 0
                };
            }
        }
        // Check circuit breaker
        if (this.config.circuitBreaker.enabled && !this.canMakeRequest()) {
            const error = 'Circuit breaker is open, request blocked';
            logger.warn(error, { endpoint: step.endpoint });
            return {
                success: false,
                error,
                duration: Date.now() - startTime,
                fromCache: false,
                retryCount: 0
            };
        }
        // Execute request with retry logic
        let lastError = null;
        let retryCount = 0;
        for (let attempt = 0; attempt <= this.config.retry.maxRetries; attempt++) {
            try {
                const response = await this.makeHttpRequest(step);
                // Success - record for circuit breaker
                this.recordSuccess();
                const result = {
                    success: true,
                    data: response.data,
                    statusCode: response.status,
                    headers: this.extractHeaders(response),
                    duration: Date.now() - startTime,
                    fromCache: false,
                    retryCount
                };
                // Cache successful response
                if (this.config.cache.enabled && response.status === 200) {
                    this.setCachedResponse(cacheKey, result);
                }
                logger.debug('Backend step completed successfully', {
                    endpoint: step.endpoint,
                    statusCode: response.status,
                    duration: result.duration,
                    retryCount
                });
                return result;
            }
            catch (error) {
                lastError = error;
                retryCount = attempt;
                const statusCode = error.response?.status;
                logger.warn('Backend step attempt failed', {
                    endpoint: step.endpoint,
                    attempt: attempt + 1,
                    maxRetries: this.config.retry.maxRetries + 1,
                    statusCode,
                    error: lastError.message
                });
                // Record failure for circuit breaker
                this.recordFailure();
                // Check if this error is retryable
                if (!this.isRetryableError(error) || attempt === this.config.retry.maxRetries) {
                    break;
                }
                // Wait before retry with exponential backoff
                if (attempt < this.config.retry.maxRetries) {
                    const delay = this.calculateRetryDelay(attempt);
                    logger.debug('Waiting before retry', { delay, attempt });
                    await this.sleep(delay);
                }
            }
        }
        // All retries failed
        const finalError = lastError?.message || 'Unknown error';
        logger.error('Backend step failed after all retries', {
            endpoint: step.endpoint,
            retryCount,
            error: finalError
        });
        return {
            success: false,
            error: finalError,
            statusCode: lastError?.response?.status,
            duration: Date.now() - startTime,
            fromCache: false,
            retryCount
        };
    }
    /**
     * Execute multiple backend steps in parallel
     */
    async executeStepsParallel(steps, maxConcurrency = 5) {
        logger.debug('Executing backend steps in parallel', {
            stepsCount: steps.length,
            maxConcurrency
        });
        const results = [];
        // Process steps in batches to respect concurrency limit
        for (let i = 0; i < steps.length; i += maxConcurrency) {
            const batch = steps.slice(i, i + maxConcurrency);
            const batchPromises = batch.map(step => this.executeStep(step));
            const batchResults = await Promise.all(batchPromises);
            results.push(...batchResults);
        }
        const successCount = results.filter(r => r.success).length;
        logger.info('Parallel backend execution completed', {
            total: steps.length,
            successful: successCount,
            failed: steps.length - successCount
        });
        return results;
    }
    /**
     * Make actual HTTP request
     */
    async makeHttpRequest(step) {
        const url = this.buildUrl(step);
        const baseURL = this.resolveBaseUrl(step);
        const config = {
            method: step.method,
            url,
            baseURL,
            params: step.queryParams,
            timeout: step.timeout || this.config.timeout
        };
        return await this.client.request(config);
    }
    /**
     * Build complete URL with path parameters
     */
    buildUrl(step) {
        let url = step.endpoint;
        // Replace path parameters
        if (step.pathParams) {
            for (const [key, value] of Object.entries(step.pathParams)) {
                url = url.replace(`{${key}}`, encodeURIComponent(String(value)));
            }
        }
        return url;
    }
    /**
     * Resolve base URL for a step, supporting per-step overrides and aliases.
     */
    resolveBaseUrl(step) {
        const override = step.baseUrlOverride?.trim();
        if (!override)
            return this.config.baseUrl;
        if (/^https?:\/\//i.test(override)) {
            return override;
        }
        if (override === 'backend')
            return this.config.baseUrl;
        if (override === 'amazon') {
            if (this.config.services?.amazonBaseUrl) {
                return this.config.services.amazonBaseUrl;
            }
            logger.warn('amazon baseUrl alias provided but AMAZON_BASE_URL not set; falling back to backend');
            return this.config.baseUrl;
        }
        logger.warn('Unknown baseUrlOverride alias; falling back to backend', { override });
        return this.config.baseUrl;
    }
    /**
     * Setup axios interceptors for logging and error handling
     */
    setupInterceptors() {
        // Request interceptor
        this.client.interceptors.request.use((config) => {
            logger.debug('HTTP request starting', {
                method: config.method?.toUpperCase(),
                url: config.url,
                baseURL: config.baseURL
            });
            return config;
        }, (error) => {
            logger.error('HTTP request setup failed', { error: error.message });
            return Promise.reject(error);
        });
        // Response interceptor
        this.client.interceptors.response.use((response) => {
            logger.debug('HTTP response received', {
                status: response.status,
                statusText: response.statusText,
                url: response.config.url
            });
            return response;
        }, (error) => {
            const status = error.response?.status;
            const statusText = error.response?.statusText;
            const url = error.config?.url;
            logger.debug('HTTP response error', {
                status,
                statusText,
                url,
                error: error.message
            });
            return Promise.reject(error);
        });
    }
    /**
     * Generate cache key for request
     */
    generateCacheKey(step) {
        const keyData = {
            method: step.method,
            endpoint: step.endpoint,
            baseUrl: this.resolveBaseUrl(step),
            pathParams: step.pathParams,
            queryParams: step.queryParams
        };
        return Buffer.from(JSON.stringify(keyData)).toString('base64');
    }
    /**
     * Get cached response if available and not expired
     */
    getCachedResponse(key) {
        const entry = this.responseCache.get(key);
        if (!entry)
            return null;
        const now = Date.now();
        if (now - entry.timestamp > entry.ttl) {
            this.responseCache.delete(key);
            return null;
        }
        return entry.data;
    }
    /**
     * Cache successful response
     */
    setCachedResponse(key, response) {
        // Enforce cache size limit
        if (this.responseCache.size >= this.config.cache.maxSize) {
            // Remove oldest entries (simple LRU approximation)
            const oldestKey = this.responseCache.keys().next().value;
            if (oldestKey) {
                this.responseCache.delete(oldestKey);
            }
        }
        this.responseCache.set(key, {
            data: response,
            timestamp: Date.now(),
            ttl: this.config.cache.ttlMs
        });
    }
    /**
     * Check if error is retryable
     */
    isRetryableError(error) {
        // Network errors are retryable
        if (error.code === 'ECONNRESET' || error.code === 'ETIMEDOUT') {
            return true;
        }
        // HTTP status code based retries
        const status = error.response?.status;
        return status ? this.config.retry.retryableStatusCodes.includes(status) : false;
    }
    /**
     * Calculate retry delay with exponential backoff
     */
    calculateRetryDelay(attempt) {
        const delay = this.config.retry.baseDelayMs *
            Math.pow(this.config.retry.backoffMultiplier, attempt);
        return Math.min(delay, this.config.retry.maxDelayMs);
    }
    /**
     * Circuit breaker: Check if request can be made
     */
    canMakeRequest() {
        const now = Date.now();
        switch (this.circuitBreaker.state) {
            case 'closed':
                return true;
            case 'open':
                if (this.circuitBreaker.nextRetryTime && now >= this.circuitBreaker.nextRetryTime) {
                    this.circuitBreaker.state = 'half-open';
                    logger.info('Circuit breaker transitioning to half-open');
                    return true;
                }
                return false;
            case 'half-open':
                return true;
            default:
                return true;
        }
    }
    /**
     * Circuit breaker: Record successful request
     */
    recordSuccess() {
        if (this.circuitBreaker.state === 'half-open') {
            this.circuitBreaker.state = 'closed';
            this.circuitBreaker.failures = 0;
            this.circuitBreaker.lastFailureTime = undefined;
            this.circuitBreaker.nextRetryTime = undefined;
            logger.info('Circuit breaker closed after successful request');
        }
    }
    /**
     * Circuit breaker: Record failed request
     */
    recordFailure() {
        this.circuitBreaker.failures++;
        this.circuitBreaker.lastFailureTime = Date.now();
        if (this.circuitBreaker.failures >= this.config.circuitBreaker.failureThreshold) {
            this.circuitBreaker.state = 'open';
            this.circuitBreaker.nextRetryTime = Date.now() + this.config.circuitBreaker.recoveryTimeMs;
            logger.warn('Circuit breaker opened due to failures', {
                failures: this.circuitBreaker.failures,
                threshold: this.config.circuitBreaker.failureThreshold
            });
        }
    }
    /**
     * Extract relevant headers from response
     */
    extractHeaders(response) {
        const relevantHeaders = ['content-type', 'x-rate-limit-remaining', 'x-response-time'];
        const extracted = {};
        for (const header of relevantHeaders) {
            if (response.headers[header]) {
                extracted[header] = response.headers[header];
            }
        }
        return extracted;
    }
    /**
     * Sleep utility for retry delays
     */
    sleep(ms) {
        return new Promise(resolve => setTimeout(resolve, ms));
    }
    /**
     * Clear response cache
     */
    clearCache() {
        this.responseCache.clear();
        logger.info('Backend connector cache cleared');
    }
    /**
     * Get cache statistics
     */
    getCacheStats() {
        return {
            size: this.responseCache.size,
            maxSize: this.config.cache.maxSize,
            enabled: this.config.cache.enabled
        };
    }
    /**
     * Get circuit breaker status
     */
    getCircuitBreakerStatus() {
        return {
            state: this.circuitBreaker.state,
            failures: this.circuitBreaker.failures,
            threshold: this.config.circuitBreaker.failureThreshold,
            enabled: this.config.circuitBreaker.enabled
        };
    }
    /**
     * Health check for backend connectivity
     */
    async healthCheck() {
        try {
            const healthStep = {
                endpoint: '/actuator/health',
                method: 'GET',
                description: 'Health check endpoint',
                required: true,
                timeout: 10000,
                retries: 1
            };
            const result = await this.executeStep(healthStep);
            return {
                status: result.success ? 'healthy' : 'unhealthy',
                details: {
                    backendConnectivity: result.success,
                    responseTime: result.duration,
                    cacheStats: this.getCacheStats(),
                    circuitBreaker: this.getCircuitBreakerStatus(),
                    config: {
                        baseUrl: this.config.baseUrl,
                        timeout: this.config.timeout
                    }
                }
            };
        }
        catch (error) {
            return {
                status: 'unhealthy',
                details: {
                    error: error.message,
                    circuitBreaker: this.getCircuitBreakerStatus()
                }
            };
        }
    }
}
// Export singleton instance
export const backendConnector = new BackendConnector();
