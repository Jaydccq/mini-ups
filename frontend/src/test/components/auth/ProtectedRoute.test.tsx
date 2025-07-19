import { describe, it, expect, vi, beforeEach } from 'vitest'
import { render, screen } from '@testing-library/react'
import { BrowserRouter } from 'react-router-dom'
import { ProtectedRoute } from '@/components/auth/ProtectedRoute'

// Mock the auth store
const mockUseAuthStore = vi.fn()
vi.mock('@/stores/authStore', () => ({
  useAuthStore: () => mockUseAuthStore()
}))

// Mock React Router
vi.mock('react-router-dom', async () => {
  const actual = await vi.importActual('react-router-dom')
  return {
    ...actual,
    Navigate: ({ to }: { to: string }) => <div data-testid="navigate">Redirecting to {to}</div>
  }
})

const TestComponent = () => <div data-testid="protected-content">Protected Content</div>

const renderWithRouter = (children: React.ReactNode) => {
  return render(
    <BrowserRouter>
      {children}
    </BrowserRouter>
  )
}

describe('ProtectedRoute', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('should render children when user is authenticated', () => {
    // Given
    mockUseAuthStore.mockReturnValue({
      isAuthenticated: true,
      user: { id: 1, username: 'testuser', role: 'USER' }
    })

    // When
    renderWithRouter(
      <ProtectedRoute>
        <TestComponent />
      </ProtectedRoute>
    )

    // Then
    expect(screen.getByTestId('protected-content')).toBeInTheDocument()
    expect(screen.queryByTestId('navigate')).not.toBeInTheDocument()
  })

  it('should redirect to login when user is not authenticated', () => {
    // Given
    mockUseAuthStore.mockReturnValue({
      isAuthenticated: false,
      user: null
    })

    // When
    renderWithRouter(
      <ProtectedRoute>
        <TestComponent />
      </ProtectedRoute>
    )

    // Then
    expect(screen.queryByTestId('protected-content')).not.toBeInTheDocument()
    expect(screen.getByTestId('navigate')).toBeInTheDocument()
    expect(screen.getByText('Redirecting to /login')).toBeInTheDocument()
  })

  it('should redirect to login when user is null', () => {
    // Given
    mockUseAuthStore.mockReturnValue({
      isAuthenticated: false,
      user: null
    })

    // When
    renderWithRouter(
      <ProtectedRoute>
        <TestComponent />
      </ProtectedRoute>
    )

    // Then
    expect(screen.queryByTestId('protected-content')).not.toBeInTheDocument()
    expect(screen.getByTestId('navigate')).toBeInTheDocument()
  })

  it('should allow access for admin role when requireAdmin is true', () => {
    // Given
    mockUseAuthStore.mockReturnValue({
      isAuthenticated: true,
      user: { id: 1, username: 'admin', role: 'ADMIN' }
    })

    // When
    renderWithRouter(
      <ProtectedRoute requireAdmin={true}>
        <TestComponent />
      </ProtectedRoute>
    )

    // Then
    expect(screen.getByTestId('protected-content')).toBeInTheDocument()
    expect(screen.queryByTestId('navigate')).not.toBeInTheDocument()
  })

  it('should deny access for non-admin role when requireAdmin is true', () => {
    // Given
    mockUseAuthStore.mockReturnValue({
      isAuthenticated: true,
      user: { id: 1, username: 'user', role: 'USER' }
    })

    // When
    renderWithRouter(
      <ProtectedRoute requireAdmin={true}>
        <TestComponent />
      </ProtectedRoute>
    )

    // Then
    expect(screen.queryByTestId('protected-content')).not.toBeInTheDocument()
    expect(screen.getByTestId('navigate')).toBeInTheDocument()
    expect(screen.getByText('Redirecting to /')).toBeInTheDocument()
  })

  it('should allow access for driver role when allowedRoles includes DRIVER', () => {
    // Given
    mockUseAuthStore.mockReturnValue({
      isAuthenticated: true,
      user: { id: 1, username: 'driver', role: 'DRIVER' }
    })

    // When
    renderWithRouter(
      <ProtectedRoute allowedRoles={['ADMIN', 'DRIVER']}>
        <TestComponent />
      </ProtectedRoute>
    )

    // Then
    expect(screen.getByTestId('protected-content')).toBeInTheDocument()
    expect(screen.queryByTestId('navigate')).not.toBeInTheDocument()
  })

  it('should deny access when user role is not in allowedRoles', () => {
    // Given
    mockUseAuthStore.mockReturnValue({
      isAuthenticated: true,
      user: { id: 1, username: 'user', role: 'USER' }
    })

    // When
    renderWithRouter(
      <ProtectedRoute allowedRoles={['ADMIN', 'DRIVER']}>
        <TestComponent />
      </ProtectedRoute>
    )

    // Then
    expect(screen.queryByTestId('protected-content')).not.toBeInTheDocument()
    expect(screen.getByTestId('navigate')).toBeInTheDocument()
  })

  it('should handle undefined user gracefully', () => {
    // Given
    mockUseAuthStore.mockReturnValue({
      isAuthenticated: false,
      user: undefined
    })

    // When
    renderWithRouter(
      <ProtectedRoute>
        <TestComponent />
      </ProtectedRoute>
    )

    // Then
    expect(screen.queryByTestId('protected-content')).not.toBeInTheDocument()
    expect(screen.getByTestId('navigate')).toBeInTheDocument()
  })

  it('should handle multiple children', () => {
    // Given
    mockUseAuthStore.mockReturnValue({
      isAuthenticated: true,
      user: { id: 1, username: 'testuser', role: 'USER' }
    })

    // When
    renderWithRouter(
      <ProtectedRoute>
        <div data-testid="child1">Child 1</div>
        <div data-testid="child2">Child 2</div>
      </ProtectedRoute>
    )

    // Then
    expect(screen.getByTestId('child1')).toBeInTheDocument()
    expect(screen.getByTestId('child2')).toBeInTheDocument()
  })

  it('should pass through custom redirect path', () => {
    // Given
    mockUseAuthStore.mockReturnValue({
      isAuthenticated: false,
      user: null
    })

    // When
    renderWithRouter(
      <ProtectedRoute redirectTo="/custom-login">
        <TestComponent />
      </ProtectedRoute>
    )

    // Then
    expect(screen.getByText('Redirecting to /custom-login')).toBeInTheDocument()
  })
})