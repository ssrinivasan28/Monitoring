package com.islandpacific.sentinel.integration.teams;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.islandpacific.sentinel.security.SecretProtector;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TeamsChannelConfigTest {

    private final ObjectMapper mapper = new ObjectMapper();
    private final SecretProtector secretProtector = new SecretProtector.DefaultSecretProtector();

    @Test
    void fromJson_parsesAllFieldsAndResolvesPlaintextSecret() {
        String json = "{\"aadTenantId\":\"aad-1\",\"clientId\":\"client-1\",\"clientSecret\":\"plain-secret\","
                + "\"teamId\":\"team-1\",\"channelId\":\"chan-1\"}";

        TeamsChannelConfig config = TeamsChannelConfig.fromJson(mapper, json, secretProtector);

        assertThat(config.getAadTenantId()).isEqualTo("aad-1");
        assertThat(config.getClientId()).isEqualTo("client-1");
        assertThat(config.getClientSecret()).isEqualTo("plain-secret");
        assertThat(config.getTeamId()).isEqualTo("team-1");
        assertThat(config.getChannelId()).isEqualTo("chan-1");
    }

    @Test
    void fromJson_resolvesDpapiWrappedSecret() {
        String protectedSecret = secretProtector.protect("super-secret-client-secret");
        String json = String.format(
                "{\"aadTenantId\":\"aad-1\",\"clientId\":\"client-1\",\"clientSecret\":\"%s\",\"teamId\":\"t\",\"channelId\":\"c\"}",
                protectedSecret);

        TeamsChannelConfig config = TeamsChannelConfig.fromJson(mapper, json, secretProtector);

        assertThat(config.getClientSecret()).isEqualTo("super-secret-client-secret");
    }

    @Test
    void fromJson_missingRequiredField_throwsIllegalArgumentException() {
        String json = "{\"aadTenantId\":\"aad-1\",\"clientId\":\"client-1\",\"teamId\":\"t\",\"channelId\":\"c\"}";

        assertThatThrownBy(() -> TeamsChannelConfig.fromJson(mapper, json, secretProtector))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("clientSecret");
    }

    @Test
    void fromJson_blankConfig_throwsIllegalArgumentException() {
        assertThatThrownBy(() -> TeamsChannelConfig.fromJson(mapper, "", secretProtector))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> TeamsChannelConfig.fromJson(mapper, null, secretProtector))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void fromJson_malformedJson_throwsIllegalArgumentException() {
        assertThatThrownBy(() -> TeamsChannelConfig.fromJson(mapper, "{not-json", secretProtector))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
