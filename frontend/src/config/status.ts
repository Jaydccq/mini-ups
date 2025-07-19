import type { VariantProps } from "class-variance-authority"
import { badgeVariants } from "@/components/ui/badge"

export type ShipmentStatus = 
  | 'PENDING'
  | 'CONFIRMED'
  | 'PICKED_UP'
  | 'IN_TRANSIT'
  | 'OUT_FOR_DELIVERY'
  | 'DELIVERED'
  | 'FAILED_DELIVERY'
  | 'RETURNED'

export type BadgeVariant = VariantProps<typeof badgeVariants>['variant']

export interface StatusConfig {
  label: string
  badgeVariant: BadgeVariant
  dotColor: string
  textColor: string
  bgColor: string
  description?: string
}

/**
 * 统一的状态配置
 * 包含所有状态的显示文本、颜色、图标等信息
 */
export const statusConfig: Record<ShipmentStatus, StatusConfig> = {
  PENDING: {
    label: '待处理',
    badgeVariant: 'pending',
    dotColor: 'bg-status-pending',
    textColor: 'text-status-pending',
    bgColor: 'bg-status-pending/10',
    description: '订单已创建，等待处理'
  },
  CONFIRMED: {
    label: '已确认',
    badgeVariant: 'default',
    dotColor: 'bg-primary',
    textColor: 'text-primary',
    bgColor: 'bg-primary/10',
    description: '订单已确认，准备取件'
  },
  PICKED_UP: {
    label: '已取件',
    badgeVariant: 'transit',
    dotColor: 'bg-status-transit',
    textColor: 'text-status-transit',
    bgColor: 'bg-status-transit/10',
    description: '包裹已从发件地取件'
  },
  IN_TRANSIT: {
    label: '运输中',
    badgeVariant: 'transit',
    dotColor: 'bg-status-transit',
    textColor: 'text-status-transit',
    bgColor: 'bg-status-transit/10',
    description: '包裹正在运输途中'
  },
  OUT_FOR_DELIVERY: {
    label: '派送中',
    badgeVariant: 'warning',
    dotColor: 'bg-warning',
    textColor: 'text-warning',
    bgColor: 'bg-warning/10',
    description: '包裹正在派送中'
  },
  DELIVERED: {
    label: '已送达',
    badgeVariant: 'delivered',
    dotColor: 'bg-status-delivered',
    textColor: 'text-status-delivered',
    bgColor: 'bg-status-delivered/10',
    description: '包裹已成功送达'
  },
  FAILED_DELIVERY: {
    label: '派送失败',
    badgeVariant: 'destructive',
    dotColor: 'bg-destructive',
    textColor: 'text-destructive',
    bgColor: 'bg-destructive/10',
    description: '包裹派送失败'
  },
  RETURNED: {
    label: '已退回',
    badgeVariant: 'destructive',
    dotColor: 'bg-destructive',
    textColor: 'text-destructive',
    bgColor: 'bg-destructive/10',
    description: '包裹已退回发件人'
  }
}

/**
 * 获取状态配置
 */
export function getStatusConfig(status: string): StatusConfig {
  return statusConfig[status as ShipmentStatus] || {
    label: status,
    badgeVariant: 'secondary',
    dotColor: 'bg-muted-foreground',
    textColor: 'text-muted-foreground',
    bgColor: 'bg-muted',
    description: '未知状态'
  }
}

/**
 * 格式化运单状态显示文本
 */
export function formatShipmentStatus(status: string): string {
  return getStatusConfig(status).label
}

/**
 * 获取运单状态对应的颜色类
 */
export function getStatusColor(status: string): string {
  const config = getStatusConfig(status)
  return `${config.textColor} ${config.bgColor}`
}

/**
 * 获取状态对应的Badge变体
 */
export function getStatusBadgeVariant(status: string): BadgeVariant {
  return getStatusConfig(status).badgeVariant
}

/**
 * 获取状态对应的点颜色
 */
export function getStatusDotColor(status: string): string {
  return getStatusConfig(status).dotColor
}

/**
 * 获取状态描述
 */
export function getStatusDescription(status: string): string {
  return getStatusConfig(status).description || ''
}

/**
 * 检查状态是否为最终状态（不会再变化）
 */
export function isFinalStatus(status: string): boolean {
  const finalStatuses: ShipmentStatus[] = ['DELIVERED', 'FAILED_DELIVERY', 'RETURNED']
  return finalStatuses.includes(status as ShipmentStatus)
}

/**
 * 检查状态是否为活跃状态（正在处理中）
 */
export function isActiveStatus(status: string): boolean {
  const activeStatuses: ShipmentStatus[] = ['CONFIRMED', 'PICKED_UP', 'IN_TRANSIT', 'OUT_FOR_DELIVERY']
  return activeStatuses.includes(status as ShipmentStatus)
}

/**
 * Get the processing priority of the status (the larger the number, the higher the priority)
 */
export function getStatusPriority(status: string): number {
  const priorities: Record<ShipmentStatus, number> = {
    PENDING: 1,
    CONFIRMED: 2,
    PICKED_UP: 3,
    IN_TRANSIT: 4,
    OUT_FOR_DELIVERY: 5,
    DELIVERED: 6,
    FAILED_DELIVERY: 0,
    RETURNED: 0
  }
  return priorities[status as ShipmentStatus] || 0
}