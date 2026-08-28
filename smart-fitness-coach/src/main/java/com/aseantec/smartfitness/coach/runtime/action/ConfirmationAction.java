package com.aseantec.smartfitness.coach.runtime.action;

import java.util.Map;

/**
 * 请求用户确认有副作用的操作（通常对应 WRITE Tool）。
 *
 * @param summary   向用户展示的变更摘要
 * @param toolName  确认后要执行的 Tool
 * @param arguments Tool 入参
 */
public record ConfirmationAction(String summary, String toolName, Map<String, Object> arguments) implements AgentAction {
}
