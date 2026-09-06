package com.islandpacific.sentinel.service;

import com.islandpacific.sentinel.entity.*;
import com.islandpacific.sentinel.repository.*;
import com.islandpacific.sentinel.security.*;
import io.jsonwebtoken.Claims;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;

@Service
public class CustomerAuthService {

    private final UserRepository userRepository;
    private final TenantRepository tenantRepository;
    private final RoleRepository roleRepository;
    private final UserTenantRoleRepository userTenantRoleRepository;
    private final EntitlementRepository entitlementRepository;
    private final UserTokenRepository userTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final TotpService totpService;
    private final JwtTokenService jwtTokenService;
    private final UserAuthCache userAuthCache;
    private final AuthAuditService authAuditService;

    @Autowired
    public CustomerAuthService(UserRepository userRepository,
                               TenantRepository tenantRepository,
                               RoleRepository roleRepository,
                               UserTenantRoleRepository userTenantRoleRepository,
                               EntitlementRepository entitlementRepository,
                               UserTokenRepository userTokenRepository,
                               PasswordEncoder passwordEncoder,
                               TotpService totpService,
                               JwtTokenService jwtTokenService,
                               UserAuthCache userAuthCache,
                               AuthAuditService authAuditService) {
        this.userRepository = userRepository;
        this.tenantRepository = tenantRepository;
        this.roleRepository = roleRepository;
        this.userTenantRoleRepository = userTenantRoleRepository;
        this.entitlementRepository = entitlementRepository;
        this.userTokenRepository = userTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.totpService = totpService;
        this.jwtTokenService = jwtTokenService;
        this.userAuthCache = userAuthCache;
        this.authAuditService = authAuditService;
    }

    @Transactional
    public String inviteCustomerUser(UUID tenantId, String email, String displayName, String roleKey) {
        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new IllegalArgumentException("Tenant not found: " + tenantId));
        Role role = roleRepository.findByKey(roleKey)
                .orElseThrow(() -> new IllegalArgumentException("Role not found: " + roleKey));

        User user = userRepository.findByEmail(email).orElseGet(() -> {
            User newUser = new User(email, displayName, "local");
            newUser.setStatus("INVITED");
            return userRepository.save(newUser);
        });

        UserTenantRoleId utrId = new UserTenantRoleId(user.getId(), tenant.getId(), role.getId());
        if (!userTenantRoleRepository.existsById(utrId)) {
            userTenantRoleRepository.save(new UserTenantRole(user, tenant, role));
        }

        String rawToken = TokenHasher.generateRawToken();
        String tokenHash = TokenHasher.hashToken(rawToken);
        Instant expiry = Instant.now().plusSeconds(48 * 3600); // 48h TTL

        userTokenRepository.deleteByUserIdAndTokenType(user.getId(), "INVITATION");
        userTokenRepository.save(new UserToken(user.getId(), "INVITATION", tokenHash, expiry));

