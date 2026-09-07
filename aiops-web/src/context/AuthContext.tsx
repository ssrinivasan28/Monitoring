import React, { createContext, useContext, useState, useEffect } from 'react';
import { UserPrincipal, EntitlementTier } from '../types';
import { authService } from '../services/authService';
import { clearStoredAuth, getStoredToken, getStoredTenantId, setStoredTenantId } from '../services/apiClient';

interface AuthContextType {
  user: UserPrincipal | null;
  loading: boolean;
  activeTenantId: string | null;
  entitlementTier: EntitlementTier;
  setActiveTenantId: (tenantId: string) => void;
  setUser: (user: UserPrincipal | null) => void;
  setEntitlementTier: (tier: EntitlementTier) => void;
  logout: () => void;
  refreshUser: () => Promise<UserPrincipal | null>;
}

const AuthContext = createContext<AuthContextType | undefined>(undefined);

export const AuthProvider: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  const [user, setUser] = useState<UserPrincipal | null>(null);
  const [loading, setLoading] = useState<boolean>(true);
  const [activeTenantId, setActiveTenantIdState] = useState<string | null>(getStoredTenantId());
  const [entitlementTier, setEntitlementTier] = useState<EntitlementTier>('BASIC');

  const setActiveTenantId = (tenantId: string) => {
    setActiveTenantIdState(tenantId);
    setStoredTenantId(tenantId);
  };

  const logout = () => {
    clearStoredAuth();
    setUser(null);
    setActiveTenantIdState(null);
    setEntitlementTier('BASIC');
  };

  const refreshUser = async (): Promise<UserPrincipal | null> => {
    try {
      const token = getStoredToken();
      if (!token) {
        setLoading(false);
        return null;
      }
      const currentUser = await authService.getCurrentUser();
      setUser(currentUser);

      // If user is a customer, pin activeTenantId to user.tenantId
      if (!currentUser.staff && currentUser.tenantId) {
        setActiveTenantId(currentUser.tenantId);
      } else if (!activeTenantId && currentUser.tenantId) {
        setActiveTenantId(currentUser.tenantId);
      }

      // Check entitlement tier if tenantId is available
      const targetTenantId = currentUser.tenantId || activeTenantId;
      if (targetTenantId) {
        try {
          const ent = await authService.getEntitlement(targetTenantId);
          if (ent && ent.tier) {
            setEntitlementTier(ent.tier);
          }
        } catch (_) {
          // Default to BASIC if entitlement lookup fails or unprivileged
        }
      }

      return currentUser;
    } catch (err) {
      console.error('Failed to load active session:', err);
      logout();
      return null;
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    refreshUser();
  }, []);

  return (
    <AuthContext.Provider
      value={{
        user,
        loading,
        activeTenantId,
        entitlementTier,
        setActiveTenantId,
        setUser,
        setEntitlementTier,
        logout,
        refreshUser,
      }}
    >
      {children}
    </AuthContext.Provider>
  );
};

export const useAuth = () => {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error('useAuth must be used within an AuthProvider');
  }
  return context;
};
