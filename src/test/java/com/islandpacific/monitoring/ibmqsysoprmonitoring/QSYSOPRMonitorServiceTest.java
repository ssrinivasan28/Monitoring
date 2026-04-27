package com.islandpacific.monitoring.ibmqsysoprmonitoring;

import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Timestamp;
import java.util.Set;

import static org.testng.Assert.*;

public class QSYSOPRMonitorServiceTest {

    @Mock
    private EmailService emailService;

    private AutoCloseable mocks;
    private Path tempDir;
    private Path stateFile;

    @BeforeMethod
    public void setUp() throws IOException {
        mocks = MockitoAnnotations.openMocks(this);
        tempDir = Files.createTempDirectory("qsysopr-test");
        stateFile = tempDir.resolve("last_checked.txt");
    }

    @AfterMethod
    public void tearDown() throws Exception {
        mocks.close();
        deleteRecursively(tempDir);
    }

    // --- isJobFailureMessage — via reflection (private method) ---

    @Test
    public void isJobFailureMessage_matchesById() throws Exception {
        QSYSOPRMonitorService svc = buildService(Set.of("CPF9897"), Set.of());
        MessageInfo msg = new MessageInfo("CPF9897", "Some text", null, "JOB1", "USER1");
        assertTrue(invokeIsJobFailure(svc, msg));
    }

    @Test
    public void isJobFailureMessage_matchesByKeyword() throws Exception {
        QSYSOPRMonitorService svc = buildService(Set.of(), Set.of("ABNORMAL"));
        MessageInfo msg = new MessageInfo("MSGX", "Job ended abnormal termination", null, "JOB1", "USER1");
        assertTrue(invokeIsJobFailure(svc, msg));
    }

    @Test
    public void isJobFailureMessage_keywordMatchCaseInsensitive() throws Exception {
        QSYSOPRMonitorService svc = buildService(Set.of(), Set.of("FAILED"));
        MessageInfo msg = new MessageInfo("MSGX", "job failed due to error", null, "JOB", "USER");
        assertTrue(invokeIsJobFailure(svc, msg));
    }

    @Test
    public void isJobFailureMessage_noMatch_returnsFalse() throws Exception {
        QSYSOPRMonitorService svc = buildService(Set.of("CPF1234"), Set.of("CRASH"));
        MessageInfo msg = new MessageInfo("INFO001", "Everything is fine", null, "JOB", "USER");
        assertFalse(invokeIsJobFailure(svc, msg));
    }

    @Test
    public void isJobFailureMessage_nullMsgId_matchesByKeyword() throws Exception {
        QSYSOPRMonitorService svc = buildService(Set.of("CPF9897"), Set.of("ERROR"));
        MessageInfo msg = new MessageInfo(null, "Critical ERROR detected", null, "JOB", "USER");
        assertTrue(invokeIsJobFailure(svc, msg));
    }

    @Test
    public void isJobFailureMessage_nullText_doesNotThrow() throws Exception {
        QSYSOPRMonitorService svc = buildService(Set.of("CPF9897"), Set.of("ERROR"));
        MessageInfo msg = new MessageInfo("OTHER", null, null, "JOB", "USER");
        assertFalse(invokeIsJobFailure(svc, msg));
    }

    // --- loadLastCheckedTimestamp / saveLastCheckedTimestamp ---

    @Test
    public void saveAndLoadTimestamp_roundTrip() throws IOException {
        QSYSOPRMonitorService svc = buildService(Set.of(), Set.of());
        String ts = "2026-04-27 10:30:00.0";
        svc.saveLastCheckedTimestamp(ts);
        assertEquals(svc.loadLastCheckedTimestamp(), ts);
    }

    @Test
    public void loadLastCheckedTimestamp_missingFile_returnsNull() throws IOException {
        QSYSOPRMonitorService svc = buildService(Set.of(), Set.of());
        // stateFile hasn't been written yet
        assertNull(svc.loadLastCheckedTimestamp());
    }

    @Test
    public void saveLastCheckedTimestamp_nullValue_writesNothing() throws IOException {
        QSYSOPRMonitorService svc = buildService(Set.of(), Set.of());
        svc.saveLastCheckedTimestamp(null);
        // File may be created but empty — load should return null
        assertNull(svc.loadLastCheckedTimestamp());
    }

    // --- MessageInfo ---

    @Test
    public void messageInfo_storesAllFields() {
        Timestamp ts = new Timestamp(System.currentTimeMillis());
        MessageInfo msg = new MessageInfo("CPF1234", "text", ts, "MYJOB", "MYUSER");
        assertEquals(msg.getMessageId(), "CPF1234");
        assertEquals(msg.getMessageText(), "text");
        assertEquals(msg.getMessageTimestamp(), ts);
        assertEquals(msg.getFromJob(), "MYJOB");
        assertEquals(msg.getFromUser(), "MYUSER");
    }

    // --- Helpers ---

    private QSYSOPRMonitorService buildService(Set<String> ids, Set<String> keywords) {
        QSYSOPRMonitorConfig config = buildConfig(ids, keywords);
        return new QSYSOPRMonitorService(config, emailService);
    }

    private QSYSOPRMonitorConfig buildConfig(Set<String> ids, Set<String> keywords) {
        return new QSYSOPRMonitorConfig() {
            @Override public Set<String> getJobFailureMessageIds() { return ids; }
            @Override public Set<String> getJobFailureKeywords() { return keywords; }
            @Override public String getStateFileName() { return stateFile.toString(); }
            @Override public String getClientName() { return "TestClient"; }
            @Override public String getDbUrl() { return "jdbc:test://localhost"; }
            @Override public String getDbUsername() { return "user"; }
            @Override public String getDbPassword() { return "pass"; }
        };
    }

    private boolean invokeIsJobFailure(QSYSOPRMonitorService svc, MessageInfo msg) throws Exception {
        Method m = QSYSOPRMonitorService.class.getDeclaredMethod("isJobFailureMessage", MessageInfo.class);
        m.setAccessible(true);
        return (boolean) m.invoke(svc, msg);
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
