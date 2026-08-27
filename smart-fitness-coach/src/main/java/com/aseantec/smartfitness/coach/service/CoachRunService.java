package com.aseantec.smartfitness.coach.service;

import com.aseantec.smartfitness.athlete.service.AthleteService;
import com.aseantec.smartfitness.coach.agent.skill.TodaySessionCore;
import com.aseantec.smartfitness.coach.entity.Advice;
import com.aseantec.smartfitness.coach.entity.CoachEvent;
import com.aseantec.smartfitness.coach.entity.CoachPolicyVersion;
import com.aseantec.smartfitness.coach.entity.CoachRun;
import com.aseantec.smartfitness.coach.mapper.CoachEventMapper;
import com.aseantec.smartfitness.coach.mapper.CoachPolicyVersionMapper;
import com.aseantec.smartfitness.coach.mapper.CoachRunMapper;
import com.aseantec.smartfitness.common.exception.BizException;
import com.aseantec.smartfitness.common.exception.ErrorCode;
import com.aseantec.smartfitness.common.port.llm.LlmCompleteEvent;
import com.aseantec.smartfitness.common.port.llm.LlmGateway;
import com.aseantec.smartfitness.common.port.llm.LlmRequest;
import com.aseantec.smartfitness.common.port.llm.LlmStreamListener;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 旧 P0 教练回合入口：Observe → Retrieve → LLM 流 → 护栏 → 落 advice。
 * <p><b>已降级</b>：对话驱动主入口改为 {@code ConversationService} + {@code AgentOrchestrator}。
 * 本类保留向后兼容（旧客户端 {@code POST /v1/coach/runs}），核心逻辑复用 {@link TodaySessionCore}。
 * 不直接写 {@code session_log}。同一 athlete 同时只允许一个 RUNNING。
 */
@Service
@RequiredArgsConstructor
public class CoachRunService {

    private final AthleteService athleteService;
    private final LlmGateway llmGateway;
    private final CoachRunMapper runMapper;
    private final CoachEventMapper eventMapper;
    private final CoachPolicyVersionMapper policyVersionMapper;
    private final TodaySessionCore core;
    private final ObjectMapper objectMapper;

    /**
     * 开启 SSE 回合。先落 {@code coach_run}，再异步拉模型。
     *
     * @param trigger 可空，默认 MANUAL
     * @throws BizException {@code RUN_ACTIVE} 已有 RUNNING；{@code NOT_ONBOARDED}
     */
    public SseEmitter start(Long athleteId, String trigger) {
        athleteService.assertOnboarded(athleteId);
        CoachRun active = runMapper.selectOne(new LambdaQueryWrapper<CoachRun>()
                .eq(CoachRun::getAthleteId, athleteId)
                .eq(CoachRun::getStatus, "RUNNING")
                .last("LIMIT 1"));
        if (active != null) {
            throw new BizException(ErrorCode.RUN_ACTIVE);
        }
        CoachPolicyVersion policy = policyVersionMapper.selectOne(new LambdaQueryWrapper<CoachPolicyVersion>()
                .eq(CoachPolicyVersion::getStatus, "ACTIVE")
                .orderByDesc(CoachPolicyVersion::getVersion)
                .last("LIMIT 1"));
        if (policy == null) {
            throw new BizException(ErrorCode.SYSTEM, "no active coach policy");
        }
        CoachRun run = new CoachRun();
        run.setAthleteId(athleteId);
        run.setPolicyVersionId(policy.getId());
        run.setStatus("RUNNING");
        run.setTrigger(trigger == null ? "MANUAL" : trigger);
        run.setKeySource("SYSTEM");
        run.setStartedAt(OffsetDateTime.now(ZoneOffset.UTC));
        runMapper.insert(run);
        SseEmitter emitter = new SseEmitter(120_000L);
        Thread.startVirtualThread(() -> execute(athleteId, run, policy, emitter));
        return emitter;
    }

