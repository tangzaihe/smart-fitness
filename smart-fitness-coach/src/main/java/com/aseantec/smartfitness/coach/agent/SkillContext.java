package com.aseantec.smartfitness.coach.agent;

import lombok.Builder;
import lombok.Value;

/**
 * Skill 执行上下文。由 Orchestrator 在路由后注入。
 */
@Value
@Builder
public class SkillContext {
    Long athleteId;
    Long conversationId;
    /** 触发本 Skill 的 USER 消息 id。 */
    Long userMessageId;
    String userText;
    /** L0 事实上下文（中文自然语言片段），供拼进 user message。 */
    String l0Context;
}
