/**
 * NotificationCenter Component
 * 
 * Comprehensive notification UI with filtering, actions, and real-time updates.
 * Implements the notification design patterns for logistics applications.
 */

import React, { useState, useEffect } from 'react';
import { 
  Bell, 
  X, 
  Filter, 
  Settings, 
  Check, 
  Archive, 
  Trash2, 
  ExternalLink,
  Clock,
  AlertCircle,
  Info,
  CheckCircle,
  XCircle,
  RefreshCw,
  WifiOff,
  Wifi
} from 'lucide-react';
import { useNotificationStore } from '@/stores/notificationStore';
import { notificationApi } from '@/services/notification';
import { socketService } from '@/services/socketService';
import { Button } from '@/components/ui/button';
import { Badge } from '@/components/ui/badge';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
import { Separator } from '@/components/ui/separator';
import { ScrollArea } from '@/components/ui/scroll-area';
import { 
  DropdownMenu, 
  DropdownMenuContent, 
  DropdownMenuItem, 
  DropdownMenuSeparator, 
  DropdownMenuTrigger,
  DropdownMenuCheckboxItem,
  DropdownMenuLabel
} from '@/components/ui/dropdown-menu';
import { 
  Sheet, 
  SheetContent, 
  SheetDescription, 
  SheetHeader, 
  SheetTitle 
} from '@/components/ui/sheet';
import { Notification, NotificationType, NotificationPriority } from '@/types/notification';
import { formatRelativeTime } from '@/lib/utils';
import { toast } from 'sonner';
import { useMutation } from '@tanstack/react-query';

// Notification icon mapping
const getNotificationIcon = (type: NotificationType, priority: NotificationPriority) => {
  if (priority === 'critical') return <XCircle className="h-5 w-5 text-red-500" />;
  
  switch (type) {
    case 'shipment_status':
    case 'shipment_created':
    case 'shipment_updated':
      return <CheckCircle className="h-5 w-5 text-blue-500" />;
    case 'system_alert':
      return <AlertCircle className="h-5 w-5 text-yellow-500" />;
    case 'user_message':
      return <Info className="h-5 w-5 text-green-500" />;
    case 'conflict_resolution':
      return <XCircle className="h-5 w-5 text-orange-500" />;
    case 'delivery_confirmation':
      return <CheckCircle className="h-5 w-5 text-green-500" />;
    default:
      return <Bell className="h-5 w-5 text-gray-500" />;
  }
};

// Priority badge variant mapping
const getPriorityVariant = (priority: NotificationPriority) => {
  switch (priority) {
    case 'critical': return 'destructive';
    case 'high': return 'destructive';
    case 'medium': return 'secondary';
    case 'low': return 'outline';
    default: return 'outline';
  }
};

// Connection status indicator
const ConnectionStatus: React.FC = () => {
  const connectionStatus = useNotificationStore(state => state.connectionStatus);
  const socketStats = socketService.getConnectionStats();

  const getStatusIcon = () => {
    switch (connectionStatus) {
      case 'online': return <Wifi className="h-4 w-4 text-green-500" />;
      case 'syncing': return <RefreshCw className="h-4 w-4 text-blue-500 animate-spin" />;
      case 'offline': return <WifiOff className="h-4 w-4 text-red-500" />;
      default: return <WifiOff className="h-4 w-4 text-gray-500" />;
    }
  };

  const getStatusText = () => {
    switch (connectionStatus) {
      case 'online': return 'Connected';
      case 'syncing': return 'Syncing...';
      case 'offline': return 'Offline';
      default: return 'Disconnected';
    }
  };

  return (
    <div className="flex items-center gap-2 text-sm text-gray-600">
      {getStatusIcon()}
      <span>{getStatusText()}</span>
      {socketStats.reconnectAttempts > 0 && (
        <span className="text-xs text-gray-500">
          (Attempt {socketStats.reconnectAttempts}/{socketStats.maxReconnectAttempts})
        </span>
      )}
    </div>
  );
};

