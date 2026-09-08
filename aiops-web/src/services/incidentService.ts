import { apiRequest } from './apiClient';

export type IncidentSeverity = 'low' | 'medium' | 'high' | 'critical';
export type IncidentStatus = 'open' | 'ack' | 'assigned' | 'resolved';

export interface Incident {
  id: string;
  tenantId: string;
  severity: IncidentSeverity;
  status: IncidentStatus;
  title: string;
  rootCauseJson: string | null;
  openedAt: string;
  resolvedAt: string | null;
  assigneeUserId: string | null;
}

export async function getIncidents(): Promise<Incident[]> {
  return apiRequest<Incident[]>('/api/v1/incidents');
}

export async function acknowledgeIncident(id: string): Promise<void> {
  await apiRequest(`/api/v1/incidents/${id}/acknowledge`, { method: 'POST' });
}
