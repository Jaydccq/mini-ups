/**
 * Admin Dashboard Page
 * 
 * Features:
 * - Comprehensive admin dashboard with KPI metrics
 * - Real-time system statistics and fleet overview
 * - Recent activities and system health monitoring
 * - Interactive charts and data visualization
 * - Responsive design for desktop and mobile
 * 
 * Components:
 * - DashboardStatistics: KPI cards for key metrics
 * - FleetOverview: Truck status and location overview
 * - RecentActivities: System activity timeline
 * - SystemHealth: Infrastructure health monitoring
 * - AnalyticsCharts: Business trend visualizations
 * 
 * @author Mini-UPS Team
 * @version 1.0.0
 */
import React, { useState, useEffect, useRef } from 'react';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Badge } from '@/components/ui/badge';
import { Button } from '@/components/ui/button';
import { Progress } from '@/components/ui/progress';
import { StatsCard } from '@/components/ui/stats-card';
import { SimpleChart } from '@/components/charts/SimpleChart';
import { FlashOnUpdate } from '@/components/ui/flash-on-update';
import { RealTimeFleetMap } from '@/components/admin/RealTimeFleetMap';
import { EmergencyIssuesPanel } from '@/components/admin/EmergencyIssuesPanel';
import { 
  Users, 
  Truck, 
  Package, 
  TrendingUp, 
  AlertTriangle, 
  Activity, 
  Shield,
  RefreshCw,
  MapPin,
  Clock,
  DollarSign,
  CheckCircle,
  XCircle,
  AlertCircle
} from 'lucide-react';

interface DashboardStatistics {
  orders: {
    total: number;
    active: number;
    completed: number;
    cancelled: number;
    completionRate: number;
  };
  fleet: {
    total: number;
    available: number;
    inTransit: number;
    maintenance: number;
    utilizationRate: number;
  };
  users: {
    total: number;
    admins: number;
    regular: number;
  };
  revenue: {
    today: number;
    thisWeek: number;
    thisMonth: number;
    growth: number;
  };
  lastUpdated: string;
}

interface Activity {
  id: string;
  action: string;
  entityType: string;
  entityId: string;
  userId: string;
  timestamp: string;
  details: string;
}

interface SystemHealth {
  database: {
    status: string;
    responseTime: number;
    connections: number;
    maxConnections: number;
  };
  redis: {
    status: string;
    responseTime: number;
    memoryUsage: number;
  };
  rabbitmq: {
    status: string;
    queueCount: number;
    messagesInQueue: number;
  };
  application: {
    uptime: string;
    activeUsers: number;
    requestsPerMinute: number;
    errorRate: number;
  };
  overallStatus: string;
  lastCheck: string;
}

