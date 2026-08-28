package com.aseantec.smartfitness.coach.runtime.handler;

import com.aseantec.smartfitness.coach.agent.SkillOutcome;
import com.aseantec.smartfitness.coach.entity.AgentTask;
import com.aseantec.smartfitness.coach.entity.CoachRun;
import com.aseantec.smartfitness.coach.runtime.context.AgentContext;

/**
 * 按意图执行 Skill 的 Runtime 处理器。由 {@link com.aseantec.smartfitness.coach.runtime.AgentRuntime} 调度。
 */
public interface RuntimeHandler {

    /** 支持的意图，如 {@code CHAT}、{@code TODAY_SESSION}。 */
    String intent();

    /**
     * 在已创建 Task + Execution 后执行。
     */
    SkillOutcome execute(AgentContext context, AgentTask task, CoachRun execution,
                         RuntimeEmitter emitter) throws Exception;
}
