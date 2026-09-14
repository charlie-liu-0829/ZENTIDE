package com.zentide.agent;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Single-agent, tool-driven orchestrator. Model output is always treated as an untrusted next action. */
public final class DefaultAgentOrchestrator implements AgentOrchestrator {
    private final ModelGateway model;
    private final AgentToolRegistry registry;
    private final AgentCheckpointStore checkpoints;
    private final AgentToolCallStore toolCalls;

    public DefaultAgentOrchestrator(ModelGateway model, AgentToolRegistry registry,
                                    AgentCheckpointStore checkpoints, AgentToolCallStore toolCalls) {
        this.model = model; this.registry = registry; this.checkpoints = checkpoints; this.toolCalls = toolCalls;
    }

    @Override
    public AgentExecutionResult execute(AgentRun run) {
        if (run == null) throw new IllegalArgumentException("run is required");
        if (run.status().terminal()) return new AgentExecutionResult(run.status(), run.stopReason(), Map.of(), List.of());
        if (run.status() == AgentRunStatus.WAITING_APPROVAL) {
            return new AgentExecutionResult(run.status(), AgentStopReason.WAITING_HUMAN_REVIEW, Map.of(), List.of());
        }
        if (run.status() == AgentRunStatus.QUEUED) run.preparing();
        run.running();
        AgentCheckpoint checkpoint = checkpoints.latest(run.id());
        List<String> warnings = new ArrayList<>();
        try {
            for (;;) {
                if (run.timedOut()) { run.fail(AgentStopReason.TIMEOUT); break; }
                run.consumeStep();
                ModelGateway.ModelResponse response = model.invoke(run, checkpoint);
                run.consumeModelUsage(response.tokens(), response.cost());
                AgentDecision decision = response.decision();
                switch (decision.action()) {
                    case CALL_TOOL -> {
                        run.consumeToolCall();
                        AgentDecision.ToolRequest request = decision.tool();
                        AgentTool<?, ?> tool = registry.require(request.name());
                        ToolResult<?> result = toolCalls.find(run.id(), request.idempotencyKey());
                        if (result == null) {
                            result = invokeChecked(tool, run, request);
                            toolCalls.save(run.id(), request, result);
                        }
                        if (result.status() == ToolResult.Status.FAILED && result.retryable()) {
                            run.waitingRetry(); checkpoint = saveCheckpoint(run, checkpoint, result);
                            run.running(); continue;
                        }
                        warnings.addAll(result.warnings());
                        checkpoint = saveCheckpoint(run, checkpoint, result);
                    }
                    case REQUEST_APPROVAL -> { run.waitingApproval(); saveCheckpoint(run, checkpoint, null); return new AgentExecutionResult(run.status(), AgentStopReason.WAITING_HUMAN_REVIEW, decision.result(), warnings); }
                    case FINISH -> { run.validating(); validateResult(decision); run.complete(AgentStopReason.GOAL_COMPLETED); saveCheckpoint(run, checkpoint, null); return new AgentExecutionResult(run.status(), run.stopReason(), decision.result(), warnings); }
                    case STOP -> { run.complete(decision.stopReason()); saveCheckpoint(run, checkpoint, null); return new AgentExecutionResult(run.status(), run.stopReason(), decision.result(), warnings); }
                }
            }
        } catch (AgentBudgetExceededException e) {
            run.complete(e.reason());
        } catch (AgentPolicyException | IllegalArgumentException e) {
            run.fail(AgentStopReason.POLICY_BLOCKED); warnings.add("Agent action rejected by policy");
        } catch (Exception e) {
            run.fail(AgentStopReason.PROVIDER_ERROR); warnings.add("Agent provider or tool failed");
        }
        return new AgentExecutionResult(run.status(), run.stopReason(), Map.of(), warnings);
    }

    @SuppressWarnings("unchecked")
    private ToolResult<?> invokeChecked(AgentTool<?, ?> raw, AgentRun run, AgentDecision.ToolRequest request) {
        if (raw.policy() == ToolPolicy.WRITE_SIDE_EFFECT) throw new AgentPolicyException("Side-effect tools are disabled");
        if (!raw.inputType().isInstance(request.arguments())) throw new AgentPolicyException("Tool arguments do not match schema");
        return ((AgentTool<Object, ?>) raw).execute(new ToolExecutionContext(run.id(), run.userId(), run.traceId()), request.arguments());
    }

    private AgentCheckpoint saveCheckpoint(AgentRun run, AgentCheckpoint previous, ToolResult<?> result) {
        List<String> evidence = new ArrayList<>(previous == null ? List.of() : previous.evidenceIds());
        if (result != null) evidence.addAll(result.evidenceIds());
        AgentCheckpoint next = new AgentCheckpoint(run.id(), run.usage().steps(), Map.of("status", run.status().name()), evidence,
                previous == null ? List.of() : previous.completedToolCalls());
        checkpoints.save(next); return next;
    }

    private void validateResult(AgentDecision decision) {
        Object claims = decision.result().get("claims");
        if (claims instanceof List<?> list) for (Object claim : list) {
            if (!(claim instanceof Map<?, ?> map) || !(map.get("evidenceIds") instanceof List<?> ids) || ids.isEmpty())
                throw new AgentPolicyException("Verified claims require evidence");
        }
    }
}
