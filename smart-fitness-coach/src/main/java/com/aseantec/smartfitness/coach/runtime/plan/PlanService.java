package com.aseantec.smartfitness.coach.runtime.plan;

import com.aseantec.smartfitness.coach.entity.AgentPlan;
import com.aseantec.smartfitness.coach.entity.AgentPlanStep;
import com.aseantec.smartfitness.coach.mapper.AgentPlanMapper;
import com.aseantec.smartfitness.coach.mapper.AgentPlanStepMapper;
import com.aseantec.smartfitness.coach.runtime.event.AgentEventPublisher;
import com.aseantec.smartfitness.coach.runtime.event.AgentEventType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

/**
 * Agent 执行计划持久化与步骤状态更新。
 */
@Service
@RequiredArgsConstructor
public class PlanService {

    private final AgentPlanMapper planMapper;
    private final AgentPlanStepMapper stepMapper;
    private final AgentEventPublisher eventPublisher;

    /**
     * 为 Task/Execution 创建 Plan 及步骤。
     */
    @Transactional
    public AgentPlan create(Long taskId, Long executionId, List<String> stepNames) {
        AgentPlan plan = new AgentPlan();
        plan.setTaskId(taskId);
        plan.setExecutionId(executionId);
        plan.setStatus("ACTIVE");
        planMapper.insert(plan);
        int order = 1;
        for (String name : stepNames) {
            AgentPlanStep step = new AgentPlanStep();
            step.setPlanId(plan.getId());
            step.setStepOrder(order++);
            step.setName(name);
            step.setDescription(name);
            step.setStatus("PENDING");
            stepMapper.insert(step);
        }
        eventPublisher.publish(executionId, AgentEventType.PLAN_CREATED,
                Map.of("planId", plan.getId(), "steps", stepNames));
        return plan;
    }

    @Transactional
    public void markStep(Long executionId, Long planId, int stepOrder, String status) {
        AgentPlanStep step = stepMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<AgentPlanStep>()
                        .eq(AgentPlanStep::getPlanId, planId)
                        .eq(AgentPlanStep::getStepOrder, stepOrder)
                        .last("LIMIT 1")).stream().findFirst().orElse(null);
        if (step != null) {
            step.setStatus(status);
            stepMapper.updateById(step);
            eventPublisher.publish(executionId, AgentEventType.PLAN_UPDATED,
                    Map.of("planId", planId, "stepOrder", stepOrder, "status", status));
        }
    }
}
