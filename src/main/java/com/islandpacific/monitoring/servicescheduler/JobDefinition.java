package com.islandpacific.monitoring.servicescheduler;

import java.time.DayOfWeek;
import java.time.LocalTime;

public class JobDefinition {

    public final String label;
    public final String server;
    public final String username;
    public final String password;
    public final String serviceName;
    public final String url;
    public final LocalTime stopTime;
    public final LocalTime startTime;
    public final ScheduleType scheduleType;

    /** For WEEKLY — which day of the week */
    public final DayOfWeek weeklyDay;

    /** For MONTHLY_NTH_WEEKDAY — which occurrence (1=first, 2=second, etc.) */
    public final int monthlyNth;

    /** For MONTHLY_NTH_WEEKDAY — which day of the week */
    public final DayOfWeek monthlyWeekday;

    /** Seconds to wait between poll attempts */
    public final int pollIntervalSeconds;

    /** Maximum seconds to wait for the service URL to become available */
    public final int pollTimeoutSeconds;

    public JobDefinition(
            String label,
            String server,
            String username,
            String password,
            String serviceName,
            String url,
            LocalTime stopTime,
            LocalTime startTime,
            ScheduleType scheduleType,
            DayOfWeek weeklyDay,
            int monthlyNth,
            DayOfWeek monthlyWeekday,
            int pollIntervalSeconds,
            int pollTimeoutSeconds) {
        this.label = label;
        this.server = server;
        this.username = username;
        this.password = password;
        this.serviceName = serviceName;
        this.url = url;
        this.stopTime = stopTime;
        this.startTime = startTime;
        this.scheduleType = scheduleType;
        this.weeklyDay = weeklyDay;
        this.monthlyNth = monthlyNth;
        this.monthlyWeekday = monthlyWeekday;
        this.pollIntervalSeconds = pollIntervalSeconds;
        this.pollTimeoutSeconds = pollTimeoutSeconds;
    }
}
