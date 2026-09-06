package com.islandpacific.sentinel.query;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

@Component
public class QueryFanoutExecutor {

    @Value("${sentinel.datasource.limits.backend-timeout-ms:10000}")
    private int backendTimeoutMs;

    @Value("${sentinel.datasource.limits.max-concurrent-backend-requests-per-query:10}")
    private int maxConcurrentBackendRequests;

    @Value("${sentinel.datasource.limits.max-response-bytes:5242880}")
    private long maxResponseBytes = 5242880L;

    private final HttpClient httpClient;

    public QueryFanoutExecutor() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .followRedirects(HttpClient.Redirect.NEVER)
                .build();
    }

    public Map<DatasourceResolverService.ResolvedDatasource, BackendResponse> executeFanout(
            List<DatasourceResolverService.ResolvedDatasource> datasources,
            String path,
            Map<String, String> queryParams,
            String correlationId) {

        if (datasources == null || datasources.isEmpty()) {
            return Collections.emptyMap();
        }

        int concurrency = Math.min(datasources.size(), maxConcurrentBackendRequests);
        ExecutorService executor = Executors.newFixedThreadPool(concurrency);

        try {
            List<CompletableFuture<Map.Entry<DatasourceResolverService.ResolvedDatasource, BackendResponse>>> futures = datasources.stream()
                    .map(ds -> CompletableFuture.supplyAsync(() -> {
                        long start = System.currentTimeMillis();
                        try {
                            String url = buildFullUrl(ds.getUrl(), path, queryParams);
                            HttpRequest.Builder reqBuilder = HttpRequest.newBuilder()
                                    .uri(URI.create(url))
                                    .GET()
                                    .timeout(Duration.ofMillis(backendTimeoutMs));

                            if (correlationId != null && !correlationId.isBlank()) {
                                reqBuilder.header("X-Correlation-ID", correlationId);
                            }

                            // Attach authentication header if configured
                            if ("token".equalsIgnoreCase(ds.getAuthType()) && ds.getCredentials() != null) {
                                reqBuilder.header("Authorization", "Bearer " + ds.getCredentials());
                            } else if ("basic".equalsIgnoreCase(ds.getAuthType()) && ds.getCredentials() != null) {
                                reqBuilder.header("Authorization", "Basic " + Base64.getEncoder().encodeToString(ds.getCredentials().getBytes(StandardCharsets.UTF_8)));
                            }

                            HttpResponse<String> response = httpClient.send(reqBuilder.build(), responseInfo -> new BoundedBodySubscriber(maxResponseBytes));
                            long latency = System.currentTimeMillis() - start;

                            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                                return Map.entry(ds, new BackendResponse(response.statusCode(), response.body(), null, latency, true));
                            } else {
                                return Map.entry(ds, new BackendResponse(response.statusCode(), response.body(), "Backend returned HTTP " + response.statusCode(), latency, false));
                            }
                        } catch (Exception e) {
                            long latency = System.currentTimeMillis() - start;
                            String msg = e.getMessage() != null && e.getMessage().contains("PAYLOAD_BYTES_EXCEEDED") ?
                                    "PAYLOAD_BYTES_EXCEEDED" : e.getMessage();
                            return Map.entry(ds, new BackendResponse(502, null, msg, latency, false));
                        }
                    }, executor))
                    .collect(Collectors.toList());

            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

            Map<DatasourceResolverService.ResolvedDatasource, BackendResponse> results = new HashMap<>();
            for (var future : futures) {
                try {
                    var entry = future.get();
                    results.put(entry.getKey(), entry.getValue());
                } catch (Exception e) {
                    // Ignore individual future extraction error
                }
            }
            return results;
        } finally {
            executor.shutdown();
        }
    }

    private String buildFullUrl(String baseUrl, String path, Map<String, String> queryParams) {
        String base = baseUrl.trim();
        if (base.endsWith("/")) {
            base = base.substring(0, base.length() - 1);
        }
        String p = path.trim();
        if (!p.startsWith("/")) {
            p = "/" + p;
        }

        StringBuilder sb = new StringBuilder(base + p);
        if (queryParams != null && !queryParams.isEmpty()) {
            sb.append("?");
            boolean first = true;
            for (Map.Entry<String, String> entry : queryParams.entrySet()) {
                if (!first) sb.append("&");
                sb.append(URLEncoder.encode(entry.getKey(), StandardCharsets.UTF_8));
                sb.append("=");
                sb.append(URLEncoder.encode(entry.getValue(), StandardCharsets.UTF_8));
                first = false;
            }
        }
        return sb.toString();
    }

    public static class BoundedBodySubscriber implements HttpResponse.BodySubscriber<String> {
        private final HttpResponse.BodySubscriber<String> delegate = HttpResponse.BodySubscribers.ofString(StandardCharsets.UTF_8);
        private final long maxBytes;
        private long currentBytes = 0;
        private boolean exceeded = false;
        private java.util.concurrent.Flow.Subscription subscription;

        public BoundedBodySubscriber(long maxBytes) {
            this.maxBytes = maxBytes;
        }

        @Override
        public java.util.concurrent.CompletionStage<String> getBody() {
            return delegate.getBody().thenApply(s -> {
                if (exceeded) {
                    throw new IllegalStateException("PAYLOAD_BYTES_EXCEEDED");
                }
                return s;
            });
        }

        @Override
        public void onSubscribe(java.util.concurrent.Flow.Subscription subscription) {
            this.subscription = subscription;
            delegate.onSubscribe(subscription);
        }

        @Override
        public void onNext(List<java.nio.ByteBuffer> buffers) {
            if (exceeded) return;
            for (java.nio.ByteBuffer buf : buffers) {
                currentBytes += buf.remaining();
                if (currentBytes > maxBytes) {
                    exceeded = true;
                    if (subscription != null) {
                        subscription.cancel();
                    }
                    delegate.onError(new IllegalStateException("PAYLOAD_BYTES_EXCEEDED"));
                    return;
                }
            }
            delegate.onNext(buffers);
        }

        @Override
        public void onError(Throwable throwable) {
            delegate.onError(throwable);
        }

        @Override
        public void onComplete() {
            if (!exceeded) {
                delegate.onComplete();
            }
        }
    }

    public static class BackendResponse {
        private final int statusCode;
        private final String body;
        private final String errorMessage;
        private final long latencyMs;
        private final boolean success;

        public BackendResponse(int statusCode, String body, String errorMessage, long latencyMs, boolean success) {
            this.statusCode = statusCode;
            this.body = body;
            this.errorMessage = errorMessage;
            this.latencyMs = latencyMs;
            this.success = success;
        }

        public int getStatusCode() { return statusCode; }
        public String getBody() { return body; }
        public String getErrorMessage() { return errorMessage; }
        public long getLatencyMs() { return latencyMs; }
        public boolean isSuccess() { return success; }
    }
}
