package com.islandpacific.sentinel.llm.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Request payload for text embedding generation.
 */
public class EmbeddingRequest {

    private List<String> inputs = new ArrayList<>();
    private String model;

    public EmbeddingRequest() {
    }

    public EmbeddingRequest(List<String> inputs) {
        this.inputs = inputs != null ? inputs : new ArrayList<>();
    }

    public EmbeddingRequest(String input) {
        if (input != null) {
            this.inputs.add(input);
        }
    }

    public List<String> getInputs() {
        return inputs;
    }

    public void setInputs(List<String> inputs) {
        this.inputs = inputs != null ? inputs : new ArrayList<>();
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        EmbeddingRequest that = (EmbeddingRequest) o;
        return Objects.equals(inputs, that.inputs) && Objects.equals(model, that.model);
    }

    @Override
    public int hashCode() {
        return Objects.hash(inputs, model);
    }
}
