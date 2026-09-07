import React from 'react';
import { useAuth } from '../../context/AuthContext';
import { useTenants } from '../../context/TenantContext';
import { Building2, ChevronDown, Lock } from 'lucide-react';

export const TenantSwitcher: React.FC = () => {
  const { user, activeTenantId, setActiveTenantId } = useAuth();
  const { tenants } = useTenants();

  if (!user) return null;

  // Customers are pinned to their own tenant
  if (!user.staff) {
    const currentTenant = tenants.find((t) => t.id === user.tenantId);
    return (
      <div 
        style={{
          display: 'flex',
          alignItems: 'center',
          gap: '0.5rem',
          padding: '0.4rem 0.75rem',
          backgroundColor: 'rgba(255, 255, 255, 0.1)',
          borderRadius: 'var(--radius-sm)',
          fontSize: '0.85rem',
          color: 'white',
        }}
        title="Pinned to assigned customer tenant"
      >
        <Building2 size={16} color="#F5A300" />
        <span style={{ fontWeight: 500 }}>{currentTenant?.name || user.tenantId?.substring(0, 8) || 'Tenant'}</span>
        <Lock size={14} style={{ opacity: 0.7, marginLeft: '0.25rem' }} />
      </div>
    );
  }

  // Staff users span tenants and can select any active tenant
  return (
    <div style={{ position: 'relative', display: 'inline-block' }}>
      <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
        <Building2 size={16} color="#F5A300" />
        <select
          value={activeTenantId || ''}
          onChange={(e) => setActiveTenantId(e.target.value)}
          style={{
            appearance: 'none',
            backgroundColor: 'rgba(255, 255, 255, 0.12)',
            color: 'white',
            border: '1px solid rgba(255, 255, 255, 0.2)',
            borderRadius: 'var(--radius-sm)',
            padding: '0.4rem 2rem 0.4rem 0.75rem',
            fontSize: '0.85rem',
            fontWeight: 500,
            cursor: 'pointer',
            outline: 'none',
          }}
        >
          <option value="" style={{ color: '#0F172A' }}>Select Tenant...</option>
          {tenants.map((t) => (
            <option key={t.id} value={t.id} style={{ color: '#0F172A' }}>
              {t.name} ({t.id.substring(0, 8)}) {t.tier === 'PRO' ? '⭐ PRO' : ''}
            </option>
          ))}
        </select>
        <ChevronDown size={14} style={{ position: 'absolute', right: '0.75rem', pointerEvents: 'none', color: 'white' }} />
      </div>
    </div>
  );
};
