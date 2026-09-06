package com.islandpacific.sentinel.security;

import org.springframework.security.core.authority.SimpleGrantedAuthority;

public class RoleMapper {

    private RoleMapper() {}

    /**
     * Converts a DB role key (e.g. "staff-admin") to a GrantedAuthority ("ROLE_STAFF_ADMIN").
     */
    public static SimpleGrantedAuthority toAuthority(String roleKey) {
        if (roleKey == null || roleKey.isBlank()) {
            return new SimpleGrantedAuthority("ROLE_USER");
        }
        String formatted = roleKey.trim().toUpperCase().replace('-', '_');
        if (!formatted.startsWith("ROLE_")) {
            formatted = "ROLE_" + formatted;
        }
        return new SimpleGrantedAuthority(formatted);
    }

    /**
     * Normalizes a role key to canonical lowercase format with hyphens (e.g. "staff-admin").
     */
    public static String toCanonicalKey(String roleKey) {
        if (roleKey == null || roleKey.isBlank()) {
            return "customer-viewer";
        }
        String key = roleKey.trim().toLowerCase();
        if (key.startsWith("role_")) {
            key = key.substring(5);
        }
        return key.replace('_', '-');
    }
}
