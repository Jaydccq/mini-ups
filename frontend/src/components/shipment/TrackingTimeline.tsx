import React from 'react'
import { Check, Clock, Package, Truck, MapPin, AlertCircle } from 'lucide-react'
import { ShipmentStatusHistory } from '@/services/shipment'
import { formatDateTime } from '@/lib/utils'

interface TrackingTimelineProps {
  history: ShipmentStatusHistory[]
  currentStatus: string
  loading?: boolean
}

const getStatusIcon = (status: string, isCompleted: boolean) => {
  const iconClass = `h-5 w-5 ${isCompleted ? 'text-white' : 'text-gray-400'}`
  
  switch (status) {
    case 'PENDING':
      return <Clock className={iconClass} />
    case 'PICKED_UP':
      return <Package className={iconClass} />
    case 'IN_TRANSIT':
      return <Truck className={iconClass} />
    case 'DELIVERED':
      return <Check className={iconClass} />
    case 'CANCELLED':
    case 'RETURNED':
      return <AlertCircle className={iconClass} />
    default:
      return <MapPin className={iconClass} />
  }
}

const getStatusColor = (status: string, isCompleted: boolean) => {
  if (!isCompleted) return 'bg-gray-200 border-gray-300'
  
  switch (status) {
    case 'PENDING':
      return 'bg-yellow-500 border-yellow-600'
    case 'PICKED_UP':
      return 'bg-blue-500 border-blue-600'
    case 'IN_TRANSIT':
      return 'bg-indigo-500 border-indigo-600'
    case 'DELIVERED':
      return 'bg-green-500 border-green-600'
    case 'CANCELLED':
    case 'RETURNED':
      return 'bg-red-500 border-red-600'
    default:
      return 'bg-gray-500 border-gray-600'
  }
}

export function TrackingTimeline({ history, currentStatus, loading }: TrackingTimelineProps) {
  if (loading) {
    return (
      <div className="space-y-4">
        {Array.from({ length: 4 }).map((_, i) => (
          <div key={i} className="flex items-center space-x-4">
            <div className="w-10 h-10 bg-gray-200 rounded-full animate-pulse" />
            <div className="flex-1 space-y-2">
              <div className="h-4 bg-gray-200 rounded w-1/4 animate-pulse" />
              <div className="h-3 bg-gray-200 rounded w-1/2 animate-pulse" />
            </div>
          </div>
        ))}
      </div>
    )
  }

  if (history.length === 0) {
    return (
      <div className="text-center py-8 text-gray-500">
        <Package className="h-12 w-12 mx-auto mb-4 opacity-50" />
        <p>No tracking history available</p>
      </div>
    )
  }

  return (
    <div className="relative">
      {history.map((event, index) => {
        const isCompleted = true // All events in history are completed
        const isLast = index === history.length - 1
        const statusColor = getStatusColor(event.status, isCompleted)
        
        return (
          <div key={`${event.status}-${event.timestamp}`} className="relative flex items-start space-x-4">
            {/* Timeline Line */}
            {!isLast && (
              <div className="absolute left-5 top-10 w-0.5 h-16 bg-gray-200" />
            )}
            
            {/* Status Icon */}
            <div className={`relative flex items-center justify-center w-10 h-10 rounded-full border-2 ${statusColor}`}>
              {getStatusIcon(event.status, isCompleted)}
            </div>
            
            {/* Event Details */}
            <div className="flex-1 min-w-0">
              <div className="flex items-center justify-between">
                <h3 className="text-lg font-semibold text-gray-900">
                  {event.status_display}
                </h3>
                <span className="text-sm text-gray-500">
                  {formatDateTime(event.timestamp)}
                </span>
              </div>
              
              {event.comment && (
                <p className="mt-1 text-sm text-gray-600">
                  {event.comment}
                </p>
              )}
              
              {/* Status Badge */}
              <div className="mt-2">
                <span className="inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium bg-gray-100 text-gray-800">
                  {event.status}
                </span>
              </div>
            </div>
          </div>
        )
      })}
    </div>
  )
}