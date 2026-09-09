package com.islandpacific.sentinel.integration.teams;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.islandpacific.sentinel.entity.Incident;
import com.islandpacific.sentinel.entity.IntegrationConfig;
import com.islandpacific.sentinel.entity.TeamsNotification;
import com.islandpacific.sentinel.repository.IncidentRepository;
import com.islandpacific.sentinel.repository.IncidentSignalRepository;
import com.islandpacific.sentinel.repository.IncidentTimelineRepository;
import com.islandpacific.sentinel.repository.IntegrationConfigRepository;
import com.islandpacific.sentinel.repository.MonitorRepository;
import com.islandpacific.sentinel.repository.TeamsNotificationRepository;
import com.islandpacific.sentinel.repository.TenantRepository;
import com.islandpacific.sentinel.security.SecretProtector;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * 1.6 Teams integration behavioral coverage: one card per incident, dedupe/idempotency on retry,
 * card updates on status change, graceful root-cause omission, and strict tenant isolation.
 * Follows this codebase's in-memory-fake-repository convention (see CorrelationEngineServiceTest)
 * rather than mocking every call individually.
 */
class TeamsNotificationServiceTest {

    private static final String TENANT_A_CONFIG_JSON =
            "{\"aadTenantId\":\"aad-a\",\"clientId\":\"client-a\",\"clientSecret\":\"secret-a\",\"teamId\":\"team-a\",\"channelId\":\"chan-a\"}";
    private static final String TENANT_B_CONFIG_JSON =
            "{\"aadTenantId\":\"aad-b\",\"clientId\":\"client-b\",\"clientSecret\":\"secret-b\",\"teamId\":\"team-b\",\"channelId\":\"chan-b\"}";

    private final UUID tenantA = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private final UUID tenantB = UUID.fromString("22222222-2222-2222-2222-222222222222");

    private List<TeamsNotification> notifications;
    private List<IntegrationConfig> integrationConfigs;

    private TenantRepository tenantRepository;
    private IncidentRepository incidentRepository;
    private IncidentSignalRepository incidentSignalRepository;
    private IncidentTimelineRepository incidentTimelineRepository;
    private MonitorRepository monitorRepository;
    private IntegrationConfigRepository integrationConfigRepository;
    private TeamsNotificationRepository teamsNotificationRepository;
    private TeamsAuthTokenProvider tokenProvider;
    private TeamsGraphClient graphClient;
    private TeamsNotificationService service;

    @BeforeEach
    void setUp() {
        notifications = new ArrayList<>();
        integrationConfigs = new ArrayList<>();
        integrationConfigs.add(new IntegrationConfig(tenantA, "teams", TENANT_A_CONFIG_JSON, true));
        integrationConfigs.add(new IntegrationConfig(tenantB, "teams", TENANT_B_CONFIG_JSON, true));

        tenantRepository = mock(TenantRepository.class);
        incidentRepository = mock(IncidentRepository.class);
        incidentSignalRepository = mock(IncidentSignalRepository.class);
        incidentTimelineRepository = mock(IncidentTimelineRepository.class);
        monitorRepository = mock(MonitorRepository.class);
        when(incidentSignalRepository.findByTenantIdAndIncidentId(any(), any())).thenReturn(List.of());
        when(monitorRepository.findByTenantId(any())).thenReturn(List.of());

        integrationConfigRepository = mock(IntegrationConfigRepository.class);
        when(integrationConfigRepository.findByTenantIdAndKind(any(), eq("teams"))).thenAnswer(inv -> {
            UUID tenantId = inv.getArgument(0);
            return integrationConfigs.stream().filter(c -> c.getTenantId().equals(tenantId)).findFirst();
        });

        teamsNotificationRepository = mock(TeamsNotificationRepository.class);
        when(teamsNotificationRepository.findByTenantIdAndIncidentId(any(), any())).thenAnswer(inv -> {
            UUID tenantId = inv.getArgument(0);
            UUID incidentId = inv.getArgument(1);
            return notifications.stream()
                    .filter(n -> n.getTenantId().equals(tenantId) && n.getIncidentId().equals(incidentId))
                    .findFirst();
        });
        when(teamsNotificationRepository.findByTenantId(any())).thenAnswer(inv -> {
            UUID tenantId = inv.getArgument(0);
            return notifications.stream().filter(n -> n.getTenantId().equals(tenantId)).collect(Collectors.toList());
        });
        when(teamsNotificationRepository.save(any())).thenAnswer(inv -> {
            TeamsNotification n = inv.getArgument(0);
            if (n.getId() == null) {
                n.setId(UUID.randomUUID());
            }
            notifications.removeIf(x -> x.getId().equals(n.getId()));
            notifications.add(n);
            return n;
        });

        tokenProvider = mock(TeamsAuthTokenProvider.class);
        when(tokenProvider.getAccessToken(any())).thenReturn("test-token");

        graphClient = mock(TeamsGraphClient.class);
        when(graphClient.postCard(any(), anyString(), any())).thenReturn("msg-1");

        service = new TeamsNotificationService(
                tenantRepository, incidentRepository, incidentSignalRepository, incidentTimelineRepository,
                monitorRepository, integrationConfigRepository, teamsNotificationRepository,
                new SecretProtector.DefaultSecretProtector(), tokenProvider, graphClient, new AdaptiveCardFactory());
    }

