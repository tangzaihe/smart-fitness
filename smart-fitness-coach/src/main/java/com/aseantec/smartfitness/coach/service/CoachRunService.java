package com.aseantec.smartfitness.coach.service;

import com.aseantec.smartfitness.athlete.entity.AthleteConstraint;
import com.aseantec.smartfitness.athlete.service.AthleteService;
import com.aseantec.smartfitness.athlete.vo.AthleteVO;
import com.aseantec.smartfitness.coach.entity.Advice;
import com.aseantec.smartfitness.coach.entity.CoachEvent;
import com.aseantec.smartfitness.coach.entity.CoachPolicyVersion;
import com.aseantec.smartfitness.coach.entity.CoachRun;
import com.aseantec.smartfitness.coach.guardrail.GuardContext;
import com.aseantec.smartfitness.coach.guardrail.GuardrailService;
import com.aseantec.smartfitness.coach.mapper.AdviceMapper;
import com.aseantec.smartfitness.coach.mapper.CoachEventMapper;
import com.aseantec.smartfitness.coach.mapper.CoachPolicyVersionMapper;
import com.aseantec.smartfitness.coach.mapper.CoachRunMapper;
import com.aseantec.smartfitness.common.exception.BizException;
import com.aseantec.smartfitness.common.exception.ErrorCode;
import com.aseantec.smartfitness.common.port.llm.LlmCompleteEvent;
import com.aseantec.smartfitness.common.port.llm.LlmGateway;
import com.aseantec.smartfitness.common.port.llm.LlmRequest;
import com.aseantec.smartfitness.common.port.llm.LlmStreamListener;
import com.aseantec.smartfitness.knowledge.port.KnowledgePort;
import com.aseantec.smartfitness.knowledge.port.RetrieveQuery;
import com.aseantec.smartfitness.readiness.entity.AthleticState;
import com.aseantec.smartfitness.readiness.service.ReadinessService;
import com.aseantec.smartfitness.training.entity.ExerciseCatalog;
import com.aseantec.smartfitness.training.service.CatalogService;
import com.aseantec.smartfitness.training.service.LoadQueryService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 教练回合编排：Observe → Retrieve → LLM 流 → 护栏 → 落 advice。
 * <p>不直接写 {@code session_log}。同一 athlete 同时只允许一个 RUNNING。
 */
@Service
@RequiredArgsConstructor
public class CoachRunService {
    private static final Map<String, Set<String>> BODY_EXCLUDE = Map.of(
            "SHOULDER", Set.of("VERTICAL_PUSH"),
            "ELBOW", Set.of("HORIZONTAL_PUSH", "VERTICAL_PUSH", "ISOLATION"),
            "KNEE", Set.of("SQUAT", "LUNGE"),
            "LOWER_BACK", Set.of("HINGE"),
            "WRIST", Set.of("HORIZONTAL_PUSH", "VERTICAL_PUSH")
    );

