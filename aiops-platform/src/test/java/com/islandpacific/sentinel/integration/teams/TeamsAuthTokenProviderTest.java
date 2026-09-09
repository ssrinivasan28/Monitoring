package com.islandpacific.sentinel.integration.teams;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpEntity;
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
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TeamsAuthTokenProviderTest {

    private RestTemplate restTemplate;
    private TeamsAuthTokenProvider provider;

    private final TeamsChannelConfig config = TeamsChannelConfig.fromJson(
            new ObjectMapper(),
            "{\"aadTenantId\":\"aad-1\",\"clientId\":\"client-1\",\"clientSecret\":\"secret-1\",\"teamId\":\"t\",\"channelId\":\"c\"}",
            null);

    private final TeamsChannelConfig otherTenantConfig = TeamsChannelConfig.fromJson(
            new ObjectMapper(),
            "{\"aadTenantId\":\"aad-2\",\"clientId\":\"client-2\",\"clientSecret\":\"secret-2\",\"teamId\":\"t\",\"channelId\":\"c\"}",
            null);

    @BeforeEach
    void setUp() {
        restTemplate = mock(RestTemplate.class);
        provider = new TeamsAuthTokenProvider(restTemplate);
    }

    @Test
    @SuppressWarnings("unchecked")
    void getAccessToken_requestsTokenAndCachesIt() {
        when(restTemplate.exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class), eq(Map.class)))
                .thenReturn(ResponseEntity.ok(Map.of("access_token", "token-1", "expires_in", 3600)));

        String first = provider.getAccessToken(config);
        String second = provider.getAccessToken(config);

        assertThat(first).isEqualTo("token-1");
        assertThat(second).isEqualTo("token-1");
        verify(restTemplate, times(1)).exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class), eq(Map.class));
    }

    @Test
    @SuppressWarnings("unchecked")
    void getAccessToken_differentTenants_fetchIndependently() {
        when(restTemplate.exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class), eq(Map.class)))
                .thenReturn(ResponseEntity.ok(Map.of("access_token", "token-1", "expires_in", 3600)))
                .thenReturn(ResponseEntity.ok(Map.of("access_token", "token-2", "expires_in", 3600)));

        String forFirst = provider.getAccessToken(config);
        String forSecond = provider.getAccessToken(otherTenantConfig);

        assertThat(forFirst).isEqualTo("token-1");
        assertThat(forSecond).isEqualTo("token-2");
        verify(restTemplate, times(2)).exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class), eq(Map.class));
    }

    @Test
    @SuppressWarnings("unchecked")
    void getAccessToken_tokenEndpointFails_throwsTeamsIntegrationException() {
        when(restTemplate.exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class), eq(Map.class)))
                .thenThrow(new RestClientException("connection refused"));

        assertThatThrownBy(() -> provider.getAccessToken(config))
                .isInstanceOf(TeamsIntegrationException.class);
    }

    @Test
    @SuppressWarnings("unchecked")
    void getAccessToken_missingAccessTokenInResponse_throwsTeamsIntegrationException() {
        when(restTemplate.exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class), eq(Map.class)))
                .thenReturn(ResponseEntity.ok(Map.of("expires_in", 3600)));

        assertThatThrownBy(() -> provider.getAccessToken(config))
                .isInstanceOf(TeamsIntegrationException.class);
    }
}
