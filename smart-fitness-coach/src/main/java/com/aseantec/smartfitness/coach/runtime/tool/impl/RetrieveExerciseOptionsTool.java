package com.aseantec.smartfitness.coach.runtime.tool.impl;

import com.aseantec.smartfitness.athlete.entity.AthleteConstraint;
import com.aseantec.smartfitness.athlete.service.AthleteService;
import com.aseantec.smartfitness.athlete.vo.AthleteVO;
import com.aseantec.smartfitness.coach.agent.skill.TodaySessionCore;
import com.aseantec.smartfitness.coach.runtime.tool.AgentTool;
import com.aseantec.smartfitness.coach.runtime.tool.ToolPermission;
import com.aseantec.smartfitness.coach.runtime.tool.ToolResult;
import com.aseantec.smartfitness.knowledge.port.KnowledgePort;
import com.aseantec.smartfitness.knowledge.port.RetrieveQuery;
import com.aseantec.smartfitness.readiness.entity.AthleticState;
import com.aseantec.smartfitness.readiness.service.ReadinessService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 按器械/偏好/伤痛约束召回候选动作。对应 {@code retrieve_exercise_options}。
 */
@Component
@RequiredArgsConstructor
public class RetrieveExerciseOptionsTool implements AgentTool {

    private final AthleteService athleteService;
    private final ReadinessService readinessService;
    private final KnowledgePort knowledgePort;

    @Override
    public String name() {
        return "retrieveExerciseOptions";
    }

    @Override
    public String description() {
        return "按用户器材、偏好与伤痛约束检索候选动作";
    }

    @Override
    public ToolPermission permission() {
        return ToolPermission.READ;
    }

    @Override
    public ToolResult execute(Long athleteId, Map<String, Object> arguments) {
        AthleteVO athlete = athleteService.getMe(athleteId);
        AthleticState state = readinessService.current(athleteId);
        List<AthleteConstraint> constraints = athleteService.listActiveConstraints(athleteId);
        Set<String> exclude = new HashSet<>();
        boolean medical = false;
        for (AthleteConstraint constraint : constraints) {
            if ("MEDICAL".equals(constraint.getType())) {
                medical = true;
            }
            if (constraint.getBodyPart() != null && TodaySessionCore.BODY_EXCLUDE.containsKey(constraint.getBodyPart())) {
                exclude.addAll(TodaySessionCore.BODY_EXCLUDE.get(constraint.getBodyPart()));
            }
        }
        List<Map<String, Object>> options = knowledgePort.retrieve(RetrieveQuery.builder()
                .equipment(athlete.getEquipment() == null ? List.of() : athlete.getEquipment())
                .liked(prefList(athlete.getPreferences(), "liked"))
                .disliked(prefList(athlete.getPreferences(), "disliked"))
                .never(prefList(athlete.getPreferences(), "never"))
                .excludePatterns(exclude)
                .limit(8)
                .build());
        boolean forceRest = medical || state.getReadiness() < 40;
        return ToolResult.success(Map.of(
                "options", options,
                "readiness", state.getReadiness(),
                "forceRest", forceRest,
                "excludePatterns", exclude));
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
}
