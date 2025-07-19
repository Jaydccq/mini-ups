import React, { useState } from 'react';
import { Scale, Calculator, Package, Info } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
import { Dialog, DialogContent, DialogHeader, DialogTitle, DialogTrigger } from '@/components/ui/dialog';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select';
import { Badge } from '@/components/ui/badge';
import { Alert, AlertDescription } from '@/components/ui/alert';
import { Slider } from '@/components/ui/slider';

interface WeightEstimatorProps {
  onWeightSelect: (weight: number) => void;
  currentWeight?: number;
  packageType?: string;
  dimensions?: { length: number; width: number; height: number };
}

const ITEM_CATEGORIES = [
  {
    id: 'electronics',
    name: 'Electronics',
    items: [
      { name: 'Smartphone', weight: 0.2, density: 0.8 },
      { name: 'Laptop', weight: 2.5, density: 0.6 },
      { name: 'Tablet', weight: 0.5, density: 0.7 },
      { name: 'Headphones', weight: 0.3, density: 0.3 },
      { name: 'Smartwatch', weight: 0.1, density: 0.9 },
      { name: 'Camera', weight: 1.2, density: 0.8 },
    ],
  },
  {
    id: 'clothing',
    name: 'Clothing & Accessories',
    items: [
      { name: 'T-shirt', weight: 0.15, density: 0.1 },
      { name: 'Jeans', weight: 0.8, density: 0.2 },
      { name: 'Jacket', weight: 1.2, density: 0.15 },
      { name: 'Shoes', weight: 0.9, density: 0.4 },
      { name: 'Belt', weight: 0.3, density: 0.5 },
      { name: 'Handbag', weight: 0.8, density: 0.2 },
    ],
  },
  {
    id: 'books',
    name: 'Books & Documents',
    items: [
      { name: 'Paperback Book', weight: 0.3, density: 0.7 },
      { name: 'Hardcover Book', weight: 0.8, density: 0.8 },
      { name: 'Magazine', weight: 0.1, density: 0.3 },
      { name: 'Document Stack', weight: 0.5, density: 0.6 },
    ],
  },
  {
    id: 'household',
    name: 'Household Items',
    items: [
      { name: 'Kitchen Utensils', weight: 0.5, density: 0.3 },
      { name: 'Decorative Item', weight: 1.0, density: 0.4 },
      { name: 'Small Appliance', weight: 3.0, density: 0.7 },
      { name: 'Pillow', weight: 0.8, density: 0.1 },
      { name: 'Blanket', weight: 1.5, density: 0.15 },
    ],
  },
];

const MATERIAL_DENSITIES = {
  plastic: 0.9,
  metal: 7.8,
  wood: 0.6,
  glass: 2.5,
  fabric: 0.2,
  paper: 0.8,
  electronic: 1.5,
};

