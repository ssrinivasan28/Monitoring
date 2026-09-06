package com.islandpacific.sentinel;

import com.islandpacific.sentinel.entity.AgentRun;
import com.islandpacific.sentinel.entity.Tenant;
import com.islandpacific.sentinel.repository.AgentRunRepository;
import com.islandpacific.sentinel.repository.TenantRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class AuditImmutabilityTest extends AbstractIntegrationTest {

    @Autowired
    private TenantRepository tenantRepository;

    @Autowired
    private AgentRunRepository agentRunRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void updateAndDeleteOnAgentRunsThrowsImmutabilityTriggerException() {
        Tenant tenant = tenantRepository.save(new Tenant("Audit Tenant", "client-audit-01"));
        UUID tenantId = tenant.getId();

        AgentRun run = new AgentRun(tenantId, "triage_agent", "Check status", "claude-3-5-sonnet", 100, 200, BigDecimal.valueOf(0.005));
        run = agentRunRepository.save(run);
        UUID runId = run.getId();

        // Attempt direct UPDATE via SQL
        assertThatThrownBy(() -> jdbcTemplate.update("UPDATE agent_runs SET model = ? WHERE id = ?", "gpt-4", runId))
                .hasMessageContaining("Audit tables are append-only");

        // Attempt direct DELETE via SQL
        assertThatThrownBy(() -> jdbcTemplate.update("DELETE FROM agent_runs WHERE id = ?", runId))
                .hasMessageContaining("Audit tables are append-only");
    }

    @Test
    void updateAndDeleteOnIncidentTimelineThrowsImmutabilityTriggerException() {
        Tenant tenant = tenantRepository.save(new Tenant("Timeline Audit Tenant", "client-audit-02"));
        UUID tenantId = tenant.getId();
        UUID incidentId = UUID.randomUUID();

        // Insert parent incident row for FK
        jdbcTemplate.update(
            "INSERT INTO incidents (id, tenant_id, severity, status, title) VALUES (?, ?, ?, ?, ?)",
            incidentId, tenantId, "HIGH", "open", "Test Incident"
        );

        jdbcTemplate.update(
            "INSERT INTO incident_timeline (id, incident_id, tenant_id, actor, event_type, note) VALUES (?, ?, ?, ?, ?, ?)",
            UUID.randomUUID(), incidentId, tenantId, "system", "INCIDENT_CREATED", "Incident auto-created"
        );

        assertThatThrownBy(() -> jdbcTemplate.update("UPDATE incident_timeline SET note = ? WHERE tenant_id = ?", "altered", tenantId))
                .hasMessageContaining("Audit tables are append-only");

        assertThatThrownBy(() -> jdbcTemplate.update("DELETE FROM incident_timeline WHERE tenant_id = ?", tenantId))
                .hasMessageContaining("Audit tables are append-only");
    }

    @Test
    void updateAndDeleteOnToolCallsThrowsImmutabilityTriggerException() {
        Tenant tenant = tenantRepository.save(new Tenant("ToolCall Audit Tenant", "client-audit-03"));
        UUID tenantId = tenant.getId();
        UUID runId = UUID.randomUUID();

        // Insert direct run record for FK
        jdbcTemplate.update(
            "INSERT INTO agent_runs (id, tenant_id, agent, prompt_redacted, model, tokens_in, tokens_out, cost) VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
            runId, tenantId, "test_agent", "prompt", "model", 10, 10, BigDecimal.ZERO
        );

        UUID toolCallId = UUID.randomUUID();
        jdbcTemplate.update(
            "INSERT INTO tool_calls (id, agent_run_id, tenant_id, tool, result_summary) VALUES (?, ?, ?, ?, ?)",
            toolCallId, runId, tenantId, "query_prometheus", "success"
        );

        assertThatThrownBy(() -> jdbcTemplate.update("UPDATE tool_calls SET result_summary = ? WHERE id = ?", "tampered", toolCallId))
                .hasMessageContaining("Audit tables are append-only");

        assertThatThrownBy(() -> jdbcTemplate.update("DELETE FROM tool_calls WHERE id = ?", toolCallId))
                .hasMessageContaining("Audit tables are append-only");
    }

    @Test
    void updateAndDeleteOnAuthAuditLogThrowsImmutabilityTriggerException() {
        Tenant tenant = tenantRepository.save(new Tenant("Auth Audit Tenant", "client-audit-04"));
        UUID tenantId = tenant.getId();
        UUID logId = UUID.randomUUID();

        jdbcTemplate.update(
            "INSERT INTO auth_audit_log (id, event_type, tenant_id, success, detail) VALUES (?, ?, ?, ?, ?)",
            logId, "LOGIN_SUCCESS", tenantId, true, "User logged in"
        );

        assertThatThrownBy(() -> jdbcTemplate.update("UPDATE auth_audit_log SET detail = ? WHERE id = ?", "tampered", logId))
                .hasMessageContaining("Audit tables are append-only");

        assertThatThrownBy(() -> jdbcTemplate.update("DELETE FROM auth_audit_log WHERE id = ?", logId))
                .hasMessageContaining("Audit tables are append-only");
    }
}
