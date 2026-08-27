package com.aseantec.smartfitness.coach.agent;

import com.aseantec.smartfitness.athlete.entity.AthleteConstraint;
import com.aseantec.smartfitness.athlete.service.AthleteService;
import com.aseantec.smartfitness.athlete.vo.AthleteVO;
import com.aseantec.smartfitness.coach.dto.CoachContextVO;
import com.aseantec.smartfitness.readiness.entity.AthleticState;
import com.aseantec.smartfitness.readiness.service.ReadinessService;
import com.aseantec.smartfitness.training.entity.SessionLog;
import com.aseantec.smartfitness.training.service.SessionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * L0 上下文构建：聚合档案 / 准备度 / 约束 / 进行中课次，供首屏与 prompt 注入。
 * <p>纯规则读取，不调 LLM。首屏 {@link #contextFor(Long)} 零成本展示；
 * Skill 调 LLM 前用 {@link #promptContext(Long)} 把事实拼进 user message。
 */
@Component
@RequiredArgsConstructor
public class L0ContextBuilder {

    private final AthleteService athleteService;
    private final ReadinessService readinessService;
    private final SessionService sessionService;

    /**
     * 首屏 L0 聚合。不调 LLM，进入教练对话 Tab 即返回。
     */
    public CoachContextVO contextFor(Long athleteId) {
        AthleteVO athlete = athleteService.getMe(athleteId);
        AthleticState state = readinessService.current(athleteId);
        SessionLog active = sessionService.findInProgress(athleteId);
        String greeting = buildGreeting(athlete.getDisplayName(), state.getReadiness(), active != null);
        return CoachContextVO.builder()
                .readiness(state.getReadiness())
                .readinessSource(state.getSource())
                .calcVersion(state.getCalcVersion())
                .goal(athlete.getGoal())
                .onboarded(athlete.isOnboarded())
                .hasActiveSession(active != null)
                .greeting(greeting)
                .build();
    }

    /**
     * 注入 prompt 的事实上下文（中文自然语言片段），供 Skill 拼接 user message。
     */
    public String promptContext(Long athleteId) {
        AthleteVO athlete = athleteService.getMe(athleteId);
        AthleticState state = readinessService.current(athleteId);
        List<AthleteConstraint> constraints = athleteService.listActiveConstraints(athleteId);
        SessionLog active = sessionService.findInProgress(athleteId);
        StringBuilder sb = new StringBuilder();
        sb.append("用户档案：目标=").append(athlete.getGoal())
                .append("，器材=").append(athlete.getEquipment())
                .append("。准备度=").append(state.getReadiness())
                .append("（来源 ").append(state.getSource()).append(" v").append(state.getCalcVersion()).append("）。");
        if (active != null) {
            sb.append("已有一堂进行中课次（sessionId=").append(active.getId()).append("），不应再生成新课。");
        }
        if (!constraints.isEmpty()) {
            sb.append("活跃约束：");
            for (AthleteConstraint c : constraints) {
                sb.append('[').append(c.getType());
                if (c.getBodyPart() != null) {
                    sb.append('/').append(c.getBodyPart());
                }
                sb.append("] ");
            }
        }
        return sb.toString();
    }

    private String buildGreeting(String name, int readiness, boolean hasActiveSession) {
        if (hasActiveSession) {
            return "你还有一堂进行中的训练，继续吗？";
        }
        if (readiness < 40) {
            return String.format("早安 %s，今天恢复一般，要不要休息或减量？", name);
        }
        return String.format("早安 %s，准备度 %d 不错，要看看今天练什么吗？", name, readiness);
    }
}