// Individual notification item
const NotificationItem: React.FC<{
  notification: Notification;
  onAction: (actionId: string, payload?: any) => void;
  onMarkAsRead: () => void;
  onArchive: () => void;
  onDelete: () => void;
}> = ({ notification, onAction, onMarkAsRead, onArchive, onDelete }) => {
  const isUnread = notification.status === 'unread';

  const handleActionClick = (action: any) => {
    if (action.action === 'navigate' && action.payload?.url) {
      window.open(action.payload.url, '_blank');
    } else if (action.action === 'api_call') {
      onAction(action.id, action.payload);
    } else if (action.action === 'dismiss') {
      onMarkAsRead();
    }
  };

  return (
    <Card className={`mb-3 ${isUnread ? 'border-blue-300 bg-blue-50' : 'border-gray-200'}`}>
      <CardContent className="p-4">
        <div className="flex items-start gap-3">
          <div className="flex-shrink-0 mt-1">
            {getNotificationIcon(notification.type, notification.priority)}
          </div>
          
          <div className="flex-1 min-w-0">
            <div className="flex items-start justify-between gap-2">
              <div className="flex-1">
                <h4 className={`text-sm font-medium ${isUnread ? 'text-gray-900' : 'text-gray-700'}`}>
                  {notification.title}
                </h4>
                <p className="text-sm text-gray-600 mt-1">
                  {notification.message}
                </p>
              </div>
              
              <div className="flex items-center gap-2 flex-shrink-0">
                <Badge variant={getPriorityVariant(notification.priority)}>
                  {notification.priority}
                </Badge>
                {isUnread && (
                  <div className="w-2 h-2 bg-blue-500 rounded-full" />
                )}
              </div>
            </div>

            <div className="flex items-center justify-between mt-3">
              <div className="flex items-center gap-2 text-xs text-gray-500">
                <Clock className="h-3 w-3" />
                <span>{formatRelativeTime(notification.timestamp)}</span>
                {notification.relatedEntityId && (
                  <>
                    <span>•</span>
                    <span className="font-mono">{notification.relatedEntityId}</span>
                  </>
                )}
              </div>

              <div className="flex items-center gap-1">
                {notification.actions?.map((action) => (
                  <Button
                    key={action.id}
                    size="sm"
                    variant={action.variant === 'primary' ? 'default' : 'ghost'}
                    onClick={() => handleActionClick(action)}
                    className="h-6 px-2 text-xs"
                  >
                    {action.action === 'navigate' && <ExternalLink className="h-3 w-3 mr-1" />}
                    {action.label}
                  </Button>
                ))}
                
                <DropdownMenu>
                  <DropdownMenuTrigger asChild>
                    <Button size="sm" variant="ghost" className="h-6 w-6 p-0">
                      <Settings className="h-3 w-3" />
                    </Button>
                  </DropdownMenuTrigger>
                  <DropdownMenuContent align="end">
                    {isUnread && (
                      <DropdownMenuItem onClick={onMarkAsRead}>
                        <Check className="h-4 w-4 mr-2" />
                        Mark as Read
                      </DropdownMenuItem>
                    )}
                    <DropdownMenuItem onClick={onArchive}>
                      <Archive className="h-4 w-4 mr-2" />
                      Archive
                    </DropdownMenuItem>
                    <DropdownMenuSeparator />
                    <DropdownMenuItem onClick={onDelete} className="text-red-600">
                      <Trash2 className="h-4 w-4 mr-2" />
                      Delete
                    </DropdownMenuItem>
                  </DropdownMenuContent>
                </DropdownMenu>
              </div>
            </div>
          </div>
        </div>
      </CardContent>
    </Card>
  );
};

