import { create } from 'zustand'
import { persist } from 'zustand/middleware'
import { socketService } from '@/services/socketService'

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
        socketService.connect(token)
      },

      logout: () => {
        set({
          user: null,
          token: null,
          isAuthenticated: false,
          status: 'unauthenticated'
        })
        // Disconnect WebSocket on logout
        socketService.disconnect()
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