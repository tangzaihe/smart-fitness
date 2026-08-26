package com.aseantec.smartfitness.readiness.service;

import com.aseantec.smartfitness.athlete.entity.AthleteConstraint;
import com.aseantec.smartfitness.athlete.entity.WellnessLog;
import com.aseantec.smartfitness.athlete.service.AthleteService;
import com.aseantec.smartfitness.readiness.entity.AthleticState;
import com.aseantec.smartfitness.readiness.mapper.AthleticStateMapper;
import com.aseantec.smartfitness.readiness.model.ReadinessInput;
import com.aseantec.smartfitness.readiness.model.ReadinessScore;
import com.aseantec.smartfitness.training.service.LoadQueryService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ReadinessService {
    private final ReadinessCalculator calculator;
    private final AthleticStateMapper stateMapper;
    private final AthleteService athleteService;
    private final LoadQueryService loadQueryService;
    private final ObjectMapper objectMapper;

    @Transactional
    public AthleticState snapshot(Long athleteId) {
        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        WellnessLog wellness = athleteService.findWellness(athleteId, today);
        if (wellness == null) {
            wellness = athleteService.findWellness(athleteId, today.minusDays(1));
        }
        double sleep = wellness == null || wellness.getSleepHours() == null ? 7.0 : wellness.getSleepHours().doubleValue();
        int fatigue = wellness == null || wellness.getSubjectiveFatigue() == null ? 5 : wellness.getSubjectiveFatigue();
        List<AthleteConstraint> constraints = athleteService.listActiveConstraints(athleteId);
        boolean medical = constraints.stream().anyMatch(c -> "MEDICAL".equals(c.getType()));
        boolean pain = constraints.stream().anyMatch(c -> "PAIN".equals(c.getType()) || "INJURY".equals(c.getType()));
        Map<String, Integer> sets = loadQueryService.completedSetsLast7Days(athleteId);
        ReadinessScore score = calculator.calculate(ReadinessInput.builder()
                .sleepHours(sleep)
                .subjectiveFatigue(fatigue)
                .sets7d(sets)
                .medical(medical)
                .painOrInjury(pain)
                .build());
        AthleticState row = new AthleticState();
        row.setAthleteId(athleteId);
        row.setAsOf(OffsetDateTime.now(ZoneOffset.UTC));
        row.setReadiness(score.getReadiness());
        row.setRecovery(score.getRecovery());
        row.setSleepHours(BigDecimal.valueOf(sleep));
        row.setSource("RULE");
        row.setCalcVersion("v1");
        try {
            row.setFatigueByMuscle(objectMapper.writeValueAsString(score.getFatigueByMuscle()));
        } catch (Exception ex) {
            row.setFatigueByMuscle("{}");
        }
        stateMapper.insert(row);
        return row;
    }

    public AthleticState current(Long athleteId) {
        AthleticState latest = stateMapper.selectOne(new LambdaQueryWrapper<AthleticState>()
                .eq(AthleticState::getAthleteId, athleteId)
                .orderByDesc(AthleticState::getAsOf)
                .last("LIMIT 1"));
        if (latest == null) {
            return snapshot(athleteId);
        }
        return latest;
    }
}
