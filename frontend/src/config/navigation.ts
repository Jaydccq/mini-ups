import {
  Home,
  Package,
  Search,
  Plus,
  User,
  Settings,
  Truck,
  MapPin,
  ClipboardList,
  Users,
  BarChart3,
  Activity,
  type LucideIcon
} from 'lucide-react'

export interface NavItem {
  path: string
  label: string
  icon: LucideIcon
  roles: string[] // Empty array means public page
  inNav: boolean // Whether to show in the navigation
  children?: NavItem[]
}

export const navigationConfig: NavItem[] = [
  // Public pages
  {
    path: '/',
    label: 'Home',
    icon: Home,
    roles: [],
    inNav: false,
  },
  {
    path: '/tracking',
    label: 'Track Package',
    icon: Search,
    roles: [],
    inNav: true,
  },
  
  // User pages
  {
    path: '/dashboard',
    label: 'Dashboard',
    icon: Home,
    roles: ['USER', 'ADMIN'],
    inNav: true,
  },
  {
    path: '/shipments',
    label: 'My Shipments',
    icon: Package,
    roles: ['USER', 'ADMIN'],
    inNav: true,
  },
  {
    path: '/shipments/create',
    label: 'Create Shipment',
    icon: Plus,
    roles: ['USER', 'ADMIN'],
    inNav: false,
  },
  {
    path: '/profile',
    label: 'Profile',
    icon: User,
    roles: ['USER', 'ADMIN'],
    inNav: false,
  },

  // Admin pages
  {
    path: '/admin',
    label: 'Admin Center',
    icon: Settings,
    roles: ['ADMIN'],
    inNav: true,
    children: [
      {
        path: '/admin/users',
        label: 'User Management',
        icon: Users,
        roles: ['ADMIN'],
        inNav: true,
      },
      {
        path: '/admin/shipments',
        label: 'Shipment Management',
        icon: Package,
        roles: ['ADMIN'],
        inNav: true,
      },
      {
        path: '/admin/analytics',
        label: 'Analytics',
        icon: BarChart3,
        roles: ['ADMIN'],
        inNav: true,
      },
      {
        path: '/admin/debug/simulator',
        label: 'World Simulator Debug',
        icon: Activity,
        roles: ['ADMIN'],
        inNav: true,
      },
    ],
  },
  {
    path: '/trucks',
    label: 'Truck Management',
    icon: Truck,
    roles: ['ADMIN'],
    inNav: true,
  },

  // Driver pages
  {
    path: '/deliveries',
    label: 'Delivery Tasks',
    icon: ClipboardList,
    roles: ['DRIVER'],
    inNav: true,
  },
  {
    path: '/truck-status',
    label: 'Truck Status',
    icon: MapPin,
    roles: ['DRIVER'],
    inNav: true,
  },
]