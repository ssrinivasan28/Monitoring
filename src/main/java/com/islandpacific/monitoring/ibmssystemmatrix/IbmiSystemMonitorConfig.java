package com.islandpacific.monitoring.ibmssystemmatrix;

import java.util.*;
import java.util.function.BiFunction;

public class IbmiSystemMonitorConfig {
    private final List<String> hosts;
    private final String user, password;
    private final double cpuT, aspT, poolT;
    private final long jobsT, activeJobsT;
    private final String eHost, ePort, eFrom, eTo, eBcc, eUser, ePass, importance;
    private final boolean eAuth, eTls;
    private final int intervalMs, metricsPort;

    private IbmiSystemMonitorConfig(List<String> h, String u, String p,
                                    double c, double a, double pool,
                                    long j, long aj,
                                    String eh, String ep, String ef, String eto, String ebcc,
                                    String eu, String epass, boolean ea, boolean etls, String imp,
                                    int ms, int mPort) {
        hosts = h; user = u; password = p;
        cpuT = c; aspT = a; poolT = pool; jobsT = j; activeJobsT = aj;
        eHost = eh; ePort = ep; eFrom = ef; eTo = eto; eBcc = ebcc;
        eUser = eu; ePass = epass; eAuth = ea; eTls = etls; importance = imp;
        intervalMs = ms; metricsPort = mPort;
    }

    public static IbmiSystemMonitorConfig fromProperties(Properties app, Properties email) {
        BiFunction<Properties,String,String> getReq = (p,k) -> {
            String v = p.getProperty(k); if (v==null||v.isEmpty()) throw new IllegalArgumentException("Missing "+k); return v;
        };

        List<String> hosts = Arrays.asList(getReq.apply(app, "ibmi.hosts").split(","));
        String u = getReq.apply(app,"ibmi.user");
        String p = app.getProperty("ibmi.password","");

        double cpu = Double.parseDouble(getReq.apply(app,"ibmi.alert.threshold.cpu"));
        double asp = Double.parseDouble(getReq.apply(app,"ibmi.alert.threshold.asp"));
        double pool = Double.parseDouble(getReq.apply(app,"ibmi.alert.threshold.shared.processor.pool"));
        long jobs = Long.parseLong(getReq.apply(app,"ibmi.alert.threshold.total.jobs"));
        long ajobs = Long.parseLong(getReq.apply(app,"ibmi.alert.threshold.active.jobs"));

        int interval = Integer.parseInt(app.getProperty("ibmi.monitor.interval.seconds","60"))*1000;
        int mPort = Integer.parseInt(app.getProperty("metrics.exporter.port","9100"));

        String eh = getReq.apply(email,"mail.smtp.host");
        String ep = email.getProperty("mail.smtp.port","25");
        String ef = getReq.apply(email,"mail.from");
        String eto = getReq.apply(email,"mail.to");
        String ebcc = email.getProperty("mail.bcc","");
        String eu = email.getProperty("mail.smtp.username","");
        String epass = email.getProperty("mail.smtp.password","");
        boolean ea = Boolean.parseBoolean(email.getProperty("mail.smtp.auth","false"));
        boolean etls = Boolean.parseBoolean(email.getProperty("mail.smtp.starttls.enable","false"));
        String imp = email.getProperty("mail.importance","Normal");

        return new IbmiSystemMonitorConfig(hosts,u,p,cpu,asp,pool,jobs,ajobs,eh,ep,ef,eto,ebcc,eu,epass,ea,etls,imp,interval,mPort);
    }

    public List<String> getIbmiHosts() { return hosts; }
    public String getIbmiUser() { return user; }
    public String getIbmiPassword() { return password; }
    public double getCpuAlertThreshold() { return cpuT; }
    public double getAspAlertThreshold() { return aspT; }
    public double getSharedProcessorPoolAlertThreshold() { return poolT; }
    public long getTotalJobsAlertThreshold() { return jobsT; }
    public long getActiveJobsAlertThreshold() { return activeJobsT; }
    public String getEmailHost() { return eHost; }
    public String getEmailPort() { return ePort; }
    public String getEmailFrom() { return eFrom; }
    public String getEmailTo() { return eTo; }
    public String getEmailBcc() { return eBcc; }
    public String getEmailUsername() { return eUser; }
    public String getEmailPassword() { return ePass; }
    public boolean isEmailAuthEnabled() { return eAuth; }
    public boolean isEmailStartTlsEnabled() { return eTls; }
    public String getEmailImportance() { return importance; }
    public int getMonitorIntervalMs() { return intervalMs; }
    public int getMetricsPort() { return metricsPort; }
}
