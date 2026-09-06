package com.islandpacific.sentinel.query;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class ResponseMerger {

    private static final String RESERVED_SOURCE_LABEL = "__sentinel_source";
    private static final Gson GSON = new Gson();

    @Value("${sentinel.datasource.limits.max-result-series:5000}")
    private int maxResultSeries = 5000;

    @Value("${sentinel.datasource.limits.max-response-bytes:5242880}")
    private long maxResponseBytes = 5242880L;

    public MergedResult mergeResponses(Map<DatasourceResolverService.ResolvedDatasource, QueryFanoutExecutor.BackendResponse> responses) {
        if (responses == null || responses.isEmpty()) {
            return new MergedResult(502, GSON.toJson(createErrorMap("No backend response received")));
        }

        int successCount = 0;
        List<String> warnings = new ArrayList<>();
        List<JsonObject> allSeries = new ArrayList<>();
        String resultType = "vector";
        boolean payloadExceeded = false;
        boolean backendFailure = false;

        for (var entry : responses.entrySet()) {
            DatasourceResolverService.ResolvedDatasource ds = entry.getKey();
            QueryFanoutExecutor.BackendResponse resp = entry.getValue();

            if (resp.isSuccess() && resp.getBody() != null && !resp.getBody().isBlank()) {
                if (resp.getBody().length() > maxResponseBytes) {
                    payloadExceeded = true;
                    warnings.add("Datasource " + ds.getName() + " payload exceeded maximum size limit (" + maxResponseBytes + " bytes)");
                    continue;
                }

                try {
                    JsonObject json = JsonParser.parseString(resp.getBody()).getAsJsonObject();
                    if (json.has("data") && json.get("data").isJsonObject()) {
                        JsonObject data = json.getAsJsonObject("data");
                        if (data.has("resultType")) {
                            resultType = data.get("resultType").getAsString();
                        }
                        if (data.has("result") && data.get("result").isJsonArray()) {
                            JsonArray array = data.getAsJsonArray("result");
                            for (JsonElement elem : array) {
                                if (elem.isJsonObject()) {
                                    JsonObject item = elem.getAsJsonObject();
                                    // Strip any upstream-spoofed __sentinel_source label and inject authoritative gateway source label
                                    injectAuthoritativeSourceLabel(item, ds.getName());
                                    allSeries.add(item);
                                }
                            }
                        }
                    }
                    successCount++;
                } catch (Exception e) {
                    backendFailure = true;
                    warnings.add("Datasource " + ds.getName() + " returned invalid payload structure");
                }
            } else {
                backendFailure = true;
                if (resp.getErrorMessage() != null && resp.getErrorMessage().contains("PAYLOAD_BYTES_EXCEEDED")) {
                    payloadExceeded = true;
                    warnings.add("Datasource " + ds.getName() + " payload exceeded streaming byte limit (" + maxResponseBytes + " bytes)");
                } else {
                    warnings.add("Datasource " + ds.getName() + " unavailable");
                }
            }
        }

        if (successCount == 0) {
            Map<String, Object> errorBody = new HashMap<>();
            errorBody.put("status", "error");
            errorBody.put("errorType", "bad_gateway");
            errorBody.put("error", "All target datasources failed to respond");
            errorBody.put("partial", false);
            errorBody.put("warnings", warnings);
            return new MergedResult(502, GSON.toJson(errorBody));
        }

        // Deterministic sorting of all series by canonical label key string before truncation
        allSeries.sort((a, b) -> getCanonicalLabelKey(a).compareTo(getCanonicalLabelKey(b)));

        boolean seriesLimitExceeded = false;
        List<JsonObject> finalSeries = allSeries;
        if (allSeries.size() > maxResultSeries) {
            seriesLimitExceeded = true;
            finalSeries = new ArrayList<>(allSeries.subList(0, maxResultSeries));
            warnings.add("Result truncated: exceeded maximum series limit (" + maxResultSeries + ")");
        }

        boolean isPartial = backendFailure || payloadExceeded || seriesLimitExceeded;
        List<String> partialReasons = new ArrayList<>();
        String primaryPartialReason = null;

        if (backendFailure) partialReasons.add("BACKEND_PARTIAL_FAILURE");
        if (payloadExceeded) partialReasons.add("PAYLOAD_BYTES_EXCEEDED");
        if (seriesLimitExceeded) partialReasons.add("SERIES_LIMIT_EXCEEDED");

        if (!partialReasons.isEmpty()) {
            // Fixed Enum Precedence: BACKEND_PARTIAL_FAILURE > PAYLOAD_BYTES_EXCEEDED > SERIES_LIMIT_EXCEEDED
            if (partialReasons.contains("BACKEND_PARTIAL_FAILURE")) {
                primaryPartialReason = "BACKEND_PARTIAL_FAILURE";
            } else if (partialReasons.contains("PAYLOAD_BYTES_EXCEEDED")) {
                primaryPartialReason = "PAYLOAD_BYTES_EXCEEDED";
            } else {
                primaryPartialReason = "SERIES_LIMIT_EXCEEDED";
            }
        }

        Map<String, Object> finalBody = new HashMap<>();
        finalBody.put("status", "success");
        finalBody.put("partial", isPartial);
        if (isPartial) {
            finalBody.put("partialReasons", partialReasons);
            finalBody.put("partialReason", primaryPartialReason);
        }

        Map<String, Object> dataObj = new HashMap<>();
        dataObj.put("resultType", resultType);
        dataObj.put("result", finalSeries);
        finalBody.put("data", dataObj);

        if (!warnings.isEmpty()) {
            finalBody.put("warnings", warnings);
        }

        return new MergedResult(200, GSON.toJson(finalBody));
    }

    private String getCanonicalLabelKey(JsonObject item) {
        JsonObject labelsObj = null;
        if (item.has("metric") && item.get("metric").isJsonObject()) {
            labelsObj = item.getAsJsonObject("metric");
        } else if (item.has("stream") && item.get("stream").isJsonObject()) {
            labelsObj = item.getAsJsonObject("stream");
        }

        if (labelsObj == null || labelsObj.keySet().isEmpty()) {
            return "";
        }

        List<String> sortedKeys = new ArrayList<>(labelsObj.keySet());
        java.util.Collections.sort(sortedKeys);
        StringBuilder sb = new StringBuilder("{");
        for (int i = 0; i < sortedKeys.size(); i++) {
            String k = sortedKeys.get(i);
            if (i > 0) sb.append(",");
            sb.append(k).append("=\"").append(labelsObj.get(k).getAsString()).append("\"");
        }
        sb.append("}");
        return sb.toString();
    }

    private void injectAuthoritativeSourceLabel(JsonObject item, String sourceName) {
        // PromQL uses "metric", LogQL uses "stream"
        if (item.has("metric") && item.get("metric").isJsonObject()) {
            JsonObject metric = item.getAsJsonObject("metric");
            metric.remove(RESERVED_SOURCE_LABEL); // Remove upstream spoofed label if present
            metric.addProperty(RESERVED_SOURCE_LABEL, sourceName);
        } else if (item.has("stream") && item.get("stream").isJsonObject()) {
            JsonObject stream = item.getAsJsonObject("stream");
            stream.remove(RESERVED_SOURCE_LABEL); // Remove upstream spoofed label if present
            stream.addProperty(RESERVED_SOURCE_LABEL, sourceName);
        } else {
            JsonObject metric = new JsonObject();
            metric.addProperty(RESERVED_SOURCE_LABEL, sourceName);
            item.add("metric", metric);
        }
    }

    private Map<String, Object> createErrorMap(String message) {
        Map<String, Object> map = new HashMap<>();
        map.put("status", "error");
        map.put("error", message);
        return map;
    }

    public static class MergedResult {
        private final int httpStatusCode;
        private final String jsonResponse;

        public MergedResult(int httpStatusCode, String jsonResponse) {
            this.httpStatusCode = httpStatusCode;
            this.jsonResponse = jsonResponse;
        }

        public int getHttpStatusCode() { return httpStatusCode; }
        public String getJsonResponse() { return jsonResponse; }
    }
}
