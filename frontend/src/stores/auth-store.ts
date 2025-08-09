import { create } from 'zustand'
import { persist } from 'zustand/middleware'
import { getWebSocketService } from '@/services/websocket'

interface User {
  id: string
  email: string
  name: string
  role: 'USER' | 'ADMIN'
}

interface AuthState {
  user: User | null
  token: string | null
  isAuthenticated: boolean
  status: 'idle' | 'loading' | 'authenticated' | 'unauthenticated'
  
  // Actions
  login: (user: User, token: string) => void
  logout: () => void
  setLoading: (loading: boolean) => void
  updateUser: (user: Partial<User>) => void
}

export const useAuthStore = create<AuthState>()(
  persist(
    (set, get) => ({
      user: null,
      token: null,
      isAuthenticated: false,
      status: 'idle',

      login: (user, token) => {
        set({
          user,
          token,
          isAuthenticated: true,
          status: 'authenticated'
        })
        // Connect WebSocket after successful login
        try {
          const wsService = getWebSocketService()
          wsService.connect().catch(console.error)
        } catch (error) {
          console.error('Failed to connect WebSocket:', error)
        }
      },

      logout: () => {
        set({
          user: null,
          token: null,
          isAuthenticated: false,
          status: 'unauthenticated'
        })
        // Disconnect WebSocket on logout
        try {
          const wsService = getWebSocketService()
          wsService.disconnect()
        } catch (error) {
          console.error('Failed to disconnect WebSocket:', error)
        }
      },

      setLoading: (loading) => {
        set({
          status: loading ? 'loading' : get().isAuthenticated ? 'authenticated' : 'unauthenticated'
        })
      },

      updateUser: (userData) => {
        const currentUser = get().user
        if (currentUser) {
          set({
            user: { ...currentUser, ...userData }
          })
        }
      }
    }),
    {
      name: 'auth-storage',
      partialize: (state) => ({ 
        user: state.user, 
        token: state.token, 
        isAuthenticated: state.isAuthenticated 
      })
    }
  )
)