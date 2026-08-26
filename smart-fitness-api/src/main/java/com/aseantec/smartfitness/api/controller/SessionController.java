package com.aseantec.smartfitness.api.controller;

import com.aseantec.smartfitness.coach.entity.Advice;
import com.aseantec.smartfitness.coach.service.AdviceService;
import com.aseantec.smartfitness.common.context.UserContext;
import com.aseantec.smartfitness.common.result.Result;
import com.aseantec.smartfitness.common.vo.PageResult;
import com.aseantec.smartfitness.readiness.service.ReadinessService;
import com.aseantec.smartfitness.training.dto.CompleteSessionRequest;
import com.aseantec.smartfitness.training.dto.PatchSetRequest;
import com.aseantec.smartfitness.training.entity.SessionLog;
import com.aseantec.smartfitness.training.entity.SetLog;
import com.aseantec.smartfitness.training.service.SessionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 进行中课、记组、完课/放弃。完课会插入新的 athletic_state。
 */
@RestController
@RequiredArgsConstructor
@Tag(name = "session")
public class SessionController {
    private final SessionService sessionService;
    private final AdviceService adviceService;
    private final ReadinessService readinessService;

    @GetMapping("/v1/sessions/current")
    @Operation(summary = "当前进行中课及组；无则 data=null")
    public Result<Map<String, Object>> current() {
        Long athleteId = UserContext.requireAthleteId();
        SessionLog session = sessionService.findInProgress(athleteId);
        if (session == null) {
            return Result.ok(null);
        }
        return Result.ok(Map.of("session", session, "sets", sessionService.listSets(session.getId())));
    }

    @GetMapping("/v1/sessions/{id}")
    @Operation(summary = "session detail")
    public Result<Map<String, Object>> detail(@PathVariable("id") Long id) {
        Long athleteId = UserContext.requireAthleteId();
        SessionLog session = sessionService.requireOwned(athleteId, id);
        return Result.ok(Map.of("session", session, "sets", sessionService.listSets(id)));
    }

    @GetMapping("/v1/sessions")
    @Operation(summary = "session history")
    public Result<PageResult<SessionLog>> history(@RequestParam(defaultValue = "1") long page,
                                                @RequestParam(defaultValue = "20") long size) {
        return Result.ok(sessionService.history(UserContext.requireAthleteId(), page, size));
    }

    @PatchMapping("/v1/sessions/{id}/sets/{setId}")
    @Operation(summary = "记一组 reps/kg/RPE；动作编码须在处方允许集合内")
    public Result<SetLog> patchSet(@PathVariable("id") Long id,
                                   @PathVariable("setId") Long setId,
                                   @RequestBody PatchSetRequest request) {
        Long athleteId = UserContext.requireAthleteId();
        SessionLog session = sessionService.requireOwned(athleteId, id);
        Advice advice = session.getAdviceId() == null ? null : adviceService.requireOwned(athleteId, session.getAdviceId());
        return Result.ok(sessionService.patchSet(athleteId, id, setId, request,
                advice == null ? java.util.Set.of() : adviceService.allowedCodes(advice)));
    }

    @PostMapping("/v1/sessions/{id}/complete")
    @Operation(summary = "完课并追加准备度快照")
    public Result<SessionLog> complete(@PathVariable("id") Long id, @RequestBody(required = false) CompleteSessionRequest request) {
        Long athleteId = UserContext.requireAthleteId();
        SessionLog session = sessionService.complete(athleteId, id, request == null ? new CompleteSessionRequest() : request);
        readinessService.snapshot(athleteId);
        return Result.ok(session);
    }

    @PostMapping("/v1/sessions/{id}/abandon")
    @Operation(summary = "放弃进行中课并追加准备度快照")
    public Result<SessionLog> abandon(@PathVariable("id") Long id) {
        Long athleteId = UserContext.requireAthleteId();
        SessionLog session = sessionService.abandon(athleteId, id);
        readinessService.snapshot(athleteId);
        return Result.ok(session);
    }
}
