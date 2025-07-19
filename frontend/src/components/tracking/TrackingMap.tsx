import React, { useMemo } from 'react';
import { MapPin, Truck, Package } from 'lucide-react';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Badge } from '@/components/ui/badge';
import { calculateDistance } from '@/lib/utils';

interface TrackingMapProps {
  origin: { x: number; y: number };
  destination: { x: number; y: number };
  currentLocation?: { x: number; y: number };
  truckStatus?: string;
  className?: string;
}

export const TrackingMap: React.FC<TrackingMapProps> = ({
  origin,
  destination,
  currentLocation,
  truckStatus,
  className = ''
}) => {
  // Calculate map bounds and center
  const mapData = useMemo(() => {
    const points = [origin, destination];
    if (currentLocation) {
      points.push(currentLocation);
    }

    const minX = Math.min(...points.map(p => p.x));
    const maxX = Math.max(...points.map(p => p.x));
    const minY = Math.min(...points.map(p => p.y));
    const maxY = Math.max(...points.map(p => p.y));

    const centerX = (minX + maxX) / 2;
    const centerY = (minY + maxY) / 2;
    
    const rangeX = maxX - minX;
    const rangeY = maxY - minY;
    const range = Math.max(rangeX, rangeY, 50); // Minimum range of 50

    const bounds = {
      minX: centerX - range / 2 - 10,
      maxX: centerX + range / 2 + 10,
      minY: centerY - range / 2 - 10,
      maxY: centerY + range / 2 + 10
    };

    const svgWidth = 400;
    const svgHeight = 300;

    // Transform coordinates from map space to SVG space
    const scaleX = svgWidth / (bounds.maxX - bounds.minX);
    const scaleY = svgHeight / (bounds.maxY - bounds.minY);
    const scale = Math.min(scaleX, scaleY) * 0.8; // 80% to leave margin

    const transformX = (x: number) => (x - bounds.minX) * scale + (svgWidth - (bounds.maxX - bounds.minX) * scale) / 2;
    const transformY = (y: number) => svgHeight - ((y - bounds.minY) * scale + (svgHeight - (bounds.maxY - bounds.minY) * scale) / 2);

    return {
      svgWidth,
      svgHeight,
      transformX,
      transformY,
      bounds
    };
  }, [origin, destination, currentLocation]);

  // Calculate progress percentage
  const progress = useMemo(() => {
    if (!currentLocation) return 0;
    
    const totalDistance = calculateDistance(origin, destination);
    const traveledDistance = calculateDistance(origin, currentLocation);
    
    return Math.min(Math.round((traveledDistance / totalDistance) * 100), 100);
  }, [origin, destination, currentLocation]);

  const totalDistance = useMemo(() => calculateDistance(origin, destination), [origin, destination]);

  // Transform coordinates using the computed transformation functions
  const transformedOrigin = useMemo(() => ({
    x: mapData.transformX(origin.x),
    y: mapData.transformY(origin.y)
  }), [origin, mapData]);

  const transformedDestination = useMemo(() => ({
    x: mapData.transformX(destination.x),
    y: mapData.transformY(destination.y)
  }), [destination, mapData]);

  const transformedCurrentLocation = useMemo(() => {
    if (!currentLocation) return null;
    return {
      x: mapData.transformX(currentLocation.x),
      y: mapData.transformY(currentLocation.y)
    };
  }, [currentLocation, mapData]);

  return (
    <Card className={className}>
      <CardHeader>
        <CardTitle className="flex items-center gap-2">
          <MapPin className="h-5 w-5" />
          Shipment Route
        </CardTitle>
      </CardHeader>
      <CardContent className="space-y-4">
        {/* Map container with declarative SVG */}
        <div className="w-full h-[300px] flex items-center justify-center bg-gray-50 rounded-lg">
          <svg
            width={mapData.svgWidth}
            height={mapData.svgHeight}
            viewBox={`0 0 ${mapData.svgWidth} ${mapData.svgHeight}`}
            className="border rounded-lg bg-blue-50"
          >
            {/* Route lines */}
            {transformedCurrentLocation ? (
              <>
                {/* Completed route (green) */}
                <line
                  x1={transformedOrigin.x}
                  y1={transformedOrigin.y}
                  x2={transformedCurrentLocation.x}
                  y2={transformedCurrentLocation.y}
                  stroke="#10b981"
                  strokeWidth="3"
                  strokeDasharray="5,5"
                />
                {/* Remaining route (gray) */}
                <line
                  x1={transformedCurrentLocation.x}
                  y1={transformedCurrentLocation.y}
                  x2={transformedDestination.x}
                  y2={transformedDestination.y}
                  stroke="#9ca3af"
                  strokeWidth="2"
                  strokeDasharray="3,3"
                />
              </>
            ) : (
              /* Planned route (blue) */
              <line
                x1={transformedOrigin.x}
                y1={transformedOrigin.y}
                x2={transformedDestination.x}
                y2={transformedDestination.y}
                stroke="#3b82f6"
                strokeWidth="2"
                strokeDasharray="5,5"
              />
            )}

            {/* Origin marker */}
            <circle
              cx={transformedOrigin.x}
              cy={transformedOrigin.y}
              r="8"
              fill="#ef4444"
              stroke="#ffffff"
              strokeWidth="2"
            />

            {/* Destination marker */}
            <circle
              cx={transformedDestination.x}
              cy={transformedDestination.y}
              r="8"
              fill="#10b981"
              stroke="#ffffff"
              strokeWidth="2"
            />

            {/* Current location marker (truck) */}
            {transformedCurrentLocation && (
              <rect
                x={transformedCurrentLocation.x - 6}
                y={transformedCurrentLocation.y - 6}
                width="12"
                height="12"
                fill="#f59e0b"
                stroke="#ffffff"
                strokeWidth="2"
                rx="2"
              />
            )}
          </svg>
        </div>

        {/* Legend */}
        <div className="grid grid-cols-2 md:grid-cols-3 gap-4 text-sm">
          <div className="flex items-center gap-2">
            <div className="w-4 h-4 bg-red-500 rounded-full border-2 border-white"></div>
            <span>Origin</span>
          </div>
          <div className="flex items-center gap-2">
            <div className="w-4 h-4 bg-green-500 rounded-full border-2 border-white"></div>
            <span>Destination</span>
          </div>
          {currentLocation && (
            <div className="flex items-center gap-2">
              <div className="w-4 h-4 bg-yellow-500 rounded border-2 border-white"></div>
              <span>Current Location</span>
            </div>
          )}
        </div>

        {/* Route Information */}
        <div className="space-y-3 pt-4 border-t">
          <div className="grid grid-cols-2 gap-4 text-sm">
            <div>
              <p className="text-gray-600">Origin</p>
              <p className="font-medium">({origin.x}, {origin.y})</p>
            </div>
            <div>
              <p className="text-gray-600">Destination</p>
              <p className="font-medium">({destination.x}, {destination.y})</p>
            </div>
          </div>

          {currentLocation && (
            <div className="grid grid-cols-2 gap-4 text-sm">
              <div>
                <p className="text-gray-600">Current Position</p>
                <p className="font-medium">({currentLocation.x}, {currentLocation.y})</p>
              </div>
              <div>
                <p className="text-gray-600">Progress</p>
                <div className="flex items-center gap-2">
                  <div className="flex-1 bg-gray-200 rounded-full h-2">
                    <div 
                      className="bg-blue-600 h-2 rounded-full transition-all duration-300"
                      style={{ width: `${progress}%` }}
                    ></div>
                  </div>
                  <span className="font-medium">{progress}%</span>
                </div>
              </div>
            </div>
          )}

          <div className="flex items-center justify-between text-sm">
            <div>
              <span className="text-gray-600">Total Distance: </span>
              <span className="font-medium">{totalDistance.toFixed(1)} units</span>
            </div>
            {truckStatus && (
              <Badge variant="outline" className="flex items-center gap-1">
                <Truck className="h-3 w-3" />
                {truckStatus}
              </Badge>
            )}
          </div>
        </div>
      </CardContent>
    </Card>
  );
};