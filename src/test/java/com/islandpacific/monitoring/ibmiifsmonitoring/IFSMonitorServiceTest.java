package com.islandpacific.monitoring.ibmiifsmonitoring;

import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.testng.Assert.*;

public class IFSMonitorServiceTest {

    @Mock
    private EmailService emailService;

    private Logger logger;
    private ConcurrentHashMap<String, ConcurrentHashMap<String, Integer>> totalFileCounts;
    private ConcurrentHashMap<String, ConcurrentHashMap<String, Integer>> newFileCounts;

    private AutoCloseable mocks;
    private Path tempDir;
    private Path stateDir;

    @BeforeMethod
    public void setUp() throws IOException {
        mocks = MockitoAnnotations.openMocks(this);
        logger = Logger.getLogger("test.ifsmonitor");
        totalFileCounts = new ConcurrentHashMap<>();
        newFileCounts = new ConcurrentHashMap<>();
        tempDir = Files.createTempDirectory("ifsmon-test");
        stateDir = Files.createTempDirectory("ifsmon-state");
    }

    @AfterMethod
    public void tearDown() throws Exception {
        mocks.close();
        deleteRecursively(tempDir);
        deleteRecursively(stateDir);
    }

    // --- Below minimum threshold ---

    @Test
    public void fileCountBelowMin_triggersEmail() throws IOException {
        // 0 files, min=1
        IFSMonitorConfig.MonitoringConfig cfg = buildConfig("loc1", tempDir, 1, 100, false, "");
        buildService().monitorFolder(cfg);
        verify(emailService, times(1)).sendEmail(eq("loc1"), anyString(), anyString(), anyString(), anyString());
    }

    // --- Above maximum threshold ---

    @Test
    public void fileCountAboveMax_triggersEmail() throws IOException {
        createFiles(tempDir, 3);
        IFSMonitorConfig.MonitoringConfig cfg = buildConfig("loc2", tempDir, 0, 1, false, "");
        buildService().monitorFolder(cfg);
        verify(emailService, times(1)).sendEmail(eq("loc2"), anyString(), anyString(), anyString(), anyString());
    }

    // --- Within range — no email ---

    @Test
    public void fileCountInRange_noEmail() throws IOException {
        createFiles(tempDir, 2);
        IFSMonitorConfig.MonitoringConfig cfg = buildConfig("loc3", tempDir, 1, 5, false, "");
        buildService().monitorFolder(cfg);
        verify(emailService, never()).sendEmail(anyString(), anyString(), anyString(), anyString(), anyString());
    }

    // --- Zero file suppression ---

    @Test
    public void zeroFiles_suppressedWhenFlagSet() throws IOException {
        IFSMonitorConfig.MonitoringConfig cfg = buildConfig("loc4", tempDir, 1, 100, true, "");
        buildService().monitorFolder(cfg);
        verify(emailService, never()).sendEmail(anyString(), anyString(), anyString(), anyString(), anyString());
    }

    @Test
    public void zeroFiles_alertWhenFlagNotSet() throws IOException {
        IFSMonitorConfig.MonitoringConfig cfg = buildConfig("loc5", tempDir, 1, 100, false, "");
        buildService().monitorFolder(cfg);
        verify(emailService, times(1)).sendEmail(anyString(), anyString(), anyString(), anyString(), anyString());
    }

    // --- Extension filtering ---

    @Test
    public void extensionFilter_countsOnlyMatchingFiles() throws IOException {
        Files.createFile(tempDir.resolve("report.pdf"));
        Files.createFile(tempDir.resolve("notes.txt"));

        // Only .pdf, max=0 → 1 pdf > max=0 → email
        IFSMonitorConfig.MonitoringConfig cfg = buildConfig("loc6", tempDir, 0, 0, false, "pdf");
        buildService().monitorFolder(cfg);
        verify(emailService, times(1)).sendEmail(eq("loc6"), anyString(), anyString(), anyString(), anyString());
    }

    @Test
    public void extensionFilter_noMatchMeansZeroCount() throws IOException {
        Files.createFile(tempDir.resolve("data.zip"));

        // Only .pdf, none present → count=0, min=0 max=10 → in range
        IFSMonitorConfig.MonitoringConfig cfg = buildConfig("loc7", tempDir, 0, 10, false, "pdf");
        buildService().monitorFolder(cfg);
        verify(emailService, never()).sendEmail(anyString(), anyString(), anyString(), anyString(), anyString());
    }

    // --- Missing folder ---

    @Test
    public void missingFolder_doesNotThrowAndSendsNoAlert() throws IOException {
        Path missing = tempDir.resolve("ghost");
        IFSMonitorConfig.MonitoringConfig cfg = buildConfig("loc8", missing, 0, 100, false, "");
        buildService().monitorFolder(cfg);
        verify(emailService, never()).sendEmail(anyString(), anyString(), anyString(), anyString(), anyString());
    }

    // --- Counts ---

    @Test
    public void totalFileCount_updatedCorrectly() throws IOException {
        createFiles(tempDir, 3);
        IFSMonitorConfig.MonitoringConfig cfg = buildConfig("loc9", tempDir, 0, 100, false, "");
        buildService().monitorFolder(cfg);
        assertEquals(totalFileCounts.get("loc9").get("all").intValue(), 3);
    }

    @Test
    public void newFileCount_zeroOnSecondCycleNoNewFiles() throws IOException {
        Files.createFile(tempDir.resolve("a.txt"));
        IFSMonitorConfig.MonitoringConfig cfg = buildConfig("loc10", tempDir, 0, 100, false, "");
        IFSMonitorService svc = buildService();

        svc.monitorFolder(cfg);
        assertEquals(newFileCounts.get("loc10").get("all").intValue(), 1);

        svc.monitorFolder(cfg);
        assertEquals(newFileCounts.get("loc10").get("all").intValue(), 0);
    }

    // --- Helpers ---

    private IFSMonitorConfig.MonitoringConfig buildConfig(String name, Path path, int min, int max,
            boolean ignoreZero, String fileTypes) {
        IFSMonitorConfig.MonitoringConfig cfg = new IFSMonitorConfig.MonitoringConfig(
                name, path.toString(), min, max,
                "[%s] Too Few - %s", "Server %s path %s has %d files, min %d",
                "[%s] Too Many - %s", "Server %s path %s has %d files, max %d",
                "Normal", ignoreZero, "TestServer", logger, fileTypes);
        cfg.setLocationLogger(logger);
        overrideStateFilePath(cfg, stateDir.resolve("state_" + name + ".txt").toString());
        return cfg;
    }

    private IFSMonitorService buildService() {
        return new IFSMonitorService(logger, emailService, totalFileCounts, newFileCounts,
                new ConcurrentHashMap<>());
    }

    private void overrideStateFilePath(IFSMonitorConfig.MonitoringConfig cfg, String path) {
        try {
            var field = IFSMonitorConfig.MonitoringConfig.class.getDeclaredField("processedFilesStateFilePath");
            field.setAccessible(true);
            field.set(cfg, path);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

    private void createFiles(Path dir, int count) throws IOException {
        for (int i = 0; i < count; i++) {
            Files.createFile(dir.resolve("file" + i + ".txt"));
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
