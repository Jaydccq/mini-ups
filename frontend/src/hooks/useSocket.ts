import { useState, useEffect } from 'react';
import { socketService, SocketStatus } from '@/services/socketService';

export const useSocketStatus = () => {
  const [status, setStatus] = useState<SocketStatus>(socketService.getStatus());

  useEffect(() => {
    const unsubscribe = socketService.onStatusChange(setStatus);
    return unsubscribe;
  }, []);

  return {
    status,
    isConnected: status === 'connected',
    isConnecting: status === 'connecting',
    isDisconnected: status === 'disconnected',
    hasError: status === 'error',
  };
};