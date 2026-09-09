package com.islandpacific.sentinel.integration.teams;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.islandpacific.sentinel.entity.Incident;
import com.islandpacific.sentinel.entity.IncidentSignal;
import com.islandpacific.sentinel.entity.IncidentTimeline;
import com.islandpacific.sentinel.entity.IntegrationConfig;
import com.islandpacific.sentinel.entity.Monitor;
import com.islandpacific.sentinel.entity.Tenant;
import com.islandpacific.sentinel.entity.TeamsNotification;
import com.islandpacific.sentinel.repository.IncidentRepository;
import com.islandpacific.sentinel.repository.IncidentSignalRepository;
import com.islandpacific.sentinel.repository.IncidentTimelineRepository;
import com.islandpacific.sentinel.repository.IntegrationConfigRepository;
import com.islandpacific.sentinel.repository.MonitorRepository;
import com.islandpacific.sentinel.repository.TeamsNotificationRepository;
import com.islandpacific.sentinel.repository.TenantRepository;
import com.islandpacific.sentinel.security.SecretProtector;
import com.islandpacific.sentinel.triage.model.RootCauseResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 1.6 - posts/updates a Microsoft Teams Adaptive Card per incident. Follows the same poll-and-sync
 * pattern as 1.1's correlation engine and 1.2's triage agent: a scheduled sweep finds incidents
 * whose Teams card is missing or stale (status changed since last sync) and pushes them. The
 * manual "Push to Teams" button (1.4) calls the very same {@link #pushIncidentCard} method used by
 * the sweep, so behavior never diverges between the two paths.
 * <p>
 * Entirely optional per tenant: a tenant with no enabled "teams" {@code integration_config} row is
 * skipped with no error - this integration must never block or fail incident correlation/lifecycle
 * work, matching the platform's graceful-degradation rule for non-core capabilities.
 */
@Service
public class TeamsNotificationService {

    private static final Logger log = LoggerFactory.getLogger(TeamsNotificationService.class);
    private static final String KIND = "teams";
    private static final ObjectMapper MAPPER = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    private final TenantRepository tenantRepository;
    private final IncidentRepository incidentRepository;
    private final IncidentSignalRepository incidentSignalRepository;
    private final IncidentTimelineRepository incidentTimelineRepository;
    private final MonitorRepository monitorRepository;
    private final IntegrationConfigRepository integrationConfigRepository;
    private final TeamsNotificationRepository teamsNotificationRepository;
    private final SecretProtector secretProtector;
    private final TeamsAuthTokenProvider tokenProvider;
    private final TeamsGraphClient graphClient;
    private final AdaptiveCardFactory cardFactory;

    @Autowired
    public TeamsNotificationService(
            TenantRepository tenantRepository,
            IncidentRepository incidentRepository,
            IncidentSignalRepository incidentSignalRepository,
            IncidentTimelineRepository incidentTimelineRepository,
            MonitorRepository monitorRepository,
            IntegrationConfigRepository integrationConfigRepository,
            TeamsNotificationRepository teamsNotificationRepository,
            SecretProtector secretProtector,
            TeamsAuthTokenProvider tokenProvider,
            TeamsGraphClient graphClient,
            AdaptiveCardFactory cardFactory) {
        this.tenantRepository = tenantRepository;
        this.incidentRepository = incidentRepository;
        this.incidentSignalRepository = incidentSignalRepository;
        this.incidentTimelineRepository = incidentTimelineRepository;
        this.monitorRepository = monitorRepository;
        this.integrationConfigRepository = integrationConfigRepository;
        this.teamsNotificationRepository = teamsNotificationRepository;
        this.secretProtector = secretProtector;
        this.tokenProvider = tokenProvider;
        this.graphClient = graphClient;
        this.cardFactory = cardFactory;
    }

    @Scheduled(
            initialDelayString = "#{@teamsNotificationProperties.pollIntervalMs}",
            fixedDelayString = "#{@teamsNotificationProperties.pollIntervalMs}")
    public void pollAllTenants() {
        for (Tenant tenant : tenantRepository.findAll()) {
            try {
                syncTenant(tenant.getId());
            } catch (Exception e) {
                log.error("Teams notification sync failed for tenant {}: {}", tenant.getId(), e.getMessage(), e);
            }
        }
    }

    /**
     * Syncs every incident for a tenant whose Teams card is missing or whose last-synced status is
     * stale. Returns 0 with no work done when the tenant has no enabled "teams" integration_config
     * row - not an error, just nothing to do.
     */
    public int syncTenant(UUID tenantId) {
        Optional<IntegrationConfig> configRow = integrationConfigRepository.findByTenantIdAndKind(tenantId, KIND);
        if (configRow.isEmpty() || !configRow.get().isEnabled()) {
            return 0;
        }

        Map<UUID, TeamsNotification> existingByIncident = teamsNotificationRepository.findByTenantId(tenantId).stream()
                .collect(Collectors.toMap(TeamsNotification::getIncidentId, n -> n));

        int synced = 0;
        for (Incident incident : incidentRepository.findByTenantId(tenantId)) {
            TeamsNotification existing = existingByIncident.get(incident.getId());
            if (existing != null && existing.getLastStatus().equals(incident.getStatus())) {
                continue; // already reflects the current status - nothing to push
            }
            TeamsSyncOutcome outcome = pushIncidentCard(tenantId, incident, configRow.get());
            if (outcome.getStatus() == TeamsSyncOutcome.Status.CREATED || outcome.getStatus() == TeamsSyncOutcome.Status.UPDATED) {
                synced++;
            }
        }
        return synced;
    }

    /**
     * Posts a new card for {@code incident} or updates its existing one, whichever applies. Shared
     * by the scheduled sweep above and the manual "Push to Teams" console action. Never throws -
     * every failure is logged, timelined on the incident, and reflected in the returned outcome.
     */
    public TeamsSyncOutcome pushIncidentCard(UUID tenantId, Incident incident) {
        Optional<IntegrationConfig> configRow = integrationConfigRepository.findByTenantIdAndKind(tenantId, KIND);
        if (configRow.isEmpty() || !configRow.get().isEnabled()) {
            return TeamsSyncOutcome.skippedNotConfigured();
        }
        return pushIncidentCard(tenantId, incident, configRow.get());
    }

    private TeamsSyncOutcome pushIncidentCard(UUID tenantId, Incident incident, IntegrationConfig configRow) {
        if (incident == null || tenantId == null || !tenantId.equals(incident.getTenantId())) {
            // Tenant isolation: never post using another tenant's incident data, even if this method
            // were ever called with a mismatched pair.
            log.error("Refused to push a Teams card: incident tenant does not match requested tenant {}", tenantId);
            return TeamsSyncOutcome.failed("Incident does not belong to the requested tenant");
        }

        TeamsChannelConfig channelConfig;
        try {
            channelConfig = TeamsChannelConfig.fromJson(MAPPER, configRow.getConfigJson(), secretProtector);
        } catch (IllegalArgumentException e) {
            log.error("Tenant {} has an invalid Teams integration config: {}", tenantId, e.getMessage());
            return TeamsSyncOutcome.failed("Teams integration is misconfigured for this tenant");
        }

        try {
            String accessToken = tokenProvider.getAccessToken(channelConfig);
            ObjectNode card = cardFactory.build(incident, affectedSystems(tenantId, incident.getId()), parseRootCause(incident));

            Optional<TeamsNotification> existing = teamsNotificationRepository.findByTenantIdAndIncidentId(tenantId, incident.getId());
            if (existing.isPresent()) {
                return updateExistingCard(tenantId, incident, channelConfig, accessToken, card, existing.get());
            }
            return createNewCard(tenantId, incident, channelConfig, accessToken, card);
        } catch (TeamsIntegrationException e) {
            log.error("Failed to push incident {} to Teams for tenant {}: {}", incident.getId(), tenantId, e.getMessage());
            recordTimeline(tenantId, incident.getId(), "teams_notify_failed", e.getMessage());
            return TeamsSyncOutcome.failed(e.getMessage());
        } catch (RuntimeException e) {
            log.error("Unexpected error pushing incident {} to Teams for tenant {}: {}", incident.getId(), tenantId, e.getMessage(), e);
            recordTimeline(tenantId, incident.getId(), "teams_notify_failed", "Unexpected error: " + e.getMessage());
            return TeamsSyncOutcome.failed("Unexpected error contacting Microsoft Teams");
        }
    }

    private TeamsSyncOutcome createNewCard(UUID tenantId, Incident incident, TeamsChannelConfig channelConfig,
                                            String accessToken, ObjectNode card) {
        String messageId = graphClient.postCard(channelConfig, accessToken, card);
        TeamsNotification notification = new TeamsNotification(
                tenantId, incident.getId(), channelConfig.getChannelId(), messageId, incident.getStatus());
        try {
            teamsNotificationRepository.save(notification);
        } catch (DataIntegrityViolationException concurrentInsert) {
            // Another caller (sweep vs. manual push) won the race and already created the row -
            // fall back to updating it instead of leaving an orphaned duplicate Teams message.
            TeamsNotification winner = teamsNotificationRepository.findByTenantIdAndIncidentId(tenantId, incident.getId())
                    .orElseThrow(() -> concurrentInsert);
            graphClient.updateCard(channelConfig, accessToken, winner.getMessageId(), card);
            winner.setLastStatus(incident.getStatus());
            winner.setUpdatedAt(Instant.now());
            teamsNotificationRepository.save(winner);
            recordTimeline(tenantId, incident.getId(), "teams_card_updated",
                    "Teams card updated (status: " + incident.getStatus() + ")");
            return TeamsSyncOutcome.updated();
        }
        recordTimeline(tenantId, incident.getId(), "teams_card_posted", "Incident posted to the tenant's Teams channel");
        return TeamsSyncOutcome.created();
    }

    private TeamsSyncOutcome updateExistingCard(UUID tenantId, Incident incident, TeamsChannelConfig channelConfig,
                                                 String accessToken, ObjectNode card, TeamsNotification notification) {
        graphClient.updateCard(channelConfig, accessToken, notification.getMessageId(), card);
        notification.setLastStatus(incident.getStatus());
        notification.setUpdatedAt(Instant.now());
        teamsNotificationRepository.save(notification);
        recordTimeline(tenantId, incident.getId(), "teams_card_updated",
                "Teams card updated (status: " + incident.getStatus() + ")");
        return TeamsSyncOutcome.updated();
    }

    private List<String> affectedSystems(UUID tenantId, UUID incidentId) {
        List<IncidentSignal> signals = incidentSignalRepository.findByTenantIdAndIncidentId(tenantId, incidentId);
        Set<UUID> monitorIds = signals.stream()
                .map(IncidentSignal::getMonitorId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<UUID, Monitor> monitorsById = monitorRepository.findByTenantId(tenantId).stream()
                .filter(m -> monitorIds.contains(m.getId()))
                .collect(Collectors.toMap(Monitor::getId, m -> m));

        Set<String> names = new LinkedHashSet<>();
        for (IncidentSignal signal : signals) {
            Monitor monitor = signal.getMonitorId() != null ? monitorsById.get(signal.getMonitorId()) : null;
            names.add(monitor != null ? monitor.getName() + " (" + monitor.getPlatform() + ")" : signal.getPlatform());
        }
        return List.copyOf(names);
    }

    /** Best-effort parse mirroring IncidentQueryService's: malformed/absent JSON degrades to null, never throws. */
    private RootCauseResult parseRootCause(Incident incident) {
        String json = incident.getRootCauseJson();
        if (json == null || json.isBlank()) {
            return null;
        }
        try {
            return MAPPER.readValue(json, RootCauseResult.class);
        } catch (Exception e) {
            log.warn("Incident {} has unparseable root_cause_json; posting Teams card without a narrative: {}",
                    incident.getId(), e.getMessage());
            return null;
        }
    }

    private void recordTimeline(UUID tenantId, UUID incidentId, String eventType, String note) {
        incidentTimelineRepository.save(new IncidentTimeline(incidentId, tenantId, "teams-integration", eventType, note));
    }
}
