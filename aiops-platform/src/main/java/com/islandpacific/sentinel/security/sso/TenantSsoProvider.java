package com.islandpacific.sentinel.security.sso;

import com.islandpacific.sentinel.entity.TenantSsoConfig;
import com.islandpacific.sentinel.entity.User;

public interface TenantSsoProvider {
    
    boolean supports(String ssoType);

    SsoAuthResult authenticate(TenantSsoConfig config, String authCodeOrAssertion, String redirectUri);

    class SsoAuthResult {
        private final boolean success;
        private final String email;
        private final String displayName;
        private final String errorMessage;

        public SsoAuthResult(boolean success, String email, String displayName, String errorMessage) {
            this.success = success;
            this.email = email;
            this.displayName = displayName;
            this.errorMessage = errorMessage;
        }

        public boolean isSuccess() { return success; }
        public String getEmail() { return email; }
        public String getDisplayName() { return displayName; }
        public String getErrorMessage() { return errorMessage; }
    }
}
