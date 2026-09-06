package com.islandpacific.sentinel.security;

public class EntitlementDeniedException extends RuntimeException {

    private final EntitlementTier currentTier;
    private final EntitlementTier requiredTier;

    public EntitlementDeniedException(EntitlementTier currentTier, EntitlementTier requiredTier) {
        super("This feature requires " + requiredTier.name() + " entitlement tier. Current tier: " + currentTier.name() + ".");
        this.currentTier = currentTier;
        this.requiredTier = requiredTier;
    }

    public EntitlementTier getCurrentTier() {
        return currentTier;
    }

    public EntitlementTier getRequiredTier() {
        return requiredTier;
    }
}
