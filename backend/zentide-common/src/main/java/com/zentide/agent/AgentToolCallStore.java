package com.zentide.agent;

public interface AgentToolCallStore {
    ToolResult<?> find(String runId, String idempotencyKey);
    void save(String runId, AgentDecision.ToolRequest request, ToolResult<?> result);
}
