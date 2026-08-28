package com.aseantec.smartfitness.coach.runtime.tool.impl;

import com.aseantec.smartfitness.athlete.service.AthleteService;
import com.aseantec.smartfitness.athlete.vo.AthleteVO;
import com.aseantec.smartfitness.coach.runtime.tool.AgentTool;
import com.aseantec.smartfitness.coach.runtime.tool.ToolPermission;
import com.aseantec.smartfitness.coach.runtime.tool.ToolResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * 读取运动员档案与活跃约束。对应 PRD {@code get_athlete_profile}。
 */
@Component
@RequiredArgsConstructor
public class GetUserProfileTool implements AgentTool {

    private final AthleteService athleteService;

    @Override
    public String name() {
        return "getUserProfile";
    }

    @Override
    public String description() {
        return "获取用户档案、目标、器材偏好与活跃约束";
    }

    @Override
    public ToolPermission permission() {
        return ToolPermission.READ;
    }

    @Override
    public ToolResult execute(Long athleteId, Map<String, Object> arguments) {
        AthleteVO athlete = athleteService.getMe(athleteId);
        Map<String, Object> data = new HashMap<>();
        data.put("goal", athlete.getGoal());
        data.put("equipment", athlete.getEquipment());
        data.put("preferences", athlete.getPreferences());
        data.put("constraints", athleteService.listActiveConstraints(athleteId));
        return ToolResult.success(data);
    }
}
