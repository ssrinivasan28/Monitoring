package com.islandpacific.sentinel.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.util.UUID;

@Entity
@Table(name = "integration_config")
public class IntegrationConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(nullable = false)
    private String kind; // teams | servicenow | jira | pagerduty | opsgenie | siem | email

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "config_json", nullable = false, columnDefinition = "jsonb")
    private String configJson;

    @Column(nullable = false)
    private boolean enabled = true;

    public IntegrationConfig() {}

    public IntegrationConfig(UUID tenantId, String kind, String configJson, boolean enabled) {
        this.tenantId = tenantId;
        this.kind = kind;
        this.configJson = configJson;
        this.enabled = enabled;
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getTenantId() { return tenantId; }
    public void setTenantId(UUID tenantId) { this.tenantId = tenantId; }

    public String getKind() { return kind; }
    public void setKind(String kind) { this.kind = kind; }

    public String getConfigJson() { return configJson; }
    public void setConfigJson(String configJson) { this.configJson = configJson; }

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
}
