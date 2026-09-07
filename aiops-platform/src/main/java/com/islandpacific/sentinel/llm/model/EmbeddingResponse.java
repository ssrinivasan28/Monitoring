package com.islandpacific.sentinel.llm.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Response containing vector embeddings for inputs.
 */
public class EmbeddingResponse {

    private List<float[]> embeddings = new ArrayList<>();
    private int dimensions;
    private String providerName;
    private String modelName;
    private boolean aiAvailable = true;
    private String errorMessage;

    public EmbeddingResponse() {
    }

    public static EmbeddingResponse success(List<float[]> embeddings, int dimensions, String providerName, String modelName) {
        EmbeddingResponse resp = new EmbeddingResponse();
        resp.setEmbeddings(embeddings);
        resp.setDimensions(dimensions);
        resp.setProviderName(providerName);
        resp.setModelName(modelName);
        resp.setAiAvailable(true);
        return resp;
    }

    public static EmbeddingResponse unavailable(String message) {
        EmbeddingResponse resp = new EmbeddingResponse();
        resp.setAiAvailable(false);
        resp.setErrorMessage(message != null ? message : "Embedding service unavailable");
        return resp;
    }

    public List<float[]> getEmbeddings() {
        return embeddings;
    }

    public void setEmbeddings(List<float[]> embeddings) {
        this.embeddings = embeddings != null ? embeddings : new ArrayList<>();
    }

    public int getDimensions() {
        return dimensions;
    }

    public void setDimensions(int dimensions) {
        this.dimensions = dimensions;
    }

    public String getProviderName() {
        return providerName;
    }

    public void setProviderName(String providerName) {
        this.providerName = providerName;
    }

    public String getModelName() {
        return modelName;
    }

    public void setModelName(String modelName) {
        this.modelName = modelName;
    }

    public boolean isAiAvailable() {
        return aiAvailable;
    }

    public void setAiAvailable(boolean aiAvailable) {
        this.aiAvailable = aiAvailable;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        EmbeddingResponse that = (EmbeddingResponse) o;
        return dimensions == that.dimensions &&
                aiAvailable == that.aiAvailable &&
                Objects.equals(embeddings, that.embeddings) &&
                Objects.equals(providerName, that.providerName) &&
                Objects.equals(modelName, that.modelName) &&
                Objects.equals(errorMessage, that.errorMessage);
    }

    @Override
    public int hashCode() {
        return Objects.hash(embeddings, dimensions, providerName, modelName, aiAvailable, errorMessage);
    }
}
