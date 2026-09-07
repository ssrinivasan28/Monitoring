package com.islandpacific.sentinel.tool;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.islandpacific.sentinel.entity.KbChunk;
import com.islandpacific.sentinel.entity.KbDoc;
import com.islandpacific.sentinel.llm.model.EmbeddingRequest;
import com.islandpacific.sentinel.llm.model.EmbeddingResponse;
import com.islandpacific.sentinel.llm.model.LlmTool;
import com.islandpacific.sentinel.llm.provider.EmbeddingProvider;
import com.islandpacific.sentinel.repository.KbChunkRepository;
import com.islandpacific.sentinel.repository.KbDocRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Tool for performing pgvector similarity search over Knowledge Base chunks for a tenant.
 */
@Component
public class KbSearchTool implements SentinelTool {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    private final EmbeddingProvider embeddingProvider;
    private final KbChunkRepository kbChunkRepository;
    private final KbDocRepository kbDocRepository;

    @Autowired
    public KbSearchTool(
            EmbeddingProvider embeddingProvider,
            KbChunkRepository kbChunkRepository,
            KbDocRepository kbDocRepository) {
        this.embeddingProvider = embeddingProvider;
        this.kbChunkRepository = kbChunkRepository;
        this.kbDocRepository = kbDocRepository;
    }

    @Override
    public String getName() {
        return "kb_search";
    }

    @Override
    public String getDescription() {
        return "Performs semantic vector similarity search over tenant knowledge base documentation chunks.";
    }

    @Override
    public LlmTool getLlmToolDefinition() {
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("type", "object");
        Map<String, Object> props = new LinkedHashMap<>();

        Map<String, Object> queryProp = new LinkedHashMap<>();
        queryProp.put("type", "string");
        queryProp.put("description", "Search query or question to match against knowledge base chunks");
        props.put("query", queryProp);

        Map<String, Object> limitProp = new LinkedHashMap<>();
        limitProp.put("type", "integer");
        limitProp.put("description", "Maximum number of chunks to return (default 5)");
        props.put("limit", limitProp);

        params.put("properties", props);
        params.put("required", List.of("query"));

        return new LlmTool(getName(), getDescription(), params);
    }

    @Override
    public ToolExecutionResult execute(UUID tenantId, Map<String, Object> arguments) {
        if (arguments == null || !arguments.containsKey("query")) {
            return ToolExecutionResult.failure("Missing required 'query' argument");
        }

        String queryText = (String) arguments.get("query");
        int limit = 5;
        if (arguments.containsKey("limit")) {
            Object limVal = arguments.get("limit");
            if (limVal instanceof Number) {
                limit = ((Number) limVal).intValue();
            } else {
                try {
                    limit = Integer.parseInt(limVal.toString());
                } catch (NumberFormatException ignored) {}
            }
        }

        if (!embeddingProvider.isAvailable()) {
            return ToolExecutionResult.failure("Embedding provider is disabled or unavailable. Semantic KB search is inactive.");
        }

        try {
            EmbeddingResponse embedResp = embeddingProvider.embed(new EmbeddingRequest(List.of(queryText)));
            if (!embedResp.isAiAvailable() || embedResp.getEmbeddings().isEmpty()) {
                return ToolExecutionResult.failure("Failed to generate search vector: " + embedResp.getErrorMessage());
            }

            float[] vec = embedResp.getEmbeddings().get(0);
            String vectorStr = Arrays.toString(vec);

            List<KbChunk> chunks = kbChunkRepository.findSimilarChunks(tenantId, vectorStr, limit);

            // Fetch document metadata for matched chunks
            Set<UUID> docIds = chunks.stream().map(KbChunk::getKbDocId).collect(Collectors.toSet());
            Map<UUID, KbDoc> docMap = new HashMap<>();
            if (!docIds.isEmpty()) {
                kbDocRepository.findAllById(docIds).forEach(d -> {
                    if (d.getTenantId().equals(tenantId)) {
                        docMap.put(d.getId(), d);
                    }
                });
            }

            List<Map<String, Object>> matches = new ArrayList<>();
            for (KbChunk chunk : chunks) {
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("chunkId", chunk.getId());
                item.put("kbDocId", chunk.getKbDocId());
                item.put("text", chunk.getChunkText());

                KbDoc doc = docMap.get(chunk.getKbDocId());
                if (doc != null) {
                    item.put("title", doc.getTitle());
                    item.put("source", doc.getSource());
                }
                matches.add(item);
            }

            String jsonResult = objectMapper.writeValueAsString(matches);
            String summary = String.format("Found %d relevant KB chunk(s) for query '%s'.", matches.size(), queryText);
            return ToolExecutionResult.success(matches, summary + "\n" + jsonResult);

        } catch (Exception e) {
            return ToolExecutionResult.failure("KB search error: " + e.getMessage());
        }
    }
}
