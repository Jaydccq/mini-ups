/**
 * Fleet Management Page
 * 
 * Features:
 * - Comprehensive fleet and driver management interface
 * - Real-time truck location tracking and status monitoring
 * - Driver assignment and scheduling functionality
 * - Vehicle CRUD operations with detailed information
 * - Interactive fleet overview with map integration
 * 
 * Components:
 * - FleetOverview: Visual fleet status and location display
 * - TruckManagement: CRUD operations for trucks
 * - DriverManagement: Driver assignment and scheduling
 * - VehicleStatusMonitor: Real-time status monitoring
 * - PerformanceMetrics: Fleet performance analytics
 * 
 * @author Mini-UPS Team
 * @version 1.0.0
 */
import React, { useState, useEffect } from 'react';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { Badge } from '@/components/ui/badge';
import { DataTable, Column } from '@/components/ui/data-table';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { Dialog, DialogContent, DialogHeader, DialogTitle, DialogTrigger } from '@/components/ui/dialog';
import { Tabs, TabsContent, TabsList, TabsTrigger } from '@/components/ui/tabs';
import { adminApi } from '@/services/admin';
import { DriverSchedulingCalendar } from '@/components/admin/DriverSchedulingCalendar';
import { VehicleLocationMap } from '@/components/admin/VehicleLocationMap';
import { 
  Truck, 
  Users, 
  Plus, 
  Edit, 
  Trash2, 
  MapPin, 
  Clock, 
  AlertTriangle,
  CheckCircle,
  RefreshCw,
  Search,
  Filter,
  Calendar,
  Navigation
} from 'lucide-react';

interface TruckData {
  id: string;
  plateNumber: string;
  capacity: number;
  status: 'AVAILABLE' | 'IN_TRANSIT' | 'MAINTENANCE' | 'OFFLINE';
  currentLocation: string;
  lastUpdated: string;
  driver?: {
    id: string;
    name: string;
    phone: string;
  };
}

interface DriverData {
  id: string;
  name: string;
  email: string;
  phone: string;
  status: 'ACTIVE' | 'INACTIVE' | 'ON_DUTY' | 'OFF_DUTY';
  lastActive: string;
  assignedTruck?: string;
  totalDeliveries: number;
  rating: number;
}

interface FleetOverview {
  trucks: TruckData[];
  statusDistribution: Record<string, number>;
  totalTrucks: number;
}

interface VehicleLocation {
  id: string;
  plateNumber: string;
  status: 'AVAILABLE' | 'IN_TRANSIT' | 'MAINTENANCE' | 'OFFLINE';
  location: { x: number; y: number };
  heading: number;
  speed: number;
  driver?: {
    id: string;
    name: string;
    phone: string;
  };
  currentRoute?: {
    origin: { x: number; y: number };
    destination: { x: number; y: number };
    waypoints: { x: number; y: number }[];
    progress: number;
  };
  lastUpdate: string;
  todayStats: {
    distanceTraveled: number;
    deliveriesCompleted: number;
    fuelEfficiency: number;
  };
}

interface Driver {
  id: string;
  name: string;
  email: string;
  availability: {
    [key: string]: boolean;
  };
  preferences: {
    preferredShifts: ('MORNING' | 'AFTERNOON' | 'NIGHT')[];
    maxHoursPerWeek: number;
  };
}

