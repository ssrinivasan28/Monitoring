import React, { useEffect, useState, useMemo } from 'react';
import { useAuth } from '../context/AuthContext';
import { getFleetOverview, TenantFleet, FleetInputScore } from '../services/fleetService';
import { 
  LayoutDashboard, 
  ShieldCheck, 
  Activity, 
  Cpu, 
  HardDrive, 
  AlertOctagon, 
  AlertTriangle, 
  CheckCircle2, 
  HelpCircle, 
  Search, 
  Filter, 
  ArrowUpDown,
  RefreshCw,
  ExternalLink,
  Layers,
  Database,
  TrendingDown
} from 'lucide-react';

export const FleetPage: React.FC = () => {
  const { activeTenantId, setActiveTenantId, entitlementTier } = useAuth();
  const [fleetData, setFleetData] = useState<TenantFleet[]>([]);
  const [loading, setLoading] = useState<boolean>(true);
  const [error, setError] = useState<string | null>(null);
  const [searchTerm, setSearchTerm] = useState<string>('');
  const [severityFilter, setSeverityFilter] = useState<string>('ALL');
  const [platformFilter, setPlatformFilter] = useState<string>('ALL');
  const [sortBy, setSortBy] = useState<'score' | 'name' | 'severity'>('severity');
  const [sortOrder, setSortOrder] = useState<'asc' | 'desc'>('desc');

  const fetchFleet = async () => {
    setLoading(true);
    setError(null);
    try {
      const data = await getFleetOverview();
      setFleetData(data);
    } catch (err: any) {
      setError(err.message || 'Failed to load fleet overview');
      setFleetData([]);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchFleet();
  }, []);

  const filteredAndSortedFleet = useMemo(() => {
    return fleetData
      .filter((t) => {
        const matchesSearch = 
          t.name.toLowerCase().includes(searchTerm.toLowerCase()) ||
          t.clientInstanceId.toLowerCase().includes(searchTerm.toLowerCase());

        const matchesSeverity =
          severityFilter === 'ALL' || t.band === severityFilter;

        let matchesPlatform = true;
        if (platformFilter === 'IBMi') {
          matchesPlatform = t.inputs.some(i => i.key === 'asp' && !i.stale);
        } else if (platformFilter === 'WINDOWS') {
          matchesPlatform = t.inputs.some(i => i.key === 'disk' && !i.stale);
        }

        return matchesSearch && matchesSeverity && matchesPlatform;
      })
      .sort((a, b) => {
        let comp = 0;
        if (sortBy === 'score') {
          comp = a.score - b.score;
        } else if (sortBy === 'name') {
          comp = a.name.localeCompare(b.name);
        } else if (sortBy === 'severity') {
          const rank = { RED: 4, AMBER: 3, UNKNOWN: 2, GREEN: 1 };
          comp = (rank[a.band] || 0) - (rank[b.band] || 0);
        }
        return sortOrder === 'desc' ? -comp : comp;
      });
  }, [fleetData, searchTerm, severityFilter, platformFilter, sortBy, sortOrder]);

  const summaryCounts = useMemo(() => {
    let red = 0, amber = 0, green = 0, unknown = 0;
    fleetData.forEach((t) => {
      if (t.band === 'RED') red++;
      else if (t.band === 'AMBER') amber++;
      else if (t.band === 'GREEN') green++;
      else unknown++;
    });
    return { total: fleetData.length, red, amber, green, unknown };
  }, [fleetData]);

  // 1.8 alert-noise reduction KPI: average across tenants that have a known ratio for the period.
  const avgNoiseReduction = useMemo(() => {
    const known = fleetData.filter((t) => t.noiseReductionRatio !== null && t.noiseReductionRatio !== undefined);
    if (known.length === 0) return null;
    const sum = known.reduce((acc, t) => acc + (t.noiseReductionRatio as number), 0);
    return Math.round((sum / known.length) * 10) / 10;
  }, [fleetData]);

  const getBandBadge = (band: string) => {
    switch (band) {
      case 'RED':
        return (
          <span className="badge badge-red" style={{ display: 'inline-flex', alignItems: 'center', gap: '0.35rem', fontWeight: 700, padding: '0.3rem 0.6rem', border: '1px solid #EF4444' }}>
            <AlertOctagon size={14} color="#EF4444" /> CRITICAL (RED)
          </span>
        );
      case 'AMBER':
        return (
          <span className="badge badge-amber" style={{ display: 'inline-flex', alignItems: 'center', gap: '0.35rem', fontWeight: 700, padding: '0.3rem 0.6rem', border: '1px solid #F5A300' }}>
            <AlertTriangle size={14} color="#F5A300" /> WARNING (AMBER)
          </span>
        );
      case 'GREEN':
        return (
          <span className="badge badge-green" style={{ display: 'inline-flex', alignItems: 'center', gap: '0.35rem', fontWeight: 700, padding: '0.3rem 0.6rem', border: '1px solid #10B981' }}>
            <CheckCircle2 size={14} color="#10B981" /> HEALTHY (GREEN)
          </span>
        );
      default:
        return (
          <span className="badge" style={{ display: 'inline-flex', alignItems: 'center', gap: '0.35rem', fontWeight: 700, padding: '0.3rem 0.6rem', backgroundColor: '#64748B', color: '#FFF', border: '1px solid #475569' }}>
            <HelpCircle size={14} color="#FFF" /> UNKNOWN / STALE
          </span>
        );
    }
  };

  const getCardBorderStyle = (band: string) => {
    switch (band) {
      case 'RED': return { borderLeft: '6px solid #EF4444', borderTop: '2px solid #EF4444' };
      case 'AMBER': return { borderLeft: '6px solid #F5A300', borderTop: '2px solid #F5A300' };
      case 'GREEN': return { borderLeft: '6px solid #10B981', borderTop: '2px solid #10B981' };
      default: return { borderLeft: '6px dashed #64748B', opacity: 0.9 };
    }
  };

  return (
    <div style={{ maxWidth: '1300px', margin: '0 auto' }}>
      {/* Title Header */}
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '1.5rem', flexWrap: 'wrap', gap: '1rem' }}>
        <div>
          <h1 style={{ fontSize: '1.6rem', fontWeight: 800, color: 'var(--text-main)', display: 'flex', alignItems: 'center', gap: '0.6rem' }}>
            <LayoutDashboard color="#0057B8" size={28} /> Fleet Capacity Headroom Overview
          </h1>
          <p style={{ fontSize: '0.875rem', color: 'var(--text-muted)', marginTop: '0.2rem' }}>
            Deterministic operational telemetry across multi-tenant IBM i & Windows infrastructure.
          </p>
        </div>
        <button
          onClick={fetchFleet}
          className="btn btn-secondary"
          style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', padding: '0.5rem 1rem' }}
          disabled={loading}
        >
          <RefreshCw size={16} className={loading ? 'spin' : ''} /> Refresh Telemetry
        </button>
      </div>

      {/* KPI Overview Strip */}
      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(220px, 1fr))', gap: '1.25rem', marginBottom: '1.75rem' }}>
        <div className="card" style={{ borderLeft: '4px solid #0057B8' }}>
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', color: 'var(--text-muted)', fontSize: '0.85rem' }}>
            <span>Monitored Fleet</span>
            <Layers size={18} color="#0057B8" />
          </div>
          <div style={{ fontSize: '1.8rem', fontWeight: 800, marginTop: '0.3rem', color: 'var(--text-main)' }}>
            {summaryCounts.total} Tenants
          </div>
          <div style={{ fontSize: '0.75rem', color: 'var(--color-primary)', fontWeight: 600, marginTop: '0.25rem' }}>
            {entitlementTier} ENTITLEMENT
          </div>
        </div>

        <div className="card" style={{ borderLeft: '4px solid #EF4444' }}>
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', color: 'var(--text-muted)', fontSize: '0.85rem' }}>
            <span>Critical Pressure</span>
            <AlertOctagon size={18} color="#EF4444" />
          </div>
          <div style={{ fontSize: '1.8rem', fontWeight: 800, marginTop: '0.3rem', color: '#EF4444' }}>
            {summaryCounts.red}
          </div>
          <div style={{ fontSize: '0.75rem', color: '#EF4444', fontWeight: 600 }}>
            Hard Override Triggered
          </div>
        </div>

        <div className="card" style={{ borderLeft: '4px solid #F5A300' }}>
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', color: 'var(--text-muted)', fontSize: '0.85rem' }}>
            <span>Warning Pressure</span>
            <AlertTriangle size={18} color="#F5A300" />
          </div>
          <div style={{ fontSize: '1.8rem', fontWeight: 800, marginTop: '0.3rem', color: '#D97706' }}>
            {summaryCounts.amber}
          </div>
          <div style={{ fontSize: '0.75rem', color: 'var(--text-muted)' }}>
            75% - 89% Headroom
          </div>
        </div>

        <div className="card" style={{ borderLeft: '4px solid #10B981' }}>
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', color: 'var(--text-muted)', fontSize: '0.85rem' }}>
            <span>Optimal Headroom</span>
            <CheckCircle2 size={18} color="#10B981" />
          </div>
          <div style={{ fontSize: '1.8rem', fontWeight: 800, marginTop: '0.3rem', color: '#10B981' }}>
            {summaryCounts.green}
          </div>
          <div style={{ fontSize: '0.75rem', color: 'var(--text-muted)' }}>
            &lt; 75% Capacity Pressure
          </div>
        </div>

        <div className="card" style={{ borderLeft: '4px solid #0057B8' }}>
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', color: 'var(--text-muted)', fontSize: '0.85rem' }}>
            <span>Alert-Noise Reduction</span>
            <TrendingDown size={18} color="#0057B8" />
          </div>
          <div style={{ fontSize: '1.8rem', fontWeight: 800, marginTop: '0.3rem', color: 'var(--text-main)' }}>
            {avgNoiseReduction !== null ? `${avgNoiseReduction}%` : 'N/A'}
          </div>
          <div style={{ fontSize: '0.75rem', color: 'var(--text-muted)' }}>
            Raw Alerts &rarr; Correlated Incidents (30d)
          </div>
        </div>
      </div>

      {/* Search & Filter Toolbar */}
      <div className="card" style={{ marginBottom: '1.75rem', padding: '1.25rem' }}>
        <div style={{ display: 'flex', gap: '1rem', flexWrap: 'wrap', alignItems: 'center' }}>
          {/* Search Box */}
          <div style={{ position: 'relative', flex: 1, minWidth: '240px' }}>
            <Search size={18} style={{ position: 'absolute', left: '0.75rem', top: '50%', transform: 'translateY(-50%)', color: 'var(--text-muted)' }} />
            <input
              type="text"
              placeholder="Search tenant name or client instance ID..."
              value={searchTerm}
              onChange={(e) => setSearchTerm(e.target.value)}
              className="input"
              style={{ paddingLeft: '2.5rem', width: '100%' }}
            />
          </div>

          {/* Severity Filter */}
          <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
            <Filter size={16} color="var(--text-muted)" />
            <select
              value={severityFilter}
              onChange={(e) => setSeverityFilter(e.target.value)}
              className="input"
              style={{ minWidth: '140px' }}
            >
              <option value="ALL">All Bands</option>
              <option value="RED">Critical (Red)</option>
              <option value="AMBER">Warning (Amber)</option>
              <option value="GREEN">Healthy (Green)</option>
              <option value="UNKNOWN">Stale / Unknown</option>
            </select>
          </div>

          {/* Platform Filter */}
          <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
            <select
              value={platformFilter}
              onChange={(e) => setPlatformFilter(e.target.value)}
              className="input"
              style={{ minWidth: '140px' }}
            >
              <option value="ALL">All Platforms</option>
              <option value="IBMi">IBM i (ASP)</option>
              <option value="WINDOWS">Windows (Disks)</option>
            </select>
          </div>

          {/* Sort By */}
          <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
            <ArrowUpDown size={16} color="var(--text-muted)" />
            <select
              value={sortBy}
              onChange={(e) => setSortBy(e.target.value as any)}
              className="input"
              style={{ minWidth: '130px' }}
            >
              <option value="severity">Sort Severity</option>
              <option value="score">Sort Score</option>
              <option value="name">Sort Name</option>
            </select>
            <button
              onClick={() => setSortOrder(prev => prev === 'asc' ? 'desc' : 'asc')}
              className="btn btn-secondary"
              style={{ padding: '0.5rem 0.75rem' }}
              title="Toggle sort order"
            >
              {sortOrder.toUpperCase()}
            </button>
          </div>
        </div>
      </div>

      {/* Fleet Tenant Grid */}
      {loading ? (
        <div className="card" style={{ textAlign: 'center', padding: '3rem' }}>
          <Activity size={36} color="#0057B8" className="spin" style={{ margin: '0 auto 1rem' }} />
          <h3>Computing Capacity Headroom...</h3>
          <p style={{ color: 'var(--text-muted)', fontSize: '0.875rem' }}>Executing deterministic queries via Audited Query Gateway</p>
        </div>
      ) : error ? (
        <div className="card" style={{ textAlign: 'center', padding: '3rem' }}>
          <AlertOctagon size={36} color="#EF4444" style={{ margin: '0 auto 1rem' }} />
          <h3>Unable to Load Fleet Overview</h3>
          <p style={{ color: '#EF4444', fontSize: '0.875rem' }}>{error}</p>
        </div>
      ) : filteredAndSortedFleet.length === 0 ? (
        <div className="card" style={{ textAlign: 'center', padding: '3rem' }}>
          <Database size={36} color="var(--text-muted)" style={{ margin: '0 auto 1rem' }} />
          <h3>No Tenants Match Selection</h3>
          <p style={{ color: 'var(--text-muted)', fontSize: '0.875rem' }}>Try clearing your search term or adjusting filters.</p>
        </div>
      ) : (
        <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(360px, 1fr))', gap: '1.5rem' }}>
          {filteredAndSortedFleet.map((tenant) => {
            const cardStyle = getCardBorderStyle(tenant.band);
            const isSelected = activeTenantId === tenant.tenantId;

            return (
              <div
                key={tenant.tenantId}
                className="card"
                style={{
                  ...cardStyle,
                  position: 'relative',
                  transition: 'transform 0.2s, box-shadow 0.2s',
                  boxShadow: isSelected ? '0 0 0 2px #0057B8' : undefined
                }}
              >
                {/* Header */}
                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', marginBottom: '0.75rem' }}>
                  <div>
                    <h3 style={{ fontSize: '1.15rem', fontWeight: 800, color: 'var(--text-main)' }}>
                      {tenant.name}
                    </h3>
                    <div style={{ fontSize: '0.75rem', fontFamily: 'var(--font-mono)', color: 'var(--text-muted)' }}>
                      {tenant.clientInstanceId}
                    </div>
                  </div>
                  {getBandBadge(tenant.band)}
                </div>

                {/* Score Summary */}
                <div style={{ display: 'flex', alignItems: 'baseline', gap: '0.75rem', margin: '1rem 0', padding: '0.75rem', backgroundColor: 'var(--bg-main)', borderRadius: 'var(--radius-sm)' }}>
                  <div>
                    <div style={{ fontSize: '0.75rem', color: 'var(--text-muted)', fontWeight: 600 }}>HEADROOM PRESSURE</div>
                    <div style={{ fontSize: '1.75rem', fontWeight: 900, color: tenant.band === 'RED' ? '#EF4444' : tenant.band === 'AMBER' ? '#D97706' : tenant.band === 'GREEN' ? '#10B981' : '#64748B' }}>
                      {tenant.band === 'UNKNOWN' ? 'N/A' : `${tenant.score}%`}
                    </div>
                  </div>
                  <div style={{ marginLeft: 'auto', textAlign: 'right' }}>
                    <div style={{ fontSize: '0.75rem', color: 'var(--text-muted)', fontWeight: 600 }}>WORST INPUT</div>
                    <div style={{ fontSize: '0.9rem', fontWeight: 700, color: 'var(--text-main)', textTransform: 'uppercase' }}>
                      {tenant.worstInput || 'None'}
                    </div>
                  </div>
                </div>

                {/* 1.8 Alert-Noise Reduction KPI */}
                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', fontSize: '0.8rem', marginBottom: '0.75rem', color: 'var(--text-muted)' }}>
                  <span style={{ display: 'flex', alignItems: 'center', gap: '0.4rem' }}>
                    <TrendingDown size={14} /> Alert-Noise Reduction (30d)
                  </span>
                  <span style={{ fontWeight: 700, color: 'var(--text-main)' }}>
                    {tenant.noiseReductionRatio !== null && tenant.noiseReductionRatio !== undefined ? `${tenant.noiseReductionRatio}%` : 'N/A'}
                  </span>
                </div>

                {/* Input Capacity Breakdown */}
                <div style={{ fontSize: '0.8rem', display: 'flex', flexDirection: 'column', gap: '0.5rem', marginBottom: '1.25rem' }}>
                  {tenant.inputs.map((input) => (
                    <div key={input.key} style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                      <span style={{ color: 'var(--text-muted)', display: 'flex', alignItems: 'center', gap: '0.4rem' }}>
                        {input.key === 'asp' ? <HardDrive size={14} /> : input.key === 'disk' ? <Database size={14} /> : <Cpu size={14} />}
                        {input.name}
                      </span>
                      {input.stale ? (
                        <span style={{ fontSize: '0.75rem', color: '#64748B', fontWeight: 600, backgroundColor: '#E2E8F0', padding: '0.1rem 0.4rem', borderRadius: '4px' }}>
                          UNKNOWN / STALE
                        </span>
                      ) : (
                        <span style={{
                          fontWeight: 700,
                          color: input.band === 'RED' ? '#EF4444' : input.band === 'AMBER' ? '#D97706' : '#10B981'
                        }}>
                          {input.value}%
                        </span>
                      )}
                    </div>
                  ))}
                </div>

                {/* Card Action */}
                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', paddingTop: '0.75rem', borderTop: '1px solid var(--border-color)' }}>
                  <button
                    onClick={() => setActiveTenantId(tenant.tenantId)}
                    className="btn btn-secondary"
                    style={{ fontSize: '0.8rem', padding: '0.4rem 0.8rem', display: 'flex', alignItems: 'center', gap: '0.4rem' }}
                  >
                    <ShieldCheck size={14} color="#0057B8" /> Switch Context
                  </button>

                  <a
                    href={`/datasources?tenantId=${tenant.tenantId}`}
                    style={{ fontSize: '0.8rem', color: '#0057B8', fontWeight: 600, textDecoration: 'none', display: 'flex', alignItems: 'center', gap: '0.25rem' }}
                  >
                    View Telemetry <ExternalLink size={14} />
                  </a>
                </div>
              </div>
            );
          })}
        </div>
      )}
    </div>
  );
};
