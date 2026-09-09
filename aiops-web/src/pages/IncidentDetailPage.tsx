import React, { useEffect, useState, useCallback } from 'react';
import { useParams, Link } from 'react-router-dom';
import {
  ArrowLeft, AlertTriangle, Activity, Sparkles, BookOpen, Copy,
  Clock, ListChecks, Send, Ticket, CheckCircle2, XCircle, ShieldOff,
} from 'lucide-react';
import { useAuth } from '../context/AuthContext';
import { useTenants } from '../context/TenantContext';
import {
  getIncidentDetail,
  pushIncidentToTeams,
  createIncidentTicket,
  IncidentDetail,
} from '../services/incidentService';
import { SeverityBadge, severityStripeStyle } from '../components/Incidents/SeverityBadge';

const PLATFORM_LABELS: Record<string, string> = { ibmi: 'IBM i', windows: 'Windows' };
const platformLabel = (p: string) => PLATFORM_LABELS[p] || p;

const formatDateTime = (iso: string): string => new Date(iso).toLocaleString();

const parseSignalDetail = (detailJson: string | null): Array<[string, string]> => {
  if (!detailJson) return [];
  try {
    const obj = JSON.parse(detailJson);
    return Object.entries(obj).map(([k, v]) => [k, String(v)]);
  } catch {
    return [['detail', detailJson]];
  }
};

type ToastState = { type: 'success' | 'error'; message: string } | null;

