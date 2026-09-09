package com.islandpacific.sentinel.service.incident;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.islandpacific.sentinel.entity.Incident;
import com.islandpacific.sentinel.entity.IncidentSignal;
import com.islandpacific.sentinel.entity.IncidentTimeline;
import com.islandpacific.sentinel.repository.IncidentRepository;
import com.islandpacific.sentinel.repository.IncidentSignalRepository;
import com.islandpacific.sentinel.repository.IncidentTimelineRepository;
import com.islandpacific.sentinel.triage.model.RootCauseResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.AbstractMap.SimpleEntry;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 1.3 — read-only query layer behind the tenant-scoped incident REST API: ranking, pagination,
 * filtering, and detail assembly (signals, root cause/evidence, runbook references, similar
 * incidents, timeline). Purely derived from persisted, deterministic data - makes no LLM calls,
 * so it works identically whether the 1.2 triage agent has run or the provider is disabled.
 */
@Service
public class IncidentQueryService {

    private static final Logger log = LoggerFactory.getLogger(IncidentQueryService.class);

    /** Highest to lowest priority; anything unrecognized ranks below all of these. */
    private static final List<String> SEVERITY_RANK = List.of("critical", "high", "medium", "low");
    private static final int SIMILAR_INCIDENTS_LIMIT = 5;

    private static final ObjectMapper MAPPER = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    private final IncidentRepository incidentRepository;
    private final IncidentSignalRepository incidentSignalRepository;
    private final IncidentTimelineRepository incidentTimelineRepository;

    @Autowired
    public IncidentQueryService(
            IncidentRepository incidentRepository,
            IncidentSignalRepository incidentSignalRepository,
            IncidentTimelineRepository incidentTimelineRepository) {
        this.incidentRepository = incidentRepository;
        this.incidentSignalRepository = incidentSignalRepository;
        this.incidentTimelineRepository = incidentTimelineRepository;
    }

    /**
     * Tenant-scoped, ranked (severity desc, then most-recent-first), optionally filtered
     * (status/platform, either may be null for "no filter") and paged incident list.
     */
    public IncidentListResult listIncidents(UUID tenantId, String status, String platform, int page, int size) {
        List<Incident> matched = incidentRepository.findForTenant(tenantId, status, platform);
        Map<UUID, List<IncidentSignal>> signalsByIncident = groupSignalsByIncident(tenantId);

        List<IncidentSummaryDto> ranked = matched.stream()
                .sorted(rankingComparator())
                .map(i -> toSummary(i, signalsByIncident.getOrDefault(i.getId(), List.of())))
                .collect(Collectors.toList());

        int total = ranked.size();
        int from = Math.min(page * size, total);
        int to = Math.min(from + size, total);
        return new IncidentListResult(ranked.subList(from, to), total, page, size);
    }

    /** Tenant-scoped incident detail. Empty if the incident doesn't exist for this tenant. */
    public Optional<IncidentDetailDto> getIncidentDetail(UUID tenantId, UUID incidentId) {
        return incidentRepository.findByTenantIdAndId(tenantId, incidentId)
                .map(incident -> buildDetail(tenantId, incident));
    }

    private Comparator<Incident> rankingComparator() {
        return Comparator.<Incident>comparingInt(i -> severityRank(i.getSeverity()))
                .thenComparing(Incident::getOpenedAt, Comparator.reverseOrder());
    }

    private int severityRank(String severity) {
        if (severity == null) {
            return SEVERITY_RANK.size();
        }
        int idx = SEVERITY_RANK.indexOf(severity.toLowerCase(Locale.ROOT));
        return idx >= 0 ? idx : SEVERITY_RANK.size();
    }

    private Map<UUID, List<IncidentSignal>> groupSignalsByIncident(UUID tenantId) {
        return incidentSignalRepository.findByTenantId(tenantId).stream()
                .collect(Collectors.groupingBy(IncidentSignal::getIncidentId));
    }

    private IncidentSummaryDto toSummary(Incident incident, List<IncidentSignal> signals) {
        IncidentSummaryDto dto = new IncidentSummaryDto();
        dto.setId(incident.getId());
        dto.setTenantId(incident.getTenantId());
        dto.setSeverity(incident.getSeverity());
        dto.setStatus(incident.getStatus());
        dto.setTitle(incident.getTitle());
        dto.setOpenedAt(incident.getOpenedAt());
        dto.setResolvedAt(incident.getResolvedAt());
        dto.setAssigneeUserId(incident.getAssigneeUserId());
        dto.setSignalCount(signals.size());
        dto.setHasRootCause(incident.getRootCauseJson() != null);
        dto.setPlatforms(signals.stream()
                .map(IncidentSignal::getPlatform)
                .distinct()
                .sorted()
                .collect(Collectors.toList()));
        return dto;
    }

