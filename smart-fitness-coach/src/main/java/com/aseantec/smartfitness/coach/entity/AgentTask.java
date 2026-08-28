package com.aseantec.smartfitness.coach.entity;

import com.aseantec.smartfitness.common.entity.AuditedEntity;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Agent 任务：用户希望完成的一件事。对应表 {@code agent_task}。
 * <p>与 HTTP 请求解耦；一次 Task 可有多条 {@code coach_run}（Execution）重试。
 * 不变量：{@code status} 使用 {@link com.aseantec.smartfitness.coach.runtime.task.TaskState} 枚举名。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("agent_task")
public class AgentTask extends AuditedEntity {

    private Long athleteId;

    /** 所属 {@code conversation.id}，可空（未来非对话入口）。 */
    private Long conversationId;

    /** 任务标题，通常取自用户首句摘要。 */
    private String title;

    /**
     * {@code CREATED} | {@code RUNNING} | {@code WAITING_USER} | {@code WAITING_CONFIRMATION}
     * | {@code PAUSED} | {@code COMPLETED} | {@code FAILED} | {@code CANCELLED}。
     */
    private String status;

    /** 触发本任务的 {@code conversation_message.id}（USER 消息）。 */
    private Long sourceMessageId;
}
