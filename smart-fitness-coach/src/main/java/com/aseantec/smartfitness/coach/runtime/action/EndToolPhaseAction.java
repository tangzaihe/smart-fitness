package com.aseantec.smartfitness.coach.runtime.action;

/**
 * 结束 Tool 阶段，由 Handler 继续后续 LLM / 业务步骤（不标记 Task 完成）。
 */
public record EndToolPhaseAction() implements AgentAction {
}
