package com.islandpacific.sentinel.integration.teams;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.islandpacific.sentinel.entity.Incident;
import com.islandpacific.sentinel.triage.model.RootCauseResult;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class AdaptiveCardFactoryTest {

    private final AdaptiveCardFactory factory = new AdaptiveCardFactory();

    private Incident incident(String severity, String status) {
        Incident incident = new Incident(UUID.randomUUID(), severity, status, "Disk pressure on APP01");
        incident.setId(UUID.randomUUID());
        return incident;
    }

    @Test
    void build_includesSeverityStatusAndAffectedSystems() {
        ObjectNode card = factory.build(incident("critical", "open"), List.of("APP01 (windows)", "PRDLIB (ibmi)"), null);

        assertThat(card.get("type").asText()).isEqualTo("AdaptiveCard");
        String json = card.toString();
        assertThat(json).contains("Disk pressure on APP01");
        assertThat(json).contains("Critical");
        assertThat(json).contains("Open");
        assertThat(json).contains("APP01 (windows), PRDLIB (ibmi)");
    }

    @Test
    void build_noRootCause_omitsRootCauseSectionGracefully() {
        ObjectNode card = factory.build(incident("low", "open"), List.of(), null);

        assertThat(card.toString()).doesNotContain("Root cause");
    }

    @Test
    void build_insufficientRootCause_omitsRootCauseSection() {
        ObjectNode card = factory.build(incident("low", "open"), List.of(), RootCauseResult.insufficient());

        assertThat(card.toString()).doesNotContain("Root cause");
    }

    @Test
    void build_presentRootCause_includesHypothesisAndEvidence() {
        RootCauseResult rootCause = new RootCauseResult();
        rootCause.setRootCauseHypothesis("Disk volume nearly full due to runaway log growth");
        rootCause.setEvidence(List.of(new RootCauseResult.EvidenceItem("promql", "disk_free_pct", "12% free on C:")));
        rootCause.setConfidence(0.82);

        ObjectNode card = factory.build(incident("high", "open"), List.of("APP01 (windows)"), rootCause);

        String json = card.toString();
        assertThat(json).contains("Root cause");
        assertThat(json).contains("Disk volume nearly full due to runaway log growth");
        assertThat(json).contains("12% free on C:");
    }

    @Test
    void build_affectedSystemsEmpty_stillProducesValidCard() {
        ObjectNode card = factory.build(incident("medium", "open"), List.of(), null);

        assertThat(card.get("body").isArray()).isTrue();
        assertThat(card.toString()).contains("—");
    }
}
