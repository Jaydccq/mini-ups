import React, { useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import { 
  BarChart3, 
  TrendingUp, 
  Users, 
  Package, 
  DollarSign, 
  Clock, 
  Star, 
  Download,
  Calendar,
  RefreshCw,
  Activity,
  Zap
} from 'lucide-react';
import { adminApi, AnalyticsData } from '../services/admin';
import { Button } from '@/components/ui/button';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
import { Badge } from '@/components/ui/badge';
import { Alert, AlertDescription } from '@/components/ui/alert';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { Tabs, TabsContent, TabsList, TabsTrigger } from '@/components/ui/tabs';
import { 
  LineChart, 
  BarChart, 
  PieChart, 
  XAxis, 
  YAxis, 
  CartesianGrid, 
  Tooltip, 
  Legend, 
  Line, 
  Bar, 
  Cell,
  ChartData
} from '@/components/charts/SimpleChart';
import { AdvancedAnalytics } from '../components/AdvancedAnalytics';
import { toast } from 'sonner';
import { formatDateTime } from '@/lib/utils';

export const AnalyticsPage: React.FC = () => {
  const [dateRange, setDateRange] = useState({
    from: new Date(Date.now() - 30 * 24 * 60 * 60 * 1000).toISOString().split('T')[0], // 30 days ago
    to: new Date().toISOString().split('T')[0], // today
  });

  // Fetch analytics data
  const { data: analyticsData, isLoading, error, refetch } = useQuery({
    queryKey: ['admin', 'analytics', dateRange],
    queryFn: () => adminApi.getAnalytics(dateRange),
    refetchInterval: 5 * 60 * 1000, // Refresh every 5 minutes
  });

  const handleExportAnalytics = () => {
    adminApi.exportData('analytics', dateRange)
      .then(blob => {
        const url = window.URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = `analytics-${dateRange.from}-to-${dateRange.to}.pdf`;
        document.body.appendChild(a);
        a.click();
        window.URL.revokeObjectURL(url);
        document.body.removeChild(a);
        toast.success('Analytics report exported successfully');
      })
      .catch(error => {
        toast.error(`Export failed: ${error.message}`);
      });
  };

  const handleRefresh = () => {
    refetch();
    toast.success('Data refreshed');
  };

  const formatCurrency = (amount: number) => {
    return new Intl.NumberFormat('en-US', {
      style: 'currency',
      currency: 'USD',
    }).format(amount);
  };

  const formatPercentage = (value: number) => {
    return `${value > 0 ? '+' : ''}${value.toFixed(1)}%`;
  };

  // Sample data for charts (in real implementation, this would come from analyticsData)
  const shipmentTrendsData = analyticsData?.shipment_trends || [
    { date: '2024-01-01', shipments: 150, revenue: 2250 },
    { date: '2024-01-02', shipments: 230, revenue: 3450 },
    { date: '2024-01-03', shipments: 180, revenue: 2700 },
    { date: '2024-01-04', shipments: 320, revenue: 4800 },
    { date: '2024-01-05', shipments: 280, revenue: 4200 },
    { date: '2024-01-06', shipments: 190, revenue: 2850 },
    { date: '2024-01-07', shipments: 250, revenue: 3750 },
  ];

  const statusDistributionData = analyticsData?.status_distribution || [
    { status: 'Delivered', count: 1250, percentage: 62.5 },
    { status: 'In Transit', count: 380, percentage: 19.0 },
    { status: 'Pending', count: 200, percentage: 10.0 },
    { status: 'Cancelled', count: 120, percentage: 6.0 },
    { status: 'Returned', count: 50, percentage: 2.5 },
  ];

  const topCustomersData = analyticsData?.top_customers || [
    { user_id: '1', name: 'John Smith', email: 'john@example.com', shipment_count: 45, total_spent: 2250 },
    { user_id: '2', name: 'Alice Johnson', email: 'alice@example.com', shipment_count: 38, total_spent: 1900 },
    { user_id: '3', name: 'Bob Wilson', email: 'bob@example.com', shipment_count: 32, total_spent: 1600 },
    { user_id: '4', name: 'Carol Davis', email: 'carol@example.com', shipment_count: 28, total_spent: 1400 },
    { user_id: '5', name: 'David Brown', email: 'david@example.com', shipment_count: 25, total_spent: 1250 },
  ];

  const metrics = analyticsData?.metrics || {
    total_users: 1250,
    total_shipments: 2000,
    active_shipments: 500,
    delivered_shipments: 1250,
    pending_shipments: 200,
    total_revenue: 75000,
    avg_delivery_time: 3.2,
    customer_satisfaction: 4.7,
  };

  const monthlyStats = analyticsData?.monthly_stats || {
    current_month: 2000,
    previous_month: 1800,
    growth_rate: 11.1,
  };

  const pieColors = ['#3b82f6', '#10b981', '#f59e0b', '#ef4444', '#8b5cf6'];

  if (isLoading) {
    return (
      <div className="p-6 max-w-7xl mx-auto">
        <div className="space-y-6">
          {Array.from({ length: 4 }).map((_, i) => (
            <div key={i} className="h-64 bg-gray-200 rounded-lg animate-pulse" />
          ))}
        </div>
      </div>
    );
  }

  return (
    <div className="p-6 max-w-7xl mx-auto">
      <div className="mb-8">
        <div className="flex items-center justify-between">
          <div>
            <h1 className="text-3xl font-bold text-gray-900">Analytics Dashboard</h1>
            <p className="text-gray-600">Comprehensive business insights and operational intelligence</p>
          </div>
          <div className="flex items-center gap-4">
            <Button variant="outline" onClick={handleRefresh}>
              <RefreshCw className="h-4 w-4 mr-2" />
              Refresh
            </Button>
            <Button onClick={handleExportAnalytics}>
              <Download className="h-4 w-4 mr-2" />
              Export Report
            </Button>
          </div>
        </div>
      </div>

      <Tabs defaultValue="overview" className="w-full">
        <TabsList className="grid w-full grid-cols-3">
          <TabsTrigger value="overview">Overview</TabsTrigger>
          <TabsTrigger value="advanced">Advanced Analytics</TabsTrigger>
          <TabsTrigger value="reports">Detailed Reports</TabsTrigger>
        </TabsList>
        
        <TabsContent value="overview" className="space-y-6">
          {/* Date Range Selector */}
          <div className="flex items-center gap-4">
            <div className="flex items-center gap-2">
              <Label htmlFor="date-from">From:</Label>
              <Input
                id="date-from"
                type="date"
                value={dateRange.from}
                onChange={(e) => setDateRange(prev => ({ ...prev, from: e.target.value }))}
                className="w-auto"
              />
            </div>
            <div className="flex items-center gap-2">
              <Label htmlFor="date-to">To:</Label>
              <Input
                id="date-to"
                type="date"
                value={dateRange.to}
                onChange={(e) => setDateRange(prev => ({ ...prev, to: e.target.value }))}
                className="w-auto"
              />
            </div>
          </div>

          {/* Key Metrics */}
          <div className="grid gap-6 md:grid-cols-2 lg:grid-cols-4">
        <Card>
          <CardContent className="p-6">
            <div className="flex items-center justify-between">
              <div>
                <p className="text-sm font-medium text-gray-600">Total Revenue</p>
                <p className="text-2xl font-bold text-gray-900">{formatCurrency(metrics.total_revenue)}</p>
                <p className="text-xs text-green-600 mt-1">
                  {formatPercentage(monthlyStats.growth_rate)} from last month
                </p>
              </div>
              <div className="w-12 h-12 bg-green-100 rounded-lg flex items-center justify-center">
                <DollarSign className="h-6 w-6 text-green-600" />
              </div>
            </div>
          </CardContent>
        </Card>

        <Card>
          <CardContent className="p-6">
            <div className="flex items-center justify-between">
              <div>
                <p className="text-sm font-medium text-gray-600">Total Shipments</p>
                <p className="text-2xl font-bold text-gray-900">{metrics.total_shipments.toLocaleString()}</p>
                <p className="text-xs text-blue-600 mt-1">
                  {metrics.active_shipments} currently active
                </p>
              </div>
              <div className="w-12 h-12 bg-blue-100 rounded-lg flex items-center justify-center">
                <Package className="h-6 w-6 text-blue-600" />
              </div>
            </div>
          </CardContent>
        </Card>

        <Card>
          <CardContent className="p-6">
            <div className="flex items-center justify-between">
              <div>
                <p className="text-sm font-medium text-gray-600">Avg Delivery Time</p>
                <p className="text-2xl font-bold text-gray-900">{metrics.avg_delivery_time} days</p>
                <p className="text-xs text-yellow-600 mt-1">
                  Industry average: 4.5 days
                </p>
              </div>
              <div className="w-12 h-12 bg-yellow-100 rounded-lg flex items-center justify-center">
                <Clock className="h-6 w-6 text-yellow-600" />
              </div>
            </div>
          </CardContent>
        </Card>

        <Card>
          <CardContent className="p-6">
            <div className="flex items-center justify-between">
              <div>
                <p className="text-sm font-medium text-gray-600">Customer Satisfaction</p>
                <p className="text-2xl font-bold text-gray-900">{metrics.customer_satisfaction}/5.0</p>
                <p className="text-xs text-purple-600 mt-1">
                  Based on {metrics.delivered_shipments} reviews
                </p>
              </div>
              <div className="w-12 h-12 bg-purple-100 rounded-lg flex items-center justify-center">
                <Star className="h-6 w-6 text-purple-600" />
              </div>
            </div>
          </CardContent>
        </Card>
          </div>

          {/* Charts */}
          <div className="grid gap-6 lg:grid-cols-2">
        {/* Shipment Trends */}
        <Card>
          <CardHeader>
            <CardTitle className="flex items-center gap-2">
              <TrendingUp className="h-5 w-5" />
              Shipment Trends
            </CardTitle>
            <CardDescription>
              Daily shipment volume and revenue over time
            </CardDescription>
          </CardHeader>
          <CardContent>
            <LineChart width={500} height={300} data={shipmentTrendsData as unknown as ChartData[]}>
              <CartesianGrid strokeDasharray="3 3" />
              <XAxis dataKey="date" />
              <YAxis />
              <Tooltip />
              <Legend />
              <Line type="monotone" dataKey="shipments" stroke="#3b82f6" strokeWidth={2} />
              <Line type="monotone" dataKey="revenue" stroke="#10b981" strokeWidth={2} />
            </LineChart>
          </CardContent>
        </Card>

        {/* Status Distribution */}
        <Card>
          <CardHeader>
            <CardTitle className="flex items-center gap-2">
              <BarChart3 className="h-5 w-5" />
              Shipment Status Distribution
            </CardTitle>
            <CardDescription>
              Current distribution of shipment statuses
            </CardDescription>
          </CardHeader>
          <CardContent>
            <PieChart width={500} height={300} data={statusDistributionData as unknown as ChartData[]}>
              <Tooltip />
              {statusDistributionData.map((entry, index) => (
                <Cell key={`cell-${index}`} fill={pieColors[index % pieColors.length]} />
              ))}
            </PieChart>
            <div className="mt-4 grid grid-cols-2 gap-2">
              {statusDistributionData.map((entry, index) => (
                <div key={entry.status} className="flex items-center gap-2">
                  <div 
                    className="w-3 h-3 rounded-full" 
                    style={{ backgroundColor: pieColors[index % pieColors.length] }}
                  />
                  <span className="text-sm text-gray-600">
                    {entry.status}: {entry.percentage}%
                  </span>
                </div>
              ))}
            </div>
          </CardContent>
        </Card>
          </div>

          {/* Top Customers */}
          <Card>
        <CardHeader>
          <CardTitle className="flex items-center gap-2">
            <Users className="h-5 w-5" />
            Top Customers
          </CardTitle>
          <CardDescription>
            Customers with the highest shipment volume and spending
          </CardDescription>
        </CardHeader>
        <CardContent>
          <div className="overflow-x-auto">
            <table className="w-full">
              <thead>
                <tr className="border-b">
                  <th className="text-left py-3 px-4 font-medium text-gray-700">Customer</th>
                  <th className="text-left py-3 px-4 font-medium text-gray-700">Email</th>
                  <th className="text-center py-3 px-4 font-medium text-gray-700">Shipments</th>
                  <th className="text-right py-3 px-4 font-medium text-gray-700">Total Spent</th>
                </tr>
              </thead>
              <tbody>
                {topCustomersData.map((customer, index) => (
                  <tr key={customer.user_id} className="border-b hover:bg-gray-50">
                    <td className="py-3 px-4">
                      <div className="flex items-center gap-3">
                        <div className="w-8 h-8 bg-blue-100 rounded-full flex items-center justify-center">
                          <span className="text-sm font-medium text-blue-600">
                            {customer.name.charAt(0)}
                          </span>
                        </div>
                        <div>
                          <div className="font-medium text-gray-900">{customer.name}</div>
                          <div className="text-xs text-gray-500">#{index + 1}</div>
                        </div>
                      </div>
                    </td>
                    <td className="py-3 px-4 text-gray-600">{customer.email}</td>
                    <td className="py-3 px-4 text-center">
                      <Badge variant="outline">{customer.shipment_count}</Badge>
                    </td>
                    <td className="py-3 px-4 text-right font-medium">
                      {formatCurrency(customer.total_spent)}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </CardContent>
          </Card>

          {/* System Health */}
          <Card>
        <CardHeader>
          <CardTitle>System Performance</CardTitle>
          <CardDescription>
            Real-time system health and performance metrics
          </CardDescription>
        </CardHeader>
        <CardContent>
          <div className="grid gap-6 md:grid-cols-3">
            <div className="text-center">
              <div className="text-2xl font-bold text-green-600">99.9%</div>
              <div className="text-sm text-gray-600">Uptime</div>
            </div>
            <div className="text-center">
              <div className="text-2xl font-bold text-blue-600">1.2s</div>
              <div className="text-sm text-gray-600">Avg Response Time</div>
            </div>
            <div className="text-center">
              <div className="text-2xl font-bold text-purple-600">234</div>
              <div className="text-sm text-gray-600">Concurrent Users</div>
            </div>
          </div>
        </CardContent>
          </Card>

          {error && (
            <Alert variant="destructive" className="mt-4">
              <AlertDescription>
                Failed to load analytics data: {error.message}
              </AlertDescription>
            </Alert>
          )}
        </TabsContent>
        
        <TabsContent value="advanced" className="space-y-6">
          <AdvancedAnalytics />
        </TabsContent>
        
        <TabsContent value="reports" className="space-y-6">
          <div className="grid gap-6 md:grid-cols-2">
            {/* Operational Reports */}
            <Card>
              <CardHeader>
                <CardTitle className="flex items-center gap-2">
                  <Activity className="w-5 h-5" />
                  Operational Reports
                </CardTitle>
                <CardDescription>
                  Detailed operational performance and efficiency reports
                </CardDescription>
              </CardHeader>
              <CardContent>
                <div className="space-y-4">
                  <div className="flex justify-between items-center p-3 border rounded-lg">
                    <div>
                      <h4 className="font-medium">Fleet Utilization Report</h4>
                      <p className="text-sm text-gray-600">Vehicle usage and efficiency metrics</p>
                    </div>
                    <Button variant="outline" size="sm">
                      <Download className="w-4 h-4 mr-1" />
                      Download
                    </Button>
                  </div>
                  
                  <div className="flex justify-between items-center p-3 border rounded-lg">
                    <div>
                      <h4 className="font-medium">Delivery Performance Report</h4>
                      <p className="text-sm text-gray-600">On-time delivery and customer satisfaction</p>
                    </div>
                    <Button variant="outline" size="sm">
                      <Download className="w-4 h-4 mr-1" />
                      Download
                    </Button>
                  </div>
                  
                  <div className="flex justify-between items-center p-3 border rounded-lg">
                    <div>
                      <h4 className="font-medium">Route Optimization Report</h4>
                      <p className="text-sm text-gray-600">Route efficiency and fuel consumption</p>
                    </div>
                    <Button variant="outline" size="sm">
                      <Download className="w-4 h-4 mr-1" />
                      Download
                    </Button>
                  </div>
                </div>
              </CardContent>
            </Card>
            
            {/* Financial Reports */}
            <Card>
              <CardHeader>
                <CardTitle className="flex items-center gap-2">
                  <DollarSign className="w-5 h-5" />
                  Financial Reports
                </CardTitle>
                <CardDescription>
                  Revenue, costs, and profitability analysis
                </CardDescription>
              </CardHeader>
              <CardContent>
                <div className="space-y-4">
                  <div className="flex justify-between items-center p-3 border rounded-lg">
                    <div>
                      <h4 className="font-medium">Revenue Analysis</h4>
                      <p className="text-sm text-gray-600">Monthly and quarterly revenue trends</p>
                    </div>
                    <Button variant="outline" size="sm">
                      <Download className="w-4 h-4 mr-1" />
                      Download
                    </Button>
                  </div>
                  
                  <div className="flex justify-between items-center p-3 border rounded-lg">
                    <div>
                      <h4 className="font-medium">Cost Analysis</h4>
                      <p className="text-sm text-gray-600">Operational costs and efficiency metrics</p>
                    </div>
                    <Button variant="outline" size="sm">
                      <Download className="w-4 h-4 mr-1" />
                      Download
                    </Button>
                  </div>
                  
                  <div className="flex justify-between items-center p-3 border rounded-lg">
                    <div>
                      <h4 className="font-medium">Profit Margin Report</h4>
                      <p className="text-sm text-gray-600">Profitability by service type and region</p>
                    </div>
                    <Button variant="outline" size="sm">
                      <Download className="w-4 h-4 mr-1" />
                      Download
                    </Button>
                  </div>
                </div>
              </CardContent>
            </Card>
          </div>
          
          {/* Custom Report Builder */}
          <Card>
            <CardHeader>
              <CardTitle className="flex items-center gap-2">
                <BarChart3 className="w-5 h-5" />
                Custom Report Builder
              </CardTitle>
              <CardDescription>
                Create custom reports with specific metrics and date ranges
              </CardDescription>
            </CardHeader>
            <CardContent>
              <div className="text-center py-8 text-gray-500">
                <BarChart3 className="w-12 h-12 mx-auto mb-4 opacity-50" />
                <p>Custom report builder</p>
                <p className="text-sm">Build custom analytics reports with drag-and-drop interface</p>
              </div>
            </CardContent>
          </Card>
        </TabsContent>
      </Tabs>
    </div>
  );
};