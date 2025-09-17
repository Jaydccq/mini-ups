#!/usr/bin/env node
/**
 * Mini-UPS NLQ MCP Server - 主入口文件
 *
 * 智能自然语言查询MCP服务器，支持：
 * - 意图识别和计划生成
 * - 后端API调用执行
 * - 自然语言答案生成
 * - 多模型策略和成本控制
 */
import { Server } from '@modelcontextprotocol/sdk/server/index.js';
import { StdioServerTransport } from '@modelcontextprotocol/sdk/server/stdio.js';
import { CallToolRequestSchema, ErrorCode, ListToolsRequestSchema, McpError, } from '@modelcontextprotocol/sdk/types.js';
import { getConfig } from './utils/config.js';
import { logger, createRequestLogger } from './utils/logger.js';
import { rateLimiter } from './utils/rate-limiter.js';
import { nlqOrchestrator } from './orchestrator/nlq-orchestrator.js';
import { openRouterProvider } from './providers/openrouter.js';
import { backendConnector } from './connectors/backend-connector.js';
// ===== MCP Server Implementation =====
export class NlqMcpServer {
    server;
    config;
    constructor() {
        this.config = getConfig();
        // Initialize logger with config
        logger.updateConfig({
            level: this.config.logging.level,
            format: this.config.logging.format,
            piiEnabled: this.config.logging.piiEnabled,
            service: this.config.server.name,
            version: this.config.server.version,
            environment: this.config.server.environment
        });
        this.server = new Server({
            name: this.config.server.name,
            version: this.config.server.version,
        }, {
            capabilities: {
                tools: {},
            },
        });
        this.setupHandlers();
        logger.info('NLQ MCP Server initialized', {
            name: this.config.server.name,
            version: this.config.server.version,
            environment: this.config.server.environment
        });
    }
    setupHandlers() {
        // List available tools
        this.server.setRequestHandler(ListToolsRequestSchema, async () => {
            logger.debug('Listing available tools');
            return {
                tools: [
                    {
                        name: 'nlq_query',
                        description: 'Process natural language queries about Mini-UPS logistics operations',
                        inputSchema: {
                            type: 'object',
                            properties: {
                                query: {
                                    type: 'string',
                                    description: 'Natural language query about shipments, orders, tracking, inventory, etc.'
                                },
                                userId: {
                                    type: 'string',
                                    description: 'Optional user ID for personalized results and rate limiting'
                                },
                                context: {
                                    type: 'object',
                                    description: 'Optional context information',
                                    properties: {
                                        sessionId: { type: 'string' },
                                        userHistory: {
                                            type: 'array',
                                            items: { type: 'string' },
                                            description: 'Previous queries in this session'
                                        },
                                        timeContext: {
                                            type: 'string',
                                            description: 'Current time context for relative queries'
                                        },
                                        domainContext: {
                                            type: 'string',
                                            description: 'Domain-specific context'
                                        }
                                    }
                                }
                            },
                            required: ['query']
                        }
                    },
                    {
                        name: 'health_check',
                        description: 'Check the health and status of the NLQ system',
                        inputSchema: {
                            type: 'object',
                            properties: {
                                includeDetails: {
                                    type: 'boolean',
                                    description: 'Include detailed health information',
                                    default: false
                                }
                            }
                        }
                    },
                    {
                        name: 'get_system_stats',
                        description: 'Get system statistics and performance metrics',
                        inputSchema: {
                            type: 'object',
                            properties: {
                                includeCache: {
                                    type: 'boolean',
                                    description: 'Include cache statistics',
                                    default: true
                                },
                                includeRateLimit: {
                                    type: 'boolean',
                                    description: 'Include rate limiting statistics',
                                    default: true
                                }
                            }
                        }
                    },
                    {
                        name: 'clear_cache',
                        description: 'Clear system caches (orchestrator, backend connector, OpenRouter)',
                        inputSchema: {
                            type: 'object',
                            properties: {
                                component: {
                                    type: 'string',
                                    enum: ['all', 'orchestrator', 'backend', 'openrouter'],
                                    description: 'Which cache to clear',
                                    default: 'all'
                                }
                            }
                        }
                    }
                ],
            };
        });
        // Handle tool calls
        this.server.setRequestHandler(CallToolRequestSchema, async (request) => {
            const context = {
                requestId: this.generateRequestId(),
                startTime: Date.now()
            };
            const requestLogger = createRequestLogger(context.requestId, context.userId);
            try {
                const { name, arguments: args } = request.params;
                requestLogger.info('Tool call started', {
                    toolName: name,
                    arguments: args
                });
                switch (name) {
                    case 'nlq_query':
                        return await this.handleNlqQuery(args, context, requestLogger);
                    case 'health_check':
                        return await this.handleHealthCheck(args, context, requestLogger);
                    case 'get_system_stats':
                        return await this.handleGetSystemStats(args, context, requestLogger);
                    case 'clear_cache':
                        return await this.handleClearCache(args, context, requestLogger);
                    default:
                        throw new McpError(ErrorCode.MethodNotFound, `Unknown tool: ${name}`);
                }
            }
            catch (error) {
                const duration = Date.now() - context.startTime;
                requestLogger.error('Tool call failed', {
                    duration,
                    error: error instanceof Error ? error.message : String(error)
                }, error instanceof Error ? error : undefined);
                if (error instanceof McpError) {
                    throw error;
                }
                throw new McpError(ErrorCode.InternalError, `Tool execution failed: ${error instanceof Error ? error.message : String(error)}`);
            }
        });
    }
    /**
     * Handle natural language query processing
     */
    async handleNlqQuery(args, context, requestLogger) {
        const { query, userId, context: queryContext } = args;
        if (!query || typeof query !== 'string') {
            throw new McpError(ErrorCode.InvalidParams, 'Query parameter is required and must be a string');
        }
        // Update context with userId
        if (userId) {
            context.userId = userId;
        }
        // Check rate limits
        const rateLimitResult = rateLimiter.checkLimit(userId);
        if (!rateLimitResult.allowed) {
            requestLogger.warn('Rate limit exceeded', {
                userId,
                tokensRemaining: rateLimitResult.tokensRemaining,
                retryAfter: rateLimitResult.retryAfter
            });
            throw new McpError(ErrorCode.InvalidRequest, `Rate limit exceeded. Try again in ${rateLimitResult.retryAfter}ms`);
        }
        requestLogger.info('Processing NLQ query', {
            query,
            userId,
            context: queryContext,
            rateLimitTokensRemaining: rateLimitResult.tokensRemaining
        });
        // Process query through orchestrator
        const result = await nlqOrchestrator.processQuery(query, {
            userId,
            sessionId: queryContext?.sessionId,
            userHistory: queryContext?.userHistory,
            timeContext: queryContext?.timeContext || new Date().toISOString(),
            domainContext: queryContext?.domainContext
        });
        const duration = Date.now() - context.startTime;
        requestLogger.info('NLQ query completed', {
            success: result.success,
            intent: result.metadata.intent,
            confidence: result.confidence,
            processingTime: result.metadata.processingTime,
            tokensUsed: result.metadata.tokensUsed,
            cost: result.metadata.cost,
            duration
        });
        // Log token usage for cost tracking
        if (result.metadata.tokensUsed > 0) {
            logger.tokenUsage('mixed', // Multiple models used
            Math.floor(result.metadata.tokensUsed * 0.6), // Estimate input tokens
            Math.floor(result.metadata.tokensUsed * 0.4), // Estimate output tokens
            result.metadata.cost);
        }
        return {
            content: [
                {
                    type: 'text',
                    text: result.answer
                }
            ],
            isError: !result.success,
            _meta: {
                requestId: context.requestId,
                success: result.success,
                confidence: result.confidence,
                intent: result.metadata.intent,
                processingTime: result.metadata.processingTime,
                tokensUsed: result.metadata.tokensUsed,
                cost: result.metadata.cost,
                stagesCompleted: result.metadata.stagesCompleted,
                cacheHits: result.metadata.cacheHits,
                warnings: result.warnings,
                rateLimitTokensRemaining: rateLimitResult.tokensRemaining
            }
        };
    }
    /**
     * Handle health check
     */
    async handleHealthCheck(args, context, requestLogger) {
        const { includeDetails = false } = args;
        requestLogger.info('Health check started', { includeDetails });
        const healthChecks = await Promise.allSettled([
            nlqOrchestrator.healthCheck(),
            openRouterProvider.healthCheck(),
            backendConnector.healthCheck()
        ]);
        const [orchestratorHealth, openRouterHealth, backendHealth] = healthChecks;
        const overallStatus = healthChecks.every(check => check.status === 'fulfilled' && check.value.status === 'healthy') ? 'healthy' : 'unhealthy';
        const duration = Date.now() - context.startTime;
        const result = {
            status: overallStatus,
            timestamp: new Date().toISOString(),
            version: this.config.server.version,
            environment: this.config.server.environment,
            duration,
            components: {
                orchestrator: orchestratorHealth.status === 'fulfilled' ? orchestratorHealth.value : { status: 'unhealthy', error: 'Check failed' },
                openrouter: openRouterHealth.status === 'fulfilled' ? openRouterHealth.value : { status: 'unhealthy', error: 'Check failed' },
                backend: backendHealth.status === 'fulfilled' ? backendHealth.value : { status: 'unhealthy', error: 'Check failed' }
            }
        };
        if (!includeDetails) {
            // Remove detailed information for basic health check
            delete result.components.orchestrator.details;
            delete result.components.openrouter.details;
            delete result.components.backend.details;
        }
        requestLogger.info('Health check completed', {
            status: overallStatus,
            duration,
            includeDetails
        });
        return {
            content: [
                {
                    type: 'text',
                    text: JSON.stringify(result, null, 2)
                }
            ],
            _meta: {
                requestId: context.requestId,
                status: overallStatus,
                duration
            }
        };
    }
    /**
     * Handle system statistics request
     */
    async handleGetSystemStats(args, context, requestLogger) {
        const { includeCache = true, includeRateLimit = true } = args;
        requestLogger.info('Getting system stats', { includeCache, includeRateLimit });
        const stats = {
            timestamp: new Date().toISOString(),
            uptime: process.uptime(),
            memory: process.memoryUsage(),
            config: {
                environment: this.config.server.environment,
                rateLimit: this.config.rateLimit,
                costControl: this.config.costControl,
                cache: this.config.cache
            }
        };
        if (includeCache) {
            stats.cache = {
                orchestrator: nlqOrchestrator.getCacheStats(),
                backend: backendConnector.getCacheStats(),
                openrouter: { size: 'N/A' } // OpenRouter cache is internal
            };
        }
        if (includeRateLimit) {
            stats.rateLimit = rateLimiter.getStats();
        }
        // Add cost tracking from OpenRouter
        stats.costTracking = openRouterProvider.getCostTracking();
        const duration = Date.now() - context.startTime;
        requestLogger.info('System stats completed', { duration });
        return {
            content: [
                {
                    type: 'text',
                    text: JSON.stringify(stats, null, 2)
                }
            ],
            _meta: {
                requestId: context.requestId,
                duration
            }
        };
    }
    /**
     * Handle cache clearing
     */
    async handleClearCache(args, context, requestLogger) {
        const { component = 'all' } = args;
        requestLogger.info('Clearing cache', { component });
        const results = {};
        try {
            if (component === 'all' || component === 'orchestrator') {
                nlqOrchestrator.clearCache();
                results.orchestrator = true;
            }
            if (component === 'all' || component === 'backend') {
                backendConnector.clearCache();
                results.backend = true;
            }
            if (component === 'all' || component === 'openrouter') {
                openRouterProvider.clearCache();
                results.openrouter = true;
            }
            const duration = Date.now() - context.startTime;
            requestLogger.info('Cache cleared successfully', { component, results, duration });
            return {
                content: [
                    {
                        type: 'text',
                        text: JSON.stringify({
                            success: true,
                            component,
                            cleared: results,
                            timestamp: new Date().toISOString()
                        }, null, 2)
                    }
                ],
                _meta: {
                    requestId: context.requestId,
                    duration,
                    cleared: results
                }
            };
        }
        catch (error) {
            const duration = Date.now() - context.startTime;
            requestLogger.error('Cache clearing failed', { component, error: error.message }, error);
            throw new McpError(ErrorCode.InternalError, `Failed to clear cache: ${error.message}`);
        }
    }
    /**
     * Generate unique request ID
     */
    generateRequestId() {
        return `nlq_${Date.now()}_${Math.random().toString(36).substr(2, 9)}`;
    }
    /**
     * Start the MCP server
     */
    async run() {
        const transport = new StdioServerTransport();
        logger.info('Starting NLQ MCP Server', {
            transport: 'stdio',
            capabilities: ['tools']
        });
        await this.server.connect(transport);
        logger.info('NLQ MCP Server started successfully');
    }
    /**
     * Graceful shutdown
     */
    async shutdown() {
        logger.info('Shutting down NLQ MCP Server');
        try {
            // Cleanup resources
            rateLimiter.shutdown();
            logger.info('NLQ MCP Server shutdown completed');
        }
        catch (error) {
            logger.error('Error during shutdown', { error: error.message }, error);
        }
    }
}
// ===== Error Handling =====
process.on('unhandledRejection', (reason, promise) => {
    logger.error('Unhandled Rejection', {
        reason: reason instanceof Error ? reason.message : String(reason),
        promise: promise.toString()
    }, reason instanceof Error ? reason : undefined);
});
process.on('uncaughtException', (error) => {
    logger.error('Uncaught Exception', { error: error.message }, error);
    process.exit(1);
});
// ===== Server Startup =====
async function main() {
    try {
        const server = new NlqMcpServer();
        // Setup graceful shutdown
        process.on('SIGINT', async () => {
            logger.info('Received SIGINT, shutting down gracefully');
            await server.shutdown();
            process.exit(0);
        });
        process.on('SIGTERM', async () => {
            logger.info('Received SIGTERM, shutting down gracefully');
            await server.shutdown();
            process.exit(0);
        });
        await server.run();
    }
    catch (error) {
        logger.error('Failed to start NLQ MCP Server', {
            error: error instanceof Error ? error.message : String(error)
        }, error instanceof Error ? error : undefined);
        process.exit(1);
    }
}
// Start the server if this file is run directly
if (import.meta.url === `file://${process.argv[1]}`) {
    main().catch((error) => {
        console.error('Fatal error:', error);
        process.exit(1);
    });
}
export default NlqMcpServer;