export const WeightEstimator: React.FC<WeightEstimatorProps> = ({ 
  onWeightSelect, 
  currentWeight, 
  packageType,
  dimensions 
}) => {
  const [selectedCategory, setSelectedCategory] = useState<string>('');
  const [selectedItems, setSelectedItems] = useState<{ item: any; quantity: number }[]>([]);
  const [customWeight, setCustomWeight] = useState<number>(currentWeight || 1);
  const [estimationMethod, setEstimationMethod] = useState<'items' | 'density' | 'custom'>('items');
  const [selectedMaterial, setSelectedMaterial] = useState<string>('');
  const [densityFactor, setDensityFactor] = useState<number[]>([50]);

  const calculateVolumetricWeight = () => {
    if (!dimensions) return 0;
    const volume = (dimensions.length * dimensions.width * dimensions.height) / 1000; // Convert to liters
    return volume * (densityFactor[0] / 100); // Adjustable density factor
  };

  const calculateItemsWeight = () => {
    return selectedItems.reduce((total, selected) => {
      return total + (selected.item.weight * selected.quantity);
    }, 0);
  };

  const calculateDensityWeight = () => {
    if (!dimensions || !selectedMaterial) return 0;
    const volume = (dimensions.length * dimensions.width * dimensions.height) / 1000000; // Convert to cubic meters
    const density = MATERIAL_DENSITIES[selectedMaterial as keyof typeof MATERIAL_DENSITIES];
    return volume * density * 1000; // Convert to kg
  };

  const getWeightEstimate = () => {
    switch (estimationMethod) {
      case 'items':
        return calculateItemsWeight();
      case 'density':
        return calculateDensityWeight();
      case 'custom':
        return customWeight;
      default:
        return currentWeight || 1;
    }
  };

  const handleAddItem = (item: any) => {
    const existingIndex = selectedItems.findIndex(selected => selected.item.name === item.name);
    if (existingIndex >= 0) {
      const updated = [...selectedItems];
      updated[existingIndex].quantity += 1;
      setSelectedItems(updated);
    } else {
      setSelectedItems([...selectedItems, { item, quantity: 1 }]);
    }
  };

  const handleRemoveItem = (itemName: string) => {
    setSelectedItems(selectedItems.filter(selected => selected.item.name !== itemName));
  };

  const handleQuantityChange = (itemName: string, quantity: number) => {
    const updated = selectedItems.map(selected => 
      selected.item.name === itemName 
        ? { ...selected, quantity: Math.max(0, quantity) }
        : selected
    ).filter(selected => selected.quantity > 0);
    setSelectedItems(updated);
  };

  const handleWeightConfirm = () => {
    const estimatedWeight = getWeightEstimate();
    onWeightSelect(estimatedWeight);
  };

  const volumetricWeight = calculateVolumetricWeight();
  const estimatedWeight = getWeightEstimate();

  return (
    <Dialog>
      <DialogTrigger asChild>
        <Button variant="outline" className="w-full">
          <Scale className="h-4 w-4 mr-2" />
          Weight Estimator
        </Button>
      </DialogTrigger>
      <DialogContent className="max-w-4xl">
        <DialogHeader>
          <DialogTitle className="flex items-center gap-2">
            <Calculator className="h-5 w-5" />
            Smart Weight Estimator
          </DialogTitle>
        </DialogHeader>
        
        <div className="space-y-6">
          {/* Current Weight Display */}
          <Card>
            <CardHeader>
              <CardTitle className="text-lg">Weight Estimation</CardTitle>
              <CardDescription>
                Current weight: {currentWeight ? `${currentWeight}kg` : 'Not set'}
              </CardDescription>
            </CardHeader>
            <CardContent>
              <div className="flex gap-4 items-center">
                <div className="flex-1">
                  <div className="text-3xl font-bold text-blue-600">
                    {estimatedWeight.toFixed(2)} kg
                  </div>
                  <div className="text-sm text-gray-600">
                    Estimated weight
                  </div>
                </div>
                {dimensions && (
                  <div>
                    <div className="text-lg font-semibold text-gray-700">
                      {volumetricWeight.toFixed(2)} kg
                    </div>
                    <div className="text-sm text-gray-600">
                      Volumetric weight
                    </div>
                  </div>
                )}
                <Button onClick={handleWeightConfirm}>
                  Use This Weight
                </Button>
              </div>
            </CardContent>
          </Card>

          {/* Estimation Method Selection */}
          <div className="flex gap-2 mb-4">
            <Button
              variant={estimationMethod === 'items' ? 'default' : 'outline'}
              onClick={() => setEstimationMethod('items')}
            >
              By Items
            </Button>
            <Button
              variant={estimationMethod === 'density' ? 'default' : 'outline'}
              onClick={() => setEstimationMethod('density')}
            >
              By Material
            </Button>
            <Button
              variant={estimationMethod === 'custom' ? 'default' : 'outline'}
              onClick={() => setEstimationMethod('custom')}
            >
              Manual Input
            </Button>
          </div>

          {/* Items Method */}
          {estimationMethod === 'items' && (
            <div className="space-y-4">
              <div>
                <Label htmlFor="category">Select Category</Label>
                <Select value={selectedCategory} onValueChange={setSelectedCategory}>
                  <SelectTrigger>
                    <SelectValue placeholder="Choose item category" />
                  </SelectTrigger>
                  <SelectContent>
                    {ITEM_CATEGORIES.map((category) => (
                      <SelectItem key={category.id} value={category.id}>
                        {category.name}
                      </SelectItem>
                    ))}
                  </SelectContent>
                </Select>
              </div>

              {selectedCategory && (
                <div>
                  <h4 className="font-medium mb-2">Available Items</h4>
                  <div className="grid grid-cols-2 md:grid-cols-3 gap-2">
                    {ITEM_CATEGORIES
                      .find(cat => cat.id === selectedCategory)
                      ?.items.map((item) => (
                        <Button
                          key={item.name}
                          variant="outline"
                          onClick={() => handleAddItem(item)}
                          className="justify-start"
                        >
                          <Package className="h-4 w-4 mr-2" />
                          {item.name}
                          <Badge variant="secondary" className="ml-2">
                            {item.weight}kg
                          </Badge>
                        </Button>
                      ))}
                  </div>
                </div>
              )}

              {selectedItems.length > 0 && (
                <div>
                  <h4 className="font-medium mb-2">Selected Items</h4>
                  <div className="space-y-2">
                    {selectedItems.map((selected) => (
                      <div key={selected.item.name} className="flex items-center justify-between p-2 border rounded">
                        <div className="flex items-center gap-2">
                          <span className="font-medium">{selected.item.name}</span>
                          <Badge variant="outline">{selected.item.weight}kg each</Badge>
                        </div>
                        <div className="flex items-center gap-2">
                          <Label htmlFor={`qty-${selected.item.name}`} className="text-sm">Qty:</Label>
                          <Input
                            id={`qty-${selected.item.name}`}
                            type="number"
                            value={selected.quantity}
                            onChange={(e) => handleQuantityChange(selected.item.name, parseInt(e.target.value))}
                            className="w-20"
                            min="0"
                          />
                          <Button
                            variant="ghost"
                            size="sm"
                            onClick={() => handleRemoveItem(selected.item.name)}
                          >
                            Remove
                          </Button>
                        </div>
                      </div>
                    ))}
                  </div>
                </div>
              )}
            </div>
          )}

          {/* Density Method */}
          {estimationMethod === 'density' && (
            <div className="space-y-4">
              <div>
                <Label htmlFor="material">Primary Material</Label>
                <Select value={selectedMaterial} onValueChange={setSelectedMaterial}>
                  <SelectTrigger>
                    <SelectValue placeholder="Choose primary material" />
                  </SelectTrigger>
                  <SelectContent>
                    {Object.entries(MATERIAL_DENSITIES).map(([key, density]) => (
                      <SelectItem key={key} value={key}>
                        {key.charAt(0).toUpperCase() + key.slice(1)} (≈{density} kg/L)
                      </SelectItem>
                    ))}
                  </SelectContent>
                </Select>
              </div>

              {selectedMaterial && dimensions && (
                <div>
                  <Label>Density Factor: {densityFactor[0]}%</Label>
                  <Slider
                    value={densityFactor}
                    onValueChange={setDensityFactor}
                    min={10}
                    max={100}
                    step={5}
                    className="mt-2"
                  />
                  <div className="text-sm text-gray-600 mt-1">
                    Adjust based on how densely packed your items are
                  </div>
                </div>
              )}

              {!dimensions && (
                <Alert>
                  <Info className="h-4 w-4" />
                  <AlertDescription>
                    Package dimensions are required for density-based estimation.
                  </AlertDescription>
                </Alert>
              )}
            </div>
          )}

          {/* Custom Method */}
          {estimationMethod === 'custom' && (
            <div className="space-y-4">
              <div>
                <Label htmlFor="custom-weight">Enter Weight (kg)</Label>
                <Input
                  id="custom-weight"
                  type="number"
                  value={customWeight}
                  onChange={(e) => setCustomWeight(parseFloat(e.target.value))}
                  min="0.1"
                  step="0.1"
                  placeholder="Enter weight in kg"
                />
              </div>
            </div>
          )}

          {/* Weight Comparison */}
          {dimensions && (
            <Alert>
              <Info className="h-4 w-4" />
              <AlertDescription>
                <strong>Shipping Weight:</strong> The final shipping weight will be the greater of 
                actual weight ({estimatedWeight.toFixed(2)}kg) or volumetric weight ({volumetricWeight.toFixed(2)}kg).
              </AlertDescription>
            </Alert>
          )}
        </div>
      </DialogContent>
    </Dialog>
  );
};