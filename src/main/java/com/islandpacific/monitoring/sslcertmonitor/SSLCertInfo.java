package com.islandpacific.monitoring.sslcertmonitor;

import java.util.Date;

public class SSLCertInfo {

    private final String host;
    private final int port;
    private final Date expiryDate;
    private final long daysUntilExpiry;
    private final String error;

    public SSLCertInfo(String host, int port, Date expiryDate, long daysUntilExpiry) {
        this.host = host;
        this.port = port;
        this.expiryDate = expiryDate;
        this.daysUntilExpiry = daysUntilExpiry;
        this.error = null;
    }

    public SSLCertInfo(String host, int port, String error) {
        this.host = host;
        this.port = port;
        this.expiryDate = null;
        this.daysUntilExpiry = -1;
        this.error = error;
    }

    public String getHost() { return host; }
    public int getPort() { return port; }
    public Date getExpiryDate() { return expiryDate; }
    public long getDaysUntilExpiry() { return daysUntilExpiry; }
    public String getError() { return error; }
    public boolean hasError() { return error != null; }
    public String getHostPort() { return host + ":" + port; }
}
