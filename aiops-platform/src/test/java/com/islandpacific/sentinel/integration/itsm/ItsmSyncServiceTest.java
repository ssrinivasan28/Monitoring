package com.islandpacific.sentinel.integration.itsm;

import com.islandpacific.sentinel.entity.Incident;
import com.islandpacific.sentinel.entity.IntegrationConfig;
import com.islandpacific.sentinel.entity.ItsmSyncFailure;
import com.islandpacific.sentinel.entity.ItsmTicket;
import com.islandpacific.sentinel.repository.IncidentRepository;
import com.islandpacific.sentinel.repository.IncidentTimelineRepository;
import com.islandpacific.sentinel.repository.IntegrationConfigRepository;
import com.islandpacific.sentinel.repository.ItsmSyncFailureRepository;
import com.islandpacific.sentinel.repository.ItsmTicketRepository;
import com.islandpacific.sentinel.repository.TenantRepository;
import com.islandpacific.sentinel.triage.model.RootCauseResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * 1.7 ITSM two-way sync behavioral coverage: one ticket per incident, idempotency on retry,
 * bidirectional status reconciliation (in-app resolve pushes to ITSM, ITSM resolve pulls to
 * in-app), retry-with-dead-letter, and strict tenant isolation. Follows this codebase's
 * in-memory-fake-repository convention (see TeamsNotificationServiceTest) rather than mocking
 * every call individually; a hand-written {@link FakeItsmAdapter} plays the role of a mock ITSM.
 */
class ItsmSyncServiceTest {

    private static final String TENANT_A_CONFIG_JSON = "{\"instanceUrl\":\"https://a.service-now.com\",\"username\":\"u\",\"password\":\"p\"}";
    private static final String TENANT_B_CONFIG_JSON = "{\"instanceUrl\":\"https://b.service-now.com\",\"username\":\"u\",\"password\":\"p\"}";

    private final UUID tenantA = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private final UUID tenantB = UUID.fromString("22222222-2222-2222-2222-222222222222");

    private List<ItsmTicket> tickets;
    private List<ItsmSyncFailure> failures;
    private List<IntegrationConfig> integrationConfigs;

    private TenantRepository tenantRepository;
    private IncidentRepository incidentRepository;
    private IncidentTimelineRepository incidentTimelineRepository;
    private IntegrationConfigRepository integrationConfigRepository;
    private ItsmTicketRepository itsmTicketRepository;
    private ItsmSyncFailureRepository itsmSyncFailureRepository;
    private ItsmSyncProperties properties;
    private FakeItsmAdapter serviceNowAdapter;
    private ItsmSyncService service;

