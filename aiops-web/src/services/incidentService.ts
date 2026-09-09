import { apiRequest } from './apiClient';

export type IncidentSeverity = 'low' | 'medium' | 'high' | 'critical';
export type IncidentStatus = 'open' | 'ack' | 'assigned' | 'resolved';

/** Row shape returned by `GET /api/v1/incidents` (also used for `similarIncidents` in the detail response). */
export interface IncidentSummary {
  id: string;
  tenantId: string;
  severity: IncidentSeverity;
  status: IncidentStatus;
  title: string;
  openedAt: string;
  resolvedAt: string | null;
  assigneeUserId: string | null;
  platforms: string[];
  signalCount: number;
  hasRootCause: boolean;
}

export interface IncidentSignal {
  id: string;
  monitorId: string;
  alertId: string;
  platform: string;
  detailJson: string | null;
}

export interface IncidentTimelineEntry {
  id: string;
  at: string;
  actor: string;
  eventType: string;
  note: string | null;
}

export interface EvidenceItem {
  source: string;
  query: string | null;
  snippet: string | null;
}

/**
 * Structured root-cause output from the 1.2 triage agent. Null on the wire whenever the LLM
 * hasn't run or was unavailable. Field names mirror RootCauseResult's @JsonProperty overrides
 * (snake_case for these two — everything else on this DTO is plain camelCase).
 */
export interface RootCauseResult {
  root_cause_hypothesis: string | null;
  evidence: EvidenceItem[];
  severity: string | null;
  suggested_checks: string[];
  confidence: number | null;
  insufficient: boolean;
}

/** Full shape returned by `GET /api/v1/incidents/{id}`. `rootCause` is null when the AI narrative is absent. */
export interface IncidentDetail {
  id: string;
  tenantId: string;
  severity: IncidentSeverity;
  status: IncidentStatus;
  title: string;
  openedAt: string;
  resolvedAt: string | null;
  assigneeUserId: string | null;
  signals: IncidentSignal[];
  timeline: IncidentTimelineEntry[];
  rootCause: RootCauseResult | null;
  runbookReferences: EvidenceItem[];
  similarIncidents: IncidentSummary[];
}

export interface IncidentListFilters {
  status?: IncidentStatus;
  platform?: string;
  page?: number;
  size?: number;
}

/** Server-side max accepted by the incident API; also our "long list" cutoff for the console. */
export const INCIDENT_LIST_MAX_SIZE = 200;

export async function getIncidents(filters: IncidentListFilters = {}): Promise<IncidentSummary[]> {
  const params = new URLSearchParams();
  if (filters.status) params.set('status', filters.status);
  if (filters.platform) params.set('platform', filters.platform);
  params.set('page', String(filters.page ?? 0));
  params.set('size', String(filters.size ?? INCIDENT_LIST_MAX_SIZE));
  return apiRequest<IncidentSummary[]>(`/api/v1/incidents?${params.toString()}`);
}

export async function getIncidentDetail(id: string): Promise<IncidentDetail> {
  return apiRequest<IncidentDetail>(`/api/v1/incidents/${id}`);
}

export async function acknowledgeIncident(id: string): Promise<void> {
  await apiRequest(`/api/v1/incidents/${id}/acknowledge`, { method: 'POST' });
}

/** Manual "Push to Teams" action (1.4 UI) — served by the 1.6 Teams integration. */
export async function pushIncidentToTeams(id: string): Promise<void> {
  await apiRequest(`/api/v1/incidents/${id}/push-teams`, { method: 'POST' });
}

/** Manual "Create/enrich ticket" action (1.4 UI) — served by the 1.7 ITSM sync. */
export async function createIncidentTicket(id: string): Promise<void> {
  await apiRequest(`/api/v1/incidents/${id}/create-ticket`, { method: 'POST' });
}
