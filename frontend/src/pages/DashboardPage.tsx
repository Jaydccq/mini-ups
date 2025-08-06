import React, { useState, useEffect } from 'react'
import { Link } from 'react-router-dom'
import { Package, TrendingUp, Clock, CheckCircle, Plus, RefreshCw, AlertCircle, Search, Bell, Settings, MapPin, Calendar, Activity } from 'lucide-react'
import { useAuthStore } from '@/stores/auth-store'
import { shipmentApi, DashboardStats } from '@/services/shipment'
import { StatsCard } from '@/components/ui/stats-card'
import { RecentShipments } from '@/components/dashboard/RecentShipments'
import { Button } from '@/components/ui/button'
import { Alert, AlertDescription } from '@/components/ui/alert'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { Badge } from '@/components/ui/badge'
import { Avatar, AvatarFallback } from '@/components/ui/avatar'
import { Input } from '@/components/ui/input'
import { toast } from 'sonner'

export const DashboardPage: React.FC = () => {
  const { user } = useAuthStore()
  const [stats, setStats] = useState<DashboardStats | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [searchTerm, setSearchTerm] = useState('')

  const fetchDashboardData = async () => {
    if (!user) return
    
    try {
      setLoading(true)
      setError(null)
      const dashboardStats = await shipmentApi.getDashboardStats(parseInt(user.id))
      setStats(dashboardStats)
    } catch (err: unknown) {
      console.error('Failed to fetch dashboard data:', err)
      const errorMessage = err instanceof Error ? err.message : 'Failed to load dashboard data'
      setError(errorMessage)
      toast.error('Failed to load dashboard data')
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    fetchDashboardData()
  }, [user])

  const handleRefresh = () => {
    fetchDashboardData()
  }

  if (!user) {
    return (
      <div className="p-8">
        <Alert variant="destructive">
          <AlertCircle className="h-4 w-4" />
          <AlertDescription>
            Please log in to view your dashboard.
          </AlertDescription>
        </Alert>
      </div>
    )
  }

  return (
    <div className="min-h-screen bg-gray-50/50">
      <div className="p-6 max-w-7xl mx-auto">
        {/* Enhanced Header */}
        <div className="bg-white rounded-lg shadow-sm border mb-8">
          <div className="p-6">
            <div className="flex items-center justify-between">
              <div className="flex items-center gap-4">
                <Avatar className="h-12 w-12">
                  <AvatarFallback className="bg-primary text-primary-foreground">
                    {user.name.split(' ').map(n => n[0]).join('').toUpperCase()}
                  </AvatarFallback>
                </Avatar>
                <div>
                  <h1 className="text-2xl font-bold text-gray-900">
                    Welcome back, {user.name}
                  </h1>
                  <p className="text-gray-600 flex items-center gap-2 mt-1">
                    <Badge variant="secondary" className="px-2 py-1">
                      {user.role.toLowerCase()}
                    </Badge>
                    <span className="text-sm">•</span>
                    <span className="text-sm">Last login: Today</span>
                  </p>
                </div>
              </div>
              <div className="flex items-center gap-3">
                <Button
                  variant="outline"
                  size="sm"
                  onClick={handleRefresh}
                  disabled={loading}
                >
                  <RefreshCw className={`h-4 w-4 mr-2 ${loading ? 'animate-spin' : ''}`} />
                  Refresh
                </Button>
                <Link to="/shipments/create">
                  <Button size="sm" className="bg-primary hover:bg-primary/90">
                    <Plus className="h-4 w-4 mr-2" />
                    New Shipment
                  </Button>
                </Link>
              </div>
            </div>
            
            {/* Quick Search */}
            <div className="mt-6 max-w-md">
              <div className="relative">
                <Search className="absolute left-3 top-3 h-4 w-4 text-gray-400" />
                <Input
                  placeholder="Search shipments, tracking numbers..."
                  value={searchTerm}
                  onChange={(e) => setSearchTerm(e.target.value)}
                  className="pl-10"
                />
              </div>
            </div>
          </div>
        </div>

      {/* Error State */}
      {error && (
        <Alert variant="destructive" className="mb-6">
          <AlertCircle className="h-4 w-4" />
          <AlertDescription>
            {error}
          </AlertDescription>
        </Alert>
      )}

        {/* Enhanced Stats Cards */}
        <div className="grid gap-6 md:grid-cols-2 lg:grid-cols-4 mb-8">
          <StatsCard
            title="Total Shipments"
            value={loading ? '-' : stats?.total_shipments || 0}
            description="All time shipments"
            icon={Package}
            trend={{
              value: 12.5,
              label: 'vs last month',
              type: 'positive'
            }}
            className={loading ? 'animate-pulse' : 'hover:shadow-md transition-shadow'}
          />
          <StatsCard
            title="Active Shipments"
            value={loading ? '-' : stats?.active_shipments || 0}
            description="Currently in transit"
            icon={TrendingUp}
            trend={{
              value: 8.2,
              label: 'vs last week',
              type: 'positive'
            }}
            className={loading ? 'animate-pulse' : 'hover:shadow-md transition-shadow'}
          />
          <StatsCard
            title="Pending Pickup"
            value={loading ? '-' : stats?.pending_shipments || 0}
            description="Waiting for pickup"
            icon={Clock}
            trend={{
              value: -5.1,
              label: 'vs yesterday',
              type: 'negative'
            }}
            className={loading ? 'animate-pulse' : 'hover:shadow-md transition-shadow'}
          />
          <StatsCard
            title="Delivered"
            value={loading ? '-' : stats?.delivered_shipments || 0}
            description="Successfully delivered"
            icon={CheckCircle}
            trend={{
              value: 15.3,
              label: 'vs last month',
              type: 'positive'
            }}
            className={loading ? 'animate-pulse' : 'hover:shadow-md transition-shadow'}
          />
        </div>

        {/* Main Content Grid */}
        <div className="grid gap-6 lg:grid-cols-3">
          {/* Recent Shipments */}
          <div className="lg:col-span-2">
            <Card>
              <CardHeader>
                <CardTitle className="flex items-center gap-2">
                  <Activity className="h-5 w-5" />
                  Recent Shipments
                </CardTitle>
              </CardHeader>
              <CardContent>
                <RecentShipments 
                  shipments={stats?.recent_shipments || []} 
                  loading={loading}
                />
              </CardContent>
            </Card>
          </div>
          
          {/* Sidebar */}
          <div className="space-y-6">
            {/* Quick Actions */}
            <Card>
              <CardHeader>
                <CardTitle className="flex items-center gap-2">
                  <Plus className="h-5 w-5" />
                  Quick Actions
                </CardTitle>
              </CardHeader>
              <CardContent className="space-y-3">
                <Link to="/shipments/create" className="block">
                  <Button className="w-full justify-start" variant="outline" size="sm">
                    <Plus className="h-4 w-4 mr-2" />
                    Create New Shipment
                  </Button>
                </Link>
                <Link to="/tracking" className="block">
                  <Button className="w-full justify-start" variant="outline" size="sm">
                    <Search className="h-4 w-4 mr-2" />
                    Track Package
                  </Button>
                </Link>
                <Link to="/shipments" className="block">
                  <Button className="w-full justify-start" variant="outline" size="sm">
                    <Package className="h-4 w-4 mr-2" />
                    View All Shipments
                  </Button>
                </Link>
                <Link to="/profile" className="block">
                  <Button className="w-full justify-start" variant="outline" size="sm">
                    <Settings className="h-4 w-4 mr-2" />
                    Account Settings
                  </Button>
                </Link>
              </CardContent>
            </Card>
            
            {/* Account Summary */}
            <Card>
              <CardHeader>
                <CardTitle className="flex items-center gap-2">
                  <Settings className="h-5 w-5" />
                  Account Summary
                </CardTitle>
              </CardHeader>
              <CardContent className="space-y-3">
                <div className="flex justify-between text-sm">
                  <span className="text-muted-foreground">Account Type</span>
                  <Badge variant="secondary" className="capitalize">
                    {user.role.toLowerCase()}
                  </Badge>
                </div>
                <div className="flex justify-between text-sm">
                  <span className="text-muted-foreground">Email</span>
                  <span className="font-medium truncate max-w-32">{user.email}</span>
                </div>
                <div className="flex justify-between text-sm">
                  <span className="text-muted-foreground">Total Shipments</span>
                  <span className="font-medium">{stats?.total_shipments || 0}</span>
                </div>
                <div className="flex justify-between text-sm">
                  <span className="text-muted-foreground">Status</span>
                  <Badge variant="success" className="text-xs">
                    Active
                  </Badge>
                </div>
              </CardContent>
            </Card>
            
            {/* Recent Activity */}
            <Card>
              <CardHeader>
                <CardTitle className="flex items-center gap-2">
                  <Bell className="h-5 w-5" />
                  Recent Activity
                </CardTitle>
              </CardHeader>
              <CardContent>
                {stats && stats.recent_shipments && stats.recent_shipments.length > 0 ? (
                  <div className="space-y-3">
                    {stats.recent_shipments.map((shipment) => (
                      <div key={shipment.tracking_number} className="flex items-start gap-3">
                        <div className={`w-2 h-2 rounded-full mt-2 flex-shrink-0 ${
                          shipment.status === 'DELIVERED' ? 'bg-green-500' :
                          shipment.status === 'IN_TRANSIT' ? 'bg-blue-500' :
                          shipment.status === 'PENDING' ? 'bg-yellow-500' :
                          'bg-gray-500'
                        }`}></div>
                        <div>
                          <p className="text-sm font-medium">
                            {shipment.status === 'DELIVERED' ? 'Package delivered' :
                             shipment.status === 'IN_TRANSIT' ? 'Package in transit' :
                             shipment.status === 'PENDING' ? 'Pickup scheduled' :
                             shipment.status_display}
                          </p>
                          <p className="text-xs text-muted-foreground">
                            {shipment.tracking_number} • {new Date(shipment.created_at).toLocaleString()}
                          </p>
                        </div>
                      </div>
                    ))}
                  </div>
                ) : (
                  <div className="text-center py-4">
                    <Activity className="h-8 w-8 mx-auto text-gray-400 mb-2" />
                    <p className="text-sm text-muted-foreground">No recent activity</p>
                    <p className="text-xs text-muted-foreground mt-1">
                      Create your first shipment to see activity here
                    </p>
                  </div>
                )}
              </CardContent>
            </Card>
          </div>
        </div>
      </div>
    </div>
  )
}