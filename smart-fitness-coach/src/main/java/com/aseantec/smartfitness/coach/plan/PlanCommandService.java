package com.aseantec.smartfitness.coach.plan;

import com.aseantec.smartfitness.coach.entity.Advice;
import com.aseantec.smartfitness.coach.entity.TrainingPlan;
import com.aseantec.smartfitness.coach.mapper.AdviceMapper;
import com.aseantec.smartfitness.coach.mapper.TrainingPlanMapper;
import com.aseantec.smartfitness.common.exception.BizException;
import com.aseantec.smartfitness.common.exception.ErrorCode;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

/**
 * 训练计划写入口：所有日级/周期计划变更必须经本 Service，保证 plan_source 互斥与 status 废弃链。
 * <p>不变量：同一 athlete+plan_date 仅一条 scope=DAY 且 status∈{PENDING,ACTIVE}；
 * 同一 athlete 仅一条 scope=CYCLE 且 status=ACTIVE。
 */
@Service
@RequiredArgsConstructor
public class PlanCommandService {

    private static final Set<String> ACTIVE_DAY_STATUSES = Set.of(
            PlanStatus.PENDING.name(), PlanStatus.ACTIVE.name());

    private final TrainingPlanMapper planMapper;
    private final AdviceMapper adviceMapper;

    /**
     * 查询某日权威日计划（ACTIVE 或 PENDING）。
     */
    public TrainingPlan findActiveDay(Long athleteId, LocalDate planDate) {
        return planMapper.selectOne(new LambdaQueryWrapper<TrainingPlan>()
                .eq(TrainingPlan::getAthleteId, athleteId)
                .eq(TrainingPlan::getScope, PlanScope.DAY.name())
                .eq(TrainingPlan::getPlanDate, planDate)
                .in(TrainingPlan::getStatus, ACTIVE_DAY_STATUSES)
                .last("LIMIT 1"));
    }

    /**
     * 查询当前 ACTIVE 周期计划。
     */
    public TrainingPlan findActiveCycle(Long athleteId) {
        return planMapper.selectOne(new LambdaQueryWrapper<TrainingPlan>()
                .eq(TrainingPlan::getAthleteId, athleteId)
                .eq(TrainingPlan::getScope, PlanScope.CYCLE.name())
                .eq(TrainingPlan::getStatus, PlanStatus.ACTIVE.name())
                .last("LIMIT 1"));
    }

    /**
     * 声明某日计划：先将同日已有 ACTIVE/PENDING 行标记 SUPERSEDED，再插入新行。
     * <p>适用于 USER / AGENT / COACH 任意 plan_source；后写覆盖先写。
     *
     * @return 新建的 ACTIVE 日计划
     */
    @Transactional
    public TrainingPlan claimDay(Long athleteId, DayPlanCommand command) {
        supersedeActiveDays(athleteId, command.getPlanDate(), null);
        expirePendingAdvice(athleteId, command.getPlanDate());

        TrainingPlan row = new TrainingPlan();
        row.setAthleteId(athleteId);
        row.setScope(PlanScope.DAY.name());
        row.setPlanSource(command.getPlanSource().name());
        row.setStatus(PlanStatus.ACTIVE.name());
        row.setPlanDate(command.getPlanDate());
        row.setLabel(command.getLabel());
        row.setSessionTemplate(command.getSessionTemplate());
        row.setParentId(command.getParentId());
        planMapper.insert(row);

        markSupersededBy(athleteId, command.getPlanDate(), row.getId());
        return row;
    }

    /**
     * 创建周期 DRAFT（尚未物化日计划）。
     */
    @Transactional
    public TrainingPlan createCycleDraft(Long athleteId, CyclePlanCommand command) {
        TrainingPlan row = new TrainingPlan();
        row.setAthleteId(athleteId);
        row.setScope(PlanScope.CYCLE.name());
        row.setPlanSource(PlanSource.COACH.name());
        row.setStatus(PlanStatus.DRAFT.name());
        row.setTitle(command.getTitle());
        row.setGoal(command.getGoal());
        row.setStartedOn(command.getStartedOn());
        row.setConversationId(command.getConversationId());
        row.setPayload(command.getPayload());
        row.setDurationDays(command.getDays() == null ? null : command.getDays().size());
        row.setCurrentDayIndex(1);
        planMapper.insert(row);
        return row;
    }

