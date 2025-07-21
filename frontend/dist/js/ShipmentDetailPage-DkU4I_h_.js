import { c as createLucideIcon, o as calculateDistance, j as jsxRuntimeExports, M as MapPin, T as Truck, B as Button, L as Link, t as toast, p as useParams, r as reactExports, P as Package, f as formatDateTime } from "./index-D-bA2lkg.js";
import { u as useQuery } from "./useQuery-ChBPFYRJ.js";
import { s as shipmentApi } from "./shipment-DiICAfSs.js";
import { u as useSocketStatus, a as useShipmentSubscription, F as FlashOnUpdate, T as TrackingMap, b as TrackingTimeline } from "./useShipmentSubscription-BGd_JxmS.js";
import { S as StatusIndicator } from "./status-indicator-S_BRd8a9.js";
import { C as Card, a as CardHeader, b as CardTitle, d as CardContent, c as CardDescription } from "./card-CsLEcWf3.js";
import { B as Badge } from "./badge-2PvS7lZW.js";
import { A as Alert, a as AlertDescription } from "./alert-Qw-zBtar.js";
import { A as ArrowLeft } from "./arrow-left-B7fHnPbY.js";
import { A as AlertCircle } from "./alert-circle-DRL_kTZp.js";
import { R as RefreshCw } from "./refresh-cw-DEEM45_N.js";
import "./check-DBMoG0qQ.js";
import "./clock-BBOft3LF.js";
/**
 * @license lucide-react v0.309.0 - ISC
 *
 * This source code is licensed under the ISC license.
 * See the LICENSE file in the root directory of this source tree.
 */
const Copy = createLucideIcon("Copy", [
  ["rect", { width: "14", height: "14", x: "8", y: "8", rx: "2", ry: "2", key: "17jyea" }],
  ["path", { d: "M4 16c-1.1 0-2-.9-2-2V4c0-1.1.9-2 2-2h10c1.1 0 2 .9 2 2", key: "zix9uf" }]
]);
/**
 * @license lucide-react v0.309.0 - ISC
 *
 * This source code is licensed under the ISC license.
 * See the LICENSE file in the root directory of this source tree.
 */
