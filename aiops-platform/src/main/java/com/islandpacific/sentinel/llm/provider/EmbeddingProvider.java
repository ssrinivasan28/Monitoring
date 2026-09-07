package com.islandpacific.sentinel.llm.provider;

import com.islandpacific.sentinel.llm.model.EmbeddingRequest;
import com.islandpacific.sentinel.llm.model.EmbeddingResponse;

/**
 * Interface for vector embedding generation (used for RAG vector search).
 */
public interface EmbeddingProvider {

    /**
     * Generates vector embeddings for requested text inputs.
     * Guarantees returning EmbeddingResponse with aiAvailable=false on failure or disabled mode.
     */
    EmbeddingResponse embed(EmbeddingRequest request);

    /**
     * Returns true if embedding provider is configured and available.
     */
    boolean isAvailable();

    /**
     * Provider identifier string.
     */
    String getProviderName();
}
