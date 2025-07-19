import React from 'react'
import { useParams, Link } from 'react-router-dom'
import { useQuery } from '@tanstack/react-query'
import { ArrowLeft, Package, RefreshCw, Copy, ExternalLink, AlertCircle } from 'lucide-react'
import { shipmentApi, Shipment, ShipmentStatusHistory } from '@/services/shipment'
import { TrackingTimeline } from '@/components/shipment/TrackingTimeline'
import { TrackingMap } from '@/components/tracking/TrackingMap'
import { StatusIndicator } from '@/components/ui/status-indicator'
import { LocationInfoCard } from '@/components/shipment/LocationInfoCard'
import { TruckInfoCard } from '@/components/shipment/TruckInfoCard'
import { ActionsCard } from '@/components/shipment/ActionsCard'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { Alert, AlertDescription } from '@/components/ui/alert'
import { FlashOnUpdate } from '@/components/ui/flash-on-update'
import { formatDateTime, formatRelativeTime } from '@/lib/utils'
import { toast } from 'sonner'
import { useShipmentSubscription } from '@/hooks/useShipmentSubscription'
import { useSocketStatus } from '@/hooks/useSocket'

export const ShipmentDetailPage: React.FC = () => {
  const { trackingNumber } = useParams<{ trackingNumber: string }>()
  const { isConnected } = useSocketStatus()

  // Subscribe to real-time updates for this shipment
  useShipmentSubscription(trackingNumber)

  // Fetch shipment data using TanStack Query
  const { 
    data: shipment, 
    isLoading, 
    error: shipmentError,
    refetch: refetchShipment
  } = useQuery({
    queryKey: ['shipment', trackingNumber],
    queryFn: () => shipmentApi.getShipmentByTrackingNumber(trackingNumber!),
    enabled: !!trackingNumber,
    refetchInterval: isConnected ? false : 30000, // Poll every 30s if WebSocket is down
    onError: (err: Error) => {
      console.error('Failed to fetch shipment details:', err)
      toast.error('Failed to load shipment details')
    }
  })

  // Fetch tracking history using TanStack Query
  const { 
    data: historyData, 
    isLoading: historyLoading,
    refetch: refetchHistory
  } = useQuery({
    queryKey: ['trackingHistory', trackingNumber],
    queryFn: () => shipmentApi.getTrackingHistory(trackingNumber!),
    enabled: !!trackingNumber,
    refetchInterval: isConnected ? false : 30000, // Poll every 30s if WebSocket is down
  })

  const history = historyData?.history || []
  const loading = isLoading || historyLoading
  const error = shipmentError?.message

  const handleRefresh = async () => {
    await Promise.all([refetchShipment(), refetchHistory()])
    toast.success('Data refreshed')
  }

  const copyTrackingNumber = () => {
    if (trackingNumber) {
      navigator.clipboard.writeText(trackingNumber)
      toast.success('Tracking number copied to clipboard')
    }
  }

  if (loading) {
    return (
      <div className="p-6 max-w-6xl mx-auto">
        <div className="animate-pulse">
          <div className="h-8 bg-gray-200 rounded w-1/4 mb-4" />
          <div className="grid gap-6 lg:grid-cols-3">
            <div className="lg:col-span-2 space-y-6">
              <div className="h-64 bg-gray-200 rounded" />
              <div className="h-96 bg-gray-200 rounded" />
            </div>
            <div className="space-y-6">
              <div className="h-48 bg-gray-200 rounded" />
              <div className="h-32 bg-gray-200 rounded" />
            </div>
          </div>
        </div>
      </div>
    )
  }

  if (error || !shipment) {
    return (
      <div className="p-6 max-w-6xl mx-auto">
        <div className="mb-6">
          <Link to="/shipments" className="inline-flex items-center text-blue-600 hover:text-blue-800">
            <ArrowLeft className="h-4 w-4 mr-2" />
            Back to Shipments
          </Link>
        </div>
        
        <Alert variant="destructive">
          <AlertCircle className="h-4 w-4" />
          <AlertDescription>
            {error || 'Shipment not found'}
          </AlertDescription>
        </Alert>
      </div>
    )
  }

  return (
    <div className="p-6 max-w-6xl mx-auto">
      {/* Header */}
      <div className="mb-6">
        <div className="flex items-center justify-between">
          <div className="flex items-center space-x-4">
            <Link to="/shipments" className="inline-flex items-center text-blue-600 hover:text-blue-800">
              <ArrowLeft className="h-4 w-4 mr-2" />
              Back to Shipments
            </Link>
          </div>
          <Button
            variant="outline"
            size="sm"
            onClick={handleRefresh}
            disabled={loading}
          >
            <RefreshCw className={`h-4 w-4 mr-2 ${loading ? 'animate-spin' : ''}`} />
            Refresh
          </Button>
        </div>
      </div>

      {/* Title */}
      <div className="mb-8">
        <div className="flex items-center gap-4 mb-2">
          <h1 className="text-3xl font-bold text-gray-900">Shipment Details</h1>
          <FlashOnUpdate value={shipment.status} className="transition-colors duration-300">
            <StatusIndicator 
              status={shipment.status}
              showDot={true}
              animated={!['DELIVERED', 'CANCELLED', 'RETURNED'].includes(shipment.status)}
            />
          </FlashOnUpdate>
        </div>
        <div className="flex items-center gap-4">
          <div className="flex items-center gap-2">
            <span className="text-lg font-mono text-gray-600">{shipment.tracking_number}</span>
            <Button
              variant="ghost"
              size="sm"
              onClick={copyTrackingNumber}
              className="p-1 h-auto"
            >
              <Copy className="h-4 w-4" />
            </Button>
          </div>
          <Link to={`/tracking?q=${shipment.tracking_number}`} className="text-blue-600 hover:text-blue-800">
            <ExternalLink className="h-4 w-4" />
          </Link>
        </div>
      </div>

      <div className="grid gap-6 lg:grid-cols-3">
        {/* Main Content */}
        <div className="lg:col-span-2 space-y-6">
          {/* Shipment Overview */}
          <Card>
            <CardHeader>
              <CardTitle className="flex items-center gap-2">
                <Package className="h-5 w-5" />
                Shipment Overview
              </CardTitle>
            </CardHeader>
            <CardContent>
              <div className="grid gap-4 md:grid-cols-2">
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-1">Shipment ID</label>
                  <p className="text-sm text-gray-900">{shipment.shipment_id}</p>
                </div>
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-1">Status</label>
                  <p className="text-sm text-gray-900">{shipment.status_display}</p>
                </div>
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-1">Created</label>
                  <p className="text-sm text-gray-900">{formatDateTime(shipment.created_at)}</p>
                </div>
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-1">Estimated Delivery</label>
                  <p className="text-sm text-gray-900">
                    {shipment.estimated_delivery ? formatDateTime(shipment.estimated_delivery) : 'Not available'}
                  </p>
                </div>
                {shipment.actual_delivery && (
                  <div>
                    <label className="block text-sm font-medium text-gray-700 mb-1">Actual Delivery</label>
                    <p className="text-sm text-gray-900">{formatDateTime(shipment.actual_delivery)}</p>
                  </div>
                )}
                {shipment.pickup_time && (
                  <div>
                    <label className="block text-sm font-medium text-gray-700 mb-1">Pickup Time</label>
                    <p className="text-sm text-gray-900">{formatDateTime(shipment.pickup_time)}</p>
                  </div>
                )}
              </div>
            </CardContent>
          </Card>

          {/* Route Map */}
          <TrackingMap 
            origin={shipment.origin}
            destination={shipment.destination}
            currentLocation={shipment.truck?.current_location}
            truckStatus={shipment.truck?.status}
          />

          {/* Tracking Timeline */}
          <Card>
            <CardHeader>
              <CardTitle className="flex items-center gap-2">
                <MapPin className="h-5 w-5" />
                Tracking Timeline
              </CardTitle>
              <CardDescription>
                Follow your shipment's journey from pickup to delivery
              </CardDescription>
            </CardHeader>
            <CardContent>
              <TrackingTimeline 
                history={history}
                currentStatus={shipment.status}
                loading={historyLoading}
              />
            </CardContent>
          </Card>
        </div>

        {/* Sidebar */}
        <div className="space-y-6">
          {/* Location Information */}
          <LocationInfoCard
            origin={shipment.origin}
            destination={shipment.destination}
          />

          {/* Truck Information */}
          {shipment.truck && (
            <TruckInfoCard truck={shipment.truck} />
          )}

          {/* Actions */}
          <ActionsCard trackingNumber={shipment.tracking_number} />
        </div>
      </div>
    </div>
  )
}