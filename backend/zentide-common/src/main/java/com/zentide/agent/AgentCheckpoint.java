package com.zentide.agent;

import java.util.List;
import java.util.Map;

public record AgentCheckpoint(String runId, int step, Map<String, Object> state,
                              List<String> evidenceIds, List<String> completedToolCalls) {
    public AgentCheckpoint {
        if (runId == null || runId.isBlank() || step < 0) throw new IllegalArgumentException("Invalid checkpoint");
        state = state == null ? Map.of() : Map.copyOf(state);
        evidenceIds = evidenceIds == null ? List.of() : List.copyOf(evidenceIds);
        completedToolCalls = completedToolCalls == null ? List.of() : List.copyOf(completedToolCalls);
    }
}
