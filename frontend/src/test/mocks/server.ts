import { setupServer } from 'msw/node'
import { rest } from 'msw'

// Mock API handlers for testing
export const handlers = [
  // Authentication endpoints
  rest.post('/api/auth/login', (req, res, ctx) => {
    return res(
      ctx.json({
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
    )
  }),

  rest.post('/api/auth/register', (req, res, ctx) => {
    return res(
      ctx.json({
        success: true,
        data: {
          message: 'User registered successfully'
        }
      })
    )
  }),

  // Tracking endpoints
  rest.get('/api/tracking/:trackingNumber', (req, res, ctx) => {
    const { trackingNumber } = req.params
    
    if (trackingNumber === 'UPS123456789') {
      return res(
        ctx.json({
          success: true,
          data: {
            trackingNumber: 'UPS123456789',
            status: 'IN_TRANSIT',
            weight: 5.0,
            createdAt: '2024-01-01T10:00:00',
            updatedAt: '2024-01-01T12:00:00'
          }
        })
      )
    }
    
    return res(
      ctx.status(404),
      ctx.json({
        success: false,
        message: 'Shipment not found'
      })
    )
  }),

  rest.get('/api/tracking/:trackingNumber/history', (req, res, ctx) => {
    return res(
      ctx.json({
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
    )
  }),

  // User shipments
  rest.get('/api/tracking/user/:userId', (req, res, ctx) => {
    return res(
      ctx.json({
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
    )
  }),

  // Truck management
  rest.get('/api/trucks', (req, res, ctx) => {
    return res(
      ctx.json({
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
    )
  }),

  // Error handling
  rest.get('/api/error', (req, res, ctx) => {
    return res(
      ctx.status(500),
      ctx.json({
        success: false,
        message: 'Internal Server Error'
      })
    )
  })
]

export const server = setupServer(...handlers)