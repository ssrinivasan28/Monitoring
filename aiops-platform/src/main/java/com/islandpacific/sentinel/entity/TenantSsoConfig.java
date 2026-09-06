package com.islandpacific.sentinel.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "tenant_sso_configs")
public class TenantSsoConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(name = "tenant_id", nullable = false, unique = true)
    private UUID tenantId;

    @Column(name = "sso_type", nullable = false)
    private String ssoType; // oidc | saml

    @Column(name = "issuer_url")
    private String issuerUrl;

    @Column(name = "client_id")
    private String clientId;

    @Column(name = "client_secret_ref")
    private String clientSecretRef;

    @Column(name = "saml_metadata_url")
    private String samlMetadataUrl;

    @Column(name = "saml_certificate_ref")
    private String samlCertificateRef;

    @Column(name = "saml_entity_id")
    private String samlEntityId;

    @Column(nullable = false)
    private boolean enabled = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    public TenantSsoConfig() {}

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getTenantId() { return tenantId; }
    public void setTenantId(UUID tenantId) { this.tenantId = tenantId; }

    public String getSsoType() { return ssoType; }
    public void setSsoType(String ssoType) { this.ssoType = ssoType; }

    public String getIssuerUrl() { return issuerUrl; }
    public void setIssuerUrl(String issuerUrl) { this.issuerUrl = issuerUrl; }

    public String getClientId() { return clientId; }
    public void setClientId(String clientId) { this.clientId = clientId; }

    public String getClientSecretRef() { return clientSecretRef; }
    public void setClientSecretRef(String clientSecretRef) { this.clientSecretRef = clientSecretRef; }

    public String getSamlMetadataUrl() { return samlMetadataUrl; }
    public void setSamlMetadataUrl(String samlMetadataUrl) { this.samlMetadataUrl = samlMetadataUrl; }

    public String getSamlCertificateRef() { return samlCertificateRef; }
    public void setSamlCertificateRef(String samlCertificateRef) { this.samlCertificateRef = samlCertificateRef; }

    public String getSamlEntityId() { return samlEntityId; }
    public void setSamlEntityId(String samlEntityId) { this.samlEntityId = samlEntityId; }

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
