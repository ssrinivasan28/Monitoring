package com.islandpacific.sentinel.integration.itsm;

import com.islandpacific.sentinel.entity.Incident;
import com.islandpacific.sentinel.triage.model.RootCauseResult;

/**
 * Shared interface for ITSM systems (ServiceNow, Jira) that can be a tenant's ticket
 * system-of-record (1.7). Each implementation owns parsing its own {@code integration_config.config_json}
 * shape and talking to its own REST API; {@link ItsmSyncService} only ever depends on this interface.
 */
public interface ItsmAdapter {

    /** {@code integration_config.kind} value this adapter handles, e.g. "servicenow" or "jira". */
    String kind();

    /** Creates a new ticket for the incident. Never returns null; throws {@link ItsmIntegrationException} on failure. */
    ItsmTicketRef createTicket(String configJson, Incident incident, RootCauseResult rootCause);

    /** Transitions an existing ticket to the given ITSM-vocabulary state (see {@link ItsmStateMapper}). */
    void updateStatus(String configJson, String externalId, String itsmState);

    /** Returns the ticket's current ITSM-vocabulary state, or null if it could not be determined. */
    String fetchStatus(String configJson, String externalId);
}
