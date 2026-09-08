package com.islandpacific.sentinel.triage.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;
import java.util.List;

/**
 * Structured root-cause output required from the 1.2 triage agent. Field names match the JSON
 * shape the LLM is instructed to produce (see TriageAgentService.SYSTEM_PROMPT).
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class RootCauseResult {

    @JsonProperty("root_cause_hypothesis")
    private String rootCauseHypothesis;

    private List<EvidenceItem> evidence = new ArrayList<>();

    private String severity;

    @JsonProperty("suggested_checks")
    private List<String> suggestedChecks = new ArrayList<>();

    private Double confidence;

    /** True only for the synthetic "insufficient data" record persisted after a failed retry. */
    private boolean insufficient;

    public RootCauseResult() {}

    public static RootCauseResult insufficient() {
        RootCauseResult r = new RootCauseResult();
        r.rootCauseHypothesis = "insufficient data";
        r.confidence = 0.0;
        r.insufficient = true;
        return r;
    }

    public String getRootCauseHypothesis() { return rootCauseHypothesis; }
    public void setRootCauseHypothesis(String rootCauseHypothesis) { this.rootCauseHypothesis = rootCauseHypothesis; }

    public List<EvidenceItem> getEvidence() { return evidence; }
    public void setEvidence(List<EvidenceItem> evidence) { this.evidence = evidence != null ? evidence : new ArrayList<>(); }

    public String getSeverity() { return severity; }
    public void setSeverity(String severity) { this.severity = severity; }

    public List<String> getSuggestedChecks() { return suggestedChecks; }
    public void setSuggestedChecks(List<String> suggestedChecks) { this.suggestedChecks = suggestedChecks != null ? suggestedChecks : new ArrayList<>(); }

    public Double getConfidence() { return confidence; }
    public void setConfidence(Double confidence) { this.confidence = confidence; }

    public boolean isInsufficient() { return insufficient; }
    public void setInsufficient(boolean insufficient) { this.insufficient = insufficient; }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class EvidenceItem {
        private String source;
        private String query;
        private String snippet;

        public EvidenceItem() {}

        public EvidenceItem(String source, String query, String snippet) {
            this.source = source;
            this.query = query;
            this.snippet = snippet;
        }

        public String getSource() { return source; }
        public void setSource(String source) { this.source = source; }

        public String getQuery() { return query; }
        public void setQuery(String query) { this.query = query; }

        public String getSnippet() { return snippet; }
        public void setSnippet(String snippet) { this.snippet = snippet; }
    }
}
