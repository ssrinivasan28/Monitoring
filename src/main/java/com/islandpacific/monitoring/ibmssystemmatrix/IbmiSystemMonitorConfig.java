package com.islandpacific.monitoring.ibmssystemmatrix;

import com.islandpacific.monitoring.common.CredentialProtector;
import java.util.*;
import java.util.function.BiFunction;

public class IbmiSystemMonitorConfig {
    private final List<String> hosts;
    private final String user, password, clientName;
    private final double cpuT, aspT, poolT;
    private final long jobsT, activeJobsT;
    private final int intervalMs, metricsPort;
    private final boolean reportEnabled;
    private final String reportSendTime;
    private final String reportDataFolder;
    private final int reportRetentionDays;
    private final String reportLogoPath;

    private IbmiSystemMonitorConfig(List<String> h, String u, String p, String cn,
                                    double c, double a, double pool,
                                    long j, long aj,
                                    int ms, int mPort,
                                    boolean reportEnabled, String reportSendTime,
                                    String reportDataFolder, int reportRetentionDays,
                                    String reportLogoPath) {
        hosts = h; user = u; password = p; clientName = cn;
        cpuT = c; aspT = a; poolT = pool; jobsT = j; activeJobsT = aj;
        intervalMs = ms; metricsPort = mPort;
        this.reportEnabled = reportEnabled;
        this.reportSendTime = reportSendTime;
        this.reportDataFolder = reportDataFolder;
        this.reportRetentionDays = reportRetentionDays;
        this.reportLogoPath = reportLogoPath;
    }

    public static IbmiSystemMonitorConfig fromProperties(Properties app, Properties email) {
        BiFunction<Properties,String,String> getReq = (p,k) -> {
            String v = p.getProperty(k); if (v==null||v.isEmpty()) throw new IllegalArgumentException("Missing "+k); return v;
        };

        List<String> hosts = Arrays.asList(getReq.apply(app, "ibmi.host").split(","));
        String u = getReq.apply(app,"ibmi.user");
        String ibmiPasswordRaw = app.getProperty("ibmi.password");
        if (ibmiPasswordRaw == null || ibmiPasswordRaw.trim().isEmpty()) {
            throw new IllegalArgumentException("Required property 'ibmi.password' is missing or empty.");
        }
        String p = CredentialProtector.resolve(ibmiPasswordRaw);
        String cn = app.getProperty("client.name", email.getProperty("mail.clientName", ""));

        double cpu = Double.parseDouble(getReq.apply(app,"ibmi.alert.threshold.cpu"));
        double asp = Double.parseDouble(getReq.apply(app,"ibmi.alert.threshold.asp"));
        double pool = Double.parseDouble(getReq.apply(app,"ibmi.alert.threshold.shared.processor.pool"));
        long jobs = Long.parseLong(getReq.apply(app,"ibmi.alert.threshold.total.jobs"));
        long ajobs = Long.parseLong(getReq.apply(app,"ibmi.alert.threshold.active.jobs"));

        int interval = Integer.parseInt(app.getProperty("monitor.interval.ms","60000"));
        int mPort = Integer.parseInt(app.getProperty("metrics.port","9100"));

        boolean reportEnabled = Boolean.parseBoolean(app.getProperty("report.enabled","false"));
        String reportSendTime = app.getProperty("report.send.time","08:00");
        String reportDataFolder = app.getProperty("report.data.folder","data/ibmmatrix");
        int reportRetentionDays = Integer.parseInt(app.getProperty("report.retention.days","7"));
        String reportLogoPath = app.getProperty("report.logo.path","logo.jpg");

        return new IbmiSystemMonitorConfig(hosts,u,p,cn,cpu,asp,pool,jobs,ajobs,interval,mPort,
                reportEnabled,reportSendTime,reportDataFolder,reportRetentionDays,reportLogoPath);
    }

    public List<String> getIbmiHosts() { return hosts; }
    public String getIbmiUser() { return user; }
    public String getIbmiPassword() { return password; }
    public String getClientName() { return clientName; }
    public double getCpuAlertThreshold() { return cpuT; }
    public double getAspAlertThreshold() { return aspT; }
    public double getSharedProcessorPoolAlertThreshold() { return poolT; }
    public long getTotalJobsAlertThreshold() { return jobsT; }
    public long getActiveJobsAlertThreshold() { return activeJobsT; }
    public int getMonitorIntervalMs() { return intervalMs; }
    public int getMetricsPort() { return metricsPort; }
    public boolean isReportEnabled() { return reportEnabled; }
    public String getReportSendTime() { return reportSendTime; }
    public String getReportDataFolder() { return reportDataFolder; }
    public int getReportRetentionDays() { return reportRetentionDays; }
    public String getReportLogoPath() { return reportLogoPath; }
}
