package com.aseantec.smartfitness.coach.runtime.tool.impl;

import com.aseantec.smartfitness.coach.runtime.tool.AgentTool;
import com.aseantec.smartfitness.coach.runtime.tool.ToolPermission;
import com.aseantec.smartfitness.coach.runtime.tool.ToolResult;
import com.aseantec.smartfitness.training.entity.SessionLog;
import com.aseantec.smartfitness.training.mapper.SessionLogMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;

/**
 * 查询近期训练课次摘要。对应架构方案 {@code getTrainingRecords}。
 */
@Component
@RequiredArgsConstructor
public class GetTrainingRecordsTool implements AgentTool {

    private final SessionLogMapper sessionLogMapper;

    @Override
    public String name() {
        return "getTrainingRecords";
    }

    @Override
    public String description() {
        return "查询指定天数内已完成训练课次数量与列表";
    }

    @Override
    public ToolPermission permission() {
        return ToolPermission.READ;
    }

    @Override
    public ToolResult execute(Long athleteId, Map<String, Object> arguments) {
        int days = 30;
        if (arguments != null && arguments.get("days") instanceof Number n) {
            days = n.intValue();
        }
        OffsetDateTime from = OffsetDateTime.now(ZoneOffset.UTC).minusDays(days);
        List<SessionLog> sessions = sessionLogMapper.selectList(new LambdaQueryWrapper<SessionLog>()
                .eq(SessionLog::getAthleteId, athleteId)
                .eq(SessionLog::getStatus, "COMPLETED")
                .ge(SessionLog::getEndedAt, from)
                .orderByDesc(SessionLog::getEndedAt));
        List<Map<String, Object>> items = sessions.stream()
                .map(s -> Map.<String, Object>of(
                        "sessionId", s.getId(),
                        "startedAt", s.getStartedAt() == null ? "" : s.getStartedAt().toString(),
                        "endedAt", s.getEndedAt() == null ? "" : s.getEndedAt().toString()))
                .toList();
        return ToolResult.success(Map.of(
                "days", days,
                "count", sessions.size(),
                "sessions", items));
    }
}
