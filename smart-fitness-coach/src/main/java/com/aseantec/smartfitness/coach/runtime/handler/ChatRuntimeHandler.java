package com.aseantec.smartfitness.coach.runtime.handler;

import com.aseantec.smartfitness.coach.agent.CoachPolicyService;
import com.aseantec.smartfitness.coach.agent.LlmRunner;
import com.aseantec.smartfitness.coach.agent.SkillContext;
import com.aseantec.smartfitness.coach.agent.SkillEmitter;
import com.aseantec.smartfitness.coach.agent.SkillOutcome;
import com.aseantec.smartfitness.coach.entity.AgentTask;
import com.aseantec.smartfitness.coach.entity.CoachRun;
import com.aseantec.smartfitness.coach.runtime.action.*;
import com.aseantec.smartfitness.coach.runtime.context.AgentContext;
import com.aseantec.smartfitness.coach.runtime.event.AgentEventType;
import com.aseantec.smartfitness.coach.runtime.execution.ExecutionManager;
import com.aseantec.smartfitness.coach.runtime.execution.ExecutionState;
import com.aseantec.smartfitness.coach.runtime.loop.AgentLoop;
import com.aseantec.smartfitness.coach.runtime.loop.LoopResult;
import com.aseantec.smartfitness.common.port.llm.LlmCompleteEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * 闲聊 / 计划咨询 Runtime：AgentLoop + Tool Calling + LLM 回答。
 */
@Component
@RequiredArgsConstructor
public class ChatRuntimeHandler implements RuntimeHandler {

    private static final Set<String> TRAINING_COUNT_KEYS = Set.of(
            "训练了几次", "练了几次", "上了几节课", "上了几节", "训练次数", "练了多少次");

    private final AgentLoop agentLoop;
    private final ExecutionManager executionManager;
    private final LlmRunner llmRunner;
    private final CoachPolicyService policyService;

    @Override
    public String intent() {
        return "CHAT";
    }

    @Override
    public SkillOutcome execute(AgentContext context, AgentTask task, CoachRun execution,
                                RuntimeEmitter emitter) throws Exception {
        String routedIntent = context.getIntent();
        if ("WORKOUT_PLANNING".equals(routedIntent)) {
            return executePlanning(context, task, execution, emitter);
        }
        if ("REST".equals(routedIntent)) {
            return new SimpleOutcome("今天如果想休息完全可以，恢复也是训练的一部分。需要我帮你标记休息吗？");
        }
        return executeChat(context, task, execution, emitter);
    }

    private SkillOutcome executeChat(AgentContext context, AgentTask task, CoachRun execution,
                                     RuntimeEmitter emitter) throws Exception {
        LoopResult result = agentLoop.run(context, (ctx, state) -> {
            if (needsTrainingRecords(ctx) && !ctx.getToolResults().containsKey("getTrainingRecords")) {
                return new ToolCallAction("getTrainingRecords", Map.of("days", 30));
            }
            return new EndToolPhaseAction();
        }, (state, iteration) -> {
            executionManager.updateState(execution, state, iteration);
            return null;
        }, false);

        if (result.status() == LoopResult.Status.FAILED) {
            throw new IllegalStateException(result.error());
        }
        AgentContext enriched = result.finalContext() != null ? result.finalContext() : context;
        String systemPrompt = policyService.promptFor("CHAT");
        SkillEmitter llmEmitter = toSkillEmitter(emitter);
        SkillContext skillCtx = SkillContext.builder()
                .athleteId(context.getAthleteId())
                .conversationId(null)
                .userText(context.toPromptBlock())
                .l0Context(context.getL0Facts())
                .build();
        LlmCompleteEvent complete = llmRunner.stream(skillCtx, llmEmitter, execution.getId(), "chat",
                systemPrompt, enriched.toPromptBlock(),
                Map.of("toolResults", enriched.getToolResults()));
        executionManager.complete(execution, task, complete.getPromptTokens(), complete.getCompletionTokens());
        return new SimpleOutcome(complete.getText());
    }

    private SkillOutcome executePlanning(AgentContext context, AgentTask task, CoachRun execution,
                                         RuntimeEmitter emitter) throws Exception {
        if (context.getPendingUserAnswer() == null && lacksPlanningInfo(context.getUserText())) {
            LoopResult waiting = agentLoop.run(context, (ctx, state) ->
                            new AskUserAction("好的，先告诉我：你每周大概能训练几天？每次大概多久？"),
                    (state, iteration) -> {
                        executionManager.updateState(execution, state, iteration);
                        return null;
                    }, false);
            executionManager.markWaitingUser(task, execution, waiting.pendingArguments() == null
                    ? "" : String.valueOf(waiting.pendingArguments()));
            emitter.send(AgentEventType.MESSAGE_COMPLETED, Map.of("text", waiting.pendingQuestion()));
            return new SimpleOutcome(waiting.pendingQuestion());
        }
        emitter.send(AgentEventType.PLAN_CREATED, Map.of("steps", List.of(
                "了解目标", "评估当前状态", "生成周期计划草案", "等待你确认采纳")));
        String systemPrompt = policyService.promptFor("WORKOUT_PLANNING");
        SkillEmitter llmEmitter = toSkillEmitter(emitter);
        SkillContext skillCtx = SkillContext.builder()
                .athleteId(context.getAthleteId())
                .userText(context.toPromptBlock())
                .l0Context(context.getL0Facts())
                .build();
        LlmCompleteEvent complete = llmRunner.stream(skillCtx, llmEmitter, execution.getId(), "plan",
                systemPrompt, context.toPromptBlock(), null);
        executionManager.complete(execution, task, complete.getPromptTokens(), complete.getCompletionTokens());
        return new SimpleOutcome(complete.getText());
    }

    private boolean needsTrainingRecords(AgentContext ctx) {
        String lower = ctx.getUserText().toLowerCase(Locale.ROOT);
        return TRAINING_COUNT_KEYS.stream().anyMatch(lower::contains);
    }

    private boolean lacksPlanningInfo(String text) {
        String lower = text == null ? "" : text.toLowerCase(Locale.ROOT);
        return !(lower.contains("每周") || lower.contains("一周") || lower.contains("天")
                || lower.matches(".*\\d+.*"));
    }

    private SkillEmitter toSkillEmitter(RuntimeEmitter emitter) {
        return new SkillEmitter() {
            @Override
            public void send(String type, Object payload) throws Exception {
                if ("token".equals(type)) {
                    return;
                }
                emitter.send(AgentEventType.MESSAGE_DELTA, payload);
            }

            @Override
            public void token(String delta) throws Exception {
                emitter.token(delta);
            }

            @Override
            public org.springframework.web.servlet.mvc.method.annotation.SseEmitter sse() {
                return null;
            }
        };
    }

    private record SimpleOutcome(String text) implements SkillOutcome {
    }
}
