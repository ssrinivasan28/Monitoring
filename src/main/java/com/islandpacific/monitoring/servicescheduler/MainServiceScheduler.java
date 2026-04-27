package com.islandpacific.monitoring.servicescheduler;

import com.islandpacific.monitoring.common.AppLogger;

import java.io.FileInputStream;
import java.io.IOException;
import java.time.Duration;
import java.time.LocalTime;
import java.util.Properties;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

public class MainServiceScheduler {

    private static Logger logger = Logger.getLogger(MainServiceScheduler.class.getName());

    public static void main(String[] args) throws IOException {
        String emailPropsPath = args.length > 0 ? args[0] : "email.properties";
        String schedulerPropsPath = args.length > 1 ? args[1] : "servicescheduler.properties";

        Properties emailProps = new Properties();
        try (FileInputStream fis = new FileInputStream(emailPropsPath)) {
            emailProps.load(fis);
        }

        String logLevel = emailProps.getProperty("log.level", "INFO");
        String logFolder = emailProps.getProperty("log.folder", "logs");
        int retentionDays = Integer.parseInt(emailProps.getProperty("log.retention.days", "30"));
        int purgeInterval = Integer.parseInt(emailProps.getProperty("log.purge.interval.hours", "24"));

        AppLogger.setupLogger("ServiceScheduler", logLevel, logFolder);
        AppLogger.startScheduledLogPurge(retentionDays, purgeInterval);
        logger = AppLogger.getLogger();

        ServiceSchedulerConfig config = ServiceSchedulerConfig.load(schedulerPropsPath);
        ScreenshotService screenshotService = new ScreenshotService(config.screenshotFolder);

        logger.info("Service Scheduler starting - " + config.jobs.size() + " job(s) configured");

        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(config.jobs.size());

        for (JobDefinition job : config.jobs) {
            JobRunner runner = new JobRunner(job, screenshotService, emailProps);
            scheduleNextRun(scheduler, job, runner);
        }

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            logger.info("Shutting down Service Scheduler...");
            scheduler.shutdown();
            try {
                if (!scheduler.awaitTermination(30, TimeUnit.SECONDS)) {
                    scheduler.shutdownNow();
                }
            } catch (InterruptedException e) {
                scheduler.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }));
    }

    private static long secondsUntil(LocalTime target) {
        LocalTime now = LocalTime.now();
        Duration d = Duration.between(now, target);
        if (d.isNegative()) {
            d = d.plusDays(1);
        }
        return d.toSeconds();
    }

    private static void scheduleNextRun(ScheduledExecutorService scheduler, JobDefinition job, JobRunner runner) {
        long delaySeconds = secondsUntil(job.stopTime);
        logger.info("Scheduled [" + job.label + "] next run in " + delaySeconds + "s at " + job.stopTime
                + " (" + job.scheduleType + ")");

        scheduler.schedule(() -> {
            try {
                runner.run();
            } finally {
                if (!scheduler.isShutdown()) {
                    scheduleNextRun(scheduler, job, runner);
                }
            }
        }, delaySeconds, TimeUnit.SECONDS);
    }
}
