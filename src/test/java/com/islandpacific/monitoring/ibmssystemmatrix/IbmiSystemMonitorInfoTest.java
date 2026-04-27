package com.islandpacific.monitoring.ibmssystemmatrix;

import org.testng.annotations.Test;

import static org.testng.Assert.*;

public class IbmiSystemMonitorInfoTest {

    private IbmiSystemMonitorInfo build(double cpu, double asp, double pool, long total, long active) {
        return new IbmiSystemMonitorInfo("ibmi-host", cpu, asp, pool, total, active);
    }

    @Test
    public void getHost_returnsCorrectValue() {
        assertEquals(build(50.0, 30.0, 10.0, 100L, 50L).getHost(), "ibmi-host");
    }

    @Test
    public void getCpuUtilization_returnsCorrectValue() {
        assertEquals(build(75.5, 0, 0, 0, 0).getCpuUtilization(), 75.5);
    }

    @Test
    public void getAspUtilization_returnsCorrectValue() {
        assertEquals(build(0, 42.0, 0, 0, 0).getAspUtilization(), 42.0);
    }

    @Test
    public void getSharedPoolUtilization_returnsCorrectValue() {
        assertEquals(build(0, 0, 15.0, 0, 0).getSharedPoolUtilization(), 15.0);
    }

    @Test
    public void getTotalJobs_returnsCorrectValue() {
        assertEquals(build(0, 0, 0, 999L, 0).getTotalJobs(), 999L);
    }

    @Test
    public void getActiveJobs_returnsCorrectValue() {
        assertEquals(build(0, 0, 0, 100L, 45L).getActiveJobs(), 45L);
    }

    @Test
    public void zeroValues_validState() {
        IbmiSystemMonitorInfo info = build(0.0, 0.0, 0.0, 0L, 0L);
        assertEquals(info.getCpuUtilization(), 0.0);
        assertEquals(info.getTotalJobs(), 0L);
        assertEquals(info.getActiveJobs(), 0L);
    }

    @Test
    public void activeJobs_lessThanOrEqualTotalJobs() {
        IbmiSystemMonitorInfo info = build(50.0, 20.0, 5.0, 200L, 80L);
        assertTrue(info.getActiveJobs() <= info.getTotalJobs());
    }

    @Test
    public void cpuUtilization_boundedBetween0And100() {
        IbmiSystemMonitorInfo info = build(99.9, 0, 0, 0, 0);
        assertTrue(info.getCpuUtilization() >= 0.0 && info.getCpuUtilization() <= 100.0);
    }
}
