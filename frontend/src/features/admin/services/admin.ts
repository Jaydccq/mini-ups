import api from '../../../services/api';

// User Management Types
export interface AdminUser {
  id: string;
  name: string;
  email: string;
  role: 'USER' | 'ADMIN' | 'DRIVER' | 'OPERATOR';
  status: 'ACTIVE' | 'INACTIVE' | 'SUSPENDED';
  created_at: string;
  last_login?: string;
  shipment_count: number;
}

export interface UserFilters {
  search?: string;
  role?: string;
  status?: string;
  page?: number;
  limit?: number;
  sortBy?: string;
  sortOrder?: 'asc' | 'desc';
}

export interface UsersResponse {
  users: AdminUser[];
  total_count: number;
  page: number;
  limit: number;
  total_pages: number;
}

export interface UpdateUserRequest {
  name?: string;
  email?: string;
  role?: string;
  status?: string;
}

// Shipment Management Types
export interface AdminShipment {
  shipment_id: string;
  tracking_number: string;
  status: string;
  status_display: string;
  sender_name: string;
  recipient_name: string;
  origin: { x: number; y: number };
  destination: { x: number; y: number };
  created_at: string;
  estimated_delivery?: string;
  actual_delivery?: string;
  truck_id?: number;
  user_id: string;
}

export interface ShipmentFilters {
  search?: string;
  status?: string;
  date_from?: string;
  date_to?: string;
  user_id?: string;
  truck_id?: string;
  page?: number;
  limit?: number;
  sortBy?: string;
  sortOrder?: 'asc' | 'desc';
}

export interface ShipmentsResponse {
  shipments: AdminShipment[];
  total_count: number;
  page: number;
  limit: number;
  total_pages: number;
}

export interface BatchUpdateRequest {
  shipment_ids: string[];
  new_status: string;
  comment?: string;
}

// Analytics Types
export interface BusinessMetrics {
  total_users: number;
  total_shipments: number;
  active_shipments: number;
  delivered_shipments: number;
  pending_shipments: number;
  total_revenue: number;
  avg_delivery_time: number;
  customer_satisfaction: number;
}

export interface ShipmentTrend {
  date: string;
  shipments: number;
  revenue: number;
}

export interface StatusDistribution {
  status: string;
  count: number;
  percentage: number;
}

export interface TopCustomers {
  user_id: string;
  name: string;
  email: string;
  shipment_count: number;
  total_spent: number;
}

export interface AnalyticsData {
  metrics: BusinessMetrics;
  shipment_trends: ShipmentTrend[];
  status_distribution: StatusDistribution[];
  top_customers: TopCustomers[];
  monthly_stats: {
    current_month: number;
    previous_month: number;
    growth_rate: number;
  };
}

export const adminApi = {
  // User Management
  getUsers: (filters: UserFilters): Promise<UsersResponse> => {
    const params = new URLSearchParams();
    Object.entries(filters).forEach(([key, value]) => {
      if (value !== undefined && value !== null) {
        params.append(key, value.toString());
      }
    });
    
    return api.get(`/admin/users?${params.toString()}`).then(res => res.data.data);
  },

  getUserById: (userId: string): Promise<AdminUser> => {
    return api.get(`/admin/users/${userId}`).then(res => res.data.data);
  },

  updateUser: (userId: string, data: UpdateUserRequest): Promise<AdminUser> => {
    return api.put(`/admin/users/${userId}`, data).then(res => res.data.data);
  },

  deleteUser: (userId: string): Promise<{ success: boolean }> => {
    return api.delete(`/admin/users/${userId}`).then(res => res.data);
  },

  // Shipment Management
  getAllShipments: (filters: ShipmentFilters): Promise<ShipmentsResponse> => {
    const params = new URLSearchParams();
    Object.entries(filters).forEach(([key, value]) => {
      if (value !== undefined && value !== null) {
        params.append(key, value.toString());
      }
    });
    
    return api.get(`/admin/shipments?${params.toString()}`).then(res => res.data.data);
  },

  updateShipmentStatus: (trackingNumber: string, status: string, comment?: string): Promise<{ success: boolean }> => {
    return api.put(`/admin/shipments/${trackingNumber}/status`, { status, comment }).then(res => res.data);
  },

  batchUpdateShipments: (data: BatchUpdateRequest): Promise<{ success: boolean; updated_count: number }> => {
    return api.put('/admin/shipments/batch-update', data).then(res => res.data);
  },

  deleteShipment: (trackingNumber: string): Promise<{ success: boolean }> => {
    return api.delete(`/admin/shipments/${trackingNumber}`).then(res => res.data);
  },

  // Analytics
  getAnalytics: (dateRange?: { from: string; to: string }): Promise<AnalyticsData> => {
    const params = new URLSearchParams();
    if (dateRange) {
      params.append('from', dateRange.from);
      params.append('to', dateRange.to);
    }
    
    return api.get(`/admin/analytics?${params.toString()}`).then(res => res.data.data);
  },

  exportData: (type: 'users' | 'shipments' | 'analytics', filters?: any): Promise<Blob> => {
    const params = new URLSearchParams();
    if (filters) {
      Object.entries(filters).forEach(([key, value]) => {
        if (value !== undefined && value !== null) {
          params.append(key, value.toString());
        }
      });
    }
    
    return api.get(`/admin/export/${type}?${params.toString()}`, {
      responseType: 'blob'
    }).then(res => res.data);
  },

  // System Health
  getSystemHealth: (): Promise<{
    status: string;
    uptime: number;
    memory_usage: number;
    cpu_usage: number;
    database_status: string;
    redis_status: string;
  }> => {
    return api.get('/admin/health').then(res => res.data.data);
  },

  // Fleet Management CRUD Operations
  createTruck: (truckData: {
    plateNumber: string;
    capacity: number;
    location: string;
  }): Promise<any> => {
    return api.post('/admin/fleet/trucks', truckData).then(res => res.data.data);
  },

  updateTruck: (truckId: number, truckData: {
    plateNumber?: string;
    capacity?: number;
    location?: string;
    status?: string;
  }): Promise<any> => {
    return api.put(`/admin/fleet/trucks/${truckId}`, truckData).then(res => res.data.data);
  },

  deleteTruck: (truckId: number): Promise<void> => {
    return api.delete(`/admin/fleet/trucks/${truckId}`).then(res => res.data);
  },

  createDriver: (driverData: {
    name: string;
    email: string;
    phone: string;
    licenseNumber: string;
  }): Promise<any> => {
    return api.post('/admin/fleet/drivers', driverData).then(res => res.data.data);
  },

  updateDriver: (driverId: number, driverData: {
    name?: string;
    email?: string;
    phone?: string;
    licenseNumber?: string;
    status?: string;
  }): Promise<any> => {
    return api.put(`/admin/fleet/drivers/${driverId}`, driverData).then(res => res.data.data);
  },

  deleteDriver: (driverId: number): Promise<void> => {
    return api.delete(`/admin/fleet/drivers/${driverId}`).then(res => res.data);
  },

  // Dashboard APIs
  getDashboardStatistics: (): Promise<any> => {
    return api.get('/admin/dashboard/statistics').then(res => res.data.data);
  },

  getFleetOverview: (): Promise<any> => {
    return api.get('/admin/fleet/overview').then(res => res.data.data);
  },

  getDriverManagement: (): Promise<any> => {
    return api.get('/admin/fleet/drivers').then(res => res.data.data);
  },

  getRecentActivities: (page: number = 0, size: number = 10): Promise<any> => {
    return api.get(`/admin/dashboard/activities?page=${page}&size=${size}`).then(res => res.data.data);
  },
};