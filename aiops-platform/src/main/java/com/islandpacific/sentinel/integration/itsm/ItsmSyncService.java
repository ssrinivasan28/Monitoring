package com.islandpacific.sentinel.integration.itsm;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.islandpacific.sentinel.entity.Incident;
import com.islandpacific.sentinel.entity.IncidentTimeline;
import com.islandpacific.sentinel.entity.IntegrationConfig;
import com.islandpacific.sentinel.entity.ItsmSyncFailure;
import com.islandpacific.sentinel.entity.ItsmTicket;
import com.islandpacific.sentinel.entity.Tenant;
import com.islandpacific.sentinel.repository.IncidentRepository;
import com.islandpacific.sentinel.repository.IncidentTimelineRepository;
import com.islandpacific.sentinel.repository.IntegrationConfigRepository;
import com.islandpacific.sentinel.repository.ItsmSyncFailureRepository;
import com.islandpacific.sentinel.repository.ItsmTicketRepository;
import com.islandpacific.sentinel.repository.TenantRepository;
import com.islandpacific.sentinel.triage.model.RootCauseResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 1.7 - keeps ServiceNow/Jira as system-of-record with lightweight two-way status sync. Mirrors
 * 1.6's poll-and-sync design: a scheduled sweep reconciles every incident, and the manual "Push to
 * ITSM" console action (1.4) calls the very same {@link #syncIncident(UUID, Incident)} method, so
 * behavior never diverges between the two paths.
 * <p>
 * Entirely optional per tenant: a tenant with no enabled "servicenow"/"jira" {@code integration_config}
 * row is skipped with no error - this integration must never block core incident lifecycle work.
 * Failures are retried on the next sweep and, after too many consecutive attempts, dead-lettered so
 * the sweep stops hammering a broken config; a manual push can still retry a dead-lettered incident.
 */
@Service
public class ItsmSyncService {

    private static final Logger log = LoggerFactory.getLogger(ItsmSyncService.class);
    private static final List<String> ITSM_KINDS = List.of(ItsmStateMapper.KIND_SERVICENOW, ItsmStateMapper.KIND_JIRA);
    private static final String RESOLVED_STATUS = "resolved";
    private static final ObjectMapper MAPPER = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    private final TenantRepository tenantRepository;
    private final IncidentRepository incidentRepository;
    private final IncidentTimelineRepository incidentTimelineRepository;
    private final IntegrationConfigRepository integrationConfigRepository;
    private final ItsmTicketRepository itsmTicketRepository;
    private final ItsmSyncFailureRepository itsmSyncFailureRepository;
    private final ItsmSyncProperties properties;
    private final Map<String, ItsmAdapter> adaptersByKind;

    @Autowired
    public ItsmSyncService(
            TenantRepository tenantRepository,
            IncidentRepository incidentRepository,
            IncidentTimelineRepository incidentTimelineRepository,
            IntegrationConfigRepository integrationConfigRepository,
            ItsmTicketRepository itsmTicketRepository,
            ItsmSyncFailureRepository itsmSyncFailureRepository,
            ItsmSyncProperties properties,
            List<ItsmAdapter> adapters) {
        this.tenantRepository = tenantRepository;
        this.incidentRepository = incidentRepository;
        this.incidentTimelineRepository = incidentTimelineRepository;
        this.integrationConfigRepository = integrationConfigRepository;
        this.itsmTicketRepository = itsmTicketRepository;
        this.itsmSyncFailureRepository = itsmSyncFailureRepository;
        this.properties = properties;
        this.adaptersByKind = adapters.stream().collect(Collectors.toMap(ItsmAdapter::kind, a -> a));
    }

    @Scheduled(
            initialDelayString = "#{@itsmSyncProperties.pollIntervalMs}",
            fixedDelayString = "#{@itsmSyncProperties.pollIntervalMs}")
    public void pollAllTenants() {
        for (Tenant tenant : tenantRepository.findAll()) {
            try {
                syncTenant(tenant.getId());
            } catch (Exception e) {
                log.error("ITSM sync failed for tenant {}: {}", tenant.getId(), e.getMessage(), e);
            }
        }
    }

    /**
     * Reconciles every incident for a tenant against its active ITSM system. Skips incidents whose
     * last sync attempt is dead-lettered - a broken config shouldn't be retried forever in the
     * background. Returns 0 with no work done when the tenant has no enabled ServiceNow/Jira
     * integration_config row - not an error, just nothing to do.
     */
    public int syncTenant(UUID tenantId) {
        Optional<ActiveIntegration> active = resolveActiveIntegration(tenantId);
        if (active.isEmpty()) {
            return 0;
        }

        Map<UUID, ItsmSyncFailure> failuresByIncident = itsmSyncFailureRepository.findByTenantId(tenantId).stream()
                .collect(Collectors.toMap(ItsmSyncFailure::getIncidentId, f -> f));

        int synced = 0;
        for (Incident incident : incidentRepository.findByTenantId(tenantId)) {
            ItsmSyncFailure failure = failuresByIncident.get(incident.getId());
            if (failure != null && failure.isDeadLettered()) {
                continue;
            }
            ItsmSyncOutcome outcome = doSync(tenantId, incident, active.get());
            if (isProgress(outcome)) {
                synced++;
            }
        }
        return synced;
    }

