package com.aseantec.smartfitness.api.controller;

import com.aseantec.smartfitness.common.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 存活探针，无需登录。
 */
@RestController
@Tag(name = "health")
public class HealthController {
    @GetMapping("/v1/health")
    @Operation(summary = "liveness")
    public Result<Map<String, String>> health() {
        return Result.ok(Map.of("status", "UP"));
    }
}
