import React, { useState } from 'react';
import { useQuery, useMutation } from '@tanstack/react-query';
import { Users, UserPlus, Search, Filter, Shield, Ban, CheckCircle, MoreHorizontal, Edit, Trash2, Activity, Eye, Calendar } from 'lucide-react';
import { adminApi, AdminUser, UserFilters } from '../services/admin';
import { DataTable, Column } from '@/components/ui/data-table';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
import { Badge } from '@/components/ui/badge';
import { Alert, AlertDescription } from '@/components/ui/alert';
import { Tabs, TabsContent, TabsList, TabsTrigger } from '@/components/ui/tabs';
import { Dialog, DialogContent, DialogHeader, DialogTitle, DialogTrigger } from '@/components/ui/dialog';
import { UserActivityLogs } from '../components/UserActivityLogs';
import { RoleVisualization } from '../components/RoleVisualization';
import { 
  DropdownMenu, 
  DropdownMenuContent, 
  DropdownMenuItem, 
  DropdownMenuSeparator, 
  DropdownMenuTrigger 
} from '@/components/ui/dropdown-menu';
import { toast } from 'sonner';
import { queryClient } from '@/lib/queryClient';
import { formatDateTime, formatRelativeTime } from '@/lib/utils';

export const UserManagementPage: React.FC = () => {
  const [filters, setFilters] = useState<UserFilters>({
    page: 1,
    limit: 20,
    sortBy: 'created_at',
    sortOrder: 'desc'
  });
  const [selectedUserId, setSelectedUserId] = useState<string | null>(null);
  const [isActivityDialogOpen, setIsActivityDialogOpen] = useState(false);

  // Fetch users
  const { data: usersData, isLoading, error } = useQuery({
    queryKey: ['admin', 'users', filters],
    queryFn: () => adminApi.getUsers(filters),
    refetchInterval: 30000, // Refresh every 30 seconds
  });

  // Update user mutation
  const updateUserMutation = useMutation({
    mutationFn: ({ userId, data }: { userId: string; data: any }) => 
      adminApi.updateUser(userId, data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['admin', 'users'] });
      toast.success('User updated successfully');
    },
    onError: (error: any) => {
      toast.error(`Failed to update user: ${error.message}`);
    },
  });

  // Delete user mutation
  const deleteUserMutation = useMutation({
    mutationFn: (userId: string) => adminApi.deleteUser(userId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['admin', 'users'] });
      toast.success('User deleted successfully');
    },
    onError: (error: any) => {
      toast.error(`Failed to delete user: ${error.message}`);
    },
  });

  const handleUpdateUserStatus = (userId: string, status: string) => {
    updateUserMutation.mutate({ userId, data: { status } });
  };

  const handleUpdateUserRole = (userId: string, role: string) => {
    updateUserMutation.mutate({ userId, data: { role } });
  };

  const handleDeleteUser = (userId: string) => {
    if (confirm('Are you sure you want to delete this user? This action cannot be undone.')) {
      deleteUserMutation.mutate(userId);
    }
  };

  const handleViewUserActivity = (userId: string) => {
    setSelectedUserId(userId);
    setIsActivityDialogOpen(true);
  };

  const handleBulkAction = (selectedUsers: AdminUser[], action: string) => {
    if (selectedUsers.length === 0) {
      toast.error('Please select at least one user');
      return;
    }

    const userIds = selectedUsers.map(user => user.id);
    
    switch (action) {
      case 'activate':
        userIds.forEach(userId => 
          updateUserMutation.mutate({ userId, data: { status: 'ACTIVE' } })
        );
        break;
      case 'deactivate':
        userIds.forEach(userId => 
          updateUserMutation.mutate({ userId, data: { status: 'INACTIVE' } })
        );
        break;
      case 'suspend':
        userIds.forEach(userId => 
          updateUserMutation.mutate({ userId, data: { status: 'SUSPENDED' } })
        );
        break;
      case 'delete':
        if (confirm(`Are you sure you want to delete ${selectedUsers.length} users? This action cannot be undone.`)) {
          userIds.forEach(userId => deleteUserMutation.mutate(userId));
        }
        break;
    }
  };

  const handleExport = () => {
    adminApi.exportData('users', filters)
      .then(blob => {
        const url = window.URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = `users-${new Date().toISOString().split('T')[0]}.csv`;
        document.body.appendChild(a);
        a.click();
        window.URL.revokeObjectURL(url);
        document.body.removeChild(a);
        toast.success('Users exported successfully');
      })
      .catch(error => {
        toast.error(`Export failed: ${error.message}`);
      });
  };

  const getRoleColor = (role: string) => {
    switch (role) {
      case 'ADMIN': return 'destructive';
      case 'DRIVER': return 'default';
      case 'OPERATOR': return 'secondary';
      default: return 'outline';
    }
  };

  const getStatusColor = (status: string) => {
    switch (status) {
      case 'ACTIVE': return 'default';
      case 'INACTIVE': return 'secondary';
      case 'SUSPENDED': return 'destructive';
      default: return 'outline';
    }
  };

  const columns: Column<AdminUser>[] = [
    {
      key: 'name',
      title: 'User',
      render: (value, user) => (
        <div className="flex items-center space-x-3">
          <div className="w-8 h-8 bg-blue-100 rounded-full flex items-center justify-center">
            <span className="text-sm font-medium text-blue-600">
              {user.name.charAt(0).toUpperCase()}
            </span>
          </div>
          <div>
            <div className="font-medium text-gray-900">{user.name}</div>
            <div className="text-sm text-gray-500">{user.email}</div>
          </div>
        </div>
      ),
    },
    {
      key: 'role',
      title: 'Role',
      render: (value) => (
        <Badge variant={getRoleColor(value) as any}>
          {value}
        </Badge>
      ),
    },
    {
      key: 'status',
      title: 'Status',
      render: (value) => (
        <Badge variant={getStatusColor(value) as any}>
          {value}
        </Badge>
      ),
    },
    {
      key: 'shipment_count',
      title: 'Shipments',
      align: 'center',
      render: (value) => (
        <span className="font-medium">{value || 0}</span>
      ),
    },
    {
      key: 'created_at',
      title: 'Joined',
      render: (value) => (
        <div>
          <div className="text-sm text-gray-900">{formatDateTime(value)}</div>
          <div className="text-xs text-gray-500">{formatRelativeTime(value)}</div>
        </div>
      ),
    },
    {
      key: 'last_login',
      title: 'Last Login',
      render: (value) => value ? (
        <div>
          <div className="text-sm text-gray-900">{formatDateTime(value)}</div>
          <div className="text-xs text-gray-500">{formatRelativeTime(value)}</div>
        </div>
      ) : (
        <span className="text-sm text-gray-500">Never</span>
      ),
    },
    {
      key: 'actions',
      title: 'Actions',
      align: 'center',
      render: (_, user) => (
        <DropdownMenu>
          <DropdownMenuTrigger asChild>
            <Button variant="ghost" size="sm">
              <MoreHorizontal className="h-4 w-4" />
            </Button>
          </DropdownMenuTrigger>
          <DropdownMenuContent align="end">
            <DropdownMenuItem onClick={() => handleUpdateUserRole(user.id, 'USER')}>
              <Users className="h-4 w-4 mr-2" />
              Set as User
            </DropdownMenuItem>
            <DropdownMenuItem onClick={() => handleUpdateUserRole(user.id, 'DRIVER')}>
              <Users className="h-4 w-4 mr-2" />
              Set as Driver
            </DropdownMenuItem>
            <DropdownMenuItem onClick={() => handleUpdateUserRole(user.id, 'OPERATOR')}>
              <Users className="h-4 w-4 mr-2" />
              Set as Operator
            </DropdownMenuItem>
            <DropdownMenuItem onClick={() => handleUpdateUserRole(user.id, 'ADMIN')}>
              <Shield className="h-4 w-4 mr-2" />
              Set as Admin
            </DropdownMenuItem>
            <DropdownMenuSeparator />
            {user.status === 'ACTIVE' ? (
              <DropdownMenuItem onClick={() => handleUpdateUserStatus(user.id, 'INACTIVE')}>
                <Ban className="h-4 w-4 mr-2" />
                Deactivate
              </DropdownMenuItem>
            ) : (
              <DropdownMenuItem onClick={() => handleUpdateUserStatus(user.id, 'ACTIVE')}>
                <CheckCircle className="h-4 w-4 mr-2" />
                Activate
              </DropdownMenuItem>
            )}
            <DropdownMenuItem 
              onClick={() => handleUpdateUserStatus(user.id, 'SUSPENDED')}
              className="text-yellow-600"
            >
              <Ban className="h-4 w-4 mr-2" />
              Suspend
            </DropdownMenuItem>
            <DropdownMenuSeparator />
            <DropdownMenuItem onClick={() => handleViewUserActivity(user.id)}>
              <Activity className="h-4 w-4 mr-2" />
              View Activity
            </DropdownMenuItem>
            <DropdownMenuItem 
              onClick={() => handleDeleteUser(user.id)}
              className="text-red-600"
            >
              <Trash2 className="h-4 w-4 mr-2" />
              Delete User
            </DropdownMenuItem>
          </DropdownMenuContent>
        </DropdownMenu>
      ),
    },
  ];

  return (
    <div className="p-6 max-w-7xl mx-auto">
      <div className="mb-8">
        <h1 className="text-3xl font-bold text-gray-900">User Management</h1>
        <p className="text-gray-600">Manage user accounts, roles, and permissions</p>
      </div>

      <Tabs defaultValue="users" className="w-full">
        <TabsList className="grid w-full grid-cols-4">
          <TabsTrigger value="users">Users</TabsTrigger>
          <TabsTrigger value="roles">Roles & Permissions</TabsTrigger>
          <TabsTrigger value="activities">Activity Logs</TabsTrigger>
          <TabsTrigger value="analytics">Analytics</TabsTrigger>
        </TabsList>
        
        <TabsContent value="users" className="space-y-6">
          {/* Stats Cards */}
          <div className="grid gap-6 md:grid-cols-4">
            <Card>
              <CardContent className="p-6">
                <div className="flex items-center justify-between">
                  <div>
                    <p className="text-sm font-medium text-gray-600">Total Users</p>
                    <p className="text-2xl font-bold text-gray-900">
                      {usersData?.total_count || 0}
                    </p>
                  </div>
                  <div className="w-12 h-12 bg-blue-100 rounded-lg flex items-center justify-center">
                    <Users className="h-6 w-6 text-blue-600" />
                  </div>
                </div>
              </CardContent>
            </Card>

            <Card>
              <CardContent className="p-6">
                <div className="flex items-center justify-between">
                  <div>
                    <p className="text-sm font-medium text-gray-600">Active Users</p>
                    <p className="text-2xl font-bold text-green-600">
                      {usersData?.users?.filter(u => u.status === 'ACTIVE').length || 0}
                    </p>
                  </div>
                  <div className="w-12 h-12 bg-green-100 rounded-lg flex items-center justify-center">
                    <CheckCircle className="h-6 w-6 text-green-600" />
                  </div>
                </div>
              </CardContent>
            </Card>

            <Card>
              <CardContent className="p-6">
                <div className="flex items-center justify-between">
                  <div>
                    <p className="text-sm font-medium text-gray-600">Admins</p>
                    <p className="text-2xl font-bold text-red-600">
                      {usersData?.users?.filter(u => u.role === 'ADMIN').length || 0}
                    </p>
                  </div>
                  <div className="w-12 h-12 bg-red-100 rounded-lg flex items-center justify-center">
                    <Shield className="h-6 w-6 text-red-600" />
                  </div>
                </div>
              </CardContent>
            </Card>

            <Card>
              <CardContent className="p-6">
                <div className="flex items-center justify-between">
                  <div>
                    <p className="text-sm font-medium text-gray-600">Suspended</p>
                    <p className="text-2xl font-bold text-yellow-600">
                      {usersData?.users?.filter(u => u.status === 'SUSPENDED').length || 0}
                    </p>
                  </div>
                  <div className="w-12 h-12 bg-yellow-100 rounded-lg flex items-center justify-center">
                    <Ban className="h-6 w-6 text-yellow-600" />
                  </div>
                </div>
              </CardContent>
            </Card>
          </div>

          {/* Users Table */}
          <DataTable
            data={usersData?.users || []}
            columns={columns}
            loading={isLoading}
            searchable={true}
            searchPlaceholder="Search users by name or email..."
            searchFields={['name', 'email']}
            selectable={true}
            sortable={true}
            exportable={true}
            onExport={handleExport}
            actions={[
              {
                label: 'Activate',
                icon: <CheckCircle className="h-4 w-4 mr-1" />,
                onClick: (users) => handleBulkAction(users, 'activate'),
                variant: 'secondary',
              },
              {
                label: 'Deactivate',
                icon: <Ban className="h-4 w-4 mr-1" />,
                onClick: (users) => handleBulkAction(users, 'deactivate'),
                variant: 'secondary',
              },
              {
                label: 'Suspend',
                icon: <Ban className="h-4 w-4 mr-1" />,
                onClick: (users) => handleBulkAction(users, 'suspend'),
                variant: 'secondary',
              },
              {
                label: 'Delete',
                icon: <Trash2 className="h-4 w-4 mr-1" />,
                onClick: (users) => handleBulkAction(users, 'delete'),
                variant: 'destructive',
              },
            ]}
            pagination={usersData ? {
              current: filters.page || 1,
              pageSize: filters.limit || 20,
              total: usersData.total_count,
              onChange: (page, pageSize) => {
                setFilters(prev => ({ ...prev, page, limit: pageSize }));
              },
            } : undefined}
            emptyMessage="No users found"
          />

          {error && (
            <Alert variant="destructive" className="mt-4">
              <AlertDescription>
                Failed to load users: {error.message}
              </AlertDescription>
            </Alert>
          )}
        </TabsContent>
        
        <TabsContent value="roles" className="space-y-6">
          <RoleVisualization 
            users={usersData?.users || []}
            onRoleUpdate={handleUpdateUserRole}
          />
        </TabsContent>
        
        <TabsContent value="activities" className="space-y-6">
          <UserActivityLogs />
        </TabsContent>
        
        <TabsContent value="analytics" className="space-y-6">
          <div className="grid gap-6 md:grid-cols-2">
            {/* User Registration Trends */}
            <Card>
              <CardHeader>
                <CardTitle>User Registration Trends</CardTitle>
              </CardHeader>
              <CardContent>
                <div className="text-center py-8 text-gray-500">
                  <Calendar className="w-12 h-12 mx-auto mb-4 opacity-50" />
                  <p>User registration analytics</p>
                  <p className="text-sm">Charts showing user growth over time</p>
                </div>
              </CardContent>
            </Card>
            
            {/* User Activity Metrics */}
            <Card>
              <CardHeader>
                <CardTitle>Activity Metrics</CardTitle>
              </CardHeader>
              <CardContent>
                <div className="text-center py-8 text-gray-500">
                  <Activity className="w-12 h-12 mx-auto mb-4 opacity-50" />
                  <p>User activity metrics</p>
                  <p className="text-sm">Login frequency, session duration, feature usage</p>
                </div>
              </CardContent>
            </Card>
          </div>
        </TabsContent>
      </Tabs>

      {/* User Activity Dialog */}
      <Dialog open={isActivityDialogOpen} onOpenChange={setIsActivityDialogOpen}>
        <DialogContent className="max-w-4xl">
          <DialogHeader>
            <DialogTitle>User Activity Log</DialogTitle>
          </DialogHeader>
          {selectedUserId && (
            <UserActivityLogs userId={selectedUserId} />
          )}
        </DialogContent>
      </Dialog>
    </div>
  );
};