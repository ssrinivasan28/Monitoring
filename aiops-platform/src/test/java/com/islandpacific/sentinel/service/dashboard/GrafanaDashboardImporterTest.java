package com.islandpacific.sentinel.service.dashboard;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class GrafanaDashboardImporterTest {

    private GrafanaDashboardImporter importer;

    @BeforeEach
    void setUp() {
        importer = new GrafanaDashboardImporter(new ObjectMapper());
    }

    @Test
    void testConvertGrafanaJsonToSentinelDefinition() {
        String grafanaJson = """
                {
                  "title": "Windows Performance Test",
                  "uid": "win-perf-test",
                  "panels": [
                    {
                      "id": 1,
                      "title": "Header Row",
                      "type": "row"
                    },
                    {
                      "id": 2,
                      "title": "CPU Stat Panel",
                      "type": "stat",
                      "gridPos": { "x": 0, "y": 1, "w": 6, "h": 4 },
                      "fieldConfig": {
                        "defaults": {
                          "unit": "percent",
                          "thresholds": {
                            "steps": [
                              { "value": null, "color": "green" },
                              { "value": 75, "color": "orange" },
                              { "value": 90, "color": "red" }
                            ]
                          }
                        }
                      },
                      "targets": [
                        { "expr": "avg(windows_cpu_usage_percent{server=\\"$location\\"})", "legendFormat": "CPU %" }
                      ]
                    },
                    {
                      "id": 3,
                      "title": "Memory Graph Panel",
                      "type": "timeseries",
                      "gridPos": { "x": 6, "y": 1, "w": 6, "h": 4 },
                      "fieldConfig": { "defaults": { "unit": "percent" } },
                      "targets": [
                        { "expr": "windows_memory_usage_percent", "legendFormat": "{{server}}" }
                      ]
                    }
                  ]
                }
                """;

        DashboardDefinitionDto result = importer.convertGrafanaToSentinel(grafanaJson);

        assertNotNull(result);
        assertEquals("win-perf-test", result.getId());
        assertEquals("Windows Performance Test", result.getTitle());
        assertEquals("Imported", result.getCategory());
        // Should filter out 'row' panel, so 2 panels remaining
        assertEquals(2, result.getPanels().size());

        PanelDefinitionDto panel1 = result.getPanels().get(0);
        assertEquals("p-2", panel1.getId());
        assertEquals("CPU Stat Panel", panel1.getTitle());
        assertEquals("stat", panel1.getType());
        assertEquals("avg(windows_cpu_usage_percent{server=\"$location\"})", panel1.getQuery());
        assertEquals("percent", panel1.getUnit());
        assertEquals(75.0, panel1.getThresholds().get("warning"));
        assertEquals(90.0, panel1.getThresholds().get("critical"));

        PanelDefinitionDto panel2 = result.getPanels().get(1);
        assertEquals("p-3", panel2.getId());
        assertEquals("timeseries", panel2.getType());
        assertEquals("windows_memory_usage_percent", panel2.getQuery());
    }
}
