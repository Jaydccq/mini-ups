import { c as createLucideIcon, a as api } from "./index-D-bA2lkg.js";
/**
 * @license lucide-react v0.309.0 - ISC
 *
 * This source code is licensed under the ISC license.
 * See the LICENSE file in the root directory of this source tree.
 */
const Download = createLucideIcon("Download", [
  ["path", { d: "M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4", key: "ih7n3h" }],
  ["polyline", { points: "7 10 12 15 17 10", key: "2ggqvy" }],
  ["line", { x1: "12", x2: "12", y1: "15", y2: "3", key: "1vk2je" }]
]);
const adminApi = {
  // User Management
  getUsers: (filters) => {
    const params = new URLSearchParams();
    Object.entries(filters).forEach(([key, value]) => {
      if (value !== void 0 && value !== null) {
        params.append(key, value.toString());
      }
    });
    return api.get(`/admin/users?${params.toString()}`).then((res) => res.data.data);
  },
  getUserById: (userId) => {
    return api.get(`/admin/users/${userId}`).then((res) => res.data.data);
  },
  updateUser: (userId, data) => {
    return api.put(`/admin/users/${userId}`, data).then((res) => res.data.data);
  },
  deleteUser: (userId) => {
    return api.delete(`/admin/users/${userId}`).then((res) => res.data);
  },
  // Shipment Management
  getAllShipments: (filters) => {
    const params = new URLSearchParams();
    Object.entries(filters).forEach(([key, value]) => {
      if (value !== void 0 && value !== null) {
        params.append(key, value.toString());
      }
    });
    return api.get(`/admin/shipments?${params.toString()}`).then((res) => res.data.data);
  },
  updateShipmentStatus: (trackingNumber, status, comment) => {
    return api.put(`/admin/shipments/${trackingNumber}/status`, { status, comment }).then((res) => res.data);
  },
  batchUpdateShipments: (data) => {
    return api.put("/admin/shipments/batch-update", data).then((res) => res.data);
  },
  deleteShipment: (trackingNumber) => {
    return api.delete(`/admin/shipments/${trackingNumber}`).then((res) => res.data);
  },
  // Analytics
  getAnalytics: (dateRange) => {
    const params = new URLSearchParams();
    if (dateRange) {
      params.append("from", dateRange.from);
      params.append("to", dateRange.to);
    }
    return api.get(`/admin/analytics?${params.toString()}`).then((res) => res.data.data);
  },
  exportData: (type, filters) => {
    const params = new URLSearchParams();
    if (filters) {
      Object.entries(filters).forEach(([key, value]) => {
        if (value !== void 0 && value !== null) {
          params.append(key, value.toString());
        }
      });
    }
    return api.get(`/admin/export/${type}?${params.toString()}`, {
      responseType: "blob"
    }).then((res) => res.data);
  },
  // System Health
  getSystemHealth: () => {
    return api.get("/admin/health").then((res) => res.data.data);
  },
  // Fleet Management CRUD Operations
  createTruck: (truckData) => {
    return api.post("/admin/fleet/trucks", truckData).then((res) => res.data.data);
  },
  updateTruck: (truckId, truckData) => {
    return api.put(`/admin/fleet/trucks/${truckId}`, truckData).then((res) => res.data.data);
  },
  deleteTruck: (truckId) => {
    return api.delete(`/admin/fleet/trucks/${truckId}`).then((res) => res.data);
  },
  createDriver: (driverData) => {
    return api.post("/admin/fleet/drivers", driverData).then((res) => res.data.data);
  },
  updateDriver: (driverId, driverData) => {
    return api.put(`/admin/fleet/drivers/${driverId}`, driverData).then((res) => res.data.data);
  },
  deleteDriver: (driverId) => {
    return api.delete(`/admin/fleet/drivers/${driverId}`).then((res) => res.data);
  },
  // Dashboard APIs
  getDashboardStatistics: () => {
    return api.get("/admin/dashboard/statistics").then((res) => res.data.data);
  },
  getFleetOverview: () => {
    return api.get("/admin/fleet/overview").then((res) => res.data.data);
  },
  getDriverManagement: () => {
    return api.get("/admin/fleet/drivers").then((res) => res.data.data);
  },
  getRecentActivities: (page = 0, size = 10) => {
    return api.get(`/admin/dashboard/activities?page=${page}&size=${size}`).then((res) => res.data.data);
  }
};
export {
  Download as D,
  adminApi as a
};
