package com.islandpacific.sentinel.llm.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Configuration properties for sentinel LLM providers, routing, and embeddings.
 */
@Component
@ConfigurationProperties(prefix = "sentinel.llm")
public class LlmProperties {

    public enum Mode {
        AUTO,
        CLAUDE,
        LOCAL,
        DISABLED
    }

    private Mode mode = Mode.AUTO;
    private String primary = "claude";
    private String secondary = "local";

    private ClaudeProperties claude = new ClaudeProperties();
    private LocalProperties local = new LocalProperties();
    private EmbeddingProperties embedding = new EmbeddingProperties();

    public Mode getMode() {
        return mode;
    }

    public void setMode(Mode mode) {
        this.mode = mode;
    }

    public String getPrimary() {
        return primary;
    }

    public void setPrimary(String primary) {
        this.primary = primary;
    }

    public String getSecondary() {
        return secondary;
    }

    public void setSecondary(String secondary) {
        this.secondary = secondary;
    }

    public ClaudeProperties getClaude() {
        return claude;
    }

    public void setClaude(ClaudeProperties claude) {
        this.claude = claude;
    }

    public LocalProperties getLocal() {
        return local;
    }

    public void setLocal(LocalProperties local) {
        this.local = local;
    }

    public EmbeddingProperties getEmbedding() {
        return embedding;
    }

    public void setEmbedding(EmbeddingProperties embedding) {
        this.embedding = embedding;
    }

    public static class ClaudeProperties {
        private String apiKey;
        private String apiUrl = "https://api.anthropic.com/v1/messages";
        private String model = "claude-3-5-sonnet-20241022";
        private int maxTokens = 2048;
        private int timeoutMs = 10000;

        public String getApiKey() {
            return apiKey;
        }

        public void setApiKey(String apiKey) {
            this.apiKey = apiKey;
        }

        public String getApiUrl() {
            return apiUrl;
        }

        public void setApiUrl(String apiUrl) {
            this.apiUrl = apiUrl;
        }

        public String getModel() {
            return model;
        }

        public void setModel(String model) {
            this.model = model;
        }

        public int getMaxTokens() {
            return maxTokens;
        }

        public void setMaxTokens(int maxTokens) {
            this.maxTokens = maxTokens;
        }

        public int getTimeoutMs() {
            return timeoutMs;
        }

        public void setTimeoutMs(int timeoutMs) {
            this.timeoutMs = timeoutMs;
        }
    }

    public static class LocalProperties {
        private String apiKey;
        private String apiUrl = "http://localhost:11434/v1/chat/completions";
        private String model = "llama3.1";
        private int maxTokens = 2048;
        private int timeoutMs = 10000;

        public String getApiKey() {
            return apiKey;
        }

        public void setApiKey(String apiKey) {
            this.apiKey = apiKey;
        }

        public String getApiUrl() {
            return apiUrl;
        }

        public void setApiUrl(String apiUrl) {
            this.apiUrl = apiUrl;
        }

        public String getModel() {
            return model;
        }

        public void setModel(String model) {
            this.model = model;
        }

        public int getMaxTokens() {
            return maxTokens;
        }

        public void setMaxTokens(int maxTokens) {
            this.maxTokens = maxTokens;
        }

        public int getTimeoutMs() {
            return timeoutMs;
        }

        public void setTimeoutMs(int timeoutMs) {
            this.timeoutMs = timeoutMs;
        }
    }

    public static class EmbeddingProperties {
        private String provider = "local";
        private String apiKey;
        private String apiUrl = "http://localhost:11434/v1/embeddings";
        private String model = "nomic-embed-text";
        private int dimensions = 768;
        private int timeoutMs = 5000;

        public String getProvider() {
            return provider;
        }

        public void setProvider(String provider) {
            this.provider = provider;
        }

        public String getApiKey() {
            return apiKey;
        }

        public void setApiKey(String apiKey) {
            this.apiKey = apiKey;
        }

        public String getApiUrl() {
            return apiUrl;
        }

        public void setApiUrl(String apiUrl) {
            this.apiUrl = apiUrl;
        }

        public String getModel() {
            return model;
        }

        public void setModel(String model) {
            this.model = model;
        }

        public int getDimensions() {
            return dimensions;
        }

        public void setDimensions(int dimensions) {
            this.dimensions = dimensions;
        }

        public int getTimeoutMs() {
            return timeoutMs;
        }

        public void setTimeoutMs(int timeoutMs) {
            this.timeoutMs = timeoutMs;
        }
    }
}
