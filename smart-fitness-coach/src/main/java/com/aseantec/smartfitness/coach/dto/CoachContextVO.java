package com.aseantec.smartfitness.coach.dto;

import lombok.Builder;
import lombok.Value;

/**
 * 教练首屏 L0 上下文聚合。对应 {@code GET /v1/coach/context}。
 * <p>纯规则数据，不调 LLM；客户端进入教练对话 Tab 即展示。
 */
@Value
@Builder
public class CoachContextVO {
    int readiness;
    String readinessSource;
    String calcVersion;
    String goal;
    boolean onboarded;
    boolean hasActiveSession;
    String greeting;
}
