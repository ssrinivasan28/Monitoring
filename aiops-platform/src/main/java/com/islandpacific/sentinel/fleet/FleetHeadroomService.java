package com.islandpacific.sentinel.fleet;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.islandpacific.sentinel.entity.Tenant;
import com.islandpacific.sentinel.query.QueryGatewayService;
import com.islandpacific.sentinel.query.ResponseMerger;
import com.islandpacific.sentinel.repository.TenantRepository;
import com.islandpacific.sentinel.security.TenantContext;
import com.islandpacific.sentinel.security.TenantContextHolder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class FleetHeadroomService {

    private static final Logger log = LoggerFactory.getLogger(FleetHeadroomService.class);

    private final TenantRepository tenantRepository;
    private final QueryGatewayService queryGatewayService;
    private final FleetHeadroomProperties properties;

    @Autowired
    public FleetHeadroomService(
            TenantRepository tenantRepository,
            QueryGatewayService queryGatewayService,
            FleetHeadroomProperties properties) {
        this.tenantRepository = tenantRepository;
        this.queryGatewayService = queryGatewayService;
        this.properties = properties;
    }

    public List<TenantFleetDto> getFleetOverview(UUID targetTenantId, UUID callingUserId, boolean isStaff) {
        List<Tenant> tenantsToQuery = new ArrayList<>();

        if (targetTenantId != null) {
            tenantRepository.findById(targetTenantId).ifPresent(tenantsToQuery::add);
        } else if (isStaff) {
            tenantsToQuery.addAll(tenantRepository.findAll());
        } else {
            Optional<TenantContext> currentCtx = TenantContextHolder.getContext();
            if (currentCtx.isPresent()) {
                tenantRepository.findById(currentCtx.get().getTenantId()).ifPresent(tenantsToQuery::add);
            }
        }

        List<TenantFleetDto> fleetOverview = new ArrayList<>();

        for (Tenant tenant : tenantsToQuery) {
            try {
                TenantFleetDto tenantFleet = computeTenantHeadroom(tenant, callingUserId);
                fleetOverview.add(tenantFleet);
            } catch (Exception e) {
                log.error("Error computing headroom for tenant {}: {}", tenant.getId(), e.getMessage(), e);
                // Return unknown placeholder for tenant on error
                fleetOverview.add(createStaleTenantDto(tenant));
            }
        }

        return fleetOverview;
    }

    public TenantFleetDto computeTenantHeadroom(Tenant tenant, UUID callingUserId) {
        // Set tenant context for QueryGateway authorization & scoping
        Optional<TenantContext> prevContext = TenantContextHolder.getContext();
        try {
            TenantContext ctx = new TenantContext(
                    callingUserId != null ? callingUserId : UUID.randomUUID(),
                    tenant.getId(),
                    tenant.getClientInstanceId(),
                    "STAFF_ADMIN"
            );
            TenantContextHolder.setContext(ctx);

            Double aspValue = queryMaxMetricValue("ibmi_asp_utilization_percent");
            Double winDiskValue = queryMaxMetricValue("windows_disk_usage_percent");
            
            Double ibmiCpu = queryMaxMetricValue("ibmi_cpu_utilization_percent");
            Double winCpu = queryMaxMetricValue("windows_cpu_usage_percent");
            Double cpuValue = maxOf(ibmiCpu, winCpu);

            Double winMem = queryMaxMetricValue("windows_memory_usage_percent");
            Double jobQWaiting = queryMaxMetricValue("job_queue_monitor_waiting_jobs");
            Double jobQThreshold = queryMaxMetricValue("job_queue_monitor_threshold");
            Double jobQRatio = null;
            if (jobQWaiting != null && jobQThreshold != null && jobQThreshold > 0) {
                jobQRatio = (jobQWaiting / jobQThreshold) * 100.0;
            }
            Double memJobqValue = maxOf(winMem, jobQRatio);

            List<TenantFleetDto.InputScoreDto> inputs = new ArrayList<>();
            inputs.add(evaluateInput("asp", "IBM i ASP", aspValue));
            inputs.add(evaluateInput("disk", "Windows Disk", winDiskValue));
            inputs.add(evaluateInput("cpu", "CPU Utilization", cpuValue));
            inputs.add(evaluateInput("mem_jobq", "Memory / Job-Q", memJobqValue));

            // Check Hard Critical Override: ANY input >= criticalThreshold => tenant = RED
            TenantFleetDto.InputScoreDto criticalInput = null;
            TenantFleetDto.InputScoreDto worstInputDto = null;
            double maxInputVal = -1.0;

            for (TenantFleetDto.InputScoreDto input : inputs) {
                if (!input.isStale() && input.getValue() != null) {
                    if ("RED".equals(input.getBand())) {
                        if (criticalInput == null || input.getValue() > criticalInput.getValue()) {
                            criticalInput = input;
                        }
                    }
                    if (input.getValue() > maxInputVal) {
                        maxInputVal = input.getValue();
                        worstInputDto = input;
                    }
                }
            }

            String overallBand;
            double finalScore;
            String worstInputKey;

            if (criticalInput != null) {
                // Hard Critical Override triggered!
                overallBand = "RED";
                worstInputKey = criticalInput.getKey();
                // Compute blend score or use maximum value
                double blended = computeWeightedBlend(inputs);
                finalScore = Math.max(blended, criticalInput.getValue());
            } else {
                boolean allStale = inputs.stream().allMatch(TenantFleetDto.InputScoreDto::isStale);
                if (allStale) {
                    overallBand = "UNKNOWN";
                    finalScore = 0.0;
                    worstInputKey = null;
                } else {
                    finalScore = computeWeightedBlend(inputs);
                    worstInputKey = worstInputDto != null ? worstInputDto.getKey() : null;

                    if (finalScore >= properties.getCutoffs().getRed()) {
                        overallBand = "RED";
                    } else if (finalScore >= properties.getCutoffs().getAmber()) {
                        overallBand = "AMBER";
                    } else {
                        overallBand = "GREEN";
                    }
                }
            }

            return new TenantFleetDto(
                    tenant.getId(),
                    tenant.getName(),
                    tenant.getClientInstanceId(),
                    overallBand,
                    Math.round(finalScore * 10.0) / 10.0,
                    worstInputKey,
                    inputs
            );
        } finally {
            if (prevContext.isPresent()) {
                TenantContextHolder.setContext(prevContext.get());
            } else {
                TenantContextHolder.clear();
            }
        }
    }

    private TenantFleetDto.InputScoreDto evaluateInput(String key, String name, Double rawValue) {
        if (rawValue == null || Double.isNaN(rawValue) || Double.isInfinite(rawValue)) {
            return new TenantFleetDto.InputScoreDto(key, name, null, "UNKNOWN", true);
        }

        double val = Math.min(100.0, Math.max(0.0, rawValue));
        String band;
        if (val >= properties.getCriticalThreshold()) {
            band = "RED";
        } else if (val >= properties.getAmberThreshold()) {
            band = "AMBER";
        } else {
            band = "GREEN";
        }

        return new TenantFleetDto.InputScoreDto(key, name, Math.round(val * 10.0) / 10.0, band, false);
    }

    private double computeWeightedBlend(List<TenantFleetDto.InputScoreDto> inputs) {
        FleetHeadroomProperties.Weights w = properties.getWeights();
        Map<String, Double> weightMap = Map.of(
                "asp", w.getAsp(),
                "disk", w.getDisk(),
                "cpu", w.getCpu(),
                "mem_jobq", w.getMemJobq()
        );

        double totalWeight = 0.0;
        double weightedSum = 0.0;

        for (TenantFleetDto.InputScoreDto input : inputs) {
            if (!input.isStale() && input.getValue() != null) {
                double weight = weightMap.getOrDefault(input.getKey(), 0.10);
                totalWeight += weight;
                weightedSum += input.getValue() * weight;
            }
        }

        if (totalWeight <= 0.0) {
            return 0.0;
        }
        return weightedSum / totalWeight;
    }

    private Double queryMaxMetricValue(String query) {
        try {
            ResponseMerger.MergedResult mergedResult = queryGatewayService.executePromQlInstant(query, null, UUID.randomUUID().toString());
            if (mergedResult == null || mergedResult.getHttpStatusCode() != 200 || mergedResult.getJsonResponse() == null) {
                return null;
            }

            JsonObject json = JsonParser.parseString(mergedResult.getJsonResponse()).getAsJsonObject();
            if (!json.has("data") || !json.getAsJsonObject("data").has("result")) {
                return null;
            }

            JsonArray resultArr = json.getAsJsonObject("data").getAsJsonArray("result");
            if (resultArr.isEmpty()) {
                return null;
            }

            Double maxVal = null;
            for (JsonElement elem : resultArr) {
                if (elem.isJsonObject() && elem.getAsJsonObject().has("value")) {
                    JsonArray valArr = elem.getAsJsonObject().getAsJsonArray("value");
                    if (valArr.size() >= 2) {
                        try {
                            double val = Double.parseDouble(valArr.get(1).getAsString());
                            if (maxVal == null || val > maxVal) {
                                maxVal = val;
                            }
                        } catch (Exception ignored) {}
                    }
                }
            }
            return maxVal;
        } catch (Exception e) {
            log.debug("Error querying metric '{}': {}", query, e.getMessage());
            return null;
        }
    }

    private Double maxOf(Double a, Double b) {
        if (a == null) return b;
        if (b == null) return a;
        return Math.max(a, b);
    }

    private TenantFleetDto createStaleTenantDto(Tenant tenant) {
        List<TenantFleetDto.InputScoreDto> inputs = List.of(
                new TenantFleetDto.InputScoreDto("asp", "IBM i ASP", null, "UNKNOWN", true),
                new TenantFleetDto.InputScoreDto("disk", "Windows Disk", null, "UNKNOWN", true),
                new TenantFleetDto.InputScoreDto("cpu", "CPU Utilization", null, "UNKNOWN", true),
                new TenantFleetDto.InputScoreDto("mem_jobq", "Memory / Job-Q", null, "UNKNOWN", true)
        );
        return new TenantFleetDto(tenant.getId(), tenant.getName(), tenant.getClientInstanceId(), "UNKNOWN", 0.0, null, inputs);
    }
}
