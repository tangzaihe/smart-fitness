package com.aseantec.smartfitness.coach.runtime.tool.impl;

import com.aseantec.smartfitness.coach.runtime.tool.AgentTool;
import com.aseantec.smartfitness.coach.runtime.tool.ToolPermission;
import com.aseantec.smartfitness.coach.runtime.tool.ToolResult;
import com.aseantec.smartfitness.readiness.entity.AthleticState;
import com.aseantec.smartfitness.readiness.service.ReadinessService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 读取当前准备度快照。对应 PRD {@code get_readiness}。
 */
@Component
@RequiredArgsConstructor
public class GetReadinessTool implements AgentTool {

    private final ReadinessService readinessService;

    @Override
    public String name() {
        return "getReadiness";
    }

    @Override
    public String description() {
        return "获取当前 athletic_state 准备度快照";
    }

    @Override
    public ToolPermission permission() {
        return ToolPermission.READ;
    }

    @Override
    public ToolResult execute(Long athleteId, Map<String, Object> arguments) {
        AthleticState state = readinessService.current(athleteId);
        return ToolResult.success(Map.of(
                "readiness", state.getReadiness(),
                "recovery", state.getRecovery(),
                "source", state.getSource(),
                "calcVersion", state.getCalcVersion(),
                "fatigueByMuscle", state.getFatigueByMuscle()));
    }
}
