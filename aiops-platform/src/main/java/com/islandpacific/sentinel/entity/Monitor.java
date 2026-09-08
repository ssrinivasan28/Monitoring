package com.islandpacific.sentinel.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "monitors")
public class Monitor {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String kind;

    @Column(nullable = false)
    private int port;

    @Column(nullable = false)
    private String platform = "windows"; // ibmi | windows — used by the correlation engine for cross-platform grouping

    @Column(name = "last_seen_at")
    private Instant lastSeenAt;

    public Monitor() {}

    public Monitor(UUID tenantId, String name, String kind, int port) {
        this.tenantId = tenantId;
        this.name = name;
        this.kind = kind;
        this.port = port;
    }

    public Monitor(UUID tenantId, String name, String kind, int port, String platform) {
        this(tenantId, name, kind, port);
        this.platform = platform;
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getTenantId() { return tenantId; }
    public void setTenantId(UUID tenantId) { this.tenantId = tenantId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getKind() { return kind; }
    public void setKind(String kind) { this.kind = kind; }

    public int getPort() { return port; }
    public void setPort(int port) { this.port = port; }

    public String getPlatform() { return platform; }
    public void setPlatform(String platform) { this.platform = platform; }

    public Instant getLastSeenAt() { return lastSeenAt; }
    public void setLastSeenAt(Instant lastSeenAt) { this.lastSeenAt = lastSeenAt; }
}
