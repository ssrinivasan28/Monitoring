const TOKEN_KEY = 'sentinel_access_token';
const REFRESH_TOKEN_KEY = 'sentinel_refresh_token';
const ACTIVE_TENANT_KEY = 'sentinel_active_tenant_id';

export const getStoredToken = (): string | null => localStorage.getItem(TOKEN_KEY);
export const setStoredToken = (token: string): void => localStorage.setItem(TOKEN_KEY, token);

export const getStoredRefreshToken = (): string | null => localStorage.getItem(REFRESH_TOKEN_KEY);
export const setStoredRefreshToken = (token: string): void => localStorage.setItem(REFRESH_TOKEN_KEY, token);

export const getStoredTenantId = (): string | null => localStorage.getItem(ACTIVE_TENANT_KEY);
export const setStoredTenantId = (tenantId: string): void => localStorage.setItem(ACTIVE_TENANT_KEY, tenantId);

export const clearStoredAuth = (): void => {
  localStorage.removeItem(TOKEN_KEY);
  localStorage.removeItem(REFRESH_TOKEN_KEY);
  localStorage.removeItem(ACTIVE_TENANT_KEY);
};

export async function apiRequest<T = any>(
  endpoint: string,
  options: RequestInit = {}
): Promise<T> {
  const token = getStoredToken();
  const activeTenantId = getStoredTenantId();

  const headers: Record<string, string> = {
    'Content-Type': 'application/json',
    ...(options.headers as Record<string, string> || {}),
  };

  if (token) {
    headers['Authorization'] = `Bearer ${token}`;
  }

  if (activeTenantId) {
    headers['X-Tenant-Id'] = activeTenantId;
  }

  const response = await fetch(endpoint, {
    ...options,
    headers,
  });

  if (response.status === 401) {
    // Session expired or unauthenticated
    clearStoredAuth();
    if (!window.location.pathname.startsWith('/login')) {
      window.location.href = '/login?expired=true';
    }
    throw new Error('Unauthorized');
  }

  if (!response.ok) {
    let errorMessage = `HTTP ${response.status} ${response.statusText}`;
    try {
      const errorJson = await response.json();
      if (errorJson.message) {
        errorMessage = errorJson.message;
      } else if (errorJson.error) {
        errorMessage = errorJson.error;
      }
    } catch (_) {
      // Ignore JSON parse errors for non-json error responses
    }
    throw new Error(errorMessage);
  }

  // Handle empty responses like 204 No Content
  if (response.status === 204) {
    return {} as T;
  }

  return response.json();
}
