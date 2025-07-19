import { Navigate, Outlet } from 'react-router-dom';
import { useAuthStore } from '@/stores/auth-store';

/**
 * Protected Layout Component
 * 
 * This layout component handles authentication checks for all nested routes.
 * It renders an Outlet for child routes if the user is authenticated,
 * otherwise redirects to the login page.
 */
export const ProtectedLayout = () => {
  const { isAuthenticated } = useAuthStore();

  if (!isAuthenticated) {
    return <Navigate to="/login" replace />;
  }

  return <Outlet />;
};