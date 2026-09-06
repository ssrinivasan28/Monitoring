package com.islandpacific.sentinel.query;

import com.islandpacific.sentinel.entity.TenantDatasource;

import java.time.Instant;
import java.util.UUID;

public class TenantDatasourceResponseDto {
    private UUID id;
    private UUID tenantId;
    private String name;
    private String kind;
    private String url;
    private String authType;
    private String credentialsRefMasked;
    private boolean enabled;
    private boolean isDefault;
    private Instant createdAt;
    private Instant updatedAt;

    public TenantDatasourceResponseDto() {}

    public static TenantDatasourceResponseDto fromEntity(TenantDatasource entity) {
        TenantDatasourceResponseDto dto = new TenantDatasourceResponseDto();
        dto.setId(entity.getId());
        dto.setTenantId(entity.getTenantId());
        dto.setName(entity.getName());
        dto.setKind(entity.getKind());
        dto.setUrl(entity.getUrl());
        dto.setAuthType(entity.getAuthType());
        dto.setCredentialsRefMasked(entity.getCredentialsRef() != null && !entity.getCredentialsRef().isBlank() ? "********" : "NONE");
        dto.setEnabled(entity.isEnabled());
        dto.setDefault(entity.isDefault());
        dto.setCreatedAt(entity.getCreatedAt());
        dto.setUpdatedAt(entity.getUpdatedAt());
        return dto;
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getTenantId() { return tenantId; }
    public void setTenantId(UUID tenantId) { this.tenantId = tenantId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getKind() { return kind; }
    public void setKind(String kind) { this.kind = kind; }

    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }

    public String getAuthType() { return authType; }
    public void setAuthType(String authType) { this.authType = authType; }

    public String getCredentialsRefMasked() { return credentialsRefMasked; }
    public void setCredentialsRefMasked(String credentialsRefMasked) { this.credentialsRefMasked = credentialsRefMasked; }

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }

    public boolean isDefault() { return isDefault; }
    public void setDefault(boolean aDefault) { isDefault = aDefault; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
