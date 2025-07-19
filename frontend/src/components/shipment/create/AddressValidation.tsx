import React, { useState, useEffect } from 'react';
import { CheckCircle, AlertCircle, Search, MapPin, Navigation } from 'lucide-react';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { Button } from '@/components/ui/button';
import { Card, CardContent } from '@/components/ui/card';
import { Alert, AlertDescription } from '@/components/ui/alert';
import { Badge } from '@/components/ui/badge';
import { Skeleton } from '@/components/ui/skeleton';

interface AddressValidationProps {
  address: string;
  onAddressSelect: (address: string, coordinates?: { x: number; y: number }) => void;
  onValidationChange: (isValid: boolean) => void;
  placeholder?: string;
  error?: boolean;
  id?: string;
}

interface AddressSuggestion {
  formatted_address: string;
  confidence: number;
  coordinates?: { x: number; y: number };
  components: {
    street_number?: string;
    street_name?: string;
    city?: string;
    state?: string;
    postal_code?: string;
    country?: string;
  };
}

// Mock address validation service - In real app, this would call Google Places API or similar
const validateAddress = async (address: string): Promise<{
  isValid: boolean;
  suggestions: AddressSuggestion[];
  confidence: number;
}> => {
  // Simulate API delay
  await new Promise(resolve => setTimeout(resolve, 500));

  // Mock validation logic
  const isValid = address.length > 10 && address.includes(',');
  const suggestions: AddressSuggestion[] = [];

  if (!isValid && address.length > 5) {
    // Generate mock suggestions
    const baseSuggestions = [
      `${address} Street, New York, NY 10001`,
      `${address} Avenue, Los Angeles, CA 90210`,
      `${address} Road, Chicago, IL 60601`,
    ];

    suggestions.push(...baseSuggestions.map((addr, index) => ({
      formatted_address: addr,
      confidence: 0.9 - (index * 0.1),
      coordinates: { x: Math.random() * 100, y: Math.random() * 100 },
      components: {
        street_name: address,
        city: ['New York', 'Los Angeles', 'Chicago'][index],
        state: ['NY', 'CA', 'IL'][index],
        postal_code: ['10001', '90210', '60601'][index],
        country: 'USA',
      },
    })));
  }

  return {
    isValid,
    suggestions,
    confidence: isValid ? 1.0 : 0.5,
  };
};

export const AddressValidation: React.FC<AddressValidationProps> = ({
  address,
  onAddressSelect,
  onValidationChange,
  placeholder = 'Enter address...',
  error = false,
  id,
}) => {
  const [inputValue, setInputValue] = useState(address);
  const [isValidating, setIsValidating] = useState(false);
  const [validationResult, setValidationResult] = useState<{
    isValid: boolean;
    suggestions: AddressSuggestion[];
    confidence: number;
  } | null>(null);
  const [showSuggestions, setShowSuggestions] = useState(false);

  useEffect(() => {
    setInputValue(address);
  }, [address]);

  useEffect(() => {
    const validateAddressAsync = async () => {
      if (inputValue.length < 5) {
        setValidationResult(null);
        setShowSuggestions(false);
        onValidationChange(false);
        return;
      }

      setIsValidating(true);
      try {
        const result = await validateAddress(inputValue);
        setValidationResult(result);
        setShowSuggestions(!result.isValid && result.suggestions.length > 0);
        onValidationChange(result.isValid);
      } catch (error) {
        console.error('Address validation error:', error);
        setValidationResult(null);
        onValidationChange(false);
      } finally {
        setIsValidating(false);
      }
    };

    const timeoutId = setTimeout(validateAddressAsync, 300);
    return () => clearTimeout(timeoutId);
  }, [inputValue, onValidationChange]);

  const handleInputChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const value = e.target.value;
    setInputValue(value);
    onAddressSelect(value);
  };

  const handleSuggestionSelect = (suggestion: AddressSuggestion) => {
    setInputValue(suggestion.formatted_address);
    onAddressSelect(suggestion.formatted_address, suggestion.coordinates);
    setShowSuggestions(false);
    onValidationChange(true);
  };

  const getValidationIcon = () => {
    if (isValidating) return <Search className="h-4 w-4 text-gray-400 animate-spin" />;
    if (validationResult?.isValid) return <CheckCircle className="h-4 w-4 text-green-500" />;
    if (validationResult && !validationResult.isValid) return <AlertCircle className="h-4 w-4 text-yellow-500" />;
    return <MapPin className="h-4 w-4 text-gray-400" />;
  };

  const getValidationStatus = () => {
    if (isValidating) return 'Validating...';
    if (validationResult?.isValid) return 'Valid address';
    if (validationResult && !validationResult.isValid) return 'Address suggestions available';
    return '';
  };

  return (
    <div className="space-y-2">
      <div className="relative">
        <Input
          id={id}
          value={inputValue}
          onChange={handleInputChange}
          placeholder={placeholder}
          error={error}
          className="pr-10"
        />
        <div className="absolute right-3 top-3">
          {getValidationIcon()}
        </div>
      </div>

      {/* Validation Status */}
      {validationResult && (
        <div className="flex items-center gap-2 text-sm">
          <Badge
            variant={validationResult.isValid ? 'default' : 'secondary'}
            className={validationResult.isValid ? 'bg-green-100 text-green-800' : ''}
          >
            {validationResult.isValid ? 'Verified' : 'Unverified'}
          </Badge>
          <span className="text-gray-600">{getValidationStatus()}</span>
        </div>
      )}

      {/* Loading State */}
      {isValidating && (
        <div className="space-y-2">
          <Skeleton className="h-4 w-3/4" />
          <Skeleton className="h-4 w-1/2" />
        </div>
      )}

      {/* Address Suggestions */}
      {showSuggestions && validationResult && (
        <Card>
          <CardContent className="p-4">
            <div className="flex items-center gap-2 mb-3">
              <Navigation className="h-4 w-4 text-blue-500" />
              <span className="font-medium text-sm">Did you mean:</span>
            </div>
            <div className="space-y-2">
              {validationResult.suggestions.map((suggestion, index) => (
                <div
                  key={index}
                  className="p-3 border rounded-lg hover:bg-gray-50 cursor-pointer transition-colors"
                  onClick={() => handleSuggestionSelect(suggestion)}
                >
                  <div className="flex items-start justify-between">
                    <div className="flex-1">
                      <p className="font-medium text-sm">{suggestion.formatted_address}</p>
                      <div className="flex items-center gap-2 mt-1">
                        <Badge variant="outline" className="text-xs">
                          {Math.round(suggestion.confidence * 100)}% match
                        </Badge>
                        {suggestion.coordinates && (
                          <span className="text-xs text-gray-500">
                            Coordinates: ({suggestion.coordinates.x.toFixed(1)}, {suggestion.coordinates.y.toFixed(1)})
                          </span>
                        )}
                      </div>
                    </div>
                    <Button variant="ghost" size="sm">
                      Select
                    </Button>
                  </div>
                </div>
              ))}
            </div>
          </CardContent>
        </Card>
      )}

      {/* Address not found */}
      {validationResult && !validationResult.isValid && validationResult.suggestions.length === 0 && inputValue.length > 5 && (
        <Alert variant="destructive">
          <AlertCircle className="h-4 w-4" />
          <AlertDescription>
            Address could not be validated. Please check the address and try again.
          </AlertDescription>
        </Alert>
      )}
    </div>
  );
};