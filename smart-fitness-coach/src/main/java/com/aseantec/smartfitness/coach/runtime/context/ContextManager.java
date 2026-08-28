package com.aseantec.smartfitness.coach.runtime.context;

import com.aseantec.smartfitness.coach.agent.L0ContextBuilder;
import com.aseantec.smartfitness.coach.entity.ConversationMessage;
import com.aseantec.smartfitness.coach.mapper.ConversationMessageMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 组装 AgentContext：L0 事实 + 近期对话 + Tool 结果，不直接把全量历史塞进 prompt。
 */
@Component
@RequiredArgsConstructor
public class ContextManager {

    private static final int RECENT_MESSAGE_LIMIT = 6;

    private final L0ContextBuilder l0ContextBuilder;
    private final ConversationMessageMapper messageMapper;

    /**
     * 为一次 Execution 构建初始上下文。
     */
    public AgentContext build(Long athleteId, Long taskId, Long executionId, Long conversationId,
                              String intent, String userText, String systemInstructions) {
        return AgentContext.builder()
                .athleteId(athleteId)
                .taskId(taskId)
                .executionId(executionId)
                .intent(intent)
                .userText(userText)
                .systemInstructions(systemInstructions)
                .l0Facts(l0ContextBuilder.promptContext(athleteId))
                .recentConversation(loadRecentMessages(conversationId))
                .build();
    }

    private List<Map<String, String>> loadRecentMessages(Long conversationId) {
        if (conversationId == null) {
            return List.of();
        }
        List<ConversationMessage> rows = messageMapper.selectList(new LambdaQueryWrapper<ConversationMessage>()
                .eq(ConversationMessage::getConversationId, conversationId)
                .orderByDesc(ConversationMessage::getCreatedAt)
                .last("LIMIT " + RECENT_MESSAGE_LIMIT));
        List<Map<String, String>> out = new ArrayList<>();
        for (int i = rows.size() - 1; i >= 0; i--) {
            ConversationMessage m = rows.get(i);
            out.add(Map.of("role", m.getRole(), "content", m.getContent()));
        }
        return out;
    }
}