    private final AthleteService athleteService;
    private final ReadinessService readinessService;
    private final LoadQueryService loadQueryService;
    private final KnowledgePort knowledgePort;
    private final CatalogService catalogService;
    private final LlmGateway llmGateway;
    private final GuardrailService guardrailService;
    private final CoachRunMapper runMapper;
    private final CoachEventMapper eventMapper;
    private final AdviceMapper adviceMapper;
    private final CoachPolicyVersionMapper policyVersionMapper;
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
            send(emitter, run.getId(), seq, "run.created",
                    Map.of("runId", run.getId().toString(), "policyVersion", policy.getVersion()));
            AthleteVO athlete = athleteService.getMe(athleteId);
            AthleticState state = readinessService.current(athleteId);
            List<AthleteConstraint> constraints = athleteService.listActiveConstraints(athleteId);
            send(emitter, run.getId(), seq, "tool.start", Map.of("tool", "observe"));
            Set<String> exclude = new HashSet<>();
            boolean medical = false;
            for (AthleteConstraint constraint : constraints) {
                if ("MEDICAL".equals(constraint.getType())) {
                    medical = true;
                }
                if (constraint.getBodyPart() != null && BODY_EXCLUDE.containsKey(constraint.getBodyPart())) {
                    exclude.addAll(BODY_EXCLUDE.get(constraint.getBodyPart()));
                }
            }
            send(emitter, run.getId(), seq, "tool.result",
                    Map.of("tool", "observe", "readiness", state.getReadiness()));
            send(emitter, run.getId(), seq, "tool.start", Map.of("tool", "retrieve"));
            List<Map<String, Object>> options = knowledgePort.retrieve(RetrieveQuery.builder()
                    .equipment(athlete.getEquipment() == null ? List.of() : athlete.getEquipment())
                    .liked(prefList(athlete.getPreferences(), "liked"))
                    .disliked(prefList(athlete.getPreferences(), "disliked"))
                    .never(prefList(athlete.getPreferences(), "never"))
                    .excludePatterns(exclude)
                    .limit(8)
                    .build());
            send(emitter, run.getId(), seq, "tool.result", Map.of("tool", "retrieve", "size", options.size()));
            boolean forceRest = medical || state.getReadiness() < 40;
            AtomicReference<LlmCompleteEvent> completeRef = new AtomicReference<>();
            AtomicReference<Throwable> errorRef = new AtomicReference<>();
            CountDownLatch latch = new CountDownLatch(1);
            llmGateway.chatStream(LlmRequest.builder()
                            .athleteId(athleteId)
                            .runId(run.getId())
                            .purpose("coach")
                            .systemPrompt(policy.getSystemPrompt())
                            .userPrompt("Create today's prescription JSON.")
                            .context(Map.of("retrieveOptions", options, "forceRest", forceRest))
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
            JsonNode raw = objectMapper.readTree(complete.getText());
            Map<String, ExerciseCatalog> catalog = catalogService.mapByCode();
            Map<String, String> toPattern = new HashMap<>();
            Map<String, String> toMuscle = new HashMap<>();
            Map<String, String> toSwap = new HashMap<>();
            Set<String> retrieveCodes = new HashSet<>();
            for (Map<String, Object> option : options) {
                String code = String.valueOf(option.get("code"));
                retrieveCodes.add(code);
                ExerciseCatalog exercise = catalog.get(code);
                if (exercise != null) {
                    toPattern.put(code, exercise.getPattern());
                    toMuscle.put(code, exercise.getMuscleGroup());
                    toSwap.put(code, exercise.getSwapGroup());
                }
            }
            ObjectNode guarded = guardrailService.apply(raw, GuardContext.builder()
                    .readiness(state.getReadiness())
                    .avgFatigue(0)
                    .medical(medical)
                    .excludePatterns(exclude)
                    .recentMuscles(loadQueryService.musclesCompletedWithinHours(athleteId, 48))
                    .retrieveCodes(retrieveCodes)
                    .codeToPattern(toPattern)
                    .codeToMuscle(toMuscle)
                    .codeToSwapGroup(toSwap)
                    .build());
            Advice advice = new Advice();
            advice.setRunId(run.getId());
            advice.setAthleteId(athleteId);
            advice.setType(guarded.path("kind").asText("SESSION"));
            advice.setPayload(objectMapper.writeValueAsString(guarded));
            advice.setEvidence(objectMapper.writeValueAsString(Map.of(
                    "readiness", state.getReadiness(),
                    "calcVersion", "v1")));
            advice.setRiskLevel(medical ? "HIGH" : "LOW");
            advice.setConfirmRequired(true);
            advice.setStatus("PENDING");
            adviceMapper.insert(advice);
            send(emitter, run.getId(), seq, "advice", Map.of(
                    "adviceId", advice.getId().toString(),
                    "kind", advice.getType(),
                    "payload", objectMapper.readValue(advice.getPayload(), Object.class)
            ));
            run.setStatus("COMPLETED");
            run.setTokenIn(complete.getPromptTokens());
            run.setTokenOut(complete.getCompletionTokens());
            run.setEndedAt(OffsetDateTime.now(ZoneOffset.UTC));
            runMapper.updateById(run);
            send(emitter, run.getId(), seq, "done", Map.of("runId", run.getId().toString()));
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

    private List<String> prefList(Object preferences, String key) {
        if (preferences instanceof Map<?, ?> map) {
            Object value = map.get(key);
            if (value instanceof List<?> list) {
                return list.stream().map(String::valueOf).toList();
            }
        }
        return List.of();
    }

    private void send(SseEmitter emitter, Long runId, AtomicInteger seq, String type, Object payload) throws Exception {
        CoachEvent event = new CoachEvent();
        event.setRunId(runId);
        event.setSeq(seq.getAndIncrement());
        event.setType(type);
        event.setPayload(objectMapper.writeValueAsString(payload));
        eventMapper.insert(event);
        emitter.send(SseEmitter.event().name(type).data(payload));
    }
}
