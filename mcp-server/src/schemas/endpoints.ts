/**
 * Backend Endpoint Specifications
 *
 * Central allowlist of read-only backend endpoints the MCP server can call.
 * Update these to match your actual Spring Boot REST controllers.
 */

export type HttpMethod = 'GET';

export interface EndpointSpec {
  name: string;
  method: HttpMethod;
  pathTemplate: string; // e.g. /api/orders/{orderId}
  description: string;
  notes?: string;
}

// Endpoints based on actual Swagger API documentation
export const ENDPOINTS: EndpointSpec[] = [
  // Tracking & Shipment APIs
  {
    name: 'track_package',
    method: 'GET',
    pathTemplate: '/api/tracking/{trackingNumber}',
    description: '根据追踪号查询包裹状态和位置信息'
  },
  {
    name: 'tracking_history',
    method: 'GET',
    pathTemplate: '/api/tracking/{trackingNumber}/history',
    description: '获取包裹的完整追踪历史记录'
  },
  {
    name: 'validate_tracking',
    method: 'GET',
    pathTemplate: '/api/tracking/validate/{trackingNumber}',
    description: '验证追踪号格式是否正确'
  },
  {
    name: 'user_shipments',
    method: 'GET',
    pathTemplate: '/api/tracking/user/{userId}',
    description: '获取指定用户的所有运单信息'
  },

  // User Management APIs
  {
    name: 'get_user_by_id',
    method: 'GET',
    pathTemplate: '/users/{userId}',
    description: '根据用户ID获取用户详细信息'
  },
  {
    name: 'get_current_user',
    method: 'GET',
    pathTemplate: '/api/auth/me',
    description: '获取当前登录用户的信息'
  },
  {
    name: 'get_user_profile',
    method: 'GET',
    pathTemplate: '/users/profile',
    description: '获取当前用户的个人档案'
  },
  {
    name: 'get_users_list',
    method: 'GET',
    pathTemplate: '/users',
    description: '获取用户列表（支持角色过滤和分页）',
    notes: 'Supports role filter and pagination'
  },

  // Driver Management APIs
  {
    name: 'get_all_drivers',
    method: 'GET',
    pathTemplate: '/drivers',
    description: '获取所有司机列表（支持状态过滤和分页）'
  },
  {
    name: 'get_driver_by_id',
    method: 'GET',
    pathTemplate: '/drivers/{driverId}',
    description: '根据司机ID获取司机详细信息'
  },
  {
    name: 'get_available_drivers',
    method: 'GET',
    pathTemplate: '/drivers/available',
    description: '获取可用的司机列表'
  },
  {
    name: 'search_drivers',
    method: 'GET',
    pathTemplate: '/drivers/search',
    description: '根据姓名搜索司机',
    notes: 'Requires name parameter'
  },
  {
    name: 'get_driver_statistics',
    method: 'GET',
    pathTemplate: '/drivers/statistics',
    description: '获取司机相关统计信息'
  },

  // Truck & Fleet Management APIs
  {
    name: 'get_all_trucks',
    method: 'GET',
    pathTemplate: '/trucks',
    description: '获取所有卡车信息'
  },
  {
    name: 'get_fleet_statistics',
    method: 'GET',
    pathTemplate: '/trucks/statistics',
    description: '获取车队统计信息'
  },
  {
    name: 'find_nearest_truck',
    method: 'GET',
    pathTemplate: '/trucks/nearest',
    description: '根据坐标查找最近的卡车',
    notes: 'Requires targetX and targetY parameters'
  },

  // Admin Dashboard APIs
  {
    name: 'dashboard_statistics',
    method: 'GET',
    pathTemplate: '/api/admin/dashboard/statistics',
    description: '获取管理员仪表盘统计数据'
  },
  {
    name: 'fleet_overview',
    method: 'GET',
    pathTemplate: '/api/admin/fleet/overview',
    description: '获取车队概览信息'
  },
  {
    name: 'driver_management',
    method: 'GET',
    pathTemplate: '/api/admin/fleet/drivers',
    description: '获取司机管理信息'
  },
  {
    name: 'order_summary',
    method: 'GET',
    pathTemplate: '/api/admin/orders/summary',
    description: '获取订单汇总信息'
  },
  {
    name: 'recent_activities',
    method: 'GET',
    pathTemplate: '/api/admin/dashboard/activities',
    description: '获取最近活动记录（支持分页）'
  },
  {
    name: 'analytics_trends',
    method: 'GET',
    pathTemplate: '/api/admin/analytics/trends',
    description: '获取数据分析趋势'
  },

  // System Health & Debug APIs
  {
    name: 'api_health_check',
    method: 'GET',
    pathTemplate: '/api/health',
    description: '系统健康检查'
  },
  {
    name: 'system_health',
    method: 'GET',
    pathTemplate: '/api/admin/system/health',
    description: '详细系统健康状态检查'
  },
  {
    name: 'debug_statistics',
    method: 'GET',
    pathTemplate: '/api/debug/statistics',
    description: '获取调试统计信息',
    notes: 'Supports hoursBack parameter'
  },
  {
    name: 'debug_communication_logs',
    method: 'GET',
    pathTemplate: '/api/debug/communications',
    description: '获取通信日志',
    notes: 'Supports filtering by direction, messageType, success, hoursBack, limit'
  },
  {
    name: 'debug_recent_errors',
    method: 'GET',
    pathTemplate: '/api/debug/errors',
    description: '获取最近的系统错误'
  },

  // Authentication & Validation APIs
  {
    name: 'validate_token',
    method: 'GET',
    pathTemplate: '/api/auth/validate',
    description: '验证JWT令牌是否有效'
  },
  {
    name: 'check_username',
    method: 'GET',
    pathTemplate: '/api/auth/check-username',
    description: '检查用户名是否可用'
  },
  {
    name: 'check_email',
    method: 'GET',
    pathTemplate: '/api/auth/check-email',
    description: '检查邮箱地址是否可用'
  },

  // Test Endpoints
  {
    name: 'test_hello',
    method: 'GET',
    pathTemplate: '/api/test/hello',
    description: '简单的Hello World测试端点'
  },
  {
    name: 'test_health',
    method: 'GET',
    pathTemplate: '/api/test/health',
    description: '测试API健康状态'
  },
  {
    name: 'test_public',
    method: 'GET',
    pathTemplate: '/api/test/public',
    description: '公开测试端点（无需认证）'
  }
];

/**
 * Render endpoints as text for prompts.
 */
export function renderEndpointList(): string {
  return ENDPOINTS.map(e => `- ${e.method} ${e.pathTemplate} - ${e.description}`).join('\n');
}

/**
 * Check if a requested endpoint path is allowed by the allowlist.
 * The check tolerates concrete paths (with actual values) or templates.
 */
export function isAllowedEndpointPath(path: string): boolean {
  // Normalize by stripping query string
  const basePath = path.split('?')[0];

  // Convert templates to regex: /api/orders/{orderId} -> ^/api/orders/[^/]+$
  const toRegex = (tpl: string) =>
    new RegExp('^' + tpl.replace(/\{[^}]+\}/g, '[^/]+') + '$');

  return ENDPOINTS.some(e => toRegex(e.pathTemplate).test(basePath));
}
