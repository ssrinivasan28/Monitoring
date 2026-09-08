import React, { useState, useEffect, useCallback } from 'react';
import { useSearchParams } from 'react-router-dom';
import { FileText, Search, Play, RefreshCw, AlertCircle, Calendar } from 'lucide-react';
import { queryLogqlRange } from '../services/queryService';
import { useAuth } from '../context/AuthContext';

export interface LogLine {
  timestamp: string;
  labels: Record<string, string>;
  message: string;
}

export const LogExplorerPage: React.FC = () => {
  const [searchParams] = useSearchParams();
  const { activeTenantId } = useAuth();

  const initialQuery = searchParams.get('query') || '{job=~".+"}';
  const initialServer = searchParams.get('server');
  const initialService = searchParams.get('service');

  const [logQuery, setLogQuery] = useState<string>(
    initialServer ? `{server="${initialServer}"}` : initialQuery
  );

  const [loading, setLoading] = useState<boolean>(false);
  const [error, setError] = useState<string | null>(null);
  const [logs, setLogs] = useState<LogLine[]>([]);
  const [resultCount, setResultCount] = useState<number>(0);

  const executeLogSearch = useCallback(async () => {
    if (!activeTenantId || !logQuery.trim()) return;
    setLoading(true);
    setError(null);

    const now = Math.floor(Date.now() / 1000);
    const startParam = searchParams.get('start');
    const endParam = searchParams.get('end');

    const start = startParam ? parseFloat(startParam) : now - 21600; // default 6h
    const end = endParam ? parseFloat(endParam) : now;

    try {
      const res = await queryLogqlRange(logQuery, start, end, '30s');

      if (res.data?.result) {
        const parsedLogs: LogLine[] = [];
        res.data.result.forEach((streamItem: any) => {
          const streamLabels = streamItem.stream || {};
          (streamItem.values || []).forEach(([nanoTs, logMsg]: [string, string]) => {
            const epochSec = parseFloat(nanoTs) / 1e9;
            const d = isNaN(epochSec) ? new Date() : new Date(epochSec * 1000);
            parsedLogs.push({
              timestamp: d.toISOString().replace('T', ' ').substring(0, 19),
              labels: streamLabels,
              message: logMsg,
            });
          });
        });

        // Sort descending by timestamp
        parsedLogs.sort((a, b) => b.timestamp.localeCompare(a.timestamp));
        setLogs(parsedLogs);
        setResultCount(parsedLogs.length);
      } else {
        setLogs([]);
        setResultCount(0);
      }
    } catch (err: any) {
      setError(err.message || 'Failed to query Loki logs via gateway');
      setLogs([]);
    } finally {
      setLoading(false);
    }
  }, [activeTenantId, logQuery, searchParams]);

  useEffect(() => {
    executeLogSearch();
  }, [executeLogSearch]);

  return (
    <div style={{ maxWidth: '1400px', margin: '0 auto' }}>
      {/* Page Header */}
      <div style={{ marginBottom: '1.25rem' }}>
        <h1 style={{ fontSize: '1.5rem', fontWeight: 800, color: 'var(--text-main)', display: 'flex', alignItems: 'center', gap: '0.6rem' }}>
          <FileText color="#0057B8" size={26} /> Log Explorer (Gateway Audited)
        </h1>
        <p style={{ fontSize: '0.875rem', color: 'var(--text-muted)' }}>
          Scoped LogQL log inspection with automated SOC 2 audit trail and tenant isolation.
        </p>
      </div>

      {/* Query Bar */}
      <div className="card" style={{ padding: '1rem', marginBottom: '1.5rem' }}>
        <div style={{ display: 'flex', gap: '0.75rem', alignItems: 'center' }}>
          <div style={{ flex: 1, position: 'relative' }}>
            <Search size={16} style={{ position: 'absolute', left: '0.75rem', top: '50%', transform: 'translateY(-50%)', color: '#94A3B8' }} />
            <input
              type="text"
              value={logQuery}
              onChange={(e) => setLogQuery(e.target.value)}
              placeholder='Enter LogQL query (e.g. {server="SVR-PROD-01"} |~ "error")'
              style={{
                width: '100%',
                padding: '0.65rem 0.75rem 0.65rem 2.25rem',
                fontSize: '0.875rem',
                fontFamily: 'var(--font-mono)',
                borderRadius: 'var(--radius-sm)',
                border: '1px solid var(--border-color)',
                outline: 'none',
              }}
              onKeyDown={(e) => {
                if (e.key === 'Enter') executeLogSearch();
              }}
            />
          </div>

          <button
            onClick={executeLogSearch}
            className="btn btn-primary"
            style={{ display: 'flex', alignItems: 'center', gap: '0.4rem', padding: '0.65rem 1.25rem' }}
            disabled={loading}
          >
            {loading ? <RefreshCw size={16} className="spin" /> : <Play size={16} />}
            Run LogQL
          </button>
        </div>

        {/* Filter context badges */}
        {(initialServer || initialService) && (
          <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', marginTop: '0.75rem', fontSize: '0.75rem', color: '#64748B' }}>
            <span>Cross-linked Context:</span>
            {initialServer && (
              <span className="badge badge-primary">Server: {initialServer}</span>
            )}
            {initialService && (
              <span className="badge badge-warning">Service: {initialService}</span>
            )}
          </div>
        )}
      </div>

      {/* Results Header */}
      <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '0.75rem', fontSize: '0.85rem' }}>
        <div style={{ fontWeight: 600, color: 'var(--text-main)' }}>
          Log Results ({resultCount} log entries)
        </div>
        <div style={{ color: 'var(--text-muted)', display: 'flex', alignItems: 'center', gap: '0.3rem' }}>
          <Calendar size={14} /> Time Range: {searchParams.get('start') ? 'Custom Window' : 'Last 6 Hours'}
        </div>
      </div>

      {/* Log Output Container */}
      <div className="card" style={{ padding: '1rem', backgroundColor: '#0F172A', color: '#F8FAFC', minHeight: '400px' }}>
        {loading ? (
          <div style={{ padding: '3rem', textAlign: 'center', color: '#94A3B8' }}>
            <RefreshCw size={24} className="spin" style={{ display: 'inline', marginBottom: '0.5rem' }} />
            <div>Querying Loki via audited query gateway...</div>
          </div>
        ) : error ? (
          <div style={{ padding: '1.5rem', backgroundColor: '#450A0A', color: '#FCA5A5', borderRadius: 'var(--radius-sm)', display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
            <AlertCircle size={18} /> {error}
          </div>
        ) : logs.length === 0 ? (
          <div style={{ padding: '3rem', textAlign: 'center', color: '#64748B', fontFamily: 'var(--font-mono)', fontSize: '0.85rem' }}>
            No log entries match the query for the active tenant context.
          </div>
        ) : (
          <div style={{ display: 'flex', flexDirection: 'column', gap: '0.4rem', fontFamily: 'var(--font-mono)', fontSize: '0.8rem' }}>
            {logs.map((log, idx) => {
              const isError = log.message.toLowerCase().includes('error') || log.message.toLowerCase().includes('failed');
              const isWarn = log.message.toLowerCase().includes('warn');

              return (
                <div
                  key={idx}
                  style={{
                    padding: '0.4rem 0.6rem',
                    borderRadius: '2px',
                    backgroundColor: idx % 2 === 0 ? 'rgba(255, 255, 255, 0.03)' : 'transparent',
                    borderLeft: isError ? '3px solid #EF4444' : isWarn ? '3px solid #F5A300' : '3px solid #3B82F6',
                    display: 'flex',
                    flexDirection: 'column',
                    gap: '0.2rem',
                  }}
                >
                  <div style={{ display: 'flex', alignItems: 'center', gap: '0.75rem', fontSize: '0.75rem', color: '#94A3B8' }}>
                    <span style={{ color: '#60A5FA', fontWeight: 600 }}>{log.timestamp}</span>
                    {Object.entries(log.labels).map(([k, v]) => (
                      <span key={k} style={{ backgroundColor: '#1E293B', padding: '0.1rem 0.35rem', borderRadius: '2px', color: '#CBD5E1' }}>
                        {k}={v}
                      </span>
                    ))}
                  </div>
                  <div style={{ color: isError ? '#FCA5A5' : isWarn ? '#FDE68A' : '#E2E8F0', wordBreak: 'break-all' }}>
                    {log.message}
                  </div>
                </div>
              );
            })}
          </div>
        )}
      </div>
    </div>
  );
};
