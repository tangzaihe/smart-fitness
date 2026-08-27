package com.aseantec.smartfitness.coach.agent;

import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * Skill 流式输出端口。Orchestrator 创建后注入 Skill，Skill 通过它发 SSE 事件。
 * <p>事件类型契约（对话驱动）：
 * <ul>
 *   <li>{@code token} — 流式文本片段</li>
 *   <li>{@code tool.start} / {@code tool.result} — 真工具调用</li>
 *   <li>{@code card.advice} / {@code card.plan} / {@code card.session_summary} — 结构化卡片</li>
 *   <li>{@code chip.suggest} — 快捷回复建议</li>
 * </ul>
 */
public interface SkillEmitter {

    /** 发送任意类型事件；payload 会被 JSON 序列化。 */
    void send(String type, Object payload) throws Exception;

    /** 发送 token 片段。 */
    default void token(String delta) throws Exception {
        send("token", java.util.Map.of("delta", delta));
    }

    /** 发送工具开始。 */
    default void toolStart(String tool) throws Exception {
        send("tool.start", java.util.Map.of("tool", tool));
    }

    /** 发送工具结果。 */
    default void toolResult(String tool, Object result) throws Exception {
        send("tool.result", java.util.Map.of("tool", tool, "result", result));
    }

    SseEmitter sse();
}
