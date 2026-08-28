package com.aseantec.smartfitness.coach.runtime.tool.impl;

import com.aseantec.smartfitness.coach.runtime.tool.AgentTool;
import com.aseantec.smartfitness.coach.runtime.tool.ToolPermission;
import com.aseantec.smartfitness.coach.runtime.tool.ToolResult;
import com.aseantec.smartfitness.training.service.LoadQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Set;

/**
 * 查询近 7 日肌群负荷与 48h 内练过的肌群。对应 PRD {@code get_recent_load}。
 */
@Component
@RequiredArgsConstructor
public class GetRecentLoadTool implements AgentTool {

    private final LoadQueryService loadQueryService;

    @Override
    public String name() {
        return "getRecentLoad";
    }

    @Override
    public String description() {
        return "获取近 7 日各肌群完成组数与 48h 内练过的肌群";
    }

    @Override
    public ToolPermission permission() {
        return ToolPermission.READ;
    }

    @Override
    public ToolResult execute(Long athleteId, Map<String, Object> arguments) {
        Map<String, Integer> setsByMuscle = loadQueryService.completedSetsLast7Days(athleteId);
        Set<String> recentMuscles = loadQueryService.musclesCompletedWithinHours(athleteId, 48);
        return ToolResult.success(Map.of(
                "setsByMuscle7d", setsByMuscle,
                "musclesWithin48h", recentMuscles));
    }
}
