import { c as createLucideIcon, r as reactExports, j as jsxRuntimeExports, P as Package, T as Truck, t as toast, f as formatDateTime, g as formatRelativeTime, B as Button, a1 as queryClient } from "./index-D-bA2lkg.js";
import { u as useQuery } from "./useQuery-ChBPFYRJ.js";
import { u as useMutation } from "./useMutation-glqHjIDc.js";
import { a as adminApi } from "./admin-NK0n_rcZ.js";
import { D as DataTable, a as DropdownMenu, b as DropdownMenuTrigger, M as MoreHorizontal, c as DropdownMenuContent, d as DropdownMenuItem, e as DropdownMenuSeparator, T as Trash2 } from "./dropdown-menu-CyXceFGG.js";
import { C as Card, d as CardContent, a as CardHeader, b as CardTitle, c as CardDescription } from "./card-CsLEcWf3.js";
import { B as Badge } from "./badge-2PvS7lZW.js";
import { A as Alert, a as AlertDescription } from "./alert-Qw-zBtar.js";
import { S as StatusIndicator } from "./status-indicator-S_BRd8a9.js";
import { R as RefreshCw } from "./refresh-cw-DEEM45_N.js";
import "./input-BHuFzm78.js";
import "./chevron-up-5dIABbZ-.js";
import "./chevron-right-B1ZewIMz.js";
import "./check-DBMoG0qQ.js";
/**
 * @license lucide-react v0.309.0 - ISC
 *
 * This source code is licensed under the ISC license.
 * See the LICENSE file in the root directory of this source tree.
 */
