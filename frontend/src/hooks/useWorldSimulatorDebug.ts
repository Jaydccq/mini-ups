/**
 * World Simulator Debug Hook
 * 
 * Purpose:
 * - Custom React hook for World Simulator debugging functionality
 * - Manages WebSocket connection for real-time message streaming
 * - Provides debug message state management and API interactions
 * - Handles connection lifecycle and error recovery
 * 
 * Features:
 * - Real-time WebSocket message streaming
 * - Automatic connection management with retry logic
 * - Message history with bounded buffer
 * - Statistics tracking and performance metrics
 * - REST API integration for debug controls
 * - Error handling and toast notifications
 * - Connection health monitoring
 * 
 * WebSocket Topics:
 * - /topic/admin/world-simulator-debug - Live message stream
 * - /topic/admin/world-simulator-stats - Statistics updates
 * 
 * API Endpoints:
 * - GET /api/admin/debug/simulator/messages - Message history
 * - GET /api/admin/debug/simulator/stats - Current statistics  
 * - POST /api/admin/debug/simulator/clear - Clear message cache
 * - GET /api/admin/debug/simulator/status - System status
 * 
 * @author Mini-UPS Team
 * @version 1.0.0
 */
import { useState, useEffect, useCallback, useRef } from 'react';
import { Client } from '@stomp/stompjs';
import SockJS from 'sockjs-client';
import { useToast } from '@/hooks/use-toast';
import { useAuth } from '@/hooks/useAuth';
import { WorldSimulatorMessage, DebugStatistics } from '@/types/debug';

const MAX_MESSAGES = 5000; // Keep last 5000 messages in memory
const WEBSOCKET_URL = process.env.NODE_ENV === 'production' 
  ? '/ws' 
  : 'http://localhost:8081/ws';

