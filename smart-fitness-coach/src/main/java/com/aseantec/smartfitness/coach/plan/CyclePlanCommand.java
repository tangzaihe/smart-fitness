package com.aseantec.smartfitness.coach.plan;

import lombok.Builder;
import lombok.Value;

import java.time.LocalDate;
import java.util.List;

/**
 * 采纳周期计划的输入。
 */
@Value
@Builder
public class CyclePlanCommand {

    String title;
    String goal;
    LocalDate startedOn;
    Long conversationId;
    /** 完整计划 JSON（schema_version=1），含 slots[]。 */
    String payload;
    List<DayPlanCommand> days;
}
