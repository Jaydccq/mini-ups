import { describe, it, expect, vi, beforeEach } from 'vitest'
import { render, screen, fireEvent } from '@testing-library/react'
import { BrowserRouter } from 'react-router-dom'
import { Layout } from '@/components/layout/Layout'

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
    useNavigate: () => vi.fn(),
    Link: ({ children, to }: { children: React.ReactNode, to: string }) => 
      <a href={to} data-testid={`link-${to.replace('/', '')}`}>{children}</a>
  }
})

// Mock Radix UI components
vi.mock('@radix-ui/react-navigation-menu', () => ({
  Root: ({ children }: { children: React.ReactNode }) => <nav data-testid="navigation">{children}</nav>,
  List: ({ children }: { children: React.ReactNode }) => <ul>{children}</ul>,
  Item: ({ children }: { children: React.ReactNode }) => <li>{children}</li>,
  Trigger: ({ children }: { children: React.ReactNode }) => <button>{children}</button>,
  Content: ({ children }: { children: React.ReactNode }) => <div>{children}</div>,
  Link: ({ children, href }: { children: React.ReactNode, href: string }) => 
    <a href={href}>{children}</a>,
  Viewport: ({ children }: { children: React.ReactNode }) => <div>{children}</div>
}))

vi.mock('@radix-ui/react-dropdown-menu', () => ({
  Root: ({ children }: { children: React.ReactNode }) => <div>{children}</div>,
  Trigger: ({ children, ...props }: { children: React.ReactNode }) => 
    <button data-testid="user-menu-trigger" {...props}>{children}</button>,
  Content: ({ children }: { children: React.ReactNode }) => 
    <div data-testid="user-menu-content">{children}</div>,
  Item: ({ children, onClick }: { children: React.ReactNode, onClick?: () => void }) => 
    <button onClick={onClick} data-testid="menu-item">{children}</button>,
  Separator: () => <hr data-testid="menu-separator" />
}))

const renderWithRouter = (children: React.ReactNode) => {
  return render(
    <BrowserRouter>
      {children}
    </BrowserRouter>
  )
}

