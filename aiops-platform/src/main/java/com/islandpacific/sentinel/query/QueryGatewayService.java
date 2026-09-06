package com.islandpacific.sentinel.query;

import com.islandpacific.sentinel.security.TenantContext;
import com.islandpacific.sentinel.security.TenantContextHolder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class QueryGatewayService {

    private final QueryPolicyService policyService;
    private final PromQlAstSanitizer promQlSanitizer;
    private final LogQlAstSanitizer logQlSanitizer;
    private final DatasourceResolverService resolverService;
    private final QueryFanoutExecutor fanoutExecutor;
    private final ResponseMerger responseMerger;
    private final QueryAuditService auditService;

    @Autowired
    public QueryGatewayService(
            QueryPolicyService policyService,
            PromQlAstSanitizer promQlSanitizer,
            LogQlAstSanitizer logQlSanitizer,
            DatasourceResolverService resolverService,
            QueryFanoutExecutor fanoutExecutor,
            ResponseMerger responseMerger,
            QueryAuditService auditService) {
        this.policyService = policyService;
        this.promQlSanitizer = promQlSanitizer;
        this.logQlSanitizer = logQlSanitizer;
        this.resolverService = resolverService;
        this.fanoutExecutor = fanoutExecutor;
        this.responseMerger = responseMerger;
        this.auditService = auditService;
    }

    public ResponseMerger.MergedResult executePromQlInstant(String query, String timeStr, String correlationId) {
        return processQuery("PROMQL_INSTANT", "prometheus", query, false, 0, 0, null, timeStr, correlationId);
    }

    public ResponseMerger.MergedResult executePromQlRange(String query, double start, double end, String step, String correlationId) {
        return processQuery("PROMQL_RANGE", "prometheus", query, true, start, end, step, null, correlationId);
    }

    public ResponseMerger.MergedResult executeLogQlInstant(String query, String timeStr, String correlationId) {
        return processQuery("LOGQL_INSTANT", "loki", query, false, 0, 0, null, timeStr, correlationId);
    }

    public ResponseMerger.MergedResult executeLogQlRange(String query, double start, double end, String step, String correlationId) {
        return processQuery("LOGQL_RANGE", "loki", query, true, start, end, step, null, correlationId);
    }

    private ResponseMerger.MergedResult processQuery(
            String queryType,
            String kind,
            String query,
            boolean isRange,
            double start,
            double end,
            String step,
            String timeStr,
            String correlationId) {

        long startTime = System.currentTimeMillis();
        TenantContext tenantCtx = TenantContextHolder.getContext().orElse(null);

        if (tenantCtx == null || tenantCtx.getTenantId() == null) {
            throw new org.springframework.security.access.AccessDeniedException("Valid tenant context required for query execution");
        }

        UUID userId = tenantCtx.getUserId();
        UUID tenantId = tenantCtx.getTenantId();

        // 1. Policy bounds check
        if (isRange) {
            policyService.validateRangeQuery(query, start, end, step);
        } else {
            policyService.validateInstantQuery(query);
        }

        // 2. Concurrency acquire
        policyService.acquireSlots(tenantId);

        String sanitizedQuery = null;
        List<DatasourceResolverService.ResolvedDatasource> datasources = null;
        ResponseMerger.MergedResult mergedResult = null;
        String status = "FAILED";
        boolean partialFailure = false;
        int failureCount = 0;

        try {
            // 3. AST Query Sanitization & Tenant Label Injection
            if ("loki".equalsIgnoreCase(kind)) {
                sanitizedQuery = logQlSanitizer.sanitize(query, tenantId.toString());
            } else {
                sanitizedQuery = promQlSanitizer.sanitize(query, tenantId.toString());
            }

            // 4. Datasource Resolution
            datasources = resolverService.resolveDatasources(tenantId, kind);

            // 5. Query Fanout Execution
            Map<String, String> queryParams = new HashMap<>();
            queryParams.put("query", sanitizedQuery);
            if (isRange) {
                queryParams.put("start", String.valueOf(start));
                queryParams.put("end", String.valueOf(end));
                if (step != null && !step.isBlank()) {
                    queryParams.put("step", step);
                }
            } else if (timeStr != null && !timeStr.isBlank()) {
                queryParams.put("time", timeStr);
            }

            String backendPath = isRange ?
                    ("loki".equalsIgnoreCase(kind) ? "/loki/api/v1/query_range" : "/api/v1/query_range") :
                    ("loki".equalsIgnoreCase(kind) ? "/loki/api/v1/query" : "/api/v1/query");

            Map<DatasourceResolverService.ResolvedDatasource, QueryFanoutExecutor.BackendResponse> responses =
                    fanoutExecutor.executeFanout(datasources, backendPath, queryParams, correlationId);

            // Calculate failures
            for (var entry : responses.entrySet()) {
                if (!entry.getValue().isSuccess()) {
                    failureCount++;
                }
            }
            int total = responses.size();
            if (failureCount == 0) {
                status = "SUCCESS";
                partialFailure = false;
            } else if (failureCount < total) {
                status = "PARTIAL_FAILURE";
                partialFailure = true;
            } else {
                status = "ALL_FAILED";
                partialFailure = true;
            }

            // 6. Response Merging
            mergedResult = responseMerger.mergeResponses(responses);
            return mergedResult;

        } finally {
            policyService.releaseSlots(tenantId);
            long latencyMs = System.currentTimeMillis() - startTime;

            List<String> datasourceNames = datasources != null ?
                    datasources.stream().map(DatasourceResolverService.ResolvedDatasource::getName).collect(Collectors.toList()) : null;

            // 7. Mandatory Service-Layer Query Auditing
            auditService.recordQueryExecution(
                    userId,
                    tenantId,
                    queryType,
                    query,
                    sanitizedQuery != null ? sanitizedQuery : query,
                    datasourceNames,
                    latencyMs,
                    status,
                    partialFailure,
                    failureCount,
                    correlationId
            );
        }
    }
}
