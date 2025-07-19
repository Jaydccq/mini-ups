import React, { useState } from 'react';
import { Package, Ruler, Info, Check, AlertTriangle } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
import { Dialog, DialogContent, DialogHeader, DialogTitle, DialogTrigger } from '@/components/ui/dialog';
import { Badge } from '@/components/ui/badge';
import { Alert, AlertDescription } from '@/components/ui/alert';
import { Progress } from '@/components/ui/progress';

interface PackageSizeGuideProps {
  onSizeSelect: (dimensions: { length: number; width: number; height: number }, type: string) => void;
  currentDimensions?: { length: number; width: number; height: number };
}

const PACKAGE_SIZES = [
  {
    id: 'small',
    name: 'Small Package',
    description: 'Books, electronics, small items',
    dimensions: { length: 20, width: 15, height: 10 },
    maxWeight: 5,
    icon: '📦',
    color: 'bg-green-100 text-green-800',
    examples: ['Books', 'Jewelry', 'Small electronics', 'Documents'],
  },
  {
    id: 'medium',
    name: 'Medium Package',
    description: 'Clothing, shoes, medium items',
    dimensions: { length: 35, width: 25, height: 15 },
    maxWeight: 15,
    icon: '📋',
    color: 'bg-blue-100 text-blue-800',
    examples: ['Clothing', 'Shoes', 'Kitchen items', 'Sports equipment'],
  },
  {
    id: 'large',
    name: 'Large Package',
    description: 'Home goods, large items',
    dimensions: { length: 50, width: 40, height: 30 },
    maxWeight: 30,
    icon: '📦',
    color: 'bg-purple-100 text-purple-800',
    examples: ['Home appliances', 'Furniture parts', 'Large electronics', 'Sporting goods'],
  },
  {
    id: 'extra-large',
    name: 'Extra Large Package',
    description: 'Bulky items, furniture',
    dimensions: { length: 80, width: 60, height: 40 },
    maxWeight: 50,
    icon: '📦',
    color: 'bg-orange-100 text-orange-800',
    examples: ['Furniture', 'Large appliances', 'Bicycles', 'Artwork'],
  },
];

const PACKAGING_TIPS = [
  {
    title: 'Fragile Items',
    description: 'Use bubble wrap and padding',
    icon: '⚠️',
  },
  {
    title: 'Electronics',
    description: 'Anti-static protection recommended',
    icon: '🔌',
  },
  {
    title: 'Liquids',
    description: 'Secure containers, leak-proof packaging',
    icon: '💧',
  },
  {
    title: 'Documents',
    description: 'Waterproof envelope or bag',
    icon: '📄',
  },
];

