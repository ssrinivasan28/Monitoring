package com.islandpacific.monitoring.servicescheduler;

import java.nio.file.Path;
import java.time.LocalDate;
import java.time.DayOfWeek;
import java.time.temporal.TemporalAdjusters;
import java.util.Properties;
import java.util.logging.Logger;
import com.islandpacific.monitoring.common.AppLogger;

public class JobRunner implements Runnable {

    private static final Logger logger = AppLogger.getLogger();

    private final JobDefinition job;
    private final ScreenshotService screenshotService;
    private final EmailService emailService;

    public JobRunner(JobDefinition job, ScreenshotService screenshotService, Properties emailProps) {
        this.job = job;
        this.screenshotService = screenshotService;
        this.emailService = new EmailService(emailProps);
    }

    @Override
    public void run() {
        if (!shouldRunToday()) {
            logger.fine("[" + job.label + "] Skipping — not scheduled today (" + LocalDate.now().getDayOfWeek() + ")");
            return;
        }

        logger.info("[" + job.label + "] Starting job run");

        boolean serviceRestarted = false;
        boolean urlReady = false;
        Path screenshot = null;

        try {
            // Stop service
            PowerShellRunner.Result stopResult = PowerShellRunner.stopService(
                    job.server, job.username, job.password, job.serviceName);
            if (!stopResult.success()) {
                logger.warning("[" + job.label + "] Stop service failed (exit=" + stopResult.exitCode + "): " + stopResult.output);
                emailService.sendServiceStopped(job.label, job.serviceName, job.server, false);
                // Abort — do not attempt restart if stop failed
                emailService.sendJobReport(job.label, job.serviceName, job.url, false, false, null);
                return;
            }
            logger.info("[" + job.label + "] Service stopped successfully");
            emailService.sendServiceStopped(job.label, job.serviceName, job.server, true);

            // Wait until start time
            waitUntilStartTime();

            // Start service
            PowerShellRunner.Result startResult = PowerShellRunner.startService(
                    job.server, job.username, job.password, job.serviceName);
            if (!startResult.success()) {
                logger.warning("[" + job.label + "] Start service failed (exit=" + startResult.exitCode + "): " + startResult.output);
            } else {
                serviceRestarted = true;
                logger.info("[" + job.label + "] Service started successfully");
            }

            // Poll URL
            urlReady = UrlPoller.waitForReady(job.url, job.pollIntervalSeconds, job.pollTimeoutSeconds);

            // Screenshot
            if (urlReady) {
                screenshot = screenshotService.capture(job.label, job.url);
            }

        } catch (Exception e) {
            logger.severe("[" + job.label + "] Job execution error: " + e.getMessage());
        }

        // Final report — service started + URL ready + screenshot
        emailService.sendJobReport(job.label, job.serviceName, job.url, serviceRestarted, urlReady, screenshot);
        logger.info("[" + job.label + "] Job complete — restarted=" + serviceRestarted + " urlReady=" + urlReady);
    }

    private boolean shouldRunToday() {
        LocalDate today = LocalDate.now();
        DayOfWeek dow = today.getDayOfWeek();

        switch (job.scheduleType) {
            case DAILY:
                return true;
            case DAILY_EXCEPT_SATURDAY:
                return dow != DayOfWeek.SATURDAY;
            case DAILY_EXCEPT_SUNDAY:
                return dow != DayOfWeek.SUNDAY;
            case WEEKLY:
                return dow == job.weeklyDay;
            case MONTHLY_NTH_WEEKDAY:
                return today.equals(nthWeekdayOfMonth(today, job.monthlyNth, job.monthlyWeekday));
            default:
                return false;
        }
    }

    private LocalDate nthWeekdayOfMonth(LocalDate today, int nth, DayOfWeek weekday) {
        LocalDate first = today.withDayOfMonth(1).with(TemporalAdjusters.nextOrSame(weekday));
        return first.plusWeeks(nth - 1);
    }

    private void waitUntilStartTime() throws InterruptedException {
        java.time.LocalTime now = java.time.LocalTime.now();
        java.time.Duration gap = java.time.Duration.between(now, job.startTime);
        // If startTime is before now (e.g. stopTime=23:55, startTime=00:05 crosses midnight),
        // add 24h so we wait into the next day rather than skipping the wait.
        if (gap.isNegative()) {
            gap = gap.plusDays(1);
        }
        if (!gap.isZero()) {
            long millis = gap.toMillis();
            logger.info("[" + job.label + "] Waiting " + (millis / 1000) + "s until start time " + job.startTime);
            Thread.sleep(millis);
        }
    }
}
