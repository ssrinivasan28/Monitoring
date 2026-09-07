package com.islandpacific.sentinel.tool;

import com.islandpacific.sentinel.entity.IntegrationConfig;
import com.islandpacific.sentinel.entity.TenantDatasource;
import com.islandpacific.sentinel.repository.IntegrationConfigRepository;
import com.islandpacific.sentinel.repository.TenantDatasourceRepository;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ReadConfigToolTest {

    @Test
    void testReadConfigRedactsSecrets() {
        IntegrationConfigRepository integrationRepo = mock(IntegrationConfigRepository.class);
        TenantDatasourceRepository datasourceRepo = mock(TenantDatasourceRepository.class);

        UUID tenantId = UUID.randomUUID();

        TenantDatasource ds = new TenantDatasource(tenantId, "Prometheus-Central", "prometheus", "http://localhost:9090");
        ds.setCredentialsRef("DPAPI(secret_blob_123)");
        when(datasourceRepo.findByTenantId(tenantId)).thenReturn(List.of(ds));

        IntegrationConfig ic = new IntegrationConfig(tenantId, "servicenow",
                "{\"instanceUrl\":\"https://dev.service-now.com\",\"username\":\"admin\",\"password\":\"super_secret_123\"}", true);
        when(integrationRepo.findByTenantId(tenantId)).thenReturn(List.of(ic));

        ReadConfigTool tool = new ReadConfigTool(integrationRepo, datasourceRepo);
        ToolExecutionResult result = tool.execute(tenantId, Map.of("category", "all"));

        assertTrue(result.isSuccess());

        @SuppressWarnings("unchecked")
        Map<String, Object> data = (Map<String, Object>) result.getData();
        assertNotNull(data);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> datasources = (List<Map<String, Object>>) data.get("datasources");
        assertEquals(1, datasources.size());
        assertEquals("[REDACTED]", datasources.get(0).get("credentialsRef"));

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> integrations = (List<Map<String, Object>>) data.get("integrations");
        assertEquals(1, integrations.size());
        @SuppressWarnings("unchecked")
        Map<String, Object> configMap = (Map<String, Object>) integrations.get(0).get("config");
        assertEquals("admin", configMap.get("username"));
        assertEquals("[REDACTED]", configMap.get("password"));
    }
}
