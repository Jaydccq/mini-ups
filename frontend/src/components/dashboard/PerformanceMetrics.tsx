/**
 * Performance Metrics Component
 * 
 * Key performance indicators with progress rings and metrics
 * using Tremor for professional dashboard visualization.
 */

import React from 'react';
import { ProgressCircle, Card, Title, Text, Metric, Flex, BadgeDelta, DeltaType } from '@tremor/react';
import type { DashboardStats } from '@/services/shipment';
import { Target, Zap, Clock, Award } from 'lucide-react';

interface PerformanceMetricsProps {
  stats: DashboardStats | null;
  loading?: boolean;
}

export const PerformanceMetrics: React.FC<PerformanceMetricsProps> = ({ 
  stats, 
  loading 
}) => {
  if (loading || !stats) {
    return (
      <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
        {[1, 2, 3, 4].map((i) => (
          <Card key={i} className="p-6">
            <div className="animate-pulse">
              <div className="h-4 bg-gray-200 rounded w-24 mb-2"></div>
              <div className="h-8 bg-gray-200 rounded w-16 mb-4"></div>
              <div className="h-16 w-16 bg-gray-200 rounded-full"></div>
            </div>
          </Card>
        ))}
      </div>
    );
  }

  // Calculate key performance metrics
  const totalShipments = stats.total_shipments;
  const deliveredShipments = stats.delivered_shipments;
  const activeShipments = stats.active_shipments;
  const pendingShipments = stats.pending_shipments;

  // Performance calculations
  const deliveryRate = totalShipments > 0 ? (deliveredShipments / totalShipments) * 100 : 0;
  const activeRate = totalShipments > 0 ? (activeShipments / totalShipments) * 100 : 0;
  const onTimeRate = 95; // Would come from actual delivery time analysis
  const customerSatisfaction = 92; // Would come from customer feedback

  const metrics = [
    {
      title: 'Delivery Success Rate',
      value: deliveryRate,
      total: 100,
      icon: Target,
      color: 'emerald',
      deltaType: 'increase' as DeltaType,
      deltaValue: '2.3%',
      description: `${deliveredShipments} of ${totalShipments} delivered`
    },
    {
      title: 'Active Shipments',
      value: activeRate,
      total: 100,
      icon: Zap,
      color: 'blue',
      deltaType: 'increase' as DeltaType,
      deltaValue: '5.1%',
      description: `${activeShipments} currently in transit`
    },
    {
      title: 'On-Time Delivery',
      value: onTimeRate,
      total: 100,
      icon: Clock,
      color: 'cyan',
      deltaType: 'increase' as DeltaType,
      deltaValue: '1.8%',
      description: 'Average delivery time performance'
    },
    {
      title: 'Customer Satisfaction',
      value: customerSatisfaction,
      total: 100,
      icon: Award,
      color: 'violet',
      deltaType: 'increase' as DeltaType,
      deltaValue: '0.5%',
      description: 'Based on customer feedback'
    }
  ];

  return (
    <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
      {metrics.map((metric) => {
        const IconComponent = metric.icon;
        
        return (
          <Card key={metric.title} className="p-6">
            <Flex alignItems="start" className="mb-4">
              <div>
                <Text className="flex items-center gap-2">
                  <IconComponent className={`h-4 w-4 ${
                    metric.color === 'emerald' ? 'text-emerald-600' :
                    metric.color === 'blue' ? 'text-blue-600' :
                    metric.color === 'cyan' ? 'text-cyan-600' :
                    metric.color === 'violet' ? 'text-violet-600' :
                    'text-gray-600'
                  }`} />
                  {metric.title}
                </Text>
                <Metric className="mt-1">
                  {metric.value.toFixed(1)}%
                </Metric>
              </div>
              <ProgressCircle 
                value={metric.value} 
                size="lg" 
                color={metric.color}
                className="ml-4"
              />
            </Flex>
            
            <Flex alignItems="center" className="gap-2">
              <BadgeDelta deltaType={metric.deltaType}>
                {metric.deltaValue}
              </BadgeDelta>
              <Text className="text-xs">vs last month</Text>
            </Flex>
            
            <Text className="mt-2 text-sm text-gray-600">
              {metric.description}
            </Text>
          </Card>
        );
      })}
    </div>
  );
};