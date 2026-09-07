import React, { useState } from 'react';
import { Header } from './Header';
import { Sidebar } from './Sidebar';

export const AppLayout: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  const [mobileOpen, setMobileOpen] = useState<boolean>(false);

  return (
    <div style={{ display: 'flex', flexDirection: 'column', minHeight: '100vh' }}>
      <Header onToggleMobileSidebar={() => setMobileOpen(!mobileOpen)} />
      
      <div style={{ display: 'flex', flex: 1, position: 'relative' }}>
        <Sidebar mobileOpen={mobileOpen} onCloseMobile={() => setMobileOpen(false)} />
        
        <main
          style={{
            flex: 1,
            padding: '1.75rem',
            backgroundColor: '#F8FAFC',
            overflowY: 'auto',
            minWidth: 0,
          }}
        >
          {children}
        </main>
      </div>
    </div>
  );
};
