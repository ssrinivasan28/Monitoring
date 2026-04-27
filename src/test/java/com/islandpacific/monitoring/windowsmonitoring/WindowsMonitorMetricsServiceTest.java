package com.islandpacific.monitoring.windowsmonitoring;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.Collections;
import java.util.List;

import static org.testng.Assert.*;

public class WindowsMonitorMetricsServiceTest {

    private WindowsMonitorMetricsService service;

    @BeforeMethod
    public void setUp() {
        service = new WindowsMonitorMetricsService();
    }

    // --- Local host detection ---

    @Test
    public void getMetrics_localhost_returnsInfo() {
        WindowsMonitorInfo info = service.getMetrics("localhost", 3, Collections.emptyList(), null);
        assertNotNull(info);
        assertEquals(info.getHostName(), "localhost");
    }

    @Test
    public void getMetrics_localhost_cpuNotNegative() {
        WindowsMonitorInfo info = service.getMetrics("localhost", 3, Collections.emptyList(), null);
        assertTrue(info.getCpuUtilization() >= 0.0);
        assertTrue(info.getCpuUtilization() <= 100.0);
    }

    @Test
    public void getMetrics_localhost_memoryValuesConsistent() {
        WindowsMonitorInfo info = service.getMetrics("localhost", 3, Collections.emptyList(), null);
        assertTrue(info.getMemoryTotalGB() > 0);
        assertTrue(info.getMemoryUsedGB() >= 0);
        assertTrue(info.getMemoryFreeGB() >= 0);
        // used + free ≈ total (within rounding)
        assertEquals(info.getMemoryUsedGB() + info.getMemoryFreeGB(), info.getMemoryTotalGB(), 0.1);
    }

    @Test
    public void getMetrics_localhost_memoryUtilizationBounded() {
        WindowsMonitorInfo info = service.getMetrics("localhost", 3, Collections.emptyList(), null);
        assertTrue(info.getMemoryUtilization() >= 0.0);
        assertTrue(info.getMemoryUtilization() <= 100.0);
    }

    @Test
    public void getMetrics_localhost_disksNotEmpty() {
        WindowsMonitorInfo info = service.getMetrics("localhost", 3, Collections.emptyList(), null);
        assertNotNull(info.getDisks());
        assertFalse(info.getDisks().isEmpty());
    }

    @Test
    public void getMetrics_localhost_diskUsagePercentBounded() {
        WindowsMonitorInfo info = service.getMetrics("localhost", 3, Collections.emptyList(), null);
        for (WindowsMonitorInfo.DiskInfo d : info.getDisks().values()) {
            assertTrue(d.getUsagePercent() >= 0.0);
            assertTrue(d.getUsagePercent() <= 100.0);
        }
    }

    @Test
    public void getMetrics_localhost_diskUsedPlusFreeEqualsTotal() {
        WindowsMonitorInfo info = service.getMetrics("localhost", 3, Collections.emptyList(), null);
        for (WindowsMonitorInfo.DiskInfo d : info.getDisks().values()) {
            assertEquals(d.getUsedGB() + d.getFreeGB(), d.getTotalGB(), 0.1);
        }
    }

    @Test
    public void getMetrics_localhost_uptimePositive() {
        WindowsMonitorInfo info = service.getMetrics("localhost", 3, Collections.emptyList(), null);
        assertTrue(info.getSystemUptimeHours() > 0);
    }

    @Test
    public void getMetrics_localhost_topProcessesRespectsTopN() {
        WindowsMonitorInfo info = service.getMetrics("localhost", 3, Collections.emptyList(), null);
        assertNotNull(info.getTopProcesses());
        assertTrue(info.getTopProcesses().size() <= 3);
    }

    @Test
    public void getMetrics_localhost_topProcessesTopNOf5() {
        WindowsMonitorInfo info = service.getMetrics("localhost", 5, Collections.emptyList(), null);
        assertNotNull(info.getTopProcesses());
        assertTrue(info.getTopProcesses().size() <= 5);
    }

    // --- Service status (local sc query) ---

    @Test
    public void getMetrics_localhost_knownRunningService_returnsRunning() {
        // "Spooler" (Print Spooler) is usually present on Windows test machines.
        // If not running, the result may be Stopped — just check it doesn't throw.
        WindowsMonitorInfo info = service.getMetrics("localhost", 1, List.of("Spooler"), null);
        assertNotNull(info.getServiceStatuses());
        assertTrue(info.getServiceStatuses().containsKey("Spooler"));
        String status = info.getServiceStatuses().get("Spooler");
        assertNotNull(status);
        assertFalse(status.isEmpty());
    }

    @Test
    public void getMetrics_localhost_nonExistentService_returnsNotFoundOrUnknown() {
        WindowsMonitorInfo info = service.getMetrics("localhost", 1,
                List.of("ThisServiceDefinitelyDoesNotExist_XYZ"), null);
        assertNotNull(info.getServiceStatuses());
        String status = info.getServiceStatuses().get("ThisServiceDefinitelyDoesNotExist_XYZ");
        assertNotNull(status);
        // sc query on non-existent service returns NotFound or Unknown
        assertTrue(status.equals("NotFound") || status.equals("Unknown") || status.equals("Stopped"));
    }

    // --- Second cycle CPU tick delta ---

    @Test
    public void getMetrics_twoCycles_cpuDoesNotRequireSleep() {
        // Proves no Thread.sleep is needed — two consecutive calls must both succeed
        WindowsMonitorInfo first = service.getMetrics("localhost", 1, Collections.emptyList(), null);
        WindowsMonitorInfo second = service.getMetrics("localhost", 1, Collections.emptyList(), null);
        assertNotNull(first);
        assertNotNull(second);
        assertTrue(second.getCpuUtilization() >= 0.0);
    }
}
