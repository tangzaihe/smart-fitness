package com.aseantec.smartfitness.coach.runtime.action;

import java.util.Map;

/**
 * 调用注册表中的 Tool。
 *
 * @param toolName  {@link com.aseantec.smartfitness.coach.runtime.tool.AgentTool#name()}
 * @param arguments Tool 入参
 */
public record ToolCallAction(String toolName, Map<String, Object> arguments) implements AgentAction {
    public ToolCallAction(String toolName) {
        this(toolName, Map.of());
    }
}
