package com.islandpacific.sentinel.security;

public enum EntitlementTier {
    BASIC,
    PRO;

    /**
     * Explicit evaluation method for entitlement tier comparison.
     * Prevents enum ordinal reordering bugs.
     */
    public boolean satisfies(EntitlementTier required) {
        if (required == BASIC) {
            return true;
        }
        return this == PRO;
    }

    public static EntitlementTier fromString(String tierStr) {
        if (tierStr == null || tierStr.isBlank()) {
            return BASIC;
        }
        try {
            return EntitlementTier.valueOf(tierStr.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return BASIC;
        }
    }
}