    @BeforeEach
    void setUp() {
        tickets = new ArrayList<>();
        failures = new ArrayList<>();
        integrationConfigs = new ArrayList<>();
        integrationConfigs.add(new IntegrationConfig(tenantA, "servicenow", TENANT_A_CONFIG_JSON, true));
        integrationConfigs.add(new IntegrationConfig(tenantB, "servicenow", TENANT_B_CONFIG_JSON, true));

        tenantRepository = mock(TenantRepository.class);
        incidentRepository = mock(IncidentRepository.class);
        incidentTimelineRepository = mock(IncidentTimelineRepository.class);

        integrationConfigRepository = mock(IntegrationConfigRepository.class);
        when(integrationConfigRepository.findByTenantIdAndKind(any(), any())).thenAnswer(inv -> {
            UUID tenantId = inv.getArgument(0);
            String kind = inv.getArgument(1);
            return integrationConfigs.stream()
                    .filter(c -> c.getTenantId().equals(tenantId) && c.getKind().equals(kind))
                    .findFirst();
        });

        itsmTicketRepository = mock(ItsmTicketRepository.class);
        when(itsmTicketRepository.findByTenantIdAndIncidentId(any(), any())).thenAnswer(inv -> {
            UUID tenantId = inv.getArgument(0);
            UUID incidentId = inv.getArgument(1);
            return tickets.stream()
                    .filter(t -> t.getTenantId().equals(tenantId) && t.getIncidentId().equals(incidentId))
                    .findFirst();
        });
        when(itsmTicketRepository.save(any())).thenAnswer(inv -> {
            ItsmTicket t = inv.getArgument(0);
            if (t.getId() == null) {
                t.setId(UUID.randomUUID());
            }
            tickets.removeIf(x -> x.getId().equals(t.getId()));
            tickets.add(t);
            return t;
        });

        itsmSyncFailureRepository = mock(ItsmSyncFailureRepository.class);
        when(itsmSyncFailureRepository.findByTenantIdAndIncidentId(any(), any())).thenAnswer(inv -> {
            UUID tenantId = inv.getArgument(0);
            UUID incidentId = inv.getArgument(1);
            return failures.stream()
                    .filter(f -> f.getTenantId().equals(tenantId) && f.getIncidentId().equals(incidentId))
                    .findFirst();
        });
        when(itsmSyncFailureRepository.findByTenantId(any())).thenAnswer(inv -> {
            UUID tenantId = inv.getArgument(0);
            return failures.stream().filter(f -> f.getTenantId().equals(tenantId)).collect(Collectors.toList());
        });
        when(itsmSyncFailureRepository.save(any())).thenAnswer(inv -> {
            ItsmSyncFailure f = inv.getArgument(0);
            if (f.getId() == null) {
                f.setId(UUID.randomUUID());
            }
            failures.removeIf(x -> x.getId().equals(f.getId()));
            failures.add(f);
            return f;
        });
        doAnswer(inv -> {
            ItsmSyncFailure f = inv.getArgument(0);
            failures.removeIf(x -> x.getId() != null && x.getId().equals(f.getId()));
            return null;
        }).when(itsmSyncFailureRepository).delete(any());

        properties = new ItsmSyncProperties();
        properties.setMaxAttempts(3);

        serviceNowAdapter = new FakeItsmAdapter("servicenow");

        service = new ItsmSyncService(
                tenantRepository, incidentRepository, incidentTimelineRepository,
                integrationConfigRepository, itsmTicketRepository, itsmSyncFailureRepository,
                properties, List.of(serviceNowAdapter));
    }

    private Incident newIncident(UUID tenantId, String status) {
        Incident incident = new Incident(tenantId, "high", status, "Disk pressure on APP01");
        incident.setId(UUID.randomUUID());
        return incident;
    }

    @Test
    void syncIncident_newIncident_createsExactlyOneTicket() {
        Incident incident = newIncident(tenantA, "open");

        ItsmSyncOutcome outcome = service.syncIncident(tenantA, incident);

        assertThat(outcome.getStatus()).isEqualTo(ItsmSyncOutcome.Status.CREATED);
        assertThat(serviceNowAdapter.createCallCount).isEqualTo(1);
        assertThat(tickets).hasSize(1);
        assertThat(tickets.get(0).getLastLocalStatus()).isEqualTo("open");
    }

    @Test
    void syncIncident_calledTwiceWithNoStatusChange_doesNotCreateASecondTicket() {
        Incident incident = newIncident(tenantA, "open");

        service.syncIncident(tenantA, incident);
        ItsmSyncOutcome second = service.syncIncident(tenantA, incident);

        assertThat(second.getStatus()).isEqualTo(ItsmSyncOutcome.Status.NO_CHANGE);
        assertThat(serviceNowAdapter.createCallCount).isEqualTo(1);
        assertThat(tickets).hasSize(1);
    }

    @Test
    void syncIncident_localResolve_pushesResolvedStateToItsm() {
        Incident incident = newIncident(tenantA, "open");
        service.syncIncident(tenantA, incident);
        String externalId = tickets.get(0).getExternalId();

        incident.setStatus("resolved");
        ItsmSyncOutcome outcome = service.syncIncident(tenantA, incident);

        assertThat(outcome.getStatus()).isEqualTo(ItsmSyncOutcome.Status.UPDATED);
        assertThat(serviceNowAdapter.updateCallCount).isEqualTo(1);
        assertThat(serviceNowAdapter.remoteStateOf(externalId)).isEqualTo(ItsmStateMapper.toItsmState("servicenow", "resolved"));
        assertThat(tickets.get(0).getLastLocalStatus()).isEqualTo("resolved");
    }

