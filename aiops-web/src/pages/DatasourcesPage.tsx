import React, { useState, useEffect } from 'react';
import { useAuth } from '../context/AuthContext';
import { datasourceService } from '../services/datasourceService';
import { 
  TenantDatasource, 
  DatasourceKind, 
  AuthType, 
  CreateDatasourceRequest,
  TestConnectionResponse
} from '../types';
import { 
  Database, 
  Plus, 
  CheckCircle2, 
  XCircle, 
  Trash2, 
  Edit3, 
  Wifi, 
  Lock, 
  RefreshCw, 
  AlertTriangle,
  Server,
  Layers,
  FileCode
} from 'lucide-react';

export const DatasourcesPage: React.FC = () => {
  const { activeTenantId } = useAuth();

  const [datasources, setDatasources] = useState<TenantDatasource[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [successMessage, setSuccessMessage] = useState<string | null>(null);

  // Modal State
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [editingId, setEditingId] = useState<string | null>(null);

  // Form Fields
  const [name, setName] = useState('');
  const [kind, setKind] = useState<DatasourceKind>('prometheus');
  const [url, setUrl] = useState('');
  const [authType, setAuthType] = useState<AuthType>('NONE');
  const [authUsername, setAuthUsername] = useState('');
  const [authSecret, setAuthSecret] = useState('');
  const [enabled, setEnabled] = useState(true);

  // Test Connection State
  const [testing, setTesting] = useState(false);
  const [testResult, setTestResult] = useState<TestConnectionResponse | null>(null);

  const loadDatasources = async () => {
    setLoading(true);
    setError(null);
    try {
      const data = await datasourceService.listDatasources(activeTenantId || undefined);
      setDatasources(data);
    } catch (err: any) {
      setError(err.message || 'Failed to load data sources');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadDatasources();
  }, [activeTenantId]);

  const resetForm = () => {
    setEditingId(null);
    setName('');
    setKind('prometheus');
    setUrl('');
    setAuthType('NONE');
    setAuthUsername('');
    setAuthSecret('');
    setEnabled(true);
    setTestResult(null);
  };

  const handleOpenAddModal = () => {
    resetForm();
    setIsModalOpen(true);
  };

  const handleOpenEditModal = (ds: TenantDatasource) => {
    setEditingId(ds.id);
    setName(ds.name);
    setKind(ds.kind);
    setUrl(ds.url);
    setAuthType(ds.authType);
    setAuthUsername(ds.authUsername || '');
    setAuthSecret(''); // Secrets are never echoed back
    setEnabled(ds.enabled);
    setTestResult(null);
    setIsModalOpen(true);
  };

  const handleTestConnection = async () => {
    if (!url.trim()) {
      setError('Please provide a valid URL to test.');
      return;
    }
    setTesting(true);
    setTestResult(null);
    setError(null);

    const dto: CreateDatasourceRequest = {
      tenantId: activeTenantId || undefined,
      name: name || 'Test Source',
      kind,
      url,
      authType,
      authUsername: authUsername || undefined,
      authSecret: authSecret || undefined,
      enabled,
    };

    try {
      const result = await datasourceService.testConnection(dto);
      setTestResult(result);
    } catch (err: any) {
      setTestResult({
        reachable: false,
        message: err.message || 'Connection test failed',
      });
    } finally {
      setTesting(false);
    }
  };

  const handleSave = async (e: React.FormEvent) => {
    e.preventDefault();
    setError(null);
    setSuccessMessage(null);

    const dto: CreateDatasourceRequest = {
      tenantId: activeTenantId || undefined,
      name,
      kind,
      url,
      authType,
      authUsername: authUsername || undefined,
      authSecret: authSecret || undefined,
      enabled,
    };

    try {
      if (editingId) {
        await datasourceService.updateDatasource(editingId, dto);
        setSuccessMessage(`Data source '${name}' updated successfully.`);
      } else {
        await datasourceService.createDatasource(dto);
        setSuccessMessage(`Data source '${name}' created successfully.`);
      }
      setIsModalOpen(false);
      resetForm();
      loadDatasources();
    } catch (err: any) {
      setError(err.message || 'Failed to save data source.');
    }
  };

  const handleToggleEnable = async (ds: TenantDatasource) => {
    try {
      await datasourceService.updateDatasource(ds.id, { enabled: !ds.enabled });
      setSuccessMessage(`Data source '${ds.name}' ${!ds.enabled ? 'enabled' : 'disabled'}.`);
      loadDatasources();
    } catch (err: any) {
      setError(err.message || 'Failed to toggle data source state.');
    }
  };

  const handleDelete = async (ds: TenantDatasource) => {
    if (!window.confirm(`Are you sure you want to delete data source '${ds.name}'?`)) {
      return;
    }
    try {
      await datasourceService.deleteDatasource(ds.id);
      setSuccessMessage(`Data source '${ds.name}' deleted.`);
      loadDatasources();
    } catch (err: any) {
      setError(err.message || 'Failed to delete data source.');
    }
  };

  const getKindBadge = (kind: DatasourceKind) => {
    switch (kind) {
      case 'prometheus':
        return <span className="badge badge-accent"><Server size={12} /> PROMETHEUS</span>;
      case 'thanos':
        return <span className="badge badge-primary"><Layers size={12} /> THANOS</span>;
      case 'loki':
        return <span className="badge badge-warning"><FileCode size={12} /> LOKI</span>;
    }
  };

  return (
    <div style={{ maxWidth: '1200px', margin: '0 auto' }}>
      {/* Page Title & Controls */}
      <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '1.5rem', flexWrap: 'wrap', gap: '1rem' }}>
        <div>
          <h1 style={{ fontSize: '1.5rem', fontWeight: 800, color: 'var(--text-main)', display: 'flex', alignItems: 'center', gap: '0.6rem' }}>
            <Database color="#0057B8" size={26} /> Admin › Data Sources
          </h1>
          <p style={{ fontSize: '0.875rem', color: 'var(--text-muted)' }}>
            Configure multi-source Prometheus, Thanos, and Loki endpoints for the active tenant context.
          </p>
        </div>

        <div style={{ display: 'flex', gap: '0.75rem' }}>
          <button className="btn btn-secondary" onClick={loadDatasources} disabled={loading}>
            <RefreshCw size={16} className={loading ? 'spin' : ''} /> Refresh
          </button>
          <button className="btn btn-primary" onClick={handleOpenAddModal}>
            <Plus size={18} /> Add Data Source
          </button>
        </div>
      </div>

      {/* Notifications */}
      {successMessage && (
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
          <span>{successMessage}</span>
        </div>
      )}

      {error && (
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
          <AlertTriangle size={18} />
          <span>{error}</span>
        </div>
      )}

      {/* Data Sources Table / Cards */}
      {loading ? (
        <div className="card" style={{ textAlign: 'center', padding: '3rem' }}>
          <RefreshCw size={32} className="spin" color="#0057B8" style={{ marginBottom: '0.75rem' }} />
          <div style={{ color: 'var(--text-muted)' }}>Loading configured data sources...</div>
        </div>
      ) : datasources.length === 0 ? (
        <div className="card" style={{ textAlign: 'center', padding: '3.5rem 1.5rem' }}>
          <Database size={48} color="#CBD5E1" style={{ marginBottom: '1rem' }} />
          <h3 style={{ fontSize: '1.1rem', fontWeight: 700, marginBottom: '0.5rem' }}>No Data Sources Configured</h3>
          <p style={{ fontSize: '0.875rem', color: 'var(--text-muted)', maxWidth: '480px', margin: '0 auto 1.5rem auto' }}>
            No metrics or log sources found for this tenant. Add a Prometheus, Thanos, or Loki endpoint to begin proxying telemetry through IP Sentinel.
          </p>
          <button className="btn btn-primary" onClick={handleOpenAddModal}>
            <Plus size={18} /> Add Your First Data Source
          </button>
        </div>
      ) : (
        <div className="card" style={{ padding: 0, overflow: 'hidden' }}>
          <div style={{ overflowX: 'auto' }}>
            <table style={{ width: '100%', borderCollapse: 'collapse', textAlign: 'left', fontSize: '0.875rem' }}>
              <thead>
                <tr style={{ backgroundColor: '#F8FAFC', borderBottom: '1px solid var(--border-color)', color: 'var(--text-muted)' }}>
                  <th style={{ padding: '0.85rem 1.25rem' }}>Name & Kind</th>
                  <th style={{ padding: '0.85rem 1.25rem' }}>Endpoint URL</th>
                  <th style={{ padding: '0.85rem 1.25rem' }}>Authentication</th>
                  <th style={{ padding: '0.85rem 1.25rem' }}>Status</th>
                  <th style={{ padding: '0.85rem 1.25rem', textAlign: 'right' }}>Actions</th>
                </tr>
              </thead>
              <tbody>
                {datasources.map((ds) => (
                  <tr key={ds.id} style={{ borderBottom: '1px solid var(--border-subtle)' }}>
                    <td style={{ padding: '1rem 1.25rem' }}>
                      <div style={{ fontWeight: 600, color: 'var(--text-main)', marginBottom: '0.2rem' }}>
                        {ds.name}
                      </div>
                      {getKindBadge(ds.kind)}
                    </td>
                    <td style={{ padding: '1rem 1.25rem', fontFamily: 'var(--font-mono)', fontSize: '0.8rem', color: '#334155' }}>
                      {ds.url}
                    </td>
                    <td style={{ padding: '1rem 1.25rem' }}>
                      <span className="badge badge-gray" style={{ display: 'inline-flex', alignItems: 'center', gap: '0.25rem' }}>
                        <Lock size={12} /> {ds.authType}
                      </span>
                    </td>
                    <td style={{ padding: '1rem 1.25rem' }}>
                      <label className="toggle-switch" style={{ verticalAlign: 'middle' }}>
                        <input
                          type="checkbox"
                          checked={ds.enabled}
                          onChange={() => handleToggleEnable(ds)}
                        />
                        <span className="toggle-slider" />
                      </label>
                      <span style={{ marginLeft: '0.5rem', fontSize: '0.8rem', color: ds.enabled ? 'var(--color-success)' : 'var(--text-muted)' }}>
                        {ds.enabled ? 'Enabled' : 'Disabled'}
                      </span>
                    </td>
                    <td style={{ padding: '1rem 1.25rem', textAlign: 'right' }}>
                      <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '0.5rem' }}>
                        <button
                          className="btn btn-secondary"
                          style={{ padding: '0.35rem 0.6rem' }}
                          onClick={() => handleOpenEditModal(ds)}
                          title="Edit Data Source"
                        >
                          <Edit3 size={15} />
                        </button>
                        <button
                          className="btn btn-secondary"
                          style={{ padding: '0.35rem 0.6rem', color: 'var(--color-danger)' }}
                          onClick={() => handleDelete(ds)}
                          title="Delete Data Source"
                        >
                          <Trash2 size={15} />
                        </button>
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      )}

      {/* Add/Edit Modal */}
      {isModalOpen && (
        <div
          style={{
            position: 'fixed',
            inset: 0,
            backgroundColor: 'rgba(13, 27, 46, 0.5)',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            padding: '1rem',
            zIndex: 200,
          }}
        >
          <div
            className="card"
            style={{
              width: '100%',
              maxWidth: '560px',
              backgroundColor: 'white',
              borderRadius: 'var(--radius-lg)',
              boxShadow: 'var(--shadow-lg)',
            }}
          >
            <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '1.25rem', paddingBottom: '0.75rem', borderBottom: '1px solid var(--border-color)' }}>
              <h3 style={{ fontSize: '1.15rem', fontWeight: 700 }}>
                {editingId ? 'Edit Data Source' : 'Add New Data Source'}
              </h3>
              <button onClick={() => setIsModalOpen(false)} style={{ color: 'var(--text-muted)' }}>
                <XCircle size={20} />
              </button>
            </div>

            <form onSubmit={handleSave}>
              <div className="form-group">
                <label className="form-label">Data Source Name</label>
                <input
                  type="text"
                  required
                  className="form-control"
                  placeholder="e.g. Production Prometheus Fleet"
                  value={name}
                  onChange={(e) => setName(e.target.value)}
                />
              </div>

              <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '1rem' }}>
                <div className="form-group">
                  <label className="form-label">Telemetry Kind</label>
                  <select
                    className="form-control"
                    value={kind}
                    onChange={(e) => setKind(e.target.value as DatasourceKind)}
                  >
                    <option value="prometheus">Prometheus</option>
                    <option value="thanos">Thanos</option>
                    <option value="loki">Loki</option>
                  </select>
                </div>

                <div className="form-group">
                  <label className="form-label">Auth Type</label>
                  <select
                    className="form-control"
                    value={authType}
                    onChange={(e) => setAuthType(e.target.value as AuthType)}
                  >
                    <option value="NONE">None</option>
                    <option value="BASIC">Basic Auth</option>
                    <option value="BEARER">Bearer Token</option>
                  </select>
                </div>
              </div>

              <div className="form-group">
                <label className="form-label">Endpoint Base URL</label>
                <input
                  type="url"
                  required
                  className="form-control"
                  placeholder="http://prometheus.internal:9090 or http://loki.internal:3100"
                  value={url}
                  onChange={(e) => setUrl(e.target.value)}
                />
              </div>

              {authType === 'BASIC' && (
                <div className="form-group">
                  <label className="form-label">Username</label>
                  <input
                    type="text"
                    className="form-control"
                    placeholder="Auth Username"
                    value={authUsername}
                    onChange={(e) => setAuthUsername(e.target.value)}
                  />
                </div>
              )}

              {authType !== 'NONE' && (
                <div className="form-group">
                  <label className="form-label">
                    {authType === 'BASIC' ? 'Password' : 'Bearer Token'} 
                    <span style={{ fontSize: '0.75rem', color: 'var(--text-muted)', marginLeft: '0.4rem' }}>
                      (Stored protected; never echoed back)
                    </span>
                  </label>
                  <input
                    type="password"
                    className="form-control"
                    placeholder={editingId ? 'Leave blank to keep existing secret' : 'Secret / Password / Token'}
                    value={authSecret}
                    onChange={(e) => setAuthSecret(e.target.value)}
                  />
                </div>
              )}

              {/* Test Connection Output Box */}
              {testResult && (
                <div
                  style={{
                    backgroundColor: testResult.reachable ? 'var(--color-success-bg)' : 'var(--color-danger-bg)',
                    color: testResult.reachable ? 'var(--color-success)' : 'var(--color-danger)',
                    border: `1px solid ${testResult.reachable ? '#6EE7B7' : '#FCA5A5'}`,
                    borderRadius: 'var(--radius-sm)',
                    padding: '0.75rem 1rem',
                    marginBottom: '1rem',
                    fontSize: '0.85rem',
                    display: 'flex',
                    alignItems: 'center',
                    justifyContent: 'space-between',
                  }}
                >
                  <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
                    {testResult.reachable ? <CheckCircle2 size={18} /> : <XCircle size={18} />}
                    <span>{testResult.message}</span>
                  </div>
                  {testResult.latencyMs !== undefined && (
                    <span style={{ fontWeight: 700, fontSize: '0.75rem', fontFamily: 'var(--font-mono)' }}>
                      {testResult.latencyMs} ms
                    </span>
                  )}
                </div>
              )}

              <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginTop: '1.5rem', paddingTop: '1rem', borderTop: '1px solid var(--border-color)' }}>
                <button
                  type="button"
                  className="btn btn-secondary"
                  onClick={handleTestConnection}
                  disabled={testing}
                >
                  <Wifi size={16} className={testing ? 'spin' : ''} />
                  {testing ? 'Testing...' : 'Test Connection'}
                </button>

                <div style={{ display: 'flex', gap: '0.75rem' }}>
                  <button
                    type="button"
                    className="btn btn-secondary"
                    onClick={() => setIsModalOpen(false)}
                  >
                    Cancel
                  </button>
                  <button type="submit" className="btn btn-primary">
                    Save Data Source
                  </button>
                </div>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
};
