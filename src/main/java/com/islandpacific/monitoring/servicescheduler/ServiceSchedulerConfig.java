package com.islandpacific.monitoring.servicescheduler;

import java.io.FileInputStream;
import java.io.IOException;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public class ServiceSchedulerConfig {

    public final String screenshotFolder;
    public final List<JobDefinition> jobs;

    private ServiceSchedulerConfig(String screenshotFolder, List<JobDefinition> jobs) {
        this.screenshotFolder = screenshotFolder;
        this.jobs = jobs;
    }

    public static ServiceSchedulerConfig load(String propertiesPath) throws IOException {
        Properties props = new Properties();
        try (FileInputStream fis = new FileInputStream(propertiesPath)) {
            props.load(fis);
        }
        return parse(props);
    }

    public static ServiceSchedulerConfig loadFromProperties(Properties props) {
        return parse(props);
    }

    private static ServiceSchedulerConfig parse(Properties props) {
        String screenshotFolder = props.getProperty("screenshot.folder", "screenshots");

        // Collect all defined job indices (handles gaps like job.1, job.3 with no job.2)
        java.util.Set<Integer> indices = new java.util.TreeSet<>();
        for (String key : props.stringPropertyNames()) {
            if (key.matches("job\\.\\d+\\.label")) {
                indices.add(Integer.parseInt(key.split("\\.")[1]));
            }
        }

        List<JobDefinition> jobs = new ArrayList<>();
        for (int index : indices) {
            String prefix = "job." + index + ".";
            String label = props.getProperty(prefix + "label");

            String server = require(props, prefix + "server", label);
            String username = require(props, prefix + "username", label);
            String password = require(props, prefix + "password", label);
            String serviceName = require(props, prefix + "service.name", label);
            String url = require(props, prefix + "url", label);
            LocalTime stopTime = parseTime(require(props, prefix + "stop.time", label), prefix + "stop.time");
            LocalTime startTime = parseTime(require(props, prefix + "start.time", label), prefix + "start.time");

            String scheduleStr = require(props, prefix + "schedule", label).toUpperCase().replace("-", "_");
            ScheduleType scheduleType;
            try {
                scheduleType = ScheduleType.valueOf(scheduleStr);
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Invalid schedule '" + scheduleStr + "' for job '" + label
                        + "'. Valid values: DAILY, DAILY_EXCEPT_SATURDAY, DAILY_EXCEPT_SUNDAY, WEEKLY, MONTHLY_NTH_WEEKDAY");
            }

            DayOfWeek weeklyDay = null;
            if (scheduleType == ScheduleType.WEEKLY) {
                weeklyDay = DayOfWeek.valueOf(
                        require(props, prefix + "schedule.day", label).toUpperCase());
            }

            int monthlyNth = 0;
            DayOfWeek monthlyWeekday = null;
            if (scheduleType == ScheduleType.MONTHLY_NTH_WEEKDAY) {
                monthlyNth = Integer.parseInt(require(props, prefix + "schedule.nth", label));
                if (monthlyNth < 1 || monthlyNth > 4) {
                    throw new IllegalArgumentException("'" + prefix + "schedule.nth' must be 1–4 for job '" + label + "', got: " + monthlyNth);
                }
                monthlyWeekday = DayOfWeek.valueOf(
                        require(props, prefix + "schedule.weekday", label).toUpperCase());
            }

            if (startTime.equals(stopTime)) {
                throw new IllegalArgumentException("start.time and stop.time cannot be the same for job '" + label + "'");
            }

            int pollInterval = Integer.parseInt(props.getProperty(prefix + "poll.interval.seconds", "10"));
            int pollTimeout = Integer.parseInt(props.getProperty(prefix + "poll.timeout.seconds", "120"));

            jobs.add(new JobDefinition(
                    label, server, username, password,
                    serviceName, url, stopTime, startTime,
                    scheduleType, weeklyDay, monthlyNth, monthlyWeekday,
                    pollInterval, pollTimeout));
        }

        if (jobs.isEmpty()) {
            throw new IllegalArgumentException("No jobs defined in servicescheduler.properties (expected job.1.label, etc.)");
        }

        return new ServiceSchedulerConfig(screenshotFolder, jobs);
    }

    private static String require(Properties props, String key, String jobLabel) {
        String value = props.getProperty(key);
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("Missing required property '" + key + "' for job '" + jobLabel + "'");
        }
        return value.trim();
    }

    private static LocalTime parseTime(String value, String key) {
        try {
            return LocalTime.parse(value);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("Invalid time format for '" + key + "': '" + value + "' (expected HH:mm or HH:mm:ss)");
        }
    }
}
