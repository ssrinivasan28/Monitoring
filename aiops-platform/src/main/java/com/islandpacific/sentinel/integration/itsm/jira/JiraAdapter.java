package com.islandpacific.sentinel.integration.itsm.jira;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.islandpacific.sentinel.entity.Incident;
import com.islandpacific.sentinel.integration.itsm.ItsmAdapter;
import com.islandpacific.sentinel.integration.itsm.ItsmStateMapper;
import com.islandpacific.sentinel.integration.itsm.ItsmTicketRef;
import com.islandpacific.sentinel.security.SecretProtector;
import com.islandpacific.sentinel.triage.model.RootCauseResult;
import org.springframework.stereotype.Component;

import java.util.Map;

/** {@link ItsmAdapter} implementation backed by Jira Cloud's REST API v3. */
@Component
public class JiraAdapter implements ItsmAdapter {

    private static final ObjectMapper MAPPER = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    private final JiraClient client;
    private final SecretProtector secretProtector;

    public JiraAdapter(JiraClient client, SecretProtector secretProtector) {
        this.client = client;
        this.secretProtector = secretProtector;
    }

    @Override
    public String kind() {
        return ItsmStateMapper.KIND_JIRA;
    }

    @Override
    public ItsmTicketRef createTicket(String configJson, Incident incident, RootCauseResult rootCause) {
        JiraConfig config = JiraConfig.fromJson(MAPPER, configJson, secretProtector);

        Map<String, Object> result = client.createIssue(config, incident.getTitle(), buildDescription(incident, rootCause));
        String key = String.valueOf(result.get("key"));
        String externalUrl = config.getBaseUrl() + "/browse/" + key;

        // A freshly-created Jira issue always starts in its workflow's initial status (typically
        // "To Do"); the caller reconciles from there on the next status change.
        String initialState = ItsmStateMapper.toItsmState(kind(), "open");
        return new ItsmTicketRef(key, externalUrl, initialState);
    }

    @Override
    public void updateStatus(String configJson, String externalId, String itsmState) {
        JiraConfig config = JiraConfig.fromJson(MAPPER, configJson, secretProtector);
        client.transitionToStatus(config, externalId, itsmState);
    }

    @Override
    public String fetchStatus(String configJson, String externalId) {
        JiraConfig config = JiraConfig.fromJson(MAPPER, configJson, secretProtector);
        return client.fetchStatus(config, externalId);
    }

    private String buildDescription(Incident incident, RootCauseResult rootCause) {
        if (rootCause == null || rootCause.isInsufficient() || rootCause.getRootCauseHypothesis() == null) {
            return "IP Sentinel incident " + incident.getId() + " (severity: " + incident.getSeverity() + ")";
        }
        StringBuilder sb = new StringBuilder();
        sb.append("IP Sentinel incident ").append(incident.getId())
                .append(" (severity: ").append(incident.getSeverity()).append(")\n\n")
                .append("Root cause hypothesis: ").append(rootCause.getRootCauseHypothesis());
        if (rootCause.getConfidence() != null) {
            sb.append(" (confidence: ").append(rootCause.getConfidence()).append(")");
        }
        return sb.toString();
    }
}