    /**
     * 采纳周期计划：旧 ACTIVE 周期 → ABANDONED；逐日 claimDay(COACH)，覆盖同日已有任意 plan_source。
     *
     * @param confirmReplace 必须为 true 当已存在 ACTIVE 周期
     */
    @Transactional
    public TrainingPlan adoptCycle(Long athleteId, Long cyclePlanId, CyclePlanCommand command,
                                   boolean confirmReplace) {
        TrainingPlan cycle = requireOwnedCycle(athleteId, cyclePlanId);
        TrainingPlan existingActive = findActiveCycle(athleteId);
        if (existingActive != null && !existingActive.getId().equals(cyclePlanId)) {
            if (!confirmReplace) {
                throw new BizException(ErrorCode.BAD_REQUEST, "replacing active cycle requires confirmReplace");
            }
            abandonCycle(existingActive);
        }

        cycle.setStatus(PlanStatus.ACTIVE.name());
        cycle.setTitle(command.getTitle());
        cycle.setGoal(command.getGoal());
        cycle.setStartedOn(command.getStartedOn());
        cycle.setPayload(command.getPayload());
        cycle.setDurationDays(command.getDays() == null ? null : command.getDays().size());
        cycle.setCurrentDayIndex(1);
        planMapper.updateById(cycle);

        if (command.getDays() != null) {
            int index = 1;
            for (DayPlanCommand day : command.getDays()) {
                claimDay(athleteId, DayPlanCommand.builder()
                        .planDate(day.getPlanDate())
                        .planSource(PlanSource.COACH)
                        .label(day.getLabel())
                        .sessionTemplate(day.getSessionTemplate())
                        .parentId(cycle.getId())
                        .build());
                index++;
            }
        }
        return cycle;
    }

    /**
     * 将日计划标记完成（开练并完成课后调用）。
     */
    @Transactional
    public void markDayDone(Long dayPlanId) {
        TrainingPlan day = planMapper.selectById(dayPlanId);
        if (day == null) {
            return;
        }
        day.setStatus(PlanStatus.DONE.name());
        planMapper.updateById(day);
    }

    private void supersedeActiveDays(Long athleteId, LocalDate planDate, Long supersededBy) {
        List<TrainingPlan> active = planMapper.selectList(new LambdaQueryWrapper<TrainingPlan>()
                .eq(TrainingPlan::getAthleteId, athleteId)
                .eq(TrainingPlan::getScope, PlanScope.DAY.name())
                .eq(TrainingPlan::getPlanDate, planDate)
                .in(TrainingPlan::getStatus, ACTIVE_DAY_STATUSES));
        for (TrainingPlan old : active) {
            old.setStatus(PlanStatus.SUPERSEDED.name());
            if (supersededBy != null) {
                old.setSupersededBy(supersededBy);
            }
            planMapper.updateById(old);
            if (PlanSource.COACH.name().equals(old.getPlanSource()) && old.getParentId() != null) {
                markCoachDayAdjusted(old.getParentId(), planDate);
            }
        }
    }

    /** 插入新行后，回填同日已 SUPERSEDED 但 superseded_by 为空的行的指针。 */
    private void markSupersededBy(Long athleteId, LocalDate planDate, Long newId) {
        planMapper.update(null, new UpdateWrapper<TrainingPlan>()
                .eq("athlete_id", athleteId)
                .eq("scope", PlanScope.DAY.name())
                .eq("plan_date", planDate)
                .eq("status", PlanStatus.SUPERSEDED.name())
                .isNull("superseded_by")
                .set("superseded_by", newId));
    }

    private void markCoachDayAdjusted(Long cycleId, LocalDate planDate) {
        planMapper.update(null, new UpdateWrapper<TrainingPlan>()
                .eq("parent_id", cycleId)
                .eq("scope", PlanScope.DAY.name())
                .eq("plan_date", planDate)
                .eq("plan_source", PlanSource.COACH.name())
                .eq("status", PlanStatus.SUPERSEDED.name())
                .set("status", PlanStatus.ADJUSTED.name()));
    }

    private void expirePendingAdvice(Long athleteId, LocalDate planDate) {
        adviceMapper.update(null, new UpdateWrapper<Advice>()
                .eq("athlete_id", athleteId)
                .eq("plan_date", planDate)
                .eq("status", "PENDING")
                .set("status", "EXPIRED"));
    }

    private void abandonCycle(TrainingPlan cycle) {
        cycle.setStatus(PlanStatus.ABANDONED.name());
        planMapper.updateById(cycle);
        planMapper.update(null, new UpdateWrapper<TrainingPlan>()
                .eq("parent_id", cycle.getId())
                .eq("scope", PlanScope.DAY.name())
                .in("status", ACTIVE_DAY_STATUSES)
                .set("status", PlanStatus.SUPERSEDED.name()));
    }

    private TrainingPlan requireOwnedCycle(Long athleteId, Long cyclePlanId) {
        TrainingPlan cycle = planMapper.selectById(cyclePlanId);
        if (cycle == null || !athleteId.equals(cycle.getAthleteId())
                || !PlanScope.CYCLE.name().equals(cycle.getScope())) {
            throw new BizException(ErrorCode.NOT_FOUND);
        }
        return cycle;
    }
}
