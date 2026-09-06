package com.islandpacific.sentinel.query;

public class TenantDatasourceUpdateDto {
    private String name;
    private String kind;
    private String url;
    private String authType;
    private String credentialsRef;
    private Boolean enabled;
    private Boolean isDefault;

    public TenantDatasourceUpdateDto() {}

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

    public Boolean getEnabled() { return enabled; }
    public void setEnabled(Boolean enabled) { this.enabled = enabled; }

    public Boolean getIsDefault() { return isDefault; }
    public void setIsDefault(Boolean isDefault) { this.isDefault = isDefault; }
}