const ExternalLink = createLucideIcon("ExternalLink", [
  ["path", { d: "M18 13v6a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V8a2 2 0 0 1 2-2h6", key: "a6xqqp" }],
  ["polyline", { points: "15 3 21 3 21 9", key: "mznyad" }],
  ["line", { x1: "10", x2: "21", y1: "14", y2: "3", key: "18c3s4" }]
]);
const LocationInfoCard = ({
  origin,
  destination
}) => {
  const distance = calculateDistance(origin, destination);
  return /* @__PURE__ */ jsxRuntimeExports.jsxs(Card, { children: [
    /* @__PURE__ */ jsxRuntimeExports.jsx(CardHeader, { children: /* @__PURE__ */ jsxRuntimeExports.jsxs(CardTitle, { className: "flex items-center gap-2", children: [
      /* @__PURE__ */ jsxRuntimeExports.jsx(MapPin, { className: "h-5 w-5" }),
      "Location Information"
    ] }) }),
    /* @__PURE__ */ jsxRuntimeExports.jsx(CardContent, { children: /* @__PURE__ */ jsxRuntimeExports.jsxs("div", { className: "space-y-4", children: [
      /* @__PURE__ */ jsxRuntimeExports.jsxs("div", { children: [
        /* @__PURE__ */ jsxRuntimeExports.jsx("label", { className: "block text-sm font-medium text-gray-700 mb-1", children: "Origin" }),
        /* @__PURE__ */ jsxRuntimeExports.jsxs("p", { className: "text-sm text-gray-900", children: [
          "(",
          origin.x,
          ", ",
          origin.y,
          ")"
        ] })
      ] }),
      /* @__PURE__ */ jsxRuntimeExports.jsxs("div", { children: [
        /* @__PURE__ */ jsxRuntimeExports.jsx("label", { className: "block text-sm font-medium text-gray-700 mb-1", children: "Destination" }),
        /* @__PURE__ */ jsxRuntimeExports.jsxs("p", { className: "text-sm text-gray-900", children: [
          "(",
          destination.x,
          ", ",
          destination.y,
          ")"
        ] })
      ] }),
      /* @__PURE__ */ jsxRuntimeExports.jsx("div", { className: "pt-4 border-t", children: /* @__PURE__ */ jsxRuntimeExports.jsxs("div", { className: "flex items-center justify-between text-sm", children: [
        /* @__PURE__ */ jsxRuntimeExports.jsx("span", { className: "text-gray-600", children: "Distance" }),
        /* @__PURE__ */ jsxRuntimeExports.jsxs("span", { className: "font-medium", children: [
          distance.toFixed(1),
          " units"
        ] })
      ] }) })
    ] }) })
  ] });
};
const TruckInfoCard = ({ truck }) => {
  return /* @__PURE__ */ jsxRuntimeExports.jsxs(Card, { children: [
    /* @__PURE__ */ jsxRuntimeExports.jsx(CardHeader, { children: /* @__PURE__ */ jsxRuntimeExports.jsxs(CardTitle, { className: "flex items-center gap-2", children: [
      /* @__PURE__ */ jsxRuntimeExports.jsx(Truck, { className: "h-5 w-5" }),
      "Truck Information"
    ] }) }),
    /* @__PURE__ */ jsxRuntimeExports.jsx(CardContent, { children: /* @__PURE__ */ jsxRuntimeExports.jsxs("div", { className: "space-y-2", children: [
      /* @__PURE__ */ jsxRuntimeExports.jsxs("div", { className: "flex justify-between text-sm", children: [
        /* @__PURE__ */ jsxRuntimeExports.jsx("span", { className: "text-gray-600", children: "Truck ID" }),
        /* @__PURE__ */ jsxRuntimeExports.jsx("span", { className: "font-medium", children: truck.truck_id })
      ] }),
      /* @__PURE__ */ jsxRuntimeExports.jsxs("div", { className: "flex justify-between text-sm", children: [
        /* @__PURE__ */ jsxRuntimeExports.jsx("span", { className: "text-gray-600", children: "Status" }),
        /* @__PURE__ */ jsxRuntimeExports.jsx(Badge, { variant: "outline", children: truck.status })
      ] }),
      truck.current_location && /* @__PURE__ */ jsxRuntimeExports.jsxs("div", { className: "flex justify-between text-sm", children: [
        /* @__PURE__ */ jsxRuntimeExports.jsx("span", { className: "text-gray-600", children: "Current Location" }),
        /* @__PURE__ */ jsxRuntimeExports.jsxs("span", { className: "font-medium", children: [
          "(",
          truck.current_location.x,
          ", ",
          truck.current_location.y,
          ")"
        ] })
      ] })
    ] }) })
  ] });
};
const ActionsCard = ({ trackingNumber }) => {
  const copyTrackingNumber = () => {
    navigator.clipboard.writeText(trackingNumber);
    toast.success("Tracking number copied to clipboard");
  };
  return /* @__PURE__ */ jsxRuntimeExports.jsxs(Card, { children: [
    /* @__PURE__ */ jsxRuntimeExports.jsx(CardHeader, { children: /* @__PURE__ */ jsxRuntimeExports.jsx(CardTitle, { children: "Actions" }) }),
    /* @__PURE__ */ jsxRuntimeExports.jsx(CardContent, { children: /* @__PURE__ */ jsxRuntimeExports.jsxs("div", { className: "space-y-3", children: [
      /* @__PURE__ */ jsxRuntimeExports.jsxs(
        Button,
        {
          variant: "outline",
          className: "w-full justify-start",
          onClick: copyTrackingNumber,
          children: [
            /* @__PURE__ */ jsxRuntimeExports.jsx(Copy, { className: "h-4 w-4 mr-2" }),
            "Copy Tracking Number"
          ]
        }
      ),
      /* @__PURE__ */ jsxRuntimeExports.jsx(Link, { to: `/tracking?q=${trackingNumber}`, className: "block", children: /* @__PURE__ */ jsxRuntimeExports.jsxs(Button, { variant: "outline", className: "w-full justify-start", children: [
        /* @__PURE__ */ jsxRuntimeExports.jsx(ExternalLink, { className: "h-4 w-4 mr-2" }),
        "Public Tracking Page"
      ] }) })
    ] }) })
  ] });
};
const ShipmentDetailPage = () => {
  const { trackingNumber } = useParams();
  const { isConnected } = useSocketStatus();
  useShipmentSubscription(trackingNumber);
  const {
    data: shipment,
    isLoading,
    error: shipmentError,
    refetch: refetchShipment
  } = useQuery({
    queryKey: ["shipment", trackingNumber],
    queryFn: () => shipmentApi.getShipmentByTrackingNumber(trackingNumber),
    enabled: !!trackingNumber,
    refetchInterval: isConnected ? false : 3e4
    // Poll every 30s if WebSocket is down
  });
  reactExports.useEffect(() => {
    if (shipmentError) {
      console.error("Failed to fetch shipment details:", shipmentError);
      toast.error("Failed to load shipment details");
    }
  }, [shipmentError]);
  const {
    data: historyData,
    isLoading: historyLoading,
    refetch: refetchHistory
  } = useQuery({
    queryKey: ["trackingHistory", trackingNumber],
    queryFn: () => shipmentApi.getTrackingHistory(trackingNumber),
    enabled: !!trackingNumber,
    refetchInterval: isConnected ? false : 3e4
    // Poll every 30s if WebSocket is down
  });
  const history = historyData?.history || [];
  const loading = isLoading || historyLoading;
  shipmentError?.message;
  const handleRefresh = async () => {
    await Promise.all([refetchShipment(), refetchHistory()]);
    toast.success("Data refreshed");
  };
  const copyTrackingNumber = () => {
    if (trackingNumber) {
      navigator.clipboard.writeText(trackingNumber);
      toast.success("Tracking number copied to clipboard");
    }
  };
  if (loading) {
    return /* @__PURE__ */ jsxRuntimeExports.jsx("div", { className: "p-6 max-w-6xl mx-auto", children: /* @__PURE__ */ jsxRuntimeExports.jsxs("div", { className: "animate-pulse", children: [
      /* @__PURE__ */ jsxRuntimeExports.jsx("div", { className: "h-8 bg-gray-200 rounded w-1/4 mb-4" }),
      /* @__PURE__ */ jsxRuntimeExports.jsxs("div", { className: "grid gap-6 lg:grid-cols-3", children: [
        /* @__PURE__ */ jsxRuntimeExports.jsxs("div", { className: "lg:col-span-2 space-y-6", children: [
          /* @__PURE__ */ jsxRuntimeExports.jsx("div", { className: "h-64 bg-gray-200 rounded" }),
          /* @__PURE__ */ jsxRuntimeExports.jsx("div", { className: "h-96 bg-gray-200 rounded" })
        ] }),
        /* @__PURE__ */ jsxRuntimeExports.jsxs("div", { className: "space-y-6", children: [
          /* @__PURE__ */ jsxRuntimeExports.jsx("div", { className: "h-48 bg-gray-200 rounded" }),
          /* @__PURE__ */ jsxRuntimeExports.jsx("div", { className: "h-32 bg-gray-200 rounded" })
        ] })
      ] })
    ] }) });
  }
  if (shipmentError || !shipment) {
    return /* @__PURE__ */ jsxRuntimeExports.jsxs("div", { className: "p-6 max-w-6xl mx-auto", children: [
      /* @__PURE__ */ jsxRuntimeExports.jsx("div", { className: "mb-6", children: /* @__PURE__ */ jsxRuntimeExports.jsxs(Link, { to: "/shipments", className: "inline-flex items-center text-blue-600 hover:text-blue-800", children: [
        /* @__PURE__ */ jsxRuntimeExports.jsx(ArrowLeft, { className: "h-4 w-4 mr-2" }),
        "Back to Shipments"
      ] }) }),
      /* @__PURE__ */ jsxRuntimeExports.jsxs(Alert, { variant: "destructive", children: [
        /* @__PURE__ */ jsxRuntimeExports.jsx(AlertCircle, { className: "h-4 w-4" }),
        /* @__PURE__ */ jsxRuntimeExports.jsx(AlertDescription, { children: shipmentError?.message || "Shipment not found" })
      ] })
    ] });
  }
  const typedShipment = shipment;
  return /* @__PURE__ */ jsxRuntimeExports.jsxs("div", { className: "p-6 max-w-6xl mx-auto", children: [
    /* @__PURE__ */ jsxRuntimeExports.jsx("div", { className: "mb-6", children: /* @__PURE__ */ jsxRuntimeExports.jsxs("div", { className: "flex items-center justify-between", children: [
      /* @__PURE__ */ jsxRuntimeExports.jsx("div", { className: "flex items-center space-x-4", children: /* @__PURE__ */ jsxRuntimeExports.jsxs(Link, { to: "/shipments", className: "inline-flex items-center text-blue-600 hover:text-blue-800", children: [
        /* @__PURE__ */ jsxRuntimeExports.jsx(ArrowLeft, { className: "h-4 w-4 mr-2" }),
        "Back to Shipments"
      ] }) }),
      /* @__PURE__ */ jsxRuntimeExports.jsxs(
        Button,
        {
          variant: "outline",
          size: "sm",
          onClick: handleRefresh,
          disabled: loading,
          children: [
            /* @__PURE__ */ jsxRuntimeExports.jsx(RefreshCw, { className: `h-4 w-4 mr-2 ${loading ? "animate-spin" : ""}` }),
            "Refresh"
          ]
        }
      )
    ] }) }),
    /* @__PURE__ */ jsxRuntimeExports.jsxs("div", { className: "mb-8", children: [
      /* @__PURE__ */ jsxRuntimeExports.jsxs("div", { className: "flex items-center gap-4 mb-2", children: [
        /* @__PURE__ */ jsxRuntimeExports.jsx("h1", { className: "text-3xl font-bold text-gray-900", children: "Shipment Details" }),
        /* @__PURE__ */ jsxRuntimeExports.jsx(FlashOnUpdate, { trigger: typedShipment.status, className: "transition-colors duration-300", children: /* @__PURE__ */ jsxRuntimeExports.jsx(
          StatusIndicator,
          {
            status: typedShipment.status,
            showDot: true,
            animated: !["DELIVERED", "CANCELLED", "RETURNED"].includes(typedShipment.status)
          }
        ) })
      ] }),
      /* @__PURE__ */ jsxRuntimeExports.jsxs("div", { className: "flex items-center gap-4", children: [
        /* @__PURE__ */ jsxRuntimeExports.jsxs("div", { className: "flex items-center gap-2", children: [
          /* @__PURE__ */ jsxRuntimeExports.jsx("span", { className: "text-lg font-mono text-gray-600", children: typedShipment.tracking_number }),
          /* @__PURE__ */ jsxRuntimeExports.jsx(
            Button,
            {
              variant: "ghost",
              size: "sm",
              onClick: copyTrackingNumber,
              className: "p-1 h-auto",
              children: /* @__PURE__ */ jsxRuntimeExports.jsx(Copy, { className: "h-4 w-4" })
            }
          )
        ] }),
        /* @__PURE__ */ jsxRuntimeExports.jsx(Link, { to: `/tracking?q=${typedShipment.tracking_number}`, className: "text-blue-600 hover:text-blue-800", children: /* @__PURE__ */ jsxRuntimeExports.jsx(ExternalLink, { className: "h-4 w-4" }) })
      ] })
    ] }),
    /* @__PURE__ */ jsxRuntimeExports.jsxs("div", { className: "grid gap-6 lg:grid-cols-3", children: [
      /* @__PURE__ */ jsxRuntimeExports.jsxs("div", { className: "lg:col-span-2 space-y-6", children: [
        /* @__PURE__ */ jsxRuntimeExports.jsxs(Card, { children: [
          /* @__PURE__ */ jsxRuntimeExports.jsx(CardHeader, { children: /* @__PURE__ */ jsxRuntimeExports.jsxs(CardTitle, { className: "flex items-center gap-2", children: [
            /* @__PURE__ */ jsxRuntimeExports.jsx(Package, { className: "h-5 w-5" }),
            "Shipment Overview"
          ] }) }),
          /* @__PURE__ */ jsxRuntimeExports.jsx(CardContent, { children: /* @__PURE__ */ jsxRuntimeExports.jsxs("div", { className: "grid gap-4 md:grid-cols-2", children: [
            /* @__PURE__ */ jsxRuntimeExports.jsxs("div", { children: [
              /* @__PURE__ */ jsxRuntimeExports.jsx("label", { className: "block text-sm font-medium text-gray-700 mb-1", children: "Shipment ID" }),
              /* @__PURE__ */ jsxRuntimeExports.jsx("p", { className: "text-sm text-gray-900", children: typedShipment.shipment_id })
            ] }),
            /* @__PURE__ */ jsxRuntimeExports.jsxs("div", { children: [
              /* @__PURE__ */ jsxRuntimeExports.jsx("label", { className: "block text-sm font-medium text-gray-700 mb-1", children: "Status" }),
              /* @__PURE__ */ jsxRuntimeExports.jsx("p", { className: "text-sm text-gray-900", children: typedShipment.status_display })
            ] }),
            /* @__PURE__ */ jsxRuntimeExports.jsxs("div", { children: [
              /* @__PURE__ */ jsxRuntimeExports.jsx("label", { className: "block text-sm font-medium text-gray-700 mb-1", children: "Created" }),
              /* @__PURE__ */ jsxRuntimeExports.jsx("p", { className: "text-sm text-gray-900", children: formatDateTime(typedShipment.created_at) })
            ] }),
            /* @__PURE__ */ jsxRuntimeExports.jsxs("div", { children: [
              /* @__PURE__ */ jsxRuntimeExports.jsx("label", { className: "block text-sm font-medium text-gray-700 mb-1", children: "Estimated Delivery" }),
              /* @__PURE__ */ jsxRuntimeExports.jsx("p", { className: "text-sm text-gray-900", children: typedShipment.estimated_delivery ? formatDateTime(typedShipment.estimated_delivery) : "Not available" })
            ] }),
            typedShipment.actual_delivery && /* @__PURE__ */ jsxRuntimeExports.jsxs("div", { children: [
              /* @__PURE__ */ jsxRuntimeExports.jsx("label", { className: "block text-sm font-medium text-gray-700 mb-1", children: "Actual Delivery" }),
              /* @__PURE__ */ jsxRuntimeExports.jsx("p", { className: "text-sm text-gray-900", children: formatDateTime(typedShipment.actual_delivery) })
            ] }),
            typedShipment.pickup_time && /* @__PURE__ */ jsxRuntimeExports.jsxs("div", { children: [
              /* @__PURE__ */ jsxRuntimeExports.jsx("label", { className: "block text-sm font-medium text-gray-700 mb-1", children: "Pickup Time" }),
              /* @__PURE__ */ jsxRuntimeExports.jsx("p", { className: "text-sm text-gray-900", children: formatDateTime(typedShipment.pickup_time) })
            ] })
          ] }) })
        ] }),
        /* @__PURE__ */ jsxRuntimeExports.jsx(
          TrackingMap,
          {
            origin: typedShipment.origin,
            destination: typedShipment.destination,
            currentLocation: typedShipment.truck?.current_location,
            truckStatus: typedShipment.truck?.status
          }
        ),
        /* @__PURE__ */ jsxRuntimeExports.jsxs(Card, { children: [
          /* @__PURE__ */ jsxRuntimeExports.jsxs(CardHeader, { children: [
            /* @__PURE__ */ jsxRuntimeExports.jsxs(CardTitle, { className: "flex items-center gap-2", children: [
              /* @__PURE__ */ jsxRuntimeExports.jsx(MapPin, { className: "h-5 w-5" }),
              "Tracking Timeline"
            ] }),
            /* @__PURE__ */ jsxRuntimeExports.jsx(CardDescription, { children: "Follow your shipment's journey from pickup to delivery" })
          ] }),
          /* @__PURE__ */ jsxRuntimeExports.jsx(CardContent, { children: /* @__PURE__ */ jsxRuntimeExports.jsx(
            TrackingTimeline,
            {
              history,
              currentStatus: typedShipment.status,
              loading: historyLoading
            }
          ) })
        ] })
      ] }),
      /* @__PURE__ */ jsxRuntimeExports.jsxs("div", { className: "space-y-6", children: [
        /* @__PURE__ */ jsxRuntimeExports.jsx(
          LocationInfoCard,
          {
            origin: typedShipment.origin,
            destination: typedShipment.destination
          }
        ),
        typedShipment.truck && /* @__PURE__ */ jsxRuntimeExports.jsx(TruckInfoCard, { truck: typedShipment.truck }),
        /* @__PURE__ */ jsxRuntimeExports.jsx(ActionsCard, { trackingNumber: typedShipment.tracking_number })
      ] })
    ] })
  ] });
};
export {
  ShipmentDetailPage
};
