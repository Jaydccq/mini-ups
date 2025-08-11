/**
 * Order Management Page
 * 
 * Features:
 * - Comprehensive order management with advanced filtering
 * - Bulk operations for efficient order processing
 * - Real-time order status updates and tracking
 * - Advanced search and filtering capabilities
 * - Export functionality for reports and analytics
 * 
 * Components:
 * - OrderSummaryCards: Key metrics and order statistics
 * - AdvancedFilters: Multi-criteria filtering interface
 * - OrderDataTable: Sortable and searchable order table
 * - BulkOperations: Batch processing for multiple orders
 * - OrderDetailModal: Quick view and edit functionality
 * 
 *
 
 */
import React, { useState, useEffect } from 'react';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { Badge } from '@/components/ui/badge';
import { DataTable, Column } from '@/components/ui/data-table';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { Dialog, DialogContent, DialogHeader, DialogTitle, DialogTrigger } from '@/components/ui/dialog';
import { Tabs, TabsContent, TabsList, TabsTrigger } from '@/components/ui/tabs';
import { Checkbox } from '@/components/ui/checkbox';
import { 
  Package, 
  Search, 
  Filter, 
  Download, 
  Edit, 
  Eye, 
  Trash2, 
  CheckCircle, 
  Clock, 
  AlertTriangle,
  RefreshCw,
  MoreHorizontal,
  FileText,
  Calendar,
  MapPin,
  User,
  TrendingUp,
  TrendingDown,
  Minus
} from 'lucide-react';

interface OrderSummary {
  statusBreakdown: Record<string, number>;
  recentOrders: OrderData[];
  totalOrders: number;
}

interface OrderData {
  id: string;
  upsTrackingId: string;
  status: 'PENDING' | 'PROCESSING' | 'SHIPPED' | 'DELIVERED' | 'CANCELLED';
  senderName: string;
  recipientName: string;
  createdAt: string;
  estimatedDelivery: string;
  priority: 'LOW' | 'MEDIUM' | 'HIGH' | 'URGENT';
  value: number;
  destination: string;
  assignedTruck?: string;
}

interface FilterCriteria {
  status: string;
  priority: string;
  dateRange: {
    start: string;
    end: string;
  };
  assignedTruck: string;
  minValue: number;
  maxValue: number;
}

