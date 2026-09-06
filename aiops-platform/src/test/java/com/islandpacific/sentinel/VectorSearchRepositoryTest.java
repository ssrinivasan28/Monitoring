package com.islandpacific.sentinel;

import com.islandpacific.sentinel.entity.KbChunk;
import com.islandpacific.sentinel.entity.KbDoc;
import com.islandpacific.sentinel.entity.Tenant;
import com.islandpacific.sentinel.repository.KbChunkRepository;
import com.islandpacific.sentinel.repository.KbDocRepository;
import com.islandpacific.sentinel.repository.TenantRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

public class VectorSearchRepositoryTest extends AbstractIntegrationTest {

    @Autowired
    private TenantRepository tenantRepository;

    @Autowired
    private KbDocRepository kbDocRepository;

    @Autowired
    private KbChunkRepository kbChunkRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void vectorSimilarityQueryReturnsOrderedRows() {
        Tenant tenant = tenantRepository.save(new Tenant("Vector Test Tenant", "client-vec-01"));
        UUID tenantId = tenant.getId();

        KbDoc doc = kbDocRepository.save(new KbDoc(tenantId, "runbook", "IBM i CPU Troubleshooting"));

        // Construct 1536-dim vector arrays
        float[] v1 = new float[1536];
        v1[0] = 1.0f; // Vector A: [1.0, 0, 0, ...]

        float[] v2 = new float[1536];
        v2[1] = 1.0f; // Vector B: [0, 1.0, 0, ...]

        // Insert using JdbcTemplate to format vector array literal accurately for pgvector
        UUID chunk1Id = UUID.randomUUID();
        UUID chunk2Id = UUID.randomUUID();

        jdbcTemplate.update(
            "INSERT INTO kb_chunks (id, kb_doc_id, tenant_id, chunk_text, embedding) VALUES (?, ?, ?, ?, CAST(? AS vector))",
            chunk1Id, doc.getId(), tenantId, "CPU High Runbook Chunk A", buildVectorString(v1)
        );

        jdbcTemplate.update(
            "INSERT INTO kb_chunks (id, kb_doc_id, tenant_id, chunk_text, embedding) VALUES (?, ?, ?, ?, CAST(? AS vector))",
            chunk2Id, doc.getId(), tenantId, "Disk Full Runbook Chunk B", buildVectorString(v2)
        );

        // Query with vector target [1.0, 0, 0, ...]
        String targetQueryVectorStr = buildVectorString(v1);
        List<KbChunk> results = kbChunkRepository.findSimilarChunks(tenantId, targetQueryVectorStr, 2);

        assertThat(results).hasSize(2);
        // First result should be Chunk A (identical vector, distance 0)
        assertThat(results.get(0).getChunkText()).isEqualTo("CPU High Runbook Chunk A");
        assertThat(results.get(1).getChunkText()).isEqualTo("Disk Full Runbook Chunk B");
    }

    private String buildVectorString(float[] vec) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < vec.length; i++) {
            sb.append(vec[i]);
            if (i < vec.length - 1) {
                sb.append(",");
            }
        }
        sb.append("]");
        return sb.toString();
    }
}
