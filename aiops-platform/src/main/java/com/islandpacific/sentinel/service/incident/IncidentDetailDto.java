package com.islandpacific.sentinel.service.incident;

import com.islandpacific.sentinel.triage.model.RootCauseResult;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Full incident detail shape returned by {@code GET /api/v1/incidents/{id}}: signals, root cause
 * (with cited evidence), matched runbook references, similar past incidents, and the timeline.
 * {@code rootCause} is null whenever the 1.2 triage agent has not run or the LLM was unavailable -
 * the rest of the response is unaffected (AI-optional).
 */
public class IncidentDetailDto {

    private UUID id;
    private UUID tenantId;
    private String severity;
    private String status;
    private String title;
    private Instant openedAt;
    private Instant resolvedAt;
    private UUID assigneeUserId;
    private List<IncidentSignalDto> signals = new ArrayList<>();
    private List<IncidentTimelineEntryDto> timeline = new ArrayList<>();
    private RootCauseResult rootCause;
    private List<RootCauseResult.EvidenceItem> runbookReferences = new ArrayList<>();
    private List<IncidentSummaryDto> similarIncidents = new ArrayList<>();

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

    public List<IncidentSignalDto> getSignals() { return signals; }
    public void setSignals(List<IncidentSignalDto> signals) { this.signals = signals != null ? signals : new ArrayList<>(); }

    public List<IncidentTimelineEntryDto> getTimeline() { return timeline; }
    public void setTimeline(List<IncidentTimelineEntryDto> timeline) { this.timeline = timeline != null ? timeline : new ArrayList<>(); }

    public RootCauseResult getRootCause() { return rootCause; }
    public void setRootCause(RootCauseResult rootCause) { this.rootCause = rootCause; }

    public List<RootCauseResult.EvidenceItem> getRunbookReferences() { return runbookReferences; }
    public void setRunbookReferences(List<RootCauseResult.EvidenceItem> runbookReferences) {
        this.runbookReferences = runbookReferences != null ? runbookReferences : new ArrayList<>();
    }

    public List<IncidentSummaryDto> getSimilarIncidents() { return similarIncidents; }
    public void setSimilarIncidents(List<IncidentSummaryDto> similarIncidents) {
        this.similarIncidents = similarIncidents != null ? similarIncidents : new ArrayList<>();
    }
}
