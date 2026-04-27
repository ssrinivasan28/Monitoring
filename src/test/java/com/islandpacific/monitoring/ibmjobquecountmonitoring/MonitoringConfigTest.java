package com.islandpacific.monitoring.ibmjobquecountmonitoring;

import org.testng.annotations.Test;

import java.util.Properties;

import static org.testng.Assert.*;

public class MonitoringConfigTest {

    private Properties baseEmailProps() {
        Properties p = new Properties();
        p.setProperty("mail.smtp.host", "smtp.example.com");
        p.setProperty("mail.from", "from@example.com");
        p.setProperty("mail.to", "to@example.com");
        return p;
    }

    private Properties baseIbmiProps() {
        Properties p = new Properties();
        p.setProperty("ibmi.host", "ibmi-host");
        p.setProperty("ibmi.user", "IBMUSER");
        p.setProperty("ibmi.password", "secret");
        p.setProperty("jobqueue.monitor.ids", "1");
        p.setProperty("jobqueue.1.name", "QBATCH");
        p.setProperty("jobqueue.1.library", "QSYS");
        p.setProperty("jobqueue.1.threshold", "10");
        return p;
    }

    @Test
    public void config_parsedCorrectly() {
        MonitoringConfig cfg = MonitoringConfig.fromProperties(baseEmailProps(), baseIbmiProps());
        assertEquals(cfg.getIbmiHost(), "ibmi-host");
        assertEquals(cfg.getIbmiUser(), "IBMUSER");
        assertEquals(cfg.getJobQueuesToMonitor().size(), 1);
        assertEquals(cfg.getJobQueuesToMonitor().get(0).getName(), "QBATCH");
        assertEquals(cfg.getJobQueuesToMonitor().get(0).getLibrary(), "QSYS");
        assertEquals(cfg.getJobQueuesToMonitor().get(0).getThreshold(), 10);
    }

    @Test
    public void config_defaultsApplied() {
        MonitoringConfig cfg = MonitoringConfig.fromProperties(baseEmailProps(), baseIbmiProps());
        assertEquals(cfg.getMonitorIntervalMs(), 60000);
        assertEquals(cfg.getMetricsPort(), 8081);
        assertEquals(cfg.getEmailImportance(), "Normal");
        assertEquals(cfg.getEmailPort(), "25");
    }

    @Test
    public void config_multipleQueues_parsedAll() {
        Properties ibmi = baseIbmiProps();
        ibmi.setProperty("jobqueue.monitor.ids", "1,2");
        ibmi.setProperty("jobqueue.2.name", "QINTER");
        ibmi.setProperty("jobqueue.2.library", "QSYS");
        ibmi.setProperty("jobqueue.2.threshold", "5");

        MonitoringConfig cfg = MonitoringConfig.fromProperties(baseEmailProps(), ibmi);
        assertEquals(cfg.getJobQueuesToMonitor().size(), 2);
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void config_missingIbmiHost_throwsIllegalArgument() {
        Properties ibmi = baseIbmiProps();
        ibmi.remove("ibmi.host");
        MonitoringConfig.fromProperties(baseEmailProps(), ibmi);
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void config_missingQueueIds_throwsIllegalArgument() {
        Properties ibmi = baseIbmiProps();
        ibmi.remove("jobqueue.monitor.ids");
        MonitoringConfig.fromProperties(baseEmailProps(), ibmi);
    }

    @Test
    public void config_clientMonitorName_defaultsToEmpty() {
        MonitoringConfig cfg = MonitoringConfig.fromProperties(baseEmailProps(), baseIbmiProps());
        assertEquals(cfg.getClientMonitorName(), "");
    }

    @Test
    public void config_clientMonitorName_parsedWhenSet() {
        Properties ibmi = baseIbmiProps();
        ibmi.setProperty("client.monitor", "CLIENT_A");
        MonitoringConfig cfg = MonitoringConfig.fromProperties(baseEmailProps(), ibmi);
        assertEquals(cfg.getClientMonitorName(), "CLIENT_A");
    }

    @Test
    public void jobQueueList_isUnmodifiable() {
        MonitoringConfig cfg = MonitoringConfig.fromProperties(baseEmailProps(), baseIbmiProps());
        assertThrows(UnsupportedOperationException.class,
                () -> cfg.getJobQueuesToMonitor().add(new JobQueueInfo("x", "X", "LIB", 1)));
    }
}