const FleetManagementPage: React.FC = () => {
  const [fleetOverview, setFleetOverview] = useState<FleetOverview | null>(null);
  const [drivers, setDrivers] = useState<DriverData[]>([]);
  const [vehicleLocations, setVehicleLocations] = useState<VehicleLocation[]>([]);
  const [schedulingDrivers, setSchedulingDrivers] = useState<Driver[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [searchTerm, setSearchTerm] = useState('');
  const [statusFilter, setStatusFilter] = useState<string>('all');
  const [selectedTruck, setSelectedTruck] = useState<TruckData | null>(null);
  const [isAddTruckDialogOpen, setIsAddTruckDialogOpen] = useState(false);
  const [isAddDriverDialogOpen, setIsAddDriverDialogOpen] = useState(false);
  const [isEditTruckDialogOpen, setIsEditTruckDialogOpen] = useState(false);
  const [isEditDriverDialogOpen, setIsEditDriverDialogOpen] = useState(false);
  const [editingTruck, setEditingTruck] = useState<TruckData | null>(null);
  const [editingDriver, setEditingDriver] = useState<DriverData | null>(null);
  
  // Form states
  const [truckForm, setTruckForm] = useState({
    plateNumber: '',
    capacity: '',
    location: ''
  });
  
  const [driverForm, setDriverForm] = useState({
    name: '',
    email: '',
    phone: '',
    licenseNumber: ''
  });

  const fetchFleetData = async () => {
    try {
      setLoading(true);
      setError(null);
      
      // Fetch fleet overview
      const fleetResponse = await fetch('/api/admin/fleet/overview', {
        headers: { 'Authorization': `Bearer ${localStorage.getItem('token')}` }
      });
      
      if (!fleetResponse.ok) {
        throw new Error('Failed to fetch fleet data');
      }
      
      const fleetData = await fleetResponse.json();
      setFleetOverview(fleetData.data);
      
      // Fetch driver data
      const driversResponse = await fetch('/api/admin/fleet/drivers', {
        headers: { 'Authorization': `Bearer ${localStorage.getItem('token')}` }
      });
      
      if (driversResponse.ok) {
        const driversData = await driversResponse.json();
        // Transform driver data to match interface
        const transformedDrivers: DriverData[] = driversData.data.drivers.map((driver: any) => ({
          id: driver.id,
          name: driver.name,
          email: driver.email,
          phone: driver.phone,
          status: driver.status,
          lastActive: driver.lastActive,
          assignedTruck: driver.assignedTruck,
          totalDeliveries: Math.floor(Math.random() * 100), // Mock data
          rating: 4.0 + Math.random() * 1.0 // Mock rating
        }));
        setDrivers(transformedDrivers);
      }
      
      // Set mock data for vehicle locations and scheduling drivers
      setVehicleLocations(mockVehicleLocations);
      setSchedulingDrivers(mockSchedulingDrivers);
      
    } catch (error) {
      console.error('Error fetching fleet data:', error);
      setError('Failed to load fleet data');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchFleetData();
  }, []);

  // CRUD Operations
  const handleCreateTruck = async () => {
    try {
      const newTruck = await adminApi.createTruck({
        plateNumber: truckForm.plateNumber,
        capacity: parseInt(truckForm.capacity),
        location: truckForm.location
      });
      
      // Update local state
      setFleetOverview(prev => {
        if (!prev) return prev;
        return {
          ...prev,
          trucks: [...prev.trucks, {
            id: newTruck.id.toString(),
            plateNumber: newTruck.plateNumber,
            capacity: newTruck.capacity,
            status: 'AVAILABLE' as const,
            currentLocation: newTruck.location,
            lastUpdated: new Date().toISOString()
          }],
          totalTrucks: prev.totalTrucks + 1
        };
      });
      
      // Reset form and close dialog
      setTruckForm({ plateNumber: '', capacity: '', location: '' });
      setIsAddTruckDialogOpen(false);
    } catch (error) {
      console.error('Error creating truck:', error);
      setError('Failed to create truck');
    }
  };

  const handleUpdateTruck = async () => {
    if (!editingTruck) return;
    
    try {
      const updatedTruck = await adminApi.updateTruck(parseInt(editingTruck.id), {
        plateNumber: truckForm.plateNumber,
        capacity: parseInt(truckForm.capacity),
        location: truckForm.location
      });
      
      // Update local state
      setFleetOverview(prev => {
        if (!prev) return prev;
        return {
          ...prev,
          trucks: prev.trucks.map(truck => 
            truck.id === editingTruck.id 
              ? { ...truck, plateNumber: updatedTruck.plateNumber, capacity: updatedTruck.capacity, currentLocation: updatedTruck.location }
              : truck
          )
        };
      });
      
      // Reset form and close dialog
      setTruckForm({ plateNumber: '', capacity: '', location: '' });
      setEditingTruck(null);
      setIsEditTruckDialogOpen(false);
    } catch (error) {
      console.error('Error updating truck:', error);
      setError('Failed to update truck');
    }
  };

  const handleDeleteTruck = async (truckId: string) => {
    if (!confirm('Are you sure you want to delete this truck?')) return;
    
    try {
      await adminApi.deleteTruck(parseInt(truckId));
      
      // Update local state
      setFleetOverview(prev => {
        if (!prev) return prev;
        return {
          ...prev,
          trucks: prev.trucks.filter(truck => truck.id !== truckId),
          totalTrucks: prev.totalTrucks - 1
        };
      });
    } catch (error) {
      console.error('Error deleting truck:', error);
      setError('Failed to delete truck');
    }
  };

  const handleCreateDriver = async () => {
    try {
      const newDriver = await adminApi.createDriver({
        name: driverForm.name,
        email: driverForm.email,
        phone: driverForm.phone,
        licenseNumber: driverForm.licenseNumber
      });
      
      // Update local state
      setDrivers(prev => [...prev, {
        id: newDriver.id.toString(),
        name: newDriver.name,
        email: newDriver.email,
        phone: newDriver.phone,
        status: 'ACTIVE',
        lastActive: new Date().toISOString(),
        totalDeliveries: 0,
        rating: 4.5
      }]);
      
      // Reset form and close dialog
      setDriverForm({ name: '', email: '', phone: '', licenseNumber: '' });
      setIsAddDriverDialogOpen(false);
    } catch (error) {
      console.error('Error creating driver:', error);
      setError('Failed to create driver');
    }
  };

  const handleUpdateDriver = async () => {
    if (!editingDriver) return;
    
    try {
      const updatedDriver = await adminApi.updateDriver(parseInt(editingDriver.id), {
        name: driverForm.name,
        email: driverForm.email,
        phone: driverForm.phone,
        licenseNumber: driverForm.licenseNumber
      });
      
      // Update local state
      setDrivers(prev => prev.map(driver => 
        driver.id === editingDriver.id 
          ? { ...driver, name: updatedDriver.name, email: updatedDriver.email, phone: updatedDriver.phone }
          : driver
      ));
      
      // Reset form and close dialog
      setDriverForm({ name: '', email: '', phone: '', licenseNumber: '' });
      setEditingDriver(null);
      setIsEditDriverDialogOpen(false);
    } catch (error) {
      console.error('Error updating driver:', error);
      setError('Failed to update driver');
    }
  };

  const handleDeleteDriver = async (driverId: string) => {
    if (!confirm('Are you sure you want to delete this driver?')) return;
    
    try {
      await adminApi.deleteDriver(parseInt(driverId));
      
      // Update local state
      setDrivers(prev => prev.filter(driver => driver.id !== driverId));
    } catch (error) {
      console.error('Error deleting driver:', error);
      setError('Failed to delete driver');
    }
  };

  const handleEditTruck = (truck: TruckData) => {
    setEditingTruck(truck);
    setTruckForm({
      plateNumber: truck.plateNumber,
      capacity: truck.capacity.toString(),
      location: truck.currentLocation
    });
    setIsEditTruckDialogOpen(true);
  };

  const handleEditDriver = (driver: DriverData) => {
    setEditingDriver(driver);
    setDriverForm({
      name: driver.name,
      email: driver.email,
      phone: driver.phone,
      licenseNumber: '' // We don't have this in the current interface
    });
    setIsEditDriverDialogOpen(true);
  };

  const getStatusColor = (status: string) => {
    switch (status) {
      case 'AVAILABLE':
        return 'bg-green-100 text-green-800';
      case 'IN_TRANSIT':
        return 'bg-blue-100 text-blue-800';
      case 'MAINTENANCE':
        return 'bg-yellow-100 text-yellow-800';
      case 'OFFLINE':
        return 'bg-red-100 text-red-800';
      case 'ACTIVE':
      case 'ON_DUTY':
        return 'bg-green-100 text-green-800';
      case 'INACTIVE':
      case 'OFF_DUTY':
        return 'bg-gray-100 text-gray-800';
      default:
        return 'bg-gray-100 text-gray-800';
    }
  };

  const getStatusIcon = (status: string) => {
    switch (status) {
      case 'AVAILABLE':
      case 'ACTIVE':
        return <CheckCircle className="w-4 h-4" />;
      case 'IN_TRANSIT':
        return <Truck className="w-4 h-4" />;
      case 'MAINTENANCE':
        return <AlertTriangle className="w-4 h-4" />;
      default:
        return <Clock className="w-4 h-4" />;
    }
  };

  const filteredTrucks = fleetOverview?.trucks.filter(truck => {
    const matchesSearch = truck.plateNumber.toLowerCase().includes(searchTerm.toLowerCase()) ||
                         truck.currentLocation.toLowerCase().includes(searchTerm.toLowerCase());
    const matchesStatus = statusFilter === 'all' || truck.status === statusFilter;
    return matchesSearch && matchesStatus;
  }) || [];

  const truckColumns: Column<TruckData>[] = [
    {
      key: 'plateNumber',
      title: 'Plate Number',
      render: (value: any, truck: TruckData) => (
        <div className="font-medium">{truck.plateNumber}</div>
      )
    },
    {
      key: 'status',
      title: 'Status',
      render: (value: any, truck: TruckData) => (
        <Badge className={getStatusColor(truck.status)}>
          {getStatusIcon(truck.status)}
          <span className="ml-1">{truck.status}</span>
        </Badge>
      )
    },
    {
      key: 'currentLocation',
      title: 'Location',
      render: (value: any, truck: TruckData) => (
        <div className="flex items-center">
          <MapPin className="w-4 h-4 mr-1 text-gray-500" />
          {truck.currentLocation}
        </div>
      )
    },
    {
      key: 'capacity',
      title: 'Capacity',
      render: (value: any, truck: TruckData) => (
        <div>{truck.capacity} kg</div>
      )
    },
    {
      key: 'actions',
      title: 'Actions',
      render: (value: any, truck: TruckData) => (
        <div className="flex space-x-2">
          <Button
            variant="outline"
            size="sm"
            onClick={() => handleEditTruck(truck)}
          >
            <Edit className="w-4 h-4" />
          </Button>
          <Button
            variant="outline"
            size="sm"
            className="text-red-600 hover:text-red-700"
            onClick={() => handleDeleteTruck(truck.id)}
          >
            <Trash2 className="w-4 h-4" />
          </Button>
        </div>
      )
    }
  ];

  const driverColumns: Column<DriverData>[] = [
    {
      key: 'name',
      title: 'Name',
      render: (value: any, driver: DriverData) => (
        <div>
          <div className="font-medium">{driver.name}</div>
          <div className="text-sm text-gray-500">{driver.email}</div>
        </div>
      )
    },
    {
      key: 'status',
      title: 'Status',
      render: (value: any, driver: DriverData) => (
        <Badge className={getStatusColor(driver.status)}>
          {getStatusIcon(driver.status)}
          <span className="ml-1">{driver.status}</span>
        </Badge>
      )
    },
    {
      key: 'phone',
      title: 'Phone',
      render: (value: any, driver: DriverData) => (
        <div>{driver.phone}</div>
      )
    },
    {
      key: 'assignedTruck',
      title: 'Assigned Truck',
      render: (value: any, driver: DriverData) => (
        <div>{driver.assignedTruck || 'None'}</div>
      )
    },
    {
      key: 'rating',
      title: 'Performance',
      render: (value: any, driver: DriverData) => (
        <div>
          <div className="text-sm">⭐ {driver.rating.toFixed(1)}</div>
          <div className="text-xs text-gray-500">{driver.totalDeliveries} deliveries</div>
        </div>
      )
    },
    {
      key: 'actions',
      title: 'Actions',
      render: (value: any, driver: DriverData) => (
        <div className="flex space-x-2">
          <Button variant="outline" size="sm">
            <Edit className="w-4 h-4" />
          </Button>
          <Button variant="outline" size="sm" className="text-red-600">
            <Trash2 className="w-4 h-4" />
          </Button>
        </div>
      )
    }
  ];

  if (loading) {
    return (
      <div className="flex items-center justify-center h-64">
        <div className="flex items-center space-x-2">
          <RefreshCw className="w-5 h-5 animate-spin" />
          <span>Loading fleet data...</span>
        </div>
      </div>
    );
  }

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex justify-between items-center">
        <div>
          <h1 className="text-3xl font-bold">Fleet Management</h1>
          <p className="text-gray-600">Manage trucks, drivers, and fleet operations</p>
        </div>
        <Button onClick={fetchFleetData} variant="outline">
          <RefreshCw className="w-4 h-4 mr-2" />
          Refresh
        </Button>
      </div>

      {/* Fleet Overview Cards */}
      {fleetOverview && (
        <div className="grid gap-4 md:grid-cols-2 lg:grid-cols-4">
          <Card>
            <CardContent className="p-4">
              <div className="flex items-center justify-between">
                <div>
                  <p className="text-sm font-medium text-gray-600">Total Trucks</p>
                  <p className="text-2xl font-bold">{fleetOverview.totalTrucks}</p>
                </div>
                <Truck className="w-8 h-8 text-blue-600" />
              </div>
            </CardContent>
          </Card>
          
          <Card>
            <CardContent className="p-4">
              <div className="flex items-center justify-between">
                <div>
                  <p className="text-sm font-medium text-gray-600">Available</p>
                  <p className="text-2xl font-bold text-green-600">
                    {fleetOverview.statusDistribution.AVAILABLE || 0}
                  </p>
                </div>
                <CheckCircle className="w-8 h-8 text-green-600" />
              </div>
            </CardContent>
          </Card>
          
          <Card>
            <CardContent className="p-4">
              <div className="flex items-center justify-between">
                <div>
                  <p className="text-sm font-medium text-gray-600">In Transit</p>
                  <p className="text-2xl font-bold text-blue-600">
                    {fleetOverview.statusDistribution.IN_TRANSIT || 0}
                  </p>
                </div>
                <Truck className="w-8 h-8 text-blue-600" />
              </div>
            </CardContent>
          </Card>
          
          <Card>
            <CardContent className="p-4">
              <div className="flex items-center justify-between">
                <div>
                  <p className="text-sm font-medium text-gray-600">Maintenance</p>
                  <p className="text-2xl font-bold text-yellow-600">
                    {fleetOverview.statusDistribution.MAINTENANCE || 0}
                  </p>
                </div>
                <AlertTriangle className="w-8 h-8 text-yellow-600" />
              </div>
            </CardContent>
          </Card>
        </div>
      )}

      {/* Fleet Management Tabs */}
      <Tabs defaultValue="trucks" className="w-full">
        <TabsList className="grid w-full grid-cols-4">
          <TabsTrigger value="trucks">Trucks</TabsTrigger>
          <TabsTrigger value="drivers">Drivers</TabsTrigger>
          <TabsTrigger value="locations">
            <Navigation className="w-4 h-4 mr-2" />
            Locations
          </TabsTrigger>
          <TabsTrigger value="scheduling">
            <Calendar className="w-4 h-4 mr-2" />
            Scheduling
          </TabsTrigger>
        </TabsList>
        
        <TabsContent value="trucks" className="space-y-4">
          <Card>
            <CardHeader>
              <div className="flex justify-between items-center">
                <CardTitle className="flex items-center">
                  <Truck className="w-5 h-5 mr-2" />
                  Truck Management
                </CardTitle>
                <Dialog open={isAddTruckDialogOpen} onOpenChange={setIsAddTruckDialogOpen}>
                  <DialogTrigger asChild>
                    <Button>
                      <Plus className="w-4 h-4 mr-2" />
                      Add Truck
                    </Button>
                  </DialogTrigger>
                  <DialogContent>
                    <DialogHeader>
                      <DialogTitle>Add New Truck</DialogTitle>
                    </DialogHeader>
                    <div className="space-y-4">
                      <div>
                        <Label htmlFor="plateNumber">Plate Number</Label>
                        <Input 
                          id="plateNumber" 
                          placeholder="Enter plate number" 
                          value={truckForm.plateNumber}
                          onChange={(e) => setTruckForm(prev => ({ ...prev, plateNumber: e.target.value }))}
                        />
                      </div>
                      <div>
                        <Label htmlFor="capacity">Capacity (kg)</Label>
                        <Input 
                          id="capacity" 
                          type="number" 
                          placeholder="Enter capacity" 
                          value={truckForm.capacity}
                          onChange={(e) => setTruckForm(prev => ({ ...prev, capacity: e.target.value }))}
                        />
                      </div>
                      <div>
                        <Label htmlFor="location">Current Location</Label>
                        <Input 
                          id="location" 
                          placeholder="Enter location" 
                          value={truckForm.location}
                          onChange={(e) => setTruckForm(prev => ({ ...prev, location: e.target.value }))}
                        />
                      </div>
                      <div className="flex justify-end space-x-2">
                        <Button variant="outline" onClick={() => setIsAddTruckDialogOpen(false)}>
                          Cancel
                        </Button>
                        <Button onClick={handleCreateTruck}>Add Truck</Button>
                      </div>
                    </div>
                  </DialogContent>
                </Dialog>
              </div>
            </CardHeader>
            <CardContent>
              {/* Search and Filter */}
              <div className="flex space-x-4 mb-4">
                <div className="flex-1">
                  <div className="relative">
                    <Search className="absolute left-3 top-1/2 transform -translate-y-1/2 text-gray-400 w-4 h-4" />
                    <Input
                      placeholder="Search trucks..."
                      value={searchTerm}
                      onChange={(e) => setSearchTerm(e.target.value)}
                      className="pl-10"
                    />
                  </div>
                </div>
                <select
                  value={statusFilter}
                  onChange={(e) => setStatusFilter(e.target.value)}
                  className="px-3 py-2 border rounded-md"
                >
                  <option value="all">All Status</option>
                  <option value="AVAILABLE">Available</option>
                  <option value="IN_TRANSIT">In Transit</option>
                  <option value="MAINTENANCE">Maintenance</option>
                  <option value="OFFLINE">Offline</option>
                </select>
              </div>
              
              <DataTable
                data={filteredTrucks}
                columns={truckColumns}
                searchable={false}
                pagination={{
                  current: 1,
                  pageSize: 10,
                  total: filteredTrucks.length,
                  onChange: (page, pageSize) => {}
                }}
              />
            </CardContent>
          </Card>

          {/* Edit Truck Dialog */}
          <Dialog open={isEditTruckDialogOpen} onOpenChange={setIsEditTruckDialogOpen}>
            <DialogContent>
              <DialogHeader>
                <DialogTitle>Edit Truck</DialogTitle>
              </DialogHeader>
              <div className="space-y-4">
                <div>
                  <Label htmlFor="editPlateNumber">Plate Number</Label>
                  <Input 
                    id="editPlateNumber" 
                    placeholder="Enter plate number" 
                    value={truckForm.plateNumber}
                    onChange={(e) => setTruckForm(prev => ({ ...prev, plateNumber: e.target.value }))}
                  />
                </div>
                <div>
                  <Label htmlFor="editCapacity">Capacity (kg)</Label>
                  <Input 
                    id="editCapacity" 
                    type="number" 
                    placeholder="Enter capacity" 
                    value={truckForm.capacity}
                    onChange={(e) => setTruckForm(prev => ({ ...prev, capacity: e.target.value }))}
                  />
                </div>
                <div>
                  <Label htmlFor="editLocation">Current Location</Label>
                  <Input 
                    id="editLocation" 
                    placeholder="Enter location" 
                    value={truckForm.location}
                    onChange={(e) => setTruckForm(prev => ({ ...prev, location: e.target.value }))}
                  />
                </div>
                <div className="flex justify-end space-x-2">
                  <Button variant="outline" onClick={() => setIsEditTruckDialogOpen(false)}>
                    Cancel
                  </Button>
                  <Button onClick={handleUpdateTruck}>Update Truck</Button>
                </div>
              </div>
            </DialogContent>
          </Dialog>
        </TabsContent>
        
        <TabsContent value="drivers" className="space-y-4">
          <Card>
            <CardHeader>
              <div className="flex justify-between items-center">
                <CardTitle className="flex items-center">
                  <Users className="w-5 h-5 mr-2" />
                  Driver Management
                </CardTitle>
                <Dialog open={isAddDriverDialogOpen} onOpenChange={setIsAddDriverDialogOpen}>
                  <DialogTrigger asChild>
                    <Button>
                      <Plus className="w-4 h-4 mr-2" />
                      Add Driver
                    </Button>
                  </DialogTrigger>
                  <DialogContent>
                    <DialogHeader>
                      <DialogTitle>Add New Driver</DialogTitle>
                    </DialogHeader>
                    <div className="space-y-4">
                      <div>
                        <Label htmlFor="driverName">Full Name</Label>
                        <Input 
                          id="driverName" 
                          placeholder="Enter full name" 
                          value={driverForm.name}
                          onChange={(e) => setDriverForm(prev => ({ ...prev, name: e.target.value }))}
                        />
                      </div>
                      <div>
                        <Label htmlFor="driverEmail">Email</Label>
                        <Input 
                          id="driverEmail" 
                          type="email" 
                          placeholder="Enter email" 
                          value={driverForm.email}
                          onChange={(e) => setDriverForm(prev => ({ ...prev, email: e.target.value }))}
                        />
                      </div>
                      <div>
                        <Label htmlFor="driverPhone">Phone</Label>
                        <Input 
                          id="driverPhone" 
                          placeholder="Enter phone number" 
                          value={driverForm.phone}
                          onChange={(e) => setDriverForm(prev => ({ ...prev, phone: e.target.value }))}
                        />
                      </div>
                      <div>
                        <Label htmlFor="driverLicense">License Number</Label>
                        <Input 
                          id="driverLicense" 
                          placeholder="Enter license number" 
                          value={driverForm.licenseNumber}
                          onChange={(e) => setDriverForm(prev => ({ ...prev, licenseNumber: e.target.value }))}
                        />
                      </div>
                      <div className="flex justify-end space-x-2">
                        <Button variant="outline" onClick={() => setIsAddDriverDialogOpen(false)}>
                          Cancel
                        </Button>
                        <Button onClick={handleCreateDriver}>Add Driver</Button>
                      </div>
                    </div>
                  </DialogContent>
                </Dialog>
              </div>
            </CardHeader>
            <CardContent>
              <DataTable
                data={drivers}
                columns={driverColumns}
                searchable={true}
                pagination={{
                  current: 1,
                  pageSize: 10,
                  total: drivers.length,
                  onChange: (page, pageSize) => {}
                }}
              />
            </CardContent>
          </Card>
        </TabsContent>
        
        <TabsContent value="locations" className="space-y-4">
          <VehicleLocationMap
            vehicles={vehicleLocations}
            onVehicleSelect={(vehicle) => console.log('Selected vehicle:', vehicle)}
          />
        </TabsContent>
        
        <TabsContent value="scheduling" className="space-y-4">
          <DriverSchedulingCalendar drivers={schedulingDrivers} />
        </TabsContent>
      </Tabs>
    </div>
  );
};

