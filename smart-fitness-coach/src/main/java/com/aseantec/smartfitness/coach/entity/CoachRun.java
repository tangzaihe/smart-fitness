package com.aseantec.smartfitness.coach.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.OffsetDateTime;

/**
 * 一次教练回合。对应表 {@code coach_run}。
 * <p>同一 athlete 同时只允许一个 {@code RUNNING}（错误码 RUN_ACTIVE）。SSE 绑定本行 id。
 */
@Data
@TableName("coach_run")
public class CoachRun {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long athleteId;

    /** 本回合钉死的 {@code coach_policy_version.id}。 */
    private Long policyVersionId;

    /** {@code RUNNING} | {@code COMPLETED} | {@code FAILED}。 */
    private String status;

    /** {@code MANUAL} | {@code APP_OPEN} 等。对话驱动后弱化。 */
    private String trigger;

    /** 所属 {@code conversation.id}；对话驱动路径必填，旧 P0 路径可空。 */
    private Long conversationId;

    /** 触发本 run 的 {@code conversation_message.id}（USER 消息）。 */
    private Long messageId;

    /** 执行本 run 的 Skill，如 {@code today_session}。 */
    private String skill;

    private String model;

    /** {@code SYSTEM} | {@code BYOK}。P0 为 SYSTEM。 */
    private String keySource;

    private Integer tokenIn;
    private Integer tokenOut;
    private OffsetDateTime startedAt;
    private OffsetDateTime endedAt;
}
