package com.islandpacific.sentinel.demo;

import com.islandpacific.sentinel.correlation.CorrelationEngineProperties;
import com.islandpacific.sentinel.correlation.CorrelationEngineService;
import com.islandpacific.sentinel.entity.Alert;
import com.islandpacific.sentinel.entity.Incident;
import com.islandpacific.sentinel.entity.IncidentSignal;
import com.islandpacific.sentinel.entity.KbChunk;
import com.islandpacific.sentinel.entity.KbDoc;
import com.islandpacific.sentinel.entity.Monitor;
import com.islandpacific.sentinel.entity.Tenant;
import com.islandpacific.sentinel.entity.TopologyLink;
import com.islandpacific.sentinel.repository.AlertRepository;
import com.islandpacific.sentinel.repository.IncidentRepository;
import com.islandpacific.sentinel.repository.IncidentSignalRepository;
import com.islandpacific.sentinel.repository.KbChunkRepository;
import com.islandpacific.sentinel.repository.KbDocRepository;
import com.islandpacific.sentinel.repository.MonitorRepository;
import com.islandpacific.sentinel.repository.TenantRepository;
import com.islandpacific.sentinel.repository.TopologyLinkRepository;
import com.islandpacific.sentinel.triage.TriageAgentService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 1.8 — flagship cross-platform correlation demo. Seeds a deterministic, self-contained fixture
 * (an IBM i subsystem outage plus its Windows service fallout, with a matching runbook chunk for
 * RAG) and drives it through the real 1.1 correlation engine and 1.2 triage agent, exactly as a
 * live deployment would - no live IBM i/Windows connectivity required. Reused verbatim by
 * {@code CrossPlatformDemoIntegrationTest} as the acceptance test for both 1.1 and 1.2.
 */
@Service
public class CrossPlatformDemoService {

    private static final String IBMI_MONITOR_NAME = "IBMSYS01";
    private static final String WINDOWS_MONITOR_NAME = "APPSRV01";

    private final TenantRepository tenantRepository;
    private final MonitorRepository monitorRepository;
    private final TopologyLinkRepository topologyLinkRepository;
    private final AlertRepository alertRepository;
    private final KbDocRepository kbDocRepository;
    private final KbChunkRepository kbChunkRepository;
    private final IncidentRepository incidentRepository;
    private final IncidentSignalRepository incidentSignalRepository;
    private final CorrelationEngineService correlationEngineService;
    private final CorrelationEngineProperties correlationEngineProperties;
    private final TriageAgentService triageAgentService;

    @Autowired
    public CrossPlatformDemoService(
            TenantRepository tenantRepository,
            MonitorRepository monitorRepository,
            TopologyLinkRepository topologyLinkRepository,
            AlertRepository alertRepository,
            KbDocRepository kbDocRepository,
            KbChunkRepository kbChunkRepository,
            IncidentRepository incidentRepository,
            IncidentSignalRepository incidentSignalRepository,
            CorrelationEngineService correlationEngineService,
            CorrelationEngineProperties correlationEngineProperties,
            TriageAgentService triageAgentService) {
        this.tenantRepository = tenantRepository;
        this.monitorRepository = monitorRepository;
        this.topologyLinkRepository = topologyLinkRepository;
        this.alertRepository = alertRepository;
        this.kbDocRepository = kbDocRepository;
        this.kbChunkRepository = kbChunkRepository;
        this.incidentRepository = incidentRepository;
        this.incidentSignalRepository = incidentSignalRepository;
        this.correlationEngineService = correlationEngineService;
        this.correlationEngineProperties = correlationEngineProperties;
        this.triageAgentService = triageAgentService;
    }

    /**
     * Seeds the fixture, drives correlation to convergence, then triages the resulting incident.
     * Deterministic: always yields exactly one cross-platform incident, or throws.
     */
    public CrossPlatformDemoResult runDemo() {
        UUID tenantId = seedFixture();

        int cycles = Math.max(1, correlationEngineProperties.getConsecutiveBreachThreshold());
        for (int i = 0; i < cycles; i++) {
            correlationEngineService.processTenant(tenantId);
        }

        List<Incident> incidents = incidentRepository.findByTenantId(tenantId);
        if (incidents.size() != 1) {
            throw new IllegalStateException(
                    "Cross-platform demo seed did not converge to exactly one incident (found " + incidents.size() + ")");
        }
        Incident incident = incidents.get(0);

        triageAgentService.triageIncident(tenantId, incident.getId());
        Incident triaged = incidentRepository.findByTenantIdAndId(tenantId, incident.getId()).orElse(incident);

        List<String> platforms = incidentSignalRepository.findByTenantIdAndIncidentId(tenantId, incident.getId()).stream()
                .map(IncidentSignal::getPlatform)
                .distinct()
                .sorted()
                .collect(Collectors.toList());

        return new CrossPlatformDemoResult(tenantId, incident.getId(), triaged.getSeverity(), platforms, triaged.getRootCauseJson());
    }

    /**
     * Persists the fixture: a fresh demo tenant, a linked IBM i/Windows monitor pair, two breach
     * alerts close together in time, and a seeded runbook chunk so {@code kb_search} RAG can match
     * it during triage. No live IBM i/Windows connectivity is involved - everything is a plain row.
     */
    private UUID seedFixture() {
        Tenant tenant = tenantRepository.save(new Tenant("Cross-Platform Demo", "demo-xplat-" + UUID.randomUUID()));
        UUID tenantId = tenant.getId();

        Monitor ibmiMonitor = monitorRepository.save(
                new Monitor(tenantId, IBMI_MONITOR_NAME, "subsystem", 0, "ibmi"));
        Monitor windowsMonitor = monitorRepository.save(
                new Monitor(tenantId, WINDOWS_MONITOR_NAME, "service", 0, "windows"));
        topologyLinkRepository.save(new TopologyLink(tenantId, WINDOWS_MONITOR_NAME, IBMI_MONITOR_NAME));

        Instant base = Instant.now();
        alertRepository.save(new Alert(tenantId, ibmiMonitor.getId(), "qinter_subsystem_active", 0.0, 1.0, base));
        alertRepository.save(new Alert(tenantId, windowsMonitor.getId(), "windows_service_running", 0.0, 1.0, base.plusSeconds(60)));

        seedRunbook(tenantId);

        return tenantId;
    }

    private void seedRunbook(UUID tenantId) {
        KbDoc runbook = kbDocRepository.save(new KbDoc(tenantId, "runbook", "QINTER Subsystem Outage Runbook"));

        // Fixed, deterministic embedding - the demo/test never calls a live embedding provider,
        // matching the "no live IBM i/Windows" and "self-contained" constraints for this chunk.
        float[] embedding = new float[1536];
        embedding[0] = 1.0f;

        kbChunkRepository.save(new KbChunk(runbook.getId(), tenantId,
                "When IBM i subsystem QINTER goes down, dependent Windows application services that rely on "
                        + "5250/telnet connectivity to APPSRV01 fail with connection-refused errors. Remediation: "
                        + "restart the subsystem with STRSBS QINTER, confirm interactive job queues drain, then verify "
                        + "the dependent Windows services on APPSRV01 auto-recover or restart them manually.",
                embedding));
    }
}
