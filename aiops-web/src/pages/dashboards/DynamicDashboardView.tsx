import React, { useEffect, useState, useCallback } from 'react';
import { useParams, Link, useNavigate } from 'react-router-dom';
import { useAuth } from '../../context/AuthContext';
import { useTenants } from '../../context/TenantContext';
import { TimeRangeSelector, TimeRangeOption, TIME_RANGE_OPTIONS } from '../../components/Charts/TimeRangeSelector';
import { TimeSeriesChart } from '../../components/Charts/TimeSeriesChart';
import { StatCard, ThresholdStep } from '../../components/Charts/StatCard';
import { StatusTable, ServiceStatusItem } from '../../components/Charts/StatusTable';
import { getDashboard, DashboardDefinition, PanelDefinition } from '../../services/dashboardService';
import { queryPromqlInstant, queryPromqlRange, queryLogqlRange, transformMatrixToUPlot, UPlotDataBundle } from '../../services/queryService';
import { 
  ArrowLeft, 
  RefreshCw, 
  Code, 
  AlertCircle, 
  CheckCircle, 
  Sliders, 
  Layers, 
  Search,
  ExternalLink 
} from 'lucide-react';

interface DynamicDashboardViewProps {
  initialId?: string;
}

export const DynamicDashboardView: React.FC<DynamicDashboardViewProps> = ({ initialId }) => {
  const { id: routeId } = useParams<{ id: string }>();
  const dashboardId = initialId || routeId || 'windows-monitor';
  const navigate = useNavigate();

  const { activeTenantId } = useAuth();
  const { tenants } = useTenants();
  const activeTenant = tenants.find(t => t.id === activeTenantId);
  const tenantIdStr = activeTenant?.id || activeTenantId || 'default';

  const [dashboard, setDashboard] = useState<DashboardDefinition | null>(null);
  const [loading, setLoading] = useState<boolean>(true);
  const [error, setError] = useState<string | null>(null);

  // Time range selector state
  const [selectedRangeSeconds, setSelectedRangeSeconds] = useState<number>(3600); // Default 1 hour

  // Panel query results state: panelId -> data / error / loading
  const [panelResults, setPanelResults] = useState<Record<string, { loading: boolean; data: any; error?: string }>>({});
  const [showJsonModal, setShowJsonModal] = useState<boolean>(false);

  // 1. Fetch dashboard definition
  const fetchDefinition = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const def = await getDashboard(dashboardId);
      setDashboard(def);
    } catch (err: any) {
      setError(err?.message || `Failed to load dashboard definition: ${dashboardId}`);
    } finally {
      setLoading(false);
    }
  }, [dashboardId]);

  useEffect(() => {
    fetchDefinition();
  }, [fetchDefinition]);

  // Compute time window in epoch seconds
  const getWindowEpochs = useCallback(() => {
    const end = Math.floor(Date.now() / 1000);
    const start = end - selectedRangeSeconds;
    return { start, end };
  }, [selectedRangeSeconds]);

  // 2. Substitute query variables ($tenant -> current tenant)
  const substituteQuery = (queryStr: string): string => {
    if (!queryStr) return '';
    return queryStr
      .replace(/\$tenant/g, tenantIdStr)
      .replace(/\$\{tenant\}/g, tenantIdStr);
  };

  // 3. Execute queries for all panels with graceful per-panel error handling
  const loadPanelData = useCallback(async () => {
    if (!dashboard || !dashboard.panels) return;

    const { start, end } = getWindowEpochs();

    // Mark all panels as loading
    const initialStates: Record<string, { loading: boolean; data: any; error?: string }> = {};
    dashboard.panels.forEach(p => {
      initialStates[p.id] = { loading: true, data: null };
    });
    setPanelResults(initialStates);

    // Run panel queries in parallel
    const promises = dashboard.panels.map(async (panel) => {
      const substitutedQuery = substituteQuery(panel.query);
      try {
        if (panel.type === 'stat') {
          const res = await queryPromqlInstant(substitutedQuery);
          let value: number | string = 'N/A';
          if (res?.data?.result && res.data.result.length > 0) {
            const valArr = res.data.result[0].value;
            if (valArr && valArr.length > 1) {
              const parsed = parseFloat(valArr[1]);
              value = isNaN(parsed) ? valArr[1] : parsed;
            }
          }
          return { panelId: panel.id, data: value };
        } else if (panel.type === 'table') {
          const res = await queryPromqlInstant(substitutedQuery);
          const items: ServiceStatusItem[] = [];
          if (res?.data?.result && Array.isArray(res.data.result)) {
            res.data.result.forEach((r, idx) => {
              const metric = r.metric || {};
              const service = metric.service || metric.name || metric.subsystem || metric.queue || metric.file || `item-${idx+1}`;
              const server = metric.server || metric.host || tenantIdStr;
              const val = r.value ? parseFloat(r.value[1]) : 0;
              items.push({
                server,
                service,
                statusValue: isNaN(val) ? 0 : val,
              });
            });
          }
          return { panelId: panel.id, data: items };
        } else if (panel.type === 'logs') {
          const res = await queryLogqlRange(substitutedQuery, start, end);
          return { panelId: panel.id, data: res?.data?.result || [] };
        } else {
          const res = await queryPromqlRange(substitutedQuery, start, end);
          const matrix = res?.data?.result || [];
          const uplotBundle = transformMatrixToUPlot(matrix, panel.legendFormat);
          return { panelId: panel.id, data: uplotBundle };
        }
      } catch (err: any) {
        return { panelId: panel.id, data: null, error: err?.message || 'Panel query failed' };
      }
    });

    const results = await Promise.allSettled(promises);
    setPanelResults(prev => {
      const updated = { ...prev };
      results.forEach(res => {
        if (res.status === 'fulfilled') {
          const { panelId, data, error: panelErr } = res.value;
          updated[panelId] = { loading: false, data, error: panelErr };
        }
      });
      return updated;
    });
  }, [dashboard, getWindowEpochs, tenantIdStr]);

  useEffect(() => {
    if (dashboard) {
      loadPanelData();
    }
  }, [dashboard, loadPanelData, selectedRangeSeconds, tenantIdStr]);

  const mapThresholds = (threshObj?: Record<string, any>): ThresholdStep[] | undefined => {
    if (!threshObj) return undefined;
    const steps: ThresholdStep[] = [{ value: null, color: 'green' }];
    if (typeof threshObj.warning === 'number') {
      steps.push({ value: threshObj.warning, color: 'orange' });
    }
    if (typeof threshObj.critical === 'number') {
      steps.push({ value: threshObj.critical, color: 'red' });
    }
    return steps.length > 1 ? steps : undefined;
  };

  if (loading) {
    return (
      <div style={{ padding: '2rem', textAlign: 'center', color: '#94A3B8' }}>
        <RefreshCw size={24} className="spin" style={{ marginBottom: '0.5rem' }} />
        <div>Loading dashboard definition...</div>
      </div>
    );
  }

  if (error || !dashboard) {
    return (
      <div style={{ padding: '2rem' }}>
        <div style={{ padding: '1.5rem', backgroundColor: '#FEF2F2', border: '1px solid #FECACA', borderRadius: 'var(--radius-md)', color: '#991B1B' }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', fontWeight: 700, marginBottom: '0.5rem' }}>
            <AlertCircle size={20} />
            <span>Dashboard Load Error</span>
          </div>
          <div>{error || 'Dashboard definition not found.'}</div>
          <button
            onClick={() => navigate('/dashboards')}
            style={{ marginTop: '1rem', padding: '0.5rem 1rem', backgroundColor: '#0057B8', color: 'white', border: 'none', borderRadius: 'var(--radius-sm)', cursor: 'pointer' }}
          >
            Back to Dashboard Catalog
          </button>
        </div>
      </div>
    );
  }

  return (
    <div style={{ padding: '1.5rem', display: 'flex', flexDirection: 'column', gap: '1.5rem' }}>
      {/* Header Bar */}
      <div style={{ display: 'flex', flexWrap: 'wrap', alignItems: 'center', justifyContent: 'space-between', gap: '1rem' }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: '1rem' }}>
          <Link
            to="/dashboards"
            style={{ display: 'inline-flex', alignItems: 'center', gap: '0.4rem', color: '#94A3B8', textDecoration: 'none', fontSize: '0.875rem' }}
          >
            <ArrowLeft size={16} /> Catalog
          </Link>

          <div>
            <div style={{ display: 'flex', alignItems: 'center', gap: '0.6rem' }}>
              <h1 style={{ fontSize: '1.5rem', fontWeight: 800, color: 'white', margin: 0 }}>
                {dashboard.title}
              </h1>
              <span
                style={{
                  fontSize: '0.75rem',
                  fontWeight: 600,
                  padding: '0.2rem 0.6rem',
                  borderRadius: 'var(--radius-full)',
                  backgroundColor: '#1E293B',
                  color: '#38BDF8',
                  border: '1px solid #0284C7',
                }}
              >
                {dashboard.category || 'General'}
              </span>
              {dashboard.custom && (
                <span
                  style={{
                    fontSize: '0.75rem',
                    fontWeight: 700,
                    padding: '0.2rem 0.6rem',
                    borderRadius: 'var(--radius-full)',
                    backgroundColor: '#F5A300',
                    color: '#0F172A',
                  }}
                >
                  Custom
                </span>
              )}
            </div>
            <div style={{ fontSize: '0.8rem', color: '#94A3B8', marginTop: '0.25rem' }}>
              Config-driven dashboard engine • Tenant: <strong style={{ color: 'white' }}>{activeTenant?.name || tenantIdStr}</strong>
            </div>
          </div>
        </div>

        {/* Right Controls */}
        <div style={{ display: 'flex', alignItems: 'center', gap: '0.75rem' }}>
          <button
            onClick={() => setShowJsonModal(true)}
            style={{
              display: 'inline-flex',
              alignItems: 'center',
              gap: '0.4rem',
              padding: '0.5rem 0.85rem',
              backgroundColor: '#0F172A',
              color: '#38BDF8',
              border: '1px solid #0284C7',
              borderRadius: 'var(--radius-sm)',
              cursor: 'pointer',
              fontSize: '0.85rem',
            }}
          >
            <Code size={14} /> View JSON
          </button>
        </div>
      </div>

      {/* Global Time Range Selector Bar */}
      <TimeRangeSelector
        selectedRangeSeconds={selectedRangeSeconds}
        onRangeChange={(opt: TimeRangeOption) => setSelectedRangeSeconds(opt.seconds)}
        onManualRefresh={loadPanelData}
      />

      {/* Grid Layout of Panels */}
      <div
        style={{
          display: 'grid',
          gridTemplateColumns: 'repeat(12, 1fr)',
          gap: '1rem',
        }}
      >
        {dashboard.panels.map((panel) => {
          const w = panel.gridPos?.w || 12;
          const colSpan = Math.min(Math.max(w, 1), 12);
          const pResult = panelResults[panel.id] || { loading: true, data: null };

          return (
            <div
              key={panel.id}
              style={{
                gridColumn: `span ${colSpan}`,
                backgroundColor: '#1E293B',
                border: '1px solid #334155',
                borderRadius: 'var(--radius-md)',
                padding: '1rem',
                display: 'flex',
                flexDirection: 'column',
                gap: '0.75rem',
                minHeight: panel.type === 'stat' ? '140px' : '280px',
              }}
            >
              {/* Panel Header */}
              <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
                <h3 style={{ fontSize: '0.95rem', fontWeight: 700, color: 'white', margin: 0 }}>
                  {panel.title}
                </h3>
                <span style={{ fontSize: '0.7rem', color: '#64748B', fontFamily: 'monospace' }}>
                  {panel.type}
                </span>
              </div>

              {/* Panel Content / Isolated Fallback */}
              {pResult.loading ? (
                <div style={{ flex: 1, display: 'flex', alignItems: 'center', justifyContent: 'center', color: '#94A3B8', fontSize: '0.85rem' }}>
                  <RefreshCw size={16} className="spin" style={{ marginRight: '0.4rem' }} /> Querying...
                </div>
              ) : pResult.error ? (
                <div style={{ padding: '0.75rem', backgroundColor: '#451A1A', border: '1px solid #7F1D1D', borderRadius: 'var(--radius-sm)', color: '#FCA5A5', fontSize: '0.8rem' }}>
                  <div style={{ fontWeight: 600, marginBottom: '0.2rem' }}>Panel Query Error</div>
                  <div>{pResult.error}</div>
                </div>
              ) : panel.type === 'stat' ? (
                <StatCard
                  title={panel.title}
                  value={pResult.data}
                  unit={panel.unit}
                  thresholds={mapThresholds(panel.thresholds)}
                />
              ) : panel.type === 'table' ? (
                <StatusTable
                  title={panel.title}
                  items={Array.isArray(pResult.data) ? pResult.data : []}
                  tenantId={tenantIdStr}
                />
              ) : panel.type === 'logs' ? (
                <div style={{ flex: 1, backgroundColor: '#0F172A', borderRadius: 'var(--radius-sm)', padding: '0.75rem', fontFamily: 'monospace', fontSize: '0.75rem', color: '#CBD5E1', overflowY: 'auto', maxHeight: '300px' }}>
                  {Array.isArray(pResult.data) && pResult.data.length > 0 ? (
                    pResult.data.map((stream: any, idx: number) => (
                      <div key={idx} style={{ marginBottom: '0.5rem' }}>
                        <div style={{ color: '#F5A300', fontWeight: 600 }}>{JSON.stringify(stream.stream || {})}</div>
                        {(stream.values || []).map(([ts, line]: any, lIdx: number) => (
                          <div key={lIdx} style={{ paddingLeft: '0.5rem', whiteSpace: 'pre-wrap', wordBreak: 'break-all' }}>
                            <span style={{ color: '#64748B' }}>{new Date(parseInt(ts) / 1000000).toISOString()}</span> {line}
                          </div>
                        ))}
                      </div>
                    ))
                  ) : (
                    <div style={{ color: '#64748B', textAlign: 'center', padding: '1rem' }}>No log entries found for time range.</div>
                  )}
                </div>
              ) : (
                <div style={{ flex: 1 }}>
                  <TimeSeriesChart
                    title=""
                    dataBundle={pResult.data as UPlotDataBundle}
                    unit={panel.unit}
                  />
                </div>
              )}
            </div>
          );
        })}
      </div>

      {/* View JSON Definition Modal */}
      {showJsonModal && (
        <div style={{ position: 'fixed', inset: 0, backgroundColor: 'rgba(15, 23, 42, 0.75)', zIndex: 100, display: 'flex', alignItems: 'center', justifyContent: 'center', padding: '1.5rem' }}>
          <div style={{ backgroundColor: '#1E293B', width: '100%', maxWidth: '700px', borderRadius: 'var(--radius-md)', border: '1px solid #334155', display: 'flex', flexDirection: 'column', maxHeight: '80vh' }}>
            <div style={{ padding: '1rem 1.25rem', borderBottom: '1px solid #334155', display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
              <h2 style={{ fontSize: '1.1rem', fontWeight: 700, color: 'white', margin: 0 }}>
                JSON Definition — {dashboard.title}
              </h2>
              <button
                onClick={() => setShowJsonModal(false)}
                style={{ backgroundColor: 'transparent', border: 'none', color: '#94A3B8', cursor: 'pointer', fontSize: '1.2rem' }}
              >
                ✕
              </button>
            </div>
            <div style={{ padding: '1.25rem', overflowY: 'auto', flex: 1 }}>
              <pre style={{ backgroundColor: '#0F172A', padding: '1rem', borderRadius: 'var(--radius-sm)', color: '#38BDF8', fontSize: '0.8rem', fontFamily: 'monospace', whiteSpace: 'pre-wrap' }}>
                {JSON.stringify(dashboard, null, 2)}
              </pre>
            </div>
            <div style={{ padding: '1rem', borderTop: '1px solid #334155', textAlign: 'right' }}>
              <button
                onClick={() => setShowJsonModal(false)}
                style={{ padding: '0.5rem 1rem', backgroundColor: '#0057B8', color: 'white', border: 'none', borderRadius: 'var(--radius-sm)', cursor: 'pointer' }}
              >
                Close
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};
