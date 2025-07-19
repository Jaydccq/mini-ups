import { Navigate, Outlet } from 'react-router-dom';
import { useAuthStore } from '@/stores/auth-store';

/**
 * Admin Layout Component
 * 
 * This layout component handles both authentication and admin role authorization.
 * It renders an Outlet for child routes if the user is authenticated and has admin role,
 * otherwise redirects appropriately.
 */
export const AdminLayout = () => {
  const { isAuthenticated, user } = useAuthStore();

  if (!isAuthenticated || !user) {
    return <Navigate to="/login" replace />;
  }

  if (user.role !== 'ADMIN') {
    // Redirect non-admin users to dashboard
    return <Navigate to="/dashboard" replace />;
  }

  return <Outlet />;
};