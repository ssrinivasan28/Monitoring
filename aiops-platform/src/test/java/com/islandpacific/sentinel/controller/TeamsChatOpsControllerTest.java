package com.islandpacific.sentinel.controller;

import com.islandpacific.sentinel.assistant.AssistantAgentService;
import com.islandpacific.sentinel.assistant.AssistantAnswer;
import com.islandpacific.sentinel.assistant.AssistantProgressListener;
import com.islandpacific.sentinel.entity.IntegrationConfig;
import com.islandpacific.sentinel.entity.Role;
import com.islandpacific.sentinel.entity.Tenant;
import com.islandpacific.sentinel.entity.User;
import com.islandpacific.sentinel.entity.UserTenantRole;
import com.islandpacific.sentinel.integration.teams.TeamsAuthTokenProvider;
import com.islandpacific.sentinel.integration.teams.TeamsChannelConfig;
import com.islandpacific.sentinel.integration.teams.TeamsGraphClient;
import com.islandpacific.sentinel.repository.IntegrationConfigRepository;
import com.islandpacific.sentinel.repository.TenantRepository;
import com.islandpacific.sentinel.repository.UserRepository;
import com.islandpacific.sentinel.repository.UserTenantRoleRepository;
import com.islandpacific.sentinel.security.EntitlementTier;
import com.islandpacific.sentinel.security.SecretProtector;
import com.islandpacific.sentinel.security.TenantContextHolder;
import com.islandpacific.sentinel.service.AuthAuditService;
import com.islandpacific.sentinel.service.EntitlementService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * 2.3 Teams ChatOps controller - the two acceptance criteria under test: a Pro user's question is
 * answered with the correct tenant-scoped answer, a Basic user is told it's a Pro feature, and
 * identity/tenant resolution never lets a signature-valid request from an unprovisioned or
 * wrong-tenant user reach the assistant.
 */
class TeamsChatOpsControllerTest {

    private static final String WEBHOOK_SECRET_B64 =
            Base64.getEncoder().encodeToString("teams-webhook-secret".getBytes(StandardCharsets.UTF_8));
    private static final UUID TENANT_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");

    private TenantRepository tenantRepository;
    private IntegrationConfigRepository integrationConfigRepository;
    private UserRepository userRepository;
    private UserTenantRoleRepository userTenantRoleRepository;
    private EntitlementService entitlementService;
    private AuthAuditService authAuditService;
    private TeamsAuthTokenProvider tokenProvider;
    private TeamsGraphClient graphClient;
    private AssistantAgentService assistantAgentService;
    private TeamsChatOpsController controller;

    private final Tenant tenant = new Tenant("Acme Retail", "acme-retail");

    @BeforeEach
    void setUp() {
        tenantRepository = mock(TenantRepository.class);
        integrationConfigRepository = mock(IntegrationConfigRepository.class);
        userRepository = mock(UserRepository.class);
        userTenantRoleRepository = mock(UserTenantRoleRepository.class);
        entitlementService = mock(EntitlementService.class);
        authAuditService = mock(AuthAuditService.class);
        tokenProvider = mock(TeamsAuthTokenProvider.class);
        graphClient = mock(TeamsGraphClient.class);
        assistantAgentService = mock(AssistantAgentService.class);

        controller = new TeamsChatOpsController(
                tenantRepository, integrationConfigRepository, userRepository, userTenantRoleRepository,
                entitlementService, authAuditService, new SecretProtector.DefaultSecretProtector(),
                tokenProvider, graphClient, assistantAgentService);

        when(tenantRepository.findById(TENANT_ID)).thenReturn(Optional.of(tenant));
        when(integrationConfigRepository.findByTenantIdAndKind(TENANT_ID, "teams"))
                .thenReturn(Optional.of(configRow(true)));
        when(tokenProvider.getAccessToken(any(TeamsChannelConfig.class))).thenReturn("graph-token");
        when(graphClient.getUserPrincipalName(any(TeamsChannelConfig.class), eq("graph-token"), eq("aad-object-42")))
                .thenReturn("jane@acme.com");
    }

