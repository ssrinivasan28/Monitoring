package com.islandpacific.monitoring.ibmifilemembermonitor;

import io.prometheus.client.Gauge;
import javax.mail.MessagingException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

public class FileMemberMonitor {

    private static final Logger logger = Logger.getLogger(FileMemberMonitor.class.getName());

    private final IbmiFileMemberService ibmiFileMemberService;
    private final EmailService emailService;
    private final FileMemberMonitorConfig config;
    private final String clientName;

    private final ScheduledExecutorService scheduler;
    private final Map<String, Long> lastKnownRecordCounts; // Key: library/file/member
    private final Map<String, Boolean> isBreached; // Key: library/file/member, Value: true if currently in breach state

    // Prometheus Gauge for metrics
    private static final Gauge FILE_MEMBER_RECORD_COUNT = Gauge.build()
            .name("ibmi_file_member_record_count")
            .help("Current record count of an IBM i physical file member.")
            .labelNames("library", "file", "member")
            .register();

    private static final Gauge FILE_MEMBER_THRESHOLD_BREACH = Gauge.build()
            .name("ibmi_file_member_threshold_breach")
            .help("Indicates if an IBM i file member's record count has breached its threshold (1 for breach, 0 for no breach).")
            .labelNames("library", "file", "member")
            .register();

    public FileMemberMonitor(IbmiFileMemberService ibmiFileMemberService, EmailService emailService, FileMemberMonitorConfig config, String clientName) {
        this.ibmiFileMemberService = ibmiFileMemberService;
        this.emailService = emailService;
        this.config = config;
        this.clientName = clientName;
        this.scheduler = Executors.newSingleThreadScheduledExecutor();
        this.lastKnownRecordCounts = new HashMap<>();
        this.isBreached = new HashMap<>();
    }

    public void start() {
        long interval;
        TimeUnit timeUnit;

        try {
            long seconds = config.getFileMemberPollingIntervalSeconds();
            if (seconds > 0) {
                interval = seconds;
                timeUnit = TimeUnit.SECONDS;
                logger.info("Starting file member monitoring. Interval: " + interval + " seconds.");
            } else {
                interval = config.getMonitoringIntervalMinutes();
                timeUnit = TimeUnit.MINUTES;
                logger.info("Starting file member monitoring. Interval: " + interval + " minutes (seconds config invalid or not positive).");
            }
        } catch (NumberFormatException e) {
            interval = config.getMonitoringIntervalMinutes();
            timeUnit = TimeUnit.MINUTES;
            logger.warning("Configuration 'file.member.pollingIntervalSeconds' is not a valid number. Falling back to monitoring.interval.minutes: " + interval + " minutes.");
        }

        scheduler.scheduleAtFixedRate(this::monitorFileMembers, 0, interval, timeUnit);
    }

    public void stop() {
        logger.info("Stopping file member monitoring...");
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
                logger.warning("Scheduler did not terminate gracefully.");
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
            logger.log(Level.SEVERE, "Scheduler termination interrupted.", e);
        }
        logger.info("File member monitoring stopped.");
    }

    private void monitorFileMembers() {
        logger.info("Initiating file member monitoring cycle...");
        List<FileMemberThresholdBreach> breaches = new ArrayList<>();

        for (String fullMemberPath : config.getAllUniqueMonitoredMembers()) {
            String[] parts = fullMemberPath.split("/");
            if (parts.length != 3) {
                logger.warning("Invalid file member path obtained from config: " + fullMemberPath + ". Expected format: LIBRARY/FILE/MEMBER");
                continue;
            }
            String library = parts[0];
            String fileName = parts[1];
            String memberName = parts[2];
            String memberKey = library + "/" + fileName + "/" + memberName;

            try {
                long currentCount = ibmiFileMemberService.getMemberRecordCount(library, fileName, memberName);
                if (currentCount == -1) {
                    logger.warning("Could not retrieve record count for " + memberKey + ". It might not exist.");
                    continue;
                }

                long effectiveBreachThreshold = config.getEffectiveBreachThreshold(memberKey);

                FILE_MEMBER_RECORD_COUNT.labels(library, fileName, memberName).set(currentCount);

                boolean previouslyBreached = isBreached.getOrDefault(memberKey, false);

                if (currentCount > effectiveBreachThreshold) {
                    if (!previouslyBreached) {
                        breaches.add(new FileMemberThresholdBreach(library, fileName, memberName, currentCount, effectiveBreachThreshold, "BREACHED"));
                        isBreached.put(memberKey, true);
                        FILE_MEMBER_THRESHOLD_BREACH.labels(library, fileName, memberName).set(1);
                        logger.info("BREACH DETECTED for " + memberKey + ": Current=" + currentCount + ", Threshold=" + effectiveBreachThreshold);
                    } else {
                        logger.fine("Still in BREACH state for " + memberKey + ".");
                    }
                } else if (currentCount <= effectiveBreachThreshold) {
                    if (previouslyBreached) {
                        isBreached.put(memberKey, false);
                        FILE_MEMBER_THRESHOLD_BREACH.labels(library, fileName, memberName).set(0);
                        logger.info("Returned to NORMAL state from BREACH for " + memberKey + ": Current=" + currentCount);
                    } else {
                        logger.fine("Currently in NORMAL state for " + memberKey + ".");
                    }
                }

                lastKnownRecordCounts.put(memberKey, currentCount);

            } catch (Exception e) {
                logger.log(Level.SEVERE, "Error monitoring file member " + memberKey + ": " + e.getMessage(), e);
            }
        }

        if (!breaches.isEmpty()) {
            sendBreachEmail(breaches, "BREACH DETECTED");
        }
        logger.info("File member monitoring cycle completed.");
    }

    private void sendBreachEmail(List<FileMemberThresholdBreach> breaches, String breachType) {
        if (emailService == null || !config.isFileMemberMonitorEnabled()) {
            logger.warning("Email service not configured or monitor disabled. Cannot send email.");
            return;
        }

        try {
            String subject;
            if (!breaches.isEmpty()) {
                // Use the first breached member's details for the subject as per your request
                FileMemberThresholdBreach firstBreach = breaches.get(0);
                subject = String.format("[%s] IBM i Member Record Count Deviation - %s/%s(%s)",
                                       config.getClientName(),
                                       firstBreach.getLibrary(),
                                       firstBreach.getFileName(),
                                       firstBreach.getMemberName());
            } else {
                // Fallback subject if for some reason breaches list is empty (shouldn't happen here)
                subject = String.format("[%s] IBM i Member Record Count Deviation - %s",
                                       config.getClientName(), breachType);
            }

            String emailContent = emailService.buildEmailHtmlContent(breaches, breachType);
            emailService.sendEmail(subject, emailContent);
            logger.info("Email sent for " + breachType + " with " + breaches.size() + " members.");
        } catch (MessagingException e) {
            logger.log(Level.SEVERE, "Failed to send email for " + breachType + ": " + e.getMessage(), e);
        }
    }
}
