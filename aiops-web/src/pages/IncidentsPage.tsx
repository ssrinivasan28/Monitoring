import React from 'react';
import { AlertTriangle, Filter, CheckCircle2, TrendingDown } from 'lucide-react';

export const IncidentsPage: React.FC = () => {
  return (
    <div style={{ maxWidth: '1200px', margin: '0 auto' }}>
      <div style={{ marginBottom: '1.5rem' }}>
        <h1 style={{ fontSize: '1.5rem', fontWeight: 800, color: 'var(--text-main)', display: 'flex', alignItems: 'center', gap: '0.6rem' }}>
          <AlertTriangle color="#0057B8" size={26} /> Incident Intelligence
        </h1>
        <p style={{ fontSize: '0.875rem', color: 'var(--text-muted)' }}>
          Correlated multi-system alerts and root cause tracking.
        </p>
      </div>

      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(280px, 1fr))', gap: '1.25rem', marginBottom: '1.5rem' }}>
        <div className="card">
          <div style={{ fontSize: '0.85rem', color: 'var(--text-muted)' }}>Raw Alert Reduction</div>
          <div style={{ fontSize: '1.8rem', fontWeight: 800, color: '#10B981', marginTop: '0.25rem', display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
            <TrendingDown size={28} /> 84.2% Noise Reduction
          </div>
          <div style={{ fontSize: '0.75rem', color: 'var(--text-muted)', marginTop: '0.25rem' }}>
            1,240 raw sensor alerts → 196 correlated incidents
          </div>
        </div>

        <div className="card">
          <div style={{ fontSize: '0.85rem', color: 'var(--text-muted)' }}>Active Incidents</div>
          <div style={{ fontSize: '1.8rem', fontWeight: 800, color: '#0F172A', marginTop: '0.25rem' }}>
            3 Active
          </div>
          <div style={{ fontSize: '0.75rem', color: '#D97706', fontWeight: 600, marginTop: '0.25rem' }}>
            1 High, 2 Medium Severity
          </div>
        </div>
      </div>

      <div className="card">
        <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '1rem' }}>
          <h3 style={{ fontSize: '1.1rem', fontWeight: 700 }}>Correlated Incident Log</h3>
          <button className="btn btn-secondary" style={{ padding: '0.4rem 0.75rem', fontSize: '0.8rem' }}>
            <Filter size={14} /> Filter Severity
          </button>
        </div>

        <div style={{ display: 'flex', flexDirection: 'column', gap: '0.75rem' }}>
          <div style={{ padding: '1rem', border: '1px solid var(--border-color)', borderRadius: 'var(--radius-sm)', backgroundColor: 'white', display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
            <div>
              <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', marginBottom: '0.25rem' }}>
                <span className="badge badge-warning">HIGH</span>
                <span style={{ fontWeight: 600, fontSize: '0.9rem' }}>IFS Job Queue Delay + WinFS Disk High IO</span>
              </div>
              <div style={{ fontSize: '0.8rem', color: 'var(--text-muted)' }}>
                Correlated across IBM i subsystem QBATCH and Windows Server SVR-PROD-02.
              </div>
            </div>
            <span style={{ fontSize: '0.75rem', color: 'var(--text-muted)', fontFamily: 'var(--font-mono)' }}>12 mins ago</span>
          </div>

          <div style={{ padding: '1rem', border: '1px solid var(--border-color)', borderRadius: 'var(--radius-sm)', backgroundColor: 'white', display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
            <div>
              <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', marginBottom: '0.25rem' }}>
                <span className="badge badge-primary">INFO</span>
                <span style={{ fontWeight: 600, fontSize: '0.9rem' }}>Log Keyword Match - Exception Threshold</span>
              </div>
              <div style={{ fontSize: '0.8rem', color: 'var(--text-muted)' }}>
                Matched keyword 'SSL_CERT_EXPIRING' on monitor endpoint.
              </div>
            </div>
            <span style={{ fontSize: '0.75rem', color: 'var(--text-muted)', fontFamily: 'var(--font-mono)' }}>45 mins ago</span>
          </div>
        </div>
      </div>
    </div>
  );
};
