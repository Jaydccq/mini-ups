import React, { useState, useEffect } from 'react'
import { useSearchParams } from 'react-router-dom'
import { useQuery } from '@tanstack/react-query'
import { Package, Search, MapPin, Calendar, Truck, AlertCircle, CheckCircle, Wifi, WifiOff } from 'lucide-react'
import { shipmentApi, Shipment, ShipmentStatusHistory, TrackingHistoryResponse } from '@/services/shipment'
import { TrackingTimeline } from '@/components/shipment/TrackingTimeline'
import { TrackingMap } from '@/components/tracking/TrackingMap'
import { StatusIndicator } from '@/components/ui/status-indicator'
import { FlashOnUpdate } from '@/components/ui/flash-on-update'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { Alert, AlertDescription } from '@/components/ui/alert'
import { Badge } from '@/components/ui/badge'
import { formatDateTime, formatRelativeTime } from '@/lib/utils'
import { toast } from 'sonner'
import { useSocketStatus } from '@/hooks/useSocket'
import { useShipmentSubscription } from '@/hooks/useShipmentSubscription'

export const TrackingPage: React.FC = () => {
  const [searchParams, setSearchParams] = useSearchParams()
  const [trackingNumber, setTrackingNumber] = useState(searchParams.get('q') || '')
  const [searchedTrackingNumber, setSearchedTrackingNumber] = useState<string | null>(searchParams.get('q'))
  const [validationError, setValidationError] = useState<string | null>(null)
  const { isConnected, hasError: socketError } = useSocketStatus()

  // Subscribe to real-time updates for the searched shipment
  useShipmentSubscription(searchedTrackingNumber || undefined)

  // Fetch shipment data with WebSocket fallback strategy
  const { 
    data: shipment, 
    isLoading: shipmentLoading, 
    error: shipmentError,
    refetch: refetchShipment
  } = useQuery<Shipment>({
    queryKey: ['shipment', searchedTrackingNumber],
    queryFn: () => shipmentApi.getShipmentByTrackingNumber(searchedTrackingNumber!),
    enabled: !!searchedTrackingNumber,
    refetchInterval: isConnected ? false : 30000, // Poll every 30s if WebSocket is down
    retry: 2,
  })

  // Fetch tracking history with WebSocket fallback strategy
  const { 
    data: historyData, 
    isLoading: historyLoading,
    refetch: refetchHistory
  } = useQuery<TrackingHistoryResponse>({
    queryKey: ['trackingHistory', searchedTrackingNumber],
    queryFn: () => shipmentApi.getTrackingHistory(searchedTrackingNumber!),
    enabled: !!searchedTrackingNumber,
    refetchInterval: isConnected ? false : 30000, // Poll every 30s if WebSocket is down
    retry: 2,
  })

  const history = historyData?.history || []
  const loading = shipmentLoading || historyLoading
  const error = validationError || shipmentError?.message
  const hasSearched = !!searchedTrackingNumber

  const validateAndSearch = async (trackingNum: string) => {
    if (!trackingNum.trim()) {
      setValidationError('Please enter a tracking number')
      return
    }

    try {
      setValidationError(null)
      
      // Validate tracking number format first
      const validation = await shipmentApi.validateTrackingNumber(trackingNum.trim())
      
      if (!validation.valid) {
        setValidationError('Invalid tracking number format')
        setSearchedTrackingNumber(null)
        return
      }

      // Set the tracking number to trigger queries
      setSearchedTrackingNumber(trackingNum.trim())
      
      // Update URL with tracking number
      setSearchParams({ q: trackingNum.trim() })
      
    } catch (err: unknown) {
      console.error('Failed to validate tracking number:', err)
      setValidationError('Failed to validate tracking number')
      setSearchedTrackingNumber(null)
    }
  }

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault()
    validateAndSearch(trackingNumber)
  }

  const handleInputChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    setTrackingNumber(e.target.value)
    setValidationError(null)
  }

  // Auto-search if tracking number is provided in URL
  useEffect(() => {
    const urlTrackingNumber = searchParams.get('q')
    if (urlTrackingNumber && !searchedTrackingNumber) {
      validateAndSearch(urlTrackingNumber)
    }
  }, [searchParams, searchedTrackingNumber])

  return (
    <div className="min-h-screen bg-gray-50">
      {/* Hero Section */}
      <div className="bg-gradient-to-br from-blue-600 to-indigo-700 text-white">
        <div className="max-w-4xl mx-auto px-6 py-16">
          <div className="text-center mb-8">
            <Package className="h-16 w-16 mx-auto mb-4" />
            <h1 className="text-4xl font-bold mb-4">Track Your Package</h1>
            <p className="text-xl text-blue-100">
              Enter your tracking number to get real-time updates on your shipment
            </p>
          </div>

          {/* Search Form */}
          <div className="max-w-2xl mx-auto">
            <form onSubmit={handleSubmit} className="flex gap-4">
              <div className="flex-1">
                <Input
                  type="text"
                  placeholder="Enter tracking number (e.g., UPS123456789)"
                  value={trackingNumber}
                  onChange={handleInputChange}
                  className="text-lg py-3 bg-white text-gray-900 border-none focus:ring-2 focus:ring-blue-300"
                />
              </div>
              <Button
                type="submit"
                size="lg"
                disabled={loading}
                className="px-8 bg-white text-blue-600 hover:bg-gray-100 border-none"
              >
                {loading ? (
                  <div className="animate-spin rounded-full h-5 w-5 border-b-2 border-blue-600" />
                ) : (
                  <>
                    <Search className="h-5 w-5 mr-2" />
                    Track
                  </>
                )}
              </Button>
            </form>

            {/* Connection Status Indicator */}
            {hasSearched && (
              <div className="flex justify-center mt-4">
                <div className={`flex items-center gap-2 px-3 py-1 rounded-full text-sm ${
                  isConnected 
                    ? 'bg-green-100 text-green-800' 
                    : socketError 
                    ? 'bg-red-100 text-red-800'
                    : 'bg-yellow-100 text-yellow-800'
                }`}>
                  {isConnected ? (
                    <>
                      <Wifi className="h-3 w-3" />
                      Real-time updates active
                    </>
                  ) : socketError ? (
                    <>
                      <WifiOff className="h-3 w-3" />
                      Connection failed - Using periodic updates
                    </>
                  ) : (
                    <>
                      <WifiOff className="h-3 w-3" />
                      Real-time unavailable - Using periodic updates
                    </>
                  )}
                </div>
              </div>
            )}
          </div>
        </div>
      </div>

      {/* Results Section */}
      <div className="max-w-6xl mx-auto px-6 py-8">
        {/* Error State */}
        {error && (
          <Alert variant="destructive" className="mb-6">
            <AlertCircle className="h-4 w-4" />
            <AlertDescription>{error}</AlertDescription>
          </Alert>
        )}

        {/* Loading State */}
        {loading && (
          <div className="grid gap-6 lg:grid-cols-3">
            <div className="lg:col-span-2 space-y-6">
              <Card className="animate-pulse">
                <CardContent className="p-6">
                  <div className="h-6 bg-gray-200 rounded w-1/4 mb-4" />
                  <div className="space-y-3">
                    <div className="h-4 bg-gray-200 rounded w-3/4" />
                    <div className="h-4 bg-gray-200 rounded w-1/2" />
                  </div>
                </CardContent>
              </Card>
              <Card className="animate-pulse">
                <CardContent className="p-6">
                  <div className="h-96 bg-gray-200 rounded" />
                </CardContent>
              </Card>
            </div>
            <div className="space-y-6">
              <Card className="animate-pulse">
                <CardContent className="p-6">
                  <div className="h-48 bg-gray-200 rounded" />
                </CardContent>
              </Card>
            </div>
          </div>
        )}

        {/* Success State */}
        {!loading && shipment && (
          <div className="grid gap-6 lg:grid-cols-3">
            {/* Main Content */}
            <div className="lg:col-span-2 space-y-6">
              {/* Shipment Status */}
              <Card>
                <CardHeader>
                  <div className="flex items-center justify-between">
                    <CardTitle className="text-2xl">Package Status</CardTitle>
                    <FlashOnUpdate trigger={shipment.status} className="transition-colors duration-300">
                      <StatusIndicator 
                        status={shipment.status}
                        showDot={true}
                        showDescription={true}
                        animated={!['DELIVERED', 'CANCELLED', 'RETURNED'].includes(shipment.status)}
                      />
                    </FlashOnUpdate>
                  </div>
                </CardHeader>
                <CardContent>
                  <div className="grid gap-4 md:grid-cols-2">
                    <div>
                      <label className="block text-sm font-medium text-gray-700 mb-1">Tracking Number</label>
                      <p className="text-lg font-mono text-gray-900">{shipment.tracking_number}</p>
                    </div>
                    <div>
                      <label className="block text-sm font-medium text-gray-700 mb-1">Current Status</label>
                      <p className="text-lg text-gray-900">{shipment.status_display}</p>
                    </div>
                    <div>
                      <label className="block text-sm font-medium text-gray-700 mb-1">Shipped Date</label>
                      <p className="text-sm text-gray-900">{formatDateTime(shipment.created_at)}</p>
                    </div>
                    <div>
                      <label className="block text-sm font-medium text-gray-700 mb-1">Estimated Delivery</label>
                      <p className="text-sm text-gray-900">
                        {shipment.estimated_delivery ? formatDateTime(shipment.estimated_delivery) : 'Calculating...'}
                      </p>
                    </div>
                    {shipment.actual_delivery && (
                      <div className="md:col-span-2">
                        <label className="block text-sm font-medium text-gray-700 mb-1">Delivered</label>
                        <div className="flex items-center gap-2">
                          <CheckCircle className="h-5 w-5 text-green-600" />
                          <p className="text-lg text-green-600 font-semibold">
                            {formatDateTime(shipment.actual_delivery)}
                          </p>
                        </div>
                      </div>
                    )}
                  </div>
                </CardContent>
              </Card>

              {/* Route Map */}
              <TrackingMap 
                origin={shipment.origin}
                destination={shipment.destination}
                currentLocation={shipment.truck ? { x: shipment.origin.x + 20, y: shipment.origin.y + 15 } : undefined}
                truckStatus={shipment.truck?.status}
              />

              {/* Tracking Timeline */}
              <Card>
                <CardHeader>
                  <CardTitle className="flex items-center gap-2">
                    <MapPin className="h-5 w-5" />
                    Tracking History
                  </CardTitle>
                  <CardDescription>
                    Follow your package's journey from pickup to delivery
                  </CardDescription>
                </CardHeader>
                <CardContent>
                  <TrackingTimeline 
                    history={history}
                    currentStatus={shipment.status}
                    loading={false}
                  />
                </CardContent>
              </Card>
            </div>

            {/* Sidebar */}
            <div className="space-y-6">
              {/* Delivery Information */}
              <Card>
                <CardHeader>
                  <CardTitle className="flex items-center gap-2">
                    <MapPin className="h-5 w-5" />
                    Delivery Details
                  </CardTitle>
                </CardHeader>
                <CardContent>
                  <div className="space-y-4">
                    <div>
                      <label className="block text-sm font-medium text-gray-700 mb-1">Origin</label>
                      <p className="text-sm text-gray-900">
                        ({shipment.origin.x}, {shipment.origin.y})
                      </p>
                    </div>
                    <div>
                      <label className="block text-sm font-medium text-gray-700 mb-1">Destination</label>
                      <p className="text-sm text-gray-900">
                        ({shipment.destination.x}, {shipment.destination.y})
                      </p>
                    </div>
                    <div className="pt-4 border-t">
                      <div className="flex items-center justify-between text-sm">
                        <span className="text-gray-600">Distance</span>
                        <span className="font-medium">
                          {Math.sqrt(
                            Math.pow(shipment.destination.x - shipment.origin.x, 2) +
                            Math.pow(shipment.destination.y - shipment.origin.y, 2)
                          ).toFixed(1)} units
                        </span>
                      </div>
                    </div>
                  </div>
                </CardContent>
              </Card>

              {/* Service Information */}
              <Card>
                <CardHeader>
                  <CardTitle>Service Information</CardTitle>
                </CardHeader>
                <CardContent>
                  <div className="space-y-3">
                    <div className="flex justify-between text-sm">
                      <span className="text-gray-600">Service Type</span>
                      <span className="font-medium">Standard Delivery</span>
                    </div>
                    <div className="flex justify-between text-sm">
                      <span className="text-gray-600">Shipment ID</span>
                      <span className="font-medium">{shipment.shipment_id}</span>
                    </div>
                    {shipment.truck && (
                      <div className="flex justify-between text-sm">
                        <span className="text-gray-600">Truck</span>
                        <Badge variant="outline">#{shipment.truck.truck_id}</Badge>
                      </div>
                    )}
                  </div>
                </CardContent>
              </Card>

              {/* Need Help */}
              <Card>
                <CardHeader>
                  <CardTitle>Need Help?</CardTitle>
                </CardHeader>
                <CardContent>
                  <div className="space-y-3 text-sm">
                    <p className="text-gray-600">
                      If you have questions about your shipment, please contact our customer service.
                    </p>
                    <div className="space-y-2">
                      <div className="flex justify-between">
                        <span className="text-gray-600">Phone</span>
                        <span className="font-medium">1-800-MINI-UPS</span>
                      </div>
                      <div className="flex justify-between">
                        <span className="text-gray-600">Email</span>
                        <span className="font-medium">support@mini-ups.com</span>
                      </div>
                    </div>
                  </div>
                </CardContent>
              </Card>
            </div>
          </div>
        )}

        {/* Empty State */}
        {!loading && !shipment && hasSearched && !error && (
          <Card>
            <CardContent className="text-center py-12">
              <Package className="h-12 w-12 mx-auto mb-4 text-gray-400" />
              <h3 className="text-lg font-medium text-gray-900 mb-2">No results found</h3>
              <p className="text-gray-600 mb-4">
                We couldn't find a package with that tracking number. Please check the number and try again.
              </p>
            </CardContent>
          </Card>
        )}

        {/* Instructions */}
        {!hasSearched && (
          <Card>
            <CardHeader>
              <CardTitle>How to Track Your Package</CardTitle>
            </CardHeader>
            <CardContent>
              <div className="grid gap-6 md:grid-cols-3">
                <div className="text-center">
                  <div className="w-12 h-12 bg-blue-100 rounded-full flex items-center justify-center mx-auto mb-3">
                    <Search className="h-6 w-6 text-blue-600" />
                  </div>
                  <h3 className="font-semibold mb-2">Enter Tracking Number</h3>
                  <p className="text-sm text-gray-600">
                    Type or paste your tracking number in the search box above
                  </p>
                </div>
                <div className="text-center">
                  <div className="w-12 h-12 bg-blue-100 rounded-full flex items-center justify-center mx-auto mb-3">
                    <Package className="h-6 w-6 text-blue-600" />
                  </div>
                  <h3 className="font-semibold mb-2">Get Real-Time Updates</h3>
                  <p className="text-sm text-gray-600">
                    See your package's current status and location
                  </p>
                </div>
                <div className="text-center">
                  <div className="w-12 h-12 bg-blue-100 rounded-full flex items-center justify-center mx-auto mb-3">
                    <MapPin className="h-6 w-6 text-blue-600" />
                  </div>
                  <h3 className="font-semibold mb-2">Track Journey</h3>
                  <p className="text-sm text-gray-600">
                    Follow your package's complete journey from pickup to delivery
                  </p>
                </div>
              </div>
            </CardContent>
          </Card>
        )}
      </div>
    </div>
  )
}