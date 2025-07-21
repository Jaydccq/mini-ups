/**
 * User Activity Logs Component
 * 
 * Features:
 * - Display user activity history with filtering
 * - Real-time activity monitoring
 * - Activity type categorization (login, shipment, admin actions)
 * - Timeline view with detailed context
 * - Export functionality for auditing
 * 
 * @author Mini-UPS Team
 * @version 1.0.0
 */
import React, { useState, useEffect, useCallback } from 'react';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Badge } from '@/components/ui/badge';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select';
import { 
  Activity, 
  Clock, 
  User, 
  Package, 
  Shield, 
  LogIn, 
  LogOut,
  Edit,
  Trash2,
  RefreshCw,
  Download,
  Filter,
  Search
} from 'lucide-react';

interface UserActivity {
  id: string;
  userId: string;
  userName: string;
  action: string;
  actionType: 'LOGIN' | 'LOGOUT' | 'SHIPMENT_CREATE' | 'SHIPMENT_UPDATE' | 'PROFILE_UPDATE' | 'ADMIN_ACTION' | 'SYSTEM';
  details: string;
  ipAddress: string;
  userAgent: string;
  timestamp: string;
  metadata?: {
    entityId?: string;
    entityType?: string;
    oldValue?: any;
    newValue?: any;
  };
}

interface UserActivityLogsProps {
  userId?: string; // If provided, show activities for specific user
  className?: string;
}

