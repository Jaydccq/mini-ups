/**
 * System Provider Component
 * 
 * Comprehensive provider that integrates all real-time systems:
 * - WebSocket connection management
 * - Notification synchronization  
 * - Conflict resolution
 * - Error boundaries
 * - Data synchronization
 */

import React, { useEffect, ReactNode } from 'react';
import { QueryClientProvider } from '@tanstack/react-query';
import { ReactQueryDevtools } from '@tanstack/react-query-devtools';
import { Toaster } from 'sonner';
import { queryClient } from '@/lib/queryClient';
import { useAuthStore } from '@/stores/auth-store';
import { socketService } from '@/services/socketService';
import { useNotificationSync } from '@/hooks/useNotificationSync';
import { useGlobalConflictState } from '@/hooks/useConflictResolution';
import { GlobalErrorBoundary } from '@/components/error/ErrorBoundary';
import { ConflictResolver } from '@/components/conflict/ConflictResolver';
import { NotificationCenter } from '@/components/notifications/NotificationCenter';

// WebSocket Connection Manager
const WebSocketManager: React.FC = () => {
  const { token, isAuthenticated } = useAuthStore();

  useEffect(() => {
    if (isAuthenticated && token) {
      console.log('Connecting to WebSocket...');
      socketService.connect(token);
      
      // Request notification permission on first connection
      socketService.requestNotificationPermission();
      
      return () => {
        console.log('Disconnecting from WebSocket...');
        socketService.disconnect();
      };
    }
  }, [isAuthenticated, token]);

  return null;
};

// Notification Sync Manager
const NotificationSyncManager: React.FC = () => {
  const { isAuthenticated } = useAuthStore();
  
  // This hook handles all notification synchronization
  useNotificationSync();

  // Only sync when authenticated
  if (!isAuthenticated) {
    return null;
  }

  return null;
};

// Global Conflict Resolution Manager
const ConflictResolutionManager: React.FC = () => {
  const {
    activeConflict,
    hasActiveConflict,
    isResolving,
    resolveActiveConflict,
    cancelActiveConflict,
  } = useGlobalConflictState();

  if (!hasActiveConflict || !activeConflict) {
    return null;
  }

  return (
    <ConflictResolver
      conflict={activeConflict}
      onResolve={resolveActiveConflict}
      onCancel={cancelActiveConflict}
      isLoading={isResolving}
      entityDisplayName={`${activeConflict.entityType} ${activeConflict.entityId}`}
      fieldLabels={{
        // Common field label mappings
        tracking_number: 'Tracking Number',
        delivery_address: 'Delivery Address',
        status: 'Status',
        estimated_delivery: 'Estimated Delivery',
        weight: 'Weight',
        service_level: 'Service Level',
        special_instructions: 'Special Instructions',
        contact_phone: 'Contact Phone',
        recipient_name: 'Recipient Name',
      }}
    />
  );
};

// Connection Status Display
const ConnectionStatusDisplay: React.FC = () => {
  const { isAuthenticated } = useAuthStore();
  const connectionStats = socketService.getConnectionStats();

  // Only show in development or when there are connection issues
  if (process.env.NODE_ENV === 'production' && connectionStats.status === 'connected') {
    return null;
  }

  if (!isAuthenticated) {
    return null;
  }

  return (
    <div className="fixed bottom-4 right-4 z-50">
      {connectionStats.status === 'error' && (
        <div className="bg-red-100 border border-red-300 text-red-700 px-3 py-2 rounded-lg text-sm">
          Connection failed. 
          <button 
            onClick={() => socketService.forceReconnect()}
            className="ml-2 underline hover:no-underline"
          >
            Retry
          </button>
        </div>
      )}
      
      {connectionStats.status === 'connecting' && connectionStats.reconnectAttempts > 0 && (
        <div className="bg-yellow-100 border border-yellow-300 text-yellow-700 px-3 py-2 rounded-lg text-sm">
          Reconnecting... (Attempt {connectionStats.reconnectAttempts}/{connectionStats.maxReconnectAttempts})
        </div>
      )}
      
      {process.env.NODE_ENV === 'development' && (
        <div className="bg-gray-100 border border-gray-300 text-gray-700 px-3 py-2 rounded-lg text-xs mt-2">
          Status: {connectionStats.status}
          {connectionStats.socketId && (
            <div>Socket: {connectionStats.socketId.slice(0, 8)}...</div>
          )}
        </div>
      )}
    </div>
  );
};

// Main System Provider
interface SystemProviderProps {
  children: ReactNode;
}

export const SystemProvider: React.FC<SystemProviderProps> = ({ children }) => {
  return (
    <GlobalErrorBoundary>
      <QueryClientProvider client={queryClient}>
        {/* WebSocket connection management */}
        <WebSocketManager />
        
        {/* Notification synchronization */}
        <NotificationSyncManager />
        
        {/* Global conflict resolution */}
        <ConflictResolutionManager />
        
        {/* Connection status display */}
        <ConnectionStatusDisplay />
        
        {/* Toast notifications */}
        <Toaster 
          position="top-right"
          expand={true}
          richColors={true}
          closeButton={true}
        />
        
        {/* Main application content */}
        {children}
        
        {/* React Query DevTools (development only) */}
        {process.env.NODE_ENV === 'development' && (
          <ReactQueryDevtools 
            initialIsOpen={false}
            position="bottom-right"
          />
        )}
      </QueryClientProvider>
    </GlobalErrorBoundary>
  );
};

// Enhanced App Shell with notification integration
export const AppShellWithNotifications: React.FC<{ children: ReactNode }> = ({ children }) => {
  return (
    <div className="min-h-screen bg-gray-50">
      {/* Header with notification bell */}
      <header className="bg-white border-b border-gray-200 px-4 py-3">
        <div className="flex items-center justify-between max-w-7xl mx-auto">
          <div className="flex items-center gap-4">
            <h1 className="text-xl font-semibold text-gray-900">
              Mini-UPS
            </h1>
          </div>
          
          <div className="flex items-center gap-3">
            {/* Notification Center */}
            <NotificationCenter />
            
            {/* User menu would go here */}
            <div className="h-8 w-8 bg-gray-300 rounded-full"></div>
          </div>
        </div>
      </header>
      
      {/* Main content */}
      <main className="max-w-7xl mx-auto px-4 py-6">
        {children}
      </main>
    </div>
  );
};

// Hook for system integration status
export const useSystemStatus = () => {
  const { isAuthenticated } = useAuthStore();
  const { connectionStatus, isSyncing } = useNotificationSync();
  const { conflictCount } = useGlobalConflictState();
  const socketStats = socketService.getConnectionStats();

  return {
    isAuthenticated,
    connectionStatus,
    isSyncing,
    hasConflicts: conflictCount > 0,
    conflictCount,
    socketConnected: socketStats.isConnected,
    reconnectAttempts: socketStats.reconnectAttempts,
    
    // Overall system health
    systemHealthy: isAuthenticated && 
                   socketStats.isConnected && 
                   connectionStatus === 'online' && 
                   conflictCount === 0,
  };
};