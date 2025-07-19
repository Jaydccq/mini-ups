/**
 * Conflict Resolution Hook
 * 
 * Provides utilities for handling 409 version conflicts with user-friendly resolution.
 */

import { useState, useCallback } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { useConflictStore } from '@/stores/conflictStore';
import { ConflictData, ConflictResolution } from '@/types/conflict';
import { toast } from 'sonner';

export const useConflictResolution = () => {
  const [isResolving, setIsResolving] = useState(false);
  const conflictStore = useConflictStore();
  const queryClient = useQueryClient();

  const resolveMutation = useMutation({
    mutationFn: async ({
      conflictId,
      resolution,
      originalMutationFn,
    }: {
      conflictId: string;
      resolution: ConflictResolution;
      originalMutationFn: (data: any) => Promise<any>;
    }) => {
      const conflict = conflictStore.getConflictById(conflictId);
      if (!conflict) {
        throw new Error('Conflict not found');
      }

      let dataToSend;
      switch (resolution.resolutionType) {
        case 'force_overwrite':
          dataToSend = {
            ...resolution.resolvedData,
            _forceVersion: resolution.forceVersion,
            _resolutionComment: resolution.comment,
          };
          break;
          
        case 'accept_server':
          // Just refetch the latest data, no mutation needed
          return { action: 'refetch' };
          
        case 'merge_fields':
          dataToSend = {
            ...resolution.resolvedData,
            _forceVersion: resolution.forceVersion,
            _resolutionComment: resolution.comment,
            _mergedFields: resolution.mergedFields,
          };
          break;
          
        default:
          throw new Error(`Unknown resolution type: ${resolution.resolutionType}`);
      }

      // Call the original mutation with resolved data
      return await originalMutationFn(dataToSend);
    },
    
    onSuccess: (data, variables) => {
      const { conflictId, resolution } = variables;
      
      // Record the resolution
      conflictStore.addResolution(conflictId, resolution);
      
      // Remove the conflict
      conflictStore.removeConflict(conflictId);
      
      // Show success message
      toast.success('Conflict resolved successfully');
      
      // If we just accepted server version, refetch the relevant queries
      if (resolution.resolutionType === 'accept_server') {
        const conflict = conflictStore.getConflictById(conflictId);
        if (conflict) {
          // Invalidate queries related to this entity
          queryClient.invalidateQueries({
            predicate: (query) => {
              const [domain, type, id] = query.queryKey;
              return domain === conflict.entityType && 
                     (type === 'detail' || type === 'list') &&
                     (!id || id === conflict.entityId);
            },
          });
        }
      }
    },
    
    onError: (error: any, variables) => {
      console.error('Conflict resolution failed:', error);
      
      // Check if this resulted in another conflict
      if (error.status === 409) {
        toast.error('Another conflict occurred. Please try resolving again.');
        // The mutation cache will handle creating a new conflict
      } else {
        toast.error(`Failed to resolve conflict: ${error.message}`);
      }
    },
    
    onSettled: () => {
      setIsResolving(false);
    },
  });

  const resolveConflict = useCallback(async (
    conflictId: string,
    resolution: ConflictResolution,
    originalMutationFn: (data: any) => Promise<any>
  ) => {
    setIsResolving(true);
    
    try {
      await resolveMutation.mutateAsync({
        conflictId,
        resolution,
        originalMutationFn,
      });
    } catch (error) {
      // Error handling is done in the mutation
      throw error;
    }
  }, [resolveMutation]);

  const cancelConflictResolution = useCallback((conflictId: string) => {
    conflictStore.removeConflict(conflictId);
    toast.info('Conflict resolution cancelled');
  }, [conflictStore]);

  return {
    resolveConflict,
    cancelConflictResolution,
    isResolving: isResolving || resolveMutation.isPending,
    error: resolveMutation.error,
    
    // Store state
    conflicts: conflictStore.conflicts,
    activeConflictId: conflictStore.activeConflictId,
    setActiveConflict: conflictStore.setActiveConflict,
  };
};

/**
 * Hook for automatically handling conflicts in mutations
 */
export const useConflictAwareMutation = <TData, TVariables>(
  mutationFn: (variables: TVariables) => Promise<TData>,
  options?: {
    entityType?: string;
    entityIdExtractor?: (variables: TVariables) => string;
    onConflict?: (conflict: ConflictData) => void;
  }
) => {
  const conflictStore = useConflictStore();
  const { resolveConflict } = useConflictResolution();

  return useMutation({
    mutationFn,
    
    onError: (error: any, variables) => {
      // Handle 409 conflicts
      if (error.status === 409 && error.data?.serverState) {
        const entityId = options?.entityIdExtractor?.(variables) || 'unknown';
        const entityType = options?.entityType || 'unknown';
        
        const conflict: ConflictData = {
          entityId,
          entityType,
          ourVersion: error.data.ourVersion || 0,
          serverVersion: error.data.serverVersion || 0,
          ourChanges: variables,
          serverState: error.data.serverState,
          conflictType: error.data.conflictType || 'data_conflict',
          timestamp: new Date().toISOString(),
          operation: 'mutation',
          operationMetadata: { variables },
        };
        
        const conflictId = conflictStore.addConflict(conflict);
        
        // Call custom conflict handler if provided
        if (options?.onConflict) {
          options.onConflict(conflict);
        }
        
        // Store the original mutation function for resolution
        (window as any).__conflictResolutionMutations = {
          ...(window as any).__conflictResolutionMutations,
          [conflictId]: mutationFn,
        };
      }
    },
  });
};

/**
 * Global conflict resolution component state
 */
export const useGlobalConflictState = () => {
  const conflictStore = useConflictStore();
  const { resolveConflict, cancelConflictResolution, isResolving } = useConflictResolution();
  
  const activeConflict = conflictStore.activeConflictId 
    ? conflictStore.conflicts[conflictStore.activeConflictId]
    : null;

  const handleResolveActiveConflict = useCallback(async (resolution: ConflictResolution) => {
    if (!activeConflict || !conflictStore.activeConflictId) return;
    
    // Get the stored mutation function
    const mutations = (window as any).__conflictResolutionMutations || {};
    const originalMutationFn = mutations[conflictStore.activeConflictId];
    
    if (!originalMutationFn) {
      toast.error('Original operation not found. Please try the operation again.');
      conflictStore.removeConflict(conflictStore.activeConflictId);
      return;
    }
    
    await resolveConflict(conflictStore.activeConflictId, resolution, originalMutationFn);
    
    // Clean up the stored mutation function
    delete mutations[conflictStore.activeConflictId];
  }, [activeConflict, conflictStore, resolveConflict]);

  const handleCancelActiveConflict = useCallback(() => {
    if (!conflictStore.activeConflictId) return;
    
    // Clean up the stored mutation function
    const mutations = (window as any).__conflictResolutionMutations || {};
    delete mutations[conflictStore.activeConflictId];
    
    cancelConflictResolution(conflictStore.activeConflictId);
  }, [conflictStore.activeConflictId, cancelConflictResolution]);

  return {
    activeConflict,
    hasActiveConflict: !!activeConflict,
    isResolving,
    resolveActiveConflict: handleResolveActiveConflict,
    cancelActiveConflict: handleCancelActiveConflict,
    
    // All conflicts
    allConflicts: Object.values(conflictStore.conflicts),
    conflictCount: Object.keys(conflictStore.conflicts).length,
  };
};