import { setupServer } from 'msw/node'
import { http, HttpResponse } from 'msw'

// Mock API handlers for testing
export const handlers = [
  // Authentication endpoints
  http.post('/api/auth/login', () => {
    return HttpResponse.json({
      success: true,
      data: {
        token: 'mock-jwt-token',
        user: {
          id: 1,
          username: 'testuser',
          email: 'test@example.com',
          role: 'USER'
        }
      }
    })
  }),

  http.post('/api/auth/register', () => {
    return HttpResponse.json({
      success: true,
      data: {
        message: 'User registered successfully'
      }
    })
  }),

  // Tracking endpoints
  http.get('/api/tracking/:trackingNumber', ({ params }) => {
    const { trackingNumber } = params
    
    if (trackingNumber === 'UPS123456789') {
      return HttpResponse.json({
        success: true,
        data: {
          trackingNumber: 'UPS123456789',
          status: 'IN_TRANSIT',
          weight: 5.0,
          createdAt: '2024-01-01T10:00:00',
          updatedAt: '2024-01-01T12:00:00'
        }
      })
    }
    
    return HttpResponse.json({
      success: false,
      message: 'Shipment not found'
    }, { status: 404 })
  }),

  http.get('/api/tracking/:trackingNumber/history', () => {
    return HttpResponse.json({
      success: true,
      data: [
        {
          id: 1,
          status: 'CREATED',
          notes: 'Order created',
          timestamp: '2024-01-01T10:00:00'
        },
        {
          id: 2,
          status: 'IN_TRANSIT',
          notes: 'Package picked up',
          timestamp: '2024-01-01T12:00:00'
        }
      ]
    })
  }),

  // User shipments
  http.get('/api/tracking/user/:userId', () => {
    return HttpResponse.json({
      success: true,
      data: [
        {
          trackingNumber: 'UPS123456789',
          status: 'IN_TRANSIT',
          weight: 5.0,
          createdAt: '2024-01-01T10:00:00'
        },
        {
          trackingNumber: 'UPS987654321',
          status: 'DELIVERED',
          weight: 3.0,
          createdAt: '2024-01-02T10:00:00'
        }
      ]
    })
  }),

  // Truck management
  http.get('/api/trucks', () => {
    return HttpResponse.json({
      success: true,
      data: [
        {
          id: 1,
          licensePlate: 'TRUCK001',
          status: 'AVAILABLE',
          currentLatitude: 10.0,
          currentLongitude: 20.0,
          capacity: 1000.0,
          currentLoad: 500.0
        }
      ]
    })
  }),

  // Error handling
  http.get('/api/error', () => {
    return HttpResponse.json({
      success: false,
      message: 'Internal Server Error'
    }, { status: 500 })
  })
]

export const server = setupServer(...handlers)