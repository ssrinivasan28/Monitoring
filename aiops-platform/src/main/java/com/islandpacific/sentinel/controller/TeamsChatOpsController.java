package com.islandpacific.sentinel.controller;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.islandpacific.sentinel.assistant.AssistantAgentService;
import com.islandpacific.sentinel.assistant.AssistantAnswer;
import com.islandpacific.sentinel.assistant.AssistantProgressListener;
import com.islandpacific.sentinel.entity.IntegrationConfig;
import com.islandpacific.sentinel.entity.Tenant;
import com.islandpacific.sentinel.entity.User;
import com.islandpacific.sentinel.entity.UserTenantRole;
import com.islandpacific.sentinel.integration.teams.TeamsAuthTokenProvider;
import com.islandpacific.sentinel.integration.teams.TeamsChannelConfig;
import com.islandpacific.sentinel.integration.teams.TeamsChatMessage;
import com.islandpacific.sentinel.integration.teams.TeamsGraphClient;
import com.islandpacific.sentinel.integration.teams.TeamsIntegrationException;
import com.islandpacific.sentinel.integration.teams.TeamsWebhookSignatureVerifier;
import com.islandpacific.sentinel.repository.IntegrationConfigRepository;
import com.islandpacific.sentinel.repository.TenantRepository;
import com.islandpacific.sentinel.repository.UserRepository;
import com.islandpacific.sentinel.repository.UserTenantRoleRepository;
import com.islandpacific.sentinel.security.EntitlementTier;
import com.islandpacific.sentinel.security.SecretProtector;
import com.islandpacific.sentinel.security.TenantContext;
import com.islandpacific.sentinel.security.TenantContextHolder;
import com.islandpacific.sentinel.service.AuthAuditService;
import com.islandpacific.sentinel.service.EntitlementService;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * 2.3 - Microsoft Teams ChatOps entry point: reaches the same 2.1 {@link AssistantAgentService}
 * from a Teams Outgoing Webhook message instead of the 2.2 SPA. Reuses 1.6's per-tenant Teams
 * config/Graph-app plumbing ({@link TeamsChannelConfig}, {@link TeamsAuthTokenProvider},
 * {@link TeamsGraphClient}) both to validate the inbound request and to resolve who sent it.
 * <p>
 * This endpoint is necessarily public ({@code SecurityConfig} permits it) - Teams cannot present an
 * IP Sentinel bearer token - so it authenticates itself: every request must carry a valid
 * {@code Authorization: HMAC <signature>} header computed over the raw body with the tenant's
 * Teams-issued shared secret ({@link TeamsWebhookSignatureVerifier}). Tenant scope is taken solely
 * from the URL path, which only Island Pacific hands out (one URL per tenant, matching how a Teams
 * Outgoing Webhook connector is actually provisioned) - never from anything in the request body.
 * The sender's identity is then independently re-derived from Microsoft Graph (Azure AD object id
 * -&gt; user principal name) and must match a {@link UserTenantRole} already provisioned for this
 * exact tenant; a signature that validates but names an unprovisioned user, or a user provisioned
 * only for a different tenant, gets a polite denial - never the assistant.
 */
@RestController
public class TeamsChatOpsController {

    private static final Logger log = LoggerFactory.getLogger(TeamsChatOpsController.class);
    private static final String KIND = "teams";
    private static final ObjectMapper MAPPER = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    private final TenantRepository tenantRepository;
    private final IntegrationConfigRepository integrationConfigRepository;
    private final UserRepository userRepository;
    private final UserTenantRoleRepository userTenantRoleRepository;
    private final EntitlementService entitlementService;
    private final AuthAuditService authAuditService;
    private final SecretProtector secretProtector;
    private final TeamsAuthTokenProvider tokenProvider;
    private final TeamsGraphClient graphClient;
    private final AssistantAgentService assistantAgentService;

    @Autowired
    public TeamsChatOpsController(TenantRepository tenantRepository,
                                   IntegrationConfigRepository integrationConfigRepository,
                                   UserRepository userRepository,
                                   UserTenantRoleRepository userTenantRoleRepository,
                                   EntitlementService entitlementService,
                                   AuthAuditService authAuditService,
                                   SecretProtector secretProtector,
                                   TeamsAuthTokenProvider tokenProvider,
                                   TeamsGraphClient graphClient,
                                   AssistantAgentService assistantAgentService) {
        this.tenantRepository = tenantRepository;
        this.integrationConfigRepository = integrationConfigRepository;
        this.userRepository = userRepository;
        this.userTenantRoleRepository = userTenantRoleRepository;
        this.entitlementService = entitlementService;
        this.authAuditService = authAuditService;
        this.secretProtector = secretProtector;
        this.tokenProvider = tokenProvider;
        this.graphClient = graphClient;
        this.assistantAgentService = assistantAgentService;
    }

