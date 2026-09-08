import React, { useEffect, useMemo, useState } from 'react';
import { AlertTriangle, CheckCircle2, Activity, Inbox } from 'lucide-react';
import { getIncidents, Incident, IncidentSeverity } from '../services/incidentService';

const severityBadgeClass: Record<IncidentSeverity, string> = {
  critical: 'badge-red',
  high: 'badge-warning',
  medium: 'badge-amber',
  low: 'badge-primary',
};

const formatRelativeTime = (iso: string): string => {
  const diffMs = Date.now() - new Date(iso).getTime();
  const minutes = Math.floor(diffMs / 60000);
  if (minutes < 1) return 'just now';
  if (minutes < 60) return `${minutes} min${minutes === 1 ? '' : 's'} ago`;
  const hours = Math.floor(minutes / 60);
  if (hours < 24) return `${hours} hour${hours === 1 ? '' : 's'} ago`;
  const days = Math.floor(hours / 24);
  return `${days} day${days === 1 ? '' : 's'} ago`;
};

export const IncidentsPage: React.FC = () => {
  const [incidents, setIncidents] = useState<Incident[]>([]);
  const [loading, setLoading] = useState<boolean>(true);
  const [error, setError] = useState<string | null>(null);

  const loadIncidents = async () => {
    setLoading(true);
    setError(null);
    try {
      const data = await getIncidents();
      setIncidents(data);
    } catch (err: any) {
      setError(err.message || 'Failed to load incidents');
      setIncidents([]);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadIncidents();
  }, []);

  const summary = useMemo(() => {
    const active = incidents.filter((i) => i.status !== 'resolved');
    const bySeverity: Record<IncidentSeverity, number> = { critical: 0, high: 0, medium: 0, low: 0 };
    active.forEach((i) => { bySeverity[i.severity] = (bySeverity[i.severity] || 0) + 1; });
    return { activeCount: active.length, bySeverity };
  }, [incidents]);

  const severityLabel = Object.entries(summary.bySeverity)
    .filter(([, count]) => count > 0)
    .map(([sev, count]) => `${count} ${sev[0].toUpperCase()}${sev.slice(1)}`)
    .join(', ') || 'No active incidents';

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
          <div style={{ fontSize: '0.85rem', color: 'var(--text-muted)' }}>Total Incidents</div>
          <div style={{ fontSize: '1.8rem', fontWeight: 800, color: '#0F172A', marginTop: '0.25rem' }}>
            {incidents.length}
          </div>
        </div>

        <div className="card">
          <div style={{ fontSize: '0.85rem', color: 'var(--text-muted)' }}>Active Incidents</div>
          <div style={{ fontSize: '1.8rem', fontWeight: 800, color: '#0F172A', marginTop: '0.25rem' }}>
            {summary.activeCount} Active
          </div>
          <div style={{ fontSize: '0.75rem', color: '#D97706', fontWeight: 600, marginTop: '0.25rem' }}>
            {severityLabel}
          </div>
        </div>
      </div>

      <div className="card">
        <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '1rem' }}>
          <h3 style={{ fontSize: '1.1rem', fontWeight: 700 }}>Correlated Incident Log</h3>
        </div>

        {loading ? (
          <div style={{ textAlign: 'center', padding: '2.5rem' }}>
            <Activity size={32} color="#0057B8" className="spin" style={{ margin: '0 auto 0.75rem' }} />
            <p style={{ color: 'var(--text-muted)', fontSize: '0.875rem' }}>Loading incidents...</p>
          </div>
        ) : error ? (
          <div style={{ textAlign: 'center', padding: '2.5rem' }}>
            <AlertTriangle size={32} color="#EF4444" style={{ margin: '0 auto 0.75rem' }} />
            <p style={{ color: '#EF4444', fontSize: '0.875rem', fontWeight: 600 }}>{error}</p>
          </div>
        ) : incidents.length === 0 ? (
          <div style={{ textAlign: 'center', padding: '2.5rem' }}>
            <Inbox size={32} color="var(--text-muted)" style={{ margin: '0 auto 0.75rem' }} />
            <p style={{ color: 'var(--text-muted)', fontSize: '0.875rem' }}>No incidents have been recorded for this tenant.</p>
          </div>
        ) : (
          <div style={{ display: 'flex', flexDirection: 'column', gap: '0.75rem' }}>
            {incidents.map((incident) => (
              <div
                key={incident.id}
                style={{ padding: '1rem', border: '1px solid var(--border-color)', borderRadius: 'var(--radius-sm)', backgroundColor: 'white', display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}
              >
                <div>
                  <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', marginBottom: '0.25rem' }}>
                    <span className={`badge ${severityBadgeClass[incident.severity] || 'badge-primary'}`}>
                      {incident.severity.toUpperCase()}
                    </span>
                    <span style={{ fontWeight: 600, fontSize: '0.9rem' }}>{incident.title}</span>
                    {incident.status === 'resolved' && (
                      <span style={{ display: 'inline-flex', alignItems: 'center', gap: '0.25rem', fontSize: '0.75rem', color: '#10B981', fontWeight: 600 }}>
                        <CheckCircle2 size={13} /> Resolved
                      </span>
                    )}
                  </div>
                  <div style={{ fontSize: '0.8rem', color: 'var(--text-muted)' }}>
                    Status: {incident.status}
                  </div>
                </div>
                <span style={{ fontSize: '0.75rem', color: 'var(--text-muted)', fontFamily: 'var(--font-mono)' }}>
                  {formatRelativeTime(incident.openedAt)}
                </span>
              </div>
            ))}
          </div>
        )}
      </div>
    </div>
  );
};
