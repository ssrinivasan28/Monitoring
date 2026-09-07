import { apiRequest, setStoredToken, setStoredRefreshToken } from './apiClient';
import { UserPrincipal, EntitlementTier } from '../types';

export const authService = {
  async staffAzureAdLogin(token: string): Promise<{ accessToken: string }> {
    const data = await apiRequest<{ accessToken: string }>('/api/v1/auth/staff/azure-ad', {
      method: 'POST',
      body: JSON.stringify({ token }),
    });
    if (data.accessToken) {
      setStoredToken(data.accessToken);
    }
    return data;
  },

  async loginPassword(email: string, password: string): Promise<{ mfaPendingToken: string; mfaRequired: boolean }> {
    return apiRequest('/api/v1/auth/login', {
      method: 'POST',
      body: JSON.stringify({ email, password }),
    });
  },

  async verifyMfa(mfaPendingToken: string, code: string): Promise<{ accessToken: string; refreshToken?: string }> {
    const data = await apiRequest<{ accessToken: string; refreshToken?: string }>('/api/v1/auth/mfa/verify', {
      method: 'POST',
      body: JSON.stringify({ mfaPendingToken, code }),
    });
    if (data.accessToken) {
      setStoredToken(data.accessToken);
    }
    if (data.refreshToken) {
      setStoredRefreshToken(data.refreshToken);
    }
    return data;
  },

  async activateUser(inviteToken: string, password: string, totpCode: string, totpSecret: string): Promise<{ message: string }> {
    return apiRequest('/api/v1/auth/activate', {
      method: 'POST',
      body: JSON.stringify({ inviteToken, password, totpCode, totpSecret }),
    });
  },

  async requestPasswordReset(email: string): Promise<{ message: string }> {
    return apiRequest('/api/v1/auth/reset-password/request', {
      method: 'POST',
      body: JSON.stringify({ email }),
    });
  },

  async confirmPasswordReset(resetToken: string, newPassword: string): Promise<{ message: string }> {
    return apiRequest('/api/v1/auth/reset-password/confirm', {
      method: 'POST',
      body: JSON.stringify({ resetToken, newPassword }),
    });
  },

  async federatedLogin(tenantId: string, code: string, redirectUri: string): Promise<{ accessToken: string }> {
    const data = await apiRequest<{ accessToken: string }>('/api/v1/auth/federated/login', {
      method: 'POST',
      body: JSON.stringify({ tenantId, code, redirectUri }),
    });
    if (data.accessToken) {
      setStoredToken(data.accessToken);
    }
    return data;
  },

  async getCurrentUser(): Promise<UserPrincipal> {
    return apiRequest<UserPrincipal>('/api/v1/auth/me');
  },

  async getEntitlement(tenantId: string): Promise<{ tenantId: string; tier: EntitlementTier }> {
    return apiRequest<{ tenantId: string; tier: EntitlementTier }>(`/api/v1/admin/tenants/${tenantId}/entitlement`);
  }
};
