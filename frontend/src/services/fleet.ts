/**
 * Fleet Management API Service
 * 
 * Description:
 * - Centralized API calls for fleet management operations
 * - Handles trucks, drivers, and assignment operations
 * - Provides typed responses for better development experience
 * 
 * API Endpoints:
 * - Truck CRUD operations
 * - Driver CRUD operations  
 * - Driver-truck assignments
 * - Fleet statistics and monitoring
 * 
 */

import { API_BASE_URL } from './config';

// Types
export interface TruckData {
  id: string;
  truckId: number;
  plateNumber: string;
  capacity: number;
  status: 'IDLE' | 'EN_ROUTE' | 'AT_WAREHOUSE' | 'LOADING' | 'DELIVERING' | 'MAINTENANCE' | 'OUT_OF_SERVICE';
  currentX: number;
  currentY: number;
  currentLocation: string;
  lastUpdated: string;
  driverName?: string;
  driver?: {
    id: string;
    name: string;
    phone: string;
  };
}

export interface DriverData {
  id: string;
  name: string;
  email: string;
  phone: string;
  licenseNumber: string;
  status: 'UNASSIGNED' | 'ASSIGNED' | 'ON_DUTY' | 'OFF_DUTY' | 'ON_LEAVE' | 'INACTIVE';
  statusDescription: string;
  hireDate: string;
  lastActive: string;
  totalDeliveries: number;
  rating: number;
  availableForAssignment: boolean;
  canStartWork: boolean;
  daysSinceHire: number;
  assignedTruck?: {
    truckId: number;
    plateNumber: string;
    status: string;
    currentLocation: string;
  };
}

export interface FleetStatistics {
  totalTrucks: number;
  statusDistribution: Record<string, number>;
  totalDrivers: number;
  unassigned: number;
  assigned: number;
  onDuty: number;
  averageRating: number;
}

export interface CreateDriverRequest {
  name: string;
  email: string;
  phone: string;
  licenseNumber: string;
}

export interface UpdateDriverRequest {
  name?: string;
  email?: string;
  phone?: string;
  licenseNumber?: string;
}

export interface CreateTruckRequest {
  plateNumber: string;
  capacity: number;
  location: string;
}

export interface UpdateTruckRequest {
  plateNumber?: string;
  capacity?: number;
  location?: string;
}

// API Service Class
class FleetApiService {
  private async makeRequest<T>(
    endpoint: string,
    options: RequestInit = {}
  ): Promise<T> {
    const token = localStorage.getItem('token');
    
    const response = await fetch(`${API_BASE_URL}${endpoint}`, {
      headers: {
        'Content-Type': 'application/json',
        ...(token && { 'Authorization': `Bearer ${token}` }),
        ...options.headers,
      },
      ...options,
    });

    if (!response.ok) {
      const error = await response.json().catch(() => ({ message: 'Request failed' }));
      throw new Error(error.message || `HTTP ${response.status}`);
    }

    const result = await response.json();
    return result.data || result;
  }

  // Truck API Methods
  async getTrucks(): Promise<TruckData[]> {
    const response = await this.makeRequest<{ trucks: any[], total_count: number }>('/trucks');
    
    // Transform the response to match our interface
    return response.trucks?.map(truck => ({
      id: truck.truck_id?.toString() || truck.id?.toString(),
      truckId: truck.truck_id || truck.truckId,
      plateNumber: truck.license_plate || truck.plateNumber || 'N/A',
      capacity: truck.capacity || 0,
      status: this.mapTruckStatus(truck.status),
      currentX: truck.current_x || truck.currentX || 0,
      currentY: truck.current_y || truck.currentY || 0,
      currentLocation: `${truck.current_x || 0}, ${truck.current_y || 0}`,
      lastUpdated: truck.updated_at || truck.lastUpdated || new Date().toISOString(),
      driverName: truck.driver_name || truck.driverName,
      driver: truck.driver ? {
        id: truck.driver.id?.toString(),
        name: truck.driver.name,
        phone: truck.driver.phone
      } : undefined
    })) || [];
  }

  async getTruckStatistics(): Promise<FleetStatistics> {
    return await this.makeRequest<FleetStatistics>('/trucks/statistics');
  }

