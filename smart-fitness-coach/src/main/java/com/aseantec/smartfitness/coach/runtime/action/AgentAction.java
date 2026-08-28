package com.aseantec.smartfitness.coach.runtime.action;

/**
 * Agent Loop 单步决策动作。LLM 或规则引擎产出，由 {@link com.aseantec.smartfitness.coach.runtime.loop.AgentLoop} 执行。
 */
public sealed interface AgentAction permits
        AnswerAction,
        ToolCallAction,
        AskUserAction,
        ConfirmationAction,
        PlanAction,
        CompleteAction,
        EndToolPhaseAction {
}
