package com.islandpacific.sentinel.integration.itsm.jira;

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

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class JiraClientTest {

    private RestTemplate restTemplate;
    private JiraClient client;

    private final JiraConfig config = JiraConfig.fromJson(
            new ObjectMapper(),
            "{\"baseUrl\":\"https://acme.atlassian.net\",\"email\":\"svc@acme.com\",\"apiToken\":\"token\",\"projectKey\":\"OPS\"}",
            null);

    @BeforeEach
    void setUp() {
        restTemplate = mock(RestTemplate.class);
        client = new JiraClient(restTemplate);
    }

    @Test
    @SuppressWarnings("unchecked")
    void createIssue_success_returnsKey() {
        when(restTemplate.exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class), eq(Map.class)))
                .thenReturn(ResponseEntity.ok(Map.of("id", "10001", "key", "OPS-123", "self", "https://acme.atlassian.net/rest/api/3/issue/10001")));

        Map<String, Object> result = client.createIssue(config, "Disk pressure", "Details here");

        assertThat(result.get("key")).isEqualTo("OPS-123");
    }

    @Test
    @SuppressWarnings("unchecked")
    void createIssue_usesProjectKeyAndBasicAuth() {
        when(restTemplate.exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class), eq(Map.class)))
                .thenReturn(ResponseEntity.ok(Map.of("key", "OPS-123")));

        client.createIssue(config, "Disk pressure", "Details here");

        ArgumentCaptor<HttpEntity> entityCaptor = ArgumentCaptor.forClass(HttpEntity.class);
        verify(restTemplate).exchange(anyString(), eq(HttpMethod.POST), entityCaptor.capture(), eq(Map.class));

        @SuppressWarnings("unchecked")
        Map<String, Object> body = (Map<String, Object>) entityCaptor.getValue().getBody();
        @SuppressWarnings("unchecked")
        Map<String, Object> fields = (Map<String, Object>) body.get("fields");
        @SuppressWarnings("unchecked")
        Map<String, Object> project = (Map<String, Object>) fields.get("project");
        assertThat(project.get("key")).isEqualTo("OPS");

        HttpHeaders headers = entityCaptor.getValue().getHeaders();
        assertThat(headers.getFirst(HttpHeaders.AUTHORIZATION)).startsWith("Basic ");
    }

    @Test
    @SuppressWarnings("unchecked")
    void createIssue_missingKeyInResponse_throwsItsmIntegrationException() {
        when(restTemplate.exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class), eq(Map.class)))
                .thenReturn(ResponseEntity.ok(Map.of("id", "10001")));

        assertThatThrownBy(() -> client.createIssue(config, "Disk pressure", "Details here"))
                .isInstanceOf(ItsmIntegrationException.class);
    }

    @Test
    @SuppressWarnings("unchecked")
    void createIssue_restClientException_wrapsAsItsmIntegrationException() {
        when(restTemplate.exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class), eq(Map.class)))
                .thenThrow(new RestClientException("connection refused"));

        assertThatThrownBy(() -> client.createIssue(config, "Disk pressure", "Details here"))
                .isInstanceOf(ItsmIntegrationException.class);
    }

    @Test
    @SuppressWarnings("unchecked")
    void fetchStatus_success_returnsStatusName() {
        when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), any(HttpEntity.class), eq(Map.class)))
                .thenReturn(ResponseEntity.ok(Map.of("fields", Map.of("status", Map.of("name", "In Progress")))));

        String status = client.fetchStatus(config, "OPS-123");

        assertThat(status).isEqualTo("In Progress");
    }

    @Test
    @SuppressWarnings("unchecked")
    void transitionToStatus_findsMatchingTransitionAndPostsIt() {
        when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), any(HttpEntity.class), eq(Map.class)))
                .thenReturn(ResponseEntity.ok(Map.of("transitions", List.of(
                        Map.of("id", "11", "to", Map.of("name", "To Do")),
                        Map.of("id", "21", "to", Map.of("name", "Done"))))));
        when(restTemplate.exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class), eq(Void.class)))
                .thenReturn(ResponseEntity.ok().build());

        client.transitionToStatus(config, "OPS-123", "Done");

        ArgumentCaptor<HttpEntity> entityCaptor = ArgumentCaptor.forClass(HttpEntity.class);
        verify(restTemplate).exchange(anyString(), eq(HttpMethod.POST), entityCaptor.capture(), eq(Void.class));
        @SuppressWarnings("unchecked")
        Map<String, Object> body = (Map<String, Object>) entityCaptor.getValue().getBody();
        @SuppressWarnings("unchecked")
        Map<String, Object> transition = (Map<String, Object>) body.get("transition");
        assertThat(transition.get("id")).isEqualTo("21");
    }

    @Test
    @SuppressWarnings("unchecked")
    void transitionToStatus_noMatchingTransition_throwsItsmIntegrationException() {
        when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), any(HttpEntity.class), eq(Map.class)))
                .thenReturn(ResponseEntity.ok(Map.of("transitions", List.of(
                        Map.of("id", "11", "to", Map.of("name", "To Do"))))));

        assertThatThrownBy(() -> client.transitionToStatus(config, "OPS-123", "Done"))
                .isInstanceOf(ItsmIntegrationException.class);
    }
}
