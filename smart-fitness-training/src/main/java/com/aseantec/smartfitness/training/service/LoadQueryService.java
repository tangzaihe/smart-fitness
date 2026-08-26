package com.aseantec.smartfitness.training.service;

import com.aseantec.smartfitness.training.entity.SessionLog;
import com.aseantec.smartfitness.training.entity.SetLog;
import com.aseantec.smartfitness.training.mapper.SessionLogMapper;
import com.aseantec.smartfitness.training.mapper.SetLogMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class LoadQueryService {
    private final SessionLogMapper sessionLogMapper;
    private final SetLogMapper setLogMapper;

    public Map<String, Integer> completedSetsLast7Days(Long athleteId) {
        OffsetDateTime from = OffsetDateTime.now(ZoneOffset.UTC).minusDays(7);
        List<SessionLog> sessions = sessionLogMapper.selectList(new LambdaQueryWrapper<SessionLog>()
                .eq(SessionLog::getAthleteId, athleteId)
                .eq(SessionLog::getStatus, "COMPLETED")
                .ge(SessionLog::getEndedAt, from));
        if (sessions.isEmpty()) {
            return Map.of();
        }
        List<Long> ids = sessions.stream().map(SessionLog::getId).toList();
        List<SetLog> sets = setLogMapper.selectList(new LambdaQueryWrapper<SetLog>()
                .in(SetLog::getSessionId, ids)
                .eq(SetLog::getCompleted, true));
        Map<String, Integer> counts = new HashMap<>();
        for (SetLog set : sets) {
            if (set.getMuscleGroup() == null) {
                continue;
            }
            counts.merge(set.getMuscleGroup(), 1, Integer::sum);
        }
        return counts;
    }

    public Set<String> musclesCompletedWithinHours(Long athleteId, int hours) {
        OffsetDateTime from = OffsetDateTime.now(ZoneOffset.UTC).minusHours(hours);
        List<SessionLog> sessions = sessionLogMapper.selectList(new LambdaQueryWrapper<SessionLog>()
                .eq(SessionLog::getAthleteId, athleteId)
                .eq(SessionLog::getStatus, "COMPLETED")
                .ge(SessionLog::getEndedAt, from));
        if (sessions.isEmpty()) {
            return Set.of();
        }
        List<Long> ids = sessions.stream().map(SessionLog::getId).toList();
        List<SetLog> sets = setLogMapper.selectList(new LambdaQueryWrapper<SetLog>()
                .in(SetLog::getSessionId, ids)
                .eq(SetLog::getCompleted, true));
        return sets.stream().map(SetLog::getMuscleGroup).filter(v -> v != null).collect(Collectors.toCollection(HashSet::new));
    }
}
