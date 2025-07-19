import React, { useState } from 'react';
import { Calendar, Clock, AlertCircle, CheckCircle } from 'lucide-react';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { Badge } from '@/components/ui/badge';
import { Alert, AlertDescription } from '@/components/ui/alert';
import { Calendar as CalendarComponent } from '@/components/ui/calendar';
import { Popover, PopoverContent, PopoverTrigger } from '@/components/ui/popover';
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select';

interface TimeWindowSelectorProps {
  serviceType: string;
  onTimeWindowSelect: (window: {
    pickupDate?: Date;
    pickupTimeSlot?: string;
    deliveryDate?: Date;
    deliveryTimeSlot?: string;
  }) => void;
  currentSelection?: {
    pickupDate?: Date;
    pickupTimeSlot?: string;
    deliveryDate?: Date;
    deliveryTimeSlot?: string;
  };
}

const TIME_SLOTS = [
  { id: 'morning', label: '9:00 AM - 12:00 PM', available: true, premium: false },
  { id: 'afternoon', label: '12:00 PM - 5:00 PM', available: true, premium: false },
  { id: 'evening', label: '5:00 PM - 8:00 PM', available: true, premium: true },
  { id: 'anytime', label: 'Anytime (9:00 AM - 8:00 PM)', available: true, premium: false },
];

const PICKUP_SLOTS = [
  { id: 'morning', label: '9:00 AM - 12:00 PM', available: true, premium: false },
  { id: 'afternoon', label: '1:00 PM - 5:00 PM', available: true, premium: false },
  { id: 'evening', label: '5:00 PM - 7:00 PM', available: false, premium: true },
];

