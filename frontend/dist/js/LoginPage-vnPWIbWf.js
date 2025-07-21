import { c as createLucideIcon, a as api, r as reactExports, b as useAuthStore, j as jsxRuntimeExports, N as Navigate, P as Package, B as Button, L as Link, t as toast } from "./index-D-bA2lkg.js";
import { u as useForm, a, M as Mail, o as object, s as string } from "./schemas-DKKe2DjN.js";
import { I as Input } from "./input-BHuFzm78.js";
import { L as Label } from "./label-BLnRWPXy.js";
import { A as Alert, a as AlertDescription } from "./alert-Qw-zBtar.js";
import { C as Card, a as CardHeader, b as CardTitle, c as CardDescription, d as CardContent } from "./card-CsLEcWf3.js";
import { A as AlertCircle } from "./alert-circle-DRL_kTZp.js";
import { L as Lock } from "./lock-k0Jw1FCP.js";
import { E as Eye } from "./eye-f0qCM07h.js";
import { L as LogIn } from "./log-in-CeVa6HUc.js";
/**
 * @license lucide-react v0.309.0 - ISC
 *
 * This source code is licensed under the ISC license.
 * See the LICENSE file in the root directory of this source tree.
 */
const EyeOff = createLucideIcon("EyeOff", [
  ["path", { d: "M9.88 9.88a3 3 0 1 0 4.24 4.24", key: "1jxqfv" }],
  [
    "path",
    {
      d: "M10.73 5.08A10.43 10.43 0 0 1 12 5c7 0 10 7 10 7a13.16 13.16 0 0 1-1.67 2.68",
      key: "9wicm4"
    }
  ],
  [
    "path",
    { d: "M6.61 6.61A13.526 13.526 0 0 0 2 12s3 7 10 7a9.74 9.74 0 0 0 5.39-1.61", key: "1jreej" }
  ],
  ["line", { x1: "2", x2: "22", y1: "2", y2: "22", key: "a6p6uj" }]
]);
const authApi = {
  login: (data) => {
    return api.post("/auth/login", data).then((res) => res.data);
  },
  register: (data) => {
    return api.post("/auth/register", data).then((res) => res.data);
  },
  logout: () => {
    return api.post("/auth/logout").then((res) => res.data);
  },
  getCurrentUser: () => {
    return api.get("/auth/me").then((res) => res.data);
  },
  changePassword: (data) => {
    return api.put("/auth/change-password", data).then((res) => res.data);
  }
};
const loginSchema = object({
  email: string().email("Please enter a valid email address"),
  password: string().min(8, "Password must be at least 8 characters")
});
const LoginPage = () => {
  const [showPassword, setShowPassword] = reactExports.useState(false);
  const [isLoading, setIsLoading] = reactExports.useState(false);
  const [error, setError] = reactExports.useState(null);
  const { isAuthenticated, login } = useAuthStore();
  const {
    register,
    handleSubmit,
    formState: { errors }
  } = useForm({
    resolver: a(loginSchema)
  });
  if (isAuthenticated) {
    return /* @__PURE__ */ jsxRuntimeExports.jsx(Navigate, { to: "/dashboard", replace: true });
  }
  const onSubmit = async (data) => {
    setIsLoading(true);
    setError(null);
    try {
      const response = await authApi.login(data);
      login(response.user, response.token);
      toast.success("Login successful!");
    } catch (err) {
      let errorMessage = "Login failed, please try again later";
      if (typeof err === "object" && err !== null && "response" in err) {
        const response = err.response;
        if (response?.data?.message) {
          errorMessage = response.data.message;
        }
      }
      setError(errorMessage);
    } finally {
      setIsLoading(false);
    }
  };
  return /* @__PURE__ */ jsxRuntimeExports.jsx("div", { className: "min-h-screen flex items-center justify-center bg-gradient-to-br from-blue-50 to-indigo-100 px-4", children: /* @__PURE__ */ jsxRuntimeExports.jsxs(Card, { className: "w-full max-w-md", children: [
    /* @__PURE__ */ jsxRuntimeExports.jsxs(CardHeader, { className: "text-center", children: [
      /* @__PURE__ */ jsxRuntimeExports.jsx("div", { className: "flex justify-center mb-4", children: /* @__PURE__ */ jsxRuntimeExports.jsx(Package, { className: "h-12 w-12 text-blue-600" }) }),
      /* @__PURE__ */ jsxRuntimeExports.jsx(CardTitle, { className: "text-2xl font-bold text-gray-900", children: "Mini-UPS Login" }),
      /* @__PURE__ */ jsxRuntimeExports.jsx(CardDescription, { className: "text-gray-600", children: "Log in to your account to start using the service" })
    ] }),
    /* @__PURE__ */ jsxRuntimeExports.jsxs(CardContent, { children: [
      /* @__PURE__ */ jsxRuntimeExports.jsxs("form", { onSubmit: handleSubmit(onSubmit), className: "space-y-4", children: [
        error && /* @__PURE__ */ jsxRuntimeExports.jsxs(Alert, { variant: "destructive", children: [
          /* @__PURE__ */ jsxRuntimeExports.jsx(AlertCircle, { className: "h-4 w-4" }),
          /* @__PURE__ */ jsxRuntimeExports.jsx(AlertDescription, { children: error })
        ] }),
        /* @__PURE__ */ jsxRuntimeExports.jsxs("div", { className: "space-y-2", children: [
          /* @__PURE__ */ jsxRuntimeExports.jsx(Label, { htmlFor: "email", children: "Email" }),
          /* @__PURE__ */ jsxRuntimeExports.jsxs("div", { className: "relative", children: [
            /* @__PURE__ */ jsxRuntimeExports.jsx(Mail, { className: "absolute left-3 top-3 h-4 w-4 text-gray-400" }),
            /* @__PURE__ */ jsxRuntimeExports.jsx(
              Input,
              {
                id: "email",
                type: "email",
                autoComplete: "email",
                placeholder: "Enter your email",
                className: "pl-10",
                ...register("email"),
                "aria-invalid": errors.email ? "true" : "false",
                "aria-describedby": "email-error"
              }
            )
          ] }),
          errors.email && /* @__PURE__ */ jsxRuntimeExports.jsx("p", { id: "email-error", className: "text-sm text-red-500", role: "alert", children: errors.email.message })
        ] }),
        /* @__PURE__ */ jsxRuntimeExports.jsxs("div", { className: "space-y-2", children: [
          /* @__PURE__ */ jsxRuntimeExports.jsx(Label, { htmlFor: "password", children: "Password" }),
          /* @__PURE__ */ jsxRuntimeExports.jsxs("div", { className: "relative", children: [
            /* @__PURE__ */ jsxRuntimeExports.jsx(Lock, { className: "absolute left-3 top-3 h-4 w-4 text-gray-400" }),
            /* @__PURE__ */ jsxRuntimeExports.jsx(
              Input,
              {
                id: "password",
                type: showPassword ? "text" : "password",
                autoComplete: "current-password",
                placeholder: "Enter your password",
                className: "pl-10 pr-10",
                ...register("password"),
                "aria-invalid": errors.password ? "true" : "false",
                "aria-describedby": "password-error"
              }
            ),
            /* @__PURE__ */ jsxRuntimeExports.jsx(
              "button",
              {
                type: "button",
                onClick: () => setShowPassword(!showPassword),
                className: "absolute right-3 top-3 text-gray-400 hover:text-gray-600",
                "aria-label": showPassword ? "Hide password" : "Show password",
                children: showPassword ? /* @__PURE__ */ jsxRuntimeExports.jsx(EyeOff, { className: "h-4 w-4" }) : /* @__PURE__ */ jsxRuntimeExports.jsx(Eye, { className: "h-4 w-4" })
              }
            )
          ] }),
          errors.password && /* @__PURE__ */ jsxRuntimeExports.jsx("p", { id: "password-error", className: "text-sm text-red-500", role: "alert", children: errors.password.message })
        ] }),
        /* @__PURE__ */ jsxRuntimeExports.jsx(
          Button,
          {
            type: "submit",
            className: "w-full",
            disabled: isLoading,
            children: isLoading ? /* @__PURE__ */ jsxRuntimeExports.jsxs(jsxRuntimeExports.Fragment, { children: [
              /* @__PURE__ */ jsxRuntimeExports.jsx("div", { className: "animate-spin rounded-full h-4 w-4 border-b-2 border-white mr-2" }),
              "Logging in..."
            ] }) : /* @__PURE__ */ jsxRuntimeExports.jsxs(jsxRuntimeExports.Fragment, { children: [
              /* @__PURE__ */ jsxRuntimeExports.jsx(LogIn, { className: "h-4 w-4 mr-2" }),
              "Login"
            ] })
          }
        )
      ] }),
      /* @__PURE__ */ jsxRuntimeExports.jsxs("div", { className: "mt-6 text-center text-sm text-gray-600", children: [
        "Don't have an account yet?",
        " ",
        /* @__PURE__ */ jsxRuntimeExports.jsx(
          Link,
          {
            to: "/register",
            className: "font-medium text-blue-600 hover:text-blue-500",
            children: "Register now"
          }
        )
      ] }),
      /* @__PURE__ */ jsxRuntimeExports.jsx("div", { className: "mt-4 text-center", children: /* @__PURE__ */ jsxRuntimeExports.jsx(
        Link,
        {
          to: "/forgot-password",
          className: "text-sm text-blue-600 hover:text-blue-500",
          children: "Forgot password?"
        }
      ) })
    ] })
  ] }) });
};
export {
  LoginPage
};
