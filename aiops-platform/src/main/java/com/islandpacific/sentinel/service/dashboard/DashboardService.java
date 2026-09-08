package com.islandpacific.sentinel.service.dashboard;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.islandpacific.sentinel.entity.DashboardEntity;
import com.islandpacific.sentinel.repository.DashboardRepository;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.InputStream;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class DashboardService {

    private static final Logger log = LoggerFactory.getLogger(DashboardService.class);

    private final DashboardRepository dashboardRepository;
    private final GrafanaDashboardImporter grafanaImporter;
    private final ObjectMapper objectMapper;

    // In-memory cache for bundled seed definitions loaded from classpath:dashboards/*.json
    private final Map<String, DashboardDefinitionDto> bundledDashboards = new ConcurrentHashMap<>();

    public DashboardService(DashboardRepository dashboardRepository,
                            GrafanaDashboardImporter grafanaImporter,
                            ObjectMapper objectMapper) {
        this.dashboardRepository = dashboardRepository;
        this.grafanaImporter = grafanaImporter;
        this.objectMapper = objectMapper;
    }

    @PostConstruct
    public void loadBundledDashboards() {
        try {
            PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
            Resource[] resources = resolver.getResources("classpath:dashboards/*.json");
            for (Resource r : resources) {
                try (InputStream is = r.getInputStream()) {
                    DashboardDefinitionDto def = objectMapper.readValue(is, DashboardDefinitionDto.class);
                    if (def != null && def.getId() != null) {
                        def.setCustom(false);
                        bundledDashboards.put(def.getId(), def);
                        log.info("Loaded bundled seed dashboard: {} [{}]", def.getTitle(), def.getId());
                    }
                } catch (Exception e) {
                    log.error("Failed to load bundled dashboard resource: {}", r.getFilename(), e);
                }
            }
        } catch (Exception e) {
            log.warn("No bundled dashboards loaded or error scanning classpath:dashboards/*.json", e);
        }
    }

    public List<DashboardDefinitionDto> getDashboardsForTenant(UUID tenantId) {
        Map<String, DashboardDefinitionDto> merged = new LinkedHashMap<>();

        // 1. Add bundled seed dashboards
        bundledDashboards.values().forEach(b -> merged.put(b.getId(), copyDto(b)));

        // 2. Add global & tenant-custom DB dashboards
        if (tenantId != null) {
            List<DashboardEntity> dbEntities = dashboardRepository.findAllGlobalAndTenant(tenantId);
            for (DashboardEntity entity : dbEntities) {
                try {
                    DashboardDefinitionDto dto = objectMapper.readValue(entity.getDefinitionJson(), DashboardDefinitionDto.class);
                    dto.setCustom(entity.getTenantId() != null);
                    merged.put(dto.getId(), dto);
                } catch (Exception e) {
                    log.error("Error parsing dashboard entity definition JSON for ID: {}", entity.getId(), e);
                }
            }
        }

        return new ArrayList<>(merged.values());
    }

    public Optional<DashboardDefinitionDto> getDashboard(String id, UUID tenantId) {
        // 1. Check custom tenant DB dashboard first
        if (tenantId != null) {
            Optional<DashboardEntity> tenantEntity = dashboardRepository.findByIdAndTenantId(id, tenantId);
            if (tenantEntity.isPresent()) {
                return parseEntity(tenantEntity.get(), true);
            }
        }

        // 2. Check global DB dashboard
        Optional<DashboardEntity> globalEntity = dashboardRepository.findByIdAndTenantIdIsNull(id);
        if (globalEntity.isPresent()) {
            return parseEntity(globalEntity.get(), false);
        }

        // 3. Fallback to bundled classpath dashboard
        if (bundledDashboards.containsKey(id)) {
            return Optional.of(copyDto(bundledDashboards.get(id)));
        }

        return Optional.empty();
    }

    @Transactional
    public DashboardDefinitionDto saveCustomDashboard(UUID tenantId, DashboardDefinitionDto dto) {
        if (tenantId == null) {
            throw new IllegalArgumentException("Tenant ID cannot be null when saving a custom dashboard.");
        }
        if (dto.getId() == null || dto.getId().isBlank()) {
            dto.setId("custom-" + UUID.randomUUID().toString().substring(0, 8));
        }

        dto.setCustom(true);
        try {
            String json = objectMapper.writeValueAsString(dto);
            DashboardEntity entity = new DashboardEntity(dto.getId(), tenantId, json);
            dashboardRepository.save(entity);
            return dto;
        } catch (Exception e) {
            throw new RuntimeException("Failed to save dashboard definition: " + e.getMessage(), e);
        }
    }

    @Transactional
    public void deleteCustomDashboard(String id, UUID tenantId) {
        if (tenantId == null) {
            throw new IllegalArgumentException("Tenant ID cannot be null when deleting a custom dashboard.");
        }
        dashboardRepository.deleteByIdAndTenantId(id, tenantId);
    }

    @Transactional
    public DashboardDefinitionDto importGrafanaDashboard(UUID tenantId, String grafanaJson) {
        DashboardDefinitionDto imported = grafanaImporter.convertGrafanaToSentinel(grafanaJson);
        return saveCustomDashboard(tenantId, imported);
    }

    private Optional<DashboardDefinitionDto> parseEntity(DashboardEntity entity, boolean isCustom) {
        try {
            DashboardDefinitionDto dto = objectMapper.readValue(entity.getDefinitionJson(), DashboardDefinitionDto.class);
            dto.setCustom(isCustom);
            return Optional.of(dto);
        } catch (Exception e) {
            log.error("Error parsing dashboard JSON for entity ID: {}", entity.getId(), e);
            return Optional.empty();
        }
    }

    private DashboardDefinitionDto copyDto(DashboardDefinitionDto src) {
        try {
            String json = objectMapper.writeValueAsString(src);
            DashboardDefinitionDto copy = objectMapper.readValue(json, DashboardDefinitionDto.class);
            copy.setCustom(src.isCustom());
            return copy;
        } catch (Exception e) {
            return src;
        }
    }
}
