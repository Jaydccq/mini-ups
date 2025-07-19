import { useEffect } from 'react';
import { socketService } from '@/services/socketService';

export const useShipmentSubscription = (trackingNumber: string | undefined) => {
  useEffect(() => {
    if (!trackingNumber) return;

    // Subscribe when component mounts or trackingNumber changes
    socketService.subscribeToShipment(trackingNumber);

    // Cleanup: unsubscribe when component unmounts or trackingNumber changes
    return () => {
      socketService.unsubscribeFromShipment(trackingNumber);
    };
  }, [trackingNumber]);
};