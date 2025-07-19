/**
 * Notification Synchronization Hook
 * 
 * Integrates the notification system with TanStack Query for seamless
 * data synchronization and real-time updates.
 */

import { useEffect } from 'react';
import { useQuery, useQueryClient } from '@tanstack/react-query';
import { useNotificationStore } from '@/stores/notificationStore';
import { socketService } from '@/services/socketService';
import { notificationApi } from '@/services/notification';
import { queryKeys } from '@/lib/queryClientConfig';

export const useNotificationSync = () => {
  const queryClient = useQueryClient();
  const {
    lastSyncId,
    connectionStatus,
    setConnectionStatus,
    addNotifications,
    setLastSyncId,
  } = useNotificationStore();

  // Query for syncing missed notifications
  const missedNotificationsQuery = useQuery({
    queryKey: queryKeys.notifications.missed(lastSyncId),
    queryFn: () => notificationApi.fetchMissedNotifications(lastSyncId),
    enabled: connectionStatus === 'online' && !!lastSyncId,
    refetchOnMount: true,
    refetchOnReconnect: true,
    retry: 3,
    retryDelay: (attempt) => Math.min(1000 * 2 ** attempt, 10000),
    meta: {
      background: true, // Don't show error toasts for this query
    },
  });

  // Process missed notifications when query succeeds
  useEffect(() => {
    if (missedNotificationsQuery.data) {
      const { notifications, lastId } = missedNotificationsQuery.data;
      
      if (notifications.length > 0) {
        console.log(`Synced ${notifications.length} missed notifications`);
        addNotifications(notifications);
        setLastSyncId(lastId);
      }
    }
  }, [missedNotificationsQuery.data, addNotifications, setLastSyncId]);

  // Listen for WebSocket connection status changes
  useEffect(() => {
    const unsubscribe = socketService.onStatusChange((status) => {
      if (status === 'connected') {
        setConnectionStatus('online');
        // Trigger sync of missed notifications
        queryClient.invalidateQueries({ 
          queryKey: queryKeys.notifications.missed(lastSyncId) 
        });
      } else if (status === 'syncing') {
        setConnectionStatus('syncing');
      } else {
        setConnectionStatus('offline');
      }
    });

    return unsubscribe;
  }, [setConnectionStatus, queryClient, lastSyncId]);

  return {
    connectionStatus,
    isSyncing: missedNotificationsQuery.isFetching,
    syncError: missedNotificationsQuery.error,
    lastSyncTime: missedNotificationsQuery.dataUpdatedAt,
    
    // Manual sync trigger
    triggerSync: () => {
      queryClient.invalidateQueries({ 
        queryKey: queryKeys.notifications.missed(lastSyncId) 
      });
    },
  };
};

/**
 * Hook for managing notification preferences with TanStack Query
 */
export const useNotificationPreferences = () => {
  const queryClient = useQueryClient();

  const preferencesQuery = useQuery({
    queryKey: queryKeys.notifications.preferences(),
    queryFn: notificationApi.getNotificationPreferences,
    staleTime: 1000 * 60 * 30, // 30 minutes
  });

  const updatePreferences = async (preferences: any) => {
    // Optimistically update cache
    queryClient.setQueryData(
      queryKeys.notifications.preferences(),
      (old: any) => ({ ...old, ...preferences })
    );

    try {
      const updated = await notificationApi.updateNotificationPreferences(preferences);
      
      // Update cache with server response
      queryClient.setQueryData(queryKeys.notifications.preferences(), updated);
      
      return updated;
    } catch (error) {
      // Revert optimistic update on error
      queryClient.invalidateQueries({ 
        queryKey: queryKeys.notifications.preferences() 
      });
      throw error;
    }
  };

  return {
    preferences: preferencesQuery.data,
    isLoading: preferencesQuery.isLoading,
    error: preferencesQuery.error,
    updatePreferences,
  };
};

/**
 * Hook for notification statistics
 */
export const useNotificationStats = () => {
  return useQuery({
    queryKey: queryKeys.notifications.stats(),
    queryFn: notificationApi.getNotificationStats,
    refetchInterval: 1000 * 60 * 5, // Refetch every 5 minutes
    staleTime: 1000 * 60 * 2, // Consider stale after 2 minutes
  });
};