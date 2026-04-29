package com.islandpacific.monitoring.sharefilemonitoring;

import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.logging.Logger;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.testng.Assert.*;

public class ShareFileMonitorServiceTest {

    @Mock
    private EmailService emailService;

    private Logger logger;
    private ShareFileMonitorService service;
    private AutoCloseable mocks;

    private static final int WINDOW = 3;

    @BeforeMethod
    public void setUp() {
        mocks = MockitoAnnotations.openMocks(this);
        logger = Logger.getLogger("test");
        service = new ShareFileMonitorService(logger, emailService, "host", "user", "pass", WINDOW);
    }

    @AfterMethod
    public void tearDown() throws Exception {
        mocks.close();
    }

    // --- Alert windowing: too many ---

    @Test
    public void tooMany_emailOnlyOnWindowBreach() {
        ShareFileMonitorConfig.FolderConfig cfg = buildConfig("F1", 0, 2);

        // 3 files, max=2 → breach
        service.evaluateThreshold(cfg, 3, "F1", "/path", 0, 2);
        service.evaluateThreshold(cfg, 3, "F1", "/path", 0, 2);
        verify(emailService, never()).sendEmail(anyString(), anyString(), anyString(), anyString(), anyString());

        service.evaluateThreshold(cfg, 3, "F1", "/path", 0, 2);
        verify(emailService, times(1)).sendEmail(eq("F1"), anyString(), anyString(), anyString(), anyString());
    }

    @Test
    public void tooMany_noSecondEmailAfterWindowFires() {
        ShareFileMonitorConfig.FolderConfig cfg = buildConfig("F2", 0, 2);

        service.evaluateThreshold(cfg, 3, "F2", "/path", 0, 2);
        service.evaluateThreshold(cfg, 3, "F2", "/path", 0, 2);
        service.evaluateThreshold(cfg, 3, "F2", "/path", 0, 2);
        verify(emailService, times(1)).sendEmail(anyString(), anyString(), anyString(), anyString(), anyString());

        // 4th, 5th — counter > WINDOW, no repeat
        service.evaluateThreshold(cfg, 3, "F2", "/path", 0, 2);
        service.evaluateThreshold(cfg, 3, "F2", "/path", 0, 2);
        verify(emailService, times(1)).sendEmail(anyString(), anyString(), anyString(), anyString(), anyString());
    }

    // --- Alert windowing: too few ---

    @Test
    public void tooFew_emailOnlyOnWindowBreach() {
        ShareFileMonitorConfig.FolderConfig cfg = buildConfig("F3", 5, 100);

        // 0 files, min=5
        service.evaluateThreshold(cfg, 0, "F3", "/path", 5, 100);
        service.evaluateThreshold(cfg, 0, "F3", "/path", 5, 100);
        verify(emailService, never()).sendEmail(anyString(), anyString(), anyString(), anyString(), anyString());

        service.evaluateThreshold(cfg, 0, "F3", "/path", 5, 100);
        verify(emailService, times(1)).sendEmail(eq("F3"), anyString(), anyString(), anyString(), anyString());
    }

    // --- Counter reset ---

    @Test
    public void breachCounterResets_whenBackInRange() {
        ShareFileMonitorConfig.FolderConfig cfg = buildConfig("F4", 0, 2);

        service.evaluateThreshold(cfg, 3, "F4", "/path", 0, 2);
        service.evaluateThreshold(cfg, 3, "F4", "/path", 0, 2);

        // Back in range — reset
        service.evaluateThreshold(cfg, 1, "F4", "/path", 0, 2);

        // Breach again — needs full window before next email
        service.evaluateThreshold(cfg, 3, "F4", "/path", 0, 2);
        service.evaluateThreshold(cfg, 3, "F4", "/path", 0, 2);
        verify(emailService, never()).sendEmail(anyString(), anyString(), anyString(), anyString(), anyString());

        service.evaluateThreshold(cfg, 3, "F4", "/path", 0, 2);
        verify(emailService, times(1)).sendEmail(eq("F4"), anyString(), anyString(), anyString(), anyString());
    }

    // --- Zero file suppression ---

    @Test
    public void zeroFileAlert_suppressedWhenFlagSet() {
        ShareFileMonitorConfig.FolderConfig cfg = buildConfigIgnoreZero("F5", 1, 100, true);

        service.evaluateThreshold(cfg, 0, "F5", "/path", 1, 100);
        service.evaluateThreshold(cfg, 0, "F5", "/path", 1, 100);
        service.evaluateThreshold(cfg, 0, "F5", "/path", 1, 100);
        verify(emailService, never()).sendEmail(anyString(), anyString(), anyString(), anyString(), anyString());
    }

    @Test
    public void zeroFileAlert_firedWhenFlagNotSet() {
        ShareFileMonitorConfig.FolderConfig cfg = buildConfig("F6", 1, 100);

        service.evaluateThreshold(cfg, 0, "F6", "/path", 1, 100);
        service.evaluateThreshold(cfg, 0, "F6", "/path", 1, 100);
        service.evaluateThreshold(cfg, 0, "F6", "/path", 1, 100);
        verify(emailService, times(1)).sendEmail(eq("F6"), anyString(), anyString(), anyString(), anyString());
    }

    // --- In range: no alert ---

    @Test
    public void inRange_noAlert() {
        ShareFileMonitorConfig.FolderConfig cfg = buildConfig("F7", 0, 10);

        service.evaluateThreshold(cfg, 5, "F7", "/path", 0, 10);
        service.evaluateThreshold(cfg, 5, "F7", "/path", 0, 10);
        service.evaluateThreshold(cfg, 5, "F7", "/path", 0, 10);
        verify(emailService, never()).sendEmail(anyString(), anyString(), anyString(), anyString(), anyString());
    }

    // --- Email content ---

    @Test
    public void tooMany_emailContainsStatus() {
        ShareFileMonitorConfig.FolderConfig cfg = buildConfig("F8", 0, 2);

        service.evaluateThreshold(cfg, 5, "F8", "/OCFTP/TestFolder", 0, 2);
        service.evaluateThreshold(cfg, 5, "F8", "/OCFTP/TestFolder", 0, 2);
        service.evaluateThreshold(cfg, 5, "F8", "/OCFTP/TestFolder", 0, 2);

        verify(emailService).sendEmail(
                eq("F8"),
                eq("/OCFTP/TestFolder"),
                anyString(),
                eq("5 file(s) detected - exceeds maximum threshold of 2"),
                eq("Normal")
        );
    }

    @Test
    public void tooFew_emailContainsStatus() {
        ShareFileMonitorConfig.FolderConfig cfg = buildConfig("F9", 5, 100);

        service.evaluateThreshold(cfg, 2, "F9", "/OCFTP/TestFolder", 5, 100);
        service.evaluateThreshold(cfg, 2, "F9", "/OCFTP/TestFolder", 5, 100);
        service.evaluateThreshold(cfg, 2, "F9", "/OCFTP/TestFolder", 5, 100);

        verify(emailService).sendEmail(
                eq("F9"),
                eq("/OCFTP/TestFolder"),
                anyString(),
                eq("2 file(s) detected - below minimum threshold of 5"),
                eq("High")
        );
    }

    // --- Helpers ---

    private ShareFileMonitorConfig.FolderConfig buildConfig(String name, int min, int max) {
        return buildConfigIgnoreZero(name, min, max, false);
    }

    private ShareFileMonitorConfig.FolderConfig buildConfigIgnoreZero(String name, int min, int max, boolean ignoreZero) {
        return new ShareFileMonitorConfig.FolderConfig(name, "/path", min, max, ignoreZero, "TestClient");
    }
}
