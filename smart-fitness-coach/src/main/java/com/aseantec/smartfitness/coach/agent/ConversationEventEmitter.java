package com.aseantec.smartfitness.coach.agent;

import com.aseantec.smartfitness.coach.entity.CoachEvent;
import com.aseantec.smartfitness.coach.mapper.CoachEventMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * 对话驱动 SSE 事件发射器：同时写 {@code coach_event} 与推流。
 * <p>每条 assistant 消息独立 seq，{@code conversationId}/{@code messageId} 由 Orchestrator 注入。
 */
public class ConversationEventEmitter implements SkillEmitter {

    private final SseEmitter sse;
    private final Long conversationId;
    private final Long assistantMessageId;
    private final CoachEventMapper eventMapper;
    private final ObjectMapper objectMapper;
    private final AtomicInteger seq = new AtomicInteger(1);

    public ConversationEventEmitter(SseEmitter sse, Long conversationId, Long assistantMessageId,
                                   CoachEventMapper eventMapper, ObjectMapper objectMapper) {
        this.sse = sse;
        this.conversationId = conversationId;
        this.assistantMessageId = assistantMessageId;
        this.eventMapper = eventMapper;
        this.objectMapper = objectMapper;
    }

    @Override
    public void send(String type, Object payload) throws Exception {
        CoachEvent event = new CoachEvent();
        event.setConversationId(conversationId);
        event.setMessageId(assistantMessageId);
        event.setSeq(seq.getAndIncrement());
        event.setType(type);
        event.setPayload(objectMapper.writeValueAsString(payload));
        eventMapper.insert(event);
        sse.send(SseEmitter.event().name(type).data(payload));
    }

    @Override
    public SseEmitter sse() {
        return sse;
    }
}
