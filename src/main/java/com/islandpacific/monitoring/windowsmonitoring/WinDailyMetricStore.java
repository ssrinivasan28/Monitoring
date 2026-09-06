package com.islandpacific.monitoring.windowsmonitoring;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class WinDailyMetricStore {

    private static final Logger logger = Logger.getLogger(WinDailyMetricStore.class.getName());
    private static final String HEADER = "timestamp,host,cpu,memPct,uptime,disk,net,procs";

    private final Path csvFile;

    public WinDailyMetricStore(String dataFolder) {
        Path dir = Paths.get(dataFolder);
        try {
            Files.createDirectories(dir);
        } catch (IOException e) {
            logger.log(Level.WARNING, "Could not create metric data folder: " + dataFolder, e);
        }
        this.csvFile = dir.resolve("winmonitor_metrics.csv");
    }

    public synchronized void append(WinMetricRecord record) {
        try {
            boolean writeHeader = !Files.exists(csvFile) || Files.size(csvFile) == 0;
            try (BufferedWriter w = Files.newBufferedWriter(csvFile, StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND)) {
                if (writeHeader) w.write(HEADER + "\n");
                w.write(record.toCsvLine() + "\n");
            }
        } catch (IOException e) {
            logger.log(Level.WARNING, "Failed to write WinMetricRecord to CSV: " + e.getMessage(), e);
        }
    }

    public synchronized List<WinMetricRecord> readLast24Hours() {
        List<WinMetricRecord> result = new ArrayList<>();
        if (!Files.exists(csvFile)) return result;
        Instant cutoff = Instant.now().minus(24, ChronoUnit.HOURS);
        try (BufferedReader r = Files.newBufferedReader(csvFile, StandardCharsets.UTF_8)) {
            String line;
            while ((line = r.readLine()) != null) {
                if (line.startsWith("timestamp") || line.isBlank()) continue;
                WinMetricRecord rec = WinMetricRecord.fromCsvLine(line);
                if (rec != null && rec.timestamp.isAfter(cutoff)) result.add(rec);
            }
        } catch (IOException e) {
            logger.log(Level.WARNING, "Failed to read WinMetricRecord CSV", e);
        }
        return result;
    }

    public synchronized void pruneOlderThan(int retentionDays) {
        if (!Files.exists(csvFile)) return;
        Instant cutoff = Instant.now().minus(retentionDays, ChronoUnit.DAYS);
        List<String> kept = new ArrayList<>();
        kept.add(HEADER);
        try (BufferedReader r = Files.newBufferedReader(csvFile, StandardCharsets.UTF_8)) {
            String line;
            while ((line = r.readLine()) != null) {
                if (line.startsWith("timestamp") || line.isBlank()) continue;
                WinMetricRecord rec = WinMetricRecord.fromCsvLine(line);
                if (rec != null && rec.timestamp.isAfter(cutoff)) kept.add(line);
            }
            Files.write(csvFile, kept, StandardCharsets.UTF_8, StandardOpenOption.TRUNCATE_EXISTING);
        } catch (IOException e) {
            logger.log(Level.WARNING, "Failed to prune WinMetricRecord CSV: " + e.getMessage(), e);
        }
    }
}
