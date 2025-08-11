/**
 * Admin Feature - Barrel Exports
 * 
 * Centralized export point for the admin feature module.
 * This provides a clean API for importing admin functionality
 * throughout the application.
 */

// Pages
export { default as AdminDashboardPage } from './pages/AdminDashboardPage';
export { AnalyticsPage } from './pages/AnalyticsPage';
export { default as FleetManagementPage } from './pages/FleetManagementPage';
export { UserManagementPage } from './pages/UserManagementPage';
export { ShipmentManagementPage } from './pages/ShipmentManagementPage';
export { WorldSimulatorDebugPage } from './pages/WorldSimulatorDebugPage';

// Components
export { AdvancedAnalytics } from './components/AdvancedAnalytics';
export { DriverSchedulingCalendar } from './components/DriverSchedulingCalendar';
export { EmergencyIssuesPanel } from './components/EmergencyIssuesPanel';
export { RealTimeFleetMap } from './components/RealTimeFleetMap';
export { RoleVisualization } from './components/RoleVisualization';
export { UserActivityLogs } from './components/UserActivityLogs';
export { VehicleLocationMap } from './components/VehicleLocationMap';

// Hooks
export { useWorldSimulatorDebug } from './hooks/useWorldSimulatorDebug';

// Services
export * from './services/admin';

// Types (if any admin-specific types are created)
// export * from './types';

// Utils (if any admin-specific utilities are created)
// export * from './utils';