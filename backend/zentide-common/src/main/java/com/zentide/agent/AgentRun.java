package com.zentide.agent;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/** Mutable state machine. Every mutation is deliberately explicit and synchronized. */
public final class AgentRun {
    private final String id;
    private final String userId;
    private final AgentRunType runType;
    private final String targetType;
    private final String targetId;
    private final AgentBudget budget;
    private final AgentUsage usage = new AgentUsage();
    private final String traceId;
    private final Clock clock;
    private AgentRunStatus status = AgentRunStatus.QUEUED;
    private AgentStopReason stopReason;
    private Instant startedAt;
    private Instant finishedAt;

    public AgentRun(String userId, AgentRunType runType, String targetType, String targetId,
                    AgentBudget budget) {
        this(UUID.randomUUID().toString().replace("-", ""), userId, runType, targetType, targetId,
                budget, UUID.randomUUID().toString().replace("-", ""), Clock.systemUTC());
    }

    AgentRun(String id, String userId, AgentRunType runType, String targetType, String targetId,
             AgentBudget budget, String traceId, Clock clock) {
        this.id = require(id, "id"); this.userId = userId; this.runType = Objects.requireNonNull(runType);
        this.targetType = require(targetType, "targetType"); this.targetId = require(targetId, "targetId");
        this.budget = Objects.requireNonNull(budget); this.traceId = require(traceId, "traceId"); this.clock = clock;
    }

    public String id() { return id; }
    public String userId() { return userId; }
    public AgentRunType runType() { return runType; }
    public String targetType() { return targetType; }
    public String targetId() { return targetId; }
    public AgentBudget budget() { return budget; }
    public AgentUsage usage() { return usage; }
    public String traceId() { return traceId; }
    public synchronized AgentRunStatus status() { return status; }
    public synchronized AgentStopReason stopReason() { return stopReason; }
    public synchronized Instant startedAt() { return startedAt; }
    public synchronized Instant finishedAt() { return finishedAt; }

    public synchronized void preparing() { transition(AgentRunStatus.PREPARING); }
    public synchronized void running() {
        transition(AgentRunStatus.RUNNING);
        if (startedAt == null) startedAt = clock.instant();
    }
    public synchronized void validating() { transition(AgentRunStatus.VALIDATING); }
    public synchronized void waitingApproval() { transition(AgentRunStatus.WAITING_APPROVAL); }
    public synchronized void waitingRetry() { transition(AgentRunStatus.WAITING_RETRY); }
    public synchronized void complete(AgentStopReason reason) { finish(AgentRunStatus.COMPLETED, reason); }
    public synchronized void fail(AgentStopReason reason) { finish(AgentRunStatus.FAILED, reason); }
    public synchronized void cancel() { finish(AgentRunStatus.CANCELLED, AgentStopReason.USER_CANCELLED); }

    public synchronized void consumeStep() {
        ensureActive();
        if (usage.steps() >= budget.maxSteps()) throw new AgentBudgetExceededException(AgentStopReason.BUDGET_EXHAUSTED);
        usage.step();
    }
    public synchronized void consumeToolCall() {
        ensureActive();
        if (usage.toolCalls() >= budget.maxToolCalls()) throw new AgentBudgetExceededException(AgentStopReason.BUDGET_EXHAUSTED);
        usage.toolCall();
    }
    public synchronized void consumeModelUsage(long tokens, BigDecimal cost) {
        ensureActive();
        if (tokens < 0 || cost == null || cost.signum() < 0) throw new IllegalArgumentException("Invalid model usage");
        if (usage.tokens() + tokens > budget.maxTokens() || usage.cost().add(cost).compareTo(budget.maxCost()) > 0)
            throw new AgentBudgetExceededException(AgentStopReason.BUDGET_EXHAUSTED);
        usage.addTokens(tokens); usage.addCost(cost);
    }
    public synchronized boolean timedOut() {
        return startedAt != null && clock.instant().isAfter(startedAt.plus(budget.maxWallTime()));
    }

    private void ensureActive() { if (status.terminal()) throw new IllegalStateException("Agent run is terminal"); }
    private void transition(AgentRunStatus next) {
        ensureActive();
        if (next == AgentRunStatus.RUNNING && status == AgentRunStatus.QUEUED) status = AgentRunStatus.PREPARING;
        if (next == AgentRunStatus.PREPARING && status != AgentRunStatus.QUEUED) throw new IllegalStateException("Invalid AgentRun transition");
        if (next != AgentRunStatus.PREPARING && next != AgentRunStatus.RUNNING && next != AgentRunStatus.VALIDATING
                && next != AgentRunStatus.WAITING_APPROVAL && next != AgentRunStatus.WAITING_RETRY) throw new IllegalStateException("Invalid AgentRun transition");
        status = next;
    }
    private void finish(AgentRunStatus next, AgentStopReason reason) { ensureActive(); status = next; stopReason = Objects.requireNonNull(reason); finishedAt = clock.instant(); }
    private static String require(String value, String name) { if (value == null || value.isBlank()) throw new IllegalArgumentException(name + " is required"); return value; }
}
