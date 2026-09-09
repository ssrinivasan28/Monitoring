package com.islandpacific.sentinel.integration.itsm.servicenow;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.islandpacific.sentinel.security.SecretProtector;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ServiceNowConfigTest {

    private final ObjectMapper mapper = new ObjectMapper();
    private final SecretProtector secretProtector = new SecretProtector.DefaultSecretProtector();

    @Test
    void fromJson_parsesAllFieldsAndResolvesPlaintextPassword() {
        String json = "{\"instanceUrl\":\"https://acme.service-now.com/\",\"username\":\"svc-user\","
                + "\"password\":\"plain-pass\",\"tableName\":\"incident\"}";

        ServiceNowConfig config = ServiceNowConfig.fromJson(mapper, json, secretProtector);

        assertThat(config.getInstanceUrl()).isEqualTo("https://acme.service-now.com"); // trailing slash trimmed
        assertThat(config.getUsername()).isEqualTo("svc-user");
        assertThat(config.getPassword()).isEqualTo("plain-pass");
        assertThat(config.getTableName()).isEqualTo("incident");
    }

    @Test
    void fromJson_missingTableName_defaultsToIncident() {
        String json = "{\"instanceUrl\":\"https://acme.service-now.com\",\"username\":\"u\",\"password\":\"p\"}";

        ServiceNowConfig config = ServiceNowConfig.fromJson(mapper, json, secretProtector);

        assertThat(config.getTableName()).isEqualTo("incident");
    }

    @Test
    void fromJson_resolvesDpapiWrappedPassword() {
        String protectedPassword = secretProtector.protect("super-secret-password");
        String json = String.format(
                "{\"instanceUrl\":\"https://acme.service-now.com\",\"username\":\"u\",\"password\":\"%s\"}",
                protectedPassword);

        ServiceNowConfig config = ServiceNowConfig.fromJson(mapper, json, secretProtector);

        assertThat(config.getPassword()).isEqualTo("super-secret-password");
    }

    @Test
    void fromJson_missingRequiredField_throwsIllegalArgumentException() {
        String json = "{\"instanceUrl\":\"https://acme.service-now.com\",\"username\":\"u\"}";

        assertThatThrownBy(() -> ServiceNowConfig.fromJson(mapper, json, secretProtector))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("password");
    }

    @Test
    void fromJson_blankConfig_throwsIllegalArgumentException() {
        assertThatThrownBy(() -> ServiceNowConfig.fromJson(mapper, "", secretProtector))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> ServiceNowConfig.fromJson(mapper, null, secretProtector))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void fromJson_malformedJson_throwsIllegalArgumentException() {
        assertThatThrownBy(() -> ServiceNowConfig.fromJson(mapper, "{not-json", secretProtector))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