export const IncidentDetailPage: React.FC = () => {
  const { id } = useParams<{ id: string }>();
  const { user } = useAuth();
  const { tenants } = useTenants();

  const isStaff = !!user?.staff || ['SUPER_ADMIN', 'STAFF_ADMIN', 'STAFF'].includes(user?.roleKey || '');
  // Push/ticket actions are write-side effects on external systems, gated to the same roles that
  // can act on an incident today (see IncidentController#acknowledgeIncident) — not by entitlement
  // tier, since only the AI Assistant is Pro-gated per the product decisions.
  const canPushActions = isStaff || user?.roleKey === 'TENANT_ADMIN';

  const [incident, setIncident] = useState<IncidentDetail | null>(null);
  const [loading, setLoading] = useState<boolean>(true);
  const [notFound, setNotFound] = useState<boolean>(false);
  const [error, setError] = useState<string | null>(null);

  const [pushingTeams, setPushingTeams] = useState(false);
  const [creatingTicket, setCreatingTicket] = useState(false);
  const [toast, setToast] = useState<ToastState>(null);

  const load = useCallback(async () => {
    if (!id) return;
    setLoading(true);
    setError(null);
    setNotFound(false);
    try {
      const data = await getIncidentDetail(id);
      setIncident(data);
    } catch (err: any) {
      if (String(err.message || '').includes('404')) {
        setNotFound(true);
      } else {
        setError(err.message || 'Failed to load incident');
      }
      setIncident(null);
    } finally {
      setLoading(false);
    }
  }, [id]);

  useEffect(() => { load(); }, [load]);

  useEffect(() => {
    if (!toast) return;
    const t = setTimeout(() => setToast(null), 4000);
    return () => clearTimeout(t);
  }, [toast]);

  const handlePushToTeams = async () => {
    if (!id) return;
    setPushingTeams(true);
    try {
      await pushIncidentToTeams(id);
      setToast({ type: 'success', message: 'Pushed to the tenant\'s Teams channel.' });
    } catch (err: any) {
      setToast({ type: 'error', message: err.message || 'Failed to push to Teams.' });
    } finally {
      setPushingTeams(false);
    }
  };

  const handleCreateTicket = async () => {
    if (!id) return;
    setCreatingTicket(true);
    try {
      await createIncidentTicket(id);
      setToast({ type: 'success', message: 'Ticket created/enriched in the ITSM system.' });
    } catch (err: any) {
      setToast({ type: 'error', message: err.message || 'Failed to create/enrich the ticket.' });
    } finally {
      setCreatingTicket(false);
    }
  };

  if (loading) {
    return (
      <div style={{ maxWidth: '1100px', margin: '0 auto', textAlign: 'center', padding: '4rem' }}>
        <Activity size={32} color="#0057B8" className="spin" style={{ margin: '0 auto 0.75rem' }} />
        <p style={{ color: 'var(--text-muted)', fontSize: '0.875rem' }}>Loading incident...</p>
      </div>
    );
  }

  if (notFound) {
    return (
      <div style={{ maxWidth: '1100px', margin: '0 auto' }}>
        <BackLink />
        <div className="card" style={{ textAlign: 'center', padding: '3rem' }}>
          <ShieldOff size={32} color="var(--text-muted)" style={{ margin: '0 auto 0.75rem' }} />
          <h3>Incident Not Found</h3>
          <p style={{ color: 'var(--text-muted)', fontSize: '0.875rem' }}>
            It doesn't exist, or it doesn't belong to your current tenant.
          </p>
        </div>
      </div>
    );
  }

  if (error || !incident) {
    return (
      <div style={{ maxWidth: '1100px', margin: '0 auto' }}>
        <BackLink />
        <div className="card" style={{ textAlign: 'center', padding: '3rem' }}>
          <AlertTriangle size={32} color="#EF4444" style={{ margin: '0 auto 0.75rem' }} />
          <p style={{ color: '#EF4444', fontSize: '0.875rem', fontWeight: 600 }}>{error || 'Failed to load incident'}</p>
        </div>
      </div>
    );
  }

  const hasNarrative = !!incident.rootCause && !incident.rootCause.insufficient;
  const tenantName = tenants.find((t) => t.id === incident.tenantId)?.name;

  return (
    <div style={{ maxWidth: '1100px', margin: '0 auto', position: 'relative' }}>
      <BackLink />

      {/* Header */}
      <div className="card" style={{ marginBottom: '1.5rem', ...severityStripeStyle(incident.severity) }}>
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', flexWrap: 'wrap', gap: '1rem' }}>
          <div>
            <div style={{ display: 'flex', alignItems: 'center', gap: '0.6rem', marginBottom: '0.4rem' }}>
              <SeverityBadge severity={incident.severity} />
              <span className="badge badge-gray" style={{ textTransform: 'capitalize' }}>{incident.status}</span>
              {isStaff && tenantName && <span className="badge badge-primary">{tenantName}</span>}
            </div>
            <h1 style={{ fontSize: '1.35rem', fontWeight: 800, color: 'var(--text-main)' }}>{incident.title}</h1>
            <p style={{ fontSize: '0.8rem', color: 'var(--text-muted)', marginTop: '0.35rem' }}>
              Opened {formatDateTime(incident.openedAt)}
              {incident.resolvedAt && ` · Resolved ${formatDateTime(incident.resolvedAt)}`}
            </p>
          </div>

          {canPushActions && (
            <div style={{ display: 'flex', gap: '0.6rem', flexWrap: 'wrap' }}>
              <button className="btn btn-secondary" onClick={handlePushToTeams} disabled={pushingTeams}>
                <Send size={15} className={pushingTeams ? 'spin' : ''} /> Push to Teams
              </button>
              <button className="btn btn-secondary" onClick={handleCreateTicket} disabled={creatingTicket}>
                <Ticket size={15} className={creatingTicket ? 'spin' : ''} /> Create/Enrich Ticket
              </button>
            </div>
          )}
        </div>
      </div>

      {!hasNarrative && (
        <div className="card" style={{ marginBottom: '1.5rem', borderLeft: '4px solid #94A3B8', backgroundColor: '#F8FAFC' }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', color: 'var(--text-muted)', fontSize: '0.85rem' }}>
            <Sparkles size={16} /> AI root-cause narrative unavailable for this incident — showing verified signals and timeline below.
          </div>
        </div>
      )}

      <div style={{ display: 'grid', gridTemplateColumns: 'minmax(0, 2fr) minmax(0, 1fr)', gap: '1.5rem' }}>
        <div style={{ display: 'flex', flexDirection: 'column', gap: '1.5rem', minWidth: 0 }}>
          {/* Signals */}
          <section className="card">
            <h3 style={{ fontSize: '1rem', fontWeight: 700, marginBottom: '1rem', display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
              <Activity size={17} color="#0057B8" /> Correlated Signals ({incident.signals.length})
            </h3>
            {incident.signals.length === 0 ? (
              <p style={{ color: 'var(--text-muted)', fontSize: '0.85rem' }}>No signals recorded.</p>
            ) : (
              <div style={{ display: 'flex', flexDirection: 'column', gap: '0.75rem' }}>
                {incident.signals.map((s) => (
                  <div key={s.id} style={{ padding: '0.75rem 1rem', border: '1px solid var(--border-color)', borderRadius: 'var(--radius-sm)' }}>
                    <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: '0.35rem' }}>
                      <span className="badge badge-gray">{platformLabel(s.platform)}</span>
                    </div>
                    <div style={{ display: 'flex', flexWrap: 'wrap', gap: '0.5rem 1.5rem', fontSize: '0.8rem', color: 'var(--text-muted)' }}>
                      {parseSignalDetail(s.detailJson).map(([k, v]) => (
                        <span key={k}><strong style={{ color: 'var(--text-main)' }}>{k}:</strong> {v}</span>
                      ))}
                    </div>
                  </div>
                ))}
              </div>
            )}
          </section>

          {/* AI root cause + evidence */}
          {hasNarrative && incident.rootCause && (
            <section className="card">
              <h3 style={{ fontSize: '1rem', fontWeight: 700, marginBottom: '0.75rem', display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
                <Sparkles size={17} color="#F5A300" /> AI Root Cause
                {incident.rootCause.confidence != null && (
                  <span className="badge badge-accent">{Math.round(incident.rootCause.confidence * 100)}% confidence</span>
                )}
              </h3>
              <p style={{ fontSize: '0.9rem', color: 'var(--text-main)', marginBottom: '1rem', lineHeight: 1.6 }}>
                {incident.rootCause.root_cause_hypothesis}
              </p>

              {incident.rootCause.evidence.length > 0 && (
                <>
                  <div style={{ fontSize: '0.75rem', fontWeight: 700, color: 'var(--text-muted)', textTransform: 'uppercase', letterSpacing: '0.06em', marginBottom: '0.5rem' }}>
                    Evidence & Queries
                  </div>
                  <div style={{ display: 'flex', flexDirection: 'column', gap: '0.5rem' }}>
                    {incident.rootCause.evidence.map((e, idx) => (
                      <div key={idx} style={{ padding: '0.6rem 0.85rem', backgroundColor: 'var(--bg-main)', borderRadius: 'var(--radius-sm)', fontSize: '0.8rem' }}>
                        <div style={{ fontWeight: 600, marginBottom: '0.2rem' }}>{e.source}</div>
                        {e.query && <div style={{ fontFamily: 'var(--font-mono)', color: '#0057B8', marginBottom: '0.2rem' }}>{e.query}</div>}
                        {e.snippet && <div style={{ color: 'var(--text-muted)' }}>{e.snippet}</div>}
                      </div>
                    ))}
                  </div>
                </>
              )}
            </section>
          )}

          {/* Suggested checks */}
          {hasNarrative && incident.rootCause && incident.rootCause.suggested_checks.length > 0 && (
            <section className="card">
              <h3 style={{ fontSize: '1rem', fontWeight: 700, marginBottom: '0.75rem', display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
                <ListChecks size={17} color="#0057B8" /> Suggested Checks
              </h3>
              <ul style={{ paddingLeft: '1.25rem', display: 'flex', flexDirection: 'column', gap: '0.4rem' }}>
                {incident.rootCause.suggested_checks.map((c, idx) => (
                  <li key={idx} style={{ fontSize: '0.85rem', color: 'var(--text-main)' }}>{c}</li>
                ))}
              </ul>
            </section>
          )}

          {/* Runbook references */}
          {incident.runbookReferences.length > 0 && (
            <section className="card">
              <h3 style={{ fontSize: '1rem', fontWeight: 700, marginBottom: '0.75rem', display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
                <BookOpen size={17} color="#0057B8" /> Matched Runbook
              </h3>
              <div style={{ display: 'flex', flexDirection: 'column', gap: '0.5rem' }}>
                {incident.runbookReferences.map((r, idx) => (
                  <div key={idx} style={{ padding: '0.6rem 0.85rem', backgroundColor: 'var(--bg-main)', borderRadius: 'var(--radius-sm)', fontSize: '0.8rem' }}>
                    {r.snippet || r.query || r.source}
                  </div>
                ))}
              </div>
            </section>
          )}

          {/* Timeline */}
          <section className="card">
            <h3 style={{ fontSize: '1rem', fontWeight: 700, marginBottom: '1rem', display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
              <Clock size={17} color="#0057B8" /> Timeline
            </h3>
            {incident.timeline.length === 0 ? (
              <p style={{ color: 'var(--text-muted)', fontSize: '0.85rem' }}>No timeline events recorded.</p>
            ) : (
              <div style={{ display: 'flex', flexDirection: 'column', gap: '0.9rem' }}>
                {incident.timeline.map((entry) => (
                  <div key={entry.id} style={{ display: 'flex', gap: '0.75rem' }}>
                    <div style={{ width: 8, height: 8, borderRadius: '50%', background: 'var(--brand-primary)', marginTop: '0.4rem', flexShrink: 0 }} />
                    <div>
                      <div style={{ fontSize: '0.8rem', fontWeight: 600, color: 'var(--text-main)' }}>
                        {entry.eventType} <span style={{ fontWeight: 400, color: 'var(--text-muted)' }}>· {entry.actor}</span>
                      </div>
                      {entry.note && <div style={{ fontSize: '0.8rem', color: 'var(--text-muted)' }}>{entry.note}</div>}
                      <div style={{ fontSize: '0.72rem', color: 'var(--text-light)', fontFamily: 'var(--font-mono)' }}>{formatDateTime(entry.at)}</div>
                    </div>
                  </div>
                ))}
              </div>
            )}
          </section>
        </div>

        {/* Similar past incidents */}
        <div style={{ minWidth: 0 }}>
          <section className="card">
            <h3 style={{ fontSize: '1rem', fontWeight: 700, marginBottom: '1rem', display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
              <Copy size={17} color="#0057B8" /> Similar Past Incidents
            </h3>
            {incident.similarIncidents.length === 0 ? (
              <p style={{ color: 'var(--text-muted)', fontSize: '0.85rem' }}>No similar past incidents found.</p>
            ) : (
              <div style={{ display: 'flex', flexDirection: 'column', gap: '0.6rem' }}>
                {incident.similarIncidents.map((s) => (
                  <Link
                    key={s.id}
                    to={`/incidents/${s.id}`}
                    style={{ padding: '0.6rem 0.75rem', border: '1px solid var(--border-color)', borderRadius: 'var(--radius-sm)', display: 'block' }}
                  >
                    <div style={{ display: 'flex', alignItems: 'center', gap: '0.4rem', marginBottom: '0.25rem' }}>
                      <SeverityBadge severity={s.severity} />
                    </div>
                    <div style={{ fontSize: '0.82rem', fontWeight: 600, color: 'var(--text-main)' }}>{s.title}</div>
                    <div style={{ fontSize: '0.72rem', color: 'var(--text-muted)' }}>{formatDateTime(s.openedAt)}</div>
                  </Link>
                ))}
              </div>
            )}
          </section>
        </div>
      </div>

      {toast && (
        <div
          role="status"
          aria-live="polite"
          style={{
            position: 'fixed',
            bottom: '1.5rem',
            right: '1.5rem',
            zIndex: 100,
            display: 'flex',
            alignItems: 'center',
            gap: '0.5rem',
            padding: '0.75rem 1.1rem',
            borderRadius: 'var(--radius-sm)',
            boxShadow: 'var(--shadow-modal)',
            backgroundColor: toast.type === 'success' ? '#ECFDF5' : '#FEF2F2',
            color: toast.type === 'success' ? '#16A34A' : '#DC2626',
            fontSize: '0.85rem',
            fontWeight: 600,
          }}
        >
          {toast.type === 'success' ? <CheckCircle2 size={16} /> : <XCircle size={16} />}
          {toast.message}
        </div>
      )}
    </div>
  );
};

const BackLink: React.FC = () => (
  <Link to="/incidents" style={{ display: 'inline-flex', alignItems: 'center', gap: '0.4rem', fontSize: '0.85rem', color: 'var(--text-muted)', marginBottom: '1rem' }}>
    <ArrowLeft size={15} /> Back to Incidents
  </Link>
);
