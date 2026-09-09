package com.islandpacific.sentinel.integration.teams;

/** Result of one {@code pushIncidentCard} attempt - lets callers (API vs. scheduled sweep) react differently. */
public final class TeamsSyncOutcome {

    public enum Status { SKIPPED_NOT_CONFIGURED, CREATED, UPDATED, FAILED }

    private final Status status;
    private final String message;

    private TeamsSyncOutcome(Status status, String message) {
        this.status = status;
        this.message = message;
    }

    public static TeamsSyncOutcome skippedNotConfigured() {
        return new TeamsSyncOutcome(Status.SKIPPED_NOT_CONFIGURED, "Teams integration is not configured or is disabled for this tenant");
    }

    public static TeamsSyncOutcome created() {
        return new TeamsSyncOutcome(Status.CREATED, "Card posted");
    }

    public static TeamsSyncOutcome updated() {
        return new TeamsSyncOutcome(Status.UPDATED, "Card updated");
    }

    public static TeamsSyncOutcome failed(String message) {
        return new TeamsSyncOutcome(Status.FAILED, message);
    }

    public Status getStatus() { return status; }
    public String getMessage() { return message; }
}
