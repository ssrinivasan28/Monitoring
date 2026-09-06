package com.islandpacific.sentinel;

import com.islandpacific.sentinel.query.DatasourceResolverService;
import com.islandpacific.sentinel.query.QueryFanoutExecutor;
import com.islandpacific.sentinel.query.ResponseMerger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class ResponseSecurityTest {

    private ResponseMerger merger;

    @BeforeEach
    void setUp() {
        merger = new ResponseMerger();
        ReflectionTestUtils.setField(merger, "maxResultSeries", 2); // Set small max series for testing
        ReflectionTestUtils.setField(merger, "maxResponseBytes", 1024L); // Set small max bytes for testing
    }

    @Test
    void testUpstreamSentinelSourceSpoofingOverwritten() {
        DatasourceResolverService.ResolvedDatasource ds = new DatasourceResolverService.ResolvedDatasource(
                UUID.randomUUID(), UUID.randomUUID(), "authentic-source-1", "prometheus", "https://prometheus.io", "none", null
        );

        String spoofedBackendJson = "{"
                + "\"status\":\"success\","
                + "\"data\":{"
                + "  \"resultType\":\"vector\","
                + "  \"result\":["
                + "    {\"metric\":{\"__sentinel_source\":\"fake-spoofed-source\",\"job\":\"api\"},\"value\":[1600000000,\"100\"]}"
                + "  ]"
                + "}"
                + "}";

        QueryFanoutExecutor.BackendResponse response = new QueryFanoutExecutor.BackendResponse(200, spoofedBackendJson, null, 10, true);
        Map<DatasourceResolverService.ResolvedDatasource, QueryFanoutExecutor.BackendResponse> responses = Map.of(ds, response);

        ResponseMerger.MergedResult merged = merger.mergeResponses(responses);

        assertEquals(200, merged.getHttpStatusCode());
        assertTrue(merged.getJsonResponse().contains("\"__sentinel_source\":\"authentic-source-1\""));
        assertFalse(merged.getJsonResponse().contains("fake-spoofed-source"));
    }

    @Test
    void testMaxSeriesCardinalityTruncation() {
        DatasourceResolverService.ResolvedDatasource ds = new DatasourceResolverService.ResolvedDatasource(
                UUID.randomUUID(), UUID.randomUUID(), "authentic-source-1", "prometheus", "https://prometheus.io", "none", null
        );

        String largeSeriesJson = "{"
                + "\"status\":\"success\","
                + "\"data\":{"
                + "  \"resultType\":\"vector\","
                + "  \"result\":["
                + "    {\"metric\":{\"instance\":\"1\"},\"value\":[1600000000,\"1\"]},"
                + "    {\"metric\":{\"instance\":\"2\"},\"value\":[1600000000,\"2\"]},"
                + "    {\"metric\":{\"instance\":\"3\"},\"value\":[1600000000,\"3\"]}"
                + "  ]"
                + "}"
                + "}";

        QueryFanoutExecutor.BackendResponse response = new QueryFanoutExecutor.BackendResponse(200, largeSeriesJson, null, 10, true);
        Map<DatasourceResolverService.ResolvedDatasource, QueryFanoutExecutor.BackendResponse> responses = Map.of(ds, response);

        ResponseMerger.MergedResult merged = merger.mergeResponses(responses);

        assertEquals(200, merged.getHttpStatusCode());
        assertTrue(merged.getJsonResponse().contains("Result truncated: exceeded maximum series limit (2)"));
    }

    @Test
    void testMaxResponseBytesPayloadSkipped() {
        DatasourceResolverService.ResolvedDatasource ds = new DatasourceResolverService.ResolvedDatasource(
                UUID.randomUUID(), UUID.randomUUID(), "authentic-source-1", "prometheus", "https://prometheus.io", "none", null
        );

        StringBuilder hugeBody = new StringBuilder("{\"status\":\"success\",\"data\":{\"resultType\":\"vector\",\"result\":[");
        for (int i = 0; i < 100; i++) {
            hugeBody.append("{\"metric\":{\"padding\":\"").append("x".repeat(20)).append("\"},\"value\":[1600000000,\"1\"]},");
        }
        hugeBody.append("{\"metric\":{\"instance\":\"end\"},\"value\":[1600000000,\"1\"]}]}}");

        QueryFanoutExecutor.BackendResponse response = new QueryFanoutExecutor.BackendResponse(200, hugeBody.toString(), null, 10, true);
        Map<DatasourceResolverService.ResolvedDatasource, QueryFanoutExecutor.BackendResponse> responses = Map.of(ds, response);

        ResponseMerger.MergedResult merged = merger.mergeResponses(responses);

        assertEquals(502, merged.getHttpStatusCode()); // All payloads exceeded byte limit
        assertTrue(merged.getJsonResponse().contains("payload exceeded maximum size limit"));
    }
}
