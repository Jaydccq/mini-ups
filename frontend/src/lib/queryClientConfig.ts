/**
 * Enhanced TanStack Query Configuration
 * 
 * Advanced configuration for robust data synchronization, caching strategies,
 * and error handling optimized for logistics applications.
 */

import { QueryClient, QueryCache, MutationCache, DefaultOptions } from '@tanstack/react-query';
import { toast } from 'sonner';
import { useNotificationStore } from '@/stores/notificationStore';
import { useConflictStore } from '@/stores/conflictStore';
import { Notification } from '@/types/notification';

// Cache time constants (in milliseconds)
const CACHE_TIMES = {
  // Static data that rarely changes
  STATIC: 1000 * 60 * 60 * 24, // 24 hours
  
  // Reference data that changes infrequently
  REFERENCE: 1000 * 60 * 30, // 30 minutes
  
  // Business data that changes moderately
  BUSINESS: 1000 * 60 * 5, // 5 minutes
  
  // Real-time data that changes frequently
  REALTIME: 1000 * 60 * 1, // 1 minute
  
  // Critical data that must be fresh
  CRITICAL: 1000 * 30, // 30 seconds
} as const;

// Retry configuration for different operation types
const RETRY_CONFIG = {
  // Critical operations get more retry attempts
  CRITICAL: {
    retry: 5,
    retryDelay: (attemptIndex: number) => Math.min(1000 * 2 ** attemptIndex, 30000),
  },
  
  // Standard operations
  STANDARD: {
    retry: 3,
    retryDelay: (attemptIndex: number) => Math.min(1000 * 2 ** attemptIndex, 10000),
  },
  
  // Background operations can fail gracefully
  BACKGROUND: {
    retry: 1,
    retryDelay: 1000,
  },
} as const;

// Query key factories for consistent caching
export const queryKeys = {
  // User-related queries
  user: {
    all: ['users'] as const,
    lists: () => [...queryKeys.user.all, 'list'] as const,
    list: (filters: Record<string, any>) => [...queryKeys.user.lists(), filters] as const,
    details: () => [...queryKeys.user.all, 'detail'] as const,
    detail: (id: string) => [...queryKeys.user.details(), id] as const,
    profile: () => [...queryKeys.user.all, 'profile'] as const,
  },
  
  // Shipment-related queries
  shipment: {
    all: ['shipments'] as const,
    lists: () => [...queryKeys.shipment.all, 'list'] as const,
    list: (filters: Record<string, any>) => [...queryKeys.shipment.lists(), filters] as const,
    details: () => [...queryKeys.shipment.all, 'detail'] as const,
    detail: (id: string) => [...queryKeys.shipment.details(), id] as const,
    tracking: (trackingNumber: string) => [...queryKeys.shipment.all, 'tracking', trackingNumber] as const,
    history: (trackingNumber: string) => [...queryKeys.shipment.all, 'history', trackingNumber] as const,
    comments: (shipmentId: string) => [...queryKeys.shipment.all, 'comments', shipmentId] as const,
  },
  
  // Dashboard and analytics
  dashboard: {
    all: ['dashboard'] as const,
    stats: () => [...queryKeys.dashboard.all, 'stats'] as const,
    analytics: (dateRange: { from: string; to: string }) => 
      [...queryKeys.dashboard.all, 'analytics', dateRange] as const,
  },
  
  // Admin queries
  admin: {
    all: ['admin'] as const,
    users: (filters?: Record<string, any>) => [...queryKeys.admin.all, 'users', filters] as const,
    shipments: (filters?: Record<string, any>) => [...queryKeys.admin.all, 'shipments', filters] as const,
    analytics: (filters?: Record<string, any>) => [...queryKeys.admin.all, 'analytics', filters] as const,
  },
  
  // Notification queries
  notifications: {
    all: ['notifications'] as const,
    lists: () => [...queryKeys.notifications.all, 'list'] as const,
    list: (filters?: Record<string, any>) => [...queryKeys.notifications.lists(), filters] as const,
    missed: (lastSyncId: string | null) => [...queryKeys.notifications.all, 'missed', lastSyncId] as const,
    stats: () => [...queryKeys.notifications.all, 'stats'] as const,
    preferences: () => [...queryKeys.notifications.all, 'preferences'] as const,
  },
} as const;

