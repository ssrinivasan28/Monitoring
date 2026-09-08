import React from 'react';
import { useLocation } from 'react-router-dom';
import { useAuth } from '../../context/AuthContext';
import { TenantSwitcher } from './TenantSwitcher';
import { Menu, LogOut, User, Sparkles, ShieldCheck } from 'lucide-react';

interface HeaderProps {
  onToggleMobileSidebar: () => void;
}

const humanize = (segment: string) =>
  segment.replace(/-/g, ' ').replace(/\b\w/g, (c) => c.toUpperCase());

export const Header: React.FC<HeaderProps> = ({ onToggleMobileSidebar }) => {
  const { user, entitlementTier, logout } = useAuth();
  const location = useLocation();
  const lastSegment = location.pathname.split('/').filter(Boolean).pop() || 'fleet';

  return (
    <header className="app-topbar">
      <div style={{ display: 'flex', alignItems: 'center', gap: '1rem' }}>
        <button
          onClick={onToggleMobileSidebar}
          className="mobile-menu-btn"
          style={{ color: 'var(--text-muted)', display: 'none', padding: '0.4rem' }}
          aria-label="Toggle menu"
        >
          <Menu size={20} />
        </button>

        <div style={{ fontSize: 18, fontWeight: 600, letterSpacing: '-0.02em', color: 'var(--text-main)' }}>
          {humanize(lastSegment)}
        </div>
      </div>

      <div style={{ display: 'flex', alignItems: 'center', gap: '1rem' }}>
        <TenantSwitcher />

        <div
          className="pill"
          style={{
            background: entitlementTier === 'PRO' ? 'var(--brand-accent)' : 'var(--brand-primary-light)',
            color: entitlementTier === 'PRO' ? '#1E293B' : 'var(--brand-primary)',
          }}
        >
          {entitlementTier === 'PRO' ? <Sparkles size={12} /> : <ShieldCheck size={12} />}
          {entitlementTier} TIER
        </div>

        {user && (
          <div style={{ display: 'flex', alignItems: 'center', gap: '0.75rem', borderLeft: '1px solid var(--border-subtle)', paddingLeft: '1rem' }}>
            <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
              <div
                style={{
                  width: 32,
                  height: 32,
                  borderRadius: '50%',
                  backgroundColor: 'var(--brand-primary-light)',
                  color: 'var(--brand-primary)',
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'center',
                  fontWeight: 600,
                  fontSize: 13,
                }}
              >
                {user.displayName ? user.displayName.charAt(0).toUpperCase() : <User size={16} />}
              </div>
              <div style={{ display: 'none', flexDirection: 'column', fontSize: 12 }} className="user-details-text">
                <span style={{ fontWeight: 600, color: 'var(--text-main)' }}>{user.displayName || user.email}</span>
                <span style={{ fontSize: 11, color: 'var(--color-text-dim)' }}>{user.roleKey}</span>
              </div>
            </div>

            <button
              onClick={logout}
              title="Log out"
              style={{ color: '#ef4444', padding: '0.4rem', borderRadius: 'var(--radius-sm)', transition: 'background-color 150ms' }}
              onMouseOver={(e) => (e.currentTarget.style.backgroundColor = 'rgba(239, 68, 68, 0.06)')}
              onMouseOut={(e) => (e.currentTarget.style.backgroundColor = 'transparent')}
            >
              <LogOut size={17} strokeWidth={1.75} />
            </button>
          </div>
        )}
      </div>
    </header>
  );
};
