/**
 * Error Boundary Components
 * 
 * Three-tiered error boundary system for graceful error handling:
 * 1. Global Boundary - Last resort fallback
 * 2. Route Boundary - Feature-level error containment  
 * 3. Widget Boundary - Component-level error isolation
 */

import React, { Component, ReactNode } from 'react';
import { AlertTriangle, RefreshCw, Home, Mail, Bug } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
import { Alert, AlertDescription } from '@/components/ui/alert';

// Error types for different boundary levels
export type ErrorBoundaryLevel = 'global' | 'route' | 'widget';

export interface ErrorInfo {
  componentStack: string;
  errorBoundary?: string;
  errorBoundaryStack?: string;
}

export interface ErrorBoundaryState {
  hasError: boolean;
  error: Error | null;
  errorInfo: ErrorInfo | null;
  errorId: string | null;
}

export interface ErrorBoundaryProps {
  level: ErrorBoundaryLevel;
  fallbackComponent?: ReactNode;
  onError?: (error: Error, errorInfo: ErrorInfo) => void;
  children: ReactNode;
  
  // Route-specific props
  routeName?: string;
  
  // Widget-specific props
  widgetName?: string;
  essential?: boolean; // If true, shows more prominent error UI
}

// Base Error Boundary Class
class BaseErrorBoundary extends Component<ErrorBoundaryProps, ErrorBoundaryState> {
  constructor(props: ErrorBoundaryProps) {
    super(props);
    this.state = {
      hasError: false,
      error: null,
      errorInfo: null,
      errorId: null,
    };
  }

  static getDerivedStateFromError(error: Error): Partial<ErrorBoundaryState> {
    return {
      hasError: true,
      error,
      errorId: `error_${Date.now()}_${Math.random().toString(36).substr(2, 9)}`,
    };
  }

  componentDidCatch(error: Error, errorInfo: ErrorInfo) {
    this.setState({
      error,
      errorInfo,
    });

    // Log error to monitoring service
    this.logError(error, errorInfo);

    // Call custom error handler
    if (this.props.onError) {
      this.props.onError(error, errorInfo);
    }
  }

  private logError(error: Error, errorInfo: ErrorInfo) {
    const errorReport = {
      level: this.props.level,
      routeName: this.props.routeName,
      widgetName: this.props.widgetName,
      error: {
        name: error.name,
        message: error.message,
        stack: error.stack,
      },
      errorInfo,
      errorId: this.state.errorId,
      timestamp: new Date().toISOString(),
      userAgent: navigator.userAgent,
      url: window.location.href,
    };

    // Log to console in development
    if (process.env.NODE_ENV === 'development') {
      console.group(`🚨 Error Boundary (${this.props.level})`);
      console.error('Error:', error);
      console.error('Error Info:', errorInfo);
      console.error('Full Report:', errorReport);
      console.groupEnd();
    }

    // In production, send to monitoring service
    // Example: Sentry, DataDog, etc.
    // sentry.captureException(error, { extra: errorReport });
  }

  private handleRetry = () => {
    this.setState({
      hasError: false,
      error: null,
      errorInfo: null,
      errorId: null,
    });
  };

  private handleReload = () => {
    window.location.reload();
  };

  private handleGoHome = () => {
    window.location.href = '/';
  };

  private getErrorReport = () => {
    const { error, errorInfo, errorId } = this.state;
    const report = {
      errorId,
      error: error?.message,
      stack: error?.stack,
      componentStack: errorInfo?.componentStack,
      timestamp: new Date().toISOString(),
    };
    return encodeURIComponent(JSON.stringify(report, null, 2));
  };

  render() {
    if (this.state.hasError) {
      // Use custom fallback if provided
      if (this.props.fallbackComponent) {
        return this.props.fallbackComponent;
      }

      // Render appropriate error UI based on boundary level
      switch (this.props.level) {
        case 'global':
          return this.renderGlobalError();
        case 'route':
          return this.renderRouteError();
        case 'widget':
          return this.renderWidgetError();
        default:
          return this.renderGlobalError();
      }
    }

    return this.props.children;
  }

  private renderGlobalError() {
    return (
      <div className="min-h-screen flex items-center justify-center bg-gray-50 px-4">
        <Card className="max-w-lg w-full">
          <CardHeader className="text-center">
            <div className="mx-auto w-12 h-12 bg-red-100 rounded-full flex items-center justify-center mb-4">
              <AlertTriangle className="h-6 w-6 text-red-600" />
            </div>
            <CardTitle className="text-xl">Application Error</CardTitle>
            <CardDescription>
              An unexpected error occurred. We apologize for the inconvenience.
            </CardDescription>
          </CardHeader>
          <CardContent className="space-y-4">
            <Alert variant="destructive">
              <Bug className="h-4 w-4" />
              <AlertDescription>
                <strong>Error ID:</strong> {this.state.errorId}
                <br />
                <strong>Error:</strong> {this.state.error?.message}
              </AlertDescription>
            </Alert>

            <div className="flex flex-col gap-3">
              <Button onClick={this.handleReload} className="w-full">
                <RefreshCw className="h-4 w-4 mr-2" />
                Reload Application
              </Button>
              
              <Button variant="outline" onClick={this.handleGoHome} className="w-full">
                <Home className="h-4 w-4 mr-2" />
                Go to Home Page
              </Button>
              
              <Button 
                variant="outline" 
                onClick={() => {
                  const report = this.getErrorReport();
                  window.open(`mailto:support@miniups.com?subject=Application Error Report&body=Error Report: ${report}`);
                }}
                className="w-full"
              >
                <Mail className="h-4 w-4 mr-2" />
                Contact Support
              </Button>
            </div>

            {process.env.NODE_ENV === 'development' && (
              <details className="mt-4">
                <summary className="cursor-pointer text-sm font-medium text-gray-700">
                  Developer Details
                </summary>
                <pre className="mt-2 p-3 bg-gray-100 rounded text-xs overflow-auto max-h-40">
                  {this.state.error?.stack}
                </pre>
                {this.state.errorInfo && (
                  <pre className="mt-2 p-3 bg-gray-100 rounded text-xs overflow-auto max-h-40">
                    {this.state.errorInfo.componentStack}
                  </pre>
                )}
              </details>
            )}
          </CardContent>
        </Card>
      </div>
    );
  }

