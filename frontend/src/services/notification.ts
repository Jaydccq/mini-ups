/**
 * Notification API Service
 * 
 * Handles REST API calls for notification synchronization, preferences,
 * and missed message recovery following the stateless message queue pattern.
 */

import { apiClient } from './api';
import { 
  Notification, 
  NotificationSyncResponse, 
  NotificationPreferences,
  NotificationFilters 
} from '@/types/notification';

export class NotificationApi {
  /**
   * Fetch missed notifications since the last sync point
   * This is the critical method for the REST API sync pattern
   */
  async fetchMissedNotifications(lastSyncId: string | null): Promise<NotificationSyncResponse> {
    const params = new URLSearchParams();
    if (lastSyncId) {
      params.append('since', lastSyncId);
    }
    params.append('limit', '100'); // Reasonable batch size

    const response = await apiClient.get(`/api/notifications/sync?${params}`);
    return response.data;
  }

  /**
   * Fetch notifications with pagination and filtering
   */
  async getNotifications(
    page: number = 1,
    limit: number = 20,
    filters?: NotificationFilters
  ): Promise<{
    notifications: Notification[];
    total: number;
    page: number;
    limit: number;
    hasMore: boolean;
  }> {
    const params = new URLSearchParams({
      page: page.toString(),
      limit: limit.toString(),
    });

    if (filters) {
      if (filters.types?.length) {
        params.append('types', filters.types.join(','));
      }
      if (filters.priorities?.length) {
        params.append('priorities', filters.priorities.join(','));
      }
      if (filters.status) {
        params.append('status', filters.status);
      }
      if (filters.dateFrom) {
        params.append('dateFrom', filters.dateFrom);
      }
      if (filters.dateTo) {
        params.append('dateTo', filters.dateTo);
      }
      if (filters.relatedEntityType) {
        params.append('relatedEntityType', filters.relatedEntityType);
      }
    }

    const response = await apiClient.get(`/api/notifications?${params}`);
    return response.data;
  }

  /**
   * Mark a notification as read
   */
  async markAsRead(notificationId: string): Promise<void> {
    await apiClient.patch(`/api/notifications/${notificationId}/read`);
  }

  /**
   * Mark multiple notifications as read
   */
  async markMultipleAsRead(notificationIds: string[]): Promise<void> {
    await apiClient.patch('/api/notifications/bulk/read', {
      notificationIds,
    });
  }

  /**
   * Mark all notifications as read
   */
  async markAllAsRead(): Promise<void> {
    await apiClient.patch('/api/notifications/read-all');
  }

  /**
   * Archive a notification
   */
  async archiveNotification(notificationId: string): Promise<void> {
    await apiClient.patch(`/api/notifications/${notificationId}/archive`);
  }

  /**
   * Delete a notification
   */
  async deleteNotification(notificationId: string): Promise<void> {
    await apiClient.delete(`/api/notifications/${notificationId}`);
  }

  /**
   * Get notification statistics
   */
  async getNotificationStats(): Promise<{
    unreadCount: number;
    totalCount: number;
    countByType: Record<string, number>;
    countByPriority: Record<string, number>;
  }> {
    const response = await apiClient.get('/api/notifications/stats');
    return response.data;
  }

  /**
   * Execute a notification action
   */
  async executeNotificationAction(
    notificationId: string,
    actionId: string,
    payload?: Record<string, any>
  ): Promise<void> {
    await apiClient.post(`/api/notifications/${notificationId}/actions/${actionId}`, {
      payload,
    });
  }

  /**
   * Get user notification preferences
   */
  async getNotificationPreferences(): Promise<NotificationPreferences> {
    const response = await apiClient.get('/api/user/notification-preferences');
    return response.data;
  }

  /**
   * Update user notification preferences
   */
  async updateNotificationPreferences(
    preferences: Partial<NotificationPreferences>
  ): Promise<NotificationPreferences> {
    const response = await apiClient.patch('/api/user/notification-preferences', preferences);
    return response.data;
  }

  /**
   * Test notification delivery (for admin/debug purposes)
   */
  async sendTestNotification(
    type: string,
    message: string,
    targetUserId?: string
  ): Promise<void> {
    await apiClient.post('/api/notifications/test', {
      type,
      message,
      targetUserId,
    });
  }

  /**
   * Subscribe to push notifications (browser notifications)
   */
  async subscribeToPushNotifications(subscription: PushSubscription): Promise<void> {
    await apiClient.post('/api/notifications/push/subscribe', {
      subscription: subscription.toJSON(),
    });
  }

  /**
   * Unsubscribe from push notifications
   */
  async unsubscribeFromPushNotifications(): Promise<void> {
    await apiClient.post('/api/notifications/push/unsubscribe');
  }
}

export const notificationApi = new NotificationApi();