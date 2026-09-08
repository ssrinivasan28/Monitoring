import React, { createContext, useContext, useState, useEffect } from 'react';
import { Tenant } from '../types';
import { useAuth } from './AuthContext';
import { apiRequest } from '../services/apiClient';

interface TenantContextType {
  tenants: Tenant[];
  loading: boolean;
  error: string | null;
  refreshTenants: () => Promise<void>;
}

const TenantContext = createContext<TenantContextType | undefined>(undefined);

export const TenantProvider: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  const { user } = useAuth();
  const [tenants, setTenants] = useState<Tenant[]>([]);
  const [loading, setLoading] = useState<boolean>(false);
  const [error, setError] = useState<string | null>(null);

  const refreshTenants = async () => {
    if (!user) return;
    setLoading(true);
    setError(null);
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
    } catch (err: any) {
      console.warn('Could not load tenant list:', err);
      setError(err.message || 'Failed to load tenant list');
      setTenants([]);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    refreshTenants();
  }, [user]);

  return (
    <TenantContext.Provider value={{ tenants, loading, error, refreshTenants }}>
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
