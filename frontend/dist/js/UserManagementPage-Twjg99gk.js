import { c as createLucideIcon, r as reactExports, j as jsxRuntimeExports, B as Button, S as Search, U as User, P as Package, $ as LogOut, a0 as Users, T as Truck, n as Settings, _ as React, X, t as toast, f as formatDateTime, g as formatRelativeTime, a1 as queryClient } from "./index-D-bA2lkg.js";
import { u as useQuery } from "./useQuery-ChBPFYRJ.js";
import { u as useMutation } from "./useMutation-glqHjIDc.js";
import { D as Download, a as adminApi } from "./admin-NK0n_rcZ.js";
import { D as DataTable, T as Trash2, a as DropdownMenu, b as DropdownMenuTrigger, M as MoreHorizontal, c as DropdownMenuContent, d as DropdownMenuItem, e as DropdownMenuSeparator } from "./dropdown-menu-CyXceFGG.js";
import { C as Card, a as CardHeader, b as CardTitle, d as CardContent } from "./card-CsLEcWf3.js";
import { B as Badge } from "./badge-2PvS7lZW.js";
import { A as Alert, a as AlertDescription } from "./alert-Qw-zBtar.js";
import { S as Select, a as SelectTrigger, b as SelectValue, d as SelectContent, e as SelectItem, T as Tabs, f as TabsList, g as TabsTrigger, h as TabsContent } from "./tabs-BW6p3V7a.js";
import { N as Navigation, D as Dialog, b as DialogContent, c as DialogHeader, d as DialogTitle } from "./dialog-BrBMijH0.js";
import { I as Input } from "./input-BHuFzm78.js";
import { A as Activity } from "./activity-D4A68VaF.js";
import { R as RefreshCw } from "./refresh-cw-DEEM45_N.js";
import { S as Shield } from "./shield-DvnO75-W.js";
import { L as LogIn } from "./log-in-CeVa6HUc.js";
import { C as Check } from "./check-DBMoG0qQ.js";
import { C as CheckCircle } from "./check-circle-9Vi05KqK.js";
import { C as Calendar } from "./calendar-OA7BM1TB.js";
import "./chevron-up-5dIABbZ-.js";
import "./chevron-right-B1ZewIMz.js";
/**
 * @license lucide-react v0.309.0 - ISC
 *
 * This source code is licensed under the ISC license.
 * See the LICENSE file in the root directory of this source tree.
 */
const Ban = createLucideIcon("Ban", [
  ["circle", { cx: "12", cy: "12", r: "10", key: "1mglay" }],
  ["path", { d: "m4.9 4.9 14.2 14.2", key: "1m5liu" }]
]);
/**
 * @license lucide-react v0.309.0 - ISC
 *
 * This source code is licensed under the ISC license.
 * See the LICENSE file in the root directory of this source tree.
 */
const Crown = createLucideIcon("Crown", [
  ["path", { d: "m2 4 3 12h14l3-12-6 7-4-7-4 7-6-7zm3 16h14", key: "zkxr6b" }]
]);
/**
 * @license lucide-react v0.309.0 - ISC
 *
 * This source code is licensed under the ISC license.
 * See the LICENSE file in the root directory of this source tree.
 */
