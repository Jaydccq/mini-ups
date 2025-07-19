import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { useMutation } from '@tanstack/react-query';
import { ArrowLeft, ArrowRight, Package, Save, AlertCircle } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
import { Alert, AlertDescription } from '@/components/ui/alert';
import { Progress } from '@/components/ui/progress';
import { Badge } from '@/components/ui/badge';
import { toast } from 'sonner';
import { useAuthStore } from '@/stores/auth-store';
import { shipmentApi } from '@/services/shipment';
import {
  CreateShipmentFormData,
  createShipmentSchema,
  FORM_STEPS,
  DRAFT_STORAGE_KEY,
} from '@/types/shipment-form';
import { SenderInfoStep } from '@/components/shipment/create/SenderInfoStep';
import { RecipientInfoStep } from '@/components/shipment/create/RecipientInfoStep';
import { PackageDetailsStep } from '@/components/shipment/create/PackageDetailsStep';
import { ServiceSelectionStep } from '@/components/shipment/create/ServiceSelectionStep';
import { ConfirmationStep } from '@/components/shipment/create/ConfirmationStep';

export const CreateShipmentPage: React.FC = () => {
  const navigate = useNavigate();
  const { user } = useAuthStore();
  const [currentStep, setCurrentStep] = useState(1);
  const [isDraftSaved, setIsDraftSaved] = useState(false);

  const form = useForm<CreateShipmentFormData>({
    resolver: zodResolver(createShipmentSchema),
    mode: 'onChange',
    defaultValues: {
      senderName: user?.name || '',
      senderEmail: user?.email || '',
      senderPhone: '',
      senderAddress: '',
      recipientName: '',
      recipientEmail: '',
      recipientPhone: '',
      recipientAddress: '',
      destinationX: 0,
      destinationY: 0,
      packageDescription: '',
      packageWeight: 1,
      packageLength: 10,
      packageWidth: 10,
      packageHeight: 10,
      packageValue: 0,
      deliverySpeed: 'STANDARD',
      specialInstructions: '',
      termsAccepted: false,
      marketingOptIn: false,
    },
  });

  const { watch, trigger, getValues, setValue } = form;
  const formData = watch();

  // Load draft data on component mount
  useEffect(() => {
    const savedDraft = localStorage.getItem(DRAFT_STORAGE_KEY);
    if (savedDraft) {
      try {
        const parsedDraft = JSON.parse(savedDraft);
        Object.keys(parsedDraft).forEach((key) => {
          setValue(key as keyof CreateShipmentFormData, parsedDraft[key]);
        });
        toast.info('Draft loaded from previous session');
      } catch (error) {
        console.error('Failed to load draft:', error);
        localStorage.removeItem(DRAFT_STORAGE_KEY);
      }
    }
  }, [setValue]);

  // Auto-save draft with debouncing
  useEffect(() => {
    const timeoutId = setTimeout(() => {
      const currentData = getValues();
      localStorage.setItem(DRAFT_STORAGE_KEY, JSON.stringify(currentData));
      setIsDraftSaved(true);
      setTimeout(() => setIsDraftSaved(false), 2000);
    }, 1000);

    return () => clearTimeout(timeoutId);
  }, [formData, getValues]);

  // Create shipment mutation
  const createShipmentMutation = useMutation({
    mutationFn: shipmentApi.createShipment,
    onSuccess: (response) => {
      localStorage.removeItem(DRAFT_STORAGE_KEY);
      toast.success('Shipment created successfully!');
      navigate(`/shipments/tracking/${response.tracking_number}`);
    },
    onError: (error: Error) => {
      toast.error(`Failed to create shipment: ${error.message}`);
    },
  });

  const handleNext = async () => {
    const stepSchemas = [
      ['senderName', 'senderEmail', 'senderPhone', 'senderAddress'],
      ['recipientName', 'recipientEmail', 'recipientPhone', 'recipientAddress', 'destinationX', 'destinationY'],
      ['packageDescription', 'packageWeight', 'packageLength', 'packageWidth', 'packageHeight', 'packageValue'],
      ['deliverySpeed'],
      ['termsAccepted'],
    ];

    const fieldsToValidate = stepSchemas[currentStep - 1];
    const isValid = await trigger(fieldsToValidate as (keyof CreateShipmentFormData)[]);

    if (isValid) {
      setCurrentStep((prev) => Math.min(prev + 1, FORM_STEPS.length));
    } else {
      toast.error('Please fix the errors before proceeding');
    }
  };

  const handlePrevious = () => {
    setCurrentStep((prev) => Math.max(prev - 1, 1));
  };

  const handleSubmit = async () => {
    const isValid = await trigger();
    if (!isValid) {
      toast.error('Please fix all errors before submitting');
      return;
    }

    const formData = getValues();
    
    // Transform form data to match API expectations
    const shipmentData = {
      recipient_name: formData.recipientName,
      recipient_email: formData.recipientEmail,
      recipient_phone: formData.recipientPhone,
      recipient_address: formData.recipientAddress,
      destination_x: formData.destinationX,
      destination_y: formData.destinationY,
      package_description: formData.packageDescription,
      package_weight: formData.packageWeight,
      package_dimensions: {
        length: formData.packageLength,
        width: formData.packageWidth,
        height: formData.packageHeight,
      },
      package_value: formData.packageValue,
      delivery_speed: formData.deliverySpeed,
      special_instructions: formData.specialInstructions,
    };

    createShipmentMutation.mutate(shipmentData);
  };

  const saveDraft = () => {
    const currentData = getValues();
    localStorage.setItem(DRAFT_STORAGE_KEY, JSON.stringify(currentData));
    toast.success('Draft saved successfully');
    setIsDraftSaved(true);
    setTimeout(() => setIsDraftSaved(false), 2000);
  };

  const clearDraft = () => {
    localStorage.removeItem(DRAFT_STORAGE_KEY);
    form.reset();
    toast.info('Draft cleared');
  };

  const progress = (currentStep / FORM_STEPS.length) * 100;

  const renderStep = () => {
    switch (currentStep) {
      case 1:
        return <SenderInfoStep form={form} />;
      case 2:
        return <RecipientInfoStep form={form} />;
      case 3:
        return <PackageDetailsStep form={form} />;
      case 4:
        return <ServiceSelectionStep form={form} />;
      case 5:
        return <ConfirmationStep form={form} />;
      default:
        return null;
    }
  };

  const isLastStep = currentStep === FORM_STEPS.length;
  const isFirstStep = currentStep === 1;

  return (
    <div className="p-6 max-w-4xl mx-auto">
      {/* Header */}
      <div className="mb-8">
        <div className="flex items-center justify-between mb-4">
          <div className="flex items-center space-x-4">
            <button
              onClick={() => navigate('/dashboard')}
              className="inline-flex items-center text-blue-600 hover:text-blue-800"
            >
              <ArrowLeft className="h-4 w-4 mr-2" />
              Back to Dashboard
            </button>
          </div>
          <div className="flex items-center space-x-2">
            <Button
              variant="outline"
              size="sm"
              onClick={saveDraft}
              disabled={createShipmentMutation.isPending}
            >
              <Save className="h-4 w-4 mr-2" />
              Save Draft
            </Button>
            {isDraftSaved && (
              <Badge variant="outline" className="text-green-600 border-green-600">
                Draft Saved
              </Badge>
            )}
          </div>
        </div>

        <div className="space-y-4">
          <div>
            <h1 className="text-3xl font-bold text-gray-900">Create New Shipment</h1>
            <p className="text-gray-600">Follow the steps below to create your shipment</p>
          </div>

          {/* Progress Bar */}
          <div className="space-y-2">
            <div className="flex justify-between text-sm text-gray-600">
              <span>Step {currentStep} of {FORM_STEPS.length}</span>
              <span>{Math.round(progress)}% Complete</span>
            </div>
            <Progress value={progress} className="h-2" />
          </div>

          {/* Step Indicators */}
          <div className="flex justify-between">
            {FORM_STEPS.map((step) => (
              <div
                key={step.id}
                className={`flex-1 text-center ${
                  step.id <= currentStep ? 'text-blue-600' : 'text-gray-400'
                }`}
              >
                <div
                  className={`w-8 h-8 rounded-full mx-auto mb-2 flex items-center justify-center text-sm font-medium ${
                    step.id < currentStep
                      ? 'bg-blue-600 text-white'
                      : step.id === currentStep
                      ? 'bg-blue-100 text-blue-600 border-2 border-blue-600'
                      : 'bg-gray-200 text-gray-400'
                  }`}
                >
                  {step.id < currentStep ? '✓' : step.id}
                </div>
                <div className="text-xs font-medium">{step.title}</div>
                <div className="text-xs text-gray-500">{step.description}</div>
              </div>
            ))}
          </div>
        </div>
      </div>

      {/* Form Content */}
      <Card>
        <CardHeader>
          <CardTitle className="flex items-center gap-2">
            <Package className="h-5 w-5" />
            {FORM_STEPS[currentStep - 1].title}
          </CardTitle>
          <CardDescription>
            {FORM_STEPS[currentStep - 1].description}
          </CardDescription>
        </CardHeader>
        <CardContent>
          {renderStep()}
        </CardContent>
      </Card>

      {/* Form Actions */}
      <div className="flex justify-between items-center mt-8">
        <div>
          {!isFirstStep && (
            <Button
              variant="outline"
              onClick={handlePrevious}
              disabled={createShipmentMutation.isPending}
            >
              <ArrowLeft className="h-4 w-4 mr-2" />
              Previous
            </Button>
          )}
        </div>

        <div className="flex space-x-4">
          <Button
            variant="outline"
            onClick={clearDraft}
            disabled={createShipmentMutation.isPending}
          >
            Clear Form
          </Button>
          
          {isLastStep ? (
            <Button
              onClick={handleSubmit}
              disabled={createShipmentMutation.isPending}
              className="min-w-[120px]"
            >
              {createShipmentMutation.isPending ? (
                'Creating...'
              ) : (
                <>
                  <Package className="h-4 w-4 mr-2" />
                  Create Shipment
                </>
              )}
            </Button>
          ) : (
            <Button onClick={handleNext} disabled={createShipmentMutation.isPending}>
              Next
              <ArrowRight className="h-4 w-4 ml-2" />
            </Button>
          )}
        </div>
      </div>

      {/* Error Alert */}
      {createShipmentMutation.error && (
        <Alert variant="destructive" className="mt-4">
          <AlertCircle className="h-4 w-4" />
          <AlertDescription>
            {createShipmentMutation.error.message}
          </AlertDescription>
        </Alert>
      )}
    </div>
  );
};