export const PackageSizeGuide: React.FC<PackageSizeGuideProps> = ({ 
  onSizeSelect, 
  currentDimensions 
}) => {
  const [selectedSize, setSelectedSize] = useState<string | null>(null);
  const [showCustom, setShowCustom] = useState(false);

  const calculateVolume = (dimensions: { length: number; width: number; height: number }) => {
    return (dimensions.length * dimensions.width * dimensions.height) / 1000; // Convert to liters
  };

  const getSizeRecommendation = (volume: number) => {
    if (volume <= 3) return 'Small';
    if (volume <= 13) return 'Medium';
    if (volume <= 60) return 'Large';
    return 'Extra Large';
  };

  const currentVolume = currentDimensions ? calculateVolume(currentDimensions) : 0;

  const handleSizeSelect = (size: typeof PACKAGE_SIZES[0]) => {
    setSelectedSize(size.id);
    onSizeSelect(size.dimensions, size.id);
  };

  const PackageSizeVisualizer = ({ dimensions, name }: { dimensions: { length: number; width: number; height: number }, name: string }) => {
    const scale = 0.5; // Scale factor for visualization
    const scaledDimensions = {
      width: dimensions.length * scale,
      height: dimensions.height * scale,
      depth: dimensions.width * scale,
    };

    return (
      <div className="flex flex-col items-center space-y-2">
        <div className="relative flex items-end justify-center">
          {/* 3D Box visualization */}
          <div 
            className="relative border-2 border-gray-400 bg-gray-100"
            style={{
              width: `${scaledDimensions.width}px`,
              height: `${scaledDimensions.height}px`,
              transform: 'perspective(100px) rotateX(15deg) rotateY(15deg)',
            }}
          >
            {/* Front face */}
            <div className="absolute inset-0 bg-blue-200 border border-blue-300 opacity-80"></div>
            
            {/* Top face */}
            <div 
              className="absolute bg-blue-100 border border-blue-300 opacity-90"
              style={{
                width: `${scaledDimensions.width}px`,
                height: `${scaledDimensions.depth}px`,
                transform: `rotateX(90deg) translateZ(${scaledDimensions.height}px)`,
                transformOrigin: 'top',
              }}
            ></div>
            
            {/* Right face */}
            <div 
              className="absolute bg-blue-300 border border-blue-400 opacity-70"
              style={{
                width: `${scaledDimensions.depth}px`,
                height: `${scaledDimensions.height}px`,
                transform: `rotateY(90deg) translateZ(${scaledDimensions.width}px)`,
                transformOrigin: 'right',
              }}
            ></div>
          </div>
        </div>
        
        {/* Dimensions labels */}
        <div className="text-xs text-gray-600 space-y-1">
          <div className="flex justify-between">
            <span>L: {dimensions.length}cm</span>
            <span>W: {dimensions.width}cm</span>
            <span>H: {dimensions.height}cm</span>
          </div>
          <div className="text-center text-gray-500">
            Volume: {calculateVolume(dimensions).toFixed(1)}L
          </div>
        </div>
      </div>
    );
  };

  return (
    <Dialog>
      <DialogTrigger asChild>
        <Button variant="outline" className="w-full">
          <Package className="h-4 w-4 mr-2" />
          Package Size Guide
        </Button>
      </DialogTrigger>
      <DialogContent className="max-w-4xl">
        <DialogHeader>
          <DialogTitle className="flex items-center gap-2">
            <Ruler className="h-5 w-5" />
            Package Size Guide
          </DialogTitle>
        </DialogHeader>
        
        <div className="space-y-6">
          {/* Current Package Analysis */}
          {currentDimensions && (
            <Card>
              <CardHeader>
                <CardTitle className="text-lg">Your Package Analysis</CardTitle>
              </CardHeader>
              <CardContent>
                <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                  <div>
                    <PackageSizeVisualizer 
                      dimensions={currentDimensions} 
                      name="Your Package" 
                    />
                  </div>
                  <div className="space-y-4">
                    <div>
                      <Badge className="mb-2">
                        {getSizeRecommendation(currentVolume)} Package
                      </Badge>
                      <p className="text-sm text-gray-600">
                        Volume: {currentVolume.toFixed(1)} liters
                      </p>
                    </div>
                    
                    <div className="space-y-2">
                      <h4 className="font-medium">Packaging Recommendations:</h4>
                      <ul className="text-sm text-gray-600 space-y-1">
                        {currentVolume <= 3 && (
                          <li className="flex items-center gap-2">
                            <Check className="h-3 w-3 text-green-500" />
                            Use padded envelope or small box
                          </li>
                        )}
                        {currentVolume > 3 && currentVolume <= 13 && (
                          <li className="flex items-center gap-2">
                            <Check className="h-3 w-3 text-green-500" />
                            Standard shipping box with padding
                          </li>
                        )}
                        {currentVolume > 13 && (
                          <li className="flex items-center gap-2">
                            <Check className="h-3 w-3 text-green-500" />
                            Sturdy box with extra cushioning
                          </li>
                        )}
                      </ul>
                    </div>
                  </div>
                </div>
              </CardContent>
            </Card>
          )}

          {/* Standard Size Options */}
          <div>
            <h3 className="text-lg font-semibold mb-4">Standard Package Sizes</h3>
            <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
              {PACKAGE_SIZES.map((size) => (
                <Card 
                  key={size.id} 
                  className={`cursor-pointer transition-all hover:shadow-md ${
                    selectedSize === size.id ? 'ring-2 ring-blue-500' : ''
                  }`}
                  onClick={() => handleSizeSelect(size)}
                >
                  <CardHeader className="pb-2">
                    <div className="flex items-center justify-between">
                      <div className="flex items-center gap-2">
                        <span className="text-2xl">{size.icon}</span>
                        <div>
                          <CardTitle className="text-base">{size.name}</CardTitle>
                          <CardDescription className="text-sm">
                            {size.description}
                          </CardDescription>
                        </div>
                      </div>
                      <Badge className={size.color}>
                        Max {size.maxWeight}kg
                      </Badge>
                    </div>
                  </CardHeader>
                  <CardContent>
                    <div className="flex justify-between items-center mb-3">
                      <PackageSizeVisualizer 
                        dimensions={size.dimensions} 
                        name={size.name} 
                      />
                      <div className="text-right">
                        <div className="text-xs text-gray-600">
                          {size.dimensions.length} × {size.dimensions.width} × {size.dimensions.height} cm
                        </div>
                        <div className="text-xs text-gray-500">
                          {calculateVolume(size.dimensions).toFixed(1)}L volume
                        </div>
                      </div>
                    </div>
                    
                    <div className="text-xs text-gray-600">
                      <p className="font-medium mb-1">Suitable for:</p>
                      <div className="flex flex-wrap gap-1">
                        {size.examples.map((example, index) => (
                          <Badge key={index} variant="outline" className="text-xs">
                            {example}
                          </Badge>
                        ))}
                      </div>
                    </div>
                  </CardContent>
                </Card>
              ))}
            </div>
          </div>

          {/* Packaging Tips */}
          <div>
            <h3 className="text-lg font-semibold mb-4">Packaging Tips</h3>
            <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
              {PACKAGING_TIPS.map((tip, index) => (
                <Alert key={index}>
                  <div className="flex items-start gap-3">
                    <span className="text-lg">{tip.icon}</span>
                    <div>
                      <h4 className="font-medium">{tip.title}</h4>
                      <AlertDescription>{tip.description}</AlertDescription>
                    </div>
                  </div>
                </Alert>
              ))}
            </div>
          </div>

          {/* Shipping Restrictions */}
          <Alert variant="destructive">
            <AlertTriangle className="h-4 w-4" />
            <AlertDescription>
              <strong>Shipping Restrictions:</strong> Maximum dimensions: 100cm × 80cm × 60cm. 
              Maximum weight: 50kg. Prohibited items include hazardous materials, 
              perishable goods, and fragile items without proper packaging.
            </AlertDescription>
          </Alert>
        </div>
      </DialogContent>
    </Dialog>
  );
};