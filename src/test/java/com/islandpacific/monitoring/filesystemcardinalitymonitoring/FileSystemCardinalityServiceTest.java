package com.islandpacific.monitoring.filesystemcardinalitymonitoring;

import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.testng.Assert.*;

public class FileSystemCardinalityServiceTest {

    @Mock
    private EmailService emailService;

    private Logger logger;
    private FileSystemCardinalityService service;
    private ConcurrentHashMap<String, ConcurrentHashMap<String, Integer>> totalFileCounts;
    private ConcurrentHashMap<String, ConcurrentHashMap<String, Integer>> newFileCounts;

    private AutoCloseable mocks;
    private Path tempDir;
    private Path stateDir;

    @BeforeMethod
    public void setUp() throws IOException {
        mocks = MockitoAnnotations.openMocks(this);
        logger = Logger.getLogger("test");
        totalFileCounts = new ConcurrentHashMap<>();
        newFileCounts = new ConcurrentHashMap<>();
        service = new FileSystemCardinalityService(logger, emailService, totalFileCounts, newFileCounts);

        tempDir = Files.createTempDirectory("fsc-test");
        stateDir = Files.createTempDirectory("fsc-state");
    }

    @AfterMethod
    public void tearDown() throws Exception {
        mocks.close();
        deleteRecursively(tempDir);
        deleteRecursively(stateDir);
    }

    // --- Alert windowing ---

    @Test
    public void alertSentOnlyOnThirdConsecutiveBreach_tooFew() throws IOException {
        FileSystemCardinalityConfig.MonitoringConfig config = buildConfig("loc1", tempDir, 5, 100, false);

        // 0 files in folder — below min=5
        // Cycle 1 and 2: no email
        service.monitorFolder(config);
        service.monitorFolder(config);
        verify(emailService, never()).sendEmail(anyString(), anyString(), anyString(), anyString(), anyString());

        // Cycle 3: email fires
        service.monitorFolder(config);
        verify(emailService, times(1)).sendEmail(eq("loc1"), anyString(), anyString(), anyString(), anyString());
    }

    @Test
    public void alertSentOnlyOnThirdConsecutiveBreach_tooMany() throws IOException {
        createFiles(tempDir, 3);
        FileSystemCardinalityConfig.MonitoringConfig config = buildConfig("loc2", tempDir, 0, 1, false);

        // 3 files, max=1
        service.monitorFolder(config);
        service.monitorFolder(config);
        verify(emailService, never()).sendEmail(anyString(), anyString(), anyString(), anyString(), anyString());

        service.monitorFolder(config);
        verify(emailService, times(1)).sendEmail(eq("loc2"), anyString(), anyString(), anyString(), anyString());
    }

    @Test
    public void breachCounterResets_whenFileCountReturnsToNormal() throws IOException {
        FileSystemCardinalityConfig.MonitoringConfig config = buildConfig("loc3", tempDir, 5, 100, false);

        // Two breach cycles
        service.monitorFolder(config);
        service.monitorFolder(config);

        // Add enough files to be in range
        createFiles(tempDir, 10);
        service.monitorFolder(config);

        // Remove files again — counter should have reset, so 3 more cycles needed
        deleteAllFiles(tempDir);
        service.monitorFolder(config);
        service.monitorFolder(config);
        verify(emailService, never()).sendEmail(anyString(), anyString(), anyString(), anyString(), anyString());

        service.monitorFolder(config);
        verify(emailService, times(1)).sendEmail(eq("loc3"), anyString(), anyString(), anyString(), anyString());
    }

    @Test
    public void noSecondAlertAfterWindowFires() throws IOException {
        FileSystemCardinalityConfig.MonitoringConfig config = buildConfig("loc4", tempDir, 5, 100, false);

        // Trigger the window (3 cycles)
        service.monitorFolder(config);
        service.monitorFolder(config);
        service.monitorFolder(config);
        verify(emailService, times(1)).sendEmail(anyString(), anyString(), anyString(), anyString(), anyString());

        // 4th, 5th cycle — counter is at 4, 5 — not equal to ALERT_WINDOW_SIZE (3) → no more emails
        service.monitorFolder(config);
        service.monitorFolder(config);
        verify(emailService, times(1)).sendEmail(anyString(), anyString(), anyString(), anyString(), anyString());
    }

    // --- Zero-file suppression ---

    @Test
    public void zeroFileAlert_suppressedWhenFlagSet() throws IOException {
        // Empty folder, ignoreZeroFileAlert=true, min=1
        FileSystemCardinalityConfig.MonitoringConfig config = buildConfig("loc5", tempDir, 1, 100, true);

        service.monitorFolder(config);
        service.monitorFolder(config);
        service.monitorFolder(config);
        verify(emailService, never()).sendEmail(anyString(), anyString(), anyString(), anyString(), anyString());
    }

    @Test
    public void zeroFileAlert_firedWhenFlagNotSet() throws IOException {
        FileSystemCardinalityConfig.MonitoringConfig config = buildConfig("loc6", tempDir, 1, 100, false);

        service.monitorFolder(config);
        service.monitorFolder(config);
        service.monitorFolder(config);
        verify(emailService, times(1)).sendEmail(eq("loc6"), anyString(), anyString(), anyString(), anyString());
    }

    // --- File extension filtering ---

