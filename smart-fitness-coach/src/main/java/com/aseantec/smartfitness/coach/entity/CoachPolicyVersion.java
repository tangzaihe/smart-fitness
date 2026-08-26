package com.aseantec.smartfitness.coach.entity;

import com.aseantec.smartfitness.common.entity.BaseEntity;
import com.aseantec.smartfitness.infra.mybatis.JsonbStringTypeHandler;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.apache.ibatis.type.JdbcType;

/**
 * 教练人设的一个版本。对应表 {@code coach_policy_version}。
 * <p>回合开始时把 version id 钉到 {@link CoachRun}，运行中不切换 prompt。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName(value = "coach_policy_version", autoResultMap = true)
public class CoachPolicyVersion extends BaseEntity {

    private Long policyId;

    /** 同一 policy 下递增的版本号。 */
    private Integer version;

    private String systemPrompt;

    /** JSON：允许的工具开关。 */
    @TableField(jdbcType = JdbcType.OTHER, typeHandler = JsonbStringTypeHandler.class)
    private String toolFlags;

    /** JSON：护栏开关 G1–G5。 */
    @TableField(jdbcType = JdbcType.OTHER, typeHandler = JsonbStringTypeHandler.class)
    private String guardrails;

    /** {@code DRAFT} | {@code ACTIVE} | {@code ARCHIVED}。 */
    private String status;

    /** 灰度百分比 0–100。P0 种子为 0 且 status=ACTIVE。 */
    private Integer grayPercent;

    /** 发版人 {@code admin_user.id}，可空。 */
    private Long createdBy;
}
