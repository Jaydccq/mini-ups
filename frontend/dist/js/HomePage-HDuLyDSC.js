import { r as reactExports, u as useNavigate, j as jsxRuntimeExports, P as Package, S as Search, B as Button, T as Truck, M as MapPin } from "./index-D-bA2lkg.js";
import { C as Card, a as CardHeader, b as CardTitle, c as CardDescription, d as CardContent } from "./card-CsLEcWf3.js";
import { I as Input } from "./input-BHuFzm78.js";
import { B as Badge } from "./badge-2PvS7lZW.js";
import { A as ArrowRight } from "./arrow-right-Dc_2qAW5.js";
import { S as Star } from "./star-DGBC-hS9.js";
import { C as CheckCircle } from "./check-circle-9Vi05KqK.js";
import { S as Shield } from "./shield-DvnO75-W.js";
import { C as Clock } from "./clock-BBOft3LF.js";
const HomePage = () => {
  const [trackingNumber, setTrackingNumber] = reactExports.useState("");
  const navigate = useNavigate();
  const handleTrackPackage = () => {
    if (trackingNumber.trim()) {
      navigate(`/tracking?number=${trackingNumber}`);
    }
  };
  const features = [
    {
      icon: /* @__PURE__ */ jsxRuntimeExports.jsx(Truck, { className: "h-8 w-8 text-primary" }),
      title: "Fast Delivery",
      description: "Next-day delivery available to most locations"
    },
    {
      icon: /* @__PURE__ */ jsxRuntimeExports.jsx(Shield, { className: "h-8 w-8 text-primary" }),
      title: "Secure Shipping",
      description: "Your packages are protected with our insurance coverage"
    },
    {
      icon: /* @__PURE__ */ jsxRuntimeExports.jsx(MapPin, { className: "h-8 w-8 text-primary" }),
      title: "Real-Time Tracking",
      description: "Track your package every step of the way"
    },
    {
      icon: /* @__PURE__ */ jsxRuntimeExports.jsx(Clock, { className: "h-8 w-8 text-primary" }),
      title: "Flexible Scheduling",
      description: "Choose delivery times that work for you"
    }
  ];
  const testimonials = [
    {
      name: "Sarah Johnson",
      role: "Small Business Owner",
      content: "Mini UPS has transformed how I ship products to my customers. Reliable and affordable!",
      rating: 5
    },
    {
      name: "Michael Chen",
      role: "E-commerce Manager",
      content: "The real-time tracking feature is amazing. My customers love being able to see exactly where their packages are.",
      rating: 5
    },
    {
      name: "Emily Rodriguez",
      role: "Online Seller",
      content: "Customer service is excellent and shipping rates are competitive. Highly recommend!",
      rating: 5
    }
  ];
  return /* @__PURE__ */ jsxRuntimeExports.jsxs("div", { className: "min-h-screen", children: [
    /* @__PURE__ */ jsxRuntimeExports.jsx("section", { className: "bg-gradient-to-br from-primary/10 via-background to-accent/5 py-20", children: /* @__PURE__ */ jsxRuntimeExports.jsx("div", { className: "container mx-auto px-4", children: /* @__PURE__ */ jsxRuntimeExports.jsxs("div", { className: "max-w-4xl mx-auto text-center", children: [
      /* @__PURE__ */ jsxRuntimeExports.jsxs(Badge, { className: "mb-6", variant: "secondary", children: [
        /* @__PURE__ */ jsxRuntimeExports.jsx(Package, { className: "h-4 w-4 mr-2" }),
        "Trusted by 10,000+ businesses"
      ] }),
      /* @__PURE__ */ jsxRuntimeExports.jsx("h1", { className: "text-4xl md:text-6xl font-bold mb-6 bg-gradient-to-r from-primary to-accent bg-clip-text text-transparent", children: "Fast, Reliable Package Delivery" }),
      /* @__PURE__ */ jsxRuntimeExports.jsx("p", { className: "text-lg md:text-xl text-muted-foreground mb-8 max-w-2xl mx-auto", children: "Experience the future of shipping with Mini UPS. From small packages to large freight, we deliver with speed, security, and transparency." }),
      /* @__PURE__ */ jsxRuntimeExports.jsxs(Card, { className: "max-w-md mx-auto mb-8", children: [
        /* @__PURE__ */ jsxRuntimeExports.jsxs(CardHeader, { children: [
          /* @__PURE__ */ jsxRuntimeExports.jsxs(CardTitle, { className: "flex items-center gap-2", children: [
            /* @__PURE__ */ jsxRuntimeExports.jsx(Search, { className: "h-5 w-5" }),
            "Track Your Package"
          ] }),
          /* @__PURE__ */ jsxRuntimeExports.jsx(CardDescription, { children: "Enter your tracking number to get real-time updates" })
        ] }),
        /* @__PURE__ */ jsxRuntimeExports.jsx(CardContent, { children: /* @__PURE__ */ jsxRuntimeExports.jsxs("div", { className: "flex gap-2", children: [
          /* @__PURE__ */ jsxRuntimeExports.jsx(
            Input,
            {
              placeholder: "Enter tracking number",
              value: trackingNumber,
              onChange: (e) => setTrackingNumber(e.target.value),
              onKeyPress: (e) => e.key === "Enter" && handleTrackPackage(),
              className: "flex-1"
            }
          ),
          /* @__PURE__ */ jsxRuntimeExports.jsx(Button, { onClick: handleTrackPackage, disabled: !trackingNumber.trim(), children: /* @__PURE__ */ jsxRuntimeExports.jsx(Search, { className: "h-4 w-4" }) })
        ] }) })
      ] }),
      /* @__PURE__ */ jsxRuntimeExports.jsxs("div", { className: "flex flex-col sm:flex-row gap-4 justify-center", children: [
        /* @__PURE__ */ jsxRuntimeExports.jsxs(Button, { size: "lg", className: "text-lg px-8", onClick: () => navigate("/register"), children: [
          "Get Started",
          /* @__PURE__ */ jsxRuntimeExports.jsx(ArrowRight, { className: "h-5 w-5 ml-2" })
        ] }),
        /* @__PURE__ */ jsxRuntimeExports.jsx(Button, { size: "lg", variant: "outline", className: "text-lg px-8", onClick: () => navigate("/login"), children: "Sign In" })
      ] })
    ] }) }) }),
    /* @__PURE__ */ jsxRuntimeExports.jsx("section", { className: "py-20 bg-background", children: /* @__PURE__ */ jsxRuntimeExports.jsx("div", { className: "container mx-auto px-4", children: /* @__PURE__ */ jsxRuntimeExports.jsxs("div", { className: "max-w-4xl mx-auto", children: [
      /* @__PURE__ */ jsxRuntimeExports.jsxs("div", { className: "text-center mb-16", children: [
        /* @__PURE__ */ jsxRuntimeExports.jsx("h2", { className: "text-3xl md:text-4xl font-bold mb-4", children: "Why Choose Mini UPS?" }),
        /* @__PURE__ */ jsxRuntimeExports.jsx("p", { className: "text-lg text-muted-foreground", children: "We're committed to providing the best shipping experience possible" })
      ] }),
      /* @__PURE__ */ jsxRuntimeExports.jsx("div", { className: "grid md:grid-cols-2 lg:grid-cols-4 gap-6", children: features.map((feature, index) => /* @__PURE__ */ jsxRuntimeExports.jsxs(Card, { className: "text-center hover:shadow-lg transition-shadow", children: [
        /* @__PURE__ */ jsxRuntimeExports.jsxs(CardHeader, { children: [
          /* @__PURE__ */ jsxRuntimeExports.jsx("div", { className: "flex justify-center mb-4", children: feature.icon }),
          /* @__PURE__ */ jsxRuntimeExports.jsx(CardTitle, { className: "text-xl", children: feature.title })
        ] }),
        /* @__PURE__ */ jsxRuntimeExports.jsx(CardContent, { children: /* @__PURE__ */ jsxRuntimeExports.jsx("p", { className: "text-muted-foreground", children: feature.description }) })
      ] }, index)) })
    ] }) }) }),
    /* @__PURE__ */ jsxRuntimeExports.jsx("section", { className: "py-20 bg-muted/30", children: /* @__PURE__ */ jsxRuntimeExports.jsx("div", { className: "container mx-auto px-4", children: /* @__PURE__ */ jsxRuntimeExports.jsxs("div", { className: "max-w-4xl mx-auto", children: [
      /* @__PURE__ */ jsxRuntimeExports.jsxs("div", { className: "text-center mb-16", children: [
        /* @__PURE__ */ jsxRuntimeExports.jsx("h2", { className: "text-3xl md:text-4xl font-bold mb-4", children: "What Our Customers Say" }),
        /* @__PURE__ */ jsxRuntimeExports.jsx("p", { className: "text-lg text-muted-foreground", children: "Join thousands of satisfied customers who trust Mini UPS" })
      ] }),
      /* @__PURE__ */ jsxRuntimeExports.jsx("div", { className: "grid md:grid-cols-3 gap-6", children: testimonials.map((testimonial, index) => /* @__PURE__ */ jsxRuntimeExports.jsxs(Card, { className: "hover:shadow-lg transition-shadow", children: [
        /* @__PURE__ */ jsxRuntimeExports.jsxs(CardHeader, { children: [
          /* @__PURE__ */ jsxRuntimeExports.jsx("div", { className: "flex items-center gap-1 mb-2", children: Array.from({ length: testimonial.rating }).map((_, i) => /* @__PURE__ */ jsxRuntimeExports.jsx(Star, { className: "h-4 w-4 fill-yellow-400 text-yellow-400" }, i)) }),
          /* @__PURE__ */ jsxRuntimeExports.jsx(CardTitle, { className: "text-lg", children: testimonial.name }),
          /* @__PURE__ */ jsxRuntimeExports.jsx(CardDescription, { children: testimonial.role })
        ] }),
        /* @__PURE__ */ jsxRuntimeExports.jsx(CardContent, { children: /* @__PURE__ */ jsxRuntimeExports.jsxs("p", { className: "text-muted-foreground italic", children: [
          '"',
          testimonial.content,
          '"'
        ] }) })
      ] }, index)) })
    ] }) }) }),
    /* @__PURE__ */ jsxRuntimeExports.jsx("section", { className: "py-20 bg-primary text-primary-foreground", children: /* @__PURE__ */ jsxRuntimeExports.jsx("div", { className: "container mx-auto px-4", children: /* @__PURE__ */ jsxRuntimeExports.jsxs("div", { className: "max-w-2xl mx-auto text-center", children: [
      /* @__PURE__ */ jsxRuntimeExports.jsx("h2", { className: "text-3xl md:text-4xl font-bold mb-6", children: "Ready to Get Started?" }),
      /* @__PURE__ */ jsxRuntimeExports.jsx("p", { className: "text-lg mb-8 opacity-90", children: "Join thousands of businesses that trust Mini UPS for their shipping needs. Create your account today and experience the difference." }),
      /* @__PURE__ */ jsxRuntimeExports.jsxs(Button, { size: "lg", variant: "secondary", className: "text-lg px-8", onClick: () => navigate("/register"), children: [
        "Create Account",
        /* @__PURE__ */ jsxRuntimeExports.jsx(CheckCircle, { className: "h-5 w-5 ml-2" })
      ] })
    ] }) }) })
  ] });
};
export {
  HomePage
};
