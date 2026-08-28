package com.aseantec.smartfitness.coach.runtime.event;

import java.time.Instant;
import java.util.Map;

/**
 * Agent Runtime 结构化事件。持久化到 {@code coach_event}，SSE 为传输出口之一。
 *
 * @param executionId 关联 {@code coach_run.id}
 * @param type          事件类型
 * @param timestamp     UTC 时间
 * @param data          结构化载荷
 */
public record AgentEvent(Long executionId, AgentEventType type, Instant timestamp, Map<String, Object> data) {
}
