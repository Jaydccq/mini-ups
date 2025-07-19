/**
 * WebSocket Service for Real-time Communication
 * 
 * Features:
 * - STOMP protocol over WebSocket connection
 * - Automatic reconnection with exponential backoff
 * - Connection state management
 * - Type-safe message handling
 * - Authentication header support
 * 
 * Usage:
 * - Connect to WebSocket server
 * - Subscribe to specific topics
 * - Handle incoming messages
 * - Automatic cleanup on disconnect
 * 
 * @author Mini-UPS Team
 * @version 1.0.0
 */

import { Client, IMessage, StompSubscription } from '@stomp/stompjs';

interface WebSocketConfig {
  url: string;
  reconnectDelay?: number;
  heartbeatIncoming?: number;
  heartbeatOutgoing?: number;
}

interface SubscriptionConfig {
  destination: string;
  callback: (message: any) => void;
  headers?: Record<string, string>;
}

export class WebSocketService {
  private client: Client;
  private subscriptions: Map<string, StompSubscription> = new Map();
  private connected = false;
  private connecting = false;
  private reconnectAttempts = 0;
  private maxReconnectAttempts = 5;
  private reconnectDelay = 1000;
  private onConnectionStateChange?: (connected: boolean) => void;

  constructor(config: WebSocketConfig) {
    this.client = new Client({
      brokerURL: config.url,
      connectHeaders: {
        Authorization: `Bearer ${localStorage.getItem('token') || ''}`,
      },
      heartbeatIncoming: config.heartbeatIncoming || 10000,
      heartbeatOutgoing: config.heartbeatOutgoing || 10000,
      reconnectDelay: config.reconnectDelay || 5000,
      debug: (str) => {
        console.log('WebSocket Debug:', str);
      },
    });

    this.client.onConnect = this.handleConnect.bind(this);
    this.client.onDisconnect = this.handleDisconnect.bind(this);
    this.client.onStompError = this.handleError.bind(this);
    this.client.onWebSocketClose = this.handleWebSocketClose.bind(this);
  }

  /**
   * Connect to WebSocket server
   */
  connect(): Promise<void> {
    if (this.connected || this.connecting) {
      return Promise.resolve();
    }

    this.connecting = true;
    
    return new Promise((resolve, reject) => {
      const originalOnConnect = this.client.onConnect;
      const originalOnStompError = this.client.onStompError;

      this.client.onConnect = (frame) => {
        originalOnConnect?.(frame);
        resolve();
      };

      this.client.onStompError = (frame) => {
        originalOnStompError?.(frame);
        reject(new Error(`WebSocket connection failed: ${frame.headers.message}`));
      };

      try {
        this.client.activate();
      } catch (error) {
        this.connecting = false;
        reject(error);
      }
    });
  }

  /**
   * Disconnect from WebSocket server
   */
  disconnect(): void {
    if (this.client) {
      this.client.deactivate();
    }
  }

  /**
   * Subscribe to a topic
   */
  subscribe(config: SubscriptionConfig): () => void {
    if (!this.connected) {
      console.warn('WebSocket not connected, queuing subscription');
      // Queue subscription for when connection is established
      setTimeout(() => {
        if (this.connected) {
          this.subscribe(config);
        }
      }, 1000);
      return () => {};
    }

    const subscription = this.client.subscribe(
      config.destination,
      (message: IMessage) => {
        try {
          const data = JSON.parse(message.body);
          config.callback(data);
        } catch (error) {
          console.error('Error parsing WebSocket message:', error);
          config.callback(message.body);
        }
      },
      config.headers
    );

    this.subscriptions.set(config.destination, subscription);

    // Return unsubscribe function
    return () => {
      subscription.unsubscribe();
      this.subscriptions.delete(config.destination);
    };
  }

  /**
   * Send message to server
   */
  send(destination: string, body: any, headers?: Record<string, string>): void {
    if (!this.connected) {
      console.warn('WebSocket not connected, message not sent');
      return;
    }

    this.client.publish({
      destination,
      body: JSON.stringify(body),
      headers,
    });
  }

  /**
   * Get connection state
   */
  isConnected(): boolean {
    return this.connected;
  }

  /**
   * Set connection state change callback
   */
  onConnectionChange(callback: (connected: boolean) => void): void {
    this.onConnectionStateChange = callback;
  }

  /**
   * Handle successful connection
   */
  private handleConnect(frame: any): void {
    console.log('WebSocket connected:', frame);
    this.connected = true;
    this.connecting = false;
    this.reconnectAttempts = 0;
    this.onConnectionStateChange?.(true);
  }

  /**
   * Handle disconnection
   */
  private handleDisconnect(): void {
    console.log('WebSocket disconnected');
    this.connected = false;
    this.connecting = false;
    this.subscriptions.clear();
    this.onConnectionStateChange?.(false);
  }

  /**
   * Handle STOMP errors
   */
  private handleError(frame: any): void {
    console.error('WebSocket STOMP error:', frame);
    this.connected = false;
    this.connecting = false;
    this.onConnectionStateChange?.(false);
  }

  /**
   * Handle WebSocket close
   */
  private handleWebSocketClose(event: any): void {
    console.log('WebSocket closed:', event);
    this.connected = false;
    this.connecting = false;
    
    // Attempt reconnection with exponential backoff
    if (this.reconnectAttempts < this.maxReconnectAttempts) {
      const delay = this.reconnectDelay * Math.pow(2, this.reconnectAttempts);
      this.reconnectAttempts++;
      
      console.log(`Attempting to reconnect in ${delay}ms (attempt ${this.reconnectAttempts})`);
      
      setTimeout(() => {
        this.connect().catch(console.error);
      }, delay);
    } else {
      console.error('Max reconnection attempts reached');
    }
  }
}

// Create singleton instance
let webSocketService: WebSocketService | null = null;

export const getWebSocketService = (): WebSocketService => {
  if (!webSocketService) {
    const wsUrl = import.meta.env.VITE_WS_URL || 'ws://localhost:8081/ws';
    webSocketService = new WebSocketService({
      url: wsUrl,
      reconnectDelay: 5000,
      heartbeatIncoming: 10000,
      heartbeatOutgoing: 10000,
    });
  }
  return webSocketService;
};

export default WebSocketService;