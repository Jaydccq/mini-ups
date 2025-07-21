import { c as createLucideIcon, j as jsxRuntimeExports, r as reactExports, B as Button, T as Truck, a0 as Users, P as Package, an as BarChart3, t as toast } from "./index-D-bA2lkg.js";
import { u as useQuery } from "./useQuery-ChBPFYRJ.js";
import { D as Download, a as adminApi } from "./admin-NK0n_rcZ.js";
import { C as Card, a as CardHeader, b as CardTitle, d as CardContent, c as CardDescription } from "./card-CsLEcWf3.js";
import { B as Badge } from "./badge-2PvS7lZW.js";
import { A as Alert, a as AlertDescription } from "./alert-Qw-zBtar.js";
import { I as Input } from "./input-BHuFzm78.js";
import { L as Label } from "./label-BLnRWPXy.js";
import { S as Select, a as SelectTrigger, b as SelectValue, d as SelectContent, e as SelectItem, T as Tabs, f as TabsList, g as TabsTrigger, h as TabsContent } from "./tabs-BW6p3V7a.js";
import { R as RefreshCw } from "./refresh-cw-DEEM45_N.js";
import { A as Activity } from "./activity-D4A68VaF.js";
import { A as AlertTriangle, D as DollarSign } from "./dollar-sign-BbdAF-ke.js";
import { T as TrendingUp } from "./trending-up-BuGMLgFK.js";
import { C as Clock } from "./clock-BBOft3LF.js";
import { S as Star } from "./star-DGBC-hS9.js";
import "./chevron-up-5dIABbZ-.js";
import "./check-DBMoG0qQ.js";
/**
 * @license lucide-react v0.309.0 - ISC
 *
 * This source code is licensed under the ISC license.
 * See the LICENSE file in the root directory of this source tree.
 */
const LineChart$1 = createLucideIcon("LineChart", [
  ["path", { d: "M3 3v18h18", key: "1s2lah" }],
  ["path", { d: "m19 9-5 5-4-4-3 3", key: "2osh9i" }]
]);
/**
 * @license lucide-react v0.309.0 - ISC
 *
 * This source code is licensed under the ISC license.
 * See the LICENSE file in the root directory of this source tree.
 */
const Target = createLucideIcon("Target", [
  ["circle", { cx: "12", cy: "12", r: "10", key: "1mglay" }],
  ["circle", { cx: "12", cy: "12", r: "6", key: "1vlfrh" }],
  ["circle", { cx: "12", cy: "12", r: "2", key: "1c9p78" }]
]);
/**
 * @license lucide-react v0.309.0 - ISC
 *
 * This source code is licensed under the ISC license.
 * See the LICENSE file in the root directory of this source tree.
 */
const TrendingDown = createLucideIcon("TrendingDown", [
  ["polyline", { points: "22 17 13.5 8.5 8.5 13.5 2 7", key: "1r2t7k" }],
  ["polyline", { points: "16 17 22 17 22 11", key: "11uiuu" }]
]);
/**
 * @license lucide-react v0.309.0 - ISC
 *
 * This source code is licensed under the ISC license.
 * See the LICENSE file in the root directory of this source tree.
 */
