package com.aseantec.smartfitness.coach.plan;

import lombok.Builder;
import lombok.Value;

import java.time.LocalDate;

/**
 * 声明一条日计划的输入（claimDay）。
 */
@Value
@Builder
public class DayPlanCommand {

    LocalDate planDate;
    PlanSource planSource;
    String label;
    /** JSON 字符串，session_template。 */
    String sessionTemplate;
    /** scope=DAY 且来自周期物化时的父 CYCLE id。 */
    Long parentId;
}
