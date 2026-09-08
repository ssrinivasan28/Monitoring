import React, { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import {
  listDashboards,
  deleteCustomDashboard,
  importGrafanaDashboard,
  saveCustomDashboard,
  DashboardDefinition
} from '../../services/dashboardService';
import {
  LayoutDashboard,
  Plus,
  Upload,
  Trash2,
  Eye,
  Search,
  AlertCircle,
  FileJson,
  Layers
} from 'lucide-react';

export const DashboardPickerPage: React.FC = () => {
  const navigate = useNavigate();

  const [dashboards, setDashboards] = useState<DashboardDefinition[]>([]);
  const [loading, setLoading] = useState<boolean>(true);
  const [error, setError] = useState<string | null>(null);

  const [searchQuery, setSearchQuery] = useState<string>('');
  const [selectedCategory, setSelectedCategory] = useState<string>('All');

  // Modals
  const [showImportModal, setShowImportModal] = useState<boolean>(false);
  const [grafanaJsonText, setGrafanaJsonText] = useState<string>('');
  const [importing, setImporting] = useState<boolean>(false);

  const [showCreateModal, setShowCreateModal] = useState<boolean>(false);
  const [customJsonText, setCustomJsonText] = useState<string>(
    JSON.stringify(
      {
        id: "custom-my-dashboard",
        title: "My Custom Dashboard",
        category: "Custom",
        variables: ["tenant", "timeRange"],
        panels: [
          {
            id: "panel-1",
            title: "Custom Metric Stat",
            type: "stat",
            query: "avg(windows_cpu_usage_percent{tenant=\"$tenant\"})",
            unit: "percent",
            gridPos: { x: 0, y: 0, w: 12, h: 4 }
          }
        ]
      },
      null,
      2
    )
  );
  const [savingCustom, setSavingCustom] = useState<boolean>(false);

  const loadCatalog = async () => {
    setLoading(true);
    setError(null);
    try {
      const list = await listDashboards();
      setDashboards(list);
    } catch (err: any) {
      setError(err?.message || 'Failed to load dashboards catalog');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadCatalog();
  }, []);

  const handleDelete = async (id: string, title: string, e: React.MouseEvent) => {
    e.stopPropagation();
    if (!window.confirm(`Are you sure you want to delete custom dashboard "${title}"?`)) {
      return;
    }
    try {
      await deleteCustomDashboard(id);
      loadCatalog();
    } catch (err: any) {
      alert(err?.message || 'Failed to delete dashboard');
    }
  };

  const handleImportGrafana = async () => {
    if (!grafanaJsonText.trim()) return;
    setImporting(true);
    try {
      const imported = await importGrafanaDashboard(grafanaJsonText);
      setShowImportModal(false);
      setGrafanaJsonText('');
      await loadCatalog();
      navigate(`/dashboards/view/${imported.id}`);
    } catch (err: any) {
      alert(err?.message || 'Failed to import Grafana JSON');
    } finally {
      setImporting(false);
    }
  };

  const handleCreateCustom = async () => {
    if (!customJsonText.trim()) return;
    setSavingCustom(true);
    try {
      const parsed = JSON.parse(customJsonText);
      const saved = await saveCustomDashboard(parsed);
      setShowCreateModal(false);
      await loadCatalog();
      navigate(`/dashboards/view/${saved.id}`);
    } catch (err: any) {
      alert('Invalid JSON definition or save error: ' + err?.message);
    } finally {
      setSavingCustom(false);
    }
  };

  // Categories list
  const categories = ['All', ...Array.from(new Set(dashboards.map(d => d.category || 'General')))];

  // Filtered list
  const filteredDashboards = dashboards.filter(d => {
    const matchesSearch = d.title.toLowerCase().includes(searchQuery.toLowerCase()) ||
                          d.category.toLowerCase().includes(searchQuery.toLowerCase()) ||
                          d.id.toLowerCase().includes(searchQuery.toLowerCase());
    const matchesCategory = selectedCategory === 'All' || d.category === selectedCategory;
    return matchesSearch && matchesCategory;
  });

  const jsonEditorStyle: React.CSSProperties = {
    width: '100%',
    backgroundColor: '#0D1B2E',
    border: '1px solid var(--border-color)',
    borderRadius: 'var(--radius-sm)',
    padding: '0.75rem',
    color: '#7dd3fc',
    fontFamily: 'var(--font-mono)',
    fontSize: '0.75rem',
  };

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: '1.5rem' }}>
      {/* Header */}
      <div style={{ display: 'flex', flexWrap: 'wrap', alignItems: 'center', justifyContent: 'space-between', gap: '1rem' }}>
        <div>
          <h1 style={{ fontSize: '1.4rem', fontWeight: 700, color: 'var(--text-main)', margin: 0, display: 'flex', alignItems: 'center', gap: '0.6rem' }}>
            <LayoutDashboard size={22} strokeWidth={1.75} color="var(--brand-primary)" /> Dashboard Catalog & Engine
          </h1>
          <div style={{ fontSize: '0.85rem', color: 'var(--text-muted)', marginTop: '0.25rem' }}>
            Config-driven dashboard definitions for IBM i, Windows, Network services & custom Grafana imports
          </div>
        </div>

        <div style={{ display: 'flex', alignItems: 'center', gap: '0.75rem' }}>
          <button onClick={() => setShowImportModal(true)} className="btn btn-secondary">
            <Upload size={14} /> Import Grafana JSON
          </button>

          <button onClick={() => setShowCreateModal(true)} className="btn btn-primary">
            <Plus size={14} /> New Custom Dashboard
          </button>
        </div>
      </div>

      {/* Filter / Search Bar */}
      <div className="card" style={{ display: 'flex', flexWrap: 'wrap', alignItems: 'center', justifyContent: 'space-between', gap: '1rem', padding: '0.85rem 1rem' }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', flex: 1, minWidth: '240px' }}>
          <Search size={16} strokeWidth={1.75} color="var(--text-muted)" />
          <input
            type="text"
            placeholder="Search dashboards by title, category, or ID..."
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
            className="form-control"
            style={{ width: '100%' }}
          />
        </div>

        {/* Category Tabs */}
        <div style={{ display: 'flex', alignItems: 'center', gap: '0.4rem', flexWrap: 'wrap' }}>
          {categories.map((cat) => (
            <button
              key={cat}
              onClick={() => setSelectedCategory(cat)}
              className="pill"
              style={{
                border: selectedCategory === cat ? '1px solid var(--brand-primary)' : '1px solid var(--border-color)',
                backgroundColor: selectedCategory === cat ? 'var(--brand-primary)' : 'white',
                color: selectedCategory === cat ? 'white' : 'var(--text-muted)',
                cursor: 'pointer',
                fontSize: '0.75rem',
                padding: '0.35rem 0.75rem',
              }}
            >
              {cat}
            </button>
          ))}
        </div>
      </div>

      {/* Catalog Grid */}
      {loading ? (
        <div style={{ padding: '3rem', textAlign: 'center', color: 'var(--text-muted)' }}>
          Loading dashboard catalog...
        </div>
      ) : error ? (
        <div style={{ padding: '1.5rem', backgroundColor: 'var(--color-danger-bg)', border: '1px solid #FECACA', borderRadius: 'var(--radius-md)', color: '#991B1B', display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
          <AlertCircle size={16} /> {error}
        </div>
      ) : filteredDashboards.length === 0 ? (
        <div className="card-flat" style={{ padding: '3rem', textAlign: 'center', color: 'var(--text-muted)', borderStyle: 'dashed' }}>
          No dashboards match your filter criteria.
        </div>
      ) : (
        <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fill, minmax(320px, 1fr))', gap: '1.25rem' }}>
          {filteredDashboards.map((dash) => (
            <div
              key={dash.id}
              onClick={() => navigate(`/dashboards/view/${dash.id}`)}
              className="card"
              style={{
                cursor: 'pointer',
                display: 'flex',
                flexDirection: 'column',
                justifyContent: 'space-between',
                gap: '1rem',
              }}
            >
              <div>
                <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '0.5rem' }}>
                  <span className="badge badge-primary">
                    {dash.category || 'General'}
                  </span>
                  {dash.custom ? (
                    <span className="badge badge-accent">
                      Custom
                    </span>
                  ) : (
                    <span style={{ fontSize: '0.65rem', color: 'var(--color-text-dim)' }}>Bundled</span>
                  )}
                </div>

                <h3 style={{ fontSize: '1.1rem', fontWeight: 600, color: 'var(--text-main)', margin: 0, marginBottom: '0.4rem' }}>
                  {dash.title}
                </h3>
                <div style={{ fontSize: '0.75rem', color: 'var(--color-text-dim)', fontFamily: 'var(--font-mono)' }}>
                  ID: {dash.id}
                </div>
              </div>

              <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', borderTop: '1px solid var(--border-subtle)', paddingTop: '0.75rem' }}>
                <span style={{ fontSize: '0.8rem', color: 'var(--text-muted)', display: 'flex', alignItems: 'center', gap: '0.4rem' }}>
                  <Layers size={14} strokeWidth={1.75} color="var(--brand-accent)" /> {(dash.panels || []).length} Panels
                </span>

                <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
                  {dash.custom && (
                    <button
                      onClick={(e) => handleDelete(dash.id, dash.title, e)}
                      title="Delete Custom Dashboard"
                      style={{ color: '#ef4444', padding: '0.3rem' }}
                    >
                      <Trash2 size={16} />
                    </button>
                  )}
                  <span style={{ fontSize: '0.8rem', color: 'var(--brand-primary)', fontWeight: 600, display: 'flex', alignItems: 'center', gap: '0.2rem' }}>
                    Open <Eye size={14} />
                  </span>
                </div>
              </div>
            </div>
          ))}
        </div>
      )}

      {/* Modal: Import Grafana JSON */}
      {showImportModal && (
        <div style={{ position: 'fixed', inset: 0, backgroundColor: 'rgba(13, 27, 46, 0.5)', zIndex: 100, display: 'flex', alignItems: 'center', justifyContent: 'center', padding: '1.5rem' }}>
          <div className="card" style={{ width: '100%', maxWidth: '650px', display: 'flex', flexDirection: 'column', gap: '1rem' }}>
            <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
              <h2 style={{ fontSize: '1.1rem', fontWeight: 600, color: 'var(--text-main)', margin: 0, display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
                <FileJson size={18} strokeWidth={1.75} color="var(--brand-primary)" /> Import Grafana Dashboard JSON
              </h2>
              <button onClick={() => setShowImportModal(false)} style={{ color: 'var(--text-muted)' }}>✕</button>
            </div>
            <p style={{ fontSize: '0.8rem', color: 'var(--text-muted)', margin: 0 }}>
              Paste raw exported Grafana dashboard JSON below. It will automatically be converted into an IP Sentinel config-driven dashboard definition.
            </p>
            <textarea
              rows={12}
              placeholder="Paste Grafana JSON here..."
              value={grafanaJsonText}
              onChange={(e) => setGrafanaJsonText(e.target.value)}
              style={jsonEditorStyle}
            />
            <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '0.75rem' }}>
              <button onClick={() => setShowImportModal(false)} className="btn btn-secondary">
                Cancel
              </button>
              <button
                onClick={handleImportGrafana}
                disabled={importing || !grafanaJsonText.trim()}
                className="btn btn-primary"
              >
                {importing ? 'Importing...' : 'Import Dashboard'}
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Modal: New Custom Dashboard */}
      {showCreateModal && (
        <div style={{ position: 'fixed', inset: 0, backgroundColor: 'rgba(13, 27, 46, 0.5)', zIndex: 100, display: 'flex', alignItems: 'center', justifyContent: 'center', padding: '1.5rem' }}>
          <div className="card" style={{ width: '100%', maxWidth: '650px', display: 'flex', flexDirection: 'column', gap: '1rem' }}>
            <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
              <h2 style={{ fontSize: '1.1rem', fontWeight: 600, color: 'var(--text-main)', margin: 0, display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
                <Plus size={18} strokeWidth={1.75} color="var(--brand-accent)" /> Create Custom Dashboard Definition
              </h2>
              <button onClick={() => setShowCreateModal(false)} style={{ color: 'var(--text-muted)' }}>✕</button>
            </div>
            <p style={{ fontSize: '0.8rem', color: 'var(--text-muted)', margin: 0 }}>
              Edit the JSON definition for your custom dashboard:
            </p>
            <textarea
              rows={14}
              value={customJsonText}
              onChange={(e) => setCustomJsonText(e.target.value)}
              style={jsonEditorStyle}
            />
            <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '0.75rem' }}>
              <button onClick={() => setShowCreateModal(false)} className="btn btn-secondary">
                Cancel
              </button>
              <button
                onClick={handleCreateCustom}
                disabled={savingCustom || !customJsonText.trim()}
                className="btn btn-primary"
              >
                {savingCustom ? 'Saving...' : 'Save & Open'}
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};
