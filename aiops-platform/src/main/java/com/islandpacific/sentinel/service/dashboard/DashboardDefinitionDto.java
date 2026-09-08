package com.islandpacific.sentinel.service.dashboard;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public class DashboardDefinitionDto {

    private String id;
    private String title;
    private String category; // e.g. "Windows", "IBM i", "Network & Services", "Custom"
    private List<String> variables = new ArrayList<>();
    private List<PanelDefinitionDto> panels = new ArrayList<>();
    private boolean custom = false;

    public DashboardDefinitionDto() {}

    public DashboardDefinitionDto(String id, String title, String category, List<String> variables, List<PanelDefinitionDto> panels) {
        this.id = id;
        this.title = title;
        this.category = category;
        this.variables = variables != null ? variables : new ArrayList<>();
        this.panels = panels != null ? panels : new ArrayList<>();
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public List<String> getVariables() { return variables; }
    public void setVariables(List<String> variables) { this.variables = variables; }

    public List<PanelDefinitionDto> getPanels() { return panels; }
    public void setPanels(List<PanelDefinitionDto> panels) { this.panels = panels; }

    public boolean isCustom() { return custom; }
    public void setCustom(boolean custom) { this.custom = custom; }
}
