package com.islandpacific.sentinel.entity;

import jakarta.persistence.*;
import java.util.UUID;

@Entity
@Table(name = "kb_chunks")
public class KbChunk {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(name = "kb_doc_id", nullable = false)
    private UUID kbDocId;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "chunk_text", nullable = false, columnDefinition = "TEXT")
    private String chunkText;

    @Convert(converter = VectorConverter.class)
    @Column(name = "embedding", columnDefinition = "vector(1536)")
    private float[] embedding;

    public KbChunk() {}

    public KbChunk(UUID kbDocId, UUID tenantId, String chunkText, float[] embedding) {
        this.kbDocId = kbDocId;
        this.tenantId = tenantId;
        this.chunkText = chunkText;
        this.embedding = embedding;
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getKbDocId() { return kbDocId; }
    public void setKbDocId(UUID kbDocId) { this.kbDocId = kbDocId; }

    public UUID getTenantId() { return tenantId; }
    public void setTenantId(UUID tenantId) { this.tenantId = tenantId; }

    public String getChunkText() { return chunkText; }
    public void setChunkText(String chunkText) { this.chunkText = chunkText; }

    public float[] getEmbedding() { return embedding; }
    public void setEmbedding(float[] embedding) { this.embedding = embedding; }
}
