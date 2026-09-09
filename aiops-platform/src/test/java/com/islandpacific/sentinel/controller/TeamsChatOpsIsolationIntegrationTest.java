package com.islandpacific.sentinel.controller;

import com.islandpacific.sentinel.AbstractIntegrationTest;
import com.islandpacific.sentinel.assistant.AssistantAgentService;
import com.islandpacific.sentinel.assistant.AssistantAnswer;
import com.islandpacific.sentinel.entity.Entitlement;
import com.islandpacific.sentinel.entity.IntegrationConfig;
import com.islandpacific.sentinel.entity.Role;
import com.islandpacific.sentinel.entity.Tenant;
import com.islandpacific.sentinel.entity.User;
import com.islandpacific.sentinel.entity.UserTenantRole;
import com.islandpacific.sentinel.integration.teams.TeamsAuthTokenProvider;
import com.islandpacific.sentinel.integration.teams.TeamsChannelConfig;
import com.islandpacific.sentinel.integration.teams.TeamsGraphClient;
import com.islandpacific.sentinel.repository.EntitlementRepository;
import com.islandpacific.sentinel.repository.IntegrationConfigRepository;
import com.islandpacific.sentinel.repository.RoleRepository;
import com.islandpacific.sentinel.repository.TenantRepository;
import com.islandpacific.sentinel.repository.UserRepository;
import com.islandpacific.sentinel.repository.UserTenantRoleRepository;
import com.islandpacific.sentinel.security.SecretProtector;
import com.islandpacific.sentinel.security.TenantContextHolder;
import com.islandpacific.sentinel.service.AuthAuditService;
import com.islandpacific.sentinel.service.EntitlementService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockHttpServletRequest;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * 2.3's hard acceptance requirement, exercised against real repositories/Postgres rather than
 * mocks: the same Azure-AD-verified person (same {@code aadObjectId}, same resolved UPN) who is
 * genuinely provisioned on Tenant A gets denied - never routed to the assistant - when the request
 * arrives on Tenant B's ChatOps URL, because {@code UserTenantRoleRepository.findByTenantIdAndUserId}
 * finds no row for (Tenant B, that user). Only the Graph identity call and the LLM-backed agent are
 * stubbed; tenant/config/user/role/entitlement resolution all go through the real JPA repositories.
 */
class TeamsChatOpsIsolationIntegrationTest extends AbstractIntegrationTest {

    @Autowired private TenantRepository tenantRepository;
    @Autowired private IntegrationConfigRepository integrationConfigRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private UserTenantRoleRepository userTenantRoleRepository;
    @Autowired private RoleRepository roleRepository;
    @Autowired private EntitlementRepository entitlementRepository;
    @Autowired private EntitlementService entitlementService;
    @Autowired private AuthAuditService authAuditService;
    @Autowired private SecretProtector secretProtector;

    private static final String AAD_OBJECT_ID = "aad-object-shared-identity";
    private static final String UPN = "jane@acme.com";

    @AfterEach
    void tearDown() {
        TenantContextHolder.clear();
    }

    private String webhookSecretFor(String suffix) {
        return Base64.getEncoder().encodeToString(("secret-" + suffix).getBytes(StandardCharsets.UTF_8));
    }

    private Tenant seedTenant(String name, String clientInstanceId, String webhookSecret) {
        Tenant tenant = tenantRepository.save(new Tenant(name, clientInstanceId));
        String json = String.format(
                "{\"aadTenantId\":\"aad-1\",\"clientId\":\"client-1\",\"clientSecret\":\"secret-1\","
                        + "\"teamId\":\"team-1\",\"channelId\":\"chan-1\",\"chatOpsHmacSecret\":\"%s\"}",
                webhookSecret);
        integrationConfigRepository.save(new IntegrationConfig(tenant.getId(), "teams", json, true));
        entitlementRepository.save(new Entitlement(tenant.getId(), "pro"));
        return tenant;
    }

    private String sign(byte[] body, String secretB64) throws Exception {
        byte[] keyBytes = Base64.getDecoder().decode(secretB64);
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(keyBytes, "HmacSHA256"));
        return Base64.getEncoder().encodeToString(mac.doFinal(body));
    }

    private MockHttpServletRequest signedRequest(String bodyJson, String webhookSecret) throws Exception {
        byte[] body = bodyJson.getBytes(StandardCharsets.UTF_8);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setContent(body);
        request.addHeader("Authorization", "HMAC " + sign(body, webhookSecret));
        return request;
    }

    private String activityJson(String text) {
        return "{\"type\":\"message\",\"text\":\"" + text + "\","
                + "\"from\":{\"id\":\"29:xyz\",\"name\":\"Jane\",\"aadObjectId\":\"" + AAD_OBJECT_ID + "\"}}";
    }

    @Test
    void sameVerifiedUser_answeredOnProvisionedTenant_deniedOnUnprovisionedTenant() throws Exception {
        String secretA = webhookSecretFor("a");
        String secretB = webhookSecretFor("b");
        Tenant tenantA = seedTenant("Tenant A", "client-tenant-a-chatops", secretA);
        Tenant tenantB = seedTenant("Tenant B", "client-tenant-b-chatops", secretB);

        User user = userRepository.save(new User(UPN, "Jane", "azure_ad"));
        Role role = roleRepository.findByKey("customer-viewer").orElseGet(() -> roleRepository.save(new Role("customer-viewer")));
        userTenantRoleRepository.save(new UserTenantRole(user, tenantA, role));
        // Deliberately no UserTenantRole row for tenantB - same person, no access there.

        TeamsAuthTokenProvider tokenProvider = mock(TeamsAuthTokenProvider.class);
        TeamsGraphClient graphClient = mock(TeamsGraphClient.class);
        when(tokenProvider.getAccessToken(any(TeamsChannelConfig.class))).thenReturn("graph-token");
        when(graphClient.getUserPrincipalName(any(TeamsChannelConfig.class), anyString(), org.mockito.ArgumentMatchers.eq(AAD_OBJECT_ID)))
                .thenReturn(UPN);

        AssistantAgentService assistantAgentService = mock(AssistantAgentService.class);
        when(assistantAgentService.answer(org.mockito.ArgumentMatchers.eq(tenantA.getId()), anyString(), anyString(), any()))
                .thenReturn(AssistantAnswer.of("Tenant A's answer", java.util.List.of()));

        TeamsChatOpsController controller = new TeamsChatOpsController(
                tenantRepository, integrationConfigRepository, userRepository, userTenantRoleRepository,
                entitlementService, authAuditService, secretProtector, tokenProvider, graphClient, assistantAgentService);

        var responseA = controller.chatOps(tenantA.getId(), signedRequest(activityJson("What's up?"), secretA));
        assertThat(responseA.getStatusCode().value()).isEqualTo(200);
        assertThat(responseA.getBody().get("text")).isEqualTo("Tenant A's answer");

        var responseB = controller.chatOps(tenantB.getId(), signedRequest(activityJson("What's up?"), secretB));
        assertThat(responseB.getStatusCode().value()).isEqualTo(200);
        assertThat((String) responseB.getBody().get("text")).contains("don't have access");

        verify(assistantAgentService).answer(org.mockito.ArgumentMatchers.eq(tenantA.getId()), anyString(), anyString(), any());
        verify(assistantAgentService, org.mockito.Mockito.never())
                .answer(org.mockito.ArgumentMatchers.eq(tenantB.getId()), anyString(), anyString(), any());
        assertThat(TenantContextHolder.getContext()).isEmpty();
    }
}
