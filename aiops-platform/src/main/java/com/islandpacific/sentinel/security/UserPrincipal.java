package com.islandpacific.sentinel.security;

import java.util.List;
import java.util.UUID;

public class UserPrincipal {

    private UUID userId;
    private String email;
    private String displayName;
    private String source; // azure_ad | local | federated
    private boolean mfaVerified;
    private String tokenType; // ACCESS | MFA_PENDING
    private List<TenantAccess> tenants;

    public UserPrincipal() {}

    public UserPrincipal(UUID userId, String email, String displayName, String source, boolean mfaVerified, String tokenType, List<TenantAccess> tenants) {
        this.userId = userId;
        this.email = email;
        this.displayName = displayName;
        this.source = source;
        this.mfaVerified = mfaVerified;
        this.tokenType = tokenType;
        this.tenants = tenants;
    }

    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }

    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }

    public boolean isMfaVerified() { return mfaVerified; }
    public void setMfaVerified(boolean mfaVerified) { this.mfaVerified = mfaVerified; }

    public String getTokenType() { return tokenType; }
    public void setTokenType(String tokenType) { this.tokenType = tokenType; }

    public List<TenantAccess> getTenants() { return tenants; }
    public void setTenants(List<TenantAccess> tenants) { this.tenants = tenants; }

    public static class TenantAccess {
        private UUID tenantId;
        private String tenantName;
        private String clientInstanceId;
        private String role;
        private String tier;

        public TenantAccess() {}

        public TenantAccess(UUID tenantId, String tenantName, String clientInstanceId, String role, String tier) {
            this.tenantId = tenantId;
            this.tenantName = tenantName;
            this.clientInstanceId = clientInstanceId;
            this.role = role;
            this.tier = tier;
        }

        public UUID getTenantId() { return tenantId; }
        public void setTenantId(UUID tenantId) { this.tenantId = tenantId; }

        public String getTenantName() { return tenantName; }
        public void setTenantName(String tenantName) { this.tenantName = tenantName; }

        public String getClientInstanceId() { return clientInstanceId; }
        public void setClientInstanceId(String clientInstanceId) { this.clientInstanceId = clientInstanceId; }

        public String getRole() { return role; }
        public void setRole(String role) { this.role = role; }

        public String getTier() { return tier; }
        public void setTier(String tier) { this.tier = tier; }
    }
}
