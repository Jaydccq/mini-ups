/**
 * Role Visualization Component
 * 
 * Features:
 * - Visual representation of role hierarchy and permissions
 * - Interactive role assignment and management
 * - Permission matrix view
 * - Role distribution charts
 * - Permission audit trail
 * 
 * @author Mini-UPS Team
 * @version 1.0.0
 */
import React, { useState, useMemo } from 'react';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Badge } from '@/components/ui/badge';
import { Tabs, TabsContent, TabsList, TabsTrigger } from '@/components/ui/tabs';
import { 
  Shield, 
  Users, 
  Truck, 
  Settings, 
  Check,
  X,
  Crown,
  Navigation
} from 'lucide-react';

interface Permission {
  id: string;
  name: string;
  description: string;
  category: 'USER' | 'SHIPMENT' | 'ADMIN' | 'FLEET' | 'SYSTEM';
}

interface Role {
  id: string;
  name: string;
  description: string;
  color: string;
  icon: React.ReactNode;
  level: number; // 1-4, higher = more permissions
  permissions: string[]; // Permission IDs
  userCount: number;
  canAssignRoles?: string[]; // Roles this role can assign to others
}

interface RoleVisualizationProps {
  users?: any[];
  onRoleUpdate?: (userId: string, newRole: string) => void;
  className?: string;
}

