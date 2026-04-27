package com.islandpacific.monitoring.filesystemerrormonitoring;

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

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.testng.Assert.*;

public class FileSystemErrorServiceTest {

    @Mock
    private EmailService emailService;

    private Logger logger;
    private ConcurrentHashMap<String, ConcurrentHashMap<String, Integer>> totalFileCounts;
    private ConcurrentHashMap<String, ConcurrentHashMap<String, Integer>> newFileCounts;
    private FileSystemErrorMetrics metrics;

    private AutoCloseable mocks;
    private Path tempDir;
    private Path stateDir;

    @BeforeMethod
    public void setUp() throws IOException {
        mocks = MockitoAnnotations.openMocks(this);
        logger = Logger.getLogger("test.fserror");
        totalFileCounts = new ConcurrentHashMap<>();
        newFileCounts = new ConcurrentHashMap<>();
        metrics = new FileSystemErrorMetrics(logger, totalFileCounts, newFileCounts);
        tempDir = Files.createTempDirectory("fse-test");
        stateDir = Files.createTempDirectory("fse-state");
    }

    @AfterMethod
    public void tearDown() throws Exception {
        mocks.close();
        deleteRecursively(tempDir);
        deleteRecursively(stateDir);
    }

    // --- New file detection ---

    @Test
    public void newErrorFile_triggersEmail() throws IOException {
        Files.createFile(tempDir.resolve("crash.err"));
        FileSystemErrorConfig.MonitoringConfig config = buildConfig("loc1", tempDir, "err");
        FileSystemErrorService service = buildService(List.of(config));

        service.checkNewFilesAndSendEmail();

        verify(emailService, times(1)).sendEmail(eq(config), argThat(m -> m.containsKey(".err")));
    }

    @Test
    public void noFiles_noEmail() throws IOException {
        FileSystemErrorConfig.MonitoringConfig config = buildConfig("loc2", tempDir, "err");
        FileSystemErrorService service = buildService(List.of(config));

        service.checkNewFilesAndSendEmail();

        verify(emailService, never()).sendEmail(any(), any());
    }

    // --- Deduplication across cycles ---

    @Test
    public void existingFile_notReportedAgainOnSecondCycle() throws IOException {
        Files.createFile(tempDir.resolve("old.err"));
        FileSystemErrorConfig.MonitoringConfig config = buildConfig("loc3", tempDir, "err");
        FileSystemErrorService service = buildService(List.of(config));

        service.checkNewFilesAndSendEmail();
        verify(emailService, times(1)).sendEmail(any(), any());

        // Second cycle — same file, no new email
        service.checkNewFilesAndSendEmail();
        verify(emailService, times(1)).sendEmail(any(), any());
    }

    @Test
    public void newFileAddedAfterFirstCycle_triggersSecondEmail() throws IOException {
        Files.createFile(tempDir.resolve("first.err"));
        FileSystemErrorConfig.MonitoringConfig config = buildConfig("loc4", tempDir, "err");
        FileSystemErrorService service = buildService(List.of(config));

        service.checkNewFilesAndSendEmail();
        verify(emailService, times(1)).sendEmail(any(), any());

        Files.createFile(tempDir.resolve("second.err"));
        service.checkNewFilesAndSendEmail();
        verify(emailService, times(2)).sendEmail(any(), any());
    }

    // --- Extension filtering ---

    @Test
    public void onlyConfiguredExtensionCounted() throws IOException {
        Files.createFile(tempDir.resolve("error.err"));
        Files.createFile(tempDir.resolve("report.pdf"));
        Files.createFile(tempDir.resolve("data.log"));

        FileSystemErrorConfig.MonitoringConfig config = buildConfig("loc5", tempDir, "err");
        FileSystemErrorService service = buildService(List.of(config));

        service.checkNewFilesAndSendEmail();

        // Only .err file should trigger an email — 1 email with 1 .err file
        verify(emailService, times(1)).sendEmail(eq(config), argThat(m ->
            m.containsKey(".err") && m.get(".err").size() == 1 && !m.containsKey(".pdf") && !m.containsKey(".log")
        ));
    }

    @Test
    public void multipleExtensions_bothReportedInSameEmail() throws IOException {
        Files.createFile(tempDir.resolve("crash.err"));
        Files.createFile(tempDir.resolve("alert.wrn"));

        FileSystemErrorConfig.MonitoringConfig config = buildConfig("loc6", tempDir, "err,wrn");
        FileSystemErrorService service = buildService(List.of(config));

        service.checkNewFilesAndSendEmail();

        verify(emailService, times(1)).sendEmail(eq(config), argThat(m ->
            m.containsKey(".err") && m.containsKey(".wrn")
        ));
    }

