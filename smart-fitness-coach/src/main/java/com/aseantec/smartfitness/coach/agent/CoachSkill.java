package com.aseantec.smartfitness.coach.agent;

/**
 * Skill：对话驱动下的可执行专业能力。
 * <p>每个 Skill 接收用户消息 + L0 上下文 + 流式 emitter，执行后返回 {@link SkillOutcome}。
 * Orchestrator 负责落 assistant 消息与卡片引用，Skill 只关心专业产出。
 */
public interface CoachSkill {

    /** Skill 名，与意图对齐，如 {@code chat} / {@code today_session}。 */
    String name();

    /**
     * 执行 Skill。
     *
     * @param ctx    Skill 执行上下文（athleteId、conversationId、userMessageId、userText、l0Context）
     * @param emitter 流式输出
     * @return Skill 产出（完整文本 + 可选卡片引用）
     * @throws Exception 失败由 Orchestrator 捕获并发 error 事件
     */
    SkillOutcome run(SkillContext ctx, SkillEmitter emitter) throws Exception;
}
