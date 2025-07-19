import React, { useState } from 'react';
import { Shield, Package, AlertTriangle, CheckCircle, Info, Star } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
import { Dialog, DialogContent, DialogHeader, DialogTitle, DialogTrigger } from '@/components/ui/dialog';
import { Badge } from '@/components/ui/badge';
import { Alert, AlertDescription } from '@/components/ui/alert';
import { Tabs, TabsContent, TabsList, TabsTrigger } from '@/components/ui/tabs';
import { Progress } from '@/components/ui/progress';

interface PackagingRecommendationsProps {
  packageType?: string;
  weight?: number;
  dimensions?: { length: number; width: number; height: number };
  itemType?: string;
  value?: number;
}

const PACKAGING_MATERIALS = [
  {
    id: 'bubble-wrap',
    name: 'Bubble Wrap',
    description: 'Excellent for fragile items and cushioning',
    icon: '🫧',
    suitableFor: ['fragile', 'electronics', 'glassware'],
    protection: 9,
    cost: 3,
    eco: 4,
  },
  {
    id: 'foam-padding',
    name: 'Foam Padding',
    description: 'Superior protection for delicate items',
    icon: '🟡',
    suitableFor: ['electronics', 'fragile', 'artwork'],
    protection: 10,
    cost: 5,
    eco: 3,
  },
  {
    id: 'packing-peanuts',
    name: 'Packing Peanuts',
    description: 'Fill empty spaces, prevent movement',
    icon: '🥜',
    suitableFor: ['general', 'electronics', 'odd-shapes'],
    protection: 7,
    cost: 2,
    eco: 5,
  },
  {
    id: 'newspaper',
    name: 'Newspaper/Paper',
    description: 'Eco-friendly option for non-fragile items',
    icon: '📰',
    suitableFor: ['books', 'clothing', 'non-fragile'],
    protection: 5,
    cost: 1,
    eco: 9,
  },
  {
    id: 'air-pillows',
    name: 'Air Pillows',
    description: 'Lightweight void fill solution',
    icon: '💨',
    suitableFor: ['general', 'clothing', 'lightweight'],
    protection: 6,
    cost: 2,
    eco: 6,
  },
];

const BOX_TYPES = [
  {
    id: 'cardboard',
    name: 'Cardboard Box',
    description: 'Standard shipping box for most items',
    icon: '📦',
    strengthRating: 7,
    suitableFor: ['general', 'books', 'clothing'],
    maxWeight: 20,
    cost: 2,
  },
  {
    id: 'double-wall',
    name: 'Double-Wall Box',
    description: 'Extra strength for heavier items',
    icon: '📦',
    strengthRating: 9,
    suitableFor: ['heavy', 'fragile', 'electronics'],
    maxWeight: 50,
    cost: 4,
  },
  {
    id: 'padded-envelope',
    name: 'Padded Envelope',
    description: 'Lightweight protection for small items',
    icon: '📮',
    strengthRating: 5,
    suitableFor: ['documents', 'small-electronics', 'jewelry'],
    maxWeight: 2,
    cost: 1,
  },
  {
    id: 'tube',
    name: 'Mailing Tube',
    description: 'Perfect for documents and artwork',
    icon: '📜',
    strengthRating: 6,
    suitableFor: ['documents', 'artwork', 'posters'],
    maxWeight: 5,
    cost: 3,
  },
];

const ITEM_SPECIFIC_GUIDELINES = {
  electronics: {
    title: 'Electronics Packaging',
    icon: '📱',
    guidelines: [
      'Use anti-static bubble wrap or foam',
      'Include original packaging if available',
      'Ensure all cables are secured',
      'Add "FRAGILE" and "THIS SIDE UP" labels',
      'Consider insurance for valuable items',
    ],
    materials: ['foam-padding', 'bubble-wrap', 'air-pillows'],
    boxes: ['double-wall', 'cardboard'],
  },
  fragile: {
    title: 'Fragile Items',
    icon: '🔍',
    guidelines: [
      'Wrap each item individually',
      'Use plenty of cushioning material',
      'Fill all empty spaces to prevent movement',
      'Mark clearly as "FRAGILE"',
      'Consider double-boxing for very fragile items',
    ],
    materials: ['bubble-wrap', 'foam-padding', 'packing-peanuts'],
    boxes: ['double-wall', 'cardboard'],
  },
  clothing: {
    title: 'Clothing & Textiles',
    icon: '👔',
    guidelines: [
      'Fold items neatly to minimize wrinkles',
      'Use plastic bags for protection from moisture',
      'Consider vacuum sealing for space efficiency',
      'Add tissue paper for delicate fabrics',
    ],
    materials: ['newspaper', 'air-pillows'],
    boxes: ['cardboard', 'padded-envelope'],
  },
  books: {
    title: 'Books & Documents',
    icon: '📚',
    guidelines: [
      'Wrap in plastic for moisture protection',
      'Use sturdy box to prevent bending',
      'Don\'t overpack - books are heavy',
      'Consider media mail for cost savings',
    ],
    materials: ['newspaper', 'bubble-wrap'],
    boxes: ['cardboard', 'double-wall'],
  },
  artwork: {
    title: 'Artwork & Collectibles',
    icon: '🎨',
    guidelines: [
      'Use acid-free materials when possible',
      'Sandwich flat items between cardboard',
      'Consider professional art shipping services',
      'Include detailed handling instructions',
      'Document condition before shipping',
    ],
    materials: ['foam-padding', 'bubble-wrap'],
    boxes: ['double-wall', 'tube'],
  },
};

