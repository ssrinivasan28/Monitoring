package com.islandpacific.sentinel.fleet;

import java.util.List;
import java.util.UUID;

public class TenantFleetDto {

    private UUID tenantId;
    private String name;
    private String clientInstanceId;
    private String band; // GREEN, AMBER, RED, UNKNOWN
    private double score; // 0.0 - 100.0
    private String worstInput; // key of worst input e.g. "asp"
    private List<InputScoreDto> inputs;

    public TenantFleetDto() {}

    public TenantFleetDto(UUID tenantId, String name, String clientInstanceId, String band, double score, String worstInput, List<InputScoreDto> inputs) {
        this.tenantId = tenantId;
        this.name = name;
        this.clientInstanceId = clientInstanceId;
        this.band = band;
        this.score = score;
        this.worstInput = worstInput;
        this.inputs = inputs;
    }

    public UUID getTenantId() { return tenantId; }
    public void setTenantId(UUID tenantId) { this.tenantId = tenantId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getClientInstanceId() { return clientInstanceId; }
    public void setClientInstanceId(String clientInstanceId) { this.clientInstanceId = clientInstanceId; }

    public String getBand() { return band; }
    public void setBand(String band) { this.band = band; }

    public double getScore() { return score; }
    public void setScore(double score) { this.score = score; }

    public String getWorstInput() { return worstInput; }
    public void setWorstInput(String worstInput) { this.worstInput = worstInput; }

    public List<InputScoreDto> getInputs() { return inputs; }
    public void setInputs(List<InputScoreDto> inputs) { this.inputs = inputs; }

    public static class InputScoreDto {
        private String key;
        private String name;
        private Double value; // null if stale/missing
        private String band; // GREEN, AMBER, RED, UNKNOWN
        private boolean stale;

        public InputScoreDto() {}

        public InputScoreDto(String key, String name, Double value, String band, boolean stale) {
            this.key = key;
            this.name = name;
            this.value = value;
            this.band = band;
            this.stale = stale;
        }

        public String getKey() { return key; }
        public void setKey(String key) { this.key = key; }

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }

        public Double getValue() { return value; }
        public void setValue(Double value) { this.value = value; }

        public String getBand() { return band; }
        public void setBand(String band) { this.band = band; }

        public boolean isStale() { return stale; }
        public void setStale(boolean stale) { this.stale = stale; }
    }
}
