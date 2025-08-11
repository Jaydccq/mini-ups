/**
 * API Configuration
 * 
 * Description:
 * - Centralized configuration for API endpoints
 * - Environment-based API base URL configuration
 * - Default API settings and timeouts
 * 
 */

// Determine API base URL based on environment
const getApiBaseUrl = (): string => {
  // In development, use the backend server directly
  if (import.meta.env.DEV) {
    return 'http://localhost:8081/api';
  }
  
  // In production, use relative path (assuming same domain)
  return '/api';
};

export const API_BASE_URL = getApiBaseUrl();

// Other API configuration
export const API_CONFIG = {
  timeout: 30000, // 30 seconds
  retries: 3,
  retryDelay: 1000, // 1 second
};

// API response types
export interface ApiResponse<T> {
  success: boolean;
  message: string;
  data: T;
  timestamp?: string;
}

export interface ApiError {
  success: false;
  message: string;
  error?: string;
  timestamp?: string;
}