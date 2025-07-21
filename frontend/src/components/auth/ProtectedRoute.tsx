import React from 'react'
import { Navigate } from 'react-router-dom'
import { useAuthStore } from '@/stores/auth-store'

interface ProtectedRouteProps {
  children: React.ReactNode
  requireAdmin?: boolean
  allowedRoles?: string[]
  redirectTo?: string
}

export const ProtectedRoute: React.FC<ProtectedRouteProps> = ({
  children,
  requireAdmin = false,
  allowedRoles,
  redirectTo = '/login'
}) => {
  const { isAuthenticated, user } = useAuthStore()

  // Check if user is authenticated
  if (!isAuthenticated || !user) {
    return <Navigate to={redirectTo} replace />
  }

  // Check admin requirement
  if (requireAdmin && user.role !== 'ADMIN') {
    return <Navigate to="/" replace />
  }

  // Check allowed roles
  if (allowedRoles && !allowedRoles.includes(user.role)) {
    return <Navigate to="/" replace />
  }

  return <>{children}</>
}