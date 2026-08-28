package com.aseantec.smartfitness.coach.runtime.event;

import java.util.function.Consumer;

/**
 * SSE 等实时订阅者。由 {@link AgentEventPublisherImpl} 在持久化后回调。
 */
@FunctionalInterface
public interface AgentEventSubscriber {

    void onEvent(AgentEvent event);
}
