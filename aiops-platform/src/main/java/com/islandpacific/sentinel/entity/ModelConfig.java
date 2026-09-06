package com.islandpacific.sentinel.entity;

import jakarta.persistence.*;
import java.util.UUID;

@Entity
@Table(name = "model_config")
public class ModelConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(name = "tenant_id")
    private UUID tenantId; // NULL allowed for global model config

    @Column(nullable = false)
    private String provider;

    @Column(length = 500)
    private String endpoint;

    @Column(nullable = false)
    private String model;

    @Column(nullable = false)
    private boolean active = true;

    public ModelConfig() {}

    public ModelConfig(UUID tenantId, String provider, String endpoint, String model, boolean active) {
        this.tenantId = tenantId;
        this.provider = provider;
        this.endpoint = endpoint;
        this.model = model;
        this.active = active;
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getTenantId() { return tenantId; }
    public void setTenantId(UUID tenantId) { this.tenantId = tenantId; }

    public String getProvider() { return provider; }
    public void setProvider(String provider) { this.provider = provider; }

    public String getEndpoint() { return endpoint; }
    public void setEndpoint(String endpoint) { this.endpoint = endpoint; }

    public String getModel() { return model; }
    public void setModel(String model) { this.model = model; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
}
