package com.islandpacific.sentinel;

import com.islandpacific.sentinel.entity.Incident;
import com.islandpacific.sentinel.entity.IncidentSignal;
import com.islandpacific.sentinel.entity.Tenant;
import com.islandpacific.sentinel.repository.IncidentRepository;
import com.islandpacific.sentinel.repository.IncidentSignalRepository;
import com.islandpacific.sentinel.repository.TenantRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

public class IncidentAndSignalRepositoryTest extends AbstractIntegrationTest {

    @Autowired
    private TenantRepository tenantRepository;

    @Autowired
    private IncidentRepository incidentRepository;

    @Autowired
    private IncidentSignalRepository incidentSignalRepository;

    @Test
    void incidentAndSignalsRoundTrip() {
        Tenant tenant = tenantRepository.save(new Tenant("Test Tenant", "client-001"));
        UUID tenantId = tenant.getId();

        Incident incident = new Incident(tenantId, "HIGH", "open", "High CPU Utilization on AS400");
        incident.setRootCauseJson("{\"reason\": \"Interactive job overload\"}");
        incident = incidentRepository.save(incident);

        IncidentSignal signal = new IncidentSignal(incident.getId(), tenantId, "ibmi");
        signal.setDetailJson("{\"cpu_percent\": 95.5}");
        incidentSignalRepository.save(signal);

        // Fetch back using tenantId isolation
        List<Incident> tenantIncidents = incidentRepository.findByTenantId(tenantId);
        assertThat(tenantIncidents).hasSize(1);
        assertThat(tenantIncidents.get(0).getTitle()).isEqualTo("High CPU Utilization on AS400");
        assertThat(tenantIncidents.get(0).getRootCauseJson()).contains("Interactive job overload");

        List<IncidentSignal> signals = incidentSignalRepository.findByTenantIdAndIncidentId(tenantId, incident.getId());
        assertThat(signals).hasSize(1);
        assertThat(signals.get(0).getPlatform()).isEqualTo("ibmi");
        assertThat(signals.get(0).getDetailJson()).contains("95.5");
    }
}