    /**
     * Syncs a single incident against the tenant's active ITSM system: creates its ticket if none
     * exists yet, otherwise reconciles whichever side (in-app status vs. ITSM ticket state) changed
     * since the last sync. Unlike the scheduled sweep, always attempts even if previously
     * dead-lettered - an explicit manual push is an operator override. Never throws: every failure
     * is logged, timelined on the incident, and reflected in the returned outcome.
     */
    public ItsmSyncOutcome syncIncident(UUID tenantId, Incident incident) {
        Optional<ActiveIntegration> active = resolveActiveIntegration(tenantId);
        if (active.isEmpty()) {
            return ItsmSyncOutcome.skippedNotConfigured();
        }
        return doSync(tenantId, incident, active.get());
    }

    private ItsmSyncOutcome doSync(UUID tenantId, Incident incident, ActiveIntegration active) {
        if (incident == null || tenantId == null || !tenantId.equals(incident.getTenantId())) {
            // Tenant isolation: never sync using another tenant's incident data, even if this method
            // were ever called with a mismatched pair.
            log.error("Refused to sync an ITSM ticket: incident tenant does not match requested tenant {}", tenantId);
            return ItsmSyncOutcome.failed("Incident does not belong to the requested tenant");
        }

        try {
            Optional<ItsmTicket> existing = itsmTicketRepository.findByTenantIdAndIncidentId(tenantId, incident.getId());
            ItsmSyncOutcome outcome = existing.isEmpty()
                    ? createTicket(tenantId, incident, active)
                    : reconcile(tenantId, incident, active, existing.get());
            clearFailure(tenantId, incident.getId());
            return outcome;
        } catch (ItsmIntegrationException e) {
            log.error("ITSM sync failed for incident {} (tenant {}): {}", incident.getId(), tenantId, e.getMessage());
            recordFailure(tenantId, incident.getId(), active.kind, "SYNC", e.getMessage());
            return ItsmSyncOutcome.failed(e.getMessage());
        } catch (RuntimeException e) {
            log.error("Unexpected error syncing incident {} to ITSM (tenant {}): {}", incident.getId(), tenantId, e.getMessage(), e);
            recordFailure(tenantId, incident.getId(), active.kind, "SYNC", "Unexpected error: " + e.getMessage());
            return ItsmSyncOutcome.failed("Unexpected error contacting the ITSM system");
        }
    }

    private ItsmSyncOutcome createTicket(UUID tenantId, Incident incident, ActiveIntegration active) {
        RootCauseResult rootCause = parseRootCause(incident);
        ItsmTicketRef ref = active.adapter.createTicket(active.configRow.getConfigJson(), incident, rootCause);

        ItsmTicket ticket = new ItsmTicket(tenantId, incident.getId(), active.kind,
                ref.getExternalId(), ref.getExternalUrl(), incident.getStatus(), ref.getItsmState());
        try {
            itsmTicketRepository.save(ticket);
        } catch (DataIntegrityViolationException concurrentInsert) {
            // Another caller (sweep vs. manual push) won the race and already created a ticket -
            // adopt its row rather than tracking (or creating) a duplicate.
            itsmTicketRepository.findByTenantIdAndIncidentId(tenantId, incident.getId())
                    .orElseThrow(() -> concurrentInsert);
            recordTimeline(tenantId, incident.getId(), "itsm_ticket_already_exists",
                    "A ticket for this incident already exists; skipped creating a duplicate");
            return ItsmSyncOutcome.updated();
        }
        recordTimeline(tenantId, incident.getId(), "itsm_ticket_created",
                "Created " + active.kind + " ticket " + ref.getExternalId());
        return ItsmSyncOutcome.created();
    }

    private ItsmSyncOutcome reconcile(UUID tenantId, Incident incident, ActiveIntegration active, ItsmTicket ticket) {
        if (!incident.getStatus().equals(ticket.getLastLocalStatus())) {
            return pushLocalStatusToItsm(tenantId, incident, active, ticket);
        }

        if (bothSidesSettled(active.kind, incident.getStatus(), ticket.getLastItsmState())) {
            return ItsmSyncOutcome.noChange();
        }

        String remoteState = active.adapter.fetchStatus(active.configRow.getConfigJson(), ticket.getExternalId());
        if (remoteState == null || remoteState.equals(ticket.getLastItsmState())) {
            return ItsmSyncOutcome.noChange();
        }
        return pullItsmStateToLocal(tenantId, incident, active, ticket, remoteState);
    }

