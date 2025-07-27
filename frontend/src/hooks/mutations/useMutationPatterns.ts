/**
 * Mutation Patterns for Logistics Applications
 * 
 * Implements optimistic vs pessimistic update patterns based on business risk.
 * Critical for data integrity in logistics operations where errors have real-world consequences.
 */

import { useMutation, useQueryClient, UseMutationOptions } from '@tanstack/react-query';
import { toast } from 'sonner';
import { useNotificationStore } from '@/stores/notificationStore';
import { Notification } from '@/types/notification';

// Risk classification for different types of mutations
export type MutationRisk = 'low' | 'medium' | 'high' | 'critical';

export type OptimisticMutationConfig<TData, TError, TVariables, TContext> = {
  mutationFn: (variables: TVariables) => Promise<TData>;
  onMutate?: (variables: TVariables) => Promise<TContext> | TContext;
  onSuccess?: (data: TData, variables: TVariables, context: TContext | undefined) => void;
  onError?: (error: TError, variables: TVariables, context: TContext | undefined) => void;
  successMessage?: string | ((data: TData, variables: TVariables) => string);
  errorMessage?: string | ((error: TError, variables: TVariables) => string);
  invalidateQueries?: string[][];
  updateCache?: (data: TData, variables: TVariables) => void;
  rollback?: (variables: TVariables, context: TContext | undefined) => void;
};

export type PessimisticMutationConfig<TData, TError, TVariables> = {
  mutationFn: (variables: TVariables) => Promise<TData>;
  onSuccess?: (data: TData, variables: TVariables) => void;
  onError?: (error: TError, variables: TVariables) => void;
  successMessage?: string | ((data: TData, variables: TVariables) => string);
  errorMessage?: string | ((error: TError, variables: TVariables) => string);
  confirmationMessage?: string | ((variables: TVariables) => string);
  requiresConfirmation?: boolean;
  invalidateQueries?: string[][];
  loadingMessage?: string;
  criticalOperation?: boolean;
};

/**
 * Optimistic Mutation Hook
 * 
 * For low-risk operations where immediate UI feedback improves UX.
 * Examples: Adding comments, updating preferences, non-critical edits.
 */
export function useOptimisticMutation<TData = unknown, TError = Error, TVariables = void, TContext = unknown>(
  config: OptimisticMutationConfig<TData, TError, TVariables, TContext>
) {
  const queryClient = useQueryClient();
  const notificationStore = useNotificationStore();

  return useMutation<TData, TError, TVariables, TContext>({
    mutationFn: config.mutationFn,
    
    onMutate: async (variables) => {
      // Execute custom onMutate if provided
      let context: TContext | undefined;
      if (config.onMutate) {
        context = await config.onMutate(variables);
      }

      // Cancel any outgoing refetches
      if (config.invalidateQueries) {
        await Promise.all(
          config.invalidateQueries.map(queryKey =>
            queryClient.cancelQueries({ queryKey })
          )
        );
      }

      // Apply optimistic update if provided
      if (config.updateCache) {
        // For optimistic updates, we create a mock success response
        // This is a simplified approach - in real implementations,
        // you'd provide the expected data structure
        const optimisticData = variables as unknown as TData;
        config.updateCache(optimisticData, variables);
      }

      return context;
    },

    onSuccess: (data, variables, context) => {
      // Show success message
      if (config.successMessage) {
        const message = typeof config.successMessage === 'function' 
          ? config.successMessage(data, variables)
          : config.successMessage;
        toast.success(message);
      }

      // Invalidate and refetch queries
      if (config.invalidateQueries) {
        config.invalidateQueries.forEach(queryKey => {
          queryClient.invalidateQueries({ queryKey });
        });
      }

      // Execute custom onSuccess
      if (config.onSuccess) {
        config.onSuccess(data, variables, context);
      }
    },

    onError: (error, variables, context) => {
      // Rollback optimistic update
      if (config.rollback && context) {
        config.rollback(variables, context);
      }

      // Show error message
      const message = config.errorMessage
        ? typeof config.errorMessage === 'function'
          ? config.errorMessage(error, variables)
          : config.errorMessage
        : 'Operation failed. Please try again.';
      
      toast.error(message);

      // Create error notification for serious issues
      const notification: Notification = {
        id: `error_${Date.now()}`,
        type: 'system_alert',
        priority: 'medium',
        status: 'unread',
        title: 'Operation Failed',
        message: message,
        timestamp: new Date().toISOString(),
        data: { error, variables },
      };
      notificationStore.addNotification(notification);

      // Execute custom onError
      if (config.onError) {
        config.onError(error, variables, context);
      }

      // Refetch to ensure data consistency
      if (config.invalidateQueries) {
        config.invalidateQueries.forEach(queryKey => {
          queryClient.invalidateQueries({ queryKey });
        });
      }
    },

    onSettled: () => {
      // Always refetch after error or success to ensure consistency
      if (config.invalidateQueries) {
        config.invalidateQueries.forEach(queryKey => {
          queryClient.invalidateQueries({ queryKey });
        });
      }
    },
  });
}

/**
 * Pessimistic Mutation Hook
 * 
 * For high-risk operations where data integrity is paramount.
 * Examples: Status changes, delivery confirmations, financial transactions.
 * Shows loading state immediately and only updates UI after server confirmation.
 */