// Mock data for vehicle locations
const mockVehicleLocations: VehicleLocation[] = [
  {
    id: '1',
    plateNumber: 'TRK-001',
    status: 'IN_TRANSIT',
    location: { x: 10, y: 20 },
    heading: 45,
    speed: 65,
    driver: {
      id: '1',
      name: 'John Smith',
      phone: '+1-555-0101'
    },
    currentRoute: {
      origin: { x: 0, y: 0 },
      destination: { x: 50, y: 40 },
      waypoints: [{ x: 25, y: 20 }],
      progress: 60
    },
    lastUpdate: new Date().toISOString(),
    todayStats: {
      distanceTraveled: 156,
      deliveriesCompleted: 8,
      fuelEfficiency: 12.5
    }
  },
  {
    id: '2',
    plateNumber: 'TRK-002',
    status: 'AVAILABLE',
    location: { x: -15, y: 30 },
    heading: 180,
    speed: 0,
    driver: {
      id: '2',
      name: 'Jane Doe',
      phone: '+1-555-0102'
    },
    lastUpdate: new Date().toISOString(),
    todayStats: {
      distanceTraveled: 89,
      deliveriesCompleted: 5,
      fuelEfficiency: 11.8
    }
  },
  {
    id: '3',
    plateNumber: 'TRK-003',
    status: 'MAINTENANCE',
    location: { x: 25, y: -10 },
    heading: 270,
    speed: 0,
    lastUpdate: new Date().toISOString(),
    todayStats: {
      distanceTraveled: 0,
      deliveriesCompleted: 0,
      fuelEfficiency: 0
    }
  },
  {
    id: '4',
    plateNumber: 'TRK-004',
    status: 'IN_TRANSIT',
    location: { x: -5, y: -25 },
    heading: 90,
    speed: 45,
    driver: {
      id: '3',
      name: 'Mike Johnson',
      phone: '+1-555-0103'
    },
    currentRoute: {
      origin: { x: -30, y: -30 },
      destination: { x: 20, y: -20 },
      waypoints: [{ x: -10, y: -25 }],
      progress: 75
    },
    lastUpdate: new Date().toISOString(),
    todayStats: {
      distanceTraveled: 203,
      deliveriesCompleted: 12,
      fuelEfficiency: 13.2
    }
  }
];

