package com.aseantec.smartfitness.api.controller;

import com.aseantec.smartfitness.coach.dto.DecideRequest;
import com.aseantec.smartfitness.coach.service.AdviceService;
import com.aseantec.smartfitness.coach.service.CoachRunService;
import com.aseantec.smartfitness.common.context.UserContext;
import com.aseantec.smartfitness.common.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Map;

/**
 * 教练回合 SSE 与处方确认。需已 onboard；同时仅一个 RUNNING。
 */
@RestController
@RequiredArgsConstructor
@Tag(name = "coach")
public class CoachController {
    private final CoachRunService coachRunService;
    private final AdviceService adviceService;

    @PostMapping(value = "/v1/coach/runs", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(summary = "开启教练回合（SSE）", description = "需 Authorization Bearer。已有 RUNNING 则 3007。")
    public SseEmitter start(@RequestParam(required = false) String trigger) {
        return coachRunService.start(UserContext.requireAthleteId(), trigger);
    }

    @PostMapping("/v1/advice/{id}/decide")
    @Operation(summary = "确认/休息/拒绝处方", description = "重复 ACCEPT 已确认单返回原 sessionId，不新建课。")
    public Result<Map<String, Object>> decide(@PathVariable("id") Long id, @Valid @RequestBody DecideRequest request) {
        return Result.ok(adviceService.decide(UserContext.requireAthleteId(), id, request));
    }
}
