import { b as useAuthStore, r as reactExports, j as jsxRuntimeExports, B as Button, L as Link, m as Plus, S as Search, P as Package, g as formatRelativeTime, M as MapPin, f as formatDateTime, t as toast } from "./index-D-bA2lkg.js";
import { s as shipmentApi } from "./shipment-DiICAfSs.js";
import { S as StatusIndicator } from "./status-indicator-S_BRd8a9.js";
import { I as Input } from "./input-BHuFzm78.js";
import { A as Alert, a as AlertDescription } from "./alert-Qw-zBtar.js";
import { C as Card, a as CardHeader, b as CardTitle, d as CardContent } from "./card-CsLEcWf3.js";
import { A as AlertCircle } from "./alert-circle-DRL_kTZp.js";
import { R as RefreshCw } from "./refresh-cw-DEEM45_N.js";
import { C as Calendar } from "./calendar-OA7BM1TB.js";
import { E as Eye } from "./eye-f0qCM07h.js";
import "./badge-2PvS7lZW.js";
const ShipmentsPage = () => {
  const { user } = useAuthStore();
  const [shipments, setShipments] = reactExports.useState([]);
  const [filteredShipments, setFilteredShipments] = reactExports.useState([]);
  const [loading, setLoading] = reactExports.useState(true);
  const [error, setError] = reactExports.useState(null);
  const [searchTerm, setSearchTerm] = reactExports.useState("");
  const [statusFilter, setStatusFilter] = reactExports.useState("all");
  const [sortBy, setSortBy] = reactExports.useState("date");
  const [sortOrder, setSortOrder] = reactExports.useState("desc");
  const fetchShipments = async () => {
    if (!user) return;
    try {
      setLoading(true);
      setError(null);
      const userShipments = await shipmentApi.getUserShipments(parseInt(user.id));
      setShipments(userShipments.shipments);
      setFilteredShipments(userShipments.shipments);
    } catch (err) {
      console.error("Failed to fetch shipments:", err);
      const errorMessage = err instanceof Error ? err.message : "Failed to load shipments";
      setError(errorMessage);
      toast.error("Failed to load shipments");
    } finally {
      setLoading(false);
    }
  };
  reactExports.useEffect(() => {
    fetchShipments();
  }, [user]);
  reactExports.useEffect(() => {
    let filtered = [...shipments];
    if (searchTerm) {
      filtered = filtered.filter(
        (shipment) => shipment.tracking_number.toLowerCase().includes(searchTerm.toLowerCase()) || shipment.shipment_id.toLowerCase().includes(searchTerm.toLowerCase())
      );
    }
    if (statusFilter !== "all") {
      filtered = filtered.filter((shipment) => shipment.status === statusFilter);
    }
    filtered.sort((a, b) => {
      let aValue, bValue;
      if (sortBy === "date") {
        aValue = new Date(a.created_at).getTime();
        bValue = new Date(b.created_at).getTime();
      } else {
        aValue = a.status;
        bValue = b.status;
      }
      if (sortOrder === "asc") {
        return aValue > bValue ? 1 : -1;
      } else {
        return aValue < bValue ? 1 : -1;
      }
    });
    setFilteredShipments(filtered);
  }, [shipments, searchTerm, statusFilter, sortBy, sortOrder]);
  const handleRefresh = () => {
    fetchShipments();
  };
  const getUniqueStatuses = () => {
    const statuses = [...new Set(shipments.map((s) => s.status))];
    return statuses;
  };
  if (!user) {
    return /* @__PURE__ */ jsxRuntimeExports.jsx("div", { className: "p-8", children: /* @__PURE__ */ jsxRuntimeExports.jsxs(Alert, { variant: "destructive", children: [
      /* @__PURE__ */ jsxRuntimeExports.jsx(AlertCircle, { className: "h-4 w-4" }),
      /* @__PURE__ */ jsxRuntimeExports.jsx(AlertDescription, { children: "Please log in to view your shipments." })
    ] }) });
  }
  return /* @__PURE__ */ jsxRuntimeExports.jsxs("div", { className: "p-6 max-w-7xl mx-auto", children: [
    /* @__PURE__ */ jsxRuntimeExports.jsxs("div", { className: "flex items-center justify-between mb-8", children: [
      /* @__PURE__ */ jsxRuntimeExports.jsxs("div", { children: [
        /* @__PURE__ */ jsxRuntimeExports.jsx("h1", { className: "text-3xl font-bold text-gray-900", children: "My Shipments" }),
        /* @__PURE__ */ jsxRuntimeExports.jsx("p", { className: "text-gray-600 mt-1", children: "Track and manage your shipments" })
      ] }),
      /* @__PURE__ */ jsxRuntimeExports.jsxs("div", { className: "flex items-center gap-3", children: [
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
        ),
        /* @__PURE__ */ jsxRuntimeExports.jsx(Link, { to: "/shipments/create", children: /* @__PURE__ */ jsxRuntimeExports.jsxs(Button, { size: "sm", children: [
          /* @__PURE__ */ jsxRuntimeExports.jsx(Plus, { className: "h-4 w-4 mr-2" }),
          "New Shipment"
        ] }) })
      ] })
    ] }),
    /* @__PURE__ */ jsxRuntimeExports.jsxs(Card, { className: "mb-6", children: [
      /* @__PURE__ */ jsxRuntimeExports.jsx(CardHeader, { children: /* @__PURE__ */ jsxRuntimeExports.jsx(CardTitle, { className: "text-lg", children: "Filters" }) }),
      /* @__PURE__ */ jsxRuntimeExports.jsx(CardContent, { children: /* @__PURE__ */ jsxRuntimeExports.jsxs("div", { className: "grid gap-4 md:grid-cols-4", children: [
        /* @__PURE__ */ jsxRuntimeExports.jsxs("div", { children: [
          /* @__PURE__ */ jsxRuntimeExports.jsx("label", { className: "block text-sm font-medium mb-2", children: "Search" }),
          /* @__PURE__ */ jsxRuntimeExports.jsxs("div", { className: "relative", children: [
            /* @__PURE__ */ jsxRuntimeExports.jsx(Search, { className: "absolute left-3 top-3 h-4 w-4 text-gray-400" }),
            /* @__PURE__ */ jsxRuntimeExports.jsx(
              Input,
              {
                placeholder: "Search tracking number...",
                value: searchTerm,
                onChange: (e) => setSearchTerm(e.target.value),
                className: "pl-10"
              }
            )
          ] })
        ] }),
        /* @__PURE__ */ jsxRuntimeExports.jsxs("div", { children: [
          /* @__PURE__ */ jsxRuntimeExports.jsx("label", { className: "block text-sm font-medium mb-2", children: "Status" }),
          /* @__PURE__ */ jsxRuntimeExports.jsxs(
            "select",
            {
              value: statusFilter,
              onChange: (e) => setStatusFilter(e.target.value),
              className: "w-full p-2 border border-gray-300 rounded-md focus:ring-2 focus:ring-blue-500 focus:border-blue-500",
              children: [
                /* @__PURE__ */ jsxRuntimeExports.jsx("option", { value: "all", children: "All Statuses" }),
                getUniqueStatuses().map((status) => /* @__PURE__ */ jsxRuntimeExports.jsx("option", { value: status, children: status }, status))
              ]
            }
          )
        ] }),
        /* @__PURE__ */ jsxRuntimeExports.jsxs("div", { children: [
          /* @__PURE__ */ jsxRuntimeExports.jsx("label", { className: "block text-sm font-medium mb-2", children: "Sort By" }),
          /* @__PURE__ */ jsxRuntimeExports.jsxs(
            "select",
            {
              value: sortBy,
              onChange: (e) => setSortBy(e.target.value),
              className: "w-full p-2 border border-gray-300 rounded-md focus:ring-2 focus:ring-blue-500 focus:border-blue-500",
              children: [
                /* @__PURE__ */ jsxRuntimeExports.jsx("option", { value: "date", children: "Date" }),
                /* @__PURE__ */ jsxRuntimeExports.jsx("option", { value: "status", children: "Status" })
              ]
            }
          )
        ] }),
        /* @__PURE__ */ jsxRuntimeExports.jsxs("div", { children: [
          /* @__PURE__ */ jsxRuntimeExports.jsx("label", { className: "block text-sm font-medium mb-2", children: "Order" }),
          /* @__PURE__ */ jsxRuntimeExports.jsxs(
            "select",
            {
              value: sortOrder,
              onChange: (e) => setSortOrder(e.target.value),
              className: "w-full p-2 border border-gray-300 rounded-md focus:ring-2 focus:ring-blue-500 focus:border-blue-500",
              children: [
                /* @__PURE__ */ jsxRuntimeExports.jsx("option", { value: "desc", children: "Newest First" }),
                /* @__PURE__ */ jsxRuntimeExports.jsx("option", { value: "asc", children: "Oldest First" })
              ]
            }
          )
        ] })
      ] }) })
    ] }),
    error && /* @__PURE__ */ jsxRuntimeExports.jsxs(Alert, { variant: "destructive", className: "mb-6", children: [
      /* @__PURE__ */ jsxRuntimeExports.jsx(AlertCircle, { className: "h-4 w-4" }),
      /* @__PURE__ */ jsxRuntimeExports.jsx(AlertDescription, { children: error })
    ] }),
    loading && /* @__PURE__ */ jsxRuntimeExports.jsx("div", { className: "space-y-4", children: Array.from({ length: 5 }).map((_, i) => /* @__PURE__ */ jsxRuntimeExports.jsx(Card, { className: "animate-pulse", children: /* @__PURE__ */ jsxRuntimeExports.jsx(CardContent, { className: "p-6", children: /* @__PURE__ */ jsxRuntimeExports.jsxs("div", { className: "flex items-center space-x-4", children: [
      /* @__PURE__ */ jsxRuntimeExports.jsx("div", { className: "w-12 h-12 bg-gray-200 rounded-full" }),
      /* @__PURE__ */ jsxRuntimeExports.jsxs("div", { className: "flex-1 space-y-2", children: [
        /* @__PURE__ */ jsxRuntimeExports.jsx("div", { className: "h-4 bg-gray-200 rounded w-1/4" }),
        /* @__PURE__ */ jsxRuntimeExports.jsx("div", { className: "h-3 bg-gray-200 rounded w-1/2" })
      ] })
    ] }) }) }, i)) }),
    !loading && filteredShipments.length === 0 && /* @__PURE__ */ jsxRuntimeExports.jsx(Card, { children: /* @__PURE__ */ jsxRuntimeExports.jsxs(CardContent, { className: "text-center py-12", children: [
      /* @__PURE__ */ jsxRuntimeExports.jsx(Package, { className: "h-12 w-12 mx-auto mb-4 text-gray-400" }),
      /* @__PURE__ */ jsxRuntimeExports.jsx("h3", { className: "text-lg font-medium text-gray-900 mb-2", children: searchTerm || statusFilter !== "all" ? "No shipments found" : "No shipments yet" }),
      /* @__PURE__ */ jsxRuntimeExports.jsx("p", { className: "text-gray-600 mb-4", children: searchTerm || statusFilter !== "all" ? "Try adjusting your search or filters" : "Create your first shipment to get started" }),
      !searchTerm && statusFilter === "all" && /* @__PURE__ */ jsxRuntimeExports.jsx(Link, { to: "/shipments/create", children: /* @__PURE__ */ jsxRuntimeExports.jsxs(Button, { children: [
        /* @__PURE__ */ jsxRuntimeExports.jsx(Plus, { className: "h-4 w-4 mr-2" }),
        "Create Shipment"
      ] }) })
    ] }) }),
    !loading && filteredShipments.length > 0 && /* @__PURE__ */ jsxRuntimeExports.jsx("div", { className: "space-y-4", children: filteredShipments.map((shipment) => /* @__PURE__ */ jsxRuntimeExports.jsx(Card, { className: "hover:shadow-md transition-shadow", children: /* @__PURE__ */ jsxRuntimeExports.jsx(CardContent, { className: "p-6", children: /* @__PURE__ */ jsxRuntimeExports.jsxs("div", { className: "flex items-center justify-between", children: [
      /* @__PURE__ */ jsxRuntimeExports.jsxs("div", { className: "flex items-center space-x-4", children: [
        /* @__PURE__ */ jsxRuntimeExports.jsx("div", { className: "w-12 h-12 bg-blue-100 rounded-full flex items-center justify-center", children: /* @__PURE__ */ jsxRuntimeExports.jsx(Package, { className: "h-6 w-6 text-blue-600" }) }),
        /* @__PURE__ */ jsxRuntimeExports.jsxs("div", { className: "flex-1", children: [
          /* @__PURE__ */ jsxRuntimeExports.jsxs("div", { className: "flex items-center gap-3 mb-2", children: [
            /* @__PURE__ */ jsxRuntimeExports.jsx("h3", { className: "font-semibold text-lg", children: shipment.tracking_number }),
            /* @__PURE__ */ jsxRuntimeExports.jsx(
              StatusIndicator,
              {
                status: shipment.status,
                showDot: true,
                animated: !["DELIVERED", "CANCELLED", "RETURNED"].includes(shipment.status)
              }
            )
          ] }),
          /* @__PURE__ */ jsxRuntimeExports.jsxs("div", { className: "grid grid-cols-1 md:grid-cols-3 gap-4 text-sm text-gray-600", children: [
            /* @__PURE__ */ jsxRuntimeExports.jsxs("div", { className: "flex items-center gap-1", children: [
              /* @__PURE__ */ jsxRuntimeExports.jsx(Calendar, { className: "h-4 w-4" }),
              /* @__PURE__ */ jsxRuntimeExports.jsxs("span", { children: [
                "Created: ",
                formatRelativeTime(shipment.created_at)
              ] })
            ] }),
            /* @__PURE__ */ jsxRuntimeExports.jsxs("div", { className: "flex items-center gap-1", children: [
              /* @__PURE__ */ jsxRuntimeExports.jsx(MapPin, { className: "h-4 w-4" }),
              /* @__PURE__ */ jsxRuntimeExports.jsxs("span", { children: [
                "(",
                shipment.origin.x,
                ", ",
                shipment.origin.y,
                ") → (",
                shipment.destination.x,
                ", ",
                shipment.destination.y,
                ")"
              ] })
            ] }),
            shipment.estimated_delivery && /* @__PURE__ */ jsxRuntimeExports.jsxs("div", { className: "flex items-center gap-1", children: [
              /* @__PURE__ */ jsxRuntimeExports.jsx(Calendar, { className: "h-4 w-4" }),
              /* @__PURE__ */ jsxRuntimeExports.jsxs("span", { children: [
                "Est. delivery: ",
                formatDateTime(shipment.estimated_delivery)
              ] })
            ] })
          ] })
        ] })
      ] }),
      /* @__PURE__ */ jsxRuntimeExports.jsx("div", { className: "flex items-center gap-2", children: /* @__PURE__ */ jsxRuntimeExports.jsx(Link, { to: `/shipments/${shipment.tracking_number}`, children: /* @__PURE__ */ jsxRuntimeExports.jsxs(Button, { variant: "outline", size: "sm", children: [
        /* @__PURE__ */ jsxRuntimeExports.jsx(Eye, { className: "h-4 w-4 mr-2" }),
        "Details"
      ] }) }) })
    ] }) }) }, shipment.shipment_id)) }),
    !loading && filteredShipments.length > 0 && /* @__PURE__ */ jsxRuntimeExports.jsx("div", { className: "mt-8 p-4 bg-gray-50 rounded-lg", children: /* @__PURE__ */ jsxRuntimeExports.jsxs("div", { className: "flex items-center justify-between text-sm text-gray-600", children: [
      /* @__PURE__ */ jsxRuntimeExports.jsxs("span", { children: [
        "Showing ",
        filteredShipments.length,
        " of ",
        shipments.length,
        " shipments"
      ] }),
      /* @__PURE__ */ jsxRuntimeExports.jsxs("div", { className: "flex items-center gap-4", children: [
        /* @__PURE__ */ jsxRuntimeExports.jsxs("span", { children: [
          shipments.filter((s) => s.status === "DELIVERED").length,
          " delivered"
        ] }),
        /* @__PURE__ */ jsxRuntimeExports.jsxs("span", { children: [
          shipments.filter((s) => !["DELIVERED", "CANCELLED", "RETURNED"].includes(s.status)).length,
          " in transit"
        ] }),
        /* @__PURE__ */ jsxRuntimeExports.jsxs("span", { children: [
          shipments.filter((s) => s.status === "PENDING").length,
          " pending"
        ] })
      ] })
    ] }) })
  ] });
};
export {
  ShipmentsPage
};
