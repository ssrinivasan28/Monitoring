package com.islandpacific.sentinel.security;

import com.islandpacific.sentinel.entity.Entitlement;
import com.islandpacific.sentinel.entity.User;
import com.islandpacific.sentinel.entity.UserTenantRole;
import com.islandpacific.sentinel.repository.EntitlementRepository;
import com.islandpacific.sentinel.repository.UserRepository;
import com.islandpacific.sentinel.repository.UserTenantRoleRepository;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenService jwtTokenService;
    private final UserAuthCache userAuthCache;
    private final UserRepository userRepository;
    private final UserTenantRoleRepository userTenantRoleRepository;
    private final EntitlementRepository entitlementRepository;

    @Autowired
    public JwtAuthenticationFilter(JwtTokenService jwtTokenService,
                                   UserAuthCache userAuthCache,
                                   UserRepository userRepository,
                                   UserTenantRoleRepository userTenantRoleRepository,
                                   EntitlementRepository entitlementRepository) {
        this.jwtTokenService = jwtTokenService;
        this.userAuthCache = userAuthCache;
        this.userRepository = userRepository;
        this.userTenantRoleRepository = userTenantRoleRepository;
        this.entitlementRepository = entitlementRepository;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = authHeader.substring(7).trim();
        try {
            Claims claims = jwtTokenService.parseAndValidateToken(token);
            UUID userId = UUID.fromString(claims.getSubject());

            // 1. Server-side user active check
            if (!userAuthCache.isUserActive(userId)) {
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "User account is disabled or inactive");
                return;
            }

            // 2. Strict MFA_PENDING isolation check
            boolean isMfaPending = jwtTokenService.isMfaPendingToken(claims);
            String requestUri = request.getRequestURI();
            if (isMfaPending) {
                if (!requestUri.endsWith("/api/v1/auth/mfa/verify")) {
                    response.sendError(HttpServletResponse.SC_FORBIDDEN, "MFA verification required before accessing resources");
                    return;
                }
            }

            // 3. Resolve user entity and multi-tenant access from DB
            Optional<User> userOpt = userRepository.findById(userId);
            if (userOpt.isEmpty() || !"ACTIVE".equalsIgnoreCase(userOpt.get().getStatus())) {
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "User account is disabled");
                return;
            }

            User user = userOpt.get();
            List<UserTenantRole> utrList = userTenantRoleRepository.findByUserId(userId);
            List<UserPrincipal.TenantAccess> tenantAccessList = new ArrayList<>();
            List<SimpleGrantedAuthority> authorities = new ArrayList<>();

            for (UserTenantRole utr : utrList) {
                String roleKey = utr.getRole().getKey();
                authorities.add(new SimpleGrantedAuthority("ROLE_" + roleKey.toUpperCase()));

                String tier = "basic";
                Optional<Entitlement> entOpt = entitlementRepository.findByTenantId(utr.getTenant().getId());
                if (entOpt.isPresent()) {
                    tier = entOpt.get().getTier();
                }

                tenantAccessList.add(new UserPrincipal.TenantAccess(
                        utr.getTenant().getId(),
                        utr.getTenant().getName(),
                        utr.getTenant().getClientInstanceId(),
                        roleKey,
                        tier
                ));
            }

            if (authorities.isEmpty()) {
                authorities.add(new SimpleGrantedAuthority("ROLE_USER"));
            }

            boolean mfaVerified = isMfaPending ? false : Boolean.TRUE.equals(claims.get("mfaVerified", Boolean.class));
            String tokenType = isMfaPending ? "MFA_PENDING" : "ACCESS";

            UserPrincipal principal = new UserPrincipal(
                    user.getId(),
                    user.getEmail(),
                    user.getDisplayName(),
                    user.getSource(),
                    mfaVerified,
                    tokenType,
                    tenantAccessList
            );

            UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                    principal, null, authorities
            );
            SecurityContextHolder.getContext().setAuthentication(authentication);

        } catch (Exception e) {
            SecurityContextHolder.clearContext();
        }

        filterChain.doFilter(request, response);
    }
}
