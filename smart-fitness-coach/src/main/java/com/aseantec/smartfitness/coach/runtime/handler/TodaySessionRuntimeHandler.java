package com.aseantec.smartfitness.coach.runtime.handler;

import com.aseantec.smartfitness.coach.agent.CoachPolicyService;
import com.aseantec.smartfitness.coach.agent.LlmRunner;
import com.aseantec.smartfitness.coach.agent.SkillContext;
import com.aseantec.smartfitness.coach.agent.SkillEmitter;
import com.aseantec.smartfitness.coach.agent.SkillOutcome;
import com.aseantec.smartfitness.coach.agent.skill.TodaySessionCore;
import com.aseantec.smartfitness.coach.entity.Advice;
import com.aseantec.smartfitness.coach.entity.AgentTask;
import com.aseantec.smartfitness.coach.entity.CoachRun;
import com.aseantec.smartfitness.coach.runtime.context.AgentContext;
import com.aseantec.smartfitness.coach.runtime.event.AgentEventType;
import com.aseantec.smartfitness.coach.runtime.execution.ExecutionManager;
import com.aseantec.smartfitness.common.port.llm.LlmCompleteEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 今日训练 Runtime：Observe（Tool 链）→ LLM 处方 → 护栏 → Advice 卡片。
 */
@Component
@RequiredArgsConstructor
public class TodaySessionRuntimeHandler implements RuntimeHandler {

    private final ExecutionManager executionManager;
    private final TodaySessionCore todaySessionCore;
    private final LlmRunner llmRunner;
    private final CoachPolicyService policyService;

    @Override
    public String intent() {
        return "TODAY_SESSION";
    }

    @Override
    public SkillOutcome execute(AgentContext context, AgentTask task, CoachRun execution,
                                RuntimeEmitter emitter) throws Exception {
        emitter.send(AgentEventType.PROGRESS, Map.of("stage", "正在分析你的训练数据…"));
        TodaySessionCore.ObserveResult obs = todaySessionCore.observe(context.getAthleteId(),
                new TodaySessionCore.TodaySessionObserver() {
                    @Override
                    public void toolStart(String tool) throws Exception {
                        emitter.send(AgentEventType.TOOL_CALL_STARTED, Map.of("tool", tool));
                    }

                    @Override
                    public void toolResult(String tool, Object result) throws Exception {
                        emitter.send(AgentEventType.TOOL_CALL_COMPLETED, Map.of("tool", tool, "result", result));
                    }
                });

        emitter.send(AgentEventType.PROGRESS, Map.of("stage", "正在生成今日处方…"));
        SkillEmitter silent = silentEmitter(emitter);
        SkillContext skillCtx = SkillContext.builder()
                .athleteId(context.getAthleteId())
                .userText(context.getUserText())
                .l0Context(context.getL0Facts())
                .build();
        String userPrompt = "用户事实：" + context.getL0Facts()
                + "\n候选动作（retrieve 结果）：" + obs.options()
                + "\nforceRest=" + obs.forceRest()
                + "\n\n用户消息：" + context.getUserText()
                + "\n\n请输出 schema_version=1 的今日处方 JSON。";
        LlmCompleteEvent complete = llmRunner.stream(skillCtx, silent, execution.getId(), "coach",
                policyService.requireActive().getSystemPrompt(), userPrompt,
                Map.of("retrieveOptions", obs.options(), "forceRest", obs.forceRest()));

        Advice advice = todaySessionCore.guardAndPersist(
                context.getAthleteId(), execution.getId(), null, obs, complete);
        emitter.send(AgentEventType.CARD_ADVICE, Map.of(
                "adviceId", advice.getId().toString(),
                "kind", advice.getType(),
                "payload", advice.getPayload()));
        executionManager.complete(execution, task, complete.getPromptTokens(), complete.getCompletionTokens());
        return new AdviceOutcome(advice.getId().toString(), advice.getType(), advice.getPayload());
    }

    private SkillEmitter silentEmitter(RuntimeEmitter emitter) {
        return new SkillEmitter() {
            @Override
            public void send(String type, Object payload) {
                // prescription JSON — do not stream to chat bubble
            }

            @Override
            public void token(String delta) {
                // prescription JSON — do not stream to chat bubble
            }

            @Override
            public org.springframework.web.servlet.mvc.method.annotation.SseEmitter sse() {
                return null;
            }
        };
    }

    private record AdviceOutcome(String adviceId, String kind, String payloadJson) implements SkillOutcome {
        @Override
        public String text() {
            return "我根据你的准备度和动作库排了今日训练，看下面这张卡。";
        }

        @Override
        public String cardType() {
            return "advice";
        }

        @Override
        public String cardRef() {
            return adviceId;
        }
    }
}