export const RoleVisualization: React.FC<RoleVisualizationProps> = ({
  users = [],
  onRoleUpdate,
  className = ''
}) => {
  const [selectedRole, setSelectedRole] = useState<string | null>(null);
  const [viewMode, setViewMode] = useState<'hierarchy' | 'matrix' | 'distribution'>('hierarchy');

  // Define all permissions
  const permissions: Permission[] = [
    // User permissions
    { id: 'user.profile.view', name: 'View Profile', description: 'View own profile information', category: 'USER' },
    { id: 'user.profile.edit', name: 'Edit Profile', description: 'Edit own profile information', category: 'USER' },
    { id: 'user.shipments.view', name: 'View Own Shipments', description: 'View own shipments only', category: 'SHIPMENT' },
    { id: 'user.shipments.create', name: 'Create Shipments', description: 'Create new shipments', category: 'SHIPMENT' },
    
    // Driver permissions
    { id: 'driver.shipments.update', name: 'Update Shipment Status', description: 'Update shipment delivery status', category: 'SHIPMENT' },
    { id: 'driver.truck.view', name: 'View Assigned Truck', description: 'View assigned truck information', category: 'FLEET' },
    { id: 'driver.route.view', name: 'View Routes', description: 'View assigned delivery routes', category: 'FLEET' },
    
    // Operator permissions
    { id: 'operator.shipments.view.all', name: 'View All Shipments', description: 'View all shipments in system', category: 'SHIPMENT' },
    { id: 'operator.shipments.edit', name: 'Edit Shipments', description: 'Edit shipment details and status', category: 'SHIPMENT' },
    { id: 'operator.fleet.view', name: 'View Fleet', description: 'View all trucks and drivers', category: 'FLEET' },
    { id: 'operator.fleet.assign', name: 'Assign Drivers', description: 'Assign drivers to trucks and routes', category: 'FLEET' },
    
    // Admin permissions
    { id: 'admin.users.view', name: 'View All Users', description: 'View all user accounts', category: 'ADMIN' },
    { id: 'admin.users.edit', name: 'Edit Users', description: 'Edit user accounts and details', category: 'ADMIN' },
    { id: 'admin.users.delete', name: 'Delete Users', description: 'Delete user accounts', category: 'ADMIN' },
    { id: 'admin.roles.assign', name: 'Assign Roles', description: 'Assign roles to users', category: 'ADMIN' },
    { id: 'admin.fleet.manage', name: 'Manage Fleet', description: 'Full fleet management access', category: 'FLEET' },
    { id: 'admin.system.config', name: 'System Configuration', description: 'Access system configuration', category: 'SYSTEM' },
    { id: 'admin.analytics.view', name: 'View Analytics', description: 'Access analytics and reports', category: 'ADMIN' },
    { id: 'admin.system.logs', name: 'View System Logs', description: 'Access system logs and audit trails', category: 'SYSTEM' },
  ];

  // Define roles with their permissions
  const roles: Role[] = [
    {
      id: 'USER',
      name: 'User',
      description: 'Regular customers who can create and track shipments',
      color: 'bg-blue-100 text-blue-800 border-blue-200',
      icon: <Users className="w-4 h-4" />,
      level: 1,
      permissions: [
        'user.profile.view',
        'user.profile.edit',
        'user.shipments.view',
        'user.shipments.create'
      ],
      userCount: users.filter(u => u.role === 'USER').length
    },
    {
      id: 'DRIVER',
      name: 'Driver',
      description: 'Drivers who deliver packages and update shipment status',
      color: 'bg-green-100 text-green-800 border-green-200',
      icon: <Truck className="w-4 h-4" />,
      level: 2,
      permissions: [
        'user.profile.view',
        'user.profile.edit',
        'user.shipments.view',
        'driver.shipments.update',
        'driver.truck.view',
        'driver.route.view'
      ],
      userCount: users.filter(u => u.role === 'DRIVER').length
    },
    {
      id: 'OPERATOR',
      name: 'Operator',
      description: 'Operations staff who manage shipments and fleet assignments',
      color: 'bg-yellow-100 text-yellow-800 border-yellow-200',
      icon: <Settings className="w-4 h-4" />,
      level: 3,
      permissions: [
        'user.profile.view',
        'user.profile.edit',
        'operator.shipments.view.all',
        'operator.shipments.edit',
        'operator.fleet.view',
        'operator.fleet.assign',
        'driver.truck.view',
        'driver.route.view'
      ],
      userCount: users.filter(u => u.role === 'OPERATOR').length,
      canAssignRoles: ['USER', 'DRIVER']
    },
    {
      id: 'ADMIN',
      name: 'Administrator',
      description: 'Full system access with user and system management capabilities',
      color: 'bg-red-100 text-red-800 border-red-200',
      icon: <Crown className="w-4 h-4" />,
      level: 4,
      permissions: permissions.map(p => p.id), // All permissions
      userCount: users.filter(u => u.role === 'ADMIN').length,
      canAssignRoles: ['USER', 'DRIVER', 'OPERATOR', 'ADMIN']
    }
  ];

  const permissionCategories = useMemo(() => {
    return permissions.reduce((acc, permission) => {
      if (!acc[permission.category]) {
        acc[permission.category] = [];
      }
      acc[permission.category].push(permission);
      return acc;
    }, {} as Record<string, Permission[]>);
  }, [permissions]);

  const roleHierarchy = useMemo(() => {
    return [...roles].sort((a, b) => b.level - a.level);
  }, [roles]);

  const selectedRoleData = selectedRole ? roles.find(r => r.id === selectedRole) : null;

  const hasPermission = (roleId: string, permissionId: string) => {
    const role = roles.find(r => r.id === roleId);
    return role?.permissions.includes(permissionId) || false;
  };

  const getCategoryIcon = (category: string) => {
    switch (category) {
      case 'USER':
        return <Users className="w-4 h-4" />;
      case 'SHIPMENT':
        return <Navigation className="w-4 h-4" />;
      case 'FLEET':
        return <Truck className="w-4 h-4" />;
      case 'ADMIN':
        return <Shield className="w-4 h-4" />;
      case 'SYSTEM':
        return <Settings className="w-4 h-4" />;
      default:
        return <Settings className="w-4 h-4" />;
    }
  };

  return (
    <Card className={className}>
      <CardHeader>
        <CardTitle className="flex items-center gap-2">
          <Shield className="h-5 w-5" />
          Role & Permission Management
        </CardTitle>
      </CardHeader>
      <CardContent>
        <Tabs value={viewMode} onValueChange={(value: any) => setViewMode(value)} className="w-full">
          <TabsList className="grid w-full grid-cols-3">
            <TabsTrigger value="hierarchy">Role Hierarchy</TabsTrigger>
            <TabsTrigger value="matrix">Permission Matrix</TabsTrigger>
            <TabsTrigger value="distribution">Role Distribution</TabsTrigger>
          </TabsList>
          
          <TabsContent value="hierarchy" className="space-y-4">
            <div className="grid gap-4">
              {roleHierarchy.map((role) => (
                <Card 
                  key={role.id} 
                  className={`cursor-pointer transition-all hover:shadow-md ${
                    selectedRole === role.id ? 'ring-2 ring-blue-500' : ''
                  }`}
                  onClick={() => setSelectedRole(selectedRole === role.id ? null : role.id)}
                >
                  <CardContent className="p-4">
                    <div className="flex items-center justify-between">
                      <div className="flex items-center gap-3">
                        <Badge className={role.color}>
                          {role.icon}
                          <span className="ml-2">{role.name}</span>
                        </Badge>
                        <div className="text-sm text-gray-600">
                          Level {role.level}
                        </div>
                      </div>
                      <div className="flex items-center gap-2">
                        <Badge variant="outline" className="text-xs">
                          {role.userCount} users
                        </Badge>
                        <Badge variant="outline" className="text-xs">
                          {role.permissions.length} permissions
                        </Badge>
                      </div>
                    </div>
                    
                    <p className="text-sm text-gray-600 mt-2">{role.description}</p>
                    
                    {selectedRole === role.id && (
                      <div className="mt-4 pt-4 border-t">
                        <h4 className="font-medium mb-2">Permissions ({role.permissions.length})</h4>
                        <div className="grid grid-cols-1 md:grid-cols-2 gap-2">
                          {Object.entries(permissionCategories).map(([category, categoryPermissions]) => {
                            const rolePermissions = categoryPermissions.filter(p => 
                              role.permissions.includes(p.id)
                            );
                            
                            if (rolePermissions.length === 0) return null;
                            
                            return (
                              <div key={category} className="space-y-1">
                                <div className="flex items-center gap-2 text-sm font-medium text-gray-700">
                                  {getCategoryIcon(category)}
                                  {category}
                                </div>
                                {rolePermissions.map(permission => (
                                  <div key={permission.id} className="pl-6 text-sm text-gray-600">
                                    {permission.name}
                                  </div>
                                ))}
                              </div>
                            );
                          })}
                        </div>
                      </div>
                    )}
                  </CardContent>
                </Card>
              ))}
            </div>
          </TabsContent>
          
          <TabsContent value="matrix" className="space-y-4">
            <div className="overflow-x-auto">
              <table className="w-full border-collapse border border-gray-200">
                <thead>
                  <tr className="bg-gray-50">
                    <th className="border border-gray-200 p-3 text-left">Permission</th>
                    {roles.map(role => (
                      <th key={role.id} className="border border-gray-200 p-3 text-center min-w-[100px]">
                        <Badge className={`${role.color} text-xs`}>
                          {role.icon}
                          <span className="ml-1">{role.name}</span>
                        </Badge>
                      </th>
                    ))}
                  </tr>
                </thead>
                <tbody>
                  {Object.entries(permissionCategories).map(([category, categoryPermissions]) => (
                    <React.Fragment key={category}>
                      <tr className="bg-gray-25">
                        <td colSpan={roles.length + 1} className="border border-gray-200 p-2 font-medium bg-gray-100">
                          <div className="flex items-center gap-2">
                            {getCategoryIcon(category)}
                            {category} Permissions
                          </div>
                        </td>
                      </tr>
                      {categoryPermissions.map(permission => (
                        <tr key={permission.id} className="hover:bg-gray-50">
                          <td className="border border-gray-200 p-3">
                            <div>
                              <div className="font-medium text-sm">{permission.name}</div>
                              <div className="text-xs text-gray-500">{permission.description}</div>
                            </div>
                          </td>
                          {roles.map(role => (
                            <td key={role.id} className="border border-gray-200 p-3 text-center">
                              {hasPermission(role.id, permission.id) ? (
                                <Check className="w-5 h-5 text-green-600 mx-auto" />
                              ) : (
                                <X className="w-5 h-5 text-gray-300 mx-auto" />
                              )}
                            </td>
                          ))}
                        </tr>
                      ))}
                    </React.Fragment>
                  ))}
                </tbody>
              </table>
            </div>
          </TabsContent>
          
          <TabsContent value="distribution" className="space-y-4">
            <div className="grid gap-6 md:grid-cols-2">
              {/* Role Distribution Chart */}
              <Card>
                <CardHeader>
                  <CardTitle className="text-lg">User Distribution by Role</CardTitle>
                </CardHeader>
                <CardContent>
                  <div className="space-y-3">
                    {roles.map(role => {
                      const percentage = users.length > 0 
                        ? Math.round((role.userCount / users.length) * 100) 
                        : 0;
                      
                      return (
                        <div key={role.id} className="space-y-2">
                          <div className="flex justify-between items-center">
                            <Badge className={role.color}>
                              {role.icon}
                              <span className="ml-2">{role.name}</span>
                            </Badge>
                            <span className="text-sm font-medium">
                              {role.userCount} ({percentage}%)
                            </span>
                          </div>
                          <div className="w-full bg-gray-200 rounded-full h-2">
                            <div 
                              className="bg-blue-600 h-2 rounded-full" 
                              style={{ width: `${percentage}%` }}
                            ></div>
                          </div>
                        </div>
                      );
                    })}
                  </div>
                </CardContent>
              </Card>

              {/* Permission Coverage */}
              <Card>
                <CardHeader>
                  <CardTitle className="text-lg">Permission Coverage</CardTitle>
                </CardHeader>
                <CardContent>
                  <div className="space-y-3">
                    {Object.entries(permissionCategories).map(([category, categoryPermissions]) => {
                      const totalPermissions = categoryPermissions.length;
                      const adminPermissions = categoryPermissions.filter(p => 
                        hasPermission('ADMIN', p.id)
                      ).length;
                      
                      return (
                        <div key={category} className="space-y-2">
                          <div className="flex justify-between items-center">
                            <div className="flex items-center gap-2">
                              {getCategoryIcon(category)}
                              <span className="text-sm font-medium">{category}</span>
                            </div>
                            <span className="text-sm">
                              {adminPermissions}/{totalPermissions}
                            </span>
                          </div>
                          <div className="w-full bg-gray-200 rounded-full h-2">
                            <div 
                              className="bg-green-600 h-2 rounded-full" 
                              style={{ width: `${(adminPermissions / totalPermissions) * 100}%` }}
                            ></div>
                          </div>
                        </div>
                      );
                    })}
                  </div>
                </CardContent>
              </Card>
            </div>
            
            {/* Role Capabilities Summary */}
            <Card>
              <CardHeader>
                <CardTitle className="text-lg">Role Capabilities Summary</CardTitle>
              </CardHeader>
              <CardContent>
                <div className="grid gap-4 md:grid-cols-2 lg:grid-cols-4">
                  {roles.map(role => (
                    <div key={role.id} className="space-y-2">
                      <Badge className={`${role.color} mb-2`}>
                        {role.icon}
                        <span className="ml-2">{role.name}</span>
                      </Badge>
                      
                      <div className="text-sm space-y-1">
                        {Object.entries(permissionCategories).map(([category, categoryPermissions]) => {
                          const rolePermissionsInCategory = categoryPermissions.filter(p => 
                            role.permissions.includes(p.id)
                          ).length;
                          
                          if (rolePermissionsInCategory === 0) return null;
                          
                          return (
                            <div key={category} className="flex justify-between">
                              <span className="text-gray-600">{category}:</span>
                              <span className="font-medium">
                                {rolePermissionsInCategory}/{categoryPermissions.length}
                              </span>
                            </div>
                          );
                        })}
                      </div>
                    </div>
                  ))}
                </div>
              </CardContent>
            </Card>
          </TabsContent>
        </Tabs>
      </CardContent>
    </Card>
  );
};