import { c as createLucideIcon, d as useSearchParams, r as reactExports, j as jsxRuntimeExports, P as Package, B as Button, S as Search, f as formatDateTime, M as MapPin } from "./index-D-bA2lkg.js";
import { u as useQuery } from "./useQuery-ChBPFYRJ.js";
import { s as shipmentApi } from "./shipment-DiICAfSs.js";
import { u as useSocketStatus, a as useShipmentSubscription, F as FlashOnUpdate, T as TrackingMap, b as TrackingTimeline } from "./useShipmentSubscription-BGd_JxmS.js";
import { S as StatusIndicator } from "./status-indicator-S_BRd8a9.js";
import { I as Input } from "./input-BHuFzm78.js";
import { C as Card, d as CardContent, a as CardHeader, b as CardTitle, c as CardDescription } from "./card-CsLEcWf3.js";
import { A as Alert, a as AlertDescription } from "./alert-Qw-zBtar.js";
import { B as Badge } from "./badge-2PvS7lZW.js";
import { A as AlertCircle } from "./alert-circle-DRL_kTZp.js";
import { C as CheckCircle } from "./check-circle-9Vi05KqK.js";
import "./check-DBMoG0qQ.js";
import "./clock-BBOft3LF.js";
/**
 * @license lucide-react v0.309.0 - ISC
 *
 * This source code is licensed under the ISC license.
 * See the LICENSE file in the root directory of this source tree.
 */
const WifiOff = createLucideIcon("WifiOff", [
  ["line", { x1: "2", x2: "22", y1: "2", y2: "22", key: "a6p6uj" }],
  ["path", { d: "M8.5 16.5a5 5 0 0 1 7 0", key: "sej527" }],
  ["path", { d: "M2 8.82a15 15 0 0 1 4.17-2.65", key: "11utq1" }],
  ["path", { d: "M10.66 5c4.01-.36 8.14.9 11.34 3.76", key: "hxefdu" }],
  ["path", { d: "M16.85 11.25a10 10 0 0 1 2.22 1.68", key: "q734kn" }],
  ["path", { d: "M5 13a10 10 0 0 1 5.24-2.76", key: "piq4yl" }],
  ["line", { x1: "12", x2: "12.01", y1: "20", y2: "20", key: "of4bc4" }]
]);
/**
 * @license lucide-react v0.309.0 - ISC
 *
 * This source code is licensed under the ISC license.
 * See the LICENSE file in the root directory of this source tree.
 */
