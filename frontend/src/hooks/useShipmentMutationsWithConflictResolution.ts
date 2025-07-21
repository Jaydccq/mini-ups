/**
 * Example: Shipment Mutations with Conflict Resolution
 * 
 * Demonstrates how to use the complete real-time notification and conflict resolution system
 * in practice with real shipment operations.
 */

import { useQueryClient } from '@tanstack/react-query';
import { useConflictAwareMutation } from '@/hooks/useConflictResolution';
import { usePessimisticMutation, useOptimisticMutation } from '@/hooks/mutations/useMutationPatterns';
import { shipmentApi } from '@/services/shipment';
import { queryKeys } from '@/lib/queryClientConfig';

// Example: Update shipment status with conflict resolution
export const useUpdateShipmentStatusWithConflictResolution = () => {
  return useConflictAwareMutation(
    ({ trackingNumber, status, version, comment }: {
      trackingNumber: string;
      status: string;
      version: number;
      comment?: string;
    }) => shipmentApi.updateStatus(trackingNumber, status, version, comment),
    {
      entityType: 'shipment',
      entityIdExtractor: (variables) => variables.trackingNumber,
      onConflict: (conflict) => {
        console.log('Shipment status conflict detected:', conflict);
        // Conflict will be automatically shown in the ConflictResolver
      },
    }
  );
};

// Example: Update delivery address with conflict resolution and pessimistic pattern
export const useUpdateDeliveryAddressWithConflictResolution = () => {
  const conflictAwareMutation = useConflictAwareMutation(
    ({ trackingNumber, address, version }: {
      trackingNumber: string;
      address: any;
      version: number;
    }) => shipmentApi.updateDeliveryAddress(trackingNumber, version, address),
    {
      entityType: 'shipment',
      entityIdExtractor: (variables) => variables.trackingNumber,
    }
  );

  // Wrap with pessimistic pattern for critical operations
  return usePessimisticMutation({
    mutationFn: conflictAwareMutation.mutateAsync,
    
    successMessage: (data, variables) => 
      `Delivery address updated for shipment ${variables.trackingNumber}`,
    
    errorMessage: (error: any) => {
      if (error.status === 409) {
        return 'Address update conflict detected. Please resolve the conflict to continue.';
      }
      return 'Failed to update delivery address';
    },
    
    confirmationMessage: (variables) => 
      `Update delivery address for shipment ${variables.trackingNumber}? This may affect delivery timing.`,
    
    requiresConfirmation: true,
    loadingMessage: 'Updating delivery address...',
    
    invalidateQueries: (variables: any) => [
      queryKeys.shipment.detail(variables.trackingNumber),
      queryKeys.shipment.tracking(variables.trackingNumber),
    ],
  });
};

// Example: Add comment with optimistic updates and automatic conflict handling
export const useAddShipmentCommentWithOptimisticUpdates = () => {
  const queryClient = useQueryClient();
  
  const conflictAwareMutation = useConflictAwareMutation(
    ({ shipmentId, comment }: { shipmentId: string; comment: string }) =>
      shipmentApi.addComment({ shipmentId, comment } as any),
    {
      entityType: 'shipment_comment',
      entityIdExtractor: (variables) => variables.shipmentId,
    }
  );

  return useOptimisticMutation({
    mutationFn: conflictAwareMutation.mutateAsync,
    
    successMessage: 'Comment added successfully',
    errorMessage: (error: any) => {
      if (error.status === 409) {
        return 'Comment conflict detected. Your comment will be merged automatically.';
      }
      return 'Failed to add comment';
    },
    
    invalidateQueries: (variables: any) => [
      queryKeys.shipment.comments(variables.shipmentId),
      queryKeys.shipment.detail(variables.shipmentId),
    ],

    onMutate: async (variables) => {
      // Cancel outgoing refetches
      await queryClient.cancelQueries({ 
        queryKey: queryKeys.shipment.comments(variables.shipmentId) 
      });
      
      // Snapshot previous value
      const previousComments = queryClient.getQueryData(
        queryKeys.shipment.comments(variables.shipmentId)
      );
      
      // Optimistically add comment
      const optimisticComment = {
        id: `temp_${Date.now()}`,
        comment: variables.comment,
        createdAt: new Date().toISOString(),
        author: 'You',
        status: 'pending',
      };
      
      queryClient.setQueryData(
        queryKeys.shipment.comments(variables.shipmentId),
        (old: any[]) => old ? [...old, optimisticComment] : [optimisticComment]
      );

      return { previousComments, optimisticComment };
    },

    rollback: (variables: any, context: any) => {
      if (context?.previousComments) {
        queryClient.setQueryData(
          queryKeys.shipment.comments(variables.shipmentId), 
          context.previousComments
        );
      }
    },
  });
};

// Example: Comprehensive shipment operations hook
export const useShipmentOperations = (trackingNumber: string) => {
  const updateStatus = useUpdateShipmentStatusWithConflictResolution();
  const updateAddress = useUpdateDeliveryAddressWithConflictResolution();
  const addComment = useAddShipmentCommentWithOptimisticUpdates();

  return {
    // Status updates (critical operations with conflict resolution)
    updateStatus: (status: string, version: number, comment?: string) =>
      updateStatus.mutate({ trackingNumber, status, version, comment }),
    
    // Address updates (critical operations with confirmation)
    updateAddress: (address: any, version: number) =>
      updateAddress.mutate({ trackingNumber, address, version }),
    
    // Comments (low-risk operations with optimistic updates)
    addComment: (comment: string) =>
      addComment.mutate({ shipmentId: trackingNumber, comment }),
    
    // Status
    isUpdatingStatus: updateStatus.isPending,
    isUpdatingAddress: updateAddress.isPending,
    isAddingComment: addComment.isPending,
    
    // Errors
    statusError: updateStatus.error,
    addressError: updateAddress.error,
    commentError: addComment.error,
  };
};