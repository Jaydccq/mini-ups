import { b as useAuthStore, r as reactExports, j as jsxRuntimeExports, U as User, M as MapPin, B as Button, t as toast } from "./index-D-bA2lkg.js";
import { u as useForm, a, M as Mail, o as object, s as string, b as boolean } from "./schemas-DKKe2DjN.js";
import { u as useMutation } from "./useMutation-glqHjIDc.js";
import { I as Input } from "./input-BHuFzm78.js";
import { L as Label } from "./label-BLnRWPXy.js";
import { C as Card, d as CardContent, a as CardHeader, b as CardTitle, c as CardDescription } from "./card-CsLEcWf3.js";
import { A as Alert, a as AlertDescription } from "./alert-Qw-zBtar.js";
import { B as Badge } from "./badge-2PvS7lZW.js";
import { L as Lock } from "./lock-k0Jw1FCP.js";
import { B as Bell } from "./bell-BJDesh0I.js";
import { P as Phone, S as Save } from "./save-CQA7iOAY.js";
import { A as AlertCircle } from "./alert-circle-DRL_kTZp.js";
const profileSchema = object({
  name: string().min(1, "Name is required"),
  email: string().email("Invalid email address"),
  phone: string().min(10, "Phone number must be at least 10 digits"),
  address: string().min(1, "Address is required")
});
const passwordSchema = object({
  currentPassword: string().min(1, "Current password is required"),
  newPassword: string().min(8, "Password must be at least 8 characters"),
  confirmPassword: string().min(1, "Please confirm your password")
}).refine((data) => data.newPassword === data.confirmPassword, {
  message: "Passwords don't match",
  path: ["confirmPassword"]
});
const notificationSchema = object({
  emailNotifications: boolean(),
  smsNotifications: boolean(),
  pushNotifications: boolean(),
  marketingEmails: boolean()
});
const ProfilePage = () => {
  const { user, updateUser } = useAuthStore();
  const [activeTab, setActiveTab] = reactExports.useState("profile");
  const profileForm = useForm({
    resolver: a(profileSchema),
    defaultValues: {
      name: user?.name || "",
      email: user?.email || "",
      phone: "",
      address: ""
    }
  });
  const passwordForm = useForm({
    resolver: a(passwordSchema),
    defaultValues: {
      currentPassword: "",
      newPassword: "",
      confirmPassword: ""
    }
  });
  const notificationForm = useForm({
    resolver: a(notificationSchema),
    defaultValues: {
      emailNotifications: true,
      smsNotifications: false,
      pushNotifications: true,
      marketingEmails: false
    }
  });
  const updateProfileMutation = useMutation({
    mutationFn: async (data) => {
      await new Promise((resolve) => setTimeout(resolve, 1e3));
      return { success: true, user: { ...user, ...data } };
    },
    onSuccess: (response) => {
      updateUser(response.user);
      toast.success("Profile updated successfully");
    },
    onError: (error) => {
      toast.error(`Failed to update profile: ${error.message}`);
    }
  });
  const updatePasswordMutation = useMutation({
    mutationFn: async (data) => {
      await new Promise((resolve) => setTimeout(resolve, 1e3));
      return { success: true };
    },
    onSuccess: () => {
      passwordForm.reset();
      toast.success("Password updated successfully");
    },
    onError: (error) => {
      toast.error(`Failed to update password: ${error.message}`);
    }
  });
  const updateNotificationsMutation = useMutation({
    mutationFn: async (data) => {
      await new Promise((resolve) => setTimeout(resolve, 1e3));
      return { success: true };
    },
    onSuccess: () => {
      toast.success("Notification preferences updated");
    },
    onError: (error) => {
      toast.error(`Failed to update preferences: ${error.message}`);
    }
  });
  const onProfileSubmit = (data) => {
    updateProfileMutation.mutate(data);
  };
  const onPasswordSubmit = (data) => {
    updatePasswordMutation.mutate(data);
  };
  const onNotificationSubmit = (data) => {
    updateNotificationsMutation.mutate(data);
  };
  const tabs = [
    { id: "profile", label: "Profile", icon: User },
    { id: "password", label: "Password", icon: Lock },
    { id: "notifications", label: "Notifications", icon: Bell }
  ];
  return /* @__PURE__ */ jsxRuntimeExports.jsxs("div", { className: "p-6 max-w-4xl mx-auto", children: [
    /* @__PURE__ */ jsxRuntimeExports.jsxs("div", { className: "mb-8", children: [
      /* @__PURE__ */ jsxRuntimeExports.jsx("h1", { className: "text-3xl font-bold text-gray-900", children: "Account Settings" }),
      /* @__PURE__ */ jsxRuntimeExports.jsx("p", { className: "text-gray-600", children: "Manage your account information and preferences" })
    ] }),
    /* @__PURE__ */ jsxRuntimeExports.jsx(Card, { className: "mb-8", children: /* @__PURE__ */ jsxRuntimeExports.jsx(CardContent, { className: "pt-6", children: /* @__PURE__ */ jsxRuntimeExports.jsxs("div", { className: "flex items-center space-x-4", children: [
      /* @__PURE__ */ jsxRuntimeExports.jsx("div", { className: "w-16 h-16 bg-blue-100 rounded-full flex items-center justify-center", children: /* @__PURE__ */ jsxRuntimeExports.jsx(User, { className: "h-8 w-8 text-blue-600" }) }),
      /* @__PURE__ */ jsxRuntimeExports.jsxs("div", { children: [
        /* @__PURE__ */ jsxRuntimeExports.jsx("h2", { className: "text-xl font-semibold", children: user?.name }),
        /* @__PURE__ */ jsxRuntimeExports.jsx("p", { className: "text-gray-600", children: user?.email }),
        /* @__PURE__ */ jsxRuntimeExports.jsx(Badge, { variant: "outline", className: "mt-1", children: user?.role })
      ] })
    ] }) }) }),
    /* @__PURE__ */ jsxRuntimeExports.jsx("div", { className: "mb-8", children: /* @__PURE__ */ jsxRuntimeExports.jsx("nav", { className: "flex space-x-1 bg-gray-100 p-1 rounded-lg", children: tabs.map((tab) => /* @__PURE__ */ jsxRuntimeExports.jsxs(
      "button",
      {
        onClick: () => setActiveTab(tab.id),
        className: `flex items-center space-x-2 px-4 py-2 rounded-md font-medium transition-colors ${activeTab === tab.id ? "bg-white text-blue-600 shadow-sm" : "text-gray-600 hover:text-gray-900"}`,
        children: [
          /* @__PURE__ */ jsxRuntimeExports.jsx(tab.icon, { className: "h-4 w-4" }),
          /* @__PURE__ */ jsxRuntimeExports.jsx("span", { children: tab.label })
        ]
      },
      tab.id
    )) }) }),
    activeTab === "profile" && /* @__PURE__ */ jsxRuntimeExports.jsxs(Card, { children: [
      /* @__PURE__ */ jsxRuntimeExports.jsxs(CardHeader, { children: [
        /* @__PURE__ */ jsxRuntimeExports.jsxs(CardTitle, { className: "flex items-center gap-2", children: [
          /* @__PURE__ */ jsxRuntimeExports.jsx(User, { className: "h-5 w-5" }),
          "Profile Information"
        ] }),
        /* @__PURE__ */ jsxRuntimeExports.jsx(CardDescription, { children: "Update your personal information and contact details" })
      ] }),
      /* @__PURE__ */ jsxRuntimeExports.jsx(CardContent, { children: /* @__PURE__ */ jsxRuntimeExports.jsxs("form", { onSubmit: profileForm.handleSubmit(onProfileSubmit), className: "space-y-6", children: [
        /* @__PURE__ */ jsxRuntimeExports.jsxs("div", { className: "grid gap-6 md:grid-cols-2", children: [
          /* @__PURE__ */ jsxRuntimeExports.jsxs("div", { className: "space-y-2", children: [
            /* @__PURE__ */ jsxRuntimeExports.jsxs(Label, { htmlFor: "name", children: [
              /* @__PURE__ */ jsxRuntimeExports.jsx(User, { className: "h-4 w-4 inline mr-1" }),
              "Full Name *"
            ] }),
            /* @__PURE__ */ jsxRuntimeExports.jsx(
              Input,
              {
                id: "name",
                ...profileForm.register("name"),
                error: !!profileForm.formState.errors.name
              }
            ),
            profileForm.formState.errors.name && /* @__PURE__ */ jsxRuntimeExports.jsx("p", { className: "text-sm text-red-600", children: profileForm.formState.errors.name.message })
          ] }),
          /* @__PURE__ */ jsxRuntimeExports.jsxs("div", { className: "space-y-2", children: [
            /* @__PURE__ */ jsxRuntimeExports.jsxs(Label, { htmlFor: "email", children: [
              /* @__PURE__ */ jsxRuntimeExports.jsx(Mail, { className: "h-4 w-4 inline mr-1" }),
              "Email Address *"
            ] }),
            /* @__PURE__ */ jsxRuntimeExports.jsx(
              Input,
              {
                id: "email",
                type: "email",
                ...profileForm.register("email"),
                error: !!profileForm.formState.errors.email
              }
            ),
            profileForm.formState.errors.email && /* @__PURE__ */ jsxRuntimeExports.jsx("p", { className: "text-sm text-red-600", children: profileForm.formState.errors.email.message })
          ] }),
          /* @__PURE__ */ jsxRuntimeExports.jsxs("div", { className: "space-y-2", children: [
            /* @__PURE__ */ jsxRuntimeExports.jsxs(Label, { htmlFor: "phone", children: [
              /* @__PURE__ */ jsxRuntimeExports.jsx(Phone, { className: "h-4 w-4 inline mr-1" }),
              "Phone Number *"
            ] }),
            /* @__PURE__ */ jsxRuntimeExports.jsx(
              Input,
              {
                id: "phone",
                ...profileForm.register("phone"),
                placeholder: "+1 (555) 123-4567",
                error: !!profileForm.formState.errors.phone
              }
            ),
            profileForm.formState.errors.phone && /* @__PURE__ */ jsxRuntimeExports.jsx("p", { className: "text-sm text-red-600", children: profileForm.formState.errors.phone.message })
          ] }),
          /* @__PURE__ */ jsxRuntimeExports.jsxs("div", { className: "space-y-2 md:col-span-2", children: [
            /* @__PURE__ */ jsxRuntimeExports.jsxs(Label, { htmlFor: "address", children: [
              /* @__PURE__ */ jsxRuntimeExports.jsx(MapPin, { className: "h-4 w-4 inline mr-1" }),
              "Address *"
            ] }),
            /* @__PURE__ */ jsxRuntimeExports.jsx(
              Input,
              {
                id: "address",
                ...profileForm.register("address"),
                placeholder: "123 Main St, City, State, ZIP",
                error: !!profileForm.formState.errors.address
              }
            ),
            profileForm.formState.errors.address && /* @__PURE__ */ jsxRuntimeExports.jsx("p", { className: "text-sm text-red-600", children: profileForm.formState.errors.address.message })
          ] })
        ] }),
        /* @__PURE__ */ jsxRuntimeExports.jsx("div", { className: "flex justify-end", children: /* @__PURE__ */ jsxRuntimeExports.jsx(
          Button,
          {
            type: "submit",
            disabled: updateProfileMutation.isPending,
            className: "min-w-[120px]",
            children: updateProfileMutation.isPending ? "Saving..." : /* @__PURE__ */ jsxRuntimeExports.jsxs(jsxRuntimeExports.Fragment, { children: [
              /* @__PURE__ */ jsxRuntimeExports.jsx(Save, { className: "h-4 w-4 mr-2" }),
              "Save Changes"
            ] })
          }
        ) })
      ] }) })
    ] }),
    activeTab === "password" && /* @__PURE__ */ jsxRuntimeExports.jsxs(Card, { children: [
      /* @__PURE__ */ jsxRuntimeExports.jsxs(CardHeader, { children: [
        /* @__PURE__ */ jsxRuntimeExports.jsxs(CardTitle, { className: "flex items-center gap-2", children: [
          /* @__PURE__ */ jsxRuntimeExports.jsx(Lock, { className: "h-5 w-5" }),
          "Change Password"
        ] }),
        /* @__PURE__ */ jsxRuntimeExports.jsx(CardDescription, { children: "Update your password to keep your account secure" })
      ] }),
      /* @__PURE__ */ jsxRuntimeExports.jsx(CardContent, { children: /* @__PURE__ */ jsxRuntimeExports.jsxs("form", { onSubmit: passwordForm.handleSubmit(onPasswordSubmit), className: "space-y-6", children: [
        /* @__PURE__ */ jsxRuntimeExports.jsxs("div", { className: "space-y-4 max-w-md", children: [
          /* @__PURE__ */ jsxRuntimeExports.jsxs("div", { className: "space-y-2", children: [
            /* @__PURE__ */ jsxRuntimeExports.jsx(Label, { htmlFor: "currentPassword", children: "Current Password *" }),
            /* @__PURE__ */ jsxRuntimeExports.jsx(
              Input,
              {
                id: "currentPassword",
                type: "password",
                ...passwordForm.register("currentPassword"),
                error: !!passwordForm.formState.errors.currentPassword
              }
            ),
            passwordForm.formState.errors.currentPassword && /* @__PURE__ */ jsxRuntimeExports.jsx("p", { className: "text-sm text-red-600", children: passwordForm.formState.errors.currentPassword.message })
          ] }),
          /* @__PURE__ */ jsxRuntimeExports.jsxs("div", { className: "space-y-2", children: [
            /* @__PURE__ */ jsxRuntimeExports.jsx(Label, { htmlFor: "newPassword", children: "New Password *" }),
            /* @__PURE__ */ jsxRuntimeExports.jsx(
              Input,
              {
                id: "newPassword",
                type: "password",
                ...passwordForm.register("newPassword"),
                error: !!passwordForm.formState.errors.newPassword
              }
            ),
            passwordForm.formState.errors.newPassword && /* @__PURE__ */ jsxRuntimeExports.jsx("p", { className: "text-sm text-red-600", children: passwordForm.formState.errors.newPassword.message })
          ] }),
          /* @__PURE__ */ jsxRuntimeExports.jsxs("div", { className: "space-y-2", children: [
            /* @__PURE__ */ jsxRuntimeExports.jsx(Label, { htmlFor: "confirmPassword", children: "Confirm New Password *" }),
            /* @__PURE__ */ jsxRuntimeExports.jsx(
              Input,
              {
                id: "confirmPassword",
                type: "password",
                ...passwordForm.register("confirmPassword"),
                error: !!passwordForm.formState.errors.confirmPassword
              }
            ),
            passwordForm.formState.errors.confirmPassword && /* @__PURE__ */ jsxRuntimeExports.jsx("p", { className: "text-sm text-red-600", children: passwordForm.formState.errors.confirmPassword.message })
          ] })
        ] }),
        /* @__PURE__ */ jsxRuntimeExports.jsxs(Alert, { children: [
          /* @__PURE__ */ jsxRuntimeExports.jsx(AlertCircle, { className: "h-4 w-4" }),
          /* @__PURE__ */ jsxRuntimeExports.jsx(AlertDescription, { children: "Password must be at least 8 characters long and contain a mix of letters, numbers, and symbols." })
        ] }),
        /* @__PURE__ */ jsxRuntimeExports.jsx("div", { className: "flex justify-end", children: /* @__PURE__ */ jsxRuntimeExports.jsx(
          Button,
          {
            type: "submit",
            disabled: updatePasswordMutation.isPending,
            className: "min-w-[140px]",
            children: updatePasswordMutation.isPending ? "Updating..." : /* @__PURE__ */ jsxRuntimeExports.jsxs(jsxRuntimeExports.Fragment, { children: [
              /* @__PURE__ */ jsxRuntimeExports.jsx(Lock, { className: "h-4 w-4 mr-2" }),
              "Update Password"
            ] })
          }
        ) })
      ] }) })
    ] }),
    activeTab === "notifications" && /* @__PURE__ */ jsxRuntimeExports.jsxs(Card, { children: [
      /* @__PURE__ */ jsxRuntimeExports.jsxs(CardHeader, { children: [
        /* @__PURE__ */ jsxRuntimeExports.jsxs(CardTitle, { className: "flex items-center gap-2", children: [
          /* @__PURE__ */ jsxRuntimeExports.jsx(Bell, { className: "h-5 w-5" }),
          "Notification Preferences"
        ] }),
        /* @__PURE__ */ jsxRuntimeExports.jsx(CardDescription, { children: "Choose how you want to receive notifications about your shipments" })
      ] }),
      /* @__PURE__ */ jsxRuntimeExports.jsx(CardContent, { children: /* @__PURE__ */ jsxRuntimeExports.jsxs("form", { onSubmit: notificationForm.handleSubmit(onNotificationSubmit), className: "space-y-6", children: [
        /* @__PURE__ */ jsxRuntimeExports.jsxs("div", { className: "space-y-4", children: [
          /* @__PURE__ */ jsxRuntimeExports.jsxs("div", { className: "flex items-center justify-between", children: [
            /* @__PURE__ */ jsxRuntimeExports.jsxs("div", { children: [
              /* @__PURE__ */ jsxRuntimeExports.jsx(Label, { className: "text-base", children: "Email Notifications" }),
              /* @__PURE__ */ jsxRuntimeExports.jsx("p", { className: "text-sm text-gray-600", children: "Receive shipment updates via email" })
            ] }),
            /* @__PURE__ */ jsxRuntimeExports.jsx(
              "input",
              {
                type: "checkbox",
                ...notificationForm.register("emailNotifications"),
                className: "h-4 w-4 text-blue-600 focus:ring-blue-500 border-gray-300 rounded"
              }
            )
          ] }),
          /* @__PURE__ */ jsxRuntimeExports.jsxs("div", { className: "flex items-center justify-between", children: [
            /* @__PURE__ */ jsxRuntimeExports.jsxs("div", { children: [
              /* @__PURE__ */ jsxRuntimeExports.jsx(Label, { className: "text-base", children: "SMS Notifications" }),
              /* @__PURE__ */ jsxRuntimeExports.jsx("p", { className: "text-sm text-gray-600", children: "Receive shipment updates via text message" })
            ] }),
            /* @__PURE__ */ jsxRuntimeExports.jsx(
              "input",
              {
                type: "checkbox",
                ...notificationForm.register("smsNotifications"),
                className: "h-4 w-4 text-blue-600 focus:ring-blue-500 border-gray-300 rounded"
              }
            )
          ] }),
          /* @__PURE__ */ jsxRuntimeExports.jsxs("div", { className: "flex items-center justify-between", children: [
            /* @__PURE__ */ jsxRuntimeExports.jsxs("div", { children: [
              /* @__PURE__ */ jsxRuntimeExports.jsx(Label, { className: "text-base", children: "Push Notifications" }),
              /* @__PURE__ */ jsxRuntimeExports.jsx("p", { className: "text-sm text-gray-600", children: "Receive notifications in your browser" })
            ] }),
            /* @__PURE__ */ jsxRuntimeExports.jsx(
              "input",
              {
                type: "checkbox",
                ...notificationForm.register("pushNotifications"),
                className: "h-4 w-4 text-blue-600 focus:ring-blue-500 border-gray-300 rounded"
              }
            )
          ] }),
          /* @__PURE__ */ jsxRuntimeExports.jsxs("div", { className: "flex items-center justify-between", children: [
            /* @__PURE__ */ jsxRuntimeExports.jsxs("div", { children: [
              /* @__PURE__ */ jsxRuntimeExports.jsx(Label, { className: "text-base", children: "Marketing Emails" }),
              /* @__PURE__ */ jsxRuntimeExports.jsx("p", { className: "text-sm text-gray-600", children: "Receive promotional emails and offers" })
            ] }),
            /* @__PURE__ */ jsxRuntimeExports.jsx(
              "input",
              {
                type: "checkbox",
                ...notificationForm.register("marketingEmails"),
                className: "h-4 w-4 text-blue-600 focus:ring-blue-500 border-gray-300 rounded"
              }
            )
          ] })
        ] }),
        /* @__PURE__ */ jsxRuntimeExports.jsx("div", { className: "flex justify-end", children: /* @__PURE__ */ jsxRuntimeExports.jsx(
          Button,
          {
            type: "submit",
            disabled: updateNotificationsMutation.isPending,
            className: "min-w-[140px]",
            children: updateNotificationsMutation.isPending ? "Saving..." : /* @__PURE__ */ jsxRuntimeExports.jsxs(jsxRuntimeExports.Fragment, { children: [
              /* @__PURE__ */ jsxRuntimeExports.jsx(Save, { className: "h-4 w-4 mr-2" }),
              "Save Preferences"
            ] })
          }
        ) })
      ] }) })
    ] })
  ] });
};
export {
  ProfilePage
};
