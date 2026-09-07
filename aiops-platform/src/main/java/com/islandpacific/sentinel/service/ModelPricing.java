package com.islandpacific.sentinel.service;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Pricing rates for LLM models per token.
 */
public class ModelPricing {

    private ModelPricing() {}

    /**
     * Calculates estimated monetary cost for an LLM call.
     *
     * @param model model identifier (e.g. claude-3-5-sonnet, local, etc)
     * @param tokensIn prompt input tokens
     * @param tokensOut completion output tokens
     * @return calculated cost in USD
     */
    public static BigDecimal calculateCost(String model, int tokensIn, int tokensOut) {
        if (model == null || model.isBlank()) {
            model = "default";
        }

        String normalizedModel = model.toLowerCase();

        BigDecimal inputRate;
        BigDecimal outputRate;

        if (normalizedModel.contains("local") || normalizedModel.contains("ollama") || normalizedModel.contains("vllm")) {
            return BigDecimal.ZERO.setScale(6, RoundingMode.HALF_UP);
        } else if (normalizedModel.contains("haiku")) {
            inputRate = new BigDecimal("0.00000025");  // $0.25 / 1M
            outputRate = new BigDecimal("0.00000125"); // $1.25 / 1M
        } else if (normalizedModel.contains("claude-3-5") || normalizedModel.contains("sonnet")) {
            inputRate = new BigDecimal("0.00000300");  // $3.00 / 1M
            outputRate = new BigDecimal("0.00001500"); // $15.00 / 1M
        } else if (normalizedModel.contains("claude-3-opus") || normalizedModel.contains("opus")) {
            inputRate = new BigDecimal("0.00001500");  // $15.00 / 1M
            outputRate = new BigDecimal("0.00007500"); // $75.00 / 1M
        } else {
            // Default rate: $1.00 / 1M input, $3.00 / 1M output
            inputRate = new BigDecimal("0.00000100");
            outputRate = new BigDecimal("0.00000300");
        }

        BigDecimal costIn = inputRate.multiply(BigDecimal.valueOf(tokensIn));
        BigDecimal costOut = outputRate.multiply(BigDecimal.valueOf(tokensOut));

        return costIn.add(costOut).setScale(6, RoundingMode.HALF_UP);
    }
}
