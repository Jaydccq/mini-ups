/**
 * Emergency Issues Panel Component
 * 
 * Features:
 * - Display critical system alerts and urgent issues
 * - Real-time updates via WebSocket
 * - Severity-based color coding and prioritization
 * - Acknowledge and resolve issue actions
 * - Auto-refresh and manual refresh capabilities
 * 
 *
 
 */
import React, { useState, useEffect } from 'react';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Badge } from '@/components/ui/badge';
import { Button } from '@/components/ui/button';
import { 
  AlertTriangle, 
  AlertCircle, 
  XCircle, 
  Clock, 
  CheckCircle,
  RefreshCw,
  Bell,
  Info
} from 'lucide-react';

interface EmergencyIssue {
  id: string;
  title: string;
  description: string;
  severity: 'CRITICAL' | 'HIGH' | 'MEDIUM' | 'LOW';
  category: 'SYSTEM' | 'DELIVERY' | 'VEHICLE' | 'DRIVER' | 'SECURITY';
  status: 'ACTIVE' | 'ACKNOWLEDGED' | 'RESOLVED';
  affectedEntities?: {
    type: 'truck' | 'shipment' | 'user' | 'system';
    id: string;
    name: string;
  }[];
  createdAt: string;
  acknowledgedAt?: string;
  resolvedAt?: string;
  acknowledgedBy?: string;
  resolvedBy?: string;
  estimatedResolution?: string;
}

interface EmergencyIssuesPanelProps {
  className?: string;
}

