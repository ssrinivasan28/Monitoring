package com.islandpacific.sentinel.integration.itsm.servicenow;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.islandpacific.sentinel.integration.itsm.ItsmIntegrationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ServiceNowClientTest {

    private RestTemplate restTemplate;
    private ServiceNowClient client;

    private final ServiceNowConfig config = ServiceNowConfig.fromJson(
            new ObjectMapper(),
            "{\"instanceUrl\":\"https://acme.service-now.com\",\"username\":\"svc-user\",\"password\":\"secret\",\"tableName\":\"incident\"}",
            null);

    @BeforeEach
    void setUp() {
        restTemplate = mock(RestTemplate.class);
        client = new ServiceNowClient(restTemplate);
    }

    @Test
    @SuppressWarnings("unchecked")
    void createRecord_success_returnsResultMap() {
        when(restTemplate.exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class), eq(Map.class)))
                .thenReturn(ResponseEntity.ok(Map.of("result", Map.of("sys_id", "abc123", "number", "INC0001"))));

        Map<String, Object> result = client.createRecord(config, Map.of("short_description", "Disk pressure"));

        assertThat(result.get("sys_id")).isEqualTo("abc123");
    }

    @Test
    @SuppressWarnings("unchecked")
    void createRecord_usesTableUrlAndBasicAuth() {
        when(restTemplate.exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class), eq(Map.class)))
                .thenReturn(ResponseEntity.ok(Map.of("result", Map.of("sys_id", "abc123"))));

        client.createRecord(config, Map.of("short_description", "Disk pressure"));

        ArgumentCaptor<String> urlCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<HttpEntity> entityCaptor = ArgumentCaptor.forClass(HttpEntity.class);
        verify(restTemplate).exchange(urlCaptor.capture(), eq(HttpMethod.POST), entityCaptor.capture(), eq(Map.class));

        assertThat(urlCaptor.getValue()).isEqualTo("https://acme.service-now.com/api/now/table/incident");
        HttpHeaders headers = entityCaptor.getValue().getHeaders();
        assertThat(headers.getFirst(HttpHeaders.AUTHORIZATION)).startsWith("Basic ");
    }

    @Test
    @SuppressWarnings("unchecked")
    void createRecord_missingSysIdInResponse_throwsItsmIntegrationException() {
        when(restTemplate.exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class), eq(Map.class)))
                .thenReturn(ResponseEntity.ok(Map.of("result", Map.of())));

        assertThatThrownBy(() -> client.createRecord(config, Map.of()))
                .isInstanceOf(ItsmIntegrationException.class);
    }

    @Test
    @SuppressWarnings("unchecked")
    void createRecord_restClientException_wrapsAsItsmIntegrationException() {
        when(restTemplate.exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class), eq(Map.class)))
                .thenThrow(new RestClientException("connection refused"));

        assertThatThrownBy(() -> client.createRecord(config, Map.of()))
                .isInstanceOf(ItsmIntegrationException.class);
    }

    @Test
    @SuppressWarnings("unchecked")
    void updateState_success_patchesRecordUrl() {
        when(restTemplate.exchange(anyString(), eq(HttpMethod.PATCH), any(HttpEntity.class), eq(Void.class)))
                .thenReturn(ResponseEntity.ok().build());

        client.updateState(config, "abc123", "6");

        ArgumentCaptor<String> urlCaptor = ArgumentCaptor.forClass(String.class);
        verify(restTemplate).exchange(urlCaptor.capture(), eq(HttpMethod.PATCH), any(HttpEntity.class), eq(Void.class));
        assertThat(urlCaptor.getValue()).isEqualTo("https://acme.service-now.com/api/now/table/incident/abc123");
    }

    @Test
    @SuppressWarnings("unchecked")
    void fetchState_success_returnsStateField() {
        when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), any(HttpEntity.class), eq(Map.class)))
                .thenReturn(ResponseEntity.ok(Map.of("result", Map.of("state", "6"))));

        String state = client.fetchState(config, "abc123");

        assertThat(state).isEqualTo("6");
    }

    @Test
    @SuppressWarnings("unchecked")
    void fetchState_noResult_returnsNull() {
        when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), any(HttpEntity.class), eq(Map.class)))
                .thenReturn(ResponseEntity.ok(Map.of()));

        assertThat(client.fetchState(config, "abc123")).isNull();
    }
}
