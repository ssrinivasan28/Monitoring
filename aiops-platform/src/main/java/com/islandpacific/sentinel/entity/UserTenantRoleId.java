package com.islandpacific.sentinel.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

@Embeddable
public class UserTenantRoleId implements Serializable {

    @Column(name = "user_id")
    private UUID userId;

    @Column(name = "tenant_id")
    private UUID tenantId;

    @Column(name = "role_id")
    private UUID roleId;

    public UserTenantRoleId() {}

    public UserTenantRoleId(UUID userId, UUID tenantId, UUID roleId) {
        this.userId = userId;
        this.tenantId = tenantId;
        this.roleId = roleId;
    }

    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }

    public UUID getTenantId() { return tenantId; }
    public void setTenantId(UUID tenantId) { this.tenantId = tenantId; }

    public UUID getRoleId() { return roleId; }
    public void setRoleId(UUID roleId) { this.roleId = roleId; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        UserTenantRoleId that = (UserTenantRoleId) o;
        return Objects.equals(userId, that.userId) &&
               Objects.equals(tenantId, that.tenantId) &&
               Objects.equals(roleId, that.roleId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(userId, tenantId, roleId);
    }
}
