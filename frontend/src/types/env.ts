/**
 * Environment Variables Type Definitions
 * 
 * Provides type safety and validation for environment variables.
 */

export interface EnvironmentConfig {
  // API Configuration
  VITE_API_BASE_URL: string;
  VITE_WS_BASE_URL: string;
  
  // App Configuration
  VITE_APP_NAME: string;
  VITE_APP_VERSION: string;
  VITE_APP_DESCRIPTION: string;
  
  // Feature Flags
  VITE_ENABLE_NOTIFICATIONS: boolean;
  VITE_ENABLE_REAL_TIME: boolean;
  VITE_ENABLE_ANALYTICS: boolean;
  VITE_ENABLE_PWA: boolean;
  
  // External Services
  VITE_ANALYTICS_ID?: string;
  VITE_SENTRY_DSN?: string;
  VITE_GOOGLE_MAPS_API_KEY?: string;
  VITE_MAPBOX_ACCESS_TOKEN?: string;
  
  // Security & Auth
  VITE_JWT_SECRET_KEY?: string;
  VITE_SESSION_TIMEOUT: number;
  VITE_ENABLE_HTTPS: boolean;
  
  // Performance & Debugging
  VITE_LOG_LEVEL: 'debug' | 'info' | 'warn' | 'error';
  VITE_ENABLE_SOURCE_MAPS: boolean;
  VITE_ENABLE_BUNDLE_ANALYZER: boolean;
  
  // CDN & Assets
  VITE_CDN_URL?: string;
  VITE_STATIC_ASSETS_URL?: string;
  
  // Notification Services
  VITE_PUSHER_APP_KEY?: string;
  VITE_PUSHER_CLUSTER?: string;
  VITE_FIREBASE_CONFIG?: string;
  
  // Third-party Integrations
  VITE_STRIPE_PUBLISHABLE_KEY?: string;
  VITE_PAYPAL_CLIENT_ID?: string;
}

// Extend Vite's ImportMetaEnv interface
declare global {
  interface ImportMetaEnv extends EnvironmentConfig {
    readonly MODE: string;
    readonly BASE_URL: string;
    readonly PROD: boolean;
    readonly DEV: boolean;
    readonly SSR: boolean;
  }

  interface ImportMeta {
    readonly env: ImportMetaEnv;
  }
}

export {};