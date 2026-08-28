package com.aseantec.smartfitness.coach.runtime;

import com.aseantec.smartfitness.coach.agent.CoachPolicyService;
import com.aseantec.smartfitness.coach.agent.IntentRouter;
import com.aseantec.smartfitness.coach.agent.SkillOutcome;
import com.aseantec.smartfitness.coach.entity.AgentTask;
import com.aseantec.smartfitness.coach.entity.CoachRun;
import com.aseantec.smartfitness.coach.entity.Conversation;
import com.aseantec.smartfitness.coach.entity.ConversationMessage;
import com.aseantec.smartfitness.coach.runtime.context.AgentContext;
import com.aseantec.smartfitness.coach.runtime.context.ContextManager;
import com.aseantec.smartfitness.coach.runtime.event.AgentEventPublisherImpl;
import com.aseantec.smartfitness.coach.runtime.event.AgentEventType;
import com.aseantec.smartfitness.coach.runtime.execution.ExecutionManager;
import com.aseantec.smartfitness.coach.runtime.handler.ChatRuntimeHandler;
import com.aseantec.smartfitness.coach.runtime.handler.RuntimeEmitter;
import com.aseantec.smartfitness.coach.runtime.handler.RuntimeHandler;
import com.aseantec.smartfitness.coach.runtime.handler.TodaySessionRuntimeHandler;
import com.aseantec.smartfitness.coach.runtime.task.TaskService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.Map;

/**
 * Agent Runtime 入口：Task → Execution → Handler → Event。
 * <p>对话路径由 {@link com.aseantec.smartfitness.coach.agent.AgentOrchestrator} 调用；
 * Execution 持久化在 {@code coach_run}，事件经 {@link AgentEventPublisherImpl} 与 SSE 解耦。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AgentRuntime {

    private final TaskService taskService;
    private final ExecutionManager executionManager;
    private final ContextManager contextManager;
    private final IntentRouter intentRouter;
    private final CoachPolicyService policyService;
    private final AgentEventPublisherImpl eventPublisher;
    private final TodaySessionRuntimeHandler todaySessionHandler;
    private final ChatRuntimeHandler chatRuntimeHandler;

    /**
     * 处理对话消息：创建 Task + Execution，绑定事件上下文，调度 RuntimeHandler。
     */
    public RuntimeResult execute(Conversation conversation, ConversationMessage userMessage,
                                 ConversationMessage assistantMessage, SseEmitter sse) {
        String intent = intentRouter.classify(userMessage.getContent(), null);
        AgentTask task = taskService.create(
                conversation.getAthleteId(), conversation.getId(), userMessage.getId(), userMessage.getContent());
        String skill = mapSkill(intent);
        CoachRun execution = executionManager.create(
                task, conversation.getId(), userMessage.getId(), skill, "CONVERSATION");

        eventPublisher.bindContext(conversation.getId(), assistantMessage.getId());
        SseRuntimeEmitter emitter = new SseRuntimeEmitter(sse, eventPublisher, execution.getId());
        eventPublisher.subscribe(emitter);

        try {
            String systemPrompt = policyService.promptFor(intent);
            eventPublisher.publish(execution.getId(), AgentEventType.TASK_CREATED,
                    Map.of("taskId", task.getId(), "title", task.getTitle()));
            eventPublisher.publish(execution.getId(), AgentEventType.TASK_STARTED,
                    Map.of("executionId", execution.getId(), "intent", intent, "skill", skill));

            AgentContext context = contextManager.build(
                    conversation.getAthleteId(), task.getId(), execution.getId(),
                    conversation.getId(), intent, userMessage.getContent(), systemPrompt);

            RuntimeHandler handler = selectHandler(intent);
            SkillOutcome outcome = handler.execute(context, task, execution, emitter);
            return new RuntimeResult(task.getId(), execution.getId(), intent, skill, outcome);
        } catch (Exception ex) {
            log.error("agent runtime failed", ex);
            executionManager.fail(execution, task, ex.getMessage());
            eventPublisher.publish(execution.getId(), AgentEventType.TASK_FAILED,
                    Map.of("message", ex.getMessage() == null ? "agent failed" : ex.getMessage()));
            throw new RuntimeException(ex);
        } finally {
            eventPublisher.unsubscribe(emitter);
            eventPublisher.clearContext();
        }
    }

    private RuntimeHandler selectHandler(String intent) {
        if ("TODAY_SESSION".equals(intent)) {
            return todaySessionHandler;
        }
        return chatRuntimeHandler;
    }

    private String mapSkill(String intent) {
        return "TODAY_SESSION".equals(intent) ? "today_session" : "chat";
    }

    public record RuntimeResult(Long taskId, Long executionId, String intent, String skill, SkillOutcome outcome) {
    }

    /**
     * 将 Runtime 事件映射为 SSE + 兼容旧事件名。
     */
    private static final class SseRuntimeEmitter implements RuntimeEmitter,
            com.aseantec.smartfitness.coach.runtime.event.AgentEventSubscriber {

        private final SseEmitter sse;
        private final AgentEventPublisherImpl publisher;
        private final Long executionId;

        private SseRuntimeEmitter(SseEmitter sse, AgentEventPublisherImpl publisher, Long executionId) {
            this.sse = sse;
            this.publisher = publisher;
            this.executionId = executionId;
        }

        @Override
        public void send(AgentEventType type, Object payload) throws Exception {
            publisher.publish(executionId, type, payload instanceof Map<?, ?> m
                    ? castMap(m) : Map.of("data", payload));
            String sseName = mapSseName(type);
            if (sseName != null) {
                sse.send(SseEmitter.event().name(sseName).data(payload));
            }
        }

        @Override
        public void token(String delta) throws Exception {
            Map<String, Object> payload = Map.of("delta", delta);
            publisher.publish(executionId, AgentEventType.MESSAGE_DELTA, payload);
            sse.send(SseEmitter.event().name("token").data(payload));
        }

        @Override
        public void onEvent(com.aseantec.smartfitness.coach.runtime.event.AgentEvent event) {
            // primary SSE path is send()/token(); subscriber for future multiplexing
        }

        private String mapSseName(AgentEventType type) {
            return switch (type) {
                case TOOL_CALL_STARTED -> "tool.start";
                case TOOL_CALL_COMPLETED, TOOL_CALL_FAILED -> "tool.result";
                case MESSAGE_DELTA -> "token";
                case CARD_ADVICE -> "card.advice";
                case PROGRESS -> "progress";
                case MESSAGE_COMPLETED -> null;
                case TASK_FAILED, ERROR -> "error";
                default -> null;
            };
        }

        @SuppressWarnings("unchecked")
        private Map<String, Object> castMap(Map<?, ?> map) {
            return (Map<String, Object>) map;
        }
    }
}