  async updateTruckStatus(truckId: number, status: string, x?: number, y?: number): Promise<void> {
    await this.makeRequest(`/trucks/${truckId}/status`, {
      method: 'PUT',
      body: JSON.stringify({ status, x: x || 0, y: y || 0 }),
    });
  }

  async assignDriverToTruck(truckId: number, driverId: number): Promise<void> {
    await this.makeRequest(`/trucks/${truckId}/assign-driver/${driverId}`, {
      method: 'POST',
    });
  }

  async unassignDriverFromTruck(truckId: number): Promise<void> {
    await this.makeRequest(`/trucks/${truckId}/unassign-driver`, {
      method: 'POST',
    });
  }

  // Driver API Methods
  async getDrivers(status?: string): Promise<DriverData[]> {
    const queryParams = status ? `?status=${status}` : '';
    return await this.makeRequest<DriverData[]>(`/drivers${queryParams}`);
  }

  async getDriverById(driverId: number): Promise<DriverData> {
    return await this.makeRequest<DriverData>(`/drivers/${driverId}`);
  }

  async createDriver(driver: CreateDriverRequest): Promise<DriverData> {
    return await this.makeRequest<DriverData>('/drivers', {
      method: 'POST',
      body: JSON.stringify(driver),
    });
  }

  async updateDriver(driverId: number, driver: UpdateDriverRequest): Promise<DriverData> {
    return await this.makeRequest<DriverData>(`/drivers/${driverId}`, {
      method: 'PUT',
      body: JSON.stringify(driver),
    });
  }

  async updateDriverStatus(driverId: number, status: string): Promise<DriverData> {
    return await this.makeRequest<DriverData>(`/drivers/${driverId}/status`, {
      method: 'PUT',
      body: JSON.stringify({ status }),
    });
  }

  async deleteDriver(driverId: number): Promise<void> {
    await this.makeRequest(`/drivers/${driverId}`, {
      method: 'DELETE',
    });
  }

  async getAvailableDrivers(): Promise<DriverData[]> {
    return await this.makeRequest<DriverData[]>('/drivers/available');
  }

  async searchDrivers(name: string): Promise<DriverData[]> {
    return await this.makeRequest<DriverData[]>(`/drivers/search?name=${encodeURIComponent(name)}`);
  }

  async getDriverStatistics(): Promise<any> {
    return await this.makeRequest('/drivers/statistics');
  }

  // Helper methods
  private mapTruckStatus(status: string): TruckData['status'] {
    const statusMap: Record<string, TruckData['status']> = {
      'IDLE': 'IDLE',
      'EN_ROUTE': 'EN_ROUTE',
      'AT_WAREHOUSE': 'AT_WAREHOUSE',
      'LOADING': 'LOADING',
      'DELIVERING': 'DELIVERING',
      'MAINTENANCE': 'MAINTENANCE',
      'OUT_OF_SERVICE': 'OUT_OF_SERVICE'
    };
    
    return statusMap[status] || 'IDLE';
  }

  // Mock data fallback methods (for development)
  async getFleetOverviewMock(): Promise<{
    trucks: TruckData[];
    statusDistribution: Record<string, number>;
    totalTrucks: number;
  }> {
    // This is fallback mock data if real API fails
    const mockTrucks: TruckData[] = [
      {
        id: '1',
        truckId: 1,
        plateNumber: 'TRK-001',
        capacity: 1000,
        status: 'IDLE',
        currentX: 0,
        currentY: 0,
        currentLocation: '0, 0',
        lastUpdated: new Date().toISOString(),
        driverName: 'John Doe'
      },
      {
        id: '2',
        truckId: 2,
        plateNumber: 'TRK-002',
        capacity: 1200,
        status: 'EN_ROUTE',
        currentX: 10,
        currentY: 15,
        currentLocation: '10, 15',
        lastUpdated: new Date().toISOString(),
        driverName: 'Jane Smith'
      }
    ];

    const statusDistribution = mockTrucks.reduce((acc, truck) => {
      acc[truck.status] = (acc[truck.status] || 0) + 1;
      return acc;
    }, {} as Record<string, number>);

    return {
      trucks: mockTrucks,
      statusDistribution,
      totalTrucks: mockTrucks.length
    };
  }
}

// Export singleton instance
export const fleetApi = new FleetApiService();
export default fleetApi;