package com.islandpacific.monitoring.logkeywordmonitoring;

import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;
import static org.testng.Assert.*;

public class LogKeywordMonitorServiceTest {

    @Mock
    private EmailService emailService;

    private Logger logger;
    private ConcurrentHashMap<String, ConcurrentHashMap<String, Long>> totalMatchCounts;
    private ConcurrentHashMap<String, ConcurrentHashMap<String, Long>> newMatchCounts;
    private ConcurrentHashMap<String, Long> linesScanned;
    private ConcurrentHashMap<String, Long> readErrors;
    private LogKeywordMonitorMetrics metrics;

    private AutoCloseable mocks;
    private Path tempDir;

    @BeforeMethod
    public void setUp() throws IOException {
        mocks = MockitoAnnotations.openMocks(this);
        logger = Logger.getLogger("test.logkeyword");
        totalMatchCounts = new ConcurrentHashMap<>();
        newMatchCounts = new ConcurrentHashMap<>();
        linesScanned = new ConcurrentHashMap<>();
        readErrors = new ConcurrentHashMap<>();
        metrics = new LogKeywordMonitorMetrics(logger, totalMatchCounts, newMatchCounts, linesScanned, readErrors);
        tempDir = Files.createTempDirectory("lkm-test");
    }

    @AfterMethod
    public void tearDown() throws Exception {
        mocks.close();
        deleteRecursively(tempDir);
    }

    // --- Basic keyword detection ---

    @Test
    public void keywordFound_triggersAlert() throws IOException {
        Path logFile = writeLog("app.log", "2024-01-01 ERROR Something went wrong");
        LogKeywordMonitorConfig.LogFileConfig config = buildConfig(logFile, List.of("ERROR"), false, false, false);
        LogKeywordMonitorService service = buildService(List.of(config));

        service.checkLogsAndSendAlerts();

        verify(emailService, times(1)).sendAlert(anyString(), anyString());
    }

    @Test
    public void noKeywordInLog_noAlert() throws IOException {
        Path logFile = writeLog("app.log", "2024-01-01 INFO Everything is fine");
        LogKeywordMonitorConfig.LogFileConfig config = buildConfig(logFile, List.of("ERROR"), false, false, false);
        LogKeywordMonitorService service = buildService(List.of(config));

        service.checkLogsAndSendAlerts();

        verify(emailService, never()).sendAlert(anyString(), anyString());
    }

    @Test
    public void emptyLogFile_noAlert() throws IOException {
        Path logFile = writeLog("empty.log", "");
        LogKeywordMonitorConfig.LogFileConfig config = buildConfig(logFile, List.of("ERROR"), false, false, false);
        LogKeywordMonitorService service = buildService(List.of(config));

        service.checkLogsAndSendAlerts();

        verify(emailService, never()).sendAlert(anyString(), anyString());
    }

    @Test
    public void missingLogFile_noAlert() throws IOException {
        Path missing = tempDir.resolve("nonexistent.log");
        LogKeywordMonitorConfig.LogFileConfig config = buildConfig(missing, List.of("ERROR"), false, false, false);
        LogKeywordMonitorService service = buildService(List.of(config));

        service.checkLogsAndSendAlerts();

        verify(emailService, never()).sendAlert(anyString(), anyString());
    }

    // --- Incremental reading (file position tracking) ---

    @Test
    public void secondCycle_onlyReadsNewLines() throws IOException {
        Path logFile = writeLog("app.log", "INFO normal line");
        LogKeywordMonitorConfig.LogFileConfig config = buildConfig(logFile, List.of("ERROR"), false, false, false);
        LogKeywordMonitorService service = buildService(List.of(config));

        // First cycle — no match
        service.checkLogsAndSendAlerts();
        verify(emailService, never()).sendAlert(anyString(), anyString());

        // Append an error line — only the new line should be read
        appendLog(logFile, "ERROR new problem");
        service.checkLogsAndSendAlerts();
        verify(emailService, times(1)).sendAlert(anyString(), anyString());
    }