    // --- Total and new file counts ---

    @Test
    public void totalFileCount_reflectsCurrentFolderState() throws IOException {
        Files.createFile(tempDir.resolve("a.err"));
        Files.createFile(tempDir.resolve("b.err"));

        FileSystemErrorConfig.MonitoringConfig config = buildConfig("loc7", tempDir, "err");
        FileSystemErrorService service = buildService(List.of(config));

        service.checkNewFilesAndSendEmail();

        assertEquals(totalFileCounts.get("loc7").get(".err").intValue(), 2);
    }

    @Test
    public void newFileCount_zeroOnSecondCycleWithNoNewFiles() throws IOException {
        Files.createFile(tempDir.resolve("a.err"));
        FileSystemErrorConfig.MonitoringConfig config = buildConfig("loc8", tempDir, "err");
        FileSystemErrorService service = buildService(List.of(config));

        service.checkNewFilesAndSendEmail();
        assertEquals(newFileCounts.get("loc8").get(".err").intValue(), 1);

        service.checkNewFilesAndSendEmail();
        assertEquals(newFileCounts.get("loc8").get(".err").intValue(), 0);
    }

    // --- Missing folder ---

    @Test
    public void missingFolder_doesNotThrowAndSendsNoEmail() throws IOException {
        Path missing = tempDir.resolve("ghost");
        FileSystemErrorConfig.MonitoringConfig config = buildConfig("loc9", missing, "err");
        FileSystemErrorService service = buildService(List.of(config));

        service.checkNewFilesAndSendEmail();

        verify(emailService, never()).sendEmail(any(), any());
        // Counts initialised to 0 for configured extensions
        assertEquals(totalFileCounts.get("loc9").get(".err").intValue(), 0);
    }

    // --- Multiple locations ---

    @Test
    public void multipleLocations_eachGetsSeparateEmail() throws IOException {
        Path dir2 = Files.createTempDirectory("fse-test2");
        try {
            Files.createFile(tempDir.resolve("a.err"));
            Files.createFile(dir2.resolve("b.err"));

            FileSystemErrorConfig.MonitoringConfig cfg1 = buildConfig("locA", tempDir, "err");
            FileSystemErrorConfig.MonitoringConfig cfg2 = buildConfig("locB", dir2, "err");
            FileSystemErrorService service = buildService(List.of(cfg1, cfg2));

            service.checkNewFilesAndSendEmail();

            verify(emailService, times(1)).sendEmail(eq(cfg1), any());
            verify(emailService, times(1)).sendEmail(eq(cfg2), any());
        } finally {
            deleteRecursively(dir2);
        }
    }

    @Test
    public void locationWithNoNewFiles_doesNotSendEmail_whileOtherLocationDoes() throws IOException {
        Path dir2 = Files.createTempDirectory("fse-test3");
        try {
            // dir2 has a file, tempDir is empty
            Files.createFile(dir2.resolve("b.err"));

            FileSystemErrorConfig.MonitoringConfig cfg1 = buildConfig("locX", tempDir, "err");
            FileSystemErrorConfig.MonitoringConfig cfg2 = buildConfig("locY", dir2, "err");
            FileSystemErrorService service = buildService(List.of(cfg1, cfg2));

            service.checkNewFilesAndSendEmail();

            verify(emailService, never()).sendEmail(eq(cfg1), any());
            verify(emailService, times(1)).sendEmail(eq(cfg2), any());
        } finally {
            deleteRecursively(dir2);
        }
    }

    // --- Helpers ---

    private FileSystemErrorConfig.MonitoringConfig buildConfig(String name, Path path, String fileTypes) {
        FileSystemErrorConfig.MonitoringConfig config =
            new FileSystemErrorConfig.MonitoringConfig(name, path.toString(), fileTypes, "", "Normal", logger);
        config.setLocationLogger(logger);
        overrideStateFilePath(config, stateDir.resolve("state_" + name + ".txt").toString());
        return config;
    }

    private FileSystemErrorService buildService(List<FileSystemErrorConfig.MonitoringConfig> configs) {
        return new FileSystemErrorService(logger, configs, emailService, totalFileCounts, newFileCounts, metrics);
    }

    private void overrideStateFilePath(FileSystemErrorConfig.MonitoringConfig config, String newPath) {
        try {
            var field = FileSystemErrorConfig.MonitoringConfig.class.getDeclaredField("processedFilesStateFilePath");
            field.setAccessible(true);
            field.set(config, newPath);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException("Could not override state file path in test", e);
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
