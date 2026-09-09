package com.islandpacific.sentinel.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "tool_calls")
public class ToolCall {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(name = "agent_run_id", nullable = false)
    private UUID agentRunId;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(nullable = false)
    private String tool;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "args_redacted_json", columnDefinition = "jsonb")
    private String argsRedactedJson;

    @Column(name = "result_summary", columnDefinition = "TEXT")
    private String resultSummary;

    /** 1.9: correlates this tool call to the incident investigation it belongs to (nullable). */
    @Column(name = "incident_id")
    private UUID incidentId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    public ToolCall() {}

    public ToolCall(UUID agentRunId, UUID tenantId, String tool, String argsRedactedJson, String resultSummary) {
        this.agentRunId = agentRunId;
        this.tenantId = tenantId;
        this.tool = tool;
        this.argsRedactedJson = argsRedactedJson;
        this.resultSummary = resultSummary;
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getAgentRunId() { return agentRunId; }
    public void setAgentRunId(UUID agentRunId) { this.agentRunId = agentRunId; }

    public UUID getTenantId() { return tenantId; }
    public void setTenantId(UUID tenantId) { this.tenantId = tenantId; }

    public String getTool() { return tool; }
    public void setTool(String tool) { this.tool = tool; }

    public String getArgsRedactedJson() { return argsRedactedJson; }
    public void setArgsRedactedJson(String argsRedactedJson) { this.argsRedactedJson = argsRedactedJson; }

    public String getResultSummary() { return resultSummary; }
    public void setResultSummary(String resultSummary) { this.resultSummary = resultSummary; }

    public UUID getIncidentId() { return incidentId; }
    public void setIncidentId(UUID incidentId) { this.incidentId = incidentId; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
