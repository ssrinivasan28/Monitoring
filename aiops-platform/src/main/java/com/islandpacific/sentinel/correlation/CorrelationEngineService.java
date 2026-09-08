package com.islandpacific.sentinel.correlation;

import com.google.gson.JsonObject;
import com.islandpacific.sentinel.entity.Alert;
import com.islandpacific.sentinel.entity.Correlation;
import com.islandpacific.sentinel.entity.Incident;
import com.islandpacific.sentinel.entity.IncidentSignal;
import com.islandpacific.sentinel.entity.IncidentTimeline;
import com.islandpacific.sentinel.entity.Monitor;
import com.islandpacific.sentinel.entity.Tenant;
import com.islandpacific.sentinel.entity.TopologyLink;
import com.islandpacific.sentinel.repository.AlertRepository;
import com.islandpacific.sentinel.repository.CorrelationRepository;
import com.islandpacific.sentinel.repository.IncidentRepository;
import com.islandpacific.sentinel.repository.IncidentSignalRepository;
import com.islandpacific.sentinel.repository.IncidentTimelineRepository;
import com.islandpacific.sentinel.repository.MonitorRepository;
import com.islandpacific.sentinel.repository.TenantRepository;
import com.islandpacific.sentinel.repository.TopologyLinkRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Deterministic, rule-based correlation engine (1.1). Collapses simultaneous breaches
 * (same tenant, related by host/topology, within a time window) into a single incident.
 * No LLM calls of any kind — this must run fully with the LLM disabled (1.2 enriches later).
 */
@Service
public class CorrelationEngineService {

    private static final Logger log = LoggerFactory.getLogger(CorrelationEngineService.class);

    private final TenantRepository tenantRepository;
    private final AlertRepository alertRepository;
    private final MonitorRepository monitorRepository;
    private final IncidentRepository incidentRepository;
    private final IncidentSignalRepository incidentSignalRepository;
    private final CorrelationRepository correlationRepository;
    private final IncidentTimelineRepository incidentTimelineRepository;
    private final TopologyLinkRepository topologyLinkRepository;
    private final CorrelationEngineProperties properties;

    // Per-key (tenant|monitor|metric) consecutive-breach debounce state. In-memory by design,
    // mirroring the existing monitors' breach-cycle suppression (see filesystemcardinalitymonitoring).
    private final ConcurrentHashMap<String, BreachStreak> streaks = new ConcurrentHashMap<>();
    // Remembers the last incident a given key was correlated into, so a flapping signal keeps
    // rejoining the same incident instead of spawning a new one each cycle.
    private final ConcurrentHashMap<String, UUID> lastIncidentByKey = new ConcurrentHashMap<>();

    @Autowired
    public CorrelationEngineService(
            TenantRepository tenantRepository,
            AlertRepository alertRepository,
            MonitorRepository monitorRepository,
            IncidentRepository incidentRepository,
            IncidentSignalRepository incidentSignalRepository,
            CorrelationRepository correlationRepository,
            IncidentTimelineRepository incidentTimelineRepository,
            TopologyLinkRepository topologyLinkRepository,
            CorrelationEngineProperties properties) {
        this.tenantRepository = tenantRepository;
        this.alertRepository = alertRepository;
        this.monitorRepository = monitorRepository;
        this.incidentRepository = incidentRepository;
        this.incidentSignalRepository = incidentSignalRepository;
        this.correlationRepository = correlationRepository;
        this.incidentTimelineRepository = incidentTimelineRepository;
        this.topologyLinkRepository = topologyLinkRepository;
        this.properties = properties;
    }

    @Scheduled(
            initialDelayString = "#{@correlationEngineProperties.pollIntervalMs}",
            fixedDelayString = "#{@correlationEngineProperties.pollIntervalMs}")
    public void pollAllTenants() {
        for (Tenant tenant : tenantRepository.findAll()) {
            try {
                processTenant(tenant.getId());
            } catch (Exception e) {
                log.error("Correlation engine failed for tenant {}: {}", tenant.getId(), e.getMessage(), e);
            }
        }
    }

