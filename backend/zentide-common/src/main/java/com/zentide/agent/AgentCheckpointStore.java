package com.zentide.agent;

public interface AgentCheckpointStore {
    AgentCheckpoint latest(String runId);
    void save(AgentCheckpoint checkpoint);
}
