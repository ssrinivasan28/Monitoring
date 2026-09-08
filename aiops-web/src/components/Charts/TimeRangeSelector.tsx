import React from 'react';
import { Clock, RefreshCw, Building2 } from 'lucide-react';
import { useAuth } from '../../context/AuthContext';
import { useTenants } from '../../context/TenantContext';

export interface TimeRangeOption {
  label: string;
  seconds: number;
  step: string;
}

export const TIME_RANGE_OPTIONS: TimeRangeOption[] = [
  { label: 'Last 1 Hour', seconds: 3600, step: '14s' },
  { label: 'Last 6 Hours', seconds: 21600, step: '30s' },
  { label: 'Last 24 Hours', seconds: 86400, step: '2m' },
  { label: 'Last 7 Days', seconds: 604800, step: '15m' },
  { label: 'Last 13 Months', seconds: 34020000, step: '1h' },
];

interface TimeRangeSelectorProps {
  selectedRangeSeconds: number;
  onRangeChange: (option: TimeRangeOption) => void;
  onManualRefresh: () => void;
  loading?: boolean;
}

export const TimeRangeSelector: React.FC<TimeRangeSelectorProps> = ({
  selectedRangeSeconds,
  onRangeChange,
  onManualRefresh,
  loading = false,
}) => {
  const { activeTenantId, setActiveTenantId } = useAuth();
  const { tenants } = useTenants();

  const currentOption = TIME_RANGE_OPTIONS.find((o) => o.seconds === selectedRangeSeconds) || TIME_RANGE_OPTIONS[1];
  const activeTenant = tenants.find((t) => t.id === activeTenantId);

  return (
    <div
      className="card"
      style={{
        padding: '0.75rem 1.25rem',
        marginBottom: '1.25rem',
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'space-between',
        flexWrap: 'wrap',
        gap: '1rem',
        backgroundColor: '#1E293B',
        color: 'white',
      }}
    >
      {/* Tenant Context Selector */}
      <div style={{ display: 'flex', alignItems: 'center', gap: '0.6rem' }}>
        <Building2 size={18} color="#60A5FA" />
        <span style={{ fontSize: '0.85rem', fontWeight: 600, color: '#94A3B8' }}>Tenant Context:</span>
        {tenants.length > 1 ? (
          <select
            value={activeTenantId || ''}
            onChange={(e) => setActiveTenantId(e.target.value)}
            style={{
              backgroundColor: '#0F172A',
              color: 'white',
              border: '1px solid #334155',
              borderRadius: 'var(--radius-sm)',
              padding: '0.35rem 0.65rem',
              fontSize: '0.85rem',
              fontWeight: 600,
              cursor: 'pointer',
            }}
          >
            {tenants.map((t) => (
              <option key={t.id} value={t.id}>
                {t.name} ({t.id.substring(0, 8)}) [{t.tier}]
              </option>
            ))}
          </select>
        ) : (
          <span style={{ fontSize: '0.875rem', fontWeight: 700, color: 'white' }}>
            {activeTenant ? `${activeTenant.name} (${activeTenant.id.substring(0, 8)})` : 'Active Tenant'}
          </span>
        )}
      </div>

      {/* Time Range & Refresh Control */}
      <div style={{ display: 'flex', alignItems: 'center', gap: '0.75rem' }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: '0.4rem' }}>
          <Clock size={16} color="#94A3B8" />
          <span style={{ fontSize: '0.8rem', color: '#94A3B8' }}>Time Range:</span>
          <select
            value={currentOption.seconds}
            onChange={(e) => {
              const sec = Number(e.target.value);
              const found = TIME_RANGE_OPTIONS.find((o) => o.seconds === sec);
              if (found) onRangeChange(found);
            }}
            style={{
              backgroundColor: '#0F172A',
              color: 'white',
              border: '1px solid #334155',
              borderRadius: 'var(--radius-sm)',
              padding: '0.35rem 0.65rem',
              fontSize: '0.85rem',
              fontWeight: 600,
              cursor: 'pointer',
            }}
          >
            {TIME_RANGE_OPTIONS.map((opt) => (
              <option key={opt.seconds} value={opt.seconds}>
                {opt.label}
              </option>
            ))}
          </select>
        </div>

        <button
          onClick={onManualRefresh}
          className="btn"
          style={{
            backgroundColor: '#0057B8',
            color: 'white',
            border: 'none',
            padding: '0.35rem 0.75rem',
            fontSize: '0.8rem',
            display: 'flex',
            alignItems: 'center',
            gap: '0.4rem',
          }}
          disabled={loading}
        >
          <RefreshCw size={14} className={loading ? 'spin' : ''} />
          {loading ? 'Refreshing...' : 'Refresh'}
        </button>
      </div>
    </div>
  );
};
