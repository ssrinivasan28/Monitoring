package com.islandpacific.monitoring.winservicemonitor;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.testng.Assert.*;

public class WinServiceMonitorServiceTest {

    private WinServiceMonitorService service;

    @BeforeMethod
    public void setUp() {
        service = new WinServiceMonitorService();
    }

    // -------------------------------------------------------------------------
    // parseServicesJson — status code mapping
    // -------------------------------------------------------------------------

    @Test
    public void parseJson_numericStatus4_mapsToRunning() {
        String json = """
                [{"Name":"Spooler","DisplayName":"Print Spooler","Status":4}]
                """;
        Map<String, String> result = service.parseServicesJson(json, List.of("Print Spooler"), new HashMap<>());
        assertEquals(result.get("Print Spooler"), "Running");
    }

    @Test
    public void parseJson_numericStatus1_mapsToStopped() {
        String json = """
                [{"Name":"W32Time","DisplayName":"Windows Time","Status":1}]
                """;
        Map<String, String> result = service.parseServicesJson(json, List.of("Windows Time"), new HashMap<>());
        assertEquals(result.get("Windows Time"), "Stopped");
    }

    @Test
    public void parseJson_numericStatus2_mapsToStartPending() {
        String json = """
                [{"Name":"MySvc","DisplayName":"My Service","Status":2}]
                """;
        Map<String, String> result = service.parseServicesJson(json, List.of("My Service"), new HashMap<>());
        assertEquals(result.get("My Service"), "StartPending");
    }

    @Test
    public void parseJson_numericStatus3_mapsToStopPending() {
        String json = """
                [{"Name":"MySvc","DisplayName":"My Service","Status":3}]
                """;
        Map<String, String> result = service.parseServicesJson(json, List.of("My Service"), new HashMap<>());
        assertEquals(result.get("My Service"), "StopPending");
    }

    @Test
    public void parseJson_numericStatus7_mapsToPaused() {
        String json = """
                [{"Name":"MySvc","DisplayName":"My Service","Status":7}]
                """;
        Map<String, String> result = service.parseServicesJson(json, List.of("My Service"), new HashMap<>());
        assertEquals(result.get("My Service"), "Paused");
    }

    @Test
    public void parseJson_stringStatusRunning_mapsToRunning() {
        String json = """
                [{"Name":"Spooler","DisplayName":"Print Spooler","Status":"Running"}]
                """;
        Map<String, String> result = service.parseServicesJson(json, List.of("Print Spooler"), new HashMap<>());
        assertEquals(result.get("Print Spooler"), "Running");
    }

    @Test
    public void parseJson_unknownStatus_passedThrough() {
        String json = """
                [{"Name":"MySvc","DisplayName":"My Service","Status":"Degraded"}]
                """;
        Map<String, String> result = service.parseServicesJson(json, List.of("My Service"), new HashMap<>());
        assertEquals(result.get("My Service"), "Degraded");
    }

    // -------------------------------------------------------------------------
    // parseServicesJson — case-insensitive DisplayName matching
    // -------------------------------------------------------------------------

    @Test
    public void parseJson_caseInsensitiveDisplayNameMatch() {
        // Config has "print spooler" (lowercase), JSON returns "Print Spooler"
        String json = """
                [{"Name":"Spooler","DisplayName":"Print Spooler","Status":4}]
                """;
        Map<String, String> result = service.parseServicesJson(json, List.of("print spooler"), new HashMap<>());
        // Should be keyed under the configured name (lowercase as supplied)
        assertEquals(result.get("print spooler"), "Running");
    }

    // -------------------------------------------------------------------------
    // parseServicesJson — missing / not-found services
    // -------------------------------------------------------------------------

    @Test
    public void parseJson_configuredServiceMissingFromJson_markedNotFound() {
        String json = """
                [{"Name":"Spooler","DisplayName":"Print Spooler","Status":4}]
                """;
        Map<String, String> result = service.parseServicesJson(json,
                List.of("Print Spooler", "Missing Service"), new HashMap<>());
        assertEquals(result.get("Missing Service"), "NotFound");
    }

    @Test
    public void parseJson_emptyJson_allServicesNotFound() {
        Map<String, String> result = service.parseServicesJson("", List.of("Svc A", "Svc B"), new HashMap<>());
        assertEquals(result.get("Svc A"), "Unknown");
        assertEquals(result.get("Svc B"), "Unknown");
    }

