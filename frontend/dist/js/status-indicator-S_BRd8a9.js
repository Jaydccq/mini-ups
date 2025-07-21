import { r as reactExports, j as jsxRuntimeExports, e as cn } from "./index-D-bA2lkg.js";
import { B as Badge } from "./badge-2PvS7lZW.js";
const statusConfig = {
  PENDING: {
    label: "待处理",
    badgeVariant: "pending",
    dotColor: "bg-status-pending",
    textColor: "text-status-pending",
    bgColor: "bg-status-pending/10",
    description: "订单已创建，等待处理"
  },
  CONFIRMED: {
    label: "已确认",
    badgeVariant: "default",
    dotColor: "bg-primary",
    textColor: "text-primary",
    bgColor: "bg-primary/10",
    description: "订单已确认，准备取件"
  },
  PICKED_UP: {
    label: "已取件",
    badgeVariant: "transit",
    dotColor: "bg-status-transit",
    textColor: "text-status-transit",
    bgColor: "bg-status-transit/10",
    description: "包裹已从发件地取件"
  },
  IN_TRANSIT: {
    label: "运输中",
    badgeVariant: "transit",
    dotColor: "bg-status-transit",
    textColor: "text-status-transit",
    bgColor: "bg-status-transit/10",
    description: "包裹正在运输途中"
  },
  OUT_FOR_DELIVERY: {
    label: "派送中",
    badgeVariant: "warning",
    dotColor: "bg-warning",
    textColor: "text-warning",
    bgColor: "bg-warning/10",
    description: "包裹正在派送中"
  },
  DELIVERED: {
    label: "已送达",
    badgeVariant: "delivered",
    dotColor: "bg-status-delivered",
    textColor: "text-status-delivered",
    bgColor: "bg-status-delivered/10",
    description: "包裹已成功送达"
  },
  FAILED_DELIVERY: {
    label: "派送失败",
    badgeVariant: "destructive",
    dotColor: "bg-destructive",
    textColor: "text-destructive",
    bgColor: "bg-destructive/10",
    description: "包裹派送失败"
  },
  RETURNED: {
    label: "已退回",
    badgeVariant: "destructive",
    dotColor: "bg-destructive",
    textColor: "text-destructive",
    bgColor: "bg-destructive/10",
    description: "包裹已退回发件人"
  }
};
function getStatusConfig(status) {
  return statusConfig[status] || {
    label: status,
    badgeVariant: "secondary",
    dotColor: "bg-muted-foreground",
    textColor: "text-muted-foreground",
    bgColor: "bg-muted",
    description: "未知状态"
  };
}
function formatShipmentStatus(status) {
  return getStatusConfig(status).label;
}
function getStatusBadgeVariant(status) {
  return getStatusConfig(status).badgeVariant;
}
function getStatusDotColor(status) {
  return getStatusConfig(status).dotColor;
}
function getStatusDescription(status) {
  return getStatusConfig(status).description || "";
}
const StatusIndicator = reactExports.forwardRef(
  ({ status, showDot = false, animated = false, showDescription = false, className, ...props }, ref) => {
    const badgeVariant = getStatusBadgeVariant(status);
    const dotColor = getStatusDotColor(status);
    const description = getStatusDescription(status);
    return /* @__PURE__ */ jsxRuntimeExports.jsxs("div", { ref, className: cn("flex items-center gap-2", className), ...props, children: [
      showDot && /* @__PURE__ */ jsxRuntimeExports.jsx(
        "div",
        {
          className: cn(
            "h-2 w-2 rounded-full",
            dotColor,
            animated && "animate-pulse-slow"
          )
        }
      ),
      /* @__PURE__ */ jsxRuntimeExports.jsxs("div", { className: "flex flex-col", children: [
        /* @__PURE__ */ jsxRuntimeExports.jsx(Badge, { variant: badgeVariant, children: formatShipmentStatus(status) }),
        showDescription && description && /* @__PURE__ */ jsxRuntimeExports.jsx("span", { className: "text-xs text-muted-foreground mt-1", children: description })
      ] })
    ] });
  }
);
StatusIndicator.displayName = "StatusIndicator";
export {
  StatusIndicator as S
};
