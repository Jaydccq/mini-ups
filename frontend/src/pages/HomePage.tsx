import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { Button } from '@/components/ui/button';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
import { Input } from '@/components/ui/input';
import { Badge } from '@/components/ui/badge';
import { Skeleton } from '@/components/ui/skeleton';
import { useTinaContent } from '@/hooks/useTinaContent';
import * as LucideIcons from 'lucide-react';
import { Package, Search, CheckCircle, ArrowRight, Star } from 'lucide-react';

// Helper function to dynamically get Lucide icons
const getLucideIcon = (iconName: string) => {
  const IconComponent = (LucideIcons as any)[iconName];
  return IconComponent ? <IconComponent className="h-8 w-8 text-primary" /> : <Package className="h-8 w-8 text-primary" />;
};

export const HomePage: React.FC = () => {
  const [trackingNumber, setTrackingNumber] = useState('');
  const navigate = useNavigate();
  const { content, isLoading, error, hero, features, testimonials, cta } = useTinaContent();

  const handleTrackPackage = () => {
    if (trackingNumber.trim()) {
      navigate(`/tracking?number=${trackingNumber}`);
    }
  };

  const handleCTAClick = () => {
    navigate(cta.button_link);
  };

  // Loading state
  if (isLoading) {
    return (
      <div className="min-h-screen">
        <section className="py-20">
          <div className="container mx-auto px-4">
            <div className="max-w-4xl mx-auto text-center space-y-6">
              <Skeleton className="h-6 w-48 mx-auto" />
              <Skeleton className="h-16 w-full max-w-2xl mx-auto" />
              <Skeleton className="h-4 w-full max-w-xl mx-auto" />
              <div className="flex gap-4 justify-center">
                <Skeleton className="h-12 w-32" />
                <Skeleton className="h-12 w-32" />
              </div>
            </div>
          </div>
        </section>
      </div>
    );
  }

  // Error fallback - will use fallback content from hook
  if (error) {
    console.warn('Using fallback content due to:', error);
  }

  return (
    <div className="min-h-screen">
      {/* Hero Section */}
      <section className="bg-gradient-to-br from-primary/10 via-background to-accent/5 py-20">
        <div className="container mx-auto px-4">
          <div className="max-w-4xl mx-auto text-center">
            <Badge className="mb-6" variant="secondary">
              <Package className="h-4 w-4 mr-2" />
              {hero.badge_text}
            </Badge>
            
            <h1 className="text-4xl md:text-6xl font-bold mb-6 bg-gradient-to-r from-primary to-accent bg-clip-text text-transparent">
              {hero.headline}
            </h1>
            
            <p className="text-lg md:text-xl text-muted-foreground mb-8 max-w-2xl mx-auto">
              {hero.subheadline}
            </p>

            {/* Quick Tracking Tool */}
            <Card className="max-w-md mx-auto mb-8">
              <CardHeader>
                <CardTitle className="flex items-center gap-2">
                  <Search className="h-5 w-5" />
                  Track Your Package
                </CardTitle>
                <CardDescription>
                  Enter your tracking number to get real-time updates
                </CardDescription>
              </CardHeader>
              <CardContent>
                <div className="flex gap-2">
                  <Input
                    placeholder="Enter tracking number"
                    value={trackingNumber}
                    onChange={(e) => setTrackingNumber(e.target.value)}
                    onKeyPress={(e) => e.key === 'Enter' && handleTrackPackage()}
                    className="flex-1"
                  />
                  <Button onClick={handleTrackPackage} disabled={!trackingNumber.trim()}>
                    <Search className="h-4 w-4" />
                  </Button>
                </div>
              </CardContent>
            </Card>

            <div className="flex flex-col sm:flex-row gap-4 justify-center">
              <Button size="lg" className="text-lg px-8" onClick={handleCTAClick}>
                {hero.cta_primary}
                <ArrowRight className="h-5 w-5 ml-2" />
              </Button>
              <Button size="lg" variant="outline" className="text-lg px-8" onClick={() => navigate('/login')}>
                {hero.cta_secondary}
              </Button>
            </div>
          </div>
        </div>
      </section>

      {/* Features Section */}
      <section className="py-20 bg-background">
        <div className="container mx-auto px-4">
          <div className="max-w-4xl mx-auto">
            <div className="text-center mb-16">
              <h2 className="text-3xl md:text-4xl font-bold mb-4">{features.section_title}</h2>
              <p className="text-lg text-muted-foreground">
                {features.section_description}
              </p>
            </div>

            <div className="grid md:grid-cols-2 lg:grid-cols-4 gap-6">
              {features.feature_list.map((feature, index) => (
                <Card key={index} className="text-center hover:shadow-lg transition-shadow">
                  <CardHeader>
                    <div className="flex justify-center mb-4">
                      {getLucideIcon(feature.icon)}
                    </div>
                    <CardTitle className="text-xl">{feature.title}</CardTitle>
                  </CardHeader>
                  <CardContent>
                    <p className="text-muted-foreground">{feature.description}</p>
                  </CardContent>
                </Card>
              ))}
            </div>
          </div>
        </div>
      </section>

      {/* Testimonials Section */}
      <section className="py-20 bg-muted/30">
        <div className="container mx-auto px-4">
          <div className="max-w-4xl mx-auto">
            <div className="text-center mb-16">
              <h2 className="text-3xl md:text-4xl font-bold mb-4">{testimonials.section_title}</h2>
              <p className="text-lg text-muted-foreground">
                {testimonials.section_description}
              </p>
            </div>

            <div className="grid md:grid-cols-3 gap-6">
              {testimonials.testimonial_list.map((testimonial, index) => (
                <Card key={index} className="hover:shadow-lg transition-shadow">
                  <CardHeader>
                    <div className="flex items-center gap-1 mb-2">
                      {Array.from({ length: testimonial.rating }).map((_, i) => (
                        <Star key={i} className="h-4 w-4 fill-yellow-400 text-yellow-400" />
                      ))}
                    </div>
                    <CardTitle className="text-lg">{testimonial.name}</CardTitle>
                    <CardDescription>{testimonial.role}</CardDescription>
                  </CardHeader>
                  <CardContent>
                    <p className="text-muted-foreground italic">"{testimonial.content}"</p>
                  </CardContent>
                </Card>
              ))}
            </div>
          </div>
        </div>
      </section>

      {/* CTA Section */}
      <section className={`py-20 ${cta.background_color} text-primary-foreground`}>
        <div className="container mx-auto px-4">
          <div className="max-w-2xl mx-auto text-center">
            <h2 className="text-3xl md:text-4xl font-bold mb-6">
              {cta.title}
            </h2>
            <p className="text-lg mb-8 opacity-90">
              {cta.description}
            </p>
            <Button size="lg" variant="secondary" className="text-lg px-8" onClick={handleCTAClick}>
              {cta.button_text}
              <CheckCircle className="h-5 w-5 ml-2" />
            </Button>
          </div>
        </div>
      </section>
    </div>
  );
};