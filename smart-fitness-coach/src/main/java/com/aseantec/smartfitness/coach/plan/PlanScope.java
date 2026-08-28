package com.aseantec.smartfitness.coach.plan;

/**
 * 训练计划粒度。对应 {@code training_plan.scope}。
 */
public enum PlanScope {
    /** 整份周期计划（如 4 周推拉腿）。 */
    CYCLE,
    /** 单日计划（互斥键为 athlete_id + plan_date）。 */
    DAY
}
