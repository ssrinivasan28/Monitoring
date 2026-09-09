package com.islandpacific.sentinel.service.incident;

import java.time.Instant;
import java.util.UUID;

public class IncidentTimelineEntryDto {

    private UUID id;
    private Instant at;
    private String actor;
    private String eventType;
    private String note;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public Instant getAt() { return at; }
    public void setAt(Instant at) { this.at = at; }

    public String getActor() { return actor; }
    public void setActor(String actor) { this.actor = actor; }

    public String getEventType() { return eventType; }
    public void setEventType(String eventType) { this.eventType = eventType; }

    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }
}
