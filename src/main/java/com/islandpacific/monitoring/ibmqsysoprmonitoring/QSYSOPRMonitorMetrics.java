package com.islandpacific.monitoring.ibmqsysoprmonitoring;

import io.prometheus.client.Counter;
import io.prometheus.client.Gauge;

public class QSYSOPRMonitorMetrics {

    // Define your custom metrics
    private static final Counter jobFailureAlertsSent = Counter.build()
            .name("qsysopr_job_failure_alerts_sent_total")
            .help("Total number of job failure email alerts sent.")
            .register();

    private static final Counter messagesProcessedTotal = Counter.build()
            .name("qsysopr_messages_processed_total")
            .help("Total number of messages processed from QSYSOPR message queue.")
            .register();

    private static final Counter dbConnectionErrors = Counter.build()
            .name("qsysopr_db_connection_errors_total")
            .help("Total number of database connection errors encountered.")
            .register();

    private static final Counter sqlQueryErrors = Counter.build()
            .name("qsysopr_sql_query_errors_total")
            .help("Total number of SQL query errors encountered.")
            .register();

    private static final Counter emailSendErrors = Counter.build()
            .name("qsysopr_email_send_errors_total")
            .help("Total number of email sending errors encountered.")
            .register();

    private static final Gauge monitorStatus = Gauge.build()
            .name("qsysopr_monitor_status")
            .help("Current operational status of the QSYSOPR monitor (1=running, 0=stopped/error).")
            .register();

    private static final Gauge lastProcessedMessageTimestamp = Gauge.build()
            .name("qsysopr_last_processed_message_timestamp_seconds")
            .help("Timestamp of the last message successfully processed from QSYSOPR (Unix timestamp in seconds).")
            .register();

    public static void initializeMetrics() {
       
        monitorStatus.set(1); // Set initial status to running
    }

    // --- Getters for all custom metrics ---
    public static Counter getJobFailureAlertsSent() {
        return jobFailureAlertsSent;
    }

    public static Counter getMessagesProcessedTotal() {
        return messagesProcessedTotal;
    }

    public static Counter getDbConnectionErrors() {
        return dbConnectionErrors;
    }

    public static Counter getSqlQueryErrors() {
        return sqlQueryErrors;
    }

    public static Counter getEmailSendErrors() {
        return emailSendErrors;
    }

    public static Gauge getMonitorStatus() {
        return monitorStatus;
    }

    public static Gauge getLastProcessedMessageTimestamp() {
        return lastProcessedMessageTimestamp;
    }

    public static void setMonitorStopped() {
        monitorStatus.set(0);
    }
}
