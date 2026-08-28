package com.aseantec.smartfitness.coach.runtime.tool;

import java.util.Map;

/**
 * Agent 可调用的 Tool 抽象。Tool 是 Agent 与业务 Service 之间的适配层，不复制业务逻辑。
 */
public interface AgentTool {

    /** 唯一名称，如 {@code getUserProfile}。 */
    String name();

    /** 给 LLM / 规则引擎看的描述。 */
    String description();

    /** {@link ToolPermission#READ} 或 {@link ToolPermission#WRITE}。 */
    ToolPermission permission();

    /**
     * 执行 Tool。
     *
     * @param athleteId 当前运动员
     * @param arguments 调用参数
     */
    ToolResult execute(Long athleteId, Map<String, Object> arguments) throws Exception;
}
