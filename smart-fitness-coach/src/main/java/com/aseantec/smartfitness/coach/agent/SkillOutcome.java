package com.aseantec.smartfitness.coach.agent;

/**
 * Skill 执行结果回传给 Orchestrator。
 * <p>{@code text} 为完整 assistant 文本（已流式发完）；{@code cardType} 可空，表示是否附带结构化卡片。
 */
public interface SkillOutcome {
    String text();

    /** {@code advice} | {@code plan} | {@code session_summary} | null。 */
    default String cardType() {
        return null;
    }

    /** 卡片引用 id（adviceId / planId），可空。 */
    default String cardRef() {
        return null;
    }
}
