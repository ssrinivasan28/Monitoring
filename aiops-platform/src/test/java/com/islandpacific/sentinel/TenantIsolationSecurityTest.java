package com.islandpacific.sentinel;

import com.islandpacific.sentinel.entity.TenantDatasource;
import com.islandpacific.sentinel.query.DatasourceResolverService;
import com.islandpacific.sentinel.query.QueryPolicyException;
import com.islandpacific.sentinel.repository.TenantDatasourceRepository;
import com.islandpacific.sentinel.security.SecretProtector;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

class TenantIsolationSecurityTest {

    private TenantDatasourceRepository repository;
    private SecretProtector secretProtector;
    private DatasourceResolverService resolverService;

    private final UUID tenantA = UUID.randomUUID();
    private final UUID tenantB = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        repository = Mockito.mock(TenantDatasourceRepository.class);
        secretProtector = Mockito.mock(SecretProtector.class);
        resolverService = new DatasourceResolverService(repository, secretProtector);

        ReflectionTestUtils.setField(resolverService, "centralEnabled", true);
        ReflectionTestUtils.setField(resolverService, "centralTenantAware", true);
        ReflectionTestUtils.setField(resolverService, "centralPrometheusUrl", "http://prometheus.io:9090");
        ReflectionTestUtils.setField(resolverService, "centralLokiUrl", "http://prometheus.io:3100");
        ReflectionTestUtils.setField(resolverService, "maxFanoutDatasources", 10);
    }

    @Test
    void testTenantCannotQueryOtherTenantDatasources() {
        TenantDatasource dsB = new TenantDatasource(tenantB, "Tenant B Source", "prometheus", "https://prometheus.io");
        when(repository.findByTenantId(tenantA)).thenReturn(Collections.emptyList());
        when(repository.findByTenantId(tenantB)).thenReturn(List.of(dsB));

        // When Tenant A resolves datasources, it should NOT see Tenant B's source
        var resolvedA = resolverService.resolveDatasources(tenantA, "prometheus");
        assertEquals(1, resolvedA.size());
        assertEquals("central-prometheus", resolvedA.get(0).getName());
        assertEquals(tenantA, resolvedA.get(0).getTenantId());
    }

    @Test
    void testInactiveDatasourceCannotBeSelected() {
        TenantDatasource inactiveDs = new TenantDatasource(tenantA, "Inactive Source", "prometheus", "https://prometheus.io");
        inactiveDs.setEnabled(false);
        when(repository.findByTenantId(tenantA)).thenReturn(List.of(inactiveDs));

        assertThrows(QueryPolicyException.class, () -> resolverService.resolveDatasources(tenantA, "prometheus"));
    }

    @Test
    void testCentralFallbackDisabledRejectsQuery() {
        ReflectionTestUtils.setField(resolverService, "centralEnabled", false);
        when(repository.findByTenantId(tenantA)).thenReturn(Collections.emptyList());

        assertThrows(QueryPolicyException.class, () -> resolverService.resolveDatasources(tenantA, "prometheus"));
    }

    @Test
    void testCentralTenantAwareFalseRejectsQuery() {
        ReflectionTestUtils.setField(resolverService, "centralTenantAware", false);
        when(repository.findByTenantId(tenantA)).thenReturn(Collections.emptyList());

        assertThrows(QueryPolicyException.class, () -> resolverService.resolveDatasources(tenantA, "prometheus"));
    }

    @Test
    void testFallbackOnlyAvailableWhenTenantHasZeroRegisteredDatasources() {
        TenantDatasource dsA = new TenantDatasource(tenantA, "Tenant A Source", "prometheus", "https://prometheus.io");
        dsA.setEnabled(true);
        when(repository.findByTenantId(tenantA)).thenReturn(List.of(dsA));

        var resolved = resolverService.resolveDatasources(tenantA, "prometheus");
        assertEquals(1, resolved.size());
        assertEquals("Tenant A Source", resolved.get(0).getName());
        assertNotEquals("central-prometheus", resolved.get(0).getName());
    }

    @Test
    void testIncompatibleKindReturnsPolicyException() {
        TenantDatasource dsLoki = new TenantDatasource(tenantA, "Loki Source", "loki", "https://prometheus.io:3100");
        dsLoki.setEnabled(true);
        when(repository.findByTenantId(tenantA)).thenReturn(List.of(dsLoki));

        assertThrows(QueryPolicyException.class, () -> resolverService.resolveDatasources(tenantA, "prometheus"));
    }
}
