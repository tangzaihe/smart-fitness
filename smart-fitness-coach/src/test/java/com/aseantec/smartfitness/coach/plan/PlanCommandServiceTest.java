package com.aseantec.smartfitness.coach.plan;

import com.aseantec.smartfitness.coach.entity.Advice;
import com.aseantec.smartfitness.coach.entity.TrainingPlan;
import com.aseantec.smartfitness.coach.mapper.AdviceMapper;
import com.aseantec.smartfitness.coach.mapper.TrainingPlanMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PlanCommandServiceTest {

    private static final Long ATHLETE = 100L;
    private static final LocalDate DATE = LocalDate.of(2026, 8, 28);

    @Mock
    private TrainingPlanMapper planMapper;
    @Mock
    private AdviceMapper adviceMapper;
    @InjectMocks
    private PlanCommandService service;

    @Test
    void claimDaySupersedesExistingUserPlan() {
        TrainingPlan existing = dayPlan(1L, PlanSource.USER, PlanStatus.ACTIVE);
        when(planMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(existing));
        when(planMapper.insert(any(TrainingPlan.class))).thenAnswer(inv -> {
            TrainingPlan p = inv.getArgument(0);
            p.setId(2L);
            return 1;
        });

        DayPlanCommand cmd = DayPlanCommand.builder()
                .planDate(DATE)
                .planSource(PlanSource.AGENT)
                .label("轻量 upper")
                .sessionTemplate("{\"patterns\":[\"ISOLATION\"]}")
                .build();
        TrainingPlan created = service.claimDay(ATHLETE, cmd);

        assertEquals(2L, created.getId());
        assertEquals(PlanSource.AGENT.name(), created.getPlanSource());
        assertEquals(PlanStatus.SUPERSEDED.name(), existing.getStatus());
        verify(adviceMapper, atLeastOnce()).update(any(), any());
    }

    @Test
    void adoptCycleClaimsEachDayAndCanReplaceSameDate() {
        TrainingPlan oldCycle = cyclePlan(10L, PlanStatus.ACTIVE);
        TrainingPlan draft = cyclePlan(11L, PlanStatus.DRAFT);
        when(planMapper.selectById(11L)).thenReturn(draft);
        when(planMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(oldCycle);
        when(planMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of());
        when(planMapper.insert(any(TrainingPlan.class))).thenReturn(1);

        CyclePlanCommand command = CyclePlanCommand.builder()
                .title("4 周增肌")
                .goal("HYPERTROPHY")
                .startedOn(DATE)
                .payload("{\"schema_version\":1}")
                .days(List.of(
                        DayPlanCommand.builder()
                                .planDate(DATE)
                                .planSource(PlanSource.COACH)
                                .label("推日")
                                .sessionTemplate("{}")
                                .build()
                ))
                .build();

        TrainingPlan adopted = service.adoptCycle(ATHLETE, 11L, command, true);
        assertNotNull(adopted);
        assertEquals(PlanStatus.ABANDONED.name(), oldCycle.getStatus());

        ArgumentCaptor<TrainingPlan> insertCaptor = ArgumentCaptor.forClass(TrainingPlan.class);
        verify(planMapper, atLeastOnce()).insert(insertCaptor.capture());
        TrainingPlan insertedDay = insertCaptor.getAllValues().stream()
                .filter(p -> PlanScope.DAY.name().equals(p.getScope()))
                .findFirst()
                .orElseThrow();
        assertEquals(PlanSource.COACH.name(), insertedDay.getPlanSource());
        assertEquals(DATE, insertedDay.getPlanDate());
    }

    private TrainingPlan dayPlan(Long id, PlanSource source, PlanStatus status) {
        TrainingPlan p = new TrainingPlan();
        p.setId(id);
        p.setAthleteId(ATHLETE);
        p.setScope(PlanScope.DAY.name());
        p.setPlanSource(source.name());
        p.setStatus(status.name());
        p.setPlanDate(DATE);
        return p;
    }

    private TrainingPlan cyclePlan(Long id, PlanStatus status) {
        TrainingPlan p = new TrainingPlan();
        p.setId(id);
        p.setAthleteId(ATHLETE);
        p.setScope(PlanScope.CYCLE.name());
        p.setPlanSource(PlanSource.COACH.name());
        p.setStatus(status.name());
        return p;
    }
}
