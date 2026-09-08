import React, { useState } from 'react';
import { Header } from './Header';
import { Sidebar } from './Sidebar';

export const AppLayout: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  const [mobileOpen, setMobileOpen] = useState<boolean>(false);

  return (
    <div style={{ display: 'flex', minHeight: '100vh', background: 'var(--bg-main)' }}>
      <Sidebar mobileOpen={mobileOpen} onCloseMobile={() => setMobileOpen(false)} />

      <div className="app-main">
        <Header onToggleMobileSidebar={() => setMobileOpen(!mobileOpen)} />
        <main className="app-main-body">{children}</main>
      </div>
    </div>
  );
};
