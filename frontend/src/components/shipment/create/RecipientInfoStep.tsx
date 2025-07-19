import React, { useState } from 'react';
import { UseFormReturn } from 'react-hook-form';
import { User, Mail, Phone, MapPin, Navigation } from 'lucide-react';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { Alert, AlertDescription } from '@/components/ui/alert';
import { CreateShipmentFormData } from '@/types/shipment-form';
import { AddressBook } from './AddressBook';
import { AddressValidation } from './AddressValidation';

interface RecipientInfoStepProps {
  form: UseFormReturn<CreateShipmentFormData>;
}

export const RecipientInfoStep: React.FC<RecipientInfoStepProps> = ({ form }) => {
  const {
    register,
    formState: { errors },
    setValue,
    watch,
  } = form;

  const [addressValidation, setAddressValidation] = useState<{
    isValid: boolean;
    isValidating: boolean;
  }>({ isValid: false, isValidating: false });

  const currentAddress = watch('recipientAddress');

  const handleAddressBookSelect = (address: any) => {
    setValue('recipientName', address.name);
    setValue('recipientEmail', address.email);
    setValue('recipientPhone', address.phone);
    setValue('recipientAddress', address.address);
  };

  const handleAddressValidation = (address: string, coordinates?: { x: number; y: number }) => {
    setValue('recipientAddress', address);
    if (coordinates) {
      setValue('destinationX', coordinates.x);
      setValue('destinationY', coordinates.y);
    }
  };

  const handleValidationChange = (isValid: boolean) => {
    setAddressValidation(prev => ({ ...prev, isValid }));
  };

  return (
    <div className="space-y-6">
      <Alert>
        <Navigation className="h-4 w-4" />
        <AlertDescription>
          Provide the recipient's contact information and delivery coordinates.
        </AlertDescription>
      </Alert>

      {/* Address Book Integration */}
      <div className="w-full">
        <AddressBook
          type="recipient"
          onSelectAddress={handleAddressBookSelect}
          currentAddress={{
            name: watch('recipientName'),
            email: watch('recipientEmail'),
            phone: watch('recipientPhone'),
            address: watch('recipientAddress'),
          }}
        />
      </div>

      <div className="grid gap-6 md:grid-cols-2">
        <div className="space-y-2">
          <Label htmlFor="recipientName" className="text-sm font-medium">
            <User className="h-4 w-4 inline mr-1" />
            Recipient Name *
          </Label>
          <Input
            id="recipientName"
            {...register('recipientName')}
            placeholder="Enter recipient's full name"
            error={!!errors.recipientName}
          />
          {errors.recipientName && (
            <p className="text-sm text-red-600">{errors.recipientName.message}</p>
          )}
        </div>

        <div className="space-y-2">
          <Label htmlFor="recipientEmail" className="text-sm font-medium">
            <Mail className="h-4 w-4 inline mr-1" />
            Email Address *
          </Label>
          <Input
            id="recipientEmail"
            type="email"
            {...register('recipientEmail')}
            placeholder="recipient@example.com"
            error={!!errors.recipientEmail}
          />
          {errors.recipientEmail && (
            <p className="text-sm text-red-600">{errors.recipientEmail.message}</p>
          )}
        </div>

        <div className="space-y-2">
          <Label htmlFor="recipientPhone" className="text-sm font-medium">
            <Phone className="h-4 w-4 inline mr-1" />
            Phone Number *
          </Label>
          <Input
            id="recipientPhone"
            {...register('recipientPhone')}
            placeholder="+1 (555) 987-6543"
            error={!!errors.recipientPhone}
          />
          {errors.recipientPhone && (
            <p className="text-sm text-red-600">{errors.recipientPhone.message}</p>
          )}
        </div>

        <div className="space-y-2 md:col-span-2">
          <Label htmlFor="recipientAddress" className="text-sm font-medium">
            <MapPin className="h-4 w-4 inline mr-1" />
            Delivery Address *
          </Label>
          <AddressValidation
            id="recipientAddress"
            address={currentAddress || ''}
            onAddressSelect={handleAddressValidation}
            onValidationChange={handleValidationChange}
            placeholder="456 Delivery Ave, City, State, ZIP"
            error={!!errors.recipientAddress}
          />
          {errors.recipientAddress && (
            <p className="text-sm text-red-600">{errors.recipientAddress.message}</p>
          )}
        </div>
      </div>

      {/* Delivery Coordinates */}
      <div className="border-t pt-6">
        <h3 className="text-lg font-medium mb-4 flex items-center gap-2">
          <Navigation className="h-5 w-5" />
          Delivery Coordinates
        </h3>
        
        <div className="grid gap-4 md:grid-cols-2">
          <div className="space-y-2">
            <Label htmlFor="destinationX" className="text-sm font-medium">
              X Coordinate *
            </Label>
            <Input
              id="destinationX"
              type="number"
              {...register('destinationX', { valueAsNumber: true })}
              placeholder="0"
              min="-1000"
              max="1000"
              step="0.1"
              error={!!errors.destinationX}
            />
            {errors.destinationX && (
              <p className="text-sm text-red-600">{errors.destinationX.message}</p>
            )}
          </div>

          <div className="space-y-2">
            <Label htmlFor="destinationY" className="text-sm font-medium">
              Y Coordinate *
            </Label>
            <Input
              id="destinationY"
              type="number"
              {...register('destinationY', { valueAsNumber: true })}
              placeholder="0"
              min="-1000"
              max="1000"
              step="0.1"
              error={!!errors.destinationY}
            />
            {errors.destinationY && (
              <p className="text-sm text-red-600">{errors.destinationY.message}</p>
            )}
          </div>
        </div>

        <div className="p-4 bg-yellow-50 rounded-lg mt-4">
          <p className="text-sm text-yellow-800">
            <strong>Coordinate System:</strong> The delivery coordinates represent the exact location 
            in our warehouse system. Valid range is -1000 to 1000 for both X and Y coordinates.
          </p>
        </div>
      </div>
    </div>
  );
};