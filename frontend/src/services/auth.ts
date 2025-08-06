import api from './api'

export interface LoginRequest {
  usernameOrEmail: string
  password: string
}

export interface RegisterRequest {
  username: string
  email: string
  password: string
  confirmPassword: string
  firstName?: string
  lastName?: string
}

export interface AuthResponse {
  access_token: string
  user: {
    id: number
    username: string
    email: string
    firstName: string
    lastName: string
    displayName: string
    fullName: string
    role: 'USER' | 'ADMIN'
    enabled: boolean
    admin: boolean
    driver: boolean
  }
}

export const authApi = {
  login: (data: LoginRequest): Promise<AuthResponse> => {
    return api.post('/auth/login', data).then(res => {
      console.log('Raw API response:', res.data)
      return res.data.data // Extract from ApiResponse wrapper
    })
  },

  register: (data: RegisterRequest): Promise<AuthResponse> => {
    return api.post('/auth/register', data).then(res => {
      console.log('Raw register response:', res.data)
      return res.data.data // Extract from ApiResponse wrapper
    })
  },

  logout: (): Promise<void> => {
    return api.post('/auth/logout').then(res => res.data)
  },

  getCurrentUser: (): Promise<AuthResponse['user']> => {
    return api.get('/auth/me').then(res => res.data)
  },

  changePassword: (data: { currentPassword: string; newPassword: string }): Promise<void> => {
    return api.put('/auth/change-password', data).then(res => res.data)
  }
}