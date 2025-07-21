import { j as jsxRuntimeExports, e as cn, P as Package, L as Link, g as formatRelativeTime, M as MapPin, f as formatDateTime, s as shimExports, r as reactExports, h as createContextScope, i as Primitive, k as useCallbackRef, l as useLayoutEffect2, b as useAuthStore, B as Button, m as Plus, S as Search, n as Settings, t as toast } from "./index-D-bA2lkg.js";
import { s as shipmentApi } from "./shipment-DiICAfSs.js";
import { C as Card, a as CardHeader, b as CardTitle, d as CardContent, c as CardDescription } from "./card-CsLEcWf3.js";
import { S as StatusIndicator } from "./status-indicator-S_BRd8a9.js";
import { C as Calendar } from "./calendar-OA7BM1TB.js";
import { A as Alert, a as AlertDescription } from "./alert-Qw-zBtar.js";
import { B as Badge } from "./badge-2PvS7lZW.js";
import { I as Input } from "./input-BHuFzm78.js";
import { A as AlertCircle } from "./alert-circle-DRL_kTZp.js";
import { R as RefreshCw } from "./refresh-cw-DEEM45_N.js";
import { T as TrendingUp } from "./trending-up-BuGMLgFK.js";
import { C as Clock } from "./clock-BBOft3LF.js";
import { C as CheckCircle } from "./check-circle-9Vi05KqK.js";
import { A as Activity } from "./activity-D4A68VaF.js";
import { B as Bell } from "./bell-BJDesh0I.js";
function StatsCard({
  title,
  value,
  description,
  icon: Icon,
  trend,
  className
}) {
  return /* @__PURE__ */ jsxRuntimeExports.jsxs(Card, { className: cn("", className), children: [
    /* @__PURE__ */ jsxRuntimeExports.jsxs(CardHeader, { className: "flex flex-row items-center justify-between space-y-0 pb-2", children: [
      /* @__PURE__ */ jsxRuntimeExports.jsx(CardTitle, { className: "text-sm font-medium", children: title }),
      Icon && /* @__PURE__ */ jsxRuntimeExports.jsx(Icon, { className: "h-4 w-4 text-muted-foreground" })
    ] }),
    /* @__PURE__ */ jsxRuntimeExports.jsxs(CardContent, { children: [
      /* @__PURE__ */ jsxRuntimeExports.jsx("div", { className: "text-2xl font-bold", children: value }),
      description && /* @__PURE__ */ jsxRuntimeExports.jsx("p", { className: "text-xs text-muted-foreground mt-1", children: description }),
      trend && /* @__PURE__ */ jsxRuntimeExports.jsxs("div", { className: "flex items-center mt-2", children: [
        /* @__PURE__ */ jsxRuntimeExports.jsxs(
          "span",
          {
            className: cn(
              "text-xs font-medium",
              trend.type === "positive" && "text-green-600",
              trend.type === "negative" && "text-red-600",
              trend.type === "neutral" && "text-gray-600"
            ),
            children: [
              trend.type === "positive" && "+",
              trend.value,
              "%"
            ]
          }
        ),
        /* @__PURE__ */ jsxRuntimeExports.jsx("span", { className: "text-xs text-muted-foreground ml-1", children: trend.label })
      ] })
    ] })
  ] });
}
function RecentShipments({ shipments, loading }) {
  if (loading) {
    return /* @__PURE__ */ jsxRuntimeExports.jsxs(Card, { children: [
      /* @__PURE__ */ jsxRuntimeExports.jsxs(CardHeader, { children: [
        /* @__PURE__ */ jsxRuntimeExports.jsx(CardTitle, { children: "Recent Shipments" }),
        /* @__PURE__ */ jsxRuntimeExports.jsx(CardDescription, { children: "Your latest shipment activities" })
      ] }),
      /* @__PURE__ */ jsxRuntimeExports.jsx(CardContent, { children: /* @__PURE__ */ jsxRuntimeExports.jsx("div", { className: "space-y-4", children: Array.from({ length: 3 }).map((_, i) => /* @__PURE__ */ jsxRuntimeExports.jsxs("div", { className: "flex items-center space-x-4", children: [
        /* @__PURE__ */ jsxRuntimeExports.jsx("div", { className: "w-10 h-10 bg-gray-200 rounded-full animate-pulse" }),
        /* @__PURE__ */ jsxRuntimeExports.jsxs("div", { className: "flex-1 space-y-2", children: [
          /* @__PURE__ */ jsxRuntimeExports.jsx("div", { className: "h-4 bg-gray-200 rounded animate-pulse" }),
          /* @__PURE__ */ jsxRuntimeExports.jsx("div", { className: "h-3 bg-gray-200 rounded w-2/3 animate-pulse" })
        ] })
      ] }, i)) }) })
    ] });
  }
  return /* @__PURE__ */ jsxRuntimeExports.jsxs(Card, { children: [
    /* @__PURE__ */ jsxRuntimeExports.jsxs(CardHeader, { children: [
      /* @__PURE__ */ jsxRuntimeExports.jsxs(CardTitle, { className: "flex items-center gap-2", children: [
        /* @__PURE__ */ jsxRuntimeExports.jsx(Package, { className: "h-5 w-5" }),
        "Recent Shipments"
      ] }),
      /* @__PURE__ */ jsxRuntimeExports.jsx(CardDescription, { children: "Your latest shipment activities" })
    ] }),
    /* @__PURE__ */ jsxRuntimeExports.jsxs(CardContent, { children: [
      shipments.length === 0 ? /* @__PURE__ */ jsxRuntimeExports.jsxs("div", { className: "text-center py-8 text-muted-foreground", children: [
        /* @__PURE__ */ jsxRuntimeExports.jsx(Package, { className: "h-12 w-12 mx-auto mb-4 opacity-50" }),
        /* @__PURE__ */ jsxRuntimeExports.jsx("p", { children: "No shipments yet" }),
        /* @__PURE__ */ jsxRuntimeExports.jsx("p", { className: "text-sm", children: "Create your first shipment to get started" })
      ] }) : /* @__PURE__ */ jsxRuntimeExports.jsx("div", { className: "space-y-4", children: shipments.map((shipment) => /* @__PURE__ */ jsxRuntimeExports.jsxs("div", { className: "flex items-center space-x-4 p-4 border rounded-lg hover:bg-gray-50 transition-colors", children: [
        /* @__PURE__ */ jsxRuntimeExports.jsx("div", { className: "flex-shrink-0", children: /* @__PURE__ */ jsxRuntimeExports.jsx("div", { className: "w-10 h-10 bg-blue-100 rounded-full flex items-center justify-center", children: /* @__PURE__ */ jsxRuntimeExports.jsx(Package, { className: "h-5 w-5 text-blue-600" }) }) }),
        /* @__PURE__ */ jsxRuntimeExports.jsxs("div", { className: "flex-1 min-w-0", children: [
          /* @__PURE__ */ jsxRuntimeExports.jsxs("div", { className: "flex items-center gap-2 mb-1", children: [
            /* @__PURE__ */ jsxRuntimeExports.jsx(
              Link,
              {
                to: `/shipments/tracking/${shipment.tracking_number}`,
                className: "font-medium text-blue-600 hover:text-blue-800 transition-colors",
                children: shipment.tracking_number
              }
            ),
            /* @__PURE__ */ jsxRuntimeExports.jsx(
              StatusIndicator,
              {
                status: shipment.status,
                showDot: true,
                animated: !["DELIVERED", "CANCELLED", "RETURNED"].includes(shipment.status)
              }
            )
          ] }),
          /* @__PURE__ */ jsxRuntimeExports.jsxs("div", { className: "flex items-center gap-4 text-sm text-gray-600", children: [
            /* @__PURE__ */ jsxRuntimeExports.jsxs("div", { className: "flex items-center gap-1", children: [
              /* @__PURE__ */ jsxRuntimeExports.jsx(Calendar, { className: "h-3 w-3" }),
              /* @__PURE__ */ jsxRuntimeExports.jsx("span", { children: formatRelativeTime(shipment.created_at) })
            ] }),
            /* @__PURE__ */ jsxRuntimeExports.jsxs("div", { className: "flex items-center gap-1", children: [
              /* @__PURE__ */ jsxRuntimeExports.jsx(MapPin, { className: "h-3 w-3" }),
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
            ] })
          ] }),
          shipment.estimated_delivery && /* @__PURE__ */ jsxRuntimeExports.jsxs("div", { className: "text-xs text-gray-500 mt-1", children: [
            "Est. delivery: ",
            formatDateTime(shipment.estimated_delivery)
          ] })
        ] })
      ] }, shipment.shipment_id)) }),
      shipments.length > 0 && /* @__PURE__ */ jsxRuntimeExports.jsx("div", { className: "mt-6 text-center", children: /* @__PURE__ */ jsxRuntimeExports.jsx(
        Link,
        {
          to: "/shipments",
          className: "text-sm text-blue-600 hover:text-blue-800 font-medium",
          children: "View all shipments →"
        }
      ) })
    ] })
  ] });
}
function useIsHydrated() {
  return shimExports.useSyncExternalStore(
    subscribe,
    () => true,
    () => false
  );
}
function subscribe() {
  return () => {
  };
}
var AVATAR_NAME = "Avatar";
var [createAvatarContext, createAvatarScope] = createContextScope(AVATAR_NAME);
var [AvatarProvider, useAvatarContext] = createAvatarContext(AVATAR_NAME);
var Avatar$1 = reactExports.forwardRef(
  (props, forwardedRef) => {
    const { __scopeAvatar, ...avatarProps } = props;
    const [imageLoadingStatus, setImageLoadingStatus] = reactExports.useState("idle");
    return /* @__PURE__ */ jsxRuntimeExports.jsx(
      AvatarProvider,
      {
        scope: __scopeAvatar,
        imageLoadingStatus,
        onImageLoadingStatusChange: setImageLoadingStatus,
        children: /* @__PURE__ */ jsxRuntimeExports.jsx(Primitive.span, { ...avatarProps, ref: forwardedRef })
      }
    );
  }
);
Avatar$1.displayName = AVATAR_NAME;
var IMAGE_NAME = "AvatarImage";
var AvatarImage$1 = reactExports.forwardRef(
  (props, forwardedRef) => {
    const { __scopeAvatar, src, onLoadingStatusChange = () => {
    }, ...imageProps } = props;
    const context = useAvatarContext(IMAGE_NAME, __scopeAvatar);
    const imageLoadingStatus = useImageLoadingStatus(src, imageProps);
    const handleLoadingStatusChange = useCallbackRef((status) => {
      onLoadingStatusChange(status);
      context.onImageLoadingStatusChange(status);
    });
    useLayoutEffect2(() => {
      if (imageLoadingStatus !== "idle") {
        handleLoadingStatusChange(imageLoadingStatus);
      }
    }, [imageLoadingStatus, handleLoadingStatusChange]);
    return imageLoadingStatus === "loaded" ? /* @__PURE__ */ jsxRuntimeExports.jsx(Primitive.img, { ...imageProps, ref: forwardedRef, src }) : null;
  }
);
AvatarImage$1.displayName = IMAGE_NAME;
var FALLBACK_NAME = "AvatarFallback";
var AvatarFallback$1 = reactExports.forwardRef(
  (props, forwardedRef) => {
    const { __scopeAvatar, delayMs, ...fallbackProps } = props;
    const context = useAvatarContext(FALLBACK_NAME, __scopeAvatar);
    const [canRender, setCanRender] = reactExports.useState(delayMs === void 0);
    reactExports.useEffect(() => {
      if (delayMs !== void 0) {
        const timerId = window.setTimeout(() => setCanRender(true), delayMs);
        return () => window.clearTimeout(timerId);
      }
    }, [delayMs]);
    return canRender && context.imageLoadingStatus !== "loaded" ? /* @__PURE__ */ jsxRuntimeExports.jsx(Primitive.span, { ...fallbackProps, ref: forwardedRef }) : null;
  }
);
AvatarFallback$1.displayName = FALLBACK_NAME;
function resolveLoadingStatus(image, src) {
  if (!image) {
    return "idle";
  }
  if (!src) {
    return "error";
  }
  if (image.src !== src) {
    image.src = src;
  }
  return image.complete && image.naturalWidth > 0 ? "loaded" : "loading";
}
function useImageLoadingStatus(src, { referrerPolicy, crossOrigin }) {
  const isHydrated = useIsHydrated();
  const imageRef = reactExports.useRef(null);
  const image = (() => {
    if (!isHydrated) return null;
    if (!imageRef.current) {
      imageRef.current = new window.Image();
    }
    return imageRef.current;
  })();
  const [loadingStatus, setLoadingStatus] = reactExports.useState(
    () => resolveLoadingStatus(image, src)
  );
  useLayoutEffect2(() => {
    setLoadingStatus(resolveLoadingStatus(image, src));
  }, [image, src]);
  useLayoutEffect2(() => {
    const updateStatus = (status) => () => {
      setLoadingStatus(status);
    };
    if (!image) return;
    const handleLoad = updateStatus("loaded");
    const handleError = updateStatus("error");
    image.addEventListener("load", handleLoad);
    image.addEventListener("error", handleError);
    if (referrerPolicy) {
      image.referrerPolicy = referrerPolicy;
    }
    if (typeof crossOrigin === "string") {
      image.crossOrigin = crossOrigin;
    }
    return () => {
      image.removeEventListener("load", handleLoad);
      image.removeEventListener("error", handleError);
    };
  }, [image, crossOrigin, referrerPolicy]);
  return loadingStatus;
}
var Root = Avatar$1;
var Image = AvatarImage$1;
var Fallback = AvatarFallback$1;
const Avatar = reactExports.forwardRef(({ className, ...props }, ref) => /* @__PURE__ */ jsxRuntimeExports.jsx(
  Root,
  {
    ref,
    className: cn(
      "relative flex h-10 w-10 shrink-0 overflow-hidden rounded-full",
      className
    ),
    ...props
  }
));
Avatar.displayName = Root.displayName;
const AvatarImage = reactExports.forwardRef(({ className, ...props }, ref) => /* @__PURE__ */ jsxRuntimeExports.jsx(
  Image,
  {
    ref,
    className: cn("aspect-square h-full w-full", className),
    ...props
  }
));
AvatarImage.displayName = Image.displayName;
const AvatarFallback = reactExports.forwardRef(({ className, ...props }, ref) => /* @__PURE__ */ jsxRuntimeExports.jsx(
  Fallback,
  {
    ref,
    className: cn(
      "flex h-full w-full items-center justify-center rounded-full bg-muted",
      className
    ),
    ...props
  }
));
AvatarFallback.displayName = Fallback.displayName;
const DashboardPage = () => {
  const { user } = useAuthStore();
  const [stats, setStats] = reactExports.useState(null);
  const [loading, setLoading] = reactExports.useState(true);
  const [error, setError] = reactExports.useState(null);
  const [searchTerm, setSearchTerm] = reactExports.useState("");
  const fetchDashboardData = async () => {
    if (!user) return;
    try {
      setLoading(true);
      setError(null);
      const dashboardStats = await shipmentApi.getDashboardStats(parseInt(user.id));
      setStats(dashboardStats);
    } catch (err) {
      console.error("Failed to fetch dashboard data:", err);
      const errorMessage = err instanceof Error ? err.message : "Failed to load dashboard data";
      setError(errorMessage);
      toast.error("Failed to load dashboard data");
    } finally {
      setLoading(false);
    }
  };
  reactExports.useEffect(() => {
    fetchDashboardData();
  }, [user]);
  const handleRefresh = () => {
    fetchDashboardData();
  };
  if (!user) {
    return /* @__PURE__ */ jsxRuntimeExports.jsx("div", { className: "p-8", children: /* @__PURE__ */ jsxRuntimeExports.jsxs(Alert, { variant: "destructive", children: [
      /* @__PURE__ */ jsxRuntimeExports.jsx(AlertCircle, { className: "h-4 w-4" }),
      /* @__PURE__ */ jsxRuntimeExports.jsx(AlertDescription, { children: "Please log in to view your dashboard." })
    ] }) });
  }
  return /* @__PURE__ */ jsxRuntimeExports.jsx("div", { className: "min-h-screen bg-gray-50/50", children: /* @__PURE__ */ jsxRuntimeExports.jsxs("div", { className: "p-6 max-w-7xl mx-auto", children: [
    /* @__PURE__ */ jsxRuntimeExports.jsx("div", { className: "bg-white rounded-lg shadow-sm border mb-8", children: /* @__PURE__ */ jsxRuntimeExports.jsxs("div", { className: "p-6", children: [
      /* @__PURE__ */ jsxRuntimeExports.jsxs("div", { className: "flex items-center justify-between", children: [
        /* @__PURE__ */ jsxRuntimeExports.jsxs("div", { className: "flex items-center gap-4", children: [
          /* @__PURE__ */ jsxRuntimeExports.jsx(Avatar, { className: "h-12 w-12", children: /* @__PURE__ */ jsxRuntimeExports.jsx(AvatarFallback, { className: "bg-primary text-primary-foreground", children: user.name.split(" ").map((n) => n[0]).join("").toUpperCase() }) }),
          /* @__PURE__ */ jsxRuntimeExports.jsxs("div", { children: [
            /* @__PURE__ */ jsxRuntimeExports.jsxs("h1", { className: "text-2xl font-bold text-gray-900", children: [
              "Welcome back, ",
              user.name
            ] }),
            /* @__PURE__ */ jsxRuntimeExports.jsxs("p", { className: "text-gray-600 flex items-center gap-2 mt-1", children: [
              /* @__PURE__ */ jsxRuntimeExports.jsx(Badge, { variant: "secondary", className: "px-2 py-1", children: user.role.toLowerCase() }),
              /* @__PURE__ */ jsxRuntimeExports.jsx("span", { className: "text-sm", children: "•" }),
              /* @__PURE__ */ jsxRuntimeExports.jsx("span", { className: "text-sm", children: "Last login: Today" })
            ] })
          ] })
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
          /* @__PURE__ */ jsxRuntimeExports.jsx(Link, { to: "/shipments/create", children: /* @__PURE__ */ jsxRuntimeExports.jsxs(Button, { size: "sm", className: "bg-primary hover:bg-primary/90", children: [
            /* @__PURE__ */ jsxRuntimeExports.jsx(Plus, { className: "h-4 w-4 mr-2" }),
            "New Shipment"
          ] }) })
        ] })
      ] }),
      /* @__PURE__ */ jsxRuntimeExports.jsx("div", { className: "mt-6 max-w-md", children: /* @__PURE__ */ jsxRuntimeExports.jsxs("div", { className: "relative", children: [
        /* @__PURE__ */ jsxRuntimeExports.jsx(Search, { className: "absolute left-3 top-3 h-4 w-4 text-gray-400" }),
        /* @__PURE__ */ jsxRuntimeExports.jsx(
          Input,
          {
            placeholder: "Search shipments, tracking numbers...",
            value: searchTerm,
            onChange: (e) => setSearchTerm(e.target.value),
            className: "pl-10"
          }
        )
      ] }) })
    ] }) }),
    error && /* @__PURE__ */ jsxRuntimeExports.jsxs(Alert, { variant: "destructive", className: "mb-6", children: [
      /* @__PURE__ */ jsxRuntimeExports.jsx(AlertCircle, { className: "h-4 w-4" }),
      /* @__PURE__ */ jsxRuntimeExports.jsx(AlertDescription, { children: error })
    ] }),
    /* @__PURE__ */ jsxRuntimeExports.jsxs("div", { className: "grid gap-6 md:grid-cols-2 lg:grid-cols-4 mb-8", children: [
      /* @__PURE__ */ jsxRuntimeExports.jsx(
        StatsCard,
        {
          title: "Total Shipments",
          value: loading ? "-" : stats?.total_shipments || 0,
          description: "All time shipments",
          icon: Package,
          trend: {
            value: 12.5,
            label: "vs last month",
            type: "positive"
          },
          className: loading ? "animate-pulse" : "hover:shadow-md transition-shadow"
        }
      ),
      /* @__PURE__ */ jsxRuntimeExports.jsx(
        StatsCard,
        {
          title: "Active Shipments",
          value: loading ? "-" : stats?.active_shipments || 0,
          description: "Currently in transit",
          icon: TrendingUp,
          trend: {
            value: 8.2,
            label: "vs last week",
            type: "positive"
          },
          className: loading ? "animate-pulse" : "hover:shadow-md transition-shadow"
        }
      ),
      /* @__PURE__ */ jsxRuntimeExports.jsx(
        StatsCard,
        {
          title: "Pending Pickup",
          value: loading ? "-" : stats?.pending_shipments || 0,
          description: "Waiting for pickup",
          icon: Clock,
          trend: {
            value: -5.1,
            label: "vs yesterday",
            type: "negative"
          },
          className: loading ? "animate-pulse" : "hover:shadow-md transition-shadow"
        }
      ),
      /* @__PURE__ */ jsxRuntimeExports.jsx(
        StatsCard,
        {
          title: "Delivered",
          value: loading ? "-" : stats?.delivered_shipments || 0,
          description: "Successfully delivered",
          icon: CheckCircle,
          trend: {
            value: 15.3,
            label: "vs last month",
            type: "positive"
          },
          className: loading ? "animate-pulse" : "hover:shadow-md transition-shadow"
        }
      )
    ] }),
    /* @__PURE__ */ jsxRuntimeExports.jsxs("div", { className: "grid gap-6 lg:grid-cols-3", children: [
      /* @__PURE__ */ jsxRuntimeExports.jsx("div", { className: "lg:col-span-2", children: /* @__PURE__ */ jsxRuntimeExports.jsxs(Card, { children: [
        /* @__PURE__ */ jsxRuntimeExports.jsx(CardHeader, { children: /* @__PURE__ */ jsxRuntimeExports.jsxs(CardTitle, { className: "flex items-center gap-2", children: [
          /* @__PURE__ */ jsxRuntimeExports.jsx(Activity, { className: "h-5 w-5" }),
          "Recent Shipments"
        ] }) }),
        /* @__PURE__ */ jsxRuntimeExports.jsx(CardContent, { children: /* @__PURE__ */ jsxRuntimeExports.jsx(
          RecentShipments,
          {
            shipments: stats?.recent_shipments || [],
            loading
          }
        ) })
      ] }) }),
      /* @__PURE__ */ jsxRuntimeExports.jsxs("div", { className: "space-y-6", children: [
        /* @__PURE__ */ jsxRuntimeExports.jsxs(Card, { children: [
          /* @__PURE__ */ jsxRuntimeExports.jsx(CardHeader, { children: /* @__PURE__ */ jsxRuntimeExports.jsxs(CardTitle, { className: "flex items-center gap-2", children: [
            /* @__PURE__ */ jsxRuntimeExports.jsx(Plus, { className: "h-5 w-5" }),
            "Quick Actions"
          ] }) }),
          /* @__PURE__ */ jsxRuntimeExports.jsxs(CardContent, { className: "space-y-3", children: [
            /* @__PURE__ */ jsxRuntimeExports.jsx(Link, { to: "/shipments/create", className: "block", children: /* @__PURE__ */ jsxRuntimeExports.jsxs(Button, { className: "w-full justify-start", variant: "outline", size: "sm", children: [
              /* @__PURE__ */ jsxRuntimeExports.jsx(Plus, { className: "h-4 w-4 mr-2" }),
              "Create New Shipment"
            ] }) }),
            /* @__PURE__ */ jsxRuntimeExports.jsx(Link, { to: "/tracking", className: "block", children: /* @__PURE__ */ jsxRuntimeExports.jsxs(Button, { className: "w-full justify-start", variant: "outline", size: "sm", children: [
              /* @__PURE__ */ jsxRuntimeExports.jsx(Search, { className: "h-4 w-4 mr-2" }),
              "Track Package"
            ] }) }),
            /* @__PURE__ */ jsxRuntimeExports.jsx(Link, { to: "/shipments", className: "block", children: /* @__PURE__ */ jsxRuntimeExports.jsxs(Button, { className: "w-full justify-start", variant: "outline", size: "sm", children: [
              /* @__PURE__ */ jsxRuntimeExports.jsx(Package, { className: "h-4 w-4 mr-2" }),
              "View All Shipments"
            ] }) }),
            /* @__PURE__ */ jsxRuntimeExports.jsx(Link, { to: "/profile", className: "block", children: /* @__PURE__ */ jsxRuntimeExports.jsxs(Button, { className: "w-full justify-start", variant: "outline", size: "sm", children: [
              /* @__PURE__ */ jsxRuntimeExports.jsx(Settings, { className: "h-4 w-4 mr-2" }),
              "Account Settings"
            ] }) })
          ] })
        ] }),
        /* @__PURE__ */ jsxRuntimeExports.jsxs(Card, { children: [
          /* @__PURE__ */ jsxRuntimeExports.jsx(CardHeader, { children: /* @__PURE__ */ jsxRuntimeExports.jsxs(CardTitle, { className: "flex items-center gap-2", children: [
            /* @__PURE__ */ jsxRuntimeExports.jsx(Settings, { className: "h-5 w-5" }),
            "Account Summary"
          ] }) }),
          /* @__PURE__ */ jsxRuntimeExports.jsxs(CardContent, { className: "space-y-3", children: [
            /* @__PURE__ */ jsxRuntimeExports.jsxs("div", { className: "flex justify-between text-sm", children: [
              /* @__PURE__ */ jsxRuntimeExports.jsx("span", { className: "text-muted-foreground", children: "Account Type" }),
              /* @__PURE__ */ jsxRuntimeExports.jsx(Badge, { variant: "secondary", className: "capitalize", children: user.role.toLowerCase() })
            ] }),
            /* @__PURE__ */ jsxRuntimeExports.jsxs("div", { className: "flex justify-between text-sm", children: [
              /* @__PURE__ */ jsxRuntimeExports.jsx("span", { className: "text-muted-foreground", children: "Email" }),
              /* @__PURE__ */ jsxRuntimeExports.jsx("span", { className: "font-medium truncate max-w-32", children: user.email })
            ] }),
            /* @__PURE__ */ jsxRuntimeExports.jsxs("div", { className: "flex justify-between text-sm", children: [
              /* @__PURE__ */ jsxRuntimeExports.jsx("span", { className: "text-muted-foreground", children: "Total Shipments" }),
              /* @__PURE__ */ jsxRuntimeExports.jsx("span", { className: "font-medium", children: stats?.total_shipments || 0 })
            ] }),
            /* @__PURE__ */ jsxRuntimeExports.jsxs("div", { className: "flex justify-between text-sm", children: [
              /* @__PURE__ */ jsxRuntimeExports.jsx("span", { className: "text-muted-foreground", children: "Status" }),
              /* @__PURE__ */ jsxRuntimeExports.jsx(Badge, { variant: "success", className: "text-xs", children: "Active" })
            ] })
          ] })
        ] }),
        /* @__PURE__ */ jsxRuntimeExports.jsxs(Card, { children: [
          /* @__PURE__ */ jsxRuntimeExports.jsx(CardHeader, { children: /* @__PURE__ */ jsxRuntimeExports.jsxs(CardTitle, { className: "flex items-center gap-2", children: [
            /* @__PURE__ */ jsxRuntimeExports.jsx(Bell, { className: "h-5 w-5" }),
            "Recent Activity"
          ] }) }),
          /* @__PURE__ */ jsxRuntimeExports.jsx(CardContent, { children: /* @__PURE__ */ jsxRuntimeExports.jsxs("div", { className: "space-y-3", children: [
            /* @__PURE__ */ jsxRuntimeExports.jsxs("div", { className: "flex items-start gap-3", children: [
              /* @__PURE__ */ jsxRuntimeExports.jsx("div", { className: "w-2 h-2 bg-green-500 rounded-full mt-2 flex-shrink-0" }),
              /* @__PURE__ */ jsxRuntimeExports.jsxs("div", { children: [
                /* @__PURE__ */ jsxRuntimeExports.jsx("p", { className: "text-sm font-medium", children: "Package delivered" }),
                /* @__PURE__ */ jsxRuntimeExports.jsx("p", { className: "text-xs text-muted-foreground", children: "UPS123456789 • 2 hours ago" })
              ] })
            ] }),
            /* @__PURE__ */ jsxRuntimeExports.jsxs("div", { className: "flex items-start gap-3", children: [
              /* @__PURE__ */ jsxRuntimeExports.jsx("div", { className: "w-2 h-2 bg-blue-500 rounded-full mt-2 flex-shrink-0" }),
              /* @__PURE__ */ jsxRuntimeExports.jsxs("div", { children: [
                /* @__PURE__ */ jsxRuntimeExports.jsx("p", { className: "text-sm font-medium", children: "New shipment created" }),
                /* @__PURE__ */ jsxRuntimeExports.jsx("p", { className: "text-xs text-muted-foreground", children: "UPS987654321 • 4 hours ago" })
              ] })
            ] }),
            /* @__PURE__ */ jsxRuntimeExports.jsxs("div", { className: "flex items-start gap-3", children: [
              /* @__PURE__ */ jsxRuntimeExports.jsx("div", { className: "w-2 h-2 bg-yellow-500 rounded-full mt-2 flex-shrink-0" }),
              /* @__PURE__ */ jsxRuntimeExports.jsxs("div", { children: [
                /* @__PURE__ */ jsxRuntimeExports.jsx("p", { className: "text-sm font-medium", children: "Pickup scheduled" }),
                /* @__PURE__ */ jsxRuntimeExports.jsx("p", { className: "text-xs text-muted-foreground", children: "UPS456789123 • 6 hours ago" })
              ] })
            ] })
          ] }) })
        ] })
      ] })
    ] })
  ] }) });
};
export {
  DashboardPage
};
