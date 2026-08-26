package com.aseantec.smartfitness.athlete.service;

import com.aseantec.smartfitness.athlete.dto.UpdateAthleteRequest;
import com.aseantec.smartfitness.athlete.dto.WellnessRequest;
import com.aseantec.smartfitness.athlete.entity.Athlete;
import com.aseantec.smartfitness.athlete.entity.AthleteConstraint;
import com.aseantec.smartfitness.athlete.entity.WellnessLog;
import com.aseantec.smartfitness.athlete.mapper.AthleteConstraintMapper;
import com.aseantec.smartfitness.athlete.mapper.AthleteMapper;
import com.aseantec.smartfitness.athlete.mapper.WellnessLogMapper;
import com.aseantec.smartfitness.athlete.vo.AthleteVO;
import com.aseantec.smartfitness.common.exception.BizException;
import com.aseantec.smartfitness.common.exception.ErrorCode;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AthleteService {
    private static final List<String> GOALS = List.of("HYPERTROPHY", "FAT_LOSS", "STRENGTH", "REHAB");
    private static final List<String> EQUIPMENT = List.of(
            "BARBELL", "DUMBBELL", "KETTLEBELL", "SMITH", "LEG_PRESS",
            "CABLE", "MACHINE", "BODYWEIGHT", "BAND", "PULLUP_BAR");

    private final AthleteMapper athleteMapper;
    private final AthleteConstraintMapper constraintMapper;
    private final WellnessLogMapper wellnessLogMapper;
    private final ObjectMapper objectMapper;

    @Transactional
    public Athlete createStub(Long userId, String email) {
        Athlete athlete = new Athlete();
        athlete.setUserId(userId);
        athlete.setDisplayName(email.contains("@") ? email.substring(0, email.indexOf('@')) : email);
        athlete.setGoal("STRENGTH");
        athlete.setEquipment("[]");
        athlete.setPreferences("{\"liked\":[],\"disliked\":[],\"never\":[]}");
        athleteMapper.insert(athlete);
        return athlete;
    }

    public Athlete requireByUserId(Long userId) {
        Athlete athlete = athleteMapper.selectOne(new LambdaQueryWrapper<Athlete>()
                .eq(Athlete::getUserId, userId).last("LIMIT 1"));
        if (athlete == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "athlete not found");
        }
        return athlete;
    }

    public Athlete require(Long athleteId) {
        Athlete athlete = athleteMapper.selectById(athleteId);
        if (athlete == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "athlete not found");
        }
        return athlete;
    }

    public AthleteVO getMe(Long athleteId) {
        return toVo(require(athleteId));
    }

    /**
     * 更新 {@code athlete} 档案与 {@code athlete_constraint}，首次写入时设置 {@code onboardedAt}。
     *
     * @throws BizException {@link ErrorCode#BAD_REQUEST} 展示名为空、目标/器材非法
     */
    @Transactional
    public AthleteVO updateMe(Long athleteId, UpdateAthleteRequest request) {
        String displayName = request.getDisplayName() == null ? "" : request.getDisplayName().trim();
        if (displayName.isBlank()) {
            throw new BizException(ErrorCode.BAD_REQUEST, "displayName is required");
        }
        if (!GOALS.contains(request.getGoal())) {
            throw new BizException(ErrorCode.BAD_REQUEST, "invalid goal");
        }
        for (String eq : request.getEquipment()) {
            if (!EQUIPMENT.contains(eq)) {
                throw new BizException(ErrorCode.BAD_REQUEST, "invalid equipment: " + eq);
            }
        }
        Athlete athlete = require(athleteId);
        athlete.setDisplayName(displayName);
        athlete.setSex(request.getSex());
        athlete.setBirthDate(request.getBirthDate());
        athlete.setHeightCm(request.getHeightCm());
        athlete.setGoal(request.getGoal());
        athlete.setWeeklyMin(request.getWeeklyMin());
        athlete.setEquipment(writeJson(request.getEquipment()));
        athlete.setPreferences(writeJson(request.getPreferences()));
        if (athlete.getOnboardedAt() == null) {
            athlete.setOnboardedAt(OffsetDateTime.now(ZoneOffset.UTC));
        }
        athleteMapper.updateById(athlete);
        constraintMapper.delete(new LambdaQueryWrapper<AthleteConstraint>()
                .eq(AthleteConstraint::getAthleteId, athleteId));
        if (request.getConstraints() != null) {
            for (UpdateAthleteRequest.ConstraintItem item : request.getConstraints()) {
                AthleteConstraint row = new AthleteConstraint();
                row.setAthleteId(athleteId);
                row.setType(item.getType());
                row.setBodyPart(item.getBodyPart());
                row.setSeverity(item.getSeverity());
                row.setStartsOn(item.getStartsOn());
                row.setEndsOn(item.getEndsOn());
                row.setNotes(item.getNotes());
                constraintMapper.insert(row);
            }
        }
        return toVo(athlete);
    }

    @Transactional
    public WellnessLog upsertToday(Long athleteId, WellnessRequest request) {
        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        WellnessLog existing = wellnessLogMapper.selectOne(new LambdaQueryWrapper<WellnessLog>()
                .eq(WellnessLog::getAthleteId, athleteId)
                .eq(WellnessLog::getLogDate, today)
                .last("LIMIT 1"));
        if (existing == null) {
            WellnessLog row = new WellnessLog();
            row.setAthleteId(athleteId);
            row.setLogDate(today);
            row.setSleepHours(request.getSleepHours());
            row.setSubjectiveFatigue(request.getSubjectiveFatigue());
            wellnessLogMapper.insert(row);
            return row;
        }
        existing.setSleepHours(request.getSleepHours());
        existing.setSubjectiveFatigue(request.getSubjectiveFatigue());
        wellnessLogMapper.updateById(existing);
        return existing;
    }

    public List<AthleteConstraint> listActiveConstraints(Long athleteId) {
        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        return constraintMapper.selectList(new LambdaQueryWrapper<AthleteConstraint>()
                .eq(AthleteConstraint::getAthleteId, athleteId)
                .and(w -> w.isNull(AthleteConstraint::getEndsOn).or().ge(AthleteConstraint::getEndsOn, today)));
    }

    public WellnessLog findWellness(Long athleteId, LocalDate date) {
        return wellnessLogMapper.selectOne(new LambdaQueryWrapper<WellnessLog>()
                .eq(WellnessLog::getAthleteId, athleteId)
                .eq(WellnessLog::getLogDate, date)
                .last("LIMIT 1"));
    }

    public void assertOnboarded(Long athleteId) {
        Athlete athlete = require(athleteId);
        if (athlete.getOnboardedAt() == null) {
            throw new BizException(ErrorCode.NOT_ONBOARDED);
        }
    }

    private AthleteVO toVo(Athlete athlete) {
        return AthleteVO.builder()
                .athleteId(String.valueOf(athlete.getId()))
                .displayName(athlete.getDisplayName())
                .sex(athlete.getSex())
                .birthDate(athlete.getBirthDate())
                .heightCm(athlete.getHeightCm())
                .goal(athlete.getGoal())
                .weeklyMin(athlete.getWeeklyMin())
                .equipment(readList(athlete.getEquipment()))
                .preferences(readJson(athlete.getPreferences()))
                .onboarded(athlete.getOnboardedAt() != null)
                .onboardedAt(athlete.getOnboardedAt())
                .build();
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception ex) {
            throw new BizException(ErrorCode.SYSTEM, "json write failed");
        }
    }

    private List<String> readList(String json) {
        try {
            if (json == null || json.isBlank()) {
                return List.of();
            }
            return objectMapper.readValue(json, new TypeReference<List<String>>() {
            });
        } catch (Exception ex) {
            return List.of();
        }
    }

    private Object readJson(String json) {
        try {
            if (json == null || json.isBlank()) {
                return java.util.Map.of();
            }
            return objectMapper.readValue(json, Object.class);
        } catch (Exception ex) {
            return java.util.Map.of();
        }
    }
}
