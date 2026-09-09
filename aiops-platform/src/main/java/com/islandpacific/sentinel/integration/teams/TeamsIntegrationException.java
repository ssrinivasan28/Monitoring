package com.islandpacific.sentinel.integration.teams;

/** Raised on any unrecoverable failure talking to Azure AD / Microsoft Graph for Teams delivery. */
public class TeamsIntegrationException extends RuntimeException {

    public TeamsIntegrationException(String message) {
        super(message);
    }

    public TeamsIntegrationException(String message, Throwable cause) {
        super(message, cause);
    }
}
