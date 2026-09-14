package com.zentide.agent;

public final class AgentBudgetExceededException extends RuntimeException {
    private final AgentStopReason reason;
    public AgentBudgetExceededException(AgentStopReason reason) { super(reason.name()); this.reason = reason; }
    public AgentStopReason reason() { return reason; }
}
