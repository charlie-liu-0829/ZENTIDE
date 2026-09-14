package com.zentide.agent;

public enum AgentRunStatus {
    QUEUED,
    PREPARING,
    RUNNING,
    VALIDATING,
    WAITING_APPROVAL,
    WAITING_RETRY,
    COMPLETED,
    FAILED,
    CANCELLED;

    public boolean terminal() {
        return this == COMPLETED || this == FAILED || this == CANCELLED;
    }
}