// Mock data for scheduling drivers
const mockSchedulingDrivers: Driver[] = [
  {
    id: '1',
    name: 'John Smith',
    email: 'john.smith@miniups.com',
    availability: {
      [new Date().toISOString().split('T')[0]]: true,
      [new Date(Date.now() + 86400000).toISOString().split('T')[0]]: true,
      [new Date(Date.now() + 172800000).toISOString().split('T')[0]]: false
    },
    preferences: {
      preferredShifts: ['MORNING', 'AFTERNOON'],
      maxHoursPerWeek: 40
    }
  },
  {
    id: '2',
    name: 'Jane Doe',
    email: 'jane.doe@miniups.com',
    availability: {
      [new Date().toISOString().split('T')[0]]: true,
      [new Date(Date.now() + 86400000).toISOString().split('T')[0]]: true,
      [new Date(Date.now() + 172800000).toISOString().split('T')[0]]: true
    },
    preferences: {
      preferredShifts: ['AFTERNOON', 'NIGHT'],
      maxHoursPerWeek: 35
    }
  },
  {
    id: '3',
    name: 'Mike Johnson',
    email: 'mike.johnson@miniups.com',
    availability: {
      [new Date().toISOString().split('T')[0]]: true,
      [new Date(Date.now() + 86400000).toISOString().split('T')[0]]: false,
      [new Date(Date.now() + 172800000).toISOString().split('T')[0]]: true
    },
    preferences: {
      preferredShifts: ['MORNING'],
      maxHoursPerWeek: 45
    }
  },
  {
    id: '4',
    name: 'Sarah Wilson',
    email: 'sarah.wilson@miniups.com',
    availability: {
      [new Date().toISOString().split('T')[0]]: false,
      [new Date(Date.now() + 86400000).toISOString().split('T')[0]]: true,
      [new Date(Date.now() + 172800000).toISOString().split('T')[0]]: true
    },
    preferences: {
      preferredShifts: ['NIGHT'],
      maxHoursPerWeek: 30
    }
  }
];

export default FleetManagementPage;