// Main NotificationCenter component
export const NotificationCenter: React.FC = () => {
  const {
    isNotificationCenterOpen,
    toggleNotificationCenter,
    unreadCount,
    filters,
    setFilters,
    getFilteredNotifications,
    markAsRead,
    markAllAsRead,
    archiveNotification,
    removeNotification,
  } = useNotificationStore();

  const [filterOpen, setFilterOpen] = useState(false);
  const notifications = getFilteredNotifications();

  // Execute notification action mutation
  const executeActionMutation = useMutation({
    mutationFn: ({ notificationId, actionId, payload }: {
      notificationId: string;
      actionId: string;
      payload?: any;
    }) => notificationApi.executeNotificationAction(notificationId, actionId, payload),
    onSuccess: () => {
      toast.success('Action executed successfully');
    },
    onError: (error: any) => {
      toast.error(`Action failed: ${error.message}`);
    },
  });

  const handleNotificationAction = (notificationId: string, actionId: string, payload?: any) => {
    executeActionMutation.mutate({ notificationId, actionId, payload });
  };

  const handleRequestPermission = async () => {
    const permission = await socketService.requestNotificationPermission();
    if (permission === 'granted') {
      toast.success('Browser notifications enabled');
    } else {
      toast.error('Browser notifications denied');
    }
  };

  const filteredTypeOptions: NotificationType[] = [
    'shipment_status',
    'shipment_created', 
    'shipment_updated',
    'system_alert',
    'user_message',
    'conflict_resolution',
    'delivery_confirmation'
  ];

  const filteredPriorityOptions: NotificationPriority[] = ['low', 'medium', 'high', 'critical'];

  return (
    <>
      {/* Notification Bell Button */}
      <Button
        variant="ghost"
        size="sm"
        onClick={toggleNotificationCenter}
        className="relative"
      >
        <Bell className="h-5 w-5" />
        {unreadCount > 0 && (
          <Badge 
            variant="destructive" 
            className="absolute -top-1 -right-1 h-5 w-5 flex items-center justify-center p-0 text-xs"
          >
            {unreadCount > 99 ? '99+' : unreadCount}
          </Badge>
        )}
      </Button>

      {/* Notification Panel */}
      <Sheet open={isNotificationCenterOpen} onOpenChange={toggleNotificationCenter}>
        <SheetContent className="w-full sm:w-[500px] sm:max-w-[500px]">
          <SheetHeader>
            <div className="flex items-center justify-between">
              <SheetTitle>Notifications</SheetTitle>
              <div className="flex items-center gap-2">
                <DropdownMenu open={filterOpen} onOpenChange={setFilterOpen}>
                  <DropdownMenuTrigger asChild>
                    <Button variant="outline" size="sm">
                      <Filter className="h-4 w-4 mr-2" />
                      Filter
                    </Button>
                  </DropdownMenuTrigger>
                  <DropdownMenuContent align="end" className="w-56">
                    <DropdownMenuLabel>Filter by Status</DropdownMenuLabel>
                    <DropdownMenuCheckboxItem
                      checked={filters.status === 'unread'}
                      onCheckedChange={(checked) => 
                        setFilters({ status: checked ? 'unread' : undefined })
                      }
                    >
                      Unread only
                    </DropdownMenuCheckboxItem>
                    
                    <DropdownMenuSeparator />
                    <DropdownMenuLabel>Types</DropdownMenuLabel>
                    {filteredTypeOptions.map((type) => (
                      <DropdownMenuCheckboxItem
                        key={type}
                        checked={filters.types?.includes(type) || false}
                        onCheckedChange={(checked) => {
                          const currentTypes = filters.types || [];
                          const newTypes = checked 
                            ? [...currentTypes, type]
                            : currentTypes.filter(t => t !== type);
                          setFilters({ types: newTypes.length > 0 ? newTypes : undefined });
                        }}
                      >
                        {type.replace('_', ' ')}
                      </DropdownMenuCheckboxItem>
                    ))}
                    
                    <DropdownMenuSeparator />
                    <DropdownMenuLabel>Priorities</DropdownMenuLabel>
                    {filteredPriorityOptions.map((priority) => (
                      <DropdownMenuCheckboxItem
                        key={priority}
                        checked={filters.priorities?.includes(priority) || false}
                        onCheckedChange={(checked) => {
                          const currentPriorities = filters.priorities || [];
                          const newPriorities = checked 
                            ? [...currentPriorities, priority]
                            : currentPriorities.filter(p => p !== priority);
                          setFilters({ priorities: newPriorities.length > 0 ? newPriorities : undefined });
                        }}
                      >
                        {priority}
                      </DropdownMenuCheckboxItem>
                    ))}
                  </DropdownMenuContent>
                </DropdownMenu>

                <Button 
                  variant="outline" 
                  size="sm"
                  onClick={markAllAsRead}
                  disabled={unreadCount === 0}
                >
                  <Check className="h-4 w-4 mr-2" />
                  Read All
                </Button>
              </div>
            </div>
            
            <div className="flex items-center justify-between">
              <SheetDescription>
                {notifications.length} notification{notifications.length !== 1 ? 's' : ''}
                {unreadCount > 0 && ` • ${unreadCount} unread`}
              </SheetDescription>
              <ConnectionStatus />
            </div>
          </SheetHeader>

          <Separator className="my-4" />

          {/* Browser Notification Permission */}
          {typeof window !== 'undefined' && 'Notification' in window && Notification.permission === 'default' && (
            <Card className="mb-4 border-blue-200 bg-blue-50">
              <CardContent className="p-4">
                <div className="flex items-center justify-between">
                  <div>
                    <h4 className="text-sm font-medium text-blue-900">Enable Browser Notifications</h4>
                    <p className="text-xs text-blue-700 mt-1">
                      Get notified even when the app is in the background
                    </p>
                  </div>
                  <Button size="sm" onClick={handleRequestPermission}>
                    Enable
                  </Button>
                </div>
              </CardContent>
            </Card>
          )}

          {/* Notification List */}
          <ScrollArea className="flex-1 h-[calc(100vh-200px)]">
            {notifications.length === 0 ? (
              <div className="flex flex-col items-center justify-center py-12 text-center">
                <Bell className="h-12 w-12 text-gray-300 mb-4" />
                <h3 className="text-lg font-medium text-gray-900 mb-2">No notifications</h3>
                <p className="text-sm text-gray-500">
                  {filters.status === 'unread' 
                    ? "You're all caught up! No unread notifications."
                    : "No notifications match your current filters."
                  }
                </p>
              </div>
            ) : (
              <div className="space-y-0">
                {notifications.map((notification) => (
                  <NotificationItem
                    key={notification.id}
                    notification={notification}
                    onAction={(actionId, payload) => 
                      handleNotificationAction(notification.id, actionId, payload)
                    }
                    onMarkAsRead={() => markAsRead(notification.id)}
                    onArchive={() => archiveNotification(notification.id)}
                    onDelete={() => removeNotification(notification.id)}
                  />
                ))}
              </div>
            )}
          </ScrollArea>
        </SheetContent>
      </Sheet>
    </>
  );
};