import React from 'react';
import { AlertCircle, RefreshCw } from 'lucide-react';

export interface ThresholdStep {
  value: number | null; // null represents the base value (-Infinity)
  color: string; // 'green', 'orange', 'red', or hex
}

export interface ValueMapping {
  value: number;
  text: string;
  color: string;
}

interface StatCardProps {
  title: string;
  value: number | string | null;
  unit?: string;
  decimals?: number;
  thresholds?: ThresholdStep[];
  mappings?: ValueMapping[];
  subtitle?: string;
  loading?: boolean;
  error?: string | null;
  colorMode?: 'background' | 'value' | 'none';
}

export const StatCard: React.FC<StatCardProps> = ({
  title,
  value,
  unit = '',
  decimals = 1,
  thresholds,
  mappings,
  subtitle,
  loading = false,
  error = null,
  colorMode = 'background',
}) => {
  const resolveColorAndText = (): { color: string; text: string } => {
    if (value === null || value === undefined || isNaN(Number(value))) {
      return { color: '#64748B', text: 'N/A' };
    }

    const numVal = Number(value);

    // 1. Check direct value mappings
    if (mappings && mappings.length > 0) {
      const match = mappings.find((m) => m.value === numVal);
      if (match) {
        return { color: resolveColorHex(match.color), text: match.text };
      }
    }

    // 2. Format number text
    let formattedText = numVal.toFixed(decimals);
    if (unit === 'percent' || unit === '%') {
      formattedText += '%';
    } else if (unit === 'h' || unit === 'hours') {
      formattedText += ' h';
    } else if (unit === 'short' || unit === '') {
      formattedText = numVal.toLocaleString(undefined, { maximumFractionDigits: decimals });
    }

    // 3. Resolve threshold color
    let chosenColor = '#10B981'; // default green
    if (thresholds && thresholds.length > 0) {
      // Sort threshold steps ascending by value
      const sorted = [...thresholds].sort((a, b) => (a.value ?? -Infinity) - (b.value ?? -Infinity));
      for (const step of sorted) {
        if (step.value === null || numVal >= step.value) {
          chosenColor = resolveColorHex(step.color);
        }
      }
    }

    return { color: chosenColor, text: formattedText };
  };

  const resolveColorHex = (c: string): string => {
    switch (c) {
      case 'green':
        return '#10B981';
      case 'orange':
      case 'yellow':
        return '#F5A300';
      case 'red':
        return '#EF4444';
      default:
        return c;
    }
  };

  const { color, text } = resolveColorAndText();

  const isBgMode = colorMode === 'background';
  const bgColor = isBgMode ? `${color}1A` : 'white'; // 10% opacity hex
  const borderColor = isBgMode ? `${color}40` : 'var(--border-color)';
  const textColor = isBgMode ? color : (colorMode === 'value' ? color : 'var(--text-main)');

  return (
    <div
      className="card"
      style={{
        padding: '1.25rem',
        backgroundColor: bgColor,
        borderColor: borderColor,
        display: 'flex',
        flexDirection: 'column',
        justifyContent: 'space-between',
        position: 'relative',
        height: '100%',
        minHeight: '140px',
      }}
    >
      <div>
        <div style={{ fontSize: '0.85rem', fontWeight: 600, color: 'var(--text-muted)', marginBottom: '0.5rem' }}>
          {title}
        </div>

        {loading ? (
          <div style={{ display: 'flex', alignItems: 'center', gap: '0.4rem', color: '#64748B', fontSize: '0.875rem' }}>
            <RefreshCw size={16} className="spin" /> Loading metric...
          </div>
        ) : error ? (
          <div style={{ display: 'flex', alignItems: 'center', gap: '0.3rem', color: '#EF4444', fontSize: '0.75rem' }}>
            <AlertCircle size={14} /> Error fetching metric
          </div>
        ) : (
          <div
            className="tabular-nums"
            style={{
              fontSize: '2.2rem',
              fontWeight: 800,
              color: textColor,
              letterSpacing: '-0.5px',
              lineHeight: 1.1,
            }}
          >
            {text}
          </div>
        )}
      </div>

      {subtitle && (
        <div style={{ fontSize: '0.75rem', color: 'var(--text-muted)', marginTop: '0.5rem' }}>
          {subtitle}
        </div>
      )}
    </div>
  );
};
