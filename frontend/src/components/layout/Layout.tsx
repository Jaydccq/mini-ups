import React, { useState } from 'react';
import { Link } from 'react-router-dom';
import { Menu, X, User, Settings, LogOut } from 'lucide-react';
import { useAuthStore } from '@/stores/auth-store';
import { useNavigation } from '@/hooks/use-navigation';
import { Button } from '@/components/ui/button';
import * as NavigationMenu from '@radix-ui/react-navigation-menu';
import * as DropdownMenu from '@radix-ui/react-dropdown-menu';

interface LayoutProps {
  children: React.ReactNode;
}

export const Layout: React.FC<LayoutProps> = ({ children }) => {
  const { isAuthenticated, user, logout } = useAuthStore();
  const { visibleNavigation } = useNavigation();
  const [mobileMenuOpen, setMobileMenuOpen] = useState(false);

  const handleLogout = () => {
    logout();
  };

  return (
    <div className="min-h-screen bg-gray-50">
      {/* Header */}
      <header className="bg-white shadow-sm border-b">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
          <div className="flex justify-between items-center h-16">
            {/* Logo */}
            <div className="flex items-center">
              <Link to="/" className="text-xl font-bold text-primary">
                Mini-UPS
              </Link>
            </div>

            {/* Desktop Navigation */}
            {isAuthenticated && (
              <NavigationMenu.Root className="hidden md:flex" data-testid="navigation">
                <NavigationMenu.List className="flex space-x-6">
                  {visibleNavigation.map((item) => (
                    <NavigationMenu.Item key={item.path}>
                      <Link
                        to={item.path}
                        data-testid={`link-${item.path.replace('/', '')}`}
                        className="text-gray-700 hover:text-primary px-3 py-2 rounded-md text-sm font-medium"
                      >
                        <item.icon className="w-4 h-4 inline mr-2" />
                        {item.label}
                      </Link>
                    </NavigationMenu.Item>
                  ))}
                </NavigationMenu.List>
              </NavigationMenu.Root>
            )}

            {/* Right side - User menu or Auth links */}
            <div className="flex items-center space-x-4">
              {isAuthenticated ? (
                <>
                  {/* User Menu */}
                  <DropdownMenu.Root>
                    <DropdownMenu.Trigger asChild>
                      <Button variant="ghost" data-testid="user-menu-trigger">
                        <User className="w-4 h-4 mr-2" />
                        {user?.name || user?.email}
                      </Button>
                    </DropdownMenu.Trigger>
                    <DropdownMenu.Content
                      className="bg-white rounded-md shadow-lg border min-w-[200px] p-1"
                      data-testid="user-menu-content"
                    >
                      <DropdownMenu.Item asChild>
                        <Link
                          to="/profile"
                          className="flex items-center px-3 py-2 text-sm text-gray-700 hover:bg-gray-100 rounded-md"
                          data-testid="menu-item"
                        >
                          <User className="w-4 h-4 mr-2" />
                          Profile
                        </Link>
                      </DropdownMenu.Item>
                      <DropdownMenu.Item asChild>
                        <Link
                          to="/settings"
                          className="flex items-center px-3 py-2 text-sm text-gray-700 hover:bg-gray-100 rounded-md"
                          data-testid="menu-item"
                        >
                          <Settings className="w-4 h-4 mr-2" />
                          Settings
                        </Link>
                      </DropdownMenu.Item>
                      <DropdownMenu.Separator className="my-1 h-px bg-gray-200" data-testid="menu-separator" />
                      <DropdownMenu.Item asChild>
                        <button
                          onClick={handleLogout}
                          className="flex items-center w-full px-3 py-2 text-sm text-gray-700 hover:bg-gray-100 rounded-md"
                          data-testid="menu-item"
                        >
                          <LogOut className="w-4 h-4 mr-2" />
                          Logout
                        </button>
                      </DropdownMenu.Item>
                    </DropdownMenu.Content>
                  </DropdownMenu.Root>
                </>
              ) : (
                <>
                  <Link
                    to="/login"
                    className="text-gray-700 hover:text-primary px-3 py-2 rounded-md text-sm font-medium"
                    data-testid="link-login"
                  >
                    Login
                  </Link>
                  <Link
                    to="/register"
                    className="bg-primary text-white px-4 py-2 rounded-md text-sm font-medium hover:bg-primary/90"
                    data-testid="link-register"
                  >
                    Register
                  </Link>
                </>
              )}

              {/* Mobile menu button */}
              <Button
                variant="ghost"
                size="icon"
                className="md:hidden"
                onClick={() => setMobileMenuOpen(!mobileMenuOpen)}
                data-testid="mobile-menu-toggle"
              >
                {mobileMenuOpen ? <X className="w-5 h-5" /> : <Menu className="w-5 h-5" />}
              </Button>
            </div>
          </div>
        </div>

        {/* Mobile Navigation */}
        {mobileMenuOpen && isAuthenticated && (
          <div className="md:hidden bg-white border-t" data-testid="mobile-menu">
            <div className="px-2 pt-2 pb-3 space-y-1">
              {visibleNavigation.map((item) => (
                <Link
                  key={item.path}
                  to={item.path}
                  className="block px-3 py-2 text-base font-medium text-gray-700 hover:text-primary hover:bg-gray-50 rounded-md"
                  onClick={() => setMobileMenuOpen(false)}
                >
                  <item.icon className="w-4 h-4 inline mr-2" />
                  {item.label}
                </Link>
              ))}
            </div>
          </div>
        )}
      </header>

      {/* Main Content */}
      <main className="flex-1">
        {children}
      </main>

      {/* Footer */}
      <footer className="bg-white border-t mt-auto" data-testid="footer">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
          <div className="py-8 text-center text-gray-500 text-sm">
            © 2024 Mini-UPS. All rights reserved.
          </div>
        </div>
      </footer>
    </div>
  );
};