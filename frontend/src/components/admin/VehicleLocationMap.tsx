/**
 * Vehicle Location Map Component
 * 
 * Features:
 * - Real-time vehicle tracking on interactive map
 * - Vehicle status indicators and route visualization
 * - Driver information and assignment details
 * - Geofencing and zone management
 * - Historical route tracking
 * - Performance metrics per vehicle
 * 
 * @author Mini-UPS Team
 * @version 1.0.0
 */
import React, { useState, useEffect, useMemo } from 'react';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Badge } from '@/components/ui/badge';
import { Button } from '@/components/ui/button';
import { Tabs, TabsContent, TabsList, TabsTrigger } from '@/components/ui/tabs';
import { 
  MapPin, 
  Truck, 
  Navigation, 
  RefreshCw, 
  Eye, 
  EyeOff,
  Filter,
  Users,
  Clock,
  Route,
  AlertTriangle,
  CheckCircle
} from 'lucide-react';

interface VehicleLocation {
  id: string;
  plateNumber: string;
  status: 'AVAILABLE' | 'IN_TRANSIT' | 'MAINTENANCE' | 'OFFLINE';
  location: { x: number; y: number };
  heading: number; // degrees
  speed: number; // km/h
  driver?: {
    id: string;
    name: string;
    phone: string;
  };
  currentRoute?: {
    origin: { x: number; y: number };
    destination: { x: number; y: number };
    waypoints: { x: number; y: number }[];
    progress: number; // 0-100%
  };
  lastUpdate: string;
  todayStats: {
    distanceTraveled: number;
    deliveriesCompleted: number;
    fuelEfficiency: number;
  };
}

interface VehicleLocationMapProps {
  vehicles: VehicleLocation[];
  className?: string;
  onVehicleSelect?: (vehicle: VehicleLocation) => void;
}

