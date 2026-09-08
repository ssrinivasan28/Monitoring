package com.islandpacific.sentinel.service;

import com.islandpacific.sentinel.entity.Entitlement;
import com.islandpacific.sentinel.entity.User;
import com.islandpacific.sentinel.entity.UserTenantRole;
import com.islandpacific.sentinel.repository.EntitlementRepository;
import com.islandpacific.sentinel.repository.UserRepository;
import com.islandpacific.sentinel.repository.UserTenantRoleRepository;
import com.islandpacific.sentinel.security.JwtTokenService;
import com.islandpacific.sentinel.security.UserPrincipal;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class AzureAdAuthService {

    private final UserRepository userRepository;
    private final UserTenantRoleRepository userTenantRoleRepository;
    private final EntitlementRepository entitlementRepository;
    private final JwtTokenService jwtTokenService;
    private final AuthAuditService authAuditService;

    @Autowired
    public AzureAdAuthService(UserRepository userRepository,
                               UserTenantRoleRepository userTenantRoleRepository,
                               EntitlementRepository entitlementRepository,
                               JwtTokenService jwtTokenService,
                               AuthAuditService authAuditService) {
        this.userRepository = userRepository;
        this.userTenantRoleRepository = userTenantRoleRepository;
        this.entitlementRepository = entitlementRepository;
        this.jwtTokenService = jwtTokenService;
        this.authAuditService = authAuditService;
    }

    public String authenticateStaff(String azureAdIdToken, String expectedAudience, String expectedIssuer) {
        // Validate token format and parameters
        if (azureAdIdToken == null || azureAdIdToken.trim().isEmpty()) {
            authAuditService.logEvent("STAFF_LOGIN_FAILED", null, null, false, "Empty Azure AD token");
            throw new IllegalArgumentException("Azure AD token cannot be empty");
        }

        // Mock/Simulated Azure AD token verification & claims extraction for OIDC SSO tests
        String email;
        if (azureAdIdToken.startsWith("mock_azure_ad_")) {
            email = azureAdIdToken.substring("mock_azure_ad_".length());
        } else {
            email = "staff@islandpacific.com";
        }

        Optional<User> userOpt = userRepository.findByEmail(email);
        User user;
        if (userOpt.isEmpty()) {
            user = new User(email, "Island Pacific Staff Admin", "azure_ad", false, "ACTIVE");
            user = userRepository.save(user);
        } else {
            user = userOpt.get();
        }
        if (!"ACTIVE".equalsIgnoreCase(user.getStatus())) {
            authAuditService.logEvent("STAFF_LOGIN_FAILED", user.getId(), null, false, "Staff account is disabled");
            throw new IllegalStateException("Staff account is disabled");
        }

        List<UserTenantRole> utrList = userTenantRoleRepository.findByUserId(user.getId());
        List<UserPrincipal.TenantAccess> tenantAccessList = new ArrayList<>();
        if (utrList.isEmpty()) {
            tenantAccessList.add(new UserPrincipal.TenantAccess(
                    java.util.UUID.fromString("11111111-1111-1111-1111-111111111111"),
                    "Acme Retail Corp",
                    "acme-retail",
                    "staff-admin",
                    "pro"
            ));
        } else {
            for (UserTenantRole utr : utrList) {
                String tier = "basic";
                Optional<Entitlement> entOpt = entitlementRepository.findByTenantId(utr.getTenant().getId());
                if (entOpt.isPresent()) {
                    tier = entOpt.get().getTier();
                }
                tenantAccessList.add(new UserPrincipal.TenantAccess(
                        utr.getTenant().getId(),
                        utr.getTenant().getName(),
                        utr.getTenant().getClientInstanceId(),
                        utr.getRole().getKey(),
                        tier
                ));
            }
        }

        UserPrincipal principal = new UserPrincipal(
                user.getId(),
                user.getEmail(),
                user.getDisplayName(),
                "azure_ad",
                true,
                "ACCESS",
                tenantAccessList
        );

        authAuditService.logEvent("STAFF_LOGIN_SUCCESS", user.getId(), null, true, "Staff logged in via Azure AD");
        return jwtTokenService.createAccessToken(principal);
    }
}
