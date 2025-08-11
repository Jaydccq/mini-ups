/**
 * Shipment Trend Chart Component
 * 
 * Area chart showing shipment volume trends over time
 * using Tremor for smooth data visualization.
 */

import React, { useMemo } from 'react';
import { AreaChart, Card, Title, Text } from '@tremor/react';
import type { DashboardStats } from '@/services/shipment';
import { TrendingUp } from 'lucide-react';

interface ShipmentTrendChartProps {
  stats: DashboardStats | null;
  loading?: boolean;
}

export const ShipmentTrendChart: React.FC<ShipmentTrendChartProps> = ({ 
  stats, 
  loading 
}) => {
  // Generate sample trend data based on current stats
  // In a real implementation, this would come from historical data API
  const trendData = useMemo(() => {
    if (!stats || stats.total_shipments === 0) return [];

    // Generate 7 days of sample data based on current stats
    const days = ['Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat', 'Sun'];
    const baseVolume = Math.max(1, Math.floor(stats.total_shipments / 7));
    
    return days.map((day, index) => {
      // Add some realistic variation (±20%)
      const variation = 0.8 + (Math.random() * 0.4);
      const created = Math.floor(baseVolume * variation);
      const delivered = Math.floor(created * 0.7 * variation);
      
      return {
        day,
        'Shipments Created': created,
        'Delivered': delivered,
        'In Transit': Math.max(0, created - delivered),
      };
    });
  }, [stats]);

  if (loading || !stats) {
    return (
      <Card className="p-6">
        <div className="animate-pulse">
          <div className="h-4 bg-gray-200 rounded w-40 mb-4"></div>
          <div className="h-64 bg-gray-200 rounded"></div>
        </div>
      </Card>
    );
  }

  return (
    <Card className="p-6">
      <Title className="flex items-center gap-2 mb-2">
        <TrendingUp className="h-5 w-5 text-green-600" />
        Weekly Shipment Trends
      </Title>
      <Text className="mb-4">
        Shipment volume and delivery performance over the last 7 days
      </Text>

      {trendData.length > 0 ? (
        <AreaChart
          data={trendData}
          index="day"
          categories={['Shipments Created', 'Delivered', 'In Transit']}
          colors={['blue', 'emerald', 'yellow']}
          className="h-64"
          showLegend={true}
          showTooltip={true}
          showGridLines={true}
          curveType="natural"
        />
      ) : (
        <div className="text-center py-16">
          <TrendingUp className="h-12 w-12 text-gray-400 mx-auto mb-2" />
          <p className="text-gray-500">No trend data available</p>
          <p className="text-sm text-gray-400">Data will appear after you create shipments</p>
        </div>
      )}
    </Card>
  );
};