import { z } from 'zod';

// Step 1: Sender Information
export const senderInfoSchema = z.object({
  senderName: z.string().min(1, 'Sender name is required'),
  senderEmail: z.string().email('Invalid email address'),
  senderPhone: z.string().min(10, 'Phone number must be at least 10 digits'),
  senderAddress: z.string().min(1, 'Sender address is required'),
});

// Step 2: Recipient Information
export const recipientInfoSchema = z.object({
  recipientName: z.string().min(1, 'Recipient name is required'),
  recipientEmail: z.string().email('Invalid email address'),
  recipientPhone: z.string().min(10, 'Phone number must be at least 10 digits'),
  recipientAddress: z.string().min(1, 'Recipient address is required'),
  destinationX: z.number().min(-1000).max(1000),
  destinationY: z.number().min(-1000).max(1000),
});

// Step 3: Package Details
export const packageDetailsSchema = z.object({
  packageDescription: z.string().min(1, 'Package description is required'),
  packageWeight: z.number().min(0.1, 'Weight must be at least 0.1 kg'),
  packageLength: z.number().min(1, 'Length must be at least 1 cm'),
  packageWidth: z.number().min(1, 'Width must be at least 1 cm'),
  packageHeight: z.number().min(1, 'Height must be at least 1 cm'),
  packageValue: z.number().min(0, 'Package value cannot be negative'),
  specialInstructions: z.string().optional(),
});

// Step 4: Service Selection
export const serviceSelectionSchema = z.object({
  deliverySpeed: z.enum(['STANDARD', 'EXPRESS', 'OVERNIGHT']),
});

// Step 5: Confirmation
export const confirmationSchema = z.object({
  termsAccepted: z.boolean().refine(val => val === true, 'You must accept the terms and conditions'),
  marketingOptIn: z.boolean().optional(),
});

// Combined schema for the entire form
export const createShipmentSchema = senderInfoSchema
  .and(recipientInfoSchema)
  .and(packageDetailsSchema)
  .and(serviceSelectionSchema)
  .and(confirmationSchema);

export type SenderInfo = z.infer<typeof senderInfoSchema>;
export type RecipientInfo = z.infer<typeof recipientInfoSchema>;
export type PackageDetails = z.infer<typeof packageDetailsSchema>;
export type ServiceSelection = z.infer<typeof serviceSelectionSchema>;
export type Confirmation = z.infer<typeof confirmationSchema>;
export type CreateShipmentFormData = z.infer<typeof createShipmentSchema>;

export const FORM_STEPS = [
  { id: 1, title: 'Sender Information', description: 'Your contact details' },
  { id: 2, title: 'Recipient Information', description: 'Delivery destination' },
  { id: 3, title: 'Package Details', description: 'Package specifications' },
  { id: 4, title: 'Service Selection', description: 'Choose shipping service' },
  { id: 5, title: 'Review & Confirm', description: 'Final confirmation' },
] as const;

export type FormStep = typeof FORM_STEPS[number];

// Draft storage key for localStorage
export const DRAFT_STORAGE_KEY = 'shipment-form-draft';