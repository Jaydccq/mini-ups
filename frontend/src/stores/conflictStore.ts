/**
 * Conflict Resolution Store
 * 
 * Zustand store for managing version conflicts and their resolution state.
 */

import { create } from 'zustand';
import { ConflictData, ConflictResolution, ConflictStore } from '@/types/conflict';

export const useConflictStore = create<ConflictStore>((set, get) => ({
  conflicts: {},
  activeConflictId: null,
  resolutionHistory: [],

  addConflict: (conflict: ConflictData) => {
    const conflictId = `${conflict.entityType}_${conflict.entityId}_${Date.now()}`;
    set((state) => ({
      conflicts: {
        ...state.conflicts,
        [conflictId]: { ...conflict, id: conflictId },
      },
      // Auto-activate if no active conflict
      activeConflictId: state.activeConflictId || conflictId,
    }));
    return conflictId;
  },

  removeConflict: (conflictId: string) => {
    set((state) => {
      const newConflicts = { ...state.conflicts };
      delete newConflicts[conflictId];
      
      return {
        conflicts: newConflicts,
        activeConflictId: state.activeConflictId === conflictId ? null : state.activeConflictId,
      };
    });
  },

  setActiveConflict: (conflictId: string | null) => {
    set({ activeConflictId: conflictId });
  },

  addResolution: (conflictId: string, resolution: ConflictResolution) => {
    set((state) => ({
      resolutionHistory: [
        ...state.resolutionHistory,
        {
          conflictId,
          resolution,
          timestamp: new Date().toISOString(),
        },
      ],
    }));
  },

  clearConflicts: () => {
    set({
      conflicts: {},
      activeConflictId: null,
    });
  },

  getConflictById: (conflictId: string) => {
    const state = get();
    return state.conflicts[conflictId] || null;
  },
}));