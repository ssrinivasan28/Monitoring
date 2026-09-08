import React from 'react';
import { useAuth } from '../../context/AuthContext';
import { useTenants } from '../../context/TenantContext';
import { Building2, Lock } from 'lucide-react';

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
          backgroundColor: 'var(--brand-primary-light)',
          borderRadius: 'var(--radius-sm)',
          fontSize: 13,
          color: 'var(--brand-primary)',
        }}
        title="Pinned to assigned customer tenant"
      >
        <Building2 size={15} strokeWidth={1.75} />
        <span style={{ fontWeight: 600 }}>{currentTenant?.name || user.tenantId?.substring(0, 8) || 'Tenant'}</span>
        <Lock size={13} style={{ opacity: 0.7, marginLeft: '0.25rem' }} />
      </div>
    );
  }

  // Staff users span tenants and can select any active tenant
  return (
    <div style={{ position: 'relative', display: 'inline-block' }}>
      <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
        <Building2 size={15} strokeWidth={1.75} color="var(--brand-primary)" />
        <select
          value={activeTenantId || ''}
          onChange={(e) => setActiveTenantId(e.target.value)}
          style={{
            backgroundColor: 'var(--brand-primary-light)',
            color: 'var(--brand-primary)',
            border: '1px solid var(--border-color)',
            borderRadius: 'var(--radius-sm)',
            padding: '0.4rem 2rem 0.4rem 0.75rem',
            fontSize: 13,
            fontWeight: 600,
            cursor: 'pointer',
            outline: 'none',
          }}
        >
          <option value="">Select Tenant...</option>
          {tenants.map((t) => (
            <option key={t.id} value={t.id}>
              {t.name} ({t.id.substring(0, 8)}) {t.tier === 'PRO' ? '⭐ PRO' : ''}
            </option>
          ))}
        </select>
      </div>
    </div>
  );
};
