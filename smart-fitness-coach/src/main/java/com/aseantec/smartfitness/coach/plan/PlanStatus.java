package com.aseantec.smartfitness.coach.plan;

/**
 * 训练计划状态。对应 {@code training_plan.status}。
 * <p>CYCLE 与 DAY 共用枚举名，写入时按 scope 选用合法子集。
 */
public enum PlanStatus {
    DRAFT,
    ACTIVE,
    PENDING,
    PAUSED,
    DONE,
    SKIPPED,
    ADJUSTED,
    SUPERSEDED,
    ABANDONED,
    COMPLETED
}
