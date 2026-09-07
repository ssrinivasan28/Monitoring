package com.islandpacific.sentinel.tool;

import com.islandpacific.sentinel.entity.ToolCall;
import com.islandpacific.sentinel.llm.model.LlmTool;
import com.islandpacific.sentinel.repository.ToolCallRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ToolRegistrySecurityTest {

    private ToolCallRepository toolCallRepository;
    private SentinelTool mockTool;
    private ToolRegistryService registryService;

    @BeforeEach
    void setUp() {
        toolCallRepository = mock(ToolCallRepository.class);
        mockTool = mock(SentinelTool.class);
        when(mockTool.getName()).thenReturn("mock_tool");
        when(mockTool.getDescription()).thenReturn("Mock tool description");
        when(mockTool.getLlmToolDefinition()).thenReturn(new LlmTool("mock_tool", "Mock tool description", Map.of()));

        registryService = new ToolRegistryService(List.of(mockTool), toolCallRepository);
    }

    @Test
    void testRequiresTenantId() {
        assertThrows(IllegalArgumentException.class, () ->
                registryService.executeTool(UUID.randomUUID(), null, "mock_tool", Map.of()));
    }

    @Test
    void testToolCallsAuditedToRepository() {
        UUID tenantId = UUID.randomUUID();
        UUID agentRunId = UUID.randomUUID();

        when(mockTool.execute(eq(tenantId), anyMap()))
                .thenReturn(ToolExecutionResult.success(Map.of("status", "ok"), "Execution successful"));

        ToolExecutionResult result = registryService.executeTool(agentRunId, tenantId, "mock_tool", Map.of("param1", "val1", "password", "my_secret_pass"));

        assertTrue(result.isSuccess());

        ArgumentCaptor<ToolCall> captor = ArgumentCaptor.forClass(ToolCall.class);
        verify(toolCallRepository, times(1)).save(captor.capture());

        ToolCall savedCall = captor.getValue();
        assertNotNull(savedCall);
        assertEquals(tenantId, savedCall.getTenantId());
        assertEquals(agentRunId, savedCall.getAgentRunId());
        assertEquals("mock_tool", savedCall.getTool());
        assertTrue(savedCall.getArgsRedactedJson().contains("[REDACTED]"));
        assertFalse(savedCall.getArgsRedactedJson().contains("my_secret_pass"));
        assertTrue(savedCall.getResultSummary().contains("Execution successful"));
    }

    @Test
    void testGetAvailableTools() {
        List<LlmTool> tools = registryService.getAvailableTools();
        assertEquals(1, tools.size());
        assertEquals("mock_tool", tools.get(0).getName());
    }
}