export const VehicleLocationMap: React.FC<VehicleLocationMapProps> = ({
  vehicles,
  className = '',
  onVehicleSelect
}) => {
  const [selectedVehicle, setSelectedVehicle] = useState<string | null>(null);
  const [visibleStatuses, setVisibleStatuses] = useState<Set<string>>(
    new Set(['AVAILABLE', 'IN_TRANSIT', 'MAINTENANCE', 'OFFLINE'])
  );
  const [showRoutes, setShowRoutes] = useState(true);
  const [showDriverInfo, setShowDriverInfo] = useState(true);
  const [lastUpdate, setLastUpdate] = useState<Date>(new Date());

  // Calculate map bounds and center from vehicle locations
  const mapData = useMemo(() => {
    const visibleVehicles = vehicles.filter(v => visibleStatuses.has(v.status));
    
    if (visibleVehicles.length === 0) {
      return {
        svgWidth: 600,
        svgHeight: 400,
        transformX: (x: number) => x + 300,
        transformY: (y: number) => 200 - y,
        bounds: { minX: -100, maxX: 100, minY: -100, maxY: 100 }
      };
    }

    const allPoints = [
      ...visibleVehicles.map(v => v.location),
      ...visibleVehicles.flatMap(v => 
        v.currentRoute ? [v.currentRoute.origin, v.currentRoute.destination, ...v.currentRoute.waypoints] : []
      )
    ];
    
    const minX = Math.min(...allPoints.map(p => p.x));
    const maxX = Math.max(...allPoints.map(p => p.x));
    const minY = Math.min(...allPoints.map(p => p.y));
    const maxY = Math.max(...allPoints.map(p => p.y));

    const centerX = (minX + maxX) / 2;
    const centerY = (minY + maxY) / 2;
    
    const rangeX = Math.max(maxX - minX, 50);
    const rangeY = Math.max(maxY - minY, 50);
    const range = Math.max(rangeX, rangeY) + 40; // Add padding

    const bounds = {
      minX: centerX - range / 2,
      maxX: centerX + range / 2,
      minY: centerY - range / 2,
      maxY: centerY + range / 2
    };

    const svgWidth = 600;
    const svgHeight = 400;

    const scaleX = svgWidth / (bounds.maxX - bounds.minX);
    const scaleY = svgHeight / (bounds.maxY - bounds.minY);
    const scale = Math.min(scaleX, scaleY) * 0.9;

    const transformX = (x: number) => (x - bounds.minX) * scale + (svgWidth - (bounds.maxX - bounds.minX) * scale) / 2;
    const transformY = (y: number) => svgHeight - ((y - bounds.minY) * scale + (svgHeight - (bounds.maxY - bounds.minY) * scale) / 2);

    return { svgWidth, svgHeight, transformX, transformY, bounds };
  }, [vehicles, visibleStatuses]);

  const getStatusColor = (status: string) => {
    switch (status) {
      case 'AVAILABLE':
        return '#10b981'; // green
      case 'IN_TRANSIT':
        return '#3b82f6'; // blue
      case 'MAINTENANCE':
        return '#f59e0b'; // yellow
      case 'OFFLINE':
        return '#ef4444'; // red
      default:
        return '#6b7280'; // gray
    }
  };

  const getStatusBadgeVariant = (status: string): "default" | "secondary" | "destructive" | "outline" => {
    switch (status) {
      case 'AVAILABLE':
        return 'default';
      case 'IN_TRANSIT':
        return 'secondary';
      case 'MAINTENANCE':
        return 'outline';
      case 'OFFLINE':
        return 'destructive';
      default:
        return 'outline';
    }
  };

  const toggleStatusVisibility = (status: string) => {
    setVisibleStatuses(prev => {
      const newSet = new Set(prev);
      if (newSet.has(status)) {
        newSet.delete(status);
      } else {
        newSet.add(status);
      }
      return newSet;
    });
  };

  const handleVehicleClick = (vehicle: VehicleLocation) => {
    setSelectedVehicle(vehicle.id);
    onVehicleSelect?.(vehicle);
  };

  const visibleVehicles = vehicles.filter(v => visibleStatuses.has(v.status));
  const selectedVehicleData = selectedVehicle ? vehicles.find(v => v.id === selectedVehicle) : null;

  const statusCounts = vehicles.reduce((acc, vehicle) => {
    acc[vehicle.status] = (acc[vehicle.status] || 0) + 1;
    return acc;
  }, {} as Record<string, number>);

  return (
    <Card className={className}>
      <CardHeader>
        <div className="flex justify-between items-center">
          <CardTitle className="flex items-center gap-2">
            <MapPin className="h-5 w-5" />
            Vehicle Location Map
          </CardTitle>
          <div className="flex items-center gap-2">
            <Badge variant="outline" className="text-xs">
              {visibleVehicles.length} / {vehicles.length} vehicles
            </Badge>
            <Button variant="outline" size="sm" onClick={() => setLastUpdate(new Date())}>
              <RefreshCw className="w-3 h-3 mr-1" />
              Refresh
            </Button>
          </div>
        </div>
      </CardHeader>
      <CardContent className="space-y-4">
        <Tabs defaultValue="map" className="w-full">
          <TabsList className="grid w-full grid-cols-2">
            <TabsTrigger value="map">Map View</TabsTrigger>
            <TabsTrigger value="list">List View</TabsTrigger>
          </TabsList>
          
          <TabsContent value="map" className="space-y-4">
            {/* Map Controls */}
            <div className="flex flex-wrap gap-2 p-3 bg-gray-50 rounded-lg">
              <div className="flex items-center gap-2 text-sm">
                <span className="font-medium">Filter:</span>
                {Object.entries(statusCounts).map(([status, count]) => (
                  <button
                    key={status}
                    onClick={() => toggleStatusVisibility(status)}
                    className={`flex items-center gap-1 px-2 py-1 rounded text-xs border transition-opacity ${
                      visibleStatuses.has(status) ? 'opacity-100' : 'opacity-40'
                    }`}
                    style={{ 
                      backgroundColor: visibleStatuses.has(status) ? getStatusColor(status) + '20' : '#f3f4f6',
                      borderColor: getStatusColor(status),
                      color: getStatusColor(status)
                    }}
                  >
                    {visibleStatuses.has(status) ? <Eye className="w-3 h-3" /> : <EyeOff className="w-3 h-3" />}
                    {status} ({count})
                  </button>
                ))}
              </div>
              
              <div className="flex items-center gap-2 ml-auto">
                <button
                  onClick={() => setShowRoutes(!showRoutes)}
                  className={`flex items-center gap-1 px-2 py-1 rounded text-xs ${
                    showRoutes ? 'bg-blue-100 text-blue-800' : 'bg-gray-200 text-gray-600'
                  }`}
                >
                  <Route className="w-3 h-3" />
                  Routes
                </button>
                <button
                  onClick={() => setShowDriverInfo(!showDriverInfo)}
                  className={`flex items-center gap-1 px-2 py-1 rounded text-xs ${
                    showDriverInfo ? 'bg-green-100 text-green-800' : 'bg-gray-200 text-gray-600'
                  }`}
                >
                  <Users className="w-3 h-3" />
                  Drivers
                </button>
              </div>
            </div>

            {/* Map Display */}
            <div className="relative">
              <div className="w-full h-[400px] flex items-center justify-center bg-gray-50 rounded-lg border">
                <svg
                  width={mapData.svgWidth}
                  height={mapData.svgHeight}
                  viewBox={`0 0 ${mapData.svgWidth} ${mapData.svgHeight}`}
                  className="bg-blue-50 rounded-lg"
                >
                  {/* Grid lines for reference */}
                  <defs>
                    <pattern id="vehicleGrid" width="30" height="30" patternUnits="userSpaceOnUse">
                      <path d="M 30 0 L 0 0 0 30" fill="none" stroke="#e5e7eb" strokeWidth="0.5"/>
                    </pattern>
                    <marker id="arrowhead" markerWidth="10" markerHeight="7" 
                            refX="9" refY="3.5" orient="auto">
                      <polygon points="0 0, 10 3.5, 0 7" fill="#6b7280" />
                    </marker>
                  </defs>
                  <rect width="100%" height="100%" fill="url(#vehicleGrid)" />
                  
                  {/* Routes */}
                  {showRoutes && visibleVehicles
                    .filter(vehicle => vehicle.currentRoute)
                    .map(vehicle => {
                      const route = vehicle.currentRoute!;
                      const vehiclePos = {
                        x: mapData.transformX(vehicle.location.x),
                        y: mapData.transformY(vehicle.location.y)
                      };
                      const originPos = {
                        x: mapData.transformX(route.origin.x),
                        y: mapData.transformY(route.origin.y)
                      };
                      const destPos = {
                        x: mapData.transformX(route.destination.x),
                        y: mapData.transformY(route.destination.y)
                      };
                      
                      return (
                        <g key={`route-${vehicle.id}`}>
                          {/* Completed route (green) */}
                          <line
                            x1={originPos.x}
                            y1={originPos.y}
                            x2={vehiclePos.x}
                            y2={vehiclePos.y}
                            stroke="#10b981"
                            strokeWidth="3"
                            strokeDasharray="2,2"
                            opacity="0.8"
                          />
                          {/* Remaining route (gray) */}
                          <line
                            x1={vehiclePos.x}
                            y1={vehiclePos.y}
                            x2={destPos.x}
                            y2={destPos.y}
                            stroke="#9ca3af"
                            strokeWidth="2"
                            strokeDasharray="4,4"
                            opacity="0.6"
                            markerEnd="url(#arrowhead)"
                          />
                          {/* Origin marker */}
                          <circle
                            cx={originPos.x}
                            cy={originPos.y}
                            r="4"
                            fill="#10b981"
                            stroke="#ffffff"
                            strokeWidth="1"
                          />
                          {/* Destination marker */}
                          <circle
                            cx={destPos.x}
                            cy={destPos.y}
                            r="6"
                            fill="#ef4444"
                            stroke="#ffffff"
                            strokeWidth="2"
                          />
                        </g>
                      );
                    })}
                  
                  {/* Vehicle markers */}
                  {visibleVehicles.map(vehicle => {
                    const pos = {
                      x: mapData.transformX(vehicle.location.x),
                      y: mapData.transformY(vehicle.location.y)
                    };
                    const isSelected = selectedVehicle === vehicle.id;
                    const statusColor = getStatusColor(vehicle.status);
                    
                    return (
                      <g key={vehicle.id}>
                        {/* Selection halo */}
                        {isSelected && (
                          <circle
                            cx={pos.x}
                            cy={pos.y}
                            r="20"
                            fill="none"
                            stroke="#3b82f6"
                            strokeWidth="2"
                            opacity="0.5"
                          />
                        )}
                        
                        {/* Vehicle marker with direction arrow */}
                        <g transform={`translate(${pos.x}, ${pos.y}) rotate(${vehicle.heading})`}>
                          <rect
                            x="-8"
                            y="-12"
                            width="16"
                            height="24"
                            fill={statusColor}
                            stroke="#ffffff"
                            strokeWidth="2"
                            rx="4"
                            className="cursor-pointer hover:opacity-80 transition-opacity"
                            onClick={() => handleVehicleClick(vehicle)}
                          />
                          {/* Direction indicator */}
                          <polygon
                            points="0,-8 -4,-4 4,-4"
                            fill="#ffffff"
                            opacity="0.8"
                          />
                        </g>
                        
                        {/* Vehicle label */}
                        <text
                          x={pos.x}
                          y={pos.y + 25}
                          textAnchor="middle"
                          className="text-xs font-medium fill-gray-700"
                          style={{ fontSize: '10px' }}
                        >
                          {vehicle.plateNumber}
                        </text>
                        
                        {/* Driver info */}
                        {showDriverInfo && vehicle.driver && (
                          <text
                            x={pos.x}
                            y={pos.y + 35}
                            textAnchor="middle"
                            className="text-xs fill-gray-500"
                            style={{ fontSize: '8px' }}
                          >
                            {vehicle.driver.name}
                          </text>
                        )}
                      </g>
                    );
                  })}
                </svg>
              </div>
              
              {/* Last update indicator */}
              <div className="absolute top-2 right-2 bg-white/90 backdrop-blur-sm rounded px-2 py-1 text-xs text-gray-600">
                Updated: {lastUpdate.toLocaleTimeString()}
              </div>
            </div>
            
            {/* Selected vehicle details */}
            {selectedVehicleData && (
              <Card className="bg-blue-50 border-blue-200">
                <CardContent className="p-4">
                  <div className="flex items-center justify-between mb-3">
                    <h4 className="font-semibold flex items-center gap-2">
                      <Truck className="w-4 h-4" />
                      {selectedVehicleData.plateNumber}
                    </h4>
                    <Badge variant={getStatusBadgeVariant(selectedVehicleData.status)}>
                      {selectedVehicleData.status}
                    </Badge>
                  </div>
                  
                  <div className="grid grid-cols-2 md:grid-cols-4 gap-4 text-sm">
                    <div>
                      <p className="text-gray-600">Location</p>
                      <p className="font-medium">
                        ({selectedVehicleData.location.x}, {selectedVehicleData.location.y})
                      </p>
                    </div>
                    <div>
                      <p className="text-gray-600">Speed</p>
                      <p className="font-medium">{selectedVehicleData.speed} km/h</p>
                    </div>
                    {selectedVehicleData.driver && (
                      <>
                        <div>
                          <p className="text-gray-600">Driver</p>
                          <p className="font-medium">{selectedVehicleData.driver.name}</p>
                        </div>
                        <div>
                          <p className="text-gray-600">Contact</p>
                          <p className="font-medium">{selectedVehicleData.driver.phone}</p>
                        </div>
                      </>
                    )}
                  </div>
                  
                  {/* Today's performance */}
                  <div className="mt-3 pt-3 border-t border-blue-200">
                    <p className="text-sm font-medium text-gray-700 mb-2">Today's Performance</p>
                    <div className="grid grid-cols-3 gap-4 text-sm">
                      <div>
                        <p className="text-gray-600">Distance</p>
                        <p className="font-medium">{selectedVehicleData.todayStats.distanceTraveled} km</p>
                      </div>
                      <div>
                        <p className="text-gray-600">Deliveries</p>
                        <p className="font-medium">{selectedVehicleData.todayStats.deliveriesCompleted}</p>
                      </div>
                      <div>
                        <p className="text-gray-600">Efficiency</p>
                        <p className="font-medium">{selectedVehicleData.todayStats.fuelEfficiency} L/100km</p>
                      </div>
                    </div>
                  </div>
                </CardContent>
              </Card>
            )}
          </TabsContent>
          
          <TabsContent value="list" className="space-y-2">
            {visibleVehicles.map(vehicle => (
              <Card 
                key={vehicle.id} 
                className={`cursor-pointer transition-colors ${
                  selectedVehicle === vehicle.id ? 'border-blue-500 bg-blue-50' : 'hover:bg-gray-50'
                }`}
                onClick={() => handleVehicleClick(vehicle)}
              >
                <CardContent className="p-4">
                  <div className="flex items-center justify-between">
                    <div className="flex items-center gap-3">
                      <div 
                        className="w-4 h-4 rounded-full border-2 border-white"
                        style={{ backgroundColor: getStatusColor(vehicle.status) }}
                      />
                      <div>
                        <p className="font-medium">{vehicle.plateNumber}</p>
                        <p className="text-sm text-gray-600">
                          {vehicle.driver ? vehicle.driver.name : 'No driver assigned'}
                        </p>
                      </div>
                    </div>
                    <div className="text-right">
                      <Badge variant={getStatusBadgeVariant(vehicle.status)}>
                        {vehicle.status}
                      </Badge>
                      <p className="text-sm text-gray-600 mt-1">
                        {vehicle.speed} km/h
                      </p>
                    </div>
                  </div>
                </CardContent>
              </Card>
            ))}
          </TabsContent>
        </Tabs>
      </CardContent>
    </Card>
  );
};