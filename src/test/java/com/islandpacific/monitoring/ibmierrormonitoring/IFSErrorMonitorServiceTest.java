package com.islandpacific.monitoring.ibmierrormonitoring;

import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.testng.Assert.*;

public class IFSErrorMonitorServiceTest {

    @Mock
    private EmailService emailService;

    private Logger logger;
    private ConcurrentHashMap<String, ConcurrentHashMap<String, Integer>> totalFileCounts;
    private ConcurrentHashMap<String, ConcurrentHashMap<String, Integer>> newFileCounts;
    private IFSErrorMonitorMetrics metrics;

    private AutoCloseable mocks;
    private Path tempDir;
    private Path stateDir;

    @BeforeMethod
    public void setUp() throws IOException {
        mocks = MockitoAnnotations.openMocks(this);
        logger = Logger.getLogger("test.ifserror");
        totalFileCounts = new ConcurrentHashMap<>();
        newFileCounts = new ConcurrentHashMap<>();
        metrics = new IFSErrorMonitorMetrics(logger, totalFileCounts, newFileCounts);
        tempDir = Files.createTempDirectory("ifserr-test");
        stateDir = Files.createTempDirectory("ifserr-state");
    }

    @AfterMethod
    public void tearDown() throws Exception {
        mocks.close();
        deleteRecursively(tempDir);
        deleteRecursively(stateDir);
    }

    // --- New file triggers email ---

    @Test
    public void newErrorFile_localPath_triggersEmail() throws IOException {
        Files.createFile(tempDir.resolve("crash.err"));
        IFSErrorMonitorConfig.MonitoringConfig cfg = buildConfig("loc1", tempDir, "err");
        buildService(List.of(cfg)).checkNewFilesAndSendEmail();
        verify(emailService, times(1)).sendEmail(eq(cfg), any());
    }

    @Test
    public void noFiles_noEmail() throws IOException {
        IFSErrorMonitorConfig.MonitoringConfig cfg = buildConfig("loc2", tempDir, "err");
        buildService(List.of(cfg)).checkNewFilesAndSendEmail();
        verify(emailService, never()).sendEmail(any(), any());
    }

    // --- Deduplication ---

    @Test
    public void existingFile_notReportedAgainOnSecondCycle() throws IOException {
        Files.createFile(tempDir.resolve("old.err"));
        IFSErrorMonitorConfig.MonitoringConfig cfg = buildConfig("loc3", tempDir, "err");
        IFSErrorMonitorService svc = buildService(List.of(cfg));

        svc.checkNewFilesAndSendEmail();
        verify(emailService, times(1)).sendEmail(any(), any());

        svc.checkNewFilesAndSendEmail();
        verify(emailService, times(1)).sendEmail(any(), any()); // no second call
    }

    @Test
    public void newFileAfterFirstCycle_triggersSecondEmail() throws IOException {
        Files.createFile(tempDir.resolve("first.err"));
        IFSErrorMonitorConfig.MonitoringConfig cfg = buildConfig("loc4", tempDir, "err");
        IFSErrorMonitorService svc = buildService(List.of(cfg));

        svc.checkNewFilesAndSendEmail();
        verify(emailService, times(1)).sendEmail(any(), any());

        Files.createFile(tempDir.resolve("second.err"));
        svc.checkNewFilesAndSendEmail();
        verify(emailService, times(2)).sendEmail(any(), any());
    }

    // --- Extension filtering ---

    @Test
    public void onlyConfiguredExtension_counted() throws IOException {
        Files.createFile(tempDir.resolve("error.err"));
        Files.createFile(tempDir.resolve("report.pdf"));

        IFSErrorMonitorConfig.MonitoringConfig cfg = buildConfig("loc5", tempDir, "err");
        buildService(List.of(cfg)).checkNewFilesAndSendEmail();

        verify(emailService, times(1)).sendEmail(eq(cfg), argThat(m ->
                m.containsKey(".err") && !m.containsKey(".pdf")));
    }

    // --- Missing folder ---

    @Test
    public void missingFolder_doesNotThrowAndSendsNoEmail() throws IOException {
        Path missing = tempDir.resolve("ghost");
        IFSErrorMonitorConfig.MonitoringConfig cfg = buildConfig("loc6", missing, "err");
        buildService(List.of(cfg)).checkNewFilesAndSendEmail();
        verify(emailService, never()).sendEmail(any(), any());
    }

    // --- Counts ---

    @Test
    public void totalFileCount_reflectsCurrentFiles() throws IOException {
        Files.createFile(tempDir.resolve("a.err"));
        Files.createFile(tempDir.resolve("b.err"));
        IFSErrorMonitorConfig.MonitoringConfig cfg = buildConfig("loc7", tempDir, "err");
        buildService(List.of(cfg)).checkNewFilesAndSendEmail();
        assertEquals(totalFileCounts.get("loc7").get(".err").intValue(), 2);
    }

    @Test
    public void newFileCount_zeroOnSecondCycleWithNoNewFiles() throws IOException {
        Files.createFile(tempDir.resolve("a.err"));
        IFSErrorMonitorConfig.MonitoringConfig cfg = buildConfig("loc8", tempDir, "err");
        IFSErrorMonitorService svc = buildService(List.of(cfg));

        svc.checkNewFilesAndSendEmail();
        assertEquals(newFileCounts.get("loc8").get(".err").intValue(), 1);

        svc.checkNewFilesAndSendEmail();
        assertEquals(newFileCounts.get("loc8").get(".err").intValue(), 0);
    }

    // --- Helpers ---

    private IFSErrorMonitorConfig.MonitoringConfig buildConfig(String name, Path path, String fileTypes) {
        IFSErrorMonitorConfig.MonitoringConfig cfg =
                new IFSErrorMonitorConfig.MonitoringConfig(name, path.toString(), fileTypes, "", "Normal", logger);
        cfg.setLocationLogger(logger);
        overrideStateFilePath(cfg, stateDir.resolve("state_" + name + ".txt").toString());
        return cfg;
    }

    private IFSErrorMonitorService buildService(List<IFSErrorMonitorConfig.MonitoringConfig> configs) {
        return new IFSErrorMonitorService(logger, configs, new ConcurrentHashMap<>(),
                emailService, totalFileCounts, newFileCounts, metrics);
    }

    private void overrideStateFilePath(IFSErrorMonitorConfig.MonitoringConfig cfg, String path) {
        try {
            var field = IFSErrorMonitorConfig.MonitoringConfig.class.getDeclaredField("processedFilesStateFilePath");
            field.setAccessible(true);
            field.set(cfg, path);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
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
