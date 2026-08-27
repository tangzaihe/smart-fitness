package com.aseantec.smartfitness.coach.entity;

import com.aseantec.smartfitness.common.entity.AuditedEntity;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 对话线程。对应表 {@code conversation}。
 * <p>对话驱动健身的主入口：用户消息进入 {@code ConversationService}，
 * 由 {@code AgentOrchestrator} 选 Skill 执行并流式回写 assistant 消息。
 * 不变量：同一 athlete 可有多条 OPEN 线程，但同一 conversation 同时只允许一条流式回复。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("conversation")
public class Conversation extends AuditedEntity {

    private Long athleteId;

    /** {@code CHAT} | {@code TODAY_SESSION} | {@code WORKOUT_PLANNING} | {@code REST}。 */
    private String primaryIntent;

    /** {@code OPEN} | {@code ARCHIVED}。 */
    private String status;

    /** 自动生成标题，如「今日训练」「4 周增肌计划」。 */
    private String title;

    /** 滚动摘要，P0 暂不维护。 */
    private String summary;

    /** 采纳计划后绑定；P0 暂不使用。 */
    private Long coachPlanId;
}
