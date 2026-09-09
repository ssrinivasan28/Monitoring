package com.islandpacific.sentinel.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

/**
 * Dead-letter/retry bookkeeping for a failing ITSM sync (1.7): one outstanding-problem row per
 * incident, cleared on the next successful sync. Once {@code attemptCount} reaches the configured
 * max, {@code deadLettered} is set so the background sweep stops auto-retrying it - a manual
 * "Push to ITSM" can still attempt again.
 */
@Entity
@Table(name = "itsm_sync_failures")
public class ItsmSyncFailure {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "incident_id", nullable = false)
    private UUID incidentId;

    @Column(nullable = false)
    private String kind; // servicenow | jira

    @Column(nullable = false)
    private String operation; // CREATE_TICKET | UPDATE_STATUS | FETCH_STATUS

    @Column(name = "error_message", length = 2000)
    private String errorMessage;

    @Column(name = "attempt_count", nullable = false)
    private int attemptCount = 1;

    @Column(name = "dead_lettered", nullable = false)
    private boolean deadLettered = false;

    @Column(name = "first_failed_at", nullable = false, updatable = false)
    private Instant firstFailedAt = Instant.now();

    @Column(name = "last_failed_at", nullable = false)
    private Instant lastFailedAt = Instant.now();

    public ItsmSyncFailure() {}

    public ItsmSyncFailure(UUID tenantId, UUID incidentId, String kind, String operation, String errorMessage) {
        this.tenantId = tenantId;
        this.incidentId = incidentId;
        this.kind = kind;
        this.operation = operation;
        this.errorMessage = errorMessage;
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getTenantId() { return tenantId; }
    public void setTenantId(UUID tenantId) { this.tenantId = tenantId; }

    public UUID getIncidentId() { return incidentId; }
    public void setIncidentId(UUID incidentId) { this.incidentId = incidentId; }

    public String getKind() { return kind; }
    public void setKind(String kind) { this.kind = kind; }

    public String getOperation() { return operation; }
    public void setOperation(String operation) { this.operation = operation; }

    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }

    public int getAttemptCount() { return attemptCount; }
    public void setAttemptCount(int attemptCount) { this.attemptCount = attemptCount; }

    public boolean isDeadLettered() { return deadLettered; }
    public void setDeadLettered(boolean deadLettered) { this.deadLettered = deadLettered; }

    public Instant getFirstFailedAt() { return firstFailedAt; }
    public void setFirstFailedAt(Instant firstFailedAt) { this.firstFailedAt = firstFailedAt; }

    public Instant getLastFailedAt() { return lastFailedAt; }
    public void setLastFailedAt(Instant lastFailedAt) { this.lastFailedAt = lastFailedAt; }
}
