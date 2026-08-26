package com.aseantec.smartfitness.athlete.vo;

import lombok.Builder;
import lombok.Value;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

@Value
@Builder
public class AthleteVO {
    String athleteId;
    String displayName;
    String sex;
    LocalDate birthDate;
    BigDecimal heightCm;
    String goal;
    Integer weeklyMin;
    List<String> equipment;
    Object preferences;
    boolean onboarded;
    OffsetDateTime onboardedAt;
}