    @AfterEach
    void tearDown() {
        TenantContextHolder.clear();
    }

    private IntegrationConfig configRow(boolean withChatOpsSecret) {
        String json = "{\"aadTenantId\":\"aad-1\",\"clientId\":\"client-1\",\"clientSecret\":\"secret-1\","
                + "\"teamId\":\"team-1\",\"channelId\":\"chan-1\""
                + (withChatOpsSecret ? ",\"chatOpsHmacSecret\":\"" + WEBHOOK_SECRET_B64 + "\"" : "")
                + "}";
        return new IntegrationConfig(TENANT_ID, "teams", json, true);
    }

    private MockHttpServletRequest signedRequest(String bodyJson) {
        byte[] body = bodyJson.getBytes(StandardCharsets.UTF_8);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setContent(body);
        request.addHeader("Authorization", "HMAC " + sign(body));
        return request;
    }

    private String sign(byte[] body) {
        try {
            byte[] keyBytes = Base64.getDecoder().decode(WEBHOOK_SECRET_B64);
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(keyBytes, "HmacSHA256"));
            return Base64.getEncoder().encodeToString(mac.doFinal(body));
        } catch (Exception e) {
            throw new AssertionError(e);
        }
    }

    private String activityJson(String text) {
        return "{\"type\":\"message\",\"text\":\"" + text + "\","
                + "\"from\":{\"id\":\"29:xyz\",\"name\":\"Jane\",\"aadObjectId\":\"aad-object-42\"}}";
    }

    private void provisionUser(EntitlementTier tier) {
        User user = new User("jane@acme.com", "Jane", "azure_ad");
        user.setId(UUID.randomUUID());
        when(userRepository.findByEmail("jane@acme.com")).thenReturn(Optional.of(user));

        UserTenantRole utr = new UserTenantRole(user, tenant, new Role("customer-viewer"));
        when(userTenantRoleRepository.findByTenantIdAndUserId(TENANT_ID, user.getId())).thenReturn(List.of(utr));
        when(entitlementService.getEntitlementTier(TENANT_ID)).thenReturn(tier);
    }

    @Test
    void proUser_getsCorrectTenantScopedAnswer() {
        provisionUser(EntitlementTier.PRO);
        when(assistantAgentService.answer(eq(TENANT_ID), eq("customer-viewer"), eq("Which jobs are stuck?"), any()))
                .thenReturn(AssistantAnswer.of("JOBABC is stuck on ACME's queue.", List.of()));

        ResponseEntity<Map<String, Object>> response =
                controller.chatOps(TENANT_ID, signedRequest(activityJson("Which jobs are stuck?")));

        assertEquals(200, response.getStatusCode().value());
        assertEquals("JOBABC is stuck on ACME's queue.", response.getBody().get("text"));
        assertEquals("message", response.getBody().get("type"));
        verify(assistantAgentService).answer(eq(TENANT_ID), eq("customer-viewer"), eq("Which jobs are stuck?"), any());
        assertEquals(Optional.empty(), TenantContextHolder.getContext());
    }

    @Test
    void basicTierUser_isToldItsAProFeature_agentNeverInvoked() {
        provisionUser(EntitlementTier.BASIC);

        ResponseEntity<Map<String, Object>> response =
                controller.chatOps(TENANT_ID, signedRequest(activityJson("Which jobs are stuck?")));

        assertEquals(200, response.getStatusCode().value());
        assertEquals(true, ((String) response.getBody().get("text")).contains("Pro-tier feature"));
        verifyNoInteractions(assistantAgentService);
    }

    @Test
    void invalidSignature_returns401_neverResolvesIdentityOrCallsAgent() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setContent(activityJson("hi").getBytes(StandardCharsets.UTF_8));
        request.addHeader("Authorization", "HMAC not-the-right-signature");

        ResponseEntity<Map<String, Object>> response = controller.chatOps(TENANT_ID, request);

        assertEquals(401, response.getStatusCode().value());
        verifyNoInteractions(graphClient);
        verifyNoInteractions(assistantAgentService);
    }

    @Test
    void missingAuthorizationHeader_returns401() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setContent(activityJson("hi").getBytes(StandardCharsets.UTF_8));

        ResponseEntity<Map<String, Object>> response = controller.chatOps(TENANT_ID, request);

        assertEquals(401, response.getStatusCode().value());
        verifyNoInteractions(assistantAgentService);
    }

    @Test
    void tenantWithNoTeamsIntegration_returns404() {
        when(integrationConfigRepository.findByTenantIdAndKind(TENANT_ID, "teams")).thenReturn(Optional.empty());

        ResponseEntity<Map<String, Object>> response =
                controller.chatOps(TENANT_ID, signedRequest(activityJson("hi")));

        assertEquals(404, response.getStatusCode().value());
        verifyNoInteractions(assistantAgentService);
    }

    @Test
    void teamsIntegrationConfiguredButChatOpsSecretMissing_returns404() {
        when(integrationConfigRepository.findByTenantIdAndKind(TENANT_ID, "teams"))
                .thenReturn(Optional.of(configRow(false)));

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setContent(activityJson("hi").getBytes(StandardCharsets.UTF_8));
        request.addHeader("Authorization", "HMAC anything");

        ResponseEntity<Map<String, Object>> response = controller.chatOps(TENANT_ID, request);

        assertEquals(404, response.getStatusCode().value());
        verifyNoInteractions(assistantAgentService);
    }

    @Test
    void unknownTenant_returns404() {
        when(tenantRepository.findById(TENANT_ID)).thenReturn(Optional.empty());

        ResponseEntity<Map<String, Object>> response =
                controller.chatOps(TENANT_ID, signedRequest(activityJson("hi")));

        assertEquals(404, response.getStatusCode().value());
    }

    @Test
    void unregisteredUser_getsFriendlyDenial_agentNeverInvoked() {
        when(userRepository.findByEmail("jane@acme.com")).thenReturn(Optional.empty());

        ResponseEntity<Map<String, Object>> response =
                controller.chatOps(TENANT_ID, signedRequest(activityJson("Which jobs are stuck?")));

        assertEquals(200, response.getStatusCode().value());
        assertEquals(true, ((String) response.getBody().get("text")).contains("not registered"));
        verifyNoInteractions(assistantAgentService);
    }

    /** The tenant-isolation acceptance criterion: signature is valid, user exists, but not for *this* tenant. */
    @Test
    void userNotProvisionedForThisTenant_getsFriendlyDenial_agentNeverInvoked() {
        User user = new User("jane@acme.com", "Jane", "azure_ad");
        user.setId(UUID.randomUUID());
        when(userRepository.findByEmail("jane@acme.com")).thenReturn(Optional.of(user));
        when(userTenantRoleRepository.findByTenantIdAndUserId(TENANT_ID, user.getId())).thenReturn(List.of());

        ResponseEntity<Map<String, Object>> response =
                controller.chatOps(TENANT_ID, signedRequest(activityJson("Which jobs are stuck?")));

        assertEquals(200, response.getStatusCode().value());
        assertEquals(true, ((String) response.getBody().get("text")).contains("don't have access"));
        verifyNoInteractions(assistantAgentService);
        assertEquals(Optional.empty(), TenantContextHolder.getContext());
    }

    @Test
    void blankQuestion_promptsForOne_withoutResolvingIdentityOrCallingAgent() {
        ResponseEntity<Map<String, Object>> response =
                controller.chatOps(TENANT_ID, signedRequest(activityJson("")));

        assertEquals(200, response.getStatusCode().value());
        verifyNoInteractions(graphClient);
        verifyNoInteractions(assistantAgentService);
    }

    @Test
    void tenantContextIsAlwaysClearedEvenIfTheAgentThrows() {
        provisionUser(EntitlementTier.PRO);
        doThrow(new RuntimeException("boom")).when(assistantAgentService)
                .answer(eq(TENANT_ID), eq("customer-viewer"), eq("Which jobs are stuck?"), any());

        assertThrows(RuntimeException.class, () ->
                controller.chatOps(TENANT_ID, signedRequest(activityJson("Which jobs are stuck?"))));

        assertEquals(Optional.empty(), TenantContextHolder.getContext());
    }

}
