import api from './api'

export interface LoginRequest {
  email: string
  password: string
}

export interface RegisterRequest {
  name: string
  email: string
  password: string
  confirmPassword: string
}

export interface AuthResponse {
  token: string
  user: {
    id: string
    email: string
    name: string
    role: 'USER' | 'ADMIN'
  }
}

export const authApi = {
  login: (data: LoginRequest): Promise<AuthResponse> => {
    return api.post('/auth/login', data).then(res => res.data)
  },

  register: (data: RegisterRequest): Promise<AuthResponse> => {
    return api.post('/auth/register', data).then(res => res.data)
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