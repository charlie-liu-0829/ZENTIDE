package com.zentide.agent;

import java.util.List;
import java.util.Map;

public record AgentExecutionResult(AgentRunStatus status, AgentStopReason stopReason,
                                   Map<String, Object> result, List<String> warnings) {
    public AgentExecutionResult {
        result = result == null ? Map.of() : Map.copyOf(result);
        warnings = warnings == null ? List.of() : List.copyOf(warnings);
    }
}
