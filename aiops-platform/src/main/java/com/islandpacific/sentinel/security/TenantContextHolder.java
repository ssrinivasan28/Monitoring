package com.islandpacific.sentinel.security;

import java.util.Optional;
import java.util.UUID;

public class TenantContextHolder {

    private static final ThreadLocal<TenantContext> CONTEXT_HOLDER = new ThreadLocal<>();

    private TenantContextHolder() {}

    public static void setContext(TenantContext context) {
        CONTEXT_HOLDER.set(context);
    }

    public static Optional<TenantContext> getContext() {
        return Optional.ofNullable(CONTEXT_HOLDER.get());
    }

    public static UUID getTenantId() {
        return getContext().map(TenantContext::getTenantId).orElse(null);
    }

    public static void setTenantId(UUID tenantId) {
        if (tenantId == null) {
            clear();
        } else {
            setContext(new TenantContext(UUID.randomUUID(), tenantId, "client-instance", "STAFF_ADMIN"));
        }
    }

    public static UUID getRequiredTenantId() {
        return getContext()
                .map(TenantContext::getTenantId)
                .orElseThrow(() -> new IllegalStateException("No active tenant context bound to current thread"));
    }

    public static void clear() {
        CONTEXT_HOLDER.remove();
    }


}
