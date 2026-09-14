package com.zentide.agent;

import java.math.BigDecimal;
import java.time.Duration;

public record AgentBudget(int maxSteps, int maxToolCalls, long maxTokens,
                          BigDecimal maxCost, Duration maxWallTime) {
    public AgentBudget {
        if (maxSteps < 1 || maxToolCalls < 0 || maxTokens < 0 || maxCost == null
                || maxCost.signum() < 0 || maxWallTime == null || maxWallTime.isNegative()
                || maxWallTime.isZero()) {
            throw new IllegalArgumentException("Agent budget must contain positive limits");
        }
    }
}
