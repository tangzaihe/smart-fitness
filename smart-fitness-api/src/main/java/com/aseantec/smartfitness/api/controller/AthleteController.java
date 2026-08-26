package com.aseantec.smartfitness.api.controller;

import com.aseantec.smartfitness.athlete.dto.UpdateAthleteRequest;
import com.aseantec.smartfitness.athlete.dto.WellnessRequest;
import com.aseantec.smartfitness.athlete.entity.WellnessLog;
import com.aseantec.smartfitness.athlete.service.AthleteService;
import com.aseantec.smartfitness.athlete.vo.AthleteVO;
import com.aseantec.smartfitness.common.context.UserContext;
import com.aseantec.smartfitness.common.result.Result;
import com.aseantec.smartfitness.readiness.entity.AthleticState;
import com.aseantec.smartfitness.readiness.service.ReadinessService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * 档案、今日 wellness、当前准备度。需已登录；写档案会置 onboardedAt。
 */
@RestController
@RequiredArgsConstructor
@Tag(name = "athlete")
public class AthleteController {
    private final AthleteService athleteService;
    private final ReadinessService readinessService;

    @GetMapping("/v1/athlete/me")
    @Operation(summary = "当前运动员档案")
    public Result<AthleteVO> me() {
        return Result.ok(athleteService.getMe(UserContext.requireAthleteId()));
    }

    @PutMapping("/v1/athlete/me")
    @Operation(summary = "更新档案并完成 onboard")
    public Result<AthleteVO> update(@Valid @RequestBody UpdateAthleteRequest request) {
        return Result.ok(athleteService.updateMe(UserContext.requireAthleteId(), request));
    }

    @PutMapping("/v1/wellness/today")
    @Operation(summary = "写入今日睡眠/疲劳并重算准备度快照")
    public Result<WellnessLog> wellness(@Valid @RequestBody WellnessRequest request) {
        Long athleteId = UserContext.requireAthleteId();
        WellnessLog log = athleteService.upsertToday(athleteId, request);
        readinessService.snapshot(athleteId);
        return Result.ok(log);
    }

    @GetMapping("/v1/readiness/current")
    @Operation(summary = "最新准备度快照；无则当场计算")
    public Result<AthleticState> readiness() {
        return Result.ok(readinessService.current(UserContext.requireAthleteId()));
    }
}