    @Test
    void syncIncident_remoteResolve_pullsResolvedStatusIntoIncident() {
        Incident incident = newIncident(tenantA, "open");
        service.syncIncident(tenantA, incident);
        String externalId = tickets.get(0).getExternalId();

        // The ticket is resolved on the ITSM side, outside of this platform.
        serviceNowAdapter.simulateRemoteChange(externalId, ItsmStateMapper.toItsmState("servicenow", "resolved"));

        ItsmSyncOutcome outcome = service.syncIncident(tenantA, incident);

        assertThat(outcome.getStatus()).isEqualTo(ItsmSyncOutcome.Status.PULLED_FROM_ITSM);
        assertThat(incident.getStatus()).isEqualTo("resolved");
        assertThat(incident.getResolvedAt()).isNotNull();
        assertThat(tickets.get(0).getLastLocalStatus()).isEqualTo("resolved");
    }

    @Test
    void syncIncident_bothSidesAlreadyResolved_stopsPollingRemoteState() {
        Incident incident = newIncident(tenantA, "open");
        service.syncIncident(tenantA, incident);
        incident.setStatus("resolved");
        service.syncIncident(tenantA, incident); // pushes resolved to ITSM
        int fetchesSoFar = serviceNowAdapter.fetchCallCount;

        ItsmSyncOutcome outcome = service.syncIncident(tenantA, incident);

        assertThat(outcome.getStatus()).isEqualTo(ItsmSyncOutcome.Status.NO_CHANGE);
        assertThat(serviceNowAdapter.fetchCallCount).isEqualTo(fetchesSoFar); // no wasted remote calls
    }

    @Test
    void syncIncident_notConfigured_skipsWithNoAdapterCalls() {
        UUID tenantWithoutIntegration = UUID.randomUUID();
        Incident incident = newIncident(tenantWithoutIntegration, "open");

        ItsmSyncOutcome outcome = service.syncIncident(tenantWithoutIntegration, incident);

        assertThat(outcome.getStatus()).isEqualTo(ItsmSyncOutcome.Status.SKIPPED_NOT_CONFIGURED);
        assertThat(serviceNowAdapter.createCallCount).isZero();
    }

    @Test
    void syncIncident_disabledIntegration_skipsWithNoAdapterCalls() {
        integrationConfigs.clear();
        integrationConfigs.add(new IntegrationConfig(tenantA, "servicenow", TENANT_A_CONFIG_JSON, false));
        Incident incident = newIncident(tenantA, "open");

        ItsmSyncOutcome outcome = service.syncIncident(tenantA, incident);

        assertThat(outcome.getStatus()).isEqualTo(ItsmSyncOutcome.Status.SKIPPED_NOT_CONFIGURED);
        assertThat(serviceNowAdapter.createCallCount).isZero();
    }

    @Test
    void syncIncident_incidentBelongsToAnotherTenant_refusesAndNeverCallsAdapter() {
        Incident incidentForTenantB = newIncident(tenantB, "open");

        ItsmSyncOutcome outcome = service.syncIncident(tenantA, incidentForTenantB);

        assertThat(outcome.getStatus()).isEqualTo(ItsmSyncOutcome.Status.FAILED);
        assertThat(serviceNowAdapter.createCallCount).isZero();
        assertThat(tickets).isEmpty();
    }

    @Test
    void syncIncident_usesOnlyTheRequestingTenantsOwnConfig() {
        Incident incidentA = newIncident(tenantA, "open");
        Incident incidentB = newIncident(tenantB, "open");

        service.syncIncident(tenantA, incidentA);
        service.syncIncident(tenantB, incidentB);

        assertThat(tickets).hasSize(2);
        assertThat(serviceNowAdapter.createCallCount).isEqualTo(2);
    }

    @Test
    void syncIncident_createFails_recordsFailureAndDoesNotCreateATicket() {
        serviceNowAdapter.failCreateWith = new ItsmIntegrationException("ServiceNow unreachable");
        Incident incident = newIncident(tenantA, "open");

        ItsmSyncOutcome outcome = service.syncIncident(tenantA, incident);

        assertThat(outcome.getStatus()).isEqualTo(ItsmSyncOutcome.Status.FAILED);
        assertThat(tickets).isEmpty();
        assertThat(failures).hasSize(1);
        assertThat(failures.get(0).getAttemptCount()).isEqualTo(1);
        assertThat(failures.get(0).isDeadLettered()).isFalse();
    }

