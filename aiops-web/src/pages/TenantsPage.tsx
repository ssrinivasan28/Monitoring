import React, { useState } from 'react';
import { useTenants } from '../context/TenantContext';
import { useAuth } from '../context/AuthContext';
import { authService } from '../services/authService';
import { Users, Shield, Sparkles, CheckCircle2, AlertCircle } from 'lucide-react';
import { EntitlementTier } from '../types';

export const TenantsPage: React.FC = () => {
  const { tenants, loading: tenantsLoading, error: tenantsError, refreshTenants } = useTenants();
  const { setEntitlementTier: setGlobalTier, activeTenantId } = useAuth();

  const [message, setMessage] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [updatingId, setUpdatingId] = useState<string | null>(null);

  const handleUpdateTier = async (tenantId: string, newTier: EntitlementTier) => {
    setUpdatingId(tenantId);
    setMessage(null);
    setError(null);
    try {
      await authService.getEntitlement(tenantId); // verify endpoint access
      // Call PUT /api/v1/admin/tenants/{tenantId}/entitlement
      const response = await fetch(`/api/v1/admin/tenants/${tenantId}/entitlement`, {
        method: 'PUT',
        headers: {
          'Content-Type': 'application/json',
          'Authorization': `Bearer ${localStorage.getItem('sentinel_access_token')}`,
        },
        body: JSON.stringify({ tier: newTier }),
      });
      if (!response.ok) {
        throw new Error(`Failed to update tier: HTTP ${response.status}`);
      }
      setMessage(`Tenant entitlement updated to ${newTier}.`);
      if (tenantId === activeTenantId) {
        setGlobalTier(newTier);
      }
      refreshTenants();
    } catch (err: any) {
      setError(err.message || 'Failed to update tenant entitlement tier.');
    } finally {
      setUpdatingId(null);
    }
  };

  return (
    <div style={{ maxWidth: '1200px', margin: '0 auto' }}>
      <div style={{ marginBottom: '1.5rem' }}>
        <h1 style={{ fontSize: '1.5rem', fontWeight: 800, color: 'var(--text-main)', display: 'flex', alignItems: 'center', gap: '0.6rem' }}>
          <Users color="#0057B8" size={26} /> Admin › Tenants & Tier Entitlements
        </h1>
        <p style={{ fontSize: '0.875rem', color: 'var(--text-muted)' }}>
          Manage customer tenant isolation boundaries and toggle Basic vs. Pro tier capabilities.
        </p>
      </div>

      {message && (
        <div
          style={{
            backgroundColor: 'var(--color-success-bg)',
            color: 'var(--color-success)',
            border: '1px solid #6EE7B7',
            borderRadius: 'var(--radius-sm)',
            padding: '0.75rem 1rem',
            marginBottom: '1.25rem',
            display: 'flex',
            alignItems: 'center',
            gap: '0.5rem',
            fontSize: '0.875rem',
          }}
        >
          <CheckCircle2 size={18} />
          <span>{message}</span>
        </div>
      )}

      {(error || tenantsError) && (
        <div
          style={{
            backgroundColor: 'var(--color-danger-bg)',
            color: 'var(--color-danger)',
            border: '1px solid #FCA5A5',
            borderRadius: 'var(--radius-sm)',
            padding: '0.75rem 1rem',
            marginBottom: '1.25rem',
            display: 'flex',
            alignItems: 'center',
            gap: '0.5rem',
            fontSize: '0.875rem',
          }}
        >
          <AlertCircle size={18} />
          <span>{error || tenantsError}</span>
        </div>
      )}

      <div className="card" style={{ padding: 0, overflow: 'hidden' }}>
        <table style={{ width: '100%', borderCollapse: 'collapse', textAlign: 'left', fontSize: '0.875rem' }}>
          <thead>
            <tr style={{ backgroundColor: '#F8FAFC', borderBottom: '1px solid var(--border-color)', color: 'var(--text-muted)' }}>
              <th style={{ padding: '0.85rem 1.25rem' }}>Tenant Name</th>
              <th style={{ padding: '0.85rem 1.25rem' }}>Tenant ID</th>
              <th style={{ padding: '0.85rem 1.25rem' }}>Entitlement Tier</th>
              <th style={{ padding: '0.85rem 1.25rem', textAlign: 'right' }}>Actions</th>
            </tr>
          </thead>
          <tbody>
            {tenantsLoading ? (
              <tr>
                <td colSpan={4} style={{ padding: '2rem', textAlign: 'center', color: 'var(--text-muted)' }}>
                  Loading tenants...
                </td>
              </tr>
            ) : tenants.length === 0 ? (
              <tr>
                <td colSpan={4} style={{ padding: '2rem', textAlign: 'center', color: 'var(--text-muted)' }}>
                  No tenants found.
                </td>
              </tr>
            ) : tenants.map((t) => (
              <tr key={t.id} style={{ borderBottom: '1px solid var(--border-subtle)' }}>
                <td style={{ padding: '1rem 1.25rem', fontWeight: 600 }}>{t.name}</td>
                <td style={{ padding: '1rem 1.25rem', fontFamily: 'var(--font-mono)', fontSize: '0.8rem', color: '#475569' }}>
                  {t.id}
                </td>
                <td style={{ padding: '1rem 1.25rem' }}>
                  <span className={`badge ${t.tier === 'PRO' ? 'badge-accent' : 'badge-gray'}`}>
                    {t.tier === 'PRO' && <Sparkles size={12} />}
                    {t.tier} TIER
                  </span>
                </td>
                <td style={{ padding: '1rem 1.25rem', textAlign: 'right' }}>
                  <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '0.5rem' }}>
                    <button
                      className="btn btn-secondary"
                      style={{ padding: '0.35rem 0.75rem', fontSize: '0.8rem' }}
                      disabled={updatingId === t.id}
                      onClick={() => handleUpdateTier(t.id, t.tier === 'PRO' ? 'BASIC' : 'PRO')}
                    >
                      Set to {t.tier === 'PRO' ? 'BASIC' : 'PRO'} Tier
                    </button>
                  </div>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </div>
  );
};
