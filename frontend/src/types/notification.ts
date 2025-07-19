/**
 * Notification System Types
 * 
 * Comprehensive type definitions for the real-time notification system
 * including support for offline caching, deduplication, and conflict resolution.
 */

export type NotificationType = 
  | 'shipment_status'
  | 'shipment_created'
  | 'shipment_updated'
  | 'system_alert'
  | 'user_message'
  | 'conflict_resolution'
  | 'delivery_confirmation';

export type NotificationPriority = 'low' | 'medium' | 'high' | 'critical';

export type NotificationStatus = 'unread' | 'read' | 'archived';

export interface Notification {
  id: string; // Unique identifier for deduplication
  type: NotificationType;
  priority: NotificationPriority;
  status: NotificationStatus;
  title: string;
  message: string;
  timestamp: string; // ISO 8601 timestamp
  data?: Record<string, any>; // Additional context data
  actions?: NotificationAction[];
  expiresAt?: string; // ISO 8601 timestamp for auto-expiry
  userId?: string; // Target user (null for system-wide notifications)
  relatedEntityId?: string; // e.g., shipment tracking number
  relatedEntityType?: string; // e.g., 'shipment', 'user', 'system'
}

export interface NotificationAction {
  id: string;
  label: string;
  variant: 'primary' | 'secondary' | 'destructive';
  action: 'navigate' | 'api_call' | 'dismiss' | 'custom';
  payload?: Record<string, any>;
}

export interface NotificationFilters {
  types?: NotificationType[];
  priorities?: NotificationPriority[];
  status?: NotificationStatus;
  dateFrom?: string;
  dateTo?: string;
  relatedEntityType?: string;
}

export interface NotificationStore {
  // Notification storage (keyed by ID for deduplication)
  notifications: Record<string, Notification>;
  
  // Last synced notification ID for REST API sync
  lastSyncId: string | null;
  
  // Connection status
  connectionStatus: 'online' | 'offline' | 'syncing';
  
  // Filters and UI state
  filters: NotificationFilters;
  isNotificationCenterOpen: boolean;
  unreadCount: number;
  
  // Actions
  addNotification: (notification: Notification) => void;
  addNotifications: (notifications: Notification[]) => void;
  markAsRead: (notificationId: string) => void;
  markAllAsRead: () => void;
  removeNotification: (notificationId: string) => void;
  archiveNotification: (notificationId: string) => void;
  clearExpiredNotifications: () => void;
  setConnectionStatus: (status: 'online' | 'offline' | 'syncing') => void;
  setLastSyncId: (id: string) => void;
  setFilters: (filters: Partial<NotificationFilters>) => void;
  toggleNotificationCenter: () => void;
  getFilteredNotifications: () => Notification[];
  getUnreadCount: () => number;
}

// API response types
export interface NotificationSyncResponse {
  notifications: Notification[];
  hasMore: boolean;
  lastId: string;
}

export interface NotificationPreferences {
  enablePushNotifications: boolean;
  enableEmailNotifications: boolean;
  enableSMSNotifications: boolean;
  notificationTypes: Record<NotificationType, boolean>;
  quietHoursStart?: string; // HH:MM format
  quietHoursEnd?: string; // HH:MM format
}