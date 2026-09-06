package com.islandpacific.sentinel.controller;

import com.islandpacific.sentinel.entity.TenantSsoConfig;
import com.islandpacific.sentinel.security.TotpService;
import com.islandpacific.sentinel.security.UserPrincipal;
import com.islandpacific.sentinel.service.AzureAdAuthService;
import com.islandpacific.sentinel.service.CustomerAuthService;
import com.islandpacific.sentinel.service.TenantSsoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AzureAdAuthService azureAdAuthService;
    private final CustomerAuthService customerAuthService;
    private final TenantSsoService tenantSsoService;
    private final TotpService totpService;

    @Autowired
    public AuthController(AzureAdAuthService azureAdAuthService,
                          CustomerAuthService customerAuthService,
                          TenantSsoService tenantSsoService,
                          TotpService totpService) {
        this.azureAdAuthService = azureAdAuthService;
        this.customerAuthService = customerAuthService;
        this.tenantSsoService = tenantSsoService;
        this.totpService = totpService;
    }

    @PostMapping("/staff/azure-ad")
    public ResponseEntity<Map<String, Object>> staffAzureAdLogin(@RequestBody Map<String, String> body) {
        String token = body.get("token");
        String accessToken = azureAdAuthService.authenticateStaff(token, null, null);
        Map<String, Object> response = new HashMap<>();
        response.put("accessToken", accessToken);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> loginPassword(@RequestBody Map<String, String> body) {
        String email = body.get("email");
        String password = body.get("password");
        String mfaPendingToken = customerAuthService.loginPassword(email, password);
        Map<String, Object> response = new HashMap<>();
        response.put("mfaPendingToken", mfaPendingToken);
        response.put("mfaRequired", true);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/mfa/verify")
    public ResponseEntity<Map<String, Object>> verifyMfa(@RequestBody Map<String, String> body) {
        String mfaPendingToken = body.get("mfaPendingToken");
        String code = body.get("code");
        Map<String, Object> result = customerAuthService.verifyMfa(mfaPendingToken, code);
        return ResponseEntity.ok(result);
    }

    @PostMapping("/refresh")
    public ResponseEntity<Map<String, String>> refreshToken(@RequestBody Map<String, String> body) {
        String refreshToken = body.get("refreshToken");
        Map<String, String> tokens = customerAuthService.refreshToken(refreshToken);
        return ResponseEntity.ok(tokens);
    }

    @PostMapping("/invite")
    public ResponseEntity<Map<String, String>> inviteUser(@RequestBody Map<String, String> body) {
        UUID tenantId = UUID.fromString(body.get("tenantId"));
        String email = body.get("email");
        String displayName = body.get("displayName");
        String role = body.get("role");

        String rawInviteToken = customerAuthService.inviteCustomerUser(tenantId, email, displayName, role);
        String rawTotpSecret = totpService.generateRawSecret();
        String qrUrl = totpService.getQrCodeUrl(email, rawTotpSecret);

        Map<String, String> response = new HashMap<>();
        response.put("inviteToken", rawInviteToken);
        response.put("totpSecret", rawTotpSecret);
        response.put("qrCodeUrl", qrUrl);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/activate")
    public ResponseEntity<Map<String, String>> activateUser(@RequestBody Map<String, String> body) {
        String inviteToken = body.get("inviteToken");
        String password = body.get("password");
        String totpCode = body.get("totpCode");
        String totpSecret = body.get("totpSecret");

        String msg = customerAuthService.activateCustomerUser(inviteToken, password, totpCode, totpSecret);
        Map<String, String> response = new HashMap<>();
        response.put("message", msg);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/reset-password/request")
    public ResponseEntity<Map<String, String>> requestPasswordReset(@RequestBody Map<String, String> body) {
        String email = body.get("email");
        customerAuthService.requestPasswordReset(email);
        Map<String, String> response = new HashMap<>();
        response.put("message", "If an account with that email exists, password reset instructions have been sent.");
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(response); // 202 Accepted Anti-enumeration
    }

    @PostMapping("/reset-password/confirm")
    public ResponseEntity<Map<String, String>> confirmPasswordReset(@RequestBody Map<String, String> body) {
        String resetToken = body.get("resetToken");
        String newPassword = body.get("newPassword");
        customerAuthService.confirmPasswordReset(resetToken, newPassword);
        Map<String, String> response = new HashMap<>();
        response.put("message", "Password has been successfully reset.");
        return ResponseEntity.ok(response);
    }

    @PostMapping("/users/{userId}/status")
    public ResponseEntity<Map<String, String>> setUserStatus(@PathVariable UUID userId, @RequestBody Map<String, String> body) {
        String status = body.get("status");
        customerAuthService.setUserStatus(userId, status);
        Map<String, String> response = new HashMap<>();
        response.put("message", "User status updated to " + status);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/tenant-sso")
    public ResponseEntity<TenantSsoConfig> configureTenantSso(@RequestBody Map<String, String> body) {
        UUID tenantId = UUID.fromString(body.get("tenantId"));
        String ssoType = body.get("ssoType");
        String issuerUrl = body.get("issuerUrl");
        String clientId = body.get("clientId");
        String clientSecret = body.get("clientSecret");
        String samlMetadataUrl = body.get("samlMetadataUrl");
        String samlCertRef = body.get("samlCertRef");
        String samlEntityId = body.get("samlEntityId");

        TenantSsoConfig config = tenantSsoService.configureSso(tenantId, ssoType, issuerUrl, clientId, clientSecret, samlMetadataUrl, samlCertRef, samlEntityId);
        return ResponseEntity.ok(config);
    }

    @PostMapping("/federated/login")
    public ResponseEntity<Map<String, String>> federatedLogin(@RequestBody Map<String, String> body) {
        UUID tenantId = UUID.fromString(body.get("tenantId"));
        String codeOrAssertion = body.get("code");
        String redirectUri = body.get("redirectUri");

        String accessToken = tenantSsoService.authenticateFederatedUser(tenantId, codeOrAssertion, redirectUri);
        Map<String, String> response = new HashMap<>();
        response.put("accessToken", accessToken);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/me")
    public ResponseEntity<UserPrincipal> getCurrentSession(@AuthenticationPrincipal UserPrincipal principal) {
        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        return ResponseEntity.ok(principal);
    }
}
