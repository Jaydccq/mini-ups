import type { Meta, StoryObj } from '@storybook/react';
import { StatsCard } from './stats-card';
import { Package, TrendingUp, TrendingDown, Truck, Users, DollarSign } from 'lucide-react';

const meta: Meta<typeof StatsCard> = {
  title: 'UI/StatsCard',
  component: StatsCard,
  parameters: {
    layout: 'centered',
    docs: {
      description: {
        component: 'A specialized card component for displaying statistics with optional icons and trend indicators.',
      },
    },
  },
  tags: ['autodocs'],
  argTypes: {
    title: {
      control: 'text',
      description: 'The title of the stat card',
    },
    value: {
      control: 'text',
      description: 'The main value to display',
    },
    description: {
      control: 'text',
      description: 'Optional description text',
    },
    trend: {
      control: 'object',
      description: 'Optional trend information with value, label, and type',
    },
  },
};

export default meta;

type Story = StoryObj<typeof meta>;

export const Default: Story = {
  args: {
    title: 'Total Shipments',
    value: '1,234',
    description: 'Total packages processed',
  },
};

export const WithIcon: Story = {
  args: {
    title: 'Total Shipments',
    value: '1,234',
    icon: Package,
    description: 'Total packages processed',
  },
};

export const WithPositiveTrend: Story = {
  args: {
    title: 'Active Shipments',
    value: '456',
    icon: Truck,
    trend: {
      value: 12.5,
      label: 'from last week',
      type: 'positive',
    },
  },
};

export const WithNegativeTrend: Story = {
  args: {
    title: 'Cancelled Orders',
    value: '23',
    icon: TrendingDown,
    trend: {
      value: -8.2,
      label: 'from last month',
      type: 'negative',
    },
  },
};

export const WithNeutralTrend: Story = {
  args: {
    title: 'Pending Orders',
    value: '89',
    icon: Package,
    trend: {
      value: 0.5,
      label: 'from yesterday',
      type: 'neutral',
    },
  },
};

export const Revenue: Story = {
  args: {
    title: 'Revenue',
    value: '$12,345',
    icon: DollarSign,
    description: 'Monthly revenue',
    trend: {
      value: 15.3,
      label: 'from last month',
      type: 'positive',
    },
  },
};

export const LargeNumber: Story = {
  args: {
    title: 'Total Users',
    value: '1,234,567',
    icon: Users,
    description: 'Registered customers',
    trend: {
      value: 23.1,
      label: 'growth this quarter',
      type: 'positive',
    },
  },
};

export const NoTrend: Story = {
  args: {
    title: 'Warehouses',
    value: '12',
    icon: Package,
    description: 'Active warehouse locations',
  },
};

export const DashboardExample: Story = {
  render: () => (
    <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4 w-full max-w-6xl">
      <StatsCard
        title="Total Shipments"
        value="1,234"
        icon={Package}
        trend={{
          value: 12.5,
          label: 'from last week',
          type: 'positive',
        }}
      />
      <StatsCard
        title="Active Deliveries"
        value="456"
        icon={Truck}
        trend={{
          value: 8.2,
          label: 'from yesterday',
          type: 'positive',
        }}
      />
      <StatsCard
        title="Revenue"
        value="$12,345"
        icon={DollarSign}
        trend={{
          value: -2.1,
          label: 'from last month',
          type: 'negative',
        }}
      />
      <StatsCard
        title="Customers"
        value="8,945"
        icon={Users}
        trend={{
          value: 5.4,
          label: 'new this month',
          type: 'positive',
        }}
      />
    </div>
  ),
  parameters: {
    docs: {
      description: {
        story: 'Example of how stats cards would appear in a typical dashboard layout',
      },
    },
  },
};