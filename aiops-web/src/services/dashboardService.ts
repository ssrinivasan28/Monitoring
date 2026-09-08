import { apiRequest } from './apiClient';

export interface PanelDefinition {
  id: string;
  title: string;
  type: 'timeseries' | 'stat' | 'table' | 'logs' | string;
  query: string;
  unit?: string;
  thresholds?: {
    warning?: number;
    critical?: number;
    [key: string]: any;
  };
  gridPos?: {
    x: number;
    y: number;
    w: number;
    h: number;
  };
  legendFormat?: string;
}

export interface DashboardDefinition {
  id: string;
  title: string;
  category: string;
  variables: string[];
  panels: PanelDefinition[];
  custom?: boolean;
}

export async function listDashboards(): Promise<DashboardDefinition[]> {
  return apiRequest<DashboardDefinition[]>('/api/v1/dashboards');
}

export async function getDashboard(id: string): Promise<DashboardDefinition> {
  return apiRequest<DashboardDefinition>(`/api/v1/dashboards/${encodeURIComponent(id)}`);
}

export async function saveCustomDashboard(dashboard: DashboardDefinition): Promise<DashboardDefinition> {
  return apiRequest<DashboardDefinition>('/api/v1/dashboards', {
    method: 'POST',
    body: JSON.stringify(dashboard),
  });
}

export async function deleteCustomDashboard(id: string): Promise<{ message: string; id: string }> {
  return apiRequest<{ message: string; id: string }>(`/api/v1/dashboards/${encodeURIComponent(id)}`, {
    method: 'DELETE',
  });
}

export async function importGrafanaDashboard(grafanaJson: string): Promise<DashboardDefinition> {
  return apiRequest<DashboardDefinition>('/api/v1/dashboards/import/grafana', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    body: grafanaJson,
  });
}
