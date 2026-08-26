# -*- coding: utf-8 -*-
from pathlib import Path
R = Path(r"E:\aseantec\agent\Smart Fitness")
def w(rel, t):
    p = R / rel.replace("/", "\\")
    p.parent.mkdir(parents=True, exist_ok=True)
    p.write_text(t.lstrip("\n"), encoding="utf-8")
    print(rel)

w("smart-fitness-coach/src/main/java/com/aseantec/smartfitness/coach/entity/Advice.java", """
package com.aseantec.smartfitness.coach.entity;

import com.aseantec.smartfitness.common.entity.BaseEntity;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.time.OffsetDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("advice")
public class Advice extends BaseEntity {
    private Long runId;
    private Long athleteId;
    private String type;
    private String payload;
    private String evidence;
    private String riskLevel;
    private Boolean confirmRequired;
    private String status;
    private OffsetDateTime decidedAt;
}
""")

w("smart-fitness-coach/src/main/java/com/aseantec/smartfitness/coach/entity/CoachRun.java", """
package com.aseantec.smartfitness.coach.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.OffsetDateTime;

@Data
@TableName("coach_run")
public class CoachRun {
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private Long athleteId;
    private Long policyVersionId;
    private String status;
    private String trigger;
    private String model;
    private String keySource;
    private Integer tokenIn;
    private Integer tokenOut;
    private OffsetDateTime startedAt;
    private OffsetDateTime endedAt;
}
""")

w("smart-fitness-coach/src/main/java/com/aseantec/smartfitness/coach/entity/CoachEvent.java", """
package com.aseantec.smartfitness.coach.entity;

import com.aseantec.smartfitness.common.entity.BaseEntity;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("coach_event")
public class CoachEvent extends BaseEntity {
    private Long runId;
    private Integer seq;
    private String type;
    private String payload;
}
""")

w("smart-fitness-coach/src/main/java/com/aseantec/smartfitness/coach/entity/DecisionEvent.java", """
package com.aseantec.smartfitness.coach.entity;

import com.aseantec.smartfitness.common.entity.BaseEntity;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("decision_event")
public class DecisionEvent extends BaseEntity {
    private Long adviceId;
    private Long athleteId;
    private String action;
    private String note;
}
""")

w("smart-fitness-coach/src/main/java/com/aseantec/smartfitness/coach/entity/CoachPolicyVersion.java", """
package com.aseantec.smartfitness.coach.entity;

import com.aseantec.smartfitness.common.entity.BaseEntity;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("coach_policy_version")
public class CoachPolicyVersion extends BaseEntity {
    private Long policyId;
    private Integer version;
    private String systemPrompt;
    private String toolFlags;
    private String guardrails;
    private String status;
    private Integer grayPercent;
    private Long createdBy;
}
""")

for n,e in [("AdviceMapper","Advice"),("CoachRunMapper","CoachRun"),("CoachEventMapper","CoachEvent"),
            ("DecisionEventMapper","DecisionEvent"),("CoachPolicyVersionMapper","CoachPolicyVersion")]:
    w(f"smart-fitness-coach/src/main/java/com/aseantec/smartfitness/coach/mapper/{n}.java", f"""
package com.aseantec.smartfitness.coach.mapper;
import com.aseantec.smartfitness.coach.entity.{e};
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
@Mapper
public interface {n} extends BaseMapper<{e}> {{}}
""")

print("coach entities ok")
