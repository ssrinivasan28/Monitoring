package com.islandpacific.sentinel.integration.itsm;

/** Raised on any unrecoverable failure talking to ServiceNow/Jira for ticket sync delivery. */
public class ItsmIntegrationException extends RuntimeException {

    public ItsmIntegrationException(String message) {
        super(message);
    }

    public ItsmIntegrationException(String message, Throwable cause) {
        super(message, cause);
    }
}
