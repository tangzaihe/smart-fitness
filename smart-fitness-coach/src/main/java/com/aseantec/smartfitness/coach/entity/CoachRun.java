package com.aseantec.smartfitness.coach.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.OffsetDateTime;

/**
 * 一次教练回合 / Agent Execution。对应表 {@code coach_run}。
 * <p>同一 athlete 同时只允许一个 {@code RUNNING}（错误码 RUN_ACTIVE）。SSE 绑定本行 id。
 * Agent Runtime 中本行兼任 {@code Execution}，通过 {@code taskId} 关联 {@link AgentTask}。
 */
@Data
@TableName("coach_run")
public class CoachRun {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /** 所属 {@code agent_task.id}；对话驱动路径必填。 */
    private Long taskId;

    private Long athleteId;

    /** 本回合钉死的 {@code coach_policy_version.id}。 */
    private Long policyVersionId;

    /** {@code RUNNING} | {@code COMPLETED} | {@code FAILED} | {@code CANCELLED}。 */
    private String status;

    /**
     * Agent Loop 细粒度状态：{@code THINKING} | {@code TOOL_EXECUTING} | {@code WAITING_USER}
     * | {@code WAITING_CONFIRMATION} | {@code COMPLETED} | {@code FAILED} | {@code CANCELLED}。
     */
    private String executionState;

    /** Agent Loop 当前迭代次数，用于防死循环。 */
    private Integer iteration;

    /** 失败原因或最后一次异常摘要。 */
    private String errorMessage;

    /**
     * 挂起时待恢复的 {@code AgentAction} JSON（WAITING_USER / WAITING_CONFIRMATION）。
     */
    private String pendingAction;

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