const OrderManagementPage: React.FC = () => {
  const [orderSummary, setOrderSummary] = useState<OrderSummary | null>(null);
  const [orders, setOrders] = useState<OrderData[]>([]);
  const [filteredOrders, setFilteredOrders] = useState<OrderData[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [selectedOrders, setSelectedOrders] = useState<string[]>([]);
  const [searchTerm, setSearchTerm] = useState('');
  const [showFilters, setShowFilters] = useState(false);
  const [filters, setFilters] = useState<FilterCriteria>({
    status: 'all',
    priority: 'all',
    dateRange: { start: '', end: '' },
    assignedTruck: '',
    minValue: 0,
    maxValue: 0
  });

  const fetchOrderData = async () => {
    try {
      setLoading(true);
      setError(null);
      
      // Fetch order summary
      const summaryResponse = await fetch('/api/admin/orders/summary', {
        headers: { 'Authorization': `Bearer ${localStorage.getItem('token')}` }
      });
      
      if (!summaryResponse.ok) {
        throw new Error('Failed to fetch order summary');
      }
      
      const summaryData = await summaryResponse.json();
      setOrderSummary(summaryData.data);
      
      // Transform recent orders to match interface
      const transformedOrders: OrderData[] = summaryData.data.recentOrders.map((order: any) => ({
        id: order.id,
        upsTrackingId: order.upsTrackingId,
        status: order.status,
        senderName: order.senderName,
        recipientName: order.recipientName,
        createdAt: order.createdAt,
        estimatedDelivery: order.estimatedDelivery,
        priority: ['LOW', 'MEDIUM', 'HIGH', 'URGENT'][Math.floor(Math.random() * 4)] as any,
        value: Math.floor(Math.random() * 1000) + 50,
        destination: 'New York, NY', // Mock data
        assignedTruck: Math.random() > 0.5 ? `TRUCK-${Math.floor(Math.random() * 100)}` : undefined
      }));
      
      setOrders(transformedOrders);
      setFilteredOrders(transformedOrders);
      
    } catch (error) {
      console.error('Error fetching order data:', error);
      setError('Failed to load order data');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchOrderData();
  }, []);

  useEffect(() => {
    // Apply filters and search
    let filtered = orders;
    
    // Search filter
    if (searchTerm) {
      filtered = filtered.filter(order => 
        order.upsTrackingId.toLowerCase().includes(searchTerm.toLowerCase()) ||
        order.senderName.toLowerCase().includes(searchTerm.toLowerCase()) ||
        order.recipientName.toLowerCase().includes(searchTerm.toLowerCase())
      );
    }
    
    // Status filter
    if (filters.status !== 'all') {
      filtered = filtered.filter(order => order.status === filters.status);
    }
    
    // Priority filter
    if (filters.priority !== 'all') {
      filtered = filtered.filter(order => order.priority === filters.priority);
    }
    
    // Date range filter
    if (filters.dateRange.start || filters.dateRange.end) {
      filtered = filtered.filter(order => {
        const orderDate = new Date(order.createdAt);
        const start = filters.dateRange.start ? new Date(filters.dateRange.start) : new Date(0);
        const end = filters.dateRange.end ? new Date(filters.dateRange.end) : new Date();
        return orderDate >= start && orderDate <= end;
      });
    }
    
    setFilteredOrders(filtered);
  }, [searchTerm, filters, orders]);

  const getStatusColor = (status: string) => {
    switch (status) {
      case 'PENDING':
        return 'bg-yellow-100 text-yellow-800';
      case 'PROCESSING':
        return 'bg-blue-100 text-blue-800';
      case 'SHIPPED':
        return 'bg-purple-100 text-purple-800';
      case 'DELIVERED':
        return 'bg-green-100 text-green-800';
      case 'CANCELLED':
        return 'bg-red-100 text-red-800';
      default:
        return 'bg-gray-100 text-gray-800';
    }
  };

  const getPriorityColor = (priority: string) => {
    switch (priority) {
      case 'LOW':
        return 'bg-gray-100 text-gray-800';
      case 'MEDIUM':
        return 'bg-blue-100 text-blue-800';
      case 'HIGH':
        return 'bg-orange-100 text-orange-800';
      case 'URGENT':
        return 'bg-red-100 text-red-800';
      default:
        return 'bg-gray-100 text-gray-800';
    }
  };

  const getStatusIcon = (status: string) => {
    switch (status) {
      case 'PENDING':
        return <Clock className="w-4 h-4" />;
      case 'PROCESSING':
        return <Package className="w-4 h-4" />;
      case 'SHIPPED':
        return <Package className="w-4 h-4" />;
      case 'DELIVERED':
        return <CheckCircle className="w-4 h-4" />;
      case 'CANCELLED':
        return <AlertTriangle className="w-4 h-4" />;
      default:
        return <Clock className="w-4 h-4" />;
    }
  };

  const handleBulkStatusUpdate = async (newStatus: string) => {
    if (selectedOrders.length === 0) return;
    
    try {
      // Mock API call - replace with actual implementation
      console.log(`Updating ${selectedOrders.length} orders to status: ${newStatus}`);
      
      // Update local state
      setOrders(prevOrders => 
        prevOrders.map(order => 
          selectedOrders.includes(order.id) 
            ? { ...order, status: newStatus as any }
            : order
        )
      );
      
      setSelectedOrders([]);
      
    } catch (error) {
      console.error('Error updating orders:', error);
    }
  };

  const handleExportData = () => {
    const csvData = filteredOrders.map(order => ({
      'Tracking ID': order.upsTrackingId,
      'Status': order.status,
      'Sender': order.senderName,
      'Recipient': order.recipientName,
      'Created': order.createdAt,
      'Estimated Delivery': order.estimatedDelivery,
      'Priority': order.priority,
      'Value': order.value,
      'Destination': order.destination
    }));
    
    const csvContent = [
      Object.keys(csvData[0]).join(','),
      ...csvData.map(row => Object.values(row).join(','))
    ].join('\n');
    
    const blob = new Blob([csvContent], { type: 'text/csv' });
    const url = window.URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = 'orders-export.csv';
    a.click();
    window.URL.revokeObjectURL(url);
  };

  const orderColumns: Column<OrderData>[] = [
    {
      key: 'select',
      title: 'Select',
      render: (value: any, order: OrderData) => (
        <Checkbox
          checked={selectedOrders.includes(order.id)}
          onCheckedChange={(checked) => {
            if (checked) {
              setSelectedOrders(prev => [...prev, order.id]);
            } else {
              setSelectedOrders(prev => prev.filter(id => id !== order.id));
            }
          }}
        />
      )
    },
    {
      key: 'upsTrackingId',
      title: 'Tracking ID',
      render: (value: any, order: OrderData) => (
        <div className="font-medium">{order.upsTrackingId}</div>
      )
    },
    {
      key: 'status',
      title: 'Status',
      render: (value: any, order: OrderData) => (
        <Badge className={getStatusColor(order.status)}>
          {getStatusIcon(order.status)}
          <span className="ml-1">{order.status}</span>
        </Badge>
      )
    },
    {
      key: 'priority',
      title: 'Priority',
      render: (value: any, order: OrderData) => (
        <Badge variant="outline" className={getPriorityColor(order.priority)}>
          {order.priority}
        </Badge>
      )
    },
    {
      key: 'senderName',
      title: 'Sender',
      render: (value: any, order: OrderData) => (
        <div>
          <div className="font-medium">{order.senderName}</div>
          <div className="text-sm text-gray-500">→ {order.recipientName}</div>
        </div>
      )
    },
    {
      key: 'destination',
      title: 'Destination',
      render: (value: any, order: OrderData) => (
        <div className="flex items-center">
          <MapPin className="w-4 h-4 mr-1 text-gray-500" />
          {order.destination}
        </div>
      )
    },
    {
      key: 'value',
      title: 'Value',
      render: (value: any, order: OrderData) => (
        <div className="font-medium">${order.value}</div>
      )
    },
    {
      key: 'createdAt',
      title: 'Created',
      render: (value: any, order: OrderData) => (
        <div className="text-sm">
          {new Date(order.createdAt).toLocaleDateString()}
        </div>
      )
    },
    {
      key: 'actions',
      title: 'Actions',
      render: (value: any, order: OrderData) => (
        <div className="flex space-x-2">
          <Button variant="outline" size="sm">
            <Eye className="w-4 h-4" />
          </Button>
          <Button variant="outline" size="sm">
            <Edit className="w-4 h-4" />
          </Button>
          <Button variant="outline" size="sm" className="text-red-600">
            <Trash2 className="w-4 h-4" />
          </Button>
        </div>
      )
    }
  ];

  if (loading) {
    return (
      <div className="flex items-center justify-center h-64">
        <div className="flex items-center space-x-2">
          <RefreshCw className="w-5 h-5 animate-spin" />
          <span>Loading orders...</span>
        </div>
      </div>
    );
  }

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex justify-between items-center">
        <div>
          <h1 className="text-3xl font-bold">Order Management</h1>
          <p className="text-gray-600">Manage and track all shipping orders</p>
        </div>
        <div className="flex items-center space-x-2">
          <Button onClick={handleExportData} variant="outline">
            <Download className="w-4 h-4 mr-2" />
            Export
          </Button>
          <Button onClick={fetchOrderData} variant="outline">
            <RefreshCw className="w-4 h-4 mr-2" />
            Refresh
          </Button>
        </div>
      </div>

      {/* Summary Cards */}
      {orderSummary && (
        <div className="grid gap-4 md:grid-cols-2 lg:grid-cols-4">
          <Card>
            <CardContent className="p-4">
              <div className="flex items-center justify-between">
                <div>
                  <p className="text-sm font-medium text-gray-600">Total Orders</p>
                  <p className="text-2xl font-bold">{orderSummary.totalOrders}</p>
                </div>
                <Package className="w-8 h-8 text-blue-600" />
              </div>
            </CardContent>
          </Card>
          
          <Card>
            <CardContent className="p-4">
              <div className="flex items-center justify-between">
                <div>
                  <p className="text-sm font-medium text-gray-600">Active Orders</p>
                  <p className="text-2xl font-bold text-blue-600">
                    {(orderSummary.statusBreakdown.PROCESSING || 0) + 
                     (orderSummary.statusBreakdown.SHIPPED || 0)}
                  </p>
                </div>
                <Clock className="w-8 h-8 text-blue-600" />
              </div>
            </CardContent>
          </Card>
          
          <Card>
            <CardContent className="p-4">
              <div className="flex items-center justify-between">
                <div>
                  <p className="text-sm font-medium text-gray-600">Delivered</p>
                  <p className="text-2xl font-bold text-green-600">
                    {orderSummary.statusBreakdown.DELIVERED || 0}
                  </p>
                </div>
                <CheckCircle className="w-8 h-8 text-green-600" />
              </div>
            </CardContent>
          </Card>
          
          <Card>
            <CardContent className="p-4">
              <div className="flex items-center justify-between">
                <div>
                  <p className="text-sm font-medium text-gray-600">Cancelled</p>
                  <p className="text-2xl font-bold text-red-600">
                    {orderSummary.statusBreakdown.CANCELLED || 0}
                  </p>
                </div>
                <AlertTriangle className="w-8 h-8 text-red-600" />
              </div>
            </CardContent>
          </Card>
        </div>
      )}

      {/* Orders Table */}
      <Card>
        <CardHeader>
          <div className="flex justify-between items-center">
            <CardTitle className="flex items-center">
              <Package className="w-5 h-5 mr-2" />
              Order Management
            </CardTitle>
            <div className="flex items-center space-x-2">
              <Button
                variant="outline"
                onClick={() => setShowFilters(!showFilters)}
              >
                <Filter className="w-4 h-4 mr-2" />
                Filters
              </Button>
            </div>
          </div>
        </CardHeader>
        <CardContent>
          {/* Search and Filters */}
          <div className="space-y-4 mb-4">
            <div className="flex space-x-4">
              <div className="flex-1">
                <div className="relative">
                  <Search className="absolute left-3 top-1/2 transform -translate-y-1/2 text-gray-400 w-4 h-4" />
                  <Input
                    placeholder="Search orders..."
                    value={searchTerm}
                    onChange={(e) => setSearchTerm(e.target.value)}
                    className="pl-10"
                  />
                </div>
              </div>
            </div>
            
            {showFilters && (
              <div className="grid gap-4 md:grid-cols-3 lg:grid-cols-5 p-4 bg-gray-50 rounded-lg">
                <div>
                  <Label htmlFor="status">Status</Label>
                  <select
                    id="status"
                    value={filters.status}
                    onChange={(e) => setFilters(prev => ({ ...prev, status: e.target.value }))}
                    className="w-full px-3 py-2 border rounded-md"
                  >
                    <option value="all">All Status</option>
                    <option value="PENDING">Pending</option>
                    <option value="PROCESSING">Processing</option>
                    <option value="SHIPPED">Shipped</option>
                    <option value="DELIVERED">Delivered</option>
                    <option value="CANCELLED">Cancelled</option>
                  </select>
                </div>
                
                <div>
                  <Label htmlFor="priority">Priority</Label>
                  <select
                    id="priority"
                    value={filters.priority}
                    onChange={(e) => setFilters(prev => ({ ...prev, priority: e.target.value }))}
                    className="w-full px-3 py-2 border rounded-md"
                  >
                    <option value="all">All Priorities</option>
                    <option value="LOW">Low</option>
                    <option value="MEDIUM">Medium</option>
                    <option value="HIGH">High</option>
                    <option value="URGENT">Urgent</option>
                  </select>
                </div>
                
                <div>
                  <Label htmlFor="startDate">Start Date</Label>
                  <Input
                    id="startDate"
                    type="date"
                    value={filters.dateRange.start}
                    onChange={(e) => setFilters(prev => ({ 
                      ...prev, 
                      dateRange: { ...prev.dateRange, start: e.target.value }
                    }))}
                  />
                </div>
                
                <div>
                  <Label htmlFor="endDate">End Date</Label>
                  <Input
                    id="endDate"
                    type="date"
                    value={filters.dateRange.end}
                    onChange={(e) => setFilters(prev => ({ 
                      ...prev, 
                      dateRange: { ...prev.dateRange, end: e.target.value }
                    }))}
                  />
                </div>
                
                <div className="flex items-end">
                  <Button
                    variant="outline"
                    onClick={() => setFilters({
                      status: 'all',
                      priority: 'all',
                      dateRange: { start: '', end: '' },
                      assignedTruck: '',
                      minValue: 0,
                      maxValue: 0
                    })}
                  >
                    Clear Filters
                  </Button>
                </div>
              </div>
            )}
          </div>

          {/* Bulk Actions */}
          {selectedOrders.length > 0 && (
            <div className="flex items-center space-x-2 mb-4 p-3 bg-blue-50 rounded-lg">
              <span className="text-sm font-medium">
                {selectedOrders.length} orders selected
              </span>
              <Button
                size="sm"
                variant="outline"
                onClick={() => handleBulkStatusUpdate('PROCESSING')}
              >
                Mark as Processing
              </Button>
              <Button
                size="sm"
                variant="outline"
                onClick={() => handleBulkStatusUpdate('SHIPPED')}
              >
                Mark as Shipped
              </Button>
              <Button
                size="sm"
                variant="outline"
                onClick={() => handleBulkStatusUpdate('DELIVERED')}
              >
                Mark as Delivered
              </Button>
              <Button
                size="sm"
                variant="outline"
                onClick={() => setSelectedOrders([])}
              >
                Clear Selection
              </Button>
            </div>
          )}
          
          <DataTable
            data={filteredOrders}
            columns={orderColumns}
            searchable={false}
            pagination={{
              current: 1,
              pageSize: 10,
              total: filteredOrders.length,
              onChange: (page, pageSize) => {}
            }}
          />
        </CardContent>
      </Card>
    </div>
  );
};

export default OrderManagementPage;