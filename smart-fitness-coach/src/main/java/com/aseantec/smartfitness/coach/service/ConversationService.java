package com.aseantec.smartfitness.coach.service;

import com.aseantec.smartfitness.athlete.service.AthleteService;
import com.aseantec.smartfitness.coach.agent.AgentOrchestrator;
import com.aseantec.smartfitness.coach.agent.L0ContextBuilder;
import com.aseantec.smartfitness.coach.dto.CoachContextVO;
import com.aseantec.smartfitness.coach.dto.ConversationVO;
import com.aseantec.smartfitness.coach.dto.MessageVO;
import com.aseantec.smartfitness.coach.entity.Conversation;
import com.aseantec.smartfitness.coach.entity.ConversationMessage;
import com.aseantec.smartfitness.coach.mapper.ConversationMapper;
import com.aseantec.smartfitness.coach.mapper.ConversationMessageMapper;
import com.aseantec.smartfitness.common.exception.BizException;
import com.aseantec.smartfitness.common.exception.ErrorCode;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.Map;

/**
 * 对话线程与消息管理。对应 {@code /v1/conversations*} 资源。
 * <p>负责线程 CRUD、消息持久化、首屏 L0 聚合；流式回复委托 {@link AgentOrchestrator}。
 */
@Service
@RequiredArgsConstructor
public class ConversationService {

    private static final long SSE_TIMEOUT_MS = 120_000L;

    private final AthleteService athleteService;
    private final ConversationMapper conversationMapper;
    private final ConversationMessageMapper messageMapper;
    private final AgentOrchestrator orchestrator;
    private final L0ContextBuilder l0ContextBuilder;
    private final ObjectMapper objectMapper;

    /**
     * 新建对话线程。
     *
     * @param primaryIntent 可空，默认 CHAT
     * @param title          可空
     */
    @Transactional
    public Conversation create(Long athleteId, String primaryIntent, String title) {
        athleteService.assertOnboarded(athleteId);
        Conversation row = new Conversation();
        row.setAthleteId(athleteId);
        row.setPrimaryIntent(primaryIntent == null || primaryIntent.isBlank() ? "CHAT" : primaryIntent);
        row.setStatus("OPEN");
        row.setTitle(title);
        conversationMapper.insert(row);
        return row;
    }

    /** 列出当前 athlete 的对话线程（最近优先）。 */
    public List<ConversationVO> list(Long athleteId) {
        List<Conversation> rows = conversationMapper.selectList(new LambdaQueryWrapper<Conversation>()
                .eq(Conversation::getAthleteId, athleteId)
                .orderByDesc(Conversation::getCreatedAt));
        return rows.stream().map(c -> ConversationVO.builder()
                .conversationId(c.getId().toString())
                .primaryIntent(c.getPrimaryIntent())
                .status(c.getStatus())
                .title(c.getTitle())
                .lastMessage(lastMessageText(c.getId()))
                .createdAt(c.getCreatedAt() == null ? null : c.getCreatedAt().toString())
                .build()).toList();
    }

    /** 取线程内消息（升序）。 */
    public List<MessageVO> messages(Long athleteId, Long conversationId) {
        requireOwned(athleteId, conversationId);
        List<ConversationMessage> rows = messageMapper.selectList(new LambdaQueryWrapper<ConversationMessage>()
                .eq(ConversationMessage::getConversationId, conversationId)
                .orderByAsc(ConversationMessage::getCreatedAt));
        return rows.stream().map(m -> MessageVO.builder()
                .messageId(m.getId().toString())
                .role(m.getRole())
                .content(m.getContent())
                .contentType(m.getContentType())
                .metadata(parseMeta(m.getMetadata()))
                .createdAt(m.getCreatedAt() == null ? null : m.getCreatedAt().toString())
                .build()).toList();
    }

    /**
     * 发送消息并流式回复。对应 {@code POST /v1/conversations/{id}/messages}。
     * <p>同步落 USER 消息，再在虚拟线程中跑 Orchestrator；返回的 SseEmitter 由调用方持有。
     */
    public SseEmitter sendMessage(Long athleteId, Long conversationId, String text) {
        Conversation conversation = requireOwned(athleteId, conversationId);
        ConversationMessage userMessage = new ConversationMessage();
        userMessage.setConversationId(conversationId);
        userMessage.setAthleteId(athleteId);
        userMessage.setRole("USER");
        userMessage.setContent(text);
        userMessage.setContentType("TEXT");
        userMessage.setMetadata("{}");
        messageMapper.insert(userMessage);

        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT_MS);
        Thread.startVirtualThread(() -> orchestrator.streamReply(conversation, userMessage, emitter));
        return emitter;
    }

    /** 首屏 L0 上下文（不调 LLM）。 */
    public CoachContextVO context(Long athleteId) {
        return l0ContextBuilder.contextFor(athleteId);
    }

    private Conversation requireOwned(Long athleteId, Long conversationId) {
        Conversation c = conversationMapper.selectById(conversationId);
        if (c == null || !athleteId.equals(c.getAthleteId())) {
            throw new BizException(ErrorCode.NOT_FOUND);
        }
        return c;
    }

    private String lastMessageText(Long conversationId) {
        List<ConversationMessage> rows = messageMapper.selectList(new LambdaQueryWrapper<ConversationMessage>()
                .eq(ConversationMessage::getConversationId, conversationId)
                .orderByDesc(ConversationMessage::getCreatedAt)
                .last("LIMIT 1"));
        if (rows.isEmpty()) {
            return "";
        }
        return rows.get(0).getContent();
    }

    private Object parseMeta(String json) {
        if (json == null || json.isBlank()) {
            return Map.of();
        }
        try {
            return objectMapper.readValue(json, Object.class);
        } catch (Exception ex) {
            return Map.of();
        }
    }
}
