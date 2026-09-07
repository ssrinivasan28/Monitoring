import { apiRequest } from './apiClient';
import { 
  TenantDatasource, 
  CreateDatasourceRequest, 
  UpdateDatasourceRequest, 
  TestConnectionResponse 
} from '../types';

export const datasourceService = {
  async listDatasources(tenantId?: string): Promise<TenantDatasource[]> {
    const url = tenantId ? `/api/v1/admin/datasources?tenantId=${tenantId}` : '/api/v1/admin/datasources';
    return apiRequest<TenantDatasource[]>(url);
  },

  async getDatasource(id: string): Promise<TenantDatasource> {
    return apiRequest<TenantDatasource>(`/api/v1/admin/datasources/${id}`);
  },

  async createDatasource(dto: CreateDatasourceRequest): Promise<TenantDatasource> {
    return apiRequest<TenantDatasource>('/api/v1/admin/datasources', {
      method: 'POST',
      body: JSON.stringify(dto),
    });
  },

  async updateDatasource(id: string, dto: UpdateDatasourceRequest): Promise<TenantDatasource> {
    return apiRequest<TenantDatasource>(`/api/v1/admin/datasources/${id}`, {
      method: 'PUT',
      body: JSON.stringify(dto),
    });
  },

  async deleteDatasource(id: string): Promise<void> {
    return apiRequest<void>(`/api/v1/admin/datasources/${id}`, {
      method: 'DELETE',
    });
  },

  async testConnection(dto: CreateDatasourceRequest): Promise<TestConnectionResponse> {
    return apiRequest<TestConnectionResponse>('/api/v1/admin/datasources/test-connection', {
      method: 'POST',
      body: JSON.stringify(dto),
    });
  }
};
