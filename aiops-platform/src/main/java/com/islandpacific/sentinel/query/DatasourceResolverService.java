package com.islandpacific.sentinel.query;

import com.islandpacific.sentinel.entity.TenantDatasource;
import com.islandpacific.sentinel.repository.TenantDatasourceRepository;
import com.islandpacific.sentinel.security.SecretProtector;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class DatasourceResolverService {

    @Value("${sentinel.datasource.central.enabled:true}")
    private boolean centralEnabled;

    @Value("${sentinel.datasource.central.tenant_aware:true}")
    private boolean centralTenantAware;

    @Value("${sentinel.datasource.central.prometheus.url:http://localhost:9090}")
    private String centralPrometheusUrl;

    @Value("${sentinel.datasource.central.loki.url:http://localhost:3100}")
    private String centralLokiUrl;

    @Value("${sentinel.datasource.limits.max-fanout-datasources:10}")
    private int maxFanoutDatasources;

    private final TenantDatasourceRepository repository;
    private final SecretProtector secretProtector;

    @Autowired
    public DatasourceResolverService(TenantDatasourceRepository repository, SecretProtector secretProtector) {
        this.repository = repository;
        this.secretProtector = secretProtector;
    }

    public List<ResolvedDatasource> resolveDatasources(UUID tenantId, String kind) {
        if (tenantId == null) {
            throw new IllegalArgumentException("Tenant ID must not be null");
        }
        String normalizedKind = kind.toLowerCase();

        List<TenantDatasource> allTenantSources = repository.findByTenantId(tenantId);

        // Scenario 1: Tenant has 0 registered datasources -> Fallback to central if permitted
        if (allTenantSources.isEmpty()) {
            if (centralEnabled && centralTenantAware) {
                String fallbackUrl = "loki".equalsIgnoreCase(normalizedKind) ? centralLokiUrl : centralPrometheusUrl;
                ResolvedDatasource centralSource = new ResolvedDatasource(
                        UUID.nameUUIDFromBytes(("central-" + normalizedKind).getBytes()),
                        tenantId,
                        "central-" + normalizedKind,
                        normalizedKind,
                        fallbackUrl,
                        "none",
                        null
                );
                return List.of(centralSource);
            } else {
                throw new QueryPolicyException("No datasources configured for tenant " + tenantId + " and central fallback is disabled");
            }
        }

        // Scenario 2: Tenant has registered datasources, check if any enabled
        List<TenantDatasource> enabledSources = allTenantSources.stream()
                .filter(TenantDatasource::isEnabled)
                .collect(Collectors.toList());

        if (enabledSources.isEmpty()) {
            throw new QueryPolicyException("No active datasources enabled for tenant " + tenantId);
        }

        // Scenario 3: Filter by kind (prometheus/thanos or loki)
        List<TenantDatasource> matchingKindSources = enabledSources.stream()
                .filter(ds -> isKindCompatible(ds.getKind(), normalizedKind))
                .collect(Collectors.toList());

        if (matchingKindSources.isEmpty()) {
            throw new QueryPolicyException("No compatible datasource of kind '" + normalizedKind + "' found for tenant " + tenantId);
        }

        // Enforce max fanout count limit
        if (matchingKindSources.size() > maxFanoutDatasources) {
            matchingKindSources = matchingKindSources.subList(0, maxFanoutDatasources);
        }

        List<ResolvedDatasource> resolved = new ArrayList<>();
        for (TenantDatasource ds : matchingKindSources) {
            String decryptedCreds = null;
            if (ds.getCredentialsRef() != null && !ds.getCredentialsRef().isBlank()) {
                decryptedCreds = secretProtector.resolve(ds.getCredentialsRef());
            }

            resolved.add(new ResolvedDatasource(
                    ds.getId(),
                    ds.getTenantId(),
                    ds.getName(),
                    ds.getKind(),
                    ds.getUrl(),
                    ds.getAuthType(),
                    decryptedCreds
            ));
        }

        return resolved;
    }

    private boolean isKindCompatible(String sourceKind, String requestedKind) {
        if (sourceKind == null || requestedKind == null) return false;
        String s = sourceKind.toLowerCase();
        String r = requestedKind.toLowerCase();

        if (r.equals("prometheus")) {
            return s.equals("prometheus") || s.equals("thanos");
        }
        return s.equals(r);
    }

    public static class ResolvedDatasource {
        private final UUID id;
        private final UUID tenantId;
        private final String name;
        private final String kind;
        private final String url;
        private final String authType;
        private final String credentials;

        public ResolvedDatasource(UUID id, UUID tenantId, String name, String kind, String url, String authType, String credentials) {
            this.id = id;
            this.tenantId = tenantId;
            this.name = name;
            this.kind = kind;
            this.url = url;
            this.authType = authType;
            this.credentials = credentials;
        }

        public UUID getId() { return id; }
        public UUID getTenantId() { return tenantId; }
        public String getName() { return name; }
        public String getKind() { return kind; }
        public String getUrl() { return url; }
        public String getAuthType() { return authType; }
        public String getCredentials() { return credentials; }
    }
}
