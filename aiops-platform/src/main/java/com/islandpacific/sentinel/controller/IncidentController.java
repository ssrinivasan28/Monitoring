package com.islandpacific.sentinel.controller;

import com.islandpacific.sentinel.entity.Incident;
import com.islandpacific.sentinel.repository.IncidentRepository;
import com.islandpacific.sentinel.security.TenantContextHolder;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/incidents")
public class IncidentController {

    private final IncidentRepository incidentRepository;

    @Autowired
    public IncidentController(IncidentRepository incidentRepository) {
        this.incidentRepository = incidentRepository;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('STAFF_ADMIN', 'STAFF_OPERATOR', 'CUSTOMER_ADMIN', 'CUSTOMER_VIEWER')")
    public ResponseEntity<List<Incident>> listIncidents() {
        UUID tenantId = TenantContextHolder.getRequiredTenantId();
        List<Incident> incidents = incidentRepository.findByTenantId(tenantId);
        return ResponseEntity.ok(incidents);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('STAFF_ADMIN', 'STAFF_OPERATOR', 'CUSTOMER_ADMIN', 'CUSTOMER_VIEWER')")
    public ResponseEntity<?> getIncidentById(@PathVariable("id") UUID id) {
        UUID tenantId = TenantContextHolder.getRequiredTenantId();
        return incidentRepository.findByIdAndTenantId(id, tenantId)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.status(404).body(null));
    }

    @PostMapping("/{id}/acknowledge")
    @PreAuthorize("hasAnyRole('STAFF_ADMIN', 'STAFF_OPERATOR', 'CUSTOMER_ADMIN')")
    public ResponseEntity<?> acknowledgeIncident(@PathVariable("id") UUID id) {
        UUID tenantId = TenantContextHolder.getRequiredTenantId();
        Incident incident = incidentRepository.findByIdAndTenantId(id, tenantId)
                .orElse(null);

        if (incident == null) {
            return ResponseEntity.status(404).body(Map.of(
                    "status", 404,
                    "error", "Not Found",
                    "message", "Incident not found"
            ));
        }

        incident.setStatus("ack");
        incidentRepository.save(incident);

        return ResponseEntity.ok(Map.of(
                "id", incident.getId(),
                "status", incident.getStatus(),
                "message", "Incident acknowledged successfully"
        ));
    }
}
