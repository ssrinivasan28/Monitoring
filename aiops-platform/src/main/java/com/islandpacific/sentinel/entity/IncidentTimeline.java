package com.islandpacific.sentinel.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "incident_timeline")
public class IncidentTimeline {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(name = "incident_id", nullable = false)
    private UUID incidentId;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "at", nullable = false, updatable = false)
    private Instant at = Instant.now();

    @Column(nullable = false)
    private String actor;

    @Column(name = "event_type", nullable = false)
    private String eventType;

    @Column(columnDefinition = "TEXT")
    private String note;

    public IncidentTimeline() {}

    public IncidentTimeline(UUID incidentId, UUID tenantId, String actor, String eventType, String note) {
        this.incidentId = incidentId;
        this.tenantId = tenantId;
        this.actor = actor;
        this.eventType = eventType;
        this.note = note;
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getIncidentId() { return incidentId; }
    public void setIncidentId(UUID incidentId) { this.incidentId = incidentId; }

    public UUID getTenantId() { return tenantId; }
    public void setTenantId(UUID tenantId) { this.tenantId = tenantId; }

    public Instant getAt() { return at; }
    public void setAt(Instant at) { this.at = at; }

    public String getActor() { return actor; }
    public void setActor(String actor) { this.actor = actor; }

    public String getEventType() { return eventType; }
    public void setEventType(String eventType) { this.eventType = eventType; }

    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }
}
