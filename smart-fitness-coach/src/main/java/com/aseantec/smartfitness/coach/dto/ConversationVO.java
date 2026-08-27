package com.aseantec.smartfitness.coach.dto;

import lombok.Builder;
import lombok.Value;

/**
 * 对话线程摘要。对应 {@code GET /v1/conversations} 列表项。
 */
@Value
@Builder
public class ConversationVO {
    String conversationId;
    String primaryIntent;
    String status;
    String title;
    String lastMessage;
    String createdAt;
}
