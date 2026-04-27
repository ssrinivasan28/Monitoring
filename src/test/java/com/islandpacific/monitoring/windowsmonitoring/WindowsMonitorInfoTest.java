package com.islandpacific.monitoring.windowsmonitoring;

import org.testng.annotations.Test;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.testng.Assert.*;

public class WindowsMonitorInfoTest {

    // --- Rounding ---

    @Test
    public void cpuUtilization_roundedToTwoDecimalPlaces() {
        WindowsMonitorInfo info = new WindowsMonitorInfo("host");
        info.setCpuUtilization(75.12345);
        assertEquals(info.getCpuUtilization(), 75.12);
    }

    @Test
    public void memoryUtilization_roundedToTwoDecimalPlaces() {
        WindowsMonitorInfo info = new WindowsMonitorInfo("host");
        info.setMemoryUtilization(88.9876);
        assertEquals(info.getMemoryUtilization(), 88.99);
    }

    @Test
    public void uptimeHours_roundedToTwoDecimalPlaces() {
        WindowsMonitorInfo info = new WindowsMonitorInfo("host");
        info.setSystemUptimeHours(123.456789);
        assertEquals(info.getSystemUptimeHours(), 123.46);
    }

    @Test
    public void topProcesses_valuesRounded() {
        WindowsMonitorInfo info = new WindowsMonitorInfo("host");
        Map<String, Double> procs = new LinkedHashMap<>();
        procs.put("java.exe", 12.3456);
        info.setTopProcesses(procs);
        assertEquals(info.getTopProcesses().get("java.exe"), 12.35);
    }

    @Test
    public void topProcesses_nullReturnedWhenNotSet() {
        WindowsMonitorInfo info = new WindowsMonitorInfo("host");
        assertNull(info.getTopProcesses());
    }

    // --- DiskInfo rounding ---

    @Test
    public void diskInfo_usagePercentRounded() {
        WindowsMonitorInfo.DiskInfo d = new WindowsMonitorInfo.DiskInfo();
        d.usagePercent = 91.2349;
        assertEquals(d.getUsagePercent(), 91.23);
    }

    @Test
    public void diskInfo_totalGBRounded() {
        WindowsMonitorInfo.DiskInfo d = new WindowsMonitorInfo.DiskInfo();
        d.totalGB = 500.9999;
        assertEquals(d.getTotalGB(), 501.0);
    }

    // --- Service statuses ---

    @Test
    public void serviceStatuses_defaultEmptyMap() {
        WindowsMonitorInfo info = new WindowsMonitorInfo("host");
        assertNotNull(info.getServiceStatuses());
        assertTrue(info.getServiceStatuses().isEmpty());
    }

    @Test
    public void serviceStatuses_setAndRetrieved() {
        WindowsMonitorInfo info = new WindowsMonitorInfo("host");
        Map<String, String> statuses = new HashMap<>();
        statuses.put("Spooler", "Running");
        info.setServiceStatuses(statuses);
        assertEquals(info.getServiceStatuses().get("Spooler"), "Running");
    }

    // --- Disk map ---

    @Test
    public void disks_setAndRetrieved() {
        WindowsMonitorInfo info = new WindowsMonitorInfo("host");
        Map<String, WindowsMonitorInfo.DiskInfo> disks = new HashMap<>();
        WindowsMonitorInfo.DiskInfo d = new WindowsMonitorInfo.DiskInfo();
        d.totalGB = 100.0;
        d.freeGB = 40.0;
        d.usedGB = 60.0;
        d.usagePercent = 60.0;
        disks.put("C:\\", d);
        info.setDisks(disks);
        assertEquals(info.getDisks().get("C:\\").getTotalGB(), 100.0);
    }

    // --- Memory consistency ---

    @Test
    public void memoryUsed_equalsTotal_minusFree() {
        WindowsMonitorInfo info = new WindowsMonitorInfo("host");
        double total = 16.0, free = 6.0, used = total - free;
        info.setMemoryTotalGB(total);
        info.setMemoryFreeGB(free);
        info.setMemoryUsedGB(used);
        assertEquals(info.getMemoryTotalGB() - info.getMemoryFreeGB(), info.getMemoryUsedGB(), 0.01);
    }
}