// Cache time mapping for different query types
const getCacheTime = (queryKey: readonly unknown[]): number => {
  const [domain, type] = queryKey;
  
  // Static reference data
  if (domain === 'config' || domain === 'constants') {
    return CACHE_TIMES.STATIC;
  }
  
  // User profile data
  if (domain === 'users' && type === 'profile') {
    return CACHE_TIMES.REFERENCE;
  }
  
  // Real-time shipment data
  if (domain === 'shipments' && (type === 'tracking' || type === 'history')) {
    return CACHE_TIMES.REALTIME;
  }
  
  // Critical admin data
  if (domain === 'admin') {
    return CACHE_TIMES.CRITICAL;
  }
  
  // Dashboard stats
  if (domain === 'dashboard') {
    return CACHE_TIMES.BUSINESS;
  }
  
  // Default to business data cache time
  return CACHE_TIMES.BUSINESS;
};

// Stale time mapping (when to consider data stale)
const getStaleTime = (queryKey: readonly unknown[]): number => {
  return getCacheTime(queryKey) / 2; // Stale time is half of cache time
};

// Retry configuration based on query type
const getRetryConfig = (queryKey: readonly unknown[]) => {
  const [domain, type] = queryKey;
  
  // Critical operations
  if (domain === 'admin' || (domain === 'shipments' && type === 'tracking')) {
    return RETRY_CONFIG.CRITICAL;
  }
  
  // Background operations
  if (domain === 'dashboard' && type === 'analytics') {
    return RETRY_CONFIG.BACKGROUND;
  }
  
  // Standard operations
  return RETRY_CONFIG.STANDARD;
};

// Error handling for queries
const createQueryCache = () => new QueryCache({
  onError: (error: Error, query) => {
    const queryKey = query.queryKey;
    const errorMessage = error.message || 'An error occurred while fetching data';
    
    console.error('Query Error:', {
      queryKey,
      error: errorMessage,
      meta: query.meta,
    });

    // Don't show toasts for background queries
    if (query.meta?.background) {
      return;
    }

    // Show user-friendly error message
    toast.error(`Failed to load data: ${errorMessage}`);

    // Create notification for critical errors
    if (query.meta?.critical) {
      const notificationStore = useNotificationStore.getState();
      const notification: Notification = {
        id: `query_error_${Date.now()}`,
        type: 'system_alert',
        priority: 'high',
        status: 'unread',
        title: 'Data Loading Error',
        message: `Failed to load ${queryKey[0]}: ${errorMessage}`,
        timestamp: new Date().toISOString(),
        data: { queryKey, error: errorMessage },
      };
      notificationStore.addNotification(notification);
    }
  },
});

// Error handling for mutations
const createMutationCache = () => new MutationCache({
  onError: (error: any, variables, context, mutation) => {
    const errorMessage = error.message || 'An operation failed';
    
    console.error('Mutation Error:', {
      error: errorMessage,
      variables,
      context,
      meta: mutation.meta,
    });

    // Handle 409 conflicts specifically
    if (error.status === 409 && error.data?.serverState) {
      const conflictStore = useConflictStore.getState();
      
      conflictStore.addConflict({
        entityId: error.data.entityId || 'unknown',
        entityType: error.data.entityType || 'unknown',
        ourVersion: error.data.ourVersion || 0,
        serverVersion: error.data.serverVersion || 0,
        ourChanges: variables,
        serverState: error.data.serverState,
        conflictType: error.data.conflictType || 'data_conflict',
        timestamp: new Date().toISOString(),
        operation: mutation.options.mutationKey?.[0] as string || 'unknown',
        operationMetadata: { variables, context },
      });
      
      toast.error('Conflict detected. Please resolve the conflict to continue.');
      return;
    }

    // Don't show toasts for silent mutations
    if (mutation.meta?.silent) {
      return;
    }

    // Show error toast
    toast.error(errorMessage);
  },

  onSuccess: (data, variables, context, mutation) => {
    // Show success message for important operations
    if (mutation.meta?.showSuccess) {
      const successMessage = mutation.meta.successMessage || 'Operation completed successfully';
      toast.success(successMessage);
    }
  },
});

