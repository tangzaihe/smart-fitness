package com.aseantec.smartfitness.coach.entity;

import com.aseantec.smartfitness.common.entity.BaseEntity;
import com.aseantec.smartfitness.infra.mybatis.JsonbStringTypeHandler;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.apache.ibatis.type.JdbcType;

import java.time.LocalDate;
import java.time.OffsetDateTime;

/**
 * 待用户确认的教练处方。对应表 {@code advice}。
 * <p>护栏 G1–G5 通过后才插入。LLM 不写 {@code session_log}；仅用户 ACCEPT 且类型为课表/减载时建课。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName(value = "advice", autoResultMap = true)
public class Advice extends BaseEntity {

    /** 所属 {@code coach_run.id}。 */
    private Long runId;

    /** 所属 {@code conversation.id}；对话驱动路径必填。 */
    private Long conversationId;

    /** 关联 {@code training_plan.id}（scope=DAY）。 */
    private Long trainingPlanId;

    /** 处方对应日历日，用于同日互斥时 expire PENDING。 */
    private LocalDate planDate;

    private Long athleteId;

    /** {@code SESSION} | {@code DELOAD} | {@code REST}。 */
    private String type;

    /** JSON：slots[].pick / alternatives，schema_version=1。 */
    @TableField(jdbcType = JdbcType.OTHER, typeHandler = JsonbStringTypeHandler.class)
    private String payload;

    /** JSON：检索与准备度证据，给前端展示来源。 */
    @TableField(jdbcType = JdbcType.OTHER, typeHandler = JsonbStringTypeHandler.class)
    private String evidence;

    /** {@code LOW} | {@code MED} | {@code HIGH}。 */
    private String riskLevel;

    /** P0 课表类恒为 true，必须走 decide。 */
    private Boolean confirmRequired;

    /** {@code PENDING} | {@code ACCEPTED} | {@code REJECTED}。 */
    private String status;

    private OffsetDateTime decidedAt;
}
