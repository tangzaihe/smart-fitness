package com.aseantec.smartfitness.coach.agent;

import com.aseantec.smartfitness.athlete.service.AthleteService;
import com.aseantec.smartfitness.coach.entity.ConversationMessage;
import com.aseantec.smartfitness.coach.mapper.ConversationMessageMapper;
import com.aseantec.smartfitness.coach.runtime.AgentRuntime;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Agent 编排：用户消息 → {@link AgentRuntime}（Task/Execution/Loop）→ 流式回写 → 落 assistant 消息。
 * <p>对话驱动的核心入口。Runtime 负责 Task 生命周期、Tool Registry 与事件发布；本类负责消息持久化与 SSE 边界。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AgentOrchestrator {

    private final AthleteService athleteService;
    private final AgentRuntime agentRuntime;
    private final ConversationMessageMapper messageMapper;
    private final ObjectMapper objectMapper;

    /**
     * 处理一条用户消息并流式回复。
     *
     * @param conversation     已落库的对话线程
     * @param userMessage      已落库的 USER 消息
     * @param emitter          SSE 输出
     * @return 新建的 ASSISTANT 消息（已落库）；失败时仍落一条错误占位消息
     */
    public ConversationMessage streamReply(com.aseantec.smartfitness.coach.entity.Conversation conversation,
                                            ConversationMessage userMessage, SseEmitter emitter) {
        athleteService.assertOnboarded(conversation.getAthleteId());

        ConversationMessage assistant = new ConversationMessage();
        assistant.setConversationId(conversation.getId());
        assistant.setAthleteId(conversation.getAthleteId());
        assistant.setRole("ASSISTANT");
        assistant.setContent("");
        assistant.setContentType("MIXED");
        assistant.setMetadata("{}");
        messageMapper.insert(assistant);

        try {
            emitter.send(SseEmitter.event().name("message.start").data(Map.of(
                    "messageId", assistant.getId().toString())));

            AgentRuntime.RuntimeResult runtime = agentRuntime.execute(
                    conversation, userMessage, assistant, emitter);

            SkillOutcome outcome = runtime.outcome();
            Map<String, Object> meta = new HashMap<>();
            meta.put("intent", runtime.intent());
            meta.put("skill", runtime.skill());
            meta.put("taskId", runtime.taskId().toString());
            meta.put("executionId", runtime.executionId().toString());
            if (outcome.cardType() != null) {
                meta.put("cards", List.of(Map.of(
                        "type", outcome.cardType(),
                        "ref", outcome.cardRef() == null ? "" : outcome.cardRef())));
            }
            assistant.setContent(outcome.text());
            assistant.setMetadata(objectMapper.writeValueAsString(meta));
            messageMapper.updateById(assistant);

            emitter.send(SseEmitter.event().name("message.done").data(Map.of(
                    "messageId", assistant.getId().toString(),
                    "intent", runtime.intent(),
                    "skill", runtime.skill(),
                    "taskId", runtime.taskId().toString(),
                    "executionId", runtime.executionId().toString(),
                    "text", outcome.text())));
            emitter.complete();
        } catch (Exception ex) {
            log.error("agent orchestrator failed", ex);
            assistant.setContent("教练暂时开小差了，请稍后再试。");
            Map<String, Object> meta = new HashMap<>();
            meta.put("error", ex.getMessage() == null ? ex.getClass().getSimpleName() : ex.getMessage());
            try {
                assistant.setMetadata(objectMapper.writeValueAsString(meta));
            } catch (Exception ignored) {
                assistant.setMetadata("{}");
            }
            messageMapper.updateById(assistant);
            try {
                emitter.send(SseEmitter.event().name("error").data(Map.of(
                        "message", ex.getMessage() == null ? "agent failed" : ex.getMessage(),
                        "messageId", assistant.getId().toString())));
            } catch (Exception ignored) {
                // ignore
            }
            emitter.completeWithError(ex);
        }
        return assistant;
    }
}