    private Incident newIncident(UUID tenantId, String severity, String status) {
        Incident incident = new Incident(tenantId, severity, status, "Disk pressure on APP01");
        incident.setId(UUID.randomUUID());
        return incident;
    }

    @Test
    void pushIncidentCard_newIncident_postsExactlyOneCardAndStoresMessageRef() {
        Incident incident = newIncident(tenantA, "high", "open");

        TeamsSyncOutcome outcome = service.pushIncidentCard(tenantA, incident);

        assertThat(outcome.getStatus()).isEqualTo(TeamsSyncOutcome.Status.CREATED);
        verify(graphClient, times(1)).postCard(any(), eq("test-token"), any());
        verify(graphClient, never()).updateCard(any(), any(), any(), any());
        assertThat(notifications).hasSize(1);
        assertThat(notifications.get(0).getMessageId()).isEqualTo("msg-1");
        assertThat(notifications.get(0).getChannelId()).isEqualTo("chan-a");
        assertThat(notifications.get(0).getLastStatus()).isEqualTo("open");
    }

    @Test
    void pushIncidentCard_calledTwiceForSameIncident_doesNotPostASecondCard() {
        Incident incident = newIncident(tenantA, "high", "open");

        service.pushIncidentCard(tenantA, incident);
        TeamsSyncOutcome secondOutcome = service.pushIncidentCard(tenantA, incident);

        assertThat(secondOutcome.getStatus()).isEqualTo(TeamsSyncOutcome.Status.UPDATED);
        verify(graphClient, times(1)).postCard(any(), any(), any());
        verify(graphClient, times(1)).updateCard(any(), any(), eq("msg-1"), any());
        assertThat(notifications).hasSize(1); // still exactly one row/card for this incident
    }

    @Test
    void pushIncidentCard_statusChanged_updatesExistingCardInPlace() {
        Incident incident = newIncident(tenantA, "high", "open");
        service.pushIncidentCard(tenantA, incident);

        incident.setStatus("ack");
        TeamsSyncOutcome outcome = service.pushIncidentCard(tenantA, incident);

        assertThat(outcome.getStatus()).isEqualTo(TeamsSyncOutcome.Status.UPDATED);
        verify(graphClient, times(1)).postCard(any(), any(), any());
        verify(graphClient, times(1)).updateCard(any(), any(), eq("msg-1"), any());
        assertThat(notifications.get(0).getLastStatus()).isEqualTo("ack");
    }

    @Test
    void pushIncidentCard_noRootCause_postsValidCardWithoutRootCauseSection() {
        Incident incident = newIncident(tenantA, "medium", "open"); // rootCauseJson never set (LLM hasn't run)

        TeamsSyncOutcome outcome = service.pushIncidentCard(tenantA, incident);

        assertThat(outcome.getStatus()).isEqualTo(TeamsSyncOutcome.Status.CREATED);
        ArgumentCaptor<ObjectNode> cardCaptor = ArgumentCaptor.forClass(ObjectNode.class);
        verify(graphClient).postCard(any(), any(), cardCaptor.capture());
        assertThat(cardCaptor.getValue().get("type").asText()).isEqualTo("AdaptiveCard");
        assertThat(cardCaptor.getValue().toString()).doesNotContain("Root cause");
    }

