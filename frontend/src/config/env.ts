/**
 * Environment Configuration and Validation
 * 
 * Centralized environment variable management with validation and type safety.
 */

import { EnvironmentConfig } from '@/types/env';

// Environment validation schema
const requiredEnvVars = [
  'VITE_API_BASE_URL',
  'VITE_WS_BASE_URL',
  'VITE_APP_NAME',
  'VITE_APP_VERSION',
] as const;

const booleanEnvVars = [
  'VITE_ENABLE_NOTIFICATIONS',
  'VITE_ENABLE_REAL_TIME',
  'VITE_ENABLE_ANALYTICS',
  'VITE_ENABLE_PWA',
  'VITE_ENABLE_HTTPS',
  'VITE_ENABLE_SOURCE_MAPS',
  'VITE_ENABLE_BUNDLE_ANALYZER',
] as const;

const numberEnvVars = [
  'VITE_SESSION_TIMEOUT',
] as const;

/**
 * Parse boolean environment variable
 */
const parseBoolean = (value: string | undefined): boolean => {
  if (!value) return false;
  return value.toLowerCase() === 'true' || value === '1';
};

/**
 * Parse number environment variable
 */
const parseNumber = (value: string | undefined, defaultValue: number): number => {
  if (!value) return defaultValue;
  const parsed = parseInt(value, 10);
  return isNaN(parsed) ? defaultValue : parsed;
};

/**
 * Validate required environment variables
 */
const validateRequiredEnvVars = (): void => {
  const missing: string[] = [];
  
  requiredEnvVars.forEach((varName) => {
    const value = import.meta.env[varName];
    if (!value || value.trim() === '') {
      missing.push(varName);
    }
  });
  
  if (missing.length > 0) {
    const error = `Missing required environment variables: ${missing.join(', ')}`;
    console.error('❌ Environment Configuration Error:', error);
    
    if (import.meta.env.PROD) {
      throw new Error(error);
    } else {
      console.warn('⚠️ Development mode: continuing with missing variables');
    }
  }
};

/**
 * Validate environment variable format
 */
const validateEnvFormat = (): void => {
  const errors: string[] = [];
  
  // Validate API URLs
  const apiUrl = import.meta.env.VITE_API_BASE_URL;
  if (apiUrl && !apiUrl.match(/^https?:\/\/.+/)) {
    errors.push('VITE_API_BASE_URL must be a valid HTTP/HTTPS URL');
  }
  
  const wsUrl = import.meta.env.VITE_WS_BASE_URL;
  if (wsUrl && !wsUrl.match(/^wss?:\/\/.+/)) {
    errors.push('VITE_WS_BASE_URL must be a valid WS/WSS URL');
  }
  
  // Validate log level
  const logLevel = import.meta.env.VITE_LOG_LEVEL;
  if (logLevel && !['debug', 'info', 'warn', 'error'].includes(logLevel)) {
    errors.push('VITE_LOG_LEVEL must be one of: debug, info, warn, error');
  }
  
  if (errors.length > 0) {
    const error = `Invalid environment variable format: ${errors.join('; ')}`;
    console.error('❌ Environment Validation Error:', error);
    
    if (import.meta.env.PROD) {
      throw new Error(error);
    } else {
      console.warn('⚠️ Development mode: continuing with invalid format');
    }
  }
};

/**
 * Create validated environment configuration
 */
