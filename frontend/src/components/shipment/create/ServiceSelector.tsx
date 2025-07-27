import React, { useState, useEffect } from 'react';
import { Clock, Truck, Shield, Star, Calculator, Info, CheckCircle } from 'lucide-react';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
import { Badge } from '@/components/ui/badge';
import { Button } from '@/components/ui/button';
import { Alert, AlertDescription } from '@/components/ui/alert';
import { Progress } from '@/components/ui/progress';
import { Tabs, TabsContent, TabsList, TabsTrigger } from '@/components/ui/tabs';

interface ServiceSelectorProps {
  weight: number;
  dimensions?: { length: number; width: number; height: number };
  value: number;
  distance?: number;
  onServiceSelect: (service: string) => void;
  currentService?: string;
  origin?: string;
  destination?: string;
}

interface ServiceOption {
  id: string;
  name: string;
  description: string;
  estimatedDays: { min: number; max: number };
  baseCost: number;
  icon: string;
  features: string[];
  restrictions: string[];
  reliability: number;
  tracking: 'basic' | 'enhanced' | 'premium';
  insuranceIncluded: number;
  signature: boolean;
}

const SERVICE_OPTIONS: ServiceOption[] = [
  {
    id: 'STANDARD',
    name: 'Standard Shipping',
    description: 'Reliable and economical shipping for non-urgent deliveries',
    estimatedDays: { min: 5, max: 7 },
    baseCost: 12.99,
    icon: '📦',
    features: ['Basic tracking', 'Delivery confirmation', 'Up to $100 insurance'],
    restrictions: ['Business days only', 'No signature required'],
    reliability: 85,
    tracking: 'basic',
    insuranceIncluded: 100,
    signature: false,
  },
  {
    id: 'EXPRESS',
    name: 'Express Shipping',
    description: 'Faster delivery with enhanced tracking and priority handling',
    estimatedDays: { min: 2, max: 3 },
    baseCost: 20.99,
    icon: '🚚',
    features: ['Real-time tracking', 'Priority handling', 'Up to $300 insurance', 'SMS notifications'],
    restrictions: ['Business days only', 'Signature confirmation available'],
    reliability: 92,
    tracking: 'enhanced',
    insuranceIncluded: 300,
    signature: true,
  },
  {
    id: 'OVERNIGHT',
    name: 'Overnight Delivery',
    description: 'Next business day delivery with premium service',
    estimatedDays: { min: 1, max: 1 },
    baseCost: 37.99,
    icon: '⚡',
    features: ['Next day delivery', 'Premium tracking', 'Up to $500 insurance', 'Priority customer support', 'Signature required'],
    restrictions: ['Business days only', 'Cut-off time: 6 PM'],
    reliability: 96,
    tracking: 'premium',
    insuranceIncluded: 500,
    signature: true,
  },
];

