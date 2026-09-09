export type EntitlementTier = 'BASIC' | 'PRO';

export type UserRole = 
  | 'SUPER_ADMIN' 
  | 'STAFF_ADMIN' 
  | 'STAFF' 
  | 'TENANT_ADMIN' 
  | 'TENANT_USER' 
  | 'READ_ONLY';

export interface UserPrincipal {
  userId: string;
  email: string;
  displayName: string;
  roleKey: UserRole;
  tenantId: string | null; // null for staff who span tenants
  staff: boolean;
}

export interface Tenant {
  id: string;
  name: string;
  tier: EntitlementTier;
  active: boolean;
}

export type DatasourceKind = 'prometheus' | 'thanos' | 'loki';
export type AuthType = 'NONE' | 'BASIC' | 'BEARER';

export interface TenantDatasource {
  id: string;
  tenantId: string;
  name: string;
  kind: DatasourceKind;
  url: string;
  authType: AuthType;
  authHeaderRef?: string;
  authUsername?: string;
  enabled: boolean;
  createdAt?: string;
  updatedAt?: string;
}

export interface CreateDatasourceRequest {
  tenantId?: string;
  name: string;
  kind: DatasourceKind;
  url: string;
  authType: AuthType;
  authUsername?: string;
  authSecret?: string;
  enabled: boolean;
}

export interface UpdateDatasourceRequest {
  name?: string;
  kind?: DatasourceKind;
  url?: string;
  authType?: AuthType;
  authUsername?: string;
  authSecret?: string;
  enabled?: boolean;
}

export interface TestConnectionResponse {
  reachable: boolean;
  statusCode?: number;
  message: string;
  latencyMs?: number;
}

export interface AssistantCitation {
  tool: string;
  query: string;
  summary: string;
}

export interface AssistantAnswerPayload {
  tenantId?: string;
  answer: string;
  citations: AssistantCitation[];
  aiAvailable: boolean;
  insufficient: boolean;
  tier?: string;
}
