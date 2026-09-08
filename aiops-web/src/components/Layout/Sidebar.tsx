import React from 'react';
import { NavLink } from 'react-router-dom';
import { useAuth } from '../../context/AuthContext';
import {
  LayoutDashboard,
  Activity,
  AlertTriangle,
  Bot,
  FileText,
  Database,
  Users,
  Lock,
  ShieldAlert,
  Search
} from 'lucide-react';

interface SidebarProps {
  mobileOpen: boolean;
  onCloseMobile: () => void;
}

export const Sidebar: React.FC<SidebarProps> = ({ mobileOpen, onCloseMobile }) => {
  const { user, entitlementTier } = useAuth();

  const isStaff = user?.staff || ['SUPER_ADMIN', 'STAFF_ADMIN', 'STAFF'].includes(user?.roleKey || '');
  const isAdmin = isStaff || ['TENANT_ADMIN'].includes(user?.roleKey || '');
  const isProTier = entitlementTier === 'PRO';

  const navItems = [
    { to: '/fleet', label: 'Fleet Overview', icon: <LayoutDashboard size={15} strokeWidth={1.75} />, gated: false, visible: true },
    { to: '/dashboards', label: 'Dashboards Catalog', icon: <LayoutDashboard size={15} strokeWidth={1.75} />, gated: false, visible: true },
    { to: '/dashboards/windows', label: 'Windows Monitor', icon: <LayoutDashboard size={15} strokeWidth={1.75} />, gated: false, visible: true },
    { to: '/dashboards/win-service', label: 'Win Service Monitor', icon: <Activity size={15} strokeWidth={1.75} />, gated: false, visible: true },
    { to: '/logs', label: 'Log Explorer', icon: <Search size={15} strokeWidth={1.75} />, gated: false, visible: true },
    { to: '/incidents', label: 'Incidents', icon: <AlertTriangle size={15} strokeWidth={1.75} />, gated: false, visible: true },
    { to: '/assistant', label: 'AI Assistant', icon: <Bot size={15} strokeWidth={1.75} />, gated: !isProTier, badgeText: 'PRO', visible: true },
    { to: '/audit-logs', label: 'Governance Audit Logs', icon: <FileText size={15} strokeWidth={1.75} />, gated: false, visible: isStaff },
    { to: '/admin/datasources', label: 'Admin › Data Sources', icon: <Database size={15} strokeWidth={1.75} />, gated: false, visible: isAdmin },
    { to: '/admin/tenants', label: 'Admin › Tenants & Tiers', icon: <Users size={15} strokeWidth={1.75} />, gated: false, visible: isStaff },
  ];

  return (
    <>
      {mobileOpen && (
        <div
          onClick={onCloseMobile}
          style={{ position: 'fixed', inset: 0, backgroundColor: 'rgba(13, 27, 46, 0.4)', zIndex: 39 }}
        />
      )}

      <aside className={`app-sidebar ${mobileOpen ? 'open' : ''}`}>
        <div
          style={{
            height: 64,
            display: 'flex',
            alignItems: 'center',
            padding: '0 16px',
            borderBottom: '1px solid var(--border-subtle)',
            flexShrink: 0,
          }}
        >
          <img src="/IPLogo.jpg" alt="Island Pacific" style={{ maxWidth: '100%', height: 'auto', objectFit: 'contain' }} />
        </div>

        <nav className="app-sidebar-nav">
          <div className="app-sidebar-section-title">Main Navigation</div>

          {navItems.filter((item) => item.visible).map((item) => (
            <NavLink
              key={item.to}
              to={item.to}
              onClick={onCloseMobile}
              className={({ isActive }) => `nav-link ${isActive ? 'active' : ''}`}
            >
              <div style={{ display: 'flex', alignItems: 'center', gap: '10px' }}>
                {item.icon}
                <span>{item.label}</span>
              </div>

              {item.gated && (
                <span className="pill" style={{ background: 'var(--brand-accent)', color: '#1E293B', fontSize: '9px', padding: '0.15rem 0.4rem' }}>
                  <Lock size={10} /> PRO
                </span>
              )}
            </NavLink>
          ))}
        </nav>

        <div className="app-sidebar-footer">
          <div style={{ display: 'flex', alignItems: 'center', gap: '0.6rem' }}>
            <ShieldAlert size={18} strokeWidth={1.75} color="var(--brand-primary)" />
            <div>
              <div style={{ fontWeight: 600, fontSize: 12, color: 'var(--text-main)' }}>Read-Only Mode</div>
              <div style={{ fontSize: 11, color: 'var(--color-text-dim)' }}>SOC 2 Immutable Audit</div>
            </div>
          </div>
        </div>
      </aside>
    </>
  );
};
