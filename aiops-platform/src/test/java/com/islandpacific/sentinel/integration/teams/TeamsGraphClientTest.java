package com.islandpacific.sentinel.integration.teams;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TeamsGraphClientTest {

    private RestTemplate restTemplate;
    private TeamsNotificationProperties properties;
    private TeamsGraphClient client;

    private final TeamsChannelConfig config = TeamsChannelConfig.fromJson(
            new ObjectMapper(),
            "{\"aadTenantId\":\"aad-1\",\"clientId\":\"client-1\",\"clientSecret\":\"secret-1\",\"teamId\":\"team-1\",\"channelId\":\"chan-1\"}",
            null);

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        restTemplate = mock(RestTemplate.class);
        properties = new TeamsNotificationProperties();
        properties.setMaxRetries(2);
        properties.setRetryBackoffMs(1); // keep retry tests fast
        client = new TeamsGraphClient(properties, restTemplate);
    }

    private ObjectNode card() {
        return new ObjectMapper().createObjectNode().put("type", "AdaptiveCard");
    }

    @Test
    @SuppressWarnings("unchecked")
    void postCard_success_returnsMessageId() {
        when(restTemplate.exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class), eq(Map.class)))
                .thenReturn(ResponseEntity.ok(Map.of("id", "msg-123")));

        String messageId = client.postCard(config, "test-token", card());

        assertThat(messageId).isEqualTo("msg-123");
    }

    @Test
    @SuppressWarnings("unchecked")
    void postCard_usesTeamAndChannelFromConfigAndBearerToken() {
        when(restTemplate.exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class), eq(Map.class)))
                .thenReturn(ResponseEntity.ok(Map.of("id", "msg-123")));

        client.postCard(config, "test-token", card());

        ArgumentCaptor<String> urlCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<HttpEntity> entityCaptor = ArgumentCaptor.forClass(HttpEntity.class);
        verify(restTemplate).exchange(urlCaptor.capture(), eq(HttpMethod.POST), entityCaptor.capture(), eq(Map.class));

        assertThat(urlCaptor.getValue()).contains("/teams/team-1/channels/chan-1/messages");
        HttpHeaders headers = entityCaptor.getValue().getHeaders();
        assertThat(headers.getFirst(HttpHeaders.AUTHORIZATION)).isEqualTo("Bearer test-token");
    }

    @Test
    @SuppressWarnings("unchecked")
    void postCard_missingIdInResponse_throwsTeamsIntegrationException() {
        when(restTemplate.exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class), eq(Map.class)))
                .thenReturn(ResponseEntity.ok(Map.of()));

        assertThatThrownBy(() -> client.postCard(config, "test-token", card()))
                .isInstanceOf(TeamsIntegrationException.class);
    }

    @Test
    @SuppressWarnings("unchecked")
    void postCard_throttledThenSucceeds_retriesAndReturnsMessageId() {
        HttpHeaders retryHeaders = new HttpHeaders();
        retryHeaders.set("Retry-After", "0");
        HttpClientErrorException throttled = HttpClientErrorException.create(
                HttpStatus.TOO_MANY_REQUESTS, "Too Many Requests", retryHeaders, new byte[0], null);

        when(restTemplate.exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class), eq(Map.class)))
                .thenThrow(throttled)
                .thenReturn(ResponseEntity.ok(Map.of("id", "msg-after-retry")));

        String messageId = client.postCard(config, "test-token", card());

        assertThat(messageId).isEqualTo("msg-after-retry");
        verify(restTemplate, times(2)).exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class), eq(Map.class));
    }

    @Test
    @SuppressWarnings("unchecked")
    void postCard_throttledBeyondMaxRetries_throwsTeamsIntegrationException() {
        HttpClientErrorException throttled = HttpClientErrorException.create(
                HttpStatus.TOO_MANY_REQUESTS, "Too Many Requests", new HttpHeaders(), new byte[0], null);
        when(restTemplate.exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class), eq(Map.class)))
                .thenThrow(throttled);

        assertThatThrownBy(() -> client.postCard(config, "test-token", card()))
                .isInstanceOf(TeamsIntegrationException.class);
        // initial attempt + properties.getMaxRetries() retries, all throttled
        verify(restTemplate, times(1 + properties.getMaxRetries())).exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class), eq(Map.class));
    }

    @Test
    @SuppressWarnings("unchecked")
    void postCard_serverError_throwsTeamsIntegrationExceptionWithoutRetrying() {
        when(restTemplate.exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class), eq(Map.class)))
                .thenThrow(HttpServerErrorException.create(HttpStatus.INTERNAL_SERVER_ERROR, "err", new HttpHeaders(), new byte[0], null));

        assertThatThrownBy(() -> client.postCard(config, "test-token", card()))
                .isInstanceOf(TeamsIntegrationException.class);
        verify(restTemplate, times(1)).exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class), eq(Map.class));
    }

    @Test
    @SuppressWarnings("unchecked")
    void updateCard_success_patchesExistingMessageId() {
        when(restTemplate.exchange(anyString(), eq(HttpMethod.PATCH), any(HttpEntity.class), eq(Void.class)))
                .thenReturn(ResponseEntity.ok().build());

        client.updateCard(config, "test-token", "msg-123", card());

        ArgumentCaptor<String> urlCaptor = ArgumentCaptor.forClass(String.class);
        verify(restTemplate).exchange(urlCaptor.capture(), eq(HttpMethod.PATCH), any(HttpEntity.class), eq(Void.class));
        assertThat(urlCaptor.getValue()).contains("/teams/team-1/channels/chan-1/messages/msg-123");
    }

    @Test
    @SuppressWarnings("unchecked")
    void getUserPrincipalName_success_returnsUpn() {
        when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), any(HttpEntity.class), eq(Map.class)))
                .thenReturn(ResponseEntity.ok(Map.of("userPrincipalName", "jane@acme.com", "mail", "jane@acme.com")));

        String upn = client.getUserPrincipalName(config, "test-token", "aad-object-id-1");

        assertThat(upn).isEqualTo("jane@acme.com");
    }

    @Test
    @SuppressWarnings("unchecked")
    void getUserPrincipalName_usesUsersEndpointAndBearerToken() {
        when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), any(HttpEntity.class), eq(Map.class)))
                .thenReturn(ResponseEntity.ok(Map.of("userPrincipalName", "jane@acme.com")));

        client.getUserPrincipalName(config, "test-token", "aad-object-id-1");

        ArgumentCaptor<String> urlCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<HttpEntity> entityCaptor = ArgumentCaptor.forClass(HttpEntity.class);
        verify(restTemplate).exchange(urlCaptor.capture(), eq(HttpMethod.GET), entityCaptor.capture(), eq(Map.class));

        assertThat(urlCaptor.getValue()).contains("/users/aad-object-id-1");
        assertThat(entityCaptor.getValue().getHeaders().getFirst(HttpHeaders.AUTHORIZATION)).isEqualTo("Bearer test-token");
    }

    @Test
    @SuppressWarnings("unchecked")
    void getUserPrincipalName_fallsBackToMailWhenUpnMissing() {
        when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), any(HttpEntity.class), eq(Map.class)))
                .thenReturn(ResponseEntity.ok(Map.of("mail", "jane@acme.com")));

        assertThat(client.getUserPrincipalName(config, "test-token", "aad-object-id-1")).isEqualTo("jane@acme.com");
    }

    @Test
    @SuppressWarnings("unchecked")
    void getUserPrincipalName_missingBothFields_throwsTeamsIntegrationException() {
        when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), any(HttpEntity.class), eq(Map.class)))
                .thenReturn(ResponseEntity.ok(Map.of()));

        assertThatThrownBy(() -> client.getUserPrincipalName(config, "test-token", "aad-object-id-1"))
                .isInstanceOf(TeamsIntegrationException.class);
    }

    @Test
    @SuppressWarnings("unchecked")
    void getUserPrincipalName_userNotFound_throwsTeamsIntegrationException() {
        when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), any(HttpEntity.class), eq(Map.class)))
                .thenThrow(HttpClientErrorException.create(HttpStatus.NOT_FOUND, "Not Found", new HttpHeaders(), new byte[0], null));

        assertThatThrownBy(() -> client.getUserPrincipalName(config, "test-token", "unknown-aad-id"))
                .isInstanceOf(TeamsIntegrationException.class);
    }
}
