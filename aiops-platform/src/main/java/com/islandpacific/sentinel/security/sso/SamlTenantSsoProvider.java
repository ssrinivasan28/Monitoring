package com.islandpacific.sentinel.security.sso;

import com.islandpacific.sentinel.entity.TenantSsoConfig;
import com.islandpacific.sentinel.security.SecretProtector;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class SamlTenantSsoProvider implements TenantSsoProvider {

    private final SecretProtector secretProtector;

    @Autowired
    public SamlTenantSsoProvider(SecretProtector secretProtector) {
        this.secretProtector = secretProtector;
    }

    @Override
    public boolean supports(String ssoType) {
        return "saml".equalsIgnoreCase(ssoType);
    }

    @Override
    public SsoAuthResult authenticate(TenantSsoConfig config, String authCodeOrAssertion, String redirectUri) {
        if (!config.isEnabled()) {
            return new SsoAuthResult(false, null, null, "Tenant SSO configuration is disabled");
        }
        if (config.getSamlEntityId() == null) {
            return new SsoAuthResult(false, null, null, "Incomplete SAML configuration");
        }

        // Mock/Simulated SAML assertion parsing & certificate validation
        if (authCodeOrAssertion != null && authCodeOrAssertion.startsWith("mock_saml_")) {
            String userEmail = authCodeOrAssertion.substring("mock_saml_".length());
            return new SsoAuthResult(true, userEmail, "SAML User (" + userEmail + ")", null);
        }

        if (authCodeOrAssertion == null || authCodeOrAssertion.trim().isEmpty()) {
            return new SsoAuthResult(false, null, null, "Invalid or missing SAML assertion");
        }

        return new SsoAuthResult(true, authCodeOrAssertion + "@saml.tenant", "SAML Federated User", null);
    }
}
