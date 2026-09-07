package com.islandpacific.sentinel.tool;

import com.islandpacific.sentinel.entity.KbChunk;
import com.islandpacific.sentinel.entity.KbDoc;
import com.islandpacific.sentinel.llm.model.EmbeddingRequest;
import com.islandpacific.sentinel.llm.model.EmbeddingResponse;
import com.islandpacific.sentinel.llm.provider.EmbeddingProvider;
import com.islandpacific.sentinel.repository.KbChunkRepository;
import com.islandpacific.sentinel.repository.KbDocRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class KbSearchToolTest {

    private EmbeddingProvider embeddingProvider;
    private KbChunkRepository kbChunkRepository;
    private KbDocRepository kbDocRepository;
    private KbSearchTool kbSearchTool;

    @BeforeEach
    void setUp() {
        embeddingProvider = mock(EmbeddingProvider.class);
        kbChunkRepository = mock(KbChunkRepository.class);
        kbDocRepository = mock(KbDocRepository.class);
        kbSearchTool = new KbSearchTool(embeddingProvider, kbChunkRepository, kbDocRepository);
    }

    @Test
    void testKbSearchSuccess() {
        UUID tenantId = UUID.randomUUID();
        UUID docId = UUID.randomUUID();
        UUID chunkId = UUID.randomUUID();

        when(embeddingProvider.isAvailable()).thenReturn(true);
        EmbeddingResponse embedResponse = EmbeddingResponse.success(List.of(new float[]{0.1f, 0.2f, 0.3f}), 3, "local", "test-model");
        when(embeddingProvider.embed(any(EmbeddingRequest.class))).thenReturn(embedResponse);

        KbChunk chunk = new KbChunk(docId, tenantId, "High CPU usage on IBM i partition", new float[]{0.1f, 0.2f, 0.3f});
        chunk.setId(chunkId);

        when(kbChunkRepository.findSimilarChunks(eq(tenantId), anyString(), eq(5))).thenReturn(List.of(chunk));

        KbDoc doc = new KbDoc(tenantId, "kb-article-1", "IBM i CPU Troubleshooting Guide");
        doc.setId(docId);
        when(kbDocRepository.findAllById(anySet())).thenReturn(List.of(doc));

        ToolExecutionResult result = kbSearchTool.execute(tenantId, Map.of("query", "IBM i CPU troubleshooting"));

        assertTrue(result.isSuccess());

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> matches = (List<Map<String, Object>>) result.getData();
        assertEquals(1, matches.size());
        assertEquals("High CPU usage on IBM i partition", matches.get(0).get("text"));
        assertEquals("IBM i CPU Troubleshooting Guide", matches.get(0).get("title"));
    }

    @Test
    void testKbSearchDisabledGracefulDegradation() {
        UUID tenantId = UUID.randomUUID();
        when(embeddingProvider.isAvailable()).thenReturn(false);

        ToolExecutionResult result = kbSearchTool.execute(tenantId, Map.of("query", "IBM i CPU"));

        assertFalse(result.isSuccess());
        assertTrue(result.getErrorMessage().contains("unavailable") || result.getErrorMessage().contains("disabled"));
    }
}
