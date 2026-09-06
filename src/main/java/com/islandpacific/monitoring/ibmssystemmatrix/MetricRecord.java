package com.islandpacific.monitoring.ibmssystemmatrix;

import java.time.Instant;

public class MetricRecord {
    public final Instant timestamp;
    public final String host;
    public final double cpu;
    public final double asp;
    public final double pool;
    public final long totalJobs;
    public final long activeJobs;

    public MetricRecord(Instant timestamp, String host, double cpu, double asp,
                        double pool, long totalJobs, long activeJobs) {
        this.timestamp = timestamp;
        this.host = host;
        this.cpu = cpu;
        this.asp = asp;
        this.pool = pool;
        this.totalJobs = totalJobs;
        this.activeJobs = activeJobs;
    }

    public String toCsvLine() {
        return timestamp.toEpochMilli() + "," + host + "," + cpu + "," + asp + ","
                + pool + "," + totalJobs + "," + activeJobs;
    }

    public static MetricRecord fromCsvLine(String line) {
        String[] p = line.split(",", 7);
        if (p.length < 7) return null;
        return new MetricRecord(
                Instant.ofEpochMilli(Long.parseLong(p[0].trim())),
                p[1].trim(),
                Double.parseDouble(p[2].trim()),
                Double.parseDouble(p[3].trim()),
                Double.parseDouble(p[4].trim()),
                Long.parseLong(p[5].trim()),
                Long.parseLong(p[6].trim())
        );
    }
}
