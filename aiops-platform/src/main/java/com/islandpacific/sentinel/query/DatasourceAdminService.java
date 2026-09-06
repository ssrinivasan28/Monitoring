package com.islandpacific.sentinel.query;

import com.islandpacific.sentinel.entity.TenantDatasource;
import com.islandpacific.sentinel.repository.TenantDatasourceRepository;
import com.islandpacific.sentinel.security.SecretProtector;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class DatasourceAdminService {

    private final TenantDatasourceRepository repository;
    private final SecretProtector secretProtector;
    private final SsrfProtectionValidator ssrfProtectionValidator;
    private final HttpClient httpClient;

    @Autowired
    public DatasourceAdminService(TenantDatasourceRepository repository,
                                  SecretProtector secretProtector,
                                  SsrfProtectionValidator ssrfProtectionValidator) {
        this.repository = repository;
        this.secretProtector = secretProtector;
        this.ssrfProtectionValidator = ssrfProtectionValidator;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(3))
                .followRedirects(HttpClient.Redirect.NEVER)
                .build();
    }

    public List<TenantDatasourceResponseDto> listDatasources(UUID tenantId) {
        List<TenantDatasource> datasources;
        if (tenantId != null) {
            datasources = repository.findByTenantId(tenantId);
        } else {
            datasources = repository.findAll();
        }
        return datasources.stream()
                .map(TenantDatasourceResponseDto::fromEntity)
                .collect(Collectors.toList());
    }

    public TenantDatasourceResponseDto getDatasource(UUID id) {
        TenantDatasource entity = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Datasource not found: " + id));
        return TenantDatasourceResponseDto.fromEntity(entity);
    }

    @Transactional
    public TenantDatasourceResponseDto createDatasource(TenantDatasourceCreateDto dto) {
        if (dto.getTenantId() == null) {
            throw new IllegalArgumentException("Tenant ID must not be null");
        }
        if (dto.getName() == null || dto.getName().isBlank()) {
            throw new IllegalArgumentException("Name must not be null or empty");
        }
        if (dto.getKind() == null || dto.getKind().isBlank()) {
            throw new IllegalArgumentException("Kind must not be null or empty");
        }

        // Validate URL for SSRF protection
        ssrfProtectionValidator.validateUrl(dto.getUrl(), dto.getKind());

        TenantDatasource entity = new TenantDatasource(dto.getTenantId(), dto.getName(), dto.getKind().toLowerCase(), dto.getUrl());
        entity.setAuthType(dto.getAuthType() != null ? dto.getAuthType() : "none");

        if (dto.getCredentialsRef() != null && !dto.getCredentialsRef().isBlank()) {
            entity.setCredentialsRef(secretProtector.protect(dto.getCredentialsRef()));
        }

        entity.setEnabled(dto.isEnabled());
        entity.setDefault(dto.isDefault());
        entity.setCreatedAt(Instant.now());
        entity.setUpdatedAt(Instant.now());

        TenantDatasource saved = repository.save(entity);
        return TenantDatasourceResponseDto.fromEntity(saved);
    }

    @Transactional
    public TenantDatasourceResponseDto updateDatasource(UUID id, TenantDatasourceUpdateDto dto) {
        TenantDatasource entity = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Datasource not found: " + id));

        if (dto.getName() != null && !dto.getName().isBlank()) {
            entity.setName(dto.getName());
        }
        if (dto.getKind() != null && !dto.getKind().isBlank()) {
            entity.setKind(dto.getKind().toLowerCase());
        }
        if (dto.getUrl() != null && !dto.getUrl().isBlank()) {
            ssrfProtectionValidator.validateUrl(dto.getUrl(), entity.getKind());
            entity.setUrl(dto.getUrl());
        }
        if (dto.getAuthType() != null) {
            entity.setAuthType(dto.getAuthType());
        }
        if (dto.getCredentialsRef() != null && !dto.getCredentialsRef().isBlank()) {
            entity.setCredentialsRef(secretProtector.protect(dto.getCredentialsRef()));
        }
        if (dto.getEnabled() != null) {
            entity.setEnabled(dto.getEnabled());
        }
        if (dto.getIsDefault() != null) {
            entity.setDefault(dto.getIsDefault());
        }
        entity.setUpdatedAt(Instant.now());

        TenantDatasource updated = repository.save(entity);
        return TenantDatasourceResponseDto.fromEntity(updated);
    }

    @Transactional
    public void deleteDatasource(UUID id) {
        if (!repository.existsById(id)) {
            throw new IllegalArgumentException("Datasource not found: " + id);
        }
        repository.deleteById(id);
    }

    public Map<String, Object> testConnection(TenantDatasourceCreateDto dto) {
        Map<String, Object> result = new HashMap<>();

        if (dto.getUrl() == null || dto.getUrl().isBlank()) {
            result.put("status", "UNREACHABLE");
            result.put("message", "URL must not be empty");
            return result;
        }

        try {
            // SSRF validation and DNS resolution check
            ssrfProtectionValidator.validateAndPinUrl(dto.getUrl(), dto.getKind());

            long start = System.currentTimeMillis();

            // Probe endpoint with HTTP GET
            String targetUrl = dto.getUrl();
            if (targetUrl.endsWith("/")) {
                targetUrl = targetUrl.substring(0, targetUrl.length() - 1);
            }
            if ("prometheus".equalsIgnoreCase(dto.getKind()) || "thanos".equalsIgnoreCase(dto.getKind())) {
                targetUrl += "/api/v1/query?query=up";
            } else if ("loki".equalsIgnoreCase(dto.getKind())) {
                targetUrl += "/loki/api/v1/labels";
            }

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(targetUrl))
                    .GET()
                    .timeout(Duration.ofSeconds(3))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            long latencyMs = System.currentTimeMillis() - start;

            if (response.statusCode() >= 200 && response.statusCode() < 400) {
                result.put("status", "REACHABLE");
                result.put("latencyMs", latencyMs);
                result.put("httpStatus", response.statusCode());
                result.put("message", "Successfully connected to datasource endpoint");
            } else {
                result.put("status", "UNREACHABLE");
                result.put("latencyMs", latencyMs);
                result.put("httpStatus", response.statusCode());
                result.put("message", "Endpoint returned HTTP status " + response.statusCode());
            }
        } catch (SecurityException se) {
            result.put("status", "UNREACHABLE");
            result.put("message", se.getMessage());
        } catch (Exception e) {
            result.put("status", "UNREACHABLE");
            result.put("message", "Connection failed: " + e.getMessage());
        }

        return result;
    }
}
