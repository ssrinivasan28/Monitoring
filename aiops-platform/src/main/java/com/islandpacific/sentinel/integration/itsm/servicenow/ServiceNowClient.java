package com.islandpacific.sentinel.integration.itsm.servicenow;

import com.islandpacific.sentinel.integration.itsm.ItsmIntegrationException;
import com.islandpacific.sentinel.integration.itsm.ItsmSyncProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

/** Talks to ServiceNow's Table API (`/api/now/table/{table}`) to create/update/read incident records. */
@Component
public class ServiceNowClient {

    private final RestTemplate restTemplate;

    @Autowired
    public ServiceNowClient(ItsmSyncProperties properties) {
        this(createRestTemplate(properties.getTimeoutMs()));
    }

    ServiceNowClient(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    private static RestTemplate createRestTemplate(int timeoutMs) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(timeoutMs > 0 ? timeoutMs : 10_000);
        factory.setReadTimeout(timeoutMs > 0 ? timeoutMs : 10_000);
        return new RestTemplate(factory);
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> createRecord(ServiceNowConfig config, Map<String, Object> fields) {
        String url = config.getInstanceUrl() + "/api/now/table/" + config.getTableName();
        try {
            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.POST, entity(fields, config), Map.class);
            Map<String, Object> result = extractResult(response);
            if (result == null || result.get("sys_id") == null) {
                throw new ItsmIntegrationException("ServiceNow did not return a sys_id for the created record");
            }
            return result;
        } catch (RestClientException e) {
            throw new ItsmIntegrationException("ServiceNow create record failed: " + e.getMessage(), e);
        }
    }

    public void updateState(ServiceNowConfig config, String sysId, String state) {
        String url = config.getInstanceUrl() + "/api/now/table/" + config.getTableName() + "/" + sysId;
        try {
            restTemplate.exchange(url, HttpMethod.PATCH, entity(Map.of("state", state), config), Void.class);
        } catch (RestClientException e) {
            throw new ItsmIntegrationException("ServiceNow update record failed: " + e.getMessage(), e);
        }
    }

    @SuppressWarnings("unchecked")
    public String fetchState(ServiceNowConfig config, String sysId) {
        String url = config.getInstanceUrl() + "/api/now/table/" + config.getTableName() + "/" + sysId + "?sysparm_fields=state";
        try {
            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.GET, entity(null, config), Map.class);
            Map<String, Object> result = extractResult(response);
            Object state = result != null ? result.get("state") : null;
            return state != null ? String.valueOf(state) : null;
        } catch (RestClientException e) {
            throw new ItsmIntegrationException("ServiceNow fetch record failed: " + e.getMessage(), e);
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> extractResult(ResponseEntity<Map> response) {
        Map<String, Object> body = response.getBody();
        return body != null ? (Map<String, Object>) body.get("result") : null;
    }

    private <T> HttpEntity<T> entity(T body, ServiceNowConfig config) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBasicAuth(config.getUsername(), config.getPassword());
        return new HttpEntity<>(body, headers);
    }
}
