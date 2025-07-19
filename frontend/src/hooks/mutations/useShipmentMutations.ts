/**
 * Shipment-Specific Mutation Hooks
 * 
 * Demonstrates practical implementation of optimistic vs pessimistic patterns
 * for different shipment operations based on their business risk.
 */

import { useOptimisticMutation, usePessimisticMutation, useSmartMutation } from './useMutationPatterns';
import { shipmentApi, Shipment, CreateShipmentRequest, UpdateShipmentRequest } from '@/services/shipment';
import { useQueryClient } from '@tanstack/react-query';

// Low-risk: Adding comments to shipments (optimistic)
export function useAddShipmentComment() {
  const queryClient = useQueryClient();
  
  return useOptimisticMutation({
    mutationFn: ({ shipmentId, comment }: { shipmentId: string; comment: string }) =>
      shipmentApi.addComment(shipmentId, comment),
    
    successMessage: 'Comment added successfully',
    errorMessage: 'Failed to add comment',
    
    invalidateQueries: [
      ['shipment'],
      ['shipmentComments'],
    ],

    onMutate: async (variables) => {
      // Cancel outgoing refetches
      await queryClient.cancelQueries({ queryKey: ['shipmentComments', variables.shipmentId] });
      
      // Snapshot previous value
      const previousComments = queryClient.getQueryData(['shipmentComments', variables.shipmentId]);
      
      // Optimistically add comment
      const optimisticComment = {
        id: `temp_${Date.now()}`,
        comment: variables.comment,
        createdAt: new Date().toISOString(),
        author: 'You',
      };
      
      queryClient.setQueryData(
        ['shipmentComments', variables.shipmentId],
        (old: any[]) => old ? [...old, optimisticComment] : [optimisticComment]
      );

      return { previousComments };
    },

    rollback: (variables, context) => {
      // Restore previous comments state
      if (context?.previousComments) {
        queryClient.setQueryData(['shipmentComments', variables.shipmentId], context.previousComments);
      }
    },
  });
}

// Medium-risk: Updating shipment preferences (optimistic with careful rollback)
export function useUpdateShipmentPreferences() {
  return useOptimisticMutation({
    mutationFn: ({ shipmentId, preferences }: { 
      shipmentId: string; 
      preferences: { notifications?: boolean; tracking?: boolean } 
    }) => shipmentApi.updatePreferences(shipmentId, preferences),
    
    successMessage: 'Preferences updated successfully',
    errorMessage: 'Failed to update preferences',
    
    invalidateQueries: [
      ['shipment'],
      ['userPreferences'],
    ],

    onMutate: async (variables) => {
      const queryClient = useQueryClient();
      
      // Cancel outgoing refetches
      await queryClient.cancelQueries({ queryKey: ['shipment', variables.shipmentId] });
      
      // Snapshot the previous value
      const previousShipment = queryClient.getQueryData(['shipment', variables.shipmentId]);
      
      // Optimistically update
      queryClient.setQueryData(['shipment', variables.shipmentId], (old: Shipment) => ({
        ...old,
        preferences: { ...old.preferences, ...variables.preferences },
      }));

      return { previousShipment };
    },

    rollback: (variables, context) => {
      const queryClient = useQueryClient();
      if (context?.previousShipment) {
        queryClient.setQueryData(['shipment', variables.shipmentId], context.previousShipment);
      }
    },
  });
}

// High-risk: Updating shipment status (pessimistic)
export function useUpdateShipmentStatus() {
  return usePessimisticMutation({
    mutationFn: ({ trackingNumber, status, comment }: { 
      trackingNumber: string; 
      status: string; 
      comment?: string 
    }) => shipmentApi.updateStatus(trackingNumber, status, comment),
    
    successMessage: (data, variables) => 
      `Shipment ${variables.trackingNumber} status updated to ${variables.status}`,
    errorMessage: 'Critical: Failed to update shipment status',
    
    confirmationMessage: (variables) => 
      `Are you sure you want to update shipment ${variables.trackingNumber} to ${variables.status}? This action cannot be undone.`,
    
    requiresConfirmation: true,
    loadingMessage: 'Updating shipment status...',
    
    invalidateQueries: [
      ['shipment'],
      ['shipments'],
      ['dashboardStats'],
      ['trackingHistory'],
    ],
  });
}

