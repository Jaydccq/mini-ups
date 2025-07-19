import React from 'react'
import { Truck } from 'lucide-react'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { Badge } from '@/components/ui/badge'
import { TruckInfo } from '@/services/shipment'

interface TruckInfoCardProps {
  truck: TruckInfo
}

export const TruckInfoCard: React.FC<TruckInfoCardProps> = ({ truck }) => {
  return (
    <Card>
      <CardHeader>
        <CardTitle className="flex items-center gap-2">
          <Truck className="h-5 w-5" />
          Truck Information
        </CardTitle>
      </CardHeader>
      <CardContent>
        <div className="space-y-2">
          <div className="flex justify-between text-sm">
            <span className="text-gray-600">Truck ID</span>
            <span className="font-medium">{truck.truck_id}</span>
          </div>
          <div className="flex justify-between text-sm">
            <span className="text-gray-600">Status</span>
            <Badge variant="outline">{truck.status}</Badge>
          </div>
          {truck.current_location && (
            <div className="flex justify-between text-sm">
              <span className="text-gray-600">Current Location</span>
              <span className="font-medium">
                ({truck.current_location.x}, {truck.current_location.y})
              </span>
            </div>
          )}
        </div>
      </CardContent>
    </Card>
  )
}