    /**
     * Correlates all currently-active (uncleared) breaches for a single tenant. Never reads or
     * writes any other tenant's data — every lookup below is explicitly tenant-scoped.
     */
    public CorrelationResult processTenant(UUID tenantId) {
        List<Alert> activeAlerts = alertRepository.findByTenantIdAndClearedAtIsNullOrderByFiredAtAsc(tenantId);
        if (activeAlerts.isEmpty()) {
            return new CorrelationResult(0, 0, 0, 0);
        }

        Map<UUID, Monitor> monitorsById = monitorRepository.findByTenantId(tenantId).stream()
                .collect(Collectors.toMap(Monitor::getId, m -> m));
        List<TopologyLink> links = topologyLinkRepository.findByTenantIdAndEnabledTrue(tenantId);
        Map<UUID, Alert> alertsById = alertRepository.findByTenantId(tenantId).stream()
                .collect(Collectors.toMap(Alert::getId, a -> a, (a, b) -> a));

        int opened = 0;
        int joined = 0;
        int suppressed = 0;

        for (Alert alert : activeAlerts) {
            if (incidentSignalRepository.existsByTenantIdAndAlertId(tenantId, alert.getId())) {
                continue; // dedupe: already correlated on a prior poll
            }

            Monitor monitor = monitorsById.get(alert.getMonitorId());
            if (monitor == null) {
                log.warn("Skipping alert {} for tenant {}: no monitor context", alert.getId(), tenantId);
                continue;
            }

            String key = keyFor(tenantId, monitor, alert);
            BreachStreak streak = streaks.computeIfAbsent(key, k -> new BreachStreak());

            boolean flapping;
            synchronized (streak) {
                Instant now = Instant.now();
                if (!alert.getId().equals(streak.lastAlertId)) {
                    streak.lastAlertId = alert.getId();
                    streak.consecutiveCount = 1;
                    streak.episodeStarts.addLast(now);
                    Instant flapCutoff = now.minus(Duration.ofMinutes(properties.getFlapWindowMinutes()));
                    while (!streak.episodeStarts.isEmpty() && streak.episodeStarts.peekFirst().isBefore(flapCutoff)) {
                        streak.episodeStarts.pollFirst();
                    }
                } else {
                    streak.consecutiveCount++;
                }

                if (streak.consecutiveCount < properties.getConsecutiveBreachThreshold()) {
                    suppressed++;
                    continue; // debounce: not enough consecutive cycles yet
                }

                flapping = streak.episodeStarts.size() >= properties.getFlapThreshold();
            }

            Incident candidate = findJoinCandidate(tenantId, monitor, alert, monitorsById, alertsById, links, flapping, key);
            if (candidate != null) {
                joinIncident(tenantId, candidate, monitor, alert, flapping, key);
                joined++;
            } else {
                openIncident(tenantId, monitor, alert, flapping, key);
                opened++;
            }
        }

        return new CorrelationResult(activeAlerts.size(), opened, joined, suppressed);
    }

    private Incident findJoinCandidate(
            UUID tenantId,
            Monitor monitor,
            Alert alert,
            Map<UUID, Monitor> monitorsById,
            Map<UUID, Alert> alertsById,
            List<TopologyLink> links,
            boolean flapping,
            String key) {

        List<Incident> openIncidents = incidentRepository.findByTenantIdAndStatus(tenantId, "open");
        if (openIncidents.isEmpty()) {
            return null;
        }

        // Flapping: prefer reusing the incident this exact key was last correlated into, even if
        // the gap between episodes exceeds the normal correlation window — that's the point of
        // flap suppression, avoiding a fresh incident per rapid open/clear cycle.
        if (flapping) {
            UUID lastId = lastIncidentByKey.get(key);
            if (lastId != null) {
                for (Incident incident : openIncidents) {
                    if (incident.getId().equals(lastId)) {
                        return incident;
                    }
                }
            }
        }

        Duration window = Duration.ofMinutes(properties.getWindowMinutes());
        Incident best = null;
        Instant bestSignalTime = null;

        for (Incident incident : openIncidents) {
            List<IncidentSignal> signals = incidentSignalRepository.findByTenantIdAndIncidentId(tenantId, incident.getId());
            for (IncidentSignal signal : signals) {
                Monitor signalMonitor = monitorsById.get(signal.getMonitorId());
                Alert signalAlert = signal.getAlertId() != null ? alertsById.get(signal.getAlertId()) : null;
                if (signalMonitor == null || signalAlert == null) {
                    continue;
                }
                if (!related(monitor, signalMonitor, links)) {
                    continue;
                }
                Duration diff = Duration.between(signalAlert.getFiredAt(), alert.getFiredAt()).abs();
                if (diff.compareTo(window) <= 0
                        && (best == null || signalAlert.getFiredAt().isAfter(bestSignalTime))) {
                    best = incident;
                    bestSignalTime = signalAlert.getFiredAt();
                }
            }
        }
        return best;
    }