    @Test
    public void alreadyMatchedLine_notRealertedOnNextCycle() throws IOException {
        Path logFile = writeLog("app.log", "ERROR existing problem");
        LogKeywordMonitorConfig.LogFileConfig config = buildConfig(logFile, List.of("ERROR"), false, false, false);
        LogKeywordMonitorService service = buildService(List.of(config));

        service.checkLogsAndSendAlerts();
        verify(emailService, times(1)).sendAlert(anyString(), anyString());

        // No new content — no second alert
        service.checkLogsAndSendAlerts();
        verify(emailService, times(1)).sendAlert(anyString(), anyString());
    }

    @Test
    public void fileRotation_readsFromBeginning() throws IOException {
        // Write a long first line so the recorded position is larger than the rotated file
        Path logFile = writeLog("app.log", "ERROR first error - this is a long line to push the byte position high enough");
        LogKeywordMonitorConfig.LogFileConfig config = buildConfig(logFile, List.of("ERROR"), false, false, false);
        LogKeywordMonitorService service = buildService(List.of(config));

        service.checkLogsAndSendAlerts();
        verify(emailService, times(1)).sendAlert(anyString(), anyString());

        // Simulate rotation: write a shorter file — currentSize < lastPosition triggers reset
        Files.writeString(logFile, "ERROR\n", StandardOpenOption.TRUNCATE_EXISTING);
        service.checkLogsAndSendAlerts();
        verify(emailService, times(2)).sendAlert(anyString(), anyString());
    }

    // --- Case sensitivity ---

    @Test
    public void caseInsensitive_matchesRegardlessOfCase() throws IOException {
        Path logFile = writeLog("app.log", "error lowercase");
        // keyword is "ERROR", caseSensitive=false
        LogKeywordMonitorConfig.LogFileConfig config = buildConfig(logFile, List.of("ERROR"), false, false, false);
        LogKeywordMonitorService service = buildService(List.of(config));

        service.checkLogsAndSendAlerts();

        verify(emailService, times(1)).sendAlert(anyString(), anyString());
    }

    @Test
    public void caseSensitive_doesNotMatchDifferentCase() throws IOException {
        Path logFile = writeLog("app.log", "error lowercase");
        // keyword is "ERROR", caseSensitive=true — "error" != "ERROR"
        LogKeywordMonitorConfig.LogFileConfig config = buildConfig(logFile, List.of("ERROR"), true, false, false);
        LogKeywordMonitorService service = buildService(List.of(config));

        service.checkLogsAndSendAlerts();

        verify(emailService, never()).sendAlert(anyString(), anyString());
    }

    // --- Regex mode ---

    @Test
    public void regexMode_matchesPattern() throws IOException {
        Path logFile = writeLog("app.log", "NullPointerException at line 42");
        LogKeywordMonitorConfig.LogFileConfig config = buildConfig(logFile, List.of(".*Exception.*"), false, true, false);
        LogKeywordMonitorService service = buildService(List.of(config));

        service.checkLogsAndSendAlerts();

        verify(emailService, times(1)).sendAlert(anyString(), anyString());
    }

    @Test
    public void regexMode_doesNotMatchWhenPatternMisses() throws IOException {
        Path logFile = writeLog("app.log", "INFO everything is fine");
        LogKeywordMonitorConfig.LogFileConfig config = buildConfig(logFile, List.of(".*Exception.*"), false, true, false);
        LogKeywordMonitorService service = buildService(List.of(config));

        service.checkLogsAndSendAlerts();

        verify(emailService, never()).sendAlert(anyString(), anyString());
    }

    // --- alertOnFirstMatch ---

    @Test
    public void alertOnFirstMatch_suppressesSubsequentAlertsForSameKeyword() throws IOException {
        Path logFile = writeLog("app.log", "ERROR first occurrence");
        LogKeywordMonitorConfig.LogFileConfig config = buildConfig(logFile, List.of("ERROR"), false, false, true);
        LogKeywordMonitorService service = buildService(List.of(config));

        service.checkLogsAndSendAlerts();
        verify(emailService, times(1)).sendAlert(anyString(), anyString());

        // New line with same keyword — alertOnFirstMatch suppresses it
        appendLog(logFile, "ERROR second occurrence");
        service.checkLogsAndSendAlerts();
        verify(emailService, times(1)).sendAlert(anyString(), anyString());
    }

