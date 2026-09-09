package com.islandpacific.sentinel.integration.itsm;

/** Result of one sync attempt for a single incident - lets callers (API vs. scheduled sweep) react differently. */
public final class ItsmSyncOutcome {

    public enum Status { SKIPPED_NOT_CONFIGURED, CREATED, UPDATED, PULLED_FROM_ITSM, NO_CHANGE, FAILED }

    private final Status status;
    private final String message;

    private ItsmSyncOutcome(Status status, String message) {
        this.status = status;
        this.message = message;
    }

    public static ItsmSyncOutcome skippedNotConfigured() {
        return new ItsmSyncOutcome(Status.SKIPPED_NOT_CONFIGURED, "No enabled ServiceNow/Jira integration for this tenant");
    }

    public static ItsmSyncOutcome created() {
        return new ItsmSyncOutcome(Status.CREATED, "Ticket created");
    }

    public static ItsmSyncOutcome updated() {
        return new ItsmSyncOutcome(Status.UPDATED, "Ticket updated");
    }

    public static ItsmSyncOutcome pulledFromItsm() {
        return new ItsmSyncOutcome(Status.PULLED_FROM_ITSM, "Incident updated from ITSM ticket status");
    }

    public static ItsmSyncOutcome noChange() {
        return new ItsmSyncOutcome(Status.NO_CHANGE, "Already in sync");
    }

    public static ItsmSyncOutcome failed(String message) {
        return new ItsmSyncOutcome(Status.FAILED, message);
    }

    public Status getStatus() { return status; }
    public String getMessage() { return message; }
}
