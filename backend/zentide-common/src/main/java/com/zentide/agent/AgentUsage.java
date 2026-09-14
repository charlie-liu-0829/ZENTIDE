package com.zentide.agent;

import java.math.BigDecimal;

public final class AgentUsage {
    private int steps;
    private int toolCalls;
    private long tokens;
    private BigDecimal cost = BigDecimal.ZERO;

    public int steps() { return steps; }
    public int toolCalls() { return toolCalls; }
    public long tokens() { return tokens; }
    public BigDecimal cost() { return cost; }

    void step() { steps++; }
    void toolCall() { toolCalls++; }
    void addTokens(long value) { tokens = Math.addExact(tokens, value); }
    void addCost(BigDecimal value) { cost = cost.add(value); }
}
