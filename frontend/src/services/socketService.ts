import { io, Socket } from 'socket.io-client';
import { queryClient } from '@/lib/queryClient';
import { Shipment } from './shipment';
import type { Notification } from '@/types/notification';
import { useNotificationStore } from '@/stores/notificationStore';
import { notificationApi } from './notification';

export type SocketStatus = 'disconnected' | 'connecting' | 'connected' | 'error' | 'syncing';

class SocketService {
  private socket: Socket | null = null;
  private status: SocketStatus = 'disconnected';
  private listeners: ((status: SocketStatus) => void)[] = [];
  private reconnectAttempts = 0;
  private maxReconnectAttempts = 5;
  private reconnectDelay = 1000; // Start with 1 second
  private reconnectTimer: NodeJS.Timeout | null = null;
  private heartbeatInterval: NodeJS.Timeout | null = null;

  getStatus(): SocketStatus {
    return this.status;
  }

  onStatusChange(listener: (status: SocketStatus) => void) {
    this.listeners.push(listener);
    return () => {
      this.listeners = this.listeners.filter(l => l !== listener);
    };
  }

  private updateStatus(newStatus: SocketStatus) {
    this.status = newStatus;
    this.listeners.forEach(listener => listener(newStatus));
    
    // Update notification store connection status
    const notificationStore = useNotificationStore.getState();
    if (newStatus === 'connected') {
      notificationStore.setConnectionStatus('online');
    } else if (newStatus === 'syncing') {
      notificationStore.setConnectionStatus('syncing');
    } else {
      notificationStore.setConnectionStatus('offline');
    }
  }

  private startHeartbeat() {
    if (this.heartbeatInterval) {
      clearInterval(this.heartbeatInterval);
    }
    
    this.heartbeatInterval = setInterval(() => {
      if (this.socket?.connected) {
        this.socket.emit('ping');
      }
    }, 30000); // Ping every 30 seconds
  }

  private stopHeartbeat() {
    if (this.heartbeatInterval) {
      clearInterval(this.heartbeatInterval);
      this.heartbeatInterval = null;
    }
  }

  private async syncMissedNotifications() {
    try {
      this.updateStatus('syncing');
      const notificationStore = useNotificationStore.getState();
      const lastSyncId = notificationStore.lastSyncId;
      
      console.log('Syncing missed notifications since:', lastSyncId);
      
      const syncResponse = await notificationApi.fetchMissedNotifications(lastSyncId);
      
      if (syncResponse.notifications.length > 0) {
        console.log(`Retrieved ${syncResponse.notifications.length} missed notifications`);
        notificationStore.addNotifications(syncResponse.notifications);
        notificationStore.setLastSyncId(syncResponse.lastId);
      }
      
      this.updateStatus('connected');
    } catch (error) {
      console.error('Failed to sync missed notifications:', error);
      // Don't fail the connection for sync errors
      this.updateStatus('connected');
    }
  }

  private scheduleReconnect() {
    if (this.reconnectTimer) {
      clearTimeout(this.reconnectTimer);
    }

    if (this.reconnectAttempts >= this.maxReconnectAttempts) {
      console.error('Max reconnection attempts reached');
      this.updateStatus('error');
      return;
    }

    const delay = Math.min(this.reconnectDelay * Math.pow(2, this.reconnectAttempts), 30000);
    console.log(`Scheduling reconnection attempt ${this.reconnectAttempts + 1} in ${delay}ms`);
    
    this.reconnectTimer = setTimeout(() => {
      this.reconnectAttempts++;
      this.attemptReconnect();
    }, delay);
  }

  private attemptReconnect() {
    if (this.socket) {
      console.log('Attempting to reconnect...');
      this.socket.connect();
    }
  }

