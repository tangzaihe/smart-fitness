package com.aseantec.smartfitness.coach.dto;

import lombok.Builder;
import lombok.Value;

/**
 * 对话消息视图。对应 {@code GET /v1/conversations/{id}/messages} 列表项。
 */
@Value
@Builder
public class MessageVO {
    String messageId;
    String role;
    String content;
    String contentType;
    Object metadata;
    String createdAt;
}
