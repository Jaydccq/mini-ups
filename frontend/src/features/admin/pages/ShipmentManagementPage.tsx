import React, { useState } from 'react';
import { useQuery, useMutation } from '@tanstack/react-query';
import { Package, Truck, Search, Filter, MoreHorizontal, Edit, Trash2, RefreshCw, Download, CheckSquare } from 'lucide-react';
import { adminApi, AdminShipment, ShipmentFilters } from '../services/admin';
import { DataTable, Column } from '@/components/ui/data-table';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
import { Badge } from '@/components/ui/badge';
import { Alert, AlertDescription } from '@/components/ui/alert';
import { StatusIndicator } from '@/components/ui/status-indicator';
import { 
  DropdownMenu, 
  DropdownMenuContent, 
  DropdownMenuItem, 
  DropdownMenuSeparator, 
  DropdownMenuTrigger 
} from '@/components/ui/dropdown-menu';
import { toast } from 'sonner';
import { queryClient } from '@/lib/queryClient';
import { formatDateTime, formatRelativeTime } from '@/lib/utils';

export const ShipmentManagementPage: React.FC = () => {
  const [filters, setFilters] = useState<ShipmentFilters>({
    page: 1,
    limit: 20,
    sortBy: 'created_at',
    sortOrder: 'desc'
  });

  const [selectedStatus, setSelectedStatus] = useState('');

  // Fetch shipments
  const { data: shipmentsData, isLoading, error } = useQuery({
    queryKey: ['admin', 'shipments', filters],
    queryFn: () => adminApi.getAllShipments(filters),
    refetchInterval: 30000, // Refresh every 30 seconds
  });

  // Update shipment status mutation
  const updateStatusMutation = useMutation({
    mutationFn: ({ trackingNumber, status, comment }: { trackingNumber: string; status: string; comment?: string }) =>
      adminApi.updateShipmentStatus(trackingNumber, status, comment),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['admin', 'shipments'] });
      toast.success('Shipment status updated successfully');
    },
    onError: (error: any) => {
      toast.error(`Failed to update shipment: ${error.message}`);
    },
  });

  // Batch update mutation
  const batchUpdateMutation = useMutation({
    mutationFn: adminApi.batchUpdateShipments,
    onSuccess: (data) => {
      queryClient.invalidateQueries({ queryKey: ['admin', 'shipments'] });
      toast.success(`${data.updated_count} shipments updated successfully`);
    },
    onError: (error: any) => {
      toast.error(`Batch update failed: ${error.message}`);
    },
  });

  // Delete shipment mutation
  const deleteShipmentMutation = useMutation({
    mutationFn: (trackingNumber: string) => adminApi.deleteShipment(trackingNumber),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['admin', 'shipments'] });
      toast.success('Shipment deleted successfully');
    },
    onError: (error: any) => {
      toast.error(`Failed to delete shipment: ${error.message}`);
    },
  });

  const handleUpdateStatus = (trackingNumber: string, status: string) => {
    const comment = prompt('Enter a comment for this status update (optional):');
    updateStatusMutation.mutate({ trackingNumber, status, comment: comment || undefined });
  };

  const handleDeleteShipment = (trackingNumber: string) => {
    if (confirm('Are you sure you want to delete this shipment? This action cannot be undone.')) {
      deleteShipmentMutation.mutate(trackingNumber);
    }
  };

  const handleBulkStatusUpdate = (selectedShipments: AdminShipment[], newStatus: string) => {
    if (selectedShipments.length === 0) {
      toast.error('Please select at least one shipment');
      return;
    }

    const comment = prompt('Enter a comment for this batch update (optional):');
    const shipmentIds = selectedShipments.map(s => s.shipment_id);
    
    batchUpdateMutation.mutate({
      shipment_ids: shipmentIds,
      new_status: newStatus,
      comment: comment || undefined,
    });
  };

  const handleExport = () => {
    adminApi.exportData('shipments', filters)
      .then(blob => {
        const url = window.URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = `shipments-${new Date().toISOString().split('T')[0]}.csv`;
        document.body.appendChild(a);
        a.click();
        window.URL.revokeObjectURL(url);
        document.body.removeChild(a);
        toast.success('Shipments exported successfully');
      })
      .catch(error => {
        toast.error(`Export failed: ${error.message}`);
      });
  };

  const getStatusActions = (currentStatus: string) => {
    const allStatuses = ['PENDING', 'PICKED_UP', 'IN_TRANSIT', 'OUT_FOR_DELIVERY', 'DELIVERED', 'CANCELLED', 'RETURNED'];
    return allStatuses.filter(status => status !== currentStatus);
  };

  const columns: Column<AdminShipment>[] = [
    {
      key: 'tracking_number',
      title: 'Tracking Number',
      render: (value, shipment) => (
        <div className="space-y-1">
          <div className="font-mono font-medium text-blue-600">{value}</div>
          <div className="text-xs text-gray-500">ID: {shipment.shipment_id}</div>
        </div>
      ),
    },
    {
      key: 'status',
      title: 'Status',
      render: (value, shipment) => (
        <StatusIndicator 
          status={value}
          showDot={true}
          showDescription={false}
          animated={!['DELIVERED', 'CANCELLED', 'RETURNED'].includes(value)}
        />
      ),
    },
    {
      key: 'sender_name',
      title: 'Sender/Recipient',
      render: (value, shipment) => (
        <div className="space-y-1">
          <div className="text-sm font-medium text-gray-900">From: {value}</div>
          <div className="text-sm text-gray-600">To: {shipment.recipient_name}</div>
        </div>
      ),
    },
    {
      key: 'origin',
      title: 'Route',
      render: (value, shipment) => (
        <div className="text-sm">
          <div className="text-gray-900">({value.x}, {value.y})</div>
          <div className="text-gray-500 flex items-center">
            → ({shipment.destination.x}, {shipment.destination.y})
          </div>
        </div>
      ),
    },
    {
      key: 'truck_id',
      title: 'Truck',
      align: 'center',
      render: (value) => value ? (
        <Badge variant="outline" className="flex items-center gap-1">
          <Truck className="h-3 w-3" />
          #{value}
        </Badge>
      ) : (
        <span className="text-sm text-gray-400">Unassigned</span>
      ),
    },
    {
      key: 'created_at',
      title: 'Created',
      render: (value) => (
        <div>
          <div className="text-sm text-gray-900">{formatDateTime(value)}</div>
          <div className="text-xs text-gray-500">{formatRelativeTime(value)}</div>
        </div>
      ),
    },
    {
      key: 'estimated_delivery',
      title: 'Est. Delivery',
      render: (value) => value ? (
        <div>
          <div className="text-sm text-gray-900">{formatDateTime(value)}</div>
          <div className="text-xs text-gray-500">{formatRelativeTime(value)}</div>
        </div>
      ) : (
        <span className="text-sm text-gray-400">TBD</span>
      ),
    },
    {
      key: 'actions',
      title: 'Actions',
      align: 'center',
      render: (_, shipment) => (
        <DropdownMenu>
          <DropdownMenuTrigger asChild>
            <Button variant="ghost" size="sm">
              <MoreHorizontal className="h-4 w-4" />
            </Button>
          </DropdownMenuTrigger>
          <DropdownMenuContent align="end">
            <DropdownMenuItem onClick={() => window.open(`/shipments/tracking/${shipment.tracking_number}`, '_blank')}>
              <Package className="h-4 w-4 mr-2" />
              View Details
            </DropdownMenuItem>
            <DropdownMenuSeparator />
            {getStatusActions(shipment.status).map(status => (
              <DropdownMenuItem 
                key={status}
                onClick={() => handleUpdateStatus(shipment.tracking_number, status)}
              >
                <RefreshCw className="h-4 w-4 mr-2" />
                Set to {status.replace('_', ' ')}
              </DropdownMenuItem>
            ))}
            <DropdownMenuSeparator />
            <DropdownMenuItem 
              onClick={() => handleDeleteShipment(shipment.tracking_number)}
              className="text-red-600"
            >
              <Trash2 className="h-4 w-4 mr-2" />
              Delete Shipment
            </DropdownMenuItem>
          </DropdownMenuContent>
        </DropdownMenu>
      ),
    },
  ];

  // Calculate statistics
  const stats = shipmentsData?.shipments ? {
    total: shipmentsData.total_count,
    pending: shipmentsData.shipments.filter(s => s.status === 'PENDING').length,
    inTransit: shipmentsData.shipments.filter(s => ['PICKED_UP', 'IN_TRANSIT', 'OUT_FOR_DELIVERY'].includes(s.status)).length,
    delivered: shipmentsData.shipments.filter(s => s.status === 'DELIVERED').length,
  } : { total: 0, pending: 0, inTransit: 0, delivered: 0 };

  return (
    <div className="p-6 max-w-7xl mx-auto">
      <div className="mb-8">
        <h1 className="text-3xl font-bold text-gray-900">Shipment Management</h1>
        <p className="text-gray-600">Monitor and manage all shipments in the system</p>
      </div>

      {/* Stats Cards */}
      <div className="grid gap-6 md:grid-cols-4 mb-8">
        <Card>
          <CardContent className="p-6">
            <div className="flex items-center justify-between">
              <div>
                <p className="text-sm font-medium text-gray-600">Total Shipments</p>
                <p className="text-2xl font-bold text-gray-900">{stats.total}</p>
              </div>
              <div className="w-12 h-12 bg-blue-100 rounded-lg flex items-center justify-center">
                <Package className="h-6 w-6 text-blue-600" />
              </div>
            </div>
          </CardContent>
        </Card>

        <Card>
          <CardContent className="p-6">
            <div className="flex items-center justify-between">
              <div>
                <p className="text-sm font-medium text-gray-600">Pending</p>
                <p className="text-2xl font-bold text-yellow-600">{stats.pending}</p>
              </div>
              <div className="w-12 h-12 bg-yellow-100 rounded-lg flex items-center justify-center">
                <Package className="h-6 w-6 text-yellow-600" />
              </div>
            </div>
          </CardContent>
        </Card>

        <Card>
          <CardContent className="p-6">
            <div className="flex items-center justify-between">
              <div>
                <p className="text-sm font-medium text-gray-600">In Transit</p>
                <p className="text-2xl font-bold text-blue-600">{stats.inTransit}</p>
              </div>
              <div className="w-12 h-12 bg-blue-100 rounded-lg flex items-center justify-center">
                <Truck className="h-6 w-6 text-blue-600" />
              </div>
            </div>
          </CardContent>
        </Card>

        <Card>
          <CardContent className="p-6">
            <div className="flex items-center justify-between">
              <div>
                <p className="text-sm font-medium text-gray-600">Delivered</p>
                <p className="text-2xl font-bold text-green-600">{stats.delivered}</p>
              </div>
              <div className="w-12 h-12 bg-green-100 rounded-lg flex items-center justify-center">
                <CheckSquare className="h-6 w-6 text-green-600" />
              </div>
            </div>
          </CardContent>
        </Card>
      </div>

      {/* Quick Actions */}
      <Card className="mb-6">
        <CardHeader>
          <CardTitle>Quick Actions</CardTitle>
          <CardDescription>
            Select multiple shipments and perform batch operations
          </CardDescription>
        </CardHeader>
        <CardContent>
          <div className="flex items-center gap-4">
            <div className="flex items-center gap-2">
              <label htmlFor="bulk-status" className="text-sm font-medium">
                Update Status:
              </label>
              <select
                id="bulk-status"
                value={selectedStatus}
                onChange={(e) => setSelectedStatus(e.target.value)}
                className="border border-gray-300 rounded-md px-3 py-1 text-sm"
              >
                <option value="">Select status...</option>
                <option value="PENDING">Pending</option>
                <option value="PICKED_UP">Picked Up</option>
                <option value="IN_TRANSIT">In Transit</option>
                <option value="OUT_FOR_DELIVERY">Out for Delivery</option>
                <option value="DELIVERED">Delivered</option>
                <option value="CANCELLED">Cancelled</option>
                <option value="RETURNED">Returned</option>
              </select>
            </div>
            <div className="text-sm text-gray-500">
              Select shipments from the table below to enable batch operations
            </div>
          </div>
        </CardContent>
      </Card>

      {/* Shipments Table */}
      <DataTable
        data={shipmentsData?.shipments || []}
        columns={columns}
        loading={isLoading}
        searchable={true}
        searchPlaceholder="Search by tracking number, sender, or recipient..."
        searchFields={['tracking_number', 'sender_name', 'recipient_name']}
        selectable={true}
        sortable={true}
        exportable={true}
        onExport={handleExport}
        actions={[
          {
            label: 'Update Status',
            icon: <RefreshCw className="h-4 w-4 mr-1" />,
            onClick: (shipments) => {
              if (!selectedStatus) {
                toast.error('Please select a status first');
                return;
              }
              handleBulkStatusUpdate(shipments, selectedStatus);
            },
            variant: 'primary',
            disabled: (shipments) => !selectedStatus || shipments.length === 0,
          },
          {
            label: 'Set to Delivered',
            icon: <CheckSquare className="h-4 w-4 mr-1" />,
            onClick: (shipments) => handleBulkStatusUpdate(shipments, 'DELIVERED'),
            variant: 'secondary',
          },
          {
            label: 'Cancel',
            icon: <RefreshCw className="h-4 w-4 mr-1" />,
            onClick: (shipments) => handleBulkStatusUpdate(shipments, 'CANCELLED'),
            variant: 'destructive',
          },
        ]}
        pagination={shipmentsData ? {
          current: filters.page || 1,
          pageSize: filters.limit || 20,
          total: shipmentsData.total_count,
          onChange: (page, pageSize) => {
            setFilters(prev => ({ ...prev, page, limit: pageSize }));
          },
        } : undefined}
        emptyMessage="No shipments found"
      />

      {error && (
        <Alert variant="destructive" className="mt-4">
          <AlertDescription>
            Failed to load shipments: {error.message}
          </AlertDescription>
        </Alert>
      )}
    </div>
  );
};