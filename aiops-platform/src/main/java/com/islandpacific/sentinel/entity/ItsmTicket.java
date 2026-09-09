package com.islandpacific.sentinel.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

/**
 * Tracks the ServiceNow/Jira ticket created for an incident (1.7), so a later status change
 * updates that same ticket instead of creating a duplicate. Also records the last-synced status on
 * each side so two-way reconciliation can tell which side changed since the previous sync.
 */
@Entity
@Table(name = "itsm_tickets")
public class ItsmTicket {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "incident_id", nullable = false)
    private UUID incidentId;

    @Column(nullable = false)
    private String kind; // servicenow | jira

    @Column(name = "external_id", nullable = false)
    private String externalId;

    @Column(name = "external_url")
    private String externalUrl;

    @Column(name = "last_local_status", nullable = false)
    private String lastLocalStatus;

    @Column(name = "last_itsm_state", nullable = false)
    private String lastItsmState;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    public ItsmTicket() {}

    public ItsmTicket(UUID tenantId, UUID incidentId, String kind, String externalId, String externalUrl,
                       String lastLocalStatus, String lastItsmState) {
        this.tenantId = tenantId;
        this.incidentId = incidentId;
        this.kind = kind;
        this.externalId = externalId;
        this.externalUrl = externalUrl;
        this.lastLocalStatus = lastLocalStatus;
        this.lastItsmState = lastItsmState;
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getTenantId() { return tenantId; }
    public void setTenantId(UUID tenantId) { this.tenantId = tenantId; }

    public UUID getIncidentId() { return incidentId; }
    public void setIncidentId(UUID incidentId) { this.incidentId = incidentId; }

    public String getKind() { return kind; }
    public void setKind(String kind) { this.kind = kind; }

    public String getExternalId() { return externalId; }
    public void setExternalId(String externalId) { this.externalId = externalId; }

    public String getExternalUrl() { return externalUrl; }
    public void setExternalUrl(String externalUrl) { this.externalUrl = externalUrl; }

    public String getLastLocalStatus() { return lastLocalStatus; }
    public void setLastLocalStatus(String lastLocalStatus) { this.lastLocalStatus = lastLocalStatus; }

    public String getLastItsmState() { return lastItsmState; }
    public void setLastItsmState(String lastItsmState) { this.lastItsmState = lastItsmState; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
