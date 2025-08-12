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
 *
 
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
import { fleetApi, type TruckData, type DriverData } from '../../../services/fleet';
import { DriverSchedulingCalendar } from '../components/DriverSchedulingCalendar';
import { VehicleLocationMap } from '../components/VehicleLocationMap';
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

// Updated interfaces to match API response
interface FleetTruckData extends TruckData {}
interface FleetDriverData extends DriverData {}

interface FleetOverview {
  trucks: FleetTruckData[];
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
  const [drivers, setDrivers] = useState<FleetDriverData[]>([]);
  const [vehicleLocations, setVehicleLocations] = useState<VehicleLocation[]>([]);
  const [schedulingDrivers, setSchedulingDrivers] = useState<Driver[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [searchTerm, setSearchTerm] = useState('');
  const [statusFilter, setStatusFilter] = useState<string>('all');
  const [selectedTruck, setSelectedTruck] = useState<FleetTruckData | null>(null);
  const [isAddTruckDialogOpen, setIsAddTruckDialogOpen] = useState(false);
  const [isAddDriverDialogOpen, setIsAddDriverDialogOpen] = useState(false);
  const [isEditTruckDialogOpen, setIsEditTruckDialogOpen] = useState(false);
  const [isEditDriverDialogOpen, setIsEditDriverDialogOpen] = useState(false);
  const [editingTruck, setEditingTruck] = useState<FleetTruckData | null>(null);
  const [editingDriver, setEditingDriver] = useState<FleetDriverData | null>(null);
  
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
      
      // Fetch trucks data
      const trucks = await fleetApi.getTrucks();
      
      // Calculate status distribution
      const statusDistribution = trucks.reduce((acc, truck) => {
        const status = mapFrontendStatus(truck.status);
        acc[status] = (acc[status] || 0) + 1;
        return acc;
      }, {} as Record<string, number>);
      
      // Set fleet overview
      setFleetOverview({
        trucks: trucks,
        statusDistribution,
        totalTrucks: trucks.length
      });
      
      // Fetch drivers data
      const driversData = await fleetApi.getDrivers();
      setDrivers(driversData);
      
      // Set mock data for vehicle locations and scheduling drivers (for now)
      setVehicleLocations(mockVehicleLocations);
      setSchedulingDrivers(mockSchedulingDrivers);
      
    } catch (error) {
      console.error('Error fetching fleet data:', error);
      
      // Fallback to mock data if API fails
      try {
        const mockFleetData = await fleetApi.getFleetOverviewMock();
        setFleetOverview(mockFleetData);
        setError('Using mock data - API connection failed');
      } catch (mockError) {
        setError('Failed to load fleet data');
      }
    } finally {
      setLoading(false);
    }
  };

  // Helper function to map backend status to frontend status
  const mapFrontendStatus = (backendStatus: string): string => {
    const statusMap: Record<string, string> = {
      'IDLE': 'AVAILABLE',
      'EN_ROUTE': 'IN_TRANSIT',
      'AT_WAREHOUSE': 'AVAILABLE',
      'LOADING': 'IN_TRANSIT',
      'DELIVERING': 'IN_TRANSIT',
      'MAINTENANCE': 'MAINTENANCE',
      'OUT_OF_SERVICE': 'OFFLINE'
    };
    
    return statusMap[backendStatus] || 'AVAILABLE';
  };

  useEffect(() => {
    fetchFleetData();
  }, []);

  // CRUD Operations
  const handleCreateTruck = async () => {
    try {
      // For now, show message that this feature is not implemented
      // TODO: Implement createTruck in backend API
      alert('Create truck feature is not yet implemented in the backend API');
      setIsAddTruckDialogOpen(false);
    } catch (error) {
      console.error('Error creating truck:', error);
      setError('Failed to create truck');
    }
  };

  const handleUpdateTruck = async () => {
    if (!editingTruck) return;
    
    try {
      // For now, show message that this feature is not implemented
      alert('Update truck feature is not yet implemented');
      setIsEditTruckDialogOpen(false);
    } catch (error) {
      console.error('Error updating truck:', error);
      setError('Failed to update truck');
    }
  };

  const handleDeleteTruck = async (truckId: string) => {
    if (!confirm('Are you sure you want to delete this truck?')) return;
    
    try {
      // For now, show message that this feature is not implemented
      alert('Delete truck feature is not yet implemented');
    } catch (error) {
      console.error('Error deleting truck:', error);
      setError('Failed to delete truck');
    }
  };

  const handleCreateDriver = async () => {
    try {
      const newDriver = await fleetApi.createDriver({
        name: driverForm.name,
        email: driverForm.email,
        phone: driverForm.phone,
        licenseNumber: driverForm.licenseNumber
      });
      
      // Update local state
      setDrivers(prev => [...prev, newDriver]);
      
      // Reset form and close dialog
      setDriverForm({ name: '', email: '', phone: '', licenseNumber: '' });
      setIsAddDriverDialogOpen(false);
    } catch (error) {
      console.error('Error creating driver:', error);
      setError('Failed to create driver: ' + (error as Error).message);
    }
  };

  const handleUpdateDriver = async () => {
    if (!editingDriver) return;
    
    try {
      const updatedDriver = await fleetApi.updateDriver(parseInt(editingDriver.id), {
        name: driverForm.name,
        email: driverForm.email,
        phone: driverForm.phone,
        licenseNumber: driverForm.licenseNumber
      });
      
      // Update local state
      setDrivers(prev => prev.map(driver => 
        driver.id === editingDriver.id 
          ? updatedDriver
          : driver
      ));
      
      // Reset form and close dialog
      setDriverForm({ name: '', email: '', phone: '', licenseNumber: '' });
      setEditingDriver(null);
      setIsEditDriverDialogOpen(false);
    } catch (error) {
      console.error('Error updating driver:', error);
      setError('Failed to update driver: ' + (error as Error).message);
    }
  };

  const handleDeleteDriver = async (driverId: string) => {
    if (!confirm('Are you sure you want to delete this driver?')) return;
    
    try {
      await fleetApi.deleteDriver(parseInt(driverId));
      
      // Update local state
      setDrivers(prev => prev.filter(driver => driver.id !== driverId));
    } catch (error) {
      console.error('Error deleting driver:', error);
      setError('Failed to delete driver: ' + (error as Error).message);
    }
  };

  const handleEditTruck = (truck: FleetTruckData) => {
    setEditingTruck(truck);
    setTruckForm({
      plateNumber: truck.plateNumber,
      capacity: truck.capacity.toString(),
      location: truck.currentLocation
    });
    setIsEditTruckDialogOpen(true);
  };

  const handleEditDriver = (driver: FleetDriverData) => {
    setEditingDriver(driver);
    setDriverForm({
      name: driver.name,
      email: driver.email,
      phone: driver.phone,
      licenseNumber: driver.licenseNumber || ''
    });
    setIsEditDriverDialogOpen(true);
  };

  const getStatusColor = (status: string) => {
    switch (status) {
      // Truck statuses
      case 'IDLE':
      case 'AVAILABLE':
        return 'bg-green-100 text-green-800';
      case 'EN_ROUTE':
      case 'IN_TRANSIT':
      case 'LOADING':
      case 'DELIVERING':
        return 'bg-blue-100 text-blue-800';
      case 'AT_WAREHOUSE':
        return 'bg-purple-100 text-purple-800';
      case 'MAINTENANCE':
        return 'bg-yellow-100 text-yellow-800';
      case 'OUT_OF_SERVICE':
      case 'OFFLINE':
        return 'bg-red-100 text-red-800';
      
      // Driver statuses
      case 'UNASSIGNED':
        return 'bg-gray-100 text-gray-800';
      case 'ASSIGNED':
        return 'bg-green-100 text-green-800';
      case 'ON_DUTY':
        return 'bg-blue-100 text-blue-800';
      case 'OFF_DUTY':
        return 'bg-orange-100 text-orange-800';
      case 'ON_LEAVE':
        return 'bg-yellow-100 text-yellow-800';
      case 'INACTIVE':
        return 'bg-red-100 text-red-800';
      
      // Legacy statuses
      case 'ACTIVE':
        return 'bg-green-100 text-green-800';
      default:
        return 'bg-gray-100 text-gray-800';
    }
  };

  const getStatusIcon = (status: string) => {
    switch (status) {
      case 'IDLE':
      case 'AVAILABLE':
      case 'ASSIGNED':
      case 'ACTIVE':
        return <CheckCircle className="w-4 h-4" />;
      case 'EN_ROUTE':
      case 'IN_TRANSIT':
      case 'ON_DUTY':
        return <Truck className="w-4 h-4" />;
      case 'LOADING':
      case 'DELIVERING':
        return <Navigation className="w-4 h-4" />;
      case 'AT_WAREHOUSE':
        return <MapPin className="w-4 h-4" />;
      case 'MAINTENANCE':
      case 'ON_LEAVE':
        return <AlertTriangle className="w-4 h-4" />;
      case 'UNASSIGNED':
      case 'OFF_DUTY':
        return <Clock className="w-4 h-4" />;
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

  const truckColumns: Column<FleetTruckData>[] = [
    {
      key: 'plateNumber',
      title: 'Plate Number',
      render: (value: any, truck: FleetTruckData) => (
        <div className="font-medium">{truck.plateNumber}</div>
      )
    },
    {
      key: 'status',
      title: 'Status',
      render: (value: any, truck: FleetTruckData) => (
        <Badge className={getStatusColor(truck.status)}>
          {getStatusIcon(truck.status)}
          <span className="ml-1">{truck.status}</span>
        </Badge>
      )
    },
    {
      key: 'currentLocation',
      title: 'Location',
      render: (value: any, truck: FleetTruckData) => (
        <div className="flex items-center">
          <MapPin className="w-4 h-4 mr-1 text-gray-500" />
          {truck.currentLocation}
        </div>
      )
    },
    {
      key: 'capacity',
      title: 'Capacity',
      render: (value: any, truck: FleetTruckData) => (
        <div>{truck.capacity} kg</div>
      )
    },
    {
      key: 'driver',
      title: 'Driver',
      render: (value: any, truck: FleetTruckData) => (
        <div>{truck.driverName || 'Unassigned'}</div>
      )
    },
    {
      key: 'actions',
      title: 'Actions',
      render: (value: any, truck: FleetTruckData) => (
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

  const driverColumns: Column<FleetDriverData>[] = [
    {
      key: 'name',
      title: 'Name',
      render: (value: any, driver: FleetDriverData) => (
        <div>
          <div className="font-medium">{driver.name}</div>
          <div className="text-sm text-gray-500">{driver.email}</div>
        </div>
      )
    },
    {
      key: 'status',
      title: 'Status',
      render: (value: any, driver: FleetDriverData) => (
        <Badge className={getStatusColor(driver.status)}>
          {getStatusIcon(driver.status)}
          <span className="ml-1">{driver.status}</span>
        </Badge>
      )
    },
    {
      key: 'phone',
      title: 'Phone',
      render: (value: any, driver: FleetDriverData) => (
        <div>{driver.phone}</div>
      )
    },
    {
      key: 'assignedTruck',
      title: 'Assigned Truck',
      render: (value: any, driver: FleetDriverData) => (
        <div>
          {driver.assignedTruck ? 
            `${driver.assignedTruck.plateNumber} (${driver.assignedTruck.truckId})` :
            'None'
          }
        </div>
      )
    },
    {
      key: 'rating',
      title: 'Performance',
      render: (value: any, driver: FleetDriverData) => (
        <div>
          <div className="text-sm">⭐ {driver.rating?.toFixed(1) || '4.0'}</div>
          <div className="text-xs text-gray-500">{driver.totalDeliveries || 0} deliveries</div>
        </div>
      )
    },
    {
      key: 'actions',
      title: 'Actions',
      render: (value: any, driver: FleetDriverData) => (
        <div className="flex space-x-2">
          <Button 
            variant="outline" 
            size="sm" 
            onClick={() => handleEditDriver(driver)}
          >
            <Edit className="w-4 h-4" />
          </Button>
          <Button 
            variant="outline" 
            size="sm" 
            className="text-red-600 hover:text-red-700"
            onClick={() => handleDeleteDriver(driver.id)}
          >
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
          
          {/* Edit Driver Dialog */}
          <Dialog open={isEditDriverDialogOpen} onOpenChange={setIsEditDriverDialogOpen}>
            <DialogContent>
              <DialogHeader>
                <DialogTitle>Edit Driver</DialogTitle>
              </DialogHeader>
              <div className="space-y-4">
                <div>
                  <Label htmlFor="editDriverName">Full Name</Label>
                  <Input 
                    id="editDriverName" 
                    placeholder="Enter full name" 
                    value={driverForm.name}
                    onChange={(e) => setDriverForm(prev => ({ ...prev, name: e.target.value }))}
                  />
                </div>
                <div>
                  <Label htmlFor="editDriverEmail">Email</Label>
                  <Input 
                    id="editDriverEmail" 
                    type="email" 
                    placeholder="Enter email" 
                    value={driverForm.email}
                    onChange={(e) => setDriverForm(prev => ({ ...prev, email: e.target.value }))}
                  />
                </div>
                <div>
                  <Label htmlFor="editDriverPhone">Phone</Label>
                  <Input 
                    id="editDriverPhone" 
                    placeholder="Enter phone number" 
                    value={driverForm.phone}
                    onChange={(e) => setDriverForm(prev => ({ ...prev, phone: e.target.value }))}
                  />
                </div>
                <div>
                  <Label htmlFor="editDriverLicense">License Number</Label>
                  <Input 
                    id="editDriverLicense" 
                    placeholder="Enter license number" 
                    value={driverForm.licenseNumber}
                    onChange={(e) => setDriverForm(prev => ({ ...prev, licenseNumber: e.target.value }))}
                  />
                </div>
                <div className="flex justify-end space-x-2">
                  <Button variant="outline" onClick={() => setIsEditDriverDialogOpen(false)}>
                    Cancel
                  </Button>
                  <Button onClick={handleUpdateDriver}>Update Driver</Button>
                </div>
              </div>
            </DialogContent>
          </Dialog>
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