export const ServiceSelector: React.FC<ServiceSelectorProps> = ({
  weight,
  dimensions,
  value,
  distance = 500,
  onServiceSelect,
  currentService = 'STANDARD',
  origin = 'New York',
  destination = 'Los Angeles',
}) => {
  const [selectedService, setSelectedService] = useState<string>(currentService);
  const [priceBreakdown, setPriceBreakdown] = useState<{[key: string]: any}>({});

  const calculateServicePrice = (service: ServiceOption) => {
    let price = service.baseCost;
    
    // Weight-based pricing
    if (weight > 1) {
      price += (weight - 1) * 2.5;
    }
    
    // Dimensional weight pricing
    if (dimensions) {
      const dimWeight = (dimensions.length * dimensions.width * dimensions.height) / 5000;
      const billableWeight = Math.max(weight, dimWeight);
      if (billableWeight > weight) {
        price += (billableWeight - weight) * 1.5;
      }
    }
    
    // Distance-based pricing
    if (distance > 300) {
      price += (distance - 300) * 0.02;
    }
    
    // Value-based insurance adjustment
    if (value > service.insuranceIncluded) {
      price += (value - service.insuranceIncluded) * 0.01;
    }
    
    // Service-specific multipliers
    const multipliers = {
      'STANDARD': 1.0,
      'EXPRESS': 1.6,
      'OVERNIGHT': 2.9,
    };
    
    price *= multipliers[service.id as keyof typeof multipliers];
    
    return Math.round(price * 100) / 100;
  };

  const calculatePriceBreakdown = (service: ServiceOption) => {
    const basePrice = service.baseCost;
    const weightSurcharge = weight > 1 ? (weight - 1) * 2.5 : 0;
    const distanceSurcharge = distance > 300 ? (distance - 300) * 0.02 : 0;
    const insuranceSurcharge = value > service.insuranceIncluded ? (value - service.insuranceIncluded) * 0.01 : 0;
    
    let dimensionalSurcharge = 0;
    if (dimensions) {
      const dimWeight = (dimensions.length * dimensions.width * dimensions.height) / 5000;
      const billableWeight = Math.max(weight, dimWeight);
      if (billableWeight > weight) {
        dimensionalSurcharge = (billableWeight - weight) * 1.5;
      }
    }
    
    return {
      basePrice,
      weightSurcharge,
      distanceSurcharge,
      insuranceSurcharge,
      dimensionalSurcharge,
      total: calculateServicePrice(service),
    };
  };

  const handleServiceSelect = (serviceId: string) => {
    setSelectedService(serviceId);
    onServiceSelect(serviceId);
  };

  const getDeliveryDate = (service: ServiceOption) => {
    const now = new Date();
    const minDate = new Date(now);
    const maxDate = new Date(now);
    
    minDate.setDate(now.getDate() + service.estimatedDays.min);
    maxDate.setDate(now.getDate() + service.estimatedDays.max);
    
    const formatDate = (date: Date) => {
      return date.toLocaleDateString('en-US', { 
        weekday: 'short', 
        month: 'short', 
        day: 'numeric' 
      });
    };
    
    if (service.estimatedDays.min === service.estimatedDays.max) {
      return formatDate(minDate);
    } else {
      return `${formatDate(minDate)} - ${formatDate(maxDate)}`;
    }
  };

  const getSavings = (service: ServiceOption) => {
    const overnight = SERVICE_OPTIONS.find(s => s.id === 'OVERNIGHT')!;
    const overnightPrice = calculateServicePrice(overnight);
    const currentPrice = calculateServicePrice(service);
    const savings = overnightPrice - currentPrice;
    
    return savings > 0 ? savings : 0;
  };

  useEffect(() => {
    const breakdown = {};
    SERVICE_OPTIONS.forEach(service => {
      breakdown[service.id] = calculatePriceBreakdown(service);
    });
    setPriceBreakdown(breakdown);
  }, [weight, dimensions, value, distance, calculatePriceBreakdown]);

  return (
    <div className="space-y-6">
      <div className="text-center">
        <h3 className="text-lg font-semibold mb-2">Choose Your Shipping Service</h3>
        <p className="text-sm text-gray-600">
          From <strong>{origin}</strong> to <strong>{destination}</strong> • {distance} miles
        </p>
      </div>

      <Tabs defaultValue="services" className="w-full">
        <TabsList className="grid w-full grid-cols-2">
          <TabsTrigger value="services">Service Options</TabsTrigger>
          <TabsTrigger value="comparison">Price Comparison</TabsTrigger>
        </TabsList>
        
        <TabsContent value="services" className="space-y-4">
          <div className="space-y-4">
            {SERVICE_OPTIONS.map((service) => {
              const price = calculateServicePrice(service);
              const savings = getSavings(service);
              const isSelected = selectedService === service.id;
              
              return (
                <Card 
                  key={service.id} 
                  className={`cursor-pointer transition-all hover:shadow-md ${
                    isSelected ? 'ring-2 ring-blue-500 bg-blue-50' : ''
                  }`}
                  onClick={() => handleServiceSelect(service.id)}
                >
                  <CardHeader className="pb-2">
                    <div className="flex items-start justify-between">
                      <div className="flex items-center gap-3">
                        <div className="text-2xl">{service.icon}</div>
                        <div>
                          <CardTitle className="text-lg flex items-center gap-2">
                            {service.name}
                            {isSelected && <CheckCircle className="h-4 w-4 text-blue-500" />}
                          </CardTitle>
                          <CardDescription>{service.description}</CardDescription>
                        </div>
                      </div>
                      <div className="text-right">
                        <div className="text-2xl font-bold text-blue-600">
                          ${price.toFixed(2)}
                        </div>
                        {savings > 0 && (
                          <div className="text-sm text-green-600">
                            Save ${savings.toFixed(2)}
                          </div>
                        )}
                      </div>
                    </div>
                  </CardHeader>
                  <CardContent>
                    <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
                      <div>
                        <div className="flex items-center gap-2 text-sm font-medium mb-1">
                          <Clock className="h-4 w-4" />
                          Delivery Time
                        </div>
                        <div className="text-lg font-semibold text-gray-900">
                          {service.estimatedDays.min === service.estimatedDays.max 
                            ? `${service.estimatedDays.min} day${service.estimatedDays.min > 1 ? 's' : ''}`
                            : `${service.estimatedDays.min}-${service.estimatedDays.max} days`
                          }
                        </div>
                        <div className="text-sm text-gray-600">
                          {getDeliveryDate(service)}
                        </div>
                      </div>
                      
                      <div>
                        <div className="flex items-center gap-2 text-sm font-medium mb-1">
                          <Star className="h-4 w-4" />
                          Reliability
                        </div>
                        <div className="flex items-center gap-2">
                          <Progress value={service.reliability} className="h-2 flex-1" />
                          <span className="text-sm font-medium">{service.reliability}%</span>
                        </div>
                        <div className="text-sm text-gray-600">
                          On-time delivery rate
                        </div>
                      </div>
                      
                      <div>
                        <div className="flex items-center gap-2 text-sm font-medium mb-1">
                          <Shield className="h-4 w-4" />
                          Insurance
                        </div>
                        <div className="text-lg font-semibold text-gray-900">
                          ${service.insuranceIncluded}
                        </div>
                        <div className="text-sm text-gray-600">
                          Included coverage
                        </div>
                      </div>
                    </div>
                    
                    <div className="mt-4 pt-4 border-t">
                      <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                        <div>
                          <h4 className="font-medium text-sm mb-2">Features</h4>
                          <ul className="text-sm space-y-1">
                            {service.features.map((feature, index) => (
                              <li key={index} className="flex items-center gap-2">
                                <CheckCircle className="h-3 w-3 text-green-500" />
                                {feature}
                              </li>
                            ))}
                          </ul>
                        </div>
                        <div>
                          <h4 className="font-medium text-sm mb-2">Restrictions</h4>
                          <ul className="text-sm space-y-1">
                            {service.restrictions.map((restriction, index) => (
                              <li key={index} className="flex items-center gap-2">
                                <Info className="h-3 w-3 text-blue-500" />
                                {restriction}
                              </li>
                            ))}
                          </ul>
                        </div>
                      </div>
                    </div>
                  </CardContent>
                </Card>
              );
            })}
          </div>
        </TabsContent>
        
        <TabsContent value="comparison" className="space-y-4">
          <Card>
            <CardHeader>
              <CardTitle className="flex items-center gap-2">
                <Calculator className="h-5 w-5" />
                Price Breakdown Comparison
              </CardTitle>
            </CardHeader>
            <CardContent>
              <div className="overflow-x-auto">
                <table className="w-full text-sm">
                  <thead>
                    <tr className="border-b">
                      <th className="text-left p-2">Service</th>
                      <th className="text-right p-2">Base Price</th>
                      <th className="text-right p-2">Weight Fee</th>
                      <th className="text-right p-2">Distance Fee</th>
                      <th className="text-right p-2">Insurance</th>
                      <th className="text-right p-2">Dimensional</th>
                      <th className="text-right p-2 font-bold">Total</th>
                    </tr>
                  </thead>
                  <tbody>
                    {SERVICE_OPTIONS.map((service) => {
                      const breakdown = priceBreakdown[service.id];
                      if (!breakdown) return null;
                      
                      return (
                        <tr key={service.id} className="border-b hover:bg-gray-50">
                          <td className="p-2">
                            <div className="flex items-center gap-2">
                              <span>{service.icon}</span>
                              <span className="font-medium">{service.name}</span>
                            </div>
                          </td>
                          <td className="text-right p-2">${breakdown.basePrice.toFixed(2)}</td>
                          <td className="text-right p-2">${breakdown.weightSurcharge.toFixed(2)}</td>
                          <td className="text-right p-2">${breakdown.distanceSurcharge.toFixed(2)}</td>
                          <td className="text-right p-2">${breakdown.insuranceSurcharge.toFixed(2)}</td>
                          <td className="text-right p-2">${breakdown.dimensionalSurcharge.toFixed(2)}</td>
                          <td className="text-right p-2 font-bold">${breakdown.total.toFixed(2)}</td>
                        </tr>
                      );
                    })}
                  </tbody>
                </table>
              </div>
            </CardContent>
          </Card>
          
          <Alert>
            <Info className="h-4 w-4" />
            <AlertDescription>
              <strong>Pricing Details:</strong> Base price includes standard handling and delivery. 
              Additional fees apply for weight over 1kg, distance over 300 miles, and insurance 
              coverage above included amounts. All prices are estimates and may vary based on 
              final route optimization.
            </AlertDescription>
          </Alert>
        </TabsContent>
      </Tabs>
    </div>
  );
};