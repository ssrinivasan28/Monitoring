package com.islandpacific.monitoring.ibmjobquestatusmonitoring;

import org.testng.annotations.Test;

import static org.testng.Assert.*;

public class IbmiJobServiceLogicTest {

    // --- JobInfo field storage ---

    @Test
    public void jobInfo_storesAllFields() {
        JobInfo info = new JobInfo("MYJOB", "MYUSER", "123456", "RUN",
                "BCH", "QBATCH", "PGM", "MYPGM", 500L);
        assertEquals(info.getJobName(), "MYJOB");
        assertEquals(info.getJobUser(), "MYUSER");
        assertEquals(info.getJobNumber(), "123456");
        assertEquals(info.getJobStatus(), "RUN");
        assertEquals(info.getJobType(), "BCH");
        assertEquals(info.getSubsystemName(), "QBATCH");
        assertEquals(info.getFunctionType(), "PGM");
        assertEquals(info.getFunction(), "MYPGM");
        assertEquals(info.getCpuUsed(), 500L);
    }

    @Test
    public void jobInfo_nullFieldsHandled() {
        JobInfo info = new JobInfo("JOB", "USER", null, null, null, null, null, null, 0L);
        assertNull(info.getJobNumber());
        assertNull(info.getJobStatus());
        assertNull(info.getSubsystemName());
    }

    // --- JobInfo.isActive() ---

    @Test
    public void isActive_RUN_returnsTrue() {
        assertTrue(job("RUN").isActive());
    }

    @Test
    public void isActive_MSGW_returnsTrue() {
        assertTrue(job("MSGW").isActive());
    }

    @Test
    public void isActive_DEQW_returnsTrue() {
        assertTrue(job("DEQW").isActive());
    }

    @Test
    public void isActive_END_returnsFalse() {
        assertFalse(job("END").isActive());
    }

    @Test
    public void isActive_OUTQ_returnsFalse() {
        assertFalse(job("OUTQ").isActive());
    }

    @Test
    public void isActive_JOBQ_returnsFalse() {
        assertFalse(job("JOBQ").isActive());
    }

    @Test
    public void isActive_null_returnsFalse() {
        assertFalse(job(null).isActive());
    }

    @Test
    public void isActive_end_lowercase_returnsFalse() {
        assertFalse(job("end").isActive());
    }

    @Test
    public void isActive_END_withLeadingSpace_returnsFalse() {
        assertFalse(job(" END ").isActive());
    }

    // --- toString sanity ---

    @Test
    public void toString_containsJobName() {
        JobInfo info = new JobInfo("MYJOB", "USER", "001", "RUN", "BCH", "QBATCH", null, null, 0L);
        assertTrue(info.toString().contains("MYJOB"));
    }

    // Helper

    private JobInfo job(String status) {
        return new JobInfo("JOB", "USER", "001", status, "BCH", "QBATCH", null, null, 0L);
    }
}
