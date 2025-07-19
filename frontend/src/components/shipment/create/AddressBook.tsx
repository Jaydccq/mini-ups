import React, { useState } from 'react';
import { Book, Plus, MapPin, User, Phone, Mail, Check, X } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
import { Dialog, DialogContent, DialogHeader, DialogTitle, DialogTrigger } from '@/components/ui/dialog';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { Badge } from '@/components/ui/badge';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';

const addressSchema = z.object({
  name: z.string().min(1, 'Name is required'),
  email: z.string().email('Invalid email address'),
  phone: z.string().min(10, 'Phone number must be at least 10 digits'),
  address: z.string().min(1, 'Address is required'),
  label: z.string().min(1, 'Label is required'),
});

type Address = z.infer<typeof addressSchema>;

interface StoredAddress extends Address {
  id: string;
  createdAt: string;
  lastUsed?: string;
}

interface AddressBookProps {
  onSelectAddress: (address: Address) => void;
  currentAddress?: Partial<Address>;
  type: 'sender' | 'recipient';
}

const STORAGE_KEY = 'ups-address-book';

export const AddressBook: React.FC<AddressBookProps> = ({ 
  onSelectAddress, 
  currentAddress,
  type 
}) => {
  const [addresses, setAddresses] = useState<StoredAddress[]>(() => {
    const saved = localStorage.getItem(STORAGE_KEY);
    return saved ? JSON.parse(saved) : [];
  });
  const [isAddingNew, setIsAddingNew] = useState(false);
  const [searchTerm, setSearchTerm] = useState('');

  const form = useForm<Address>({
    resolver: zodResolver(addressSchema),
    defaultValues: {
      name: '',
      email: '',
      phone: '',
      address: '',
      label: '',
    },
  });

  const saveAddresses = (newAddresses: StoredAddress[]) => {
    setAddresses(newAddresses);
    localStorage.setItem(STORAGE_KEY, JSON.stringify(newAddresses));
  };

  const handleAddAddress = (data: Address) => {
    const newAddress: StoredAddress = {
      ...data,
      id: Date.now().toString(),
      createdAt: new Date().toISOString(),
    };
    
    const updatedAddresses = [newAddress, ...addresses];
    saveAddresses(updatedAddresses);
    form.reset();
    setIsAddingNew(false);
  };

  const handleSelectAddress = (address: StoredAddress) => {
    // Update last used date
    const updatedAddresses = addresses.map(addr =>
      addr.id === address.id ? { ...addr, lastUsed: new Date().toISOString() } : addr
    );
    saveAddresses(updatedAddresses);
    
    onSelectAddress(address);
  };

  const handleDeleteAddress = (addressId: string) => {
    const updatedAddresses = addresses.filter(addr => addr.id !== addressId);
    saveAddresses(updatedAddresses);
  };

  const filteredAddresses = addresses.filter(addr =>
    addr.name.toLowerCase().includes(searchTerm.toLowerCase()) ||
    addr.address.toLowerCase().includes(searchTerm.toLowerCase()) ||
    addr.label.toLowerCase().includes(searchTerm.toLowerCase())
  );

  const recentAddresses = addresses
    .filter(addr => addr.lastUsed)
    .sort((a, b) => new Date(b.lastUsed!).getTime() - new Date(a.lastUsed!).getTime())
    .slice(0, 3);

  return (
    <Dialog>
      <DialogTrigger asChild>
        <Button variant="outline" className="w-full">
          <Book className="h-4 w-4 mr-2" />
          Choose from Address Book
        </Button>
      </DialogTrigger>
      <DialogContent className="max-w-2xl">
        <DialogHeader>
          <DialogTitle>Address Book - {type === 'sender' ? 'Sender' : 'Recipient'}</DialogTitle>
        </DialogHeader>
        
        <div className="space-y-4">
          {/* Search */}
          <div className="relative">
            <Input
              placeholder="Search addresses..."
              value={searchTerm}
              onChange={(e) => setSearchTerm(e.target.value)}
              className="pr-10"
            />
            <MapPin className="absolute right-3 top-3 h-4 w-4 text-gray-400" />
          </div>

          {/* Recent Addresses */}
          {recentAddresses.length > 0 && (
            <div>
              <h3 className="text-sm font-medium mb-2">Recently Used</h3>
              <div className="grid gap-2">
                {recentAddresses.map((address) => (
                  <Card key={address.id} className="p-3 hover:shadow-md transition-shadow cursor-pointer">
                    <div className="flex justify-between items-start">
                      <div className="flex-1" onClick={() => handleSelectAddress(address)}>
                        <div className="flex items-center gap-2 mb-1">
                          <Badge variant="secondary" className="text-xs">
                            {address.label}
                          </Badge>
                          <span className="text-sm font-medium">{address.name}</span>
                        </div>
                        <p className="text-xs text-gray-600 mb-1">{address.address}</p>
                        <div className="flex gap-4 text-xs text-gray-500">
                          <span className="flex items-center gap-1">
                            <Mail className="h-3 w-3" />
                            {address.email}
                          </span>
                          <span className="flex items-center gap-1">
                            <Phone className="h-3 w-3" />
                            {address.phone}
                          </span>
                        </div>
                      </div>
                      <Button
                        variant="ghost"
                        size="sm"
                        onClick={() => handleDeleteAddress(address.id)}
                      >
                        <X className="h-4 w-4" />
                      </Button>
                    </div>
                  </Card>
                ))}
              </div>
            </div>
          )}

          {/* All Addresses */}
          <div>
            <div className="flex justify-between items-center mb-2">
              <h3 className="text-sm font-medium">All Addresses ({filteredAddresses.length})</h3>
              <Button
                variant="outline"
                size="sm"
                onClick={() => setIsAddingNew(true)}
              >
                <Plus className="h-4 w-4 mr-1" />
                Add New
              </Button>
            </div>

            {isAddingNew && (
              <Card className="mb-4">
                <CardHeader>
                  <CardTitle className="text-lg">Add New Address</CardTitle>
                </CardHeader>
                <CardContent>
                  <form onSubmit={form.handleSubmit(handleAddAddress)} className="space-y-4">
                    <div className="grid grid-cols-2 gap-4">
                      <div className="space-y-2">
                        <Label htmlFor="label">Label *</Label>
                        <Input
                          id="label"
                          {...form.register('label')}
                          placeholder="Home, Office, etc."
                        />
                        {form.formState.errors.label && (
                          <p className="text-sm text-red-600">{form.formState.errors.label.message}</p>
                        )}
                      </div>
                      <div className="space-y-2">
                        <Label htmlFor="name">Name *</Label>
                        <Input
                          id="name"
                          {...form.register('name')}
                          placeholder="Full name"
                        />
                        {form.formState.errors.name && (
                          <p className="text-sm text-red-600">{form.formState.errors.name.message}</p>
                        )}
                      </div>
                    </div>
                    <div className="grid grid-cols-2 gap-4">
                      <div className="space-y-2">
                        <Label htmlFor="email">Email *</Label>
                        <Input
                          id="email"
                          type="email"
                          {...form.register('email')}
                          placeholder="email@example.com"
                        />
                        {form.formState.errors.email && (
                          <p className="text-sm text-red-600">{form.formState.errors.email.message}</p>
                        )}
                      </div>
                      <div className="space-y-2">
                        <Label htmlFor="phone">Phone *</Label>
                        <Input
                          id="phone"
                          {...form.register('phone')}
                          placeholder="+1 (555) 123-4567"
                        />
                        {form.formState.errors.phone && (
                          <p className="text-sm text-red-600">{form.formState.errors.phone.message}</p>
                        )}
                      </div>
                    </div>
                    <div className="space-y-2">
                      <Label htmlFor="address">Address *</Label>
                      <Input
                        id="address"
                        {...form.register('address')}
                        placeholder="123 Main St, City, State, ZIP"
                      />
                      {form.formState.errors.address && (
                        <p className="text-sm text-red-600">{form.formState.errors.address.message}</p>
                      )}
                    </div>
                    <div className="flex gap-2">
                      <Button type="submit">
                        <Check className="h-4 w-4 mr-2" />
                        Save Address
                      </Button>
                      <Button type="button" variant="outline" onClick={() => setIsAddingNew(false)}>
                        <X className="h-4 w-4 mr-2" />
                        Cancel
                      </Button>
                    </div>
                  </form>
                </CardContent>
              </Card>
            )}

            <div className="grid gap-2 max-h-64 overflow-y-auto">
              {filteredAddresses.length === 0 ? (
                <p className="text-center text-gray-500 py-8">
                  No addresses found. Click "Add New" to create your first address.
                </p>
              ) : (
                filteredAddresses.map((address) => (
                  <Card key={address.id} className="p-3 hover:shadow-md transition-shadow cursor-pointer">
                    <div className="flex justify-between items-start">
                      <div className="flex-1" onClick={() => handleSelectAddress(address)}>
                        <div className="flex items-center gap-2 mb-1">
                          <Badge variant="secondary" className="text-xs">
                            {address.label}
                          </Badge>
                          <span className="text-sm font-medium">{address.name}</span>
                        </div>
                        <p className="text-xs text-gray-600 mb-1">{address.address}</p>
                        <div className="flex gap-4 text-xs text-gray-500">
                          <span className="flex items-center gap-1">
                            <Mail className="h-3 w-3" />
                            {address.email}
                          </span>
                          <span className="flex items-center gap-1">
                            <Phone className="h-3 w-3" />
                            {address.phone}
                          </span>
                        </div>
                      </div>
                      <Button
                        variant="ghost"
                        size="sm"
                        onClick={() => handleDeleteAddress(address.id)}
                      >
                        <X className="h-4 w-4" />
                      </Button>
                    </div>
                  </Card>
                ))
              )}
            </div>
          </div>
        </div>
      </DialogContent>
    </Dialog>
  );
};