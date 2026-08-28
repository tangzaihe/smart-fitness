package com.aseantec.smartfitness.coach.runtime.loop;

import com.aseantec.smartfitness.coach.runtime.action.*;
import com.aseantec.smartfitness.coach.runtime.context.AgentContext;
import com.aseantec.smartfitness.coach.runtime.event.AgentEventPublisher;
import com.aseantec.smartfitness.coach.runtime.event.AgentEventType;
import com.aseantec.smartfitness.coach.runtime.execution.ExecutionState;
import com.aseantec.smartfitness.coach.runtime.tool.ToolExecutor;
import com.aseantec.smartfitness.coach.runtime.tool.ToolResult;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.function.BiFunction;

/**
 * Agent 核心循环：决策 → 执行 Action → 更新上下文 → 发布事件，直到 COMPLETE 或挂起。
 * <p>通过 {@link NextActionStrategy} 注入 Skill 特定逻辑；内置 {@code maxIterations} 防死循环。
 */
@Component
@RequiredArgsConstructor
public class AgentLoop {

    public static final int DEFAULT_MAX_ITERATIONS = 20;

    private final ToolExecutor toolExecutor;
    private final AgentEventPublisher eventPublisher;
    private final ObjectMapper objectMapper;

    @FunctionalInterface
    public interface NextActionStrategy {
        AgentAction next(AgentContext context, LoopState state);
    }

  /**
     * 运行 Agent Loop。
     *
     * @param context        初始上下文
     * @param strategy       下一步决策
     * @param stateUpdater   更新 execution 状态（持久化）
     * @param confirmedTools 用户已确认的 WRITE Tool
     */
    public LoopResult run(AgentContext context, NextActionStrategy strategy,
                          BiFunction<ExecutionState, Integer, Void> stateUpdater,
                          boolean confirmedTools) throws Exception {
        LoopState state = new LoopState();
        AgentContext current = context;
        for (int i = 0; i < DEFAULT_MAX_ITERATIONS; i++) {
            stateUpdater.apply(ExecutionState.THINKING, i + 1);
            AgentAction action = strategy.next(current, state);
            if (action instanceof ToolCallAction toolCall) {
                stateUpdater.apply(ExecutionState.TOOL_EXECUTING, i + 1);
                ToolResult result = toolExecutor.execute(
                        current.getAthleteId(), current.getExecutionId(),
                        toolCall.toolName(), toolCall.arguments(), confirmedTools);
                if (result == null) {
                    stateUpdater.apply(ExecutionState.WAITING_CONFIRMATION, i + 1);
                    return LoopResult.waitingConfirmation(toolCall.toolName(), toolCall.arguments());
                }
                current = current.withToolResult(toolCall.toolName(), result);
                state.lastToolResult = result;
                continue;
            }
            if (action instanceof AnswerAction answer) {
                state.answerText = answer.text();
                eventPublisher.publish(current.getExecutionId(), AgentEventType.MESSAGE_COMPLETED,
                        Map.of("text", answer.text()));
                stateUpdater.apply(ExecutionState.COMPLETED, i + 1);
                eventPublisher.publish(current.getExecutionId(), AgentEventType.TASK_COMPLETED, Map.of());
                return LoopResult.completed(state.answerText, state.planSteps, current);
            }
            if (action instanceof AskUserAction ask) {
                stateUpdater.apply(ExecutionState.WAITING_USER, i + 1);
                eventPublisher.publish(current.getExecutionId(), AgentEventType.USER_INPUT_REQUIRED,
                        Map.of("question", ask.question()));
                return LoopResult.waitingUser(ask.question(), serializeAction(ask));
            }
            if (action instanceof ConfirmationAction confirm) {
                stateUpdater.apply(ExecutionState.WAITING_CONFIRMATION, i + 1);
                eventPublisher.publish(current.getExecutionId(), AgentEventType.CONFIRMATION_REQUIRED,
                        Map.of("summary", confirm.summary(), "tool", confirm.toolName()));
                return LoopResult.waitingConfirmation(confirm.toolName(), confirm.arguments());
            }
            if (action instanceof PlanAction plan) {
                eventPublisher.publish(current.getExecutionId(), AgentEventType.PLAN_CREATED,
                        Map.of("steps", plan.steps()));
                state.planSteps = plan.steps();
                continue;
            }
            if (action instanceof EndToolPhaseAction) {
                return LoopResult.completed(state.answerText, state.planSteps, current);
            }
            if (action instanceof CompleteAction) {
                stateUpdater.apply(ExecutionState.COMPLETED, i + 1);
                eventPublisher.publish(current.getExecutionId(), AgentEventType.TASK_COMPLETED, Map.of());
                return LoopResult.completed(state.answerText, state.planSteps, current);
            }
            throw new IllegalStateException("unsupported action: " + action.getClass().getSimpleName());
        }
        stateUpdater.apply(ExecutionState.FAILED, DEFAULT_MAX_ITERATIONS);
        eventPublisher.publish(current.getExecutionId(), AgentEventType.TASK_FAILED,
                Map.of("reason", "MAX_ITERATIONS_EXCEEDED"));
        return LoopResult.failed("MAX_ITERATIONS_EXCEEDED");
    }

    private String serializeAction(AgentAction action) throws Exception {
        return objectMapper.writeValueAsString(action);
    }

    public static class LoopState {
        String answerText;
        ToolResult lastToolResult;
        java.util.List<String> planSteps;
    }
}