export function usePessimisticMutation<TData = unknown, TError = Error, TVariables = void>(
  config: PessimisticMutationConfig<TData, TError, TVariables>
) {
  const queryClient = useQueryClient();
  const notificationStore = useNotificationStore();

  return useMutation<TData, TError, TVariables>({
    mutationFn: async (variables) => {
      // Show confirmation dialog for critical operations
      if (config.requiresConfirmation) {
        const confirmMessage = config.confirmationMessage
          ? typeof config.confirmationMessage === 'function'
            ? config.confirmationMessage(variables)
            : config.confirmationMessage
          : 'Are you sure you want to perform this action?';
        
        const confirmed = window.confirm(confirmMessage);
        if (!confirmed) {
          throw new Error('Operation cancelled by user');
        }
      }

      // Show loading message for critical operations
      if (config.loadingMessage) {
        toast.loading(config.loadingMessage, { id: 'pessimistic-loading' });
      }

      try {
        const result = await config.mutationFn(variables);
        
        // Dismiss loading toast
        if (config.loadingMessage) {
          toast.dismiss('pessimistic-loading');
        }
        
        return result;
      } catch (error) {
        // Dismiss loading toast on error
        if (config.loadingMessage) {
          toast.dismiss('pessimistic-loading');
        }
        throw error;
      }
    },

    onSuccess: (data, variables) => {
      // Show success message
      if (config.successMessage) {
        const message = typeof config.successMessage === 'function'
          ? config.successMessage(data, variables)
          : config.successMessage;
        toast.success(message);
      }

      // Create success notification for critical operations
      if (config.criticalOperation) {
        const notification: Notification = {
          id: `success_${Date.now()}`,
          type: 'system_alert',
          priority: 'medium',
          status: 'unread',
          title: 'Critical Operation Completed',
          message: 'The operation was completed successfully.',
          timestamp: new Date().toISOString(),
          data: { data, variables },
        };
        notificationStore.addNotification(notification);
      }

      // Invalidate and refetch queries
      if (config.invalidateQueries) {
        config.invalidateQueries.forEach(queryKey => {
          queryClient.invalidateQueries({ queryKey });
        });
      }

      // Execute custom onSuccess
      if (config.onSuccess) {
        config.onSuccess(data, variables);
      }
    },

    onError: (error, variables) => {
      // Show error message
      const message = config.errorMessage
        ? typeof config.errorMessage === 'function'
          ? config.errorMessage(error, variables)
          : config.errorMessage
        : 'Critical operation failed. Please contact support if this persists.';
      
      toast.error(message);

      // Create high-priority error notification
      const notification: Notification = {
        id: `critical_error_${Date.now()}`,
        type: 'system_alert',
        priority: config.criticalOperation ? 'critical' : 'high',
        status: 'unread',
        title: 'Critical Operation Failed',
        message: message,
        timestamp: new Date().toISOString(),
        data: { error, variables },
        actions: [{
          id: 'contact_support',
          label: 'Contact Support',
          variant: 'primary',
          action: 'navigate',
          payload: { url: '/support' },
        }],
      };
      notificationStore.addNotification(notification);

      // Execute custom onError
      if (config.onError) {
        config.onError(error, variables);
      }
    },
  });
}

/**
 * Risk Assessment Helper
 * 
 * Helps developers choose the appropriate mutation pattern based on operation risk.
 */
export function assessMutationRisk(operation: string): MutationRisk {
  // Critical operations - require pessimistic updates
  const criticalOperations = [
    'mark_delivered',
    'confirm_payment',
    'cancel_shipment',
    'assign_driver',
    'update_shipment_status',
    'delete_shipment',
    'refund_payment',
  ];

  // High-risk operations - require pessimistic updates
  const highRiskOperations = [
    'update_delivery_address',
    'change_service_level',
    'update_shipment_weight',
    'assign_truck',
    'update_user_role',
  ];

  // Medium-risk operations - can use optimistic with careful rollback
  const mediumRiskOperations = [
    'add_shipment_note',
    'update_tracking_preferences',
    'update_contact_info',
    'add_address',
  ];

  // Low-risk operations - safe for optimistic updates
  const lowRiskOperations = [
    'add_comment',
    'update_display_preferences',
    'mark_notification_read',
    'update_profile_picture',
    'add_favorite',
  ];

  if (criticalOperations.includes(operation)) return 'critical';
  if (highRiskOperations.includes(operation)) return 'high';
  if (mediumRiskOperations.includes(operation)) return 'medium';
  if (lowRiskOperations.includes(operation)) return 'low';

  // Default to high risk for unknown operations (fail-safe)
  return 'high';
}

/**
 * Smart Mutation Hook
 * 
 * Automatically chooses optimistic or pessimistic pattern based on operation risk.
 */
export function useSmartMutation<TData = unknown, TError = Error, TVariables = void, TContext = unknown>(
  operation: string,
  optimisticConfig?: OptimisticMutationConfig<TData, TError, TVariables, TContext>,
  pessimisticConfig?: PessimisticMutationConfig<TData, TError, TVariables>
) {
  const risk = assessMutationRisk(operation);
  const isOptimisticOperation = risk === 'low' || risk === 'medium';
  
  // Always call both hooks to avoid conditional hook calls
  const optimisticMutation = useOptimisticMutation(optimisticConfig || {} as OptimisticMutationConfig<TData, TError, TVariables, TContext>);
  const pessimisticMutation = usePessimisticMutation(pessimisticConfig || {} as PessimisticMutationConfig<TData, TError, TVariables>);
  
  if (isOptimisticOperation) {
    if (!optimisticConfig) {
      throw new Error(`Optimistic config required for ${risk} risk operation: ${operation}`);
    }
    return {
      mutation: optimisticMutation,
      pattern: 'optimistic' as const,
      risk,
    };
  } else {
    if (!pessimisticConfig) {
      throw new Error(`Pessimistic config required for ${risk} risk operation: ${operation}`);
    }
    return {
      mutation: pessimisticMutation,
      pattern: 'pessimistic' as const,
      risk,
    };
  }
}