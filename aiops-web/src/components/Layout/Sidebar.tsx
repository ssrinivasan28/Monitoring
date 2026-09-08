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
  ChevronRight,
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
    {
      to: '/fleet',
      label: 'Fleet Overview',
      icon: <LayoutDashboard size={18} />,
      gated: false,
      visible: true,
    },
    {
      to: '/dashboards/windows',
      label: 'Windows Monitor',
      icon: <LayoutDashboard size={18} color="#60A5FA" />,
      gated: false,
      visible: true,
    },
    {
      to: '/dashboards/win-service',
      label: 'Win Service Monitor',
      icon: <Activity size={18} color="#34D399" />,
      gated: false,
      visible: true,
    },
    {
      to: '/logs',
      label: 'Log Explorer',
      icon: <Search size={18} color="#FBBF24" />,
      gated: false,
      visible: true,
    },
    {
      to: '/incidents',
      label: 'Incidents',
      icon: <AlertTriangle size={18} />,
      gated: false,
      visible: true,
    },
    {
      to: '/assistant',
      label: 'AI Assistant',
      icon: <Bot size={18} />,
      gated: !isProTier,
      badgeText: 'PRO',
      visible: true,
    },
    {
      to: '/audit-logs',
      label: 'Governance Audit Logs',
      icon: <FileText size={18} />,
      gated: false,
      visible: isStaff,
    },
    {
      to: '/admin/datasources',
      label: 'Admin › Data Sources',
      icon: <Database size={18} />,
      gated: false,
      visible: isAdmin,
    },
    {
      to: '/admin/tenants',
      label: 'Admin › Tenants & Tiers',
      icon: <Users size={18} />,
      gated: false,
      visible: isStaff,
    },
  ];

  return (
    <>
      {/* Mobile Backdrop */}
      {mobileOpen && (
        <div
          onClick={onCloseMobile}
          style={{
            position: 'fixed',
            inset: 0,
            backgroundColor: 'rgba(15, 23, 42, 0.5)',
            zIndex: 90,
          }}
        />
      )}

      <aside
        className={`sidebar ${mobileOpen ? 'open' : ''}`}
        style={{
          width: '260px',
          backgroundColor: '#0F172A',
          color: '#94A3B8',
          display: 'flex',
          flexDirection: 'column',
          justifyContent: 'space-between',
          padding: '1.25rem 0.75rem',
          flexShrink: 0,
          transition: 'transform 0.3s ease',
        }}
      >
        <div style={{ display: 'flex', flexDirection: 'column', gap: '0.5rem' }}>
          <div style={{ padding: '0 0.75rem 0.75rem 0.75rem', fontSize: '0.75rem', fontWeight: 700, color: '#64748B', letterSpacing: '1px' }}>
            MAIN NAVIGATION
          </div>

          <nav style={{ display: 'flex', flexDirection: 'column', gap: '0.25rem' }}>
            {navItems.filter(item => item.visible).map((item) => (
              <NavLink
                key={item.to}
                to={item.to}
                onClick={onCloseMobile}
                className={({ isActive }) => `nav-link ${isActive ? 'active' : ''}`}
                style={({ isActive }) => ({
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'space-between',
                  padding: '0.65rem 0.85rem',
                  borderRadius: 'var(--radius-sm)',
                  fontSize: '0.875rem',
                  fontWeight: 500,
                  color: isActive ? 'white' : '#94A3B8',
                  backgroundColor: isActive ? '#1E293B' : 'transparent',
                  borderLeft: isActive ? '3px solid #0057B8' : '3px solid transparent',
                  transition: 'all 0.2s',
                })}
              >
                <div style={{ display: 'flex', alignItems: 'center', gap: '0.75rem' }}>
                  {item.icon}
                  <span>{item.label}</span>
                </div>

                {item.gated ? (
                  <span
                    style={{
                      display: 'inline-flex',
                      alignItems: 'center',
                      gap: '0.2rem',
                      fontSize: '0.65rem',
                      fontWeight: 700,
                      backgroundColor: '#F5A300',
                      color: '#0F172A',
                      padding: '0.15rem 0.4rem',
                      borderRadius: 'var(--radius-full)',
                    }}
                  >
                    <Lock size={10} /> PRO
                  </span>
                ) : (
                  <ChevronRight size={14} style={{ opacity: 0.4 }} />
                )}
              </NavLink>
            ))}
          </nav>
        </div>

        {/* Footer Security Badge */}
        <div
          style={{
            padding: '0.85rem',
            backgroundColor: '#1E293B',
            borderRadius: 'var(--radius-sm)',
            display: 'flex',
            alignItems: 'center',
            gap: '0.6rem',
            fontSize: '0.75rem',
            color: '#CBD5E1',
          }}
        >
          <ShieldAlert size={18} color="#F5A300" />
          <div>
            <div style={{ fontWeight: 600, color: 'white' }}>Read-Only Mode</div>
            <div style={{ fontSize: '0.7rem', color: '#94A3B8' }}>SOC 2 Immutable Audit</div>
          </div>
        </div>
      </aside>
    </>
  );
};
