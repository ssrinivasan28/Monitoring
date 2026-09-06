package com.islandpacific.sentinel.query;

import java.util.UUID;

public class TenantDatasourceCreateDto {
    private UUID tenantId;
    private String name;
    private String kind; // prometheus | thanos | loki
    private String url;
    private String authType = "none"; // none | basic | token | mtls
    private String credentialsRef;
    private boolean enabled = true;
    private boolean isDefault = false;

    public TenantDatasourceCreateDto() {}

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

    public String getCredentialsRef() { return credentialsRef; }
    public void setCredentialsRef(String credentialsRef) { this.credentialsRef = credentialsRef; }

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }

    public boolean isDefault() { return isDefault; }
    public void setDefault(boolean aDefault) { isDefault = aDefault; }
}