// Default options for queries and mutations
const createDefaultOptions = (): DefaultOptions => ({
  queries: {
    // Dynamic stale time based on query type
    staleTime: (query) => getStaleTime(query.queryKey),
    
    // Dynamic cache time based on query type
    gcTime: (query) => getCacheTime(query.queryKey),
    
    // Don't refetch on window focus for most queries
    refetchOnWindowFocus: false,
    
    // Refetch on reconnect for critical data
    refetchOnReconnect: (query) => {
      const [domain] = query.queryKey;
      return domain === 'admin' || domain === 'shipments';
    },
    
    // Dynamic retry configuration
    retry: (failureCount, error, query) => {
      const retryConfig = getRetryConfig(query.queryKey);
      return failureCount < retryConfig.retry;
    },
    
    retryDelay: (attemptIndex, error, query) => {
      const retryConfig = getRetryConfig(query.queryKey);
      return retryConfig.retryDelay(attemptIndex);
    },
  },
  
  mutations: {
    // Default retry for mutations
    retry: 1,
    retryDelay: 1000,
  },
});

// Create and configure the query client
export const createEnhancedQueryClient = () => {
  const queryClient = new QueryClient({
    queryCache: createQueryCache(),
    mutationCache: createMutationCache(),
    defaultOptions: createDefaultOptions(),
  });

  // Add global error handlers
  queryClient.setMutationDefaults(['shipment', 'update'], {
    mutationFn: async (variables: any) => {
      // Add version checking for update operations
      if (!variables.version) {
        throw new Error('Version is required for update operations');
      }
      return variables.mutationFn(variables);
    },
  });

  return queryClient;
};

// Helper functions for cache management
export const cacheUtils = {
  // Invalidate related queries when data changes
  invalidateRelated: (queryClient: QueryClient, queryKey: readonly unknown[]) => {
    const [domain] = queryKey;
    
    switch (domain) {
      case 'shipments':
        queryClient.invalidateQueries({ queryKey: queryKeys.shipment.all });
        queryClient.invalidateQueries({ queryKey: queryKeys.dashboard.all });
        break;
        
      case 'users':
        queryClient.invalidateQueries({ queryKey: queryKeys.user.all });
        queryClient.invalidateQueries({ queryKey: queryKeys.admin.users() });
        break;
        
      default:
        queryClient.invalidateQueries({ queryKey: [domain] });
    }
  },

  // Optimistic update helper
  optimisticUpdate: <T>(
    queryClient: QueryClient,
    queryKey: readonly unknown[],
    updater: (old: T | undefined) => T
  ) => {
    const previous = queryClient.getQueryData<T>(queryKey);
    queryClient.setQueryData<T>(queryKey, updater);
    return { previous };
  },

  // Remove stale data from cache
  clearStaleData: (queryClient: QueryClient) => {
    queryClient.invalidateQueries({
      predicate: (query) => {
        const staleTime = getStaleTime(query.queryKey);
        const dataUpdatedAt = query.state.dataUpdatedAt;
        return Date.now() - dataUpdatedAt > staleTime;
      },
    });
  },
};

// Export the configured query client
export const queryClient = createEnhancedQueryClient();