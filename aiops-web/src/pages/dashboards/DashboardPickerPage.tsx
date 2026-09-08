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
  Filter, 
  CheckCircle, 
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

  return (
    <div style={{ padding: '1.5rem', display: 'flex', flexDirection: 'column', gap: '1.5rem' }}>
      {/* Header */}
      <div style={{ display: 'flex', flexWrap: 'wrap', alignItems: 'center', justifyContent: 'space-between', gap: '1rem' }}>
        <div>
          <h1 style={{ fontSize: '1.5rem', fontWeight: 800, color: 'white', margin: 0, display: 'flex', alignItems: 'center', gap: '0.6rem' }}>
            <LayoutDashboard size={24} color="#0057B8" /> Dashboard Catalog & Engine
          </h1>
          <div style={{ fontSize: '0.85rem', color: '#94A3B8', marginTop: '0.25rem' }}>
            Config-driven dashboard definitions for IBM i, Windows, Network services & custom Grafana imports
          </div>
        </div>

        <div style={{ display: 'flex', alignItems: 'center', gap: '0.75rem' }}>
          <button
            onClick={() => setShowImportModal(true)}
            style={{
              display: 'inline-flex',
              alignItems: 'center',
              gap: '0.4rem',
              padding: '0.55rem 0.95rem',
              backgroundColor: '#1E293B',
              color: '#38BDF8',
              border: '1px solid #0284C7',
              borderRadius: 'var(--radius-sm)',
              cursor: 'pointer',
              fontSize: '0.85rem',
              fontWeight: 600,
            }}
          >
            <Upload size={14} /> Import Grafana JSON
          </button>

          <button
            onClick={() => setShowCreateModal(true)}
            style={{
              display: 'inline-flex',
              alignItems: 'center',
              gap: '0.4rem',
              padding: '0.55rem 0.95rem',
              backgroundColor: '#0057B8',
              color: 'white',
              border: 'none',
              borderRadius: 'var(--radius-sm)',
              cursor: 'pointer',
              fontSize: '0.85rem',
              fontWeight: 600,
            }}
          >
            <Plus size={14} /> New Custom Dashboard
          </button>
        </div>
      </div>

      {/* Filter / Search Bar */}
      <div style={{ display: 'flex', flexWrap: 'wrap', alignItems: 'center', justifyContent: 'space-between', gap: '1rem', backgroundColor: '#1E293B', padding: '0.85rem 1rem', borderRadius: 'var(--radius-md)', border: '1px solid #334155' }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', flex: 1, minWidth: '240px' }}>
          <Search size={16} color="#64748B" />
          <input
            type="text"
            placeholder="Search dashboards by title, category, or ID..."
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
            style={{
              width: '100%',
              backgroundColor: '#0F172A',
              border: '1px solid #334155',
              borderRadius: 'var(--radius-sm)',
              padding: '0.45rem 0.75rem',
              color: 'white',
              fontSize: '0.85rem',
            }}
          />
        </div>

        {/* Category Tabs */}
        <div style={{ display: 'flex', alignItems: 'center', gap: '0.4rem', flexWrap: 'wrap' }}>
          {categories.map((cat) => (
            <button
              key={cat}
              onClick={() => setSelectedCategory(cat)}
              style={{
                padding: '0.35rem 0.75rem',
                borderRadius: 'var(--radius-full)',
                fontSize: '0.75rem',
                fontWeight: 600,
                border: selectedCategory === cat ? '1px solid #0057B8' : '1px solid #334155',
                backgroundColor: selectedCategory === cat ? '#0057B8' : '#0F172A',
                color: selectedCategory === cat ? 'white' : '#94A3B8',
                cursor: 'pointer',
              }}
            >
              {cat}
            </button>
          ))}
        </div>
      </div>

      {/* Catalog Grid */}
      {loading ? (
        <div style={{ padding: '3rem', textAlign: 'center', color: '#94A3B8' }}>
          Loading dashboard catalog...
        </div>
      ) : error ? (
        <div style={{ padding: '1.5rem', backgroundColor: '#FEF2F2', border: '1px solid #FECACA', borderRadius: 'var(--radius-md)', color: '#991B1B' }}>
          {error}
        </div>
      ) : filteredDashboards.length === 0 ? (
        <div style={{ padding: '3rem', textAlign: 'center', color: '#64748B', backgroundColor: '#1E293B', borderRadius: 'var(--radius-md)', border: '1px dashed #334155' }}>
          No dashboards match your filter criteria.
        </div>
      ) : (
        <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fill, minmax(320px, 1fr))', gap: '1.25rem' }}>
          {filteredDashboards.map((dash) => (
            <div
              key={dash.id}
              onClick={() => navigate(`/dashboards/view/${dash.id}`)}
              style={{
                backgroundColor: '#1E293B',
                border: '1px solid #334155',
                borderRadius: 'var(--radius-md)',
                padding: '1.25rem',
                cursor: 'pointer',
                display: 'flex',
                flexDirection: 'column',
                justifyContent: 'space-between',
                gap: '1rem',
                transition: 'all 0.2s ease',
              }}
              onMouseEnter={(e) => (e.currentTarget.style.borderColor = '#0057B8')}
              onMouseLeave={(e) => (e.currentTarget.style.borderColor = '#334155')}
            >
              <div>
                <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '0.5rem' }}>
                  <span
                    style={{
                      fontSize: '0.7rem',
                      fontWeight: 700,
                      padding: '0.15rem 0.5rem',
                      borderRadius: 'var(--radius-full)',
                      backgroundColor: '#0F172A',
                      color: '#38BDF8',
                      border: '1px solid #0284C7',
                    }}
                  >
                    {dash.category || 'General'}
                  </span>
                  {dash.custom ? (
                    <span
                      style={{
                        fontSize: '0.65rem',
                        fontWeight: 700,
                        padding: '0.15rem 0.45rem',
                        borderRadius: 'var(--radius-full)',
                        backgroundColor: '#F5A300',
                        color: '#0F172A',
                      }}
                    >
                      Custom
                    </span>
                  ) : (
                    <span style={{ fontSize: '0.65rem', color: '#64748B' }}>Bundled</span>
                  )}
                </div>

                <h3 style={{ fontSize: '1.1rem', fontWeight: 700, color: 'white', margin: 0, marginBottom: '0.4rem' }}>
                  {dash.title}
                </h3>
                <div style={{ fontSize: '0.75rem', color: '#94A3B8', fontFamily: 'monospace' }}>
                  ID: {dash.id}
                </div>
              </div>

              <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', borderTop: '1px solid #334155', paddingTop: '0.75rem' }}>
                <span style={{ fontSize: '0.8rem', color: '#CBD5E1', display: 'flex', alignItems: 'center', gap: '0.4rem' }}>
                  <Layers size={14} color="#F5A300" /> {(dash.panels || []).length} Panels
                </span>

                <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
                  {dash.custom && (
                    <button
                      onClick={(e) => handleDelete(dash.id, dash.title, e)}
                      title="Delete Custom Dashboard"
                      style={{
                        backgroundColor: 'transparent',
                        border: 'none',
                        color: '#EF4444',
                        cursor: 'pointer',
                        padding: '0.3rem',
                      }}
                    >
                      <Trash2 size={16} />
                    </button>
                  )}
                  <span style={{ fontSize: '0.8rem', color: '#0057B8', fontWeight: 600, display: 'flex', alignItems: 'center', gap: '0.2rem' }}>
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
        <div style={{ position: 'fixed', inset: 0, backgroundColor: 'rgba(15, 23, 42, 0.75)', zIndex: 100, display: 'flex', alignItems: 'center', justifyContent: 'center', padding: '1.5rem' }}>
          <div style={{ backgroundColor: '#1E293B', width: '100%', maxWidth: '650px', borderRadius: 'var(--radius-md)', border: '1px solid #334155', padding: '1.25rem', display: 'flex', flexDirection: 'column', gap: '1rem' }}>
            <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
              <h2 style={{ fontSize: '1.1rem', fontWeight: 700, color: 'white', margin: 0, display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
                <FileJson size={18} color="#38BDF8" /> Import Grafana Dashboard JSON
              </h2>
              <button onClick={() => setShowImportModal(false)} style={{ backgroundColor: 'transparent', border: 'none', color: '#94A3B8', cursor: 'pointer' }}>✕</button>
            </div>
            <p style={{ fontSize: '0.8rem', color: '#94A3B8', margin: 0 }}>
              Paste raw exported Grafana dashboard JSON below. It will automatically be converted into an IP Sentinel config-driven dashboard definition.
            </p>
            <textarea
              rows={12}
              placeholder="Paste Grafana JSON here..."
              value={grafanaJsonText}
              onChange={(e) => setGrafanaJsonText(e.target.value)}
              style={{
                width: '100%',
                backgroundColor: '#0F172A',
                border: '1px solid #334155',
                borderRadius: 'var(--radius-sm)',
                padding: '0.75rem',
                color: '#38BDF8',
                fontFamily: 'monospace',
                fontSize: '0.75rem',
              }}
            />
            <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '0.75rem' }}>
              <button onClick={() => setShowImportModal(false)} style={{ padding: '0.5rem 1rem', backgroundColor: '#334155', color: 'white', border: 'none', borderRadius: 'var(--radius-sm)', cursor: 'pointer' }}>
                Cancel
              </button>
              <button
                onClick={handleImportGrafana}
                disabled={importing || !grafanaJsonText.trim()}
                style={{ padding: '0.5rem 1rem', backgroundColor: '#0057B8', color: 'white', border: 'none', borderRadius: 'var(--radius-sm)', cursor: 'pointer' }}
              >
                {importing ? 'Importing...' : 'Import Dashboard'}
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Modal: New Custom Dashboard */}
      {showCreateModal && (
        <div style={{ position: 'fixed', inset: 0, backgroundColor: 'rgba(15, 23, 42, 0.75)', zIndex: 100, display: 'flex', alignItems: 'center', justifyContent: 'center', padding: '1.5rem' }}>
          <div style={{ backgroundColor: '#1E293B', width: '100%', maxWidth: '650px', borderRadius: 'var(--radius-md)', border: '1px solid #334155', padding: '1.25rem', display: 'flex', flexDirection: 'column', gap: '1rem' }}>
            <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
              <h2 style={{ fontSize: '1.1rem', fontWeight: 700, color: 'white', margin: 0, display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
                <Plus size={18} color="#F5A300" /> Create Custom Dashboard Definition
              </h2>
              <button onClick={() => setShowCreateModal(false)} style={{ backgroundColor: 'transparent', border: 'none', color: '#94A3B8', cursor: 'pointer' }}>✕</button>
            </div>
            <p style={{ fontSize: '0.8rem', color: '#94A3B8', margin: 0 }}>
              Edit the JSON definition for your custom dashboard:
            </p>
            <textarea
              rows={14}
              value={customJsonText}
              onChange={(e) => setCustomJsonText(e.target.value)}
              style={{
                width: '100%',
                backgroundColor: '#0F172A',
                border: '1px solid #334155',
                borderRadius: 'var(--radius-sm)',
                padding: '0.75rem',
                color: '#38BDF8',
                fontFamily: 'monospace',
                fontSize: '0.75rem',
              }}
            />
            <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '0.75rem' }}>
              <button onClick={() => setShowCreateModal(false)} style={{ padding: '0.5rem 1rem', backgroundColor: '#334155', color: 'white', border: 'none', borderRadius: 'var(--radius-sm)', cursor: 'pointer' }}>
                Cancel
              </button>
              <button
                onClick={handleCreateCustom}
                disabled={savingCustom || !customJsonText.trim()}
                style={{ padding: '0.5rem 1rem', backgroundColor: '#0057B8', color: 'white', border: 'none', borderRadius: 'var(--radius-sm)', cursor: 'pointer' }}
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
