import type { Meta, StoryObj } from '@storybook/react';
import { Badge } from './badge';

const meta: Meta<typeof Badge> = {
  title: 'UI/Badge',
  component: Badge,
  parameters: {
    layout: 'centered',
    docs: {
      description: {
        component: 'A badge component for displaying status information, with specialized variants for shipment statuses.',
      },
    },
  },
  tags: ['autodocs'],
  argTypes: {
    variant: {
      control: 'select',
      options: ['default', 'secondary', 'destructive', 'outline', 'pending', 'transit', 'delivered', 'success', 'warning'],
      description: 'The visual variant of the badge',
    },
    children: {
      control: 'text',
      description: 'The content of the badge',
    },
  },
};

export default meta;

type Story = StoryObj<typeof meta>;

export const Default: Story = {
  args: {
    children: 'Badge',
    variant: 'default',
  },
};

export const Secondary: Story = {
  args: {
    children: 'Secondary',
    variant: 'secondary',
  },
};

export const Destructive: Story = {
  args: {
    children: 'Error',
    variant: 'destructive',
  },
};

export const Outline: Story = {
  args: {
    children: 'Outline',
    variant: 'outline',
  },
};

export const Pending: Story = {
  args: {
    children: 'Pending',
    variant: 'pending',
  },
};

export const Transit: Story = {
  args: {
    children: 'In Transit',
    variant: 'transit',
  },
};

export const Delivered: Story = {
  args: {
    children: 'Delivered',
    variant: 'delivered',
  },
};

export const Success: Story = {
  args: {
    children: 'Success',
    variant: 'success',
  },
};

export const Warning: Story = {
  args: {
    children: 'Warning',
    variant: 'warning',
  },
};

export const ShipmentStatuses: Story = {
  render: () => (
    <div className="flex flex-wrap gap-4">
      <Badge variant="pending">Pending</Badge>
      <Badge variant="transit">In Transit</Badge>
      <Badge variant="delivered">Delivered</Badge>
    </div>
  ),
  parameters: {
    docs: {
      description: {
        story: 'Badge variants specifically designed for shipment statuses',
      },
    },
  },
};

export const AllVariants: Story = {
  render: () => (
    <div className="flex flex-wrap gap-4">
      <Badge variant="default">Default</Badge>
      <Badge variant="secondary">Secondary</Badge>
      <Badge variant="destructive">Destructive</Badge>
      <Badge variant="outline">Outline</Badge>
      <Badge variant="pending">Pending</Badge>
      <Badge variant="transit">Transit</Badge>
      <Badge variant="delivered">Delivered</Badge>
      <Badge variant="success">Success</Badge>
      <Badge variant="warning">Warning</Badge>
    </div>
  ),
  parameters: {
    docs: {
      description: {
        story: 'All badge variants displayed together',
      },
    },
  },
};