/**
 * NLQ Orchestrator - 核心自然语言查询编排器
 *
 * 实现3阶段智能处理流程：
 * 1. Intent Parsing - 意图识别与计划生成
 * 2. Backend Execution - 后端API调用执行
 * 3. Answer Generation - 自然语言答案生成
 */
import { logger } from '../utils/logger.js';
import { openRouterProvider } from '../providers/openrouter.js';
import { getConfig } from '../utils/config.js';
import { validateIntentPlan, createFallbackPlan } from '../schemas/intent-plan.js';
import { INTENT_PARSING_TEMPLATE, ANSWER_GENERATION_TEMPLATE, fillPromptTemplate, getModelInstructions, createFallbackPrompt } from '../schemas/prompts.js';
import { backendConnector } from '../connectors/backend-connector.js';
// ===== Main NLQ Orchestrator =====
export class NlqOrchestrator {
    config;
    orchestratorConfig;
    queryCache = new Map();
    constructor(config) {
        this.config = getConfig();
        this.orchestratorConfig = {
            enableDebugMode: config?.enableDebugMode ?? false,
            maxRetries: config?.maxRetries ?? 3,
            timeoutMs: config?.timeoutMs ?? 30000,
            fallbackToGeneric: config?.fallbackToGeneric ?? true,
            cacheEnabled: config?.cacheEnabled ?? true,
            validateIntentConfidence: config?.validateIntentConfidence ?? true,
            confidenceThreshold: config?.confidenceThreshold ?? 0.7,
            ...config
        };
        logger.info('NLQ Orchestrator initialized', {
            enableDebugMode: this.orchestratorConfig.enableDebugMode,
            cacheEnabled: this.orchestratorConfig.cacheEnabled,
            confidenceThreshold: this.orchestratorConfig.confidenceThreshold
        });
    }
    /**
     * Main entry point for natural language query processing
     */
    async processQuery(query, context) {
        const startTime = Date.now();
        const queryId = this.generateQueryId(query, context);
        logger.info('Processing NLQ query', { query, queryId, context });
        // Check cache first
        if (this.orchestratorConfig.cacheEnabled) {
            const cached = this.queryCache.get(queryId);
            if (cached) {
                logger.debug('Returning cached NLQ result', { queryId });
                return {
                    ...cached,
                    metadata: {
                        ...cached.metadata,
                        cacheHits: cached.metadata.cacheHits + 1
                    }
                };
            }
        }
        const result = {
            success: false,
            answer: '',
            confidence: 0,
            metadata: {
                intent: 'generic_fallback',
                processingTime: 0,
                tokensUsed: 0,
                cost: 0,
                stagesCompleted: [],
                cacheHits: 0
            },
            warnings: [],
            debugInfo: this.orchestratorConfig.enableDebugMode ? {
                rawLlmResponses: []
            } : undefined
        };
        try {
            // Stage 1: Intent Parsing
            logger.debug('Starting Stage 1: Intent Parsing', { queryId });
            const intentResult = await this.parseIntent(query, context);
            result.metadata.stagesCompleted.push('intent_parsing');
            result.metadata.tokensUsed += intentResult.tokensUsed || 0;
            if (this.orchestratorConfig.enableDebugMode) {
                result.debugInfo.intentParsingResult = intentResult;
            }
            if (!intentResult.success || !intentResult.plan) {
                throw new Error(`Intent parsing failed: ${intentResult.error}`);
            }
            // Validate confidence threshold
            if (this.orchestratorConfig.validateIntentConfidence &&
                intentResult.plan.confidence < this.orchestratorConfig.confidenceThreshold) {
                if (this.orchestratorConfig.fallbackToGeneric) {
                    logger.warn('Low confidence intent, using fallback', {
                        confidence: intentResult.plan.confidence,
                        threshold: this.orchestratorConfig.confidenceThreshold
                    });
                    intentResult.plan = createFallbackPlan(query, 'Low confidence in intent recognition');
                    result.warnings.push('Intent confidence below threshold, using generic fallback');
                }
                else {
                    throw new Error(`Intent confidence too low: ${intentResult.plan.confidence}`);
                }
            }
            result.metadata.intent = intentResult.plan.intent;
            result.confidence = intentResult.plan.confidence;
            // Stage 2: Backend Execution
            logger.debug('Starting Stage 2: Backend Execution', { queryId });
            const executionResult = await this.executeBackendSteps(intentResult.plan);
            result.metadata.stagesCompleted.push('backend_execution');
            if (this.orchestratorConfig.enableDebugMode) {
                result.debugInfo.executionResult = executionResult;
            }
            if (!executionResult.success) {
                result.warnings.push(...executionResult.errors);
            }
            // Stage 3: Answer Generation
            logger.debug('Starting Stage 3: Answer Generation', { queryId });
            const finalAnswer = await this.generateAnswer(query, intentResult.plan, executionResult, context);
            result.metadata.stagesCompleted.push('answer_generation');
            result.metadata.tokensUsed += finalAnswer.metadata.tokensUsed || 0;
            if (this.orchestratorConfig.enableDebugMode && finalAnswer.metadata.modelUsed) {
                result.debugInfo.rawLlmResponses.push({
                    stage: 'answer_generation',
                    response: finalAnswer.answer
                });
            }
            // Finalize result
            result.success = finalAnswer.success;
            result.answer = finalAnswer.answer;
            result.confidence = Math.min(result.confidence, finalAnswer.confidence);
            result.warnings.push(...finalAnswer.warnings);
            const totalTime = Date.now() - startTime;
            result.metadata.processingTime = totalTime;
            // Calculate total cost (approximation)
            result.metadata.cost = this.estimateTotalCost(result.metadata.tokensUsed);
            logger.info('NLQ query processing completed', {
                queryId,
                success: result.success,
                intent: result.metadata.intent,
                processingTime: totalTime,
                tokensUsed: result.metadata.tokensUsed,
                confidence: result.confidence
            });
            // Cache successful results
            if (this.orchestratorConfig.cacheEnabled && result.success) {
                this.queryCache.set(queryId, { ...result });
            }
            return result;
        }
        catch (error) {
            const errorMessage = error.message;
            logger.error('NLQ query processing failed', { queryId, error: errorMessage });
            // Generate fallback response
            try {
                const fallbackAnswer = await this.generateFallbackAnswer(query, errorMessage, context);
                result.answer = fallbackAnswer;
                result.warnings.push(`Processing failed: ${errorMessage}`);
            }
            catch (fallbackError) {
                result.answer = "抱歉，系统暂时无法处理您的查询。请稍后重试或联系客服获取帮助。";
                result.warnings.push(`Both primary and fallback processing failed: ${errorMessage}`);
            }
            result.metadata.processingTime = Date.now() - startTime;
            return result;
        }
    }
    /**
     * Stage 1: Parse user intent and generate execution plan
     */
    async parseIntent(query, context) {
        const startTime = Date.now();
        try {
            const { system, user } = fillPromptTemplate(INTENT_PARSING_TEMPLATE, {
                USER_QUERY: query,
                AVAILABLE_ENDPOINTS: (await import('../schemas/endpoints.js')).renderEndpointList()
            });
            const enhancedSystem = this.enhancePromptWithContext(system, context);
            const modelInstructions = getModelInstructions('strict');
            const response = await openRouterProvider.complete({
                model: openRouterProvider.getModelForStrategy('strict'),
                messages: [
                    { role: 'system', content: enhancedSystem },
                    { role: 'user', content: user }
                ],
                temperature: modelInstructions.temperature,
                max_tokens: modelInstructions.maxTokens,
                response_format: modelInstructions.responseFormat
            }, 'strict');
            logger.debug('Intent parsing LLM response', {
                model: response.model,
                tokensUsed: response.tokensUsed.total,
                duration: response.duration
            });
            // Parse and validate JSON response
            let plan;
            try {
                const parsedResponse = JSON.parse(response.content);
                plan = validateIntentPlan(parsedResponse);
            }
            catch (parseError) {
                throw new Error(`Invalid JSON response from intent parsing: ${parseError.message}`);
            }
            return {
                success: true,
                plan,
                processingTimeMs: Date.now() - startTime,
                modelUsed: response.model,
                tokensUsed: response.tokensUsed.total
            };
        }
        catch (error) {
            logger.error('Intent parsing failed', { error: error.message });
            return {
                success: false,
                error: error.message,
                fallbackReason: 'LLM intent parsing failed',
                processingTimeMs: Date.now() - startTime,
                modelUsed: 'none'
            };
        }
    }
    /**
     * Stage 2: Execute backend API calls based on intent plan
     */
    async executeBackendSteps(plan) {
        const startTime = Date.now();
        const stepResults = [];
        const errors = [];
        const warnings = [];
        let aggregatedData = [];
        let totalCacheHits = 0;
        logger.debug('Executing backend steps', {
            stepsCount: plan.steps.length,
            intent: plan.intent
        });
        // Validate endpoints against allowlist before execution
        const { isAllowedEndpointPath } = await import('../schemas/endpoints.js');
        for (const step of plan.steps) {
            if (!isAllowedEndpointPath(step.endpoint)) {
                return {
                    success: false,
                    data: [],
                    errors: [`Endpoint not allowed: ${step.endpoint}`],
                    warnings: [],
                    stepResults: [],
                    totalDuration: 0,
                    cacheHits: 0
                };
            }
        }
        // Execute steps in parallel for better performance
        const maxConcurrency = this.config.backend.maxConcurrentCalls || 5;
        const backendResponses = await backendConnector.executeStepsParallel(plan.steps, maxConcurrency);
        // Process results
        for (let i = 0; i < plan.steps.length; i++) {
            const step = plan.steps[i];
            const response = backendResponses[i];
            const stepResult = {
                step,
                success: response.success,
                data: response.data,
                error: response.error,
                duration: response.duration
            };
            stepResults.push(stepResult);
            if (response.fromCache) {
                totalCacheHits++;
            }
            if (response.success && response.data) {
                if (Array.isArray(response.data)) {
                    aggregatedData.push(...response.data);
                }
                else {
                    aggregatedData.push(response.data);
                }
            }
            else {
                const errorMessage = response.error || `Step ${step.endpoint} failed`;
                if (step.required) {
                    errors.push(errorMessage);
                    logger.error('Required backend step failed', {
                        step: step.endpoint,
                        error: errorMessage,
                        retryCount: response.retryCount
                    });
                }
                else {
                    warnings.push(`Optional step failed: ${step.endpoint}`);
                    logger.warn('Optional backend step failed', {
                        step: step.endpoint,
                        error: errorMessage
                    });
                }
            }
        }
        const hasRequiredStepFailures = stepResults.some(r => !r.success && r.step.required);
        return {
            success: !hasRequiredStepFailures,
            data: aggregatedData,
            errors,
            warnings,
            stepResults,
            totalDuration: Date.now() - startTime,
            cacheHits: totalCacheHits
        };
    }
    /**
     * Stage 3: Generate natural language answer from execution results
     */
    async generateAnswer(query, plan, execution, context) {
        const startTime = Date.now();
        try {
            const { system, user } = fillPromptTemplate(ANSWER_GENERATION_TEMPLATE, {
                USER_QUERY: query,
                INTENT_TYPE: plan.intent,
                SYSTEM_DATA: JSON.stringify(execution.data, null, 2),
                EXECUTION_STATUS: execution.success ? 'SUCCESS' : 'PARTIAL_FAILURE'
            });
            const enhancedSystem = this.enhancePromptWithContext(system, context);
            const modelInstructions = getModelInstructions('answer');
            const response = await openRouterProvider.complete({
                model: openRouterProvider.getModelForStrategy('answer'),
                messages: [
                    { role: 'system', content: enhancedSystem },
                    { role: 'user', content: user }
                ],
                temperature: modelInstructions.temperature,
                max_tokens: modelInstructions.maxTokens
            }, 'answer');
            logger.debug('Answer generation LLM response', {
                model: response.model,
                tokensUsed: response.tokensUsed.total,
                duration: response.duration
            });
            return {
                success: true,
                answer: response.content,
                confidence: execution.success ? 0.9 : 0.7, // Lower confidence for partial failures
                sources: execution.stepResults
                    .filter(r => r.success)
                    .map(r => r.step.endpoint),
                metadata: {
                    intent: plan.intent,
                    resultsCount: Array.isArray(execution.data) ? execution.data.length : (execution.data ? 1 : 0),
                    processingTime: Date.now() - startTime,
                    modelUsed: response.model,
                    tokensUsed: response.tokensUsed.total,
                    cacheUsed: response.cached
                },
                warnings: execution.warnings
            };
        }
        catch (error) {
            logger.error('Answer generation failed', { error: error.message });
            return {
                success: false,
                answer: "抱歉，无法生成回答。请稍后重试。",
                confidence: 0,
                sources: [],
                metadata: {
                    intent: plan.intent,
                    processingTime: Date.now() - startTime,
                    modelUsed: 'none',
                    tokensUsed: 0,
                    cacheUsed: false
                },
                warnings: [`Answer generation failed: ${error.message}`]
            };
        }
    }
    /**
     * Generate fallback answer when processing fails
     */
    async generateFallbackAnswer(query, reason, context) {
        try {
            const fallbackPrompt = createFallbackPrompt(query, reason);
            const enhancedPrompt = this.enhancePromptWithContext(fallbackPrompt, context);
            const response = await openRouterProvider.complete({
                model: openRouterProvider.getModelForStrategy('fast'),
                messages: [
                    { role: 'system', content: enhancedPrompt }
                ],
                temperature: 0.3,
                max_tokens: 500
            }, 'fast');
            return response.content;
        }
        catch (error) {
            logger.error('Fallback answer generation failed', { error: error.message });
            return "抱歉，系统暂时无法处理您的查询。请稍后重试或联系客服获取帮助。";
        }
    }
    /**
     * Enhanced prompt with contextual information
     */
    enhancePromptWithContext(basePrompt, context) {
        if (!context)
            return basePrompt;
        let enhanced = basePrompt;
        if (context.timeContext) {
            enhanced += `\n\n## 时间上下文:\n${context.timeContext}`;
        }
        if (context.domainContext) {
            enhanced += `\n\n## 领域上下文:\n${context.domainContext}`;
        }
        if (context.userHistory && context.userHistory.length > 0) {
            enhanced += `\n\n## 用户历史查询:\n${context.userHistory.slice(-3).join('\n')}`;
        }
        return enhanced;
    }
    /**
     * Generate mock backend response for development/testing
     */
    generateMockBackendResponse(step, plan) {
        // TODO: Replace with actual backend HTTP calls
        const mockResponses = {
            '/api/tracking': {
                trackingNumber: plan.filters.trackingNumber || '1Z999AA123456789',
                status: 'IN_TRANSIT',
                currentLocation: 'Beijing Distribution Center',
                estimatedDelivery: '2024-01-17T17:00:00Z',
                shipmentHistory: [
                    { status: 'PICKED_UP', timestamp: '2024-01-15T10:00:00Z', location: 'Origin Warehouse' },
                    { status: 'IN_TRANSIT', timestamp: '2024-01-16T08:00:00Z', location: 'Beijing Distribution Center' }
                ]
            },
            '/api/users': {
                userId: plan.filters.userId || 12345,
                username: 'test_user',
                role: 'USER',
                status: 'ACTIVE',
                createdAt: '2024-01-15T09:00:00Z'
            },
            '/drivers': {
                drivers: [
                    { id: 1, name: plan.filters.driverName || '张三', status: 'AVAILABLE' },
                    { id: 2, name: '李四', status: 'ON_DUTY' }
                ]
            }
        };
        // Find matching mock response
        const endpoint = step.endpoint.replace(/\{[^}]+\}/g, ''); // Remove path parameters
        const baseEndpoint = endpoint.split('?')[0]; // Remove query parameters
        for (const [pattern, response] of Object.entries(mockResponses)) {
            if (baseEndpoint.includes(pattern)) {
                return response;
            }
        }
        return { message: 'Mock response', endpoint: step.endpoint };
    }
    /**
     * Generate unique query ID for caching
     */
    generateQueryId(query, context) {
        const contextStr = context ? JSON.stringify(context) : '';
        return Buffer.from(query + contextStr).toString('base64').substring(0, 16);
    }
    /**
     * Estimate total cost based on tokens used
     */
    estimateTotalCost(tokens) {
        // Rough estimation based on average model costs
        // Fast model: ~$0.0003 per 1k tokens
        // Answer model: ~$0.010 per 1k tokens
        return (tokens / 1000) * 0.005; // Average estimate
    }
    /**
     * Clear query cache
     */
    clearCache() {
        this.queryCache.clear();
        logger.info('NLQ orchestrator cache cleared');
    }
    /**
     * Get cache statistics
     */
    getCacheStats() {
        return {
            size: this.queryCache.size,
            enabled: this.orchestratorConfig.cacheEnabled
        };
    }
    /**
     * Health check for orchestrator
     */
    async healthCheck() {
        try {
            // Test with simple query
            const testResult = await this.processQuery('health check', {
                timeContext: 'Test context for health check'
            });
            return {
                status: 'healthy',
                details: {
                    testQuerySuccess: testResult.success,
                    cacheStats: this.getCacheStats(),
                    config: this.orchestratorConfig
                }
            };
        }
        catch (error) {
            return {
                status: 'unhealthy',
                details: {
                    error: error.message,
                    config: this.orchestratorConfig
                }
            };
        }
    }
}
// Export singleton instance
export const nlqOrchestrator = new NlqOrchestrator();