// Critical: Marking shipment as delivered (pessimistic with extra safeguards)
export function useMarkShipmentDelivered() {
  return usePessimisticMutation({
    mutationFn: ({ trackingNumber, deliveryConfirmation }: { 
      trackingNumber: string; 
      deliveryConfirmation: {
        recipientName: string;
        deliveryTime: string;
        signature?: string;
        photo?: string;
        notes?: string;
      }
    }) => shipmentApi.markDelivered(trackingNumber, deliveryConfirmation),
    
    successMessage: (data, variables) => 
      `Shipment ${variables.trackingNumber} marked as delivered successfully`,
    errorMessage: 'CRITICAL ERROR: Failed to mark shipment as delivered. Please contact support immediately.',
    
    confirmationMessage: (variables) => 
      `CRITICAL ACTION: Are you absolutely sure you want to mark shipment ${variables.trackingNumber} as DELIVERED? This will:\n\n` +
      `• Update the shipment status to DELIVERED\n` +
      `• Trigger payment processing\n` +
      `• Send delivery confirmation to customer\n` +
      `• Close the shipment tracking\n\n` +
      `This action CANNOT be undone. Confirm delivery?`,
    
    requiresConfirmation: true,
    criticalOperation: true,
    loadingMessage: 'Processing delivery confirmation...',
    
    invalidateQueries: [
      ['shipment'],
      ['shipments'],
      ['dashboardStats'],
      ['trackingHistory'],
      ['userShipments'],
    ],
  });
}

// Critical: Canceling shipment (pessimistic with extra safeguards)
export function useCancelShipment() {
  return usePessimisticMutation({
    mutationFn: ({ trackingNumber, reason }: { trackingNumber: string; reason: string }) =>
      shipmentApi.cancelShipment(trackingNumber, reason),
    
    successMessage: (data, variables) => 
      `Shipment ${variables.trackingNumber} has been cancelled`,
    errorMessage: 'CRITICAL ERROR: Failed to cancel shipment. Please contact support immediately.',
    
    confirmationMessage: (variables) => 
      `CRITICAL ACTION: Cancel shipment ${variables.trackingNumber}?\n\n` +
      `This will:\n` +
      `• Stop all tracking and delivery attempts\n` +
      `• Trigger refund processing\n` +
      `• Notify customer of cancellation\n` +
      `• Archive the shipment\n\n` +
      `This action CANNOT be undone. Proceed with cancellation?`,
    
    requiresConfirmation: true,
    criticalOperation: true,
    loadingMessage: 'Processing shipment cancellation...',
    
    invalidateQueries: [
      ['shipment'],
      ['shipments'],
      ['dashboardStats'],
      ['trackingHistory'],
    ],
  });
}

// Smart mutation: Creating new shipment (automatically chooses pattern)
export function useCreateShipment() {
  return useSmartMutation(
    'create_shipment',
    
    // Optimistic config (if classified as low/medium risk)
    {
      mutationFn: (data: CreateShipmentRequest) => shipmentApi.createShipment(data),
      successMessage: 'Shipment created successfully',
      errorMessage: 'Failed to create shipment',
      invalidateQueries: [['shipments'], ['dashboardStats']],
    },
    
    // Pessimistic config (if classified as high/critical risk)
    {
      mutationFn: (data: CreateShipmentRequest) => shipmentApi.createShipment(data),
      successMessage: 'Shipment created and processing',
      errorMessage: 'Failed to create shipment. Please verify all information and try again.',
      loadingMessage: 'Creating shipment...',
      invalidateQueries: [['shipments'], ['dashboardStats']],
    }
  );
}

// Example of a conflict-prone operation that needs special handling
export function useUpdateShipmentAddress() {
  const queryClient = useQueryClient();
  
  return usePessimisticMutation({
    mutationFn: ({ trackingNumber, newAddress, version }: { 
      trackingNumber: string; 
      newAddress: any; 
      version: number;
    }) => shipmentApi.updateAddress(trackingNumber, newAddress, version),
    
    successMessage: 'Delivery address updated successfully',
    
    errorMessage: (error: any) => {
      // Handle version conflicts specifically
      if (error.status === 409) {
        return 'Address update conflict detected. The shipment was modified by another user. Please refresh and try again.';
      }
      return 'Failed to update delivery address';
    },
    
    confirmationMessage: (variables) => 
      `Update delivery address for shipment ${variables.trackingNumber}? This may affect delivery timing and routing.`,
    
    requiresConfirmation: true,
    loadingMessage: 'Updating delivery address...',
    
    invalidateQueries: [
      ['shipment'],
      ['trackingHistory'],
    ],

    onError: (error: any, variables) => {
      // Special handling for 409 conflicts
      if (error.status === 409 && error.data?.serverState) {
        // Store conflict data for resolution UI
        queryClient.setQueryData(
          ['shipmentConflict', variables.trackingNumber],
          {
            ourUpdate: variables,
            serverState: error.data.serverState,
            conflictType: 'address_update',
            timestamp: new Date().toISOString(),
          }
        );
      }
    },
  });
}