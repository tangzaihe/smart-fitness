package com.aseantec.smartfitness.coach.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 用户发送对话消息请求。对应 {@code POST /v1/conversations/{id}/messages}。
 */
@Data
public class SendMessageRequest {

    /** 用户消息文本；P0 必填，未来可支持纯 chip 触发。 */
    @NotBlank
    @Size(max = 2000)
    private String text;

    /** 可选快捷 chip 标识，如 {@code today_session} / {@code rest}，辅助意图路由。 */
    private String chip;
}