    @Test
    public void alertOnFirstMatchFalse_alertsEveryNewMatch() throws IOException {
        Path logFile = writeLog("app.log", "ERROR first occurrence");
        LogKeywordMonitorConfig.LogFileConfig config = buildConfig(logFile, List.of("ERROR"), false, false, false);
        LogKeywordMonitorService service = buildService(List.of(config));

        service.checkLogsAndSendAlerts();
        verify(emailService, times(1)).sendAlert(anyString(), anyString());

        appendLog(logFile, "ERROR second occurrence");
        service.checkLogsAndSendAlerts();
        verify(emailService, times(2)).sendAlert(anyString(), anyString());
    }

    // --- Match counts ---

    @Test
    public void totalMatchCount_accumulatesAcrossCycles() throws IOException {
        Path logFile = writeLog("app.log", "ERROR first");
        String logPathStr = logFile.toAbsolutePath().toString();
        LogKeywordMonitorConfig.LogFileConfig config = buildConfig(logFile, List.of("ERROR"), false, false, false);
        LogKeywordMonitorService service = buildService(List.of(config));

        service.checkLogsAndSendAlerts();
        assertEquals(totalMatchCounts.get(logPathStr).get("ERROR").longValue(), 1L);

        appendLog(logFile, "ERROR second");
        service.checkLogsAndSendAlerts();
        assertEquals(totalMatchCounts.get(logPathStr).get("ERROR").longValue(), 2L);
    }

    @Test
    public void newMatchCount_resetsEachCycle() throws IOException {
        Path logFile = writeLog("app.log", "ERROR first");
        String logPathStr = logFile.toAbsolutePath().toString();
        LogKeywordMonitorConfig.LogFileConfig config = buildConfig(logFile, List.of("ERROR"), false, false, false);
        LogKeywordMonitorService service = buildService(List.of(config));

        service.checkLogsAndSendAlerts();
        assertEquals(newMatchCounts.get(logPathStr).get("ERROR").longValue(), 1L);

        // Second cycle with no new content — newMatchCounts cleared to 0 (map cleared)
        service.checkLogsAndSendAlerts();
        assertFalse(newMatchCounts.containsKey(logPathStr) && newMatchCounts.get(logPathStr).containsKey("ERROR"),
                "newMatchCounts should be cleared for this file on second cycle with no new data");
    }

    // --- Multiple keywords ---

    @Test
    public void multipleKeywords_allDetectedInOneAlert() throws IOException {
        Path logFile = writeLog("app.log", "ERROR and WARN on same line");
        LogKeywordMonitorConfig.LogFileConfig config = buildConfig(logFile, List.of("ERROR", "WARN"), false, false, false);
        LogKeywordMonitorService service = buildService(List.of(config));

        service.checkLogsAndSendAlerts();

        // One combined email, not two separate ones
        verify(emailService, times(1)).sendAlert(anyString(), argThat(body -> body.contains("ERROR") && body.contains("WARN")));
    }

    // --- Multiple configs ---

    @Test
    public void multipleLogFiles_independentlyMonitored() throws IOException {
        Path log1 = writeLog("app1.log", "ERROR in app1");
        Path log2 = writeLog("app2.log", "INFO nothing wrong");

        LogKeywordMonitorConfig.LogFileConfig cfg1 = buildConfig(log1, List.of("ERROR"), false, false, false);
        LogKeywordMonitorConfig.LogFileConfig cfg2 = buildConfig(log2, List.of("ERROR"), false, false, false);
        LogKeywordMonitorService service = buildService(List.of(cfg1, cfg2));

        service.checkLogsAndSendAlerts();

        // Only one alert — log2 had no match
        verify(emailService, times(1)).sendAlert(anyString(), anyString());
    }

