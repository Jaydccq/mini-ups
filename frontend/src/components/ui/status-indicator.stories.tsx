import type { Meta, StoryObj } from '@storybook/react-vite';
import { StatusIndicator } from './status-indicator';

const meta: Meta<typeof StatusIndicator> = {
  title: 'UI/StatusIndicator',
  component: StatusIndicator,
  parameters: {
    layout: 'centered',
    docs: {
      description: {
        component: 'A specialized status indicator component for displaying shipment statuses with optional dots and descriptions.',
      },
    },
  },
  tags: ['autodocs'],
  argTypes: {
    status: {
      control: 'select',
      options: ['pending', 'processing', 'shipped', 'in_transit', 'out_for_delivery', 'delivered', 'cancelled', 'returned'],
      description: 'The shipment status to display',
    },
    showDot: {
      control: 'boolean',
      description: 'Whether to show a colored dot indicator',
    },
    animated: {
      control: 'boolean',
      description: 'Whether to animate the dot indicator',
    },
    showDescription: {
      control: 'boolean',
      description: 'Whether to show the status description below the badge',
    },
  },
};

export default meta;

type Story = StoryObj<typeof meta>;

export const Default: Story = {
  args: {
    status: 'in_transit',
  },
};

export const WithDot: Story = {
  args: {
    status: 'in_transit',
    showDot: true,
  },
};

export const WithAnimatedDot: Story = {
  args: {
    status: 'in_transit',
    showDot: true,
    animated: true,
  },
};

export const WithDescription: Story = {
  args: {
    status: 'in_transit',
    showDescription: true,
  },
};

export const FullIndicator: Story = {
  args: {
    status: 'in_transit',
    showDot: true,
    animated: true,
    showDescription: true,
  },
};

export const Pending: Story = {
  args: {
    status: 'pending',
    showDot: true,
    showDescription: true,
  },
};

export const Processing: Story = {
  args: {
    status: 'processing',
    showDot: true,
    animated: true,
    showDescription: true,
  },
};

export const Shipped: Story = {
  args: {
    status: 'shipped',
    showDot: true,
    showDescription: true,
  },
};

export const OutForDelivery: Story = {
  args: {
    status: 'out_for_delivery',
    showDot: true,
    animated: true,
    showDescription: true,
  },
};

export const Delivered: Story = {
  args: {
    status: 'delivered',
    showDot: true,
    showDescription: true,
  },
};

export const Cancelled: Story = {
  args: {
    status: 'cancelled',
    showDot: true,
    showDescription: true,
  },
};

export const AllStatuses: Story = {
  render: () => (
    <div className="space-y-4">
      <div className="grid grid-cols-2 gap-4">
        <StatusIndicator status="pending" showDot showDescription />
        <StatusIndicator status="processing" showDot animated showDescription />
        <StatusIndicator status="shipped" showDot showDescription />
        <StatusIndicator status="in_transit" showDot animated showDescription />
        <StatusIndicator status="out_for_delivery" showDot animated showDescription />
        <StatusIndicator status="delivered" showDot showDescription />
        <StatusIndicator status="cancelled" showDot showDescription />
        <StatusIndicator status="returned" showDot showDescription />
      </div>
    </div>
  ),
  parameters: {
    docs: {
      description: {
        story: 'All possible shipment statuses with dots and descriptions',
      },
    },
  },
};

export const RealTimeTracking: Story = {
  render: () => (
    <div className="space-y-3 w-80">
      <div className="border rounded-lg p-4">
        <h3 className="font-semibold mb-3">Package Tracking - UPS123456789</h3>
        <div className="space-y-2">
          <StatusIndicator status="delivered" showDot showDescription />
          <StatusIndicator status="out_for_delivery" showDot animated showDescription />
          <StatusIndicator status="in_transit" showDot showDescription />
          <StatusIndicator status="shipped" showDot showDescription />
          <StatusIndicator status="processing" showDot showDescription />
          <StatusIndicator status="pending" showDot showDescription />
        </div>
      </div>
    </div>
  ),
  parameters: {
    docs: {
      description: {
        story: 'Example of how status indicators would appear in a real tracking interface',
      },
    },
  },
};