package com.aseantec.smartfitness.coach.runtime.event;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Agent 事件发布实现：EventStore → 订阅者（SSE 等）。
 * <p>通过 {@link #bindContext} 绑定当前 execution 的对话上下文，供持久化使用。
 */
@Component
@RequiredArgsConstructor
public class AgentEventPublisherImpl implements AgentEventPublisher {

    private final AgentEventStore eventStore;
    private final CopyOnWriteArrayList<AgentEventSubscriber> subscribers = new CopyOnWriteArrayList<>();

    private static final ThreadLocal<EventBinding> BINDING = new ThreadLocal<>();

    public record EventBinding(Long conversationId, Long messageId) {
    }

    public void bindContext(Long conversationId, Long messageId) {
        BINDING.set(new EventBinding(conversationId, messageId));
    }

    public void clearContext() {
        BINDING.remove();
    }

    public void subscribe(AgentEventSubscriber subscriber) {
        subscribers.add(subscriber);
    }

    public void unsubscribe(AgentEventSubscriber subscriber) {
        subscribers.remove(subscriber);
    }

    @Override
    public void publish(Long executionId, AgentEventType type, Map<String, Object> data) {
        EventBinding binding = BINDING.get();
        Long conversationId = binding == null ? null : binding.conversationId();
        Long messageId = binding == null ? null : binding.messageId();
        try {
            eventStore.append(executionId, conversationId, messageId, type, data);
        } catch (Exception ex) {
            throw new IllegalStateException("failed to persist agent event: " + type, ex);
        }
        AgentEvent event = new AgentEvent(executionId, type, Instant.now(), data);
        for (AgentEventSubscriber subscriber : subscribers) {
            try {
                subscriber.onEvent(event);
            } catch (Exception ignored) {
                // client gone
            }
        }
    }
}
