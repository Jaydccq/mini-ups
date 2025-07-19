import React from 'react';

// Simple chart components to simulate Recharts functionality
// In a real implementation, you would import these from 'recharts'

export interface ChartData {
  [key: string]: any;
}

interface LineChartProps {
  width?: number;
  height?: number;
  data: ChartData[];
  children: React.ReactNode;
}

export const LineChart: React.FC<LineChartProps> = ({ 
  width = 400, 
  height = 300, 
  data, 
  children 
}) => (
  <div className="relative bg-white border rounded-lg p-4" style={{ width, height }}>
    <svg width={width - 32} height={height - 32} className="overflow-visible">
      {/* Simple line chart simulation */}
      <defs>
        <linearGradient id="lineGradient" x1="0%" y1="0%" x2="0%" y2="100%">
          <stop offset="0%" stopColor="#3b82f6" stopOpacity={0.8} />
          <stop offset="100%" stopColor="#3b82f6" stopOpacity={0.1} />
        </linearGradient>
      </defs>
      
      {/* Grid lines */}
      {Array.from({ length: 5 }).map((_, i) => (
        <line
          key={i}
          x1={0}
          y1={((height - 32) / 4) * i}
          x2={width - 32}
          y2={((height - 32) / 4) * i}
          stroke="#f1f5f9"
          strokeWidth={1}
        />
      ))}
      
      {/* Sample data line */}
      {data.length > 1 && (
        <polyline
          fill="none"
          stroke="#3b82f6"
          strokeWidth={2}
          points={data.map((_, i) => 
            `${(i / (data.length - 1)) * (width - 32)},${
              (height - 32) - (Math.random() * (height - 64) + 32)
            }`
          ).join(' ')}
        />
      )}
    </svg>
    {children}
  </div>
);

interface BarChartProps {
  width?: number;
  height?: number;
  data: ChartData[];
  children: React.ReactNode;
}

export const BarChart: React.FC<BarChartProps> = ({ 
  width = 400, 
  height = 300, 
  data, 
  children 
}) => (
  <div className="relative bg-white border rounded-lg p-4" style={{ width, height }}>
    <svg width={width - 32} height={height - 32}>
      {/* Grid lines */}
      {Array.from({ length: 5 }).map((_, i) => (
        <line
          key={i}
          x1={0}
          y1={((height - 32) / 4) * i}
          x2={width - 32}
          y2={((height - 32) / 4) * i}
          stroke="#f1f5f9"
          strokeWidth={1}
        />
      ))}
      
      {/* Sample bars */}
      {data.map((_, i) => (
        <rect
          key={i}
          x={(i / data.length) * (width - 32) + 10}
          y={(height - 32) - (Math.random() * (height - 64) + 32)}
          width={(width - 32) / data.length - 20}
          height={Math.random() * (height - 64) + 32}
          fill="#3b82f6"
          opacity={0.8}
        />
      ))}
    </svg>
    {children}
  </div>
);

interface PieChartProps {
  width?: number;
  height?: number;
  data: ChartData[];
  children: React.ReactNode;
}

export const PieChart: React.FC<PieChartProps> = ({ 
  width = 300, 
  height = 300, 
  data, 
  children 
}) => {
  const radius = Math.min(width, height) / 4;
  const centerX = (width - 32) / 2;
  const centerY = (height - 32) / 2;
  
  const colors = ['#3b82f6', '#10b981', '#f59e0b', '#ef4444', '#8b5cf6'];
  
  return (
    <div className="relative bg-white border rounded-lg p-4" style={{ width, height }}>
      <svg width={width - 32} height={height - 32}>
        {data.map((_, i) => {
          const startAngle = (i / data.length) * 2 * Math.PI;
          const endAngle = ((i + 1) / data.length) * 2 * Math.PI;
          const x1 = centerX + radius * Math.cos(startAngle);
          const y1 = centerY + radius * Math.sin(startAngle);
          const x2 = centerX + radius * Math.cos(endAngle);
          const y2 = centerY + radius * Math.sin(endAngle);
          const largeArcFlag = endAngle - startAngle > Math.PI ? 1 : 0;
          
          return (
            <path
              key={i}
              d={`M ${centerX},${centerY} L ${x1},${y1} A ${radius},${radius} 0 ${largeArcFlag},1 ${x2},${y2} Z`}
              fill={colors[i % colors.length]}
              opacity={0.8}
            />
          );
        })}
      </svg>
      {children}
    </div>
  );
};

// Simple chart component props
export const XAxis: React.FC<{ dataKey: string }> = ({ dataKey }) => null;
export const YAxis: React.FC<{}> = () => null;
export const CartesianGrid: React.FC<{ strokeDasharray?: string }> = () => null;
export const Tooltip: React.FC<{}> = () => null;
export const Legend: React.FC<{}> = () => null;
export const Line: React.FC<{ type?: string; dataKey: string; stroke: string; strokeWidth?: number }> = () => null;
export const Bar: React.FC<{ dataKey: string; fill: string }> = () => null;
export const Cell: React.FC<{ fill: string }> = () => null;