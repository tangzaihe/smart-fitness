package com.aseantec.smartfitness.coach.agent;

import com.aseantec.smartfitness.athlete.service.AthleteService;
import com.aseantec.smartfitness.coach.entity.ConversationMessage;
import com.aseantec.smartfitness.coach.mapper.CoachEventMapper;
import com.aseantec.smartfitness.coach.mapper.ConversationMessageMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Agent 编排：用户消息 → 意图路由 → Skill 执行 → 流式回写 → 落 assistant 消息。
 * <p>对话驱动的核心。不直接调 LLM；Skill 负责 LLM 与工具。
 * 线程模型：调用方在虚拟线程中执行 {@link #streamReply}，本类同步阻塞至 Skill 完成。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AgentOrchestrator {

    private final AthleteService athleteService;
    private final IntentRouter intentRouter;
    private final L0ContextBuilder l0ContextBuilder;
    private final List<CoachSkill> skills;
    private final ConversationMessageMapper messageMapper;
    private final CoachEventMapper eventMapper;
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
        String intent = intentRouter.classify(userMessage.getContent(), null);
        String l0 = l0ContextBuilder.promptContext(conversation.getAthleteId());
        CoachSkill skill = selectSkill(intent);

        // 预创建 assistant 消息占位，Skill 完成后回填 content/metadata
        ConversationMessage assistant = new ConversationMessage();
        assistant.setConversationId(conversation.getId());
        assistant.setAthleteId(conversation.getAthleteId());
        assistant.setRole("ASSISTANT");
        assistant.setContent("");
        assistant.setContentType("MIXED");
        assistant.setMetadata("{}");
        messageMapper.insert(assistant);

        SkillEmitter skillEmitter = new ConversationEventEmitter(emitter, conversation.getId(),
                assistant.getId(), eventMapper, objectMapper);
        SkillContext ctx = SkillContext.builder()
                .athleteId(conversation.getAthleteId())
                .conversationId(conversation.getId())
                .userMessageId(userMessage.getId())
                .userText(userMessage.getContent())
                .l0Context(l0)
                .build();

        try {
            emitter.send(SseEmitter.event().name("message.start").data(Map.of(
                    "messageId", assistant.getId().toString(),
                    "intent", intent,
                    "skill", skill.name())));
            SkillOutcome outcome = skill.run(ctx, skillEmitter);
            Map<String, Object> meta = new HashMap<>();
            meta.put("intent", intent);
            meta.put("skill", skill.name());
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
                    "intent", intent,
                    "skill", skill.name(),
                    "text", outcome.text())));
            emitter.complete();
        } catch (Exception ex) {
            log.error("agent orchestrator failed", ex);
            assistant.setContent("教练暂时开小差了，请稍后再试。");
            Map<String, Object> meta = new HashMap<>();
            meta.put("intent", intent);
            meta.put("skill", skill.name());
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

    private CoachSkill selectSkill(String intent) {
        String skillName = switch (intent) {
            case "TODAY_SESSION" -> "today_session";
            case "WORKOUT_PLANNING", "REST" -> "chat";
            default -> "chat";
        };
        return skills.stream()
                .filter(s -> s.name().equals(skillName))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("skill not found: " + skillName));
    }
}
