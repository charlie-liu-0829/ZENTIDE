package com.zentide.agent;

import java.util.Objects;

public record ToolExecutionContext(String runId, String userId, String traceId) {
    public ToolExecutionContext {
        if (runId == null || runId.isBlank() || traceId == null || traceId.isBlank()) throw new IllegalArgumentException("runId and traceId are required");
    }
}
