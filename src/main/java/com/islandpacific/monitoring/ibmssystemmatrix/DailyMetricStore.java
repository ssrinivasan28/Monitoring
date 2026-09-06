package com.islandpacific.monitoring.ibmssystemmatrix;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class DailyMetricStore {

    private static final Logger logger = Logger.getLogger(DailyMetricStore.class.getName());
    private static final String HEADER = "timestamp,host,cpu,asp,pool,totalJobs,activeJobs";

    private final Path csvFile;

    public DailyMetricStore(String dataFolder) {
        Path dir = Paths.get(dataFolder);
        try {
            Files.createDirectories(dir);
        } catch (IOException e) {
            System.err.println("Could not create metric data folder: " + dir.toAbsolutePath() + " — " + e.getMessage());
            logger.log(Level.WARNING, "Could not create metric data folder: " + dataFolder, e);
        }
        this.csvFile = dir.resolve("ibmmatrix_metrics.csv");
    }

    public synchronized void append(MetricRecord record) {
        try {
            boolean writeHeader = !Files.exists(csvFile) || Files.size(csvFile) == 0;
            try (BufferedWriter w = Files.newBufferedWriter(csvFile, StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND)) {
                if (writeHeader) w.write(HEADER + "\n");
                w.write(record.toCsvLine() + "\n");
            }
        } catch (IOException e) {
            System.err.println("CSV WRITE FAILED: " + csvFile.toAbsolutePath() + " — " + e.getMessage());
            logger.log(Level.WARNING, "Failed to write metric record to CSV: " + e.getMessage(), e);
        }
    }

    public synchronized List<MetricRecord> readLast24Hours() {
        List<MetricRecord> result = new ArrayList<>();
        if (!Files.exists(csvFile)) return result;
        Instant cutoff = Instant.now().minus(24, ChronoUnit.HOURS);
        try (BufferedReader r = Files.newBufferedReader(csvFile, StandardCharsets.UTF_8)) {
            String line;
            while ((line = r.readLine()) != null) {
                if (line.startsWith("timestamp") || line.isBlank()) continue;
                MetricRecord rec = MetricRecord.fromCsvLine(line);
                if (rec != null && rec.timestamp.isAfter(cutoff)) {
                    result.add(rec);
                }
            }
        } catch (IOException e) {
            logger.log(Level.WARNING, "Failed to read metric CSV", e);
        }
        return result;
    }

    // Trim rows older than retentionDays to keep file small
    public synchronized void pruneOlderThan(int retentionDays) {
        if (!Files.exists(csvFile)) return;
        Instant cutoff = Instant.now().minus(retentionDays, ChronoUnit.DAYS);
        List<String> kept = new ArrayList<>();
        kept.add(HEADER);
        try (BufferedReader r = Files.newBufferedReader(csvFile, StandardCharsets.UTF_8)) {
            String line;
            while ((line = r.readLine()) != null) {
                if (line.startsWith("timestamp") || line.isBlank()) continue;
                MetricRecord rec = MetricRecord.fromCsvLine(line);
                if (rec != null && rec.timestamp.isAfter(cutoff)) kept.add(line);
            }
            Files.write(csvFile, kept, StandardCharsets.UTF_8, StandardOpenOption.TRUNCATE_EXISTING);
        } catch (IOException e) {
            logger.log(Level.WARNING, "Failed to prune metric CSV: " + e.getMessage(), e);
        }
    }
}
