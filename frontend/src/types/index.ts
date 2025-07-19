// User types
export interface User {
  id: number
  username: string
  email: string
  firstName?: string
  lastName?: string
  phone?: string
  address?: string
  role: UserRole
  enabled: boolean
  createdAt: string
  updatedAt: string
}

export enum UserRole {
  USER = 'USER',
  ADMIN = 'ADMIN',
  DRIVER = 'DRIVER',
  OPERATOR = 'OPERATOR'
}

// Authentication types
export interface LoginRequest {
  username: string
  password: string
}

export interface RegisterRequest {
  username: string
  email: string
  password: string
  firstName?: string
  lastName?: string
  phone?: string
  address?: string
}

export interface AuthResponse {
  token: string
  type: string
  user: User
}

// Shipment types
export interface Shipment {
  id: number
  shipmentId: string
  upsTrackingId?: string
  amazonOrderId?: string
  status: ShipmentStatus
  originX: number
  originY: number
  destX: number
  destY: number
  weight?: number
  estimatedDelivery?: string
  actualDelivery?: string
  pickupTime?: string
  worldId?: number
  user?: User
  truck?: Truck
  packages: Package[]
  statusHistory: ShipmentStatusHistory[]
  addressChanges: AddressChange[]
  createdAt: string
  updatedAt: string
}

export enum ShipmentStatus {
  CREATED = 'CREATED',
  TRUCK_DISPATCHED = 'TRUCK_DISPATCHED',
  PICKED_UP = 'PICKED_UP',
  IN_TRANSIT = 'IN_TRANSIT',
  OUT_FOR_DELIVERY = 'OUT_FOR_DELIVERY',
  DELIVERED = 'DELIVERED',
  EXCEPTION = 'EXCEPTION',
  RETURNED = 'RETURNED'
}

// Truck types
export interface Truck {
  id: number
  truckId: number
  status: TruckStatus
  currentX: number
  currentY: number
  capacity: number
  driverId?: number
  worldId?: number
  createdAt: string
  updatedAt: string
}

export enum TruckStatus {
  IDLE = 'IDLE',
  TRAVELING = 'TRAVELING',
  ARRIVE_WAREHOUSE = 'ARRIVE_WAREHOUSE',
  LOADING = 'LOADING',
  DELIVERING = 'DELIVERING'
}

// Package types
export interface Package {
  id: number
  packageId: string
  description?: string
  weight?: number
  length?: number
  width?: number
  height?: number
  value?: number
  fragile: boolean
  shipmentId: number
  createdAt: string
  updatedAt: string
}

// Status history types
export interface ShipmentStatusHistory {
  id: number
  status: ShipmentStatus
  timestamp: string
  locationX?: number
  locationY?: number
  notes?: string
  shipmentId: number
  createdAt: string
  updatedAt: string
}

// Address change types
export interface AddressChange {
  id: number
  oldX: number
  oldY: number
  newX: number
  newY: number
  status: AddressChangeStatus
  requestedAt: string
  processedAt?: string
  reason?: string
  shipmentId: number
  requestedById: number
  createdAt: string
  updatedAt: string
}

export enum AddressChangeStatus {
  PENDING = 'PENDING',
  APPROVED = 'APPROVED',
  REJECTED = 'REJECTED'
}

// API response types
export interface ApiResponse<T> {
  success: boolean
  data: T
  message?: string
  errors?: string[]
}

export interface PaginatedResponse<T> {
  content: T[]
  totalElements: number
  totalPages: number
  size: number
  number: number
  first: boolean
  last: boolean
}

// Form types
export interface TrackingFormData {
  trackingNumber: string
}

export interface AddressChangeFormData {
  newX: number
  newY: number
  reason?: string
}

// WebSocket message types
export interface WebSocketMessage {
  type: string
  payload: any
  timestamp: string
}

export interface ShipmentUpdateMessage {
  shipmentId: string
  status: ShipmentStatus
  location?: {
    x: number
    y: number
  }
  timestamp: string
}