import React, { useState } from 'react';
import { UseFormReturn } from 'react-hook-form';
import { User, Mail, Phone, MapPin, RefreshCw } from 'lucide-react';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { Alert, AlertDescription } from '@/components/ui/alert';
import { Button } from '@/components/ui/button';
import { CreateShipmentFormData } from '@/types/shipment-form';
import { AddressBook } from './AddressBook';
import { AddressValidation } from './AddressValidation';

interface SenderInfoStepProps {
  form: UseFormReturn<CreateShipmentFormData>;
}

export const SenderInfoStep: React.FC<SenderInfoStepProps> = ({ form }) => {
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

  const currentAddress = watch('senderAddress');

  const handleAddressBookSelect = (address: any) => {
    setValue('senderName', address.name);
    setValue('senderEmail', address.email);
    setValue('senderPhone', address.phone);
    setValue('senderAddress', address.address);
  };

  const handleAddressValidation = (address: string, coordinates?: { x: number; y: number }) => {
    setValue('senderAddress', address);
  };

  const handleValidationChange = (isValid: boolean) => {
    setAddressValidation(prev => ({ ...prev, isValid }));
  };

  const handleAutoFillFromProfile = () => {
    // This would typically fetch from user profile API
    // For now, we'll use the default values set in the form
    setValue('senderName', form.getValues('senderName'));
    setValue('senderEmail', form.getValues('senderEmail'));
  };

  return (
    <div className="space-y-6">
      <Alert>
        <User className="h-4 w-4" />
        <AlertDescription>
          Please provide your contact information. This will be used as the sender details for the shipment.
        </AlertDescription>
      </Alert>

      {/* Address Book Integration */}
      <div className="flex gap-4">
        <AddressBook
          type="sender"
          onSelectAddress={handleAddressBookSelect}
          currentAddress={{
            name: watch('senderName'),
            email: watch('senderEmail'),
            phone: watch('senderPhone'),
            address: watch('senderAddress'),
          }}
        />
        <Button
          type="button"
          variant="outline"
          onClick={handleAutoFillFromProfile}
          className="flex-1"
        >
          <RefreshCw className="h-4 w-4 mr-2" />
          Auto-fill from Profile
        </Button>
      </div>

      <div className="grid gap-6 md:grid-cols-2">
        <div className="space-y-2">
          <Label htmlFor="senderName" className="text-sm font-medium">
            <User className="h-4 w-4 inline mr-1" />
            Full Name *
          </Label>
          <Input
            id="senderName"
            {...register('senderName')}
            placeholder="Enter your full name"
            className={errors.senderName ? 'border-red-500' : ''}
          />
          {errors.senderName && (
            <p className="text-sm text-red-600">{errors.senderName.message}</p>
          )}
        </div>

        <div className="space-y-2">
          <Label htmlFor="senderEmail" className="text-sm font-medium">
            <Mail className="h-4 w-4 inline mr-1" />
            Email Address *
          </Label>
          <Input
            id="senderEmail"
            type="email"
            {...register('senderEmail')}
            placeholder="your.email@example.com"
            className={errors.senderEmail ? 'border-red-500' : ''}
          />
          {errors.senderEmail && (
            <p className="text-sm text-red-600">{errors.senderEmail.message}</p>
          )}
        </div>

        <div className="space-y-2">
          <Label htmlFor="senderPhone" className="text-sm font-medium">
            <Phone className="h-4 w-4 inline mr-1" />
            Phone Number *
          </Label>
          <Input
            id="senderPhone"
            {...register('senderPhone')}
            placeholder="+1 (555) 123-4567"
            className={errors.senderPhone ? 'border-red-500' : ''}
          />
          {errors.senderPhone && (
            <p className="text-sm text-red-600">{errors.senderPhone.message}</p>
          )}
        </div>

        <div className="space-y-2 md:col-span-2">
          <Label htmlFor="senderAddress" className="text-sm font-medium">
            <MapPin className="h-4 w-4 inline mr-1" />
            Address *
          </Label>
          <AddressValidation
            id="senderAddress"
            address={currentAddress || ''}
            onAddressSelect={handleAddressValidation}
            onValidationChange={handleValidationChange}
            placeholder="123 Main St, City, State, ZIP"
            className={errors.senderAddress ? 'border-red-500' : ''}
          />
          {errors.senderAddress && (
            <p className="text-sm text-red-600">{errors.senderAddress.message}</p>
          )}
        </div>
      </div>

      <div className="p-4 bg-blue-50 rounded-lg">
        <p className="text-sm text-blue-800">
          <strong>Note:</strong> This information will be pre-filled from your account profile. 
          You can modify it if needed for this specific shipment.
        </p>
      </div>
    </div>
  );
};