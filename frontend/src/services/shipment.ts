import api from './api'

export interface ShipmentLocation {
  x: number
  y: number
}

export interface TruckInfo {
  truck_id: number
  status: string
  current_location?: ShipmentLocation
}

export interface Shipment {
  shipment_id: string
  tracking_number: string
  status: string
  status_display: string
  origin: ShipmentLocation
  destination: ShipmentLocation
  created_at: string
  estimated_delivery: string | null
  actual_delivery: string | null
  pickup_time: string | null
  truck?: TruckInfo
}

export interface ShipmentStatusHistory {
  status: string
  status_display: string
  timestamp: string
  comment: string
}

export interface UserShipmentsResponse {
  user_id: number
  shipments: Shipment[]
  total_count: number
}

export interface TrackingHistoryResponse {
  tracking_number: string
  history: ShipmentStatusHistory[]
  total_events: number
}

export interface DashboardStats {
  total_shipments: number
  active_shipments: number
  delivered_shipments: number
  pending_shipments: number
  recent_shipments: Shipment[]
}

export interface CreateShipmentRequest {
  recipient_name: string
  recipient_email: string
  recipient_phone: string
  recipient_address: string
  destination_x: number
  destination_y: number
  package_description: string
  package_weight: number
  package_dimensions: {
    length: number
    width: number
    height: number
  }
  package_value: number
  delivery_speed: 'STANDARD' | 'EXPRESS' | 'OVERNIGHT'
  special_instructions?: string
}

export interface CreateShipmentResponse {
  shipment_id: string
  tracking_number: string
  status: string
  estimated_delivery: string
  created_at: string
}

export const shipmentApi = {
  // 获取用户所有运单
  getUserShipments: (userId: number): Promise<UserShipmentsResponse> => {
    return api.get(`/tracking/user/${userId}`).then(res => res.data.data)
  },

  // 根据追踪号查询运单
  getShipmentByTrackingNumber: (trackingNumber: string): Promise<Shipment> => {
    return api.get(`/tracking/${trackingNumber}`).then(res => res.data.data)
  },

  // 获取追踪历史
  getTrackingHistory: (trackingNumber: string): Promise<TrackingHistoryResponse> => {
    return api.get(`/tracking/${trackingNumber}/history`).then(res => res.data.data)
  },

  // 验证追踪号格式
  validateTrackingNumber: (trackingNumber: string): Promise<{ tracking_number: string; valid: boolean }> => {
    return api.get(`/tracking/validate/${trackingNumber}`).then(res => res.data.data)
  },

  // 更新运单状态（管理员）
  updateShipmentStatus: (trackingNumber: string, status: string, comment?: string): Promise<{ tracking_number: string; new_status: string }> => {
    return api.put(`/tracking/${trackingNumber}/status`, { status, comment }).then(res => res.data.data)
  },

  // 创建新运单
  createShipment: (shipmentData: CreateShipmentRequest): Promise<CreateShipmentResponse> => {
    return api.post('/shipments', shipmentData).then(res => res.data.data)
  },

  // 获取仪表盘统计数据
  getDashboardStats: async (userId: number): Promise<DashboardStats> => {
    const userShipments = await shipmentApi.getUserShipments(userId)
    const shipments = userShipments.shipments
    
    // 计算各种统计数据
    const total_shipments = shipments.length
    const delivered_shipments = shipments.filter(s => s.status === 'DELIVERED').length
    const pending_shipments = shipments.filter(s => s.status === 'PENDING').length
    const active_shipments = shipments.filter(s => 
      !['DELIVERED', 'CANCELLED', 'RETURNED'].includes(s.status)
    ).length
    
    // 获取最近5个运单（按创建时间排序）
    const recent_shipments = shipments
      .sort((a, b) => new Date(b.created_at).getTime() - new Date(a.created_at).getTime())
      .slice(0, 5)
    
    return {
      total_shipments,
      active_shipments,
      delivered_shipments,
      pending_shipments,
      recent_shipments
    }
  }
}