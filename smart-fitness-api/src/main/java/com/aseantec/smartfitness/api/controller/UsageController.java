package com.aseantec.smartfitness.api.controller;

import com.aseantec.smartfitness.common.context.UserContext;
import com.aseantec.smartfitness.common.result.Result;
import com.aseantec.smartfitness.infra.usage.UsageQueryService;
import com.aseantec.smartfitness.infra.usage.entity.LlmUsageDaily;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 当前用户 SYSTEM 密钥近 7 日用量。
 */
@RestController
@RequiredArgsConstructor
@Tag(name = "usage")
public class UsageController {
    private final UsageQueryService usageQueryService;

    @GetMapping("/v1/me/usage")
    @Operation(summary = "recent SYSTEM token usage")
    public Result<List<LlmUsageDaily>> mine() {
        return Result.ok(usageQueryService.lastDays(UserContext.requireAthleteId(), 7));
    }
}
