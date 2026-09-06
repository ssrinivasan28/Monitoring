package com.islandpacific.sentinel;

import com.islandpacific.sentinel.entity.*;
import com.islandpacific.sentinel.repository.*;
import com.islandpacific.sentinel.security.*;
import com.islandpacific.sentinel.service.AzureAdAuthService;
import com.islandpacific.sentinel.service.CustomerAuthService;
import com.islandpacific.sentinel.service.TenantSsoService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
class AuthNegativeSecurityIntegrationTest extends AbstractIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private TenantRepository tenantRepository;
    @Autowired private RoleRepository roleRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private UserTenantRoleRepository userTenantRoleRepository;
    @Autowired private UserTokenRepository userTokenRepository;
    @Autowired private AuthAuditLogRepository authAuditLogRepository;

    @Autowired private CustomerAuthService customerAuthService;
    @Autowired private AzureAdAuthService azureAdAuthService;
    @Autowired private TenantSsoService tenantSsoService;
    @Autowired private TotpService totpService;
    @Autowired private JwtTokenService jwtTokenService;
    @Autowired private UserAuthCache userAuthCache;

    private Tenant tenantA;
    private Tenant tenantB;
    private Role adminRole;

    @BeforeEach
    void setupTestData() {
        if (jdbcTemplate != null) {
            jdbcTemplate.execute("TRUNCATE TABLE user_tokens, auth_audit_log, tenant_sso_configs, user_tenant_roles, users, roles, tenants CASCADE");
        }
        tenantA = tenantRepository.save(new Tenant("Tenant Alpha", "CLIENT-ALPHA"));
        tenantB = tenantRepository.save(new Tenant("Tenant Beta", "CLIENT-BETA"));

        adminRole = roleRepository.save(new Role("ADMIN"));
    }

    @Test
    void mfaBypassAttempt_fails() throws Exception {
        User customer = new User("mfa@customer.com", "MFA User", "local");
        customer.setStatus("ACTIVE");
        customer = userRepository.save(customer);

        String mfaPendingToken = jwtTokenService.createMfaPendingToken(customer.getId(), customer.getEmail());

        mockMvc.perform(get("/api/v1/auth/me")
                        .header("Authorization", "Bearer " + mfaPendingToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void replayedInvitationToken_fails() {
        String rawInviteToken = customerAuthService.inviteCustomerUser(tenantA.getId(), "invited@customer.com", "Invited User", "ADMIN");
        String rawTotpSecret = totpService.generateRawSecret();

        long window = System.currentTimeMillis() / 1000L / 30L;
        String totpCode = String.format("%06d", totpService.generateCodeForWindow(rawTotpSecret, window));

        // First activation: Success
        assertNotNull(customerAuthService.activateCustomerUser(rawInviteToken, "Password123!", totpCode, rawTotpSecret));

        // Replay activation with same token: Must fail
        assertThrows(IllegalArgumentException.class, () ->
                customerAuthService.activateCustomerUser(rawInviteToken, "Password123!", totpCode, rawTotpSecret));
    }

    @Test
    void replayedRefreshToken_revokesTokenFamily() {
        String rawInviteToken = customerAuthService.inviteCustomerUser(tenantA.getId(), "refresh@customer.com", "Refresh User", "ADMIN");
        String rawTotpSecret = totpService.generateRawSecret();
        long window = System.currentTimeMillis() / 1000L / 30L;
        String totpCode = String.format("%06d", totpService.generateCodeForWindow(rawTotpSecret, window));
        customerAuthService.activateCustomerUser(rawInviteToken, "Password123!", totpCode, rawTotpSecret);

        String mfaToken = customerAuthService.loginPassword("refresh@customer.com", "Password123!");
        var tokens = customerAuthService.verifyMfa(mfaToken, totpCode);
        String rawRefreshToken1 = (String) tokens.get("refreshToken");

        // First refresh: Success
        var refreshedTokens = customerAuthService.refreshToken(rawRefreshToken1);
        String rawRefreshToken2 = refreshedTokens.get("refreshToken");
        assertNotNull(rawRefreshToken2);

        // Replay attack with rawRefreshToken1: Must throw SecurityException & revoke family
        assertThrows(SecurityException.class, () -> customerAuthService.refreshToken(rawRefreshToken1));

        // Subsequent attempt with rawRefreshToken2 must also fail because family was revoked!
        assertThrows(SecurityException.class, () -> customerAuthService.refreshToken(rawRefreshToken2));
    }

    @Test
    void disabledUser_blocksRequests() throws Exception {
        User customer = new User("disabled@customer.com", "Disabled User", "local");
        customer.setStatus("ACTIVE");
        customer = userRepository.save(customer);

        userTenantRoleRepository.save(new UserTenantRole(customer, tenantA, adminRole));
        userRepository.flush();
        userTenantRoleRepository.flush();

        List<UserPrincipal.TenantAccess> tenantAccess = List.of(
                new UserPrincipal.TenantAccess(tenantA.getId(), tenantA.getName(), tenantA.getClientInstanceId(), "ADMIN", "basic")
        );
        UserPrincipal principal = new UserPrincipal(customer.getId(), customer.getEmail(), customer.getDisplayName(), "local", true, "ACCESS", tenantAccess);
        String accessToken = jwtTokenService.createAccessToken(principal);

        // Active request succeeds
        mockMvc.perform(get("/api/v1/auth/me")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk());

        // Disable user
        customerAuthService.setUserStatus(customer.getId(), "DISABLED");

        // Subsequent request fails
        mockMvc.perform(get("/api/v1/auth/me")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void antiEnumerationPasswordReset_returnsSuccessForNonExistentUser() throws Exception {
        mockMvc.perform(post("/api/v1/auth/reset-password/request")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"nonexistent@customer.com\"}"))
                .andExpect(status().isAccepted());
    }

    @Test
    void unprovisionedAzureAdUser_fails() {
        assertThrows(IllegalStateException.class, () ->
                azureAdAuthService.authenticateStaff("mock_azure_ad_unprovisioned@staff.com", null, null));
    }

    @Test
    void federatedSsoTenantMismatch_fails() {
        User user = new User("sso@customer.com", "SSO User", "federated");
        user.setStatus("ACTIVE");
        user = userRepository.save(user);

        // User only belongs to Tenant A
        userTenantRoleRepository.save(new UserTenantRole(user, tenantA, adminRole));

        // Configure SSO for Tenant B
        tenantSsoService.configureSso(tenantB.getId(), "oidc", "https://idp.tenantb.com", "client-b", "secret-b", null, null, null);

        // Attempt login into Tenant B must be rejected due to tenant mismatch
        assertThrows(SecurityException.class, () ->
                tenantSsoService.authenticateFederatedUser(tenantB.getId(), "mock_oidc_sso@customer.com", "http://localhost/callback"));
    }

    @Test
    void totpSecret_encryptedAtRest() {
        String rawInviteToken = customerAuthService.inviteCustomerUser(tenantA.getId(), "dpapi@customer.com", "DPAPI User", "ADMIN");
        String rawTotpSecret = totpService.generateRawSecret();
        long window = System.currentTimeMillis() / 1000L / 30L;
        String totpCode = String.format("%06d", totpService.generateCodeForWindow(rawTotpSecret, window));
        customerAuthService.activateCustomerUser(rawInviteToken, "Password123!", totpCode, rawTotpSecret);

        User user = userRepository.findByEmail("dpapi@customer.com").orElseThrow();
        assertNotNull(user.getEncryptedTotpSecret());
        assertTrue(user.getEncryptedTotpSecret().startsWith("DPAPI("));
        assertNotEquals(rawTotpSecret, user.getEncryptedTotpSecret());
    }

    @Test
    void auditLog_sanitizesPasswordsAndTokens() {
        List<AuthAuditLog> logs = authAuditLogRepository.findAll();
        assertNotNull(logs);
        for (AuthAuditLog log : logs) {
            if (log.getDetail() != null) {
                assertFalse(log.getDetail().contains("Password123!"));
                assertFalse(log.getDetail().contains("totpSecret="));
            }
        }
    }
}
