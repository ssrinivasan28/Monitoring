package com.islandpacific.sentinel.service.incident;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Ranked list-row shape returned by {@code GET /api/v1/incidents}. */
public class IncidentSummaryDto {

    private UUID id;
    private UUID tenantId;
    private String severity;
    private String status;
    private String title;
    private Instant openedAt;
    private Instant resolvedAt;
    private UUID assigneeUserId;
    private List<String> platforms = new ArrayList<>();
    private int signalCount;
    private boolean hasRootCause;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getTenantId() { return tenantId; }
    public void setTenantId(UUID tenantId) { this.tenantId = tenantId; }

    public String getSeverity() { return severity; }
    public void setSeverity(String severity) { this.severity = severity; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public Instant getOpenedAt() { return openedAt; }
    public void setOpenedAt(Instant openedAt) { this.openedAt = openedAt; }

    public Instant getResolvedAt() { return resolvedAt; }
    public void setResolvedAt(Instant resolvedAt) { this.resolvedAt = resolvedAt; }

    public UUID getAssigneeUserId() { return assigneeUserId; }
    public void setAssigneeUserId(UUID assigneeUserId) { this.assigneeUserId = assigneeUserId; }

    public List<String> getPlatforms() { return platforms; }
    public void setPlatforms(List<String> platforms) { this.platforms = platforms != null ? platforms : new ArrayList<>(); }

    public int getSignalCount() { return signalCount; }
    public void setSignalCount(int signalCount) { this.signalCount = signalCount; }

    public boolean isHasRootCause() { return hasRootCause; }
    public void setHasRootCause(boolean hasRootCause) { this.hasRootCause = hasRootCause; }
}
