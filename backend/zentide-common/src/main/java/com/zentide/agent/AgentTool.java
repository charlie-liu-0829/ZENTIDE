package com.zentide.agent;

public interface AgentTool<I, O> {
    String name();
    Class<I> inputType();
    ToolPolicy policy();
    ToolResult<O> execute(ToolExecutionContext context, I input);
}
