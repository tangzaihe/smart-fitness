package com.aseantec.smartfitness.coach.runtime.handler;

import com.aseantec.smartfitness.coach.runtime.event.AgentEventType;

/**
 * Runtime 执行期事件出口：映射到 SSE 兼容事件名。
 */
public interface RuntimeEmitter {

    void send(AgentEventType type, Object payload) throws Exception;

    void token(String delta) throws Exception;
}