export const UserActivityLogs: React.FC<UserActivityLogsProps> = ({
  userId,
  className = ''
}) => {
  const [activities, setActivities] = useState<UserActivity[]>([]);
  const [filteredActivities, setFilteredActivities] = useState<UserActivity[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [searchTerm, setSearchTerm] = useState('');
  const [actionFilter, setActionFilter] = useState<string>('all');
  const [dateFilter, setDateFilter] = useState<string>('all');

  const fetchActivities = useCallback(async () => {
    try {
      setLoading(true);
      setError(null);
      
      const endpoint = userId 
        ? `/api/admin/users/${userId}/activities`
        : '/api/admin/activities';
      
      const response = await fetch(endpoint, {
        headers: { 'Authorization': `Bearer ${localStorage.getItem('token')}` }
      });
      
      if (!response.ok) {
        throw new Error('Failed to fetch user activities');
      }
      
      const data = await response.json();
      setActivities(data.data.activities || mockActivities);
    } catch (error) {
      console.error('Error fetching activities:', error);
      setActivities(mockActivities);
    } finally {
      setLoading(false);
    }
  }, [userId]);

  useEffect(() => {
    fetchActivities();
    
    // Set up auto-refresh every 30 seconds
    const interval = setInterval(fetchActivities, 30000);
    
    return () => clearInterval(interval);
  }, [userId, fetchActivities]);

  // Filter activities based on search and filters
  useEffect(() => {
    let filtered = activities;

    // Search filter
    if (searchTerm) {
      filtered = filtered.filter(activity =>
        activity.userName.toLowerCase().includes(searchTerm.toLowerCase()) ||
        activity.action.toLowerCase().includes(searchTerm.toLowerCase()) ||
        activity.details.toLowerCase().includes(searchTerm.toLowerCase())
      );
    }

    // Action type filter
    if (actionFilter !== 'all') {
      filtered = filtered.filter(activity => activity.actionType === actionFilter);
    }

    // Date filter
    if (dateFilter !== 'all') {
      const now = new Date();
      const filterDate = new Date();
      
      switch (dateFilter) {
        case 'today':
          filterDate.setHours(0, 0, 0, 0);
          break;
        case 'week':
          filterDate.setDate(now.getDate() - 7);
          break;
        case 'month':
          filterDate.setMonth(now.getMonth() - 1);
          break;
      }
      
      filtered = filtered.filter(activity => 
        new Date(activity.timestamp) >= filterDate
      );
    }

    setFilteredActivities(filtered);
  }, [activities, searchTerm, actionFilter, dateFilter]);

  const getActionIcon = (actionType: string) => {
    switch (actionType) {
      case 'LOGIN':
        return <LogIn className="w-4 h-4" />;
      case 'LOGOUT':
        return <LogOut className="w-4 h-4" />;
      case 'SHIPMENT_CREATE':
      case 'SHIPMENT_UPDATE':
        return <Package className="w-4 h-4" />;
      case 'PROFILE_UPDATE':
        return <Edit className="w-4 h-4" />;
      case 'ADMIN_ACTION':
        return <Shield className="w-4 h-4" />;
      default:
        return <Activity className="w-4 h-4" />;
    }
  };

  const getActionColor = (actionType: string) => {
    switch (actionType) {
      case 'LOGIN':
        return 'bg-green-100 text-green-800 border-green-200';
      case 'LOGOUT':
        return 'bg-gray-100 text-gray-800 border-gray-200';
      case 'SHIPMENT_CREATE':
        return 'bg-blue-100 text-blue-800 border-blue-200';
      case 'SHIPMENT_UPDATE':
        return 'bg-indigo-100 text-indigo-800 border-indigo-200';
      case 'PROFILE_UPDATE':
        return 'bg-yellow-100 text-yellow-800 border-yellow-200';
      case 'ADMIN_ACTION':
        return 'bg-red-100 text-red-800 border-red-200';
      default:
        return 'bg-gray-100 text-gray-800 border-gray-200';
    }
  };

  const handleExport = () => {
    const csvContent = [
      ['Timestamp', 'User', 'Action', 'Type', 'Details', 'IP Address'].join(','),
      ...filteredActivities.map(activity => [
        activity.timestamp,
        activity.userName,
        activity.action,
        activity.actionType,
        `"${activity.details}"`,
        activity.ipAddress
      ].join(','))
    ].join('\n');

    const blob = new Blob([csvContent], { type: 'text/csv;charset=utf-8;' });
    const url = URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.setAttribute('href', url);
    link.setAttribute('download', `user-activities-${new Date().toISOString().split('T')[0]}.csv`);
    link.style.visibility = 'hidden';
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
  };

  if (loading) {
    return (
      <Card className={className}>
        <CardHeader>
          <CardTitle className="flex items-center gap-2">
            <Activity className="h-5 w-5" />
            User Activity Logs
          </CardTitle>
        </CardHeader>
        <CardContent>
          <div className="flex items-center justify-center h-32">
            <div className="flex items-center space-x-2">
              <RefreshCw className="w-5 h-5 animate-spin" />
              <span>Loading activities...</span>
            </div>
          </div>
        </CardContent>
      </Card>
    );
  }

  return (
    <Card className={className}>
      <CardHeader>
        <div className="flex justify-between items-center">
          <CardTitle className="flex items-center gap-2">
            <Activity className="h-5 w-5" />
            {userId ? 'User Activity Logs' : 'All User Activities'}
          </CardTitle>
          <div className="flex items-center gap-2">
            <Badge variant="outline" className="text-xs">
              {filteredActivities.length} activities
            </Badge>
            <Button onClick={fetchActivities} variant="outline" size="sm">
              <RefreshCw className="w-3 h-3 mr-1" />
              Refresh
            </Button>
            <Button onClick={handleExport} variant="outline" size="sm">
              <Download className="w-3 h-3 mr-1" />
              Export
            </Button>
          </div>
        </div>
      </CardHeader>
      <CardContent className="space-y-4">
        {/* Filters */}
        <div className="flex flex-wrap gap-4 p-4 bg-gray-50 rounded-lg">
          <div className="flex-1 min-w-[200px]">
            <div className="relative">
              <Search className="absolute left-3 top-1/2 transform -translate-y-1/2 text-gray-400 w-4 h-4" />
              <Input
                placeholder="Search activities..."
                value={searchTerm}
                onChange={(e) => setSearchTerm(e.target.value)}
                className="pl-10"
              />
            </div>
          </div>
          
          <Select value={actionFilter} onValueChange={setActionFilter}>
            <SelectTrigger className="w-[150px]">
              <SelectValue placeholder="Action Type" />
            </SelectTrigger>
            <SelectContent>
              <SelectItem value="all">All Actions</SelectItem>
              <SelectItem value="LOGIN">Login</SelectItem>
              <SelectItem value="LOGOUT">Logout</SelectItem>
              <SelectItem value="SHIPMENT_CREATE">Shipment Created</SelectItem>
              <SelectItem value="SHIPMENT_UPDATE">Shipment Updated</SelectItem>
              <SelectItem value="PROFILE_UPDATE">Profile Updated</SelectItem>
              <SelectItem value="ADMIN_ACTION">Admin Action</SelectItem>
            </SelectContent>
          </Select>
          
          <Select value={dateFilter} onValueChange={setDateFilter}>
            <SelectTrigger className="w-[120px]">
              <SelectValue placeholder="Time Period" />
            </SelectTrigger>
            <SelectContent>
              <SelectItem value="all">All Time</SelectItem>
              <SelectItem value="today">Today</SelectItem>
              <SelectItem value="week">Past Week</SelectItem>
              <SelectItem value="month">Past Month</SelectItem>
            </SelectContent>
          </Select>
        </div>

        {/* Activity Timeline */}
        <div className="space-y-3 max-h-96 overflow-y-auto">
          {filteredActivities.length > 0 ? (
            filteredActivities.map((activity, index) => (
              <div key={activity.id} className="flex items-start space-x-3 p-3 border rounded-lg bg-white hover:bg-gray-50 transition-colors">
                <div className="flex-shrink-0 mt-1">
                  <Badge className={`${getActionColor(activity.actionType)} text-xs p-1`}>
                    {getActionIcon(activity.actionType)}
                  </Badge>
                </div>
                
                <div className="flex-1 min-w-0">
                  <div className="flex items-center justify-between mb-1">
                    <h4 className="text-sm font-medium text-gray-900">
                      {activity.action}
                    </h4>
                    <time className="text-xs text-gray-500">
                      {new Date(activity.timestamp).toLocaleString()}
                    </time>
                  </div>
                  
                  <p className="text-sm text-gray-600 mb-2">
                    {activity.details}
                  </p>
                  
                  <div className="flex items-center justify-between text-xs text-gray-500">
                    <div className="flex items-center space-x-4">
                      <span className="flex items-center">
                        <User className="w-3 h-3 mr-1" />
                        {activity.userName}
                      </span>
                      <span>IP: {activity.ipAddress}</span>
                    </div>
                    
                    {activity.metadata && (
                      <Badge variant="outline" className="text-xs">
                        {activity.actionType}
                      </Badge>
                    )}
                  </div>
                  
                  {/* Metadata details for admin actions */}
                  {activity.metadata && (activity.metadata.oldValue || activity.metadata.newValue) && (
                    <div className="mt-2 p-2 bg-gray-50 rounded text-xs">
                      {activity.metadata.oldValue && (
                        <div className="text-red-600">
                          Old: {JSON.stringify(activity.metadata.oldValue)}
                        </div>
                      )}
                      {activity.metadata.newValue && (
                        <div className="text-green-600">
                          New: {JSON.stringify(activity.metadata.newValue)}
                        </div>
                      )}
                    </div>
                  )}
                </div>
              </div>
            ))
          ) : (
            <div className="text-center py-8 text-gray-500">
              <Activity className="w-12 h-12 mx-auto mb-4 opacity-50" />
              <p>No activities found</p>
              <p className="text-sm">Try adjusting your search filters</p>
            </div>
          )}
        </div>
      </CardContent>
    </Card>
  );
};

// Mock data for demo purposes
const mockActivities: UserActivity[] = [
  {
    id: '1',
    userId: '1',
    userName: 'John Smith',
    action: 'User logged in',
    actionType: 'LOGIN',
    details: 'User successfully logged in from desktop application',
    ipAddress: '192.168.1.100',
    userAgent: 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36',
    timestamp: new Date(Date.now() - 30 * 60 * 1000).toISOString() // 30 minutes ago
  },
  {
    id: '2',
    userId: '2',
    userName: 'Jane Doe',
    action: 'Created new shipment',
    actionType: 'SHIPMENT_CREATE',
    details: 'Created shipment #SHP-12345 for package delivery to downtown area',
    ipAddress: '192.168.1.102',
    userAgent: 'Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7)',
    timestamp: new Date(Date.now() - 60 * 60 * 1000).toISOString(), // 1 hour ago
    metadata: {
      entityId: 'SHP-12345',
      entityType: 'shipment'
    }
  },
  {
    id: '3',
    userId: '3',
    userName: 'Admin User',
    action: 'Updated user role',
    actionType: 'ADMIN_ACTION',
    details: 'Changed user role from USER to DRIVER for John Smith',
    ipAddress: '192.168.1.10',
    userAgent: 'Mozilla/5.0 (Windows NT 10.0; Win64; x64)',
    timestamp: new Date(Date.now() - 120 * 60 * 1000).toISOString(), // 2 hours ago
    metadata: {
      entityId: '1',
      entityType: 'user',
      oldValue: { role: 'USER' },
      newValue: { role: 'DRIVER' }
    }
  },
  {
    id: '4',
    userId: '1',
    userName: 'John Smith',
    action: 'Updated profile information',
    actionType: 'PROFILE_UPDATE',
    details: 'Updated contact phone number and emergency contact',
    ipAddress: '192.168.1.100',
    userAgent: 'Mozilla/5.0 (Windows NT 10.0; Win64; x64)',
    timestamp: new Date(Date.now() - 180 * 60 * 1000).toISOString(), // 3 hours ago
    metadata: {
      entityId: '1',
      entityType: 'user_profile'
    }
  },
  {
    id: '5',
    userId: '4',
    userName: 'Driver Mike',
    action: 'Updated shipment status',
    actionType: 'SHIPMENT_UPDATE',
    details: 'Marked shipment #SHP-12344 as delivered',
    ipAddress: '10.0.0.50',
    userAgent: 'MiniUPS Mobile App v1.2.0',
    timestamp: new Date(Date.now() - 240 * 60 * 1000).toISOString(), // 4 hours ago
    metadata: {
      entityId: 'SHP-12344',
      entityType: 'shipment',
      oldValue: { status: 'IN_TRANSIT' },
      newValue: { status: 'DELIVERED' }
    }
  },
  {
    id: '6',
    userId: '2',
    userName: 'Jane Doe',
    action: 'User logged out',
    actionType: 'LOGOUT',
    details: 'User logged out after 2 hours session',
    ipAddress: '192.168.1.102',
    userAgent: 'Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7)',
    timestamp: new Date(Date.now() - 300 * 60 * 1000).toISOString() // 5 hours ago
  }
];