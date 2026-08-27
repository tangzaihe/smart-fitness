package com.aseantec.smartfitness.coach.entity;

import com.aseantec.smartfitness.common.entity.BaseEntity;
import com.aseantec.smartfitness.infra.mybatis.JsonbStringTypeHandler;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.apache.ibatis.type.JdbcType;

/**
 * 回合内或对话内有序事件，用于 SSE 回放。对应表 {@code coach_event}。
 * <p>旧 P0 路径 {@code (runId, seq)} 唯一；对话驱动路径 {@code runId} 可空（{@code ChatSkill} 不建 run），
 * 此时按 {@code (conversationId, messageId, seq)} 回放。payload 是事件体，不是最终处方（处方在 {@link Advice}）。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName(value = "coach_event", autoResultMap = true)
public class CoachEvent extends BaseEntity {

    /** 关联 {@code coach_run.id}；对话级事件（无 run）为 null。 */
    private Long runId;

    /** 关联 {@code conversation.id}，便于按线程回放。 */
    private Long conversationId;

    /** 关联 {@code conversation_message.id}（assistant 消息）。 */
    private Long messageId;

    /** 从 0 递增的序号。 */
    private Integer seq;

    /** 如 {@code token} | {@code tool} | {@code advice} | {@code error}。 */
    private String type;

    /** JSON 事件体。 */
    @TableField(jdbcType = JdbcType.OTHER, typeHandler = JsonbStringTypeHandler.class)
    private String payload;
}