    @Test
    void pushIncidentCard_notConfigured_skipsWithNoGraphCalls() {
        UUID tenantWithoutIntegration = UUID.randomUUID();
        Incident incident = newIncident(tenantWithoutIntegration, "high", "open");

        TeamsSyncOutcome outcome = service.pushIncidentCard(tenantWithoutIntegration, incident);

        assertThat(outcome.getStatus()).isEqualTo(TeamsSyncOutcome.Status.SKIPPED_NOT_CONFIGURED);
        verifyNoInteractions(graphClient);
    }

    @Test
    void pushIncidentCard_disabledIntegration_skipsWithNoGraphCalls() {
        integrationConfigs.clear();
        integrationConfigs.add(new IntegrationConfig(tenantA, "teams", TENANT_A_CONFIG_JSON, false));
        Incident incident = newIncident(tenantA, "high", "open");

        TeamsSyncOutcome outcome = service.pushIncidentCard(tenantA, incident);

        assertThat(outcome.getStatus()).isEqualTo(TeamsSyncOutcome.Status.SKIPPED_NOT_CONFIGURED);
        verifyNoInteractions(graphClient);
    }

    @Test
    void pushIncidentCard_incidentBelongsToAnotherTenant_refusesAndNeverCallsGraph() {
        Incident incidentForTenantB = newIncident(tenantB, "critical", "open");

        // Called with tenantA's id but an incident that actually belongs to tenantB.
        TeamsSyncOutcome outcome = service.pushIncidentCard(tenantA, incidentForTenantB);

        assertThat(outcome.getStatus()).isEqualTo(TeamsSyncOutcome.Status.FAILED);
        verifyNoInteractions(graphClient);
        assertThat(notifications).isEmpty();
    }

    @Test
    void pushIncidentCard_usesOnlyTheRequestingTenantsOwnChannel() {
        Incident incidentA = newIncident(tenantA, "high", "open");
        Incident incidentB = newIncident(tenantB, "high", "open");

        service.pushIncidentCard(tenantA, incidentA);
        service.pushIncidentCard(tenantB, incidentB);

        assertThat(notifications).hasSize(2);
        String channelForA = notifications.stream().filter(n -> n.getIncidentId().equals(incidentA.getId())).findFirst().orElseThrow().getChannelId();
        String channelForB = notifications.stream().filter(n -> n.getIncidentId().equals(incidentB.getId())).findFirst().orElseThrow().getChannelId();
        assertThat(channelForA).isEqualTo("chan-a");
        assertThat(channelForB).isEqualTo("chan-b");
    }

    @Test
    void syncTenant_skipsIncidentsAlreadyReflectingCurrentStatus() {
        Incident incident = newIncident(tenantA, "high", "open");
        when(incidentRepository.findByTenantId(tenantA)).thenReturn(List.of(incident));

        int firstSync = service.syncTenant(tenantA);
        int secondSync = service.syncTenant(tenantA); // status unchanged since last sync

        assertThat(firstSync).isEqualTo(1);
        assertThat(secondSync).isEqualTo(0);
        verify(graphClient, times(1)).postCard(any(), any(), any());
        verify(graphClient, never()).updateCard(any(), any(), any(), any());
    }

    @Test
    void syncTenant_pushesUpdateWhenStatusChangedSinceLastSync() {
        Incident incident = newIncident(tenantA, "high", "open");
        when(incidentRepository.findByTenantId(tenantA)).thenReturn(List.of(incident));
        service.syncTenant(tenantA);

        incident.setStatus("resolved");
        int synced = service.syncTenant(tenantA);

        assertThat(synced).isEqualTo(1);
        verify(graphClient, times(1)).updateCard(any(), any(), eq("msg-1"), any());
        assertThat(notifications.get(0).getLastStatus()).isEqualTo("resolved");
    }
}
