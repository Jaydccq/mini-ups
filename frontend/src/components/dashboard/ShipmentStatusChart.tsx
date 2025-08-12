/**
 * Shipment Status Chart Component
 * 
 * Interactive donut chart showing shipment status distribution
 * using Tremor for beautiful data visualization.
 */

import React from 'react';
import { DonutChart, Card, Title, Text } from '@tremor/react';
import type { DashboardStats } from '@/services/shipment';
import { Truck, Package, CheckCircle, Clock } from 'lucide-react';

interface ShipmentStatusChartProps {
  stats: DashboardStats | null;
  loading?: boolean;
}

export const ShipmentStatusChart: React.FC<ShipmentStatusChartProps> = ({ 
  stats, 
  loading 
}) => {
  if (loading || !stats) {
    return (
      <Card className="p-6">
        <div className="animate-pulse">
          <div className="h-4 bg-gray-200 rounded w-32 mb-4"></div>
          <div className="h-40 bg-gray-200 rounded"></div>
        </div>
      </Card>
    );
  }

  const chartData = [
    {
      name: 'Delivered',
      value: stats.delivered_shipments,
      color: 'emerald',
      icon: CheckCircle,
    },
    {
      name: 'In Transit',
      value: stats.active_shipments,
      color: 'blue',
      icon: Truck,
    },
    {
      name: 'Pending',
      value: stats.pending_shipments,
      color: 'yellow',
      icon: Clock,
    },
  ].filter(item => item.value > 0); // Only show categories with data

  const total = stats.total_shipments;

  return (
    <Card className="p-6">
      <Title className="flex items-center gap-2 mb-2">
        <Package className="h-5 w-5 text-blue-600" />
        Shipment Status Distribution
      </Title>
      <Text className="mb-4">
        Overview of your {total} shipments by current status
      </Text>

      {total > 0 ? (
        <>
          <DonutChart
            data={chartData}
            category="value"
            index="name"
            colors={chartData.map(item => item.color)}
            className="h-40"
            showTooltip={true}
            showLabel={true}
          />
          
          {/* Legend with icons */}
          <div className="mt-4 grid grid-cols-1 gap-2">
            {chartData.map((item) => {
              const IconComponent = item.icon;
              const percentage = total > 0 ? Math.round((item.value / total) * 100) : 0;
              
              return (
                <div key={item.name} className="flex items-center justify-between">
                  <div className="flex items-center gap-2">
                    <IconComponent className={`h-4 w-4 ${
                      item.color === 'emerald' ? 'text-emerald-600' :
                      item.color === 'blue' ? 'text-blue-600' :
                      item.color === 'yellow' ? 'text-yellow-600' :
                      'text-gray-600'
                    }`} />
                    <span className="text-sm font-medium">{item.name}</span>
                  </div>
                  <div className="flex items-center gap-2">
                    <span className="text-sm text-gray-600">{item.value}</span>
                    <span className="text-xs text-gray-500">({percentage}%)</span>
                  </div>
                </div>
              );
            })}
          </div>
        </>
      ) : (
        <div className="text-center py-8">
          <Package className="h-12 w-12 text-gray-400 mx-auto mb-2" />
          <p className="text-gray-500">No shipment data available</p>
          <p className="text-sm text-gray-400">Create your first shipment to see analytics</p>
        </div>
      )}
    </Card>
  );
};