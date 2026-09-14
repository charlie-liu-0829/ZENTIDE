package com.zentide.agent;

import java.util.Map;

public record AgentDecision(AgentAction action, String reason, ToolRequest tool, Map<String, Object> result,
                            AgentStopReason stopReason) {
    public AgentDecision {
        if (action == null) throw new IllegalArgumentException("Agent action is required");
        if (reason == null || reason.isBlank() || reason.length() > 2_000) throw new IllegalArgumentException("Agent reason is required");
        if (action == AgentAction.CALL_TOOL && tool == null) throw new IllegalArgumentException("Tool request is required");
        if (action == AgentAction.STOP && stopReason == null) throw new IllegalArgumentException("Stop reason is required");
        result = result == null ? Map.of() : Map.copyOf(result);
    }
    public record ToolRequest(String name, Object arguments, String idempotencyKey) {
        public ToolRequest {
            if (name == null || name.isBlank() || arguments == null || idempotencyKey == null || idempotencyKey.isBlank()) throw new IllegalArgumentException("Invalid tool request");
        }
    }
}