    @Test
    public void extensionFilter_countsOnlyMatchingFiles() throws IOException {
        Files.createFile(tempDir.resolve("report.pdf"));
        Files.createFile(tempDir.resolve("data.csv"));
        Files.createFile(tempDir.resolve("notes.txt"));

        // Only .pdf files, max=0 means >0 triggers too-many
        Set<String> exts = Set.of(".pdf");
        FileSystemCardinalityConfig.MonitoringConfig config = buildConfigWithExtensions("loc7", tempDir, 0, 0, false, exts);

        // 1 PDF file found — exceeds max=0, so breach
        service.monitorFolder(config);
        service.monitorFolder(config);
        service.monitorFolder(config);
        verify(emailService, times(1)).sendEmail(eq("loc7"), anyString(), anyString(), anyString(), anyString());
    }

    @Test
    public void extensionFilter_noMatchMeansZeroCount() throws IOException {
        Files.createFile(tempDir.resolve("archive.zip"));

        // Looking for .pdf only — none present, count=0, min=0 max=10 → in range
        Set<String> exts = Set.of(".pdf");
        FileSystemCardinalityConfig.MonitoringConfig config = buildConfigWithExtensions("loc8", tempDir, 0, 10, false, exts);

        service.monitorFolder(config);
        service.monitorFolder(config);
        service.monitorFolder(config);
        verify(emailService, never()).sendEmail(anyString(), anyString(), anyString(), anyString(), anyString());
    }

    // --- File deduplication across cycles ---

    @Test
    public void newFileCount_notDoubleCountedAcrossCycles() throws IOException {
        Files.createFile(tempDir.resolve("existing.txt"));
        FileSystemCardinalityConfig.MonitoringConfig config = buildConfig("loc9", tempDir, 0, 100, false);

        service.monitorFolder(config);
        // 1 new file on first cycle
        assertEquals(newFileCounts.get("loc9").get("all").intValue(), 1);

        service.monitorFolder(config);
        // Same file already processed — no new files on second cycle
        assertEquals(newFileCounts.get("loc9").get("all").intValue(), 0);
    }

    @Test
    public void newFileDetected_afterFirstCycle() throws IOException {
        Files.createFile(tempDir.resolve("file1.txt"));
        FileSystemCardinalityConfig.MonitoringConfig config = buildConfig("loc10", tempDir, 0, 100, false);

        service.monitorFolder(config);
        assertEquals(newFileCounts.get("loc10").get("all").intValue(), 1);

        Files.createFile(tempDir.resolve("file2.txt"));
        service.monitorFolder(config);
        assertEquals(newFileCounts.get("loc10").get("all").intValue(), 1);
        assertEquals(totalFileCounts.get("loc10").get("all").intValue(), 2);
    }

    // --- Missing folder ---

    @Test
    public void missingFolder_doesNotThrow() throws IOException {
        Path nonExistent = tempDir.resolve("does-not-exist");
        FileSystemCardinalityConfig.MonitoringConfig config = buildConfig("loc11", nonExistent, 0, 100, false);

        // Should log warning and return without throwing
        service.monitorFolder(config);
        verify(emailService, never()).sendEmail(anyString(), anyString(), anyString(), anyString(), anyString());
    }

    // --- Helpers ---

    private FileSystemCardinalityConfig.MonitoringConfig buildConfig(
            String name, Path path, int min, int max, boolean ignoreZero) {
        return buildConfigWithExtensions(name, path, min, max, ignoreZero, Collections.emptySet());
    }

    private FileSystemCardinalityConfig.MonitoringConfig buildConfigWithExtensions(
            String name, Path path, int min, int max, boolean ignoreZero, Set<String> extensions) {
        FileSystemCardinalityConfig.MonitoringConfig config = new FileSystemCardinalityConfig.MonitoringConfig(
            name,
            path.toString(),
            min, max,
            "[%s] Too Few - %s",
            "Server %s path %s has %d files, min %d",
            "[%s] Too Many - %s",
            "Server %s path %s has %d files, max %d",
            "Normal",
            ignoreZero,
            "TestServer",
            logger,
            extensionsToFileTypesStr(extensions),
            false
        );
        config.setLocationLogger(logger);
        // Point state file into our temp state dir so tests don't pollute the working directory
        overrideStateFilePath(config, stateDir.resolve("state_" + name + ".txt").toString());
        return config;
    }

    private String extensionsToFileTypesStr(Set<String> extensions) {
        if (extensions == null || extensions.isEmpty()) return "";
        // Extensions arrive as ".pdf", config constructor prepends "." so strip the dot here
        StringBuilder sb = new StringBuilder();
        for (String ext : extensions) {
            if (sb.length() > 0) sb.append(",");
            sb.append(ext.startsWith(".") ? ext.substring(1) : ext);
        }
        return sb.toString();
    }

    private void overrideStateFilePath(FileSystemCardinalityConfig.MonitoringConfig config, String newPath) {
        // MonitoringConfig builds the state file path in the constructor; we override it via reflection
        // so tests don't write files into logs/processed/ in the working directory.
        try {
            var field = FileSystemCardinalityConfig.MonitoringConfig.class
                    .getDeclaredField("processedFilesStateFilePath");
            field.setAccessible(true);
            field.set(config, newPath);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException("Could not override state file path in test", e);
        }
    }

    private void createFiles(Path dir, int count) throws IOException {
        for (int i = 0; i < count; i++) {
            Files.createFile(dir.resolve("file" + i + ".txt"));
        }
    }

    private void deleteAllFiles(Path dir) throws IOException {
        try (var stream = Files.list(dir)) {
            for (Path p : stream.toList()) {
                if (Files.isRegularFile(p)) Files.delete(p);
            }
        }
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
