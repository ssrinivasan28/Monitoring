package com.islandpacific.sentinel;

import com.islandpacific.sentinel.entity.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

public class EntityAndRepositoryUnitTest {

    @Test
    @DisplayName("Tenant entity instantiation & default properties")
    void tenantEntityDefaults() {
        Tenant tenant = new Tenant("Acme Corp", "client-100");
        assertThat(tenant.getName()).isEqualTo("Acme Corp");
        assertThat(tenant.getClientInstanceId()).isEqualTo("client-100");
        assertThat(tenant.getStatus()).isEqualTo("ACTIVE");
        assertThat(tenant.getCreatedAt()).isNotNull();
    }

    @Test
    @DisplayName("Incident & Signal entity association check")
    void incidentAndSignalEntities() {
        UUID tenantId = UUID.randomUUID();
        Incident incident = new Incident(tenantId, "CRITICAL", "open", "QSYSOPR Message Error");
        incident.setId(UUID.randomUUID());
        incident.setRootCauseJson("{\"error_code\": \"CPF9999\"}");

        assertThat(incident.getTenantId()).isEqualTo(tenantId);
        assertThat(incident.getSeverity()).isEqualTo("CRITICAL");
        assertThat(incident.getStatus()).isEqualTo("open");

        IncidentSignal signal = new IncidentSignal(incident.getId(), tenantId, "ibmi");
        signal.setDetailJson("{\"msgid\": \"CPF9999\"}");

        assertThat(signal.getIncidentId()).isEqualTo(incident.getId());
        assertThat(signal.getPlatform()).isEqualTo("ibmi");
    }

    @Test
    @DisplayName("Audit AgentRun entity fields & default cost")
    void agentRunAuditEntity() {
        UUID tenantId = UUID.randomUUID();
        AgentRun run = new AgentRun(tenantId, "triage_agent", "Redacted prompt", "claude-3-5-sonnet", 150, 300, BigDecimal.valueOf(0.0125));

        assertThat(run.getTenantId()).isEqualTo(tenantId);
        assertThat(run.getAgent()).isEqualTo("triage_agent");
        assertThat(run.getTokensIn()).isEqualTo(150);
        assertThat(run.getTokensOut()).isEqualTo(300);
        assertThat(run.getCost()).isEqualTo(BigDecimal.valueOf(0.0125));
        assertThat(run.getCreatedAt()).isNotNull();
    }

    @Test
    @DisplayName("TenantDatasource unique constraint mapping check")
    void tenantDatasourceMapping() {
        UUID tenantId = UUID.randomUUID();
        TenantDatasource ds = new TenantDatasource(tenantId, "prom-main", "prometheus", "http://localhost:9090");
        ds.setAuthType("basic");

        assertThat(ds.getTenantId()).isEqualTo(tenantId);
        assertThat(ds.getName()).isEqualTo("prom-main");
        assertThat(ds.getKind()).isEqualTo("prometheus");
        assertThat(ds.getUrl()).isEqualTo("http://localhost:9090");
        assertThat(ds.getAuthType()).isEqualTo("basic");
        assertThat(ds.isEnabled()).isTrue();
    }

    @Test
    @DisplayName("UserTenantRoleId equals and hashCode contract")
    void userTenantRoleIdContract() {
        UUID u = UUID.randomUUID();
        UUID t = UUID.randomUUID();
        UUID r = UUID.randomUUID();

        UserTenantRoleId id1 = new UserTenantRoleId(u, t, r);
        UserTenantRoleId id2 = new UserTenantRoleId(u, t, r);

        assertThat(id1).isEqualTo(id2);
        assertThat(id1.hashCode()).isEqualTo(id2.hashCode());
    }
}
