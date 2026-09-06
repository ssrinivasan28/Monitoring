package com.islandpacific.sentinel.service;

import com.islandpacific.sentinel.entity.Entitlement;
import com.islandpacific.sentinel.entity.TenantSsoConfig;
import com.islandpacific.sentinel.entity.User;
import com.islandpacific.sentinel.entity.UserTenantRole;
import com.islandpacific.sentinel.repository.EntitlementRepository;
import com.islandpacific.sentinel.repository.TenantSsoConfigRepository;
import com.islandpacific.sentinel.repository.UserRepository;
import com.islandpacific.sentinel.repository.UserTenantRoleRepository;
import com.islandpacific.sentinel.security.JwtTokenService;
import com.islandpacific.sentinel.security.SecretProtector;
import com.islandpacific.sentinel.security.UserPrincipal;
import com.islandpacific.sentinel.security.sso.TenantSsoProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class TenantSsoService {

    private final TenantSsoConfigRepository tenantSsoConfigRepository;
    private final UserRepository userRepository;
    private final UserTenantRoleRepository userTenantRoleRepository;
    private final EntitlementRepository entitlementRepository;
    private final JwtTokenService jwtTokenService;
    private final SecretProtector secretProtector;
    private final List<TenantSsoProvider> ssoProviders;
    private final AuthAuditService authAuditService;

    @Autowired
    public TenantSsoService(TenantSsoConfigRepository tenantSsoConfigRepository,
                             UserRepository userRepository,
                             UserTenantRoleRepository userTenantRoleRepository,
                             EntitlementRepository entitlementRepository,
                             JwtTokenService jwtTokenService,
                             SecretProtector secretProtector,
                             List<TenantSsoProvider> ssoProviders,
                             AuthAuditService authAuditService) {
        this.tenantSsoConfigRepository = tenantSsoConfigRepository;
        this.userRepository = userRepository;
        this.userTenantRoleRepository = userTenantRoleRepository;
        this.entitlementRepository = entitlementRepository;
        this.jwtTokenService = jwtTokenService;
        this.secretProtector = secretProtector;
        this.ssoProviders = ssoProviders;
        this.authAuditService = authAuditService;
    }

    public TenantSsoConfig configureSso(UUID tenantId, String ssoType, String issuerUrl, String clientId, String rawClientSecret, String samlMetadataUrl, String samlCertRef, String samlEntityId) {
        TenantSsoConfig config = tenantSsoConfigRepository.findByTenantId(tenantId).orElse(new TenantSsoConfig());
        config.setTenantId(tenantId);
        config.setSsoType(ssoType);
        config.setIssuerUrl(issuerUrl);
        config.setClientId(clientId);
        if (rawClientSecret != null) {
            config.setClientSecretRef(secretProtector.protect(rawClientSecret));
        }
        config.setSamlMetadataUrl(samlMetadataUrl);
        config.setSamlCertificateRef(samlCertRef);
        config.setSamlEntityId(samlEntityId);
        config.setEnabled(true);

        TenantSsoConfig saved = tenantSsoConfigRepository.save(config);
        authAuditService.logEvent("TENANT_SSO_CONFIGURED", null, tenantId, true, "SSO configured: " + ssoType);
        return saved;
    }

    public String authenticateFederatedUser(UUID targetTenantId, String authCodeOrAssertion, String redirectUri) {
        Optional<TenantSsoConfig> configOpt = tenantSsoConfigRepository.findByTenantId(targetTenantId);
        if (configOpt.isEmpty() || !configOpt.get().isEnabled()) {
            authAuditService.logEvent("FEDERATED_LOGIN_FAILED", null, targetTenantId, false, "SSO not configured or disabled");
            throw new IllegalStateException("SSO is not enabled for tenant");
        }

        TenantSsoConfig config = configOpt.get();
        TenantSsoProvider provider = ssoProviders.stream()
                .filter(p -> p.supports(config.getSsoType()))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Unsupported SSO type: " + config.getSsoType()));

        TenantSsoProvider.SsoAuthResult result = provider.authenticate(config, authCodeOrAssertion, redirectUri);
        if (!result.isSuccess()) {
            authAuditService.logEvent("FEDERATED_LOGIN_FAILED", null, targetTenantId, false, result.getErrorMessage());
            throw new IllegalArgumentException("Federated SSO failed: " + result.getErrorMessage());
        }

        Optional<User> userOpt = userRepository.findByEmail(result.getEmail());
        if (userOpt.isEmpty()) {
            authAuditService.logEvent("FEDERATED_LOGIN_FAILED", null, targetTenantId, false, "User not provisioned: " + result.getEmail());
            throw new IllegalStateException("Federated user not provisioned: " + result.getEmail());
        }

        User user = userOpt.get();
        if (!"ACTIVE".equalsIgnoreCase(user.getStatus())) {
            authAuditService.logEvent("FEDERATED_LOGIN_FAILED", user.getId(), targetTenantId, false, "User account disabled");
            throw new IllegalStateException("User account disabled");
        }

        // Strict Tenant Identity Binding Check
        List<UserTenantRole> utrList = userTenantRoleRepository.findByUserId(user.getId());
        boolean hasAccessToTargetTenant = utrList.stream().anyMatch(u -> u.getTenant().getId().equals(targetTenantId));
        if (!hasAccessToTargetTenant) {
            authAuditService.logEvent("FEDERATED_LOGIN_FAILED", user.getId(), targetTenantId, false, "Tenant mismatch / Unauthorized tenant access attempt");
            throw new SecurityException("User has no authorization for the target tenant");
        }

        List<UserPrincipal.TenantAccess> tenantAccessList = new ArrayList<>();
        for (UserTenantRole utr : utrList) {
            String tier = "basic";
            Optional<Entitlement> entOpt = entitlementRepository.findByTenantId(utr.getTenant().getId());
            if (entOpt.isPresent()) tier = entOpt.get().getTier();

            tenantAccessList.add(new UserPrincipal.TenantAccess(
                    utr.getTenant().getId(),
                    utr.getTenant().getName(),
                    utr.getTenant().getClientInstanceId(),
                    utr.getRole().getKey(),
                    tier
            ));
        }

        UserPrincipal principal = new UserPrincipal(
                user.getId(),
                user.getEmail(),
                user.getDisplayName(),
                "federated",
                true,
                "ACCESS",
                tenantAccessList
        );

        authAuditService.logEvent("FEDERATED_LOGIN_SUCCESS", user.getId(), targetTenantId, true, "Federated SSO success");
        return jwtTokenService.createAccessToken(principal);
    }
}