    // --- Wildcard pattern ---

    @Test
    public void wildcardPattern_matchesMultipleFiles() throws IOException {
        writeLog("service-2024-01-01.log", "ERROR in first file");
        writeLog("service-2024-01-02.log", "ERROR in second file");

        // Pattern points to temp dir with wildcard
        String pattern = tempDir.toString() + java.io.File.separator + "service-*.log";
        LogKeywordMonitorConfig.LogFileConfig config =
            new LogKeywordMonitorConfig.LogFileConfig(pattern, "wildcard-test", List.of("ERROR"), false, false, false);
        LogKeywordMonitorService service = buildService(List.of(config));

        service.checkLogsAndSendAlerts();

        // Both files matched — one combined alert
        verify(emailService, times(1)).sendAlert(anyString(), anyString());
    }

    // --- Position persistence across restarts ---

    @Test
    public void serviceRestart_doesNotReAlertOnAlreadyProcessedContent() throws IOException {
        Path logFile = writeLog("app.log", "ERROR existing problem");
        LogKeywordMonitorConfig.LogFileConfig config = buildConfig(logFile, List.of("ERROR"), false, false, false);

        // First service instance — processes the file, sends alert, persists position
        LogKeywordMonitorService first = buildService(List.of(config));
        first.checkLogsAndSendAlerts();
        verify(emailService, times(1)).sendAlert(anyString(), anyString());

        // Simulate restart — new service instance, same state file, same log file (no new content)
        LogKeywordMonitorService restarted = buildService(List.of(config));
        restarted.checkLogsAndSendAlerts();

        // Must NOT send a second alert — position was restored from disk
        verify(emailService, times(1)).sendAlert(anyString(), anyString());
    }

    @Test
    public void serviceRestart_alertsOnNewContentWrittenAfterRestart() throws IOException {
        Path logFile = writeLog("app.log", "ERROR before restart");
        LogKeywordMonitorConfig.LogFileConfig config = buildConfig(logFile, List.of("ERROR"), false, false, false);

        LogKeywordMonitorService first = buildService(List.of(config));
        first.checkLogsAndSendAlerts();
        verify(emailService, times(1)).sendAlert(anyString(), anyString());

        // New content appended after restart
        appendLog(logFile, "ERROR after restart");

        LogKeywordMonitorService restarted = buildService(List.of(config));
        restarted.checkLogsAndSendAlerts();

        // Second alert fires — new content only
        verify(emailService, times(2)).sendAlert(anyString(), anyString());
    }

    // --- Helpers ---

    private LogKeywordMonitorConfig.LogFileConfig buildConfig(Path path, List<String> keywords,
            boolean caseSensitive, boolean regexMode, boolean alertOnFirstMatch) {
        return new LogKeywordMonitorConfig.LogFileConfig(
            path.toAbsolutePath().toString(), path.getFileName().toString(),
            keywords, caseSensitive, regexMode, alertOnFirstMatch
        );
    }

    private LogKeywordMonitorService buildService(List<LogKeywordMonitorConfig.LogFileConfig> configs) {
        return new LogKeywordMonitorService(logger, configs, emailService,
            totalMatchCounts, newMatchCounts, linesScanned, readErrors, metrics,
            tempDir.resolve("lkm_positions.properties").toString());
    }

    private Path writeLog(String filename, String content) throws IOException {
        Path file = tempDir.resolve(filename);
        Files.writeString(file, content + "\n", StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        return file;
    }

    private void appendLog(Path file, String content) throws IOException {
        Files.writeString(file, "\n" + content + "\n", StandardOpenOption.APPEND);
    }

    private void deleteRecursively(Path dir) throws IOException {
        if (dir == null || !Files.exists(dir)) return;
        try (var stream = Files.walk(dir)) {
            stream.sorted(java.util.Comparator.reverseOrder())
                  .map(Path::toFile)
                  .forEach(java.io.File::delete);
        }
    }
}
