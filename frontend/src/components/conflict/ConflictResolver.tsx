/**
 * Conflict Resolver Component
 * 
 * User-friendly interface for resolving 409 version conflicts.
 * Shows side-by-side comparison and allows users to choose resolution strategy.
 */

import React, { useState, useMemo } from 'react';
import { 
  AlertTriangle, 
  RefreshCw, 
  Check, 
  X, 
  Eye, 
  GitMerge,
  Clock,
  User,
  FileText,
  ChevronRight,
  Info
} from 'lucide-react';
import { ConflictResolverProps, ConflictResolution, FieldDiff } from '@/types/conflict';
import { Button } from '@/components/ui/button';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
import { Badge } from '@/components/ui/badge';
import { Separator } from '@/components/ui/separator';
import { ScrollArea } from '@/components/ui/scroll-area';
import { Textarea } from '@/components/ui/textarea';
import { Checkbox } from '@/components/ui/checkbox';
import { Alert, AlertDescription } from '@/components/ui/alert';
import { 
  Dialog, 
  DialogContent, 
  DialogDescription, 
  DialogHeader, 
  DialogTitle 
} from '@/components/ui/dialog';
import { Tabs, TabsContent, TabsList, TabsTrigger } from '@/components/ui/tabs';
import { formatDateTime } from '@/lib/utils';

// Helper to determine if two values are different
const isDifferent = (ourValue: any, serverValue: any): boolean => {
  if (ourValue === serverValue) return false;
  if (ourValue == null && serverValue == null) return false;
  if (typeof ourValue === 'object' && typeof serverValue === 'object') {
    return JSON.stringify(ourValue) !== JSON.stringify(serverValue);
  }
  return String(ourValue) !== String(serverValue);
};

// Generate field differences for comparison
const generateFieldDiffs = (ourChanges: any, serverState: any, fieldLabels: Record<string, string> = {}): FieldDiff[] => {
  const allFields = new Set([...Object.keys(ourChanges || {}), ...Object.keys(serverState || {})]);
  const diffs: FieldDiff[] = [];

  for (const field of allFields) {
    const ourValue = ourChanges?.[field];
    const serverValue = serverState?.[field];
    
    if (isDifferent(ourValue, serverValue)) {
      const fieldType = typeof ourValue === 'number' ? 'number' :
                       typeof ourValue === 'object' && ourValue instanceof Date ? 'date' :
                       typeof ourValue === 'object' && Array.isArray(ourValue) ? 'array' :
                       typeof ourValue === 'object' ? 'object' : 'text';

      diffs.push({
        fieldName: field,
        ourValue,
        serverValue,
        canMerge: fieldType === 'text' || fieldType === 'number',
        displayName: fieldLabels[field] || field.replace(/([A-Z])/g, ' $1').replace(/^./, str => str.toUpperCase()),
        type: fieldType,
      });
    }
  }

  return diffs;
};

// Field value display component
const FieldValueDisplay: React.FC<{ value: any; type: string }> = ({ value, type }) => {
  if (value == null) {
    return <span className="text-gray-400 italic">Not set</span>;
  }

  switch (type) {
    case 'date':
      return <span>{formatDateTime(value)}</span>;
    case 'object':
      return (
        <details className="text-sm">
          <summary className="cursor-pointer text-blue-600">View object</summary>
          <pre className="mt-2 p-2 bg-gray-100 rounded text-xs overflow-auto">
            {JSON.stringify(value, null, 2)}
          </pre>
        </details>
      );
    case 'array':
      return <span>{Array.isArray(value) ? `[${value.length} items]` : String(value)}</span>;
    default:
      return <span className="break-words">{String(value)}</span>;
  }
};

// Individual field comparison component
const FieldComparison: React.FC<{
  diff: FieldDiff;
  isSelected: boolean;
  selectedSource: 'ours' | 'server' | null;
  onSelectionChange: (source: 'ours' | 'server' | null) => void;
  canSelect: boolean;
}> = ({ diff, isSelected, selectedSource, onSelectionChange, canSelect }) => {
  return (
    <Card className={`${isSelected ? 'border-blue-300 bg-blue-50' : 'border-gray-200'}`}>
      <CardHeader className="pb-3">
        <div className="flex items-center justify-between">
          <CardTitle className="text-sm font-medium">{diff.displayName}</CardTitle>
          <Badge variant="outline" className="text-xs">
            {diff.type}
          </Badge>
        </div>
      </CardHeader>
      <CardContent className="pt-0">
        <div className="grid grid-cols-2 gap-4">
          {/* Our changes */}
          <div className="space-y-2">
            <div className="flex items-center gap-2">
              {canSelect && (
                <Checkbox
                  checked={selectedSource === 'ours'}
                  onCheckedChange={(checked) => 
                    onSelectionChange(checked ? 'ours' : null)
                  }
                />
              )}
              <h4 className="text-sm font-medium text-blue-600">Your Changes</h4>
            </div>
            <div className="p-3 bg-blue-50 rounded border border-blue-200">
              <FieldValueDisplay value={diff.ourValue} type={diff.type} />
            </div>
          </div>

          {/* Server state */}
          <div className="space-y-2">
            <div className="flex items-center gap-2">
              {canSelect && (
                <Checkbox
                  checked={selectedSource === 'server'}
                  onCheckedChange={(checked) => 
                    onSelectionChange(checked ? 'server' : null)
                  }
                />
              )}
              <h4 className="text-sm font-medium text-green-600">Server Version</h4>
            </div>
            <div className="p-3 bg-green-50 rounded border border-green-200">
              <FieldValueDisplay value={diff.serverValue} type={diff.type} />
            </div>
          </div>
        </div>
      </CardContent>
    </Card>
  );
};

