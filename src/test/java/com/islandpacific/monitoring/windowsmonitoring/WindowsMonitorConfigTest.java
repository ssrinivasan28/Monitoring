package com.islandpacific.monitoring.windowsmonitoring;

import org.testng.annotations.Test;

import java.util.Properties;

import static org.testng.Assert.*;

public class WindowsMonitorConfigTest {

    private Properties baseEmailProps() {
        Properties p = new Properties();
        p.setProperty("mail.smtp.host", "smtp.example.com");
        p.setProperty("mail.from", "from@example.com");
        p.setProperty("mail.to", "to@example.com");
        return p;
    }

    // --- Defaults ---

    @Test
    public void defaults_appliedWhenPropertiesEmpty() {
        Properties app = new Properties();
        WindowsMonitorConfig cfg = WindowsMonitorConfig.fromProperties(app, baseEmailProps());

        assertEquals(cfg.getHosts().get(0), "localhost");
        assertEquals(cfg.getCpuAlertThreshold(), 90.0);
        assertEquals(cfg.getMemoryAlertThreshold(), 90.0);
        assertEquals(cfg.getDiskAlertThreshold(), 90.0);
        assertEquals(cfg.getTopNProcesses(), 5);
        assertEquals(cfg.getMonitorIntervalMs(), 300_000);
        assertEquals(cfg.getMetricsPort(), 3017);
        assertEquals(cfg.getPollThreads(), 5);
        assertEquals(cfg.getAlertWindowSize(), 3);
        assertTrue(cfg.getServicesToMonitor().isEmpty());
    }

    // --- pollThreads / alertWindowSize floor at 1 ---

    @Test
    public void pollThreads_flooredAtOne_whenZero() {
        Properties app = new Properties();
        app.setProperty("windows.poll.threads", "0");
        WindowsMonitorConfig cfg = WindowsMonitorConfig.fromProperties(app, baseEmailProps());
        assertEquals(cfg.getPollThreads(), 1);
    }

    @Test
    public void pollThreads_flooredAtOne_whenNegative() {
        Properties app = new Properties();
        app.setProperty("windows.poll.threads", "-5");
        WindowsMonitorConfig cfg = WindowsMonitorConfig.fromProperties(app, baseEmailProps());
        assertEquals(cfg.getPollThreads(), 1);
    }

    @Test
    public void alertWindowSize_flooredAtOne_whenZero() {
        Properties app = new Properties();
        app.setProperty("windows.alert.window.size", "0");
        WindowsMonitorConfig cfg = WindowsMonitorConfig.fromProperties(app, baseEmailProps());
        assertEquals(cfg.getAlertWindowSize(), 1);
    }

    // --- Host list parsing ---

    @Test
    public void multipleHosts_parsedCorrectly() {
        Properties app = new Properties();
        app.setProperty("windows.servers.list", "host1,host2,host3");
        WindowsMonitorConfig cfg = WindowsMonitorConfig.fromProperties(app, baseEmailProps());
        assertEquals(cfg.getHosts().size(), 3);
        assertTrue(cfg.getHosts().contains("host1"));
        assertTrue(cfg.getHosts().contains("host3"));
    }

    // --- Per-host credentials ---

    @Test
    public void credentials_resolvedForTrimmedHost() {
        Properties app = new Properties();
        app.setProperty("windows.servers.list", " remotehost ");
        app.setProperty("windows.server.remotehost.username", "DOMAIN\\user");
        app.setProperty("windows.server.remotehost.password", "secret");
        WindowsMonitorConfig cfg = WindowsMonitorConfig.fromProperties(app, baseEmailProps());

        WindowsMonitorConfig.Credentials creds = cfg.getCredentialsForHost("remotehost");
        assertNotNull(creds);
        assertEquals(creds.username, "DOMAIN\\user");
        assertEquals(creds.password, "secret");
    }

    @Test
    public void credentials_nullForHostWithNoneConfigured() {
        Properties app = new Properties();
        app.setProperty("windows.servers.list", "localhost");
        WindowsMonitorConfig cfg = WindowsMonitorConfig.fromProperties(app, baseEmailProps());
        assertNull(cfg.getCredentialsForHost("localhost"));
    }

    // --- Services list ---

    @Test
    public void servicesList_parsedFromProperty() {
        Properties app = new Properties();
        app.setProperty("windows.services.to.monitor", "Spooler,W32Time");
        WindowsMonitorConfig cfg = WindowsMonitorConfig.fromProperties(app, baseEmailProps());
        assertEquals(cfg.getServicesToMonitor().size(), 2);
        assertTrue(cfg.getServicesToMonitor().contains("Spooler"));
    }

    @Test
    public void servicesList_emptyWhenPropertyBlank() {
        Properties app = new Properties();
        app.setProperty("windows.services.to.monitor", "");
        WindowsMonitorConfig cfg = WindowsMonitorConfig.fromProperties(app, baseEmailProps());
        assertTrue(cfg.getServicesToMonitor().isEmpty());
    }

    // --- Email config ---

    @Test
    public void emailConfig_parsedFromEmailProperties() {
        Properties app = new Properties();
        Properties email = baseEmailProps();
        email.setProperty("mail.auth.method", "OAUTH2");
        email.setProperty("mail.oauth2.tenant.id", "tenant123");
        WindowsMonitorConfig cfg = WindowsMonitorConfig.fromProperties(app, email);
        assertEquals(cfg.getAuthMethod(), "OAUTH2");
        assertEquals(cfg.getOauth2TenantId(), "tenant123");
    }

    // --- Interval conversion ---

    @Test
    public void intervalSeconds_convertedToMillis() {
        Properties app = new Properties();
        app.setProperty("windows.monitor.interval.seconds", "60");
        WindowsMonitorConfig cfg = WindowsMonitorConfig.fromProperties(app, baseEmailProps());
        assertEquals(cfg.getMonitorIntervalMs(), 60_000);
    }
}
