package com.islandpacific.sentinel.tool;

import com.islandpacific.sentinel.security.SecretProtector;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class IbmiSqlToolTest {

    private IbmiSqlTool ibmiSqlTool;

    @BeforeEach
    void setUp() {
        ibmiSqlTool = new IbmiSqlTool(new SecretProtector.DefaultSecretProtector());
    }

    @Test
    void testValidSelectQueriesAllowed() {
        assertDoesNotThrow(() -> IbmiSqlTool.validateSelectOnly("SELECT * FROM QSYS2.SYSTEM_STATUS_INFO"));
        assertDoesNotThrow(() -> IbmiSqlTool.validateSelectOnly("SELECT STATUS, JOB_NAME FROM QSYS2.JOB_INFO WHERE JOB_NAME LIKE 'Q%'"));
        assertDoesNotThrow(() -> IbmiSqlTool.validateSelectOnly("WITH ActiveJobs AS (SELECT * FROM QSYS2.ACTIVE_JOB_INFO) SELECT * FROM ActiveJobs"));
    }

    @Test
    void testRejectsInsertUpdateDeleteDdl() {
        Exception e1 = assertThrows(IllegalArgumentException.class,
                () -> IbmiSqlTool.validateSelectOnly("INSERT INTO QSYS2.CUSTOM_TABLE VALUES (1, 'test')"));
        assertTrue(e1.getMessage().contains("Query rejected"));

        Exception e2 = assertThrows(IllegalArgumentException.class,
                () -> IbmiSqlTool.validateSelectOnly("UPDATE QSYS2.JOB_INFO SET STATUS = 'HELD'"));
        assertTrue(e2.getMessage().contains("Query rejected"));

        Exception e3 = assertThrows(IllegalArgumentException.class,
                () -> IbmiSqlTool.validateSelectOnly("DELETE FROM QSYS2.SYSTEM_STATUS_INFO"));
        assertTrue(e3.getMessage().contains("Query rejected"));

        Exception e4 = assertThrows(IllegalArgumentException.class,
                () -> IbmiSqlTool.validateSelectOnly("DROP TABLE QSYS2.IMPORTANT_DATA"));
        assertTrue(e4.getMessage().contains("Query rejected"));

        Exception e5 = assertThrows(IllegalArgumentException.class,
                () -> IbmiSqlTool.validateSelectOnly("ALTER TABLE QSYS2.USER_INFO ADD COLUMN HACKED INT"));
        assertTrue(e5.getMessage().contains("Query rejected"));

        Exception e6 = assertThrows(IllegalArgumentException.class,
                () -> IbmiSqlTool.validateSelectOnly("SELECT * FROM QSYS2.SYSTEM_STATUS_INFO; DROP TABLE USERS;"));
        assertTrue(e6.getMessage().contains("Multiple SQL statements"));
    }

    @Test
    void testExecuteReturnsFailureForMutationQuery() {
        UUID tenantId = UUID.randomUUID();
        ToolExecutionResult result = ibmiSqlTool.execute(tenantId, Map.of("query", "DELETE FROM QSYS2.USER_PROFILES"));
        assertFalse(result.isSuccess());
        assertNotNull(result.getErrorMessage());
        assertTrue(result.getErrorMessage().contains("Query rejected"));
    }
}