// Main ConflictResolver component
export const ConflictResolver: React.FC<ConflictResolverProps> = ({
  conflict,
  onResolve,
  onCancel,
  isLoading = false,
  entityDisplayName = 'item',
  fieldLabels = {},
}) => {
  const [resolutionType, setResolutionType] = useState<'force_overwrite' | 'accept_server' | 'merge_fields'>('force_overwrite');
  const [comment, setComment] = useState('');
  const [selectedFields, setSelectedFields] = useState<Record<string, 'ours' | 'server'>>({});
  const [showDetails, setShowDetails] = useState(false);

  const fieldDiffs = useMemo(() => 
    generateFieldDiffs(conflict.ourChanges, conflict.serverState, fieldLabels), 
    [conflict.ourChanges, conflict.serverState, fieldLabels]
  );

  const handleFieldSelection = (fieldName: string, source: 'ours' | 'server' | null) => {
    setSelectedFields(prev => {
      const updated = { ...prev };
      if (source === null) {
        delete updated[fieldName];
      } else {
        updated[fieldName] = source;
      }
      return updated;
    });
  };

  const handleResolve = () => {
    let resolution: ConflictResolution;

    switch (resolutionType) {
      case 'force_overwrite':
        resolution = {
          resolutionType: 'force_overwrite',
          resolvedData: conflict.ourChanges,
          comment: comment || 'Force overwrite with user changes',
          forceVersion: conflict.serverVersion,
        };
        break;

      case 'accept_server':
        resolution = {
          resolutionType: 'accept_server',
          resolvedData: conflict.serverState,
          comment: comment || 'Accept server version',
        };
        break;

      case 'merge_fields': {
        const mergedData = { ...conflict.serverState };
        const mergedFields: Record<string, unknown> = {};
        
        Object.entries(selectedFields).forEach(([fieldName, source]) => {
          const value = source === 'ours' ? conflict.ourChanges[fieldName] : conflict.serverState[fieldName];
          mergedData[fieldName] = value;
          mergedFields[fieldName] = { source, value };
        });

        resolution = {
          resolutionType: 'merge_fields',
          resolvedData: mergedData,
          mergedFields,
          comment: comment || 'Merged changes from both versions',
          forceVersion: conflict.serverVersion,
        };
        break;
      }

      default:
        throw new Error(`Unknown resolution type: ${resolutionType}`);
    }

    onResolve(resolution);
  };

  const canResolve = () => {
    if (resolutionType === 'merge_fields') {
      return fieldDiffs.length > 0 && Object.keys(selectedFields).length > 0;
    }
    return true;
  };

  return (
    <Dialog open={true} onOpenChange={(open) => !open && !isLoading && onCancel()}>
      <DialogContent className="max-w-6xl max-h-[90vh] overflow-hidden">
        <DialogHeader>
          <DialogTitle className="flex items-center gap-2">
            <AlertTriangle className="h-5 w-5 text-orange-500" />
            Conflict Detected: {entityDisplayName}
          </DialogTitle>
          <DialogDescription>
            The {entityDisplayName} was modified by another user while you were editing it. 
            Please choose how to resolve this conflict.
          </DialogDescription>
        </DialogHeader>

        <div className="space-y-6">
          {/* Conflict Information */}
          <Alert>
            <Info className="h-4 w-4" />
            <AlertDescription>
              <div className="space-y-1">
                <div><strong>Entity:</strong> {conflict.entityType} #{conflict.entityId}</div>
                <div><strong>Operation:</strong> {conflict.operation}</div>
                <div><strong>Conflict Time:</strong> {formatDateTime(conflict.timestamp)}</div>
                <div><strong>Versions:</strong> Your version {conflict.ourVersion} vs Server version {conflict.serverVersion}</div>
              </div>
            </AlertDescription>
          </Alert>

          {/* Resolution Strategy Tabs */}
          <Tabs value={resolutionType} onValueChange={(value) => setResolutionType(value as any)}>
            <TabsList className="grid w-full grid-cols-3">
              <TabsTrigger value="force_overwrite" className="flex items-center gap-2">
                <RefreshCw className="h-4 w-4" />
                Force My Changes
              </TabsTrigger>
              <TabsTrigger value="accept_server" className="flex items-center gap-2">
                <Check className="h-4 w-4" />
                Accept Server Version
              </TabsTrigger>
              <TabsTrigger value="merge_fields" className="flex items-center gap-2">
                <GitMerge className="h-4 w-4" />
                Merge Fields
              </TabsTrigger>
            </TabsList>

            <TabsContent value="force_overwrite" className="space-y-4">
              <Alert className="border-orange-200 bg-orange-50">
                <AlertTriangle className="h-4 w-4" />
                <AlertDescription>
                  <strong>Warning:</strong> This will overwrite the server version with your changes. 
                  Any modifications made by other users will be lost.
                </AlertDescription>
              </Alert>
              
              <Button 
                variant="outline" 
                onClick={() => setShowDetails(!showDetails)}
                className="w-full"
              >
                <Eye className="h-4 w-4 mr-2" />
                {showDetails ? 'Hide' : 'Show'} Changes Preview
              </Button>
              
              {showDetails && (
                <ScrollArea className="h-64 w-full border rounded p-4">
                  <div className="space-y-4">
                    {fieldDiffs.map((diff) => (
                      <FieldComparison
                        key={diff.fieldName}
                        diff={diff}
                        isSelected={false}
                        selectedSource={null}
                        onSelectionChange={() => {}}
                        canSelect={false}
                      />
                    ))}
                  </div>
                </ScrollArea>
              )}
            </TabsContent>

            <TabsContent value="accept_server" className="space-y-4">
              <Alert className="border-green-200 bg-green-50">
                <Check className="h-4 w-4" />
                <AlertDescription>
                  This will discard your changes and use the server version. 
                  Your local modifications will be lost.
                </AlertDescription>
              </Alert>
              
              <Button 
                variant="outline" 
                onClick={() => setShowDetails(!showDetails)}
                className="w-full"
              >
                <Eye className="h-4 w-4 mr-2" />
                {showDetails ? 'Hide' : 'Show'} Server Version
              </Button>
              
              {showDetails && (
                <ScrollArea className="h-64 w-full border rounded p-4">
                  <div className="space-y-4">
                    {fieldDiffs.map((diff) => (
                      <FieldComparison
                        key={diff.fieldName}
                        diff={diff}
                        isSelected={false}
                        selectedSource="server"
                        onSelectionChange={() => {}}
                        canSelect={false}
                      />
                    ))}
                  </div>
                </ScrollArea>
              )}
            </TabsContent>

            <TabsContent value="merge_fields" className="space-y-4">
              <Alert className="border-blue-200 bg-blue-50">
                <GitMerge className="h-4 w-4" />
                <AlertDescription>
                  Choose which version to keep for each conflicting field. 
                  Select at least one field to proceed.
                </AlertDescription>
              </Alert>
              
              <ScrollArea className="h-80 w-full">
                <div className="space-y-4 pr-4">
                  {fieldDiffs.map((diff) => (
                    <FieldComparison
                      key={diff.fieldName}
                      diff={diff}
                      isSelected={diff.fieldName in selectedFields}
                      selectedSource={selectedFields[diff.fieldName] || null}
                      onSelectionChange={(source) => handleFieldSelection(diff.fieldName, source)}
                      canSelect={true}
                    />
                  ))}
                </div>
              </ScrollArea>
              
              {fieldDiffs.length > 0 && Object.keys(selectedFields).length === 0 && (
                <Alert variant="destructive">
                  <AlertDescription>
                    Please select at least one field to merge.
                  </AlertDescription>
                </Alert>
              )}
            </TabsContent>
          </Tabs>

          {/* Optional Comment */}
          <div className="space-y-2">
            <label className="text-sm font-medium">Resolution Comment (Optional)</label>
            <Textarea
              placeholder="Add a comment explaining your resolution choice..."
              value={comment}
              onChange={(e) => setComment(e.target.value)}
              rows={3}
            />
          </div>

          {/* Action Buttons */}
          <div className="flex justify-end gap-3 pt-4 border-t">
            <Button 
              variant="outline" 
              onClick={onCancel}
              disabled={isLoading}
            >
              <X className="h-4 w-4 mr-2" />
              Cancel
            </Button>
            <Button 
              onClick={handleResolve}
              disabled={!canResolve() || isLoading}
              className="min-w-[120px]"
            >
              {isLoading ? (
                <RefreshCw className="h-4 w-4 mr-2 animate-spin" />
              ) : (
                <Check className="h-4 w-4 mr-2" />
              )}
              {isLoading ? 'Resolving...' : 'Resolve Conflict'}
            </Button>
          </div>
        </div>
      </DialogContent>
    </Dialog>
  );
};