    /** Same monitor, same host, or a configured cross-platform topology pair for this tenant. */
    private boolean related(Monitor a, Monitor b, List<TopologyLink> links) {
        if (a.getId().equals(b.getId())) {
            return true;
        }
        if (a.getName().equalsIgnoreCase(b.getName())) {
            return true;
        }
        if (Objects.equals(a.getPlatform(), b.getPlatform())) {
            return false; // topology map only links across platforms
        }
        String windowsName = "windows".equalsIgnoreCase(a.getPlatform()) ? a.getName() : b.getName();
        String ibmiName = "ibmi".equalsIgnoreCase(a.getPlatform()) ? a.getName() : b.getName();
        return links.stream().anyMatch(l ->
                l.getWindowsHost().equalsIgnoreCase(windowsName) && l.getIbmiSystem().equalsIgnoreCase(ibmiName));
    }

    private void joinIncident(UUID tenantId, Incident incident, Monitor monitor, Alert alert, boolean flapping, String key) {
        IncidentSignal signal = new IncidentSignal(incident.getId(), tenantId, monitor.getPlatform());
        signal.setMonitorId(monitor.getId());
        signal.setAlertId(alert.getId());
        signal.setDetailJson(detailJson(alert));
        incidentSignalRepository.save(signal);

        Instant windowStart = alert.getFiredAt();
        Instant windowEnd = windowStart.plus(Duration.ofMinutes(properties.getWindowMinutes()));
        correlationRepository.save(new Correlation(
                incident.getId(), flapping ? "flap_rejoin" : "time_topology_window", windowStart, windowEnd));

        incidentTimelineRepository.save(new IncidentTimeline(
                incident.getId(), tenantId, "correlation-engine", "signal_added",
                String.format("Joined signal from monitor '%s' (%s) metric '%s'%s",
                        monitor.getName(), monitor.getPlatform(), alert.getMetric(),
                        flapping ? " [flap detected - reused existing incident]" : "")));

        recomputeSeverityAndTitle(tenantId, incident);
        lastIncidentByKey.put(key, incident.getId());
    }

    private void openIncident(UUID tenantId, Monitor monitor, Alert alert, boolean flapping, String key) {
        Incident incident = new Incident(tenantId, "low", "open",
                truncateTitle("Correlated incident: " + monitor.getName() + " / " + alert.getMetric()));
        incident = incidentRepository.save(incident);

        IncidentSignal signal = new IncidentSignal(incident.getId(), tenantId, monitor.getPlatform());
        signal.setMonitorId(monitor.getId());
        signal.setAlertId(alert.getId());
        signal.setDetailJson(detailJson(alert));
        incidentSignalRepository.save(signal);

        Instant windowStart = alert.getFiredAt();
        Instant windowEnd = windowStart.plus(Duration.ofMinutes(properties.getWindowMinutes()));
        correlationRepository.save(new Correlation(incident.getId(), "new_incident", windowStart, windowEnd));

        incidentTimelineRepository.save(new IncidentTimeline(
                incident.getId(), tenantId, "correlation-engine", "incident_opened",
                String.format("Opened from monitor '%s' (%s) metric '%s'%s",
                        monitor.getName(), monitor.getPlatform(), alert.getMetric(),
                        flapping ? " [flap detected]" : "")));

        recomputeSeverityAndTitle(tenantId, incident);
        lastIncidentByKey.put(key, incident.getId());
    }

    private void recomputeSeverityAndTitle(UUID tenantId, Incident incident) {
        List<IncidentSignal> signals = incidentSignalRepository.findByTenantIdAndIncidentId(tenantId, incident.getId());
        Set<String> platforms = signals.stream().map(IncidentSignal::getPlatform).collect(Collectors.toCollection(TreeSet::new));
        int signalCount = signals.size();

        String severity;
        if (platforms.size() >= 2) {
            severity = "critical"; // cross-platform correlation is the flagship signal of real trouble
        } else if (signalCount >= 3) {
            severity = "high";
        } else if (signalCount == 2) {
            severity = "medium";
        } else {
            severity = "low";
        }

        incident.setSeverity(severity);
        incidentRepository.save(incident);
    }

    private String detailJson(Alert alert) {
        JsonObject obj = new JsonObject();
        obj.addProperty("metric", alert.getMetric());
        obj.addProperty("value", alert.getValue());
        obj.addProperty("threshold", alert.getThreshold());
        obj.addProperty("firedAt", alert.getFiredAt().toString());
        return obj.toString();
    }

    private String truncateTitle(String title) {
        return title.length() > 500 ? title.substring(0, 500) : title;
    }

    private String keyFor(UUID tenantId, Monitor monitor, Alert alert) {
        return tenantId + "|" + monitor.getId() + "|" + alert.getMetric();
    }

    private static final class BreachStreak {
        private UUID lastAlertId;
        private int consecutiveCount;
        private final Deque<Instant> episodeStarts = new ArrayDeque<>();
    }

    public record CorrelationResult(int activeAlerts, int incidentsOpened, int signalsJoined, int suppressed) {}
}