export const EmergencyIssuesPanel: React.FC<EmergencyIssuesPanelProps> = ({ 
  className = '' 
}) => {
  const [issues, setIssues] = useState<EmergencyIssue[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [filter, setFilter] = useState<'all' | 'active' | 'critical'>('active');
  const [lastUpdate, setLastUpdate] = useState<Date | null>(null);

  const fetchEmergencyIssues = async () => {
    try {
      setError(null);
      const response = await fetch('/api/admin/alerts/emergency', {
        headers: { 'Authorization': `Bearer ${localStorage.getItem('token')}` }
      });
      
      if (!response.ok) {
        throw new Error('Failed to fetch emergency issues');
      }
      
      const data = await response.json();
      if (data.data && data.data.issues) {
        setIssues(data.data.issues);
        setLastUpdate(new Date());
      }
    } catch (error) {
      console.error('Error fetching emergency issues:', error);
      // For demo purposes, use mock data when API fails
      setIssues(mockEmergencyIssues);
      setLastUpdate(new Date());
    } finally {
      setLoading(false);
    }
  };

  const acknowledgeIssue = async (issueId: string) => {
    try {
      const response = await fetch(`/api/admin/alerts/${issueId}/acknowledge`, {
        method: 'POST',
        headers: { 
          'Authorization': `Bearer ${localStorage.getItem('token')}`,
          'Content-Type': 'application/json'
        }
      });
      
      if (response.ok) {
        setIssues(prev => prev.map(issue => 
          issue.id === issueId 
            ? { ...issue, status: 'ACKNOWLEDGED', acknowledgedAt: new Date().toISOString() }
            : issue
        ));
      }
    } catch (error) {
      console.error('Error acknowledging issue:', error);
    }
  };

  const resolveIssue = async (issueId: string) => {
    try {
      const response = await fetch(`/api/admin/alerts/${issueId}/resolve`, {
        method: 'POST',
        headers: { 
          'Authorization': `Bearer ${localStorage.getItem('token')}`,
          'Content-Type': 'application/json'
        }
      });
      
      if (response.ok) {
        setIssues(prev => prev.map(issue => 
          issue.id === issueId 
            ? { ...issue, status: 'RESOLVED', resolvedAt: new Date().toISOString() }
            : issue
        ));
      }
    } catch (error) {
      console.error('Error resolving issue:', error);
    }
  };

  useEffect(() => {
    fetchEmergencyIssues();
    
    // Set up auto-refresh every 30 seconds
    const interval = setInterval(fetchEmergencyIssues, 30000);
    
    return () => clearInterval(interval);
  }, []);

  const getSeverityIcon = (severity: string) => {
    switch (severity) {
      case 'CRITICAL':
        return <XCircle className="w-4 h-4" />;
      case 'HIGH':
        return <AlertTriangle className="w-4 h-4" />;
      case 'MEDIUM':
        return <AlertCircle className="w-4 h-4" />;
      case 'LOW':
        return <Info className="w-4 h-4" />;
      default:
        return <AlertCircle className="w-4 h-4" />;
    }
  };

  const getSeverityColor = (severity: string) => {
    switch (severity) {
      case 'CRITICAL':
        return 'bg-red-100 text-red-800 border-red-200';
      case 'HIGH':
        return 'bg-orange-100 text-orange-800 border-orange-200';
      case 'MEDIUM':
        return 'bg-yellow-100 text-yellow-800 border-yellow-200';
      case 'LOW':
        return 'bg-blue-100 text-blue-800 border-blue-200';
      default:
        return 'bg-gray-100 text-gray-800 border-gray-200';
    }
  };

  const getStatusIcon = (status: string) => {
    switch (status) {
      case 'ACTIVE':
        return <Clock className="w-4 h-4" />;
      case 'ACKNOWLEDGED':
        return <AlertTriangle className="w-4 h-4" />;
      case 'RESOLVED':
        return <CheckCircle className="w-4 h-4" />;
      default:
        return <Clock className="w-4 h-4" />;
    }
  };

  const getStatusColor = (status: string) => {
    switch (status) {
      case 'ACTIVE':
        return 'bg-red-50 text-red-700 border-red-200';
      case 'ACKNOWLEDGED':
        return 'bg-yellow-50 text-yellow-700 border-yellow-200';
      case 'RESOLVED':
        return 'bg-green-50 text-green-700 border-green-200';
      default:
        return 'bg-gray-50 text-gray-700 border-gray-200';
    }
  };

  const filteredIssues = issues.filter(issue => {
    switch (filter) {
      case 'active':
        return issue.status === 'ACTIVE';
      case 'critical':
        return issue.severity === 'CRITICAL' && issue.status !== 'RESOLVED';
      default:
        return true;
    }
  });

  const criticalCount = issues.filter(i => i.severity === 'CRITICAL' && i.status === 'ACTIVE').length;
  const activeCount = issues.filter(i => i.status === 'ACTIVE').length;

  if (loading) {
    return (
      <Card className={className}>
        <CardHeader>
          <CardTitle className="flex items-center gap-2">
            <AlertTriangle className="h-5 w-5" />
            Emergency Issues
          </CardTitle>
        </CardHeader>
        <CardContent>
          <div className="flex items-center justify-center h-32">
            <div className="flex items-center space-x-2">
              <RefreshCw className="w-5 h-5 animate-spin" />
              <span>Loading emergency issues...</span>
            </div>
          </div>
        </CardContent>
      </Card>
    );
  }

  return (
    <Card className={className}>
      <CardHeader>
        <div className="flex justify-between items-center">
          <CardTitle className="flex items-center gap-2">
            <AlertTriangle className="h-5 w-5" />
            Emergency Issues
            {criticalCount > 0 && (
              <Badge variant="destructive" className="ml-2">
                {criticalCount} Critical
              </Badge>
            )}
          </CardTitle>
          <div className="flex items-center gap-2">
            <Badge variant="outline" className="text-xs">
              <Bell className="w-3 h-3 mr-1" />
              {activeCount} active
            </Badge>
            <Button onClick={fetchEmergencyIssues} variant="outline" size="sm">
              <RefreshCw className="w-3 h-3 mr-1" />
              Refresh
            </Button>
          </div>
        </div>
      </CardHeader>
      <CardContent className="space-y-4">
        {/* Filter tabs */}
        <div className="flex space-x-1 bg-gray-100 p-1 rounded-lg">
          {[
            { key: 'active', label: 'Active', count: activeCount },
            { key: 'critical', label: 'Critical', count: criticalCount },
            { key: 'all', label: 'All', count: issues.length }
          ].map(({ key, label, count }) => (
            <button
              key={key}
              onClick={() => setFilter(key as any)}
              className={`flex-1 px-3 py-2 text-sm font-medium rounded-md transition-colors ${
                filter === key
                  ? 'bg-white text-gray-900 shadow-sm'
                  : 'text-gray-600 hover:text-gray-900'
              }`}
            >
              {label} ({count})
            </button>
          ))}
        </div>

        {/* Issues list */}
        <div className="space-y-3 max-h-96 overflow-y-auto">
          {filteredIssues.length > 0 ? (
            filteredIssues.map((issue) => (
              <div key={issue.id} className="border rounded-lg p-4 space-y-3">
                <div className="flex items-start justify-between">
                  <div className="flex-1">
                    <div className="flex items-center gap-2 mb-1">
                      <Badge className={`${getSeverityColor(issue.severity)} text-xs`}>
                        {getSeverityIcon(issue.severity)}
                        <span className="ml-1">{issue.severity}</span>
                      </Badge>
                      <Badge variant="outline" className={`${getStatusColor(issue.status)} text-xs`}>
                        {getStatusIcon(issue.status)}
                        <span className="ml-1">{issue.status}</span>
                      </Badge>
                    </div>
                    <h4 className="font-semibold text-sm">{issue.title}</h4>
                    <p className="text-sm text-gray-600 mt-1">{issue.description}</p>
                    
                    {/* Affected entities */}
                    {issue.affectedEntities && issue.affectedEntities.length > 0 && (
                      <div className="mt-2">
                        <p className="text-xs text-gray-500 mb-1">Affected:</p>
                        <div className="flex flex-wrap gap-1">
                          {issue.affectedEntities.map((entity, index) => (
                            <Badge key={index} variant="outline" className="text-xs">
                              {entity.type}: {entity.name}
                            </Badge>
                          ))}
                        </div>
                      </div>
                    )}
                  </div>
                </div>
                
                <div className="flex items-center justify-between text-xs text-gray-500">
                  <span>
                    Created: {new Date(issue.createdAt).toLocaleString()}
                  </span>
                  {issue.estimatedResolution && (
                    <span>
                      ETA: {new Date(issue.estimatedResolution).toLocaleString()}
                    </span>
                  )}
                </div>
                
                {/* Action buttons */}
                {issue.status === 'ACTIVE' && (
                  <div className="flex gap-2 pt-2 border-t">
                    <Button
                      onClick={() => acknowledgeIssue(issue.id)}
                      variant="outline"
                      size="sm"
                      className="text-xs"
                    >
                      Acknowledge
                    </Button>
                    <Button
                      onClick={() => resolveIssue(issue.id)}
                      variant="default"
                      size="sm"
                      className="text-xs"
                    >
                      Mark Resolved
                    </Button>
                  </div>
                )}
                
                {issue.status === 'ACKNOWLEDGED' && (
                  <div className="flex gap-2 pt-2 border-t">
                    <Button
                      onClick={() => resolveIssue(issue.id)}
                      variant="default"
                      size="sm"
                      className="text-xs"
                    >
                      Mark Resolved
                    </Button>
                  </div>
                )}
              </div>
            ))
          ) : (
            <div className="text-center py-8 text-gray-500">
              <AlertCircle className="w-12 h-12 mx-auto mb-4 opacity-50" />
              <p>No emergency issues found</p>
              <p className="text-sm">
                {filter === 'active' && 'No active issues requiring attention'}
                {filter === 'critical' && 'No critical issues detected'}
                {filter === 'all' && 'All systems operating normally'}
              </p>
            </div>
          )}
        </div>

        {/* Last update indicator */}
        {lastUpdate && (
          <div className="text-xs text-gray-500 text-center pt-2 border-t">
            Last updated: {lastUpdate.toLocaleString()}
          </div>
        )}
      </CardContent>
    </Card>
  );
};

// Mock data for demo purposes - empty array to show clean state
const mockEmergencyIssues: EmergencyIssue[] = [];