# -*- coding: utf-8 -*-
from pathlib import Path
R = Path(r"E:\aseantec\agent\Smart Fitness")
def w(rel, t):
    p = R / rel.replace("/", "\\")
    p.parent.mkdir(parents=True, exist_ok=True)
    p.write_text(t.lstrip("\n"), encoding="utf-8")
    print(rel)

w("smart-fitness-training/src/main/java/com/aseantec/smartfitness/training/dto/PatchSetRequest.java", """
package com.aseantec.smartfitness.training.dto;

import lombok.Data;
import java.math.BigDecimal;
@Data
public class PatchSetRequest {
    private Integer reps;
    private BigDecimal loadKg;
    private BigDecimal rpe;
    private Boolean completed;
    private String exerciseCode;
}
""")

w("smart-fitness-training/src/main/java/com/aseantec/smartfitness/training/dto/CompleteSessionRequest.java", """
package com.aseantec.smartfitness.training.dto;
import lombok.Data;
@Data
public class CompleteSessionRequest {
    private Integer perceivedExertion;
    private String notes;
}
""")

w("smart-fitness-training/src/main/java/com/aseantec/smartfitness/training/service/SessionService.java", r'''
package com.aseantec.smartfitness.training.service;

import com.aseantec.smartfitness.common.exception.BizException;
import com.aseantec.smartfitness.common.exception.ErrorCode;
import com.aseantec.smartfitness.common.vo.PageResult;
import com.aseantec.smartfitness.training.dto.CompleteSessionRequest;
import com.aseantec.smartfitness.training.dto.PatchSetRequest;
import com.aseantec.smartfitness.training.entity.ExerciseCatalog;
import com.aseantec.smartfitness.training.entity.SessionLog;
import com.aseantec.smartfitness.training.entity.SetLog;
import com.aseantec.smartfitness.training.mapper.SessionLogMapper;
import com.aseantec.smartfitness.training.mapper.SetLogMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class SessionService {
    private final SessionLogMapper sessionLogMapper;
    private final SetLogMapper setLogMapper;
    private final CatalogService catalogService;
    private final ObjectMapper objectMapper;

    public SessionLog findInProgress(Long athleteId) {
        return sessionLogMapper.selectOne(new LambdaQueryWrapper<SessionLog>()
                .eq(SessionLog::getAthleteId, athleteId)
                .eq(SessionLog::getStatus, "IN_PROGRESS")
                .last("LIMIT 1"));
    }

    public SessionLog requireOwned(Long athleteId, Long sessionId) {
        SessionLog session = sessionLogMapper.selectById(sessionId);
        if (session == null || !athleteId.equals(session.getAthleteId())) {
            throw new BizException(ErrorCode.NOT_FOUND);
        }
        return session;
    }

    public List<SetLog> listSets(Long sessionId) {
        return setLogMapper.selectList(new LambdaQueryWrapper<SetLog>()
                .eq(SetLog::getSessionId, sessionId)
                .orderByAsc(SetLog::getSetIndex)
                .orderByAsc(SetLog::getId));
    }

    @Transactional
    public SessionLog createFromAdvice(Long athleteId, Long adviceId, String payloadJson) {
        if (findInProgress(athleteId) != null) {
            throw new BizException(ErrorCode.SESSION_ACTIVE);
        }
        try {
            JsonNode root = objectMapper.readTree(payloadJson);
            String kind = root.path("kind").asText();
            if ("REST".equals(kind)) {
                return null;
            }
            SessionLog session = new SessionLog();
            session.setAthleteId(athleteId);
            session.setAdviceId(adviceId);
            session.setStatus("IN_PROGRESS");
            session.setStartedAt(OffsetDateTime.now(ZoneOffset.UTC));
            sessionLogMapper.insert(session);
            int setIndex = 1;
            for (JsonNode slot : root.path("slots")) {
                String pick = slot.path("pick").asText();
                ExerciseCatalog ex = catalogService.requireByCode(pick);
                int sets = slot.path("sets").asInt(3);
                int reps = slot.path("reps").asInt(8);
                BigDecimal load = slot.hasNonNull("load_kg") ? BigDecimal.valueOf(slot.get("load_kg").asDouble()) : null;
                for (int i = 0; i < sets; i++) {
                    SetLog set = new SetLog();
                    set.setSessionId(session.getId());
                    set.setExerciseCode(pick);
                    set.setMuscleGroup(ex.getMuscleGroup());
                    set.setSetIndex(setIndex++);
                    set.setReps(reps);
                    set.setLoadKg(load);
                    set.setCompleted(false);
                    setLogMapper.insert(set);
                }
            }
            return session;
        } catch (BizException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new BizException(ErrorCode.SCHEMA_INVALID, "invalid advice payload");
        }
    }

    @Transactional
    public SetLog patchSet(Long athleteId, Long sessionId, Long setId, PatchSetRequest request, Set<String> allowedCodes) {
        SessionLog session = requireOwned(athleteId, sessionId);
        if (!"IN_PROGRESS".equals(session.getStatus())) {
            throw new BizException(ErrorCode.SESSION_STATE);
        }
        SetLog set = setLogMapper.selectById(setId);
        if (set == null || !sessionId.equals(set.getSessionId())) {
            throw new BizException(ErrorCode.NOT_FOUND);
        }
        if (Boolean.TRUE.equals(set.getCompleted()) && request.getCompleted() == null) {
            throw new BizException(ErrorCode.SESSION_STATE, "completed set cannot be patched");
        }
        if (request.getExerciseCode() != null) {
            if (allowedCodes == null || !allowedCodes.contains(request.getExerciseCode())) {
                throw new BizException(ErrorCode.BAD_REQUEST, "exercise not in pick/alternatives");
            }
            ExerciseCatalog ex = catalogService.requireByCode(request.getExerciseCode());
            set.setExerciseCode(request.getExerciseCode());
            set.setMuscleGroup(ex.getMuscleGroup());
        }
        if (request.getReps() != null) {
            set.setReps(request.getReps());
        }
        if (request.getLoadKg() != null) {
            set.setLoadKg(request.getLoadKg());
        }
        if (request.getRpe() != null) {
            set.setRpe(request.getRpe());
        }
        if (Boolean.TRUE.equals(request.getCompleted())) {
            if (set.getReps() == null) {
                throw new BizException(ErrorCode.BAD_REQUEST, "reps required when completing a set");
            }
            set.setCompleted(true);
        } else if (Boolean.FALSE.equals(request.getCompleted())) {
            set.setCompleted(false);
        }
        setLogMapper.updateById(set);
        return set;
    }

    @Transactional
    public SessionLog complete(Long athleteId, Long sessionId, CompleteSessionRequest request) {
        SessionLog session = requireOwned(athleteId, sessionId);
        if (!"IN_PROGRESS".equals(session.getStatus())) {
            throw new BizException(ErrorCode.SESSION_STATE);
        }
        session.setStatus("COMPLETED");
        session.setEndedAt(OffsetDateTime.now(ZoneOffset.UTC));
        if (request != null) {
            session.setPerceivedExertion(request.getPerceivedExertion());
            session.setNotes(request.getNotes());
        }
        sessionLogMapper.updateById(session);
        return session;
    }

    @Transactional
    public SessionLog abandon(Long athleteId, Long sessionId) {
        SessionLog session = requireOwned(athleteId, sessionId);
        if (!"IN_PROGRESS".equals(session.getStatus())) {
            throw new BizException(ErrorCode.SESSION_STATE);
        }
        session.setStatus("ABANDONED");
        session.setEndedAt(OffsetDateTime.now(ZoneOffset.UTC));
        sessionLogMapper.updateById(session);
        return session;
    }

    public PageResult<SessionLog> history(Long athleteId, long page, long size) {
        Page<SessionLog> mp = sessionLogMapper.selectPage(new Page<>(page, size),
                new LambdaQueryWrapper<SessionLog>()
                        .eq(SessionLog::getAthleteId, athleteId)
                        .in(SessionLog::getStatus, List.of("COMPLETED", "ABANDONED"))
                        .orderByDesc(SessionLog::getStartedAt));
        return new PageResult<>(mp.getRecords(), mp.getTotal(), mp.getCurrent(), mp.getSize());
    }
}
''')
print("session service ok")
