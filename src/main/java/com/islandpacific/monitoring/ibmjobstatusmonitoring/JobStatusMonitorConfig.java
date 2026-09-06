package com.islandpacific.monitoring.ibmjobstatusmonitoring;

import com.islandpacific.monitoring.common.CredentialProtector;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

public class JobStatusMonitorConfig {

    private final String ibmiHost;
    private final String ibmiUser;
    private final String ibmiPassword;
    private final String clientName;
    private final long monitorIntervalMs;
    private final int metricsPort;
    private final long msgwThresholdSeconds;
    private final Properties emailProps;
    private final Properties monitorProps;

    public JobStatusMonitorConfig(String emailPropsFile, String monitorPropsFile) throws IOException {
        emailProps = new Properties();
        try (FileInputStream fis = new FileInputStream(emailPropsFile)) {
            emailProps.load(fis);
        }

        monitorProps = new Properties();
        try (FileInputStream fis = new FileInputStream(monitorPropsFile)) {
            monitorProps.load(fis);
        }

        ibmiHost = this.monitorProps.getProperty("ibmi.host", "");
        ibmiUser = this.monitorProps.getProperty("ibmi.user", "");
        String rawPassword = this.monitorProps.getProperty("ibmi.password", "");
        ibmiPassword = CredentialProtector.resolve(rawPassword);
        clientName = this.monitorProps.getProperty("client.name", emailProps.getProperty("mail.clientName", ""));
        monitorIntervalMs = Long.parseLong(this.monitorProps.getProperty("monitor.interval.ms", "60000"));
        metricsPort = Integer.parseInt(this.monitorProps.getProperty("metrics.port", "3018"));
        msgwThresholdSeconds = Long.parseLong(this.monitorProps.getProperty("msgw.threshold.seconds", "300"));

        if (ibmiHost.isEmpty() || ibmiUser.isEmpty() || rawPassword.isEmpty()) {
            throw new IllegalArgumentException("Required properties ibmi.host, ibmi.user, ibmi.password must not be empty.");
        }
    }

    public String getIbmiHost()              { return ibmiHost; }
    public String getIbmiUser()              { return ibmiUser; }
    public String getIbmiPassword()          { return ibmiPassword; }
    public String getClientName()            { return clientName; }
    public long getMonitorIntervalMs()       { return monitorIntervalMs; }
    public int getMetricsPort()              { return metricsPort; }
    public long getMsgwThresholdSeconds()    { return msgwThresholdSeconds; }
    public Properties getEmailProps()        { return emailProps; }
    public Properties getMonitorProps()      { return monitorProps; }
}
