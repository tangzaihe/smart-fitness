package com.aseantec.smartfitness.api.controller;

import com.aseantec.smartfitness.coach.entity.CoachEvent;
import com.aseantec.smartfitness.coach.service.AgentExecutionService;
import com.aseantec.smartfitness.common.context.UserContext;
import com.aseantec.smartfitness.common.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * Agent Execution 资源：事件回放、确认、取消。对应架构方案 {@code /api/agent/executions/*}。
 */
@RestController
@RequiredArgsConstructor
@Tag(name = "agent-execution")
public class AgentExecutionController {

    private final AgentExecutionService executionService;

    @GetMapping("/v1/agent/executions/{id}/events")
    @Operation(summary = "回放 Execution 事件", description = "SSE 断线后拉取历史事件。")
    public Result<List<CoachEvent>> events(@PathVariable("id") Long id) {
        return Result.ok(executionService.events(UserContext.requireAthleteId(), id));
    }

    @PostMapping("/v1/agent/executions/{id}/confirm")
    @Operation(summary = "确认或拒绝挂起操作")
    public Result<Map<String, Object>> confirm(@PathVariable("id") Long id,
                                                @RequestBody Map<String, Object> body) throws Exception {
        boolean approved = Boolean.TRUE.equals(body.get("approved"));
        return Result.ok(executionService.confirm(UserContext.requireAthleteId(), id, approved));
    }

    @PostMapping("/v1/agent/executions/{id}/cancel")
    @Operation(summary = "取消 Execution")
    public Result<Map<String, Object>> cancel(@PathVariable("id") Long id) {
        return Result.ok(executionService.cancel(UserContext.requireAthleteId(), id));
    }
}
