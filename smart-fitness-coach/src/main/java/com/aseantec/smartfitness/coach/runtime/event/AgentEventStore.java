package com.aseantec.smartfitness.coach.runtime.event;

import com.aseantec.smartfitness.coach.entity.CoachEvent;
import com.aseantec.smartfitness.coach.mapper.CoachEventMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 将 Agent 事件持久化到 {@code coach_event}，支持按 execution 回放。
 */
@Component
@RequiredArgsConstructor
public class AgentEventStore {

    private final CoachEventMapper eventMapper;
    private final ObjectMapper objectMapper;

    /**
     * 写入事件并返回 seq。
     */
    public int append(Long executionId, Long conversationId, Long messageId,
                      AgentEventType type, Object payload) throws Exception {
        int seq = nextSeq(executionId, conversationId, messageId);
        CoachEvent row = new CoachEvent();
        row.setRunId(executionId);
        row.setConversationId(conversationId);
        row.setMessageId(messageId);
        row.setSeq(seq);
        row.setType(type.name());
        row.setPayload(objectMapper.writeValueAsString(payload));
        eventMapper.insert(row);
        return seq;
    }

    /** 列出 execution 的全部事件（升序）。 */
    public List<CoachEvent> listByExecution(Long executionId) {
        return eventMapper.selectList(new LambdaQueryWrapper<CoachEvent>()
                .eq(CoachEvent::getRunId, executionId)
                .orderByAsc(CoachEvent::getSeq));
    }

    private int nextSeq(Long executionId, Long conversationId, Long messageId) {
        LambdaQueryWrapper<CoachEvent> wrapper = new LambdaQueryWrapper<>();
        if (executionId != null) {
            wrapper.eq(CoachEvent::getRunId, executionId);
        } else if (conversationId != null && messageId != null) {
            wrapper.eq(CoachEvent::getConversationId, conversationId)
                    .eq(CoachEvent::getMessageId, messageId);
        }
        wrapper.orderByDesc(CoachEvent::getSeq).last("LIMIT 1");
        List<CoachEvent> rows = eventMapper.selectList(wrapper);
        if (rows.isEmpty()) {
            return 1;
        }
        Integer last = rows.get(0).getSeq();
        return last == null ? 1 : last + 1;
    }
}