export const PackagingRecommendations: React.FC<PackagingRecommendationsProps> = ({ 
  packageType, 
  weight = 0, 
  dimensions,
  itemType = 'general',
  value = 0 
}) => {
  const [selectedGuideline, setSelectedGuideline] = useState<string>(itemType);

  const getPackagingScore = () => {
    let score = 0;
    
    // Base score
    score += 20;
    
    // Weight consideration
    if (weight > 0 && weight <= 2) score += 20;
    else if (weight > 2 && weight <= 10) score += 15;
    else if (weight > 10) score += 10;
    
    // Dimensions consideration
    if (dimensions) {
      const volume = dimensions.length * dimensions.width * dimensions.height;
      if (volume <= 10000) score += 20; // Small package
      else if (volume <= 50000) score += 15; // Medium package
      else score += 10; // Large package
    }
    
    // Value consideration
    if (value > 100) score += 20;
    else if (value > 50) score += 15;
    else score += 10;
    
    // Item type consideration
    if (itemType === 'fragile') score += 20;
    else if (itemType === 'electronics') score += 15;
    else score += 10;
    
    return Math.min(score, 100);
  };

  const getRecommendedMaterials = () => {
    const guidelines = ITEM_SPECIFIC_GUIDELINES[itemType as keyof typeof ITEM_SPECIFIC_GUIDELINES];
    if (!guidelines) return PACKAGING_MATERIALS.slice(0, 3);
    
    return PACKAGING_MATERIALS.filter(material => 
      guidelines.materials.includes(material.id)
    );
  };

  const getRecommendedBoxes = () => {
    const guidelines = ITEM_SPECIFIC_GUIDELINES[itemType as keyof typeof ITEM_SPECIFIC_GUIDELINES];
    if (!guidelines) return BOX_TYPES.slice(0, 2);
    
    return BOX_TYPES.filter(box => 
      guidelines.boxes.includes(box.id) && box.maxWeight >= weight
    );
  };

  const score = getPackagingScore();
  const recommendedMaterials = getRecommendedMaterials();
  const recommendedBoxes = getRecommendedBoxes();
  const guidelines = ITEM_SPECIFIC_GUIDELINES[selectedGuideline as keyof typeof ITEM_SPECIFIC_GUIDELINES];

  return (
    <Dialog>
      <DialogTrigger asChild>
        <Button variant="outline" className="w-full">
          <Shield className="h-4 w-4 mr-2" />
          Packaging Guide
        </Button>
      </DialogTrigger>
      <DialogContent className="max-w-4xl">
        <DialogHeader>
          <DialogTitle className="flex items-center gap-2">
            <Package className="h-5 w-5" />
            Smart Packaging Recommendations
          </DialogTitle>
        </DialogHeader>
        
        <div className="space-y-6">
          {/* Packaging Score */}
          <Card>
            <CardHeader>
              <CardTitle className="text-lg">Packaging Assessment</CardTitle>
            </CardHeader>
            <CardContent>
              <div className="flex items-center gap-4">
                <div className="flex-1">
                  <div className="flex items-center gap-2 mb-2">
                    <span className="text-2xl font-bold">{score}/100</span>
                    <Badge variant={score >= 80 ? 'default' : score >= 60 ? 'secondary' : 'destructive'}>
                      {score >= 80 ? 'Excellent' : score >= 60 ? 'Good' : 'Needs Improvement'}
                    </Badge>
                  </div>
                  <Progress value={score} className="h-2" />
                  <p className="text-sm text-gray-600 mt-1">
                    Based on item type, weight, dimensions, and value
                  </p>
                </div>
                <div className="text-right">
                  <div className="text-sm text-gray-600">
                    Weight: {weight}kg
                  </div>
                  {dimensions && (
                    <div className="text-sm text-gray-600">
                      Dimensions: {dimensions.length}×{dimensions.width}×{dimensions.height}cm
                    </div>
                  )}
                  <div className="text-sm text-gray-600">
                    Value: ${value}
                  </div>
                </div>
              </div>
            </CardContent>
          </Card>

          <Tabs defaultValue="recommendations" className="w-full">
            <TabsList className="grid w-full grid-cols-3">
              <TabsTrigger value="recommendations">Recommendations</TabsTrigger>
              <TabsTrigger value="materials">Materials</TabsTrigger>
              <TabsTrigger value="guidelines">Guidelines</TabsTrigger>
            </TabsList>
            
            <TabsContent value="recommendations" className="space-y-4">
              <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                <Card>
                  <CardHeader>
                    <CardTitle className="text-lg">Recommended Boxes</CardTitle>
                  </CardHeader>
                  <CardContent>
                    <div className="space-y-3">
                      {recommendedBoxes.map((box) => (
                        <div key={box.id} className="flex items-center justify-between p-3 border rounded-lg">
                          <div className="flex items-center gap-3">
                            <span className="text-2xl">{box.icon}</span>
                            <div>
                              <div className="font-medium">{box.name}</div>
                              <div className="text-sm text-gray-600">{box.description}</div>
                            </div>
                          </div>
                          <div className="text-right">
                            <Badge variant="outline">
                              Max {box.maxWeight}kg
                            </Badge>
                            <div className="text-sm text-gray-600">
                              Strength: {box.strengthRating}/10
                            </div>
                          </div>
                        </div>
                      ))}
                    </div>
                  </CardContent>
                </Card>

                <Card>
                  <CardHeader>
                    <CardTitle className="text-lg">Recommended Materials</CardTitle>
                  </CardHeader>
                  <CardContent>
                    <div className="space-y-3">
                      {recommendedMaterials.map((material) => (
                        <div key={material.id} className="flex items-center justify-between p-3 border rounded-lg">
                          <div className="flex items-center gap-3">
                            <span className="text-2xl">{material.icon}</span>
                            <div>
                              <div className="font-medium">{material.name}</div>
                              <div className="text-sm text-gray-600">{material.description}</div>
                            </div>
                          </div>
                          <div className="text-right">
                            <div className="flex gap-1">
                              <Badge variant="outline">
                                Protection: {material.protection}/10
                              </Badge>
                            </div>
                            <div className="text-sm text-gray-600">
                              Cost: {material.cost}/5 | Eco: {material.eco}/10
                            </div>
                          </div>
                        </div>
                      ))}
                    </div>
                  </CardContent>
                </Card>
              </div>
            </TabsContent>
            
            <TabsContent value="materials" className="space-y-4">
              <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                {PACKAGING_MATERIALS.map((material) => (
                  <Card key={material.id}>
                    <CardHeader>
                      <CardTitle className="flex items-center gap-2">
                        <span className="text-2xl">{material.icon}</span>
                        {material.name}
                      </CardTitle>
                      <CardDescription>{material.description}</CardDescription>
                    </CardHeader>
                    <CardContent>
                      <div className="space-y-2">
                        <div className="flex justify-between">
                          <span className="text-sm">Protection:</span>
                          <Progress value={material.protection * 10} className="h-2 w-20" />
                        </div>
                        <div className="flex justify-between">
                          <span className="text-sm">Cost:</span>
                          <Progress value={material.cost * 20} className="h-2 w-20" />
                        </div>
                        <div className="flex justify-between">
                          <span className="text-sm">Eco-friendly:</span>
                          <Progress value={material.eco * 10} className="h-2 w-20" />
                        </div>
                        <div className="flex flex-wrap gap-1 mt-2">
                          {material.suitableFor.map((type) => (
                            <Badge key={type} variant="secondary" className="text-xs">
                              {type}
                            </Badge>
                          ))}
                        </div>
                      </div>
                    </CardContent>
                  </Card>
                ))}
              </div>
            </TabsContent>
            
            <TabsContent value="guidelines" className="space-y-4">
              <div className="flex gap-2 mb-4">
                {Object.entries(ITEM_SPECIFIC_GUIDELINES).map(([key, guide]) => (
                  <Button
                    key={key}
                    variant={selectedGuideline === key ? 'default' : 'outline'}
                    onClick={() => setSelectedGuideline(key)}
                  >
                    <span className="mr-2">{guide.icon}</span>
                    {guide.title}
                  </Button>
                ))}
              </div>

              {guidelines && (
                <Card>
                  <CardHeader>
                    <CardTitle className="flex items-center gap-2">
                      <span className="text-2xl">{guidelines.icon}</span>
                      {guidelines.title}
                    </CardTitle>
                  </CardHeader>
                  <CardContent>
                    <div className="space-y-3">
                      {guidelines.guidelines.map((guideline, index) => (
                        <div key={index} className="flex items-start gap-2">
                          <CheckCircle className="h-4 w-4 text-green-500 mt-0.5 flex-shrink-0" />
                          <span className="text-sm">{guideline}</span>
                        </div>
                      ))}
                    </div>
                  </CardContent>
                </Card>
              )}
            </TabsContent>
          </Tabs>

          {/* Special Considerations */}
          <Alert>
            <AlertTriangle className="h-4 w-4" />
            <AlertDescription>
              <strong>Important:</strong> Always consider the fragility and value of your items. 
              For high-value items (${'>'}{value > 100 ? value : '100'}+), consider additional insurance 
              and signature confirmation. Weather conditions may also affect packaging choices.
            </AlertDescription>
          </Alert>
        </div>
      </DialogContent>
    </Dialog>
  );
};