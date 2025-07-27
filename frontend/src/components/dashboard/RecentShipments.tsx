import React from 'react'
import { Link } from 'react-router-dom'
import { Package, Calendar, MapPin } from 'lucide-react'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { StatusIndicator } from '@/components/ui/status-indicator'
import { Shipment } from '@/services/shipment'
import { formatDateTime, formatRelativeTime } from '@/lib/utils'

interface RecentShipmentsProps {
  shipments: Shipment[]
  loading?: boolean
}

export function RecentShipments({ shipments, loading }: RecentShipmentsProps) {
  if (loading) {
    return (
      <Card>
        <CardHeader>
          <CardTitle>Recent Shipments</CardTitle>
          <CardDescription>Your latest shipment activities</CardDescription>
        </CardHeader>
        <CardContent>
          <div className="space-y-4">
            {Array.from({ length: 3 }).map((_, i) => (
              <div key={i} className="flex items-center space-x-4">
                <div className="w-10 h-10 bg-gray-200 rounded-full animate-pulse" />
                <div className="flex-1 space-y-2">
                  <div className="h-4 bg-gray-200 rounded animate-pulse" />
                  <div className="h-3 bg-gray-200 rounded w-2/3 animate-pulse" />
                </div>
              </div>
            ))}
          </div>
        </CardContent>
      </Card>
    )
  }

  return (
    <Card>
      <CardHeader>
        <CardTitle className="flex items-center gap-2">
          <Package className="h-5 w-5" />
          Recent Shipments
        </CardTitle>
        <CardDescription>Your latest shipment activities</CardDescription>
      </CardHeader>
      <CardContent>
        {shipments.length === 0 ? (
          <div className="text-center py-8 text-muted-foreground">
            <Package className="h-12 w-12 mx-auto mb-4 opacity-50" />
            <p>No shipments yet</p>
            <p className="text-sm">Create your first shipment to get started</p>
          </div>
        ) : (
          <div className="space-y-4">
            {shipments.map((shipment) => (
              <div key={shipment.shipment_id} className="flex items-center space-x-4 p-4 border rounded-lg hover:bg-gray-50 transition-colors">
                <div className="flex-shrink-0">
                  <div className="w-10 h-10 bg-blue-100 rounded-full flex items-center justify-center">
                    <Package className="h-5 w-5 text-blue-600" />
                  </div>
                </div>
                
                <div className="flex-1 min-w-0">
                  <div className="flex items-center gap-2 mb-1">
                    <Link 
                      to={`/shipments/tracking/${shipment.tracking_number}`}
                      className="font-medium text-blue-600 hover:text-blue-800 transition-colors"
                    >
                      {shipment.tracking_number}
                    </Link>
                    <StatusIndicator 
                      status={shipment.status} 
                      showDot={true}
                      animated={!['DELIVERED', 'CANCELLED', 'RETURNED'].includes(shipment.status)}
                    />
                  </div>
                  
                  <div className="flex items-center gap-4 text-sm text-gray-600">
                    <div className="flex items-center gap-1">
                      <Calendar className="h-3 w-3" />
                      <span>{formatRelativeTime(shipment.created_at)}</span>
                    </div>
                    <div className="flex items-center gap-1">
                      <MapPin className="h-3 w-3" />
                      <span>
                        ({shipment.origin.x}, {shipment.origin.y}) → ({shipment.destination.x}, {shipment.destination.y})
                      </span>
                    </div>
                  </div>
                  
                  {shipment.estimated_delivery && (
                    <div className="text-xs text-gray-500 mt-1">
                      Est. delivery: {formatDateTime(shipment.estimated_delivery)}
                    </div>
                  )}
                </div>
              </div>
            ))}
          </div>
        )}
        
        {shipments.length > 0 && (
          <div className="mt-6 text-center">
            <Link 
              to="/shipments"
              className="text-sm text-blue-600 hover:text-blue-800 font-medium"
            >
              View all shipments →
            </Link>
          </div>
        )}
      </CardContent>
    </Card>
  )
}