package com.islandpacific.sentinel.integration.teams;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.islandpacific.sentinel.entity.Incident;
import com.islandpacific.sentinel.triage.model.RootCauseResult;
import org.springframework.stereotype.Component;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

/**
 * Builds the Adaptive Card body posted/updated for an incident: severity, title, status, affected
 * systems, and - only when present - the 1.2 triage agent's root cause + evidence. Purely
 * deterministic (no LLM calls here); omits the root-cause section gracefully whenever it is
 * missing or the agent recorded "insufficient data".
 */
@Component
public class AdaptiveCardFactory {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    public ObjectNode build(Incident incident, List<String> affectedSystems, RootCauseResult rootCause) {
        ObjectNode card = MAPPER.createObjectNode();
        card.put("$schema", "http://adaptivecards.io/schemas/adaptive-card.json");
        card.put("type", "AdaptiveCard");
        card.put("version", "1.4");

        ArrayNode body = card.putArray("body");

        ObjectNode title = body.addObject();
        title.put("type", "TextBlock");
        title.put("text", severityEmoji(incident.getSeverity()) + " " + safe(incident.getTitle()));
        title.put("weight", "Bolder");
        title.put("size", "Medium");
        title.put("wrap", true);

        ObjectNode factSet = body.addObject();
        factSet.put("type", "FactSet");
        ArrayNode facts = factSet.putArray("facts");
        addFact(facts, "Severity", capitalize(incident.getSeverity()));
        addFact(facts, "Status", capitalize(incident.getStatus()));
        addFact(facts, "Affected systems", affectedSystems == null || affectedSystems.isEmpty()
                ? "—" : String.join(", ", affectedSystems));
        if (incident.getOpenedAt() != null) {
            addFact(facts, "Opened", DateTimeFormatter.ISO_INSTANT.format(incident.getOpenedAt()));
        }

        addRootCauseSection(body, rootCause);

        return card;
    }

    private void addRootCauseSection(ArrayNode body, RootCauseResult rootCause) {
        if (rootCause == null || rootCause.isInsufficient()
                || rootCause.getRootCauseHypothesis() == null || rootCause.getRootCauseHypothesis().isBlank()) {
            return; // LLM never ran, was unavailable, or found insufficient evidence - omit gracefully
        }

        ObjectNode rootCauseBlock = body.addObject();
        rootCauseBlock.put("type", "TextBlock");
        rootCauseBlock.put("text", "**Root cause:** " + rootCause.getRootCauseHypothesis());
        rootCauseBlock.put("wrap", true);

        if (rootCause.getEvidence() == null || rootCause.getEvidence().isEmpty()) {
            return;
        }
        String evidenceText = rootCause.getEvidence().stream()
                .map(e -> e.getSnippet() != null && !e.getSnippet().isBlank() ? e.getSnippet() : e.getSource())
                .filter(s -> s != null && !s.isBlank())
                .collect(Collectors.joining(" · "));
        if (evidenceText.isBlank()) {
            return;
        }
        ObjectNode evidenceBlock = body.addObject();
        evidenceBlock.put("type", "TextBlock");
        evidenceBlock.put("text", "Evidence: " + evidenceText);
        evidenceBlock.put("wrap", true);
        evidenceBlock.put("isSubtle", true);
    }

    private void addFact(ArrayNode facts, String title, String value) {
        ObjectNode fact = facts.addObject();
        fact.put("title", title);
        fact.put("value", value != null ? value : "—");
    }

    private String severityEmoji(String severity) {
        if (severity == null) {
            return "⚪";
        }
        return switch (severity.toLowerCase(Locale.ROOT)) {
            case "critical" -> "🔴";
            case "high" -> "🟠";
            case "medium" -> "🟡";
            case "low" -> "🟢";
            default -> "⚪";
        };
    }

    private String capitalize(String value) {
        if (value == null || value.isBlank()) {
            return "—";
        }
        return value.substring(0, 1).toUpperCase(Locale.ROOT) + value.substring(1);
    }

    private String safe(String value) {
        return value != null ? value : "(untitled incident)";
    }
}