const SquarePen = createLucideIcon("SquarePen", [
  ["path", { d: "M12 3H5a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-7", key: "1m0v6g" }],
  ["path", { d: "M18.375 2.625a2.121 2.121 0 1 1 3 3L12 15l-4 1 1-4Z", key: "1lpok0" }]
]);
const UserActivityLogs = ({
  userId,
  className = ""
}) => {
  const [activities, setActivities] = reactExports.useState([]);
  const [filteredActivities, setFilteredActivities] = reactExports.useState([]);
  const [loading, setLoading] = reactExports.useState(true);
  const [error, setError] = reactExports.useState(null);
  const [searchTerm, setSearchTerm] = reactExports.useState("");
  const [actionFilter, setActionFilter] = reactExports.useState("all");
  const [dateFilter, setDateFilter] = reactExports.useState("all");
  const fetchActivities = async () => {
    try {
      setLoading(true);
      setError(null);
      const endpoint = userId ? `/api/admin/users/${userId}/activities` : "/api/admin/activities";
      const response = await fetch(endpoint, {
        headers: { "Authorization": `Bearer ${localStorage.getItem("token")}` }
      });
      if (!response.ok) {
        throw new Error("Failed to fetch user activities");
      }
      const data = await response.json();
      setActivities(data.data.activities || mockActivities);
    } catch (error2) {
      console.error("Error fetching activities:", error2);
      setActivities(mockActivities);
    } finally {
      setLoading(false);
    }
  };
  reactExports.useEffect(() => {
    fetchActivities();
    const interval = setInterval(fetchActivities, 3e4);
    return () => clearInterval(interval);
  }, [userId]);
  reactExports.useEffect(() => {
    let filtered = activities;
    if (searchTerm) {
      filtered = filtered.filter(
        (activity) => activity.userName.toLowerCase().includes(searchTerm.toLowerCase()) || activity.action.toLowerCase().includes(searchTerm.toLowerCase()) || activity.details.toLowerCase().includes(searchTerm.toLowerCase())
      );
    }
    if (actionFilter !== "all") {
      filtered = filtered.filter((activity) => activity.actionType === actionFilter);
    }
    if (dateFilter !== "all") {
      const now = /* @__PURE__ */ new Date();
      const filterDate = /* @__PURE__ */ new Date();
      switch (dateFilter) {
        case "today":
          filterDate.setHours(0, 0, 0, 0);
          break;
        case "week":
          filterDate.setDate(now.getDate() - 7);
          break;
        case "month":
          filterDate.setMonth(now.getMonth() - 1);
          break;
      }
      filtered = filtered.filter(
        (activity) => new Date(activity.timestamp) >= filterDate
      );
    }
    setFilteredActivities(filtered);
  }, [activities, searchTerm, actionFilter, dateFilter]);
  const getActionIcon = (actionType) => {
    switch (actionType) {
      case "LOGIN":
        return /* @__PURE__ */ jsxRuntimeExports.jsx(LogIn, { className: "w-4 h-4" });
      case "LOGOUT":
        return /* @__PURE__ */ jsxRuntimeExports.jsx(LogOut, { className: "w-4 h-4" });
      case "SHIPMENT_CREATE":
      case "SHIPMENT_UPDATE":
        return /* @__PURE__ */ jsxRuntimeExports.jsx(Package, { className: "w-4 h-4" });
      case "PROFILE_UPDATE":
        return /* @__PURE__ */ jsxRuntimeExports.jsx(SquarePen, { className: "w-4 h-4" });
      case "ADMIN_ACTION":
        return /* @__PURE__ */ jsxRuntimeExports.jsx(Shield, { className: "w-4 h-4" });
      default:
        return /* @__PURE__ */ jsxRuntimeExports.jsx(Activity, { className: "w-4 h-4" });
    }
  };
  const getActionColor = (actionType) => {
    switch (actionType) {
      case "LOGIN":
        return "bg-green-100 text-green-800 border-green-200";
      case "LOGOUT":
        return "bg-gray-100 text-gray-800 border-gray-200";
      case "SHIPMENT_CREATE":
        return "bg-blue-100 text-blue-800 border-blue-200";
      case "SHIPMENT_UPDATE":
        return "bg-indigo-100 text-indigo-800 border-indigo-200";
      case "PROFILE_UPDATE":
        return "bg-yellow-100 text-yellow-800 border-yellow-200";
      case "ADMIN_ACTION":
        return "bg-red-100 text-red-800 border-red-200";
      default:
        return "bg-gray-100 text-gray-800 border-gray-200";
    }
  };
  const handleExport = () => {
    const csvContent = [
      ["Timestamp", "User", "Action", "Type", "Details", "IP Address"].join(","),
      ...filteredActivities.map((activity) => [
        activity.timestamp,
        activity.userName,
        activity.action,
        activity.actionType,
        `"${activity.details}"`,
        activity.ipAddress
      ].join(","))
    ].join("\n");
    const blob = new Blob([csvContent], { type: "text/csv;charset=utf-8;" });
    const url = URL.createObjectURL(blob);
    const link = document.createElement("a");
    link.setAttribute("href", url);
    link.setAttribute("download", `user-activities-${(/* @__PURE__ */ new Date()).toISOString().split("T")[0]}.csv`);
    link.style.visibility = "hidden";
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
  };
  if (loading) {
    return /* @__PURE__ */ jsxRuntimeExports.jsxs(Card, { className, children: [
      /* @__PURE__ */ jsxRuntimeExports.jsx(CardHeader, { children: /* @__PURE__ */ jsxRuntimeExports.jsxs(CardTitle, { className: "flex items-center gap-2", children: [
        /* @__PURE__ */ jsxRuntimeExports.jsx(Activity, { className: "h-5 w-5" }),
        "User Activity Logs"
      ] }) }),
      /* @__PURE__ */ jsxRuntimeExports.jsx(CardContent, { children: /* @__PURE__ */ jsxRuntimeExports.jsx("div", { className: "flex items-center justify-center h-32", children: /* @__PURE__ */ jsxRuntimeExports.jsxs("div", { className: "flex items-center space-x-2", children: [
        /* @__PURE__ */ jsxRuntimeExports.jsx(RefreshCw, { className: "w-5 h-5 animate-spin" }),
        /* @__PURE__ */ jsxRuntimeExports.jsx("span", { children: "Loading activities..." })
      ] }) }) })
    ] });
  }
  return /* @__PURE__ */ jsxRuntimeExports.jsxs(Card, { className, children: [
    /* @__PURE__ */ jsxRuntimeExports.jsx(CardHeader, { children: /* @__PURE__ */ jsxRuntimeExports.jsxs("div", { className: "flex justify-between items-center", children: [
      /* @__PURE__ */ jsxRuntimeExports.jsxs(CardTitle, { className: "flex items-center gap-2", children: [
        /* @__PURE__ */ jsxRuntimeExports.jsx(Activity, { className: "h-5 w-5" }),
        userId ? "User Activity Logs" : "All User Activities"
      ] }),
      /* @__PURE__ */ jsxRuntimeExports.jsxs("div", { className: "flex items-center gap-2", children: [
        /* @__PURE__ */ jsxRuntimeExports.jsxs(Badge, { variant: "outline", className: "text-xs", children: [
          filteredActivities.length,
          " activities"
        ] }),
        /* @__PURE__ */ jsxRuntimeExports.jsxs(Button, { onClick: fetchActivities, variant: "outline", size: "sm", children: [
          /* @__PURE__ */ jsxRuntimeExports.jsx(RefreshCw, { className: "w-3 h-3 mr-1" }),
          "Refresh"
        ] }),
        /* @__PURE__ */ jsxRuntimeExports.jsxs(Button, { onClick: handleExport, variant: "outline", size: "sm", children: [
          /* @__PURE__ */ jsxRuntimeExports.jsx(Download, { className: "w-3 h-3 mr-1" }),
          "Export"
        ] })
      ] })
    ] }) }),
    /* @__PURE__ */ jsxRuntimeExports.jsxs(CardContent, { className: "space-y-4", children: [
      /* @__PURE__ */ jsxRuntimeExports.jsxs("div", { className: "flex flex-wrap gap-4 p-4 bg-gray-50 rounded-lg", children: [
        /* @__PURE__ */ jsxRuntimeExports.jsx("div", { className: "flex-1 min-w-[200px]", children: /* @__PURE__ */ jsxRuntimeExports.jsxs("div", { className: "relative", children: [
          /* @__PURE__ */ jsxRuntimeExports.jsx(Search, { className: "absolute left-3 top-1/2 transform -translate-y-1/2 text-gray-400 w-4 h-4" }),
          /* @__PURE__ */ jsxRuntimeExports.jsx(
            Input,
            {
              placeholder: "Search activities...",
              value: searchTerm,
              onChange: (e) => setSearchTerm(e.target.value),
              className: "pl-10"
            }
          )
        ] }) }),
        /* @__PURE__ */ jsxRuntimeExports.jsxs(Select, { value: actionFilter, onValueChange: setActionFilter, children: [
          /* @__PURE__ */ jsxRuntimeExports.jsx(SelectTrigger, { className: "w-[150px]", children: /* @__PURE__ */ jsxRuntimeExports.jsx(SelectValue, { placeholder: "Action Type" }) }),
          /* @__PURE__ */ jsxRuntimeExports.jsxs(SelectContent, { children: [
            /* @__PURE__ */ jsxRuntimeExports.jsx(SelectItem, { value: "all", children: "All Actions" }),
            /* @__PURE__ */ jsxRuntimeExports.jsx(SelectItem, { value: "LOGIN", children: "Login" }),
            /* @__PURE__ */ jsxRuntimeExports.jsx(SelectItem, { value: "LOGOUT", children: "Logout" }),
            /* @__PURE__ */ jsxRuntimeExports.jsx(SelectItem, { value: "SHIPMENT_CREATE", children: "Shipment Created" }),
            /* @__PURE__ */ jsxRuntimeExports.jsx(SelectItem, { value: "SHIPMENT_UPDATE", children: "Shipment Updated" }),
            /* @__PURE__ */ jsxRuntimeExports.jsx(SelectItem, { value: "PROFILE_UPDATE", children: "Profile Updated" }),
            /* @__PURE__ */ jsxRuntimeExports.jsx(SelectItem, { value: "ADMIN_ACTION", children: "Admin Action" })
          ] })
        ] }),
        /* @__PURE__ */ jsxRuntimeExports.jsxs(Select, { value: dateFilter, onValueChange: setDateFilter, children: [
          /* @__PURE__ */ jsxRuntimeExports.jsx(SelectTrigger, { className: "w-[120px]", children: /* @__PURE__ */ jsxRuntimeExports.jsx(SelectValue, { placeholder: "Time Period" }) }),
          /* @__PURE__ */ jsxRuntimeExports.jsxs(SelectContent, { children: [
            /* @__PURE__ */ jsxRuntimeExports.jsx(SelectItem, { value: "all", children: "All Time" }),
            /* @__PURE__ */ jsxRuntimeExports.jsx(SelectItem, { value: "today", children: "Today" }),
            /* @__PURE__ */ jsxRuntimeExports.jsx(SelectItem, { value: "week", children: "Past Week" }),
            /* @__PURE__ */ jsxRuntimeExports.jsx(SelectItem, { value: "month", children: "Past Month" })
          ] })
        ] })
      ] }),
      /* @__PURE__ */ jsxRuntimeExports.jsx("div", { className: "space-y-3 max-h-96 overflow-y-auto", children: filteredActivities.length > 0 ? filteredActivities.map((activity, index) => /* @__PURE__ */ jsxRuntimeExports.jsxs("div", { className: "flex items-start space-x-3 p-3 border rounded-lg bg-white hover:bg-gray-50 transition-colors", children: [
        /* @__PURE__ */ jsxRuntimeExports.jsx("div", { className: "flex-shrink-0 mt-1", children: /* @__PURE__ */ jsxRuntimeExports.jsx(Badge, { className: `${getActionColor(activity.actionType)} text-xs p-1`, children: getActionIcon(activity.actionType) }) }),
        /* @__PURE__ */ jsxRuntimeExports.jsxs("div", { className: "flex-1 min-w-0", children: [
          /* @__PURE__ */ jsxRuntimeExports.jsxs("div", { className: "flex items-center justify-between mb-1", children: [
            /* @__PURE__ */ jsxRuntimeExports.jsx("h4", { className: "text-sm font-medium text-gray-900", children: activity.action }),
            /* @__PURE__ */ jsxRuntimeExports.jsx("time", { className: "text-xs text-gray-500", children: new Date(activity.timestamp).toLocaleString() })
          ] }),
          /* @__PURE__ */ jsxRuntimeExports.jsx("p", { className: "text-sm text-gray-600 mb-2", children: activity.details }),
          /* @__PURE__ */ jsxRuntimeExports.jsxs("div", { className: "flex items-center justify-between text-xs text-gray-500", children: [
            /* @__PURE__ */ jsxRuntimeExports.jsxs("div", { className: "flex items-center space-x-4", children: [
              /* @__PURE__ */ jsxRuntimeExports.jsxs("span", { className: "flex items-center", children: [
                /* @__PURE__ */ jsxRuntimeExports.jsx(User, { className: "w-3 h-3 mr-1" }),
                activity.userName
              ] }),
              /* @__PURE__ */ jsxRuntimeExports.jsxs("span", { children: [
                "IP: ",
                activity.ipAddress
              ] })
            ] }),
            activity.metadata && /* @__PURE__ */ jsxRuntimeExports.jsx(Badge, { variant: "outline", className: "text-xs", children: activity.actionType })
          ] }),
          activity.metadata && (activity.metadata.oldValue || activity.metadata.newValue) && /* @__PURE__ */ jsxRuntimeExports.jsxs("div", { className: "mt-2 p-2 bg-gray-50 rounded text-xs", children: [
            activity.metadata.oldValue && /* @__PURE__ */ jsxRuntimeExports.jsxs("div", { className: "text-red-600", children: [
              "Old: ",
              JSON.stringify(activity.metadata.oldValue)
            ] }),
            activity.metadata.newValue && /* @__PURE__ */ jsxRuntimeExports.jsxs("div", { className: "text-green-600", children: [
              "New: ",
              JSON.stringify(activity.metadata.newValue)
            ] })
          ] })
        ] })
      ] }, activity.id)) : /* @__PURE__ */ jsxRuntimeExports.jsxs("div", { className: "text-center py-8 text-gray-500", children: [
        /* @__PURE__ */ jsxRuntimeExports.jsx(Activity, { className: "w-12 h-12 mx-auto mb-4 opacity-50" }),
        /* @__PURE__ */ jsxRuntimeExports.jsx("p", { children: "No activities found" }),
        /* @__PURE__ */ jsxRuntimeExports.jsx("p", { className: "text-sm", children: "Try adjusting your search filters" })
      ] }) })
    ] })
  ] });
};
const mockActivities = [
  {
    id: "1",
    userId: "1",
    userName: "John Smith",
    action: "User logged in",
    actionType: "LOGIN",
    details: "User successfully logged in from desktop application",
    ipAddress: "192.168.1.100",
    userAgent: "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36",
    timestamp: new Date(Date.now() - 30 * 60 * 1e3).toISOString()
    // 30 minutes ago
  },
  {
    id: "2",
    userId: "2",
    userName: "Jane Doe",
    action: "Created new shipment",
    actionType: "SHIPMENT_CREATE",
    details: "Created shipment #SHP-12345 for package delivery to downtown area",
    ipAddress: "192.168.1.102",
    userAgent: "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7)",
    timestamp: new Date(Date.now() - 60 * 60 * 1e3).toISOString(),
    // 1 hour ago
    metadata: {
      entityId: "SHP-12345",
      entityType: "shipment"
    }
  },
  {
    id: "3",
    userId: "3",
    userName: "Admin User",
    action: "Updated user role",
    actionType: "ADMIN_ACTION",
    details: "Changed user role from USER to DRIVER for John Smith",
    ipAddress: "192.168.1.10",
    userAgent: "Mozilla/5.0 (Windows NT 10.0; Win64; x64)",
    timestamp: new Date(Date.now() - 120 * 60 * 1e3).toISOString(),
    // 2 hours ago
    metadata: {
      entityId: "1",
      entityType: "user",
      oldValue: { role: "USER" },
      newValue: { role: "DRIVER" }
    }
  },
  {
    id: "4",
    userId: "1",
    userName: "John Smith",
    action: "Updated profile information",
    actionType: "PROFILE_UPDATE",
    details: "Updated contact phone number and emergency contact",
    ipAddress: "192.168.1.100",
    userAgent: "Mozilla/5.0 (Windows NT 10.0; Win64; x64)",
    timestamp: new Date(Date.now() - 180 * 60 * 1e3).toISOString(),
    // 3 hours ago
    metadata: {
      entityId: "1",
      entityType: "user_profile"
    }
  },
  {
    id: "5",
    userId: "4",
    userName: "Driver Mike",
    action: "Updated shipment status",
    actionType: "SHIPMENT_UPDATE",
    details: "Marked shipment #SHP-12344 as delivered",
    ipAddress: "10.0.0.50",
    userAgent: "MiniUPS Mobile App v1.2.0",
    timestamp: new Date(Date.now() - 240 * 60 * 1e3).toISOString(),
    // 4 hours ago
    metadata: {
      entityId: "SHP-12344",
      entityType: "shipment",
      oldValue: { status: "IN_TRANSIT" },
      newValue: { status: "DELIVERED" }
    }
  },
  {
    id: "6",
    userId: "2",
    userName: "Jane Doe",
    action: "User logged out",
    actionType: "LOGOUT",
    details: "User logged out after 2 hours session",
    ipAddress: "192.168.1.102",
    userAgent: "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7)",
    timestamp: new Date(Date.now() - 300 * 60 * 1e3).toISOString()
    // 5 hours ago
  }
];
const RoleVisualization = ({
  users = [],
  onRoleUpdate,
  className = ""
}) => {
  const [selectedRole, setSelectedRole] = reactExports.useState(null);
  const [viewMode, setViewMode] = reactExports.useState("hierarchy");
  const permissions = [
    // User permissions
    { id: "user.profile.view", name: "View Profile", description: "View own profile information", category: "USER" },
    { id: "user.profile.edit", name: "Edit Profile", description: "Edit own profile information", category: "USER" },
    { id: "user.shipments.view", name: "View Own Shipments", description: "View own shipments only", category: "SHIPMENT" },
    { id: "user.shipments.create", name: "Create Shipments", description: "Create new shipments", category: "SHIPMENT" },
    // Driver permissions
    { id: "driver.shipments.update", name: "Update Shipment Status", description: "Update shipment delivery status", category: "SHIPMENT" },
    { id: "driver.truck.view", name: "View Assigned Truck", description: "View assigned truck information", category: "FLEET" },
    { id: "driver.route.view", name: "View Routes", description: "View assigned delivery routes", category: "FLEET" },
    // Operator permissions
    { id: "operator.shipments.view.all", name: "View All Shipments", description: "View all shipments in system", category: "SHIPMENT" },
    { id: "operator.shipments.edit", name: "Edit Shipments", description: "Edit shipment details and status", category: "SHIPMENT" },
    { id: "operator.fleet.view", name: "View Fleet", description: "View all trucks and drivers", category: "FLEET" },
    { id: "operator.fleet.assign", name: "Assign Drivers", description: "Assign drivers to trucks and routes", category: "FLEET" },
    // Admin permissions
    { id: "admin.users.view", name: "View All Users", description: "View all user accounts", category: "ADMIN" },
    { id: "admin.users.edit", name: "Edit Users", description: "Edit user accounts and details", category: "ADMIN" },
    { id: "admin.users.delete", name: "Delete Users", description: "Delete user accounts", category: "ADMIN" },
    { id: "admin.roles.assign", name: "Assign Roles", description: "Assign roles to users", category: "ADMIN" },
    { id: "admin.fleet.manage", name: "Manage Fleet", description: "Full fleet management access", category: "FLEET" },
    { id: "admin.system.config", name: "System Configuration", description: "Access system configuration", category: "SYSTEM" },
    { id: "admin.analytics.view", name: "View Analytics", description: "Access analytics and reports", category: "ADMIN" },
    { id: "admin.system.logs", name: "View System Logs", description: "Access system logs and audit trails", category: "SYSTEM" }
  ];
  const roles = [
    {
      id: "USER",
      name: "User",
      description: "Regular customers who can create and track shipments",
      color: "bg-blue-100 text-blue-800 border-blue-200",
      icon: /* @__PURE__ */ jsxRuntimeExports.jsx(Users, { className: "w-4 h-4" }),
      level: 1,
      permissions: [
        "user.profile.view",
        "user.profile.edit",
        "user.shipments.view",
        "user.shipments.create"
      ],
      userCount: users.filter((u) => u.role === "USER").length
    },
    {
      id: "DRIVER",
      name: "Driver",
      description: "Drivers who deliver packages and update shipment status",
      color: "bg-green-100 text-green-800 border-green-200",
      icon: /* @__PURE__ */ jsxRuntimeExports.jsx(Truck, { className: "w-4 h-4" }),
      level: 2,
      permissions: [
        "user.profile.view",
        "user.profile.edit",
        "user.shipments.view",
        "driver.shipments.update",
        "driver.truck.view",
        "driver.route.view"
      ],
      userCount: users.filter((u) => u.role === "DRIVER").length
    },
    {
      id: "OPERATOR",
      name: "Operator",
      description: "Operations staff who manage shipments and fleet assignments",
      color: "bg-yellow-100 text-yellow-800 border-yellow-200",
      icon: /* @__PURE__ */ jsxRuntimeExports.jsx(Settings, { className: "w-4 h-4" }),
      level: 3,
      permissions: [
        "user.profile.view",
        "user.profile.edit",
        "operator.shipments.view.all",
        "operator.shipments.edit",
        "operator.fleet.view",
        "operator.fleet.assign",
        "driver.truck.view",
        "driver.route.view"
      ],
      userCount: users.filter((u) => u.role === "OPERATOR").length,
      canAssignRoles: ["USER", "DRIVER"]
    },
    {
      id: "ADMIN",
      name: "Administrator",
      description: "Full system access with user and system management capabilities",
      color: "bg-red-100 text-red-800 border-red-200",
      icon: /* @__PURE__ */ jsxRuntimeExports.jsx(Crown, { className: "w-4 h-4" }),
      level: 4,
      permissions: permissions.map((p) => p.id),
      // All permissions
      userCount: users.filter((u) => u.role === "ADMIN").length,
      canAssignRoles: ["USER", "DRIVER", "OPERATOR", "ADMIN"]
    }
  ];
  const permissionCategories = reactExports.useMemo(() => {
    return permissions.reduce((acc, permission) => {
      if (!acc[permission.category]) {
        acc[permission.category] = [];
      }
      acc[permission.category].push(permission);
      return acc;
    }, {});
  }, [permissions]);
  const roleHierarchy = reactExports.useMemo(() => {
    return [...roles].sort((a, b) => b.level - a.level);
  }, [roles]);
  selectedRole ? roles.find((r) => r.id === selectedRole) : null;
  const hasPermission = (roleId, permissionId) => {
    const role = roles.find((r) => r.id === roleId);
    return role?.permissions.includes(permissionId) || false;
  };
  const getCategoryIcon = (category) => {
    switch (category) {
      case "USER":
        return /* @__PURE__ */ jsxRuntimeExports.jsx(Users, { className: "w-4 h-4" });
      case "SHIPMENT":
        return /* @__PURE__ */ jsxRuntimeExports.jsx(Navigation, { className: "w-4 h-4" });
      case "FLEET":
        return /* @__PURE__ */ jsxRuntimeExports.jsx(Truck, { className: "w-4 h-4" });
      case "ADMIN":
        return /* @__PURE__ */ jsxRuntimeExports.jsx(Shield, { className: "w-4 h-4" });
      case "SYSTEM":
        return /* @__PURE__ */ jsxRuntimeExports.jsx(Settings, { className: "w-4 h-4" });
      default:
        return /* @__PURE__ */ jsxRuntimeExports.jsx(Settings, { className: "w-4 h-4" });
    }
  };
  return /* @__PURE__ */ jsxRuntimeExports.jsxs(Card, { className, children: [
    /* @__PURE__ */ jsxRuntimeExports.jsx(CardHeader, { children: /* @__PURE__ */ jsxRuntimeExports.jsxs(CardTitle, { className: "flex items-center gap-2", children: [
      /* @__PURE__ */ jsxRuntimeExports.jsx(Shield, { className: "h-5 w-5" }),
      "Role & Permission Management"
    ] }) }),
    /* @__PURE__ */ jsxRuntimeExports.jsx(CardContent, { children: /* @__PURE__ */ jsxRuntimeExports.jsxs(Tabs, { value: viewMode, onValueChange: (value) => setViewMode(value), className: "w-full", children: [
      /* @__PURE__ */ jsxRuntimeExports.jsxs(TabsList, { className: "grid w-full grid-cols-3", children: [
        /* @__PURE__ */ jsxRuntimeExports.jsx(TabsTrigger, { value: "hierarchy", children: "Role Hierarchy" }),
        /* @__PURE__ */ jsxRuntimeExports.jsx(TabsTrigger, { value: "matrix", children: "Permission Matrix" }),
        /* @__PURE__ */ jsxRuntimeExports.jsx(TabsTrigger, { value: "distribution", children: "Role Distribution" })
      ] }),
      /* @__PURE__ */ jsxRuntimeExports.jsx(TabsContent, { value: "hierarchy", className: "space-y-4", children: /* @__PURE__ */ jsxRuntimeExports.jsx("div", { className: "grid gap-4", children: roleHierarchy.map((role) => /* @__PURE__ */ jsxRuntimeExports.jsx(
        Card,
        {
          className: `cursor-pointer transition-all hover:shadow-md ${selectedRole === role.id ? "ring-2 ring-blue-500" : ""}`,
          onClick: () => setSelectedRole(selectedRole === role.id ? null : role.id),
          children: /* @__PURE__ */ jsxRuntimeExports.jsxs(CardContent, { className: "p-4", children: [
            /* @__PURE__ */ jsxRuntimeExports.jsxs("div", { className: "flex items-center justify-between", children: [
              /* @__PURE__ */ jsxRuntimeExports.jsxs("div", { className: "flex items-center gap-3", children: [
                /* @__PURE__ */ jsxRuntimeExports.jsxs(Badge, { className: role.color, children: [
                  role.icon,
                  /* @__PURE__ */ jsxRuntimeExports.jsx("span", { className: "ml-2", children: role.name })
                ] }),
                /* @__PURE__ */ jsxRuntimeExports.jsxs("div", { className: "text-sm text-gray-600", children: [
                  "Level ",
                  role.level
                ] })
              ] }),
              /* @__PURE__ */ jsxRuntimeExports.jsxs("div", { className: "flex items-center gap-2", children: [
                /* @__PURE__ */ jsxRuntimeExports.jsxs(Badge, { variant: "outline", className: "text-xs", children: [
                  role.userCount,
                  " users"
                ] }),
                /* @__PURE__ */ jsxRuntimeExports.jsxs(Badge, { variant: "outline", className: "text-xs", children: [
                  role.permissions.length,
                  " permissions"
                ] })
              ] })
            ] }),
            /* @__PURE__ */ jsxRuntimeExports.jsx("p", { className: "text-sm text-gray-600 mt-2", children: role.description }),
            selectedRole === role.id && /* @__PURE__ */ jsxRuntimeExports.jsxs("div", { className: "mt-4 pt-4 border-t", children: [
              /* @__PURE__ */ jsxRuntimeExports.jsxs("h4", { className: "font-medium mb-2", children: [
                "Permissions (",
                role.permissions.length,
                ")"
              ] }),
              /* @__PURE__ */ jsxRuntimeExports.jsx("div", { className: "grid grid-cols-1 md:grid-cols-2 gap-2", children: Object.entries(permissionCategories).map(([category, categoryPermissions]) => {
                const rolePermissions = categoryPermissions.filter(
                  (p) => role.permissions.includes(p.id)
                );
                if (rolePermissions.length === 0) return null;
                return /* @__PURE__ */ jsxRuntimeExports.jsxs("div", { className: "space-y-1", children: [
                  /* @__PURE__ */ jsxRuntimeExports.jsxs("div", { className: "flex items-center gap-2 text-sm font-medium text-gray-700", children: [
                    getCategoryIcon(category),
                    category
                  ] }),
                  rolePermissions.map((permission) => /* @__PURE__ */ jsxRuntimeExports.jsx("div", { className: "pl-6 text-sm text-gray-600", children: permission.name }, permission.id))
                ] }, category);
              }) })
            ] })
          ] })
        },
        role.id
      )) }) }),
      /* @__PURE__ */ jsxRuntimeExports.jsx(TabsContent, { value: "matrix", className: "space-y-4", children: /* @__PURE__ */ jsxRuntimeExports.jsx("div", { className: "overflow-x-auto", children: /* @__PURE__ */ jsxRuntimeExports.jsxs("table", { className: "w-full border-collapse border border-gray-200", children: [
        /* @__PURE__ */ jsxRuntimeExports.jsx("thead", { children: /* @__PURE__ */ jsxRuntimeExports.jsxs("tr", { className: "bg-gray-50", children: [
          /* @__PURE__ */ jsxRuntimeExports.jsx("th", { className: "border border-gray-200 p-3 text-left", children: "Permission" }),
          roles.map((role) => /* @__PURE__ */ jsxRuntimeExports.jsx("th", { className: "border border-gray-200 p-3 text-center min-w-[100px]", children: /* @__PURE__ */ jsxRuntimeExports.jsxs(Badge, { className: `${role.color} text-xs`, children: [
            role.icon,
            /* @__PURE__ */ jsxRuntimeExports.jsx("span", { className: "ml-1", children: role.name })
          ] }) }, role.id))
        ] }) }),
        /* @__PURE__ */ jsxRuntimeExports.jsx("tbody", { children: Object.entries(permissionCategories).map(([category, categoryPermissions]) => /* @__PURE__ */ jsxRuntimeExports.jsxs(React.Fragment, { children: [
          /* @__PURE__ */ jsxRuntimeExports.jsx("tr", { className: "bg-gray-25", children: /* @__PURE__ */ jsxRuntimeExports.jsx("td", { colSpan: roles.length + 1, className: "border border-gray-200 p-2 font-medium bg-gray-100", children: /* @__PURE__ */ jsxRuntimeExports.jsxs("div", { className: "flex items-center gap-2", children: [
            getCategoryIcon(category),
            category,
            " Permissions"
          ] }) }) }),
          categoryPermissions.map((permission) => /* @__PURE__ */ jsxRuntimeExports.jsxs("tr", { className: "hover:bg-gray-50", children: [
            /* @__PURE__ */ jsxRuntimeExports.jsx("td", { className: "border border-gray-200 p-3", children: /* @__PURE__ */ jsxRuntimeExports.jsxs("div", { children: [
              /* @__PURE__ */ jsxRuntimeExports.jsx("div", { className: "font-medium text-sm", children: permission.name }),
              /* @__PURE__ */ jsxRuntimeExports.jsx("div", { className: "text-xs text-gray-500", children: permission.description })
            ] }) }),
            roles.map((role) => /* @__PURE__ */ jsxRuntimeExports.jsx("td", { className: "border border-gray-200 p-3 text-center", children: hasPermission(role.id, permission.id) ? /* @__PURE__ */ jsxRuntimeExports.jsx(Check, { className: "w-5 h-5 text-green-600 mx-auto" }) : /* @__PURE__ */ jsxRuntimeExports.jsx(X, { className: "w-5 h-5 text-gray-300 mx-auto" }) }, role.id))
          ] }, permission.id))
        ] }, category)) })
      ] }) }) }),
      /* @__PURE__ */ jsxRuntimeExports.jsxs(TabsContent, { value: "distribution", className: "space-y-4", children: [
        /* @__PURE__ */ jsxRuntimeExports.jsxs("div", { className: "grid gap-6 md:grid-cols-2", children: [
          /* @__PURE__ */ jsxRuntimeExports.jsxs(Card, { children: [
            /* @__PURE__ */ jsxRuntimeExports.jsx(CardHeader, { children: /* @__PURE__ */ jsxRuntimeExports.jsx(CardTitle, { className: "text-lg", children: "User Distribution by Role" }) }),
            /* @__PURE__ */ jsxRuntimeExports.jsx(CardContent, { children: /* @__PURE__ */ jsxRuntimeExports.jsx("div", { className: "space-y-3", children: roles.map((role) => {
              const percentage = users.length > 0 ? Math.round(role.userCount / users.length * 100) : 0;
              return /* @__PURE__ */ jsxRuntimeExports.jsxs("div", { className: "space-y-2", children: [
                /* @__PURE__ */ jsxRuntimeExports.jsxs("div", { className: "flex justify-between items-center", children: [
                  /* @__PURE__ */ jsxRuntimeExports.jsxs(Badge, { className: role.color, children: [
                    role.icon,
                    /* @__PURE__ */ jsxRuntimeExports.jsx("span", { className: "ml-2", children: role.name })
                  ] }),
                  /* @__PURE__ */ jsxRuntimeExports.jsxs("span", { className: "text-sm font-medium", children: [
                    role.userCount,
                    " (",
                    percentage,
                    "%)"
                  ] })
                ] }),
                /* @__PURE__ */ jsxRuntimeExports.jsx("div", { className: "w-full bg-gray-200 rounded-full h-2", children: /* @__PURE__ */ jsxRuntimeExports.jsx(
                  "div",
                  {
                    className: "bg-blue-600 h-2 rounded-full",
                    style: { width: `${percentage}%` }
                  }
                ) })
              ] }, role.id);
            }) }) })
          ] }),
          /* @__PURE__ */ jsxRuntimeExports.jsxs(Card, { children: [
            /* @__PURE__ */ jsxRuntimeExports.jsx(CardHeader, { children: /* @__PURE__ */ jsxRuntimeExports.jsx(CardTitle, { className: "text-lg", children: "Permission Coverage" }) }),
            /* @__PURE__ */ jsxRuntimeExports.jsx(CardContent, { children: /* @__PURE__ */ jsxRuntimeExports.jsx("div", { className: "space-y-3", children: Object.entries(permissionCategories).map(([category, categoryPermissions]) => {
              const totalPermissions = categoryPermissions.length;
              const adminPermissions = categoryPermissions.filter(
                (p) => hasPermission("ADMIN", p.id)
              ).length;
              return /* @__PURE__ */ jsxRuntimeExports.jsxs("div", { className: "space-y-2", children: [
                /* @__PURE__ */ jsxRuntimeExports.jsxs("div", { className: "flex justify-between items-center", children: [
                  /* @__PURE__ */ jsxRuntimeExports.jsxs("div", { className: "flex items-center gap-2", children: [
                    getCategoryIcon(category),
                    /* @__PURE__ */ jsxRuntimeExports.jsx("span", { className: "text-sm font-medium", children: category })
                  ] }),
                  /* @__PURE__ */ jsxRuntimeExports.jsxs("span", { className: "text-sm", children: [
                    adminPermissions,
                    "/",
                    totalPermissions
                  ] })
                ] }),
                /* @__PURE__ */ jsxRuntimeExports.jsx("div", { className: "w-full bg-gray-200 rounded-full h-2", children: /* @__PURE__ */ jsxRuntimeExports.jsx(
                  "div",
                  {
                    className: "bg-green-600 h-2 rounded-full",
                    style: { width: `${adminPermissions / totalPermissions * 100}%` }
                  }
                ) })
              ] }, category);
            }) }) })
          ] })
        ] }),
        /* @__PURE__ */ jsxRuntimeExports.jsxs(Card, { children: [
          /* @__PURE__ */ jsxRuntimeExports.jsx(CardHeader, { children: /* @__PURE__ */ jsxRuntimeExports.jsx(CardTitle, { className: "text-lg", children: "Role Capabilities Summary" }) }),
          /* @__PURE__ */ jsxRuntimeExports.jsx(CardContent, { children: /* @__PURE__ */ jsxRuntimeExports.jsx("div", { className: "grid gap-4 md:grid-cols-2 lg:grid-cols-4", children: roles.map((role) => /* @__PURE__ */ jsxRuntimeExports.jsxs("div", { className: "space-y-2", children: [
            /* @__PURE__ */ jsxRuntimeExports.jsxs(Badge, { className: `${role.color} mb-2`, children: [
              role.icon,
              /* @__PURE__ */ jsxRuntimeExports.jsx("span", { className: "ml-2", children: role.name })
            ] }),
            /* @__PURE__ */ jsxRuntimeExports.jsx("div", { className: "text-sm space-y-1", children: Object.entries(permissionCategories).map(([category, categoryPermissions]) => {
              const rolePermissionsInCategory = categoryPermissions.filter(
                (p) => role.permissions.includes(p.id)
              ).length;
              if (rolePermissionsInCategory === 0) return null;
              return /* @__PURE__ */ jsxRuntimeExports.jsxs("div", { className: "flex justify-between", children: [
                /* @__PURE__ */ jsxRuntimeExports.jsxs("span", { className: "text-gray-600", children: [
                  category,
                  ":"
                ] }),
                /* @__PURE__ */ jsxRuntimeExports.jsxs("span", { className: "font-medium", children: [
                  rolePermissionsInCategory,
                  "/",
                  categoryPermissions.length
                ] })
              ] }, category);
            }) })
          ] }, role.id)) }) })
        ] })
      ] })
    ] }) })
  ] });
};
const UserManagementPage = () => {
  const [filters, setFilters] = reactExports.useState({
    page: 1,
    limit: 20,
    sortBy: "created_at",
    sortOrder: "desc"
  });
  const [selectedUserId, setSelectedUserId] = reactExports.useState(null);
  const [isActivityDialogOpen, setIsActivityDialogOpen] = reactExports.useState(false);
  const { data: usersData, isLoading, error } = useQuery({
    queryKey: ["admin", "users", filters],
    queryFn: () => adminApi.getUsers(filters),
    refetchInterval: 3e4
    // Refresh every 30 seconds
  });
  const updateUserMutation = useMutation({
    mutationFn: ({ userId, data }) => adminApi.updateUser(userId, data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["admin", "users"] });
      toast.success("User updated successfully");
    },
    onError: (error2) => {
      toast.error(`Failed to update user: ${error2.message}`);
    }
  });
  const deleteUserMutation = useMutation({
    mutationFn: (userId) => adminApi.deleteUser(userId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["admin", "users"] });
      toast.success("User deleted successfully");
    },
    onError: (error2) => {
      toast.error(`Failed to delete user: ${error2.message}`);
    }
  });
  const handleUpdateUserStatus = (userId, status) => {
    updateUserMutation.mutate({ userId, data: { status } });
  };
  const handleUpdateUserRole = (userId, role) => {
    updateUserMutation.mutate({ userId, data: { role } });
  };
  const handleDeleteUser = (userId) => {
    if (confirm("Are you sure you want to delete this user? This action cannot be undone.")) {
      deleteUserMutation.mutate(userId);
    }
  };
  const handleViewUserActivity = (userId) => {
    setSelectedUserId(userId);
    setIsActivityDialogOpen(true);
  };
  const handleBulkAction = (selectedUsers, action) => {
    if (selectedUsers.length === 0) {
      toast.error("Please select at least one user");
      return;
    }
    const userIds = selectedUsers.map((user) => user.id);
    switch (action) {
      case "activate":
        userIds.forEach(
          (userId) => updateUserMutation.mutate({ userId, data: { status: "ACTIVE" } })
        );
        break;
      case "deactivate":
        userIds.forEach(
          (userId) => updateUserMutation.mutate({ userId, data: { status: "INACTIVE" } })
        );
        break;
      case "suspend":
        userIds.forEach(
          (userId) => updateUserMutation.mutate({ userId, data: { status: "SUSPENDED" } })
        );
        break;
      case "delete":
        if (confirm(`Are you sure you want to delete ${selectedUsers.length} users? This action cannot be undone.`)) {
          userIds.forEach((userId) => deleteUserMutation.mutate(userId));
        }
        break;
    }
  };
  const handleExport = () => {
    adminApi.exportData("users", filters).then((blob) => {
      const url = window.URL.createObjectURL(blob);
      const a = document.createElement("a");
      a.href = url;
      a.download = `users-${(/* @__PURE__ */ new Date()).toISOString().split("T")[0]}.csv`;
      document.body.appendChild(a);
      a.click();
      window.URL.revokeObjectURL(url);
      document.body.removeChild(a);
      toast.success("Users exported successfully");
    }).catch((error2) => {
      toast.error(`Export failed: ${error2.message}`);
    });
  };
  const getRoleColor = (role) => {
    switch (role) {
      case "ADMIN":
        return "destructive";
      case "DRIVER":
        return "default";
      case "OPERATOR":
        return "secondary";
      default:
        return "outline";
    }
  };
  const getStatusColor = (status) => {
    switch (status) {
      case "ACTIVE":
        return "default";
      case "INACTIVE":
        return "secondary";
      case "SUSPENDED":
        return "destructive";
      default:
        return "outline";
    }
  };
  const columns = [
    {
      key: "name",
      title: "User",
      render: (value, user) => /* @__PURE__ */ jsxRuntimeExports.jsxs("div", { className: "flex items-center space-x-3", children: [
        /* @__PURE__ */ jsxRuntimeExports.jsx("div", { className: "w-8 h-8 bg-blue-100 rounded-full flex items-center justify-center", children: /* @__PURE__ */ jsxRuntimeExports.jsx("span", { className: "text-sm font-medium text-blue-600", children: user.name.charAt(0).toUpperCase() }) }),
        /* @__PURE__ */ jsxRuntimeExports.jsxs("div", { children: [
          /* @__PURE__ */ jsxRuntimeExports.jsx("div", { className: "font-medium text-gray-900", children: user.name }),
          /* @__PURE__ */ jsxRuntimeExports.jsx("div", { className: "text-sm text-gray-500", children: user.email })
        ] })
      ] })
    },
    {
      key: "role",
      title: "Role",
      render: (value) => /* @__PURE__ */ jsxRuntimeExports.jsx(Badge, { variant: getRoleColor(value), children: value })
    },
    {
      key: "status",
      title: "Status",
      render: (value) => /* @__PURE__ */ jsxRuntimeExports.jsx(Badge, { variant: getStatusColor(value), children: value })
    },
    {
      key: "shipment_count",
      title: "Shipments",
      align: "center",
      render: (value) => /* @__PURE__ */ jsxRuntimeExports.jsx("span", { className: "font-medium", children: value || 0 })
    },
    {
      key: "created_at",
      title: "Joined",
      render: (value) => /* @__PURE__ */ jsxRuntimeExports.jsxs("div", { children: [
        /* @__PURE__ */ jsxRuntimeExports.jsx("div", { className: "text-sm text-gray-900", children: formatDateTime(value) }),
        /* @__PURE__ */ jsxRuntimeExports.jsx("div", { className: "text-xs text-gray-500", children: formatRelativeTime(value) })
      ] })
    },
    {
      key: "last_login",
      title: "Last Login",
      render: (value) => value ? /* @__PURE__ */ jsxRuntimeExports.jsxs("div", { children: [
        /* @__PURE__ */ jsxRuntimeExports.jsx("div", { className: "text-sm text-gray-900", children: formatDateTime(value) }),
        /* @__PURE__ */ jsxRuntimeExports.jsx("div", { className: "text-xs text-gray-500", children: formatRelativeTime(value) })
      ] }) : /* @__PURE__ */ jsxRuntimeExports.jsx("span", { className: "text-sm text-gray-500", children: "Never" })
    },
    {
      key: "actions",
      title: "Actions",
      align: "center",
      render: (_, user) => /* @__PURE__ */ jsxRuntimeExports.jsxs(DropdownMenu, { children: [
        /* @__PURE__ */ jsxRuntimeExports.jsx(DropdownMenuTrigger, { asChild: true, children: /* @__PURE__ */ jsxRuntimeExports.jsx(Button, { variant: "ghost", size: "sm", children: /* @__PURE__ */ jsxRuntimeExports.jsx(MoreHorizontal, { className: "h-4 w-4" }) }) }),
        /* @__PURE__ */ jsxRuntimeExports.jsxs(DropdownMenuContent, { align: "end", children: [
          /* @__PURE__ */ jsxRuntimeExports.jsxs(DropdownMenuItem, { onClick: () => handleUpdateUserRole(user.id, "USER"), children: [
            /* @__PURE__ */ jsxRuntimeExports.jsx(Users, { className: "h-4 w-4 mr-2" }),
            "Set as User"
          ] }),
          /* @__PURE__ */ jsxRuntimeExports.jsxs(DropdownMenuItem, { onClick: () => handleUpdateUserRole(user.id, "DRIVER"), children: [
            /* @__PURE__ */ jsxRuntimeExports.jsx(Users, { className: "h-4 w-4 mr-2" }),
            "Set as Driver"
          ] }),
          /* @__PURE__ */ jsxRuntimeExports.jsxs(DropdownMenuItem, { onClick: () => handleUpdateUserRole(user.id, "OPERATOR"), children: [
            /* @__PURE__ */ jsxRuntimeExports.jsx(Users, { className: "h-4 w-4 mr-2" }),
            "Set as Operator"
          ] }),
          /* @__PURE__ */ jsxRuntimeExports.jsxs(DropdownMenuItem, { onClick: () => handleUpdateUserRole(user.id, "ADMIN"), children: [
            /* @__PURE__ */ jsxRuntimeExports.jsx(Shield, { className: "h-4 w-4 mr-2" }),
            "Set as Admin"
          ] }),
          /* @__PURE__ */ jsxRuntimeExports.jsx(DropdownMenuSeparator, {}),
          user.status === "ACTIVE" ? /* @__PURE__ */ jsxRuntimeExports.jsxs(DropdownMenuItem, { onClick: () => handleUpdateUserStatus(user.id, "INACTIVE"), children: [
            /* @__PURE__ */ jsxRuntimeExports.jsx(Ban, { className: "h-4 w-4 mr-2" }),
            "Deactivate"
          ] }) : /* @__PURE__ */ jsxRuntimeExports.jsxs(DropdownMenuItem, { onClick: () => handleUpdateUserStatus(user.id, "ACTIVE"), children: [
            /* @__PURE__ */ jsxRuntimeExports.jsx(CheckCircle, { className: "h-4 w-4 mr-2" }),
            "Activate"
          ] }),
          /* @__PURE__ */ jsxRuntimeExports.jsxs(
            DropdownMenuItem,
            {
              onClick: () => handleUpdateUserStatus(user.id, "SUSPENDED"),
              className: "text-yellow-600",
              children: [
                /* @__PURE__ */ jsxRuntimeExports.jsx(Ban, { className: "h-4 w-4 mr-2" }),
                "Suspend"
              ]
            }
          ),
          /* @__PURE__ */ jsxRuntimeExports.jsx(DropdownMenuSeparator, {}),
          /* @__PURE__ */ jsxRuntimeExports.jsxs(DropdownMenuItem, { onClick: () => handleViewUserActivity(user.id), children: [
            /* @__PURE__ */ jsxRuntimeExports.jsx(Activity, { className: "h-4 w-4 mr-2" }),
            "View Activity"
          ] }),
          /* @__PURE__ */ jsxRuntimeExports.jsxs(
            DropdownMenuItem,
            {
              onClick: () => handleDeleteUser(user.id),
              className: "text-red-600",
              children: [
                /* @__PURE__ */ jsxRuntimeExports.jsx(Trash2, { className: "h-4 w-4 mr-2" }),
                "Delete User"
              ]
            }
          )
        ] })
      ] })
    }
  ];
  return /* @__PURE__ */ jsxRuntimeExports.jsxs("div", { className: "p-6 max-w-7xl mx-auto", children: [
    /* @__PURE__ */ jsxRuntimeExports.jsxs("div", { className: "mb-8", children: [
      /* @__PURE__ */ jsxRuntimeExports.jsx("h1", { className: "text-3xl font-bold text-gray-900", children: "User Management" }),
      /* @__PURE__ */ jsxRuntimeExports.jsx("p", { className: "text-gray-600", children: "Manage user accounts, roles, and permissions" })
    ] }),
    /* @__PURE__ */ jsxRuntimeExports.jsxs(Tabs, { defaultValue: "users", className: "w-full", children: [
      /* @__PURE__ */ jsxRuntimeExports.jsxs(TabsList, { className: "grid w-full grid-cols-4", children: [
        /* @__PURE__ */ jsxRuntimeExports.jsx(TabsTrigger, { value: "users", children: "Users" }),
        /* @__PURE__ */ jsxRuntimeExports.jsx(TabsTrigger, { value: "roles", children: "Roles & Permissions" }),
        /* @__PURE__ */ jsxRuntimeExports.jsx(TabsTrigger, { value: "activities", children: "Activity Logs" }),
        /* @__PURE__ */ jsxRuntimeExports.jsx(TabsTrigger, { value: "analytics", children: "Analytics" })
      ] }),
      /* @__PURE__ */ jsxRuntimeExports.jsxs(TabsContent, { value: "users", className: "space-y-6", children: [
        /* @__PURE__ */ jsxRuntimeExports.jsxs("div", { className: "grid gap-6 md:grid-cols-4", children: [
          /* @__PURE__ */ jsxRuntimeExports.jsx(Card, { children: /* @__PURE__ */ jsxRuntimeExports.jsx(CardContent, { className: "p-6", children: /* @__PURE__ */ jsxRuntimeExports.jsxs("div", { className: "flex items-center justify-between", children: [
            /* @__PURE__ */ jsxRuntimeExports.jsxs("div", { children: [
              /* @__PURE__ */ jsxRuntimeExports.jsx("p", { className: "text-sm font-medium text-gray-600", children: "Total Users" }),
              /* @__PURE__ */ jsxRuntimeExports.jsx("p", { className: "text-2xl font-bold text-gray-900", children: usersData?.total_count || 0 })
            ] }),
            /* @__PURE__ */ jsxRuntimeExports.jsx("div", { className: "w-12 h-12 bg-blue-100 rounded-lg flex items-center justify-center", children: /* @__PURE__ */ jsxRuntimeExports.jsx(Users, { className: "h-6 w-6 text-blue-600" }) })
          ] }) }) }),
          /* @__PURE__ */ jsxRuntimeExports.jsx(Card, { children: /* @__PURE__ */ jsxRuntimeExports.jsx(CardContent, { className: "p-6", children: /* @__PURE__ */ jsxRuntimeExports.jsxs("div", { className: "flex items-center justify-between", children: [
            /* @__PURE__ */ jsxRuntimeExports.jsxs("div", { children: [
              /* @__PURE__ */ jsxRuntimeExports.jsx("p", { className: "text-sm font-medium text-gray-600", children: "Active Users" }),
              /* @__PURE__ */ jsxRuntimeExports.jsx("p", { className: "text-2xl font-bold text-green-600", children: usersData?.users?.filter((u) => u.status === "ACTIVE").length || 0 })
            ] }),
            /* @__PURE__ */ jsxRuntimeExports.jsx("div", { className: "w-12 h-12 bg-green-100 rounded-lg flex items-center justify-center", children: /* @__PURE__ */ jsxRuntimeExports.jsx(CheckCircle, { className: "h-6 w-6 text-green-600" }) })
          ] }) }) }),
          /* @__PURE__ */ jsxRuntimeExports.jsx(Card, { children: /* @__PURE__ */ jsxRuntimeExports.jsx(CardContent, { className: "p-6", children: /* @__PURE__ */ jsxRuntimeExports.jsxs("div", { className: "flex items-center justify-between", children: [
            /* @__PURE__ */ jsxRuntimeExports.jsxs("div", { children: [
              /* @__PURE__ */ jsxRuntimeExports.jsx("p", { className: "text-sm font-medium text-gray-600", children: "Admins" }),
              /* @__PURE__ */ jsxRuntimeExports.jsx("p", { className: "text-2xl font-bold text-red-600", children: usersData?.users?.filter((u) => u.role === "ADMIN").length || 0 })
            ] }),
            /* @__PURE__ */ jsxRuntimeExports.jsx("div", { className: "w-12 h-12 bg-red-100 rounded-lg flex items-center justify-center", children: /* @__PURE__ */ jsxRuntimeExports.jsx(Shield, { className: "h-6 w-6 text-red-600" }) })
          ] }) }) }),
          /* @__PURE__ */ jsxRuntimeExports.jsx(Card, { children: /* @__PURE__ */ jsxRuntimeExports.jsx(CardContent, { className: "p-6", children: /* @__PURE__ */ jsxRuntimeExports.jsxs("div", { className: "flex items-center justify-between", children: [
            /* @__PURE__ */ jsxRuntimeExports.jsxs("div", { children: [
              /* @__PURE__ */ jsxRuntimeExports.jsx("p", { className: "text-sm font-medium text-gray-600", children: "Suspended" }),
              /* @__PURE__ */ jsxRuntimeExports.jsx("p", { className: "text-2xl font-bold text-yellow-600", children: usersData?.users?.filter((u) => u.status === "SUSPENDED").length || 0 })
            ] }),
            /* @__PURE__ */ jsxRuntimeExports.jsx("div", { className: "w-12 h-12 bg-yellow-100 rounded-lg flex items-center justify-center", children: /* @__PURE__ */ jsxRuntimeExports.jsx(Ban, { className: "h-6 w-6 text-yellow-600" }) })
          ] }) }) })
        ] }),
        /* @__PURE__ */ jsxRuntimeExports.jsx(
          DataTable,
          {
            data: usersData?.users || [],
            columns,
            loading: isLoading,
            searchable: true,
            searchPlaceholder: "Search users by name or email...",
            searchFields: ["name", "email"],
            selectable: true,
            sortable: true,
            exportable: true,
            onExport: handleExport,
            actions: [
              {
                label: "Activate",
                icon: /* @__PURE__ */ jsxRuntimeExports.jsx(CheckCircle, { className: "h-4 w-4 mr-1" }),
                onClick: (users) => handleBulkAction(users, "activate"),
                variant: "secondary"
              },
              {
                label: "Deactivate",
                icon: /* @__PURE__ */ jsxRuntimeExports.jsx(Ban, { className: "h-4 w-4 mr-1" }),
                onClick: (users) => handleBulkAction(users, "deactivate"),
                variant: "secondary"
              },
              {
                label: "Suspend",
                icon: /* @__PURE__ */ jsxRuntimeExports.jsx(Ban, { className: "h-4 w-4 mr-1" }),
                onClick: (users) => handleBulkAction(users, "suspend"),
                variant: "secondary"
              },
              {
                label: "Delete",
                icon: /* @__PURE__ */ jsxRuntimeExports.jsx(Trash2, { className: "h-4 w-4 mr-1" }),
                onClick: (users) => handleBulkAction(users, "delete"),
                variant: "destructive"
              }
            ],
            pagination: usersData ? {
              current: filters.page || 1,
              pageSize: filters.limit || 20,
              total: usersData.total_count,
              onChange: (page, pageSize) => {
                setFilters((prev) => ({ ...prev, page, limit: pageSize }));
              }
            } : void 0,
            emptyMessage: "No users found"
          }
        ),
        error && /* @__PURE__ */ jsxRuntimeExports.jsx(Alert, { variant: "destructive", className: "mt-4", children: /* @__PURE__ */ jsxRuntimeExports.jsxs(AlertDescription, { children: [
          "Failed to load users: ",
          error.message
        ] }) })
      ] }),
      /* @__PURE__ */ jsxRuntimeExports.jsx(TabsContent, { value: "roles", className: "space-y-6", children: /* @__PURE__ */ jsxRuntimeExports.jsx(
        RoleVisualization,
        {
          users: usersData?.users || [],
          onRoleUpdate: handleUpdateUserRole
        }
      ) }),
      /* @__PURE__ */ jsxRuntimeExports.jsx(TabsContent, { value: "activities", className: "space-y-6", children: /* @__PURE__ */ jsxRuntimeExports.jsx(UserActivityLogs, {}) }),
      /* @__PURE__ */ jsxRuntimeExports.jsx(TabsContent, { value: "analytics", className: "space-y-6", children: /* @__PURE__ */ jsxRuntimeExports.jsxs("div", { className: "grid gap-6 md:grid-cols-2", children: [
        /* @__PURE__ */ jsxRuntimeExports.jsxs(Card, { children: [
          /* @__PURE__ */ jsxRuntimeExports.jsx(CardHeader, { children: /* @__PURE__ */ jsxRuntimeExports.jsx(CardTitle, { children: "User Registration Trends" }) }),
          /* @__PURE__ */ jsxRuntimeExports.jsx(CardContent, { children: /* @__PURE__ */ jsxRuntimeExports.jsxs("div", { className: "text-center py-8 text-gray-500", children: [
            /* @__PURE__ */ jsxRuntimeExports.jsx(Calendar, { className: "w-12 h-12 mx-auto mb-4 opacity-50" }),
            /* @__PURE__ */ jsxRuntimeExports.jsx("p", { children: "User registration analytics" }),
            /* @__PURE__ */ jsxRuntimeExports.jsx("p", { className: "text-sm", children: "Charts showing user growth over time" })
          ] }) })
        ] }),
        /* @__PURE__ */ jsxRuntimeExports.jsxs(Card, { children: [
          /* @__PURE__ */ jsxRuntimeExports.jsx(CardHeader, { children: /* @__PURE__ */ jsxRuntimeExports.jsx(CardTitle, { children: "Activity Metrics" }) }),
          /* @__PURE__ */ jsxRuntimeExports.jsx(CardContent, { children: /* @__PURE__ */ jsxRuntimeExports.jsxs("div", { className: "text-center py-8 text-gray-500", children: [
            /* @__PURE__ */ jsxRuntimeExports.jsx(Activity, { className: "w-12 h-12 mx-auto mb-4 opacity-50" }),
            /* @__PURE__ */ jsxRuntimeExports.jsx("p", { children: "User activity metrics" }),
            /* @__PURE__ */ jsxRuntimeExports.jsx("p", { className: "text-sm", children: "Login frequency, session duration, feature usage" })
          ] }) })
        ] })
      ] }) })
    ] }),
    /* @__PURE__ */ jsxRuntimeExports.jsx(Dialog, { open: isActivityDialogOpen, onOpenChange: setIsActivityDialogOpen, children: /* @__PURE__ */ jsxRuntimeExports.jsxs(DialogContent, { className: "max-w-4xl", children: [
      /* @__PURE__ */ jsxRuntimeExports.jsx(DialogHeader, { children: /* @__PURE__ */ jsxRuntimeExports.jsx(DialogTitle, { children: "User Activity Log" }) }),
      selectedUserId && /* @__PURE__ */ jsxRuntimeExports.jsx(UserActivityLogs, { userId: selectedUserId })
    ] }) })
  ] });
};
export {
  UserManagementPage
};