const CheckSquare = createLucideIcon("CheckSquare", [
  ["path", { d: "m9 11 3 3L22 4", key: "1pflzl" }],
  ["path", { d: "M21 12v7a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h11", key: "1jnkn4" }]
]);
const ShipmentManagementPage = () => {
  const [filters, setFilters] = reactExports.useState({
    page: 1,
    limit: 20,
    sortBy: "created_at",
    sortOrder: "desc"
  });
  const [selectedStatus, setSelectedStatus] = reactExports.useState("");
  const { data: shipmentsData, isLoading, error } = useQuery({
    queryKey: ["admin", "shipments", filters],
    queryFn: () => adminApi.getAllShipments(filters),
    refetchInterval: 3e4
    // Refresh every 30 seconds
  });
  const updateStatusMutation = useMutation({
    mutationFn: ({ trackingNumber, status, comment }) => adminApi.updateShipmentStatus(trackingNumber, status, comment),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["admin", "shipments"] });
      toast.success("Shipment status updated successfully");
    },
    onError: (error2) => {
      toast.error(`Failed to update shipment: ${error2.message}`);
    }
  });
  const batchUpdateMutation = useMutation({
    mutationFn: adminApi.batchUpdateShipments,
    onSuccess: (data) => {
      queryClient.invalidateQueries({ queryKey: ["admin", "shipments"] });
      toast.success(`${data.updated_count} shipments updated successfully`);
    },
    onError: (error2) => {
      toast.error(`Batch update failed: ${error2.message}`);
    }
  });
  const deleteShipmentMutation = useMutation({
    mutationFn: (trackingNumber) => adminApi.deleteShipment(trackingNumber),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["admin", "shipments"] });
      toast.success("Shipment deleted successfully");
    },
    onError: (error2) => {
      toast.error(`Failed to delete shipment: ${error2.message}`);
    }
  });
  const handleUpdateStatus = (trackingNumber, status) => {
    const comment = prompt("Enter a comment for this status update (optional):");
    updateStatusMutation.mutate({ trackingNumber, status, comment: comment || void 0 });
  };
  const handleDeleteShipment = (trackingNumber) => {
    if (confirm("Are you sure you want to delete this shipment? This action cannot be undone.")) {
      deleteShipmentMutation.mutate(trackingNumber);
    }
  };
  const handleBulkStatusUpdate = (selectedShipments, newStatus) => {
    if (selectedShipments.length === 0) {
      toast.error("Please select at least one shipment");
      return;
    }
    const comment = prompt("Enter a comment for this batch update (optional):");
    const shipmentIds = selectedShipments.map((s) => s.shipment_id);
    batchUpdateMutation.mutate({
      shipment_ids: shipmentIds,
      new_status: newStatus,
      comment: comment || void 0
    });
  };
  const handleExport = () => {
    adminApi.exportData("shipments", filters).then((blob) => {
      const url = window.URL.createObjectURL(blob);
      const a = document.createElement("a");
      a.href = url;
      a.download = `shipments-${(/* @__PURE__ */ new Date()).toISOString().split("T")[0]}.csv`;
      document.body.appendChild(a);
      a.click();
      window.URL.revokeObjectURL(url);
      document.body.removeChild(a);
      toast.success("Shipments exported successfully");
    }).catch((error2) => {
      toast.error(`Export failed: ${error2.message}`);
    });
  };
  const getStatusActions = (currentStatus) => {
    const allStatuses = ["PENDING", "PICKED_UP", "IN_TRANSIT", "OUT_FOR_DELIVERY", "DELIVERED", "CANCELLED", "RETURNED"];
    return allStatuses.filter((status) => status !== currentStatus);
  };
  const columns = [
    {
      key: "tracking_number",
      title: "Tracking Number",
      render: (value, shipment) => /* @__PURE__ */ jsxRuntimeExports.jsxs("div", { className: "space-y-1", children: [
        /* @__PURE__ */ jsxRuntimeExports.jsx("div", { className: "font-mono font-medium text-blue-600", children: value }),
        /* @__PURE__ */ jsxRuntimeExports.jsxs("div", { className: "text-xs text-gray-500", children: [
          "ID: ",
          shipment.shipment_id
        ] })
      ] })
    },
    {
      key: "status",
      title: "Status",
      render: (value, shipment) => /* @__PURE__ */ jsxRuntimeExports.jsx(
        StatusIndicator,
        {
          status: value,
          showDot: true,
          showDescription: false,
          animated: !["DELIVERED", "CANCELLED", "RETURNED"].includes(value)
        }
      )
    },
    {
      key: "sender_name",
      title: "Sender/Recipient",
      render: (value, shipment) => /* @__PURE__ */ jsxRuntimeExports.jsxs("div", { className: "space-y-1", children: [
        /* @__PURE__ */ jsxRuntimeExports.jsxs("div", { className: "text-sm font-medium text-gray-900", children: [
          "From: ",
          value
        ] }),
        /* @__PURE__ */ jsxRuntimeExports.jsxs("div", { className: "text-sm text-gray-600", children: [
          "To: ",
          shipment.recipient_name
        ] })
      ] })
    },
    {
      key: "origin",
      title: "Route",
      render: (value, shipment) => /* @__PURE__ */ jsxRuntimeExports.jsxs("div", { className: "text-sm", children: [
        /* @__PURE__ */ jsxRuntimeExports.jsxs("div", { className: "text-gray-900", children: [
          "(",
          value.x,
          ", ",
          value.y,
          ")"
        ] }),
        /* @__PURE__ */ jsxRuntimeExports.jsxs("div", { className: "text-gray-500 flex items-center", children: [
          "→ (",
          shipment.destination.x,
          ", ",
          shipment.destination.y,
          ")"
        ] })
      ] })
    },
    {
      key: "truck_id",
      title: "Truck",
      align: "center",
      render: (value) => value ? /* @__PURE__ */ jsxRuntimeExports.jsxs(Badge, { variant: "outline", className: "flex items-center gap-1", children: [
        /* @__PURE__ */ jsxRuntimeExports.jsx(Truck, { className: "h-3 w-3" }),
        "#",
        value
      ] }) : /* @__PURE__ */ jsxRuntimeExports.jsx("span", { className: "text-sm text-gray-400", children: "Unassigned" })
    },
    {
      key: "created_at",
      title: "Created",
      render: (value) => /* @__PURE__ */ jsxRuntimeExports.jsxs("div", { children: [
        /* @__PURE__ */ jsxRuntimeExports.jsx("div", { className: "text-sm text-gray-900", children: formatDateTime(value) }),
        /* @__PURE__ */ jsxRuntimeExports.jsx("div", { className: "text-xs text-gray-500", children: formatRelativeTime(value) })
      ] })
    },
    {
      key: "estimated_delivery",
      title: "Est. Delivery",
      render: (value) => value ? /* @__PURE__ */ jsxRuntimeExports.jsxs("div", { children: [
        /* @__PURE__ */ jsxRuntimeExports.jsx("div", { className: "text-sm text-gray-900", children: formatDateTime(value) }),
        /* @__PURE__ */ jsxRuntimeExports.jsx("div", { className: "text-xs text-gray-500", children: formatRelativeTime(value) })
      ] }) : /* @__PURE__ */ jsxRuntimeExports.jsx("span", { className: "text-sm text-gray-400", children: "TBD" })
    },
    {
      key: "actions",
      title: "Actions",
      align: "center",
      render: (_, shipment) => /* @__PURE__ */ jsxRuntimeExports.jsxs(DropdownMenu, { children: [
        /* @__PURE__ */ jsxRuntimeExports.jsx(DropdownMenuTrigger, { asChild: true, children: /* @__PURE__ */ jsxRuntimeExports.jsx(Button, { variant: "ghost", size: "sm", children: /* @__PURE__ */ jsxRuntimeExports.jsx(MoreHorizontal, { className: "h-4 w-4" }) }) }),
        /* @__PURE__ */ jsxRuntimeExports.jsxs(DropdownMenuContent, { align: "end", children: [
          /* @__PURE__ */ jsxRuntimeExports.jsxs(DropdownMenuItem, { onClick: () => window.open(`/shipments/tracking/${shipment.tracking_number}`, "_blank"), children: [
            /* @__PURE__ */ jsxRuntimeExports.jsx(Package, { className: "h-4 w-4 mr-2" }),
            "View Details"
          ] }),
          /* @__PURE__ */ jsxRuntimeExports.jsx(DropdownMenuSeparator, {}),
          getStatusActions(shipment.status).map((status) => /* @__PURE__ */ jsxRuntimeExports.jsxs(
            DropdownMenuItem,
            {
              onClick: () => handleUpdateStatus(shipment.tracking_number, status),
              children: [
                /* @__PURE__ */ jsxRuntimeExports.jsx(RefreshCw, { className: "h-4 w-4 mr-2" }),
                "Set to ",
                status.replace("_", " ")
              ]
            },
            status
          )),
          /* @__PURE__ */ jsxRuntimeExports.jsx(DropdownMenuSeparator, {}),
          /* @__PURE__ */ jsxRuntimeExports.jsxs(
            DropdownMenuItem,
            {
              onClick: () => handleDeleteShipment(shipment.tracking_number),
              className: "text-red-600",
              children: [
                /* @__PURE__ */ jsxRuntimeExports.jsx(Trash2, { className: "h-4 w-4 mr-2" }),
                "Delete Shipment"
              ]
            }
          )
        ] })
      ] })
    }
  ];
  const stats = shipmentsData?.shipments ? {
    total: shipmentsData.total_count,
    pending: shipmentsData.shipments.filter((s) => s.status === "PENDING").length,
    inTransit: shipmentsData.shipments.filter((s) => ["PICKED_UP", "IN_TRANSIT", "OUT_FOR_DELIVERY"].includes(s.status)).length,
    delivered: shipmentsData.shipments.filter((s) => s.status === "DELIVERED").length
  } : { total: 0, pending: 0, inTransit: 0, delivered: 0 };
  return /* @__PURE__ */ jsxRuntimeExports.jsxs("div", { className: "p-6 max-w-7xl mx-auto", children: [
    /* @__PURE__ */ jsxRuntimeExports.jsxs("div", { className: "mb-8", children: [
      /* @__PURE__ */ jsxRuntimeExports.jsx("h1", { className: "text-3xl font-bold text-gray-900", children: "Shipment Management" }),
      /* @__PURE__ */ jsxRuntimeExports.jsx("p", { className: "text-gray-600", children: "Monitor and manage all shipments in the system" })
    ] }),
    /* @__PURE__ */ jsxRuntimeExports.jsxs("div", { className: "grid gap-6 md:grid-cols-4 mb-8", children: [
      /* @__PURE__ */ jsxRuntimeExports.jsx(Card, { children: /* @__PURE__ */ jsxRuntimeExports.jsx(CardContent, { className: "p-6", children: /* @__PURE__ */ jsxRuntimeExports.jsxs("div", { className: "flex items-center justify-between", children: [
        /* @__PURE__ */ jsxRuntimeExports.jsxs("div", { children: [
          /* @__PURE__ */ jsxRuntimeExports.jsx("p", { className: "text-sm font-medium text-gray-600", children: "Total Shipments" }),
          /* @__PURE__ */ jsxRuntimeExports.jsx("p", { className: "text-2xl font-bold text-gray-900", children: stats.total })
        ] }),
        /* @__PURE__ */ jsxRuntimeExports.jsx("div", { className: "w-12 h-12 bg-blue-100 rounded-lg flex items-center justify-center", children: /* @__PURE__ */ jsxRuntimeExports.jsx(Package, { className: "h-6 w-6 text-blue-600" }) })
      ] }) }) }),
      /* @__PURE__ */ jsxRuntimeExports.jsx(Card, { children: /* @__PURE__ */ jsxRuntimeExports.jsx(CardContent, { className: "p-6", children: /* @__PURE__ */ jsxRuntimeExports.jsxs("div", { className: "flex items-center justify-between", children: [
        /* @__PURE__ */ jsxRuntimeExports.jsxs("div", { children: [
          /* @__PURE__ */ jsxRuntimeExports.jsx("p", { className: "text-sm font-medium text-gray-600", children: "Pending" }),
          /* @__PURE__ */ jsxRuntimeExports.jsx("p", { className: "text-2xl font-bold text-yellow-600", children: stats.pending })
        ] }),
        /* @__PURE__ */ jsxRuntimeExports.jsx("div", { className: "w-12 h-12 bg-yellow-100 rounded-lg flex items-center justify-center", children: /* @__PURE__ */ jsxRuntimeExports.jsx(Package, { className: "h-6 w-6 text-yellow-600" }) })
      ] }) }) }),
      /* @__PURE__ */ jsxRuntimeExports.jsx(Card, { children: /* @__PURE__ */ jsxRuntimeExports.jsx(CardContent, { className: "p-6", children: /* @__PURE__ */ jsxRuntimeExports.jsxs("div", { className: "flex items-center justify-between", children: [
        /* @__PURE__ */ jsxRuntimeExports.jsxs("div", { children: [
          /* @__PURE__ */ jsxRuntimeExports.jsx("p", { className: "text-sm font-medium text-gray-600", children: "In Transit" }),
          /* @__PURE__ */ jsxRuntimeExports.jsx("p", { className: "text-2xl font-bold text-blue-600", children: stats.inTransit })
        ] }),
        /* @__PURE__ */ jsxRuntimeExports.jsx("div", { className: "w-12 h-12 bg-blue-100 rounded-lg flex items-center justify-center", children: /* @__PURE__ */ jsxRuntimeExports.jsx(Truck, { className: "h-6 w-6 text-blue-600" }) })
      ] }) }) }),
      /* @__PURE__ */ jsxRuntimeExports.jsx(Card, { children: /* @__PURE__ */ jsxRuntimeExports.jsx(CardContent, { className: "p-6", children: /* @__PURE__ */ jsxRuntimeExports.jsxs("div", { className: "flex items-center justify-between", children: [
        /* @__PURE__ */ jsxRuntimeExports.jsxs("div", { children: [
          /* @__PURE__ */ jsxRuntimeExports.jsx("p", { className: "text-sm font-medium text-gray-600", children: "Delivered" }),
          /* @__PURE__ */ jsxRuntimeExports.jsx("p", { className: "text-2xl font-bold text-green-600", children: stats.delivered })
        ] }),
        /* @__PURE__ */ jsxRuntimeExports.jsx("div", { className: "w-12 h-12 bg-green-100 rounded-lg flex items-center justify-center", children: /* @__PURE__ */ jsxRuntimeExports.jsx(CheckSquare, { className: "h-6 w-6 text-green-600" }) })
      ] }) }) })
    ] }),
    /* @__PURE__ */ jsxRuntimeExports.jsxs(Card, { className: "mb-6", children: [
      /* @__PURE__ */ jsxRuntimeExports.jsxs(CardHeader, { children: [
        /* @__PURE__ */ jsxRuntimeExports.jsx(CardTitle, { children: "Quick Actions" }),
        /* @__PURE__ */ jsxRuntimeExports.jsx(CardDescription, { children: "Select multiple shipments and perform batch operations" })
      ] }),
      /* @__PURE__ */ jsxRuntimeExports.jsx(CardContent, { children: /* @__PURE__ */ jsxRuntimeExports.jsxs("div", { className: "flex items-center gap-4", children: [
        /* @__PURE__ */ jsxRuntimeExports.jsxs("div", { className: "flex items-center gap-2", children: [
          /* @__PURE__ */ jsxRuntimeExports.jsx("label", { htmlFor: "bulk-status", className: "text-sm font-medium", children: "Update Status:" }),
          /* @__PURE__ */ jsxRuntimeExports.jsxs(
            "select",
            {
              id: "bulk-status",
              value: selectedStatus,
              onChange: (e) => setSelectedStatus(e.target.value),
              className: "border border-gray-300 rounded-md px-3 py-1 text-sm",
              children: [
                /* @__PURE__ */ jsxRuntimeExports.jsx("option", { value: "", children: "Select status..." }),
                /* @__PURE__ */ jsxRuntimeExports.jsx("option", { value: "PENDING", children: "Pending" }),
                /* @__PURE__ */ jsxRuntimeExports.jsx("option", { value: "PICKED_UP", children: "Picked Up" }),
                /* @__PURE__ */ jsxRuntimeExports.jsx("option", { value: "IN_TRANSIT", children: "In Transit" }),
                /* @__PURE__ */ jsxRuntimeExports.jsx("option", { value: "OUT_FOR_DELIVERY", children: "Out for Delivery" }),
                /* @__PURE__ */ jsxRuntimeExports.jsx("option", { value: "DELIVERED", children: "Delivered" }),
                /* @__PURE__ */ jsxRuntimeExports.jsx("option", { value: "CANCELLED", children: "Cancelled" }),
                /* @__PURE__ */ jsxRuntimeExports.jsx("option", { value: "RETURNED", children: "Returned" })
              ]
            }
          )
        ] }),
        /* @__PURE__ */ jsxRuntimeExports.jsx("div", { className: "text-sm text-gray-500", children: "Select shipments from the table below to enable batch operations" })
      ] }) })
    ] }),
    /* @__PURE__ */ jsxRuntimeExports.jsx(
      DataTable,
      {
        data: shipmentsData?.shipments || [],
        columns,
        loading: isLoading,
        searchable: true,
        searchPlaceholder: "Search by tracking number, sender, or recipient...",
        searchFields: ["tracking_number", "sender_name", "recipient_name"],
        selectable: true,
        sortable: true,
        exportable: true,
        onExport: handleExport,
        actions: [
          {
            label: "Update Status",
            icon: /* @__PURE__ */ jsxRuntimeExports.jsx(RefreshCw, { className: "h-4 w-4 mr-1" }),
            onClick: (shipments) => {
              if (!selectedStatus) {
                toast.error("Please select a status first");
                return;
              }
              handleBulkStatusUpdate(shipments, selectedStatus);
            },
            variant: "primary",
            disabled: (shipments) => !selectedStatus || shipments.length === 0
          },
          {
            label: "Set to Delivered",
            icon: /* @__PURE__ */ jsxRuntimeExports.jsx(CheckSquare, { className: "h-4 w-4 mr-1" }),
            onClick: (shipments) => handleBulkStatusUpdate(shipments, "DELIVERED"),
            variant: "secondary"
          },
          {
            label: "Cancel",
            icon: /* @__PURE__ */ jsxRuntimeExports.jsx(RefreshCw, { className: "h-4 w-4 mr-1" }),
            onClick: (shipments) => handleBulkStatusUpdate(shipments, "CANCELLED"),
            variant: "destructive"
          }
        ],
        pagination: shipmentsData ? {
          current: filters.page || 1,
          pageSize: filters.limit || 20,
          total: shipmentsData.total_count,
          onChange: (page, pageSize) => {
            setFilters((prev) => ({ ...prev, page, limit: pageSize }));
          }
        } : void 0,
        emptyMessage: "No shipments found"
      }
    ),
    error && /* @__PURE__ */ jsxRuntimeExports.jsx(Alert, { variant: "destructive", className: "mt-4", children: /* @__PURE__ */ jsxRuntimeExports.jsxs(AlertDescription, { children: [
      "Failed to load shipments: ",
      error.message
    ] }) })
  ] });
};
export {
  ShipmentManagementPage
};