const Zap = createLucideIcon("Zap", [
  ["polygon", { points: "13 2 3 14 12 14 11 22 21 10 12 10 13 2", key: "45s27k" }]
]);
const LineChart = ({
  width = 400,
  height = 300,
  data,
  children
}) => /* @__PURE__ */ jsxRuntimeExports.jsxs("div", { className: "relative bg-white border rounded-lg p-4", style: { width, height }, children: [
  /* @__PURE__ */ jsxRuntimeExports.jsxs("svg", { width: width - 32, height: height - 32, className: "overflow-visible", children: [
    /* @__PURE__ */ jsxRuntimeExports.jsx("defs", { children: /* @__PURE__ */ jsxRuntimeExports.jsxs("linearGradient", { id: "lineGradient", x1: "0%", y1: "0%", x2: "0%", y2: "100%", children: [
      /* @__PURE__ */ jsxRuntimeExports.jsx("stop", { offset: "0%", stopColor: "#3b82f6", stopOpacity: 0.8 }),
      /* @__PURE__ */ jsxRuntimeExports.jsx("stop", { offset: "100%", stopColor: "#3b82f6", stopOpacity: 0.1 })
    ] }) }),
    Array.from({ length: 5 }).map((_, i) => /* @__PURE__ */ jsxRuntimeExports.jsx(
      "line",
      {
        x1: 0,
        y1: (height - 32) / 4 * i,
        x2: width - 32,
        y2: (height - 32) / 4 * i,
        stroke: "#f1f5f9",
        strokeWidth: 1
      },
      i
    )),
    data.length > 1 && /* @__PURE__ */ jsxRuntimeExports.jsx(
      "polyline",
      {
        fill: "none",
        stroke: "#3b82f6",
        strokeWidth: 2,
        points: data.map(
          (_, i) => `${i / (data.length - 1) * (width - 32)},${height - 32 - (Math.random() * (height - 64) + 32)}`
        ).join(" ")
      }
    )
  ] }),
  children
] });
const PieChart = ({
  width = 300,
  height = 300,
  data,
  children
}) => {
  const radius = Math.min(width, height) / 4;
  const centerX = (width - 32) / 2;
  const centerY = (height - 32) / 2;
  const colors = ["#3b82f6", "#10b981", "#f59e0b", "#ef4444", "#8b5cf6"];
  return /* @__PURE__ */ jsxRuntimeExports.jsxs("div", { className: "relative bg-white border rounded-lg p-4", style: { width, height }, children: [
    /* @__PURE__ */ jsxRuntimeExports.jsx("svg", { width: width - 32, height: height - 32, children: data.map((_, i) => {
      const startAngle = i / data.length * 2 * Math.PI;
      const endAngle = (i + 1) / data.length * 2 * Math.PI;
      const x1 = centerX + radius * Math.cos(startAngle);
      const y1 = centerY + radius * Math.sin(startAngle);
      const x2 = centerX + radius * Math.cos(endAngle);
      const y2 = centerY + radius * Math.sin(endAngle);
      const largeArcFlag = endAngle - startAngle > Math.PI ? 1 : 0;
      return /* @__PURE__ */ jsxRuntimeExports.jsx(
        "path",
        {
          d: `M ${centerX},${centerY} L ${x1},${y1} A ${radius},${radius} 0 ${largeArcFlag},1 ${x2},${y2} Z`,
          fill: colors[i % colors.length],
          opacity: 0.8
        },
        i
      );
    }) }),
    children
  ] });
};
const XAxis = ({ dataKey }) => null;
const YAxis = () => null;
const CartesianGrid = () => null;
const Tooltip = () => null;
const Legend = () => null;
const Line = () => null;
const Cell = () => null;
const AdvancedAnalytics = ({ className = "" }) => {
  const [timeRange, setTimeRange] = reactExports.useState("30d");
  const [autoRefresh, setAutoRefresh] = reactExports.useState(true);
  const [lastUpdate, setLastUpdate] = reactExports.useState(/* @__PURE__ */ new Date());
  const [analyticsData, setAnalyticsData] = reactExports.useState(null);
  reactExports.useEffect(() => {
    const fetchAnalyticsData = () => {
      setAnalyticsData({
        deliveryEfficiency: {
          onTimeDeliveries: 94.5 + Math.random() * 2,
          averageDeliveryTime: 2.8 + Math.random() * 0.5,
          fuelEfficiency: 12.4 + Math.random() * 1,
          routeOptimization: 87.2 + Math.random() * 3
        },
        operationalMetrics: {
          truckUtilization: 78.5 + Math.random() * 5,
          driverProductivity: 91.2 + Math.random() * 3,
          warehouseEfficiency: 89.7 + Math.random() * 2,
          maintenanceCosts: 145.8 + Math.random() * 10
        },
        customerMetrics: {
          satisfactionScore: 4.6 + Math.random() * 0.3,
          retentionRate: 82.4 + Math.random() * 3,
          complaintResolution: 96.8 + Math.random() * 2,
          repeatCustomers: 67.9 + Math.random() * 4
        },
        financialMetrics: {
          revenueGrowth: 12.8 + Math.random() * 3,
          profitMargin: 18.5 + Math.random() * 2,
          costPerDelivery: 8.4 + Math.random() * 1,
          customerAcquisitionCost: 24.6 + Math.random() * 5
        }
      });
      setLastUpdate(/* @__PURE__ */ new Date());
    };
    fetchAnalyticsData();
    let interval;
    if (autoRefresh) {
      interval = setInterval(fetchAnalyticsData, 3e4);
    }
    return () => {
      if (interval) clearInterval(interval);
    };
  }, [autoRefresh, timeRange]);
  const metrics = reactExports.useMemo(() => {
    if (!analyticsData) return [];
    return [
      // Efficiency Metrics
      {
        name: "On-Time Deliveries",
        value: analyticsData.deliveryEfficiency.onTimeDeliveries,
        change: 2.3,
        trend: "up",
        target: 95,
        unit: "%",
        category: "efficiency"
      },
      {
        name: "Avg Delivery Time",
        value: analyticsData.deliveryEfficiency.averageDeliveryTime,
        change: -0.2,
        trend: "up",
        target: 3,
        unit: "days",
        category: "efficiency"
      },
      {
        name: "Fuel Efficiency",
        value: analyticsData.deliveryEfficiency.fuelEfficiency,
        change: 1.8,
        trend: "up",
        unit: "L/100km",
        category: "efficiency"
      },
      {
        name: "Route Optimization",
        value: analyticsData.deliveryEfficiency.routeOptimization,
        change: 3.2,
        trend: "up",
        unit: "%",
        category: "efficiency"
      },
      // Operational Metrics
      {
        name: "Truck Utilization",
        value: analyticsData.operationalMetrics.truckUtilization,
        change: 4.1,
        trend: "up",
        target: 85,
        unit: "%",
        category: "operations"
      },
      {
        name: "Driver Productivity",
        value: analyticsData.operationalMetrics.driverProductivity,
        change: 1.7,
        trend: "up",
        unit: "%",
        category: "operations"
      },
      {
        name: "Warehouse Efficiency",
        value: analyticsData.operationalMetrics.warehouseEfficiency,
        change: -0.8,
        trend: "down",
        target: 92,
        unit: "%",
        category: "operations"
      },
      {
        name: "Maintenance Costs",
        value: analyticsData.operationalMetrics.maintenanceCosts,
        change: -3.2,
        trend: "up",
        unit: "$",
        category: "operations"
      },
      // Customer Metrics
      {
        name: "Customer Satisfaction",
        value: analyticsData.customerMetrics.satisfactionScore,
        change: 0.2,
        trend: "up",
        target: 4.8,
        unit: "/5",
        category: "satisfaction"
      },
      {
        name: "Retention Rate",
        value: analyticsData.customerMetrics.retentionRate,
        change: 2.1,
        trend: "up",
        target: 85,
        unit: "%",
        category: "satisfaction"
      },
      {
        name: "Complaint Resolution",
        value: analyticsData.customerMetrics.complaintResolution,
        change: 1.5,
        trend: "up",
        target: 98,
        unit: "%",
        category: "satisfaction"
      },
      {
        name: "Repeat Customers",
        value: analyticsData.customerMetrics.repeatCustomers,
        change: 3.8,
        trend: "up",
        unit: "%",
        category: "satisfaction"
      },
      // Financial Metrics
      {
        name: "Revenue Growth",
        value: analyticsData.financialMetrics.revenueGrowth,
        change: 2.4,
        trend: "up",
        unit: "%",
        category: "revenue"
      },
      {
        name: "Profit Margin",
        value: analyticsData.financialMetrics.profitMargin,
        change: 1.2,
        trend: "up",
        target: 20,
        unit: "%",
        category: "revenue"
      },
      {
        name: "Cost per Delivery",
        value: analyticsData.financialMetrics.costPerDelivery,
        change: -1.8,
        trend: "up",
        unit: "$",
        category: "revenue"
      },
      {
        name: "Customer Acquisition Cost",
        value: analyticsData.financialMetrics.customerAcquisitionCost,
        change: -2.1,
        trend: "up",
        target: 25,
        unit: "$",
        category: "revenue"
      }
    ];
  }, [analyticsData]);
  const getMetricsByCategory = (category) => {
    return metrics.filter((m) => m.category === category);
  };
  const getTrendIcon = (trend) => {
    switch (trend) {
      case "up":
        return /* @__PURE__ */ jsxRuntimeExports.jsx(TrendingUp, { className: "w-4 h-4 text-green-600" });
      case "down":
        return /* @__PURE__ */ jsxRuntimeExports.jsx(TrendingDown, { className: "w-4 h-4 text-red-600" });
      default:
        return /* @__PURE__ */ jsxRuntimeExports.jsx(Activity, { className: "w-4 h-4 text-gray-600" });
    }
  };
  const getTargetStatus = (value, target) => {
    if (!target) return null;
    const percentage = value / target * 100;
    if (percentage >= 95) return "excellent";
    if (percentage >= 80) return "good";
    if (percentage >= 60) return "warning";
    return "critical";
  };
  const getStatusColor = (status) => {
    switch (status) {
      case "excellent":
        return "text-green-600";
      case "good":
        return "text-blue-600";
      case "warning":
        return "text-yellow-600";
      case "critical":
        return "text-red-600";
      default:
        return "text-gray-600";
    }
  };
  const formatValue = (value, unit) => {
    const formattedValue = unit === "$" ? `$${value.toFixed(1)}` : `${value.toFixed(1)}${unit || ""}`;
    return formattedValue;
  };
  const categories = [
    { id: "efficiency", name: "Efficiency", icon: /* @__PURE__ */ jsxRuntimeExports.jsx(Zap, { className: "w-4 h-4" }), color: "text-yellow-600" },
    { id: "operations", name: "Operations", icon: /* @__PURE__ */ jsxRuntimeExports.jsx(Truck, { className: "w-4 h-4" }), color: "text-blue-600" },
    { id: "satisfaction", name: "Customer", icon: /* @__PURE__ */ jsxRuntimeExports.jsx(Users, { className: "w-4 h-4" }), color: "text-purple-600" },
    { id: "revenue", name: "Financial", icon: /* @__PURE__ */ jsxRuntimeExports.jsx(DollarSign, { className: "w-4 h-4" }), color: "text-green-600" }
  ];
  return /* @__PURE__ */ jsxRuntimeExports.jsxs("div", { className, children: [
    /* @__PURE__ */ jsxRuntimeExports.jsxs("div", { className: "mb-6", children: [
      /* @__PURE__ */ jsxRuntimeExports.jsxs("div", { className: "flex justify-between items-center mb-4", children: [
        /* @__PURE__ */ jsxRuntimeExports.jsxs("div", { children: [
          /* @__PURE__ */ jsxRuntimeExports.jsx("h2", { className: "text-2xl font-bold", children: "Advanced Analytics" }),
          /* @__PURE__ */ jsxRuntimeExports.jsx("p", { className: "text-gray-600", children: "Real-time operational intelligence and performance insights" })
        ] }),
        /* @__PURE__ */ jsxRuntimeExports.jsxs("div", { className: "flex items-center gap-2", children: [
          /* @__PURE__ */ jsxRuntimeExports.jsxs(Select, { value: timeRange, onValueChange: setTimeRange, children: [
            /* @__PURE__ */ jsxRuntimeExports.jsx(SelectTrigger, { className: "w-[140px]", children: /* @__PURE__ */ jsxRuntimeExports.jsx(SelectValue, {}) }),
            /* @__PURE__ */ jsxRuntimeExports.jsxs(SelectContent, { children: [
              /* @__PURE__ */ jsxRuntimeExports.jsx(SelectItem, { value: "7d", children: "Last 7 days" }),
              /* @__PURE__ */ jsxRuntimeExports.jsx(SelectItem, { value: "30d", children: "Last 30 days" }),
              /* @__PURE__ */ jsxRuntimeExports.jsx(SelectItem, { value: "90d", children: "Last 90 days" }),
              /* @__PURE__ */ jsxRuntimeExports.jsx(SelectItem, { value: "1y", children: "Last year" })
            ] })
          ] }),
          /* @__PURE__ */ jsxRuntimeExports.jsxs(
            Button,
            {
              variant: autoRefresh ? "default" : "outline",
              size: "sm",
              onClick: () => setAutoRefresh(!autoRefresh),
              children: [
                /* @__PURE__ */ jsxRuntimeExports.jsx(RefreshCw, { className: `w-4 h-4 mr-2 ${autoRefresh ? "animate-spin" : ""}` }),
                "Auto Refresh"
              ]
            }
          )
        ] })
      ] }),
      /* @__PURE__ */ jsxRuntimeExports.jsxs("div", { className: "text-xs text-gray-500 mb-4", children: [
        "Last updated: ",
        lastUpdate.toLocaleTimeString(),
        autoRefresh && /* @__PURE__ */ jsxRuntimeExports.jsx("span", { className: "ml-2 text-green-600", children: "• Live" })
      ] })
    ] }),
    /* @__PURE__ */ jsxRuntimeExports.jsxs(Tabs, { defaultValue: "overview", className: "w-full", children: [
      /* @__PURE__ */ jsxRuntimeExports.jsxs(TabsList, { className: "grid w-full grid-cols-4", children: [
        /* @__PURE__ */ jsxRuntimeExports.jsx(TabsTrigger, { value: "overview", children: "Overview" }),
        /* @__PURE__ */ jsxRuntimeExports.jsx(TabsTrigger, { value: "performance", children: "Performance" }),
        /* @__PURE__ */ jsxRuntimeExports.jsx(TabsTrigger, { value: "trends", children: "Trends" }),
        /* @__PURE__ */ jsxRuntimeExports.jsx(TabsTrigger, { value: "forecasting", children: "Forecasting" })
      ] }),
      /* @__PURE__ */ jsxRuntimeExports.jsx(TabsContent, { value: "overview", className: "space-y-6", children: categories.map((category) => {
        const categoryMetrics = getMetricsByCategory(category.id);
        return /* @__PURE__ */ jsxRuntimeExports.jsxs(Card, { children: [
          /* @__PURE__ */ jsxRuntimeExports.jsx(CardHeader, { children: /* @__PURE__ */ jsxRuntimeExports.jsxs(CardTitle, { className: `flex items-center gap-2 ${category.color}`, children: [
            category.icon,
            category.name,
            " Metrics"
          ] }) }),
          /* @__PURE__ */ jsxRuntimeExports.jsx(CardContent, { children: /* @__PURE__ */ jsxRuntimeExports.jsx("div", { className: "grid gap-4 md:grid-cols-2 lg:grid-cols-4", children: categoryMetrics.map((metric) => {
            const targetStatus = getTargetStatus(metric.value, metric.target);
            return /* @__PURE__ */ jsxRuntimeExports.jsxs("div", { className: "p-4 border rounded-lg", children: [
              /* @__PURE__ */ jsxRuntimeExports.jsxs("div", { className: "flex justify-between items-start mb-2", children: [
                /* @__PURE__ */ jsxRuntimeExports.jsx("h4", { className: "text-sm font-medium text-gray-700", children: metric.name }),
                getTrendIcon(metric.trend)
              ] }),
              /* @__PURE__ */ jsxRuntimeExports.jsxs("div", { className: "space-y-1", children: [
                /* @__PURE__ */ jsxRuntimeExports.jsx("div", { className: "text-2xl font-bold", children: formatValue(metric.value, metric.unit) }),
                /* @__PURE__ */ jsxRuntimeExports.jsxs("div", { className: "flex items-center justify-between text-xs", children: [
                  /* @__PURE__ */ jsxRuntimeExports.jsxs("span", { className: metric.change >= 0 ? "text-green-600" : "text-red-600", children: [
                    metric.change >= 0 ? "+" : "",
                    metric.change.toFixed(1),
                    "%"
                  ] }),
                  metric.target && /* @__PURE__ */ jsxRuntimeExports.jsxs("span", { className: getStatusColor(targetStatus), children: [
                    "Target: ",
                    formatValue(metric.target, metric.unit)
                  ] })
                ] }),
                metric.target && /* @__PURE__ */ jsxRuntimeExports.jsx("div", { className: "w-full bg-gray-200 rounded-full h-1.5 mt-2", children: /* @__PURE__ */ jsxRuntimeExports.jsx(
                  "div",
                  {
                    className: `h-1.5 rounded-full ${targetStatus === "excellent" ? "bg-green-600" : targetStatus === "good" ? "bg-blue-600" : targetStatus === "warning" ? "bg-yellow-600" : "bg-red-600"}`,
                    style: { width: `${Math.min(metric.value / metric.target * 100, 100)}%` }
                  }
                ) })
              ] })
            ] }, metric.name);
          }) }) })
        ] }, category.id);
      }) }),
      /* @__PURE__ */ jsxRuntimeExports.jsx(TabsContent, { value: "performance", className: "space-y-6", children: /* @__PURE__ */ jsxRuntimeExports.jsxs("div", { className: "grid gap-6 md:grid-cols-2", children: [
        /* @__PURE__ */ jsxRuntimeExports.jsxs(Card, { children: [
          /* @__PURE__ */ jsxRuntimeExports.jsx(CardHeader, { children: /* @__PURE__ */ jsxRuntimeExports.jsxs(CardTitle, { className: "flex items-center gap-2", children: [
            /* @__PURE__ */ jsxRuntimeExports.jsx(Activity, { className: "w-5 h-5" }),
            "Real-time Performance"
          ] }) }),
          /* @__PURE__ */ jsxRuntimeExports.jsx(CardContent, { children: /* @__PURE__ */ jsxRuntimeExports.jsxs("div", { className: "space-y-4", children: [
            /* @__PURE__ */ jsxRuntimeExports.jsxs("div", { className: "flex justify-between items-center", children: [
              /* @__PURE__ */ jsxRuntimeExports.jsx("span", { className: "text-sm text-gray-600", children: "System Load" }),
              /* @__PURE__ */ jsxRuntimeExports.jsx("span", { className: "text-sm font-medium", children: "78%" })
            ] }),
            /* @__PURE__ */ jsxRuntimeExports.jsx("div", { className: "w-full bg-gray-200 rounded-full h-2", children: /* @__PURE__ */ jsxRuntimeExports.jsx("div", { className: "bg-blue-600 h-2 rounded-full", style: { width: "78%" } }) }),
            /* @__PURE__ */ jsxRuntimeExports.jsxs("div", { className: "flex justify-between items-center", children: [
              /* @__PURE__ */ jsxRuntimeExports.jsx("span", { className: "text-sm text-gray-600", children: "API Response Time" }),
              /* @__PURE__ */ jsxRuntimeExports.jsx("span", { className: "text-sm font-medium", children: "124ms" })
            ] }),
            /* @__PURE__ */ jsxRuntimeExports.jsx("div", { className: "w-full bg-gray-200 rounded-full h-2", children: /* @__PURE__ */ jsxRuntimeExports.jsx("div", { className: "bg-green-600 h-2 rounded-full", style: { width: "85%" } }) }),
            /* @__PURE__ */ jsxRuntimeExports.jsxs("div", { className: "flex justify-between items-center", children: [
              /* @__PURE__ */ jsxRuntimeExports.jsx("span", { className: "text-sm text-gray-600", children: "Active Connections" }),
              /* @__PURE__ */ jsxRuntimeExports.jsx("span", { className: "text-sm font-medium", children: "1,247" })
            ] }),
            /* @__PURE__ */ jsxRuntimeExports.jsx("div", { className: "w-full bg-gray-200 rounded-full h-2", children: /* @__PURE__ */ jsxRuntimeExports.jsx("div", { className: "bg-yellow-600 h-2 rounded-full", style: { width: "62%" } }) })
          ] }) })
        ] }),
        /* @__PURE__ */ jsxRuntimeExports.jsxs(Card, { children: [
          /* @__PURE__ */ jsxRuntimeExports.jsx(CardHeader, { children: /* @__PURE__ */ jsxRuntimeExports.jsxs(CardTitle, { className: "flex items-center gap-2", children: [
            /* @__PURE__ */ jsxRuntimeExports.jsx(AlertTriangle, { className: "w-5 h-5" }),
            "Alert Summary"
          ] }) }),
          /* @__PURE__ */ jsxRuntimeExports.jsx(CardContent, { children: /* @__PURE__ */ jsxRuntimeExports.jsxs("div", { className: "space-y-3", children: [
            /* @__PURE__ */ jsxRuntimeExports.jsxs("div", { className: "flex items-center justify-between p-3 bg-red-50 rounded-lg", children: [
              /* @__PURE__ */ jsxRuntimeExports.jsxs("div", { className: "flex items-center gap-2", children: [
                /* @__PURE__ */ jsxRuntimeExports.jsx("div", { className: "w-2 h-2 bg-red-500 rounded-full" }),
                /* @__PURE__ */ jsxRuntimeExports.jsx("span", { className: "text-sm font-medium", children: "Critical" })
              ] }),
              /* @__PURE__ */ jsxRuntimeExports.jsx(Badge, { variant: "destructive", children: "2" })
            ] }),
            /* @__PURE__ */ jsxRuntimeExports.jsxs("div", { className: "flex items-center justify-between p-3 bg-yellow-50 rounded-lg", children: [
              /* @__PURE__ */ jsxRuntimeExports.jsxs("div", { className: "flex items-center gap-2", children: [
                /* @__PURE__ */ jsxRuntimeExports.jsx("div", { className: "w-2 h-2 bg-yellow-500 rounded-full" }),
                /* @__PURE__ */ jsxRuntimeExports.jsx("span", { className: "text-sm font-medium", children: "Warning" })
              ] }),
              /* @__PURE__ */ jsxRuntimeExports.jsx(Badge, { variant: "outline", children: "5" })
            ] }),
            /* @__PURE__ */ jsxRuntimeExports.jsxs("div", { className: "flex items-center justify-between p-3 bg-blue-50 rounded-lg", children: [
              /* @__PURE__ */ jsxRuntimeExports.jsxs("div", { className: "flex items-center gap-2", children: [
                /* @__PURE__ */ jsxRuntimeExports.jsx("div", { className: "w-2 h-2 bg-blue-500 rounded-full" }),
                /* @__PURE__ */ jsxRuntimeExports.jsx("span", { className: "text-sm font-medium", children: "Info" })
              ] }),
              /* @__PURE__ */ jsxRuntimeExports.jsx(Badge, { variant: "secondary", children: "12" })
            ] })
          ] }) })
        ] })
      ] }) }),
      /* @__PURE__ */ jsxRuntimeExports.jsx(TabsContent, { value: "trends", className: "space-y-6", children: /* @__PURE__ */ jsxRuntimeExports.jsxs(Card, { children: [
        /* @__PURE__ */ jsxRuntimeExports.jsx(CardHeader, { children: /* @__PURE__ */ jsxRuntimeExports.jsx(CardTitle, { children: "Trend Analysis" }) }),
        /* @__PURE__ */ jsxRuntimeExports.jsx(CardContent, { children: /* @__PURE__ */ jsxRuntimeExports.jsxs("div", { className: "text-center py-8 text-gray-500", children: [
          /* @__PURE__ */ jsxRuntimeExports.jsx(LineChart$1, { className: "w-12 h-12 mx-auto mb-4 opacity-50" }),
          /* @__PURE__ */ jsxRuntimeExports.jsx("p", { children: "Advanced trend analysis charts" }),
          /* @__PURE__ */ jsxRuntimeExports.jsx("p", { className: "text-sm", children: "Historical data patterns and seasonal trends" })
        ] }) })
      ] }) }),
      /* @__PURE__ */ jsxRuntimeExports.jsx(TabsContent, { value: "forecasting", className: "space-y-6", children: /* @__PURE__ */ jsxRuntimeExports.jsxs(Card, { children: [
        /* @__PURE__ */ jsxRuntimeExports.jsx(CardHeader, { children: /* @__PURE__ */ jsxRuntimeExports.jsx(CardTitle, { children: "Predictive Analytics" }) }),
        /* @__PURE__ */ jsxRuntimeExports.jsx(CardContent, { children: /* @__PURE__ */ jsxRuntimeExports.jsxs("div", { className: "text-center py-8 text-gray-500", children: [
          /* @__PURE__ */ jsxRuntimeExports.jsx(Target, { className: "w-12 h-12 mx-auto mb-4 opacity-50" }),
          /* @__PURE__ */ jsxRuntimeExports.jsx("p", { children: "Machine learning forecasts" }),
          /* @__PURE__ */ jsxRuntimeExports.jsx("p", { className: "text-sm", children: "Demand prediction and capacity planning" })
        ] }) })
      ] }) })
    ] })
  ] });
};
const AnalyticsPage = () => {
  const [dateRange, setDateRange] = reactExports.useState({
    from: new Date(Date.now() - 30 * 24 * 60 * 60 * 1e3).toISOString().split("T")[0],
    // 30 days ago
    to: (/* @__PURE__ */ new Date()).toISOString().split("T")[0]
    // today
  });
  const { data: analyticsData, isLoading, error, refetch } = useQuery({
    queryKey: ["admin", "analytics", dateRange],
    queryFn: () => adminApi.getAnalytics(dateRange),
    refetchInterval: 5 * 60 * 1e3
    // Refresh every 5 minutes
  });
  const handleExportAnalytics = () => {
    adminApi.exportData("analytics", dateRange).then((blob) => {
      const url = window.URL.createObjectURL(blob);
      const a = document.createElement("a");
      a.href = url;
      a.download = `analytics-${dateRange.from}-to-${dateRange.to}.pdf`;
      document.body.appendChild(a);
      a.click();
      window.URL.revokeObjectURL(url);
      document.body.removeChild(a);
      toast.success("Analytics report exported successfully");
    }).catch((error2) => {
      toast.error(`Export failed: ${error2.message}`);
    });
  };
  const handleRefresh = () => {
    refetch();
    toast.success("Data refreshed");
  };
  const formatCurrency = (amount) => {
    return new Intl.NumberFormat("en-US", {
      style: "currency",
      currency: "USD"
    }).format(amount);
  };
  const formatPercentage = (value) => {
    return `${value > 0 ? "+" : ""}${value.toFixed(1)}%`;
  };
  const shipmentTrendsData = analyticsData?.shipment_trends || [
    { date: "2024-01-01", shipments: 150, revenue: 2250 },
    { date: "2024-01-02", shipments: 230, revenue: 3450 },
    { date: "2024-01-03", shipments: 180, revenue: 2700 },
    { date: "2024-01-04", shipments: 320, revenue: 4800 },
    { date: "2024-01-05", shipments: 280, revenue: 4200 },
    { date: "2024-01-06", shipments: 190, revenue: 2850 },
    { date: "2024-01-07", shipments: 250, revenue: 3750 }
  ];
  const statusDistributionData = analyticsData?.status_distribution || [
    { status: "Delivered", count: 1250, percentage: 62.5 },
    { status: "In Transit", count: 380, percentage: 19 },
    { status: "Pending", count: 200, percentage: 10 },
    { status: "Cancelled", count: 120, percentage: 6 },
    { status: "Returned", count: 50, percentage: 2.5 }
  ];
  const topCustomersData = analyticsData?.top_customers || [
    { user_id: "1", name: "John Smith", email: "john@example.com", shipment_count: 45, total_spent: 2250 },
    { user_id: "2", name: "Alice Johnson", email: "alice@example.com", shipment_count: 38, total_spent: 1900 },
    { user_id: "3", name: "Bob Wilson", email: "bob@example.com", shipment_count: 32, total_spent: 1600 },
    { user_id: "4", name: "Carol Davis", email: "carol@example.com", shipment_count: 28, total_spent: 1400 },
    { user_id: "5", name: "David Brown", email: "david@example.com", shipment_count: 25, total_spent: 1250 }
  ];
  const metrics = analyticsData?.metrics || {
    total_shipments: 2e3,
    active_shipments: 500,
    delivered_shipments: 1250,
    total_revenue: 75e3,
    avg_delivery_time: 3.2,
    customer_satisfaction: 4.7
  };
  const monthlyStats = analyticsData?.monthly_stats || {
    growth_rate: 11.1
  };
  const pieColors = ["#3b82f6", "#10b981", "#f59e0b", "#ef4444", "#8b5cf6"];
  if (isLoading) {
    return /* @__PURE__ */ jsxRuntimeExports.jsx("div", { className: "p-6 max-w-7xl mx-auto", children: /* @__PURE__ */ jsxRuntimeExports.jsx("div", { className: "space-y-6", children: Array.from({ length: 4 }).map((_, i) => /* @__PURE__ */ jsxRuntimeExports.jsx("div", { className: "h-64 bg-gray-200 rounded-lg animate-pulse" }, i)) }) });
  }
  return /* @__PURE__ */ jsxRuntimeExports.jsxs("div", { className: "p-6 max-w-7xl mx-auto", children: [
    /* @__PURE__ */ jsxRuntimeExports.jsx("div", { className: "mb-8", children: /* @__PURE__ */ jsxRuntimeExports.jsxs("div", { className: "flex items-center justify-between", children: [
      /* @__PURE__ */ jsxRuntimeExports.jsxs("div", { children: [
        /* @__PURE__ */ jsxRuntimeExports.jsx("h1", { className: "text-3xl font-bold text-gray-900", children: "Analytics Dashboard" }),
        /* @__PURE__ */ jsxRuntimeExports.jsx("p", { className: "text-gray-600", children: "Comprehensive business insights and operational intelligence" })
      ] }),
      /* @__PURE__ */ jsxRuntimeExports.jsxs("div", { className: "flex items-center gap-4", children: [
        /* @__PURE__ */ jsxRuntimeExports.jsxs(Button, { variant: "outline", onClick: handleRefresh, children: [
          /* @__PURE__ */ jsxRuntimeExports.jsx(RefreshCw, { className: "h-4 w-4 mr-2" }),
          "Refresh"
        ] }),
        /* @__PURE__ */ jsxRuntimeExports.jsxs(Button, { onClick: handleExportAnalytics, children: [
          /* @__PURE__ */ jsxRuntimeExports.jsx(Download, { className: "h-4 w-4 mr-2" }),
          "Export Report"
        ] })
      ] })
    ] }) }),
    /* @__PURE__ */ jsxRuntimeExports.jsxs(Tabs, { defaultValue: "overview", className: "w-full", children: [
      /* @__PURE__ */ jsxRuntimeExports.jsxs(TabsList, { className: "grid w-full grid-cols-3", children: [
        /* @__PURE__ */ jsxRuntimeExports.jsx(TabsTrigger, { value: "overview", children: "Overview" }),
        /* @__PURE__ */ jsxRuntimeExports.jsx(TabsTrigger, { value: "advanced", children: "Advanced Analytics" }),
        /* @__PURE__ */ jsxRuntimeExports.jsx(TabsTrigger, { value: "reports", children: "Detailed Reports" })
      ] }),
      /* @__PURE__ */ jsxRuntimeExports.jsxs(TabsContent, { value: "overview", className: "space-y-6", children: [
        /* @__PURE__ */ jsxRuntimeExports.jsxs("div", { className: "flex items-center gap-4", children: [
          /* @__PURE__ */ jsxRuntimeExports.jsxs("div", { className: "flex items-center gap-2", children: [
            /* @__PURE__ */ jsxRuntimeExports.jsx(Label, { htmlFor: "date-from", children: "From:" }),
            /* @__PURE__ */ jsxRuntimeExports.jsx(
              Input,
              {
                id: "date-from",
                type: "date",
                value: dateRange.from,
                onChange: (e) => setDateRange((prev) => ({ ...prev, from: e.target.value })),
                className: "w-auto"
              }
            )
          ] }),
          /* @__PURE__ */ jsxRuntimeExports.jsxs("div", { className: "flex items-center gap-2", children: [
            /* @__PURE__ */ jsxRuntimeExports.jsx(Label, { htmlFor: "date-to", children: "To:" }),
            /* @__PURE__ */ jsxRuntimeExports.jsx(
              Input,
              {
                id: "date-to",
                type: "date",
                value: dateRange.to,
                onChange: (e) => setDateRange((prev) => ({ ...prev, to: e.target.value })),
                className: "w-auto"
              }
            )
          ] })
        ] }),
        /* @__PURE__ */ jsxRuntimeExports.jsxs("div", { className: "grid gap-6 md:grid-cols-2 lg:grid-cols-4", children: [
          /* @__PURE__ */ jsxRuntimeExports.jsx(Card, { children: /* @__PURE__ */ jsxRuntimeExports.jsx(CardContent, { className: "p-6", children: /* @__PURE__ */ jsxRuntimeExports.jsxs("div", { className: "flex items-center justify-between", children: [
            /* @__PURE__ */ jsxRuntimeExports.jsxs("div", { children: [
              /* @__PURE__ */ jsxRuntimeExports.jsx("p", { className: "text-sm font-medium text-gray-600", children: "Total Revenue" }),
              /* @__PURE__ */ jsxRuntimeExports.jsx("p", { className: "text-2xl font-bold text-gray-900", children: formatCurrency(metrics.total_revenue) }),
              /* @__PURE__ */ jsxRuntimeExports.jsxs("p", { className: "text-xs text-green-600 mt-1", children: [
                formatPercentage(monthlyStats.growth_rate),
                " from last month"
              ] })
            ] }),
            /* @__PURE__ */ jsxRuntimeExports.jsx("div", { className: "w-12 h-12 bg-green-100 rounded-lg flex items-center justify-center", children: /* @__PURE__ */ jsxRuntimeExports.jsx(DollarSign, { className: "h-6 w-6 text-green-600" }) })
          ] }) }) }),
          /* @__PURE__ */ jsxRuntimeExports.jsx(Card, { children: /* @__PURE__ */ jsxRuntimeExports.jsx(CardContent, { className: "p-6", children: /* @__PURE__ */ jsxRuntimeExports.jsxs("div", { className: "flex items-center justify-between", children: [
            /* @__PURE__ */ jsxRuntimeExports.jsxs("div", { children: [
              /* @__PURE__ */ jsxRuntimeExports.jsx("p", { className: "text-sm font-medium text-gray-600", children: "Total Shipments" }),
              /* @__PURE__ */ jsxRuntimeExports.jsx("p", { className: "text-2xl font-bold text-gray-900", children: metrics.total_shipments.toLocaleString() }),
              /* @__PURE__ */ jsxRuntimeExports.jsxs("p", { className: "text-xs text-blue-600 mt-1", children: [
                metrics.active_shipments,
                " currently active"
              ] })
            ] }),
            /* @__PURE__ */ jsxRuntimeExports.jsx("div", { className: "w-12 h-12 bg-blue-100 rounded-lg flex items-center justify-center", children: /* @__PURE__ */ jsxRuntimeExports.jsx(Package, { className: "h-6 w-6 text-blue-600" }) })
          ] }) }) }),
          /* @__PURE__ */ jsxRuntimeExports.jsx(Card, { children: /* @__PURE__ */ jsxRuntimeExports.jsx(CardContent, { className: "p-6", children: /* @__PURE__ */ jsxRuntimeExports.jsxs("div", { className: "flex items-center justify-between", children: [
            /* @__PURE__ */ jsxRuntimeExports.jsxs("div", { children: [
              /* @__PURE__ */ jsxRuntimeExports.jsx("p", { className: "text-sm font-medium text-gray-600", children: "Avg Delivery Time" }),
              /* @__PURE__ */ jsxRuntimeExports.jsxs("p", { className: "text-2xl font-bold text-gray-900", children: [
                metrics.avg_delivery_time,
                " days"
              ] }),
              /* @__PURE__ */ jsxRuntimeExports.jsx("p", { className: "text-xs text-yellow-600 mt-1", children: "Industry average: 4.5 days" })
            ] }),
            /* @__PURE__ */ jsxRuntimeExports.jsx("div", { className: "w-12 h-12 bg-yellow-100 rounded-lg flex items-center justify-center", children: /* @__PURE__ */ jsxRuntimeExports.jsx(Clock, { className: "h-6 w-6 text-yellow-600" }) })
          ] }) }) }),
          /* @__PURE__ */ jsxRuntimeExports.jsx(Card, { children: /* @__PURE__ */ jsxRuntimeExports.jsx(CardContent, { className: "p-6", children: /* @__PURE__ */ jsxRuntimeExports.jsxs("div", { className: "flex items-center justify-between", children: [
            /* @__PURE__ */ jsxRuntimeExports.jsxs("div", { children: [
              /* @__PURE__ */ jsxRuntimeExports.jsx("p", { className: "text-sm font-medium text-gray-600", children: "Customer Satisfaction" }),
              /* @__PURE__ */ jsxRuntimeExports.jsxs("p", { className: "text-2xl font-bold text-gray-900", children: [
                metrics.customer_satisfaction,
                "/5.0"
              ] }),
              /* @__PURE__ */ jsxRuntimeExports.jsxs("p", { className: "text-xs text-purple-600 mt-1", children: [
                "Based on ",
                metrics.delivered_shipments,
                " reviews"
              ] })
            ] }),
            /* @__PURE__ */ jsxRuntimeExports.jsx("div", { className: "w-12 h-12 bg-purple-100 rounded-lg flex items-center justify-center", children: /* @__PURE__ */ jsxRuntimeExports.jsx(Star, { className: "h-6 w-6 text-purple-600" }) })
          ] }) }) })
        ] }),
        /* @__PURE__ */ jsxRuntimeExports.jsxs("div", { className: "grid gap-6 lg:grid-cols-2", children: [
          /* @__PURE__ */ jsxRuntimeExports.jsxs(Card, { children: [
            /* @__PURE__ */ jsxRuntimeExports.jsxs(CardHeader, { children: [
              /* @__PURE__ */ jsxRuntimeExports.jsxs(CardTitle, { className: "flex items-center gap-2", children: [
                /* @__PURE__ */ jsxRuntimeExports.jsx(TrendingUp, { className: "h-5 w-5" }),
                "Shipment Trends"
              ] }),
              /* @__PURE__ */ jsxRuntimeExports.jsx(CardDescription, { children: "Daily shipment volume and revenue over time" })
            ] }),
            /* @__PURE__ */ jsxRuntimeExports.jsx(CardContent, { children: /* @__PURE__ */ jsxRuntimeExports.jsxs(LineChart, { width: 500, height: 300, data: shipmentTrendsData, children: [
              /* @__PURE__ */ jsxRuntimeExports.jsx(CartesianGrid, { strokeDasharray: "3 3" }),
              /* @__PURE__ */ jsxRuntimeExports.jsx(XAxis, { dataKey: "date" }),
              /* @__PURE__ */ jsxRuntimeExports.jsx(YAxis, {}),
              /* @__PURE__ */ jsxRuntimeExports.jsx(Tooltip, {}),
              /* @__PURE__ */ jsxRuntimeExports.jsx(Legend, {}),
              /* @__PURE__ */ jsxRuntimeExports.jsx(Line, { type: "monotone", dataKey: "shipments", stroke: "#3b82f6", strokeWidth: 2 }),
              /* @__PURE__ */ jsxRuntimeExports.jsx(Line, { type: "monotone", dataKey: "revenue", stroke: "#10b981", strokeWidth: 2 })
            ] }) })
          ] }),
          /* @__PURE__ */ jsxRuntimeExports.jsxs(Card, { children: [
            /* @__PURE__ */ jsxRuntimeExports.jsxs(CardHeader, { children: [
              /* @__PURE__ */ jsxRuntimeExports.jsxs(CardTitle, { className: "flex items-center gap-2", children: [
                /* @__PURE__ */ jsxRuntimeExports.jsx(BarChart3, { className: "h-5 w-5" }),
                "Shipment Status Distribution"
              ] }),
              /* @__PURE__ */ jsxRuntimeExports.jsx(CardDescription, { children: "Current distribution of shipment statuses" })
            ] }),
            /* @__PURE__ */ jsxRuntimeExports.jsxs(CardContent, { children: [
              /* @__PURE__ */ jsxRuntimeExports.jsxs(PieChart, { width: 500, height: 300, data: statusDistributionData, children: [
                /* @__PURE__ */ jsxRuntimeExports.jsx(Tooltip, {}),
                statusDistributionData.map((entry, index) => /* @__PURE__ */ jsxRuntimeExports.jsx(Cell, { fill: pieColors[index % pieColors.length] }, `cell-${index}`))
              ] }),
              /* @__PURE__ */ jsxRuntimeExports.jsx("div", { className: "mt-4 grid grid-cols-2 gap-2", children: statusDistributionData.map((entry, index) => /* @__PURE__ */ jsxRuntimeExports.jsxs("div", { className: "flex items-center gap-2", children: [
                /* @__PURE__ */ jsxRuntimeExports.jsx(
                  "div",
                  {
                    className: "w-3 h-3 rounded-full",
                    style: { backgroundColor: pieColors[index % pieColors.length] }
                  }
                ),
                /* @__PURE__ */ jsxRuntimeExports.jsxs("span", { className: "text-sm text-gray-600", children: [
                  entry.status,
                  ": ",
                  entry.percentage,
                  "%"
                ] })
              ] }, entry.status)) })
            ] })
          ] })
        ] }),
        /* @__PURE__ */ jsxRuntimeExports.jsxs(Card, { children: [
          /* @__PURE__ */ jsxRuntimeExports.jsxs(CardHeader, { children: [
            /* @__PURE__ */ jsxRuntimeExports.jsxs(CardTitle, { className: "flex items-center gap-2", children: [
              /* @__PURE__ */ jsxRuntimeExports.jsx(Users, { className: "h-5 w-5" }),
              "Top Customers"
            ] }),
            /* @__PURE__ */ jsxRuntimeExports.jsx(CardDescription, { children: "Customers with the highest shipment volume and spending" })
          ] }),
          /* @__PURE__ */ jsxRuntimeExports.jsx(CardContent, { children: /* @__PURE__ */ jsxRuntimeExports.jsx("div", { className: "overflow-x-auto", children: /* @__PURE__ */ jsxRuntimeExports.jsxs("table", { className: "w-full", children: [
            /* @__PURE__ */ jsxRuntimeExports.jsx("thead", { children: /* @__PURE__ */ jsxRuntimeExports.jsxs("tr", { className: "border-b", children: [
              /* @__PURE__ */ jsxRuntimeExports.jsx("th", { className: "text-left py-3 px-4 font-medium text-gray-700", children: "Customer" }),
              /* @__PURE__ */ jsxRuntimeExports.jsx("th", { className: "text-left py-3 px-4 font-medium text-gray-700", children: "Email" }),
              /* @__PURE__ */ jsxRuntimeExports.jsx("th", { className: "text-center py-3 px-4 font-medium text-gray-700", children: "Shipments" }),
              /* @__PURE__ */ jsxRuntimeExports.jsx("th", { className: "text-right py-3 px-4 font-medium text-gray-700", children: "Total Spent" })
            ] }) }),
            /* @__PURE__ */ jsxRuntimeExports.jsx("tbody", { children: topCustomersData.map((customer, index) => /* @__PURE__ */ jsxRuntimeExports.jsxs("tr", { className: "border-b hover:bg-gray-50", children: [
              /* @__PURE__ */ jsxRuntimeExports.jsx("td", { className: "py-3 px-4", children: /* @__PURE__ */ jsxRuntimeExports.jsxs("div", { className: "flex items-center gap-3", children: [
                /* @__PURE__ */ jsxRuntimeExports.jsx("div", { className: "w-8 h-8 bg-blue-100 rounded-full flex items-center justify-center", children: /* @__PURE__ */ jsxRuntimeExports.jsx("span", { className: "text-sm font-medium text-blue-600", children: customer.name.charAt(0) }) }),
                /* @__PURE__ */ jsxRuntimeExports.jsxs("div", { children: [
                  /* @__PURE__ */ jsxRuntimeExports.jsx("div", { className: "font-medium text-gray-900", children: customer.name }),
                  /* @__PURE__ */ jsxRuntimeExports.jsxs("div", { className: "text-xs text-gray-500", children: [
                    "#",
                    index + 1
                  ] })
                ] })
              ] }) }),
              /* @__PURE__ */ jsxRuntimeExports.jsx("td", { className: "py-3 px-4 text-gray-600", children: customer.email }),
              /* @__PURE__ */ jsxRuntimeExports.jsx("td", { className: "py-3 px-4 text-center", children: /* @__PURE__ */ jsxRuntimeExports.jsx(Badge, { variant: "outline", children: customer.shipment_count }) }),
              /* @__PURE__ */ jsxRuntimeExports.jsx("td", { className: "py-3 px-4 text-right font-medium", children: formatCurrency(customer.total_spent) })
            ] }, customer.user_id)) })
          ] }) }) })
        ] }),
        /* @__PURE__ */ jsxRuntimeExports.jsxs(Card, { children: [
          /* @__PURE__ */ jsxRuntimeExports.jsxs(CardHeader, { children: [
            /* @__PURE__ */ jsxRuntimeExports.jsx(CardTitle, { children: "System Performance" }),
            /* @__PURE__ */ jsxRuntimeExports.jsx(CardDescription, { children: "Real-time system health and performance metrics" })
          ] }),
          /* @__PURE__ */ jsxRuntimeExports.jsx(CardContent, { children: /* @__PURE__ */ jsxRuntimeExports.jsxs("div", { className: "grid gap-6 md:grid-cols-3", children: [
            /* @__PURE__ */ jsxRuntimeExports.jsxs("div", { className: "text-center", children: [
              /* @__PURE__ */ jsxRuntimeExports.jsx("div", { className: "text-2xl font-bold text-green-600", children: "99.9%" }),
              /* @__PURE__ */ jsxRuntimeExports.jsx("div", { className: "text-sm text-gray-600", children: "Uptime" })
            ] }),
            /* @__PURE__ */ jsxRuntimeExports.jsxs("div", { className: "text-center", children: [
              /* @__PURE__ */ jsxRuntimeExports.jsx("div", { className: "text-2xl font-bold text-blue-600", children: "1.2s" }),
              /* @__PURE__ */ jsxRuntimeExports.jsx("div", { className: "text-sm text-gray-600", children: "Avg Response Time" })
            ] }),
            /* @__PURE__ */ jsxRuntimeExports.jsxs("div", { className: "text-center", children: [
              /* @__PURE__ */ jsxRuntimeExports.jsx("div", { className: "text-2xl font-bold text-purple-600", children: "234" }),
              /* @__PURE__ */ jsxRuntimeExports.jsx("div", { className: "text-sm text-gray-600", children: "Concurrent Users" })
            ] })
          ] }) })
        ] }),
        error && /* @__PURE__ */ jsxRuntimeExports.jsx(Alert, { variant: "destructive", className: "mt-4", children: /* @__PURE__ */ jsxRuntimeExports.jsxs(AlertDescription, { children: [
          "Failed to load analytics data: ",
          error.message
        ] }) })
      ] }),
      /* @__PURE__ */ jsxRuntimeExports.jsx(TabsContent, { value: "advanced", className: "space-y-6", children: /* @__PURE__ */ jsxRuntimeExports.jsx(AdvancedAnalytics, {}) }),
      /* @__PURE__ */ jsxRuntimeExports.jsxs(TabsContent, { value: "reports", className: "space-y-6", children: [
        /* @__PURE__ */ jsxRuntimeExports.jsxs("div", { className: "grid gap-6 md:grid-cols-2", children: [
          /* @__PURE__ */ jsxRuntimeExports.jsxs(Card, { children: [
            /* @__PURE__ */ jsxRuntimeExports.jsxs(CardHeader, { children: [
              /* @__PURE__ */ jsxRuntimeExports.jsxs(CardTitle, { className: "flex items-center gap-2", children: [
                /* @__PURE__ */ jsxRuntimeExports.jsx(Activity, { className: "w-5 h-5" }),
                "Operational Reports"
              ] }),
              /* @__PURE__ */ jsxRuntimeExports.jsx(CardDescription, { children: "Detailed operational performance and efficiency reports" })
            ] }),
            /* @__PURE__ */ jsxRuntimeExports.jsx(CardContent, { children: /* @__PURE__ */ jsxRuntimeExports.jsxs("div", { className: "space-y-4", children: [
              /* @__PURE__ */ jsxRuntimeExports.jsxs("div", { className: "flex justify-between items-center p-3 border rounded-lg", children: [
                /* @__PURE__ */ jsxRuntimeExports.jsxs("div", { children: [
                  /* @__PURE__ */ jsxRuntimeExports.jsx("h4", { className: "font-medium", children: "Fleet Utilization Report" }),
                  /* @__PURE__ */ jsxRuntimeExports.jsx("p", { className: "text-sm text-gray-600", children: "Vehicle usage and efficiency metrics" })
                ] }),
                /* @__PURE__ */ jsxRuntimeExports.jsxs(Button, { variant: "outline", size: "sm", children: [
                  /* @__PURE__ */ jsxRuntimeExports.jsx(Download, { className: "w-4 h-4 mr-1" }),
                  "Download"
                ] })
              ] }),
              /* @__PURE__ */ jsxRuntimeExports.jsxs("div", { className: "flex justify-between items-center p-3 border rounded-lg", children: [
                /* @__PURE__ */ jsxRuntimeExports.jsxs("div", { children: [
                  /* @__PURE__ */ jsxRuntimeExports.jsx("h4", { className: "font-medium", children: "Delivery Performance Report" }),
                  /* @__PURE__ */ jsxRuntimeExports.jsx("p", { className: "text-sm text-gray-600", children: "On-time delivery and customer satisfaction" })
                ] }),
                /* @__PURE__ */ jsxRuntimeExports.jsxs(Button, { variant: "outline", size: "sm", children: [
                  /* @__PURE__ */ jsxRuntimeExports.jsx(Download, { className: "w-4 h-4 mr-1" }),
                  "Download"
                ] })
              ] }),
              /* @__PURE__ */ jsxRuntimeExports.jsxs("div", { className: "flex justify-between items-center p-3 border rounded-lg", children: [
                /* @__PURE__ */ jsxRuntimeExports.jsxs("div", { children: [
                  /* @__PURE__ */ jsxRuntimeExports.jsx("h4", { className: "font-medium", children: "Route Optimization Report" }),
                  /* @__PURE__ */ jsxRuntimeExports.jsx("p", { className: "text-sm text-gray-600", children: "Route efficiency and fuel consumption" })
                ] }),
                /* @__PURE__ */ jsxRuntimeExports.jsxs(Button, { variant: "outline", size: "sm", children: [
                  /* @__PURE__ */ jsxRuntimeExports.jsx(Download, { className: "w-4 h-4 mr-1" }),
                  "Download"
                ] })
              ] })
            ] }) })
          ] }),
          /* @__PURE__ */ jsxRuntimeExports.jsxs(Card, { children: [
            /* @__PURE__ */ jsxRuntimeExports.jsxs(CardHeader, { children: [
              /* @__PURE__ */ jsxRuntimeExports.jsxs(CardTitle, { className: "flex items-center gap-2", children: [
                /* @__PURE__ */ jsxRuntimeExports.jsx(DollarSign, { className: "w-5 h-5" }),
                "Financial Reports"
              ] }),
              /* @__PURE__ */ jsxRuntimeExports.jsx(CardDescription, { children: "Revenue, costs, and profitability analysis" })
            ] }),
            /* @__PURE__ */ jsxRuntimeExports.jsx(CardContent, { children: /* @__PURE__ */ jsxRuntimeExports.jsxs("div", { className: "space-y-4", children: [
              /* @__PURE__ */ jsxRuntimeExports.jsxs("div", { className: "flex justify-between items-center p-3 border rounded-lg", children: [
                /* @__PURE__ */ jsxRuntimeExports.jsxs("div", { children: [
                  /* @__PURE__ */ jsxRuntimeExports.jsx("h4", { className: "font-medium", children: "Revenue Analysis" }),
                  /* @__PURE__ */ jsxRuntimeExports.jsx("p", { className: "text-sm text-gray-600", children: "Monthly and quarterly revenue trends" })
                ] }),
                /* @__PURE__ */ jsxRuntimeExports.jsxs(Button, { variant: "outline", size: "sm", children: [
                  /* @__PURE__ */ jsxRuntimeExports.jsx(Download, { className: "w-4 h-4 mr-1" }),
                  "Download"
                ] })
              ] }),
              /* @__PURE__ */ jsxRuntimeExports.jsxs("div", { className: "flex justify-between items-center p-3 border rounded-lg", children: [
                /* @__PURE__ */ jsxRuntimeExports.jsxs("div", { children: [
                  /* @__PURE__ */ jsxRuntimeExports.jsx("h4", { className: "font-medium", children: "Cost Analysis" }),
                  /* @__PURE__ */ jsxRuntimeExports.jsx("p", { className: "text-sm text-gray-600", children: "Operational costs and efficiency metrics" })
                ] }),
                /* @__PURE__ */ jsxRuntimeExports.jsxs(Button, { variant: "outline", size: "sm", children: [
                  /* @__PURE__ */ jsxRuntimeExports.jsx(Download, { className: "w-4 h-4 mr-1" }),
                  "Download"
                ] })
              ] }),
              /* @__PURE__ */ jsxRuntimeExports.jsxs("div", { className: "flex justify-between items-center p-3 border rounded-lg", children: [
                /* @__PURE__ */ jsxRuntimeExports.jsxs("div", { children: [
                  /* @__PURE__ */ jsxRuntimeExports.jsx("h4", { className: "font-medium", children: "Profit Margin Report" }),
                  /* @__PURE__ */ jsxRuntimeExports.jsx("p", { className: "text-sm text-gray-600", children: "Profitability by service type and region" })
                ] }),
                /* @__PURE__ */ jsxRuntimeExports.jsxs(Button, { variant: "outline", size: "sm", children: [
                  /* @__PURE__ */ jsxRuntimeExports.jsx(Download, { className: "w-4 h-4 mr-1" }),
                  "Download"
                ] })
              ] })
            ] }) })
          ] })
        ] }),
        /* @__PURE__ */ jsxRuntimeExports.jsxs(Card, { children: [
          /* @__PURE__ */ jsxRuntimeExports.jsxs(CardHeader, { children: [
            /* @__PURE__ */ jsxRuntimeExports.jsxs(CardTitle, { className: "flex items-center gap-2", children: [
              /* @__PURE__ */ jsxRuntimeExports.jsx(BarChart3, { className: "w-5 h-5" }),
              "Custom Report Builder"
            ] }),
            /* @__PURE__ */ jsxRuntimeExports.jsx(CardDescription, { children: "Create custom reports with specific metrics and date ranges" })
          ] }),
          /* @__PURE__ */ jsxRuntimeExports.jsx(CardContent, { children: /* @__PURE__ */ jsxRuntimeExports.jsxs("div", { className: "text-center py-8 text-gray-500", children: [
            /* @__PURE__ */ jsxRuntimeExports.jsx(BarChart3, { className: "w-12 h-12 mx-auto mb-4 opacity-50" }),
            /* @__PURE__ */ jsxRuntimeExports.jsx("p", { children: "Custom report builder" }),
            /* @__PURE__ */ jsxRuntimeExports.jsx("p", { className: "text-sm", children: "Build custom analytics reports with drag-and-drop interface" })
          ] }) })
        ] })
      ] })
    ] })
  ] });
};
export {
  AnalyticsPage
};
