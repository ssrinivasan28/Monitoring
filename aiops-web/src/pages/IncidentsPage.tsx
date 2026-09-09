import React, { useEffect, useMemo, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { AlertTriangle, Activity, Inbox, RefreshCw, Filter } from 'lucide-react';
import { useAuth } from '../context/AuthContext';
import { useTenants } from '../context/TenantContext';
import {
  getIncidents,
  IncidentSummary,
  IncidentSeverity,
  IncidentStatus,
  INCIDENT_LIST_MAX_SIZE,
} from '../services/incidentService';
import { SeverityBadge, severityStripeStyle } from '../components/Incidents/SeverityBadge';

const STATUS_OPTIONS: IncidentStatus[] = ['open', 'ack', 'assigned', 'resolved'];
const SEVERITY_OPTIONS: IncidentSeverity[] = ['critical', 'high', 'medium', 'low'];

const PLATFORM_LABELS: Record<string, string> = { ibmi: 'IBM i', windows: 'Windows' };
const platformLabel = (p: string) => PLATFORM_LABELS[p] || p;

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
  const navigate = useNavigate();
  const { user, activeTenantId, setActiveTenantId } = useAuth();
  const { tenants } = useTenants();
  const isStaff = !!user?.staff || ['SUPER_ADMIN', 'STAFF_ADMIN', 'STAFF'].includes(user?.roleKey || '');

  const [incidents, setIncidents] = useState<IncidentSummary[]>([]);
  const [loading, setLoading] = useState<boolean>(true);
  const [error, setError] = useState<string | null>(null);

  const [statusFilter, setStatusFilter] = useState<string>('ALL');
  const [platformFilter, setPlatformFilter] = useState<string>('ALL');
  const [severityFilter, setSeverityFilter] = useState<string>('ALL');

  const loadIncidents = async () => {
    setLoading(true);
    setError(null);
    try {
      const data = await getIncidents({
        status: statusFilter === 'ALL' ? undefined : (statusFilter as IncidentStatus),
        platform: platformFilter === 'ALL' ? undefined : platformFilter,
      });
      setIncidents(data);
    } catch (err: any) {
      setError(err.message || 'Failed to load incidents');
      setIncidents([]);
    } finally {
      setLoading(false);
    }
  };

  // Re-fetch whenever the tenant scope or a server-side filter (status/platform) changes.
  useEffect(() => {
    loadIncidents();
  }, [activeTenantId, statusFilter, platformFilter]);

  // The API already returns incidents ranked severity-desc then recency-desc; filtering preserves that order.
  const filtered = useMemo(() => {
    return incidents.filter((i) => severityFilter === 'ALL' || i.severity === severityFilter);
  }, [incidents, severityFilter]);

  const bySeverity = useMemo(() => {
    const counts: Record<IncidentSeverity, number> = { critical: 0, high: 0, medium: 0, low: 0 };
    incidents.forEach((i) => { counts[i.severity] = (counts[i.severity] || 0) + 1; });
    return counts;
  }, [incidents]);

  const tenantName = (tenantId: string) => tenants.find((t) => t.id === tenantId)?.name || tenantId.substring(0, 8);

  return (
    <div style={{ maxWidth: '1300px', margin: '0 auto' }}>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '1.5rem', flexWrap: 'wrap', gap: '1rem' }}>
        <div>
          <h1 style={{ fontSize: '1.5rem', fontWeight: 800, color: 'var(--text-main)', display: 'flex', alignItems: 'center', gap: '0.6rem' }}>
            <AlertTriangle color="#0057B8" size={26} /> Incident Intelligence
          </h1>
          <p style={{ fontSize: '0.875rem', color: 'var(--text-muted)' }}>
            Correlated multi-system alerts, ranked by severity then recency.
          </p>
        </div>
        <button onClick={loadIncidents} className="btn btn-secondary" disabled={loading}>
          <RefreshCw size={16} className={loading ? 'spin' : ''} /> Refresh
        </button>
      </div>

      {/* Severity summary strip */}
      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(180px, 1fr))', gap: '1.25rem', marginBottom: '1.5rem' }}>
        <div className="card">
          <div style={{ fontSize: '0.85rem', color: 'var(--text-muted)' }}>Total Incidents</div>
          <div style={{ fontSize: '1.8rem', fontWeight: 800, color: 'var(--text-main)', marginTop: '0.25rem' }}>{incidents.length}</div>
        </div>
        {SEVERITY_OPTIONS.map((sev) => (
          <div key={sev} className="card" style={severityStripeStyle(sev)}>
            <div style={{ fontSize: '0.85rem', color: 'var(--text-muted)', textTransform: 'capitalize' }}>{sev}</div>
            <div style={{ fontSize: '1.8rem', fontWeight: 800, color: 'var(--text-main)', marginTop: '0.25rem' }}>{bySeverity[sev]}</div>
          </div>
        ))}
      </div>

      {/* Filter toolbar */}
      <div className="card" style={{ marginBottom: '1.5rem' }}>
        <div style={{ display: 'flex', gap: '1rem', flexWrap: 'wrap', alignItems: 'center' }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', color: 'var(--text-muted)', fontSize: '0.85rem', fontWeight: 600 }}>
            <Filter size={16} /> Filters
          </div>

          {isStaff && (
            <select
              aria-label="Filter by tenant"
              value={activeTenantId || ''}
              onChange={(e) => e.target.value && setActiveTenantId(e.target.value)}
              className="form-control"
              style={{ minWidth: '180px', height: '34px' }}
            >
              {tenants.map((t) => (
                <option key={t.id} value={t.id}>{t.name}</option>
              ))}
            </select>
          )}

          <select
            aria-label="Filter by status"
            value={statusFilter}
            onChange={(e) => setStatusFilter(e.target.value)}
            className="form-control"
            style={{ minWidth: '140px', height: '34px' }}
          >
            <option value="ALL">All Statuses</option>
            {STATUS_OPTIONS.map((s) => (
              <option key={s} value={s}>{s.charAt(0).toUpperCase() + s.slice(1)}</option>
            ))}
          </select>

          <select
            aria-label="Filter by platform"
            value={platformFilter}
            onChange={(e) => setPlatformFilter(e.target.value)}
            className="form-control"
            style={{ minWidth: '140px', height: '34px' }}
          >
            <option value="ALL">All Platforms</option>
            <option value="ibmi">IBM i</option>
            <option value="windows">Windows</option>
          </select>

          <select
            aria-label="Filter by severity"
            value={severityFilter}
            onChange={(e) => setSeverityFilter(e.target.value)}
            className="form-control"
            style={{ minWidth: '140px', height: '34px' }}
          >
            <option value="ALL">All Severities</option>
            {SEVERITY_OPTIONS.map((s) => (
              <option key={s} value={s}>{s.charAt(0).toUpperCase() + s.slice(1)}</option>
            ))}
          </select>
        </div>
      </div>

      {incidents.length === INCIDENT_LIST_MAX_SIZE && (
        <div style={{ fontSize: '0.8rem', color: 'var(--text-muted)', marginBottom: '0.75rem' }}>
          Showing the first {INCIDENT_LIST_MAX_SIZE} matching incidents — narrow the filters above to see more specific results.
        </div>
      )}

      <div className="card" style={{ padding: 0, overflow: 'hidden' }}>
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
        ) : filtered.length === 0 ? (
          <div style={{ textAlign: 'center', padding: '2.5rem' }}>
            <Inbox size={32} color="var(--text-muted)" style={{ margin: '0 auto 0.75rem' }} />
            <p style={{ color: 'var(--text-muted)', fontSize: '0.875rem' }}>No incidents match the current filters.</p>
          </div>
        ) : (
          <div style={{ overflowX: 'auto' }}>
            <table style={{ width: '100%', borderCollapse: 'collapse', textAlign: 'left', fontSize: '0.85rem' }}>
              <thead>
                <tr style={{ backgroundColor: '#F8FAFC', borderBottom: '1px solid var(--border-color)', color: 'var(--text-muted)' }}>
                  <th style={{ padding: '0.85rem 1.25rem' }}>Severity</th>
                  <th style={{ padding: '0.85rem 1.25rem' }}>Title</th>
                  {isStaff && <th style={{ padding: '0.85rem 1.25rem' }}>Tenant</th>}
                  <th style={{ padding: '0.85rem 1.25rem' }}>Platform(s)</th>
                  <th style={{ padding: '0.85rem 1.25rem' }}>Status</th>
                  <th style={{ padding: '0.85rem 1.25rem' }}>Age</th>
                  <th style={{ padding: '0.85rem 1.25rem' }}>Signals</th>
                </tr>
              </thead>
              <tbody>
                {filtered.map((incident) => (
                  <tr
                    key={incident.id}
                    onClick={() => navigate(`/incidents/${incident.id}`)}
                    style={{ borderBottom: '1px solid var(--border-subtle)', cursor: 'pointer', ...severityStripeStyle(incident.severity) }}
                    className="incident-row"
                  >
                    <td style={{ padding: '0.85rem 1.25rem' }}>
                      <SeverityBadge severity={incident.severity} />
                    </td>
                    <td style={{ padding: '0.85rem 1.25rem', fontWeight: 600 }}>
                      <a
                        href={`/incidents/${incident.id}`}
                        onClick={(e) => { e.preventDefault(); navigate(`/incidents/${incident.id}`); }}
                        style={{ color: 'var(--text-main)' }}
                      >
                        {incident.title}
                      </a>
                      {incident.hasRootCause && (
                        <span className="badge badge-primary" style={{ marginLeft: '0.5rem' }}>AI narrative</span>
                      )}
                    </td>
                    {isStaff && (
                      <td style={{ padding: '0.85rem 1.25rem', color: 'var(--text-muted)' }}>
                        {tenantName(incident.tenantId)}
                      </td>
                    )}
                    <td style={{ padding: '0.85rem 1.25rem' }}>
                      {incident.platforms.length === 0 ? (
                        <span style={{ color: 'var(--text-muted)' }}>—</span>
                      ) : (
                        incident.platforms.map((p) => (
                          <span key={p} className="badge badge-gray" style={{ marginRight: '0.35rem' }}>{platformLabel(p)}</span>
                        ))
                      )}
                    </td>
                    <td style={{ padding: '0.85rem 1.25rem', textTransform: 'capitalize' }}>{incident.status}</td>
                    <td style={{ padding: '0.85rem 1.25rem', fontFamily: 'var(--font-mono)', color: 'var(--text-muted)' }}>
                      {formatRelativeTime(incident.openedAt)}
                    </td>
                    <td style={{ padding: '0.85rem 1.25rem' }}>{incident.signalCount}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>
    </div>
  );
};
