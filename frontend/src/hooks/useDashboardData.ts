/**
 * Dashboard Data Hook
 * 
 * Optimized TanStack Query hook for dashboard statistics with intelligent
 * caching, error handling, and real-time data synchronization.
 */

import { useQuery } from '@tanstack/react-query';
import { shipmentApi, type DashboardStats } from '@/services/shipment';
import { queryKeys } from '@/lib/queryClientConfig';

interface UseDashboardDataOptions {
  userId: number;
  enabled?: boolean;
  refetchInterval?: number;
}

interface UseDashboardDataReturn {
  data: DashboardStats | undefined;
  isLoading: boolean;
  isError: boolean;
  error: Error | null;
  refetch: () => void;
  isRefetching: boolean;
  dataUpdatedAt: number;
}

/**
 * Hook for fetching dashboard statistics with optimized caching
 * 
 * Features:
 * - Intelligent caching based on data freshness requirements
 * - Automatic error handling with user-friendly messages
 * - Real-time refetching for critical metrics
 * - Optimistic updates via cache invalidation
 */
export const useDashboardData = ({ 
  userId, 
  enabled = true,
  refetchInterval 
}: UseDashboardDataOptions): UseDashboardDataReturn => {
  const query = useQuery({
    queryKey: queryKeys.dashboard.stats(),
    queryFn: () => shipmentApi.getDashboardStats(userId),
    enabled: enabled && !!userId,
    
    // Stale time: Consider data stale after 2 minutes for dashboard
    staleTime: 1000 * 60 * 2,
    
    // Cache time: Keep in cache for 10 minutes
    gcTime: 1000 * 60 * 10,
    
    // Refetch every 5 minutes for real-time dashboard feel
    refetchInterval: refetchInterval || 1000 * 60 * 5,
    
    // Don't refetch on window focus for dashboard (can be expensive)
    refetchOnWindowFocus: false,
    
    // Refetch on reconnection to ensure data freshness
    refetchOnReconnect: true,
    
    // Retry failed requests with exponential backoff
    retry: 3,
    retryDelay: (attemptIndex) => Math.min(1000 * 2 ** attemptIndex, 30000),
    
    // Meta for error handling customization
    meta: {
      errorMessage: 'Failed to load dashboard statistics',
      critical: false, // Don't show critical notifications for dashboard
    },
  });

  return {
    data: query.data,
    isLoading: query.isLoading,
    isError: query.isError,
    error: query.error,
    refetch: query.refetch,
    isRefetching: query.isRefetching,
    dataUpdatedAt: query.dataUpdatedAt,
  };
};

/**
 * Hook for fetching user shipments with pagination support
 */
export const useUserShipments = (userId: number, enabled = true) => {
  return useQuery({
    queryKey: queryKeys.shipment.list({ userId }),
    queryFn: () => shipmentApi.getUserShipments(userId),
    enabled: enabled && !!userId,
    
    // User shipments can be cached longer
    staleTime: 1000 * 60 * 3, // 3 minutes
    gcTime: 1000 * 60 * 15, // 15 minutes
    
    // Don't auto-refetch - user will manually refresh if needed
    refetchOnWindowFocus: false,
    refetchInterval: false,
    
    meta: {
      errorMessage: 'Failed to load your shipments',
    },
  });
};

/**
 * Hook for real-time shipment tracking
 */
export const useShipmentTracking = (trackingNumber: string, enabled = true) => {
  return useQuery({
    queryKey: queryKeys.shipment.tracking(trackingNumber),
    queryFn: () => shipmentApi.getShipmentByTrackingNumber(trackingNumber),
    enabled: enabled && !!trackingNumber,
    
    // Real-time tracking needs fresh data
    staleTime: 1000 * 30, // 30 seconds
    gcTime: 1000 * 60 * 5, // 5 minutes
    
    // Refetch frequently for tracking
    refetchInterval: 1000 * 60 * 1, // Every minute
    refetchOnWindowFocus: true,
    refetchOnReconnect: true,
    
    retry: 5, // More retries for critical tracking
    
    meta: {
      errorMessage: 'Failed to load shipment tracking information',
      critical: true, // Tracking failures are critical
    },
  });
};

/**
 * Hook for shipment history with optimized caching
 */
export const useShipmentHistory = (trackingNumber: string, enabled = true) => {
  return useQuery({
    queryKey: queryKeys.shipment.history(trackingNumber),
    queryFn: () => shipmentApi.getTrackingHistory(trackingNumber),
    enabled: enabled && !!trackingNumber,
    
    // History doesn't change frequently
    staleTime: 1000 * 60 * 5, // 5 minutes
    gcTime: 1000 * 60 * 30, // 30 minutes
    
    refetchOnWindowFocus: false,
    refetchInterval: false,
    
    meta: {
      errorMessage: 'Failed to load shipment history',
    },
  });
};