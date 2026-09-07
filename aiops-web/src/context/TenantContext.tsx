import React, { createContext, useContext, useState, useEffect } from 'react';
import { Tenant } from '../types';
import { useAuth } from './AuthContext';
import { apiRequest } from '../services/apiClient';

interface TenantContextType {
  tenants: Tenant[];
  loading: boolean;
  refreshTenants: () => Promise<void>;
}

const TenantContext = createContext<TenantContextType | undefined>(undefined);

export const TenantProvider: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  const { user } = useAuth();
  const [tenants, setTenants] = useState<Tenant[]>([]);
  const [loading, setLoading] = useState<boolean>(false);

  const refreshTenants = async () => {
    if (!user) return;
    setLoading(true);
    try {
      if (user.staff) {
        // Staff can list all tenants
        const data = await apiRequest<Tenant[]>('/api/v1/admin/tenants');
        setTenants(data);
      } else if (user.tenantId) {
        // Customer pinned to single tenant
        setTenants([
          {
            id: user.tenantId,
            name: `Tenant (${user.tenantId.substring(0, 8)})`,
            tier: 'BASIC',
            active: true,
          },
        ]);
      }
    } catch (err) {
      console.warn('Could not load tenant list:', err);
      // Fallback default list for dev/demo display
      if (user.tenantId) {
        setTenants([
          { id: user.tenantId, name: `Current Tenant (${user.tenantId.substring(0, 8)})`, tier: 'BASIC', active: true }
        ]);
      } else {
        setTenants([
          { id: '11111111-1111-1111-1111-111111111111', name: 'Acme Retail Corp', tier: 'PRO', active: true },
          { id: '22222222-2222-2222-2222-222222222222', name: 'Global Logistics Inc', tier: 'BASIC', active: true },
        ]);
      }
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    refreshTenants();
  }, [user]);

  return (
    <TenantContext.Provider value={{ tenants, loading, refreshTenants }}>
      {children}
    </TenantContext.Provider>
  );
};

export const useTenants = () => {
  const context = useContext(TenantContext);
  if (!context) {
    throw new Error('useTenants must be used within a TenantProvider');
  }
  return context;
};
