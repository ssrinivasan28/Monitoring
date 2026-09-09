package com.islandpacific.sentinel.integration.itsm;

import java.util.Map;

/**
 * The 1.5 in-app status ↔ ITSM state mapping referenced by 1.7: maps the four
 * {@code incidents.status} values (open/ack/assigned/resolved) onto each ITSM system's own
 * vocabulary and back. The mapping is intentionally lossy in the ITSM→local direction (ServiceNow's
 * "In Progress" and Jira's "In Progress" both fold ack and assigned together) since neither system
 * distinguishes "acknowledged" from "assigned" the way this platform does.
 */
public final class ItsmStateMapper {

    private ItsmStateMapper() {}

    public static final String KIND_SERVICENOW = "servicenow";
    public static final String KIND_JIRA = "jira";

    // ServiceNow incident table `state` field: 1=New, 2=In Progress, 6=Resolved, 7=Closed.
    private static final Map<String, String> SERVICENOW_FROM_LOCAL = Map.of(
            "open", "1",
            "ack", "2",
            "assigned", "2",
            "resolved", "6");
    private static final Map<String, String> SERVICENOW_TO_LOCAL = Map.of(
            "1", "open",
            "2", "ack",
            "6", "resolved",
            "7", "resolved");

    // Jira's default software/business workflow statuses.
    private static final Map<String, String> JIRA_FROM_LOCAL = Map.of(
            "open", "To Do",
            "ack", "In Progress",
            "assigned", "In Progress",
            "resolved", "Done");
    private static final Map<String, String> JIRA_TO_LOCAL = Map.of(
            "To Do", "open",
            "In Progress", "ack",
            "Done", "resolved");

    /** Maps an in-app {@code incidents.status} value to the target ITSM system's state vocabulary. */
    public static String toItsmState(String kind, String localStatus) {
        String mapped = fromLocalMap(kind).get(localStatus);
        if (mapped == null) {
            throw new IllegalArgumentException("No " + kind + " state mapping for local status: " + localStatus);
        }
        return mapped;
    }

    /** Maps an ITSM state back to an in-app {@code incidents.status} value; defaults to "open" if unrecognized. */
    public static String toLocalStatus(String kind, String itsmState) {
        return toLocalMap(kind).getOrDefault(itsmState, "open");
    }

    private static Map<String, String> fromLocalMap(String kind) {
        return switch (kind) {
            case KIND_SERVICENOW -> SERVICENOW_FROM_LOCAL;
            case KIND_JIRA -> JIRA_FROM_LOCAL;
            default -> throw new IllegalArgumentException("Unsupported ITSM kind: " + kind);
        };
    }

    private static Map<String, String> toLocalMap(String kind) {
        return switch (kind) {
            case KIND_SERVICENOW -> SERVICENOW_TO_LOCAL;
            case KIND_JIRA -> JIRA_TO_LOCAL;
            default -> throw new IllegalArgumentException("Unsupported ITSM kind: " + kind);
        };
    }
}
