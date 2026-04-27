package com.islandpacific.monitoring.userprofilechecker;

import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;

import static org.testng.Assert.*;

public class UserProfileCheckerTest {

    private Path tempDir;
    private String originalSnapshotFile;
    private Path snapshotFile;

    @BeforeMethod
    public void setUp() throws Exception {
        tempDir = Files.createTempDirectory("user-profile-checker-test");
        snapshotFile = tempDir.resolve("disabled_snapshot.txt");
        originalSnapshotFile = System.getProperty("userprofilechecker.snapshot.file");
        System.setProperty("userprofilechecker.snapshot.file", snapshotFile.toString());
    }

    @AfterMethod
    public void tearDown() throws Exception {
        if (originalSnapshotFile == null) {
            System.clearProperty("userprofilechecker.snapshot.file");
        } else {
            System.setProperty("userprofilechecker.snapshot.file", originalSnapshotFile);
        }
        deleteRecursively(tempDir);
    }

    @Test
    public void snapshotRoundTrip_preservesPipeCharactersInDescription() throws Exception {
        Map<String, String> snapshot = new LinkedHashMap<>();
        snapshot.put("USR001", "Disabled | pending review");

        invokeSaveSnapshot(snapshot);
        Map<String, String> loaded = invokeLoadSnapshot();

        assertEquals(loaded.get("USR001"), "Disabled | pending review");
    }

    @Test
    public void snapshotLoader_readsLegacyPipeDelimitedFile() throws Exception {
        Files.writeString(snapshotFile, "USR002|Legacy Description");

        Map<String, String> loaded = invokeLoadSnapshot();

        assertEquals(loaded.get("USR002"), "Legacy Description");
    }

    @Test
    public void emailHtml_escapesDisabledProfileData() throws Exception {
        EmailService service = new EmailService(new Properties(), "SMTP", null, null, null);
        Map<String, String> disabledUsers = Map.of("USR<script>", "A&B <Owner>");

        String html = invokeBuildDisabledAlertHtml(service, disabledUsers, "2026-04-27 <now>", "SYS&1", false);

        assertTrue(html.contains("USR&lt;script&gt;"));
        assertTrue(html.contains("A&amp;B &lt;Owner&gt;"));
        assertTrue(html.contains("SYS&amp;1"));
        assertTrue(html.contains("2026-04-27 &lt;now&gt;"));
        assertFalse(html.contains("USR<script>"));
        assertFalse(html.contains("A&B <Owner>"));
    }

    @SuppressWarnings("unchecked")
    private Map<String, String> invokeLoadSnapshot() throws Exception {
        Method method = MainUserProfileChecker.class.getDeclaredMethod("loadSnapshot");
        method.setAccessible(true);
        return (Map<String, String>) method.invoke(null);
    }

    private void invokeSaveSnapshot(Map<String, String> snapshot) throws Exception {
        Method method = MainUserProfileChecker.class.getDeclaredMethod("saveSnapshot", Map.class);
        method.setAccessible(true);
        method.invoke(null, snapshot);
    }

    private String invokeBuildDisabledAlertHtml(EmailService service, Map<String, String> disabledUsers,
            String eventTimestamp, String systemName, boolean embedLogoAsDataUri) throws Exception {
        Method method = EmailService.class.getDeclaredMethod("buildDisabledAlertHtml",
                Map.class, String.class, String.class, String.class, boolean.class);
        method.setAccessible(true);
        return (String) method.invoke(service, disabledUsers, eventTimestamp, systemName, "", embedLogoAsDataUri);
    }

    private void deleteRecursively(Path path) throws Exception {
        if (path == null || !Files.exists(path)) {
            return;
        }
        try (var paths = Files.walk(path)) {
            paths.sorted((a, b) -> b.compareTo(a)).forEach(p -> {
                try {
                    Files.deleteIfExists(p);
                } catch (Exception ignored) {
                }
            });
        }
    }
}
