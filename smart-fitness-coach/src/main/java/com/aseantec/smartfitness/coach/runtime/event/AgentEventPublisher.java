package com.aseantec.smartfitness.coach.runtime.event;

import java.util.Map;

/**
 * Agent 事件发布端口。Runtime 只依赖本接口，不直接操作 SSE。
 */
public interface AgentEventPublisher {

    /**
     * 发布事件：写 EventStore 并通知订阅者（如 SSE）。
     *
     * @param executionId 关联 {@code coach_run.id}
     * @param type        事件类型
     * @param data        结构化载荷
     */
    void publish(Long executionId, AgentEventType type, Map<String, Object> data);
}
