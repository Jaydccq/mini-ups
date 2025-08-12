/**
 * World Simulator Debug Page - Redesigned for Clear Connection Status
 * 
 * Features:
 * - Prominent connection status and World ID display
 * - One-click World ID sharing functionality
 * - Real-time message monitoring with WebSocket connection
 * - Message filtering and search functionality
 * - Performance statistics and connection health
 * - Debug system controls (clear, export, test connection)
 * - Interactive message details with JSON viewer
 * 
 *
 * @version 2.0.0
 */
import React, { useState, useEffect, useMemo } from 'react';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Badge } from '@/components/ui/badge';
import { Tabs, TabsContent, TabsList, TabsTrigger } from '@/components/ui/tabs';
import { Alert, AlertDescription } from '@/components/ui/alert';
import { useWorldSimulatorDebug } from '../hooks/useWorldSimulatorDebug';
import { useToast } from '@/hooks/use-toast';
import { 
  Activity, 
  Wifi, 
  WifiOff, 
  RefreshCw, 
  Trash2, 
  Download, 
  TestTube,
  Search,
  Filter,
  MessageSquare,
  Clock,
  ArrowUp,
  ArrowDown,
  AlertTriangle,
  CheckCircle,
  XCircle,
  Copy,
  Globe,
  Loader2
} from 'lucide-react';

