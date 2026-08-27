package com.aseantec.smartfitness.coach.agent.skill;

import com.aseantec.smartfitness.coach.agent.CoachPolicyService;
import com.aseantec.smartfitness.coach.agent.CoachSkill;
import com.aseantec.smartfitness.coach.agent.LlmRunner;
import com.aseantec.smartfitness.coach.agent.SkillContext;
import com.aseantec.smartfitness.coach.agent.SkillEmitter;
import com.aseantec.smartfitness.coach.agent.SkillOutcome;
import com.aseantec.smartfitness.coach.entity.Advice;
import com.aseantec.smartfitness.coach.entity.CoachPolicyVersion;
import com.aseantec.smartfitness.coach.entity.CoachRun;
import com.aseantec.smartfitness.coach.mapper.CoachRunMapper;
import com.aseantec.smartfitness.common.port.llm.LlmCompleteEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Map;

/**
 * 今日训练 Skill：Observe → Retrieve → LLM 生成处方 JSON → 护栏 → 落 Advice → 卡片。
 * <p>意图 {@code TODAY_SESSION} 走本 Skill。复用 {@link TodaySessionCore} 与 {@link LlmRunner}。
 * 创建 {@code coach_run} 仅用于 usage 记账与回放，不作为产品入口。
 */
@Component
@RequiredArgsConstructor
public class TodaySessionSkill implements CoachSkill {

    private final TodaySessionCore core;
    private final LlmRunner llmRunner;
    private final CoachPolicyService policyService;
    private final CoachRunMapper runMapper;

    @Override
    public String name() {
        return "today_session";
    }

    @Override
    public SkillOutcome run(SkillContext ctx, SkillEmitter emitter) throws Exception {
        CoachPolicyVersion policy = policyService.requireActive();
        CoachRun run = createRun(ctx, policy);
        try {
            TodaySessionCore.ObserveResult obs = core.observe(ctx.getAthleteId(), new TodaySessionCore.TodaySessionObserver() {
                @Override
                public void toolStart(String tool) throws Exception {
                    emitter.toolStart(tool);
                }

                @Override
                public void toolResult(String tool, Object result) throws Exception {
                    emitter.toolResult(tool, result);
                }
            });
            // LLM 输出的是处方 JSON，不是给人读的自然语言；吞掉 token，不流到对话文本气泡。
            // 用户侧只看到 tool 进度 + 最终 advice 卡片 + assistant 文案（见 AdviceOutcome.text）。
            SkillEmitter silent = new SkillEmitter() {
                @Override
                public void send(String type, Object payload) throws Exception {
                    if ("token".equals(type)) {
                        return;
                    }
                    emitter.send(type, payload);
                }

                @Override
                public org.springframework.web.servlet.mvc.method.annotation.SseEmitter sse() {
                    return emitter.sse();
                }
            };
            emitter.send("progress", Map.of("stage", "正在生成今日处方…"));
            String userPrompt = buildUserPrompt(ctx, obs);
            LlmCompleteEvent complete = llmRunner.stream(ctx, silent, run.getId(), "coach",
                    policy.getSystemPrompt(), userPrompt,
                    Map.of("retrieveOptions", obs.options(), "forceRest", obs.forceRest()));
            Advice advice = core.guardAndPersist(ctx.getAthleteId(), run.getId(), ctx.getConversationId(), obs, complete);
            emitter.send("card.advice", Map.of(
                    "adviceId", advice.getId().toString(),
                    "kind", advice.getType(),
                    "payload", advice.getPayload()));
            completeRun(run, complete);
            return new AdviceOutcome(advice.getId().toString(), advice.getType(), advice.getPayload());
        } catch (Exception ex) {
            failRun(run);
            throw ex;
        }
    }

    private CoachRun createRun(SkillContext ctx, CoachPolicyVersion policy) {
        CoachRun run = new CoachRun();
        run.setAthleteId(ctx.getAthleteId());
        run.setPolicyVersionId(policy.getId());
        run.setStatus("RUNNING");
        run.setTrigger("CONVERSATION");
        run.setConversationId(ctx.getConversationId());
        run.setMessageId(ctx.getUserMessageId());
        run.setSkill(name());
        run.setKeySource("SYSTEM");
        run.setStartedAt(OffsetDateTime.now(ZoneOffset.UTC));
        runMapper.insert(run);
        return run;
    }

    private void completeRun(CoachRun run, LlmCompleteEvent complete) {
        run.setStatus("COMPLETED");
        run.setTokenIn(complete.getPromptTokens());
        run.setTokenOut(complete.getCompletionTokens());
        run.setEndedAt(OffsetDateTime.now(ZoneOffset.UTC));
        runMapper.updateById(run);
    }

    private void failRun(CoachRun run) {
        run.setStatus("FAILED");
        run.setEndedAt(OffsetDateTime.now(ZoneOffset.UTC));
        runMapper.updateById(run);
    }

    private String buildUserPrompt(SkillContext ctx, TodaySessionCore.ObserveResult obs) {
        return "用户事实：" + ctx.getL0Context()
                + "\n候选动作（retrieve 结果）：" + obs.options()
                + "\nforceRest=" + obs.forceRest()
                + "\n\n用户消息：" + ctx.getUserText()
                + "\n\n请输出 schema_version=1 的今日处方 JSON。";
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
