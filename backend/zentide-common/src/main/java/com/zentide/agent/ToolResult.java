package com.zentide.agent;

import java.util.List;

public record ToolResult<T>(String traceId, String toolCallId, String idempotencyKey,
                            Status status, T data, List<String> evidenceIds,
                            List<String> warnings, boolean retryable, String nextActionHint) {
    public enum Status { SUCCEEDED, REJECTED, FAILED }
    public ToolResult {
        if (traceId == null || traceId.isBlank() || toolCallId == null || toolCallId.isBlank()
                || idempotencyKey == null || idempotencyKey.isBlank() || status == null) throw new IllegalArgumentException("Invalid tool result");
        evidenceIds = evidenceIds == null ? List.of() : List.copyOf(evidenceIds);
        warnings = warnings == null ? List.of() : List.copyOf(warnings);
    }
}
