package com.islandpacific.sentinel.integration.itsm.jira;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.islandpacific.sentinel.security.SecretProtector;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JiraConfigTest {

    private final ObjectMapper mapper = new ObjectMapper();
    private final SecretProtector secretProtector = new SecretProtector.DefaultSecretProtector();

    @Test
    void fromJson_parsesAllFieldsAndResolvesPlaintextApiToken() {
        String json = "{\"baseUrl\":\"https://acme.atlassian.net/\",\"email\":\"svc@acme.com\","
                + "\"apiToken\":\"plain-token\",\"projectKey\":\"OPS\",\"issueType\":\"Bug\"}";

        JiraConfig config = JiraConfig.fromJson(mapper, json, secretProtector);

        assertThat(config.getBaseUrl()).isEqualTo("https://acme.atlassian.net"); // trailing slash trimmed
        assertThat(config.getEmail()).isEqualTo("svc@acme.com");
        assertThat(config.getApiToken()).isEqualTo("plain-token");
        assertThat(config.getProjectKey()).isEqualTo("OPS");
        assertThat(config.getIssueType()).isEqualTo("Bug");
    }

    @Test
    void fromJson_missingIssueType_defaultsToTask() {
        String json = "{\"baseUrl\":\"https://acme.atlassian.net\",\"email\":\"e\",\"apiToken\":\"t\",\"projectKey\":\"OPS\"}";

        JiraConfig config = JiraConfig.fromJson(mapper, json, secretProtector);

        assertThat(config.getIssueType()).isEqualTo("Task");
    }

    @Test
    void fromJson_resolvesDpapiWrappedApiToken() {
        String protectedToken = secretProtector.protect("super-secret-token");
        String json = String.format(
                "{\"baseUrl\":\"https://acme.atlassian.net\",\"email\":\"e\",\"apiToken\":\"%s\",\"projectKey\":\"OPS\"}",
                protectedToken);

        JiraConfig config = JiraConfig.fromJson(mapper, json, secretProtector);

        assertThat(config.getApiToken()).isEqualTo("super-secret-token");
    }

    @Test
    void fromJson_missingRequiredField_throwsIllegalArgumentException() {
        String json = "{\"baseUrl\":\"https://acme.atlassian.net\",\"email\":\"e\",\"apiToken\":\"t\"}";

        assertThatThrownBy(() -> JiraConfig.fromJson(mapper, json, secretProtector))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("projectKey");
    }

    @Test
    void fromJson_blankConfig_throwsIllegalArgumentException() {
        assertThatThrownBy(() -> JiraConfig.fromJson(mapper, "", secretProtector))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> JiraConfig.fromJson(mapper, null, secretProtector))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void fromJson_malformedJson_throwsIllegalArgumentException() {
        assertThatThrownBy(() -> JiraConfig.fromJson(mapper, "{not-json", secretProtector))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
