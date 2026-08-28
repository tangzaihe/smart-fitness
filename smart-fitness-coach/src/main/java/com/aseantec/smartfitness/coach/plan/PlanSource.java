package com.aseantec.smartfitness.coach.plan;

/**
 * 训练计划来源。对应 {@code training_plan.plan_source}。
 * <p>同一 {@link PlanScope#DAY} 日历日上，不同来源互斥，后写 supersede 先写。
 */
public enum PlanSource {
    /** 教练周期计划物化或采纳。 */
    COACH,
    /** 用户手动覆盖某日。 */
    USER,
    /** Agent 对话产出或调整。 */
    AGENT
}
