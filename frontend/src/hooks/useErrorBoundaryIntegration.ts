/**
 * Error Boundary Integration Hook
 * 
 * Provides utilities for integrating error boundaries throughout the application
 * and handling errors in a consistent manner.
 */

import { useCallback } from 'react';
import { useErrorReporting } from '@/components/error/ErrorBoundary';
import { useNotificationStore } from '@/stores/notificationStore';
import { Notification } from '@/types/notification';

export interface ErrorContext {
  component?: string;
  action?: string;
  userId?: string;
  additionalData?: Record<string, any>;
}

export const useErrorBoundaryIntegration = () => {
  const { reportError } = useErrorReporting();
  const notificationStore = useNotificationStore();

  // Enhanced error reporting with notifications
  const reportErrorWithNotification = useCallback((
    error: Error, 
    context: ErrorContext = {},
    showNotification = true
  ) => {
    // Report to monitoring service
    reportError(error, context);

    // Create user notification for serious errors
    if (showNotification) {
      const notification: Notification = {
        id: `error_${Date.now()}`,
        type: 'system_alert',
        priority: 'high',
        status: 'unread',
        title: 'Application Error',
        message: `An error occurred in ${context.component || 'the application'}: ${error.message}`,
        timestamp: new Date().toISOString(),
        data: {
          error: error.message,
          component: context.component,
          action: context.action,
        },
        actions: [{
          id: 'report_bug',
          label: 'Report Bug',
          variant: 'secondary',
          action: 'navigate',
          payload: { url: '/support' },
        }],
      };
      
      notificationStore.addNotification(notification);
    }
  }, [reportError, notificationStore]);

  // Handle async errors (Promise rejections, etc.)
  const handleAsyncError = useCallback((
    error: Error,
    context: ErrorContext = {}
  ) => {
    console.error('Async Error:', error, context);
    reportErrorWithNotification(error, {
      ...context,
      action: context.action || 'async_operation',
    });
  }, [reportErrorWithNotification]);

  // Handle API errors specifically
  const handleApiError = useCallback((
    error: any,
    endpoint: string,
    method: string = 'GET'
  ) => {
    const apiError = new Error(
      error.message || `API Error: ${method} ${endpoint} failed`
    );
    
    reportErrorWithNotification(apiError, {
      component: 'API',
      action: `${method} ${endpoint}`,
      additionalData: {
        status: error.status,
        statusText: error.statusText,
        response: error.data,
      },
    });
  }, [reportErrorWithNotification]);

  return {
    reportError: reportErrorWithNotification,
    handleAsyncError,
    handleApiError,
  };
};

// HOC for automatic error boundary integration
export const withErrorBoundary = <P extends object>(
  WrappedComponent: React.ComponentType<P>,
  boundaryProps: {
    level: 'route' | 'widget';
    name: string;
    essential?: boolean;
  }
) => {
  const { RouteErrorBoundary, WidgetErrorBoundary } = require('@/components/error/ErrorBoundary');
  
  const BoundaryComponent = boundaryProps.level === 'route' 
    ? RouteErrorBoundary 
    : WidgetErrorBoundary;

  return (props: P) => (
    <BoundaryComponent
      routeName={boundaryProps.level === 'route' ? boundaryProps.name : undefined}
      widgetName={boundaryProps.level === 'widget' ? boundaryProps.name : undefined}
      essential={boundaryProps.essential}
    >
      <WrappedComponent {...props} />
    </BoundaryComponent>
  );
};

// Error boundary integration for specific UI patterns
export const useErrorBoundaryPatterns = () => {
  const { reportError } = useErrorBoundaryIntegration();

  // Pattern for data fetching components
  const wrapDataFetch = useCallback(async <T>(
    fetchFn: () => Promise<T>,
    componentName: string
  ): Promise<T> => {
    try {
      return await fetchFn();
    } catch (error) {
      reportError(error as Error, {
        component: componentName,
        action: 'data_fetch',
      });
      throw error; // Re-throw for React Query error handling
    }
  }, [reportError]);

  // Pattern for user actions
  const wrapUserAction = useCallback(async <T>(
    actionFn: () => Promise<T>,
    actionName: string,
    componentName: string
  ): Promise<T> => {
    try {
      return await actionFn();
    } catch (error) {
      reportError(error as Error, {
        component: componentName,
        action: actionName,
      });
      throw error;
    }
  }, [reportError]);

  return {
    wrapDataFetch,
    wrapUserAction,
  };
};