export const TimeWindowSelector: React.FC<TimeWindowSelectorProps> = ({
  serviceType,
  onTimeWindowSelect,
  currentSelection = {},
}) => {
  const [pickupDate, setPickupDate] = useState<Date | undefined>(currentSelection.pickupDate);
  const [pickupTimeSlot, setPickupTimeSlot] = useState<string | undefined>(currentSelection.pickupTimeSlot);
  const [deliveryDate, setDeliveryDate] = useState<Date | undefined>(currentSelection.deliveryDate);
  const [deliveryTimeSlot, setDeliveryTimeSlot] = useState<string | undefined>(currentSelection.deliveryTimeSlot);

  const isWeekend = (date: Date) => {
    const day = date.getDay();
    return day === 0 || day === 6;
  };

  const isHoliday = (date: Date) => {
    // Simple holiday check - in real app, this would use a proper holiday API
    const holidays = [
      new Date('2024-12-25'), // Christmas
      new Date('2024-01-01'), // New Year
      new Date('2024-07-04'), // Independence Day
    ];
    return holidays.some(holiday => 
      date.toDateString() === holiday.toDateString()
    );
  };

  const getMinPickupDate = () => {
    const tomorrow = new Date();
    tomorrow.setDate(tomorrow.getDate() + 1);
    return tomorrow;
  };

  const getMinDeliveryDate = () => {
    if (!pickupDate) return new Date();
    
    const minDelivery = new Date(pickupDate);
    const daysToAdd = {
      'STANDARD': 5,
      'EXPRESS': 2,
      'OVERNIGHT': 1,
    }[serviceType] || 5;
    
    minDelivery.setDate(minDelivery.getDate() + daysToAdd);
    return minDelivery;
  };

  const getMaxDeliveryDate = () => {
    if (!pickupDate) return new Date();
    
    const maxDelivery = new Date(pickupDate);
    const daysToAdd = {
      'STANDARD': 7,
      'EXPRESS': 3,
      'OVERNIGHT': 1,
    }[serviceType] || 7;
    
    maxDelivery.setDate(maxDelivery.getDate() + daysToAdd);
    return maxDelivery;
  };

  const handlePickupDateChange = (date: Date | undefined) => {
    setPickupDate(date);
    // Reset delivery date if it's now invalid
    if (date && deliveryDate && deliveryDate < getMinDeliveryDate()) {
      setDeliveryDate(undefined);
    }
    updateSelection({ pickupDate: date });
  };

  const handlePickupTimeSlotChange = (slot: string) => {
    setPickupTimeSlot(slot);
    updateSelection({ pickupTimeSlot: slot });
  };

  const handleDeliveryDateChange = (date: Date | undefined) => {
    setDeliveryDate(date);
    updateSelection({ deliveryDate: date });
  };

  const handleDeliveryTimeSlotChange = (slot: string) => {
    setDeliveryTimeSlot(slot);
    updateSelection({ deliveryTimeSlot: slot });
  };

  const updateSelection = (updates: Partial<typeof currentSelection>) => {
    onTimeWindowSelect({
      pickupDate,
      pickupTimeSlot,
      deliveryDate,
      deliveryTimeSlot,
      ...updates,
    });
  };

  const isDateDisabled = (date: Date, type: 'pickup' | 'delivery') => {
    const today = new Date();
    today.setHours(0, 0, 0, 0);
    
    if (type === 'pickup') {
      return date < getMinPickupDate() || isWeekend(date) || isHoliday(date);
    } else {
      const minDate = getMinDeliveryDate();
      const maxDate = getMaxDeliveryDate();
      return date < minDate || date > maxDate || isWeekend(date) || isHoliday(date);
    }
  };

  const getEstimatedDeliveryRange = () => {
    if (!pickupDate) return null;
    
    const minDate = getMinDeliveryDate();
    const maxDate = getMaxDeliveryDate();
    
    return {
      min: minDate.toLocaleDateString('en-US', { 
        weekday: 'short', 
        month: 'short', 
        day: 'numeric' 
      }),
      max: maxDate.toLocaleDateString('en-US', { 
        weekday: 'short', 
        month: 'short', 
        day: 'numeric' 
      }),
    };
  };

  const estimatedRange = getEstimatedDeliveryRange();

  return (
    <div className="space-y-6">
      <div className="text-center">
        <h3 className="text-lg font-semibold mb-2">Schedule Your Shipment</h3>
        <p className="text-sm text-gray-600">
          Choose pickup and delivery time windows that work for you
        </p>
      </div>

      <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
        {/* Pickup Scheduling */}
        <Card>
          <CardHeader>
            <CardTitle className="flex items-center gap-2">
              <Calendar className="h-5 w-5" />
              Pickup Schedule
            </CardTitle>
            <CardDescription>
              When should we pick up your package?
            </CardDescription>
          </CardHeader>
          <CardContent className="space-y-4">
            <div>
              <label className="text-sm font-medium mb-2 block">Pickup Date</label>
              <Popover>
                <PopoverTrigger asChild>
                  <Button variant="outline" className="w-full justify-start">
                    <Calendar className="h-4 w-4 mr-2" />
                    {pickupDate ? 
                      pickupDate.toLocaleDateString('en-US', { 
                        weekday: 'short', 
                        month: 'short', 
                        day: 'numeric' 
                      }) : 
                      'Select pickup date'
                    }
                  </Button>
                </PopoverTrigger>
                <PopoverContent className="w-auto p-0">
                  <CalendarComponent
                    mode="single"
                    selected={pickupDate}
                    onSelect={handlePickupDateChange}
                    disabled={(date) => isDateDisabled(date, 'pickup')}
                    initialFocus
                  />
                </PopoverContent>
              </Popover>
            </div>

            <div>
              <label className="text-sm font-medium mb-2 block">Pickup Time</label>
              <Select value={pickupTimeSlot} onValueChange={handlePickupTimeSlotChange}>
                <SelectTrigger>
                  <SelectValue placeholder="Select time slot" />
                </SelectTrigger>
                <SelectContent>
                  {PICKUP_SLOTS.map((slot) => (
                    <SelectItem 
                      key={slot.id} 
                      value={slot.id}
                      disabled={!slot.available}
                    >
                      <div className="flex items-center justify-between w-full">
                        <span>{slot.label}</span>
                        {slot.premium && (
                          <Badge variant="secondary" className="ml-2">
                            Premium
                          </Badge>
                        )}
                        {!slot.available && (
                          <Badge variant="destructive" className="ml-2">
                            Unavailable
                          </Badge>
                        )}
                      </div>
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
            </div>

            {pickupDate && pickupTimeSlot && (
              <div className="p-3 bg-green-50 rounded-lg flex items-center gap-2">
                <CheckCircle className="h-4 w-4 text-green-600" />
                <span className="text-sm text-green-800">
                  Pickup scheduled for {pickupDate.toLocaleDateString()} at {
                    PICKUP_SLOTS.find(s => s.id === pickupTimeSlot)?.label
                  }
                </span>
              </div>
            )}
          </CardContent>
        </Card>

        {/* Delivery Scheduling */}
        <Card>
          <CardHeader>
            <CardTitle className="flex items-center gap-2">
              <Clock className="h-5 w-5" />
              Delivery Schedule
            </CardTitle>
            <CardDescription>
              When would you like it delivered?
            </CardDescription>
          </CardHeader>
          <CardContent className="space-y-4">
            <div>
              <label className="text-sm font-medium mb-2 block">Delivery Date</label>
              <Popover>
                <PopoverTrigger asChild>
                  <Button 
                    variant="outline" 
                    className="w-full justify-start"
                    disabled={!pickupDate}
                  >
                    <Calendar className="h-4 w-4 mr-2" />
                    {deliveryDate ? 
                      deliveryDate.toLocaleDateString('en-US', { 
                        weekday: 'short', 
                        month: 'short', 
                        day: 'numeric' 
                      }) : 
                      'Select delivery date'
                    }
                  </Button>
                </PopoverTrigger>
                <PopoverContent className="w-auto p-0">
                  <CalendarComponent
                    mode="single"
                    selected={deliveryDate}
                    onSelect={handleDeliveryDateChange}
                    disabled={(date) => isDateDisabled(date, 'delivery')}
                    initialFocus
                  />
                </PopoverContent>
              </Popover>
            </div>

            <div>
              <label className="text-sm font-medium mb-2 block">Delivery Time</label>
              <Select value={deliveryTimeSlot} onValueChange={handleDeliveryTimeSlotChange}>
                <SelectTrigger disabled={!deliveryDate}>
                  <SelectValue placeholder="Select time slot" />
                </SelectTrigger>
                <SelectContent>
                  {TIME_SLOTS.map((slot) => (
                    <SelectItem 
                      key={slot.id} 
                      value={slot.id}
                      disabled={!slot.available}
                    >
                      <div className="flex items-center justify-between w-full">
                        <span>{slot.label}</span>
                        {slot.premium && (
                          <Badge variant="secondary" className="ml-2">
                            +$5
                          </Badge>
                        )}
                      </div>
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
            </div>

            {estimatedRange && (
              <Alert>
                <AlertCircle className="h-4 w-4" />
                <AlertDescription>
                  <strong>Estimated delivery:</strong> {estimatedRange.min}
                  {estimatedRange.min !== estimatedRange.max && ` - ${estimatedRange.max}`}
                </AlertDescription>
              </Alert>
            )}

            {deliveryDate && deliveryTimeSlot && (
              <div className="p-3 bg-blue-50 rounded-lg flex items-center gap-2">
                <CheckCircle className="h-4 w-4 text-blue-600" />
                <span className="text-sm text-blue-800">
                  Delivery scheduled for {deliveryDate.toLocaleDateString()} at {
                    TIME_SLOTS.find(s => s.id === deliveryTimeSlot)?.label
                  }
                </span>
              </div>
            )}
          </CardContent>
        </Card>
      </div>

      {/* Service Level Info */}
      <Card>
        <CardHeader>
          <CardTitle className="text-lg">Service Level: {serviceType}</CardTitle>
        </CardHeader>
        <CardContent>
          <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
            <div>
              <h4 className="font-medium text-sm mb-2">Pickup Options</h4>
              <ul className="text-sm space-y-1">
                <li className="flex items-center gap-2">
                  <CheckCircle className="h-3 w-3 text-green-500" />
                  Next business day pickup
                </li>
                <li className="flex items-center gap-2">
                  <CheckCircle className="h-3 w-3 text-green-500" />
                  Flexible time slots
                </li>
                <li className="flex items-center gap-2">
                  <CheckCircle className="h-3 w-3 text-green-500" />
                  Free pickup service
                </li>
              </ul>
            </div>
            <div>
              <h4 className="font-medium text-sm mb-2">Delivery Options</h4>
              <ul className="text-sm space-y-1">
                <li className="flex items-center gap-2">
                  <CheckCircle className="h-3 w-3 text-green-500" />
                  Business day delivery
                </li>
                <li className="flex items-center gap-2">
                  <CheckCircle className="h-3 w-3 text-green-500" />
                  Signature confirmation
                </li>
                <li className="flex items-center gap-2">
                  <CheckCircle className="h-3 w-3 text-green-500" />
                  Delivery notifications
                </li>
              </ul>
            </div>
            <div>
              <h4 className="font-medium text-sm mb-2">Additional Services</h4>
              <ul className="text-sm space-y-1">
                <li className="flex items-center gap-2">
                  <CheckCircle className="h-3 w-3 text-green-500" />
                  Package tracking
                </li>
                <li className="flex items-center gap-2">
                  <CheckCircle className="h-3 w-3 text-green-500" />
                  Insurance included
                </li>
                <li className="flex items-center gap-2">
                  <CheckCircle className="h-3 w-3 text-green-500" />
                  Customer support
                </li>
              </ul>
            </div>
          </div>
        </CardContent>
      </Card>

      {/* Important Notes */}
      <Alert>
        <AlertCircle className="h-4 w-4" />
        <AlertDescription>
          <strong>Important:</strong> All times are in local time zones. 
          Pickup and delivery are only available on business days (Monday-Friday). 
          Premium time slots may incur additional charges. 
          Weather conditions may affect scheduled times.
        </AlertDescription>
      </Alert>
    </div>
  );
};