/**
 * Conflict Resolution Types
 * 
 * Types for handling 409 version conflicts with user-friendly resolution UI.
 */

export interface ConflictData<T = any> {
  // The entity that has a conflict
  entityId: string;
  entityType: string;
  
  // Version information
  ourVersion: number;
  serverVersion: number;
  
  // The conflicting data
  ourChanges: T;
  serverState: T;
  
  // Metadata
  conflictType: string;
  timestamp: string;
  conflictFields?: string[]; // Specific fields that conflict
  
  // Context for resolution
  operation: string; // The operation that caused the conflict
  operationMetadata?: Record<string, any>;
}

export interface ConflictResolution<T = any> {
  resolutionType: 'force_overwrite' | 'accept_server' | 'merge_fields' | 'retry';
  resolvedData?: T;
  mergedFields?: Record<string, any>;
  comment?: string;
  forceVersion?: number;
}

export interface ConflictResolverProps<T = any> {
  conflict: ConflictData<T>;
  onResolve: (resolution: ConflictResolution<T>) => void;
  onCancel: () => void;
  isLoading?: boolean;
  entityDisplayName?: string;
  fieldLabels?: Record<string, string>;
}

export interface FieldDiff {
  fieldName: string;
  ourValue: any;
  serverValue: any;
  canMerge: boolean;
  displayName: string;
  type: 'text' | 'number' | 'date' | 'enum' | 'object' | 'array';
}

export interface ConflictStore {
  conflicts: Record<string, ConflictData>;
  activeConflictId: string | null;
  resolutionHistory: Array<{
    conflictId: string;
    resolution: ConflictResolution;
    timestamp: string;
  }>;
  
  addConflict: (conflict: ConflictData) => void;
  removeConflict: (conflictId: string) => void;
  setActiveConflict: (conflictId: string | null) => void;
  addResolution: (conflictId: string, resolution: ConflictResolution) => void;
  clearConflicts: () => void;
  getConflictById: (conflictId: string) => ConflictData | null;
}