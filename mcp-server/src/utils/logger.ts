/**
 * Logger Utility - 结构化日志记录
 * 
 * 提供统一的日志记录功能，支持：
 * - 结构化JSON日志输出
 * - 多级别日志记录
 * - PII数据脱敏
 * - 性能监控和指标收集
 */

// ===== Types =====
export type LogLevel = 'error' | 'warn' | 'info' | 'debug';

export interface LogEntry {
  timestamp: string;
  level: LogLevel;
  message: string;
  service: string;
  version: string;
  environment: string;
  requestId?: string;
  userId?: string;
  metadata?: Record<string, any>;
  error?: {
    name: string;
    message: string;
    stack?: string;
  };
}

export interface LoggerConfig {
  level: LogLevel;
  format: 'json' | 'text';
  piiEnabled: boolean;
  service: string;
  version: string;
  environment: string;
}

// ===== PII Masking Patterns =====
const PII_PATTERNS = {
  email: /\b[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Z|a-z]{2,}\b/g,
  phone: /\b\d{3}-?\d{3}-?\d{4}\b/g,
  creditCard: /\b\d{4}[\s-]?\d{4}[\s-]?\d{4}[\s-]?\d{4}\b/g,
  ssn: /\b\d{3}-?\d{2}-?\d{4}\b/g,
  trackingNumber: /\b1Z[0-9A-Z]{16}\b/g,
  ipAddress: /\b(?:[0-9]{1,3}\.){3}[0-9]{1,3}\b/g
};

// ===== Logger Implementation =====
class Logger {
  private config: LoggerConfig;
  private logLevels: Record<LogLevel, number> = {
    error: 0,
    warn: 1,
    info: 2,
    debug: 3
  };

  constructor() {
    // Initialize with defaults, will be updated when config is loaded
    this.config = {
      level: (process.env.LOG_LEVEL as LogLevel) || 'info',
      format: (process.env.LOG_FORMAT as 'json' | 'text') || 'json',
      piiEnabled: process.env.LOG_PII_ENABLED === 'true',
      service: process.env.MCP_SERVER_NAME || 'mini-ups-nlq-mcp-server',
      version: process.env.MCP_SERVER_VERSION || '1.0.0',
      environment: process.env.NODE_ENV || 'development'
    };
  }

  /**
   * Update logger configuration
   */
  updateConfig(config: Partial<LoggerConfig>): void {
    this.config = { ...this.config, ...config };
  }

  /**
   * Log error message
   */
  error(message: string, metadata?: Record<string, any>, error?: Error): void {
    this.log('error', message, metadata, error);
  }

  /**
   * Log warning message
   */
  warn(message: string, metadata?: Record<string, any>): void {
    this.log('warn', message, metadata);
  }

  /**
   * Log info message
   */
  info(message: string, metadata?: Record<string, any>): void {
    this.log('info', message, metadata);
  }

  /**
   * Log debug message
   */
  debug(message: string, metadata?: Record<string, any>): void {
    this.log('debug', message, metadata);
  }

  /**
   * Core logging method
   */
  private log(level: LogLevel, message: string, metadata?: Record<string, any>, error?: Error): void {
    // Check if this log level should be output
    if (this.logLevels[level] > this.logLevels[this.config.level]) {
      return;
    }

    const logEntry: LogEntry = {
      timestamp: new Date().toISOString(),
      level,
      message: this.maskPii(message),
      service: this.config.service,
      version: this.config.version,
      environment: this.config.environment
    };

    // Add metadata if provided
    if (metadata) {
      logEntry.metadata = this.maskPiiInObject(metadata);
    }

    // Add error details if provided
    if (error) {
      logEntry.error = {
        name: error.name,
        message: this.maskPii(error.message),
        stack: this.config.environment === 'development' ? error.stack : undefined
      };
    }

    // Output log entry
    this.output(logEntry);
  }

  /**
   * Output log entry to console
   */
  private output(entry: LogEntry): void {
    if (this.config.format === 'json') {
      console.log(JSON.stringify(entry));
    } else {
      const timestamp = entry.timestamp;
      const level = entry.level.toUpperCase().padEnd(5);
      const message = entry.message;
      const metadata = entry.metadata ? ` ${JSON.stringify(entry.metadata)}` : '';
      const errorInfo = entry.error ? ` ERROR: ${entry.error.message}` : '';
      
      console.log(`${timestamp} [${level}] ${message}${metadata}${errorInfo}`);
    }
  }