    private IncidentDetailDto buildDetail(UUID tenantId, Incident incident) {
        List<IncidentSignal> signals = incidentSignalRepository.findByTenantIdAndIncidentId(tenantId, incident.getId());
        List<IncidentTimeline> timeline = incidentTimelineRepository.findByTenantIdAndIncidentIdOrderByAtAsc(tenantId, incident.getId());

        IncidentDetailDto dto = new IncidentDetailDto();
        dto.setId(incident.getId());
        dto.setTenantId(incident.getTenantId());
        dto.setSeverity(incident.getSeverity());
        dto.setStatus(incident.getStatus());
        dto.setTitle(incident.getTitle());
        dto.setOpenedAt(incident.getOpenedAt());
        dto.setResolvedAt(incident.getResolvedAt());
        dto.setAssigneeUserId(incident.getAssigneeUserId());
        dto.setSignals(signals.stream().map(this::toSignalDto).collect(Collectors.toList()));
        dto.setTimeline(timeline.stream().map(this::toTimelineDto).collect(Collectors.toList()));

        RootCauseResult rootCause = parseRootCause(incident.getId(), incident.getRootCauseJson());
        dto.setRootCause(rootCause);
        dto.setRunbookReferences(extractRunbookReferences(rootCause));
        dto.setSimilarIncidents(findSimilarIncidents(tenantId, incident, signals));
        return dto;
    }

    private IncidentSignalDto toSignalDto(IncidentSignal signal) {
        IncidentSignalDto dto = new IncidentSignalDto();
        dto.setId(signal.getId());
        dto.setMonitorId(signal.getMonitorId());
        dto.setAlertId(signal.getAlertId());
        dto.setPlatform(signal.getPlatform());
        dto.setDetailJson(signal.getDetailJson());
        return dto;
    }

    private IncidentTimelineEntryDto toTimelineDto(IncidentTimeline entry) {
        IncidentTimelineEntryDto dto = new IncidentTimelineEntryDto();
        dto.setId(entry.getId());
        dto.setAt(entry.getAt());
        dto.setActor(entry.getActor());
        dto.setEventType(entry.getEventType());
        dto.setNote(entry.getNote());
        return dto;
    }

    /**
     * Best-effort parse of {@code incidents.root_cause_json} (produced by the 1.2 triage agent).
     * Null/blank (agent hasn't run, or the LLM was unavailable) and malformed JSON both degrade to
     * {@code null} rather than failing the request - the rest of the incident detail still returns.
     */
    private RootCauseResult parseRootCause(UUID incidentId, String rootCauseJson) {
        if (rootCauseJson == null || rootCauseJson.isBlank()) {
            return null;
        }
        try {
            return MAPPER.readValue(rootCauseJson, RootCauseResult.class);
        } catch (Exception e) {
            log.warn("Incident {} has unparseable root_cause_json; omitting from detail response: {}", incidentId, e.getMessage());
            return null;
        }
    }

    /** The subset of cited evidence that came from the knowledge-base/runbook search tool. */
    private List<RootCauseResult.EvidenceItem> extractRunbookReferences(RootCauseResult rootCause) {
        if (rootCause == null || rootCause.getEvidence() == null) {
            return List.of();
        }
        return rootCause.getEvidence().stream()
                .filter(e -> e.getSource() != null && e.getSource().toLowerCase(Locale.ROOT).contains("kb"))
                .collect(Collectors.toList());
    }

    /**
     * Deterministic "similar past incidents" for the same tenant: scored by shared severity and
     * shared signal platform(s) with the incident being viewed, ties broken by recency. Excludes
     * non-matches (score 0) rather than falling back to "most recent" so the list stays meaningful.
     */
    private List<IncidentSummaryDto> findSimilarIncidents(UUID tenantId, Incident incident, List<IncidentSignal> ownSignals) {
        Set<String> ownPlatforms = ownSignals.stream().map(IncidentSignal::getPlatform).collect(Collectors.toSet());
        Map<UUID, List<IncidentSignal>> signalsByIncident = groupSignalsByIncident(tenantId);

        return incidentRepository.findByTenantIdAndIdNot(tenantId, incident.getId()).stream()
                .map(candidate -> new SimpleEntry<>(candidate,
                        similarityScore(incident, ownPlatforms, candidate, signalsByIncident.getOrDefault(candidate.getId(), List.of()))))
                .filter(entry -> entry.getValue() > 0)
                .sorted(Comparator.<SimpleEntry<Incident, Integer>>comparingInt(SimpleEntry::getValue).reversed()
                        .thenComparing(entry -> entry.getKey().getOpenedAt(), Comparator.reverseOrder()))
                .limit(SIMILAR_INCIDENTS_LIMIT)
                .map(entry -> toSummary(entry.getKey(), signalsByIncident.getOrDefault(entry.getKey().getId(), List.of())))
                .collect(Collectors.toList());
    }

    private int similarityScore(Incident base, Set<String> basePlatforms, Incident candidate, List<IncidentSignal> candidateSignals) {
        int score = 0;
        if (base.getSeverity() != null && base.getSeverity().equalsIgnoreCase(candidate.getSeverity())) {
            score += 2;
        }
        Set<String> candidatePlatforms = candidateSignals.stream().map(IncidentSignal::getPlatform).collect(Collectors.toSet());
        candidatePlatforms.retainAll(basePlatforms);
        score += candidatePlatforms.size();
        return score;
    }
}
