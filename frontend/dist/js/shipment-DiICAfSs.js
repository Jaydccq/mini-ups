import { a as api } from "./index-D-bA2lkg.js";
const shipmentApi = {
  // 获取用户所有运单
  getUserShipments: (userId) => {
    return api.get(`/tracking/user/${userId}`).then((res) => res.data.data);
  },
  // 根据追踪号查询运单
  getShipmentByTrackingNumber: (trackingNumber) => {
    return api.get(`/tracking/${trackingNumber}`).then((res) => res.data.data);
  },
  // 获取追踪历史
  getTrackingHistory: (trackingNumber) => {
    return api.get(`/tracking/${trackingNumber}/history`).then((res) => res.data.data);
  },
  // 验证追踪号格式
  validateTrackingNumber: (trackingNumber) => {
    return api.get(`/tracking/validate/${trackingNumber}`).then((res) => res.data.data);
  },
  // 更新运单状态（管理员）
  updateShipmentStatus: (trackingNumber, status, comment) => {
    return api.put(`/tracking/${trackingNumber}/status`, { status, comment }).then((res) => res.data.data);
  },
  // 创建新运单
  createShipment: (shipmentData) => {
    return api.post("/shipments", shipmentData).then((res) => res.data.data);
  },
  // 获取仪表盘统计数据
  getDashboardStats: async (userId) => {
    const userShipments = await shipmentApi.getUserShipments(userId);
    const shipments = userShipments.shipments;
    const total_shipments = shipments.length;
    const delivered_shipments = shipments.filter((s) => s.status === "DELIVERED").length;
    const pending_shipments = shipments.filter((s) => s.status === "PENDING").length;
    const active_shipments = shipments.filter(
      (s) => !["DELIVERED", "CANCELLED", "RETURNED"].includes(s.status)
    ).length;
    const recent_shipments = shipments.sort((a, b) => new Date(b.created_at).getTime() - new Date(a.created_at).getTime()).slice(0, 5);
    return {
      total_shipments,
      active_shipments,
      delivered_shipments,
      pending_shipments,
      recent_shipments
    };
  }
};
export {
  shipmentApi as s
};