    private ItsmSyncOutcome pushLocalStatusToItsm(UUID tenantId, Incident incident, ActiveIntegration active, ItsmTicket ticket) {
        String targetState = ItsmStateMapper.toItsmState(active.kind, incident.getStatus());
        active.adapter.updateStatus(active.configRow.getConfigJson(), ticket.getExternalId(), targetState);

        ticket.setLastLocalStatus(incident.getStatus());
        ticket.setLastItsmState(targetState);
        ticket.setUpdatedAt(Instant.now());
        itsmTicketRepository.save(ticket);

        recordTimeline(tenantId, incident.getId(), "itsm_ticket_updated",
                "Updated " + active.kind + " ticket " + ticket.getExternalId() + " to reflect status: " + incident.getStatus());
        return ItsmSyncOutcome.updated();
    }

    private ItsmSyncOutcome pullItsmStateToLocal(UUID tenantId, Incident incident, ActiveIntegration active,
                                                  ItsmTicket ticket, String remoteState) {
        String mappedLocalStatus = ItsmStateMapper.toLocalStatus(active.kind, remoteState);
        if (!mappedLocalStatus.equals(incident.getStatus())) {
            incident.setStatus(mappedLocalStatus);
            if (RESOLVED_STATUS.equals(mappedLocalStatus) && incident.getResolvedAt() == null) {
                incident.setResolvedAt(Instant.now());
            }
            incidentRepository.save(incident);
            recordTimeline(tenantId, incident.getId(), "itsm_status_pulled",
                    "Incident set to '" + mappedLocalStatus + "' from " + active.kind + " ticket " + ticket.getExternalId());
        }

        ticket.setLastItsmState(remoteState);
        ticket.setLastLocalStatus(mappedLocalStatus);
        ticket.setUpdatedAt(Instant.now());
        itsmTicketRepository.save(ticket);
        return ItsmSyncOutcome.pulledFromItsm();
    }

    private boolean bothSidesSettled(String kind, String localStatus, String lastItsmState) {
        return RESOLVED_STATUS.equals(localStatus) && RESOLVED_STATUS.equals(ItsmStateMapper.toLocalStatus(kind, lastItsmState));
    }

    private boolean isProgress(ItsmSyncOutcome outcome) {
        return outcome.getStatus() == ItsmSyncOutcome.Status.CREATED
                || outcome.getStatus() == ItsmSyncOutcome.Status.UPDATED
                || outcome.getStatus() == ItsmSyncOutcome.Status.PULLED_FROM_ITSM;
    }

    private Optional<ActiveIntegration> resolveActiveIntegration(UUID tenantId) {
        for (String kind : ITSM_KINDS) {
            Optional<IntegrationConfig> row = integrationConfigRepository.findByTenantIdAndKind(tenantId, kind);
            if (row.isPresent() && row.get().isEnabled() && adaptersByKind.containsKey(kind)) {
                return Optional.of(new ActiveIntegration(kind, row.get(), adaptersByKind.get(kind)));
            }
        }
        return Optional.empty();
    }

    private void recordFailure(UUID tenantId, UUID incidentId, String kind, String operation, String message) {
        ItsmSyncFailure failure = itsmSyncFailureRepository.findByTenantIdAndIncidentId(tenantId, incidentId)
                .orElse(null);
        if (failure == null) {
            failure = new ItsmSyncFailure(tenantId, incidentId, kind, operation, message);
        } else {
            failure.setKind(kind);
            failure.setOperation(operation);
            failure.setErrorMessage(message);
            failure.setAttemptCount(failure.getAttemptCount() + 1);
            failure.setLastFailedAt(Instant.now());
        }
        if (failure.getAttemptCount() >= properties.getMaxAttempts()) {
            failure.setDeadLettered(true);
        }
        itsmSyncFailureRepository.save(failure);
        recordTimeline(tenantId, incidentId, "itsm_sync_failed", message);
    }

    private void clearFailure(UUID tenantId, UUID incidentId) {
        itsmSyncFailureRepository.findByTenantIdAndIncidentId(tenantId, incidentId)
                .ifPresent(itsmSyncFailureRepository::delete);
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
            log.warn("Incident {} has unparseable root_cause_json; creating ITSM ticket without a narrative: {}",
                    incident.getId(), e.getMessage());
            return null;
        }
    }

    private void recordTimeline(UUID tenantId, UUID incidentId, String eventType, String note) {
        incidentTimelineRepository.save(new IncidentTimeline(incidentId, tenantId, "itsm-integration", eventType, note));
    }

    private static final class ActiveIntegration {
        private final String kind;
        private final IntegrationConfig configRow;
        private final ItsmAdapter adapter;

        private ActiveIntegration(String kind, IntegrationConfig configRow, ItsmAdapter adapter) {
            this.kind = kind;
            this.configRow = configRow;
            this.adapter = adapter;
        }
    }
}
