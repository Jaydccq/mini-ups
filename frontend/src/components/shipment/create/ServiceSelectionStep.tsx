import React, { useState } from 'react';
import { UseFormReturn } from 'react-hook-form';
import { Truck, Clock, DollarSign, Shield, Info } from 'lucide-react';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
import { Alert, AlertDescription } from '@/components/ui/alert';
import { Tabs, TabsContent, TabsList, TabsTrigger } from '@/components/ui/tabs';
import { Badge } from '@/components/ui/badge';
import { CreateShipmentFormData } from '@/types/shipment-form';
import { ServiceSelector } from './ServiceSelector';
import { TimeWindowSelector } from './TimeWindowSelector';

interface ServiceSelectionStepProps {
  form: UseFormReturn<CreateShipmentFormData>;
}

export const ServiceSelectionStep: React.FC<ServiceSelectionStepProps> = ({ form }) => {
  const {
    watch,
    setValue,
    formState: { errors },
  } = form;

  const [selectedTimeWindow, setSelectedTimeWindow] = useState<{
    pickupDate?: Date;
    pickupTimeSlot?: string;
    deliveryDate?: Date;
    deliveryTimeSlot?: string;
  }>({});

  const formData = watch();
  const {
    deliverySpeed,
    packageWeight,
    packageLength,
    packageWidth,
    packageHeight,
    packageValue,
    recipientAddress,
    senderAddress,
  } = formData;

  const dimensions = packageLength && packageWidth && packageHeight 
    ? { length: packageLength, width: packageWidth, height: packageHeight }
    : undefined;

  const handleServiceSelect = (service: string) => {
    setValue('deliverySpeed', service);
  };

  const handleTimeWindowSelect = (timeWindow: {
    pickupDate?: Date;
    pickupTimeSlot?: string;
    deliveryDate?: Date;
    deliveryTimeSlot?: string;
  }) => {
    setSelectedTimeWindow(timeWindow);
    // You can store these in form data or handle them separately
    // For now, we'll just store them in local state
  };

  const calculateEstimatedPrice = () => {
    let basePrice = 12.99;
    
    // Service multipliers
    const multipliers = {
      'STANDARD': 1.0,
      'EXPRESS': 1.6,
      'OVERNIGHT': 2.9,
    };
    
    if (deliverySpeed && multipliers[deliverySpeed as keyof typeof multipliers]) {
      basePrice *= multipliers[deliverySpeed as keyof typeof multipliers];
    }
    
    // Weight surcharge
    if (packageWeight && packageWeight > 1) {
      basePrice += (packageWeight - 1) * 2.5;
    }
    
    // Value-based insurance
    if (packageValue && packageValue > 100) {
      basePrice += packageValue * 0.01;
    }
    
    return basePrice;
  };

  const getServiceFeatures = (service: string) => {
    const features = {
      'STANDARD': [
        'Basic tracking',
        'Delivery confirmation',
        'Up to $100 insurance',
        '5-7 business days',
      ],
      'EXPRESS': [
        'Real-time tracking',
        'Priority handling',
        'Up to $300 insurance',
        '2-3 business days',
        'SMS notifications',
      ],
      'OVERNIGHT': [
        'Next day delivery',
        'Premium tracking',
        'Up to $500 insurance',
        '1 business day',
        'Signature required',
        'Priority customer support',
      ],
    };
    
    return features[service as keyof typeof features] || [];
  };

  const estimatedPrice = calculateEstimatedPrice();

  return (
    <div className="space-y-6">
      <Alert>
        <Truck className="h-4 w-4" />
        <AlertDescription>
          Choose your shipping service and schedule. Pricing is calculated based on your package details.
        </AlertDescription>
      </Alert>

      <Tabs defaultValue="service" className="w-full">
        <TabsList className="grid w-full grid-cols-3">
          <TabsTrigger value="service">Service Level</TabsTrigger>
          <TabsTrigger value="schedule">Schedule</TabsTrigger>
          <TabsTrigger value="summary">Summary</TabsTrigger>
        </TabsList>
        
        <TabsContent value="service" className="space-y-4">
          <ServiceSelector
            weight={packageWeight || 1}
            dimensions={dimensions}
            value={packageValue || 0}
            onServiceSelect={handleServiceSelect}
            currentService={deliverySpeed}
            origin={senderAddress ? 'Sender Location' : 'Origin'}
            destination={recipientAddress ? 'Recipient Location' : 'Destination'}
          />
        </TabsContent>
        
        <TabsContent value="schedule" className="space-y-4">
          {deliverySpeed ? (
            <TimeWindowSelector
              serviceType={deliverySpeed}
              onTimeWindowSelect={handleTimeWindowSelect}
              currentSelection={selectedTimeWindow}
            />
          ) : (
            <Alert>
              <Info className="h-4 w-4" />
              <AlertDescription>
                Please select a service level first to schedule your shipment.
              </AlertDescription>
            </Alert>
          )}
        </TabsContent>
        
        <TabsContent value="summary" className="space-y-4">
          <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
            {/* Service Summary */}
            <Card>
              <CardHeader>
                <CardTitle className="flex items-center gap-2">
                  <Truck className="h-5 w-5" />
                  Selected Service
                </CardTitle>
              </CardHeader>
              <CardContent>
                {deliverySpeed ? (
                  <div className="space-y-4">
                    <div className="flex items-center justify-between">
                      <div>
                        <div className="font-semibold text-lg">
                          {deliverySpeed.charAt(0) + deliverySpeed.slice(1).toLowerCase()} Shipping
                        </div>
                        <div className="text-sm text-gray-600">
                          {deliverySpeed === 'STANDARD' && '5-7 business days'}
                          {deliverySpeed === 'EXPRESS' && '2-3 business days'}
                          {deliverySpeed === 'OVERNIGHT' && '1 business day'}
                        </div>
                      </div>
                      <div className="text-right">
                        <div className="text-2xl font-bold text-blue-600">
                          ${estimatedPrice.toFixed(2)}
                        </div>
                        <div className="text-sm text-gray-600">
                          Estimated cost
                        </div>
                      </div>
                    </div>
                    
                    <div>
                      <h4 className="font-medium mb-2">Included Features</h4>
                      <div className="flex flex-wrap gap-1">
                        {getServiceFeatures(deliverySpeed).map((feature, index) => (
                          <Badge key={index} variant="secondary" className="text-xs">
                            {feature}
                          </Badge>
                        ))}
                      </div>
                    </div>
                  </div>
                ) : (
                  <div className="text-center py-8 text-gray-500">
                    No service selected
                  </div>
                )}
              </CardContent>
            </Card>

            {/* Schedule Summary */}
            <Card>
              <CardHeader>
                <CardTitle className="flex items-center gap-2">
                  <Clock className="h-5 w-5" />
                  Schedule Summary
                </CardTitle>
              </CardHeader>
              <CardContent>
                <div className="space-y-4">
                  <div>
                    <h4 className="font-medium text-sm mb-2">Pickup</h4>
                    {selectedTimeWindow.pickupDate ? (
                      <div className="text-sm">
                        <div className="font-medium">
                          {selectedTimeWindow.pickupDate.toLocaleDateString('en-US', {
                            weekday: 'long',
                            year: 'numeric',
                            month: 'long',
                            day: 'numeric',
                          })}
                        </div>
                        {selectedTimeWindow.pickupTimeSlot && (
                          <div className="text-gray-600">
                            Time: {selectedTimeWindow.pickupTimeSlot}
                          </div>
                        )}
                      </div>
                    ) : (
                      <div className="text-sm text-gray-500">
                        Not scheduled
                      </div>
                    )}
                  </div>
                  
                  <div>
                    <h4 className="font-medium text-sm mb-2">Delivery</h4>
                    {selectedTimeWindow.deliveryDate ? (
                      <div className="text-sm">
                        <div className="font-medium">
                          {selectedTimeWindow.deliveryDate.toLocaleDateString('en-US', {
                            weekday: 'long',
                            year: 'numeric',
                            month: 'long',
                            day: 'numeric',
                          })}
                        </div>
                        {selectedTimeWindow.deliveryTimeSlot && (
                          <div className="text-gray-600">
                            Time: {selectedTimeWindow.deliveryTimeSlot}
                          </div>
                        )}
                      </div>
                    ) : (
                      <div className="text-sm text-gray-500">
                        Not scheduled
                      </div>
                    )}
                  </div>
                </div>
              </CardContent>
            </Card>
          </div>

          {/* Cost Breakdown */}
          <Card>
            <CardHeader>
              <CardTitle className="flex items-center gap-2">
                <DollarSign className="h-5 w-5" />
                Cost Breakdown
              </CardTitle>
            </CardHeader>
            <CardContent>
              <div className="space-y-2">
                <div className="flex justify-between text-sm">
                  <span>Base shipping cost:</span>
                  <span className="font-medium">$12.99</span>
                </div>
                {deliverySpeed === 'EXPRESS' && (
                  <div className="flex justify-between text-sm">
                    <span>Express surcharge:</span>
                    <span className="font-medium">+$8.00</span>
                  </div>
                )}
                {deliverySpeed === 'OVERNIGHT' && (
                  <div className="flex justify-between text-sm">
                    <span>Overnight surcharge:</span>
                    <span className="font-medium">+$25.00</span>
                  </div>
                )}
                {packageWeight && packageWeight > 1 && (
                  <div className="flex justify-between text-sm">
                    <span>Weight surcharge:</span>
                    <span className="font-medium">+${((packageWeight - 1) * 2.5).toFixed(2)}</span>
                  </div>
                )}
                {packageValue && packageValue > 100 && (
                  <div className="flex justify-between text-sm">
                    <span>Additional insurance:</span>
                    <span className="font-medium">+${(packageValue * 0.01).toFixed(2)}</span>
                  </div>
                )}
                <div className="border-t pt-2">
                  <div className="flex justify-between text-base font-medium">
                    <span>Estimated Total:</span>
                    <span>${estimatedPrice.toFixed(2)}</span>
                  </div>
                </div>
              </div>
              <p className="text-xs text-gray-500 mt-2">
                Final pricing will be calculated at confirmation based on exact route and current rates.
              </p>
            </CardContent>
          </Card>
        </TabsContent>
      </Tabs>

      {/* Validation Errors */}
      {errors.deliverySpeed && (
        <Alert variant="destructive">
          <AlertDescription>
            {errors.deliverySpeed.message}
          </AlertDescription>
        </Alert>
      )}
    </div>
  );
};