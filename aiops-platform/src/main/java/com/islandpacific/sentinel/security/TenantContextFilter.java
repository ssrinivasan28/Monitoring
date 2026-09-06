package com.islandpacific.sentinel.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class TenantContextFilter extends OncePerRequestFilter {

    private static final Pattern ADMIN_TENANT_PATTERN = Pattern.compile("^/api/v1/admin/tenants/([0-9a-fA-F-]{36})(?:/.*)?$");

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof UserPrincipal principal)) {
            filterChain.doFilter(request, response);
            return;
        }

        List<UserPrincipal.TenantAccess> tenantAccessList = principal.getTenants();
        if (tenantAccessList == null || tenantAccessList.isEmpty()) {
            filterChain.doFilter(request, response);
            return;
        }

        String headerTenantId = request.getHeader("X-Tenant-Id");
        UUID targetTenantId = null;

        if (headerTenantId != null && !headerTenantId.isBlank()) {
            try {
                targetTenantId = UUID.fromString(headerTenantId.trim());
            } catch (IllegalArgumentException e) {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                response.setContentType("application/json");
                response.getWriter().write("{\"status\":400,\"error\":\"Bad Request\",\"message\":\"Invalid X-Tenant-Id header format\"}");
                return;
            }
        } else {
            if (tenantAccessList.size() == 1) {
                targetTenantId = tenantAccessList.get(0).getTenantId();
            } else {
                // Multi-tenant user without header
                // Only enforce if request is for tenant-scoped API endpoints
                String path = request.getRequestURI();
                if (path.startsWith("/api/v1/") && !path.startsWith("/api/v1/auth/")) {
                    response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                    response.setContentType("application/json");
                    response.getWriter().write("{\"status\":400,\"error\":\"Bad Request\",\"message\":\"X-Tenant-Id header required for multi-tenant users\"}");
                    return;
                }
            }
        }

        if (targetTenantId != null) {
            UUID finalTargetId = targetTenantId;
            Optional<UserPrincipal.TenantAccess> matchingAccess = tenantAccessList.stream()
                    .filter(t -> t.getTenantId().equals(finalTargetId))
                    .findFirst();

            if (matchingAccess.isEmpty()) {
                response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                response.setContentType("application/json");
                response.getWriter().write("{\"status\":403,\"error\":\"Forbidden\",\"message\":\"Access denied to target tenant\"}");
                return;
            }

            // Validate admin path tenant ID matching if applicable
            String requestUri = request.getRequestURI();
            Matcher matcher = ADMIN_TENANT_PATTERN.matcher(requestUri);
            if (matcher.matches()) {
                try {
                    UUID pathTenantId = UUID.fromString(matcher.group(1));
                    if (!pathTenantId.equals(targetTenantId)) {
                        response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                        response.setContentType("application/json");
                        response.getWriter().write("{\"status\":400,\"error\":\"Bad Request\",\"message\":\"Path tenant ID does not match X-Tenant-Id header\"}");
                        return;
                    }
                } catch (IllegalArgumentException ignored) {}
            }

            UserPrincipal.TenantAccess tenantAccess = matchingAccess.get();
            TenantContext context = new TenantContext(
                    principal.getUserId(),
                    tenantAccess.getTenantId(),
                    tenantAccess.getClientInstanceId(),
                    tenantAccess.getRole()
            );
            TenantContextHolder.setContext(context);

            // Dynamically set Spring Security authority matching active tenant role
            List<GrantedAuthority> authorities = new ArrayList<>();
            authorities.add(RoleMapper.toAuthority(tenantAccess.getRole()));

            UsernamePasswordAuthenticationToken updatedAuth = new UsernamePasswordAuthenticationToken(
                    principal, authentication.getCredentials(), authorities
            );
            SecurityContextHolder.getContext().setAuthentication(updatedAuth);
        }

        try {
            filterChain.doFilter(request, response);
        } finally {
            TenantContextHolder.clear();
        }
    }
}
