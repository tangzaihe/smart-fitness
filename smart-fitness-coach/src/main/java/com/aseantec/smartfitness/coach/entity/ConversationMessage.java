package com.aseantec.smartfitness.coach.entity;

import com.aseantec.smartfitness.common.entity.BaseEntity;
import com.aseantec.smartfitness.infra.mybatis.JsonbStringTypeHandler;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.apache.ibatis.type.JdbcType;

/**
 * 对话消息。对应表 {@code conversation_message}。
 * <p>role=USER/ASSISTANT/SYSTEM；content_type=TEXT/CARD_REF/MIXED。
 * assistant 消息的 {@code metadata} 可携带 cards（advice/plan/session_summary）与 tools_used。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName(value = "conversation_message", autoResultMap = true)
public class ConversationMessage extends BaseEntity {

    private Long conversationId;

    private Long athleteId;

    /** {@code USER} | {@code ASSISTANT} | {@code SYSTEM}。 */
    private String role;

    /** 文本内容；纯卡片消息可为空串。 */
    private String content;

    /** {@code TEXT} | {@code CARD_REF} | {@code MIXED}。 */
    private String contentType;

    /** JSON：cards、tools_used、run_id 等。 */
    @TableField(jdbcType = JdbcType.OTHER, typeHandler = JsonbStringTypeHandler.class)
    private String metadata;
}
