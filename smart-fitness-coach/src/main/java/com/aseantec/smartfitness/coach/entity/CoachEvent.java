package com.aseantec.smartfitness.coach.entity;

import com.aseantec.smartfitness.common.entity.BaseEntity;
import com.aseantec.smartfitness.infra.mybatis.JsonbStringTypeHandler;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.apache.ibatis.type.JdbcType;

/**
 * 回合内有序事件，用于 SSE 回放。对应表 {@code coach_event}。
 * <p>{@code (runId, seq)} 唯一。payload 是事件体，不是最终处方（处方在 {@link Advice}）。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName(value = "coach_event", autoResultMap = true)
public class CoachEvent extends BaseEntity {

    private Long runId;

    /** 从 0 递增的序号。 */
    private Integer seq;

    /** 如 {@code token} | {@code tool} | {@code advice} | {@code error}。 */
    private String type;

    /** JSON 事件体。 */
    @TableField(jdbcType = JdbcType.OTHER, typeHandler = JsonbStringTypeHandler.class)
    private String payload;
}
