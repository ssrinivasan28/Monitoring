package com.islandpacific.sentinel.query;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/query")
public class QueryGatewayController {

    private final QueryGatewayService queryService;

    @Autowired
    public QueryGatewayController(QueryGatewayService queryService) {
        this.queryService = queryService;
    }

    // --- Canonical Endpoints ---

    @RequestMapping(value = {"/promql", "/prometheus/query"}, method = {RequestMethod.GET, RequestMethod.POST})
    public ResponseEntity<String> promqlInstant(
            @RequestParam(name = "query") String query,
            @RequestParam(name = "time", required = false) String time,
            @RequestHeader(name = "X-Correlation-ID", required = false) String correlationId) {
        String corrId = resolveCorrelationId(correlationId);
        ResponseMerger.MergedResult result = queryService.executePromQlInstant(query, time, corrId);
        return ResponseEntity.status(result.getHttpStatusCode()).header("X-Correlation-ID", corrId).body(result.getJsonResponse());
    }

    @RequestMapping(value = {"/promql/range", "/prometheus/query_range"}, method = {RequestMethod.GET, RequestMethod.POST})
    public ResponseEntity<String> promqlRange(
            @RequestParam(name = "query") String query,
            @RequestParam(name = "start") double start,
            @RequestParam(name = "end") double end,
            @RequestParam(name = "step", required = false, defaultValue = "14s") String step,
            @RequestHeader(name = "X-Correlation-ID", required = false) String correlationId) {
        String corrId = resolveCorrelationId(correlationId);
        ResponseMerger.MergedResult result = queryService.executePromQlRange(query, start, end, step, corrId);
        return ResponseEntity.status(result.getHttpStatusCode()).header("X-Correlation-ID", corrId).body(result.getJsonResponse());
    }

    @RequestMapping(value = {"/logql", "/loki/query"}, method = {RequestMethod.GET, RequestMethod.POST})
    public ResponseEntity<String> logqlInstant(
            @RequestParam(name = "query") String query,
            @RequestParam(name = "time", required = false) String time,
            @RequestHeader(name = "X-Correlation-ID", required = false) String correlationId) {
        String corrId = resolveCorrelationId(correlationId);
        ResponseMerger.MergedResult result = queryService.executeLogQlInstant(query, time, corrId);
        return ResponseEntity.status(result.getHttpStatusCode()).header("X-Correlation-ID", corrId).body(result.getJsonResponse());
    }

    @RequestMapping(value = {"/logql/range", "/loki/query_range"}, method = {RequestMethod.GET, RequestMethod.POST})
    public ResponseEntity<String> logqlRange(
            @RequestParam(name = "query") String query,
            @RequestParam(name = "start") double start,
            @RequestParam(name = "end") double end,
            @RequestParam(name = "step", required = false, defaultValue = "14s") String step,
            @RequestHeader(name = "X-Correlation-ID", required = false) String correlationId) {
        String corrId = resolveCorrelationId(correlationId);
        ResponseMerger.MergedResult result = queryService.executeLogQlRange(query, start, end, step, corrId);
        return ResponseEntity.status(result.getHttpStatusCode()).header("X-Correlation-ID", corrId).body(result.getJsonResponse());
    }

    private String resolveCorrelationId(String headerId) {
        if (headerId != null && !headerId.isBlank()) {
            return headerId.trim();
        }
        return UUID.randomUUID().toString();
    }
}
