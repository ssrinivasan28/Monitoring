import React from 'react';
import { useAuth } from '../../context/AuthContext';
import { TenantSwitcher } from './TenantSwitcher';
import { Menu, LogOut, User, Sparkles, ShieldCheck } from 'lucide-react';

interface HeaderProps {
  onToggleMobileSidebar: () => void;
}

export const Header: React.FC<HeaderProps> = ({ onToggleMobileSidebar }) => {
  const { user, entitlementTier, logout } = useAuth();

  return (
    <header
      style={{
        height: '64px',
        backgroundColor: '#0057B8',
        color: 'white',
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'space-between',
        padding: '0 1.25rem',
        boxShadow: 'var(--shadow-md)',
        position: 'sticky',
        top: 0,
        zIndex: 100,
      }}
    >
      {/* Brand & Mobile Hamburger */}
      <div style={{ display: 'flex', alignItems: 'center', gap: '1rem' }}>
        <button
          onClick={onToggleMobileSidebar}
          className="mobile-menu-btn"
          style={{
            color: 'white',
            display: 'none', // Shown via CSS media query
            padding: '0.4rem',
          }}
          aria-label="Toggle menu"
        >
          <Menu size={22} />
        </button>

        <div style={{ display: 'flex', alignItems: 'center', gap: '0.75rem' }}>
          <div
            style={{
              width: '36px',
              height: '36px',
              backgroundColor: '#F5A300',
              borderRadius: 'var(--radius-sm)',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              fontWeight: 800,
              fontSize: '1.2rem',
              color: '#0057B8',
              letterSpacing: '-1px',
            }}
          >
            IP
          </div>
          <div>
            <div style={{ fontWeight: 800, fontSize: '1.1rem', letterSpacing: '0.5px', lineHeight: 1.1 }}>
              ISLAND PACIFIC
            </div>
            <div style={{ fontSize: '0.7rem', color: '#F5A300', fontWeight: 600, letterSpacing: '1px' }}>
              SENTINEL AIOPS
            </div>
          </div>
        </div>
      </div>

      {/* Right Controls: Tenant Switcher, Tier Badge, User Profile */}
      <div style={{ display: 'flex', alignItems: 'center', gap: '1.25rem' }}>
        <TenantSwitcher />

        {/* Tier Badge */}
        <div
          style={{
            display: 'inline-flex',
            alignItems: 'center',
            gap: '0.35rem',
            padding: '0.3rem 0.65rem',
            borderRadius: 'var(--radius-full)',
            backgroundColor: entitlementTier === 'PRO' ? '#F5A300' : 'rgba(255, 255, 255, 0.15)',
            color: entitlementTier === 'PRO' ? '#0F172A' : 'white',
            fontSize: '0.75rem',
            fontWeight: 700,
            letterSpacing: '0.5px',
          }}
        >
          {entitlementTier === 'PRO' ? <Sparkles size={14} /> : <ShieldCheck size={14} />}
          {entitlementTier} TIER
        </div>

        {/* User Info & Logout */}
        {user && (
          <div style={{ display: 'flex', alignItems: 'center', gap: '0.75rem', borderLeft: '1px solid rgba(255, 255, 255, 0.2)', paddingLeft: '1rem' }}>
            <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
              <div
                style={{
                  width: '32px',
                  height: '32px',
                  borderRadius: '50%',
                  backgroundColor: 'rgba(255, 255, 255, 0.2)',
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'center',
                  fontWeight: 600,
                  fontSize: '0.85rem',
                }}
              >
                {user.displayName ? user.displayName.charAt(0).toUpperCase() : <User size={16} />}
              </div>
              <div style={{ display: 'none', flexDirection: 'column', fontSize: '0.8rem' }} className="user-details-text">
                <span style={{ fontWeight: 600 }}>{user.displayName || user.email}</span>
                <span style={{ fontSize: '0.7rem', color: '#CBD5E1' }}>{user.roleKey}</span>
              </div>
            </div>

            <button
              onClick={logout}
              title="Log out"
              style={{
                color: 'white',
                padding: '0.4rem',
                borderRadius: 'var(--radius-sm)',
                transition: 'background-color 0.2s',
              }}
              onMouseOver={(e) => (e.currentTarget.style.backgroundColor = 'rgba(255, 255, 255, 0.15)')}
              onMouseOut={(e) => (e.currentTarget.style.backgroundColor = 'transparent')}
            >
              <LogOut size={18} />
            </button>
          </div>
        )}
      </div>
    </header>
  );
};
