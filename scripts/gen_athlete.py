# -*- coding: utf-8 -*-
from pathlib import Path
R = Path(r"E:\aseantec\agent\Smart Fitness")

def w(rel, t):
    p = R / rel.replace("/", "\\")
    p.parent.mkdir(parents=True, exist_ok=True)
    p.write_text(t.lstrip("\n"), encoding="utf-8")
    print(rel)

# ---------- athlete ----------
w("smart-fitness-athlete/src/main/java/com/aseantec/smartfitness/athlete/entity/Athlete.java", """
package com.aseantec.smartfitness.athlete.entity;

import com.aseantec.smartfitness.common.entity.AuditedEntity;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;
import java.time.OffsetDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("athlete")
public class Athlete extends AuditedEntity {
    private Long userId;
    private String displayName;
    private String sex;
    private LocalDate birthDate;
    private java.math.BigDecimal heightCm;
    private String goal;
    private Integer weeklyMin;
    private String equipment;
    private String preferences;
    private OffsetDateTime onboardedAt;
}
""")

w("smart-fitness-athlete/src/main/java/com/aseantec/smartfitness/athlete/entity/AthleteConstraint.java", """
package com.aseantec.smartfitness.athlete.entity;

import com.aseantec.smartfitness.common.entity.BaseEntity;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("athlete_constraint")
public class AthleteConstraint extends BaseEntity {
    private Long athleteId;
    private String type;
    private String bodyPart;
    private Integer severity;
    private LocalDate startsOn;
    private LocalDate endsOn;
    private String notes;
}
""")

w("smart-fitness-athlete/src/main/java/com/aseantec/smartfitness/athlete/entity/WellnessLog.java", """
package com.aseantec.smartfitness.athlete.entity;

import com.aseantec.smartfitness.common.entity.BaseEntity;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("wellness_log")
public class WellnessLog extends BaseEntity {
    private Long athleteId;
    private LocalDate logDate;
    private BigDecimal sleepHours;
    private Integer subjectiveFatigue;
}
""")

for name in ["Athlete", "AthleteConstraint", "WellnessLog"]:
    w(f"smart-fitness-athlete/src/main/java/com/aseantec/smartfitness/athlete/mapper/{name}Mapper.java", f"""
package com.aseantec.smartfitness.athlete.mapper;

import com.aseantec.smartfitness.athlete.entity.{name};
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface {name}Mapper extends BaseMapper<{name}> {{
}}
""")

w("smart-fitness-athlete/src/main/java/com/aseantec/smartfitness/athlete/dto/UpdateAthleteRequest.java", """
package com.aseantec.smartfitness.athlete.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Data
public class UpdateAthleteRequest {
    @NotBlank
    private String displayName;
    private String sex;
    private LocalDate birthDate;
    private BigDecimal heightCm;
    @NotBlank
    private String goal;
    private Integer weeklyMin;
    @NotEmpty
    private List<String> equipment = new ArrayList<>();
    private Preferences preferences = new Preferences();
    @Valid
    private List<ConstraintItem> constraints = new ArrayList<>();

    @Data
    public static class Preferences {
        private List<String> liked = new ArrayList<>();
        private List<String> disliked = new ArrayList<>();
        private List<String> never = new ArrayList<>();
    }

    @Data
    public static class ConstraintItem {
        @NotBlank
        private String type;
        private String bodyPart;
        private Integer severity;
        private LocalDate startsOn;
        private LocalDate endsOn;
        private String notes;
    }
}
""")

w("smart-fitness-athlete/src/main/java/com/aseantec/smartfitness/athlete/dto/WellnessRequest.java", """
package com.aseantec.smartfitness.athlete.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class WellnessRequest {
    @NotNull
    @DecimalMin("0.0")
    @DecimalMax("24.0")
    private BigDecimal sleepHours;
    @NotNull
    @Min(1)
    @Max(10)
    private Integer subjectiveFatigue;
}
""")

w("smart-fitness-athlete/src/main/java/com/aseantec/smartfitness/athlete/vo/AthleteVO.java", """
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
""")

print("athlete entities ok")
