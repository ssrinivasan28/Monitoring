package com.islandpacific.sentinel.tool;

import java.util.Map;

/**
 * Encapsulates the execution result of a SentinelTool call.
 */
public class ToolExecutionResult {

    private final boolean success;
    private final Object data;
    private final String resultSummary;
    private final String errorMessage;

    public ToolExecutionResult(boolean success, Object data, String resultSummary, String errorMessage) {
        this.success = success;
        this.data = data;
        this.resultSummary = resultSummary;
        this.errorMessage = errorMessage;
    }

    public static ToolExecutionResult success(Object data, String resultSummary) {
        return new ToolExecutionResult(true, data, resultSummary, null);
    }

    public static ToolExecutionResult failure(String errorMessage) {
        return new ToolExecutionResult(false, null, null, errorMessage);
    }

    public boolean isSuccess() {
        return success;
    }

    public Object getData() {
        return data;
    }

    public String getResultSummary() {
        return resultSummary;
    }

    public String getErrorMessage() {
        return errorMessage;
    }
}
