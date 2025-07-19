/**
 * Notification Store
 * 
 * Zustand store for managing real-time notifications with offline support,
 * deduplication, and synchronization capabilities.
 */

import { create } from 'zustand';
import { persist, subscribeWithSelector } from 'zustand/middleware';
import { Notification, NotificationStore, NotificationFilters } from '@/types/notification';

const initialFilters: NotificationFilters = {
  status: 'unread',
};

export const useNotificationStore = create<NotificationStore>()(
  persist(
    subscribeWithSelector(
      (set, get) => ({
        // State
        notifications: {},
        lastSyncId: null,
        connectionStatus: 'offline',
        filters: initialFilters,
        isNotificationCenterOpen: false,
        unreadCount: 0,

        // Actions
        addNotification: (notification: Notification) => {
          set((state) => {
            // Deduplication - use notification ID as key
            const updatedNotifications = {
              ...state.notifications,
              [notification.id]: notification,
            };

            // Update unread count
            const unreadCount = Object.values(updatedNotifications).filter(
              n => n.status === 'unread'
            ).length;

            return {
              notifications: updatedNotifications,
              unreadCount,
            };
          });
        },

        addNotifications: (notifications: Notification[]) => {
          set((state) => {
            const updatedNotifications = { ...state.notifications };
            
            // Add all notifications (deduplication by ID)
            notifications.forEach(notification => {
              updatedNotifications[notification.id] = notification;
            });

            // Update unread count
            const unreadCount = Object.values(updatedNotifications).filter(
              n => n.status === 'unread'
            ).length;

            // Update last sync ID to the latest notification
            const latestNotification = notifications.sort(
              (a, b) => new Date(b.timestamp).getTime() - new Date(a.timestamp).getTime()
            )[0];

            return {
              notifications: updatedNotifications,
              unreadCount,
              lastSyncId: latestNotification?.id || state.lastSyncId,
            };
          });
        },

        markAsRead: (notificationId: string) => {
          set((state) => {
            const notification = state.notifications[notificationId];
            if (!notification) return state;

            const updatedNotification = { ...notification, status: 'read' as const };
            const updatedNotifications = {
              ...state.notifications,
              [notificationId]: updatedNotification,
            };

            const unreadCount = Object.values(updatedNotifications).filter(
              n => n.status === 'unread'
            ).length;

            return {
              notifications: updatedNotifications,
              unreadCount,
            };
          });
        },

        markAllAsRead: () => {
          set((state) => {
            const updatedNotifications = { ...state.notifications };
            Object.keys(updatedNotifications).forEach(id => {
              if (updatedNotifications[id].status === 'unread') {
                updatedNotifications[id] = {
                  ...updatedNotifications[id],
                  status: 'read',
                };
              }
            });

            return {
              notifications: updatedNotifications,
              unreadCount: 0,
            };
          });
        },

        removeNotification: (notificationId: string) => {
          set((state) => {
            const updatedNotifications = { ...state.notifications };
            delete updatedNotifications[notificationId];

            const unreadCount = Object.values(updatedNotifications).filter(
              n => n.status === 'unread'
            ).length;

            return {
              notifications: updatedNotifications,
              unreadCount,
            };
          });
        },

        archiveNotification: (notificationId: string) => {
          set((state) => {
            const notification = state.notifications[notificationId];
            if (!notification) return state;

            const updatedNotification = { ...notification, status: 'archived' as const };
            const updatedNotifications = {
              ...state.notifications,
              [notificationId]: updatedNotification,
            };

            const unreadCount = Object.values(updatedNotifications).filter(
              n => n.status === 'unread'
            ).length;

            return {
              notifications: updatedNotifications,
              unreadCount,
            };
          });
        },

        clearExpiredNotifications: () => {
          set((state) => {
            const now = new Date();
            const updatedNotifications = { ...state.notifications };
            
            Object.keys(updatedNotifications).forEach(id => {
              const notification = updatedNotifications[id];
              if (notification.expiresAt && new Date(notification.expiresAt) < now) {
                delete updatedNotifications[id];
              }
            });

            const unreadCount = Object.values(updatedNotifications).filter(
              n => n.status === 'unread'
            ).length;

            return {
              notifications: updatedNotifications,
              unreadCount,
            };
          });
        },

        setConnectionStatus: (status: 'online' | 'offline' | 'syncing') => {
          set({ connectionStatus: status });
        },

        setLastSyncId: (id: string) => {
          set({ lastSyncId: id });
        },

        setFilters: (filters: Partial<NotificationFilters>) => {
          set((state) => ({
            filters: { ...state.filters, ...filters },
          }));
        },

        toggleNotificationCenter: () => {
          set((state) => ({
            isNotificationCenterOpen: !state.isNotificationCenterOpen,
          }));
        },

        getFilteredNotifications: () => {
          const state = get();
          const notifications = Object.values(state.notifications);
          const { filters } = state;

          return notifications
            .filter(notification => {
              // Filter by status
              if (filters.status && notification.status !== filters.status) {
                return false;
              }

              // Filter by types
              if (filters.types && filters.types.length > 0) {
                if (!filters.types.includes(notification.type)) {
                  return false;
                }
              }

              // Filter by priorities
              if (filters.priorities && filters.priorities.length > 0) {
                if (!filters.priorities.includes(notification.priority)) {
                  return false;
                }
              }

              // Filter by date range
              if (filters.dateFrom) {
                if (new Date(notification.timestamp) < new Date(filters.dateFrom)) {
                  return false;
                }
              }

              if (filters.dateTo) {
                if (new Date(notification.timestamp) > new Date(filters.dateTo)) {
                  return false;
                }
              }

              // Filter by related entity type
              if (filters.relatedEntityType && notification.relatedEntityType !== filters.relatedEntityType) {
                return false;
              }

              return true;
            })
            .sort((a, b) => new Date(b.timestamp).getTime() - new Date(a.timestamp).getTime());
        },

        getUnreadCount: () => {
          const state = get();
          return Object.values(state.notifications).filter(
            n => n.status === 'unread'
          ).length;
        },
      })
    ),
    {
      name: 'notification-store',
      // Only persist essential data, not UI state
      partialize: (state) => ({
        notifications: state.notifications,
        lastSyncId: state.lastSyncId,
        filters: state.filters,
      }),
    }
  )
);

// Cleanup expired notifications every 5 minutes
setInterval(() => {
  useNotificationStore.getState().clearExpiredNotifications();
}, 5 * 60 * 1000);