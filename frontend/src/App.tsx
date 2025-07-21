import { Suspense, lazy } from 'react'
import { Routes, Route } from 'react-router-dom'
import { Toaster } from '@/components/ui/toaster'
import { Layout } from '@/components/layout/Layout'
import { ProtectedLayout } from '@/components/layout/ProtectedLayout'
import { AdminLayout } from '@/components/layout/AdminLayout'
import { Skeleton } from '@/components/ui/skeleton'

// Lazy load public pages
const HomePage = lazy(() => import('@/pages/HomePage').then(module => ({ default: module.HomePage })))
const LoginPage = lazy(() => import('@/pages/LoginPage').then(module => ({ default: module.LoginPage })))
const RegisterPage = lazy(() => import('@/pages/RegisterPage').then(module => ({ default: module.RegisterPage })))
const TrackingPage = lazy(() => import('@/pages/TrackingPage').then(module => ({ default: module.TrackingPage })))

// Lazy load protected pages
const DashboardPage = lazy(() => import('@/pages/DashboardPage').then(module => ({ default: module.DashboardPage })))
const ShipmentsPage = lazy(() => import('@/pages/ShipmentsPage').then(module => ({ default: module.ShipmentsPage })))
const ShipmentDetailPage = lazy(() => import('@/pages/ShipmentDetailPage').then(module => ({ default: module.ShipmentDetailPage })))
const CreateShipmentPage = lazy(() => import('@/pages/CreateShipmentPage').then(module => ({ default: module.CreateShipmentPage })))
const ProfilePage = lazy(() => import('@/pages/ProfilePage').then(module => ({ default: module.ProfilePage })))

// Lazy load admin pages
const UserManagementPage = lazy(() => import('@/pages/admin/UserManagementPage').then(module => ({ default: module.UserManagementPage })))
const ShipmentManagementPage = lazy(() => import('@/pages/admin/ShipmentManagementPage').then(module => ({ default: module.ShipmentManagementPage })))
const AnalyticsPage = lazy(() => import('@/pages/admin/AnalyticsPage').then(module => ({ default: module.AnalyticsPage })))

// Loading component
const PageLoader = () => (
  <div className="flex h-screen w-full items-center justify-center">
    <div className="space-y-4 w-full max-w-md">
      <Skeleton className="h-8 w-3/4" />
      <Skeleton className="h-4 w-full" />
      <Skeleton className="h-4 w-2/3" />
      <div className="grid grid-cols-2 gap-4">
        <Skeleton className="h-24 w-full" />
        <Skeleton className="h-24 w-full" />
      </div>
    </div>
  </div>
)

function App() {
  return (
    <div className="min-h-screen bg-background">
      <Layout>
        <Suspense fallback={<PageLoader />}>
          <Routes>
            {/* Public routes */}
            <Route path="/" element={<HomePage />} />
            <Route path="/login" element={<LoginPage />} />
            <Route path="/register" element={<RegisterPage />} />
            <Route path="/tracking" element={<TrackingPage />} />
            
            {/* Protected routes */}
            <Route element={<ProtectedLayout />}>
              <Route path="/dashboard" element={<DashboardPage />} />
              <Route path="/shipments" element={<ShipmentsPage />} />
              <Route path="/shipments/create" element={<CreateShipmentPage />} />
              <Route path="/shipments/tracking/:trackingNumber" element={<ShipmentDetailPage />} />
              <Route path="/profile" element={<ProfilePage />} />
            </Route>
            
            {/* Admin routes */}
            <Route element={<AdminLayout />}>
              <Route path="/admin/users" element={<UserManagementPage />} />
              <Route path="/admin/shipments" element={<ShipmentManagementPage />} />
              <Route path="/admin/analytics" element={<AnalyticsPage />} />
            </Route>
          </Routes>
        </Suspense>
      </Layout>
      <Toaster />
    </div>
  )
}

export default App