    @Test
    public void parseJson_nullJson_allServicesUnknown() {
        Map<String, String> result = service.parseServicesJson(null, List.of("Svc A"), new HashMap<>());
        assertEquals(result.get("Svc A"), "Unknown");
    }

    // -------------------------------------------------------------------------
    // parseServicesJson — multiple services in one response
    // -------------------------------------------------------------------------

    @Test
    public void parseJson_multipleServices_allParsed() {
        String json = """
                [
                  {"Name":"Spooler","DisplayName":"Print Spooler","Status":4},
                  {"Name":"W32Time","DisplayName":"Windows Time","Status":1}
                ]
                """;
        Map<String, String> result = service.parseServicesJson(json,
                List.of("Print Spooler", "Windows Time"), new HashMap<>());
        assertEquals(result.get("Print Spooler"), "Running");
        assertEquals(result.get("Windows Time"), "Stopped");
    }

    @Test
    public void parseJson_singleObjectNotArray_stillParsed() {
        // PowerShell returns a bare object (not array) when only one service matches
        String json = """
                {"Name":"Spooler","DisplayName":"Print Spooler","Status":4}
                """;
        Map<String, String> result = service.parseServicesJson(json, List.of("Print Spooler"), new HashMap<>());
        assertEquals(result.get("Print Spooler"), "Running");
    }

    // -------------------------------------------------------------------------
    // parseServicesJson — PS metadata stripping (Invoke-Command wrapping)
    // -------------------------------------------------------------------------

    @Test
    public void parseJson_psMetadataStripped_serviceStillParsed() {
        // Invoke-Command adds PSComputerName, RunspaceId, PSShowComputerName
        String json = """
                [{"PSComputerName":"server1","RunspaceId":"abc-123","PSShowComputerName":true,
                  "Name":"Spooler","DisplayName":"Print Spooler","Status":4}]
                """;
        Map<String, String> result = service.parseServicesJson(json, List.of("Print Spooler"), new HashMap<>());
        assertEquals(result.get("Print Spooler"), "Running");
    }

    // -------------------------------------------------------------------------
    // parseServicesJson — displayNamesOut populated
    // -------------------------------------------------------------------------

    @Test
    public void parseJson_displayNamesOut_populated() {
        String json = """
                [{"Name":"Spooler","DisplayName":"Print Spooler","Status":4}]
                """;
        Map<String, String> displayNames = new HashMap<>();
        service.parseServicesJson(json, List.of("Print Spooler"), displayNames);
        assertTrue(displayNames.containsKey("Print Spooler"));
    }

    // -------------------------------------------------------------------------
    // isLocalHost
    // -------------------------------------------------------------------------

    @Test
    public void isLocalHost_localhost_returnsTrue() {
        assertTrue(service.isLocalHost("localhost"));
    }

    @Test
    public void isLocalHost_loopback_returnsTrue() {
        assertTrue(service.isLocalHost("127.0.0.1"));
    }

    @Test
    public void isLocalHost_localhostMixedCase_returnsTrue() {
        assertTrue(service.isLocalHost("LOCALHOST"));
    }

    @Test
    public void isLocalHost_externalAddress_returnsFalse() {
        // An address that is definitely not this machine
        assertFalse(service.isLocalHost("192.0.2.1")); // TEST-NET-1, non-routable
    }

    // -------------------------------------------------------------------------
    // checkServices — local path via subclass
    // -------------------------------------------------------------------------

    @Test
    public void checkServices_localHost_usesLocalPath() {
        // Subclass overrides queryLocalServiceStatus to avoid real sc.exe calls
        WinServiceMonitorService controlled = new WinServiceMonitorService() {
            @Override
            String queryLocalServiceStatus(String serviceName) {
                return "Running";
            }
        };

        WinServiceInfo info = controlled.checkServices("localhost", List.of("Spooler"), null);
        assertEquals(info.getServiceStatuses().get("Spooler"), "Running");
    }

    @Test
    public void checkServices_emptyServiceList_returnsEmptyInfo() {
        WinServiceInfo info = service.checkServices("localhost", List.of(), null);
        assertTrue(info.getServiceStatuses().isEmpty());
    }

    @Test
    public void checkServices_nullServiceList_returnsEmptyInfo() {
        WinServiceInfo info = service.checkServices("localhost", null, null);
        assertTrue(info.getServiceStatuses().isEmpty());
    }
}
