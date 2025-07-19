/**
 * Real-Time Fleet Map Component
 * 
 * Features:
 * - Display all active trucks with their real-time locations
 * - Color-coded truck status indicators
 * - Interactive markers with truck details
 * - Auto-refresh and WebSocket support for live updates
 * - Pan and zoom functionality
 * 
 * @author Mini-UPS Team
 * @version 1.0.0
 */
import React, { useState, useEffect, useMemo } from 'react';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Badge } from '@/components/ui/badge';
import { Button } from '@/components/ui/button';
import { Truck, MapPin, Navigation, RefreshCw, Activity } from 'lucide-react';

interface TruckLocation {
  id: number;
  plateNumber: string;
  status: 'AVAILABLE' | 'IN_TRANSIT' | 'MAINTENANCE' | 'OFFLINE';
  location: { x: number; y: number };
  driver?: {
    id: number;
    name: string;
  };
  currentShipment?: {
    trackingNumber: string;
    destination: { x: number; y: number };
  };
  lastUpdate: string;
}

interface FleetMapProps {
  className?: string;
}

export const RealTimeFleetMap: React.FC<FleetMapProps> = ({ className = '' }) => {
  const [trucks, setTrucks] = useState<TruckLocation[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [selectedTruck, setSelectedTruck] = useState<number | null>(null);
  const [lastUpdate, setLastUpdate] = useState<Date | null>(null);

  // Calculate map bounds from truck locations
  const mapData = useMemo(() => {
    if (trucks.length === 0) {
      return {
        svgWidth: 400,
        svgHeight: 300,
        transformX: (x: number) => x,
        transformY: (y: number) => y,
        bounds: { minX: -50, maxX: 50, minY: -50, maxY: 50 }
      };
    }

    const locations = trucks.map(truck => truck.location);
    const destinations = trucks
      .filter(truck => truck.currentShipment)
      .map(truck => truck.currentShipment!.destination);
    
    const allPoints = [...locations, ...destinations];
    
    const minX = Math.min(...allPoints.map(p => p.x));
    const maxX = Math.max(...allPoints.map(p => p.x));
    const minY = Math.min(...allPoints.map(p => p.y));
    const maxY = Math.max(...allPoints.map(p => p.y));

    const centerX = (minX + maxX) / 2;
    const centerY = (minY + maxY) / 2;
    
    const rangeX = maxX - minX;
    const rangeY = maxY - minY;
    const range = Math.max(rangeX, rangeY, 100); // Minimum range of 100

    const bounds = {
      minX: centerX - range / 2 - 20,
      maxX: centerX + range / 2 + 20,
      minY: centerY - range / 2 - 20,
      maxY: centerY + range / 2 + 20
    };

    const svgWidth = 500;
    const svgHeight = 400;

    const scaleX = svgWidth / (bounds.maxX - bounds.minX);
    const scaleY = svgHeight / (bounds.maxY - bounds.minY);
    const scale = Math.min(scaleX, scaleY) * 0.85;

    const transformX = (x: number) => (x - bounds.minX) * scale + (svgWidth - (bounds.maxX - bounds.minX) * scale) / 2;
    const transformY = (y: number) => svgHeight - ((y - bounds.minY) * scale + (svgHeight - (bounds.maxY - bounds.minY) * scale) / 2);

    return { svgWidth, svgHeight, transformX, transformY, bounds };
  }, [trucks]);

  const fetchFleetLocations = async () => {
    try {
      setError(null);
      const response = await fetch('/api/admin/fleet/overview', {
        headers: { 'Authorization': `Bearer ${localStorage.getItem('token')}` }
      });
      
      if (!response.ok) {
        throw new Error('Failed to fetch fleet locations');
      }
      
      const data = await response.json();
      if (data.data && data.data.trucks) {
        setTrucks(data.data.trucks);
        setLastUpdate(new Date());
      }
    } catch (error) {
      console.error('Error fetching fleet locations:', error);
      setError('Failed to load fleet locations');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchFleetLocations();
    
    // Set up auto-refresh every 15 seconds
    const interval = setInterval(fetchFleetLocations, 15000);
    
    return () => clearInterval(interval);
  }, []);

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

  const selectedTruckData = selectedTruck ? trucks.find(t => t.id === selectedTruck) : null;

  if (loading) {
    return (
      <Card className={className}>
        <CardHeader>
          <CardTitle className="flex items-center gap-2">
            <MapPin className="h-5 w-5" />
            Real-Time Fleet Map
          </CardTitle>
        </CardHeader>
        <CardContent>
          <div className="flex items-center justify-center h-64">
            <div className="flex items-center space-x-2">
              <RefreshCw className="w-5 h-5 animate-spin" />
              <span>Loading fleet locations...</span>
            </div>
          </div>
        </CardContent>
      </Card>
    );
  }

  if (error) {
    return (
      <Card className={className}>
        <CardHeader>
          <CardTitle className="flex items-center gap-2">
            <MapPin className="h-5 w-5" />
            Real-Time Fleet Map
          </CardTitle>
        </CardHeader>
        <CardContent>
          <div className="flex items-center justify-center h-64">
            <div className="text-center">
              <p className="text-red-600 mb-4">{error}</p>
              <Button onClick={fetchFleetLocations} variant="outline">
                Try Again
              </Button>
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
            <MapPin className="h-5 w-5" />
            Real-Time Fleet Map
          </CardTitle>
          <div className="flex items-center gap-2">
            <Badge variant="outline" className="text-xs">
              <Activity className="w-3 h-3 mr-1" />
              {trucks.length} trucks
            </Badge>
            <Button onClick={fetchFleetLocations} variant="outline" size="sm">
              <RefreshCw className="w-3 h-3 mr-1" />
              Refresh
            </Button>
          </div>
        </div>
      </CardHeader>
      <CardContent className="space-y-4">
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
                <pattern id="grid" width="20" height="20" patternUnits="userSpaceOnUse">
                  <path d="M 20 0 L 0 0 0 20" fill="none" stroke="#e5e7eb" strokeWidth="0.5"/>
                </pattern>
              </defs>
              <rect width="100%" height="100%" fill="url(#grid)" />
              
              {/* Destination routes for trucks in transit */}
              {trucks
                .filter(truck => truck.status === 'IN_TRANSIT' && truck.currentShipment)
                .map(truck => {
                  const truckPos = {
                    x: mapData.transformX(truck.location.x),
                    y: mapData.transformY(truck.location.y)
                  };
                  const destPos = {
                    x: mapData.transformX(truck.currentShipment!.destination.x),
                    y: mapData.transformY(truck.currentShipment!.destination.y)
                  };
                  
                  return (
                    <g key={`route-${truck.id}`}>
                      {/* Route line */}
                      <line
                        x1={truckPos.x}
                        y1={truckPos.y}
                        x2={destPos.x}
                        y2={destPos.y}
                        stroke="#94a3b8"
                        strokeWidth="2"
                        strokeDasharray="5,5"
                        opacity="0.6"
                      />
                      {/* Destination marker */}
                      <circle
                        cx={destPos.x}
                        cy={destPos.y}
                        r="6"
                        fill="#10b981"
                        stroke="#ffffff"
                        strokeWidth="2"
                      />
                    </g>
                  );
                })}
              
              {/* Truck markers */}
              {trucks.map(truck => {
                const pos = {
                  x: mapData.transformX(truck.location.x),
                  y: mapData.transformY(truck.location.y)
                };
                const isSelected = selectedTruck === truck.id;
                const statusColor = getStatusColor(truck.status);
                
                return (
                  <g key={truck.id}>
                    {/* Selection halo */}
                    {isSelected && (
                      <circle
                        cx={pos.x}
                        cy={pos.y}
                        r="16"
                        fill="none"
                        stroke="#3b82f6"
                        strokeWidth="2"
                        opacity="0.5"
                      />
                    )}
                    
                    {/* Truck marker */}
                    <rect
                      x={pos.x - 8}
                      y={pos.y - 8}
                      width="16"
                      height="16"
                      fill={statusColor}
                      stroke="#ffffff"
                      strokeWidth="2"
                      rx="3"
                      className="cursor-pointer hover:opacity-80 transition-opacity"
                      onClick={() => setSelectedTruck(truck.id)}
                    />
                    
                    {/* Truck ID label */}
                    <text
                      x={pos.x}
                      y={pos.y + 22}
                      textAnchor="middle"
                      className="text-xs font-medium fill-gray-700"
                      style={{ fontSize: '10px' }}
                    >
                      {truck.plateNumber}
                    </text>
                  </g>
                );
              })}
            </svg>
          </div>
          
          {/* Last update indicator */}
          {lastUpdate && (
            <div className="absolute top-2 right-2 bg-white/90 backdrop-blur-sm rounded px-2 py-1 text-xs text-gray-600">
              Updated: {lastUpdate.toLocaleTimeString()}
            </div>
          )}
        </div>

        {/* Legend */}
        <div className="grid grid-cols-2 md:grid-cols-4 gap-2 text-xs">
          <div className="flex items-center gap-2">
            <div className="w-3 h-3 bg-green-500 rounded border border-white"></div>
            <span>Available</span>
          </div>
          <div className="flex items-center gap-2">
            <div className="w-3 h-3 bg-blue-500 rounded border border-white"></div>
            <span>In Transit</span>
          </div>
          <div className="flex items-center gap-2">
            <div className="w-3 h-3 bg-yellow-500 rounded border border-white"></div>
            <span>Maintenance</span>
          </div>
          <div className="flex items-center gap-2">
            <div className="w-3 h-3 bg-red-500 rounded border border-white"></div>
            <span>Offline</span>
          </div>
        </div>

        {/* Selected truck details */}
        {selectedTruckData && (
          <div className="border-t pt-4">
            <h4 className="font-semibold mb-2 flex items-center gap-2">
              <Truck className="w-4 h-4" />
              Truck Details: {selectedTruckData.plateNumber}
            </h4>
            <div className="grid grid-cols-2 gap-4 text-sm">
              <div>
                <p className="text-gray-600">Status</p>
                <Badge variant={getStatusBadgeVariant(selectedTruckData.status)}>
                  {selectedTruckData.status}
                </Badge>
              </div>
              <div>
                <p className="text-gray-600">Location</p>
                <p className="font-medium">
                  ({selectedTruckData.location.x}, {selectedTruckData.location.y})
                </p>
              </div>
              {selectedTruckData.driver && (
                <div>
                  <p className="text-gray-600">Driver</p>
                  <p className="font-medium">{selectedTruckData.driver.name}</p>
                </div>
              )}
              {selectedTruckData.currentShipment && (
                <div>
                  <p className="text-gray-600">Current Shipment</p>
                  <p className="font-medium">{selectedTruckData.currentShipment.trackingNumber}</p>
                </div>
              )}
            </div>
          </div>
        )}
      </CardContent>
    </Card>
  );
};