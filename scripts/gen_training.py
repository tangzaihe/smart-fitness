# -*- coding: utf-8 -*-
from pathlib import Path
R = Path(r"E:\aseantec\agent\Smart Fitness")
def w(rel, t):
    p = R / rel.replace("/", "\\")
    p.parent.mkdir(parents=True, exist_ok=True)
    p.write_text(t.lstrip("\n"), encoding="utf-8")
    print(rel)

w("smart-fitness-training/src/main/java/com/aseantec/smartfitness/training/entity/ExerciseCatalog.java", """
package com.aseantec.smartfitness.training.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("exercise_catalog")
public class ExerciseCatalog {
    @TableId
    private Long id;
    private String code;
    private String name;
    private String muscleGroup;
    private String equipment;
    private String pattern;
    private String swapGroup;
    private String tags;
    private Boolean isStretch;
}
""")

w("smart-fitness-training/src/main/java/com/aseantec/smartfitness/training/entity/SessionLog.java", """
package com.aseantec.smartfitness.training.entity;

import com.aseantec.smartfitness.common.entity.BaseEntity;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.OffsetDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("session_log")
public class SessionLog extends BaseEntity {
    private Long athleteId;
    private Long adviceId;
    private String status;
    private OffsetDateTime startedAt;
    private OffsetDateTime endedAt;
    private Integer perceivedExertion;
    private String notes;
}
""")

w("smart-fitness-training/src/main/java/com/aseantec/smartfitness/training/entity/SetLog.java", """
package com.aseantec.smartfitness.training.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;

@Data
@TableName("set_log")
public class SetLog {
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private Long sessionId;
    private String exerciseCode;
    private String muscleGroup;
    private Integer setIndex;
    private Integer reps;
    private BigDecimal loadKg;
    private BigDecimal rpe;
    private Boolean completed;
}
""")

for n,e in [("ExerciseCatalogMapper","ExerciseCatalog"),("SessionLogMapper","SessionLog"),("SetLogMapper","SetLog")]:
    w(f"smart-fitness-training/src/main/java/com/aseantec/smartfitness/training/mapper/{n}.java", f"""
package com.aseantec.smartfitness.training.mapper;

import com.aseantec.smartfitness.training.entity.{e};
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface {n} extends BaseMapper<{e}> {{}}
""")

w("smart-fitness-training/src/main/java/com/aseantec/smartfitness/training/service/CatalogService.java", r'''
package com.aseantec.smartfitness.training.service;

import com.aseantec.smartfitness.training.entity.ExerciseCatalog;
import com.aseantec.smartfitness.training.mapper.ExerciseCatalogMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CatalogService {
    private final ExerciseCatalogMapper catalogMapper;

    public List<ExerciseCatalog> listActive() {
        return catalogMapper.selectList(new LambdaQueryWrapper<ExerciseCatalog>()
                .eq(ExerciseCatalog::getIsStretch, false));
    }

    public ExerciseCatalog requireByCode(String code) {
        ExerciseCatalog row = catalogMapper.selectOne(new LambdaQueryWrapper<ExerciseCatalog>()
                .eq(ExerciseCatalog::getCode, code).last("LIMIT 1"));
        if (row == null) {
            throw new com.aseantec.smartfitness.common.exception.BizException(
                    com.aseantec.smartfitness.common.exception.ErrorCode.BAD_REQUEST, "unknown exercise: " + code);
        }
        return row;
    }

    public Map<String, ExerciseCatalog> mapByCode() {
        return listActive().stream().collect(Collectors.toMap(ExerciseCatalog::getCode, Function.identity()));
    }
}
''')

w("smart-fitness-training/src/main/java/com/aseantec/smartfitness/training/service/LoadQueryService.java", r'''
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
''')
print("training catalog/load ok")
