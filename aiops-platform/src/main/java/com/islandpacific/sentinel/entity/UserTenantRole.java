package com.islandpacific.sentinel.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "user_tenant_roles")
public class UserTenantRole {

    @EmbeddedId
    private UserTenantRoleId id;

    @ManyToOne(fetch = FetchType.EAGER)
    @MapsId("userId")
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne(fetch = FetchType.EAGER)
    @MapsId("tenantId")
    @JoinColumn(name = "tenant_id")
    private Tenant tenant;

    @ManyToOne(fetch = FetchType.EAGER)
    @MapsId("roleId")
    @JoinColumn(name = "role_id")
    private Role role;

    public UserTenantRole() {}

    public UserTenantRole(User user, Tenant tenant, Role role) {
        this.user = user;
        this.tenant = tenant;
        this.role = role;
        this.id = new UserTenantRoleId(user.getId(), tenant.getId(), role.getId());
    }

    public UserTenantRoleId getId() { return id; }
    public void setId(UserTenantRoleId id) { this.id = id; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public Tenant getTenant() { return tenant; }
    public void setTenant(Tenant tenant) { this.tenant = tenant; }

    public Role getRole() { return role; }
    public void setRole(Role role) { this.role = role; }
}
