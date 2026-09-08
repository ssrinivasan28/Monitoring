package com.islandpacific.sentinel.service.dashboard;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class GrafanaDashboardImporter {

    private final ObjectMapper objectMapper;

    public GrafanaDashboardImporter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public DashboardDefinitionDto convertGrafanaToSentinel(String grafanaJson) {
        try {
            JsonNode root = objectMapper.readTree(grafanaJson);
            String title = root.path("title").asText("Imported Dashboard");
            String rawId = root.path("uid").asText(root.path("id").asText("imported-dashboard"));
            String id = rawId.toLowerCase().replaceAll("[^a-z0-9_-]", "-");
            if (id.isBlank() || id.equals("null")) {
                id = "imported-dashboard-" + UUID.randomUUID().toString().substring(0, 8);
            }

            DashboardDefinitionDto definition = new DashboardDefinitionDto();
            definition.setId(id);
            definition.setTitle(title);
            definition.setCategory("Imported");
            definition.setVariables(Arrays.asList("tenant", "timeRange"));

            List<PanelDefinitionDto> panels = new ArrayList<>();
            JsonNode panelsNode = root.path("panels");

            if (panelsNode.isArray()) {
                int panelIdx = 1;
                for (JsonNode pNode : panelsNode) {
                    String panelType = pNode.path("type").asText("");
                    if ("row".equalsIgnoreCase(panelType)) {
                        continue; // Skip structural row headers
                    }

                    PanelDefinitionDto panel = new PanelDefinitionDto();
                    panel.setId("p-" + (pNode.has("id") ? pNode.get("id").asText() : panelIdx));
                    panel.setTitle(pNode.path("title").asText("Panel " + panelIdx));
                    panel.setType(mapGrafanaType(panelType));

                    // Extract PromQL / LogQL Query
                    JsonNode targets = pNode.path("targets");
                    String query = "";
                    String legendFormat = "";
                    if (targets.isArray() && targets.size() > 0) {
                        JsonNode t0 = targets.get(0);
                        query = t0.path("expr").asText(t0.path("query").asText(""));
                        legendFormat = t0.path("legendFormat").asText("");
                    }
                    if (query.isBlank()) {
                        query = "up";
                    }

                    // Standardize variable placeholders (e.g., replace $location or ${datasource} with $tenant if appropriate)
                    panel.setQuery(query);
                    if (!legendFormat.isBlank()) {
                        panel.setLegendFormat(legendFormat);
                    }

                    // Extract Unit & Thresholds
                    JsonNode defaults = pNode.path("fieldConfig").path("defaults");
                    String unit = defaults.path("unit").asText("short");
                    panel.setUnit(unit);

                    JsonNode thresholdsNode = defaults.path("thresholds");
                    if (!thresholdsNode.isMissingNode()) {
                        Map<String, Object> thresholdMap = new HashMap<>();
                        JsonNode steps = thresholdsNode.path("steps");
                        if (steps.isArray()) {
                            for (JsonNode step : steps) {
                                if (step.has("value") && !step.get("value").isNull()) {
                                    String color = step.path("color").asText("yellow");
                                    double val = step.get("value").asDouble();
                                    if (color.toLowerCase().contains("red") || color.toLowerCase().contains("critical")) {
                                        thresholdMap.put("critical", val);
                                    } else {
                                        thresholdMap.put("warning", val);
                                    }
                                }
                            }
                        }
                        panel.setThresholds(thresholdMap);
                    }

                    // Extract Grid Pos
                    JsonNode gNode = pNode.path("gridPos");
                    Map<String, Integer> gridPos = new HashMap<>();
                    gridPos.put("x", gNode.path("x").asInt(0));
                    gridPos.put("y", gNode.path("y").asInt(0));
                    gridPos.put("w", gNode.path("w").asInt(12));
                    gridPos.put("h", gNode.path("h").asInt(6));
                    panel.setGridPos(gridPos);

                    panels.add(panel);
                    panelIdx++;
                }
            }

            definition.setPanels(panels);
            return definition;
        } catch (Exception e) {
            throw new IllegalArgumentException("Failed to parse Grafana JSON definition: " + e.getMessage(), e);
        }
    }

    private String mapGrafanaType(String gType) {
        if (gType == null) return "timeseries";
        switch (gType.toLowerCase()) {
            case "stat":
            case "singlestat":
                return "stat";
            case "table":
                return "table";
            case "logs":
            case "loki":
                return "logs";
            case "timeseries":
            case "graph":
            default:
                return "timeseries";
        }
    }
}