const AdminDashboardPage: React.FC = () => {
  const [statistics, setStatistics] = useState<DashboardStatistics | null>(null);
  const [activities, setActivities] = useState<Activity[]>([]);
  const [systemHealth, setSystemHealth] = useState<SystemHealth | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [wsConnected, setWsConnected] = useState(false);
  const wsService = useRef<any>(null);

  const fetchDashboardData = async () => {
    try {
      setLoading(true);
      setError(null);
      
      // Fetch dashboard statistics
      const statsResponse = await fetch('/api/admin/dashboard/statistics', {
        headers: { 'Authorization': `Bearer ${localStorage.getItem('token')}` }
      });
      
      if (!statsResponse.ok) {
        throw new Error('Failed to fetch dashboard statistics');
      }
      
      const statsData = await statsResponse.json();
      setStatistics(statsData.data);
      
      // Fetch recent activities
      const activitiesResponse = await fetch('/api/admin/dashboard/activities', {
        headers: { 'Authorization': `Bearer ${localStorage.getItem('token')}` }
      });
      
      if (activitiesResponse.ok) {
        const activitiesData = await activitiesResponse.json();
        setActivities(activitiesData.data.activities);
      }
      
      // Fetch system health
      const healthResponse = await fetch('/api/admin/system/health', {
        headers: { 'Authorization': `Bearer ${localStorage.getItem('token')}` }
      });
      
      if (healthResponse.ok) {
        const healthData = await healthResponse.json();
        setSystemHealth(healthData.data);
      }
      
    } catch (error) {
      console.error('Error fetching dashboard data:', error);
      setError('Failed to load dashboard data');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    // Initial data fetch
    fetchDashboardData();
    
    let cleanupFn = () => {};
    
    // Set up WebSocket connection
    const initializeWebSocket = async () => {
      try {
        const { getWebSocketService } = await import('@/services/websocket');
        wsService.current = getWebSocketService();
        
        // Set up connection state callback
        wsService.current.onConnectionChange((connected: boolean) => {
          setWsConnected(connected);
        });
        
        // Connect to WebSocket
        await wsService.current.connect();
        
        // Subscribe to admin analytics updates
        const unsubscribeAnalytics = wsService.current.subscribe({
          destination: '/topic/admin/analytics',
          callback: (update: any) => {
            console.log('Received analytics update:', update);
            // Update statistics with real-time data
            if (update.actionCounts || update.entityTypeCounts) {
              setStatistics(prev => {
                if (!prev) return prev;
                return {
                  ...prev,
                  lastUpdated: update.timestamp || new Date().toISOString()
                };
              });
            }
          }
        });
        
        // Subscribe to system health updates
        const unsubscribeHealth = wsService.current.subscribe({
          destination: '/topic/admin/health',
          callback: (healthUpdate: any) => {
            console.log('Received health update:', healthUpdate);
            setSystemHealth(healthUpdate);
          }
        });
        
        // Subscribe to activity updates
        const unsubscribeActivity = wsService.current.subscribe({
          destination: '/topic/admin/activities',
          callback: (activityUpdate: any) => {
            console.log('Received activity update:', activityUpdate);
            setActivities(prev => [activityUpdate, ...prev.slice(0, 4)]);
          }
        });
        
        // Store unsubscribe functions for cleanup
        cleanupFn = () => {
          unsubscribeAnalytics();
          unsubscribeHealth();
          unsubscribeActivity();
          wsService.current?.disconnect();
        };
        
      } catch (error) {
        console.error('WebSocket initialization failed:', error);
        // Fallback to polling if WebSocket fails
        const interval = setInterval(fetchDashboardData, 30000);
        cleanupFn = () => clearInterval(interval);
      }
    };
    
    initializeWebSocket();
    
    // Return a synchronous cleanup function
    return () => {
      cleanupFn();
    };
  }, []);

  const handleRefresh = () => {
    fetchDashboardData();
  };

  const getStatusColor = (status: string) => {
    switch (status.toLowerCase()) {
      case 'up':
      case 'healthy':
        return 'bg-green-100 text-green-800';
      case 'down':
      case 'unhealthy':
        return 'bg-red-100 text-red-800';
      case 'warning':
        return 'bg-yellow-100 text-yellow-800';
      default:
        return 'bg-gray-100 text-gray-800';
    }
  };

  const getStatusIcon = (status: string) => {
    switch (status.toLowerCase()) {
      case 'up':
      case 'healthy':
        return <CheckCircle className="w-4 h-4" />;
      case 'down':
      case 'unhealthy':
        return <XCircle className="w-4 h-4" />;
      case 'warning':
        return <AlertCircle className="w-4 h-4" />;
      default:
        return <Activity className="w-4 h-4" />;
    }
  };

  if (loading) {
    return (
      <div className="flex items-center justify-center h-64">
        <div className="flex items-center space-x-2">
          <RefreshCw className="w-5 h-5 animate-spin" />
          <span>Loading dashboard...</span>
        </div>
      </div>
    );
  }

  if (error) {
    return (
      <div className="flex items-center justify-center h-64">
        <div className="text-center">
          <AlertTriangle className="w-12 h-12 text-red-500 mx-auto mb-4" />
          <p className="text-red-600 mb-4">{error}</p>
          <Button onClick={handleRefresh} variant="outline">
            Try Again
          </Button>
        </div>
      </div>
    );
  }

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex justify-between items-center">
        <div>
          <h1 className="text-3xl font-bold">Admin Dashboard</h1>
          <p className="text-gray-600">System overview and key metrics</p>
        </div>
        <div className="flex items-center space-x-2">
          <Badge variant="outline" className={`text-sm ${wsConnected ? 'bg-green-50 text-green-800' : 'bg-red-50 text-red-800'}`}>
            <Activity className="w-4 h-4 mr-1" />
            {wsConnected ? 'Live' : 'Disconnected'}
          </Badge>
          <Button onClick={handleRefresh} variant="outline" size="sm">
            <RefreshCw className="w-4 h-4 mr-1" />
            Refresh
          </Button>
        </div>
      </div>

      {/* KPI Statistics */}
      {statistics && (
        <FlashOnUpdate dependencies={[statistics]}>
          <div className="grid gap-4 md:grid-cols-2 lg:grid-cols-4">
            <StatsCard
              title="Total Orders"
              value={statistics.orders.total}
              icon={<Package className="w-6 h-6" />}
              trend={{ value: statistics.orders.completionRate, label: 'Completion Rate' }}
            />
            <StatsCard
              title="Active Fleet"
              value={statistics.fleet.inTransit}
              icon={<Truck className="w-6 h-6" />}
              trend={{ value: statistics.fleet.utilizationRate, label: 'Utilization Rate' }}
            />
            <StatsCard
              title="Total Users"
              value={statistics.users.total}
              icon={<Users className="w-6 h-6" />}
              trend={{ value: statistics.users.admins, label: 'Admins' }}
            />
            <StatsCard
              title="Daily Revenue"
              value={`$${statistics.revenue.today.toLocaleString()}`}
              icon={<DollarSign className="w-6 h-6" />}
              trend={{ value: statistics.revenue.growth, label: 'Growth' }}
            />
          </div>
        </FlashOnUpdate>
      )}

      {/* Charts and Fleet Overview */}
      <div className="grid gap-6 md:grid-cols-2">
        {/* Order Status Distribution */}
        {statistics && (
          <Card>
            <CardHeader>
              <CardTitle className="flex items-center">
                <Package className="w-5 h-5 mr-2" />
                Order Status Distribution
              </CardTitle>
            </CardHeader>
            <CardContent>
              <div className="space-y-3">
                <div className="flex justify-between items-center">
                  <span className="text-sm text-gray-600">Active Orders</span>
                  <Badge variant="secondary">{statistics.orders.active}</Badge>
                </div>
                <Progress value={statistics.orders.total > 0 ? (statistics.orders.active / statistics.orders.total) * 100 : 0} />
                
                <div className="flex justify-between items-center">
                  <span className="text-sm text-gray-600">Completed</span>
                  <Badge variant="default">{statistics.orders.completed}</Badge>
                </div>
                <Progress value={statistics.orders.total > 0 ? (statistics.orders.completed / statistics.orders.total) * 100 : 0} />
                
                <div className="flex justify-between items-center">
                  <span className="text-sm text-gray-600">Cancelled</span>
                  <Badge variant="destructive">{statistics.orders.cancelled}</Badge>
                </div>
                <Progress value={statistics.orders.total > 0 ? (statistics.orders.cancelled / statistics.orders.total) * 100 : 0} />
              </div>
            </CardContent>
          </Card>
        )}

        {/* Fleet Status */}
        {statistics && (
          <Card>
            <CardHeader>
              <CardTitle className="flex items-center">
                <Truck className="w-5 h-5 mr-2" />
                Fleet Status
              </CardTitle>
            </CardHeader>
            <CardContent>
              <div className="space-y-4">
                <div className="flex justify-between items-center">
                  <div className="flex items-center space-x-2">
                    <div className="w-3 h-3 bg-green-500 rounded-full"></div>
                    <span className="text-sm">Available</span>
                  </div>
                  <span className="font-semibold">{statistics.fleet.available}</span>
                </div>
                
                <div className="flex justify-between items-center">
                  <div className="flex items-center space-x-2">
                    <div className="w-3 h-3 bg-blue-500 rounded-full"></div>
                    <span className="text-sm">In Transit</span>
                  </div>
                  <span className="font-semibold">{statistics.fleet.inTransit}</span>
                </div>
                
                <div className="flex justify-between items-center">
                  <div className="flex items-center space-x-2">
                    <div className="w-3 h-3 bg-yellow-500 rounded-full"></div>
                    <span className="text-sm">Maintenance</span>
                  </div>
                  <span className="font-semibold">{statistics.fleet.maintenance}</span>
                </div>
                
                <div className="pt-2 border-t">
                  <div className="flex justify-between items-center">
                    <span className="text-sm font-medium">Utilization Rate</span>
                    <span className="text-sm font-semibold">{statistics.fleet.utilizationRate.toFixed(1)}%</span>
                  </div>
                  <Progress value={statistics.fleet.utilizationRate} className="mt-1" />
                </div>
              </div>
            </CardContent>
          </Card>
        )}
      </div>

      {/* Real-Time Fleet Map and Emergency Issues */}
      <div className="grid gap-6 lg:grid-cols-2">
        <RealTimeFleetMap />
        <EmergencyIssuesPanel />
      </div>

      {/* Recent Activities and System Health */}
      <div className="grid gap-6 md:grid-cols-2">
        {/* Recent Activities */}
        <Card>
          <CardHeader>
            <CardTitle className="flex items-center">
              <Clock className="w-5 h-5 mr-2" />
              Recent Activities
            </CardTitle>
          </CardHeader>
          <CardContent>
            <div className="space-y-3">
              {activities.length > 0 ? (
                activities.slice(0, 5).map((activity) => (
                  <div key={activity.id} className="flex items-start space-x-3 p-2 rounded-lg bg-gray-50">
                    <div className="flex-shrink-0 w-2 h-2 bg-blue-500 rounded-full mt-2"></div>
                    <div className="flex-1 min-w-0">
                      <p className="text-sm font-medium text-gray-900">
                        {activity.action} {activity.entityType}
                      </p>
                      <p className="text-xs text-gray-500 truncate">
                        {activity.details || `ID: ${activity.entityId}`}
                      </p>
                      <p className="text-xs text-gray-400">
                        {new Date(activity.timestamp).toLocaleString()}
                      </p>
                    </div>
                  </div>
                ))
              ) : (
                <p className="text-gray-500 text-center py-4">No recent activities</p>
              )}
            </div>
          </CardContent>
        </Card>

        {/* System Health */}
        {systemHealth && (
          <Card>
            <CardHeader>
              <CardTitle className="flex items-center">
                <Shield className="w-5 h-5 mr-2" />
                System Health
              </CardTitle>
            </CardHeader>
            <CardContent>
              <div className="space-y-3">
                <div className="flex justify-between items-center">
                  <span className="text-sm font-medium">Overall Status</span>
                  <Badge className={getStatusColor(systemHealth.overallStatus)}>
                    {getStatusIcon(systemHealth.overallStatus)}
                    <span className="ml-1">{systemHealth.overallStatus}</span>
                  </Badge>
                </div>
                
                <div className="space-y-2">
                  <div className="flex justify-between items-center">
                    <span className="text-sm">Database</span>
                    <Badge variant="outline" className={getStatusColor(systemHealth.database.status)}>
                      {systemHealth.database.status}
                    </Badge>
                  </div>
                  
                  <div className="flex justify-between items-center">
                    <span className="text-sm">Redis Cache</span>
                    <Badge variant="outline" className={getStatusColor(systemHealth.redis.status)}>
                      {systemHealth.redis.status}
                    </Badge>
                  </div>
                  
                  <div className="flex justify-between items-center">
                    <span className="text-sm">Message Queue</span>
                    <Badge variant="outline" className={getStatusColor(systemHealth.rabbitmq.status)}>
                      {systemHealth.rabbitmq.status}
                    </Badge>
                  </div>
                </div>
                
                <div className="pt-2 border-t">
                  <div className="text-xs text-gray-500">
                    <p>Active Users: {systemHealth.application.activeUsers}</p>
                    <p>Uptime: {systemHealth.application.uptime}</p>
                    <p>Error Rate: {(systemHealth.application.errorRate * 100).toFixed(2)}%</p>
                  </div>
                </div>
              </div>
            </CardContent>
          </Card>
        )}
      </div>
    </div>
  );
};

export default AdminDashboardPage;