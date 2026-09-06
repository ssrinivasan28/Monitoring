package com.islandpacific.sentinel;

import com.islandpacific.sentinel.security.UserPrincipal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

class RbacMatrixSecurityTest {

    private final UUID tenantA = UUID.randomUUID();
    private final UUID tenantB = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
    }

    private void authenticateUser(String email, String roleName, String tier, UUID tenantId) {
        UserPrincipal.TenantAccess access = new UserPrincipal.TenantAccess(tenantId, "Tenant Alpha", "CLIENT-A", roleName, tier);
        UserPrincipal principal = new UserPrincipal(
                UUID.randomUUID(),
                email,
                "Test User",
                "local",
                true,
                "ACCESS",
                List.of(access)
        );

        List<SimpleGrantedAuthority> authorities = List.of(new SimpleGrantedAuthority("ROLE_" + roleName.toUpperCase().replace("-", "_")));

        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(principal, null, authorities);
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @Test
    void testSuperAdminHasGlobalAccess() {
        authenticateUser("superadmin@company.com", "super-admin", "pro", tenantA);

        var auth = SecurityContextHolder.getContext().getAuthentication();
        assertNotNull(auth);
        assertTrue(auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_SUPER_ADMIN")));
    }

    @Test
    void testTenantAdminScopedToTenantA() {
        authenticateUser("admin@tenantA.com", "tenant-admin", "basic", tenantA);

        var auth = SecurityContextHolder.getContext().getAuthentication();
        UserPrincipal principal = (UserPrincipal) auth.getPrincipal();

        assertEquals(tenantA, principal.getTenants().get(0).getTenantId());
        assertNotEquals(tenantB, principal.getTenants().get(0).getTenantId());
        assertTrue(auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_TENANT_ADMIN")));
    }

    @Test
    void testUserRoleCannotPerformAdminOperations() {
        authenticateUser("user@tenantA.com", "user", "basic", tenantA);

        var auth = SecurityContextHolder.getContext().getAuthentication();
        assertFalse(auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_TENANT_ADMIN")));
        assertFalse(auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_SUPER_ADMIN")));
    }

    @Test
    void testUnauthenticatedAccessFails() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        assertNull(auth);
    }
}
