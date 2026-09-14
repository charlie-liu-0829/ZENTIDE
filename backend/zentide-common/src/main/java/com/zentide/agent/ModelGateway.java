package com.zentide.agent;

public interface ModelGateway {
    ModelResponse invoke(AgentRun run, AgentCheckpoint checkpoint);

    record ModelResponse(AgentDecision decision, long tokens, java.math.BigDecimal cost) {
        public ModelResponse {
            if (decision == null || tokens < 0 || cost == null || cost.signum() < 0) throw new IllegalArgumentException("Invalid model response");
        }
    }
}
