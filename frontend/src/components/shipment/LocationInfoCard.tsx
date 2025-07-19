import React from 'react'
import { MapPin } from 'lucide-react'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { calculateDistance } from '@/lib/utils'

interface LocationInfoCardProps {
  origin: { x: number; y: number }
  destination: { x: number; y: number }
}

export const LocationInfoCard: React.FC<LocationInfoCardProps> = ({
  origin,
  destination
}) => {
  const distance = calculateDistance(origin, destination)

  return (
    <Card>
      <CardHeader>
        <CardTitle className="flex items-center gap-2">
          <MapPin className="h-5 w-5" />
          Location Information
        </CardTitle>
      </CardHeader>
      <CardContent>
        <div className="space-y-4">
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">Origin</label>
            <p className="text-sm text-gray-900">
              ({origin.x}, {origin.y})
            </p>
          </div>
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">Destination</label>
            <p className="text-sm text-gray-900">
              ({destination.x}, {destination.y})
            </p>
          </div>
          <div className="pt-4 border-t">
            <div className="flex items-center justify-between text-sm">
              <span className="text-gray-600">Distance</span>
              <span className="font-medium">
                {distance.toFixed(1)} units
              </span>
            </div>
          </div>
        </div>
      </CardContent>
    </Card>
  )
}