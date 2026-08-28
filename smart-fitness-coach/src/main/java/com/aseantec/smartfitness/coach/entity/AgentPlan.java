package com.aseantec.smartfitness.coach.entity;

import com.aseantec.smartfitness.common.entity.AuditedEntity;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Agent 执行计划：复杂任务的多步分解。对应表 {@code agent_plan}。
 * <p>绑定 {@code agent_task} 与一次 {@code coach_run}（execution）。
 * 不变量：Plan 步骤通过 {@link AgentPlanStep} 物化，状态可经 Event 被前端观察。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("agent_plan")
public class AgentPlan extends AuditedEntity {

    private Long taskId;

    /** 关联 {@code coach_run.id}。 */
    private Long executionId;

    /** {@code ACTIVE} | {@code COMPLETED} | {@code CANCELLED}。 */
    private String status;
}
