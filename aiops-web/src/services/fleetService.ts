import { apiRequest } from './apiClient';

export interface FleetInputScore {
  key: string;
  name: string;
  value: number | null;
  band: 'GREEN' | 'AMBER' | 'RED' | 'UNKNOWN';
  stale: boolean;
}

export interface TenantFleet {
  tenantId: string;
  name: string;
  clientInstanceId: string;
  band: 'GREEN' | 'AMBER' | 'RED' | 'UNKNOWN';
  score: number;
  worstInput: string | null;
  inputs: FleetInputScore[];
}

export async function getFleetOverview(): Promise<TenantFleet[]> {
  return apiRequest<TenantFleet[]>('/api/fleet');
}
