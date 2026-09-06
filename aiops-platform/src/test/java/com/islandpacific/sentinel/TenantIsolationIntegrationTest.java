package com.islandpacific.sentinel;

import com.islandpacific.sentinel.entity.*;
import com.islandpacific.sentinel.repository.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

public class TenantIsolationIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private TenantRepository tenantRepository;

    @Autowired
    private IncidentRepository incidentRepository;

    @Autowired
    private AlertRepository alertRepository;

    @Autowired
    private MonitorRepository monitorRepository;

    @Autowired
    private AgentRunRepository agentRunRepository;

    @Autowired
    private KbDocRepository kbDocRepository;

    @Test
    @DisplayName("Verify Tenant A cannot access or query Tenant B's incidents, alerts, monitors, agent runs, or KB docs")
    void verifyStrictTenantIsolation() {
        Tenant tenantA = tenantRepository.save(new Tenant("Tenant Alpha", "client-alpha"));
        Tenant tenantB = tenantRepository.save(new Tenant("Tenant Beta", "client-beta"));

        UUID idA = tenantA.getId();
        UUID idB = tenantB.getId();

        // Seed Tenant A resources
        Incident incidentA = incidentRepository.save(new Incident(idA, "HIGH", "open", "Alpha System Alert"));
        Alert alertA = alertRepository.save(new Alert(idA, null, "cpu_usage", 99.0, 90.0, Instant.now()));
        Monitor monitorA = monitorRepository.save(new Monitor(idA, "Alpha Monitor", "WinMonitor", 3010));
        AgentRun runA = agentRunRepository.save(new AgentRun(idA, "triage", "prompt Alpha", "claude-3-5", 10, 20, BigDecimal.ZERO));
        KbDoc docA = kbDocRepository.save(new KbDoc(idA, "kb", "Alpha Secret Runbook"));

        // Seed Tenant B resources
        Incident incidentB = incidentRepository.save(new Incident(idB, "LOW", "open", "Beta System Alert"));
        Alert alertB = alertRepository.save(new Alert(idB, null, "mem_usage", 85.0, 80.0, Instant.now()));
        Monitor monitorB = monitorRepository.save(new Monitor(idB, "Beta Monitor", "IBMSubsystemMonitor", 3011));
        AgentRun runB = agentRunRepository.save(new AgentRun(idB, "triage", "prompt Beta", "claude-3-5", 15, 25, BigDecimal.ZERO));
        KbDoc docB = kbDocRepository.save(new KbDoc(idB, "kb", "Beta Confidential Doc"));

        // 1. Querying Tenant A with Tenant B's tenantId returns ZERO results for Tenant A's records
        List<Incident> bIncidents = incidentRepository.findByTenantId(idB);
        assertThat(bIncidents).extracting(Incident::getId).containsExactly(incidentB.getId()).doesNotContain(incidentA.getId());

        List<Alert> bAlerts = alertRepository.findByTenantId(idB);
        assertThat(bAlerts).extracting(Alert::getId).containsExactly(alertB.getId()).doesNotContain(alertA.getId());

        List<Monitor> bMonitors = monitorRepository.findByTenantId(idB);
        assertThat(bMonitors).extracting(Monitor::getId).containsExactly(monitorB.getId()).doesNotContain(monitorA.getId());

        List<AgentRun> bRuns = agentRunRepository.findByTenantId(idB);
        assertThat(bRuns).extracting(AgentRun::getId).containsExactly(runB.getId()).doesNotContain(runA.getId());

        List<KbDoc> bDocs = kbDocRepository.findByTenantId(idB);
        assertThat(bDocs).extracting(KbDoc::getId).containsExactly(docB.getId()).doesNotContain(docA.getId());

        // 2. Querying specific ID scoped by tenantId fails if cross-tenant ID is passed
        Optional<Incident> crossTenantIncident = incidentRepository.findByTenantIdAndId(idB, incidentA.getId());
        assertThat(crossTenantIncident).isEmpty();

        Optional<Alert> crossTenantAlert = alertRepository.findByTenantIdAndId(idB, alertA.getId());
        assertThat(crossTenantAlert).isEmpty();

        Optional<Monitor> crossTenantMonitor = monitorRepository.findByTenantIdAndId(idB, monitorA.getId());
        assertThat(crossTenantMonitor).isEmpty();

        Optional<AgentRun> crossTenantRun = agentRunRepository.findByTenantIdAndId(idB, runA.getId());
        assertThat(crossTenantRun).isEmpty();
    }
}
