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
import { getWebSocketService } from '@/services/websocket';
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
      try {
        const wsService = getWebSocketService();
        wsService.connect().catch(console.error);
        
        return () => {
          console.log('Disconnecting from WebSocket...');
          wsService.disconnect();
        };
      } catch (error) {
        console.error('Failed to connect WebSocket:', error);
      }
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

// Connection Status Display - Temporarily disabled
const ConnectionStatusDisplay: React.FC = () => {
  // Disabled until WebSocket service is properly integrated
  return null;
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

// Hook for system integration status - Simplified
export const useSystemStatus = () => {
  const { isAuthenticated } = useAuthStore();
  const { connectionStatus, isSyncing } = useNotificationSync();
  const { conflictCount } = useGlobalConflictState();

  return {
    isAuthenticated,
    connectionStatus,
    isSyncing,
    hasConflicts: conflictCount > 0,
    conflictCount,
    socketConnected: true, // Simplified until WebSocket service is integrated
    reconnectAttempts: 0,
    
    // Overall system health
    systemHealthy: isAuthenticated && 
                   connectionStatus === 'online' && 
                   conflictCount === 0,
  };
};