describe('Layout', () => {
  const mockLogout = vi.fn()

  beforeEach(() => {
    vi.clearAllMocks()
    mockUseAuthStore.mockReturnValue({
      isAuthenticated: true,
      user: { id: 1, username: 'testuser', role: 'USER' },
      logout: mockLogout
    })
  })

  it('should render layout with navigation when user is authenticated', () => {
    // When
    renderWithRouter(
      <Layout>
        <div data-testid="page-content">Page Content</div>
      </Layout>
    )

    // Then
    expect(screen.getByTestId('navigation')).toBeInTheDocument()
    expect(screen.getByTestId('page-content')).toBeInTheDocument()
    expect(screen.getByText('Mini-UPS')).toBeInTheDocument()
  })

  it('should render navigation links for authenticated user', () => {
    // When
    renderWithRouter(
      <Layout>
        <div>Content</div>
      </Layout>
    )

    // Then
    expect(screen.getByTestId('link-dashboard')).toBeInTheDocument()
    expect(screen.getByTestId('link-tracking')).toBeInTheDocument()
    expect(screen.getByTestId('link-shipments')).toBeInTheDocument()
  })

  it('should render user menu with username', () => {
    // When
    renderWithRouter(
      <Layout>
        <div>Content</div>
      </Layout>
    )

    // Then
    expect(screen.getByTestId('user-menu-trigger')).toBeInTheDocument()
    expect(screen.getByText('testuser')).toBeInTheDocument()
  })

  it('should render admin links for admin users', () => {
    // Given
    mockUseAuthStore.mockReturnValue({
      isAuthenticated: true,
      user: { id: 1, username: 'admin', role: 'ADMIN' },
      logout: mockLogout
    })

    // When
    renderWithRouter(
      <Layout>
        <div>Content</div>
      </Layout>
    )

    // Then
    expect(screen.getByTestId('link-admin')).toBeInTheDocument()
    expect(screen.getByTestId('link-trucks')).toBeInTheDocument()
  })

  it('should not render admin links for regular users', () => {
    // Given
    mockUseAuthStore.mockReturnValue({
      isAuthenticated: true,
      user: { id: 1, username: 'user', role: 'USER' },
      logout: mockLogout
    })

    // When
    renderWithRouter(
      <Layout>
        <div>Content</div>
      </Layout>
    )

    // Then
    expect(screen.queryByTestId('link-admin')).not.toBeInTheDocument()
    expect(screen.queryByTestId('link-trucks')).not.toBeInTheDocument()
  })

  it('should render driver-specific links for driver users', () => {
    // Given
    mockUseAuthStore.mockReturnValue({
      isAuthenticated: true,
      user: { id: 1, username: 'driver', role: 'DRIVER' },
      logout: mockLogout
    })

    // When
    renderWithRouter(
      <Layout>
        <div>Content</div>
      </Layout>
    )

    // Then
    expect(screen.getByTestId('link-deliveries')).toBeInTheDocument()
    expect(screen.getByTestId('link-truck-status')).toBeInTheDocument()
  })

  it('should handle logout when logout button is clicked', () => {
    // When
    renderWithRouter(
      <Layout>
        <div>Content</div>
      </Layout>
    )

    // Click user menu to open dropdown
    fireEvent.click(screen.getByTestId('user-menu-trigger'))
    
    // Click logout button
    const logoutButtons = screen.getAllByTestId('menu-item')
    const logoutButton = logoutButtons.find(button => button.textContent?.includes('Logout'))
    
    if (logoutButton) {
      fireEvent.click(logoutButton)
    }

    // Then
    expect(mockLogout).toHaveBeenCalled()
  })

  it('should render mobile menu toggle button', () => {
    // When
    renderWithRouter(
      <Layout>
        <div>Content</div>
      </Layout>
    )

    // Then
    expect(screen.getByTestId('mobile-menu-toggle')).toBeInTheDocument()
  })

  it('should handle mobile menu toggle', () => {
    // When
    renderWithRouter(
      <Layout>
        <div>Content</div>
      </Layout>
    )

    const mobileToggle = screen.getByTestId('mobile-menu-toggle')
    fireEvent.click(mobileToggle)

    // Then
    expect(screen.getByTestId('mobile-menu')).toBeInTheDocument()
  })

  it('should render user profile link in user menu', () => {
    // When
    renderWithRouter(
      <Layout>
        <div>Content</div>
      </Layout>
    )

    // Click user menu to open dropdown
    fireEvent.click(screen.getByTestId('user-menu-trigger'))

    // Then
    const menuItems = screen.getAllByTestId('menu-item')
    const profileItem = menuItems.find(item => item.textContent?.includes('Profile'))
    expect(profileItem).toBeInTheDocument()
  })

  it('should render settings link in user menu', () => {
    // When
    renderWithRouter(
      <Layout>
        <div>Content</div>
      </Layout>
    )

    // Click user menu to open dropdown
    fireEvent.click(screen.getByTestId('user-menu-trigger'))

    // Then
    const menuItems = screen.getAllByTestId('menu-item')
    const settingsItem = menuItems.find(item => item.textContent?.includes('Settings'))
    expect(settingsItem).toBeInTheDocument()
  })

  it('should handle unauthenticated state gracefully', () => {
    // Given
    mockUseAuthStore.mockReturnValue({
      isAuthenticated: false,
      user: null,
      logout: mockLogout
    })

    // When
    renderWithRouter(
      <Layout>
        <div data-testid="page-content">Page Content</div>
      </Layout>
    )

    // Then
    expect(screen.getByTestId('page-content')).toBeInTheDocument()
    expect(screen.getByText('Mini-UPS')).toBeInTheDocument()
    expect(screen.queryByTestId('user-menu-trigger')).not.toBeInTheDocument()
  })

  it('should render login and register links for unauthenticated users', () => {
    // Given
    mockUseAuthStore.mockReturnValue({
      isAuthenticated: false,
      user: null,
      logout: mockLogout
    })

    // When
    renderWithRouter(
      <Layout>
        <div>Content</div>
      </Layout>
    )

    // Then
    expect(screen.getByTestId('link-login')).toBeInTheDocument()
    expect(screen.getByTestId('link-register')).toBeInTheDocument()
  })

  it('should apply correct styling classes', () => {
    // When
    const { container } = renderWithRouter(
      <Layout>
        <div>Content</div>
      </Layout>
    )

    // Then
    const layoutElement = container.firstChild
    expect(layoutElement).toHaveClass('min-h-screen', 'bg-gray-50')
  })

  it('should render footer', () => {
    // When
    renderWithRouter(
      <Layout>
        <div>Content</div>
      </Layout>
    )

    // Then
    expect(screen.getByTestId('footer')).toBeInTheDocument()
    expect(screen.getByText(/© 2024 Mini-UPS/)).toBeInTheDocument()
  })
})