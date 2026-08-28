package com.aseantec.smartfitness.coach.entity;

import com.aseantec.smartfitness.common.entity.AuditedEntity;
import com.aseantec.smartfitness.infra.mybatis.JsonbStringTypeHandler;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.apache.ibatis.type.JdbcType;

import java.time.LocalDate;

/**
 * 统一训练计划。对应表 {@code training_plan}。
 * <p>scope=CYCLE 为宏观周期；scope=DAY 为日级权威（同一 athlete+plan_date 仅一条 ACTIVE/PENDING）。
 * plan_source 区分 COACH|USER|AGENT，互斥由 {@link com.aseantec.smartfitness.coach.plan.PlanCommandService} 保证。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName(value = "training_plan", autoResultMap = true)
public class TrainingPlan extends AuditedEntity {

    private Long athleteId;

    /** {@code CYCLE} | {@code DAY}。 */
    private String scope;

    /** {@code COACH} | {@code USER} | {@code AGENT}。 */
    private String planSource;

    /**
     * CYCLE: DRAFT|ACTIVE|PAUSED|COMPLETED|ABANDONED；
     * DAY: PENDING|ACTIVE|DONE|SKIPPED|ADJUSTED|SUPERSEDED。
     */
    private String status;

    /** scope=CYCLE：计划标题。 */
    private String title;

    /** scope=CYCLE：HYPERTROPHY / STRENGTH 等。 */
    private String goal;

    private Integer durationDays;

    /** scope=CYCLE：完整计划 JSON（schema_version=1）。 */
    @TableField(jdbcType = JdbcType.OTHER, typeHandler = JsonbStringTypeHandler.class)
    private String payload;

    private Integer currentDayIndex;

    /** scope=CYCLE：计划起始日历日。 */
    private LocalDate startedOn;

    private Long conversationId;

    /** scope=DAY：日历日（互斥键）。 */
    private LocalDate planDate;

    /** scope=DAY：「腿日」「休息」。 */
    private String label;

    /** scope=DAY：结构意图 JSON，非最终动作 code。 */
    @TableField(jdbcType = JdbcType.OTHER, typeHandler = JsonbStringTypeHandler.class)
    private String sessionTemplate;

    /** scope=DAY 且来自周期物化时，指向 scope=CYCLE 父行。 */
    private Long parentId;

    /** 被哪条 plan 替换（审计）。 */
    private Long supersededBy;

    private Long resolvedAdviceId;
}