  connect(authToken: string) {
    if (this.socket?.connected) {
      return;
    }

    const VITE_API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8081';
    console.log('Connecting to WebSocket:', VITE_API_BASE_URL);

    this.updateStatus('connecting');
    this.reconnectAttempts = 0; // Reset reconnection attempts

    this.socket = io(VITE_API_BASE_URL, {
      auth: {
        token: authToken,
      },
      transports: ['websocket', 'polling'], // fallback support
      timeout: 10000, // 10 second timeout
      forceNew: true,
    });

    this.socket.on('connect', async () => {
      console.log('Socket.IO connected');
      this.reconnectAttempts = 0; // Reset on successful connection
      this.startHeartbeat();
      
      // Sync missed notifications after reconnection
      await this.syncMissedNotifications();
    });

    this.socket.on('disconnect', (reason) => {
      console.log('Socket.IO disconnected:', reason);
      this.stopHeartbeat();
      this.updateStatus('disconnected');
      
      // Only auto-reconnect for non-intentional disconnects
      if (reason === 'io server disconnect') {
        // Server initiated disconnect, don't reconnect automatically
        return;
      }
      
      this.scheduleReconnect();
    });

    this.socket.on('connect_error', (error) => {
      console.error('Socket.IO connection error:', error);
      this.updateStatus('error');
      this.scheduleReconnect();
    });

    this.socket.on('pong', () => {
      // Heartbeat response - connection is healthy
      console.debug('Received pong from server');
    });

    // Handle real-time notifications
    this.socket.on('notification', (notification: Notification) => {
      console.log('Received real-time notification:', notification);
      
      const notificationStore = useNotificationStore.getState();
      notificationStore.addNotification(notification);
      notificationStore.setLastSyncId(notification.id);
      
      // Show browser notification if permission granted
      this.showBrowserNotification(notification);
    });

    // Handle shipment updates
    this.socket.on('shipment_update', (updatedShipment: Shipment) => {
      console.log('Received shipment update:', updatedShipment);
      
      // Update the cache for the detailed shipment query
      queryClient.setQueryData(
        ['shipment', updatedShipment.tracking_number], 
        updatedShipment
      );

      // Invalidate shipments list to trigger refetch
      queryClient.invalidateQueries({ 
        queryKey: ['shipments'],
        type: 'active' 
      });

      // Invalidate dashboard stats
      queryClient.invalidateQueries({ 
        queryKey: ['dashboardStats'],
        type: 'active' 
      });

      // Create a notification for the shipment update
      const notification: Notification = {
        id: `shipment_${updatedShipment.tracking_number}_${Date.now()}`,
        type: 'shipment_status',
        priority: 'medium',
        status: 'unread',
        title: 'Shipment Status Updated',
        message: `Shipment ${updatedShipment.tracking_number} is now ${updatedShipment.status}`,
        timestamp: new Date().toISOString(),
        relatedEntityId: updatedShipment.tracking_number,
        relatedEntityType: 'shipment',
        data: {
          shipment: updatedShipment,
        },
        actions: [{
          id: 'view_shipment',
          label: 'View Details',
          variant: 'primary',
          action: 'navigate',
          payload: { 
            url: `/shipments/tracking/${updatedShipment.tracking_number}` 
          },
        }],
      };

      const notificationStore = useNotificationStore.getState();
      notificationStore.addNotification(notification);
    });

    // Handle tracking history updates
    this.socket.on('tracking_update', (data: { trackingNumber: string; history: any[] }) => {
      console.log('Received tracking update:', data);
      
      // Update tracking history cache
      queryClient.setQueryData(
        ['trackingHistory', data.trackingNumber],
        { history: data.history }
      );
    });

    // Handle system alerts
    this.socket.on('system_alert', (alert: {
      id: string;
      message: string;
      priority: 'low' | 'medium' | 'high' | 'critical';
      expiresAt?: string;
    }) => {
      console.log('Received system alert:', alert);
      
      const notification: Notification = {
        id: alert.id,
        type: 'system_alert',
        priority: alert.priority,
        status: 'unread',
        title: 'System Alert',
        message: alert.message,
        timestamp: new Date().toISOString(),
        expiresAt: alert.expiresAt,
      };

      const notificationStore = useNotificationStore.getState();
      notificationStore.addNotification(notification);
      this.showBrowserNotification(notification);
    });
  }

  private showBrowserNotification(notification: Notification) {
    if ('Notification' in window && Notification.permission === 'granted') {
      const browserNotification = new Notification(notification.title, {
        body: notification.message,
        icon: '/favicon.ico',
        tag: notification.id, // Prevent duplicate notifications
        requireInteraction: notification.priority === 'critical',
      });

      browserNotification.onclick = () => {
        window.focus();
        browserNotification.close();
        
        // Execute primary action if available
        const primaryAction = notification.actions?.find(a => a.variant === 'primary');
        if (primaryAction?.action === 'navigate' && primaryAction.payload?.url) {
          window.location.href = primaryAction.payload.url;
        }
      };

      // Auto-close non-critical notifications after 5 seconds
      if (notification.priority !== 'critical') {
        setTimeout(() => {
          browserNotification.close();
        }, 5000);
      }
    }
  }

  disconnect() {
    if (this.reconnectTimer) {
      clearTimeout(this.reconnectTimer);
      this.reconnectTimer = null;
    }
    
    this.stopHeartbeat();
    
    if (this.socket) {
      this.socket.removeAllListeners();
      this.socket.disconnect();
      this.socket = null;
      this.updateStatus('disconnected');
    }
    
    this.reconnectAttempts = 0;
  }

  emit(event: string, data: unknown) {
    if (this.socket?.connected) {
      this.socket.emit(event, data);
    } else {
      console.warn('Socket not connected, cannot emit event:', event);
    }
  }

  subscribeToShipment(trackingNumber: string) {
    console.log(`Subscribing to shipment ${trackingNumber}`);
    this.emit('subscribe_shipment', { trackingNumber });
  }

  unsubscribeFromShipment(trackingNumber: string) {
    console.log(`Unsubscribing from shipment ${trackingNumber}`);
    this.emit('unsubscribe_shipment', { trackingNumber });
  }

  // Manual reconnection trigger
  forceReconnect() {
    console.log('Force reconnecting...');
    this.reconnectAttempts = 0;
    if (this.socket) {
      this.socket.disconnect();
      this.socket.connect();
    }
  }

  // Request browser notification permission
  async requestNotificationPermission(): Promise<NotificationPermission> {
    if (!('Notification' in window)) {
      console.warn('This browser does not support notifications');
      return 'denied';
    }

    if (Notification.permission === 'default') {
      const permission = await Notification.requestPermission();
      return permission;
    }

    return Notification.permission;
  }

  // Get connection statistics
  getConnectionStats() {
    return {
      status: this.status,
      reconnectAttempts: this.reconnectAttempts,
      maxReconnectAttempts: this.maxReconnectAttempts,
      isConnected: this.socket?.connected || false,
      socketId: this.socket?.id || null,
    };
  }
}

export const socketService = new SocketService();