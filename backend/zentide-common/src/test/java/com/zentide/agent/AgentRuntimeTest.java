package com.zentide.agent;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class AgentRuntimeTest {
    private static final AgentBudget BUDGET = new AgentBudget(2, 1, 100, new BigDecimal("1.00"), Duration.ofMinutes(1));

    @Test
    void budgetStopsRepeatedToolCalls() {
        AgentRun run = new AgentRun("u1", AgentRunType.INVESTIGATE_CHANGE, "CHANGE", "c1", BUDGET);
        run.preparing(); run.running(); run.consumeStep(); run.consumeToolCall();
        assertThrows(AgentBudgetExceededException.class, run::consumeToolCall);
    }

    @Test
    void registryRejectsUnknownAndSideEffectTools() {
        AgentTool<String, String> tool = tool("search_evidence", ToolPolicy.READ_ONLY);
        AgentToolRegistry registry = new AgentToolRegistry(List.of(tool));
        assertSame(tool, registry.require("search_evidence"));
        assertThrows(AgentPolicyException.class, () -> registry.require("execute_sql"));
        assertThrows(IllegalArgumentException.class, () -> new AgentToolRegistry(List.of(tool("send_email", ToolPolicy.WRITE_SIDE_EFFECT))));
    }

    @Test
    void orchestratorNeverExecutesUnknownToolAndPersistsCheckpoint() {
        AgentRun run = new AgentRun("u1", AgentRunType.INVESTIGATE_CHANGE, "CHANGE", "c1", BUDGET);
        MemoryCheckpoints checkpoints = new MemoryCheckpoints();
        MemoryToolCalls calls = new MemoryToolCalls();
        ModelGateway model = (r, c) -> new ModelGateway.ModelResponse(
                new AgentDecision(AgentAction.CALL_TOOL, "need evidence",
                        new AgentDecision.ToolRequest("unknown", "x", "once"), null, null), 1, BigDecimal.ZERO);
        AgentExecutionResult result = new DefaultAgentOrchestrator(model, new AgentToolRegistry(List.of()), checkpoints, calls).execute(run);
        assertEquals(AgentRunStatus.FAILED, result.status());
        assertEquals(AgentStopReason.POLICY_BLOCKED, result.stopReason());
        assertTrue(checkpoints.value == null); // no checkpoint before a rejected tool
    }

    private AgentTool<String, String> tool(String name, ToolPolicy policy) {
        return new AgentTool<>() {
            public String name() { return name; }
            public Class<String> inputType() { return String.class; }
            public ToolPolicy policy() { return policy; }
            public ToolResult<String> execute(ToolExecutionContext c, String input) {
                return new ToolResult<>(c.traceId(), "tc", "key", ToolResult.Status.SUCCEEDED, input, List.of("ev1"), List.of(), false, null);
            }
        };
    }

    private static final class MemoryCheckpoints implements AgentCheckpointStore {
        AgentCheckpoint value;
        public AgentCheckpoint latest(String runId) { return value; }
        public void save(AgentCheckpoint checkpoint) { value = checkpoint; }
    }
    private static final class MemoryToolCalls implements AgentToolCallStore {
        final Map<String, ToolResult<?>> values = new HashMap<>();
        public ToolResult<?> find(String runId, String key) { return values.get(runId + ":" + key); }
        public void save(String runId, AgentDecision.ToolRequest request, ToolResult<?> result) { values.put(runId + ":" + request.idempotencyKey(), result); }
    }
}