  /**
   * Mask PII data in strings
   */
  private maskPii(text: string): string {
    if (this.config.piiEnabled) {
      return text; // PII logging enabled, return as-is
    }

    let masked = text;
    
    // Apply all PII masking patterns
    for (const [type, pattern] of Object.entries(PII_PATTERNS)) {
      masked = masked.replace(pattern, `[${type.toUpperCase()}]`);
    }

    return masked;
  }

  /**
   * Mask PII data in objects
   */
  private maskPiiInObject(obj: Record<string, any>): Record<string, any> {
    if (this.config.piiEnabled) {
      return obj; // PII logging enabled, return as-is
    }

    const masked: Record<string, any> = {};

    for (const [key, value] of Object.entries(obj)) {
      if (typeof value === 'string') {
        masked[key] = this.maskPii(value);
      } else if (typeof value === 'object' && value !== null) {
        if (Array.isArray(value)) {
          masked[key] = value.map(item => 
            typeof item === 'string' ? this.maskPii(item) :
            typeof item === 'object' ? this.maskPiiInObject(item) : item
          );
        } else {
          masked[key] = this.maskPiiInObject(value);
        }
      } else {
        masked[key] = value;
      }
    }

    return masked;
  }

  /**
   * Create child logger with additional context
   */
  child(context: { requestId?: string; userId?: string }): ChildLogger {
    return new ChildLogger(this, context);
  }

  /**
   * Log performance metrics
   */
  metric(name: string, value: number, unit: string, metadata?: Record<string, any>): void {
    this.info('Performance metric', {
      metric: {
        name,
        value,
        unit,
        timestamp: Date.now()
      },
      ...metadata
    });
  }

  /**
   * Log API request/response
   */
  apiLog(method: string, url: string, statusCode: number, duration: number, metadata?: Record<string, any>): void {
    this.info('API request', {
      api: {
        method,
        url: this.maskPii(url),
        statusCode,
        duration
      },
      ...metadata
    });
  }

  /**
   * Log token usage for LLM calls
   */
  tokenUsage(model: string, inputTokens: number, outputTokens: number, cost: number): void {
    if (!process.env.LOG_TOKENS_ENABLED || process.env.LOG_TOKENS_ENABLED === 'false') {
      return;
    }

    this.info('Token usage', {
      llm: {
        model,
        tokens: {
          input: inputTokens,
          output: outputTokens,
          total: inputTokens + outputTokens
        },
        cost
      }
    });
  }

  /**
   * Get current configuration
   */
  getConfig(): LoggerConfig {
    return { ...this.config };
  }
}

// ===== Child Logger =====
class ChildLogger {
  constructor(
    private parent: Logger,
    private context: { requestId?: string; userId?: string }
  ) {}

  error(message: string, metadata?: Record<string, any>, error?: Error): void {
    this.parent.error(message, { ...this.context, ...metadata }, error);
  }

  warn(message: string, metadata?: Record<string, any>): void {
    this.parent.warn(message, { ...this.context, ...metadata });
  }

  info(message: string, metadata?: Record<string, any>): void {
    this.parent.info(message, { ...this.context, ...metadata });
  }

  debug(message: string, metadata?: Record<string, any>): void {
    this.parent.debug(message, { ...this.context, ...metadata });
  }

  metric(name: string, value: number, unit: string, metadata?: Record<string, any>): void {
    this.parent.metric(name, value, unit, { ...this.context, ...metadata });
  }

  apiLog(method: string, url: string, statusCode: number, duration: number, metadata?: Record<string, any>): void {
    this.parent.apiLog(method, url, statusCode, duration, { ...this.context, ...metadata });
  }
}

// ===== Utility Functions =====
export function createRequestLogger(requestId: string, userId?: string): ChildLogger {
  return logger.child({ requestId, userId });
}

export function logPerformance<T>(
  operation: string,
  fn: () => Promise<T>,
  metadata?: Record<string, any>
): Promise<T> {
  const startTime = Date.now();
  
  return fn().then(
    result => {
      const duration = Date.now() - startTime;
      logger.metric('operation_duration', duration, 'ms', {
        operation,
        success: true,
        ...metadata
      });
      return result;
    },
    error => {
      const duration = Date.now() - startTime;
      logger.metric('operation_duration', duration, 'ms', {
        operation,
        success: false,
        ...metadata
      });
      throw error;
    }
  );
}

// Export singleton logger instance
export const logger = new Logger();