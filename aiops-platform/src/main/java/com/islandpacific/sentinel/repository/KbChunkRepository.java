package com.islandpacific.sentinel.repository;

import com.islandpacific.sentinel.entity.KbChunk;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface KbChunkRepository extends JpaRepository<KbChunk, UUID> {
    List<KbChunk> findByTenantId(UUID tenantId);
    List<KbChunk> findByTenantIdAndKbDocId(UUID tenantId, UUID kbDocId);

    @Query(value = "SELECT * FROM kb_chunks WHERE tenant_id = :tenantId ORDER BY embedding <=> CAST(:embedding AS vector) LIMIT :limit", nativeQuery = true)
    List<KbChunk> findSimilarChunks(@Param("tenantId") UUID tenantId, @Param("embedding") String embeddingVectorStr, @Param("limit") int limit);
}
