package com.islandpacific.monitoring.windowsmonitoring;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

public class WinMetricRecord {
    public static class DiskSnapshot {
        public final double pct;
        public final double totalGB;
        public final double usedGB;
        public final double freeGB;
        public DiskSnapshot(double pct, double totalGB, double usedGB, double freeGB) {
            this.pct = pct; this.totalGB = totalGB; this.usedGB = usedGB; this.freeGB = freeGB;
        }
    }

    public final Instant timestamp;
    public final String host;
    public final double cpu;
    public final double memoryPercent;
    // drive -> full disk snapshot
    public final Map<String, DiskSnapshot> diskInfo;
    // adapter -> bytesReceived
    public final Map<String, Long> netBytesReceived;
    public final Map<String, Long> netBytesSent;
    public final double uptimeHours;
    // process -> cpu% (top-N snapshot)
    public final Map<String, Double> topProcesses;

    // kept for chart compatibility
    public Map<String, Double> diskPercent() {
        Map<String, Double> m = new LinkedHashMap<>();
        diskInfo.forEach((d, s) -> m.put(d, s.pct));
        return m;
    }

    public WinMetricRecord(Instant timestamp, String host, double cpu, double memoryPercent,
                           Map<String, DiskSnapshot> diskInfo,
                           Map<String, Long> netBytesReceived, Map<String, Long> netBytesSent,
                           double uptimeHours, Map<String, Double> topProcesses) {
        this.timestamp = timestamp;
        this.host = host;
        this.cpu = cpu;
        this.memoryPercent = memoryPercent;
        this.diskInfo = diskInfo == null ? new LinkedHashMap<>() : diskInfo;
        this.netBytesReceived = netBytesReceived == null ? new LinkedHashMap<>() : netBytesReceived;
        this.netBytesSent = netBytesSent == null ? new LinkedHashMap<>() : netBytesSent;
        this.uptimeHours = uptimeHours;
        this.topProcesses = topProcesses == null ? new LinkedHashMap<>() : topProcesses;
    }

    // CSV format: timestamp,host,cpu,memPct,uptime,disk:<drive>=<pct>...,net:<adapter>=<rx>/<tx>...,proc:<name>=<pct>...
    public String toCsvLine() {
        StringBuilder sb = new StringBuilder();
        sb.append(timestamp.toEpochMilli()).append(',')
          .append(escapeCsv(host)).append(',')
          .append(cpu).append(',')
          .append(memoryPercent).append(',')
          .append(uptimeHours).append(',');

        sb.append('"');
        diskInfo.forEach((d, s) -> sb.append(escapePair(d)).append('=')
                .append(s.pct).append('/').append(s.totalGB).append('/').append(s.usedGB).append('/').append(s.freeGB).append(';'));
        sb.append('"').append(',');

        sb.append('"');
        netBytesReceived.forEach((a, rx) -> {
            long tx = netBytesSent.getOrDefault(a, 0L);
            sb.append(escapePair(a)).append('=').append(rx).append('/').append(tx).append(';');
        });
        sb.append('"').append(',');

        sb.append('"');
        topProcesses.forEach((n, p) -> sb.append(escapePair(n)).append('=').append(p).append(';'));
        sb.append('"');

        return sb.toString();
    }

    public static WinMetricRecord fromCsvLine(String line) {
        // Split on commas outside quoted fields
        String[] p = splitCsv(line);
        if (p == null || p.length < 8) return null;
        try {
            Instant ts = Instant.ofEpochMilli(Long.parseLong(p[0].trim()));
            String host = p[1].trim();
            double cpu = Double.parseDouble(p[2].trim());
            double mem = Double.parseDouble(p[3].trim());
            double uptime = Double.parseDouble(p[4].trim());

            Map<String, DiskSnapshot> disk = parseDiskMap(stripQuotes(p[5]));
            Map<String, Long[]> net = parsePairMapNet(stripQuotes(p[6]));
            Map<String, Double> procs = parsePairMapDouble(stripQuotes(p[7]));

            Map<String, Long> rx = new LinkedHashMap<>();
            Map<String, Long> tx = new LinkedHashMap<>();
            net.forEach((a, vals) -> { rx.put(a, vals[0]); tx.put(a, vals[1]); });

            return new WinMetricRecord(ts, host, cpu, mem, disk, rx, tx, uptime, procs);
        } catch (Exception e) {
            return null;
        }
    }

    private static String escapeCsv(String s) {
        return s == null ? "" : s.replace(",", "_");
    }

    private static String escapePair(String s) {
        return s == null ? "" : s.replace("=", "_").replace(";", "_").replace(",", "_").replace("\"", "");
    }

    private static String stripQuotes(String s) {
        if (s == null) return "";
        s = s.trim();
        if (s.startsWith("\"") && s.endsWith("\"")) s = s.substring(1, s.length() - 1);
        return s;
    }

    private static Map<String, DiskSnapshot> parseDiskMap(String s) {
        Map<String, DiskSnapshot> map = new LinkedHashMap<>();
        if (s == null || s.isEmpty()) return map;
        for (String pair : s.split(";")) {
            int eq = pair.indexOf('=');
            if (eq <= 0) continue;
            String key = pair.substring(0, eq);
            String[] vals = pair.substring(eq + 1).split("/");
            try {
                double pct   = Double.parseDouble(vals[0]);
                double total = vals.length > 1 ? Double.parseDouble(vals[1]) : 0;
                double used  = vals.length > 2 ? Double.parseDouble(vals[2]) : 0;
                double free  = vals.length > 3 ? Double.parseDouble(vals[3]) : 0;
                map.put(key, new DiskSnapshot(pct, total, used, free));
            } catch (Exception ignore) {}
        }
        return map;
    }

    private static Map<String, Double> parsePairMapDouble(String s) {
        Map<String, Double> map = new LinkedHashMap<>();
        if (s == null || s.isEmpty()) return map;
        for (String pair : s.split(";")) {
            int eq = pair.indexOf('=');
            if (eq <= 0) continue;
            try { map.put(pair.substring(0, eq), Double.parseDouble(pair.substring(eq + 1))); } catch (Exception ignore) {}
        }
        return map;
    }

    private static Map<String, Long[]> parsePairMapNet(String s) {
        Map<String, Long[]> map = new LinkedHashMap<>();
        if (s == null || s.isEmpty()) return map;
        for (String pair : s.split(";")) {
            int eq = pair.indexOf('=');
            if (eq <= 0) continue;
            String key = pair.substring(0, eq);
            String[] vals = pair.substring(eq + 1).split("/");
            try { map.put(key, new Long[]{Long.parseLong(vals[0]), vals.length > 1 ? Long.parseLong(vals[1]) : 0L}); } catch (Exception ignore) {}
        }
        return map;
    }

    // Simple CSV splitter that respects double-quoted fields
    private static String[] splitCsv(String line) {
        java.util.List<String> parts = new java.util.ArrayList<>();
        boolean inQuote = false;
        StringBuilder cur = new StringBuilder();
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '"') {
                inQuote = !inQuote;
                cur.append(c);
            } else if (c == ',' && !inQuote) {
                parts.add(cur.toString());
                cur.setLength(0);
            } else {
                cur.append(c);
            }
        }
        parts.add(cur.toString());
        return parts.toArray(new String[0]);
    }
}
