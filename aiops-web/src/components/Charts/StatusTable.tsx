import React, { useState } from 'react';
import { ExternalLink, Search, RefreshCw, CheckCircle, XCircle } from 'lucide-react';
import { useNavigate } from 'react-router-dom';

export interface ServiceStatusItem {
  server: string;
  service: string;
  statusValue: number; // 1 = Running, 0 = Down/Not Running
}

interface StatusTableProps {
  title: string;
  items: ServiceStatusItem[];
  loading?: boolean;
  error?: string | null;
  onRefresh?: () => void;
  tenantId?: string;
}

export const StatusTable: React.FC<StatusTableProps> = ({
  title,
  items,
  loading = false,
  error = null,
  onRefresh,
  tenantId,
}) => {
  const [searchTerm, setSearchTerm] = useState('');
  const [sortField, setSortField] = useState<'server' | 'service' | 'statusValue'>('server');
  const [sortAsc, setSortAsc] = useState(true);
  const navigate = useNavigate();

  const filteredItems = items.filter(
    (item) =>
      item.server.toLowerCase().includes(searchTerm.toLowerCase()) ||
      item.service.toLowerCase().includes(searchTerm.toLowerCase())
  );

  const sortedItems = [...filteredItems].sort((a, b) => {
    const valA = a[sortField];
    const valB = b[sortField];
    if (valA < valB) return sortAsc ? -1 : 1;
    if (valA > valB) return sortAsc ? 1 : -1;
    return 0;
  });

  const handleSort = (field: 'server' | 'service' | 'statusValue') => {
    if (sortField === field) {
      setSortAsc(!sortAsc);
    } else {
      setSortField(field);
      setSortAsc(true);
    }
  };

  const handleOpenLogsForService = (server: string, service: string) => {
    const params = new URLSearchParams();
    if (tenantId) params.set('tenantId', tenantId);
    params.set('server', server);
    params.set('service', service);
    const logQuery = `{server="${server}"}`;
    params.set('query', logQuery);
    navigate(`/logs?${params.toString()}`);
  };

  return (
    <div className="card" style={{ padding: '1rem' }}>
      <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '1rem', flexWrap: 'wrap', gap: '0.5rem' }}>
        <h3 style={{ fontSize: '0.95rem', fontWeight: 700, color: 'var(--text-main)', margin: 0 }}>
          {title} ({items.length})
        </h3>

        <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
          <div style={{ position: 'relative' }}>
            <Search size={14} style={{ position: 'absolute', left: '0.6rem', top: '50%', transform: 'translateY(-50%)', color: '#94A3B8' }} />
            <input
              type="text"
              placeholder="Search server/service..."
              value={searchTerm}
              onChange={(e) => setSearchTerm(e.target.value)}
              style={{
                padding: '0.35rem 0.6rem 0.35rem 2rem',
                fontSize: '0.8rem',
                borderRadius: 'var(--radius-sm)',
                border: '1px solid var(--border-color)',
                outline: 'none',
                width: '180px',
              }}
            />
          </div>

          {onRefresh && (
            <button onClick={onRefresh} className="btn btn-secondary" style={{ padding: '0.35rem 0.5rem', fontSize: '0.75rem' }}>
              <RefreshCw size={12} className={loading ? 'spin' : ''} />
            </button>
          )}
        </div>
      </div>

      {loading ? (
        <div style={{ padding: '2rem', textAlign: 'center', color: '#64748B', fontSize: '0.85rem' }}>
          <RefreshCw size={18} className="spin" style={{ display: 'inline', marginRight: '0.5rem' }} /> Loading service statuses...
        </div>
      ) : error ? (
        <div style={{ padding: '1rem', color: '#EF4444', backgroundColor: '#FEF2F2', borderRadius: 'var(--radius-sm)', fontSize: '0.8rem' }}>
          Failed to load status table: {error}
        </div>
      ) : sortedItems.length === 0 ? (
        <div style={{ padding: '2rem', textAlign: 'center', color: '#94A3B8', fontSize: '0.85rem', border: '1px dashed #E2E8F0', borderRadius: 'var(--radius-sm)' }}>
          No services matching filter criteria.
        </div>
      ) : (
        <div style={{ overflowX: 'auto' }}>
          <table style={{ width: '100%', borderCollapse: 'collapse', fontSize: '0.85rem', textAlign: 'left' }}>
            <thead>
              <tr style={{ borderBottom: '2px solid var(--border-color)', color: '#64748B', fontSize: '0.75rem', textTransform: 'uppercase' }}>
                <th style={{ padding: '0.5rem 0.75rem', cursor: 'pointer' }} onClick={() => handleSort('server')}>
                  Server {sortField === 'server' ? (sortAsc ? '▲' : '▼') : ''}
                </th>
                <th style={{ padding: '0.5rem 0.75rem', cursor: 'pointer' }} onClick={() => handleSort('service')}>
                  Service {sortField === 'service' ? (sortAsc ? '▲' : '▼') : ''}
                </th>
                <th style={{ padding: '0.5rem 0.75rem', cursor: 'pointer' }} onClick={() => handleSort('statusValue')}>
                  Status {sortField === 'statusValue' ? (sortAsc ? '▲' : '▼') : ''}
                </th>
                <th style={{ padding: '0.5rem 0.75rem', textAlign: 'right' }}>Actions</th>
              </tr>
            </thead>
            <tbody>
              {sortedItems.map((item, idx) => {
                const isRunning = item.statusValue === 1;
                return (
                  <tr
                    key={idx}
                    style={{
                      borderBottom: '1px solid var(--border-color)',
                      backgroundColor: idx % 2 === 0 ? 'white' : '#F8FAFC',
                    }}
                  >
                    <td style={{ padding: '0.6rem 0.75rem', fontWeight: 600, color: 'var(--text-main)' }}>
                      {item.server}
                    </td>
                    <td style={{ padding: '0.6rem 0.75rem', color: '#475569', fontFamily: 'var(--font-mono)', fontSize: '0.8rem' }}>
                      {item.service}
                    </td>
                    <td style={{ padding: '0.6rem 0.75rem' }}>
                      <span
                        style={{
                          display: 'inline-flex',
                          alignItems: 'center',
                          gap: '0.35rem',
                          padding: '0.2rem 0.55rem',
                          borderRadius: 'var(--radius-full)',
                          fontSize: '0.75rem',
                          fontWeight: 700,
                          backgroundColor: isRunning ? '#D1FAE5' : '#FEE2E2',
                          color: isRunning ? '#065F46' : '#991B1B',
                        }}
                      >
                        {isRunning ? <CheckCircle size={12} /> : <XCircle size={12} />}
                        {isRunning ? 'Running' : 'DOWN'}
                      </span>
                    </td>
                    <td style={{ padding: '0.6rem 0.75rem', textAlign: 'right' }}>
                      <button
                        onClick={() => handleOpenLogsForService(item.server, item.service)}
                        className="btn btn-secondary"
                        style={{ padding: '0.2rem 0.45rem', fontSize: '0.75rem', color: '#0057B8' }}
                        title="View server logs in Log Explorer"
                      >
                        <ExternalLink size={12} /> Logs
                      </button>
                    </td>
                  </tr>
                );
              })}
            </tbody>
          </table>
        </div>
      )}
    </div>
  );
};
