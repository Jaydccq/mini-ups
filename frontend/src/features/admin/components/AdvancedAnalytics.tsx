/**
 * Advanced Analytics Components
 * 
 * Features:
 * - Advanced chart library with interactive visualizations
 * - Trend analysis with forecasting
 * - Real-time performance metrics
 * - Operational efficiency analytics
 * - Custom dashboard builder
 * 
 *
 
 */
import React, { useState, useEffect, useMemo } from 'react';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Badge } from '@/components/ui/badge';
import { Button } from '@/components/ui/button';
import { Tabs, TabsContent, TabsList, TabsTrigger } from '@/components/ui/tabs';
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select';
import { 
  TrendingUp, 
  TrendingDown,
  LineChart as LineChartIcon,
  Activity, 
  Zap,
  Target,
  Users,
  Truck,
  DollarSign,
  AlertTriangle,
  RefreshCw
} from 'lucide-react';

interface MetricData {
  name: string;
  value: number;
  change: number;
  trend: 'up' | 'down' | 'stable';
  target?: number;
  unit?: string;
  category: 'revenue' | 'operations' | 'efficiency' | 'satisfaction';
}

interface AnalyticsData {
  deliveryEfficiency: {
    onTimeDeliveries: number;
    averageDeliveryTime: number;
    fuelEfficiency: number;
    routeOptimization: number;
  };
  operationalMetrics: {
    truckUtilization: number;
    driverProductivity: number;
    warehouseEfficiency: number;
    maintenanceCosts: number;
  };
  customerMetrics: {
    satisfactionScore: number;
    retentionRate: number;
    complaintResolution: number;
    repeatCustomers: number;
  };
  financialMetrics: {
    revenueGrowth: number;
    profitMargin: number;
    costPerDelivery: number;
    customerAcquisitionCost: number;
  };
}

interface AdvancedAnalyticsProps {
  className?: string;
}

