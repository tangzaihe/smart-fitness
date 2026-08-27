package com.aseantec.smartfitness.coach.agent.skill;

import com.aseantec.smartfitness.athlete.entity.AthleteConstraint;
import com.aseantec.smartfitness.athlete.service.AthleteService;
import com.aseantec.smartfitness.athlete.vo.AthleteVO;
import com.aseantec.smartfitness.coach.entity.Advice;
import com.aseantec.smartfitness.coach.guardrail.GuardContext;
import com.aseantec.smartfitness.coach.guardrail.GuardrailService;
import com.aseantec.smartfitness.coach.mapper.AdviceMapper;
import com.aseantec.smartfitness.common.port.llm.LlmCompleteEvent;
import com.aseantec.smartfitness.knowledge.port.KnowledgePort;
import com.aseantec.smartfitness.knowledge.port.RetrieveQuery;
import com.aseantec.smartfitness.readiness.entity.AthleticState;
import com.aseantec.smartfitness.readiness.service.ReadinessService;
import com.aseantec.smartfitness.training.entity.ExerciseCatalog;
import com.aseantec.smartfitness.training.service.CatalogService;
import com.aseantec.smartfitness.training.service.LoadQueryService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.Builder;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 今日训练 Skill 的共享内核：Observe → Retrieve → 护栏 → 落 Advice。
 * <p>从 {@code CoachRunService.execute} 抽出，供 {@link TodaySessionSkill} 与旧 P0 路径复用。
 * 不发 SSE、不创建 coach_run；run/conversation 关联由调用方在落 Advice 前注入。
 */
@Component
@RequiredArgsConstructor
public class TodaySessionCore {

    /** 部位 → 受限动作模式。医学护栏 G1 的硬编码映射。 */
    public static final Map<String, Set<String>> BODY_EXCLUDE = Map.of(
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
    private final GuardrailService guardrailService;
    private final AdviceMapper adviceMapper;
    private final ObjectMapper objectMapper;

    /**
     * 执行观察 + 检索，返回候选动作与护栏上下文所需的中间态。
     *
     * @param observer 用于回传 tool.start/result 的 emitter，可空
     */
    public ObserveResult observe(Long athleteId, TodaySessionObserver observer) throws Exception {
        AthleteVO athlete = athleteService.getMe(athleteId);
        AthleticState state = readinessService.current(athleteId);
        List<AthleteConstraint> constraints = athleteService.listActiveConstraints(athleteId);
        if (observer != null) {
            observer.toolStart("observe");
        }
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
        if (observer != null) {
            observer.toolResult("observe", Map.of("readiness", state.getReadiness()));
        }
        if (observer != null) {
            observer.toolStart("retrieve");
        }
        List<Map<String, Object>> options = knowledgePort.retrieve(RetrieveQuery.builder()
                .equipment(athlete.getEquipment() == null ? List.of() : athlete.getEquipment())
                .liked(prefList(athlete.getPreferences(), "liked"))
                .disliked(prefList(athlete.getPreferences(), "disliked"))
                .never(prefList(athlete.getPreferences(), "never"))
                .excludePatterns(exclude)
                .limit(8)
                .build());
        if (observer != null) {
            observer.toolResult("retrieve", Map.of("size", options.size()));
        }
        boolean forceRest = medical || state.getReadiness() < 40;
        return new ObserveResult(athlete, state, constraints, exclude, medical, options, forceRest);
    }

    /**
     * 对 LLM 输出跑护栏并落 Advice。返回新建的 Advice（PENDING）。
     *
     * @param runId          关联 coach_run.id，可空（对话路径由 Skill 创建 run）
     * @param conversationId 关联 conversation.id，可空
     */
    public Advice guardAndPersist(Long athleteId, Long runId, Long conversationId,
                                   ObserveResult obs, LlmCompleteEvent complete) throws Exception {
        JsonNode raw = objectMapper.readTree(complete.getText());
        Map<String, ExerciseCatalog> catalog = catalogService.mapByCode();
        Map<String, String> toPattern = new HashMap<>();
        Map<String, String> toMuscle = new HashMap<>();
        Map<String, String> toSwap = new HashMap<>();
        Set<String> retrieveCodes = new HashSet<>();
        for (Map<String, Object> option : obs.options()) {
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
                .readiness(obs.state().getReadiness())
                .avgFatigue(0)
                .medical(obs.medical())
                .excludePatterns(obs.exclude())
                .recentMuscles(loadQueryService.musclesCompletedWithinHours(athleteId, 48))
                .retrieveCodes(retrieveCodes)
                .codeToPattern(toPattern)
                .codeToMuscle(toMuscle)
                .codeToSwapGroup(toSwap)
                .build());
        Advice advice = new Advice();
        advice.setRunId(runId);
        advice.setConversationId(conversationId);
        advice.setAthleteId(athleteId);
        advice.setType(guarded.path("kind").asText("SESSION"));
        advice.setPayload(objectMapper.writeValueAsString(guarded));
        advice.setEvidence(objectMapper.writeValueAsString(Map.of(
                "readiness", obs.state().getReadiness(),
                "calcVersion", "v1")));
        advice.setRiskLevel(obs.medical() ? "HIGH" : "LOW");
        advice.setConfirmRequired(true);
        advice.setStatus("PENDING");
        adviceMapper.insert(advice);
        return advice;
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

    /** 观察阶段产出。 */
    public record ObserveResult(
            com.aseantec.smartfitness.athlete.vo.AthleteVO athlete,
            com.aseantec.smartfitness.readiness.entity.AthleticState state,
            java.util.List<com.aseantec.smartfitness.athlete.entity.AthleteConstraint> constraints,
            java.util.Set<String> exclude,
            boolean medical,
            java.util.List<java.util.Map<String, Object>> options,
            boolean forceRest) {
    }

    /** 观察阶段回调，用于向 SSE 发 tool 事件。 */
    public interface TodaySessionObserver {
        void toolStart(String tool) throws Exception;

        void toolResult(String tool, Object result) throws Exception;
    }
}