    @PostMapping("/api/v1/integrations/teams/chatops/{tenantId}")
    public ResponseEntity<Map<String, Object>> chatOps(@PathVariable UUID tenantId, HttpServletRequest request) {
        byte[] rawBody;
        try {
            rawBody = request.getInputStream().readAllBytes();
        } catch (IOException e) {
            return errorBody(HttpStatus.BAD_REQUEST, "Could not read the request body");
        }

        Optional<Tenant> tenantOpt = tenantRepository.findById(tenantId);
        if (tenantOpt.isEmpty()) {
            return errorBody(HttpStatus.NOT_FOUND, "Unknown tenant");
        }
        Tenant tenant = tenantOpt.get();

        Optional<IntegrationConfig> configRow = integrationConfigRepository.findByTenantIdAndKind(tenantId, KIND);
        if (configRow.isEmpty() || !configRow.get().isEnabled()) {
            return errorBody(HttpStatus.NOT_FOUND, "Teams integration is not configured for this tenant");
        }

        TeamsChannelConfig channelConfig;
        try {
            channelConfig = TeamsChannelConfig.fromJson(MAPPER, configRow.get().getConfigJson(), secretProtector);
        } catch (IllegalArgumentException e) {
            log.error("Tenant {} has an invalid Teams integration config: {}", tenantId, e.getMessage());
            return errorBody(HttpStatus.NOT_FOUND, "Teams integration is misconfigured for this tenant");
        }
        if (channelConfig.getChatOpsHmacSecret() == null) {
            return errorBody(HttpStatus.NOT_FOUND, "Teams ChatOps is not enabled for this tenant");
        }

        String authHeader = request.getHeader("Authorization");
        if (!TeamsWebhookSignatureVerifier.verify(rawBody, authHeader, channelConfig.getChatOpsHmacSecret())) {
            authAuditService.logEvent("TEAMS_CHATOPS_DENIED", null, tenantId, false, "Invalid webhook signature");
            return errorBody(HttpStatus.UNAUTHORIZED, "Invalid webhook signature");
        }

        TeamsChatMessage message;
        try {
            message = MAPPER.readValue(rawBody, TeamsChatMessage.class);
        } catch (IOException e) {
            return errorBody(HttpStatus.BAD_REQUEST, "Malformed Teams activity payload");
        }

        String question = message.questionText();
        if (question.isBlank()) {
            return chatReply("Please include a question after mentioning me.");
        }
        String aadObjectId = message.aadObjectId();
        if (aadObjectId == null || aadObjectId.isBlank()) {
            return chatReply("I couldn't identify your Teams account. Please contact your administrator.");
        }

        String upn;
        try {
            String accessToken = tokenProvider.getAccessToken(channelConfig);
            upn = graphClient.getUserPrincipalName(channelConfig, accessToken, aadObjectId);
        } catch (TeamsIntegrationException e) {
            log.error("Failed to resolve Teams sender identity for tenant {}: {}", tenantId, e.getMessage());
            return chatReply("I couldn't verify your identity with Microsoft Graph right now. Please try again later.");
        }

        Optional<User> userOpt = userRepository.findByEmail(upn);
        if (userOpt.isEmpty()) {
            authAuditService.logEvent("TEAMS_CHATOPS_DENIED", null, tenantId, false, "No IP Sentinel user for " + upn);
            return chatReply("You're not registered in IP Sentinel yet. Contact your administrator to get access.");
        }
        User user = userOpt.get();

        List<UserTenantRole> roles = userTenantRoleRepository.findByTenantIdAndUserId(tenantId, user.getId());
        if (roles.isEmpty()) {
            authAuditService.logEvent("TEAMS_CHATOPS_DENIED", user.getId(), tenantId,
                    false, "User has no role on this tenant");
            return chatReply("You don't have access to IP Sentinel for this organization.");
        }
        String role = roles.get(0).getRole().getKey();

        EntitlementTier tier = entitlementService.getEntitlementTier(tenantId);
        if (!tier.satisfies(EntitlementTier.PRO)) {
            authAuditService.logEvent("TEAMS_CHATOPS_DENIED", user.getId(), tenantId,
                    false, "Tenant tier " + tier + " does not include the AI Assistant");
            return chatReply("The AI Assistant is a Pro-tier feature. Ask your administrator to upgrade your plan.");
        }

        TenantContextHolder.setContext(new TenantContext(user.getId(), tenantId, tenant.getClientInstanceId(), role));
        try {
            AssistantAnswer answer = assistantAgentService.answer(tenantId, role, question, AssistantProgressListener.NOOP);
            authAuditService.logEvent("TEAMS_CHATOPS_QUERY", user.getId(), tenantId, true, "Answered via Teams ChatOps");
            return chatReply(answer.getAnswer());
        } finally {
            TenantContextHolder.clear();
        }
    }

    private ResponseEntity<Map<String, Object>> chatReply(String text) {
        return ResponseEntity.ok(Map.of("type", "message", "text", text));
    }

    private ResponseEntity<Map<String, Object>> errorBody(HttpStatus status, String message) {
        return ResponseEntity.status(status).body(Map.of(
                "status", status.value(), "error", status.getReasonPhrase(), "message", message));
    }
}
