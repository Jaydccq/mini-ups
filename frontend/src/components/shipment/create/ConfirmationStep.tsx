import React from 'react';
import { UseFormReturn } from 'react-hook-form';
import { CheckCircle, User, Navigation, Package, DollarSign, AlertTriangle, Clock, MapPin, Truck, Shield, Edit2 } from 'lucide-react';
import { Label } from '@/components/ui/label';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
import { Badge } from '@/components/ui/badge';
import { Alert, AlertDescription } from '@/components/ui/alert';
import { Button } from '@/components/ui/button';
import { Progress } from '@/components/ui/progress';
import { CreateShipmentFormData } from '@/types/shipment-form';

interface ConfirmationStepProps {
  form: UseFormReturn<CreateShipmentFormData>;
}

export const ConfirmationStep: React.FC<ConfirmationStepProps> = ({ form }) => {
  const {
    register,
    watch,
    formState: { errors },
  } = form;

  const formData = watch();
  
  const getServiceDetails = (speed: string) => {
    switch (speed) {
      case 'OVERNIGHT': 
        return {
          name: 'Overnight Delivery',
          days: '1 business day',
          description: 'Next business day delivery with premium tracking',
          features: ['Premium tracking', 'Signature required', 'Up to $500 insurance'],
          price: 37.99,
          icon: '⚡',
        };
      case 'EXPRESS': 
        return {
          name: 'Express Delivery',
          days: '2-3 business days',
          description: 'Fast delivery with enhanced tracking',
          features: ['Real-time tracking', 'SMS notifications', 'Up to $300 insurance'],
          price: 20.99,
          icon: '🚚',
        };
      case 'STANDARD': 
        return {
          name: 'Standard Delivery',
          days: '5-7 business days',
          description: 'Reliable and economical shipping',
          features: ['Basic tracking', 'Delivery confirmation', 'Up to $100 insurance'],
          price: 12.99,
          icon: '📦',
        };
      default: 
        return {
          name: 'Standard Delivery',
          days: '5-7 business days',
          description: 'Reliable and economical shipping',
          features: ['Basic tracking', 'Delivery confirmation', 'Up to $100 insurance'],
          price: 12.99,
          icon: '📦',
        };
    }
  };
  
  const calculateDetailedPricing = () => {
    const service = getServiceDetails(formData.deliverySpeed);
    const basePrice = service.price;
    const weightSurcharge = formData.packageWeight > 1 ? (formData.packageWeight - 1) * 2.5 : 0;
    const valueSurcharge = formData.packageValue > 100 ? formData.packageValue * 0.01 : 0;
    const volume = formData.packageLength * formData.packageWidth * formData.packageHeight;
    const dimensionalSurcharge = volume > 50000 ? 15 : 0;
    
    const total = basePrice + weightSurcharge + valueSurcharge + dimensionalSurcharge;
    
    return {
      basePrice,
      weightSurcharge,
      valueSurcharge,
      dimensionalSurcharge,
      total,
    };
  };
  
  const serviceDetails = getServiceDetails(formData.deliverySpeed);
  const pricing = calculateDetailedPricing();
  
  const generateTrackingNumber = () => {
    return `UPS${Date.now().toString().slice(-8)}`;
  };
  
  const getDeliveryDate = () => {
    const now = new Date();
    const deliveryDate = new Date(now);
    
    switch (formData.deliverySpeed) {
      case 'OVERNIGHT':
        deliveryDate.setDate(now.getDate() + 1);
        break;
      case 'EXPRESS':
        deliveryDate.setDate(now.getDate() + 2);
        break;
      case 'STANDARD':
        deliveryDate.setDate(now.getDate() + 5);
        break;
    }
    
    return deliveryDate.toLocaleDateString('en-US', {
      weekday: 'long',
      year: 'numeric',
      month: 'long',
      day: 'numeric',
    });
  };

  const getDeliverySpeedLabel = (speed: string) => {
    switch (speed) {
      case 'OVERNIGHT': return 'Overnight Delivery';
      case 'EXPRESS': return 'Express Delivery';
      case 'STANDARD': return 'Standard Delivery';
      default: return speed;
    }
  };

  const getDeliverySpeedBadge = (speed: string) => {
    switch (speed) {
      case 'OVERNIGHT': return 'destructive';
      case 'EXPRESS': return 'default';
      case 'STANDARD': return 'secondary';
      default: return 'outline';
    }
  };

  const estimatedTotal = (
    12.99 +
    (formData.deliverySpeed === 'EXPRESS' ? 8 : 0) +
    (formData.deliverySpeed === 'OVERNIGHT' ? 25 : 0) +
    (formData.packageWeight > 5 ? (formData.packageWeight - 5) * 2 : 0)
  ).toFixed(2);

  return (
    <div className="space-y-6">
      {/* Header with Progress */}
      <Card>
        <CardHeader>
          <CardTitle className="flex items-center gap-2 text-xl">
            <CheckCircle className="h-6 w-6 text-green-500" />
            Order Review & Confirmation
          </CardTitle>
          <CardDescription>
            Review all information and confirm your shipment details
          </CardDescription>
        </CardHeader>
        <CardContent>
          <div className="grid grid-cols-1 md:grid-cols-3 gap-4 mb-4">
            <div className="text-center">
              <div className="text-2xl font-bold text-blue-600">
                {generateTrackingNumber()}
              </div>
              <div className="text-sm text-gray-600">Tracking Number</div>
            </div>
            <div className="text-center">
              <div className="text-2xl font-bold text-green-600">
                ${pricing.total.toFixed(2)}
              </div>
              <div className="text-sm text-gray-600">Total Cost</div>
            </div>
            <div className="text-center">
              <div className="text-2xl font-bold text-purple-600">
                {serviceDetails.days}
              </div>
              <div className="text-sm text-gray-600">Delivery Time</div>
            </div>
          </div>
          <Progress value={100} className="h-2" />
          <div className="text-center mt-2 text-sm text-gray-600">
            Ready to create shipment
          </div>
        </CardContent>
      </Card>
      
      {/* Service Summary */}
      <Card>
        <CardHeader>
          <CardTitle className="flex items-center gap-2 text-lg">
            <Truck className="h-5 w-5" />
            Selected Service
          </CardTitle>
        </CardHeader>
        <CardContent>
          <div className="flex items-start gap-4">
            <div className="text-4xl">{serviceDetails.icon}</div>
            <div className="flex-1">
              <div className="flex items-center gap-2 mb-2">
                <h3 className="text-lg font-semibold">{serviceDetails.name}</h3>
                <Badge variant="outline">{serviceDetails.days}</Badge>
              </div>
              <p className="text-gray-600 mb-3">{serviceDetails.description}</p>
              <div className="grid grid-cols-2 md:grid-cols-3 gap-2">
                {serviceDetails.features.map((feature, index) => (
                  <div key={index} className="flex items-center gap-1 text-sm">
                    <CheckCircle className="h-3 w-3 text-green-500" />
                    {feature}
                  </div>
                ))}
              </div>
            </div>
            <div className="text-right">
              <div className="text-2xl font-bold text-blue-600">
                ${pricing.total.toFixed(2)}
              </div>
              <div className="text-sm text-gray-600">Estimated delivery</div>
              <div className="text-sm font-medium">{getDeliveryDate()}</div>
            </div>
          </div>
        </CardContent>
      </Card>

      <div className="grid gap-6 lg:grid-cols-2">
        {/* Sender Information */}
        <Card>
          <CardHeader>
            <CardTitle className="flex items-center justify-between text-lg">
              <div className="flex items-center gap-2">
                <User className="h-5 w-5" />
                Sender Information
              </div>
              <Button variant="ghost" size="sm">
                <Edit2 className="h-4 w-4" />
              </Button>
            </CardTitle>
          </CardHeader>
          <CardContent className="space-y-3">
            <div className="p-3 bg-blue-50 rounded-lg">
              <div className="flex items-center gap-2 mb-2">
                <MapPin className="h-4 w-4 text-blue-600" />
                <span className="text-sm font-medium">From</span>
              </div>
              <div className="text-sm">
                <p className="font-medium">{formData.senderName}</p>
                <p className="text-gray-600">{formData.senderEmail}</p>
                <p className="text-gray-600">{formData.senderPhone}</p>
                <p className="text-gray-600">{formData.senderAddress}</p>
              </div>
            </div>
          </CardContent>
        </Card>

        {/* Recipient Information */}
        <Card>
          <CardHeader>
            <CardTitle className="flex items-center justify-between text-lg">
              <div className="flex items-center gap-2">
                <Navigation className="h-5 w-5" />
                Recipient Information
              </div>
              <Button variant="ghost" size="sm">
                <Edit2 className="h-4 w-4" />
              </Button>
            </CardTitle>
          </CardHeader>
          <CardContent className="space-y-3">
            <div className="p-3 bg-green-50 rounded-lg">
              <div className="flex items-center gap-2 mb-2">
                <MapPin className="h-4 w-4 text-green-600" />
                <span className="text-sm font-medium">To</span>
              </div>
              <div className="text-sm">
                <p className="font-medium">{formData.recipientName}</p>
                <p className="text-gray-600">{formData.recipientEmail}</p>
                <p className="text-gray-600">{formData.recipientPhone}</p>
                <p className="text-gray-600">{formData.recipientAddress}</p>
                <div className="mt-2 pt-2 border-t border-green-200">
                  <p className="text-gray-600">
                    <strong>Coordinates:</strong> ({formData.destinationX}, {formData.destinationY})
                  </p>
                </div>
              </div>
            </div>
          </CardContent>
        </Card>
      </div>

      {/* Package Information */}
      <Card>
        <CardHeader>
          <CardTitle className="flex items-center justify-between text-lg">
            <div className="flex items-center gap-2">
              <Package className="h-5 w-5" />
              Package Details
            </div>
            <Button variant="ghost" size="sm">
              <Edit2 className="h-4 w-4" />
            </Button>
          </CardTitle>
        </CardHeader>
        <CardContent>
          <div className="grid gap-4 md:grid-cols-3">
            <div className="space-y-3">
              <div>
                <p className="text-sm font-medium text-gray-700">Description</p>
                <p className="text-sm">{formData.packageDescription}</p>
              </div>
              <div>
                <p className="text-sm font-medium text-gray-700">Dimensions (L × W × H)</p>
                <p className="text-sm font-mono">
                  {formData.packageLength} × {formData.packageWidth} × {formData.packageHeight} cm
                </p>
                <p className="text-xs text-gray-500">
                  Volume: {((formData.packageLength * formData.packageWidth * formData.packageHeight) / 1000).toFixed(1)}L
                </p>
              </div>
            </div>
            <div className="space-y-3">
              <div>
                <p className="text-sm font-medium text-gray-700">Weight</p>
                <p className="text-sm font-mono">{formData.packageWeight} kg</p>
              </div>
              <div>
                <p className="text-sm font-medium text-gray-700">Package Value</p>
                <p className="text-sm font-mono">${formData.packageValue}</p>
              </div>
            </div>
            <div className="space-y-3">
              <div>
                <p className="text-sm font-medium text-gray-700">Insurance Coverage</p>
                <div className="flex items-center gap-2">
                  <Shield className="h-4 w-4 text-blue-500" />
                  <p className="text-sm">${serviceDetails.features.find(f => f.includes('insurance'))?.match(/\$\d+/)?.[0] || '$100'}</p>
                </div>
              </div>
              {formData.specialInstructions && (
                <div>
                  <p className="text-sm font-medium text-gray-700">Special Instructions</p>
                  <p className="text-sm text-gray-600 italic">{formData.specialInstructions}</p>
                </div>
              )}
            </div>
          </div>
        </CardContent>
      </Card>

      {/* Detailed Pricing Summary */}
      <Card>
        <CardHeader>
          <CardTitle className="flex items-center gap-2 text-lg">
            <DollarSign className="h-5 w-5" />
            Detailed Pricing Breakdown
          </CardTitle>
        </CardHeader>
        <CardContent>
          <div className="space-y-3">
            <div className="flex justify-between text-sm">
              <span className="flex items-center gap-2">
                <Truck className="h-4 w-4" />
                Base service ({serviceDetails.name})
              </span>
              <span className="font-medium">${pricing.basePrice.toFixed(2)}</span>
            </div>
            
            {pricing.weightSurcharge > 0 && (
              <div className="flex justify-between text-sm">
                <span className="flex items-center gap-2">
                  <Package className="h-4 w-4" />
                  Weight surcharge ({formData.packageWeight}kg)
                </span>
                <span className="font-medium">+${pricing.weightSurcharge.toFixed(2)}</span>
              </div>
            )}
            
            {pricing.valueSurcharge > 0 && (
              <div className="flex justify-between text-sm">
                <span className="flex items-center gap-2">
                  <Shield className="h-4 w-4" />
                  Additional insurance
                </span>
                <span className="font-medium">+${pricing.valueSurcharge.toFixed(2)}</span>
              </div>
            )}
            
            {pricing.dimensionalSurcharge > 0 && (
              <div className="flex justify-between text-sm">
                <span className="flex items-center gap-2">
                  <Package className="h-4 w-4" />
                  Large package fee
                </span>
                <span className="font-medium">+${pricing.dimensionalSurcharge.toFixed(2)}</span>
              </div>
            )}
            
            <div className="border-t pt-3">
              <div className="flex justify-between text-lg font-bold">
                <span>Total Cost:</span>
                <span className="text-blue-600">${pricing.total.toFixed(2)}</span>
              </div>
              <p className="text-xs text-gray-500 mt-1">
                Final pricing confirmed upon package pickup
              </p>
            </div>
          </div>
        </CardContent>
      </Card>

      {/* Terms and Conditions */}
      <Card>
        <CardHeader>
          <CardTitle className="text-lg">Terms and Conditions</CardTitle>
        </CardHeader>
        <CardContent className="space-y-4">
          <div className="flex items-start space-x-2">
            <input
              id="termsAccepted"
              type="checkbox"
              {...register('termsAccepted')}
              className="mt-1 h-4 w-4 text-blue-600 focus:ring-blue-500 border-gray-300 rounded"
            />
            <div>
              <Label htmlFor="termsAccepted" className="text-sm font-medium cursor-pointer">
                I accept the Terms and Conditions *
              </Label>
              <p className="text-xs text-gray-500 mt-1">
                By checking this box, you agree to our shipping terms, conditions, and privacy policy.
              </p>
            </div>
          </div>
          {errors.termsAccepted && (
            <p className="text-sm text-red-600">{errors.termsAccepted.message}</p>
          )}

          <div className="flex items-start space-x-2">
            <input
              id="marketingOptIn"
              type="checkbox"
              {...register('marketingOptIn')}
              className="mt-1 h-4 w-4 text-blue-600 focus:ring-blue-500 border-gray-300 rounded"
            />
            <div>
              <Label htmlFor="marketingOptIn" className="text-sm font-medium cursor-pointer">
                I would like to receive marketing emails (Optional)
              </Label>
              <p className="text-xs text-gray-500 mt-1">
                Get updates about new services, promotions, and shipping tips.
              </p>
            </div>
          </div>
        </CardContent>
      </Card>

      {/* Final Warning */}
      <Alert>
        <AlertTriangle className="h-4 w-4" />
        <AlertDescription>
          <strong>Important:</strong> Once you create this shipment, you will not be able to modify 
          the recipient information or package details. Please double-check all information above.
        </AlertDescription>
      </Alert>
    </div>
  );
};