  private renderRouteError() {
    const routeName = this.props.routeName || 'page';
    
    return (
      <div className="max-w-2xl mx-auto p-6">
        <Card>
          <CardHeader className="text-center">
            <div className="mx-auto w-10 h-10 bg-orange-100 rounded-full flex items-center justify-center mb-3">
              <AlertTriangle className="h-5 w-5 text-orange-600" />
            </div>
            <CardTitle>Unable to Load {routeName}</CardTitle>
            <CardDescription>
              This page encountered an error and couldn't be displayed.
            </CardDescription>
          </CardHeader>
          <CardContent className="space-y-4">
            <Alert>
              <AlertDescription>
                <strong>What happened?</strong><br />
                The {routeName} failed to load due to an unexpected error. 
                You can try refreshing this page or navigate to another section.
              </AlertDescription>
            </Alert>

            <div className="flex gap-3 justify-center">
              <Button onClick={this.handleRetry}>
                <RefreshCw className="h-4 w-4 mr-2" />
                Try Again
              </Button>
              
              <Button variant="outline" onClick={this.handleGoHome}>
                <Home className="h-4 w-4 mr-2" />
                Go Home
              </Button>
            </div>

            {process.env.NODE_ENV === 'development' && (
              <details className="mt-4">
                <summary className="cursor-pointer text-sm font-medium text-gray-700">
                  Error Details
                </summary>
                <pre className="mt-2 p-3 bg-gray-100 rounded text-xs overflow-auto max-h-32">
                  {this.state.error?.message}
                </pre>
              </details>
            )}
          </CardContent>
        </Card>
      </div>
    );
  }

  private renderWidgetError() {
    const widgetName = this.props.widgetName || 'component';
    const isEssential = this.props.essential;
    
    if (!isEssential) {
      // Non-essential widgets get minimal error UI
      return (
        <div className="p-4 border border-red-200 bg-red-50 rounded-lg">
          <div className="flex items-center gap-2 text-sm text-red-800">
            <AlertTriangle className="h-4 w-4 flex-shrink-0" />
            <span>
              {widgetName} could not be loaded.
            </span>
            <Button 
              variant="ghost" 
              size="sm" 
              onClick={this.handleRetry}
              className="ml-auto h-6 px-2 text-xs text-red-700 hover:text-red-900"
            >
              <RefreshCw className="h-3 w-3 mr-1" />
              Retry
            </Button>
          </div>
        </div>
      );
    }

    // Essential widgets get more prominent error UI
    return (
      <Card className="border-orange-200 bg-orange-50">
        <CardContent className="p-4">
          <div className="flex items-start gap-3">
            <AlertTriangle className="h-5 w-5 text-orange-600 flex-shrink-0 mt-0.5" />
            <div className="flex-1">
              <h4 className="font-medium text-orange-900">
                {widgetName} Error
              </h4>
              <p className="text-sm text-orange-700 mt-1">
                This component encountered an error and couldn't be displayed.
              </p>
              <div className="flex gap-2 mt-3">
                <Button 
                  size="sm" 
                  variant="outline"
                  onClick={this.handleRetry}
                  className="border-orange-300 text-orange-700 hover:bg-orange-100"
                >
                  <RefreshCw className="h-3 w-3 mr-1" />
                  Retry
                </Button>
              </div>
            </div>
          </div>
        </CardContent>
      </Card>
    );
  }
}

// Specific Error Boundary Components
export const GlobalErrorBoundary: React.FC<Omit<ErrorBoundaryProps, 'level'>> = (props) => (
  <BaseErrorBoundary {...props} level="global" />
);

export const RouteErrorBoundary: React.FC<Omit<ErrorBoundaryProps, 'level'>> = (props) => (
  <BaseErrorBoundary {...props} level="route" />
);

export const WidgetErrorBoundary: React.FC<Omit<ErrorBoundaryProps, 'level'>> = (props) => (
  <BaseErrorBoundary {...props} level="widget" />
);

// Hook for programmatic error reporting
export const useErrorReporting = () => {
  const reportError = (error: Error, context?: Record<string, any>) => {
    const errorReport = {
      error: {
        name: error.name,
        message: error.message,
        stack: error.stack,
      },
      context,
      timestamp: new Date().toISOString(),
      url: window.location.href,
    };

    // Log to console in development
    if (process.env.NODE_ENV === 'development') {
      console.error('Manual Error Report:', errorReport);
    }

    // Send to monitoring service in production
    // sentry.captureException(error, { extra: errorReport });
  };

  return { reportError };
};