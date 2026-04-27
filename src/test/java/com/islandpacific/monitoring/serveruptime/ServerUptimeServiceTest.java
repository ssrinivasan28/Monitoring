package com.islandpacific.monitoring.serveruptime;

import io.prometheus.client.CollectorRegistry;
import io.prometheus.client.Gauge;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

import static org.mockito.Mockito.*;
import static org.testng.Assert.*;

public class ServerUptimeServiceTest {

    @Mock
    private EmailService emailService;

    private Logger logger;
    private CollectorRegistry registry;
    private Gauge gauge;
    private Map<String, Boolean> currentStatus;

    private AutoCloseable mocks;

    @BeforeMethod
    public void setUp() {
        mocks = MockitoAnnotations.openMocks(this);
        logger = Logger.getLogger("test.serveruptime");
        // Use a fresh isolated registry per test to avoid Gauge name collisions
        registry = new CollectorRegistry();
        gauge = Gauge.build()
                .name("server_status_test")
                .help("test gauge")
                .labelNames("server")
                .register(registry);
        currentStatus = new ConcurrentHashMap<>();
    }

    @AfterMethod
    public void tearDown() throws Exception {
        mocks.close();
    }

    // --- initializeStatus ---

    @Test
    public void initializeStatus_upServer_noAlert() {
        ServerUptimeService service = buildService(List.of("host-a"), Map.of("host-a", true));

        service.initializeStatus();

        verify(emailService, never()).sendServerStatusAlert(anyString(), anyBoolean());
        assertEquals(currentStatus.get("host-a"), Boolean.TRUE);
        assertEquals(registry.getSampleValue("server_status_test", new String[]{"server"}, new String[]{"host-a"}), 1.0);
    }

    @Test
    public void initializeStatus_downServer_sendsDownAlert() {
        ServerUptimeService service = buildService(List.of("host-b"), Map.of("host-b", false));

        service.initializeStatus();

        verify(emailService, times(1)).sendServerStatusAlert("host-b", false);
        assertEquals(currentStatus.get("host-b"), Boolean.FALSE);
        assertEquals(registry.getSampleValue("server_status_test", new String[]{"server"}, new String[]{"host-b"}), 0.0);
    }

    @Test
    public void initializeStatus_multipleServers_alertOnlyDownOnes() {
        ServerUptimeService service = buildService(
            List.of("up-host", "down-host"),
            Map.of("up-host", true, "down-host", false)
        );

        service.initializeStatus();

        verify(emailService, never()).sendServerStatusAlert(eq("up-host"), anyBoolean());
        verify(emailService, times(1)).sendServerStatusAlert("down-host", false);
    }

    // --- checkAndAlert: no change ---

    @Test
    public void checkAndAlert_statusUnchanged_noEmail() {
        // Pre-seed status as UP
        currentStatus.put("host-c", true);
        ServerUptimeService service = buildService(List.of("host-c"), Map.of("host-c", true));

        service.checkAndAlert();

        verify(emailService, never()).sendServerStatusAlert(anyString(), anyBoolean());
    }

    @Test
    public void checkAndAlert_stillDown_noEmail() {
        currentStatus.put("host-d", false);
        ServerUptimeService service = buildService(List.of("host-d"), Map.of("host-d", false));

        service.checkAndAlert();

        verify(emailService, never()).sendServerStatusAlert(anyString(), anyBoolean());
    }

    // --- checkAndAlert: status change ---

    @Test
    public void checkAndAlert_upToDown_sendsDownAlert() {
        currentStatus.put("host-e", true);
        ServerUptimeService service = buildService(List.of("host-e"), Map.of("host-e", false));

        service.checkAndAlert();

        verify(emailService, times(1)).sendServerStatusAlert("host-e", false);
        assertEquals(currentStatus.get("host-e"), Boolean.FALSE);
    }

    @Test
    public void checkAndAlert_downToUp_sendsUpAlert() {
        currentStatus.put("host-f", false);
        ServerUptimeService service = buildService(List.of("host-f"), Map.of("host-f", true));

        service.checkAndAlert();

        verify(emailService, times(1)).sendServerStatusAlert("host-f", true);
        assertEquals(currentStatus.get("host-f"), Boolean.TRUE);
    }

    // --- Gauge updates ---

    @Test
    public void checkAndAlert_gaugeUpdatedCorrectly_onStatusChange() {
        currentStatus.put("host-g", true);
        ServerUptimeService service = buildService(List.of("host-g"), Map.of("host-g", false));

        service.checkAndAlert();

        assertEquals(registry.getSampleValue("server_status_test", new String[]{"server"}, new String[]{"host-g"}), 0.0);
    }

    @Test
    public void checkAndAlert_gaugeRemainsOne_whenStillUp() {
        currentStatus.put("host-h", true);
        ServerUptimeService service = buildService(List.of("host-h"), Map.of("host-h", true));
        gauge.labels("host-h").set(1.0);

        service.checkAndAlert();

        assertEquals(registry.getSampleValue("server_status_test", new String[]{"server"}, new String[]{"host-h"}), 1.0);
        verify(emailService, never()).sendServerStatusAlert(anyString(), anyBoolean());
    }

    // --- Multiple cycles ---

    @Test
    public void multipleStatusChanges_eachChangeTriggersExactlyOneAlert() {
        currentStatus.put("host-i", true);

        // Cycle 1: goes down
        ServerUptimeService downService = buildService(List.of("host-i"), Map.of("host-i", false));
        downService.checkAndAlert();
        verify(emailService, times(1)).sendServerStatusAlert("host-i", false);

        // Cycle 2: comes back up
        ServerUptimeService upService = buildService(List.of("host-i"), Map.of("host-i", true));
        upService.checkAndAlert();
        verify(emailService, times(1)).sendServerStatusAlert("host-i", true);

        // Cycle 3: still up — no more alerts
        upService.checkAndAlert();
        verify(emailService, times(1)).sendServerStatusAlert("host-i", true);
    }

    @Test
    public void unknownServer_defaultsToDownPrevious_noAlertIfStillDown() {
        // Server not in currentStatus yet — getOrDefault returns false
        // ping returns false — no change (false == false) → no alert
        ServerUptimeService service = buildService(List.of("new-host"), Map.of("new-host", false));

        service.checkAndAlert();

        verify(emailService, never()).sendServerStatusAlert(anyString(), anyBoolean());
    }

    // --- Helpers ---

    /**
     * Builds a ServerUptimeService whose pingServer() is controlled by the supplied ping map.
     * Uses a subclass to avoid any real network I/O in tests.
     */
    private ServerUptimeService buildService(List<String> servers, Map<String, Boolean> pingResults) {
        return new ServerUptimeService(logger, servers, emailService, gauge, currentStatus) {
            @Override
            protected boolean pingServer(String serverAddress) {
                return pingResults.getOrDefault(serverAddress, false);
            }
        };
    }
}