const Wifi = createLucideIcon("Wifi", [
  ["path", { d: "M5 13a10 10 0 0 1 14 0", key: "6v8j51" }],
  ["path", { d: "M8.5 16.5a5 5 0 0 1 7 0", key: "sej527" }],
  ["path", { d: "M2 8.82a15 15 0 0 1 20 0", key: "dnpr2z" }],
  ["line", { x1: "12", x2: "12.01", y1: "20", y2: "20", key: "of4bc4" }]
]);
const TrackingPage = () => {
  const [searchParams, setSearchParams] = useSearchParams();
  const [trackingNumber, setTrackingNumber] = reactExports.useState(searchParams.get("q") || "");
  const [searchedTrackingNumber, setSearchedTrackingNumber] = reactExports.useState(searchParams.get("q"));
  const [validationError, setValidationError] = reactExports.useState(null);
  const { isConnected, hasError: socketError } = useSocketStatus();
  useShipmentSubscription(searchedTrackingNumber || void 0);
  const {
    data: shipment,
    isLoading: shipmentLoading,
    error: shipmentError,
    refetch: refetchShipment
  } = useQuery({
    queryKey: ["shipment", searchedTrackingNumber],
    queryFn: () => shipmentApi.getShipmentByTrackingNumber(searchedTrackingNumber),
    enabled: !!searchedTrackingNumber,
    refetchInterval: isConnected ? false : 3e4,
    // Poll every 30s if WebSocket is down
    retry: 2
  });
  const {
    data: historyData,
    isLoading: historyLoading,
    refetch: refetchHistory
  } = useQuery({
    queryKey: ["trackingHistory", searchedTrackingNumber],
    queryFn: () => shipmentApi.getTrackingHistory(searchedTrackingNumber),
    enabled: !!searchedTrackingNumber,
    refetchInterval: isConnected ? false : 3e4,
    // Poll every 30s if WebSocket is down
    retry: 2
  });
  const history = historyData?.history || [];
  const loading = shipmentLoading || historyLoading;
  const error = validationError || shipmentError?.message;
  const hasSearched = !!searchedTrackingNumber;
  const validateAndSearch = async (trackingNum) => {
    if (!trackingNum.trim()) {
      setValidationError("Please enter a tracking number");
      return;
    }
    try {
      setValidationError(null);
      const validation = await shipmentApi.validateTrackingNumber(trackingNum.trim());
      if (!validation.valid) {
        setValidationError("Invalid tracking number format");
        setSearchedTrackingNumber(null);
        return;
      }
      setSearchedTrackingNumber(trackingNum.trim());
      setSearchParams({ q: trackingNum.trim() });
    } catch (err) {
      console.error("Failed to validate tracking number:", err);
      setValidationError("Failed to validate tracking number");
      setSearchedTrackingNumber(null);
    }
  };
  const handleSubmit = (e) => {
    e.preventDefault();
    validateAndSearch(trackingNumber);
  };
  const handleInputChange = (e) => {
    setTrackingNumber(e.target.value);
    setValidationError(null);
  };
  reactExports.useEffect(() => {
    const urlTrackingNumber = searchParams.get("q");
    if (urlTrackingNumber && !searchedTrackingNumber) {
      validateAndSearch(urlTrackingNumber);
    }
  }, [searchParams, searchedTrackingNumber]);
  return /* @__PURE__ */ jsxRuntimeExports.jsxs("div", { className: "min-h-screen bg-gray-50", children: [
    /* @__PURE__ */ jsxRuntimeExports.jsx("div", { className: "bg-gradient-to-br from-blue-600 to-indigo-700 text-white", children: /* @__PURE__ */ jsxRuntimeExports.jsxs("div", { className: "max-w-4xl mx-auto px-6 py-16", children: [
      /* @__PURE__ */ jsxRuntimeExports.jsxs("div", { className: "text-center mb-8", children: [
        /* @__PURE__ */ jsxRuntimeExports.jsx(Package, { className: "h-16 w-16 mx-auto mb-4" }),
        /* @__PURE__ */ jsxRuntimeExports.jsx("h1", { className: "text-4xl font-bold mb-4", children: "Track Your Package" }),
        /* @__PURE__ */ jsxRuntimeExports.jsx("p", { className: "text-xl text-blue-100", children: "Enter your tracking number to get real-time updates on your shipment" })
      ] }),
      /* @__PURE__ */ jsxRuntimeExports.jsxs("div", { className: "max-w-2xl mx-auto", children: [
        /* @__PURE__ */ jsxRuntimeExports.jsxs("form", { onSubmit: handleSubmit, className: "flex gap-4", children: [
          /* @__PURE__ */ jsxRuntimeExports.jsx("div", { className: "flex-1", children: /* @__PURE__ */ jsxRuntimeExports.jsx(
            Input,
            {
              type: "text",
              placeholder: "Enter tracking number (e.g., UPS123456789)",
              value: trackingNumber,
              onChange: handleInputChange,
              className: "text-lg py-3 bg-white text-gray-900 border-none focus:ring-2 focus:ring-blue-300"
            }
          ) }),
          /* @__PURE__ */ jsxRuntimeExports.jsx(
            Button,
            {
              type: "submit",
              size: "lg",
              disabled: loading,
              className: "px-8 bg-white text-blue-600 hover:bg-gray-100 border-none",
              children: loading ? /* @__PURE__ */ jsxRuntimeExports.jsx("div", { className: "animate-spin rounded-full h-5 w-5 border-b-2 border-blue-600" }) : /* @__PURE__ */ jsxRuntimeExports.jsxs(jsxRuntimeExports.Fragment, { children: [
                /* @__PURE__ */ jsxRuntimeExports.jsx(Search, { className: "h-5 w-5 mr-2" }),
                "Track"
              ] })
            }
          )
        ] }),
        hasSearched && /* @__PURE__ */ jsxRuntimeExports.jsx("div", { className: "flex justify-center mt-4", children: /* @__PURE__ */ jsxRuntimeExports.jsx("div", { className: `flex items-center gap-2 px-3 py-1 rounded-full text-sm ${isConnected ? "bg-green-100 text-green-800" : socketError ? "bg-red-100 text-red-800" : "bg-yellow-100 text-yellow-800"}`, children: isConnected ? /* @__PURE__ */ jsxRuntimeExports.jsxs(jsxRuntimeExports.Fragment, { children: [
          /* @__PURE__ */ jsxRuntimeExports.jsx(Wifi, { className: "h-3 w-3" }),
          "Real-time updates active"
        ] }) : socketError ? /* @__PURE__ */ jsxRuntimeExports.jsxs(jsxRuntimeExports.Fragment, { children: [
          /* @__PURE__ */ jsxRuntimeExports.jsx(WifiOff, { className: "h-3 w-3" }),
          "Connection failed - Using periodic updates"
        ] }) : /* @__PURE__ */ jsxRuntimeExports.jsxs(jsxRuntimeExports.Fragment, { children: [
          /* @__PURE__ */ jsxRuntimeExports.jsx(WifiOff, { className: "h-3 w-3" }),
          "Real-time unavailable - Using periodic updates"
        ] }) }) })
      ] })
    ] }) }),
    /* @__PURE__ */ jsxRuntimeExports.jsxs("div", { className: "max-w-6xl mx-auto px-6 py-8", children: [
      error && /* @__PURE__ */ jsxRuntimeExports.jsxs(Alert, { variant: "destructive", className: "mb-6", children: [
        /* @__PURE__ */ jsxRuntimeExports.jsx(AlertCircle, { className: "h-4 w-4" }),
        /* @__PURE__ */ jsxRuntimeExports.jsx(AlertDescription, { children: error })
      ] }),
      loading && /* @__PURE__ */ jsxRuntimeExports.jsxs("div", { className: "grid gap-6 lg:grid-cols-3", children: [
        /* @__PURE__ */ jsxRuntimeExports.jsxs("div", { className: "lg:col-span-2 space-y-6", children: [
          /* @__PURE__ */ jsxRuntimeExports.jsx(Card, { className: "animate-pulse", children: /* @__PURE__ */ jsxRuntimeExports.jsxs(CardContent, { className: "p-6", children: [
            /* @__PURE__ */ jsxRuntimeExports.jsx("div", { className: "h-6 bg-gray-200 rounded w-1/4 mb-4" }),
            /* @__PURE__ */ jsxRuntimeExports.jsxs("div", { className: "space-y-3", children: [
              /* @__PURE__ */ jsxRuntimeExports.jsx("div", { className: "h-4 bg-gray-200 rounded w-3/4" }),
              /* @__PURE__ */ jsxRuntimeExports.jsx("div", { className: "h-4 bg-gray-200 rounded w-1/2" })
            ] })
          ] }) }),
          /* @__PURE__ */ jsxRuntimeExports.jsx(Card, { className: "animate-pulse", children: /* @__PURE__ */ jsxRuntimeExports.jsx(CardContent, { className: "p-6", children: /* @__PURE__ */ jsxRuntimeExports.jsx("div", { className: "h-96 bg-gray-200 rounded" }) }) })
        ] }),
        /* @__PURE__ */ jsxRuntimeExports.jsx("div", { className: "space-y-6", children: /* @__PURE__ */ jsxRuntimeExports.jsx(Card, { className: "animate-pulse", children: /* @__PURE__ */ jsxRuntimeExports.jsx(CardContent, { className: "p-6", children: /* @__PURE__ */ jsxRuntimeExports.jsx("div", { className: "h-48 bg-gray-200 rounded" }) }) }) })
      ] }),
      !loading && shipment && /* @__PURE__ */ jsxRuntimeExports.jsxs("div", { className: "grid gap-6 lg:grid-cols-3", children: [
        /* @__PURE__ */ jsxRuntimeExports.jsxs("div", { className: "lg:col-span-2 space-y-6", children: [
          /* @__PURE__ */ jsxRuntimeExports.jsxs(Card, { children: [
            /* @__PURE__ */ jsxRuntimeExports.jsx(CardHeader, { children: /* @__PURE__ */ jsxRuntimeExports.jsxs("div", { className: "flex items-center justify-between", children: [
              /* @__PURE__ */ jsxRuntimeExports.jsx(CardTitle, { className: "text-2xl", children: "Package Status" }),
              /* @__PURE__ */ jsxRuntimeExports.jsx(FlashOnUpdate, { trigger: shipment.status, className: "transition-colors duration-300", children: /* @__PURE__ */ jsxRuntimeExports.jsx(
                StatusIndicator,
                {
                  status: shipment.status,
                  showDot: true,
                  showDescription: true,
                  animated: !["DELIVERED", "CANCELLED", "RETURNED"].includes(shipment.status)
                }
              ) })
            ] }) }),
            /* @__PURE__ */ jsxRuntimeExports.jsx(CardContent, { children: /* @__PURE__ */ jsxRuntimeExports.jsxs("div", { className: "grid gap-4 md:grid-cols-2", children: [
              /* @__PURE__ */ jsxRuntimeExports.jsxs("div", { children: [
                /* @__PURE__ */ jsxRuntimeExports.jsx("label", { className: "block text-sm font-medium text-gray-700 mb-1", children: "Tracking Number" }),
                /* @__PURE__ */ jsxRuntimeExports.jsx("p", { className: "text-lg font-mono text-gray-900", children: shipment.tracking_number })
              ] }),
              /* @__PURE__ */ jsxRuntimeExports.jsxs("div", { children: [
                /* @__PURE__ */ jsxRuntimeExports.jsx("label", { className: "block text-sm font-medium text-gray-700 mb-1", children: "Current Status" }),
                /* @__PURE__ */ jsxRuntimeExports.jsx("p", { className: "text-lg text-gray-900", children: shipment.status_display })
              ] }),
              /* @__PURE__ */ jsxRuntimeExports.jsxs("div", { children: [
                /* @__PURE__ */ jsxRuntimeExports.jsx("label", { className: "block text-sm font-medium text-gray-700 mb-1", children: "Shipped Date" }),
                /* @__PURE__ */ jsxRuntimeExports.jsx("p", { className: "text-sm text-gray-900", children: formatDateTime(shipment.created_at) })
              ] }),
              /* @__PURE__ */ jsxRuntimeExports.jsxs("div", { children: [
                /* @__PURE__ */ jsxRuntimeExports.jsx("label", { className: "block text-sm font-medium text-gray-700 mb-1", children: "Estimated Delivery" }),
                /* @__PURE__ */ jsxRuntimeExports.jsx("p", { className: "text-sm text-gray-900", children: shipment.estimated_delivery ? formatDateTime(shipment.estimated_delivery) : "Calculating..." })
              ] }),
              shipment.actual_delivery && /* @__PURE__ */ jsxRuntimeExports.jsxs("div", { className: "md:col-span-2", children: [
                /* @__PURE__ */ jsxRuntimeExports.jsx("label", { className: "block text-sm font-medium text-gray-700 mb-1", children: "Delivered" }),
                /* @__PURE__ */ jsxRuntimeExports.jsxs("div", { className: "flex items-center gap-2", children: [
                  /* @__PURE__ */ jsxRuntimeExports.jsx(CheckCircle, { className: "h-5 w-5 text-green-600" }),
                  /* @__PURE__ */ jsxRuntimeExports.jsx("p", { className: "text-lg text-green-600 font-semibold", children: formatDateTime(shipment.actual_delivery) })
                ] })
              ] })
            ] }) })
          ] }),
          /* @__PURE__ */ jsxRuntimeExports.jsx(
            TrackingMap,
            {
              origin: shipment.origin,
              destination: shipment.destination,
              currentLocation: shipment.truck ? { x: shipment.origin.x + 20, y: shipment.origin.y + 15 } : void 0,
              truckStatus: shipment.truck?.status
            }
          ),
          /* @__PURE__ */ jsxRuntimeExports.jsxs(Card, { children: [
            /* @__PURE__ */ jsxRuntimeExports.jsxs(CardHeader, { children: [
              /* @__PURE__ */ jsxRuntimeExports.jsxs(CardTitle, { className: "flex items-center gap-2", children: [
                /* @__PURE__ */ jsxRuntimeExports.jsx(MapPin, { className: "h-5 w-5" }),
                "Tracking History"
              ] }),
              /* @__PURE__ */ jsxRuntimeExports.jsx(CardDescription, { children: "Follow your package's journey from pickup to delivery" })
            ] }),
            /* @__PURE__ */ jsxRuntimeExports.jsx(CardContent, { children: /* @__PURE__ */ jsxRuntimeExports.jsx(
              TrackingTimeline,
              {
                history,
                currentStatus: shipment.status,
                loading: false
              }
            ) })
          ] })
        ] }),
        /* @__PURE__ */ jsxRuntimeExports.jsxs("div", { className: "space-y-6", children: [
          /* @__PURE__ */ jsxRuntimeExports.jsxs(Card, { children: [
            /* @__PURE__ */ jsxRuntimeExports.jsx(CardHeader, { children: /* @__PURE__ */ jsxRuntimeExports.jsxs(CardTitle, { className: "flex items-center gap-2", children: [
              /* @__PURE__ */ jsxRuntimeExports.jsx(MapPin, { className: "h-5 w-5" }),
              "Delivery Details"
            ] }) }),
            /* @__PURE__ */ jsxRuntimeExports.jsx(CardContent, { children: /* @__PURE__ */ jsxRuntimeExports.jsxs("div", { className: "space-y-4", children: [
              /* @__PURE__ */ jsxRuntimeExports.jsxs("div", { children: [
                /* @__PURE__ */ jsxRuntimeExports.jsx("label", { className: "block text-sm font-medium text-gray-700 mb-1", children: "Origin" }),
                /* @__PURE__ */ jsxRuntimeExports.jsxs("p", { className: "text-sm text-gray-900", children: [
                  "(",
                  shipment.origin.x,
                  ", ",
                  shipment.origin.y,
                  ")"
                ] })
              ] }),
              /* @__PURE__ */ jsxRuntimeExports.jsxs("div", { children: [
                /* @__PURE__ */ jsxRuntimeExports.jsx("label", { className: "block text-sm font-medium text-gray-700 mb-1", children: "Destination" }),
                /* @__PURE__ */ jsxRuntimeExports.jsxs("p", { className: "text-sm text-gray-900", children: [
                  "(",
                  shipment.destination.x,
                  ", ",
                  shipment.destination.y,
                  ")"
                ] })
              ] }),
              /* @__PURE__ */ jsxRuntimeExports.jsx("div", { className: "pt-4 border-t", children: /* @__PURE__ */ jsxRuntimeExports.jsxs("div", { className: "flex items-center justify-between text-sm", children: [
                /* @__PURE__ */ jsxRuntimeExports.jsx("span", { className: "text-gray-600", children: "Distance" }),
                /* @__PURE__ */ jsxRuntimeExports.jsxs("span", { className: "font-medium", children: [
                  Math.sqrt(
                    Math.pow(shipment.destination.x - shipment.origin.x, 2) + Math.pow(shipment.destination.y - shipment.origin.y, 2)
                  ).toFixed(1),
                  " units"
                ] })
              ] }) })
            ] }) })
          ] }),
          /* @__PURE__ */ jsxRuntimeExports.jsxs(Card, { children: [
            /* @__PURE__ */ jsxRuntimeExports.jsx(CardHeader, { children: /* @__PURE__ */ jsxRuntimeExports.jsx(CardTitle, { children: "Service Information" }) }),
            /* @__PURE__ */ jsxRuntimeExports.jsx(CardContent, { children: /* @__PURE__ */ jsxRuntimeExports.jsxs("div", { className: "space-y-3", children: [
              /* @__PURE__ */ jsxRuntimeExports.jsxs("div", { className: "flex justify-between text-sm", children: [
                /* @__PURE__ */ jsxRuntimeExports.jsx("span", { className: "text-gray-600", children: "Service Type" }),
                /* @__PURE__ */ jsxRuntimeExports.jsx("span", { className: "font-medium", children: "Standard Delivery" })
              ] }),
              /* @__PURE__ */ jsxRuntimeExports.jsxs("div", { className: "flex justify-between text-sm", children: [
                /* @__PURE__ */ jsxRuntimeExports.jsx("span", { className: "text-gray-600", children: "Shipment ID" }),
                /* @__PURE__ */ jsxRuntimeExports.jsx("span", { className: "font-medium", children: shipment.shipment_id })
              ] }),
              shipment.truck && /* @__PURE__ */ jsxRuntimeExports.jsxs("div", { className: "flex justify-between text-sm", children: [
                /* @__PURE__ */ jsxRuntimeExports.jsx("span", { className: "text-gray-600", children: "Truck" }),
                /* @__PURE__ */ jsxRuntimeExports.jsxs(Badge, { variant: "outline", children: [
                  "#",
                  shipment.truck.truck_id
                ] })
              ] })
            ] }) })
          ] }),
          /* @__PURE__ */ jsxRuntimeExports.jsxs(Card, { children: [
            /* @__PURE__ */ jsxRuntimeExports.jsx(CardHeader, { children: /* @__PURE__ */ jsxRuntimeExports.jsx(CardTitle, { children: "Need Help?" }) }),
            /* @__PURE__ */ jsxRuntimeExports.jsx(CardContent, { children: /* @__PURE__ */ jsxRuntimeExports.jsxs("div", { className: "space-y-3 text-sm", children: [
              /* @__PURE__ */ jsxRuntimeExports.jsx("p", { className: "text-gray-600", children: "If you have questions about your shipment, please contact our customer service." }),
              /* @__PURE__ */ jsxRuntimeExports.jsxs("div", { className: "space-y-2", children: [
                /* @__PURE__ */ jsxRuntimeExports.jsxs("div", { className: "flex justify-between", children: [
                  /* @__PURE__ */ jsxRuntimeExports.jsx("span", { className: "text-gray-600", children: "Phone" }),
                  /* @__PURE__ */ jsxRuntimeExports.jsx("span", { className: "font-medium", children: "1-800-MINI-UPS" })
                ] }),
                /* @__PURE__ */ jsxRuntimeExports.jsxs("div", { className: "flex justify-between", children: [
                  /* @__PURE__ */ jsxRuntimeExports.jsx("span", { className: "text-gray-600", children: "Email" }),
                  /* @__PURE__ */ jsxRuntimeExports.jsx("span", { className: "font-medium", children: "support@mini-ups.com" })
                ] })
              ] })
            ] }) })
          ] })
        ] })
      ] }),
      !loading && !shipment && hasSearched && !error && /* @__PURE__ */ jsxRuntimeExports.jsx(Card, { children: /* @__PURE__ */ jsxRuntimeExports.jsxs(CardContent, { className: "text-center py-12", children: [
        /* @__PURE__ */ jsxRuntimeExports.jsx(Package, { className: "h-12 w-12 mx-auto mb-4 text-gray-400" }),
        /* @__PURE__ */ jsxRuntimeExports.jsx("h3", { className: "text-lg font-medium text-gray-900 mb-2", children: "No results found" }),
        /* @__PURE__ */ jsxRuntimeExports.jsx("p", { className: "text-gray-600 mb-4", children: "We couldn't find a package with that tracking number. Please check the number and try again." })
      ] }) }),
      !hasSearched && /* @__PURE__ */ jsxRuntimeExports.jsxs(Card, { children: [
        /* @__PURE__ */ jsxRuntimeExports.jsx(CardHeader, { children: /* @__PURE__ */ jsxRuntimeExports.jsx(CardTitle, { children: "How to Track Your Package" }) }),
        /* @__PURE__ */ jsxRuntimeExports.jsx(CardContent, { children: /* @__PURE__ */ jsxRuntimeExports.jsxs("div", { className: "grid gap-6 md:grid-cols-3", children: [
          /* @__PURE__ */ jsxRuntimeExports.jsxs("div", { className: "text-center", children: [
            /* @__PURE__ */ jsxRuntimeExports.jsx("div", { className: "w-12 h-12 bg-blue-100 rounded-full flex items-center justify-center mx-auto mb-3", children: /* @__PURE__ */ jsxRuntimeExports.jsx(Search, { className: "h-6 w-6 text-blue-600" }) }),
            /* @__PURE__ */ jsxRuntimeExports.jsx("h3", { className: "font-semibold mb-2", children: "Enter Tracking Number" }),
            /* @__PURE__ */ jsxRuntimeExports.jsx("p", { className: "text-sm text-gray-600", children: "Type or paste your tracking number in the search box above" })
          ] }),
          /* @__PURE__ */ jsxRuntimeExports.jsxs("div", { className: "text-center", children: [
            /* @__PURE__ */ jsxRuntimeExports.jsx("div", { className: "w-12 h-12 bg-blue-100 rounded-full flex items-center justify-center mx-auto mb-3", children: /* @__PURE__ */ jsxRuntimeExports.jsx(Package, { className: "h-6 w-6 text-blue-600" }) }),
            /* @__PURE__ */ jsxRuntimeExports.jsx("h3", { className: "font-semibold mb-2", children: "Get Real-Time Updates" }),
            /* @__PURE__ */ jsxRuntimeExports.jsx("p", { className: "text-sm text-gray-600", children: "See your package's current status and location" })
          ] }),
          /* @__PURE__ */ jsxRuntimeExports.jsxs("div", { className: "text-center", children: [
            /* @__PURE__ */ jsxRuntimeExports.jsx("div", { className: "w-12 h-12 bg-blue-100 rounded-full flex items-center justify-center mx-auto mb-3", children: /* @__PURE__ */ jsxRuntimeExports.jsx(MapPin, { className: "h-6 w-6 text-blue-600" }) }),
            /* @__PURE__ */ jsxRuntimeExports.jsx("h3", { className: "font-semibold mb-2", children: "Track Journey" }),
            /* @__PURE__ */ jsxRuntimeExports.jsx("p", { className: "text-sm text-gray-600", children: "Follow your package's complete journey from pickup to delivery" })
          ] })
        ] }) })
      ] })
    ] })
  ] });
};
export {
  TrackingPage
};
