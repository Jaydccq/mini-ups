import { j as jsxRuntimeExports, P as Package, f as formatDateTime, M as MapPin, T as Truck, r as reactExports, o as calculateDistance, e as cn, q as socketService } from "./index-D-bA2lkg.js";
import { A as AlertCircle } from "./alert-circle-DRL_kTZp.js";
import { C as Check } from "./check-DBMoG0qQ.js";
import { C as Clock } from "./clock-BBOft3LF.js";
import { C as Card, a as CardHeader, b as CardTitle, d as CardContent } from "./card-CsLEcWf3.js";
import { B as Badge } from "./badge-2PvS7lZW.js";
const getStatusIcon = (status, isCompleted) => {
  const iconClass = `h-5 w-5 ${"text-white"}`;
  switch (status) {
    case "PENDING":
      return /* @__PURE__ */ jsxRuntimeExports.jsx(Clock, { className: iconClass });
    case "PICKED_UP":
      return /* @__PURE__ */ jsxRuntimeExports.jsx(Package, { className: iconClass });
    case "IN_TRANSIT":
      return /* @__PURE__ */ jsxRuntimeExports.jsx(Truck, { className: iconClass });
    case "DELIVERED":
      return /* @__PURE__ */ jsxRuntimeExports.jsx(Check, { className: iconClass });
    case "CANCELLED":
    case "RETURNED":
      return /* @__PURE__ */ jsxRuntimeExports.jsx(AlertCircle, { className: iconClass });
    default:
      return /* @__PURE__ */ jsxRuntimeExports.jsx(MapPin, { className: iconClass });
  }
};
const getStatusColor = (status, isCompleted) => {
  switch (status) {
    case "PENDING":
      return "bg-yellow-500 border-yellow-600";
    case "PICKED_UP":
      return "bg-blue-500 border-blue-600";
    case "IN_TRANSIT":
      return "bg-indigo-500 border-indigo-600";
    case "DELIVERED":
      return "bg-green-500 border-green-600";
    case "CANCELLED":
    case "RETURNED":
      return "bg-red-500 border-red-600";
    default:
      return "bg-gray-500 border-gray-600";
  }
};
function TrackingTimeline({ history, currentStatus, loading }) {
  if (loading) {
    return /* @__PURE__ */ jsxRuntimeExports.jsx("div", { className: "space-y-4", children: Array.from({ length: 4 }).map((_, i) => /* @__PURE__ */ jsxRuntimeExports.jsxs("div", { className: "flex items-center space-x-4", children: [
      /* @__PURE__ */ jsxRuntimeExports.jsx("div", { className: "w-10 h-10 bg-gray-200 rounded-full animate-pulse" }),
      /* @__PURE__ */ jsxRuntimeExports.jsxs("div", { className: "flex-1 space-y-2", children: [
        /* @__PURE__ */ jsxRuntimeExports.jsx("div", { className: "h-4 bg-gray-200 rounded w-1/4 animate-pulse" }),
        /* @__PURE__ */ jsxRuntimeExports.jsx("div", { className: "h-3 bg-gray-200 rounded w-1/2 animate-pulse" })
      ] })
    ] }, i)) });
  }
  if (history.length === 0) {
    return /* @__PURE__ */ jsxRuntimeExports.jsxs("div", { className: "text-center py-8 text-gray-500", children: [
      /* @__PURE__ */ jsxRuntimeExports.jsx(Package, { className: "h-12 w-12 mx-auto mb-4 opacity-50" }),
      /* @__PURE__ */ jsxRuntimeExports.jsx("p", { children: "No tracking history available" })
    ] });
  }
  return /* @__PURE__ */ jsxRuntimeExports.jsx("div", { className: "relative", children: history.map((event, index) => {
    const isLast = index === history.length - 1;
    const statusColor = getStatusColor(event.status);
    return /* @__PURE__ */ jsxRuntimeExports.jsxs("div", { className: "relative flex items-start space-x-4", children: [
      !isLast && /* @__PURE__ */ jsxRuntimeExports.jsx("div", { className: "absolute left-5 top-10 w-0.5 h-16 bg-gray-200" }),
      /* @__PURE__ */ jsxRuntimeExports.jsx("div", { className: `relative flex items-center justify-center w-10 h-10 rounded-full border-2 ${statusColor}`, children: getStatusIcon(event.status) }),
      /* @__PURE__ */ jsxRuntimeExports.jsxs("div", { className: "flex-1 min-w-0", children: [
        /* @__PURE__ */ jsxRuntimeExports.jsxs("div", { className: "flex items-center justify-between", children: [
          /* @__PURE__ */ jsxRuntimeExports.jsx("h3", { className: "text-lg font-semibold text-gray-900", children: event.status_display }),
          /* @__PURE__ */ jsxRuntimeExports.jsx("span", { className: "text-sm text-gray-500", children: formatDateTime(event.timestamp) })
        ] }),
        event.comment && /* @__PURE__ */ jsxRuntimeExports.jsx("p", { className: "mt-1 text-sm text-gray-600", children: event.comment }),
        /* @__PURE__ */ jsxRuntimeExports.jsx("div", { className: "mt-2", children: /* @__PURE__ */ jsxRuntimeExports.jsx("span", { className: "inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium bg-gray-100 text-gray-800", children: event.status }) })
      ] })
    ] }, `${event.status}-${event.timestamp}`);
  }) });
}
const TrackingMap = ({
  origin,
  destination,
  currentLocation,
  truckStatus,
  className = ""
}) => {
  const mapData = reactExports.useMemo(() => {
    const points = [origin, destination];
    if (currentLocation) {
      points.push(currentLocation);
    }
    const minX = Math.min(...points.map((p) => p.x));
    const maxX = Math.max(...points.map((p) => p.x));
    const minY = Math.min(...points.map((p) => p.y));
    const maxY = Math.max(...points.map((p) => p.y));
    const centerX = (minX + maxX) / 2;
    const centerY = (minY + maxY) / 2;
    const rangeX = maxX - minX;
    const rangeY = maxY - minY;
    const range = Math.max(rangeX, rangeY, 50);
    const bounds = {
      minX: centerX - range / 2 - 10,
      maxX: centerX + range / 2 + 10,
      minY: centerY - range / 2 - 10,
      maxY: centerY + range / 2 + 10
    };
    const svgWidth = 400;
    const svgHeight = 300;
    const scaleX = svgWidth / (bounds.maxX - bounds.minX);
    const scaleY = svgHeight / (bounds.maxY - bounds.minY);
    const scale = Math.min(scaleX, scaleY) * 0.8;
    const transformX = (x) => (x - bounds.minX) * scale + (svgWidth - (bounds.maxX - bounds.minX) * scale) / 2;
    const transformY = (y) => svgHeight - ((y - bounds.minY) * scale + (svgHeight - (bounds.maxY - bounds.minY) * scale) / 2);
    return {
      svgWidth,
      svgHeight,
      transformX,
      transformY,
      bounds
    };
  }, [origin, destination, currentLocation]);
  const progress = reactExports.useMemo(() => {
    if (!currentLocation) return 0;
    const totalDistance2 = calculateDistance(origin, destination);
    const traveledDistance = calculateDistance(origin, currentLocation);
    return Math.min(Math.round(traveledDistance / totalDistance2 * 100), 100);
  }, [origin, destination, currentLocation]);
  const totalDistance = reactExports.useMemo(() => calculateDistance(origin, destination), [origin, destination]);
  const transformedOrigin = reactExports.useMemo(() => ({
    x: mapData.transformX(origin.x),
    y: mapData.transformY(origin.y)
  }), [origin, mapData]);
  const transformedDestination = reactExports.useMemo(() => ({
    x: mapData.transformX(destination.x),
    y: mapData.transformY(destination.y)
  }), [destination, mapData]);
  const transformedCurrentLocation = reactExports.useMemo(() => {
    if (!currentLocation) return null;
    return {
      x: mapData.transformX(currentLocation.x),
      y: mapData.transformY(currentLocation.y)
    };
  }, [currentLocation, mapData]);
  return /* @__PURE__ */ jsxRuntimeExports.jsxs(Card, { className, children: [
    /* @__PURE__ */ jsxRuntimeExports.jsx(CardHeader, { children: /* @__PURE__ */ jsxRuntimeExports.jsxs(CardTitle, { className: "flex items-center gap-2", children: [
      /* @__PURE__ */ jsxRuntimeExports.jsx(MapPin, { className: "h-5 w-5" }),
      "Shipment Route"
    ] }) }),
    /* @__PURE__ */ jsxRuntimeExports.jsxs(CardContent, { className: "space-y-4", children: [
      /* @__PURE__ */ jsxRuntimeExports.jsx("div", { className: "w-full h-[300px] flex items-center justify-center bg-gray-50 rounded-lg", children: /* @__PURE__ */ jsxRuntimeExports.jsxs(
        "svg",
        {
          width: mapData.svgWidth,
          height: mapData.svgHeight,
          viewBox: `0 0 ${mapData.svgWidth} ${mapData.svgHeight}`,
          className: "border rounded-lg bg-blue-50",
          children: [
            transformedCurrentLocation ? /* @__PURE__ */ jsxRuntimeExports.jsxs(jsxRuntimeExports.Fragment, { children: [
              /* @__PURE__ */ jsxRuntimeExports.jsx(
                "line",
                {
                  x1: transformedOrigin.x,
                  y1: transformedOrigin.y,
                  x2: transformedCurrentLocation.x,
                  y2: transformedCurrentLocation.y,
                  stroke: "#10b981",
                  strokeWidth: "3",
                  strokeDasharray: "5,5"
                }
              ),
              /* @__PURE__ */ jsxRuntimeExports.jsx(
                "line",
                {
                  x1: transformedCurrentLocation.x,
                  y1: transformedCurrentLocation.y,
                  x2: transformedDestination.x,
                  y2: transformedDestination.y,
                  stroke: "#9ca3af",
                  strokeWidth: "2",
                  strokeDasharray: "3,3"
                }
              )
            ] }) : (
              /* Planned route (blue) */
              /* @__PURE__ */ jsxRuntimeExports.jsx(
                "line",
                {
                  x1: transformedOrigin.x,
                  y1: transformedOrigin.y,
                  x2: transformedDestination.x,
                  y2: transformedDestination.y,
                  stroke: "#3b82f6",
                  strokeWidth: "2",
                  strokeDasharray: "5,5"
                }
              )
            ),
            /* @__PURE__ */ jsxRuntimeExports.jsx(
              "circle",
              {
                cx: transformedOrigin.x,
                cy: transformedOrigin.y,
                r: "8",
                fill: "#ef4444",
                stroke: "#ffffff",
                strokeWidth: "2"
              }
            ),
            /* @__PURE__ */ jsxRuntimeExports.jsx(
              "circle",
              {
                cx: transformedDestination.x,
                cy: transformedDestination.y,
                r: "8",
                fill: "#10b981",
                stroke: "#ffffff",
                strokeWidth: "2"
              }
            ),
            transformedCurrentLocation && /* @__PURE__ */ jsxRuntimeExports.jsx(
              "rect",
              {
                x: transformedCurrentLocation.x - 6,
                y: transformedCurrentLocation.y - 6,
                width: "12",
                height: "12",
                fill: "#f59e0b",
                stroke: "#ffffff",
                strokeWidth: "2",
                rx: "2"
              }
            )
          ]
        }
      ) }),
      /* @__PURE__ */ jsxRuntimeExports.jsxs("div", { className: "grid grid-cols-2 md:grid-cols-3 gap-4 text-sm", children: [
        /* @__PURE__ */ jsxRuntimeExports.jsxs("div", { className: "flex items-center gap-2", children: [
          /* @__PURE__ */ jsxRuntimeExports.jsx("div", { className: "w-4 h-4 bg-red-500 rounded-full border-2 border-white" }),
          /* @__PURE__ */ jsxRuntimeExports.jsx("span", { children: "Origin" })
        ] }),
        /* @__PURE__ */ jsxRuntimeExports.jsxs("div", { className: "flex items-center gap-2", children: [
          /* @__PURE__ */ jsxRuntimeExports.jsx("div", { className: "w-4 h-4 bg-green-500 rounded-full border-2 border-white" }),
          /* @__PURE__ */ jsxRuntimeExports.jsx("span", { children: "Destination" })
        ] }),
        currentLocation && /* @__PURE__ */ jsxRuntimeExports.jsxs("div", { className: "flex items-center gap-2", children: [
          /* @__PURE__ */ jsxRuntimeExports.jsx("div", { className: "w-4 h-4 bg-yellow-500 rounded border-2 border-white" }),
          /* @__PURE__ */ jsxRuntimeExports.jsx("span", { children: "Current Location" })
        ] })
      ] }),
      /* @__PURE__ */ jsxRuntimeExports.jsxs("div", { className: "space-y-3 pt-4 border-t", children: [
        /* @__PURE__ */ jsxRuntimeExports.jsxs("div", { className: "grid grid-cols-2 gap-4 text-sm", children: [
          /* @__PURE__ */ jsxRuntimeExports.jsxs("div", { children: [
            /* @__PURE__ */ jsxRuntimeExports.jsx("p", { className: "text-gray-600", children: "Origin" }),
            /* @__PURE__ */ jsxRuntimeExports.jsxs("p", { className: "font-medium", children: [
              "(",
              origin.x,
              ", ",
              origin.y,
              ")"
            ] })
          ] }),
          /* @__PURE__ */ jsxRuntimeExports.jsxs("div", { children: [
            /* @__PURE__ */ jsxRuntimeExports.jsx("p", { className: "text-gray-600", children: "Destination" }),
            /* @__PURE__ */ jsxRuntimeExports.jsxs("p", { className: "font-medium", children: [
              "(",
              destination.x,
              ", ",
              destination.y,
              ")"
            ] })
          ] })
        ] }),
        currentLocation && /* @__PURE__ */ jsxRuntimeExports.jsxs("div", { className: "grid grid-cols-2 gap-4 text-sm", children: [
          /* @__PURE__ */ jsxRuntimeExports.jsxs("div", { children: [
            /* @__PURE__ */ jsxRuntimeExports.jsx("p", { className: "text-gray-600", children: "Current Position" }),
            /* @__PURE__ */ jsxRuntimeExports.jsxs("p", { className: "font-medium", children: [
              "(",
              currentLocation.x,
              ", ",
              currentLocation.y,
              ")"
            ] })
          ] }),
          /* @__PURE__ */ jsxRuntimeExports.jsxs("div", { children: [
            /* @__PURE__ */ jsxRuntimeExports.jsx("p", { className: "text-gray-600", children: "Progress" }),
            /* @__PURE__ */ jsxRuntimeExports.jsxs("div", { className: "flex items-center gap-2", children: [
              /* @__PURE__ */ jsxRuntimeExports.jsx("div", { className: "flex-1 bg-gray-200 rounded-full h-2", children: /* @__PURE__ */ jsxRuntimeExports.jsx(
                "div",
                {
                  className: "bg-blue-600 h-2 rounded-full transition-all duration-300",
                  style: { width: `${progress}%` }
                }
              ) }),
              /* @__PURE__ */ jsxRuntimeExports.jsxs("span", { className: "font-medium", children: [
                progress,
                "%"
              ] })
            ] })
          ] })
        ] }),
        /* @__PURE__ */ jsxRuntimeExports.jsxs("div", { className: "flex items-center justify-between text-sm", children: [
          /* @__PURE__ */ jsxRuntimeExports.jsxs("div", { children: [
            /* @__PURE__ */ jsxRuntimeExports.jsx("span", { className: "text-gray-600", children: "Total Distance: " }),
            /* @__PURE__ */ jsxRuntimeExports.jsxs("span", { className: "font-medium", children: [
              totalDistance.toFixed(1),
              " units"
            ] })
          ] }),
          truckStatus && /* @__PURE__ */ jsxRuntimeExports.jsxs(Badge, { variant: "outline", className: "flex items-center gap-1", children: [
            /* @__PURE__ */ jsxRuntimeExports.jsx(Truck, { className: "h-3 w-3" }),
            truckStatus
          ] })
        ] })
      ] })
    ] })
  ] });
};
function FlashOnUpdateComponent({ trigger, children, duration = 1500, className, ...props }, ref) {
  const [isFlashing, setIsFlashing] = reactExports.useState(false);
  const prevTrigger = reactExports.useRef(trigger);
  reactExports.useEffect(() => {
    if (prevTrigger.current !== trigger) {
      setIsFlashing(true);
      const timer = setTimeout(() => setIsFlashing(false), duration);
      prevTrigger.current = trigger;
      return () => clearTimeout(timer);
    }
  }, [trigger, duration]);
  return /* @__PURE__ */ jsxRuntimeExports.jsx(
    "div",
    {
      ref,
      className: cn(
        "transition-colors",
        isFlashing && "animate-flash",
        className
      ),
      style: {
        ...props.style,
        "--flash-duration": `${duration}ms`
      },
      ...props,
      children
    }
  );
}
const FlashOnUpdate = reactExports.forwardRef(FlashOnUpdateComponent);
FlashOnUpdateComponent.displayName = "FlashOnUpdate";
const useSocketStatus = () => {
  const [status, setStatus] = reactExports.useState(socketService.getStatus());
  reactExports.useEffect(() => {
    const unsubscribe = socketService.onStatusChange(setStatus);
    return unsubscribe;
  }, []);
  return {
    status,
    isConnected: status === "connected",
    isConnecting: status === "connecting",
    isDisconnected: status === "disconnected",
    hasError: status === "error"
  };
};
const useShipmentSubscription = (trackingNumber) => {
  reactExports.useEffect(() => {
    if (!trackingNumber) return;
    socketService.subscribeToShipment(trackingNumber);
    return () => {
      socketService.unsubscribeFromShipment(trackingNumber);
    };
  }, [trackingNumber]);
};
export {
  FlashOnUpdate as F,
  TrackingMap as T,
  useShipmentSubscription as a,
  TrackingTimeline as b,
  useSocketStatus as u
};
