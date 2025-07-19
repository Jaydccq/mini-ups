import React from 'react';
import { UseFormReturn } from 'react-hook-form';
import { Package, Weight, Ruler, DollarSign, Truck, FileText } from 'lucide-react';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { Alert, AlertDescription } from '@/components/ui/alert';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
import { CreateShipmentFormData } from '@/types/shipment-form';
import { PackageSizeGuide } from './PackageSizeGuide';
import { WeightEstimator } from './WeightEstimator';
import { PackagingRecommendations } from './PackagingRecommendations';

interface PackageDetailsStepProps {
  form: UseFormReturn<CreateShipmentFormData>;
}

export const PackageDetailsStep: React.FC<PackageDetailsStepProps> = ({ form }) => {
  const {
    register,
    watch,
    setValue,
    formState: { errors },
  } = form;

  const handleSizeSelect = (dimensions: { length: number; width: number; height: number }, type: string) => {
    setValue('packageLength', dimensions.length);
    setValue('packageWidth', dimensions.width);
    setValue('packageHeight', dimensions.height);
  };

  const handleWeightSelect = (weight: number) => {
    setValue('packageWeight', weight);
  };

  const watchedValues = watch(['packageLength', 'packageWidth', 'packageHeight', 'packageWeight', 'deliverySpeed']);
  const [length, width, height, weight, deliverySpeed] = watchedValues;

  // Calculate volume
  const volume = length && width && height ? (length * width * height) / 1000 : 0; // in liters

  // Calculate estimated delivery time based on speed
  const getEstimatedDelivery = (speed: string) => {
    switch (speed) {
      case 'OVERNIGHT': return '1 business day';
      case 'EXPRESS': return '2-3 business days';
      case 'STANDARD': return '5-7 business days';
      default: return 'TBD';
    }
  };

  return (
    <div className="space-y-6">
      <Alert>
        <Package className="h-4 w-4" />
        <AlertDescription>
          Provide detailed information about your package to ensure safe handling and accurate pricing.
        </AlertDescription>
      </Alert>

      {/* Smart Tools */}
      <div className="grid gap-4 md:grid-cols-3">
        <PackageSizeGuide
          onSizeSelect={handleSizeSelect}
          currentDimensions={length && width && height ? { length, width, height } : undefined}
        />
        <WeightEstimator
          onWeightSelect={handleWeightSelect}
          currentWeight={weight}
          dimensions={length && width && height ? { length, width, height } : undefined}
        />
        <PackagingRecommendations
          weight={weight}
          dimensions={length && width && height ? { length, width, height } : undefined}
          value={watch('packageValue')}
        />
      </div>

      {/* Package Description */}
      <div className="space-y-2">
        <Label htmlFor="packageDescription" className="text-sm font-medium">
          <FileText className="h-4 w-4 inline mr-1" />
          Package Description *
        </Label>
        <Input
          id="packageDescription"
          {...register('packageDescription')}
          placeholder="Describe the contents (e.g., Electronics, Books, Clothing)"
          error={!!errors.packageDescription}
        />
        {errors.packageDescription && (
          <p className="text-sm text-red-600">{errors.packageDescription.message}</p>
        )}
      </div>

      {/* Physical Dimensions */}
      <Card>
        <CardHeader>
          <CardTitle className="flex items-center gap-2 text-lg">
            <Ruler className="h-5 w-5" />
            Physical Dimensions
          </CardTitle>
          <CardDescription>
            Provide accurate measurements for proper handling and shipping cost calculation
          </CardDescription>
        </CardHeader>
        <CardContent>
          <div className="grid gap-4 md:grid-cols-2 lg:grid-cols-4">
            <div className="space-y-2">
              <Label htmlFor="packageWeight" className="text-sm font-medium">
                <Weight className="h-4 w-4 inline mr-1" />
                Weight (kg) *
              </Label>
              <Input
                id="packageWeight"
                type="number"
                {...register('packageWeight', { valueAsNumber: true })}
                placeholder="1.0"
                min="0.1"
                step="0.1"
                error={!!errors.packageWeight}
              />
              {errors.packageWeight && (
                <p className="text-sm text-red-600">{errors.packageWeight.message}</p>
              )}
              <div className="text-xs text-gray-500">
                Use Weight Estimator for smart suggestions
              </div>
            </div>

            <div className="space-y-2">
              <Label htmlFor="packageLength" className="text-sm font-medium">
                Length (cm) *
              </Label>
              <Input
                id="packageLength"
                type="number"
                {...register('packageLength', { valueAsNumber: true })}
                placeholder="10"
                min="1"
                step="0.1"
                error={!!errors.packageLength}
              />
              {errors.packageLength && (
                <p className="text-sm text-red-600">{errors.packageLength.message}</p>
              )}
            </div>

            <div className="space-y-2">
              <Label htmlFor="packageWidth" className="text-sm font-medium">
                Width (cm) *
              </Label>
              <Input
                id="packageWidth"
                type="number"
                {...register('packageWidth', { valueAsNumber: true })}
                placeholder="10"
                min="1"
                step="0.1"
                error={!!errors.packageWidth}
              />
              {errors.packageWidth && (
                <p className="text-sm text-red-600">{errors.packageWidth.message}</p>
              )}
            </div>

            <div className="space-y-2">
              <Label htmlFor="packageHeight" className="text-sm font-medium">
                Height (cm) *
              </Label>
              <Input
                id="packageHeight"
                type="number"
                {...register('packageHeight', { valueAsNumber: true })}
                placeholder="10"
                min="1"
                step="0.1"
                error={!!errors.packageHeight}
              />
              {errors.packageHeight && (
                <p className="text-sm text-red-600">{errors.packageHeight.message}</p>
              )}
            </div>
          </div>

          {volume > 0 && (
            <div className="mt-4 p-3 bg-blue-50 rounded-lg">
              <p className="text-sm text-blue-800">
                <strong>Calculated Volume:</strong> {volume.toFixed(2)} liters
                {volume > 100 && (
                  <span className="block text-yellow-700 mt-1">
                    ⚠️ Large packages may incur additional handling fees
                  </span>
                )}
              </p>
              <div className="text-xs text-gray-600 mt-1">
                Use Package Size Guide for standard dimensions or Packaging Guide for protection recommendations
              </div>
            </div>
          )}
        </CardContent>
      </Card>

      {/* Package Value */}
      <div className="space-y-2">
        <Label htmlFor="packageValue" className="text-sm font-medium">
          <DollarSign className="h-4 w-4 inline mr-1" />
          Package Value ($) *
        </Label>
        <Input
          id="packageValue"
          type="number"
          {...register('packageValue', { valueAsNumber: true })}
          placeholder="0.00"
          min="0"
          step="0.01"
          error={!!errors.packageValue}
        />
        {errors.packageValue && (
          <p className="text-sm text-red-600">{errors.packageValue.message}</p>
        )}
        <p className="text-xs text-gray-500">
          Used for insurance purposes. Enter 0 if no insurance needed.
        </p>
      </div>

      {/* Special Instructions */}
      <div className="space-y-2">
        <Label htmlFor="specialInstructions" className="text-sm font-medium">
          Special Instructions (Optional)
        </Label>
        <textarea
          id="specialInstructions"
          {...register('specialInstructions')}
          placeholder="Any special handling instructions or delivery notes..."
          className="flex min-h-[80px] w-full rounded-md border border-gray-300 bg-white px-3 py-2 text-sm ring-offset-background placeholder:text-muted-foreground focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-blue-600 focus-visible:ring-offset-2 disabled:cursor-not-allowed disabled:opacity-50"
          rows={3}
        />
        <p className="text-xs text-gray-500">
          Maximum 500 characters. Include any fragile, hazardous, or special handling requirements.
        </p>
      </div>

      {/* Package Summary */}
      <Card>
        <CardHeader>
          <CardTitle className="text-lg">Package Summary</CardTitle>
          <CardDescription>
            Your package details are ready for service selection
          </CardDescription>
        </CardHeader>
        <CardContent>
          <div className="grid grid-cols-2 md:grid-cols-4 gap-4 text-sm">
            <div>
              <div className="font-medium text-gray-700">Weight</div>
              <div className="text-lg">{weight || 0} kg</div>
            </div>
            <div>
              <div className="font-medium text-gray-700">Dimensions</div>
              <div className="text-lg">
                {length && width && height ? `${length}×${width}×${height} cm` : 'Not set'}
              </div>
            </div>
            <div>
              <div className="font-medium text-gray-700">Volume</div>
              <div className="text-lg">{volume > 0 ? `${volume.toFixed(1)}L` : 'Not set'}</div>
            </div>
            <div>
              <div className="font-medium text-gray-700">Value</div>
              <div className="text-lg">${(watch('packageValue') || 0).toFixed(2)}</div>
            </div>
          </div>
          <p className="text-xs text-gray-500 mt-4">
            Complete all fields above, then proceed to service selection in the next step.
          </p>
        </CardContent>
      </Card>
    </div>
  );
};