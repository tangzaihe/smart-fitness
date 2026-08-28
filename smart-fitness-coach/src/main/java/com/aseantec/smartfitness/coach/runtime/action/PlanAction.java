package com.aseantec.smartfitness.coach.runtime.action;

import java.util.List;

/**
 * 创建多步执行计划。
 *
 * @param steps 步骤名称列表（按顺序）
 */
public record PlanAction(List<String> steps) implements AgentAction {
}
