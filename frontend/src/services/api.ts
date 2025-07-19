import axios from 'axios'
import { toast } from 'sonner'
import { useAuthStore } from '@/stores/auth-store'

const API_BASE_URL = (import.meta as any).env.VITE_API_URL || 'http://localhost:8081/api'

// Create axios instance
export const api = axios.create({
  baseURL: API_BASE_URL,
  timeout: 10000,
  headers: {
    'Content-Type': 'application/json',
  },
})

// Request interceptor - add auth token
api.interceptors.request.use(
  (config) => {
    const token = useAuthStore.getState().token
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  },
  (error) => {
    return Promise.reject(error)
  }
)

// Response interceptor - handle errors
api.interceptors.response.use(
  (response) => response,
  (error) => {
    const { response } = error
    
    if (response?.status === 401) {
      // Unauthorized, clear auth status
      useAuthStore.getState().logout()
      toast.error('Login has expired, please log in again')
      window.location.href = '/login'
    }
    
    if (response?.status === 403) {
      // Forbidden
      toast.error('You do not have permission to perform this action')
    }
    
    if (response?.status >= 500) {
      // Server error
      toast.error('The server is busy, please try again later')
    }
    
    // Network error
    if (!response) {
      toast.error('Network connection failed, please check your network connection')
    }
    
    return Promise.reject(error)
  }
)

export default api

// For backward compatibility
export const apiClient = api