/**
 * Enhanced Skeleton Loading Components
 * 
 * Comprehensive skeleton loading system with context-aware shapes
 * and animations for better perceived performance.
 */

import React from 'react';
import { cn } from '@/lib/utils';

interface SkeletonProps extends React.HTMLAttributes<HTMLDivElement> {
  pulse?: boolean;
}

const Skeleton = React.forwardRef<HTMLDivElement, SkeletonProps>(
  ({ className, pulse = true, ...props }, ref) => {
    return (
      <div
        ref={ref}
        className={cn(
          'bg-gray-200 rounded',
          pulse && 'animate-pulse',
          className
        )}
        {...props}
      />
    );
  }
);

Skeleton.displayName = 'Skeleton';

// Dashboard-specific skeleton loaders
export const DashboardSkeleton: React.FC = () => (
  <div className="space-y-6">
    {/* Header skeleton */}
    <div className="bg-white rounded-lg shadow-sm border p-6">
      <div className="flex items-center justify-between">
        <div className="flex items-center gap-4">
          <Skeleton className="h-12 w-12 rounded-full" />
          <div>
            <Skeleton className="h-6 w-48 mb-2" />
            <Skeleton className="h-4 w-32" />
          </div>
        </div>
        <div className="flex items-center gap-3">
          <Skeleton className="h-9 w-20" />
          <Skeleton className="h-9 w-32" />
        </div>
      </div>
      <div className="mt-6">
        <Skeleton className="h-10 w-80" />
      </div>
    </div>

    {/* Stats cards skeleton */}
    <div className="grid gap-6 md:grid-cols-2 lg:grid-cols-4">
      {[1, 2, 3, 4].map((i) => (
        <div key={i} className="bg-white rounded-lg shadow-sm border p-6">
          <div className="flex items-center justify-between mb-4">
            <Skeleton className="h-4 w-24" />
            <Skeleton className="h-6 w-6 rounded" />
          </div>
          <Skeleton className="h-8 w-16 mb-2" />
          <Skeleton className="h-3 w-32" />
        </div>
      ))}
    </div>

    {/* Performance metrics skeleton */}
    <div>
      <Skeleton className="h-6 w-40 mb-4" />
      <div className="grid gap-4 md:grid-cols-2">
        {[1, 2, 3, 4].map((i) => (
          <div key={i} className="bg-white rounded-lg shadow-sm border p-6">
            <div className="flex justify-between items-start">
              <div>
                <Skeleton className="h-4 w-32 mb-2" />
                <Skeleton className="h-8 w-12" />
              </div>
              <Skeleton className="h-16 w-16 rounded-full" />
            </div>
            <div className="flex items-center gap-2 mt-4">
              <Skeleton className="h-5 w-12 rounded-full" />
              <Skeleton className="h-3 w-20" />
            </div>
            <Skeleton className="h-3 w-48 mt-2" />
          </div>
        ))}
      </div>
    </div>

    {/* Charts skeleton */}
    <div className="grid gap-6 lg:grid-cols-2">
      <div className="bg-white rounded-lg shadow-sm border p-6">
        <Skeleton className="h-5 w-48 mb-4" />
        <Skeleton className="h-40 w-full rounded-lg" />
      </div>
      <div className="bg-white rounded-lg shadow-sm border p-6">
        <Skeleton className="h-5 w-40 mb-4" />
        <Skeleton className="h-64 w-full rounded-lg" />
      </div>
    </div>
  </div>
);

// Card-specific skeleton
export const CardSkeleton: React.FC<{ lines?: number }> = ({ lines = 3 }) => (
  <div className="bg-white rounded-lg shadow-sm border p-6">
    <Skeleton className="h-6 w-32 mb-4" />
    <div className="space-y-3">
      {Array.from({ length: lines }).map((_, i) => (
        <Skeleton key={i} className="h-4 w-full" />
      ))}
    </div>
  </div>
);

// Table skeleton
export const TableSkeleton: React.FC<{ rows?: number; cols?: number }> = ({ 
  rows = 5, 
  cols = 4 
}) => (
  <div className="bg-white rounded-lg shadow-sm border overflow-hidden">
    {/* Header */}
    <div className="border-b p-4">
      <div className="grid gap-4" style={{ gridTemplateColumns: `repeat(${cols}, 1fr)` }}>
        {Array.from({ length: cols }).map((_, i) => (
          <Skeleton key={i} className="h-4 w-20" />
        ))}
      </div>
    </div>
    {/* Rows */}
    <div className="divide-y">
      {Array.from({ length: rows }).map((_, rowIndex) => (
        <div key={rowIndex} className="p-4">
          <div className="grid gap-4" style={{ gridTemplateColumns: `repeat(${cols}, 1fr)` }}>
            {Array.from({ length: cols }).map((_, colIndex) => (
              <Skeleton 
                key={colIndex} 
                className={`h-4 ${
                  colIndex === 0 ? 'w-24' : 
                  colIndex === 1 ? 'w-32' : 
                  colIndex === 2 ? 'w-16' : 'w-20'
                }`} 
              />
            ))}
          </div>
        </div>
      ))}
    </div>
  </div>
);

// Chart skeleton
export const ChartSkeleton: React.FC<{ type?: 'donut' | 'area' | 'bar' }> = ({ 
  type = 'area' 
}) => (
  <div className="bg-white rounded-lg shadow-sm border p-6">
    <Skeleton className="h-5 w-40 mb-2" />
    <Skeleton className="h-3 w-64 mb-6" />
    
    {type === 'donut' && (
      <div className="flex items-center justify-center">
        <Skeleton className="h-40 w-40 rounded-full" />
      </div>
    )}
    
    {type === 'area' && (
      <Skeleton className="h-64 w-full rounded-lg" />
    )}
    
    {type === 'bar' && (
      <div className="flex items-end justify-between h-64 gap-2">
        {Array.from({ length: 7 }).map((_, i) => (
          <Skeleton 
            key={i} 
            className="w-full rounded-t-lg" 
            style={{ height: `${Math.random() * 80 + 20}%` }}
          />
        ))}
      </div>
    )}
  </div>
);

export { Skeleton };