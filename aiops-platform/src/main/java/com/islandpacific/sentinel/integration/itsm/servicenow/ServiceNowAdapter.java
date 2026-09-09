package com.islandpacific.sentinel.integration.itsm.servicenow;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.islandpacific.sentinel.entity.Incident;
import com.islandpacific.sentinel.integration.itsm.ItsmAdapter;
import com.islandpacific.sentinel.integration.itsm.ItsmStateMapper;
import com.islandpacific.sentinel.integration.itsm.ItsmTicketRef;
import com.islandpacific.sentinel.security.SecretProtector;
import com.islandpacific.sentinel.triage.model.RootCauseResult;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

/** {@link ItsmAdapter} implementation backed by ServiceNow's Table API. */
@Component
public class ServiceNowAdapter implements ItsmAdapter {

    private static final ObjectMapper MAPPER = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    private final ServiceNowClient client;
    private final SecretProtector secretProtector;

    public ServiceNowAdapter(ServiceNowClient client, SecretProtector secretProtector) {
        this.client = client;
        this.secretProtector = secretProtector;
    }

    @Override
    public String kind() {
        return ItsmStateMapper.KIND_SERVICENOW;
    }

    @Override
    public ItsmTicketRef createTicket(String configJson, Incident incident, RootCauseResult rootCause) {
        ServiceNowConfig config = ServiceNowConfig.fromJson(MAPPER, configJson, secretProtector);
        String initialState = ItsmStateMapper.toItsmState(kind(), incident.getStatus());

        Map<String, Object> fields = new LinkedHashMap<>();
        fields.put("short_description", incident.getTitle());
        fields.put("description", buildDescription(incident, rootCause));
        fields.put("urgency", severityToUrgency(incident.getSeverity()));
        fields.put("correlation_id", incident.getId().toString());
        fields.put("state", initialState);

        Map<String, Object> result = client.createRecord(config, fields);
        String sysId = String.valueOf(result.get("sys_id"));
        String externalUrl = config.getInstanceUrl() + "/nav_to.do?uri=" + config.getTableName() + ".do?sys_id=" + sysId;
        return new ItsmTicketRef(sysId, externalUrl, initialState);
    }

    @Override
    public void updateStatus(String configJson, String externalId, String itsmState) {
        ServiceNowConfig config = ServiceNowConfig.fromJson(MAPPER, configJson, secretProtector);
        client.updateState(config, externalId, itsmState);
    }

    @Override
    public String fetchStatus(String configJson, String externalId) {
        ServiceNowConfig config = ServiceNowConfig.fromJson(MAPPER, configJson, secretProtector);
        return client.fetchState(config, externalId);
    }

    private String severityToUrgency(String severity) {
        if (severity == null) {
            return "3";
        }
        return switch (severity.toLowerCase()) {
            case "critical", "high" -> "1";
            case "medium" -> "2";
            default -> "3";
        };
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
