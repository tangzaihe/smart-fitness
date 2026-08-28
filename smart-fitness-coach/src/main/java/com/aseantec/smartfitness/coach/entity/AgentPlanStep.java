package com.aseantec.smartfitness.coach.entity;

import com.aseantec.smartfitness.common.entity.AuditedEntity;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Agent 计划步骤。对应表 {@code agent_plan_step}。
 * <p>按 {@code step_order} 排序；状态 {@code PENDING|RUNNING|WAITING|COMPLETED|FAILED|SKIPPED}。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("agent_plan_step")
public class AgentPlanStep extends AuditedEntity {

    private Long planId;

    private Integer stepOrder;

    private String name;

    private String description;

    /** {@code PENDING} | {@code RUNNING} | {@code WAITING} | {@code COMPLETED} | {@code FAILED} | {@code SKIPPED}。 */
    private String status;
}