    private void execute(Long athleteId, CoachRun run, CoachPolicyVersion policy, SseEmitter emitter) {
        AtomicInteger seq = new AtomicInteger(1);
        try {
            send(emitter, run, seq, "run.created",
                    Map.of("runId", run.getId().toString(), "policyVersion", policy.getVersion()));
            TodaySessionCore.ObserveResult obs = core.observe(athleteId, new TodaySessionCore.TodaySessionObserver() {
                @Override
                public void toolStart(String tool) throws Exception {
                    send(emitter, run, seq, "tool.start", Map.of("tool", tool));
                }

                @Override
                public void toolResult(String tool, Object result) throws Exception {
                    send(emitter, run, seq, "tool.result", result);
                }
            });
            AtomicReference<LlmCompleteEvent> completeRef = new AtomicReference<>();
            AtomicReference<Throwable> errorRef = new AtomicReference<>();
            CountDownLatch latch = new CountDownLatch(1);
            llmGateway.chatStream(LlmRequest.builder()
                            .athleteId(athleteId)
                            .runId(run.getId())
                            .purpose("coach")
                            .systemPrompt(policy.getSystemPrompt())
                            .userPrompt("Create today's prescription JSON.")
                            .context(Map.of("retrieveOptions", obs.options(), "forceRest", obs.forceRest()))
                            .build(),
                    new LlmStreamListener() {
                        @Override
                        public void onToken(String delta) {
                            try {
                                emitter.send(SseEmitter.event().name("token").data(Map.of("delta", delta)));
                            } catch (Exception ignored) {
                                // client gone
                            }
                        }

                        @Override
                        public void onComplete(LlmCompleteEvent event) {
                            completeRef.set(event);
                            latch.countDown();
                        }

                        @Override
                        public void onError(Throwable error) {
                            errorRef.set(error);
                            latch.countDown();
                        }
                    });
            if (!latch.await(60, TimeUnit.SECONDS) || completeRef.get() == null) {
                throw new BizException(ErrorCode.LLM_UPSTREAM,
                        errorRef.get() == null ? "llm timeout or failed" : errorRef.get().getMessage());
            }
            LlmCompleteEvent complete = completeRef.get();
            Advice advice = core.guardAndPersist(athleteId, run.getId(), null, obs, complete);
            send(emitter, run, seq, "advice", Map.of(
                    "adviceId", advice.getId().toString(),
                    "kind", advice.getType(),
                    "payload", objectMapper.readValue(advice.getPayload(), Object.class)
            ));
            run.setStatus("COMPLETED");
            run.setTokenIn(complete.getPromptTokens());
            run.setTokenOut(complete.getCompletionTokens());
            run.setEndedAt(OffsetDateTime.now(ZoneOffset.UTC));
            runMapper.updateById(run);
            send(emitter, run, seq, "done", Map.of("runId", run.getId().toString()));
            emitter.complete();
        } catch (Exception ex) {
            run.setStatus("FAILED");
            run.setEndedAt(OffsetDateTime.now(ZoneOffset.UTC));
            runMapper.updateById(run);
            try {
                emitter.send(SseEmitter.event().name("error").data(Map.of("message", String.valueOf(ex.getMessage()))));
            } catch (Exception ignored) {
                // ignore
            }
            emitter.completeWithError(ex);
        }
    }

    private void send(SseEmitter emitter, CoachRun run, AtomicInteger seq, String type, Object payload) throws Exception {
        CoachEvent event = new CoachEvent();
        event.setRunId(run.getId());
        event.setConversationId(run.getConversationId());
        event.setMessageId(run.getMessageId());
        event.setSeq(seq.getAndIncrement());
        event.setType(type);
        event.setPayload(objectMapper.writeValueAsString(payload));
        eventMapper.insert(event);
        emitter.send(SseEmitter.event().name(type).data(payload));
    }
}
