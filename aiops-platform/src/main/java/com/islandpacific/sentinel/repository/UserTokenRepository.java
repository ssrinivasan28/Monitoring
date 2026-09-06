package com.islandpacific.sentinel.repository;

import com.islandpacific.sentinel.entity.UserToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserTokenRepository extends JpaRepository<UserToken, UUID> {
    Optional<UserToken> findByTokenHash(String tokenHash);
    List<UserToken> findByFamilyId(UUID familyId);
    void deleteByUserIdAndTokenType(UUID userId, String tokenType);
}