        authAuditService.logEvent("USER_INVITED", user.getId(), tenantId, true, "Invited email: " + email);
        return rawToken;
    }

    @Transactional
    public String activateCustomerUser(String rawInviteToken, String newPassword, String totpCode, String rawTotpSecret) {
        String tokenHash = TokenHasher.hashToken(rawInviteToken);
        UserToken userToken = userTokenRepository.findByTokenHash(tokenHash)
                .orElseThrow(() -> new IllegalArgumentException("Invalid invitation token"));

        if (!"INVITATION".equals(userToken.getTokenType()) || userToken.getConsumedAt() != null) {
            authAuditService.logEvent("ACTIVATION_FAILED", userToken.getUserId(), null, false, "Token replayed or invalid");
            throw new IllegalArgumentException("Invitation token is invalid or already consumed");
        }
        if (Instant.now().isAfter(userToken.getExpiresAt())) {
            authAuditService.logEvent("ACTIVATION_FAILED", userToken.getUserId(), null, false, "Token expired");
            throw new IllegalArgumentException("Invitation token has expired");
        }

        User user = userRepository.findById(userToken.getUserId())
                .orElseThrow(() -> new IllegalStateException("User not found"));

        if (!totpService.verifyCode(rawTotpSecret, totpCode)) {
            authAuditService.logEvent("ACTIVATION_FAILED", user.getId(), null, false, "Invalid TOTP code during activation");
            throw new IllegalArgumentException("Invalid TOTP verification code");
        }

        user.setPasswordHash(passwordEncoder.encode(newPassword));
        user.setEncryptedTotpSecret(totpService.encryptSecret(rawTotpSecret));
        user.setMfaEnabled(true);
        user.setStatus("ACTIVE");
        userRepository.save(user);

        userToken.setConsumedAt(Instant.now());
        userTokenRepository.save(userToken);
        userAuthCache.invalidate(user.getId());

        authAuditService.logEvent("USER_ACTIVATED", user.getId(), null, true, "Customer account activated");
        return "Account successfully activated";
    }

    @Transactional(readOnly = true)
    public String loginPassword(String email, String password) {
        Optional<User> userOpt = userRepository.findByEmail(email);
        if (userOpt.isEmpty()) {
            authAuditService.logEvent("LOGIN_FAILED", null, null, false, "Invalid credentials for email: " + email);
            throw new IllegalArgumentException("Invalid email or password");
        }

        User user = userOpt.get();
        if (!"ACTIVE".equalsIgnoreCase(user.getStatus())) {
            authAuditService.logEvent("LOGIN_FAILED", user.getId(), null, false, "Account disabled or unactivated");
            throw new IllegalStateException("User account is disabled or not activated");
        }

        if (user.getPasswordHash() == null || !passwordEncoder.matches(password, user.getPasswordHash())) {
            authAuditService.logEvent("LOGIN_FAILED", user.getId(), null, false, "Invalid password");
            throw new IllegalArgumentException("Invalid email or password");
        }

        authAuditService.logEvent("LOGIN_PASSWORD_SUCCESS", user.getId(), null, true, "Password step completed");
        return jwtTokenService.createMfaPendingToken(user.getId(), user.getEmail());
    }

    @Transactional
    public Map<String, Object> verifyMfa(String mfaPendingToken, String totpCode) {
        Claims claims = jwtTokenService.parseAndValidateToken(mfaPendingToken);
        if (!jwtTokenService.isMfaPendingToken(claims)) {
            throw new IllegalArgumentException("Token is not an MFA pending token");
        }

        UUID userId = UUID.fromString(claims.getSubject());
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalStateException("User not found"));

        if (!"ACTIVE".equalsIgnoreCase(user.getStatus())) {
            authAuditService.logEvent("MFA_VERIFY_FAILED", userId, null, false, "User disabled");
            throw new IllegalStateException("User account disabled");
        }

        if (!totpService.verifyCode(user.getEncryptedTotpSecret(), totpCode)) {
            authAuditService.logEvent("MFA_VERIFY_FAILED", userId, null, false, "Invalid TOTP code");
            throw new IllegalArgumentException("Invalid TOTP code");
        }

        List<UserTenantRole> utrList = userTenantRoleRepository.findByUserId(userId);
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
                user.getSource(),
                true,
                "ACCESS",
                tenantAccessList
        );

        String accessToken = jwtTokenService.createAccessToken(principal);

        // Issue single-use refresh token with family rotation
        String rawRefreshToken = TokenHasher.generateRawToken();
        String tokenHash = TokenHasher.hashToken(rawRefreshToken);
        UUID familyId = UUID.randomUUID();
        Instant refreshExpiry = Instant.now().plusSeconds(7 * 24 * 3600); // 7 days

        UserToken refreshToken = new UserToken(user.getId(), "REFRESH", tokenHash, refreshExpiry);
        refreshToken.setFamilyId(familyId);
        userTokenRepository.save(refreshToken);

        authAuditService.logEvent("MFA_VERIFY_SUCCESS", userId, null, true, "Full session established");

        Map<String, Object> result = new HashMap<>();
        result.put("accessToken", accessToken);
        result.put("refreshToken", rawRefreshToken);
        result.put("principal", principal);
        return result;
    }

    @Transactional(noRollbackFor = SecurityException.class)
    public Map<String, String> refreshToken(String rawRefreshToken) {
        String tokenHash = TokenHasher.hashToken(rawRefreshToken);
        Optional<UserToken> tokenOpt = userTokenRepository.findByTokenHash(tokenHash);

        if (tokenOpt.isEmpty()) {
            throw new IllegalArgumentException("Invalid refresh token");
        }

        UserToken oldToken = tokenOpt.get();

        // REPLAY ATTACK DETECTION!
        if (oldToken.getConsumedAt() != null) {
            // Revoke all tokens in family!
            if (oldToken.getFamilyId() != null) {
                List<UserToken> familyTokens = userTokenRepository.findByFamilyId(oldToken.getFamilyId());
                for (UserToken t : familyTokens) {
                    t.setConsumedAt(Instant.now());
                }
                userTokenRepository.saveAll(familyTokens);
                userTokenRepository.flush();
            }
            authAuditService.logEvent("REFRESH_REPLAY_ATTACK_DETECTED", oldToken.getUserId(), null, false, "Revoked entire token family");
            throw new SecurityException("Refresh token reuse detected! All sessions revoked.");
        }

        if (Instant.now().isAfter(oldToken.getExpiresAt())) {
            throw new IllegalArgumentException("Refresh token expired");
        }

        User user = userRepository.findById(oldToken.getUserId())
                .orElseThrow(() -> new IllegalStateException("User not found"));

        if (!"ACTIVE".equalsIgnoreCase(user.getStatus())) {
            throw new IllegalStateException("User account disabled");
        }

        oldToken.setConsumedAt(Instant.now());
        userTokenRepository.save(oldToken);

        // Issue new rotated refresh token in family
        String newRawRefreshToken = TokenHasher.generateRawToken();
        String newTokenHash = TokenHasher.hashToken(newRawRefreshToken);
        UserToken newToken = new UserToken(user.getId(), "REFRESH", newTokenHash, Instant.now().plusSeconds(7 * 24 * 3600));
        newToken.setFamilyId(oldToken.getFamilyId());
        newToken.setParentId(oldToken.getId());
        userTokenRepository.save(newToken);

        List<UserTenantRole> utrList = userTenantRoleRepository.findByUserId(user.getId());
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
                user.getSource(),
                true,
                "ACCESS",
                tenantAccessList
        );

        String newAccessToken = jwtTokenService.createAccessToken(principal);
        Map<String, String> result = new HashMap<>();
        result.put("accessToken", newAccessToken);
        result.put("refreshToken", newRawRefreshToken);
        return result;
    }

    @Transactional
    public boolean requestPasswordReset(String email) {
        // Anti-enumeration: returns true regardless of existence
        Optional<User> userOpt = userRepository.findByEmail(email);
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            String rawToken = TokenHasher.generateRawToken();
            String tokenHash = TokenHasher.hashToken(rawToken);
            Instant expiry = Instant.now().plusSeconds(3600); // 1h TTL

            userTokenRepository.deleteByUserIdAndTokenType(user.getId(), "PASSWORD_RESET");
            userTokenRepository.save(new UserToken(user.getId(), "PASSWORD_RESET", tokenHash, expiry));
            authAuditService.logEvent("PASSWORD_RESET_REQUESTED", user.getId(), null, true, "Reset requested");
        } else {
            authAuditService.logEvent("PASSWORD_RESET_REQUESTED", null, null, false, "Anti-enumeration request");
        }
        return true;
    }

    @Transactional
    public boolean confirmPasswordReset(String rawResetToken, String newPassword) {
        String tokenHash = TokenHasher.hashToken(rawResetToken);
        UserToken userToken = userTokenRepository.findByTokenHash(tokenHash)
                .orElseThrow(() -> new IllegalArgumentException("Invalid password reset token"));

        if (!"PASSWORD_RESET".equals(userToken.getTokenType()) || userToken.getConsumedAt() != null) {
            authAuditService.logEvent("PASSWORD_RESET_FAILED", userToken.getUserId(), null, false, "Token replayed or invalid");
            throw new IllegalArgumentException("Token is invalid or already consumed");
        }
        if (Instant.now().isAfter(userToken.getExpiresAt())) {
            authAuditService.logEvent("PASSWORD_RESET_FAILED", userToken.getUserId(), null, false, "Token expired");
            throw new IllegalArgumentException("Token expired");
        }

        User user = userRepository.findById(userToken.getUserId())
                .orElseThrow(() -> new IllegalStateException("User not found"));

        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        userToken.setConsumedAt(Instant.now());
        userTokenRepository.save(userToken);
        userAuthCache.invalidate(user.getId());

        authAuditService.logEvent("PASSWORD_RESET_SUCCESS", user.getId(), null, true, "Password reset completed");
        return true;
    }

    @Transactional
    public void setUserStatus(UUID userId, String status) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));
        user.setStatus(status);
        userRepository.save(user);
        userAuthCache.invalidate(userId);
        authAuditService.logEvent("USER_STATUS_CHANGED", userId, null, true, "Status changed to: " + status);
    }
}