const createEnvConfig = (): EnvironmentConfig => {
  // Validate environment variables
  validateRequiredEnvVars();
  validateEnvFormat();
  
  return {
    // API Configuration
    VITE_API_BASE_URL: import.meta.env.VITE_API_BASE_URL || 'http://localhost:8081',
    VITE_WS_BASE_URL: import.meta.env.VITE_WS_BASE_URL || 'ws://localhost:8081',
    
    // App Configuration
    VITE_APP_NAME: import.meta.env.VITE_APP_NAME || 'Mini-UPS',
    VITE_APP_VERSION: import.meta.env.VITE_APP_VERSION || '1.0.0',
    VITE_APP_DESCRIPTION: import.meta.env.VITE_APP_DESCRIPTION || 'Mini-UPS Logistics Management System',
    
    // Feature Flags
    VITE_ENABLE_NOTIFICATIONS: parseBoolean(import.meta.env.VITE_ENABLE_NOTIFICATIONS),
    VITE_ENABLE_REAL_TIME: parseBoolean(import.meta.env.VITE_ENABLE_REAL_TIME),
    VITE_ENABLE_ANALYTICS: parseBoolean(import.meta.env.VITE_ENABLE_ANALYTICS),
    VITE_ENABLE_PWA: parseBoolean(import.meta.env.VITE_ENABLE_PWA),
    
    // External Services
    VITE_ANALYTICS_ID: import.meta.env.VITE_ANALYTICS_ID,
    VITE_SENTRY_DSN: import.meta.env.VITE_SENTRY_DSN,
    VITE_GOOGLE_MAPS_API_KEY: import.meta.env.VITE_GOOGLE_MAPS_API_KEY,
    VITE_MAPBOX_ACCESS_TOKEN: import.meta.env.VITE_MAPBOX_ACCESS_TOKEN,
    
    // Security & Auth
    VITE_JWT_SECRET_KEY: import.meta.env.VITE_JWT_SECRET_KEY,
    VITE_SESSION_TIMEOUT: parseNumber(import.meta.env.VITE_SESSION_TIMEOUT, 3600000), // 1 hour default
    VITE_ENABLE_HTTPS: parseBoolean(import.meta.env.VITE_ENABLE_HTTPS),
    
    // Performance & Debugging
    VITE_LOG_LEVEL: (import.meta.env.VITE_LOG_LEVEL as any) || 'info',
    VITE_ENABLE_SOURCE_MAPS: parseBoolean(import.meta.env.VITE_ENABLE_SOURCE_MAPS),
    VITE_ENABLE_BUNDLE_ANALYZER: parseBoolean(import.meta.env.VITE_ENABLE_BUNDLE_ANALYZER),
    
    // CDN & Assets
    VITE_CDN_URL: import.meta.env.VITE_CDN_URL,
    VITE_STATIC_ASSETS_URL: import.meta.env.VITE_STATIC_ASSETS_URL,
    
    // Notification Services
    VITE_PUSHER_APP_KEY: import.meta.env.VITE_PUSHER_APP_KEY,
    VITE_PUSHER_CLUSTER: import.meta.env.VITE_PUSHER_CLUSTER,
    VITE_FIREBASE_CONFIG: import.meta.env.VITE_FIREBASE_CONFIG,
    
    // Third-party Integrations
    VITE_STRIPE_PUBLISHABLE_KEY: import.meta.env.VITE_STRIPE_PUBLISHABLE_KEY,
    VITE_PAYPAL_CLIENT_ID: import.meta.env.VITE_PAYPAL_CLIENT_ID,
  };
};

// Export validated configuration
export const env = createEnvConfig();

// Environment utilities
export const isProduction = import.meta.env.PROD;
export const isDevelopment = import.meta.env.DEV;
export const isStaging = import.meta.env.MODE === 'staging';

// Feature flags utilities
export const features = {
  notifications: env.VITE_ENABLE_NOTIFICATIONS,
  realTime: env.VITE_ENABLE_REAL_TIME,
  analytics: env.VITE_ENABLE_ANALYTICS,
  pwa: env.VITE_ENABLE_PWA,
} as const;

// Debug environment configuration in development
if (isDevelopment) {
  console.group('🔧 Environment Configuration');
  console.log('Mode:', import.meta.env.MODE);
  console.log('API Base URL:', env.VITE_API_BASE_URL);
  console.log('WebSocket URL:', env.VITE_WS_BASE_URL);
  console.log('Features:', features);
  console.log('App Info:', {
    name: env.VITE_APP_NAME,
    version: env.VITE_APP_VERSION,
    description: env.VITE_APP_DESCRIPTION,
  });
  console.groupEnd();
}

// Environment health check
export const envHealthCheck = (): boolean => {
  try {
    validateRequiredEnvVars();
    validateEnvFormat();
    return true;
  } catch (error) {
    console.error('Environment health check failed:', error);
    return false;
  }
};