export const AdvancedAnalytics: React.FC<AdvancedAnalyticsProps> = ({ className = '' }) => {
  const [timeRange, setTimeRange] = useState('30d');
  const [autoRefresh, setAutoRefresh] = useState(true);
  const [lastUpdate, setLastUpdate] = useState<Date>(new Date());
  const [analyticsData, setAnalyticsData] = useState<AnalyticsData | null>(null);

  // Mock real-time data fetching
  useEffect(() => {
    const fetchAnalyticsData = () => {
      // In real implementation, this would be an API call
      setAnalyticsData({
        deliveryEfficiency: {
          onTimeDeliveries: 94.5 + Math.random() * 2,
          averageDeliveryTime: 2.8 + Math.random() * 0.5,
          fuelEfficiency: 12.4 + Math.random() * 1,
          routeOptimization: 87.2 + Math.random() * 3
        },
        operationalMetrics: {
          truckUtilization: 78.5 + Math.random() * 5,
          driverProductivity: 91.2 + Math.random() * 3,
          warehouseEfficiency: 89.7 + Math.random() * 2,
          maintenanceCosts: 145.8 + Math.random() * 10
        },
        customerMetrics: {
          satisfactionScore: 4.6 + Math.random() * 0.3,
          retentionRate: 82.4 + Math.random() * 3,
          complaintResolution: 96.8 + Math.random() * 2,
          repeatCustomers: 67.9 + Math.random() * 4
        },
        financialMetrics: {
          revenueGrowth: 12.8 + Math.random() * 3,
          profitMargin: 18.5 + Math.random() * 2,
          costPerDelivery: 8.4 + Math.random() * 1,
          customerAcquisitionCost: 24.6 + Math.random() * 5
        }
      });
      setLastUpdate(new Date());
    };

    fetchAnalyticsData();
    
    let interval: NodeJS.Timeout;
    if (autoRefresh) {
      interval = setInterval(fetchAnalyticsData, 30000); // 30 seconds
    }

    return () => {
      if (interval) clearInterval(interval);
    };
  }, [autoRefresh, timeRange]);

  const metrics: MetricData[] = useMemo(() => {
    if (!analyticsData) return [];

    return [
      // Efficiency Metrics
      {
        name: 'On-Time Deliveries',
        value: analyticsData.deliveryEfficiency.onTimeDeliveries,
        change: 2.3,
        trend: 'up',
        target: 95,
        unit: '%',
        category: 'efficiency'
      },
      {
        name: 'Avg Delivery Time',
        value: analyticsData.deliveryEfficiency.averageDeliveryTime,
        change: -0.2,
        trend: 'up',
        target: 3.0,
        unit: 'days',
        category: 'efficiency'
      },
      {
        name: 'Fuel Efficiency',
        value: analyticsData.deliveryEfficiency.fuelEfficiency,
        change: 1.8,
        trend: 'up',
        unit: 'L/100km',
        category: 'efficiency'
      },
      {
        name: 'Route Optimization',
        value: analyticsData.deliveryEfficiency.routeOptimization,
        change: 3.2,
        trend: 'up',
        unit: '%',
        category: 'efficiency'
      },
      // Operational Metrics
      {
        name: 'Truck Utilization',
        value: analyticsData.operationalMetrics.truckUtilization,
        change: 4.1,
        trend: 'up',
        target: 85,
        unit: '%',
        category: 'operations'
      },
      {
        name: 'Driver Productivity',
        value: analyticsData.operationalMetrics.driverProductivity,
        change: 1.7,
        trend: 'up',
        unit: '%',
        category: 'operations'
      },
      {
        name: 'Warehouse Efficiency',
        value: analyticsData.operationalMetrics.warehouseEfficiency,
        change: -0.8,
        trend: 'down',
        target: 92,
        unit: '%',
        category: 'operations'
      },
      {
        name: 'Maintenance Costs',
        value: analyticsData.operationalMetrics.maintenanceCosts,
        change: -3.2,
        trend: 'up',
        unit: '$',
        category: 'operations'
      },
      // Customer Metrics
      {
        name: 'Customer Satisfaction',
        value: analyticsData.customerMetrics.satisfactionScore,
        change: 0.2,
        trend: 'up',
        target: 4.8,
        unit: '/5',
        category: 'satisfaction'
      },
      {
        name: 'Retention Rate',
        value: analyticsData.customerMetrics.retentionRate,
        change: 2.1,
        trend: 'up',
        target: 85,
        unit: '%',
        category: 'satisfaction'
      },
      {
        name: 'Complaint Resolution',
        value: analyticsData.customerMetrics.complaintResolution,
        change: 1.5,
        trend: 'up',
        target: 98,
        unit: '%',
        category: 'satisfaction'
      },
      {
        name: 'Repeat Customers',
        value: analyticsData.customerMetrics.repeatCustomers,
        change: 3.8,
        trend: 'up',
        unit: '%',
        category: 'satisfaction'
      },
      // Financial Metrics
      {
        name: 'Revenue Growth',
        value: analyticsData.financialMetrics.revenueGrowth,
        change: 2.4,
        trend: 'up',
        unit: '%',
        category: 'revenue'
      },
      {
        name: 'Profit Margin',
        value: analyticsData.financialMetrics.profitMargin,
        change: 1.2,
        trend: 'up',
        target: 20,
        unit: '%',
        category: 'revenue'
      },
      {
        name: 'Cost per Delivery',
        value: analyticsData.financialMetrics.costPerDelivery,
        change: -1.8,
        trend: 'up',
        unit: '$',
        category: 'revenue'
      },
      {
        name: 'Customer Acquisition Cost',
        value: analyticsData.financialMetrics.customerAcquisitionCost,
        change: -2.1,
        trend: 'up',
        target: 25,
        unit: '$',
        category: 'revenue'
      }
    ];
  }, [analyticsData]);

  const getMetricsByCategory = (category: string) => {
    return metrics.filter(m => m.category === category);
  };

  const getTrendIcon = (trend: string) => {
    switch (trend) {
      case 'up':
        return <TrendingUp className="w-4 h-4 text-green-600" />;
      case 'down':
        return <TrendingDown className="w-4 h-4 text-red-600" />;
      default:
        return <Activity className="w-4 h-4 text-gray-600" />;
    }
  };

  const getTargetStatus = (value: number, target?: number) => {
    if (!target) return null;
    const percentage = (value / target) * 100;
    if (percentage >= 95) return 'excellent';
    if (percentage >= 80) return 'good';
    if (percentage >= 60) return 'warning';
    return 'critical';
  };

  const getStatusColor = (status: string | null) => {
    switch (status) {
      case 'excellent':
        return 'text-green-600';
      case 'good':
        return 'text-blue-600';
      case 'warning':
        return 'text-yellow-600';
      case 'critical':
        return 'text-red-600';
      default:
        return 'text-gray-600';
    }
  };

  const formatValue = (value: number, unit?: string) => {
    const formattedValue = unit === '$' 
      ? `$${value.toFixed(1)}` 
      : `${value.toFixed(1)}${unit || ''}`;
    return formattedValue;
  };

  const categories = [
    { id: 'efficiency', name: 'Efficiency', icon: <Zap className="w-4 h-4" />, color: 'text-yellow-600' },
    { id: 'operations', name: 'Operations', icon: <Truck className="w-4 h-4" />, color: 'text-blue-600' },
    { id: 'satisfaction', name: 'Customer', icon: <Users className="w-4 h-4" />, color: 'text-purple-600' },
    { id: 'revenue', name: 'Financial', icon: <DollarSign className="w-4 h-4" />, color: 'text-green-600' }
  ];

  return (
    <div className={className}>
      <div className="mb-6">
        <div className="flex justify-between items-center mb-4">
          <div>
            <h2 className="text-2xl font-bold">Advanced Analytics</h2>
            <p className="text-gray-600">Real-time operational intelligence and performance insights</p>
          </div>
          <div className="flex items-center gap-2">
            <Select value={timeRange} onValueChange={setTimeRange}>
              <SelectTrigger className="w-[140px]">
                <SelectValue />
              </SelectTrigger>
              <SelectContent>
                <SelectItem value="7d">Last 7 days</SelectItem>
                <SelectItem value="30d">Last 30 days</SelectItem>
                <SelectItem value="90d">Last 90 days</SelectItem>
                <SelectItem value="1y">Last year</SelectItem>
              </SelectContent>
            </Select>
            <Button
              variant={autoRefresh ? 'default' : 'outline'}
              size="sm"
              onClick={() => setAutoRefresh(!autoRefresh)}
            >
              <RefreshCw className={`w-4 h-4 mr-2 ${autoRefresh ? 'animate-spin' : ''}`} />
              Auto Refresh
            </Button>
          </div>
        </div>
        
        {/* Last Update Indicator */}
        <div className="text-xs text-gray-500 mb-4">
          Last updated: {lastUpdate.toLocaleTimeString()}
          {autoRefresh && <span className="ml-2 text-green-600">• Live</span>}
        </div>
      </div>

      <Tabs defaultValue="overview" className="w-full">
        <TabsList className="grid w-full grid-cols-4">
          <TabsTrigger value="overview">Overview</TabsTrigger>
          <TabsTrigger value="performance">Performance</TabsTrigger>
          <TabsTrigger value="trends">Trends</TabsTrigger>
          <TabsTrigger value="forecasting">Forecasting</TabsTrigger>
        </TabsList>
        
        <TabsContent value="overview" className="space-y-6">
          {/* Category Metrics */}
          {categories.map(category => {
            const categoryMetrics = getMetricsByCategory(category.id);
            
            return (
              <Card key={category.id}>
                <CardHeader>
                  <CardTitle className={`flex items-center gap-2 ${category.color}`}>
                    {category.icon}
                    {category.name} Metrics
                  </CardTitle>
                </CardHeader>
                <CardContent>
                  <div className="grid gap-4 md:grid-cols-2 lg:grid-cols-4">
                    {categoryMetrics.map(metric => {
                      const targetStatus = getTargetStatus(metric.value, metric.target);
                      
                      return (
                        <div key={metric.name} className="p-4 border rounded-lg">
                          <div className="flex justify-between items-start mb-2">
                            <h4 className="text-sm font-medium text-gray-700">{metric.name}</h4>
                            {getTrendIcon(metric.trend)}
                          </div>
                          
                          <div className="space-y-1">
                            <div className="text-2xl font-bold">
                              {formatValue(metric.value, metric.unit)}
                            </div>
                            
                            <div className="flex items-center justify-between text-xs">
                              <span className={metric.change >= 0 ? 'text-green-600' : 'text-red-600'}>
                                {metric.change >= 0 ? '+' : ''}{metric.change.toFixed(1)}%
                              </span>
                              
                              {metric.target && (
                                <span className={getStatusColor(targetStatus)}>
                                  Target: {formatValue(metric.target, metric.unit)}
                                </span>
                              )}
                            </div>
                            
                            {metric.target && (
                              <div className="w-full bg-gray-200 rounded-full h-1.5 mt-2">
                                <div 
                                  className={`h-1.5 rounded-full ${
                                    targetStatus === 'excellent' ? 'bg-green-600' :
                                    targetStatus === 'good' ? 'bg-blue-600' :
                                    targetStatus === 'warning' ? 'bg-yellow-600' : 'bg-red-600'
                                  }`}
                                  style={{ width: `${Math.min((metric.value / metric.target) * 100, 100)}%` }}
                                ></div>
                              </div>
                            )}
                          </div>
                        </div>
                      );
                    })}
                  </div>
                </CardContent>
              </Card>
            );
          })}
        </TabsContent>
        
        <TabsContent value="performance" className="space-y-6">
          <div className="grid gap-6 md:grid-cols-2">
            {/* Real-time Performance Monitor */}
            <Card>
              <CardHeader>
                <CardTitle className="flex items-center gap-2">
                  <Activity className="w-5 h-5" />
                  Real-time Performance
                </CardTitle>
              </CardHeader>
              <CardContent>
                <div className="space-y-4">
                  <div className="flex justify-between items-center">
                    <span className="text-sm text-gray-600">System Load</span>
                    <span className="text-sm font-medium">78%</span>
                  </div>
                  <div className="w-full bg-gray-200 rounded-full h-2">
                    <div className="bg-blue-600 h-2 rounded-full" style={{ width: '78%' }}></div>
                  </div>
                  
                  <div className="flex justify-between items-center">
                    <span className="text-sm text-gray-600">API Response Time</span>
                    <span className="text-sm font-medium">124ms</span>
                  </div>
                  <div className="w-full bg-gray-200 rounded-full h-2">
                    <div className="bg-green-600 h-2 rounded-full" style={{ width: '85%' }}></div>
                  </div>
                  
                  <div className="flex justify-between items-center">
                    <span className="text-sm text-gray-600">Active Connections</span>
                    <span className="text-sm font-medium">1,247</span>
                  </div>
                  <div className="w-full bg-gray-200 rounded-full h-2">
                    <div className="bg-yellow-600 h-2 rounded-full" style={{ width: '62%' }}></div>
                  </div>
                </div>
              </CardContent>
            </Card>
            
            {/* Alert Summary */}
            <Card>
              <CardHeader>
                <CardTitle className="flex items-center gap-2">
                  <AlertTriangle className="w-5 h-5" />
                  Alert Summary
                </CardTitle>
              </CardHeader>
              <CardContent>
                <div className="space-y-3">
                  <div className="flex items-center justify-between p-3 bg-red-50 rounded-lg">
                    <div className="flex items-center gap-2">
                      <div className="w-2 h-2 bg-red-500 rounded-full"></div>
                      <span className="text-sm font-medium">Critical</span>
                    </div>
                    <Badge variant="destructive">2</Badge>
                  </div>
                  
                  <div className="flex items-center justify-between p-3 bg-yellow-50 rounded-lg">
                    <div className="flex items-center gap-2">
                      <div className="w-2 h-2 bg-yellow-500 rounded-full"></div>
                      <span className="text-sm font-medium">Warning</span>
                    </div>
                    <Badge variant="outline">5</Badge>
                  </div>
                  
                  <div className="flex items-center justify-between p-3 bg-blue-50 rounded-lg">
                    <div className="flex items-center gap-2">
                      <div className="w-2 h-2 bg-blue-500 rounded-full"></div>
                      <span className="text-sm font-medium">Info</span>
                    </div>
                    <Badge variant="secondary">12</Badge>
                  </div>
                </div>
              </CardContent>
            </Card>
          </div>
        </TabsContent>
        
        <TabsContent value="trends" className="space-y-6">
          <Card>
            <CardHeader>
              <CardTitle>Trend Analysis</CardTitle>
            </CardHeader>
            <CardContent>
              <div className="text-center py-8 text-gray-500">
                <LineChartIcon className="w-12 h-12 mx-auto mb-4 opacity-50" />
                <p>Advanced trend analysis charts</p>
                <p className="text-sm">Historical data patterns and seasonal trends</p>
              </div>
            </CardContent>
          </Card>
        </TabsContent>
        
        <TabsContent value="forecasting" className="space-y-6">
          <Card>
            <CardHeader>
              <CardTitle>Predictive Analytics</CardTitle>
            </CardHeader>
            <CardContent>
              <div className="text-center py-8 text-gray-500">
                <Target className="w-12 h-12 mx-auto mb-4 opacity-50" />
                <p>Machine learning forecasts</p>
                <p className="text-sm">Demand prediction and capacity planning</p>
              </div>
            </CardContent>
          </Card>
        </TabsContent>
      </Tabs>
    </div>
  );
};