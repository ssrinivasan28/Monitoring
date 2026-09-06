package com.islandpacific.sentinel.security;

import java.util.Objects;
import java.util.UUID;

public class TenantContext {

    private final UUID userId;
    private final UUID tenantId;
    private final String clientInstanceId;
    private final String roleKey;

    public TenantContext(UUID userId, UUID tenantId, String clientInstanceId, String roleKey) {
        this.userId = Objects.requireNonNull(userId, "userId must not be null");
        this.tenantId = Objects.requireNonNull(tenantId, "tenantId must not be null");
        this.clientInstanceId = clientInstanceId;
        this.roleKey = RoleMapper.toCanonicalKey(roleKey);
    }

    public UUID getUserId() {
        return userId;
    }

    public UUID getTenantId() {
        return tenantId;
    }

    public String getClientInstanceId() {
        return clientInstanceId;
    }

    public String getRoleKey() {
        return roleKey;
    }

    @Override
    public String toString() {
        return "TenantContext{" +
                "userId=" + userId +
                ", tenantId=" + tenantId +
                ", clientInstanceId='" + clientInstanceId + '\'' +
                ", roleKey='" + roleKey + '\'' +
                '}';
    }
}
