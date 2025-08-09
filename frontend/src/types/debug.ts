/**
 * Debug Types
 * 
 * Purpose:
 * - TypeScript type definitions for World Simulator debugging
 * - Provides type safety for debug message data structures
 * - Ensures consistency between backend DTOs and frontend interfaces
 * - Enables better IDE support and compile-time error checking
 * 
 * Types:
 * - WorldSimulatorMessage: Individual debug message representation
 * - DebugStatistics: Performance and usage statistics
 * - ConnectionStatus: WebSocket connection state tracking
 * - MessageFilter: Message filtering and search criteria
 * 
 * @author Mini-UPS Team
 * @version 1.0.0
 */

/**
 * Direction of message flow between UPS and World Simulator
 */
export type MessageDirection = 'INBOUND' | 'OUTBOUND';

/**
 * WebSocket connection status
 */
export type ConnectionStatus = 'connecting' | 'connected' | 'disconnected' | 'error';

/**
 * Individual World Simulator debug message
 */
export interface WorldSimulatorMessage {
  /** Message flow direction */
  direction: MessageDirection;
  
  /** Timestamp when message was captured */
  timestamp: string;
  
  /** Protocol Buffer message type (e.g., "UGoPickup", "UFinished") */
  messageType: string;
  
  /** JSON representation of the protobuf message */
  jsonContent: string;
  
  /** Size of the original binary message in bytes */
  sizeBytes: number;
  
  /** Sequence number for request/response correlation (if available) */
  sequenceNumber?: number;
  
  /** Human-readable summary of the message content */
  summary: string;
}

/**
 * Debug statistics and performance metrics
 */
export interface DebugStatistics {
  /** Total number of messages processed */
  totalMessages: number;
  
  /** Number of inbound messages */
  inboundMessages: number;
  
  /** Number of outbound messages */
  outboundMessages: number;
  
  /** Number of messages currently cached in memory */
  cachedMessages: number;
  
  /** Debug session start time */
  startTime: string;
  
  /** Current timestamp */
  currentTime: string;
  
  /** Average messages per second */
  messagesPerSecond: number;
}

/**
 * System status information
 */
export interface SystemStatus {
  /** Whether debug system is enabled */
  debugEnabled: boolean;
  
  /** Maximum message buffer size */
  bufferSize: number;
  
  /** Message retention time in seconds */
  retentionSeconds: number;
  
  /** Whether World Simulator is connected */
  simulatorConnected: boolean;
  
  /** Whether connection is healthy */
  connectionHealthy: boolean;
  
  /** Current World ID */
  worldId?: number;
  
  /** Debug system uptime in seconds */
  uptime: number;
}

/**
 * Message filtering criteria
 */
export interface MessageFilter {
  /** Search term for message content */
  searchTerm: string;
  
  /** Filter by message direction */
  direction: 'ALL' | MessageDirection;
  
  /** Filter by message type */
  messageType: string;
  
  /** Date range filter */
  dateRange?: {
    start: Date;
    end: Date;
  };
}

/**
 * Export options for debug messages
 */
export interface ExportOptions {
  /** Format for exported data */
  format: 'json' | 'csv' | 'txt';
  
  /** Whether to include message details */
  includeDetails: boolean;
  
  /** Date range for export */
  dateRange?: {
    start: Date;
    end: Date;
  };
  
  /** Message types to include */
  messageTypes?: string[];
  
  /** Message directions to include */
  directions?: MessageDirection[];
}

/**
 * WebSocket message wrapper
 */
export interface WebSocketMessage<T = any> {
  /** Message type identifier */
  type: string;
  
  /** Message payload */
  data: T;
  
  /** Timestamp when message was sent */
  timestamp: string;
}

/**
 * API response wrapper for debug endpoints
 */
export interface DebugApiResponse<T = any> {
  /** Whether the request was successful */
  success: boolean;
  
  /** Response data */
  data?: T;
  
  /** Error message if request failed */
  message?: string;
  
  /** Additional error details */
  error?: string;
}

/**
 * Connection test result
 */
export interface ConnectionTestResult {
  /** Whether connection test passed */
  connectionHealthy: boolean;
  
  /** Whether World Simulator is connected */
  connected: boolean;
  
  /** Current World ID */
  worldId?: number;
  
  /** Test timestamp */
  testTimestamp: string;
}

/**
 * Message statistics by type
 */
export interface MessageTypeStats {
  /** Message type name */
  messageType: string;
  
  /** Number of messages of this type */
  count: number;
  
  /** Average message size in bytes */
  averageSize: number;
  
  /** Total bytes for this message type */
  totalBytes: number;
  
  /** Last seen timestamp */
  lastSeen: string;
}

/**
 * Performance metrics
 */
export interface PerformanceMetrics {
  /** Messages processed per second (current) */
  currentMsgPerSec: number;
  
  /** Average messages per second */
  averageMsgPerSec: number;
  
  /** Peak messages per second */
  peakMsgPerSec: number;
  
  /** Memory usage for message buffer */
  memoryUsageMB: number;
  
  /** WebSocket connection uptime */
  connectionUptimeMs: number;
  
  /** Number of connection drops */
  connectionDrops: number;
}

/**
 * Debug session configuration
 */
export interface DebugConfig {
  /** Whether to auto-connect on page load */
  autoConnect: boolean;
  
  /** Whether to auto-scroll to new messages */
  autoScroll: boolean;
  
  /** Maximum messages to keep in memory */
  maxMessages: number;
  
  /** Whether to play sound on new messages */
  soundEnabled: boolean;
  
  /** Message types to highlight */
  highlightTypes: string[];
  
  /** Whether to show timestamps */
  showTimestamps: boolean;
  
  /** Timestamp format preference */
  timestampFormat: '12h' | '24h' | 'relative';
}