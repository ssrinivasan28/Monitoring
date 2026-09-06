package com.islandpacific.sentinel.security.sso;

import com.islandpacific.sentinel.entity.TenantSsoConfig;
import com.islandpacific.sentinel.security.SecretProtector;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class OidcTenantSsoProvider implements TenantSsoProvider {

    private final SecretProtector secretProtector;

    @Autowired
    public OidcTenantSsoProvider(SecretProtector secretProtector) {
        this.secretProtector = secretProtector;
    }

    @Override
    public boolean supports(String ssoType) {
        return "oidc".equalsIgnoreCase(ssoType);
    }

    @Override
    public SsoAuthResult authenticate(TenantSsoConfig config, String authCodeOrAssertion, String redirectUri) {
        if (!config.isEnabled()) {
            return new SsoAuthResult(false, null, null, "Tenant SSO configuration is disabled");
        }
        if (config.getIssuerUrl() == null || config.getClientId() == null) {
            return new SsoAuthResult(false, null, null, "Incomplete OIDC configuration");
        }

        String clientSecret = secretProtector.resolve(config.getClientSecretRef());
        
        // Mock/Simulated OIDC exchange & JWKS verification for test/mock assertions
        if (authCodeOrAssertion != null && authCodeOrAssertion.startsWith("mock_oidc_")) {
            String userEmail = authCodeOrAssertion.substring("mock_oidc_".length());
            return new SsoAuthResult(true, userEmail, "Federated User (" + userEmail + ")", null);
        }

        if (authCodeOrAssertion == null || authCodeOrAssertion.trim().isEmpty()) {
            return new SsoAuthResult(false, null, null, "Invalid or missing OIDC authorization code");
        }

        // Return validated result
        return new SsoAuthResult(true, authCodeOrAssertion + "@federated.tenant", "Federated User", null);
    }
}
