package com.zentide.agent;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Registry is the only way an Agent can resolve a tool; arbitrary tool names are rejected. */
public final class AgentToolRegistry {
    private final Map<String, AgentTool<?, ?>> tools;

    public AgentToolRegistry(List<? extends AgentTool<?, ?>> registered) {
        Map<String, AgentTool<?, ?>> values = new LinkedHashMap<>();
        List<? extends AgentTool<?, ?>> source = registered == null ? List.of() : registered;
        for (AgentTool<?, ?> tool : source) {
            if (tool == null || tool.name() == null || tool.name().isBlank() || tool.name().contains(".")
                    || tool.policy() == ToolPolicy.WRITE_SIDE_EFFECT) throw new IllegalArgumentException("Tool is not allowed");
            if (values.put(tool.name(), tool) != null) throw new IllegalArgumentException("Duplicate tool: " + tool.name());
        }
        tools = Map.copyOf(values);
    }

    public AgentTool<?, ?> require(String name) {
        AgentTool<?, ?> tool = tools.get(name);
        if (tool == null) throw new AgentPolicyException("Unknown or unavailable Agent tool");
        return tool;
    }

    public int size() { return tools.size(); }
}
