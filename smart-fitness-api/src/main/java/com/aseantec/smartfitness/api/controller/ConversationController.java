package com.aseantec.smartfitness.api.controller;

import com.aseantec.smartfitness.coach.dto.CoachContextVO;
import com.aseantec.smartfitness.coach.dto.ConversationVO;
import com.aseantec.smartfitness.coach.dto.MessageVO;
import com.aseantec.smartfitness.coach.dto.SendMessageRequest;
import com.aseantec.smartfitness.coach.service.ConversationService;
import com.aseantec.smartfitness.common.context.UserContext;
import com.aseantec.smartfitness.common.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;

/**
 * 对话驱动教练资源。需已 onboard。
 */
@RestController
@RequiredArgsConstructor
@Tag(name = "conversation")
public class ConversationController {

    private final ConversationService conversationService;

    @PostMapping("/v1/conversations")
    @Operation(summary = "新建对话线程", description = "primaryIntent 可空默认 CHAT。")
    public Result<ConversationVO> create(@RequestParam(required = false) String primaryIntent,
                                          @RequestParam(required = false) String title) {
        var c = conversationService.create(UserContext.requireAthleteId(), primaryIntent, title);
        return Result.ok(ConversationVO.builder()
                .conversationId(c.getId().toString())
                .primaryIntent(c.getPrimaryIntent())
                .status(c.getStatus())
                .title(c.getTitle())
                .createdAt(c.getCreatedAt() == null ? null : c.getCreatedAt().toString())
                .build());
    }

    @GetMapping("/v1/conversations")
    @Operation(summary = "列出我的对话线程")
    public Result<List<ConversationVO>> list() {
        return Result.ok(conversationService.list(UserContext.requireAthleteId()));
    }

    @GetMapping("/v1/conversations/{id}/messages")
    @Operation(summary = "取对话历史消息", description = "按时间升序。")
    public Result<List<MessageVO>> messages(@PathVariable("id") Long id) {
        return Result.ok(conversationService.messages(UserContext.requireAthleteId(), id));
    }

    @PostMapping(value = "/v1/conversations/{id}/messages", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(summary = "发送消息并流式回复", description = "SSE：message.start/token/tool.*/card.advice/message.done/error。")
    public SseEmitter send(@PathVariable("id") Long id, @Valid @RequestBody SendMessageRequest request) {
        return conversationService.sendMessage(UserContext.requireAthleteId(), id, request.getText());
    }

    @GetMapping("/v1/coach/context")
    @Operation(summary = "教练首屏 L0 上下文", description = "纯规则，不调 LLM。")
    public Result<CoachContextVO> context() {
        return Result.ok(conversationService.context(UserContext.requireAthleteId()));
    }
}
