package com.islandpacific.sentinel.security;

import com.islandpacific.sentinel.entity.User;
import com.islandpacific.sentinel.repository.UserRepository;
import com.islandpacific.sentinel.repository.UserTenantRoleRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class UserAuthCache {

    private static final long CACHE_TTL_MS = 60 * 1000L; // 60s TTL

    private final UserRepository userRepository;
    private final UserTenantRoleRepository userTenantRoleRepository;
    private final Map<UUID, CachedUserStatus> cache = new ConcurrentHashMap<>();

    @Autowired
    public UserAuthCache(UserRepository userRepository, UserTenantRoleRepository userTenantRoleRepository) {
        this.userRepository = userRepository;
        this.userTenantRoleRepository = userTenantRoleRepository;
    }

    public boolean isUserActive(UUID userId) {
        CachedUserStatus cached = cache.get(userId);
        long now = System.currentTimeMillis();
        if (cached == null || (now - cached.timestamp) > CACHE_TTL_MS) {
            Optional<User> userOpt = userRepository.findById(userId);
            if (userOpt.isEmpty()) {
                cache.put(userId, new CachedUserStatus(false, now));
                return false;
            }
            boolean active = "ACTIVE".equalsIgnoreCase(userOpt.get().getStatus());
            cache.put(userId, new CachedUserStatus(active, now));
            return active;
        }
        return cached.active;
    }

    public void invalidate(UUID userId) {
        cache.remove(userId);
    }

    private static class CachedUserStatus {
        final boolean active;
        final long timestamp;

        CachedUserStatus(boolean active, long timestamp) {
            this.active = active;
            this.timestamp = timestamp;
        }
    }
}