    @Test
    void syncIncident_afterFailureThenSuccess_createsExactlyOneTicketAndClearsFailure() {
        serviceNowAdapter.failCreateWith = new ItsmIntegrationException("ServiceNow unreachable");
        Incident incident = newIncident(tenantA, "open");
        service.syncIncident(tenantA, incident);

        serviceNowAdapter.failCreateWith = null; // ServiceNow recovers
        ItsmSyncOutcome outcome = service.syncIncident(tenantA, incident);

        assertThat(outcome.getStatus()).isEqualTo(ItsmSyncOutcome.Status.CREATED);
        assertThat(tickets).hasSize(1); // retries don't duplicate
        assertThat(failures).isEmpty(); // failure cleared on success
    }

    @Test
    void syncTenant_afterMaxAttempts_deadLettersAndSweepSkipsFurtherAutoRetries() {
        serviceNowAdapter.failCreateWith = new ItsmIntegrationException("ServiceNow unreachable");
        Incident incident = newIncident(tenantA, "open");
        when(incidentRepository.findByTenantId(tenantA)).thenReturn(List.of(incident));

        service.syncTenant(tenantA); // attempt 1
        service.syncTenant(tenantA); // attempt 2
        service.syncTenant(tenantA); // attempt 3 -> dead-lettered (maxAttempts = 3)

        assertThat(failures.get(0).getAttemptCount()).isEqualTo(3);
        assertThat(failures.get(0).isDeadLettered()).isTrue();
        int createCallsAtDeadLetter = serviceNowAdapter.createCallCount;

        int synced = service.syncTenant(tenantA); // sweep must skip the dead-lettered incident now

        assertThat(synced).isZero();
        assertThat(serviceNowAdapter.createCallCount).isEqualTo(createCallsAtDeadLetter); // no further auto-retry

        // A manual push is an explicit operator override and should still retry.
        serviceNowAdapter.failCreateWith = null;
        ItsmSyncOutcome manualOutcome = service.syncIncident(tenantA, incident);

        assertThat(manualOutcome.getStatus()).isEqualTo(ItsmSyncOutcome.Status.CREATED);
        assertThat(tickets).hasSize(1);
        assertThat(failures).isEmpty();
    }

    @Test
    void syncTenant_countsCreatedAndUpdatedIncidentsAsProgress() {
        Incident opened = newIncident(tenantA, "open");
        Incident alreadySynced = newIncident(tenantA, "open");
        when(incidentRepository.findByTenantId(tenantA)).thenReturn(List.of(opened, alreadySynced));
        service.syncTenant(tenantA); // both created

        alreadySynced.setStatus("resolved");
        int synced = service.syncTenant(tenantA);

        assertThat(synced).isEqualTo(1); // only the changed one counts as progress
    }

    /** Hand-written {@link ItsmAdapter} test double - this test's "mock ITSM". */
    private static final class FakeItsmAdapter implements ItsmAdapter {
        private final String kind;
        private final Map<String, String> remoteStateByExternalId = new HashMap<>();
        private int ticketCounter = 0;

        int createCallCount = 0;
        int updateCallCount = 0;
        int fetchCallCount = 0;
        RuntimeException failCreateWith;

        FakeItsmAdapter(String kind) {
            this.kind = kind;
        }

        @Override
        public String kind() {
            return kind;
        }

        @Override
        public ItsmTicketRef createTicket(String configJson, Incident incident, RootCauseResult rootCause) {
            createCallCount++;
            if (failCreateWith != null) {
                throw failCreateWith;
            }
            String externalId = "TCK-" + (++ticketCounter);
            String state = ItsmStateMapper.toItsmState(kind, incident.getStatus());
            remoteStateByExternalId.put(externalId, state);
            return new ItsmTicketRef(externalId, "https://itsm.example/" + externalId, state);
        }

        @Override
        public void updateStatus(String configJson, String externalId, String itsmState) {
            updateCallCount++;
            remoteStateByExternalId.put(externalId, itsmState);
        }

        @Override
        public String fetchStatus(String configJson, String externalId) {
            fetchCallCount++;
            return remoteStateByExternalId.get(externalId);
        }

        String remoteStateOf(String externalId) {
            return remoteStateByExternalId.get(externalId);
        }

        void simulateRemoteChange(String externalId, String newState) {
            remoteStateByExternalId.put(externalId, newState);
        }
    }
}
