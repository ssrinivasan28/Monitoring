package com.islandpacific.sentinel.service.dashboard;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public class PanelDefinitionDto {

    private String id;
    private String title;
    private String type; // timeseries | stat | table | logs
    private String query; // PromQL or LogQL
    private String unit; // percent | bytes | ms | ops | status | count
    private Map<String, Object> thresholds;
    private Map<String, Integer> gridPos; // x, y, w, h
    private String legendFormat;

    public PanelDefinitionDto() {}

    public PanelDefinitionDto(String id, String title, String type, String query, String unit, Map<String, Object> thresholds, Map<String, Integer> gridPos) {
        this.id = id;
        this.title = title;
        this.type = type;
        this.query = query;
        this.unit = unit;
        this.thresholds = thresholds;
        this.gridPos = gridPos;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getQuery() { return query; }
    public void setQuery(String query) { this.query = query; }

    public String getUnit() { return unit; }
    public void setUnit(String unit) { this.unit = unit; }

    public Map<String, Object> getThresholds() { return thresholds; }
    public void setThresholds(Map<String, Object> thresholds) { this.thresholds = thresholds; }

    public Map<String, Integer> getGridPos() { return gridPos; }
    public void setGridPos(Map<String, Integer> gridPos) { this.gridPos = gridPos; }

    public String getLegendFormat() { return legendFormat; }
    public void setLegendFormat(String legendFormat) { this.legendFormat = legendFormat; }
}