export function useWorldSimulatorDebug() {
  const [messages, setMessages] = useState<WorldSimulatorMessage[]>([]);
  const [statistics, setStatistics] = useState<DebugStatistics | null>(null);
  const [isConnected, setIsConnected] = useState(false);
  const [connectionStatus, setConnectionStatus] = useState<'connecting' | 'connected' | 'disconnected' | 'error'>('disconnected');
  const [worldId, setWorldId] = useState<number | null>(null);
  const [sessionConnected, setSessionConnected] = useState(false);
  const [sessionConnecting, setSessionConnecting] = useState(false);
  
  const clientRef = useRef<Client | null>(null);
  const { toast } = useToast();
  const { token, user } = useAuth();

  // Initialize WebSocket connection
  const connect = useCallback(() => {
    if (!token || !user || user.role !== 'ADMIN') {
      toast({
        title: "Access Denied",
        description: "Admin role required for World Simulator debugging.",
        variant: "destructive",
      });
      return;
    }

    if (clientRef.current?.connected) {
      return; // Already connected
    }

    setConnectionStatus('connecting');

    const client = new Client({
      webSocketFactory: () => new SockJS(WEBSOCKET_URL),
      connectHeaders: {
        Authorization: `Bearer ${token}`,
      },
      debug: (str) => {
        console.debug('STOMP Debug:', str);
      },
      reconnectDelay: 5000,
      heartbeatIncoming: 10000,
      heartbeatOutgoing: 10000,
    });

    client.onConnect = (frame) => {
      console.log('World Simulator Debug WebSocket connected:', frame);
      setIsConnected(true);
      setConnectionStatus('connected');

      // Subscribe to debug message stream
      client.subscribe('/topic/admin/world-simulator-debug', (message) => {
        try {
          const debugMessage: WorldSimulatorMessage = JSON.parse(message.body);
          
          setMessages(prev => {
            const updated = [...prev, debugMessage];
            // Keep only the last MAX_MESSAGES
            if (updated.length > MAX_MESSAGES) {
              updated.splice(0, updated.length - MAX_MESSAGES);
            }
            return updated;
          });
        } catch (error) {
          console.error('Error parsing debug message:', error);
          toast({
            title: "Message Parse Error",
            description: "Failed to parse incoming debug message",
            variant: "destructive",
          });
        }
      });

      // Subscribe to statistics updates
      client.subscribe('/topic/admin/world-simulator-stats', (message) => {
        try {
          const stats: DebugStatistics = JSON.parse(message.body);
          setStatistics(stats);
        } catch (error) {
          console.error('Error parsing statistics:', error);
        }
      });

      // Subscribe to reset notifications
      client.subscribe('/topic/admin/world-simulator-debug/reset', (message) => {
        setMessages([]);
        toast({
          title: "Messages Cleared",
          description: "Debug message cache has been cleared.",
        });
      });

      // Load initial message history
      loadMessageHistory();
      loadStatistics();
    };

    client.onStompError = (frame) => {
      console.error('STOMP error:', frame);
      setIsConnected(false);
      setConnectionStatus('error');
      toast({
        title: "Connection Error",
        description: "Failed to connect to World Simulator debug stream",
        variant: "destructive",
      });
    };

    client.onDisconnect = () => {
      console.log('World Simulator Debug WebSocket disconnected');
      setIsConnected(false);
      setConnectionStatus('disconnected');
    };

    client.onWebSocketError = (error) => {
      console.error('WebSocket error:', error);
      setConnectionStatus('error');
    };

    clientRef.current = client;
    client.activate();
  }, [token, user, toast]);

  // Disconnect WebSocket
  const disconnect = useCallback(() => {
    if (clientRef.current?.connected) {
      clientRef.current.deactivate();
    }
    setIsConnected(false);
    setConnectionStatus('disconnected');
  }, []);

  // Load message history from API
  const loadMessageHistory = useCallback(async (limit: number = 100) => {
    try {
      const response = await fetch(`/api/admin/debug/simulator/messages?limit=${limit}`, {
        headers: {
          'Authorization': `Bearer ${token}`,
        },
      });

      if (response.ok) {
        const result = await response.json();
        if (result.success && Array.isArray(result.data)) {
          setMessages(result.data);
        }
      }
    } catch (error) {
      console.error('Failed to load message history:', error);
    }
  }, [token]);

  // Load current statistics
  const loadStatistics = useCallback(async () => {
    try {
      const response = await fetch('/api/admin/debug/simulator/stats', {
        headers: {
          'Authorization': `Bearer ${token}`,
        },
      });

      if (response.ok) {
        const result = await response.json();
        if (result.success) {
          setStatistics(result.data);
        }
      }
    } catch (error) {
      console.error('Failed to load statistics:', error);
    }
  }, [token]);

  // Clear all messages
  const clearMessages = useCallback(async () => {
    try {
      const response = await fetch('/api/admin/debug/simulator/clear', {
        method: 'POST',
        headers: {
          'Authorization': `Bearer ${token}`,
          'Content-Type': 'application/json',
        },
      });

      if (response.ok) {
        setMessages([]);
        await loadStatistics();
        return true;
      } else {
        throw new Error('Failed to clear messages');
      }
    } catch (error) {
      console.error('Failed to clear messages:', error);
      throw error;
    }
  }, [token, loadStatistics]);

  // Export messages
  const exportMessages = useCallback(async () => {
    try {
      const dataStr = JSON.stringify(messages, null, 2);
      const dataBlob = new Blob([dataStr], { type: 'application/json' });
      const url = URL.createObjectURL(dataBlob);
      
      const link = document.createElement('a');
      link.href = url;
      link.download = `world-simulator-debug-${new Date().toISOString().split('T')[0]}.json`;
      document.body.appendChild(link);
      link.click();
      document.body.removeChild(link);
      URL.revokeObjectURL(url);
      
      return true;
    } catch (error) {
      console.error('Failed to export messages:', error);
      throw error;
    }
  }, [messages]);

  // Test connection
  const testConnection = useCallback(async () => {
    try {
      const response = await fetch('/api/admin/debug/simulator/test-connection', {
        method: 'POST',
        headers: {
          'Authorization': `Bearer ${token}`,
          'Content-Type': 'application/json',
        },
      });

      if (response.ok) {
        const result = await response.json();
        return result.data;
      } else {
        throw new Error('Connection test failed');
      }
    } catch (error) {
      console.error('Connection test failed:', error);
      throw error;
    }
  }, [token]);

  // Auto-connect when hook mounts and user is admin
  useEffect(() => {
    if (user?.role === 'ADMIN' && token) {
      connect();
    }

    // Cleanup on unmount
    return () => {
      if (clientRef.current?.connected) {
        clientRef.current.deactivate();
      }
    };
  }, [user, token, connect]);

  // Fetch current status including world ID
  const fetchStatus = useCallback(async () => {
    try {
      const response = await fetch('/world/status', {
        headers: {
          'Authorization': `Bearer ${token}`,
        },
      });

      if (response.ok) {
        const result = await response.json();
        
        // Update world ID and connection status
        if (result.world_id) {
          setWorldId(result.world_id);
          setSessionConnected(true);
        } else {
          setWorldId(null);
          setSessionConnected(false);
        }
        
        // Update connection status based on actual backend status
        const backendConnected = result.connected || false;
        
        if (backendConnected) {
          setConnectionStatus('connected');
          setIsConnected(true);
        } else {
          setConnectionStatus('disconnected');
          setIsConnected(false);
        }
        
        return result;
      } else {
        throw new Error('Failed to fetch status');
      }
    } catch (error) {
      console.error('Failed to fetch status:', error);
      setConnectionStatus('error');
      setIsConnected(false);
      setWorldId(null);
      throw error;
    }
  }, [token]);

  // Connect to World Simulator session
  const connectSimulatorSession = useCallback(async (inputWorldId?: number) => {
    if (!token || !user || user.role !== 'ADMIN') {
      throw new Error('Admin role required for World Simulator connection.');
    }

    setSessionConnecting(true);
    
    try {
      const body: any = {};
      if (inputWorldId !== undefined && inputWorldId !== 0) {
        body.world_id = inputWorldId;
      }

      const response = await fetch('/world/connect', {
        method: 'POST',
        headers: {
          'Authorization': `Bearer ${token}`,
          'Content-Type': 'application/json',
        },
        body: JSON.stringify(body),
      });

      if (response.ok) {
        const result = await response.json();
        
        if (result.success) {
          setWorldId(result.world_id);
          setSessionConnected(true);
          setSessionConnecting(false);
          
          // Also start debug WebSocket if not already connected
          if (!isConnected) {
            connect();
          }
          
          return {
            success: true,
            worldId: result.world_id,
            message: result.message,
          };
        } else {
          throw new Error(result.message || 'Connection failed');
        }
      } else {
        const errorResult = await response.json().catch(() => ({ message: 'Connection failed' }));
        throw new Error(errorResult.message || 'Connection failed');
      }
    } catch (error) {
      setSessionConnected(false);
      setSessionConnecting(false);
      throw error;
    }
  }, [token, user, isConnected, connect]);

  // Disconnect from World Simulator session  
  const disconnectSimulatorSession = useCallback(async () => {
    if (!token) return;

    try {
      const response = await fetch('/world/disconnect', {
        method: 'POST',
        headers: {
          'Authorization': `Bearer ${token}`,
        },
      });

      setSessionConnected(false);
      setWorldId(null);
      
      // Also disconnect debug WebSocket
      if (isConnected) {
        disconnect();
      }

      return response.ok;
    } catch (error) {
      console.error('Failed to disconnect session:', error);
      return false;
    }
  }, [token, isConnected, disconnect]);

  // Fetch status on mount
  useEffect(() => {
    if (user?.role === 'ADMIN' && token) {
      fetchStatus();
    }
  }, [user, token, fetchStatus]);

  // Periodic statistics and status refresh
  useEffect(() => {
    if (!isConnected) return;

    const interval = setInterval(() => {
      loadStatistics();
      fetchStatus(); // Also refresh status to get latest world ID
    }, 30000); // Every 30 seconds

    return () => clearInterval(interval);
  }, [isConnected, loadStatistics, fetchStatus]);

  return {
    messages,
    statistics,
    isConnected,
    connectionStatus,
    worldId,
    sessionConnected,
    sessionConnecting,
    connect,
    disconnect,
    connectSimulatorSession,
    disconnectSimulatorSession,
    clearMessages,
    exportMessages,
    testConnection,
    loadMessageHistory,
    loadStatistics,
    fetchStatus,
  };
}