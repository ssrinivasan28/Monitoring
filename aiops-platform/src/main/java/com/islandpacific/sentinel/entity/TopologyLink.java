package com.islandpacific.sentinel.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "topology_links")
public class TopologyLink {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "windows_host", nullable = false)
    private String windowsHost;

    @Column(name = "ibmi_system", nullable = false)
    private String ibmiSystem;

    @Column(nullable = false)
    private boolean enabled = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    public TopologyLink() {}

    public TopologyLink(UUID tenantId, String windowsHost, String ibmiSystem) {
        this.tenantId = tenantId;
        this.windowsHost = windowsHost;
        this.ibmiSystem = ibmiSystem;
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getTenantId() { return tenantId; }
    public void setTenantId(UUID tenantId) { this.tenantId = tenantId; }

    public String getWindowsHost() { return windowsHost; }
    public void setWindowsHost(String windowsHost) { this.windowsHost = windowsHost; }

    public String getIbmiSystem() { return ibmiSystem; }
    public void setIbmiSystem(String ibmiSystem) { this.ibmiSystem = ibmiSystem; }

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
