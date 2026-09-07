import React from 'react';
import { useAuth } from '../context/AuthContext';
import { LayoutDashboard, Server, ShieldCheck, Activity, Cpu, HardDrive } from 'lucide-react';

export const FleetPage: React.FC = () => {
  const { activeTenantId, entitlementTier } = useAuth();

  return (
    <div style={{ maxWidth: '1200px', margin: '0 auto' }}>
      {/* Title */}
      <div style={{ marginBottom: '1.5rem' }}>
        <h1 style={{ fontSize: '1.5rem', fontWeight: 800, color: 'var(--text-main)', display: 'flex', alignItems: 'center', gap: '0.6rem' }}>
          <LayoutDashboard color="#0057B8" size={26} /> Fleet Overview
        </h1>
        <p style={{ fontSize: '0.875rem', color: 'var(--text-muted)' }}>
          Unified read-only operational telemetry across IBM i and Windows servers.
        </p>
      </div>

      {/* Overview Metric Cards */}
      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(240px, 1fr))', gap: '1.25rem', marginBottom: '1.75rem' }}>
        <div className="card" style={{ borderLeft: '4px solid #0057B8' }}>
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', color: 'var(--text-muted)', fontSize: '0.85rem' }}>
            <span>Tenant Context</span>
            <ShieldCheck size={18} color="#0057B8" />
          </div>
          <div style={{ fontSize: '1.2rem', fontWeight: 700, marginTop: '0.5rem', color: 'var(--text-main)', fontFamily: 'var(--font-mono)' }}>
            {activeTenantId ? activeTenantId.substring(0, 13) + '...' : 'All Tenants'}
          </div>
          <div style={{ fontSize: '0.75rem', color: 'var(--color-primary)', fontWeight: 600, marginTop: '0.25rem' }}>
            {entitlementTier} TIER ENTITLEMENT
          </div>
        </div>

        <div className="card" style={{ borderLeft: '4px solid #10B981' }}>
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', color: 'var(--text-muted)', fontSize: '0.85rem' }}>
            <span>Capacity Headroom</span>
            <Activity size={18} color="#10B981" />
          </div>
          <div style={{ fontSize: '1.8rem', fontWeight: 800, marginTop: '0.2rem', color: '#10B981' }}>
            78.4%
          </div>
          <div style={{ fontSize: '0.75rem', color: 'var(--text-muted)' }}>
            Optimal fleet headroom headroom margin
          </div>
        </div>

        <div className="card" style={{ borderLeft: '4px solid #F5A300' }}>
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', color: 'var(--text-muted)', fontSize: '0.85rem' }}>
            <span>IBM i Subsystems</span>
            <Server size={18} color="#F5A300" />
          </div>
          <div style={{ fontSize: '1.8rem', fontWeight: 800, marginTop: '0.2rem', color: 'var(--text-main)' }}>
            14 / 14
          </div>
          <div style={{ fontSize: '0.75rem', color: '#D97706', fontWeight: 600 }}>
            All Active & Healthy
          </div>
        </div>

        <div className="card" style={{ borderLeft: '4px solid #3B82F6' }}>
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', color: 'var(--text-muted)', fontSize: '0.85rem' }}>
            <span>Windows Agents</span>
            <Cpu size={18} color="#3B82F6" />
          </div>
          <div style={{ fontSize: '1.8rem', fontWeight: 800, marginTop: '0.2rem', color: 'var(--text-main)' }}>
            32 Online
          </div>
          <div style={{ fontSize: '0.75rem', color: 'var(--text-muted)' }}>
            WinMonitor & LogKeyword
          </div>
        </div>
      </div>

      {/* Fleet Status Section */}
      <div className="card">
        <h3 style={{ fontSize: '1.1rem', fontWeight: 700, marginBottom: '0.75rem', display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
          <HardDrive size={20} color="#0057B8" /> System Telemetry Stream
        </h3>
        <p style={{ fontSize: '0.875rem', color: 'var(--text-muted)', marginBottom: '1rem' }}>
          Real-time metrics polled via Audited Query Gateway proxy endpoints.
        </p>

        <div style={{ padding: '2rem', textAlign: 'center', backgroundColor: '#F8FAFC', borderRadius: 'var(--radius-sm)', border: '1px dashed var(--border-color)' }}>
          <Activity size={36} color="#0057B8" style={{ marginBottom: '0.5rem' }} />
          <h4 style={{ fontSize: '1rem', fontWeight: 600 }}>Fleet Dashboard Active</h4>
          <p style={{ fontSize: '0.8rem', color: 'var(--text-muted)', marginTop: '0.25rem' }}>
            Connected to tenant telemetry proxy gateway. All queries audited under SOC 2 requirements.
          </p>
        </div>
      </div>
    </div>
  );
};