export function WorldSimulatorDebugPage() {
  const {
    messages,
    statistics,
    isConnected,
    connectionStatus,
    worldId,
    sessionConnected,
    sessionConnecting,
    connect,
    disconnect,
    connectSimulatorSession,
    disconnectSimulatorSession,
    clearMessages,
    exportMessages,
    testConnection,
    loadMessageHistory,
    loadStatistics,
    fetchStatus,
  } = useWorldSimulatorDebug();

  const { toast } = useToast();
  const [searchTerm, setSearchTerm] = useState('');
  const [filterDirection, setFilterDirection] = useState<'all' | 'inbound' | 'outbound'>('all');
  const [selectedMessage, setSelectedMessage] = useState<any>(null);
  const [showModal, setShowModal] = useState(false);
  const [inputWorldId, setInputWorldId] = useState('');

  // Filter messages based on search and direction
  const filteredMessages = useMemo(() => {
    return messages.filter(message => {
      const matchesSearch = !searchTerm || 
        message.summary?.toLowerCase().includes(searchTerm.toLowerCase()) ||
        message.messageType?.toLowerCase().includes(searchTerm.toLowerCase()) ||
        message.jsonContent?.toLowerCase().includes(searchTerm.toLowerCase());
      
      const matchesDirection = filterDirection === 'all' || 
        message.direction?.toLowerCase() === filterDirection;
      
      return matchesSearch && matchesDirection;
    });
  }, [messages, searchTerm, filterDirection]);

  // Handle clear messages
  const handleClearMessages = async () => {
    try {
      await clearMessages();
      toast({
        title: "Messages Cleared",
        description: "All debug messages have been cleared successfully.",
      });
    } catch (error) {
      toast({
        title: "Error",
        description: "Failed to clear messages. Please try again.",
        variant: "destructive",
      });
    }
  };

  // Handle export messages
  const handleExportMessages = async () => {
    try {
      await exportMessages();
      toast({
        title: "Export Complete",
        description: "Debug messages have been exported successfully.",
      });
    } catch (error) {
      toast({
        title: "Export Failed",
        description: "Failed to export messages. Please try again.",
        variant: "destructive",
      });
    }
  };

  // Handle connection test
  const handleTestConnection = async () => {
    try {
      const result = await testConnection();
      toast({
        title: "Connection Test",
        description: result.connectionHealthy ? 
          "Connection test passed successfully" : 
          "Connection test failed - check World Simulator status",
        variant: result.connectionHealthy ? "default" : "destructive",
      });
    } catch (error) {
      toast({
        title: "Test Failed",
        description: "Connection test failed. Please try again.",
        variant: "destructive",
      });
    }
  };

  // Format message timestamp
  const formatTimestamp = (timestamp: string) => {
    return new Date(timestamp).toLocaleTimeString();
  };

  // Handle World Simulator connection
  const handleConnect = async () => {
    try {
      const worldIdNumber = inputWorldId === '' || inputWorldId === '0' ? 0 : parseInt(inputWorldId);
      
      if (isNaN(worldIdNumber) || worldIdNumber < 0) {
        toast({
          title: "Invalid World ID",
          description: "Please enter a valid World ID (number) or 0 for a new world.",
          variant: "destructive",
        });
        return;
      }

      const result = await connectSimulatorSession(worldIdNumber);
      
      if (result.success) {
        toast({
          title: "Connected to World!",
          description: `Successfully connected to World ID ${result.worldId}`,
        });
        setInputWorldId(''); // Clear input after successful connection
      }
    } catch (error) {
      toast({
        title: "Connection Failed",
        description: error instanceof Error ? error.message : "Failed to connect to World Simulator",
        variant: "destructive",
      });
    }
  };

  // Handle disconnect
  const handleDisconnect = async () => {
    try {
      await disconnectSimulatorSession();
      toast({
        title: "Disconnected",
        description: "Successfully disconnected from World Simulator",
      });
    } catch (error) {
      toast({
        title: "Disconnect Error",
        description: "There was an error disconnecting from the World Simulator",
        variant: "destructive",
      });
    }
  };

  // Copy world ID to clipboard
  const copyWorldId = async () => {
    if (!worldId) return;
    
    try {
      await navigator.clipboard.writeText(worldId.toString());
      toast({
        title: "World ID Copied!",
        description: `World ID ${worldId} has been copied to your clipboard.`,
      });
    } catch (error) {
      // Fallback for older browsers
      const textArea = document.createElement('textarea');
      textArea.value = worldId.toString();
      document.body.appendChild(textArea);
      textArea.select();
      document.execCommand('copy');
      document.body.removeChild(textArea);
      
      toast({
        title: "World ID Copied!",
        description: `World ID ${worldId} has been copied to your clipboard.`,
      });
    }
  };

  // Get connection status color
  const getConnectionStatusColor = (status: string) => {
    switch (status) {
      case 'connected': return 'bg-green-100 text-green-800';
      case 'connecting': return 'bg-yellow-100 text-yellow-800';
      case 'error': return 'bg-red-100 text-red-800';
      default: return 'bg-gray-100 text-gray-800';
    }
  };

  // Get connection status icon
  const getConnectionStatusIcon = (status: string) => {
    switch (status) {
      case 'connected': return <CheckCircle className="w-4 h-4" />;
      case 'connecting': return <RefreshCw className="w-4 h-4 animate-spin" />;
      case 'error': return <XCircle className="w-4 h-4" />;
      default: return <AlertTriangle className="w-4 h-4" />;
    }
  };

  return (
    <div className="container mx-auto p-6 space-y-6">
      {/* Header */}
      <div>
        <h1 className="text-3xl font-bold">World Simulator</h1>
        <p className="text-gray-600">Manage your connection to the World Simulator</p>
      </div>

      {/* Connection Status Banner */}
      <Card className="border-2">
        <CardContent className="p-6">
          {sessionConnected && worldId ? (
            /* Connected State */
            <div className="flex flex-col space-y-4">
              <div className="flex items-center justify-between">
                <div className="flex items-center space-x-3">
                  <div className="flex items-center justify-center w-12 h-12 bg-green-100 rounded-full">
                    <CheckCircle className="w-6 h-6 text-green-600" />
                  </div>
                  <div>
                    <h2 className="text-2xl font-bold text-green-700">Connected to World</h2>
                    <p className="text-green-600">Your UPS system is successfully connected</p>
                  </div>
                </div>
                <Button 
                  onClick={handleDisconnect}
                  variant="destructive"
                  size="sm"
                >
                  <WifiOff className="w-4 h-4 mr-2" />
                  Disconnect
                </Button>
              </div>
              
              <div className="bg-green-50 p-4 rounded-lg border border-green-200">
                <div className="flex items-center justify-between">
                  <div>
                    <p className="text-sm font-medium text-green-800 mb-1">Current World ID:</p>
                    <div className="flex items-center space-x-2">
                      <code className="text-2xl font-bold text-green-700 bg-white px-3 py-1 rounded border">
                        {worldId}
                      </code>
                      <Button
                        onClick={copyWorldId}
                        variant="outline"
                        size="sm"
                        className="h-8 border-green-300 hover:bg-green-100"
                      >
                        <Copy className="w-4 h-4 mr-1" />
                        Copy
                      </Button>
                    </div>
                  </div>
                  <Globe className="w-8 h-8 text-green-400" />
                </div>
                <p className="text-sm text-green-700 mt-2">
                  Share this World ID with your Amazon partner to connect to the same simulation world.
                </p>
              </div>
            </div>
          ) : sessionConnecting ? (
            /* Connecting State */
            <div className="flex items-center space-x-4">
              <div className="flex items-center justify-center w-12 h-12 bg-blue-100 rounded-full">
                <Loader2 className="w-6 h-6 text-blue-600 animate-spin" />
              </div>
              <div>
                <h2 className="text-xl font-bold text-blue-700">Connecting to World...</h2>
                <p className="text-blue-600">Please wait while we establish connection</p>
              </div>
            </div>
          ) : connectionStatus === 'error' ? (
            /* Error State */
            <div className="flex flex-col space-y-4">
              <div className="flex items-center justify-between">
                <div className="flex items-center space-x-3">
                  <div className="flex items-center justify-center w-12 h-12 bg-red-100 rounded-full">
                    <XCircle className="w-6 h-6 text-red-600" />
                  </div>
                  <div>
                    <h2 className="text-xl font-bold text-red-700">Connection Error</h2>
                    <p className="text-red-600">Unable to connect to World Simulator</p>
                  </div>
                </div>
                <div className="flex space-x-2">
                  <Button 
                    onClick={fetchStatus}
                    variant="outline"
                    size="sm"
                  >
                    <RefreshCw className="w-4 h-4 mr-2" />
                    Refresh
                  </Button>
                  <Button 
                    onClick={connect}
                    variant="default"
                    size="sm"
                  >
                    <Wifi className="w-4 h-4 mr-2" />
                    Retry Connection
                  </Button>
                </div>
              </div>
              <Alert className="border-red-200 bg-red-50">
                <AlertTriangle className="h-4 w-4 text-red-600" />
                <AlertDescription className="text-red-700">
                  World Simulator may not be available. Check your backend configuration and ensure the World Simulator is running.
                </AlertDescription>
              </Alert>
            </div>
          ) : (
            /* Disconnected State - World ID Input Form */
            <div className="flex flex-col space-y-4">
              <div className="flex items-center space-x-3 mb-2">
                <div className="flex items-center justify-center w-12 h-12 bg-blue-100 rounded-full">
                  <Globe className="w-6 h-6 text-blue-600" />
                </div>
                <div>
                  <h2 className="text-xl font-bold text-gray-700">Connect to an Existing World</h2>
                  <p className="text-gray-600">Join a simulation world using a World ID</p>
                </div>
              </div>
              
              <div className="bg-blue-50 p-4 rounded-lg border border-blue-200">
                <div className="space-y-3">
                  <div>
                    <label htmlFor="worldId" className="block text-sm font-medium text-blue-800 mb-2">
                      World ID
                    </label>
                    <Input
                      id="worldId"
                      type="text"
                      placeholder="Enter World ID (or 0 for new world)"
                      value={inputWorldId}
                      onChange={(e) => setInputWorldId(e.target.value)}
                      className="w-full"
                      onKeyPress={(e) => {
                        if (e.key === 'Enter') {
                          handleConnect();
                        }
                      }}
                    />
                  </div>
                  <p className="text-sm text-blue-700">
                    Enter the World ID provided by your UPS partner. When you enter <code className="bg-white px-1 rounded">0</code>, you will automatically connect to a new world.
                  </p>
                  <div className="flex justify-end">
                    <Button 
                      onClick={handleConnect}
                      disabled={sessionConnecting}
                      className="px-6"
                    >
                      {sessionConnecting ? (
                        <>
                          <Loader2 className="w-4 h-4 mr-2 animate-spin" />
                          Connecting...
                        </>
                      ) : (
                        <>
                          <Wifi className="w-4 h-4 mr-2" />
                          Connect to World
                        </>
                      )}
                    </Button>
                  </div>
                </div>
              </div>
            </div>
          )}
        </CardContent>
      </Card>

      {/* Quick Stats (only show when connected) */}
      {sessionConnected && (
        <div className="grid grid-cols-1 md:grid-cols-4 gap-4">
          <Card>
            <CardContent className="p-4">
              <div className="flex items-center justify-between">
                <div>
                  <p className="text-sm text-gray-600">Total Messages</p>
                  <p className="text-2xl font-bold">{statistics?.totalMessages || 0}</p>
                </div>
                <MessageSquare className="w-6 h-6 text-blue-500" />
              </div>
            </CardContent>
          </Card>

          <Card>
            <CardContent className="p-4">
              <div className="flex items-center justify-between">
                <div>
                  <p className="text-sm text-gray-600">Messages/Sec</p>
                  <p className="text-2xl font-bold">{statistics?.messagesPerSecond?.toFixed(2) || '0.00'}</p>
                </div>
                <Activity className="w-6 h-6 text-green-500" />
              </div>
            </CardContent>
          </Card>

          <Card>
            <CardContent className="p-4">
              <div className="flex items-center justify-between">
                <div>
                  <p className="text-sm text-gray-600">Cached Messages</p>
                  <p className="text-2xl font-bold">{statistics?.cachedMessages || 0}</p>
                </div>
                <RefreshCw className="w-6 h-6 text-purple-500" />
              </div>
            </CardContent>
          </Card>

          <Card>
            <CardContent className="p-4">
              <div className="flex items-center justify-between">
                <div>
                  <p className="text-sm text-gray-600">World ID</p>
                  <p className="text-2xl font-bold text-green-600">{worldId || 'N/A'}</p>
                </div>
                <Globe className="w-6 h-6 text-green-500" />
              </div>
            </CardContent>
          </Card>
        </div>
      )}

      {/* Advanced Debug Section */}
      {sessionConnected && (
        <Card>
          <CardHeader>
            <CardTitle className="flex items-center justify-between">
              <span className="flex items-center">
                <TestTube className="w-5 h-5 mr-2" />
                Advanced Debug Tools
              </span>
              <Badge variant="secondary" className="text-xs">
                For developers
              </Badge>
            </CardTitle>
          </CardHeader>
          <CardContent>
            <Tabs defaultValue="messages" className="space-y-6">
              <TabsList>
                <TabsTrigger value="messages">Messages</TabsTrigger>
                <TabsTrigger value="statistics">Statistics</TabsTrigger>
                <TabsTrigger value="controls">Controls</TabsTrigger>
              </TabsList>

        {/* Messages Tab */}
        <TabsContent value="messages" className="space-y-4">
          {/* Search and Filters */}
          <div className="flex space-x-4">
            <div className="flex-1">
              <div className="relative">
                <Search className="absolute left-3 top-3 h-4 w-4 text-gray-400" />
                <Input
                  placeholder="Search messages..."
                  value={searchTerm}
                  onChange={(e) => setSearchTerm(e.target.value)}
                  className="pl-10"
                />
              </div>
            </div>
            
            <select 
              value={filterDirection}
              onChange={(e) => setFilterDirection(e.target.value as any)}
              className="px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
            >
              <option value="all">All Messages</option>
              <option value="inbound">Inbound Only</option>
              <option value="outbound">Outbound Only</option>
            </select>
          </div>

          {/* Messages List */}
          <Card>
            <CardHeader className="flex flex-row items-center justify-between">
              <CardTitle className="flex items-center">
                <MessageSquare className="w-5 h-5 mr-2" />
                Messages ({filteredMessages.length})
              </CardTitle>
              <div className="flex space-x-2">
                <Button onClick={() => loadMessageHistory()} variant="outline" size="sm">
                  <RefreshCw className="w-4 h-4 mr-1" />
                  Refresh
                </Button>
              </div>
            </CardHeader>
            <CardContent>
              {filteredMessages.length === 0 ? (
                <div className="text-center py-8 text-gray-500">
                  <MessageSquare className="w-12 h-12 mx-auto mb-4 text-gray-300" />
                  <p>No messages found</p>
                  <p className="text-sm">
                    {messages.length === 0 
                      ? "Connect to start receiving debug messages"
                      : "Try adjusting your search or filter criteria"
                    }
                  </p>
                </div>
              ) : (
                <div className="space-y-2 max-h-96 overflow-y-auto">
                  {filteredMessages.map((message, index) => (
                    <div 
                      key={index}
                      className="flex items-center justify-between p-3 bg-gray-50 rounded-lg hover:bg-gray-100 cursor-pointer transition-colors"
                      onClick={() => {
                        setSelectedMessage(message);
                        setShowModal(true);
                      }}
                    >
                      <div className="flex-1">
                        <div className="flex items-center space-x-2">
                          {message.direction === 'INBOUND' ? (
                            <ArrowDown className="w-4 h-4 text-blue-500" />
                          ) : (
                            <ArrowUp className="w-4 h-4 text-green-500" />
                          )}
                          <Badge variant="outline" className="text-xs">
                            {message.messageType || 'Unknown'}
                          </Badge>
                          <span className="text-sm text-gray-500">
                            <Clock className="w-3 h-3 inline mr-1" />
                            {formatTimestamp(message.timestamp)}
                          </span>
                        </div>
                        <p className="text-sm mt-1 text-gray-700">
                          {message.summary || 'No summary available'}
                        </p>
                      </div>
                      <Badge className={message.direction === 'INBOUND' ? 'bg-blue-100 text-blue-800' : 'bg-green-100 text-green-800'}>
                        {message.direction}
                      </Badge>
                    </div>
                  ))}
                </div>
              )}
            </CardContent>
          </Card>
        </TabsContent>

        {/* Statistics Tab */}
        <TabsContent value="statistics" className="space-y-4">
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4">
            <Card>
              <CardContent className="p-4">
                <div className="flex items-center justify-between">
                  <div>
                    <p className="text-sm text-gray-600">Total Messages</p>
                    <p className="text-2xl font-bold">{statistics?.totalMessages || 0}</p>
                  </div>
                  <MessageSquare className="w-8 h-8 text-blue-500" />
                </div>
              </CardContent>
            </Card>

            <Card>
              <CardContent className="p-4">
                <div className="flex items-center justify-between">
                  <div>
                    <p className="text-sm text-gray-600">Cached Messages</p>
                    <p className="text-2xl font-bold">{statistics?.cachedMessages || 0}</p>
                  </div>
                  <Activity className="w-8 h-8 text-green-500" />
                </div>
              </CardContent>
            </Card>

            <Card>
              <CardContent className="p-4">
                <div className="flex items-center justify-between">
                  <div>
                    <p className="text-sm text-gray-600">Messages/Sec</p>
                    <p className="text-2xl font-bold">{statistics?.messagesPerSecond?.toFixed(2) || '0.00'}</p>
                  </div>
                  <RefreshCw className="w-8 h-8 text-purple-500" />
                </div>
              </CardContent>
            </Card>

            <Card>
              <CardContent className="p-4">
                <div className="flex items-center justify-between">
                  <div>
                    <p className="text-sm text-gray-600">Connection</p>
                    <p className="text-lg font-bold">
                      {isConnected ? (
                        <span className="text-green-600">Connected</span>
                      ) : (
                        <span className="text-red-600">Disconnected</span>
                      )}
                    </p>
                  </div>
                  {isConnected ? (
                    <CheckCircle className="w-8 h-8 text-green-500" />
                  ) : (
                    <XCircle className="w-8 h-8 text-red-500" />
                  )}
                </div>
              </CardContent>
            </Card>
          </div>

          {statistics && (
            <Card>
              <CardHeader>
                <CardTitle>Performance Metrics</CardTitle>
              </CardHeader>
              <CardContent className="space-y-4">
                <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                  <div>
                    <p className="text-sm text-gray-600 mb-1">Start Time</p>
                    <p className="text-sm">{new Date(statistics.startTime).toLocaleString()}</p>
                  </div>
                  <div>
                    <p className="text-sm text-gray-600 mb-1">Current Time</p>
                    <p className="text-sm">{new Date(statistics.currentTime).toLocaleString()}</p>
                  </div>
                </div>
              </CardContent>
            </Card>
          )}
        </TabsContent>

        {/* Controls Tab */}
        <TabsContent value="controls" className="space-y-4">
          <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
            <Card>
              <CardHeader>
                <CardTitle>Message Management</CardTitle>
              </CardHeader>
              <CardContent className="space-y-4">
                <Button 
                  onClick={handleClearMessages}
                  variant="destructive"
                  className="w-full"
                  disabled={!isConnected}
                >
                  <Trash2 className="w-4 h-4 mr-2" />
                  Clear All Messages
                </Button>
                
                <Button 
                  onClick={handleExportMessages}
                  variant="outline"
                  className="w-full"
                  disabled={messages.length === 0}
                >
                  <Download className="w-4 h-4 mr-2" />
                  Export Messages
                </Button>
              </CardContent>
            </Card>

            <Card>
              <CardHeader>
                <CardTitle>Connection Management</CardTitle>
              </CardHeader>
              <CardContent className="space-y-4">
                <Button 
                  onClick={handleTestConnection}
                  variant="outline"
                  className="w-full"
                >
                  <TestTube className="w-4 h-4 mr-2" />
                  Test Connection
                </Button>
                
                <Button 
                  onClick={loadStatistics}
                  variant="outline"
                  className="w-full"
                >
                  <RefreshCw className="w-4 h-4 mr-2" />
                  Refresh Statistics
                </Button>
              </CardContent>
            </Card>
          </div>
        </TabsContent>
            </Tabs>
          </CardContent>
        </Card>
      )}

      {/* Message Detail Modal */}
      {showModal && selectedMessage && (
        <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50">
          <div className="bg-white rounded-lg p-6 max-w-3xl max-h-[80vh] overflow-y-auto">
            <div className="flex justify-between items-center mb-4">
              <h3 className="text-lg font-semibold">Message Details</h3>
              <Button onClick={() => setShowModal(false)} variant="outline" size="sm">
                Close
              </Button>
            </div>
            
            <div className="space-y-4">
              <div className="grid grid-cols-2 gap-4">
                <div>
                  <label className="text-sm text-gray-600">Direction</label>
                  <p className="font-medium">{selectedMessage.direction}</p>
                </div>
                <div>
                  <label className="text-sm text-gray-600">Type</label>
                  <p className="font-medium">{selectedMessage.messageType || 'Unknown'}</p>
                </div>
                <div>
                  <label className="text-sm text-gray-600">Timestamp</label>
                  <p className="font-medium">{new Date(selectedMessage.timestamp).toLocaleString()}</p>
                </div>
                <div>
                  <label className="text-sm text-gray-600">Size</label>
                  <p className="font-medium">{selectedMessage.sizeBytes || 0} bytes</p>
                </div>
              </div>
              
              <div>
                <label className="text-sm text-gray-600">Summary</label>
                <p className="font-medium">{selectedMessage.summary || 'No summary available'}</p>
              </div>
              
              <div>
                <label className="text-sm text-gray-600">Content</label>
                <pre className="bg-gray-100 p-4 rounded text-xs overflow-x-auto">
                  {JSON.stringify(selectedMessage.jsonContent ? JSON.parse(selectedMessage.jsonContent) : {}, null, 2)}
                </pre>
              </div>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}